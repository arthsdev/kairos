package br.com.artheus.kairos.occurrence;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateOccurrenceRequest(
        @NotBlank
        @Size(min = 20, max = 100, message = "Title must have between 20 and 100 characters.")
        @Pattern(
                regexp = "^(?=.*[a-zA-Z0-9à-úÀ-Ú])(?!(.)\\1{4,})[a-zA-Z0-9à-úÀ-Ú\\s.,!?'()/:ºª-]+$",
                message = "Title contains invalid characters or repetitive junk text."
        )
        String title,

        @NotBlank
        @Pattern(
                regexp = "^(?=.*[a-zA-Z0-9à-úÀ-Ú])(?!(.)\\1{4,})[a-zA-Z0-9à-úÀ-Ú\\s.,!?'()/:ºª-]+$",
                message = "Description contains invalid characters or repetitive junk text."
        )
        String description
) {
}
