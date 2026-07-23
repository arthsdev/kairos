package br.com.artheus.kairos.payment;

import br.com.artheus.kairos.shared.exception.ExternalServiceException;
import com.stripe.exception.ApiConnectionException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StripeCheckoutService Unit Tests")
class StripeCheckoutServiceTest {

    private static final String PREMIUM_PRICE_ID = "price_premium_mock_123";
    private static final String SUCCESS_URL = "http://localhost:8080/success";
    private static final String CANCEL_URL = "http://localhost:8080/cancel";
    private static final String DEFAULT_USER_ID = "user-123";

    private StripeCheckoutService stripeCheckoutService;

    @BeforeEach
    void setUp() {
        stripeCheckoutService = new StripeCheckoutService(
                PREMIUM_PRICE_ID,
                SUCCESS_URL,
                CANCEL_URL
        );
    }

    @Nested
    @DisplayName("Tests for createCheckoutSession")
    class CreateCheckoutSessionTests {

        @Test
        @DisplayName("Should create Stripe checkout session and return checkout URL successfully")
        void shouldCreateCheckoutSessionSuccessfully() {
            String expectedUrl = "https://checkout.stripe.com/pay/cs_test_abc123";
            Session mockSession = mock(Session.class);
            when(mockSession.getUrl()).thenReturn(expectedUrl);

            try (MockedStatic<Session> mockedStripeSession = mockStatic(Session.class)) {
                mockedStripeSession.when(() -> Session.create(any(SessionCreateParams.class)))
                        .thenReturn(mockSession);

                String resultUrl = stripeCheckoutService.createCheckoutSession(DEFAULT_USER_ID);

                assertThat(resultUrl).isEqualTo(expectedUrl);

                ArgumentCaptor<SessionCreateParams> paramsCaptor = ArgumentCaptor.forClass(SessionCreateParams.class);
                mockedStripeSession.verify(() -> Session.create(paramsCaptor.capture()));

                SessionCreateParams capturedParams = paramsCaptor.getValue();
                assertThat(capturedParams.getClientReferenceId()).isEqualTo(DEFAULT_USER_ID);
                assertThat(capturedParams.getMode()).isEqualTo(SessionCreateParams.Mode.SUBSCRIPTION);
                assertThat(capturedParams.getSuccessUrl()).isEqualTo(SUCCESS_URL);
                assertThat(capturedParams.getCancelUrl()).isEqualTo(CANCEL_URL);
                assertThat(capturedParams.getLineItems()).hasSize(1);
                assertThat(capturedParams.getLineItems().get(0).getPrice()).isEqualTo(PREMIUM_PRICE_ID);
                assertThat(capturedParams.getLineItems().get(0).getQuantity()).isEqualTo(1L);
            }
        }

        @Test
        @DisplayName("Should wrap StripeException into ExternalServiceException when API communication fails")
        void shouldThrowExternalServiceExceptionWhenStripeFails() {
            ApiConnectionException stripeException = new ApiConnectionException("Failed to connect to Stripe API");

            try (MockedStatic<Session> mockedStripeSession = mockStatic(Session.class)) {
                mockedStripeSession.when(() -> Session.create(any(SessionCreateParams.class)))
                        .thenThrow(stripeException);

                assertThatThrownBy(() -> stripeCheckoutService.createCheckoutSession(DEFAULT_USER_ID))
                        .isInstanceOf(ExternalServiceException.class)
                        .hasMessageContaining("Failed to communicate with payment provider (Stripe).")
                        .hasCause(stripeException);
            }
        }
    }
}