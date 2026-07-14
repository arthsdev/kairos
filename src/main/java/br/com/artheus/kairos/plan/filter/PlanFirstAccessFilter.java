package br.com.artheus.kairos.plan.filter;

import br.com.artheus.kairos.plan.PlanService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@RequiredArgsConstructor
public class PlanFirstAccessFilter extends OncePerRequestFilter {

    private final PlanService planService;
    private final StringRedisTemplate redisTemplate;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        try {
            if (authentication instanceof JwtAuthenticationToken jwtAuth) {
                String userId = jwtAuth.getToken().getSubject();

                if (userId != null) {
                    String cacheKey = "user:plan:" + userId;

                    String cachedPlan = redisTemplate.opsForValue().get(cacheKey);

                    if (cachedPlan == null) {
                        planService.ensurePlanExists(userId);

                        redisTemplate.opsForValue().set(cacheKey, "exists");
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error verifying or registering first-access plan in Redis/Database", e);
        } finally {
            filterChain.doFilter(request, response);
        }
    }
}