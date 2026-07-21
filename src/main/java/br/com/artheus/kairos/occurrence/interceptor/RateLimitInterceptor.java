package br.com.artheus.kairos.occurrence.interceptor;

import br.com.artheus.kairos.shared.exception.ApiErrorResponse;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private final ProxyManager<String> bucket4jProxyManager;
    private final JsonMapper jsonMapper;

    private static final Bandwidth LIMIT = Bandwidth.builder()
            .capacity(1)
            .refillIntervally(1, Duration.ofSeconds(30))
            .build();

    private static final BucketConfiguration CONFIG = BucketConfiguration.builder()
            .addLimit(LIMIT)
            .build();

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String userId = authentication.getName();

        String key = userId + ":occurrence";

        Bucket bucket = bucket4jProxyManager
                .builder()
                .build(key, () -> CONFIG);

        ConsumptionProbe probe = bucket.tryConsumeAndReturnRemaining(1);

        if (probe.isConsumed()) {
            response.setHeader(
                    "X-Rate-Limit-Remaining",
                    String.valueOf(probe.getRemainingTokens())
            );
            return true;
        }

        long waitTimeNanos = probe.getNanosToWaitForRefill();
        long waitTimeSeconds = Math.max(
                1,
                TimeUnit.NANOSECONDS.toSeconds(waitTimeNanos)
        );

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType("application/json");
        response.setHeader("Retry-After", String.valueOf(waitTimeSeconds));

        ApiErrorResponse error = new ApiErrorResponse(
                HttpStatus.TOO_MANY_REQUESTS.value(),
                "Too many requests. Please try again later.",
                "RATE_LIMIT_EXCEEDED",
                System.currentTimeMillis()
        );

        response.getWriter().write(
                jsonMapper.writeValueAsString(error)
        );

        return false;
    }
}