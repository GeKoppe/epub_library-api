package org.koppe.epub.api.epub_library_api.jpa.service;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.koppe.epub.api.epub_library_api.jpa.model.Author;
import org.koppe.epub.api.epub_library_api.jpa.model.Epub;
import org.koppe.epub.api.epub_library_api.jpa.repository.AuthorRepository;
import org.koppe.epub.api.epub_library_api.jpa.specs.AuthorSpecificationBuilder;
import org.koppe.epub.api.epub_library_api.web.dto.AuthorDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;

/**
 * Encapsulates database operations for authors
 */
@Service
@RequiredArgsConstructor
public class AuthorService {
    /**
     * Logger
     */
    private final Logger logger = LoggerFactory.getLogger(AuthorService.class);
    /**
     * JPA repository to work with authors in the database
     */
    private final AuthorRepository authors;
    private final SharedService shared;

    // #region exists by id
    public boolean existsById(Long id) throws IllegalArgumentException {
        if (id == null) {
            logger.info("No id given");
            throw new IllegalArgumentException("No id given");
        }
        return authors.existsById(id);
    }

    // #region find by id
    /**
     * Returns author with given id. Books and genres will not be fetched.
     * 
     * @param id Id of the author
     * @return Found author or null, if no author with given id exists
     * @throws IllegalArgumentException If no id is given or id is less than 0
     */
    public Author findById(Long id) throws IllegalArgumentException {
        return findById(id, false, false);
    }

    /**
     * Returns author with given id. Genres will not be fetched.
     * 
     * @param id        Id of the author
     * @param withBooks If true, books will be fetched as well.
     * @return Found author or null, if no author with given id exists
     * @throws IllegalArgumentException If no id is given or id is less than 0
     */
    public Author findById(Long id, boolean withBooks) throws IllegalArgumentException {
        return findById(id, withBooks, false);
    }

    /**
     * Returns author with given id. Genres will not be fetched.
     * 
     * @param id         Id of the author
     * @param withBooks  If true, books will be fetched as well.
     * @param withGenres if true, genres will be fetched as well.
     * @return Found author or null, if no author with given id exists
     * @throws IllegalArgumentException If no id is given or id is less than 0
     */
    public Author findById(Long id, boolean withBooks, boolean withGenres) throws IllegalArgumentException {
        // Check if id is valid
        if (id == null || id < 0) {
            logger.info("No id given");
            throw new IllegalArgumentException("Author id is missing");
        }

        // Get author and check if exists
        Optional<Author> authorOpt = authors.findById(id);
        if (authorOpt.isEmpty()) {
            logger.info("Id {} does not correspond to an author", id);
            return null;
        }
        Author author = authorOpt.get();

        // Fetch books and genres if required
        if (withBooks) {
            logger.debug("Books are requested, fetching books");
            author.getBooks().size();
        }

        if (withGenres) {
            logger.debug("Genres are requested, fetching genres");
            author.getGenres().size();
        }

        logger.info("Found author {} for id {}", author, id);
        return author;
    }

    // #region add author
    /**
     * Adds author with given first and surname
     * 
     * @param firstName First name of the author
     * @param surname   Surname of the author
     * @return Created author
     * @throws IllegalArgumentException If first or surname are not given
     */
    @Transactional
    public Author addAuthor(String firstName, String surname) throws IllegalArgumentException {
        if (firstName == null || firstName.isBlank() || surname == null || surname.isBlank()) {
            logger.info("No name given");
            throw new IllegalArgumentException("No name given");
        }

        Author auth = new Author();
        auth.setFirstName(firstName);
        auth.setSurname(surname);

        return addAuthor(auth);
    }

    /**
     * Adds author with given first and surname, as well as authors description
     * 
     * @param firstName   First name of the author
     * @param surname     Surname of the author
     * @param description Description of the authors style or biography
     * @return Created author
     * @throws IllegalArgumentException If first or surname are not given
     */
    @Transactional
    public Author addAuthor(String firstName, String surname, String description) throws IllegalArgumentException {
        if (firstName == null || firstName.isBlank() || surname == null || surname.isBlank()) {
            logger.info("No name given");
            throw new IllegalArgumentException("No name given");
        }

        Author auth = new Author();
        auth.setFirstName(firstName);
        auth.setSurname(surname);
        auth.setDescription(description);

        return addAuthor(auth);
    }

    /**
     * Adds a new author to the database
     * 
     * @param author Author to add
     * @return The added author
     * @throws IllegalArgumentException If author or their first name or surname are
     *                                  empty
     */
    @Transactional
    public Author addAuthor(Author author) throws IllegalArgumentException {
        if (author == null) {
            logger.info("No author given");
            throw new IllegalArgumentException("No author given");
        }

        if (author.getFirstName() == null || author.getFirstName().isBlank() || author.getSurname() == null
                || author.getSurname().isBlank()) {
            logger.info("No name given");
            throw new IllegalArgumentException("No name given");
        }

        author.setId(null);
        return authors.save(author);
    }

