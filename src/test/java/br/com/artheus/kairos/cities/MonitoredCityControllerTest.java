package br.com.artheus.kairos.cities;

import br.com.artheus.kairos.shared.contract.climate.ClimateDataProvider;
import br.com.artheus.kairos.shared.contract.climate.ClimateDataSummary;
import br.com.artheus.kairos.shared.enums.RiskLevel;
import br.com.artheus.kairos.weather.GeocodingResponse;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = MonitoredCityController.class,
        properties = {
                "KEYCLOAK_URL=http://localhost:8080",
                "KEYCLOAK_REALM=mock-realm",
                "KEYCLOAK_INTERNAL_URL=http://localhost:8080"
        }
)
@Import(MonitoredCityControllerTest.TestSecurityConfig.class)
class MonitoredCityControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private MonitoredCityService monitoredCityService;

    @MockitoBean
    private ClimateDataProvider climateDataProvider;

    @MockitoBean
    private ProxyManager<String> proxyManager;

    private static final String USER_ID = "user-123";

    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain filterChain(HttpSecurity http){
            http
                    .csrf(AbstractHttpConfigurer::disable)
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

            return http.build();
        }
    }

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUpRateLimiting() {
        var builderMock = mock(RemoteBucketBuilder.class, Answers.RETURNS_DEEP_STUBS);
        when(proxyManager.builder()).thenReturn(builderMock);

        var bucket = mock(BucketProxy.class);
        doReturn(bucket)
                .when(builderMock)
                .build(any(), any(java.util.function.Supplier.class));

        var probe = mock(ConsumptionProbe.class);
        when(probe.isConsumed()).thenReturn(true);
        when(probe.getRemainingTokens()).thenReturn(10L);
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
    }

    @Nested
    @DisplayName("GET /api/v1/monitored-cities/search")
    class SearchCityTests {

        @Test
        @DisplayName("Should return 200 OK with GeocodingResponse when valid city name is provided")
        void shouldReturn200WhenSearchingCity() throws Exception {
            GeocodingResponse expectedResponse = new GeocodingResponse(List.of());
            when(monitoredCityService.searchCity("London")).thenReturn(expectedResponse);

            mockMvc.perform(get("/api/v1/monitored-cities/search")
                            .param("name", "London")
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON));

            verify(monitoredCityService, times(1)).searchCity("London");
        }

        @Test
        @DisplayName("Should return 400 Bad Request when query parameter name is missing")
        void shouldReturn400WhenNameQueryParamIsMissing() throws Exception {
            mockMvc.perform(get("/api/v1/monitored-cities/search")
                            .with(jwt()))
                    .andExpect(status().isBadRequest());

            verifyNoInteractions(monitoredCityService);
        }
    }

    @Nested
    @DisplayName("POST /api/v1/monitored-cities")
    class AddCityToMonitorTests {

        @Test
        @DisplayName("Should return 201 Created with Location header and response body")
        void shouldReturn201WhenAddingCity() throws Exception {
            AddCityRequest request = new AddCityRequest("Joinville", -26.3, -48.8, "SC", "Brazil");
            MonitoredCityResponse response = new MonitoredCityResponse(
                    "city-1", "Joinville", "SC", -26.3, -48.8, "Brazil", true
            );

            when(monitoredCityService.addCityToMonitor(any(AddCityRequest.class), eq(USER_ID)))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/monitored-cities")
                            .with(jwt().jwt(j -> j.subject(USER_ID)))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost/api/v1/monitored-cities/city-1"))
                    .andExpect(jsonPath("$.id").value("city-1"))
                    .andExpect(jsonPath("$.name").value("Joinville"))
                    .andExpect(jsonPath("$.active").value(true));

            verify(monitoredCityService, times(1)).addCityToMonitor(any(AddCityRequest.class), eq(USER_ID));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/monitored-cities")
    class ListMyCitiesTests {

        @Test
        @DisplayName("Should return 200 OK with list of monitored cities")
        void shouldReturn200WithCityList() throws Exception {
            MonitoredCityResponse city1 = new MonitoredCityResponse("c-1", "Joinville", "SC", -26.3, -48.8, "Brazil", true);
            MonitoredCityResponse city2 = new MonitoredCityResponse("c-2", "Blumenau", "SC", -26.9, -49.0, "Brazil", true);

            when(monitoredCityService.listMyCities(USER_ID)).thenReturn(List.of(city1, city2));

            mockMvc.perform(get("/api/v1/monitored-cities")
                            .with(jwt().jwt(j -> j.subject(USER_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.[0].id").value("c-1"))
                    .andExpect(jsonPath("$.[1].id").value("c-2"));

            verify(monitoredCityService, times(1)).listMyCities(USER_ID);
        }

        @Test
        @DisplayName("Should return 200 OK with empty list when user monitors no cities")
        void shouldReturn200WithEmptyList() throws Exception {
            when(monitoredCityService.listMyCities(USER_ID)).thenReturn(Collections.emptyList());

            mockMvc.perform(get("/api/v1/monitored-cities")
                            .with(jwt().jwt(j -> j.subject(USER_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$").isArray());

            verify(monitoredCityService, times(1)).listMyCities(USER_ID);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/monitored-cities/{id}/climate")
    class FindLatestByCityTests {

        @Test
        @DisplayName("Should return 200 OK with climate summary for specified city ID")
        void shouldReturn200WithClimateSummary() throws Exception {
            String cityId = "city-100";
            ClimateDataSummary summary = new ClimateDataSummary(
                    24.5, 68.0, 0.0, 12.0, RiskLevel.LOW, LocalDateTime.now()
            );

            when(climateDataProvider.findLatestByCity(cityId)).thenReturn(summary);

            mockMvc.perform(get("/api/v1/monitored-cities/{id}/climate", cityId)
                            .with(jwt()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.temperature").value(24.5))
                    .andExpect(jsonPath("$.humidity").value(68.0))
                    .andExpect(jsonPath("$.riskLevel").value("LOW"));

            verify(climateDataProvider, times(1)).findLatestByCity(cityId);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/monitored-cities/climate")
    class ListMyCitiesWithClimateTests {

        @Test
        @DisplayName("Should return 200 OK with monitored cities enriched with climate data")
        void shouldReturn200WithCitiesAndClimateData() throws Exception {
            ClimateDataSummary summary = new ClimateDataSummary(
                    25.0, 70.0, 5.0, 10.0, RiskLevel.LOW, LocalDateTime.now()
            );

            MonitoredCityWithClimateResponse cityWithClimate = new MonitoredCityWithClimateResponse(
                    "c-1", "Joinville", "SC", -26.3, -48.8, "Brazil", true, summary
            );

            MonitoredCityWithClimateResponse cityWithoutClimate = new MonitoredCityWithClimateResponse(
                    "c-2", "Blumenau", "SC", -26.9, -49.0, "Brazil", true, null
            );

            when(monitoredCityService.listMyCitiesWithClimate(USER_ID))
                    .thenReturn(List.of(cityWithClimate, cityWithoutClimate));

            mockMvc.perform(get("/api/v1/monitored-cities/climate")
                            .with(jwt().jwt(j -> j.subject(USER_ID))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.[0].id").value("c-1"))
                    .andExpect(jsonPath("$.[0].climate.temperature").value(25.0))
                    .andExpect(jsonPath("$.[0].climate.riskLevel").value("LOW"))
                    .andExpect(jsonPath("$.[1].id").value("c-2"))
                    .andExpect(jsonPath("$.[1].climate").doesNotExist());

            verify(monitoredCityService, times(1)).listMyCitiesWithClimate(USER_ID);
        }
    }
}