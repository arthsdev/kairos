package br.com.artheus.kairos.shared.contract.risk;

public interface RiskCalculationPublisher {

    void publish(RiskMessage message);
}
