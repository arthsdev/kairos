package br.com.artheus.kairos.plan;

import br.com.artheus.kairos.shared.exception.BusinessException;
import br.com.artheus.kairos.shared.exception.GlobalExceptionHandler;
import br.com.artheus.kairos.shared.exception.ResourceNotFoundException;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(PlanController.class)
@Import(GlobalExceptionHandler.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("PlanController Web Layer Tests")
class PlanControllerTest {

    private static final String BASE_PATH = "/api/v1/plans";
    private static final String DEFAULT_USER_ID = "user-123";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProxyManager<String> proxyManager;

    @MockitoBean
    private PlanService planService;

    @Nested
    @DisplayName("POST " + BASE_PATH + "/upgrade")
    class StartPremiumCheckoutTests {

        @Test
        @DisplayName("Should return 200 OK and checkout URL when valid JWT is provided")
        void shouldStartPremiumCheckoutSuccessfully() throws Exception {
            String checkoutUrl = "https://checkout.stripe.com/pay/cs_test_123";
            CheckoutSessionResponse response = new CheckoutSessionResponse(checkoutUrl);

            when(planService.startPremiumCheckout(DEFAULT_USER_ID)).thenReturn(response);

            mockMvc.perform(post(BASE_PATH + "/upgrade")
                            .with(jwt().jwt(builder -> builder.subject(DEFAULT_USER_ID)))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.checkoutUrl").value(checkoutUrl));

            verify(planService, times(1)).startPremiumCheckout(DEFAULT_USER_ID);
        }

        @Test
        @DisplayName("Should return 422 Unprocessable Entity when user is already premium")
        void shouldReturn422WhenServiceThrowsBusinessException() throws Exception {
            String errorMessage = "Plan is already Premium";
            when(planService.startPremiumCheckout(DEFAULT_USER_ID))
                    .thenThrow(new BusinessException(errorMessage));

            mockMvc.perform(post(BASE_PATH + "/upgrade")
                            .with(jwt().jwt(builder -> builder.subject(DEFAULT_USER_ID)))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnprocessableEntity())
                    .andExpect(jsonPath("$.status").value(422))
                    .andExpect(jsonPath("$.message").value(errorMessage))
                    .andExpect(jsonPath("$.errorCode").exists())
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(planService, times(1)).startPremiumCheckout(DEFAULT_USER_ID);
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when request lacks authentication")
        void shouldReturnUnauthorizedWhenNoJwtProvided() throws Exception {
            mockMvc.perform(post(BASE_PATH + "/upgrade")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(planService);
        }
    }

    @Nested
    @DisplayName("GET " + BASE_PATH)
    class GetMyPlanTests {

        @Test
        @DisplayName("Should return 200 OK and plan details for authenticated user")
        void shouldGetMyPlanSuccessfully() throws Exception {
            PlanStatusResponse response = new PlanStatusResponse(PlanType.FREE, LocalDateTime.now().plusDays(30));

            when(planService.getMyPlan(DEFAULT_USER_ID)).thenReturn(response);

            mockMvc.perform(get(BASE_PATH)
                            .with(jwt().jwt(builder -> builder.subject(DEFAULT_USER_ID)))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.planType").value(PlanType.FREE.name()))
                    .andExpect(jsonPath("$.expiresAt").exists());

            verify(planService, times(1)).getMyPlan(DEFAULT_USER_ID);
        }

        @Test
        @DisplayName("Should return 404 Not Found when plan does not exist")
        void shouldReturn404WhenPlanDoesNotExist() throws Exception {
            String errorMessage = "Plan not found";
            when(planService.getMyPlan(DEFAULT_USER_ID))
                    .thenThrow(new ResourceNotFoundException(errorMessage));

            mockMvc.perform(get(BASE_PATH)
                            .with(jwt().jwt(builder -> builder.subject(DEFAULT_USER_ID)))
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.message").value(errorMessage))
                    .andExpect(jsonPath("$.errorCode").exists())
                    .andExpect(jsonPath("$.timestamp").exists());

            verify(planService, times(1)).getMyPlan(DEFAULT_USER_ID);
        }

        @Test
        @DisplayName("Should return 401 Unauthorized when request is unauthenticated")
        void shouldReturnUnauthorizedWhenFetchingPlanWithoutJwt() throws Exception {
            mockMvc.perform(get(BASE_PATH)
                            .contentType(MediaType.APPLICATION_JSON))
                    .andExpect(status().isUnauthorized());

            verifyNoInteractions(planService);
        }
    }
}