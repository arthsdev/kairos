package br.com.artheus.kairos.occurrence;

public record OccurrenceActions(
        boolean canEdit,
        boolean canDelete,
        boolean canVerify,
        boolean canResolve
) {
}
