package br.com.artheus.kairos.cities;

import br.com.artheus.kairos.shared.contract.climate.ClimateDataProvider;
import br.com.artheus.kairos.shared.contract.climate.ClimateDataSummary;
import br.com.artheus.kairos.weather.GeocodingResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
@Tag(name = "Monitored Cities", description = "Endpoints to manage and track weather/climate data for specific cities.")
public class MonitoredCityController {

    private final MonitoredCityService monitoredCityService;

    private final ClimateDataProvider climateDataProvider;

    @GetMapping("/search")
    @Operation(
            summary = "Search for a city by name",
            description = "Retrieves geocoding information for a specific city to find its location details."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "City information retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "City name parameter is missing or invalid"),
            @ApiResponse(responseCode = "401", description = "User not authenticated or invalid token"),
            @ApiResponse(responseCode = "502", description = "Geocoding integration service temporarily unavailable or timed out")
    })
    public ResponseEntity<GeocodingResponse> searchCity(
            @Parameter(description = "The name of the city to search for", required = true, example = "London")
            @RequestParam("name") String cityName) {
        return ResponseEntity.ok(monitoredCityService.searchCity(cityName));
    }

    @PostMapping
    @Operation(
            summary = "Add a city to monitoring list",
            description = "Registers a new city to be monitored by the system under the authenticated user's account."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "City successfully added to the monitoring list"),
            @ApiResponse(responseCode = "400", description = "Invalid or malformed request payload"),
            @ApiResponse(responseCode = "401", description = "User not authenticated or invalid token"),
            @ApiResponse(responseCode = "422", description = "Business rule violation (e.g., city already monitored, plan limit reached)")
    })
    public ResponseEntity<MonitoredCityResponse> addCityToMonitor(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @RequestBody @Valid AddCityRequest addCityRequest,
            @Parameter(hidden = true) UriComponentsBuilder uriBuilder) {

        String userId = jwt.getSubject();

        MonitoredCityResponse monitoredCityResponse = monitoredCityService.addCityToMonitor(addCityRequest, userId);

        URI uri = uriBuilder
                .path("/api/v1/monitored-cities/{id}")
                .buildAndExpand(monitoredCityResponse.id())
                .toUri();

        return ResponseEntity.created(uri).body(monitoredCityResponse);
    }

    @GetMapping
    @Operation(
            summary = "List user's monitored cities",
            description = "Retrieves all cities currently being monitored by the authenticated user."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List of monitored cities retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated or invalid token")
    })
    public ResponseEntity<List<MonitoredCityResponse>> listMyCities(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();

        return ResponseEntity.ok(monitoredCityService.listMyCities(userId));
    }

    @GetMapping("/{id}/climate")
    @Operation(
            summary = "Get latest climate data for a city",
            description = "Retrieves the most recent weather and climate data summary for a monitored city using its unique ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Climate data retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated or invalid token"),
            @ApiResponse(responseCode = "404", description = "Monitored city not found with the provided ID"),
            @ApiResponse(responseCode = "502", description = "Weather integration service temporarily unavailable or timed out")
    })
    public ResponseEntity<ClimateDataSummary> findLatestByCity(
            @Parameter(description = "The unique ID of the monitored city", required = true, example = "123e4567-e89b-12d3-a456-426614174000")
            @PathVariable String id) {
        return ResponseEntity.ok(climateDataProvider.findLatestByCity(id));
    }
}
