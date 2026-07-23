package br.com.artheus.kairos.payment;

import br.com.artheus.kairos.shared.contract.payment.PaymentWebhookProcessor;
import com.stripe.exception.EventDataObjectDeserializationException;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks")
@Slf4j
public class StripeWebhookController {

    private final String webhookSecret;
    private final PaymentWebhookProcessor paymentWebhookProcessor;

    public StripeWebhookController(
            @Value("${stripe.webhook-secret}") String webhookSecret,
            PaymentWebhookProcessor paymentWebhookProcessor
    ) {
        this.webhookSecret = webhookSecret;
        this.paymentWebhookProcessor = paymentWebhookProcessor;
    }

    @PostMapping("/stripe")
    public ResponseEntity<Void> handleStripeWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;

        try {
            event = Webhook.constructEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.error("Invalid Stripe signature header", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        } catch (Exception e) {
            log.error("Error parsing Stripe webhook payload", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }

        switch (event.getType()) {
            case "checkout.session.completed":
                processCheckoutCompleted(event);
                break;

            default:
                log.debug("Unhandled Stripe event type: {}", event.getType());
                break;
        }

        return ResponseEntity.ok().build();
    }

    private void processCheckoutCompleted(Event event) {
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();
        Session session = null;

        if (dataObjectDeserializer.getObject().isPresent()) {
            StripeObject stripeObject = dataObjectDeserializer.getObject().get();
            if (stripeObject instanceof Session s) {
                session = s;
            }
        } else {
            log.warn("Safe deserialization failed for event ID: {}. Attempting unsafe fallback...", event.getId());
            try {
                StripeObject stripeObject = dataObjectDeserializer.deserializeUnsafe();
                if (stripeObject instanceof Session s) {
                    session = s;
                }
            } catch (EventDataObjectDeserializationException e) {
                log.error("Stripe SDK deserialization error during unsafe fallback for event ID: {}", event.getId(), e);
                throw new IllegalStateException("Failed to deserialize Stripe checkout session", e);
            } catch (Exception e) {
                log.error("Unexpected error during unsafe deserialization for Stripe event ID: {}", event.getId(), e);
                throw new IllegalStateException("Failed to deserialize Stripe checkout session", e);
            }
        }

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
}