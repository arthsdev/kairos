package br.com.artheus.kairos.shared.contract.payment;

public interface PaymentWebhookProcessor {
    void handleCheckoutCompleted(String userId, String customerId, String subscriptionId);
}