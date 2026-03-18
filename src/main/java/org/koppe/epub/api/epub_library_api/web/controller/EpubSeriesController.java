package org.koppe.epub.api.epub_library_api.web.controller;

import java.util.Optional;

import org.koppe.epub.api.epub_library_api.exceptions.DuplicateKeyException;
import org.koppe.epub.api.epub_library_api.jpa.model.EpubSeries;
import org.koppe.epub.api.epub_library_api.jpa.service.SeriesService;
import org.koppe.epub.api.epub_library_api.utility.DtoFactory;
import org.koppe.epub.api.epub_library_api.web.dto.EpubSeriesDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(path = "/series")
@RequiredArgsConstructor
@Tag(name = "Epub Series", description = "Provides functionalities for managing epub series")
public class EpubSeriesController {
    private final Logger logger = LoggerFactory.getLogger(EpubSeriesController.class);
    private final SeriesService series;

    // #region add series
    @Operation(summary = "Add epub series", description = "Adds epub series with given name.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully added epub series", content = @Content(schema = @Schema(implementation = EpubSeries.class))),
            @ApiResponse(responseCode = "400", description = "Invalid body", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Series name already taken", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(path = "", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EpubSeriesDto> addSeries(@RequestBody EpubSeriesDto body) {
        if (body == null || body.getName() == null || body.getName().isBlank()) {
            logger.info("Invalid body given");
            return ResponseEntity.of(
                    ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), "Invalid body, name must be given"))
                    .build();
        }

        EpubSeries es = null;

        try {
            es = series.addEpubSeries(body.getName());
            logger.info("Successfully added epub series {}", es);
        } catch (DuplicateKeyException ex) {
            logger.info("Name {} already taken", body.getName());
            return ResponseEntity.of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(403),
                    "Name " + body.getName() + " already taken")).build();
        }

        return ResponseEntity.ok(DtoFactory.convertSeriesToDto(es, false));
    }

    // #region get series
    @Operation(summary = "Get epub series", description = "Get single epub series by id", parameters = {
            @Parameter(in = ParameterIn.PATH, name = "id", description = "Id of the epub series", required = true),
            @Parameter(in = ParameterIn.QUERY, name = "with_epubs", description = "If true, epubs will be returned as well", required = false)
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully fetched epub series", content = @Content(schema = @Schema(implementation = EpubSeriesDto.class))),
            @ApiResponse(responseCode = "404", description = "Series with given id not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EpubSeriesDto> getSeries(@PathVariable(name = "id", required = true) Long id,
            @RequestParam(name = "with_epubs", required = false) Optional<Boolean> withEpubs) {
        if (id == null || !series.existsById(id)) {
            logger.info("Invalid epub series id given");
            return ResponseEntity.of(
                    ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404), "No epub series with given id found"))
                    .build();
        }

        EpubSeries es = series.findById(id);
        logger.info("Returning series {}", es);
        return ResponseEntity.ok(DtoFactory.convertSeriesToDto(es, withEpubs.orElse(false)));
    }

    // #region delete series
    @DeleteMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EpubSeriesDto> deleteById(@PathVariable(name = "id", required = true) Long id) {
        if (id == null || !series.existsById(id)) {
            logger.info("Invalid epub series id given");
            return ResponseEntity.of(
                    ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404), "No epub series with given id found"))
                    .build();
        }

        EpubSeries es = series.delete(id);
        return ResponseEntity.ok(DtoFactory.convertSeriesToDto(es, false));
    }

    // #region update series
    @PatchMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EpubSeriesDto> updateSeries(@PathVariable(name = "id", required = true) Long id,
            @RequestBody EpubSeriesDto body) {
        if (id == null || !series.existsById(id)) {
            logger.info("Invalid epub series id given");
            return ResponseEntity.of(
                    ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404), "No epub series with given id found"))
                    .build();
        }

        if (body == null || body.getName() == null || body.getName().isBlank()) {
            logger.info("Invalid body given");
            return ResponseEntity.of(
                    ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), "Invalid body, name must be given"))
                    .build();
        }

        EpubSeries es = null;
        try {
            es = series.updateSeries(id, body.getName());
        } catch (DuplicateKeyException ex) {
            logger.info("Name {} already taken", body.getName());
            return ResponseEntity.of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(403),
                    "Name " + body.getName() + " already taken")).build();
        }

        return ResponseEntity.ok(DtoFactory.convertSeriesToDto(es, false));
    }
}
