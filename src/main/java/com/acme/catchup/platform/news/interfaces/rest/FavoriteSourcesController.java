package com.acme.catchup.platform.news.interfaces.rest;

import com.acme.catchup.platform.news.application.commandservices.FavoriteSourceCommandService;
import com.acme.catchup.platform.news.interfaces.rest.resources.CreateFavoriteSourceResource;
import com.acme.catchup.platform.news.interfaces.rest.resources.FavoriteSourceResource;
import com.acme.catchup.platform.news.interfaces.rest.transform.CreateFavoriteSourceCommandFromResourceAssembler;
import com.acme.catchup.platform.news.interfaces.rest.transform.ResponseEntityFromFavoriteSourceCommandResultAssembler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Slf4j
@RestController
@RequestMapping(value = "/api/v1/favorite-sources", produces = APPLICATION_JSON_VALUE)
@Tag(name = "Favorite Sources", description = "Endpoints for favorite sources")
public class FavoriteSourcesController {
    private final FavoriteSourceCommandService favoriteSourceCommandService;
    private final MessageSource messageSource;

    public FavoriteSourcesController(FavoriteSourceCommandService favoriteSourceCommandService, MessageSource messageSource) {
        this.favoriteSourceCommandService = favoriteSourceCommandService;
        this.messageSource = messageSource;
    }

    @Operation(
            summary = "Create a favorite source",
            description = "Creates a favorite source with the provided news API key and source ID",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Favorite source creation request",
                    content = @Content(schema = @Schema(implementation = CreateFavoriteSourceResource.class))))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Favorite source created",
                    content = @Content(schema = @Schema(implementation = FavoriteSourceResource.class))),
            @ApiResponse(responseCode = "400", description = "Bad request",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflict - favorite source already exists for the given newsApiKey and sourceId",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping
    public ResponseEntity<?> createFavoriteSource(
            @Valid @RequestBody CreateFavoriteSourceResource resource,
            @Parameter(description = "Preferred response language (e.g. en, es)")
            @RequestHeader(value = "Accept-Language", required = false) String acceptLanguage) {
        log.debug("POST /api/v1/favorite-sources – newsApiKey={}, sourceId={}, Accept-Language={}",
                mask(resource.newsApiKey()), resource.sourceId(), acceptLanguage);
        var favoriteSource = favoriteSourceCommandService
                .handle(CreateFavoriteSourceCommandFromResourceAssembler.toCommandFromResource(resource));
        var response = ResponseEntityFromFavoriteSourceCommandResultAssembler.toResponseEntityFromResult(favoriteSource, messageSource);
        log.debug("POST /api/v1/favorite-sources – response status={}", response.getStatusCode());
        return response;
    }

    private static String mask(String value) {
        if (value == null || value.length() <= 4) return "****";
        return "****" + value.substring(value.length() - 4);
    }
}