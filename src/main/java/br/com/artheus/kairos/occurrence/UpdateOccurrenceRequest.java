package br.com.artheus.kairos.occurrence;

import jakarta.validation.constraints.Size;

public record UpdateOccurrenceRequest(
        @Size(min = 20, max = 100, message = "Title must have between 20 and 100 characters.")
        String title,

        String description
) {
}
