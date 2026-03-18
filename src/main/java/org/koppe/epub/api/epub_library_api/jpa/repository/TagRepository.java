package org.koppe.epub.api.epub_library_api.jpa.repository;

import java.util.Optional;

import org.koppe.epub.api.epub_library_api.jpa.model.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TagRepository extends JpaRepository<Tag, Long>, JpaSpecificationExecutor<Tag> {
    public Optional<Tag> findByName(String name);
}
