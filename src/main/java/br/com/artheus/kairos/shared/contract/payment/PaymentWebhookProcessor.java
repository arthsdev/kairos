package br.com.artheus.kairos.shared.contract.payment;

public interface PaymentWebhookProcessor {
    void handleCheckoutCompleted(String userId, String customerId, String subscriptionId);

    void handleInvoicePaid(String customerId, String subscriptionId);

    void handlePaymentFailed(String customerId, String subscriptionId);

    void handleSubscriptionDeleted(String customerId, String subscriptionId);
}