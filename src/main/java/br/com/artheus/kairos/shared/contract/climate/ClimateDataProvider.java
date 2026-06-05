package br.com.artheus.kairos.shared.contract.climate;

public interface ClimateDataProvider {

    ClimateDataSummary findLatestByCity(String cityId);
}
