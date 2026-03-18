package org.koppe.epub.api.epub_library_api.jpa.repository;

import java.util.Optional;

import org.koppe.epub.api.epub_library_api.jpa.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query("""
            select u from User u
            where lower(u.name) like lower(:name)
            """)
    public Optional<User> findByName(String name);
}
