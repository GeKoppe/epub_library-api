package org.koppe.epub.api.epub_library_api.jpa.repository;

import java.util.Optional;

import org.koppe.epub.api.epub_library_api.jpa.model.Franchise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface FranchiseRepository extends JpaRepository<Franchise, Long>, JpaSpecificationExecutor<Franchise> {
    Optional<Franchise> findByName(String name);
}
