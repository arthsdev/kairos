package br.com.artheus.kairos.occurrence;


import br.com.artheus.kairos.shared.pagination.PaginatedResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/occurrences")
@RequiredArgsConstructor
public class OccurrenceController {


    private final OccurrenceService occurrenceService;

    @PostMapping
    public ResponseEntity<OccurrenceResponse> createOccurrence(
            @Valid @RequestBody OccurrenceRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        return ResponseEntity.ok(occurrenceService.createOccurrence(request, userId));
    }

    @GetMapping
    public ResponseEntity<PaginatedResponse<OccurrenceResponse>> listVerifiedOccurrences(
            // TODO(artheus): AFTER ADDING SWAGGER ADD A @ParameterObject BEFORE PAGEABLEDEFAULT
            @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {

        return ResponseEntity.ok(occurrenceService.getVerifiedOccurrences(pageable));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<OccurrenceResponse> updateOccurrence(@PathVariable String id,
                                                               @Valid @RequestBody OccurrenceRequest request,
                                                               @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        return ResponseEntity.ok(occurrenceService.updateOccurrence(id, request, userId));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteOccurrence(@PathVariable String id,
                                                 @AuthenticationPrincipal Jwt jwt) {
        String userId = jwt.getSubject();
        occurrenceService.deleteOccurrence(id, userId);

        return ResponseEntity.noContent().build();
    }

}
