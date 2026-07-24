package br.com.artheus.kairos.payment;

import br.com.artheus.kairos.shared.contract.payment.PaymentWebhookProcessor;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.Invoice.Parent;
import com.stripe.model.Invoice.Parent.SubscriptionDetails;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StripeEventParser Unit Tests")
class StripeEventParserTest {

    @Mock
    private PaymentWebhookProcessor paymentWebhookProcessor;

    @Mock
    private Event event;

    @Mock
    private EventDataObjectDeserializer deserializer;

    @InjectMocks
    private StripeEventParser stripeEventParser;

    @Nested
    @DisplayName("processCheckoutCompleted Tests")
    class ProcessCheckoutCompletedTests {

        @Test
        @DisplayName("Should process checkout completed event successfully when payload is valid")
        void shouldProcessCheckoutCompletedSuccessfully() {
            Session session = new Session();
            session.setClientReferenceId("user_456");
            session.setCustomer("cus_789");
            session.setSubscription("sub_012");

            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(session));

            stripeEventParser.processCheckoutCompleted(event);

            verify(paymentWebhookProcessor, times(1))
                    .handleCheckoutCompleted("user_456", "cus_789", "sub_012");
        }

        @Test
        @DisplayName("Should do nothing when userId (client_reference_id) is missing")
        void shouldDoNothingWhenUserIdIsMissing() {
            Session session = new Session();
            session.setClientReferenceId(null);
            session.setCustomer("cus_789");
            session.setSubscription("sub_012");

            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(session));

            stripeEventParser.processCheckoutCompleted(event);

            verify(paymentWebhookProcessor, never()).handleCheckoutCompleted(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should fallback to unsafe deserialization when safe deserialization fails")
        void shouldFallbackToUnsafeDeserializationWhenSafeFails() throws Exception {
            Session session = new Session();
            session.setClientReferenceId("user_fallback");
            session.setCustomer("cus_fallback");
            session.setSubscription("sub_fallback");

            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.empty());
            when(deserializer.deserializeUnsafe()).thenReturn(session);

            stripeEventParser.processCheckoutCompleted(event);

            verify(paymentWebhookProcessor, times(1))
                    .handleCheckoutCompleted("user_fallback", "cus_fallback", "sub_fallback");
        }

        @Test
        @DisplayName("Should throw IllegalStateException when both safe and unsafe deserialization fail")
        void shouldThrowExceptionWhenDeserializationFails() throws Exception {
            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.empty());
            when(deserializer.deserializeUnsafe())
                    .thenThrow(new EventDataObjectDeserializationException("Deserialization failed", "raw_json"));

            assertThatThrownBy(() -> stripeEventParser.processCheckoutCompleted(event))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Failed to deserialize Stripe event object");

            verifyNoInteractions(paymentWebhookProcessor);
        }
    }

    @Nested
    @DisplayName("Invoice Events Tests (processInvoicePaid & processPaymentFailed)")
    class InvoiceEventsTests {

        @Test
        @DisplayName("Should process invoice.paid successfully with parent subscription details")
        void shouldProcessInvoicePaidSuccessfully() {
            Invoice invoice = mock(Invoice.class);
            Parent parent = mock(Parent.class);
            SubscriptionDetails subDetails = mock(SubscriptionDetails.class);

            when(invoice.getCustomer()).thenReturn("cus_123");
            when(invoice.getParent()).thenReturn(parent);
            when(parent.getSubscriptionDetails()).thenReturn(subDetails);
            when(subDetails.getSubscription()).thenReturn("sub_999");

            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(invoice));

            stripeEventParser.processInvoicePaid(event);

            verify(paymentWebhookProcessor, times(1))
                    .handleInvoicePaid("cus_123", "sub_999");
        }

        @Test
        @DisplayName("Should process invoice.paid when parent subscription details are null")
        void shouldProcessInvoicePaidWhenSubscriptionDetailsAreNull() {
            Invoice invoice = mock(Invoice.class);
            when(invoice.getCustomer()).thenReturn("cus_123");
            when(invoice.getParent()).thenReturn(null);

            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(invoice));

            stripeEventParser.processInvoicePaid(event);

            verify(paymentWebhookProcessor, times(1))
                    .handleInvoicePaid("cus_123", null);
        }

        @Test
        @DisplayName("Should process invoice.payment_failed successfully")
        void shouldProcessPaymentFailedSuccessfully() {
            Invoice invoice = mock(Invoice.class);
            Parent parent = mock(Parent.class);
            SubscriptionDetails subDetails = mock(SubscriptionDetails.class);

            when(invoice.getCustomer()).thenReturn("cus_123");
            when(invoice.getParent()).thenReturn(parent);
            when(parent.getSubscriptionDetails()).thenReturn(subDetails);
            when(subDetails.getSubscription()).thenReturn("sub_999");

            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(invoice));

            stripeEventParser.processPaymentFailed(event);

            verify(paymentWebhookProcessor, times(1))
                    .handlePaymentFailed("cus_123", "sub_999");
        }
    }

    @Nested
    @DisplayName("processSubscriptionDeleted Tests")
    class SubscriptionDeletedTests {

        @Test
        @DisplayName("Should process customer.subscription.deleted successfully")
        void shouldProcessSubscriptionDeletedSuccessfully() {
            Subscription subscription = new Subscription();
            subscription.setId("sub_deleted_123");
            subscription.setCustomer("cus_123");

            when(event.getDataObjectDeserializer()).thenReturn(deserializer);
            when(deserializer.getObject()).thenReturn(Optional.of(subscription));

            stripeEventParser.processSubscriptionDeleted(event);

            verify(paymentWebhookProcessor, times(1))
                    .handleSubscriptionDeleted("cus_123", "sub_deleted_123");
        }
    }
}