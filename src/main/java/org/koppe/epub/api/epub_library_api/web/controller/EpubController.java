package org.koppe.epub.api.epub_library_api.web.controller;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import org.koppe.epub.api.epub_library_api.exceptions.MediaTypeException;
import org.koppe.epub.api.epub_library_api.jpa.model.Author;
import org.koppe.epub.api.epub_library_api.jpa.model.Epub;
import org.koppe.epub.api.epub_library_api.jpa.model.EpubEdition;
import org.koppe.epub.api.epub_library_api.jpa.model.Genre;
import org.koppe.epub.api.epub_library_api.jpa.service.AuthorService;
import org.koppe.epub.api.epub_library_api.jpa.service.EpubService;
import org.koppe.epub.api.epub_library_api.utility.DtoFactory;
import org.koppe.epub.api.epub_library_api.web.dto.AuthorDto;
import org.koppe.epub.api.epub_library_api.web.dto.EpubDto;
import org.koppe.epub.api.epub_library_api.web.dto.EpubEditionDto;
import org.koppe.epub.api.epub_library_api.web.dto.GenreDto;
import org.koppe.epub.api.epub_library_api.web.dto.IdDto;
import org.koppe.epub.api.epub_library_api.web.dto.PageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
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
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * Rest controller for epubs
 */
@RequestMapping(path = "/epubs")
@RestController
@RequiredArgsConstructor
@Tag(name = "Epubs", description = "Provides functionalities for managing ebooks.")
public class EpubController {
    /**
     * Logger
     */
    private final Logger logger = LoggerFactory.getLogger(EpubController.class);
    /**
     * Service for epubs
     */
    private final EpubService srv;
    private final AuthorService authorSrv;

    // #region add book
    /**
     * Adds an epub to the database
     * 
     * @param dto Data for the dto to be added
     * @return Added epub
     */
    @Operation(summary = "Add an epub", description = """
            Adds an epub to the database.
            Requires at least the title of the epub.
            """)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully added epub", content = @Content(schema = @Schema(implementation = EpubDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid body given", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @Order(2)
    @PostMapping(path = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EpubDto> addBook(@RequestBody EpubDto dto) {
        if (dto == null || dto.getTitle() == null || dto.getTitle().isBlank()) {
            logger.info("Invalid epub dto {} given", dto);
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
                            "Invalid epub object given"))
                    .build();
        }

        Epub epub = new Epub();
        epub.setTitle(dto.getTitle());
        epub.setUploadDate(LocalDate.now());
        epub.setPublishingDate(dto.getPublishDate());

        Epub added = srv.addBook(epub);
        logger.info("Added book {}", added);

