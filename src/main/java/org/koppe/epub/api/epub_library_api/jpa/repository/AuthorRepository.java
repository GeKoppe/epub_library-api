package org.koppe.epub.api.epub_library_api.jpa.repository;

import java.util.List;

import org.koppe.epub.api.epub_library_api.jpa.model.Author;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/**
 * JPA Repository to work with authors in the database
 */
public interface AuthorRepository extends JpaRepository<Author, Long>, JpaSpecificationExecutor<Author> {
    /**
     * Finds all authors with given surname
     * 
     * @param surname Surname of the authors to be found
     * @return List of all authors with the given surname
     */
    public List<Author> findAllBySurname(String surname);
}
