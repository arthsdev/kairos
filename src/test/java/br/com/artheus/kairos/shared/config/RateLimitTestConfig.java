package br.com.artheus.kairos.shared.config;

import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.mockito.Answers;
import org.mockito.Mockito;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("test")
class RateLimitTestConfig {

    @Bean
    public ProxyManager<String> bucket4jProxyManager() {
        return Mockito.mock(ProxyManager.class, Answers.RETURNS_DEEP_STUBS);
    }
}