package org.koppe.epub.api.epub_library_api.web.controller;

import java.util.Optional;

import org.koppe.epub.api.epub_library_api.exceptions.DuplicateKeyException;
import org.koppe.epub.api.epub_library_api.jpa.model.Tag;
import org.koppe.epub.api.epub_library_api.jpa.service.TagService;
import org.koppe.epub.api.epub_library_api.utility.DtoFactory;
import org.koppe.epub.api.epub_library_api.web.dto.IdDto;
import org.koppe.epub.api.epub_library_api.web.dto.TagDto;
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
import org.springframework.web.bind.annotation.PutMapping;
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
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping(path = "/tags")
@io.swagger.v3.oas.annotations.tags.Tag(name = "Tags", description = "Functionality for managing tags")
@RequiredArgsConstructor
public class TagController {
    /**
     * Logger
     */
    private final Logger logger = LoggerFactory.getLogger(TagController.class);
    /**
     * Service for working with tags
     */
    private final TagService tags;

    // #region add tag
    @Operation(summary = "Add tag", description = "Adds a new tag to the system")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully added tag", content = @Content(schema = @Schema(implementation = TagDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid body given", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "Name for new tag already taken", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PostMapping(path = "", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TagDto> addTag(@RequestBody TagDto dto) {
        if (dto == null || dto.getName() == null || dto.getName().isBlank()) {
            logger.info("Invalid body given");
            return ResponseEntity.of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
                    "Invalid dto given, at least name must be filled")).build();
        }
        logger.info("Adding tag {}", dto);

