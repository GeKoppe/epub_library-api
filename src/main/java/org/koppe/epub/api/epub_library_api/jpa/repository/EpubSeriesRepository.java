package org.koppe.epub.api.epub_library_api.jpa.repository;

import java.util.Optional;

import org.koppe.epub.api.epub_library_api.jpa.model.EpubSeries;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EpubSeriesRepository extends JpaRepository<EpubSeries, Long>, JpaSpecificationExecutor<EpubSeries> {
    public Optional<EpubSeries> findByName(String name);
}
