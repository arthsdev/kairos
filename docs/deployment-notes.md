# Notas de Deploy — Kairos na AWS EC2

## Arquitetura

Uma única instância EC2 (`c7i-flex.large`) rodando toda a stack via Docker Compose: MySQL, Redis, RabbitMQ, Keycloak e o backend Spring Boot, todos na mesma rede Docker. O frontend é hospedado separadamente na Vercel. O Nginx roda diretamente no host (não containerizado) como proxy reverso, terminando TLS via Let's Encrypt e roteando por subdomínio. Sem serviços gerenciados da AWS (RDS, ElastiCache) por restrição de orçamento, tudo roda em containers num único host, exposto através de um Elastic IP.

## Estrutura de domínios

| Domínio | Serve |
|---|---|
| `kairos-alert.me` (raiz) | Frontend (Vercel), via registro A para o IP da Vercel |
| `api.kairos-alert.me` | API do backend, via registro A para o Elastic IP |
| `auth.kairos-alert.me` | Keycloak, via registro A para o Elastic IP |

O domínio raiz originalmente apontava para o Elastic IP e servia a API diretamente. Foi reorganizado depois para servir o frontend, assim que um domínio próprio ficou disponível (registrado via Namecheap; vale notar que alguns benefícios específicos de registrador vêm atrelados a hospedagem via GitHub Pages e não oferecem controle livre de DNS, então um registro comum foi usado no lugar, para ter flexibilidade total de DNS).

Mover o domínio raiz da API para o frontend exigiu atualizar `CORS_ALLOWED_ORIGINS`, `STRIPE_SUCCESS_URL`, `STRIPE_CANCEL_URL` e o `VITE_API_BASE_URL` do frontend, além de recadastrar o endpoint de webhook do Stripe (o que estava configurado ainda apontava para o domínio raiz, que depois da mudança passou a retornar 404 da Vercel em vez de alcançar o backend).

## O problema central: duas audiências, um só Keycloak

Dois tipos bem diferentes de cliente precisam alcançar o Keycloak, com restrições opostas:

- Clientes externos (navegadores, Swagger, o frontend) só conseguem alcançar o Keycloak através do seu endereço público.
- O próprio backend, chamando o Keycloak diretamente para operações administrativas, login e validação de token, roda dentro do mesmo host Docker que o Keycloak. Usar o endereço público para esse tráfego dispara hairpin NAT: a requisição sai do container, tenta voltar para o mesmo host através do seu endereço público, e o roteamento de rede da VPC da AWS não permite esse retorno. O resultado é um timeout silencioso, não um erro claro.

A correção está em reconhecer que conectividade e identidade são duas preocupações separadas que, sem cuidado, acabam compartilhando a mesma variável:

- Conectividade (qual endereço realmente alcança o Keycloak de dentro do container) sempre usa o hostname interno do Docker: `KEYCLOAK_INTERNAL_URL=http://keycloak:8080`.
- Identidade (o que o Keycloak grava nos tokens como `iss`, e o que o backend espera ao validá-los) sempre precisa ser a URL pública: `KEYCLOAK_URL=https://auth.kairos-alert.me`.

Sem separar essas duas coisas deliberadamente, corrigir uma quebra a outra, que foi exatamente o que aconteceu ao longo desse deploy, em várias iterações:

1. Chamadas do backend para o Keycloak (login, token de admin) usavam a URL pública, causando timeouts de hairpin NAT.
2. Trocar essas chamadas para a URL interna resolveu o timeout, mas o Keycloak passou a gravar o `iss` com o hostname interno, já que essa era a URL usada para pedir o token. Todo token emitido falhava a validação de issuer contra o `issuer-uri` público.
3. Reverter para a URL pública nas chamadas que emitem token resolveu o descompasso de issuer, mas reintroduziu o timeout de hairpin NAT.
4. Correção real: a configuração `KC_HOSTNAME` do Keycloak desacopla as duas coisas. Configurada com o hostname público, ela faz o Keycloak sempre gravar o issuer público, independente de qual caminho de rede foi usado para alcançá-lo. Isso permitiu que toda chamada backend-para-Keycloak voltasse a usar a URL interna, com issuers públicos corretos em todo token.

