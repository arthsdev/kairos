package br.com.artheus.kairos.payment;

import br.com.artheus.kairos.shared.contract.payment.PaymentWebhookProcessor;
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
            log.error("Invalid Stripe signature", e);
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
                log.debug("Unhandled event type: {}", event.getType());
                break;
        }

        return ResponseEntity.ok().build();
    }

    private void processCheckoutCompleted(Event event) {
        EventDataObjectDeserializer dataObjectDeserializer = event.getDataObjectDeserializer();

        if (dataObjectDeserializer.getObject().isPresent()) {
            StripeObject stripeObject = dataObjectDeserializer.getObject().get();

            if (stripeObject instanceof Session session) {
                String userId = session.getClientReferenceId();
                String customerId = session.getCustomer();
                String subscriptionId = session.getSubscription();

                paymentWebhookProcessor.handleCheckoutCompleted(userId, customerId, subscriptionId);
            }
        } else {
            log.warn("Deserialization failed for event ID: {}", event.getId());
        }
    }
}