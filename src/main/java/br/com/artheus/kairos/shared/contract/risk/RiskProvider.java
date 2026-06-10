package br.com.artheus.kairos.shared.contract.risk;

import br.com.artheus.kairos.shared.enums.RiskLevel;

public interface RiskPersistenceProvider {

    void saveCalculatedRisk(String cityId, RiskLevel riskLevel);
}