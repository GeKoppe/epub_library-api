package org.koppe.epub.api.epub_library_api.jpa.specs;

import java.time.LocalDate;
import java.util.List;

import org.koppe.epub.api.epub_library_api.jpa.model.Author;
import org.koppe.epub.api.epub_library_api.jpa.model.Epub;
import org.koppe.epub.api.epub_library_api.jpa.model.Genre;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

/**
 * Class for building specifications for querying JPA repositories.
 */
public class EpubSpecificationBuilder {
    /**
     * Logger
     */
    private static final Logger logger = LoggerFactory.getLogger(EpubSpecificationBuilder.class);

    // #region title contains
    /**
     * Builds a specification that filters case insensitive, if the title of an epub
     * contains the given string.
     * 
     * @param title String the epub title should contain
     * @return Specification
     */
    public static Specification<Epub> titleContains(String title) {
        logger.debug("Build specification for title contains {}", title);
        return (root, query, cb) -> {
            if (title == null || title.isBlank())
                return cb.conjunction();
            return cb.like(cb.lower(root.get("title")), "%" + title.toLowerCase() + "%");
        };
    }

    // #region published before
    /**
     * Builds a specification that filters if an ePub has been published before the
     * given date.
     * 
     * @param publishedBefore Date the epub should have been published before
     * @return Specification
     */
    public static Specification<Epub> publishedBefore(LocalDate publishedBefore) {
        logger.debug("Build specification for publishing before {}", publishedBefore);
        return (root, query, cb) -> {
            if (publishedBefore == null)
                return cb.conjunction();

            return cb.lessThan(root.get("publishingDate"), publishedBefore);
        };
    }

    // #region published after
    /**
     * Builds a specification that filters if an ePub has been published after the
     * given date.
     * 
     * @param publishedAfter Date the epub should have been published before
     * @return Specification
     */
    public static Specification<Epub> publishedAfter(LocalDate publishedAfter) {
        logger.debug("Build specification for publishing before {}", publishedAfter);
        return (root, query, cb) -> {
            if (publishedAfter == null)
                return cb.conjunction();

            return cb.greaterThan(root.get("publishingDate"), publishedAfter);
        };
    }

    // #region uploaded before
    /**
     * Builds a specification that filters epubs for those uploaded before given
     * date.
     * 
     * @param uploadedBefore Date epubs are to be uploaded before
     * @return Specification
     */
    public static Specification<Epub> uploadedBefore(LocalDate uploadedBefore) {
        logger.debug("Build specification for uploading before {}", uploadedBefore);
        return (root, query, cb) -> {
            if (uploadedBefore == null)
                return cb.conjunction();

            return cb.lessThan(root.get("uploadDate"), uploadedBefore);
        };
    }

    // #region uploaded after
    /**
     * Builds a specification that filters epubs for those uploaded after given
     * date.
     * 
     * @param uploadedBefore Date epubs are to be uploaded after
     * @return Specification
     */
    public static Specification<Epub> uploadedAfter(LocalDate uploadedAfter) {
        logger.debug("Build specification for uploading after {}", uploadedAfter);
        return (root, query, cb) -> {
            if (uploadedAfter == null)
                return cb.conjunction();

            return cb.greaterThan(root.get("uploadDate"), uploadedAfter);
        };
    }

    // #region genre filter
    /**
     * Creates a specification that filters epubs for those that are associated with
     * at least one of the given genres.
     * 
     * @param genres Genres epubs should be associated with
     * @return Specification
     */
    public static Specification<Epub> genreFilter(List<Genre> genres) {
        logger.debug("Build specification for containing on of following genres: {}", genres);
        return (root, query, cb) -> {
            if (genres == null || genres.size() == 0)
                return cb.conjunction();

            Join<Epub, Genre> join = root.join("genres", JoinType.INNER);
            query.distinct(true);
            return join.get("name").in(genres);
        };
    }

    // #region author filter
    /**
     * Creates a specification that filters epubs for those that are associated with
     * at least one of the given authors.
     * 
     * @param authors Authors epubs should be associated with
     * @return Specification
     */
    public static Specification<Epub> authorFilter(List<Author> authors) {
        logger.debug("Build specification for containing on of the following authors: {}", authors);

        return (root, query, cb) -> {
            if (authors == null || authors.size() == 0)
                return cb.conjunction();

            Join<Epub, Author> join = root.join("authors", JoinType.INNER);
            query.distinct(true);
            return join.get("surname").in(authors);
        };
    }

}
