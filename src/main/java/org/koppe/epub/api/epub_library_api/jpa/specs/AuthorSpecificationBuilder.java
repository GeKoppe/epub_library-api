package org.koppe.epub.api.epub_library_api.jpa.specs;

import java.time.LocalDate;
import java.util.List;

import org.koppe.epub.api.epub_library_api.jpa.model.Author;
import org.koppe.epub.api.epub_library_api.jpa.model.Epub;
import org.koppe.epub.api.epub_library_api.jpa.model.Genre;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;

/**
 * Provides static methods for building specifications for querying authors in
 * database.
 */
public class AuthorSpecificationBuilder {

    /**
     * Creates a specification for filtering first name for given term
     * 
     * @param name Name to be checked for
     * @return Specification for jpa query
     */
    public static Specification<Author> firstNameContains(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isBlank())
                return cb.conjunction();

            return cb.like(cb.lower(root.get("firstName")), "%" + name.toLowerCase() + "%");
        };
    }

    /**
     * Creates a specification for filtering surname for given term
     * 
     * @param name Name to be checked for
     * @return Specification for JPA query
     */
    public static Specification<Author> surnameContains(String name) {
        return (root, query, cb) -> {
            if (name == null || name.isBlank())
                return cb.conjunction();

            return cb.like(cb.lower(root.get("surname")), "%" + name.toLowerCase() + "%");
        };
    }

    /**
     * Creates a specification that filters author for their birthdate, which has to
     * lie before the given birthdate
     * 
     * @param birth Date the author has to be born before
     * @return JPA specification
     */
    public static Specification<Author> birthBefore(LocalDate birth) {
        return (root, query, cb) -> {
            if (birth == null)
                return cb.conjunction();

            return cb.lessThan(root.get("birthDate"), birth);
        };
    }

    /**
     * Builds JPA Specification that filters for all authors that are associated
     * with at least one of the given genres
     * 
     * @param genres Genres to check for
     * @return JPA specification
     */
    public static Specification<Author> genreFilter(List<String> genres) {
        return (root, query, cb) -> {
            if (genres == null)
                return cb.conjunction();

            Join<Author, Genre> join = root.join("genres", JoinType.INNER);
            query.distinct(true);
            return join.get("name").in(genres);
        };
    }

    public static Specification<Author> bookFilter(List<Epub> books) {
        return (root, query, cb) -> {
            if (books == null)
                return cb.conjunction();

            Join<Author, Epub> join = root.join("books", JoinType.INNER);
            query.distinct(true);
            return join.get("name").in(books);
        };
    }

    /**
     * 
     * @return
     */
    public static Specification<Author> all() {
        return (root, query, cb) -> {
            return cb.conjunction();
        };
    }
}