    // #region find all
    /**
     * Finds all authors with the given criteria
     * 
     * @param firstNameContains If not null, only authors with given string in their
     *                          first name are returned (case insensitive).
     * @param surnameContains   If not null, only authors with given string in their
     *                          surname are returned (case insensitive).
     * @param birthBefore       If not null, only authors born before this date are
     *                          returned.
     * @param genres            If not null, only authors that are associated with
     *                          at least one of the given genres are returned.
     * @param page              Page of the author list to be returned. If not
     *                          given, 0-th page is assumed
     * @param pageSize          Size of the pages of the author list. If not given,
     *                          1000 is assumed
     * @return Page of the author list.
     */
    public Page<Author> findAll(String firstNameContains, String surnameContains, LocalDate birthBefore,
            List<String> genres, Integer page, Integer pageSize) {
        Specification<Author> spec = Specification
                .where(AuthorSpecificationBuilder.firstNameContains(firstNameContains))
                .and(AuthorSpecificationBuilder.surnameContains(surnameContains))
                .and(AuthorSpecificationBuilder.birthBefore(birthBefore))
                .and(AuthorSpecificationBuilder.genreFilter(genres));

        return authors.findAll(spec,
                PageRequest.of(page != null && page >= 0 ? page : 0, pageSize != null && pageSize > 0 ? pageSize : 1000)
                        .withSort(Sort.by(Order.desc("id"))));
    }

    // #region deleteById
    /**
     * Deletes author with given id
     * 
     * @param id Id of the author to delete
     * @return Deleted author or null, if no author with given id exists
     * @throws IllegalArgumentException If no id is given
     */
    @Transactional
    public Author deleteById(Long id, Boolean withBooks) throws IllegalArgumentException {
        if (id == null) {
            logger.info("No id given");
            throw new IllegalArgumentException("No id given");
        }

        if (!existsById(id)) {
            logger.info("No author with id {} exists", id);
            return null;
        }

        Author a = authors.findById(id).get();
        authors.delete(a);

        if (withBooks) {
            logger.info("Books are also to be deleted");
            Set<Epub> authorEpubs = new HashSet<>();
            for (var x : a.getBooks()) {
                if (x.getAuthors().size() == 1) {
                    logger.info("Only author {} associated with epub {}, deleting epub", a, x);
                    shared.deleteEpub(x.getId());
                    authorEpubs.add(x);
                }
            }
            a.setBooks(authorEpubs);
        } else {
            logger.info("Books are not to be deleted");
            a.setBooks(new HashSet<>());
        }

        return a;
    }

    // #region update author
    /**
     * Updates given author. At least dto.id needs to be given. If overwriteNulls is
     * true, dto.firstName and dto.surname cannot be null.
     * 
     * @param dto            Data the author should be updated with
     * @param overwriteNulls If true, null values in the dto will be set into the
     *                       author entity. If false, existing values will stay in
     *                       author entity.
     * @return The updated author entity
     * @throws IllegalArgumentException If dto or dto.id is null, if no author with
     *                                  given id exists or overwriteNulls is true
     *                                  and dto.firstName or dto.surname are not
     *                                  given.
     */
    @Transactional
    public Author updateAuthor(AuthorDto dto, boolean overwriteNulls) throws IllegalArgumentException {
        if (dto == null || dto.getId() == null) {
            logger.info("No author or id given");
            throw new IllegalArgumentException("No author or id given");
        }
        if (!existsById(dto.getId())) {
            logger.info("Author with given id does not exist");
            throw new IllegalArgumentException("Author with given id does not exist");
        }

        if (overwriteNulls && (dto.getFirstName() == null || dto.getFirstName().isBlank() || dto.getSurname() == null
                || dto.getSurname().isBlank())) {
            logger.info("Null overwrite is active but first name or surname is not given in {}", dto);
            throw new IllegalArgumentException("Null overwrite is active but first name or surname is not given in {}");
        }

        Author author = findById(dto.getId());
        if (overwriteNulls || !(dto.getFirstName() == null || dto.getFirstName().isBlank()))
            author.setFirstName(dto.getFirstName());

        if (overwriteNulls || !(dto.getSurname() == null || dto.getSurname().isBlank()))
            author.setSurname(dto.getSurname());

        if (overwriteNulls || !(dto.getDescription() == null || dto.getDescription().isBlank()))
            author.setDescription(dto.getDescription());

        if (overwriteNulls || dto.getBirthDate() != null)
            author.setBirthDate(dto.getBirthDate());

        if (overwriteNulls || dto.getDeathDate() != null)
            author.setDeathDate(dto.getDeathDate());

        return authors.save(author);
    }

    // #region add book
    /**
     * 
     * @param authorId
     * @param bookId
     * @return
     * @throws IllegalArgumentException
     */
    public Author addBook(long authorId, long bookId) throws IllegalArgumentException {
        if (!existsById(authorId) || !shared.epubExistsById(bookId)) {
            throw new IllegalArgumentException();
        }
        return shared.addEpubToAuthor(authorId, bookId);
    }

    // #region remove epub from author
    public Author removeEpubFromAuthor(long authorId, long epubId) throws IllegalArgumentException {
        if (!existsById(authorId) || !shared.epubExistsById(epubId)) {
            throw new IllegalArgumentException();
        }

        return shared.removeEpubFromAuthor(authorId, epubId);
    }

    // #region add genre to author
    public Author addGenre(long authorId, long genreId) throws IllegalArgumentException {
        if (!existsById(authorId) || !shared.genreExistsById(genreId)) {
            throw new IllegalArgumentException("Invalid genre id given");
        }

        return shared.addGenreToAuthor(authorId, genreId);
    }

    // #region remove genre from author
    public Author removeGenreFromAuthor(long authorId, long genreId) throws IllegalArgumentException {
        if (!existsById(authorId) || !shared.genreExistsById(genreId)) {
            throw new IllegalArgumentException("Invalid genre id given");
        }

        return shared.removeGenreFromAuthor(authorId, genreId);
    }
}
