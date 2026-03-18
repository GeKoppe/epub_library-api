package org.koppe.epub.api.epub_library_api.jpa.repository;

import org.koppe.epub.api.epub_library_api.jpa.model.Epub;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EpubRepository extends JpaRepository<Epub, Long>, JpaSpecificationExecutor<Epub> {

}
