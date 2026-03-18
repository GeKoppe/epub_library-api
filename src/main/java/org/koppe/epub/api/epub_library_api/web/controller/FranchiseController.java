package org.koppe.epub.api.epub_library_api.web.controller;

import java.io.File;
import java.io.IOException;
import java.util.Optional;

import org.koppe.epub.api.epub_library_api.exceptions.DuplicateKeyException;
import org.koppe.epub.api.epub_library_api.exceptions.MediaTypeException;
import org.koppe.epub.api.epub_library_api.jpa.model.Franchise;
import org.koppe.epub.api.epub_library_api.jpa.service.FranchiseService;
import org.koppe.epub.api.epub_library_api.utility.DtoFactory;
import org.koppe.epub.api.epub_library_api.web.dto.FranchiseDto;
import org.koppe.epub.api.epub_library_api.web.dto.IdDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
@RequestMapping(path = "/franchises")
@Tag(name = "Franchises", description = "Provides functionality for managing franchises")
@RequiredArgsConstructor
public class FranchiseController {
    /**
     * Logger
     */
    private final Logger logger = LoggerFactory.getLogger(FranchiseController.class);
    /**
     * Service for working with franchises in the database
     */
    private final FranchiseService franchises;

    // #region add franchise
    @Operation(summary = "Add a franchise", description = "Add a new franchise to the system. Only name is needed")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully added the new franchise", content = @Content(schema = @Schema(implementation = FranchiseDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid franchise definition given", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Given name already taken", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(path = "", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FranchiseDto> addFranchise(@RequestBody FranchiseDto dto) {
        if (dto == null || dto.getName() == null || dto.getName().isBlank()) {
            logger.info("No name given for new franchise");
            return ResponseEntity.of(
                    ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), "No name given for new franchise"))
                    .build();
        }

        Franchise franchise = null;
        try {
            franchise = franchises.addFranchise(dto.getName());
        } catch (DuplicateKeyException ex) {
            logger.info("Name {} already taken", dto.getName());
            return ResponseEntity.of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(403),
                    "Name " + dto.getName() + " already used within the system.")).build();
        }

        return ResponseEntity.ok(DtoFactory.convertFranchiseToDto(franchise, false));
    }

    // #region get franchise
    @Operation(summary = "Add a franchise", description = "Add a new franchise to the system. Only name is needed", parameters = {
            @Parameter(in = ParameterIn.QUERY, name = "with_epubs", description = "If set to true, associated epubs are returned as well. Defaults to false.", required = false),
            @Parameter(in = ParameterIn.PATH, name = "id", description = "Id of the franchise to be returned.", required = true)
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully returned the franchise", content = @Content(schema = @Schema(implementation = FranchiseDto.class))),
            @ApiResponse(responseCode = "404", description = "Franchise with given id not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FranchiseDto> getFranchise(@PathVariable(name = "id", required = true) Long id,
            @RequestParam(name = "with_epubs", required = false) Optional<Boolean> withEpubs) {
        if (id == null || !franchises.existsById(id)) {
            logger.info("Invalid id given");
            return ResponseEntity.of(
                    ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404), "No franchise with given id found"))
                    .build();
        }

        Franchise franchise = franchises.findById(id);
        return ResponseEntity.ok(DtoFactory.convertFranchiseToDto(franchise, withEpubs.orElse(false)));
    }

    // #region delete franchise
    @DeleteMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FranchiseDto> deleteFranchise(@PathVariable(name = "id", required = true) Long id) {
        if (id == null || !franchises.existsById(id)) {
            logger.info("Invalid id given");
            return ResponseEntity.of(
                    ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404), "No franchise with given id found"))
                    .build();
        }

        Franchise franchise = franchises.deleteFranchise(id);
        return ResponseEntity.ok(DtoFactory.convertFranchiseToDto(franchise, false));
    }

    // #region add epub
    @PutMapping(path = "/{id}/epubs", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<FranchiseDto> addEpub(@PathVariable(name = "id", required = true) Long id,
            @RequestBody IdDto dto) {
        if (id == null || !franchises.existsById(id)) {
            logger.info("Invalid id given");
            return ResponseEntity.of(
                    ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404), "No franchise with given id found"))
                    .build();
        }

        if (dto == null || dto.getId() == null) {
            logger.info("Invalid epub id given");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), "Invalid epub id given")).build();
        }

        try {
            franchises.addEpubToFranchise(id, dto.getId());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
                    "Invalid epub id given, epub does not exist")).build();
        }

        Franchise updated = franchises.findById(id);

        return ResponseEntity.ok(DtoFactory.convertFranchiseToDto(updated, true));
    }

    @PostMapping(path = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> uploadPicture(@PathVariable(name = "id", required = true) Long id,
            @RequestPart("file") MultipartFile file,
            @RequestParam(name = "overwrite", required = false) Optional<Boolean> overwrite) {

        if (id == null || !franchises.existsById(id)) {
            logger.info("Invalid id given");
            return ResponseEntity.of(
                    ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404), "No franchise with given id found"))
                    .build();
        }

        if (file == null || file.isEmpty() || file.getSize() == 0) {
            logger.info("No file given");
            return ResponseEntity.of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), "No image given"))
                    .build();
        }

        try {
            franchises.uploadImage(id, file, overwrite.orElse(false));
        } catch (MediaTypeException ex) {
            logger.warn("Invalid media type given");
            return ResponseEntity.of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(403),
                    "Required mediatype: " + ex.getAllowed() + ", received mediatype: "
                            + ex.getProvided().getType()))
                    .build();
        } catch (IOException ex) {
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(500), "Could not save the image"))
                    .build();
        } catch (IllegalStateException ex) {
            return ResponseEntity.of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
                    "Franchise already has an image and overwrite is not set to true")).build();
        }

        return ResponseEntity.noContent().build();
    }

    // #region download image
    @GetMapping(path = "/{id}/image", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> downloadImage(@PathVariable(name = "id", required = true) Long id) {
        if (id == null || !franchises.existsById(id)) {
            logger.info("Invalid id given");
            return ResponseEntity.of(
                    ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404), "No franchise with given id found"))
                    .build();
        }

        File file = null;
        try {
            file = franchises.downloadImage(id);
        } catch (IOException e) {
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), "No cover image uplaoded yet"))
                    .build();
        }

        if (file == null) {
            return ResponseEntity.noContent().build();
        }

        Resource res = new FileSystemResource(file.toPath());
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" +
                file.getName()
                + "\"").body(res);
    }
}
