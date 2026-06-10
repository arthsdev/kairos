package br.com.artheus.kairos.shared.contract.risk;


import java.util.List;

public interface OccurrenceDataProvider {

    List<OccurrenceSummary> findVerifiedByCityId(String cityId);
}
