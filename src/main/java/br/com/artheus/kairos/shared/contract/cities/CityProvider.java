package br.com.artheus.kairos.shared.contract.cities;

import java.util.List;

public interface CityProvider {

    List<CityLocation> findActiveCities();
}
