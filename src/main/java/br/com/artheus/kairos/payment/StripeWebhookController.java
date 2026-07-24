package br.com.artheus.kairos.payment;

import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.net.Webhook;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/webhooks")
@Slf4j
@Tag(name = "Webhooks", description = "Endpoints for external payment provider integrations.")
public class StripeWebhookController {

    private final String webhookSecret;
    private final StripeEventParser stripeEventParser;

    public StripeWebhookController(
            @Value("${stripe.webhook-secret}") String webhookSecret,
            StripeEventParser stripeEventParser
    ) {
        this.webhookSecret = webhookSecret;
        this.stripeEventParser = stripeEventParser;
    }

    @PostMapping("/stripe")
    @Operation(
            summary = "Handle Stripe webhook events",
            description = "Receives asynchronous events from Stripe (checkout completion, invoice payment, " +
                    "subscription cancellation) and applies the corresponding plan state changes. " +
                    "Authenticated via Stripe's HMAC signature, not JWT."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Event received and processed (or ignored if unhandled)"),
            @ApiResponse(responseCode = "400", description = "Invalid signature or malformed payload")
    })
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
            case "checkout.session.completed" -> stripeEventParser.processCheckoutCompleted(event);
            case "invoice.paid" -> stripeEventParser.processInvoicePaid(event);
            case "invoice.payment_failed" -> stripeEventParser.processPaymentFailed(event);
            case "customer.subscription.deleted" -> stripeEventParser.processSubscriptionDeleted(event);
            default -> log.debug("Unhandled Stripe event type: {}", event.getType());
        }

        return ResponseEntity.ok().build();
    }
}