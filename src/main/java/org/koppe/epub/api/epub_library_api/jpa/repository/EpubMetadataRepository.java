package org.koppe.epub.api.epub_library_api.jpa.repository;

import org.koppe.epub.api.epub_library_api.jpa.model.EpubMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EpubMetadataRepository
        extends JpaRepository<EpubMetadata, Long>, JpaSpecificationExecutor<EpubMetadata> {

}
