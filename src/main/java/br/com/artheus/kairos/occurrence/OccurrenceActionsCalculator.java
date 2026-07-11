package br.com.artheus.kairos.occurrence;

import br.com.artheus.kairos.shared.enums.OccurrenceStatus;
import org.springframework.stereotype.Component;

@Component
public class OccurrenceActionsCalculator {

    public OccurrenceActions calculate(Occurrence occurrence, String currentUserId, boolean isAdmin) {
        boolean canEdit = canModify(occurrence, currentUserId, isAdmin);
        boolean canDelete = canEdit;
        boolean canVerify = canVerify(occurrence, currentUserId, isAdmin);
        boolean canResolve = canResolve(occurrence, currentUserId, isAdmin);

        return new OccurrenceActions(canEdit, canDelete, canVerify, canResolve);
    }

    private boolean canModify(Occurrence occurrence, String currentUserId, boolean isAdmin) {
        return (currentUserId.equals(occurrence.getUserId()) || isAdmin)
                && !occurrence.isDeleted()
                && !occurrence.isFinalized();
    }

    private boolean canVerify(Occurrence occurrence, String currentUserId, boolean isAdmin) {
        return (isAdmin && !occurrence.isDeleted() && occurrence.getStatus().equals(OccurrenceStatus.PENDING));
    }

    private boolean canResolve(Occurrence occurrence, String currentUserId, boolean isAdmin) {
        return (isAdmin && !occurrence.isDeleted() && occurrence.getStatus().equals(OccurrenceStatus.VERIFIED));
    }


}