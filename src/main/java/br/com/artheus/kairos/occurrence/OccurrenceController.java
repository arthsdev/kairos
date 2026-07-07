package br.com.artheus.kairos.occurrence;

import br.com.artheus.kairos.shared.enums.OccurrenceStatus;
import br.com.artheus.kairos.shared.pagination.PaginatedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
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
@Tag(name = "Occurrences", description = "Endpoints to manage occurrences.")
public class OccurrenceController {

    private final OccurrenceService occurrenceService;

    @PostMapping
    @Operation(
            summary = "Create a new occurrence",
            description = "Registers a new occurrence linked to the authenticated user from the JWT token."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Occurrence created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or malformed request payload"),
            @ApiResponse(responseCode = "401", description = "User not authenticated or invalid token"),
            @ApiResponse(responseCode = "429", description = "Too many requests (Rate limit exceeded)")
    })
    public ResponseEntity<OccurrenceResponse> createOccurrence(
            @Valid @RequestBody OccurrenceRequest request,
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt) {

        String userId = jwt.getSubject();
        return ResponseEntity.ok(occurrenceService.createOccurrence(request, userId));
    }

    @GetMapping
    @Operation(
            summary = "List verified occurrences",
            description = "Retrieves a paginated list of all occurrences that have already been verified by the system."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "List retrieved successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated or invalid token")
    })
    public ResponseEntity<PaginatedResponse<OccurrenceResponse>> listVerifiedOccurrences(
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {

        return ResponseEntity.ok(occurrenceService.getVerifiedOccurrences(pageable));
    }

    @Operation(
            summary = "List current user occurrences",
            description = "Retrieves a paginated list of occurrences belonging to the authenticated user, optionally filtered by status."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved the list"),
            @ApiResponse(responseCode = "401", description = "Unauthorized - Invalid or missing token")
    })
    @GetMapping("/me")
    public ResponseEntity<PaginatedResponse<OccurrenceResponse>> getMyOccurrences(
            @Parameter(hidden = true) @AuthenticationPrincipal Jwt jwt,
            @Parameter(description = "Optional status to filter occurrences")
            @RequestParam(required = false) OccurrenceStatus status,
            @ParameterObject @PageableDefault(page = 0, size = 10, sort = "createdAt", direction = Sort.Direction.ASC) Pageable pageable) {

        String userId = jwt.getSubject();
        return ResponseEntity.ok(occurrenceService.getMyOccurrences(userId, status, pageable));
    }

    @PatchMapping("/{id}")
    @Operation(
            summary = "Update an occurrence",
            description = "Partially updates an existing occurrence based on the provided ID."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Occurrence updated successfully"),
            @ApiResponse(responseCode = "400", description = "Provided data is invalid"),
            @ApiResponse(responseCode = "401", description = "User not authenticated or invalid token"),
            @ApiResponse(responseCode = "403", description = "User does not have permission to modify this occurrence"),
            @ApiResponse(responseCode = "404", description = "Occurrence not found with the provided ID")
    })
    public ResponseEntity<OccurrenceResponse> updateOccurrence(
            @PathVariable String id,
            @Valid @RequestBody OccurrenceRequest request) {

        return ResponseEntity.ok(occurrenceService.updateOccurrence(id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Soft-delete an occurrence",
            description = "Soft-deletes an occurrence by setting its deletion timestamp."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Occurrence deleted successfully"),
            @ApiResponse(responseCode = "401", description = "User not authenticated or invalid token"),
            @ApiResponse(responseCode = "403", description = "User does not have permission to delete this occurrence"),
            @ApiResponse(responseCode = "404", description = "Occurrence not found with the provided ID"),
            @ApiResponse(
                    responseCode = "422",
                    description = "The occurrence cannot be deleted because it has already been deleted or finalized (status VERIFIED or RESOLVED)."
            )
    })
    public ResponseEntity<OccurrenceResponse> deleteOccurrence(@PathVariable String id) {
        return ResponseEntity.ok(occurrenceService.deleteOccurrence(id));
    }
}