package br.com.artheus.kairos.shared.filter;

import br.com.artheus.kairos.anonymization.UserReferenceService;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.io.IOException;
import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FirstAccessProvisioningFilter Unit Tests")
class FirstAccessProvisioningFilterTest {

    @Mock
    private PlanService planService;

    @Mock
    private UserReferenceService userReferenceService;

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
    private FirstAccessProvisioningFilter filter;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Tests for doFilterInternal")
    class DoFilterInternalTests {

        private final String userId = "user-123";

        @Test
        @DisplayName("Should execute provisioning when JWT token is present and cache is empty")
        void shouldExecuteProvisioningWhenCacheIsEmpty() throws ServletException, IOException {
            // Given
            Jwt jwt = Jwt.withTokenValue("token")
                    .header("alg", "none")
                    .subject(userId)
                    .build();
            JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
            SecurityContextHolder.getContext().setAuthentication(auth);

            org.springframework.test.util.ReflectionTestUtils.setField(filter, "cacheTtl", Duration.ofHours(1));

            when(valueOperations.get("user:plan:" + userId)).thenReturn(null);
            when(valueOperations.get("user:reference:" + userId)).thenReturn(null);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(planService).ensurePlanExists(userId);
            verify(userReferenceService).ensureUserReferenceExists(userId);
            verify(valueOperations).set(eq("user:plan:" + userId), eq("exists"), any(Duration.class));
            verify(valueOperations).set(eq("user:reference:" + userId), eq("exists"), any(Duration.class));
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should skip provisioning when cache already exists in Redis")
        void shouldSkipProvisioningWhenCacheExists() throws ServletException, IOException {
            // Given
            Jwt jwt = Jwt.withTokenValue("token")
                    .header("alg", "none")
                    .subject(userId)
                    .build();
            JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
            SecurityContextHolder.getContext().setAuthentication(auth);

            when(valueOperations.get("user:plan:" + userId)).thenReturn("exists");
            when(valueOperations.get("user:reference:" + userId)).thenReturn("exists");

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(planService, never()).ensurePlanExists(any());
            verify(userReferenceService, never()).ensureUserReferenceExists(any());
            verify(valueOperations, never()).set(any(), any(), any(Duration.class));
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should skip provisioning when authentication is not JwtAuthenticationToken")
        void shouldSkipProvisioningWhenNotJwt() throws ServletException, IOException {
            // Given
            Authentication nonJwtAuth = mock(Authentication.class);
            SecurityContextHolder.getContext().setAuthentication(nonJwtAuth);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(planService, never()).ensurePlanExists(any());
            verify(userReferenceService, never()).ensureUserReferenceExists(any());
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should catch exception and continue filter chain gracefully when error occurs")
        void shouldCatchExceptionAndContinueFilterChain() throws ServletException, IOException {
            // Given
            Jwt jwt = Jwt.withTokenValue("token")
                    .header("alg", "none")
                    .subject(userId)
                    .build();
            JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
            SecurityContextHolder.getContext().setAuthentication(auth);

            when(valueOperations.get("user:plan:" + userId)).thenReturn(null);
            doThrow(new RuntimeException("Redis down")).when(planService).ensurePlanExists(userId);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(planService).ensurePlanExists(userId);
            verify(filterChain).doFilter(request, response);
        }

        @Test
        @DisplayName("Should provision only missing service when mixed cache state occurs")
        void shouldProvisionOnlyMissingServiceWhenMixedCacheState() throws ServletException, IOException {
            // Given
            Jwt jwt = Jwt.withTokenValue("token")
                    .header("alg", "none")
                    .subject(userId)
                    .build();
            JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
            SecurityContextHolder.getContext().setAuthentication(auth);

            org.springframework.test.util.ReflectionTestUtils.setField(filter, "cacheTtl", Duration.ofHours(1));

            when(valueOperations.get("user:plan:" + userId)).thenReturn("exists");
            when(valueOperations.get("user:reference:" + userId)).thenReturn(null);

            // When
            filter.doFilterInternal(request, response, filterChain);

            // Then
            verify(planService, never()).ensurePlanExists(any());
            verify(userReferenceService).ensureUserReferenceExists(userId);

            verify(valueOperations, never()).set(eq("user:plan:" + userId), any(), any());
            verify(valueOperations).set(eq("user:reference:" + userId), eq("exists"), any(Duration.class));

            verify(filterChain).doFilter(request, response);
        }
    }
}