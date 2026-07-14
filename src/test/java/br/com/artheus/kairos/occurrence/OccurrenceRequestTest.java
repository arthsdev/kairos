package br.com.artheus.kairos.occurrence;

import br.com.artheus.kairos.shared.enums.OccurrenceCategory;
import br.com.artheus.kairos.shared.enums.OccurrenceSeverity;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class OccurrenceRequestTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    private OccurrenceRequest createValidRequestBuilder() {
        return new OccurrenceRequest(
                "This is a valid title with more than twenty characters", // 54 chars
                "Valid description for the occurrence",
                OccurrenceCategory.FLOOD,
                OccurrenceSeverity.HIGH,
                -23.55052, // Valid latitude
                -46.633308, // Valid longitude
                "https://image.url/photo.jpg",
                "city-123"
        );
    }

    @Test
    @DisplayName("Should pass validation when all fields are valid")
    void shouldPassWhenRequestIsValid() {
        OccurrenceRequest request = createValidRequestBuilder();

        Set<ConstraintViolation<OccurrenceRequest>> violations = validator.validate(request);

        assertThat(violations).isEmpty();
    }

    @Nested
    @DisplayName("Tests for Title Validation")
    class TitleValidation {

        @Test
        @DisplayName("Should fail when title is blank")
        void shouldFailWhenTitleIsBlank() {
            OccurrenceRequest request = new OccurrenceRequest(
                    "   ", // Blank
                    "Valid description for the occurrence",
                    OccurrenceCategory.LANDSLIDE,
                    OccurrenceSeverity.HIGH,
                    0.0, 0.0, "url", "city-id"
            );

            Set<ConstraintViolation<OccurrenceRequest>> violations = validator.validate(request);

            // It might trigger both @NotBlank and @Size depending on how the engine evaluates spaces
            assertThat(violations).isNotEmpty();
            boolean hasNotBlankMessage = violations.stream()
                    .anyMatch(v -> v.getMessage().equals("Title cannot be blank."));
            assertThat(hasNotBlankMessage).isTrue();
        }

        @Test
        @DisplayName("Should fail when title is too short")
        void shouldFailWhenTitleIsTooShort() {
            OccurrenceRequest request = new OccurrenceRequest(
                    "Too short title", // 15 chars (min is 20)
                    "Valid description for the occurrence",
                    OccurrenceCategory.WILDFIRE,
                    OccurrenceSeverity.HIGH,
                    0.0, 0.0, "url", "city-id"
            );

            Set<ConstraintViolation<OccurrenceRequest>> violations = validator.validate(request);

            assertThat(violations).hasSize(1);
            assertThat(violations.iterator().next().getMessage())
                    .isEqualTo("Title must have between 20 and 100 characters.");
        }
    }

    @Nested
    @DisplayName("Tests for Null Mandatory Fields")
    class NullMandatoryFields {

        @Test
        @DisplayName("Should fail when category, severity, or cityId are null")
        void shouldFailWhenRequiredObjectsAreNull() {
            OccurrenceRequest request = new OccurrenceRequest(
                    "This is a valid title with more than twenty characters",
                    "Valid description for the occurrence",
                    null, // Null Category
                    null, // Null Severity
                    0.0, 0.0, "url",
                    null  // Null CityId
            );

            Set<ConstraintViolation<OccurrenceRequest>> violations = validator.validate(request);

            // Expecting 3 violations (Category, Severity, CityId)
            assertThat(violations).hasSize(3);
        }
    }

    @Nested
    @DisplayName("Tests for Geographic Coordinates")
    class GeographicCoordinates {

        @Test
        @DisplayName("Should fail when coordinates are out of global bounds")
        void shouldFailWhenCoordinatesAreInvalid() {
            OccurrenceRequest request = new OccurrenceRequest(
                    "This is a valid title with more than twenty characters",
                    "Valid description for the occurrence",
                    OccurrenceCategory.SEWAGE,
                    OccurrenceSeverity.HIGH,
                    95.5,    // Invalid Latitude (Max is 90.0)
                    -185.0,  // Invalid Longitude (Min is -180.0)
                    "url", "city-id"
            );
            assertThat(validator.validate(request)).hasSize(2);
        }

        @Test
        @DisplayName("Should fail when coordinates are null")
        void shouldFailWhenCoordinatesAreNull() {
            OccurrenceRequest request = new OccurrenceRequest(
                    "This is a valid title with more than twenty characters",
                    "Valid description for the occurrence",
                    OccurrenceCategory.ILLEGAL_DUMPING,
                    OccurrenceSeverity.HIGH,
                    null, // Null Latitude
                    null, // Null Longitude
                    "url", "city-id"
            );

            assertThat(validator.validate(request)).hasSize(2);
        }
    }
}