package br.com.artheus.kairos.plan.filter;

import br.com.artheus.kairos.plan.PlanService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.time.Duration;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlanFirstAccessFilter Unit Tests")
class PlanFirstAccessFilterTest {

    @Mock
    private PlanService planService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private PlanFirstAccessFilter filter;

    private final Duration stubbedTtl = Duration.ofHours(1);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(filter, "cacheTtl", stubbedTtl);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Tests for doFilterInternal")
    class DoFilterInternalTests {

        private final String userId = "user-abc-123";
        private final String cacheKey = "user:plan:" + userId;

        @Test
        @DisplayName("Scenario 1: Cache Miss - Should invoke planService and save to Redis with TTL")
        void shouldTriggerServiceAndCacheOnCacheMiss() throws ServletException, IOException {
            // Given
            mockSecurityContextWithUser(userId);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(cacheKey)).thenReturn(null);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(planService, times(1)).ensurePlanExists(userId);
            verify(valueOperations, times(1)).set(cacheKey, "exists", stubbedTtl);
            verify(filterChain, times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("Scenario 2: Cache Hit - Should bypass service and Redis writing")
        void shouldBypassServiceOnCacheHit() throws ServletException, IOException {
            // Given
            mockSecurityContextWithUser(userId);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(cacheKey)).thenReturn("exists");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(planService, never()).ensurePlanExists(anyString());
            verify(valueOperations, never()).set(anyString(), anyString(), any(Duration.class));
            verify(filterChain, times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("Scenario 3a: Not a JWT Token - Should skip entire processing but proceed filter chain")
        void shouldSkipProcessingWhenAuthenticationIsNotJwt() throws ServletException, IOException {
            // Given
            Authentication genericAuth = mock(Authentication.class);
            SecurityContext securityContext = mock(SecurityContext.class);
            when(securityContext.getAuthentication()).thenReturn(genericAuth);
            SecurityContextHolder.setContext(securityContext);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verifyNoInteractions(redisTemplate, planService);
            verify(filterChain, times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("Scenario 3b: JWT present but userId is null - Should skip entire processing but proceed filter chain")
        void shouldSkipProcessingWhenUserIdIsNull() throws ServletException, IOException {
            // Given
            mockSecurityContextWithUser(null);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verifyNoInteractions(redisTemplate, planService);
            verify(filterChain, times(1)).doFilter(request, response);
        }

        @Test
        @DisplayName("Scenario 4: Processing throws Exception - Should catch silently and proceed filter chain")
        void shouldSwallowExceptionAndContinueFilterChain() throws ServletException, IOException {
            // Given
            mockSecurityContextWithUser(userId);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            // Simula um estouro de infraestrutura do Redis lançando uma exceção genérica
            when(valueOperations.get(cacheKey)).thenThrow(new RuntimeException("Redis connection refused"));

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(filterChain, times(1)).doFilter(request, response);
        }
    }

    /*HELPER METHOD*/
    private void mockSecurityContextWithUser(String subjectId) {
        SecurityContext securityContext = mock(SecurityContext.class);
        JwtAuthenticationToken jwtAuth = mock(JwtAuthenticationToken.class);
        Jwt jwt = mock(Jwt.class);

        when(jwt.getSubject()).thenReturn(subjectId);
        when(jwtAuth.getToken()).thenReturn(jwt);
        when(securityContext.getAuthentication()).thenReturn(jwtAuth);

        SecurityContextHolder.setContext(securityContext);
    }
}