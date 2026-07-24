package br.com.artheus.kairos.payment;

import com.stripe.model.Event;
import com.stripe.net.Webhook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@DisplayName("StripeWebhookController Unit Tests")
class StripeWebhookControllerTest {

    private static final String WEBHOOK_SECRET = "whsec_test_secret";
    private static final String SIGNATURE_HEADER = "t=12345,v1=signature";

    @Mock
    private StripeEventParser stripeEventParser;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        StripeWebhookController controller = new StripeWebhookController(WEBHOOK_SECRET, stripeEventParser);
        this.mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("Should return 400 Bad Request when Stripe signature header is missing")
    void shouldReturn400WhenSignatureHeaderIsMissing() throws Exception {
        String payload = "{\"id\": \"evt_123\"}";

        mockMvc.perform(post("/api/v1/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(stripeEventParser);
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

        verifyNoInteractions(stripeEventParser);
    }

    @Test
    @DisplayName("Should route checkout.session.completed event to parser")
    void shouldRouteCheckoutCompletedEventToParser() throws Exception {
        String payload = "{\"id\": \"evt_checkout_123\", \"type\": \"checkout.session.completed\"}";

        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            Event mockEvent = mock(Event.class);
            when(mockEvent.getType()).thenReturn("checkout.session.completed");

            webhookMock.when(() -> Webhook.constructEvent(payload, SIGNATURE_HEADER, WEBHOOK_SECRET))
                    .thenReturn(mockEvent);

            mockMvc.perform(post("/api/v1/webhooks/stripe")
                            .header("Stripe-Signature", SIGNATURE_HEADER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk());

            verify(stripeEventParser, times(1)).processCheckoutCompleted(mockEvent);
        }
    }

    @Test
    @DisplayName("Should route invoice.paid event to parser")
    void shouldRouteInvoicePaidEventToParser() throws Exception {
        String payload = "{\"id\": \"evt_invoice_123\", \"type\": \"invoice.paid\"}";

        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            Event mockEvent = mock(Event.class);
            when(mockEvent.getType()).thenReturn("invoice.paid");

            webhookMock.when(() -> Webhook.constructEvent(payload, SIGNATURE_HEADER, WEBHOOK_SECRET))
                    .thenReturn(mockEvent);

            mockMvc.perform(post("/api/v1/webhooks/stripe")
                            .header("Stripe-Signature", SIGNATURE_HEADER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk());

            verify(stripeEventParser, times(1)).processInvoicePaid(mockEvent);
        }
    }

    @Test
    @DisplayName("Should route invoice.payment_failed event to parser")
    void shouldRoutePaymentFailedEventToParser() throws Exception {
        String payload = "{\"id\": \"evt_failed_123\", \"type\": \"invoice.payment_failed\"}";

        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            Event mockEvent = mock(Event.class);
            when(mockEvent.getType()).thenReturn("invoice.payment_failed");

            webhookMock.when(() -> Webhook.constructEvent(payload, SIGNATURE_HEADER, WEBHOOK_SECRET))
                    .thenReturn(mockEvent);

            mockMvc.perform(post("/api/v1/webhooks/stripe")
                            .header("Stripe-Signature", SIGNATURE_HEADER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk());

            verify(stripeEventParser, times(1)).processPaymentFailed(mockEvent);
        }
    }

    @Test
    @DisplayName("Should route customer.subscription.deleted event to parser")
    void shouldRouteSubscriptionDeletedEventToParser() throws Exception {
        String payload = "{\"id\": \"evt_deleted_123\", \"type\": \"customer.subscription.deleted\"}";

        try (MockedStatic<Webhook> webhookMock = mockStatic(Webhook.class)) {
            Event mockEvent = mock(Event.class);
            when(mockEvent.getType()).thenReturn("customer.subscription.deleted");

            webhookMock.when(() -> Webhook.constructEvent(payload, SIGNATURE_HEADER, WEBHOOK_SECRET))
                    .thenReturn(mockEvent);

            mockMvc.perform(post("/api/v1/webhooks/stripe")
                            .header("Stripe-Signature", SIGNATURE_HEADER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(payload))
                    .andExpect(status().isOk());

            verify(stripeEventParser, times(1)).processSubscriptionDeleted(mockEvent);
        }
    }

    @Test
    @DisplayName("Should return 200 OK and ignore unhandled event types")
    void shouldIgnoreUnhandledEventTypes() throws Exception {
        String payload = "{\"id\": \"evt_unhandled\", \"type\": \"payment_intent.succeeded\"}";

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

            verifyNoInteractions(stripeEventParser);
        }
    }
}