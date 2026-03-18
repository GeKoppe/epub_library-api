package org.koppe.epub.api.epub_library_api.jpa.service;

import java.util.List;

import org.koppe.epub.api.epub_library_api.exceptions.RecursiveHeritageException;
import org.koppe.epub.api.epub_library_api.jpa.model.Author;
import org.koppe.epub.api.epub_library_api.jpa.model.Epub;
import org.koppe.epub.api.epub_library_api.jpa.model.Genre;
import org.koppe.epub.api.epub_library_api.jpa.repository.GenreRepository;
import org.koppe.epub.api.epub_library_api.jpa.specs.GenreSpecificationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class GenreService {
    /**
     * Logger
     */
    private final Logger logger = LoggerFactory.getLogger(GenreService.class);
    /**
     * JPA repository for working with genres
     */
    private final GenreRepository genres;
    /**
     * Service for working with basic operations for other entities
     */
    private final SharedService shared;

    // #region exists by id
    /**
     * Returns true, if the id for genre exists, false otherwise
     * 
     * @param id Id to check
     * @return True, if genre for id exists, false otherwise
     */
    public boolean existsById(long id) {
        return genres.existsById(id);
    }

    // #region find by id
    /**
     * Gets the genre associated to given id
     * 
     * @param id Id of the genre to return
     * @return The found genre
     * @throws IllegalArgumentException If id is null or no genre with given id
     *                                  exists
     */
    public Genre findById(long id) throws IllegalArgumentException {
        if (!existsById(id)) {
            logger.info("Invalid id given");
            throw new IllegalArgumentException("Invalid id given");
        }

        return genres.findById(id).get();
    }

    // #region add genre
    /**
     * Adds given genre to the database
     * 
     * @param genre Genre to add
     * @return The saved genre
     * @throws IllegalArgumentException If genre is null or no genre name is gievn
     */
    @Transactional
    public Genre addGenre(Genre genre) throws IllegalArgumentException {
        if (genre == null || genre.getName() == null || genre.getName().isBlank()) {
            logger.info("Invalid genre given");
            throw new IllegalArgumentException("Invalid genre given");
        }

        genre.setId(null);
        return genres.save(genre);
    }

    /**
     * Adds genre with given name to the database
     * 
     * @param name Name of the genre
     * @return Saved genre
     * @throws IllegalArgumentException If no name is given
     */
    @Transactional
    public Genre addGenre(String name) throws IllegalArgumentException {
        return addGenre(name, (Long) null);
    }

    /**
     * Adds genre with given name and parent genre to the database
     * 
     * @param name   Name of the genre
     * @param parent Parent id of the genre. If no genre with this id exists, genre
     *               will be added to db without parent
     * @return Saved genre
     * @throws IllegalArgumentException If no name is given
     */
    @Transactional
    public Genre addGenre(String name, Long parent) throws IllegalArgumentException {
        return addGenre(name, null, parent);
    }

    /**
     * Adds genre with given name and description to the database
     * 
     * @param name        Name of the genre
     * @param description Description of the genre
     * @return saved genre
     * @throws IllegalArgumentException If no name is given
     */
    @Transactional
    public Genre addGenre(String name, String description) throws IllegalArgumentException {
        return addGenre(name, description, null);
    }

    /**
     * Adds genre with given name, description and parent to the database
     * 
     * @param name        Name of the genre
     * @param description Description of the genre
     * @param parent      Parent of the genre
     * @return Saved genre
     * @throws IllegalArgumentException If no name is given
     */
    @Transactional
    public Genre addGenre(String name, String description, Long parent) throws IllegalArgumentException {
        if (parent == null || !existsById(parent)) {
            return addGenre(new Genre(null, name, description, null, null, null, null));
        }
        return addGenre(new Genre(null, name, description, null, null, findById(parent), null));
    }

    // #region find all
    public List<Genre> findAll(String nameContains) {
        Specification<Genre> spec = GenreSpecificationBuilder.nameContains(nameContains);

        return genres.findAll(spec);
    }

    // #region delete by id
    @Transactional
    public Genre deleteById(long id, boolean withSubgenres) throws IllegalArgumentException {
        if (!existsById(id)) {
            logger.info("Invalid id {} given", id);
            throw new IllegalArgumentException("Invalid id given");
        }

        Genre genre = findById(id);

        if (withSubgenres) {
            for (var child : genre.getChildren())
                deleteById(child.getId(), withSubgenres);
        }

        genres.delete(genre);
        return genre;
    }

    // #region add parent
    public Genre addParent(long id, long subgenreId) throws IllegalArgumentException, RecursiveHeritageException {
        if (!existsById(id) || !existsById(subgenreId)) {
            logger.info("Genre with id {} does not exist", id);
            throw new IllegalArgumentException("Genre with given id does not exist");
        }

        Genre sub = findById(subgenreId);
        for (var x : sub.getChildren()) {
            if (x.getId().equals(id)) {
                throw new RecursiveHeritageException("Cannot add child as parent");
            }
        }
        if (sub.getParent() != null) {
            if (sub.getParent().getId().equals(id)) {
                logger.info("Genre {} already has genre {} as parent", sub, id);
                return null;
            } else {
                logger.info("Genre {} already has another parent, overwriting with new parent");
            }
        }

        Genre parent = findById(id);
        sub.setParent(parent);

        logger.info("Set genre {} as parent to {}", parent, sub);
        return genres.save(sub);
    }

    // #region add subgenre
    public Genre addSubgenre(long id, long subgenreId) throws IllegalArgumentException, RecursiveHeritageException {
        if (!existsById(id) || !existsById(subgenreId)) {
            logger.info("Genre with id {} does not exist", id);
            throw new IllegalArgumentException("Genre with given id does not exist");
        }

        Genre g = findById(id);
        if (g.getParent().getId().equals(subgenreId)) {
            logger.info("Genre {} parent is {}, cannot add parent as subgenre", g, subgenreId);
            throw new RecursiveHeritageException("Genre " + id + " is already subgenre of " + subgenreId);
        }

        for (var x : g.getChildren()) {
            if (x.getId().equals(subgenreId)) {
                logger.info("Genre {} already has subgenre {}", g, subgenreId);
                return null;
            }
        }
        Genre sub = findById(subgenreId);
        g.getChildren().add(sub);
        logger.info("Added {} as subgenre to {}", sub, g);

        return genres.save(g);
    }

    // #region add epub to genre
    @Transactional
    public Epub addEpubToGenre(long genreId, long epubId) throws IllegalArgumentException {
        if (!existsById(genreId) || !shared.epubExistsById(epubId)) {
            logger.info("Genre or epub with given id do not exist");
            throw new IllegalArgumentException("Genre or epub with given id do not exist");
        }

        return shared.addGenreToEpub(genreId, epubId);
    }

    // #region add author to genre
    @Transactional
    public Author addAuthorToGenre(long genreId, long authorId) throws IllegalArgumentException {
        if (!existsById(genreId) || !shared.authorExistsById(authorId)) {
            logger.info("Invalid genre id or author id given");
            throw new IllegalArgumentException("Invalid author id or genre id given");
        }

        return shared.addGenreToAuthor(authorId, genreId);
    }
}
