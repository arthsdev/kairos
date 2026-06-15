package br.com.artheus.kairos.shared.contract.climate;

import br.com.artheus.kairos.shared.contract.risk.RiskMessage;

public interface ClimateDataWriter {
    void saveClimateData(RiskMessage riskMessage);
}
