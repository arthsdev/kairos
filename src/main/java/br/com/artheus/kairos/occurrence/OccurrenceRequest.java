package br.com.artheus.kairos.occurrence;

import jakarta.validation.constraints.*;

public record OccurrenceRequest(

        @NotBlank(message = "Title cannot be blank.")
        @Size(min = 20, max = 100, message = "Title must have between 20 and 100 characters.")
        String title,

        String description,

        @NotNull(message = " Category cannot be null.")
        OccurrenceCategory category,

        @NotNull(message = "Severity cannot be null.")
        OccurrenceSeverity severity,

        @NotNull(message = "Latitude cannot be null.")
        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        Double latitude,

        @NotNull(message = "Longitude cannot be null.")
        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        Double longitude,

        String imageUrl
) {
}
