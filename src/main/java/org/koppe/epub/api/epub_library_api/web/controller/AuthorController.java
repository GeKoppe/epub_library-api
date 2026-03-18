package org.koppe.epub.api.epub_library_api.web.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.koppe.epub.api.epub_library_api.jpa.model.Author;
import org.koppe.epub.api.epub_library_api.jpa.service.AuthorService;
import org.koppe.epub.api.epub_library_api.utility.DtoFactory;
import org.koppe.epub.api.epub_library_api.web.dto.AuthorDto;
import org.koppe.epub.api.epub_library_api.web.dto.EpubDto;
import org.koppe.epub.api.epub_library_api.web.dto.GenreDto;
import org.koppe.epub.api.epub_library_api.web.dto.IdDto;
import org.koppe.epub.api.epub_library_api.web.dto.PageDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/authors")
@RequiredArgsConstructor
@Tag(name = "Authors", description = "Provides functionality for managing authors.")
public class AuthorController {
    /**
     * Logger
     */
    private final Logger logger = LoggerFactory.getLogger(AuthorController.class);
    /**
     * Service for interacting with authors in the database
     */
    private final AuthorService authors;

    // #region add author
    /**
     * Adds given author to the database.
     * 
     * @param dto Author to add.
     * @return Added author or 400, if no valid dto is given.
     */
    @Operation(summary = "Add author", description = "Adds author to the system. Only name and surname are required, everything else is optional")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully added author", content = @Content(schema = @Schema(implementation = AuthorDto.class))),
            @ApiResponse(responseCode = "400", description = "Missing name or surname", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    })
    @PostMapping(path = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthorDto> addAuthor(@RequestBody AuthorDto dto) {
        if (dto == null || dto.getFirstName() == null || dto.getFirstName().isBlank()
                || dto.getSurname() == null
                || dto.getSurname().isBlank()) {
            logger.info("No valid author given to add to database");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
                            "No valid author dto given"))
                    .build();
        }

        logger.info("Adding author {} to the database", dto);
        Author a = new Author();
        a.setFirstName(dto.getFirstName());
        a.setSurname(dto.getSurname());
        a.setBirthDate(dto.getBirthDate());
        a.setDeathDate(dto.getDeathDate());
        a.setDescription(dto.getDescription());

        Author newAuthor = authors.addAuthor(a);
        logger.info("Saved author {}", newAuthor);

        AuthorDto saved = DtoFactory.convertAuthorToDto(newAuthor, false, false);
        return ResponseEntity.ok(saved);
    }

    // #region get all authors
    /**
     * 
     * @param birthBefore
     * @param genres
     * @param firstName
     * @param surname
     * @param page
     * @param pageSize
     * @param withBooks
     * @param withGenres
     * @return
     */
    @Operation(summary = "Get all authors", description = "Returns all authors paged and filtered", parameters = {
            @Parameter(in = ParameterIn.QUERY, name = "birth_before", description = "Filters authors for those who were burn before the given date.", required = false),
            @Parameter(in = ParameterIn.QUERY, name = "first_name", description = "Filters authors for those whose first name contains the given string.", required = false),
            @Parameter(in = ParameterIn.QUERY, name = "surname", description = "Filters authors for those whose surname contains the given string.", required = false),
            @Parameter(in = ParameterIn.QUERY, name = "genres", description = "Filters authors for those associated with at least one of the given genres.", required = false),
            @Parameter(in = ParameterIn.QUERY, name = "with_epubs", description = "If set to true, books of the authors will also be loaded and returned.", required = false),
            @Parameter(in = ParameterIn.QUERY, name = "with_genres", description = "If set to true, genres the authors are associated with will also be loaded and returned.", required = false),
            @Parameter(in = ParameterIn.QUERY, name = "page", description = "0-indexed page of the content. If not given, the 0-th page will be returned.", required = false),
            @Parameter(in = ParameterIn.QUERY, name = "page_size", description = "Number of elements per page. If not given, page size will be set to 1000. Only necessary if page is given.", required = false)
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully returned requested authors", content = @Content(schema = @Schema(implementation = PageDto.class), array = @ArraySchema(schema = @Schema(implementation = AuthorDto.class)))),
    })
    @GetMapping(path = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<PageDto<AuthorDto>> getAllAuthors(
            @RequestParam(name = "birth_before") Optional<LocalDate> birthBefore,
            @RequestParam(name = "genres", defaultValue = "") List<String> genres,
            @RequestParam(name = "first_name") Optional<String> firstName,
            @RequestParam(name = "surname") Optional<String> surname,
            @RequestParam(name = "page") Optional<Integer> page,
            @RequestParam(name = "page_size") Optional<Integer> pageSize,
            @RequestParam(name = "with_epubs") Optional<Boolean> withBooks,
            @RequestParam(name = "with_genres") Optional<Boolean> withGenres) {

        Page<Author> authorPage = authors.findAll(firstName.orElse(null), surname.orElse(null),
                birthBefore.orElse(null), genres, page.orElse(null), pageSize.orElse(null));

        PageDto<AuthorDto> pageDto = new PageDto<>(authorPage.getNumber(),
                DtoFactory.convertMultipleAuthorsToDto(authorPage.getContent(), withBooks.orElse(false),
                        withGenres.orElse(false)),
                authorPage.getTotalElements(), authorPage.getSize());
        return ResponseEntity.ok(pageDto);
    }

    // #region author by id
    /**
     * API endpoint for finding authors with given id
     * 
     * @param id     Id for the author to be returned
     * @param books  If true, books of the author will be loaded as well.
     * @param genres If true, genres of the author will be loaded as well.
     * @return The found author or a 404, if no author with given id exists.
     */
    @Operation(summary = "Get author", description = "Returns author with given id", parameters = {
            @Parameter(in = ParameterIn.PATH, name = "id", description = "Id of the author to be returned", required = true),
            @Parameter(in = ParameterIn.QUERY, name = "with_epubs", description = "If set to true, books of the authors will also be loaded and returned. Defaults to false.", required = false),
            @Parameter(in = ParameterIn.QUERY, name = "with_genres", description = "If set to true, genres the authors are associated with will also be loaded and returned. Defaults to false.", required = false),
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully added author", content = @Content(schema = @Schema(implementation = AuthorDto.class))),
            @ApiResponse(responseCode = "400", description = "Missing name or surname", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
    })
    @GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthorDto> authorById(@PathVariable(name = "id") Long id,
            @RequestParam(name = "with_epubs") Optional<Boolean> books,
            @RequestParam(name = "with_genres") Optional<Boolean> genres) {

        logger.info("Looking up author with id {}", id);
        Author auth = authors.findById(id, books.orElse(false), genres.orElse(false));
        if (auth == null) {
            logger.info("No author with id {} exists", id);
            return ResponseEntity.of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404),
                    "Author with given id does not exist")).build();
        }

        AuthorDto dto = DtoFactory.convertAuthorToDto(auth, books.orElse(false), genres.orElse(false));
        return ResponseEntity.ok(dto);
    }

    // #region delete author
    @Operation(summary = "Delete author by id", description = "Deletes the author with the given id. If with_epubs is set to true, all epubs only associated to this author are also deleted", parameters = {
            @Parameter(in = ParameterIn.PATH, name = "id", description = "id of the author"),
            @Parameter(in = ParameterIn.QUERY, name = "with_epubs", description = "If set to true, all books only this author is associated to are deleted as well. Defaults to false", required = false) })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Author deleted successfully", content = @Content(schema = @Schema(implementation = AuthorDto.class))),
            @ApiResponse(responseCode = "400", description = "Author not deleted", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Author with given id does not exist", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthorDto> deleteAuthor(@PathVariable(name = "id") Long id,
            @RequestParam(name = "with_epubs", required = false) Optional<Boolean> withBooks) {
        if (id == null || !authors.existsById(id)) {
            logger.info("No id given or no author with id exists");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404),
                            "Author with given id found"))
                    .build();
        }

        Author author = authors.deleteById(id, withBooks.orElse(false));
        if (author == null) {
            logger.info("Author could not be deleted, assuming it does not exist");
            return ResponseEntity.of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
                    "Author does not exist or could not be deleted")).build();
        }
        logger.info("Successfully deleted author {}", author);

        return ResponseEntity.ok(DtoFactory.convertAuthorToDto(author, true, true));
    }

    // #region update author
    /**
     * Updates author with given id
     * 
     * @param id  Id of the author to be updated
     * @param dto Author dto containing updated values
     * @return Updated author
     */
    @Operation(summary = "Update author", description = """
            Updates author with given id.
            At least first name and surname must be given if overwrite_nulls is true.
            If id in body is not null and does not match id in path, the request will result in a 400 response.
            This endpoint will not update any books or genres.
            """, parameters = {
            @Parameter(in = ParameterIn.PATH, name = "id", description = "Id of the author to be updated", required = true),
            @Parameter(in = ParameterIn.QUERY, name = "overwrite_nulls", description = "If true, null values will overwrite existing values. Defaults to false.", required = false)
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully updated author", content = @Content(schema = @Schema(implementation = AuthorDto.class))),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Invalid author id", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PatchMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthorDto> updateAuthor(@PathVariable(name = "id") Long id, @RequestBody AuthorDto dto,
            @RequestParam(name = "overwrite_nulls", required = false) Optional<Boolean> overwriteNulls) {

        if (id == null) {
            logger.info("No id given");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404),
                            "No id given"))
                    .build();
        }

        if (!authors.existsById(id)) {
            logger.info("Author with given id does not exist");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404),
                            "Author with given id does not exist"))
                    .build();
        }

        if (dto.getId() != null && !dto.getId().equals(id)) {
            logger.info("Id in body does not match id in path");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
                            "Id in body and path do not match"))
                    .build();
        }

        Boolean nullOverwrite = overwriteNulls.orElse(false);
        if (nullOverwrite && (dto.getFirstName() == null || dto.getFirstName().isBlank()
                || dto.getSurname() == null
                || dto.getSurname().isBlank())) {

            logger.info("Null overwrite is active but first name or surname is not given");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
                            "Null overwrite is active but first name or surname is not given"))
                    .build();
        }

        dto.setId(id);
        logger.info("Updating author {}", dto);
        Author updated = authors.updateAuthor(dto, nullOverwrite);

        return ResponseEntity.ok(DtoFactory.convertAuthorToDto(updated, false, false));
    }

    // #region add book to author
    @Operation(summary = "Add book to author", description = """
            Adds given book to author with given id.
            Book needs to exist in the database already, therefore only .id needs to be filled in the body.
            """, parameters = {
            @Parameter(in = ParameterIn.PATH, name = "id", description = "Id of the author the book should be added to")
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Book successfully added to author", content = @Content(schema = @Schema(implementation = AuthorDto.class))),
            @ApiResponse(responseCode = "202", description = "Book already associated with author"),
            @ApiResponse(responseCode = "400", description = "Book does not exist in database", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Author with given id does not exist", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @PutMapping(path = "/{id}/epubs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthorDto> addBookToAuthor(@PathVariable(name = "id") Long id,
            @RequestBody IdDto book) {
        if (!authors.existsById(id)) {
            logger.warn("No author exists for id");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404),
                            "No author exists for given id"))
                    .build();
        }
        if (book.getId() == null || book.getId() < 1) {
            logger.warn("No valid book id given");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
                            "Invalid book id given"))
                    .build();
        }
        Author a = null;
        try {
            a = authors.addBook(id, book.getId());
        } catch (Exception ex) {
            logger.info("Invalid author or book id");
            return ResponseEntity.of(
                    ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
                            "Invalid author or book id given"))
                    .build();
        }
        if (a == null) {
            logger.info("Author {} already associated with book {}", id, book);
            return ResponseEntity.noContent().build();
        }
        AuthorDto dto = DtoFactory.convertAuthorToDto(a, true, false);
        return ResponseEntity.ok(dto);
    }

    // #region remove epub from author
    @Operation(summary = "Remove epub from author", description = "Removes epub with given id from author with given id", parameters = {
            @Parameter(in = ParameterIn.PATH, name = "author-id", description = "Id of the author to remove the epub from", required = true),
            @Parameter(in = ParameterIn.PATH, name = "epub-id", description = "Id of the epub to remove from the author", required = true)
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Successfully removed epub from author.", content = @Content(schema = @Schema(implementation = AuthorDto.class))),
            @ApiResponse(responseCode = "204", description = "Epub is not associated with author"),
            @ApiResponse(responseCode = "400", description = "No epub with given id exists", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Author with given id not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @DeleteMapping(path = "/{author-id}/epubs/{epub-id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthorDto> removeEpubFromAuthor(
            @PathVariable(name = "author-id", required = true) Long authorId,
            @PathVariable(name = "epub-id", required = true) Long epubId) {

        if (authorId == null || !authors.existsById(authorId)) {
            logger.info("No author with given id exists");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404),
                            "Author with given id not found"))
                    .build();
        }

        Author author = null;
        try {
            author = authors.removeEpubFromAuthor(authorId, epubId);
        } catch (IllegalAccessError ex) {
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
                            "Invalid epub id given"))
                    .build();
        }

        if (author == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(DtoFactory.convertAuthorToDto(author, true, false));
    }

    // #region get epubs for author
    @Operation(summary = "Get epubs for author", description = "Get all epubs for author with given id", parameters = {
            @Parameter(in = ParameterIn.PATH, name = "id", description = "Id of the author for which to get all epubs", required = true),
            @Parameter(in = ParameterIn.QUERY, name = "with_genres", description = "If true, also get genres of the epubs. Defaults to false.", required = false),
            @Parameter(in = ParameterIn.QUERY, name = "with_editions", description = "If true, also get editions of the epubs. Defaults to false.", required = false),
            @Parameter(in = ParameterIn.QUERY, name = "with_toc", description = "If true, also get table of contents of the editions. Only relevant, if with_editions is set to true. Defaults to false.", required = false)
    })
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Fetched epubs successfully."),
            @ApiResponse(responseCode = "404", description = "No author with given id exists", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    @GetMapping(path = "/{id}/epubs", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<EpubDto>> getEpubsForAuthor(@PathVariable(name = "id", required = true) Long id,
            @RequestParam(name = "with_genres", required = false) Optional<Boolean> withGenres,
            @RequestParam(name = "with_editions", required = false) Optional<Boolean> withEditions,
            @RequestParam(name = "with_toc", required = false) Optional<Boolean> withToc) {
        if (!authors.existsById(id)) {
            logger.info("No author with id {} exists", id);
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404),
                            "Author with given id not found"))
                    .build();
        }

        Author author = authors.findById(id);

        return ResponseEntity.ok(DtoFactory.convertMultipleEpubsToDto(author.getBooks(),
                withGenres.orElse(false),
                false, withEditions.orElse(false), withToc.orElse(false), false, false));
    }

    // #region add genre to author
    @Operation(summary = "Add genre to author", description = "Adds given genre to given author. If author is already associated with that genre, 204 is returned.", parameters = {
            @Parameter(in = ParameterIn.PATH, name = "id", description = "Id of the author to add the genre to", required = true)
    })
    @PutMapping(path = "/{id}/genres", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthorDto> addGenreToAuthor(@PathVariable(name = "id", required = true) Long authorId,
            @RequestBody IdDto id) {
        if (authorId == null || !authors.existsById(authorId)) {
            logger.info("No valid author id given");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404),
                            "No author with given id found"))
                    .build();
        }

        if (id == null || id.getId() == null) {
            logger.info("Invalid id for genre given");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
                            "Invalid genre id given"))
                    .build();
        }

        Author author = null;
        try {
            author = authors.addGenre(authorId, id.getId());
        } catch (IllegalArgumentException ex) {
            logger.info("Invalid genre id given");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
                            "Invalid genre id given"))
                    .build();
        }

        if (author == null) {
            logger.info("Genre already associated with author");
            return ResponseEntity.noContent().build();
        }
        logger.info("Added genre {} to author {}", id.getId(), author);
        AuthorDto dto = DtoFactory.convertAuthorToDto(author, false, true);

        return ResponseEntity.ok(dto);
    }

    // #region remove genre from author
    @Operation(summary = "Remove genre from author", description = "Removes genre with given id from author with given id. If author is not associated with genre 204 is returned", parameters = {
            @Parameter(in = ParameterIn.PATH, name = "author-id", description = "Id of the author to remove the genre from", required = true),
            @Parameter(in = ParameterIn.PATH, name = "genre-id", description = "Id of the genre to remove from the author", required = true)
    })
    @DeleteMapping(path = "/{author-id}/genres/{genre-id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AuthorDto> removeGenreFromAuthor(
            @PathVariable(name = "author-id", required = true) Long authorId,
            @PathVariable(name = "genre-id", required = true) Long genreId) {

        if (authorId == null || !authors.existsById(authorId)) {
            logger.info("No author with given id exists");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404),
                            "Author with given id not found"))
                    .build();
        }

        Author author = null;
        try {
            author = authors.removeGenreFromAuthor(authorId, genreId);
        } catch (IllegalAccessError ex) {
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
                            "Invalid genre id given"))
                    .build();
        }

        if (author == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(DtoFactory.convertAuthorToDto(author, false, true));
    }

    // #region get genres for author
    @GetMapping(path = "/{id}/genres", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Set<GenreDto>> getGenresForAuthor(
            @PathVariable(name = "id", required = true) Long authorId) {
        if (authorId == null || !authors.existsById(authorId)) {
            logger.info("No author with given id exists");
            return ResponseEntity
                    .of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404),
                            "Author with given id not found"))
                    .build();
        }

        Author author = authors.findById(authorId);
        Set<GenreDto> genres = DtoFactory.convertMultipleGenresToDtos(author.getGenres(), false, false);

        return ResponseEntity.ok(genres);
    }
}