Uma instância mais sutil do mesmo problema apareceu uma camada mais abaixo: quando só o `issuer-uri` está configurado, o decodificador de JWT do Spring Security descobre automaticamente o endpoint de JWKS (chaves de assinatura) chamando a própria URL do issuer, a pública, de dentro do container. Mesmo hairpin NAT, dessa vez dentro da configuração automática do Spring, não em código da aplicação. Corrigido configurando também o `jwk-set-uri` explicitamente com a URL interna: o Spring passa a buscar as chaves internamente, enquanto continua validando o claim `iss` contra o `issuer-uri` público como uma comparação de string local (sem chamada de rede).

## HTTPS atrás de um proxy reverso

O Nginx termina o TLS e repassa HTTP puro para o Keycloak internamente. Por padrão, o Keycloak não tem como saber que a requisição original era HTTPS, e grava `http://` em toda URL autorreferencial que gera, incluindo o issuer. Duas configurações corrigem isso:

- `KC_PROXY=edge` e `KC_PROXY_HEADERS=xforwarded`: diz ao Keycloak para confiar nos headers `X-Forwarded-Proto`/`X-Forwarded-For` enviados pelo Nginx, em vez da conexão crua (interna, HTTP puro).
- `KC_HOSTNAME_STRICT_HTTPS=true`: força o Keycloak a sempre reportar `https://` para o hostname público configurado, independente do protocolo usado na conexão interna. Isso importa especificamente porque `AuthClient` e `KeycloakAdminClient` propositalmente não passam pelo Nginx (ver acima), então nenhum header `X-Forwarded-Proto` está presente nessas chamadas; sem essa configuração, tokens emitidos via a conexão interna carregavam `iss: http://...`, falhando a validação de issuer.

As duas configurações são controladas por variável de ambiente, com padrões seguros para o ambiente local (`none`/vazio para o modo proxy, `false` para HTTPS estrito), já que o desenvolvimento local não tem proxy reverso e precisa do comportamento oposto.

## `KC_HOSTNAME_PORT` vs. o Admin Console: uma tensão não resolvida

Um `KC_HOSTNAME_PORT=443` explícito foi adicionado para corrigir tokens cujo claim `iss` omitia a porta por completo (não batendo com o que o `issuer-uri` esperava sem ela). Isso corrigiu o problema de issuer, mas fez o Admin Console do Keycloak (`/admin/master/console/`) travar indefinidamente em "Loading the Admin UI" em produção, em três navegadores diferentes, sem erro no console e sem nenhuma requisição de rede falhando. Isso aconteceu mesmo com `KC_HOSTNAME_STRICT_HTTPS=true`, `KC_PROXY`/`KC_PROXY_HEADERS` corretos, e o próprio endpoint de debug do Keycloak (`/realms/master/hostname-debug`, habilitado via `KC_HOSTNAME_DEBUG=true`) reportando as URLs de frontend, backend e admin todas como `[OK]`.

A causa não foi identificada além de "uma porta de hostname explícita quebra especificamente o Admin Console, mesmo quando o hostname-debug reporta consistência total." Isso bate com um relato externo do mesmo sintoma com a mesma correção, mas nenhuma causa raiz foi encontrada no código-fonte ou na documentação do Keycloak.

**Resolução atual**: `KC_HOSTNAME_PORT` está comentado diretamente no `docker-compose.yml` da VM, uma edição local, não versionada, seguindo o mesmo padrão do `keycloak/kairos-realm.json`. O `KEYCLOAK_URL` no `.env` da VM foi alterado para omitir a porta (`https://auth.kairos-alert.me`, sem `:443`), batendo com o que o Keycloak gera sem `KC_HOSTNAME_PORT` definido.

Isso não pôde ser aplicado ao `docker-compose.yml` versionado, porque o desenvolvimento local precisa do oposto: `KC_HOSTNAME_PORT=8080` explícito, batendo com a porta real em que o Keycloak é alcançado localmente, sem proxy reverso envolvido. Os dois ambientes têm necessidades genuinamente incompatíveis para essa configuração, e nenhum padrão de variável de ambiente existente (com seu fallback `:-padrão`) acomoda os dois sem quebrar um ou outro.

