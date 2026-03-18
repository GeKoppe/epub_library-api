package org.koppe.epub.api.epub_library_api.jpa.specs;

import org.koppe.epub.api.epub_library_api.jpa.model.Genre;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.jpa.domain.Specification;

public abstract class GenreSpecificationBuilder {
    /**
     * Logger
     */
    private static final Logger logger = LoggerFactory.getLogger(GenreSpecificationBuilder.class);

    // #region name contains
    public static Specification<Genre> nameContains(String name) {
        logger.debug("Build specification for genre name containing {}", name);
        return (root, query, cb) -> {
            if (name == null || name.isBlank())
                return cb.conjunction();

            return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
        };
    }
}
