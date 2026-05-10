package org.koppe.epub.api.epub_library_api.jpa.repository;

import java.util.Optional;
import java.util.Set;

import org.koppe.epub.api.epub_library_api.jpa.model.EpubEdition;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EpubEditionRepository extends JpaRepository<EpubEdition, Long>, JpaSpecificationExecutor<EpubEdition> {
    public Set<EpubEdition> findAllByDownloadGuid(String downloadGuid);
    public Set<EpubEdition> findAllByUploadGuid(String uploadGuid);
    public Optional<EpubEdition> findByMd5Hash(String md5Hash);
}