**Consequência prática**: qualquer `git pull` na VM precisa dar `git stash` tanto em `keycloak/kairos-realm.json` quanto em `docker-compose.yml` antes de puxar, e depois `git stash pop`, para não perder nenhuma das duas edições locais para um conflito de merge.

## Secrets do realm do Keycloak

O `keycloak/kairos-realm.json` do repositório vem com secrets triviais de desenvolvimento (`kairos-local-dev-secret`, etc.), para que um clone novo funcione de primeira localmente. Na VM, esse arquivo é editado apenas localmente, com secrets reais gerados (clients `kairos-api` e `kairos-admin-service`), mantidos em sincronia com os valores equivalentes no `.env` da VM. Essa versão editada nunca é commitada de volta; o `git status` na VM sempre deve mostrar `kairos-realm.json` como modificado localmente, e isso é esperado.

## Tempo de boot e healthchecks

O Keycloak (Quarkus + import de realm + migrations do Liquibase) leva cerca de 60 a 90 segundos para ficar pronto, mais tempo que o próprio boot do backend. Um healthcheck real foi adicionado usando um redirecionamento `/dev/tcp` do bash contra o endpoint `/health/ready` do Keycloak (a imagem não tem nem `curl` nem `wget` disponíveis), com `KC_HEALTH_ENABLED=true` e um `start_period: 90s` para evitar falsos negativos durante o boot normal. A dependência do backend em relação ao Keycloak foi alterada de `service_started` para `service_healthy`, então o Compose agora espera o Keycloak estar de fato pronto antes de iniciar o backend, em vez de depender de esperar manualmente um tempo arbitrário depois de cada `docker compose up`.

## Backups do MySQL

Backups diários via um job de cron, rodando fora do diretório do repositório `kairos` (`~/backup-mysql.sh` e `~/kairos-backups/`, não versionados no git, para nunca correr o risco de commitar um dump de banco de dados).

```bash
#!/bin/bash
set -euo pipefail

BACKUP_DIR=~/kairos-backups
TIMESTAMP=$(date +%Y-%m-%d_%H-%M-%S)
FILENAME="kairos_backup_$TIMESTAMP.sql.gz"
DB_PASSWORD=$(grep DB_ROOT_PASSWORD ~/kairos/.env | cut -d '=' -f2)

docker exec -e MYSQL_PWD="$DB_PASSWORD" kairos-mysql mysqldump \
  -u root --all-databases | gzip > "$BACKUP_DIR/$FILENAME"

find "$BACKUP_DIR" -name "kairos_backup_*.sql.gz" -mtime +7 -delete

echo "Backup completed: $FILENAME"
```

A senha é passada através da variável de ambiente `MYSQL_PWD` para o `docker exec`, não como argumento `-p` na linha de comando, evitando o aviso de senha insegura do MySQL (argumentos de linha de comando ficam visíveis para outros processos via `ps aux`; variáveis de ambiente passadas dessa forma não ficam expostas da mesma maneira). O `set -euo pipefail` garante que o script falhe de forma visível se o `docker exec`/`mysqldump` falhar, em vez de silenciosamente gerar um arquivo de backup vazio ou corrompido e reportar sucesso mesmo assim.

Agendado via crontab, diariamente às 03:00 UTC, com a saída registrada em log para auditoria:

```
0 3 * * * /home/ubuntu/backup-mysql.sh >> /home/ubuntu/kairos-backups/backup.log 2>&
```


A retenção é de 7 dias, com limpeza automática feita pelo próprio script.

Ainda não feito: uma restauração de verdade nunca foi testada. Um backup que nunca foi restaurado é uma suposição, não uma garantia, e isso deveria ser validado antes de confiar nele num incidente real. Também não existe cópia fora da instância (S3 ou similar); isso protege contra corrupção ou erro humano, não contra a perda da instância inteira.

## Security group

As portas 8080 (Keycloak) e 8081 (backend), expostas publicamente antes como solução temporária até o Nginx ser configurado, foram removidas das regras de entrada do Security Group da EC2, agora que todo o tráfego foi confirmado como funcionando exclusivamente através do Nginx (portas 80/443). Só SSH (22, restrito ao IP do desenvolvedor) e HTTP/HTTPS (80/443, público) continuam abertos.