package br.com.artheus.kairos.shared.config;

import br.com.artheus.kairos.plan.PlanService;
import br.com.artheus.kairos.plan.filter.PlanFirstAccessFilter;
import br.com.artheus.kairos.shared.security.KairosAccessDeniedHandler;
import br.com.artheus.kairos.shared.security.KeycloakRoleConverter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final PlanService planService;
    private final StringRedisTemplate redisTemplate;
    private final KairosAccessDeniedHandler accessDeniedHandler;

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRoleConverter());
        return converter;
    }

    @Bean
    public PlanFirstAccessFilter planFirstAccessFilter() {
        return new PlanFirstAccessFilter(planService, redisTemplate);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .addFilterAfter(planFirstAccessFilter(), BearerTokenAuthenticationFilter.class)
                .exceptionHandling(ex -> ex
                        .accessDeniedHandler(accessDeniedHandler)
                )
                .authorizeHttpRequests(authz -> authz
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs",
                                "/api/v1/webhooks/stripe",
                                "/api/v1/auth/login"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/occurrences").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/occurrences/*/verify",
                                "/api/v1/occurrences/*/resolve"
                        ).hasRole("ADMIN")
                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );

        return http.build();
    }
}