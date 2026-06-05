package br.com.artheus.kairos.cities;

import br.com.artheus.kairos.shared.contract.climate.ClimateDataProvider;
import br.com.artheus.kairos.shared.contract.climate.ClimateDataSummary;
import br.com.artheus.kairos.weather.GeocodingResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/monitored-cities")
@RequiredArgsConstructor
public class MonitoredCityController {

    private final MonitoredCityService monitoredCityService;

    private final ClimateDataProvider climateDataProvider;

    @GetMapping("/search")
    public ResponseEntity<GeocodingResponse> searchCity(@RequestParam("name") String cityName) {
        return ResponseEntity.ok(monitoredCityService.searchCity(cityName));
    }

    @PostMapping
    public ResponseEntity<MonitoredCityResponse> addCityToMonitor(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody AddCityRequest addCityRequest,
            UriComponentsBuilder uriBuilder) {

        String userId = jwt.getSubject();

        MonitoredCityResponse monitoredCityResponse = monitoredCityService.addCityToMonitor(addCityRequest.cityName(), userId);

        URI uri = uriBuilder
                .path("/api/v1/monitored-cities/{id}")
                .buildAndExpand(monitoredCityResponse.id())
                .toUri();

        return ResponseEntity.created(uri).body(monitoredCityResponse);
    }

    @GetMapping
    public ResponseEntity<List<MonitoredCityResponse>> listMyCities(@AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();

        return ResponseEntity.ok(monitoredCityService.listMyCities(userId));
    }

    @GetMapping("/{id}/climate")
    public ResponseEntity<ClimateDataSummary> findLatestByCity(@PathVariable String id) {
        return ResponseEntity.ok(climateDataProvider.findLatestByCity(id));
    }

}
