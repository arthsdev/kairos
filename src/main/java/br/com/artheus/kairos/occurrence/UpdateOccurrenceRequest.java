package br.com.artheus.kairos.occurrence;

import jakarta.validation.constraints.*;

public record UpdateOccurrenceRequest(
        @Size(min = 20, max = 100, message = "Title must have between 20 and 100 characters.")
        @Pattern(
                regexp = "^(?=.*[a-zA-Z0-9à-úÀ-Ú])(?!(.)\\1{4,})[a-zA-Z0-9à-úÀ-Ú\\s.,!?'()/:ºª-]+$",
                message = "Title contains invalid characters or repetitive junk text."
        )
        String title,

        @Pattern(
                regexp = "^(?=.*[a-zA-Z0-9à-úÀ-Ú])(?!(.)\\1{4,})[a-zA-Z0-9à-úÀ-Ú\\s.,!?'()/:ºª-]+$",
                message = "Description contains invalid characters or repetitive junk text."
        )
        String description,

        @DecimalMin("-90.0")
        @DecimalMax("90.0")
        Double latitude,

        @DecimalMin("-180.0")
        @DecimalMax("180.0")
        Double longitude
) {
}
