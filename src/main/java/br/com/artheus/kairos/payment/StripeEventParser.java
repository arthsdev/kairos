package br.com.artheus.kairos.payment;

import br.com.artheus.kairos.shared.contract.payment.PaymentWebhookProcessor;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.Invoice;
import com.stripe.model.StripeObject;
import com.stripe.model.Subscription;
import com.stripe.model.checkout.Session;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class StripeEventParser {

    private final PaymentWebhookProcessor paymentWebhookProcessor;

    public void processCheckoutCompleted(Event event) {
        Session session = deserialize(event, Session.class);
        if (session == null) {
            log.error("Checkout session payload is invalid or null for event ID: {}", event.getId());
            return;
        }

        String userId = session.getClientReferenceId();
        String customerId = session.getCustomer();
        String subscriptionId = session.getSubscription();

        if (userId == null || userId.isBlank()) {
            log.error("Missing clientReferenceId (userId) in checkout session for event ID: {}", event.getId());
            return;
        }

        log.info("Processing checkout.session.completed for userId: {}, customerId: {}, subscriptionId: {}",
                userId, customerId, subscriptionId);

        paymentWebhookProcessor.handleCheckoutCompleted(userId, customerId, subscriptionId);
    }

    public void processInvoicePaid(Event event) {
        Invoice invoice = deserialize(event, Invoice.class);
        if (invoice == null) {
            log.error("Invoice payload is invalid or null for event ID: {}", event.getId());
            return;
        }

        String customerId = invoice.getCustomer();
        String subscriptionId = extractSubscriptionId(invoice);

        log.info("Processing invoice.paid for customerId: {}, subscriptionId: {}", customerId, subscriptionId);
        paymentWebhookProcessor.handleInvoicePaid(customerId, subscriptionId);
    }

    public void processPaymentFailed(Event event) {
        Invoice invoice = deserialize(event, Invoice.class);
        if (invoice == null) {
            log.error("Invoice payload is invalid or null for event ID: {}", event.getId());
            return;
        }

        String customerId = invoice.getCustomer();
        String subscriptionId = extractSubscriptionId(invoice);

        log.info("Processing invoice.payment_failed for customerId: {}, subscriptionId: {}", customerId, subscriptionId);
        paymentWebhookProcessor.handlePaymentFailed(customerId, subscriptionId);
    }

    public void processSubscriptionDeleted(Event event) {
        Subscription subscription = deserialize(event, Subscription.class);
        if (subscription == null) {
            log.error("Subscription payload is invalid or null for event ID: {}", event.getId());
            return;
        }

        String customerId = subscription.getCustomer();
        String subscriptionId = subscription.getId();

        log.info("Processing customer.subscription.deleted for customerId: {}, subscriptionId: {}", customerId, subscriptionId);
        paymentWebhookProcessor.handleSubscriptionDeleted(customerId, subscriptionId);
    }

    /**
     * Attempts to deserialize the inner event object to the target type.
     * Falls back to unsafe deserialization if the Stripe SDK fails during safe parsing.
     */
    private <T extends StripeObject> T deserialize(Event event, Class<T> clazz) {
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();

        if (dataObjectDeserializer.getObject().isPresent()) {
            StripeObject stripeObject = dataObjectDeserializer.getObject().get();
            if (clazz.isInstance(stripeObject)) {
                return clazz.cast(stripeObject);
            }
        }

        log.warn("Safe deserialization failed for event ID: {}. Attempting unsafe fallback...", event.getId());

        try {
            StripeObject stripeObject = dataObjectDeserializer.deserializeUnsafe();
            if (clazz.isInstance(stripeObject)) {
                return clazz.cast(stripeObject);
            }
        } catch (EventDataObjectDeserializationException e) {
            log.error("Stripe SDK deserialization error during unsafe fallback for event ID: {}", event.getId(), e);
            throw new IllegalStateException("Failed to deserialize Stripe event object", e);
        } catch (Exception e) {
            log.error("Unexpected error during unsafe deserialization for Stripe event ID: {}", event.getId(), e);
            throw new IllegalStateException("Failed to deserialize Stripe event object", e);
        }

        return null;
    }

    /**
     * Helper to extract the Subscription ID from an Invoice object following
     * current Stripe API standards (via Parent -> SubscriptionDetails).
     */
    private String extractSubscriptionId(Invoice invoice) {
        if (invoice == null) {
            return null;
        }

        if (invoice.getParent() != null && invoice.getParent().getSubscriptionDetails() != null) {
            return invoice.getParent()
                    .getSubscriptionDetails()
                    .getSubscription();
        }

        log.warn("Subscription ID not found for invoice ID: {}", invoice.getId());
        return null;
    }
}