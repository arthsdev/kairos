package br.com.artheus.kairos.plan;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PlanProvisioner Unit Tests")
class PlanProvisionerTest {

    @Mock
    private PlanRepository planRepository;

    @InjectMocks
    private PlanProvisioner planProvisioner;

    @Test
    @DisplayName("Should build and saveAndFlush a new plan for the given user ID")
    void shouldCreateAndPersistPlan() {
        String userId = "user-123";

        planProvisioner.createPlan(userId);

        ArgumentCaptor<Plan> planCaptor = ArgumentCaptor.forClass(Plan.class);
        verify(planRepository, times(1)).saveAndFlush(planCaptor.capture());
        assertThat(planCaptor.getValue().getUserId()).isEqualTo(userId);
    }

    @Test
    @DisplayName("Should propagate DataIntegrityViolationException when constraint is violated")
    void shouldPropagateExceptionOnConstraintViolation() {
        String userId = "user-concurrent";
        doThrow(new DataIntegrityViolationException("Duplicate key"))
                .when(planRepository).saveAndFlush(any(Plan.class));

        assertThatThrownBy(() -> planProvisioner.createPlan(userId))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}