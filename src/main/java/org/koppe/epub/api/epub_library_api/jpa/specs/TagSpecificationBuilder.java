package org.koppe.epub.api.epub_library_api.jpa.specs;

import java.util.ArrayList;
import java.util.List;

import org.koppe.epub.api.epub_library_api.jpa.model.Tag;
import org.springframework.data.jpa.domain.Specification;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

public class TagSpecificationBuilder {

    public static Specification<Tag> nameContains(String name) {
        return (root, query, cb) -> {
            return nameContainsPredicate(cb, root, name);
        };
    }

    public static Specification<Tag> oneOf(List<String> names) {
        return (root, query, cb) -> {
            if (names == null || names.size() == 0)
                return cb.conjunction();

            List<Predicate> predicates = new ArrayList<>();
            names.forEach(name -> {
                if (name != null && !name.isBlank())
                    predicates.add(nameContainsPredicate(cb, root, name));
            });
            return cb.or(predicates);
        };
    }

    private static Predicate nameContainsPredicate(CriteriaBuilder cb, Root<Tag> root, String name) {
        if (name == null || name.isBlank())
            return cb.conjunction();
        return cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }
}