        return ResponseEntity.ok(DtoFactory.convertEpubToDto(added, false, false, false, false, false, false));
    }

    // #region get all epubs
    /**
     * Gets all epubs specified by the search query
     * 
     * @param titleContains   Filters for epubs with titles containing this string
     * @param publishedBefore Filters for epubs published before this date
     * @param publishedAfter  Filters for epubs published after this date
     * @param uploadedBefore  Filters for epubs uploaded before this date
     * @param uploadedAfter   Filters for epubs uploaded after this date
     * @param genres          Filters for epubs associated with at least one of the
     *                        given genres
     * @param authors         Filters for epubs associated with at least one of the
     *                        given authors
     * @param page            Specifies the result page to be returned
     * @param pageSize        Specifies the page size
     * @param withAuthors     If true, authors will be returned as well
     * @param withGenres      If true, genres will be returned as well
     * @param withToc         If true, table of contents will be returned as well
     * @return Requested page of all the found epubs
     */
    @Operation(summary = "Get all epubs matching specifications", description = """
            Returns all epubs matching the given query specifications.
            If no specifications are given, first 1000 epubs are returned.
            """, parameters = {
            @Parameter(in = ParameterIn.QUERY, name = "title_contains", description = "Filters for ePubs that have names containing the given string.", required = false),
            @Parameter(in = ParameterIn.QUERY, name = "published_before", description = "Filters for ePubs that were published before the given date. Should be used mutually exclusive with published_after.", required = false),
            @Parameter(in = ParameterIn.QUERY, name = "published_after", description = "Filters for ePubs that were published after the given date. Should be used mutually exclusive with published_before.", required = false),
            @Parameter(in = ParameterIn.QUERY, name = "uploaded_after", description = "Filters for ePubs that were uploaded after the given date. Should be used mutually exclusive with uploaded_before.", required = false),
            @Parameter(in = ParameterIn.QUERY, name = "uploaded_before", description = "Filters for ePubs that were uploaded before the given date. Should be used mutually exclusive with uploaded_after.", required = false),
            @Parameter(in = ParameterIn.QUERY, name = "genres", description = "Filters for ePubs that are associated with at least one of the given genres.", required = false),
            @Parameter(in = ParameterIn.QUERY, name = "authors", description = "Filters for ePubs that are associated with at least one of the given authors.", required = false),
            @Parameter(in = ParameterIn.QUERY, name = "with_genres", description = "If true, genres of the epub will be supplied as well. Defaults to false.", required = false),
            @Parameter(in = ParameterIn.QUERY, name = "with_authors", description = "If true, authors of the epub will be supplied as well. Defaults to false.", required = false),
            @Parameter(in = ParameterIn.QUERY, name = "with_series", description = "If true, series of the epub will be supplied as well. Defaults to false.", required = false),
            @Parameter(in = ParameterIn.QUERY, name = "with_tags", description = "If true, tags of the epub will be supplied as well. Defaults to false.", required = false),
            @Parameter(in = ParameterIn.QUERY, name = "with_toc", description = "If true, table of content for the ePubs will be supplied as well. Defaults to false", required = false),
            @Parameter(in = ParameterIn.QUERY, name = "page", description = "0-indexed result page to be returned. Defaults to 0.", required = false),
            @Parameter(in = ParameterIn.QUERY, name = "page_size", description = "Size of the result pages to be returned. Defaults to 1000", required = false)
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully retrieved all specified ePub dtos.", content = @Content(schema = @Schema(implementation = PageDto.class)))
    })
    @Order(1)
    @GetMapping(path = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PageDto<EpubDto>> getAllEpubs(
            @RequestParam(name = "title_contains", required = false) Optional<String> titleContains,
            @RequestParam(name = "published_before", required = false) Optional<LocalDate> publishedBefore,
            @RequestParam(name = "published_after", required = false) Optional<LocalDate> publishedAfter,
            @RequestParam(name = "uploaded_before", required = false) Optional<LocalDate> uploadedBefore,
            @RequestParam(name = "uploaded_after", required = false) Optional<LocalDate> uploadedAfter,
            @RequestParam(name = "genres", required = false) Optional<List<GenreDto>> genres,
            @RequestParam(name = "authors", required = false) Optional<List<AuthorDto>> authors,
            @RequestParam(name = "page", required = false) Optional<Integer> page,
            @RequestParam(name = "page_size", required = false) Optional<Integer> pageSize,
            @RequestParam(name = "with_authors", required = false) Optional<Boolean> withAuthors,
            @RequestParam(name = "with_genres", required = false) Optional<Boolean> withGenres,
            @RequestParam(name = "with_toc", required = false) Optional<Boolean> withToc,
            @RequestParam(name = "with_series", required = false) Optional<Boolean> withSeries,
            @RequestParam(name = "with_tags", required = false) Optional<Boolean> withTags) {

        List<GenreDto> gDtoList = genres.orElse(null);
        List<AuthorDto> aDtoList = authors.orElse(null);
        List<Genre> genreList = new ArrayList<>();
        List<Author> authorList = new ArrayList<>();

        if (gDtoList != null) {
            gDtoList.forEach(g -> genreList
                    .add(new Genre(g.getId(), g.getName(), g.getDescription(), null, null, null,
                            new HashSet<>())));
        }

        if (aDtoList != null) {
            aDtoList.forEach(a -> authorList.add(new Author(a.getId(), a.getFirstName(), a.getSurname(),
                    a.getDescription(), a.getBirthDate(), a.getDeathDate(), null, null)));
        }

        Page<Epub> found = srv.findAllEpubs(titleContains.orElse(null), publishedBefore.orElse(null),
                publishedAfter.orElse(null), uploadedBefore.orElse(null), uploadedAfter.orElse(null),
                genreList,
                authorList, page.orElse(0), pageSize.orElse(1000));

        List<Epub> epubs = found.getContent();
        List<EpubDto> dtos = new ArrayList<>();
        for (var x : DtoFactory.convertMultipleEpubsToDto(new HashSet<>(epubs), withGenres.orElse(false),
                withAuthors.orElse(false), true, withToc.orElse(false), withSeries.orElse(false),
                withTags.orElse(false))) {
            dtos.add(x);
        }

        PageDto<EpubDto> pageDto = new PageDto<>();
        pageDto.setContent(dtos);
        pageDto.setItemCount(found.getTotalElements());
        pageDto.setNumber(found.getNumber());
        pageDto.setPageSize(found.getSize());

        return ResponseEntity.ok(pageDto);
    }

    // #region get epub
    @Operation(summary = "Get single epub", description = """
            Returns the epub with given ids.
            Query parameters are used to determine, whether authors, genres, editions and metadata are also to be returned.
            """, parameters = {
            @Parameter(in = ParameterIn.PATH, name = "id", description = "Id of the epub to be returned.", required = true),
            @Parameter(in = ParameterIn.QUERY, name = "with_authors", description = "If true, authors of the epubs will be returned as well. Defaults to false.", required = false),
            @Parameter(in = ParameterIn.QUERY, name = "with_genres", description = "If true, genres of the epubs will be returned as well. Defaults to false.", required = false),
            @Parameter(in = ParameterIn.QUERY, name = "with_editions", description = "If true, editions of the epubs will be returned as well. Defaults to false.", required = false),
            @Parameter(in = ParameterIn.QUERY, name = "with_toc", description = "If true, table of contents of the epub editions will be returned as well. If with_editions is set to false, this parameter is irrelevant. Defaults to false.", required = false),
            @Parameter(in = ParameterIn.QUERY, name = "with_series", description = "If true, series of the epub will be supplied as well. Defaults to false.", required = false),
            @Parameter(in = ParameterIn.QUERY, name = "with_tags", description = "If true, tags of the epub will be supplied as well. Defaults to false.", required = false)
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully returned the epub", content = @Content(schema = @Schema(implementation = EpubDto.class))),
            @ApiResponse(responseCode = "404", description = "Epub with given id does not exist", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @Order(3)
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EpubDto> getBook(@PathVariable(name = "id", required = true) Long id,
            @RequestParam(name = "with_authors", required = false) Optional<Boolean> withAuthors,
            @RequestParam(name = "with_genres", required = false) Optional<Boolean> withGenres,
            @RequestParam(name = "with_editions", required = false) Optional<Boolean> withEditions,
            @RequestParam(name = "with_toc", required = false) Optional<Boolean> withToc,
            @RequestParam(name = "with_series", required = false) Optional<Boolean> withSeries,
            @RequestParam(name = "with_tags", required = false) Optional<Boolean> withTags) {

        if (!srv.existsById(id)) {
            logger.info("No book with id {} exists", id);
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404),
                            "No book for given id exists"))
                    .build();
        }

        Epub epub = srv.findEpubById(id);
        if (epub == null) {
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404),
                            "No book for given id exists"))
                    .build();
        }

        EpubDto dto = DtoFactory.convertEpubToDto(epub, withGenres.orElse(false), withAuthors.orElse(false),
                withEditions.orElse(false), withToc.orElse(false), withSeries.orElse(false),
                withTags.orElse(false));
        return ResponseEntity.ok(dto);
    }

    // #region update epub
    /**
     * Updates epub with given values
     * 
     * @param overwriteNulls If true, null values might be added to the database
     * @param epub           Values to be updated
     * @param id             Id of the epub to be updated
     * @return Updated epub
     */
    @Operation(summary = "Update epub", description = """
            Updates epub with given id with the values in the request body.
            Id of the epub cannot be overwritten.
            If id in body is given, it must match the id in the path.
            If overwrite_nulls is set to true, values not given in the body will be set as null in the database.
            """, parameters = {
            @Parameter(in = ParameterIn.QUERY, name = "overwrite_nulls", description = "If true, missing values in the body will overwrite database values as null. Defaults to false", required = false),
            @Parameter(in = ParameterIn.PATH, name = "id", description = "Id of the epub to be updated.", required = true)
    }, requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true, description = "Updated values for the epub"))
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Updated epub successfully", content = @Content(schema = @Schema(implementation = EpubDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid body given or id in path does not match id in body", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "No epub for given id found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @Order(4)
    @PatchMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EpubDto> updateEpub(
            @RequestParam(name = "overwrite_nulls", required = false) Optional<Boolean> overwriteNulls,
            @RequestBody EpubDto epub,
            @PathVariable(name = "id") Long id) {

        if (id == null || !srv.existsById(id)) {
            logger.info("No epub with id {} exists", id);
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404),
                            "No epub with given id exists"))
                    .build();
        }

        if (epub.getId() != null && !epub.getId().equals(id)) {
            logger.info("Id in body does not match id in path");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
                            "Id in body does not match id in path"))
                    .build();
        }

        Epub updated = new Epub();
        updated.setPublishingDate(epub.getPublishDate());
        updated.setTitle(updated.getTitle());

        Epub saved = null;
        try {
            saved = srv.updateEpub(id, saved, overwriteNulls.orElse(false));
            logger.info("Updated epub {}", saved);
        } catch (IllegalArgumentException ex) {
            logger.info("Exception occurred while updating epub, assuming invalid body", ex);
            ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
                            "Invalid body given: " + ex.getMessage()))
                    .build();
        }

        EpubDto toReturn = DtoFactory.convertEpubToDto(saved, false, false, false, false, false, false);
        return ResponseEntity.ok(toReturn);
    }

    // #region add author to epub
    @Operation(summary = "Add an author to an epub", description = "Adds author with id given in body to the epub defined by the path.", parameters = {
            @Parameter(in = ParameterIn.PATH, name = "id", description = "Id of the epub to add the author to.", required = true)
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully added author to epub.", content = @Content(schema = @Schema(implementation = EpubDto.class))),
            @ApiResponse(responseCode = "204", description = "Author already associated with the given epub."),
            @ApiResponse(responseCode = "400", description = "Invalid author id given", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Epub with given id not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping(path = "/{id}/authors", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EpubDto> addAuthorToEpub(@PathVariable(name = "id") Long id,
            @RequestBody IdDto author) {
        if (id == null || !srv.existsById(id)) {
            logger.info("Invalid epub id {}", id);
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404),
                            "No epub with given id exists"))
                    .build();
        }
        if (author == null || author.getId() == null || !authorSrv.existsById(author.getId())) {
            logger.info("No valid author given");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
                            "Invalid author id given"))
                    .build();
        }

        Author updated = srv.addAuthor(id, author.getId());
        if (updated == null) {
            logger.info("Epub {} already associated with author {}", id, author.getId());
            return ResponseEntity.noContent().build();
        }
        Epub epub = srv.findEpubById(id);

        EpubDto dto = DtoFactory.convertEpubToDto(epub, false, true, false, false, false, false);

        return ResponseEntity.ok(dto);
    }

    // #region remove author
    @DeleteMapping(path = "/{epub-id}/authors/{author-id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EpubDto> removeAuthor(@PathVariable(name = "epub-id", required = true) Long epubId,
            @PathVariable(name = "author-id") Long authorId) {

        if (epubId == null || !srv.existsById(epubId)) {
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404),
                            "No epub with given id found"))
                    .build();
        }

        Author author = null;
        try {
            logger.info("Deleting author {} from epub {}", authorId, epubId);
            author = srv.removeAuthor(epubId, authorId);
        } catch (IllegalArgumentException ex) {
            logger.info("Author with given id does not exist");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
                            "Invalid author id given"))
                    .build();
        }

        if (author == null) {
            logger.info("Author {} has never been associated with epub {}", authorId, epubId);
            return ResponseEntity.noContent().build();
        }
        Epub epub = srv.findEpubById(epubId);

        EpubDto dto = DtoFactory.convertEpubToDto(epub, false, true, false, false, false, false);
        return ResponseEntity.ok(dto);
    }

    // #region delete epub
    /**
     * Deletes the epub and all it's editions
     * 
     * @param id Id of the epub to be deleted
     * @return The deleted epub
     */
    @Operation(summary = "Delete epub", description = """
            Deletes epub and all it's editions and metadata.
            """, parameters = {
            @Parameter(in = ParameterIn.PATH, name = "id", description = "Id of the epub to be deleted") })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully deleted epub", content = @Content(schema = @Schema(implementation = EpubDto.class))),
            @ApiResponse(responseCode = "404", description = "Epub with given id not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @Order(5)
    @DeleteMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EpubDto> deleteEpub(@PathVariable(name = "id") Long id) {
        if (id == null || !srv.existsById(id)) {
            logger.info("No valid epub id given");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404),
                            "No epub with given id found"))
                    .build();
        }

        logger.info("Deleting epub with id {}", id);
        Epub deleted = srv.deleteEpub(id);
        logger.info("Successfully deleted epub {}", deleted);

        EpubDto dto = DtoFactory.convertEpubToDto(deleted, false, false, true, true, false, false);
        return ResponseEntity.ok(dto);
    }

    // #region editions

    // #region add edition
    @Operation(summary = "Adds an edition to an epub", description = """
            Adds a new edition to a given epub.
            Only adds the edition itself, metadata tab won't be added.
            Only needs the version name to work.
            """, parameters = {
            @Parameter(in = ParameterIn.PATH, name = "id", description = "Id of the epub to add the edition to") })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully added", content = @Content(schema = @Schema(implementation = EpubEditionDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid body", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Invalid body", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @Order(6)
    @PostMapping(path = "/{id}/editions", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EpubEditionDto> addEpubEdition(@PathVariable(name = "id") Long id,
            @RequestBody EpubEditionDto dto) {
        if (id == null || !srv.existsById(id)) {
            logger.info("Epub id {} does not exist", id);
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404),
                            "Invalid epub id given"))
                    .build();
        }

        if (dto == null || dto.getVersionName() == null || dto.getVersionName().isBlank()) {
            logger.info("Invalid epub edition given");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
                            "Invalid edition given"))
                    .build();
        }

        EpubEdition edition = new EpubEdition();
        edition.setVersionName(dto.getVersionName());

        EpubEdition saved = srv.addEdition(id, edition);
        return ResponseEntity.ok(DtoFactory.convertEditionToDto(saved, false));
    }

    // #region upload
    @Operation(summary = "Upload an eBook", description = """
            Uploads an eBook.
            Needs the upload guid of an existing ebBook to correctly match the eBook.
            After upload is completed, the eBook will be analysed and the table of contents and metadata are saved in the database.
            """, parameters = {
            @Parameter(in = ParameterIn.QUERY, name = "upload-guid", description = "Upload guid of an existing ebook edition.") })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Upload successful"),
            @ApiResponse(responseCode = "400", description = "Invalid upload guid or file", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @Order(8)
    @PostMapping(path = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Void> upload(@RequestParam(name = "upload-guid") String uploadGuid,
            @RequestPart("file") MultipartFile file) {

        if (file == null || file.getSize() == 0) {
            logger.info("No file given");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
                            "No valid file given"))
                    .build();
        }

        if (uploadGuid == null) {
            logger.info("No upload guid given");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
                            "No upload guid given"))
                    .build();
        }

        EpubEdition edition = srv.findEditionByUploadGuid(uploadGuid);
        if (edition == null) {
            logger.info("Upload guid {} is invalid", uploadGuid);
            return ResponseEntity.of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404),
                    "No epub for given upload guid found")).build();
        }

        boolean result;
        try {
            result = srv.upload(edition, file);
        } catch (MediaTypeException ex) {
            logger.warn("Invalid media type given");
            return ResponseEntity.of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(403),
                    "Required mediatype: " + ex.getAllowed() + ", received mediatype: "
                            + ex.getProvided().getType()))
                    .build();
        } catch (Exception ex) {
            logger.warn("Exception occurred while uploading the file");
            return ResponseEntity.internalServerError().build();
        }

        if (result)
            return ResponseEntity.noContent().build();
        else
            return ResponseEntity.internalServerError().build();
    }

    // #region download
    @Operation(summary = "Download ePub or cover picture", description = """
            Downloads the ePub or cover picture of the ebook related to the given download guid.
            If the request parameter "cover" is set to true, the cover picture will be downloaded instead of the ePub.
            """, parameters = {
            @Parameter(in = ParameterIn.QUERY, name = "cover", description = "If set to true, cover picture will be downloaded instead of the ePub. Defaults to false", required = false),
            @Parameter(in = ParameterIn.QUERY, name = "download-guid", description = "Guid of the ePub to be downloaded.", required = true)
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Download successful", content = @Content(schema = @Schema(implementation = StreamingResponseBody.class))),
            @ApiResponse(responseCode = "400", description = "Missing or invalid download guid", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Preparation of download failed on the server side", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @Order(7)
    @GetMapping(path = "/download", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<Resource> download(@RequestParam(name = "cover") Optional<Boolean> cover,
            @RequestParam(name = "download-guid") String downloadGuid) {

        if (downloadGuid == null) {
            logger.info("No download guid given");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
                            "No download guid given"))
                    .build();
        }

        EpubEdition edition = srv.findEditionByDownloadGuid(downloadGuid);
        if (edition == null) {
            logger.info("Invalid download guid given");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
                            "Invalid download guid given"))
                    .build();
        }

        File file = null;
        try {
            if (!cover.orElse(false))
                file = srv.getEpubForEdition(edition);
            else
                file = srv.getCoverForEdition(edition);
        } catch (IOException ex) {
            logger.warn("Book could not be found for download guid {}", downloadGuid);
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(500),
                            "Book could not be found"))
                    .build();
        }

        if (file == null || !file.exists()) {
            logger.warn("Book could not be found for download guid {}", downloadGuid);
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(500),
                            "Book could not be found"))
                    .build();
        }
        Path path = file.toPath();

        Resource resource = new FileSystemResource(path);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\""
                                + (cover.orElse(false) ? file.getName()
                                        : edition.getOriginalFileName())
                                + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM).body(resource);
    }

    // #region update edition
    @Operation(summary = "Update edition", description = """
            Updates version name of given epub edition.
            """, operationId = "1")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated edition", content = @Content(schema = @Schema(implementation = EpubEditionDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid or no title given", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Epub edition does not exist for epub", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @Order(9)
    @PatchMapping(path = "/{epub-id}/editions/{edition-id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EpubEditionDto> updateEdition(@PathVariable(name = "epub-id") Long epubId,
            @PathVariable(name = "edition-id") Long editionId, @RequestBody EpubEditionDto dto) {

        if (epubId == null || editionId == null || !srv.editionExistsByid(epubId, editionId)) {
            logger.info("No edition with given id found for epub with given id");
            return ResponseEntity.of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404),
                    "No edition with given id found for epub with given id")).build();
        }

        if (dto.getVersionName() == null || dto.getVersionName().isBlank()) {
            logger.info("No valid version name given");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
                            "No valid version name given"))
                    .build();
        }

        EpubEdition saved = srv.updateEditionName(epubId, editionId, dto.getVersionName());
        logger.info("Updated epub {}", saved);

        EpubEditionDto updated = DtoFactory.convertEditionToDto(saved, false);

        return ResponseEntity.ok(updated);
    }

    // #region delete edition
    /**
     * Deletes given edition
     * 
     * @param epubId    Id of the epub the edition belongs to
     * @param editionId Id of the edition to be deleted
     * @return The deleted edition
     */
    @Operation(summary = "Delete edition", description = "Deletes the edition and the files in the system directory", parameters = {
            @Parameter(in = ParameterIn.PATH, name = "epub-id", description = "Id of the epub the edition belongs to. Used for verification purposes."),
            @Parameter(in = ParameterIn.PATH, name = "edition-id", description = "Id of the edition to be deleted") })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Deleted edition successfully", content = @Content(schema = @Schema(implementation = EpubEditionDto.class))),
            @ApiResponse(responseCode = "404", description = "Edition for epub does not exist", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping(path = "/{epub-id}/editions/{edition-id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EpubEditionDto> deleteEdition(@PathVariable(name = "epub-id") Long epubId,
            @PathVariable(name = "edition-id") Long editionId) {

        if (epubId == null || editionId == null || !srv.editionExistsByid(epubId, editionId)) {
            logger.info("No edition with given id found for epub with given id");
            return ResponseEntity.of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404),
                    "No edition with given id found for epub with given id")).build();
        }
        EpubEdition deleted = srv.deleteEdition(epubId, editionId);
        logger.info("Deleted edition {}", deleted);

        EpubEditionDto dto = DtoFactory.convertEditionToDto(deleted, true);

        return ResponseEntity.ok(dto);
    }

    // #region add genre
    @Operation(summary = "Add genre to epub", description = "Adds genre to epub. Only needs id property in body")
    @PutMapping(path = "/{id}/genres", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EpubDto> addGenreToEpub(@PathVariable(name = "id", required = true) Long id,
            @RequestBody IdDto dto) {

        if (id == null || !srv.existsById(id)) {
            logger.info("Invalid epub id given");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404),
                            "No epub for given id exists"))
                    .build();
        }

        if (dto == null || dto.getId() == null) {
            logger.info("No genre id given");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
                            "Invalid genre given"))
                    .build();
        }

        Epub epub = null;
        try {
            epub = srv.addGenre(id, dto.getId());
            logger.info("Added genre to epub");
        } catch (IllegalArgumentException ex) {
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
                            "Invalid genre id given"))
                    .build();
        }

        if (epub == null) {
            logger.info("Epub already associated with genre");
            return ResponseEntity.noContent().build();
        }

        EpubDto toReturn = DtoFactory.convertEpubToDto(epub, true, false, false, false, false, false);
        return ResponseEntity.ok(toReturn);
    }

    // #region remove genre from epub
    @Operation(summary = "Remove genre from epub", description = "Removes given genre from given epub..", parameters = {
            @Parameter(in = ParameterIn.PATH, name = "epub-id", description = "Id of the epub to remove the genre from.", required = true),
            @Parameter(in = ParameterIn.PATH, name = "genre-id", description = "Id of the genre to remove from the epub.", required = true)
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully remove genre from epub.", content = @Content(schema = @Schema(implementation = EpubDto.class))),
            @ApiResponse(responseCode = "204", description = "Genre has never been associated with the given epub."),
            @ApiResponse(responseCode = "400", description = "Invalid genre id given", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Epub with given id not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping(path = "/{epub-id}/genres/{genre-id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EpubDto> deleteGenre(@PathVariable(name = "epub-id", required = true) Long epubId,
            @PathVariable(name = "genre-id", required = true) Long genreId) {

        if (epubId == null || !srv.existsById(epubId)) {
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404),
                            "No epub with given id found"))
                    .build();
        }

        Epub epub = null;
        try {
            logger.info("Deletign genre {} from epub {}", genreId, epubId);
            epub = srv.deleteGenre(epubId, genreId);
        } catch (IllegalArgumentException ex) {
            logger.info("Genre with given id does not exist");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
                            "Invalid genre id given"))
                    .build();
        }

        if (epub == null) {
            logger.info("Genre {} has never been associated with epub {}", genreId, epubId);
            return ResponseEntity.noContent().build();
        }

        EpubDto dto = DtoFactory.convertEpubToDto(epub, true, false, false, false, false, false);
        return ResponseEntity.ok(dto);
    }

    // #region add tag
    @Operation(summary = "Add tag to epub", description = "Adds tag with id in body to epub with id in path", parameters = {
            @Parameter(in = ParameterIn.PATH, name = "id", description = "Id of the epub to add the tag to", required = true)
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully added tag", content = @Content(schema = @Schema(implementation = EpubDto.class))),
            @ApiResponse(responseCode = "204", description = "Tag already associated with epub"),
            @ApiResponse(responseCode = "400", description = "Invalid tag id given", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Epub with given id not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping(path = "/{id}/tags", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EpubDto> addTag(@PathVariable(name = "id", required = true) Long id,
            @RequestBody IdDto dto) {
        if (id == null || !srv.existsById(id)) {
            logger.info("Invalid epub id given");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404),
                            "Epub not found"))
                    .build();
        }

        if (dto == null || dto.getId() == null) {
            logger.info("Invalid tag id given");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
                            "No tag id given"))
                    .build();
        }

        Epub epub = null;
        try {
            epub = srv.addTag(id, dto.getId());
        } catch (IllegalArgumentException ex) {
            logger.info("Invalid tag id given");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
                            "Invalid tag id given"))
                    .build();
        }

        if (epub == null) {
            logger.info("Epub and tag already associated");
            return ResponseEntity.noContent().build();
        }
        logger.info("Successfully added tag {} to epub {}", dto.getId(), epub);
        return ResponseEntity.ok(DtoFactory.convertEpubToDto(epub, false, false, false, false, false, true));
    }

    // #region remove tag
    @Operation(summary = "Remove tag to epub", description = "Removes tag with id in body to epub with id in path", parameters = {
            @Parameter(in = ParameterIn.PATH, name = "epub-id", description = "Id of the epub to remove the tag from", required = true),
            @Parameter(in = ParameterIn.PATH, name = "tag-id", description = "Id of the tag to remove from the epub", required = true)
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully removed tag", content = @Content(schema = @Schema(implementation = EpubDto.class))),
            @ApiResponse(responseCode = "204", description = "Tag not associated with epub"),
            @ApiResponse(responseCode = "400", description = "Invalid tag id given", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Epub with given id not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping(path = "/{epub-id}/tags/{tag-id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<EpubDto> removeTag(@PathVariable(name = "epub-id", required = true) Long id,
            @PathVariable(name = "tag-id", required = true) Long tagId) {
        if (id == null || !srv.existsById(id)) {
            logger.info("Invalid epub id given");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404),
                            "Epub not found"))
                    .build();
        }

        Epub epub = null;
        try {
            epub = srv.removeTag(id, tagId);
        } catch (IllegalArgumentException ex) {
            logger.info("Invalid tag id given");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
                            "Invalid tag id given"))
                    .build();
        }

        if (epub == null) {
            logger.info("Epub and tag were not associated");
            return ResponseEntity.noContent().build();
        }
        logger.info("Successfully removed tag {} from epub {}", tagId, epub);
        return ResponseEntity.ok(DtoFactory.convertEpubToDto(epub, false, false, false, false, false, true));
    }
}