        Tag tag = null;
        try {
            tag = tags.addTag(dto.getName(), dto.getColour());
            logger.info("Tag {} successfully added", tag);
        } catch (DuplicateKeyException ex) {
            logger.info("Tag name {} already taken, cannot add it", dto.getName());
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(403), "Tag name already taken"))
                    .build();
        }

        TagDto response = DtoFactory.convertTagToDto(tag, false);
        return ResponseEntity.ok(response);
    }

    // #region get tag by id
    @Operation(summary = "Get tag by id", description = "Returns tag with given id. If with_epubs is set to tru, books the tag is associated with are returned as well.", parameters = {
            @Parameter(in = ParameterIn.PATH, name = "id", description = "Id of the tag.", required = true),
            @Parameter(in = ParameterIn.QUERY, name = "with_epubs", description = "If true, epubs the tag is associated with are returned as well. Defaults to false.", required = false)
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully fetched tag", content = @Content(schema = @Schema(implementation = TagDto.class))),
            @ApiResponse(responseCode = "404", description = "Tag not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TagDto> getTag(@PathVariable(name = "id", required = true) Long id,
            @RequestParam(name = "with_epubs", required = false) Optional<Boolean> withEpubs) {
        if (id == null || !tags.existsById(id)) {
            logger.info("No id with given tag exists");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404), "No tag with given id found"))
                    .build();
        }

        Tag tag = tags.findById(id);
        return ResponseEntity.ok(DtoFactory.convertTagToDto(tag, withEpubs.orElse(false)));
    }

    // #region update tag
    @PatchMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TagDto> updateTag(@PathVariable(name = "id", required = true) Long id,
            @RequestBody TagDto dto,
            @RequestParam(name = "overwrite_nulls", required = false) Optional<Boolean> overwriteNulls) {

        if (id == null || !tags.existsById(id)) {
            logger.info("Invalid tag id given");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404), "Tag with given id not found"))
                    .build();
        }

        if (dto == null) {
            logger.info("No tag definition given");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), "No tag definition given"))
                    .build();
        }

        Tag tag = null;
        try {
            tag = tags.updateTag(id, dto.getName(), dto.getColour(), overwriteNulls.orElse(false));
        } catch (DuplicateKeyException ex) {
            logger.info("Name {} already taken", dto.getName());
            return ResponseEntity.of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(403),
                    "Given name already used by another tag")).build();
        }

        logger.info("Updated tag {}", tag);
        return ResponseEntity.ok(DtoFactory.convertTagToDto(tag, false));
    }

    // #region delete tag
    @Operation(summary = "Delete a tag", description = "Deletes tag with given id", parameters = {
            @Parameter(in = ParameterIn.PATH, name = "id", description = "Id of the tag to be deleted", required = true)
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully deleted tag", content = @Content(schema = @Schema(implementation = TagDto.class))),
            @ApiResponse(responseCode = "404", description = "Tag not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TagDto> deleteTag(@PathVariable(name = "id", required = true) Long id) {
        if (id == null || !tags.existsById(id)) {
            logger.info("No tag with given id exists");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404), "No tag with given id exists"))
                    .build();
        }

        logger.info("Deleting tag with id {}", id);

        Tag deleted = tags.deleteById(id);
        logger.info("Deleted tag {}", deleted);

        return ResponseEntity.ok(DtoFactory.convertTagToDto(deleted, false));
    }

    // #region add epub
    @Operation(summary = "Add epub to tag", description = "Adds epub with given id to tag with given id", parameters = {
            @Parameter(in = ParameterIn.PATH, name = "tag-id", description = "Id of the tag to add the epub to", required = true),
            @Parameter(in = ParameterIn.PATH, name = "epub-id", description = "Id of the epub to add to the tag", required = true)
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully added epub to tag", content = @Content(schema = @Schema(implementation = TagDto.class))),
            @ApiResponse(responseCode = "204", description = "Tag not associated with epub", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "400", description = "Epub with given id does not exist", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Tag with given id not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping(path = "/{id}/epubs", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TagDto> addEpubToTag(@PathVariable(name = "id", required = true) Long id,
            @RequestBody IdDto dto) {
        if (id == null || !tags.existsById(id)) {
            logger.info("No tag with given id exists");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404), "No tag with given id exists"))
                    .build();
        }

        if (dto == null || dto.getId() == null) {
            logger.info("No epub id given");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), "No epub id given"))
                    .build();
        }

        Tag tag = null;
        try {
            tag = tags.addEpubToTag(id, dto.getId());
        } catch (IllegalArgumentException ex) {
            logger.info("Invalid epub id given");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), "Invalid epub id given"))
                    .build();
        }

        if (tag == null) {
            logger.info("Tag already associated with epub");
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(DtoFactory.convertTagToDto(tag, true));
    }

    // #region remove epub
    @Operation(summary = "Remove epub from tag", description = "Removes epub with given id from tag with given id", parameters = {
            @Parameter(in = ParameterIn.PATH, name = "tag-id", description = "Id of the tag to remove the epub from", required = true),
            @Parameter(in = ParameterIn.PATH, name = "epub-id", description = "Id of the epub to remove from the tag", required = true)
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully removed epub from tag", content = @Content(schema = @Schema(implementation = TagDto.class))),
            @ApiResponse(responseCode = "204", description = "Tag not associated with epub", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "400", description = "Epub with given id does not exist", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Tag with given id not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping(path = "/{tag-id}/epubs/{epub-id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TagDto> removeEpubFromTag(@PathVariable(name = "tag-id", required = true) Long tagId,
            @PathVariable(name = "epub-id", required = true) Long epubId) {
        if (tagId == null || !tags.existsById(tagId)) {
            logger.info("No tag with given id exists");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404), "No tag with given id exists"))
                    .build();
        }

        Tag tag = null;
        try {
            tag = tags.removeEpubFromTag(tagId, epubId);
        } catch (IllegalArgumentException ex) {
            logger.info("Invalid epub id given");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), "Invalid epub id given"))
                    .build();
        }

        if (tag == null) {
            logger.info("Tag wasn't associated with epub");
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(DtoFactory.convertTagToDto(tag, true));
    }
}
