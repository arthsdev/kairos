package br.com.artheus.kairos.occurrence;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/occurrences")
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
    public ResponseEntity<List<OccurrenceResponse>> listVerifiedOccurrences() {
        return ResponseEntity.ok(occurrenceService.getVerifiedOccurrences());
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
