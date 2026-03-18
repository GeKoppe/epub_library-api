package org.koppe.epub.api.epub_library_api.web.controller;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.koppe.epub.api.epub_library_api.exceptions.RecursiveHeritageException;
import org.koppe.epub.api.epub_library_api.jpa.model.Author;
import org.koppe.epub.api.epub_library_api.jpa.model.Epub;
import org.koppe.epub.api.epub_library_api.jpa.model.Genre;
import org.koppe.epub.api.epub_library_api.jpa.service.GenreService;
import org.koppe.epub.api.epub_library_api.utility.DtoFactory;
import org.koppe.epub.api.epub_library_api.web.dto.AuthorDto;
import org.koppe.epub.api.epub_library_api.web.dto.EpubDto;
import org.koppe.epub.api.epub_library_api.web.dto.GenreDto;
import org.koppe.epub.api.epub_library_api.web.dto.IdDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
@RequestMapping(path = "/genres")
@Tag(name = "Genres", description = "Functionality for managing genres.")
@RequiredArgsConstructor
public class GenreController {
	/**
	 * Logger
	 */
	private final Logger logger = LoggerFactory.getLogger(GenreController.class);
	/**
	 * Service for interacting with genres in the database
	 */
	private final GenreService genres;

	// #region add genre
	/**
	 * Adds given genre to the database
	 * 
	 * @param dto Definition of the new genre
	 * @return Added genre
	 */
	@Operation(summary = "Add a genre", description = """
			Adds a new genre to the database. Only the name of the genre is required, everything else is optional.
			If a parent id is given but no genre with that id exists, the genre will be added without parent.
			""")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Genre added successfully", content = @Content(schema = @Schema(implementation = GenreDto.class))),
			@ApiResponse(responseCode = "400", description = "Invalid body given. Further details in response body", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	})
	@PostMapping(path = "", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<GenreDto> addGenre(@RequestBody(required = true) GenreDto dto) {
		if (dto == null) {
			logger.info("No dto given");
			return ResponseEntity
					.of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
							"No genre to add given"))
					.build();
		}

		if (dto.getName() == null || dto.getName().isBlank()) {
			logger.info("No name for the new genre given");
			return ResponseEntity.of(
					ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
							"No name for the new genre given"))
					.build();
		}

		logger.info("Adding genre {}", dto);
		Genre g = genres.addGenre(dto.getName(), dto.getDescription(), dto.getParentId());
		GenreDto saved = DtoFactory.convertGenreToDto(g, false, false);

		return ResponseEntity.ok(saved);
	}

	// #region get by id
	/**
	 * Returns the genre denoted by the given id.
	 * 
	 * @param id          Id of the genre to return
	 * @param withBooks   If true, books of the genre will be returned as well
	 * @param withAuthors If true, authors of the genre will be returned as well
	 * @return Found genre
	 */
	@Operation(summary = "Get genre by id", description = """
			Returns genre associated to given id.
			Determine wether to get books and authors within the query.
			""", parameters = {
			@Parameter(name = "with_epubs", description = "If true, books will also be parsed into the response", required = false),
			@Parameter(name = "with_authors", description = "If true, authors will also be parsed into the response", required = false)
	})
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully returned the genre", content = @Content(schema = @Schema(implementation = GenreDto.class))),
			@ApiResponse(responseCode = "404", description = "No genre with given id found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	})
	@GetMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<GenreDto> getGenreById(@PathVariable(name = "id") Long id,
			@RequestParam(name = "with_epubs", required = false) Optional<Boolean> withBooks,
			@RequestParam(name = "with_authors", required = false) Optional<Boolean> withAuthors) {
		if (id == null || !genres.existsById(id)) {
			logger.info("No genre with id {} found", id);
			return ResponseEntity
					.of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404),
							"No genre found for given id"))
					.build();
		}
		Genre genre = genres.findById(id);
		GenreDto dto = DtoFactory.convertGenreToDto(genre, withBooks.orElse(false), withAuthors.orElse(false));
		return ResponseEntity.ok(dto);
	}

	// #region delete genre
	@Operation(summary = "Delete genre", description = "Deletes genre denoted by the given id", parameters = {
			@Parameter(in = ParameterIn.QUERY, name = "recursively", description = "If set to true, all subgenres will be deleted recursively as well. Defaults to false.", required = false),
			@Parameter(in = ParameterIn.PATH, name = "id", description = "Id of the genre to be deleted", required = true)
	})
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Genre deleted successfully", content = @Content(schema = @Schema(implementation = GenreDto.class))),
			@ApiResponse(responseCode = "404", description = "Genre with given idnot found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	})
	@DeleteMapping(path = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<GenreDto> deleteGenre(@PathVariable(name = "id", required = true) Long id,
			@RequestParam(name = "recursively", required = false) Optional<Boolean> recursively) {
		if (id == null || !genres.existsById(id)) {
			logger.info("Invalid genre id {}", id);
			return ResponseEntity
					.of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404),
							"No genre with given id exists"))
					.build();
		}

		Genre genre = genres.deleteById(id, recursively.orElse(false));
		GenreDto dto = DtoFactory.convertGenreToDto(genre, false, false);
		return ResponseEntity.ok(dto);
	}

	// #region add subgenre
	@Operation(summary = "Add a subgenre", description = """
			Adds a subgenre to an existing genre. Only id of the subgenre is required in the body.
			Genre cannot be it's own subgenre!
			""", parameters = {
			@Parameter(in = ParameterIn.PATH, name = "id", description = "Id of the genre to add a subgenre to", required = true)
	})
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully added subgenre to genre", content = @Content(schema = @Schema(implementation = GenreDto.class))),
			@ApiResponse(responseCode = "204", description = "Genres already associated with each other"),
			@ApiResponse(responseCode = "400", description = "Invalid body given. Further information in response body.", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
			@ApiResponse(responseCode = "404", description = "Genre with id in path not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	})
	@PutMapping(path = "/{id}/subgenres", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<GenreDto> addSubgenre(@PathVariable(name = "id", required = true) Long id,
			@RequestBody IdDto dto) {

		if (id == null || !genres.existsById(id)) {
			logger.info("Invalid id {} given", id);
			return ResponseEntity
					.of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404),
							"Invalid genre id given"))
					.build();
		}
		if (dto == null || dto.getId() == null || !genres.existsById(dto.getId())) {
			logger.info("Invalid dto given");
			return ResponseEntity.of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
					"Body or body.id are not given or genre with id in body does not exist"))
					.build();
		}

		Genre genre = null;
		try {
			genre = genres.addSubgenre(id, dto.getId());
		} catch (RecursiveHeritageException ex) {
			logger.info("Circle heritage not allowed");
			return ResponseEntity.of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400),
					"Cannot add parent of genre as subgenre")).build();
		}

		if (genre == null) {
			logger.info("Subgenre already set");
			return ResponseEntity.noContent().build();
		}

		GenreDto newDto = DtoFactory.convertGenreToDto(genre, false, false);

		return ResponseEntity.ok(newDto);
	}

	// #region get epubs for genre
	@Operation(summary = "Get epubs for genre", description = "Returns all epubs associated with given genre.", parameters = {
			@Parameter(in = ParameterIn.PATH, name = "id", description = "Id of the genre to get all epubs for", required = true)
	})
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully returned all epubs associated with given genre"),
			@ApiResponse(responseCode = "404", description = "No genre with given id exists", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	})
	@GetMapping(path = "/{id}/epubs", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Set<EpubDto>> getEpubsForGenre(@PathVariable(name = "id", required = true) Long id) {
		if (id == null || !genres.existsById(id)) {
			logger.info("Invalid id {} given", id);
			return ResponseEntity.of(
					ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404),
							"Genre for given id does not exist"))
					.build();
		}

		Genre genre = genres.findById(id);
		logger.info("Found genre {}", genre);
		Set<EpubDto> dtos = new HashSet<>();

		for (var x : genre.getBooks())
			dtos.add(DtoFactory.convertEpubToDto(x, false, false, false, false, false, false));

		logger.info("Returning epubs {}", dtos);
		return ResponseEntity.ok(dtos);
	}

	// #region add epub to genre
	@Operation(summary = "Add epub to genre", description = """
			Adds epub to genre with given id.
			If epub already is associated with genre, 204 is returned.
			""", parameters = {
			@Parameter(in = ParameterIn.PATH, description = "Id of the genre to modify", required = true) })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully added epub to genre", content = @Content(schema = @Schema(implementation = GenreDto.class))),
			@ApiResponse(responseCode = "204", description = "Genre already associated with epub"),
			@ApiResponse(responseCode = "400", description = "Invalid epub dto given", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
			@ApiResponse(responseCode = "404", description = "No genre with given id found", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
			@ApiResponse(responseCode = "500", description = "Some error occurred during the database transaction", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	})
	@PutMapping(path = "/{id}/epubs", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<GenreDto> addEpubToGenre(@PathVariable(name = "id", required = true) Long id,
			@RequestBody IdDto dto) {

		if (id == null || !genres.existsById(id)) {
			logger.info("Invalid genre id given");
			return ResponseEntity
					.of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404), "No genre with given id found"))
					.build();
		}

		Epub epub = null;
		try {
			epub = genres.addEpubToGenre(id, dto.getId());
		} catch (IllegalArgumentException ex) {
			return ResponseEntity
					.of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), "Invalid author id given"))
					.build();
		} catch (Exception ex) {
			return ResponseEntity
					.of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(500),
							"Exception occurred while adding author to genre: " + ex.getMessage()))
					.build();
		}
		if (epub == null) {
			return ResponseEntity.noContent().build();
		}
		Genre genre = genres.findById(id);
		GenreDto covnertedGenre = DtoFactory.convertGenreToDto(genre, true, false);

		return ResponseEntity.ok(covnertedGenre);
	}

	// #region add author to genre
	@Operation(summary = "Add author to genre", description = "Adds author with id given in body to genre with id given in path.", parameters = {
			@Parameter(in = ParameterIn.PATH, description = "Id of the genre to add the author to.", required = true) })
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Successfully added author to genre.", content = @Content(schema = @Schema(implementation = EpubDto.class))),
			@ApiResponse(responseCode = "204", description = "Author already associated with the given genre."),
			@ApiResponse(responseCode = "400", description = "Invalid author id given", content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
			@ApiResponse(responseCode = "404", description = "Genre with given id not found", content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
	})
	@PutMapping(path = "/{id}/authors", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<GenreDto> addAuthorToGenre(@PathVariable(name = "id", required = true) Long id,
			@RequestBody IdDto dto) {

		if (id == null || !genres.existsById(id)) {
			logger.info("Invalid genre id given");
			return ResponseEntity
					.of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404), "No genre with given id found"))
					.build();
		}

		Author epub = null;
		try {
			epub = genres.addAuthorToGenre(id, dto.getId());
		} catch (IllegalArgumentException ex) {
			return ResponseEntity
					.of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(400), "Invalid author id given"))
					.build();
		} catch (Exception ex) {
			return ResponseEntity
					.of(ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(500),
							"Exception occurred while adding author to genre: " + ex.getMessage()))
					.build();
		}
		if (epub == null) {
			return ResponseEntity.noContent().build();
		}
		Genre genre = genres.findById(id);
		GenreDto covnertedGenre = DtoFactory.convertGenreToDto(genre, false, true);

		return ResponseEntity.ok(covnertedGenre);
	}

	// #region get authors for genre
	@GetMapping(path = "/{id}/authors", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<Set<AuthorDto>> getAuthorsForGenre(@PathVariable(name = "id", required = true) Long id) {
		if (id == null || !genres.existsById(id)) {
			logger.info("Invalid id {} given", id);
			return ResponseEntity.of(
					ProblemDetail.forStatusAndDetail(HttpStatusCode.valueOf(404),
							"Genre for given id does not exist"))
					.build();
		}

		Genre genre = genres.findById(id);
		logger.info("Found genre {}", genre);
		Set<AuthorDto> dtos = new HashSet<>();

		for (var x : genre.getAuthors())
			dtos.add(DtoFactory.convertAuthorToDto(x, false, false));

		logger.info("Returning epubs {}", dtos);
		return ResponseEntity.ok(dtos);
	}
}
