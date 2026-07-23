package br.com.artheus.kairos.payment;

import br.com.artheus.kairos.shared.contract.payment.PaymentWebhookProcessor;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("StripeWebhookController Unit Tests")
class StripeWebhookControllerTest {

    private static final String WEBHOOK_SECRET = "whsec_test_secret";
    private static final String SIGNATURE_HEADER = "t=12345,v1=signature";

    @Mock
    private PaymentWebhookProcessor paymentWebhookProcessor;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        StripeWebhookController controller = new StripeWebhookController(WEBHOOK_SECRET, paymentWebhookProcessor);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Nested
    @DisplayName("Signature and Header Validation Tests")
    class SignatureAndHeaderValidationTests {

        @Test
        @DisplayName("Should return 400 Bad Request when Stripe signature header is missing")
        void shouldReturn400WhenSignatureHeaderIsMissing() throws Exception {
            String payload = "{\"id\": \"evt_123\"}";

            mockMvc.perform(post("/api/v1/webhooks/stripe")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(paymentWebhookProcessor);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when Stripe signature verification fails")
        void shouldReturn400WhenSignatureIsInvalid() throws Exception {
            String payload = "{\"id\": \"evt_123\"}";

            mockMvc.perform(post("/api/v1/webhooks/stripe")
                            .header("Stripe-Signature", "invalid_signature")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(paymentWebhookProcessor);
        }

        @Test
        @DisplayName("Should return 200 OK and ignore unhandled event types")
        void shouldIgnoreUnhandledEventTypes() throws Exception {
            String payload = """
                    {
                      "id": "evt_unhandled",
                      "type": "payment_intent.succeeded",
                      "data": {}
                    }
                    """;

            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                Event mockEvent = mock(Event.class);
                when(mockEvent.getType()).thenReturn("payment_intent.succeeded");

                webhookMock.when(() -> Webhook.constructEvent(payload, SIGNATURE_HEADER, WEBHOOK_SECRET))
                        .thenReturn(mockEvent);

                mockMvc.perform(post("/api/v1/webhooks/stripe")
                                .header("Stripe-Signature", SIGNATURE_HEADER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                        .andExpect(status().isOk());

                verifyNoInteractions(paymentWebhookProcessor);
            }
        }
    }

    @Nested
    @DisplayName("checkout.session.completed Processing Tests")
    class CheckoutCompletedTests {

        @Test
        @DisplayName("Should process checkout completed event successfully when payload is valid")
        void shouldProcessCheckoutCompletedSuccessfully() throws Exception {
            String payload = """
                    {
                      "id": "evt_checkout_123",
                      "type": "checkout.session.completed",
                      "data": {
                        "object": {
                          "object": "checkout.session",
                          "client_reference_id": "user_456",
                          "customer": "cus_789",
                          "subscription": "sub_012"
                        }
                      }
                    }
                    """;

            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                Event mockEvent = mock(Event.class);
                when(mockEvent.getType()).thenReturn("checkout.session.completed");

                Session session = new Session();
                session.setClientReferenceId("user_456");
                session.setCustomer("cus_789");
                session.setSubscription("sub_012");

                EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
                when(deserializer.getObject()).thenReturn(Optional.of(session));
                when(mockEvent.getDataObjectDeserializer()).thenReturn(deserializer);

                webhookMock.when(() -> Webhook.constructEvent(payload, SIGNATURE_HEADER, WEBHOOK_SECRET))
                        .thenReturn(mockEvent);

                mockMvc.perform(post("/api/v1/webhooks/stripe")
                                .header("Stripe-Signature", SIGNATURE_HEADER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                        .andExpect(status().isOk());

                verify(paymentWebhookProcessor, times(1))
                        .handleCheckoutCompleted("user_456", "cus_789", "sub_012");
            }
        }

        @Test
        @DisplayName("Should return 200 OK without calling processor when userId (client_reference_id) is missing")
        void shouldReturn200AndLogWhenUserIdIsMissing() throws Exception {
            String payload = """
                    {
                      "id": "evt_checkout_no_user",
                      "type": "checkout.session.completed",
                      "data": {
                        "object": {
                          "object": "checkout.session",
                          "client_reference_id": null,
                          "customer": "cus_789",
                          "subscription": "sub_012"
                        }
                      }
                    }
                    """;

            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                Event mockEvent = mock(Event.class);
                when(mockEvent.getType()).thenReturn("checkout.session.completed");

                Session session = new Session();
                session.setClientReferenceId(null);
                session.setCustomer("cus_789");
                session.setSubscription("sub_012");

                EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
                when(deserializer.getObject()).thenReturn(Optional.of(session));
                when(mockEvent.getDataObjectDeserializer()).thenReturn(deserializer);

                webhookMock.when(() -> Webhook.constructEvent(payload, SIGNATURE_HEADER, WEBHOOK_SECRET))
                        .thenReturn(mockEvent);

                mockMvc.perform(post("/api/v1/webhooks/stripe")
                                .header("Stripe-Signature", SIGNATURE_HEADER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                        .andExpect(status().isOk());

                verify(paymentWebhookProcessor, never()).handleCheckoutCompleted(anyString(), anyString(), anyString());
            }
        }

        @Test
        @DisplayName("Should use unsafe deserialization fallback when safe deserialization fails")
        void shouldFallbackToUnsafeDeserializationWhenSafeDeserializationFails() throws Exception {
            String payload = "{\"id\": \"evt_checkout_fallback\"}";

            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                Event mockEvent = mock(Event.class);
                when(mockEvent.getType()).thenReturn("checkout.session.completed");

                Session session = new Session();
                session.setClientReferenceId("user_fallback");
                session.setCustomer("cus_fallback");
                session.setSubscription("sub_fallback");

                EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
                when(deserializer.getObject()).thenReturn(Optional.empty());
                when(deserializer.deserializeUnsafe()).thenReturn(session);
                when(mockEvent.getDataObjectDeserializer()).thenReturn(deserializer);

                webhookMock.when(() -> Webhook.constructEvent(payload, SIGNATURE_HEADER, WEBHOOK_SECRET))
                        .thenReturn(mockEvent);

                mockMvc.perform(post("/api/v1/webhooks/stripe")
                                .header("Stripe-Signature", SIGNATURE_HEADER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                        .andExpect(status().isOk());

                verify(paymentWebhookProcessor, times(1))
                        .handleCheckoutCompleted("user_fallback", "cus_fallback", "sub_fallback");
            }
        }

        @Test
        @DisplayName("Should throw IllegalStateException when both safe and unsafe deserialization fail")
        void shouldThrowExceptionWhenBothDeserializationAttemptsFail() throws Exception {
            String payload = "{\"id\": \"evt_checkout_corrupted\"}";

            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                Event mockEvent = mock(Event.class);
                when(mockEvent.getType()).thenReturn("checkout.session.completed");

                EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
                when(deserializer.getObject()).thenReturn(Optional.empty());
                when(deserializer.deserializeUnsafe())
                        .thenThrow(new EventDataObjectDeserializationException("Deserialization failed", payload));
                when(mockEvent.getDataObjectDeserializer()).thenReturn(deserializer);

                webhookMock.when(() -> Webhook.constructEvent(payload, SIGNATURE_HEADER, WEBHOOK_SECRET))
                        .thenReturn(mockEvent);

                assertThatThrownBy(() ->
                        mockMvc.perform(post("/api/v1/webhooks/stripe")
                                .header("Stripe-Signature", SIGNATURE_HEADER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                ).hasCauseInstanceOf(IllegalStateException.class);

                verify(paymentWebhookProcessor, never()).handleCheckoutCompleted(anyString(), anyString(), anyString());
            }
        }

        @Test
        @DisplayName("Should propagate infrastructure exception to trigger Stripe webhook retry mechanism")
        void shouldRethrowExceptionWhenProcessorFails() {
            String payload = """
                    {
                      "id": "evt_checkout_fail",
                      "type": "checkout.session.completed",
                      "data": {
                        "object": {
                          "object": "checkout.session",
                          "client_reference_id": "user_456",
                          "customer": "cus_789",
                          "subscription": "sub_012"
                        }
                      }
                    }
                    """;

            try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
                Event mockEvent = mock(Event.class);
                when(mockEvent.getType()).thenReturn("checkout.session.completed");

                Session session = new Session();
                session.setClientReferenceId("user_456");
                session.setCustomer("cus_789");
                session.setSubscription("sub_012");

                EventDataObjectDeserializer deserializer = mock(EventDataObjectDeserializer.class);
                when(deserializer.getObject()).thenReturn(Optional.of(session));
                when(mockEvent.getDataObjectDeserializer()).thenReturn(deserializer);

                webhookMock.when(() -> Webhook.constructEvent(payload, SIGNATURE_HEADER, WEBHOOK_SECRET))
                        .thenReturn(mockEvent);

                doThrow(new RuntimeException("Database connection timeout"))
                        .when(paymentWebhookProcessor)
                        .handleCheckoutCompleted(anyString(), anyString(), anyString());

                assertThatThrownBy(() ->
                        mockMvc.perform(post("/api/v1/webhooks/stripe")
                                .header("Stripe-Signature", SIGNATURE_HEADER)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(payload))
                ).hasCauseInstanceOf(RuntimeException.class);
            }
        }
    }
}