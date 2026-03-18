package org.koppe.epub.api.epub_library_api.jpa.repository;

import org.koppe.epub.api.epub_library_api.jpa.model.Genre;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface GenreRepository extends JpaRepository<Genre, Long>, JpaSpecificationExecutor<Genre> {

}
