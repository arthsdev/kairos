package br.com.artheus.kairos.occurrence;

import br.com.artheus.kairos.shared.enums.OccurrenceCategory;
import br.com.artheus.kairos.shared.enums.OccurrenceSeverity;
import br.com.artheus.kairos.shared.enums.OccurrenceStatus;
import br.com.artheus.kairos.shared.pagination.PaginatedResponse;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.distributed.proxy.RemoteBucketBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Supplier;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = OccurrenceController.class,
        properties = {
                "KEYCLOAK_URL=http://localhost:8080",
                "KEYCLOAK_REALM=mock-realm"
        }
)
@Import(OccurrenceControllerTest.TestSecurityConfig.class)
class OccurrenceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OccurrenceService occurrenceService;

    @MockitoBean
    private ProxyManager<String> proxyManager;

    @TestConfiguration
    static class TestSecurityConfig {

        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) {
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
                .build(any(), any(Supplier.class));

        var probe = mock(ConsumptionProbe.class);
        when(probe.isConsumed()).thenReturn(true);
        when(probe.getRemainingTokens()).thenReturn(10L);
        when(bucket.tryConsumeAndReturnRemaining(1)).thenReturn(probe);
    }

    private OccurrenceResponse createMockResponse() {
        return new OccurrenceResponse(
                "occ-123",
                "This is a valid title with more than twenty characters",
                "Description",
                OccurrenceCategory.SEWAGE,
                OccurrenceSeverity.HIGH,
                OccurrenceStatus.PENDING,
                0.0,
                0.0,
                "url",
                "user-789",
                "#12345",
                LocalDateTime.now(),
                "city-123",
                new OccurrenceActions(true, true, false, false)
        );
    }

    @Nested
    @DisplayName("POST /api/v1/occurrences")
    class CreateOccurrenceTests {

        @Test
        @DisplayName("Should create occurrence and return 200 OK when payload is valid")
        void shouldCreateOccurrenceWhenValid() throws Exception {
            OccurrenceRequest request = new OccurrenceRequest(
                    "This is a valid title with more than twenty characters",
                    "Description",
                    OccurrenceCategory.FLOOD,
                    OccurrenceSeverity.HIGH,
                    0.0, 0.0, "url", "city-123"
            );
            OccurrenceResponse response = createMockResponse();

            when(occurrenceService.createOccurrence(any(OccurrenceRequest.class), eq("user-789")))
                    .thenReturn(response);

            mockMvc.perform(post("/api/v1/occurrences")
                            .with(jwt().jwt(j -> j.subject("user-789")))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value("occ-123"))
                    .andExpect(jsonPath("$.title").value(request.title()));
        }

        @Test
        @DisplayName("Should return 400 Bad Request when payload violates bean validation")
        void shouldReturn400WhenPayloadIsInvalid() throws Exception {
            OccurrenceRequest invalidRequest = new OccurrenceRequest(
                    "Short title",
                    "Description", null, null, 0.0, 0.0, "url", "city-123"
            );

            mockMvc.perform(post("/api/v1/occurrences")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/occurrences")
    class ListOccurrencesTests {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("Should return paginated list of occurrences when no status filter is provided")
        void shouldReturnPaginatedOccurrencesWithoutStatus() throws Exception {
            PaginatedResponse<OccurrenceResponse> paginatedResponse = mock(PaginatedResponse.class);
            when(paginatedResponse.data()).thenReturn(List.of(createMockResponse()));

            when(occurrenceService.getOccurrences(isNull(), any(Pageable.class)))
                    .thenReturn(paginatedResponse);

            mockMvc.perform(get("/api/v1/occurrences")
                            .with(jwt())
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("Should return paginated list of occurrences filtered by status")
        void shouldReturnPaginatedOccurrencesWithStatusFilter() throws Exception {
            PaginatedResponse<OccurrenceResponse> paginatedResponse = mock(PaginatedResponse.class);
            when(paginatedResponse.data()).thenReturn(List.of(createMockResponse()));

            when(occurrenceService.getOccurrences(eq(OccurrenceStatus.VERIFIED), any(Pageable.class)))
                    .thenReturn(paginatedResponse);

            mockMvc.perform(get("/api/v1/occurrences")
                            .with(jwt())
                            .param("status", "VERIFIED")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/occurrences/me")
    class GetMyOccurrencesTests {

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("Should return user occurrences filtered by status optionally")
        void shouldReturnUserOccurrences() throws Exception {
            PaginatedResponse<OccurrenceResponse> paginatedResponse = mock(PaginatedResponse.class);
            when(paginatedResponse.data()).thenReturn(List.of(createMockResponse()));

            when(occurrenceService.getMyOccurrences(eq("user-789"), eq(OccurrenceStatus.PENDING), any(Pageable.class)))
                    .thenReturn(paginatedResponse);

            mockMvc.perform(get("/api/v1/occurrences/me")
                            .with(jwt().jwt(j -> j.subject("user-789")))
                            .param("status", "PENDING"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }

        @SuppressWarnings("unchecked")
        @Test
        @DisplayName("Should return user occurrences without status filter")
        void shouldReturnUserOccurrencesWithoutStatusFilter() throws Exception {
            PaginatedResponse<OccurrenceResponse> paginatedResponse = mock(PaginatedResponse.class);
            when(paginatedResponse.data()).thenReturn(List.of(createMockResponse()));

            when(occurrenceService.getMyOccurrences(eq("user-789"), isNull(), any(Pageable.class)))
                    .thenReturn(paginatedResponse);

            mockMvc.perform(get("/api/v1/occurrences/me")
                            .with(jwt().jwt(j -> j.subject("user-789"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/occurrences/{id}")
    class UpdateOccurrenceTests {

        @Test
        @DisplayName("Should update occurrence partially and return 200 OK")
        void shouldUpdateOccurrence() throws Exception {
            UpdateOccurrenceRequest updateRequest = new UpdateOccurrenceRequest(
                    "New Title Update More Than Twenty",
                    "New Desc",
                    -23.55,
                    -46.63
            );
            OccurrenceResponse response = createMockResponse();

            when(occurrenceService.updateOccurrence(eq("occ-123"), any(UpdateOccurrenceRequest.class)))
                    .thenReturn(response);

            mockMvc.perform(patch("/api/v1/occurrences/occ-123")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("Should pass new latitude and longitude from request body to service layer")
        void shouldPassLocationToServiceOnUpdate() throws Exception {
            UpdateOccurrenceRequest updateRequest = new UpdateOccurrenceRequest(
                    "This is a valid updated title with more than twenty characters",
                    "New Desc",
                    -23.5555,
                    -46.6333
            );
            OccurrenceResponse response = createMockResponse();

            ArgumentCaptor<UpdateOccurrenceRequest> requestCaptor = ArgumentCaptor.forClass(UpdateOccurrenceRequest.class);

            when(occurrenceService.updateOccurrence(eq("occ-123"), requestCaptor.capture()))
                    .thenReturn(response);

            mockMvc.perform(patch("/api/v1/occurrences/occ-123")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateRequest)))
                    .andExpect(status().isOk());

            UpdateOccurrenceRequest capturedRequest = requestCaptor.getValue();
            assertThat(capturedRequest.latitude()).isEqualTo(-23.5555);
            assertThat(capturedRequest.longitude()).isEqualTo(-46.6333);
        }

        @Test
        @DisplayName("Should return 400 Bad Request when longitude is out of bounds (< -180.0)")
        void shouldReturn400WhenLongitudeIsOutOfBounds() throws Exception {
            UpdateOccurrenceRequest invalidRequest = new UpdateOccurrenceRequest(
                    "This is a valid title with more than twenty characters",
                    "Valid description",
                    -23.5555,
                    -180.01
            );

            mockMvc.perform(patch("/api/v1/occurrences/occ-123")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(invalidRequest)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.message").exists());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/occurrences/{id}/verify")
    class VerifyOccurrenceTests {

        @Test
        @DisplayName("Should verify occurrence state")
        void shouldVerifyOccurrence() throws Exception {
            OccurrenceResponse response = createMockResponse();

            when(occurrenceService.verifyOccurrence("occ-123")).thenReturn(response);

            mockMvc.perform(post("/api/v1/occurrences/occ-123/verify")
                            .with(jwt()))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("POST /api/v1/occurrences/{id}/resolve")
    class ResolveOccurrenceTests {

        @Test
        @DisplayName("Should transition occurrence state to resolved")
        void shouldResolveOccurrence() throws Exception {
            OccurrenceResponse response = createMockResponse();

            when(occurrenceService.resolveOccurrence("occ-123")).thenReturn(response);

            mockMvc.perform(post("/api/v1/occurrences/occ-123/resolve")
                            .with(jwt()))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/occurrences/{id}")
    class DeleteOccurrenceTests {

        @Test
        @DisplayName("Should soft-delete occurrence successfully")
        void shouldDeleteOccurrence() throws Exception {
            OccurrenceResponse response = createMockResponse();

            when(occurrenceService.deleteOccurrence("occ-123")).thenReturn(response);

            mockMvc.perform(delete("/api/v1/occurrences/occ-123")
                            .with(jwt()))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/occurrences/map")
    class GetMapOccurrencesTests {

        @Test
        @DisplayName("Should return 200 OK with list of map occurrences when authenticated")
        void shouldReturnMapOccurrencesWhenAuthenticated() throws Exception {
            MapOccurrenceDTO dto = new MapOccurrenceDTO(
                    "occ-1",
                    -23.55,
                    -46.63,
                    OccurrenceCategory.FLOOD,
                    OccurrenceSeverity.MEDIUM,
                    OccurrenceStatus.VERIFIED
            );

            when(occurrenceService.getMapOccurrences()).thenReturn(List.of(dto));

            mockMvc.perform(get("/api/v1/occurrences/map")
                            .with(jwt())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$[0].id").value("occ-1"))
                    .andExpect(jsonPath("$[0].latitude").value(-23.55))
                    .andExpect(jsonPath("$[0].longitude").value(-46.63))
                    .andExpect(jsonPath("$[0].category").value("FLOOD"))
                    .andExpect(jsonPath("$[0].severity").value("MEDIUM"))
                    .andExpect(jsonPath("$[0].status").value("VERIFIED"));

            verify(occurrenceService).getMapOccurrences();
        }
    }
}