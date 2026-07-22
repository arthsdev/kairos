package br.com.artheus.kairos.shared.contract.payment;

public interface PaymentCheckoutProvider {
    String createCheckoutSession(String userId);
}
