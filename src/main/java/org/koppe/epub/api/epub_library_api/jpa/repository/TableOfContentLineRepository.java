package org.koppe.epub.api.epub_library_api.jpa.repository;

import org.koppe.epub.api.epub_library_api.jpa.model.TableOfContentLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface TableOfContentLineRepository
        extends JpaRepository<TableOfContentLine, Long>, JpaSpecificationExecutor<TableOfContentLine> {

}
