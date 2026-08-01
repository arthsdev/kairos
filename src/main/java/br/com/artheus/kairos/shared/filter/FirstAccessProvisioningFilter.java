package br.com.artheus.kairos.shared.filter;

import br.com.artheus.kairos.anonymization.UserReferenceService;
import br.com.artheus.kairos.plan.PlanService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@RequiredArgsConstructor
public class FirstAccessProvisioningFilter extends OncePerRequestFilter {

    private final PlanService planService;
    private final UserReferenceService userReferenceService;
    private final StringRedisTemplate redisTemplate;

    @Value("${kairos.plan.cache-ttl:PT1H}")
    private Duration cacheTtl;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        try {
            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                String userId = jwtAuth.getToken().getSubject();

                if (userId != null) {
                    ensureOnce("user:plan:" + userId, () -> planService.ensurePlanExists(userId));
                    ensureOnce("user:reference:" + userId, () -> userReferenceService.ensureUserReferenceExists(userId));
                }
            }
        } catch (Exception e) {
            log.error("Error during first-access provisioning", e);
        } finally {
            filterChain.doFilter(request, response);
        }
    }

    private void ensureOnce(String cacheKey, Runnable action) {
        if (redisTemplate.opsForValue().get(cacheKey) == null) {
            action.run();
            redisTemplate.opsForValue().set(cacheKey, "exists", cacheTtl);
        }
    }
}