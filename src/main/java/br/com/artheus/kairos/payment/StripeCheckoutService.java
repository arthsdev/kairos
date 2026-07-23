package br.com.artheus.kairos.payment;

import br.com.artheus.kairos.shared.contract.payment.PaymentCheckoutProvider;
import br.com.artheus.kairos.shared.exception.ExternalServiceException;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeCheckoutService implements PaymentCheckoutProvider {

    private final String premiumPriceId;
    private final String successUrl;
    private final String cancelUrl;

    public StripeCheckoutService(
            @Value("${stripe.premium-price-id}") String premiumPriceId,
            @Value("${stripe.success-url}") String successUrl,
            @Value("${stripe.cancel-url}") String cancelUrl
    ) {
        this.premiumPriceId = premiumPriceId;
        this.successUrl = successUrl;
        this.cancelUrl = cancelUrl;
    }

    @Override
    public String createCheckoutSession(String userId) {
        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setClientReferenceId(userId)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(1L)
                                    .setPrice(premiumPriceId)
                                    .build()
                    )
                    .build();

            Session session = Session.create(params);

            return session.getUrl();

        } catch (StripeException e) {
            throw new ExternalServiceException(
                    "stripe.checkout.error",
                    "Failed to communicate with payment provider (Stripe).",
                    e
            );
        }
    }
}