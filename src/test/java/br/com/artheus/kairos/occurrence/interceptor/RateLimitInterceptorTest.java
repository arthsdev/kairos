package br.com.artheus.kairos.occurrence.interceptor;

import br.com.artheus.kairos.shared.exception.ApiErrorResponse;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitInterceptorTest {

    // DEEP_STUBS automatically resolves the entire Bucket4j fluent chain!
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ProxyManager<String> bucket4jProxyManager;


    @Mock
    private JsonMapper jsonMapper;

    @Mock
    private io.github.bucket4j.distributed.BucketProxy bucket;

    @Mock
    private ConsumptionProbe consumptionProbe;

    @Mock
    private Authentication authentication;

    @Mock
    private SecurityContext securityContext;

    @InjectMocks
    private RateLimitInterceptor rateLimitInterceptor;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private final Object dummyHandler = new Object();

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.setContext(securityContext);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("Tests for non-POST methods")
    class NonPostMethods {

        @Test
        @DisplayName("Should bypass rate limiting when the HTTP method is GET")
        void shouldBypassWhenGetMethod() throws Exception {
            request.setMethod("GET");
            boolean result = rateLimitInterceptor.preHandle(request, response, dummyHandler);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("Should bypass rate limiting when the HTTP method is PUT")
        void shouldBypassWhenPutMethod() throws Exception {
            request.setMethod("PUT");
            boolean result = rateLimitInterceptor.preHandle(request, response, dummyHandler);
            assertThat(result).isTrue();
        }
    }

    @Nested
    @DisplayName("Tests for POST method rate limiting")
    class PostMethodRateLimiting {

        private void setupBucketMocking(String userId) {
            // 1. Configures the standard security context
            when(securityContext.getAuthentication()).thenReturn(authentication);
            when(authentication.getName()).thenReturn(userId);

            // 2. We create the mock for the intermediate builder
            var builderMock = mock(io.github.bucket4j.distributed.proxy.RemoteBucketBuilder.class, Answers.RETURNS_DEEP_STUBS);
            when(bucket4jProxyManager.builder()).thenReturn(builderMock);

            // 3. Now Mockito intercepts the build call and returns the correct BucketProxy mock type!
            doReturn(bucket)
                    .when(builderMock)
                    .build(any(), any(java.util.function.Supplier.class));

            // 4. Configures the consumption probe behavior on the final proxy
            when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(consumptionProbe);
        }

        @Test
        @DisplayName("Should allow the request when tokens are available")
        void shouldAllowRequestWhenTokensAvailable() throws Exception {
            request.setMethod("POST");
            String userId = "user-789";

            setupBucketMocking(userId);
            when(consumptionProbe.isConsumed()).thenReturn(true);
            when(consumptionProbe.getRemainingTokens()).thenReturn(5L);

            boolean result = rateLimitInterceptor.preHandle(request, response, dummyHandler);

            assertThat(result).isTrue();
            assertThat(response.getHeader("X-Rate-Limit-Remaining")).isEqualTo("5");
        }

        @Test
        @DisplayName("Should block the request and return 429 when rate limit is exceeded")
        void shouldBlockRequestWhenRateLimitExceeded() throws Exception {
            request.setMethod("POST");
            String userId = "user-789";

            setupBucketMocking(userId);
            when(consumptionProbe.isConsumed()).thenReturn(false);
            when(consumptionProbe.getNanosToWaitForRefill()).thenReturn(30_000_000_000L); // 30s
            when(jsonMapper.writeValueAsString(any(ApiErrorResponse.class))).thenReturn("{\"message\":\"Too many requests\"}");

            boolean result = rateLimitInterceptor.preHandle(request, response, dummyHandler);

            assertThat(result).isFalse();
            assertThat(response.getStatus()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS.value());
            assertThat(response.getHeader("Retry-After")).isEqualTo("30");
        }

        @Test
        @DisplayName("Should fall back to a Retry-After header of 1 second minimum if refill wait time is very low")
        void shouldFallbackToMinimumRetryAfter() throws Exception {
            request.setMethod("POST");
            String userId = "user-789";

            setupBucketMocking(userId);
            when(consumptionProbe.isConsumed()).thenReturn(false);
            when(consumptionProbe.getNanosToWaitForRefill()).thenReturn(100_000_000L); // 0.1s
            when(jsonMapper.writeValueAsString(any(ApiErrorResponse.class))).thenReturn("error");

            boolean result = rateLimitInterceptor.preHandle(request, response, dummyHandler);

            assertThat(result).isFalse();
            assertThat(response.getHeader("Retry-After")).isEqualTo("1");
        }
    }
}