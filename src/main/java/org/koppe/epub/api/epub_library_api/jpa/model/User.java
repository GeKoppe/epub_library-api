package org.koppe.epub.api.epub_library_api.jpa.model;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode.Include;

@Entity
@Table(name = "users")
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(onlyExplicitlyIncluded = true)
public class User {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Include
    @lombok.ToString.Include
    private Long id;

    @Column(name = "name", nullable = false, unique = true)
    @Include
    @lombok.ToString.Include
    private String name;

    @Column(name = "pw_hash", nullable = false)
    private String pwHash;

    @Column(name = "creation_date", nullable = false)
    @lombok.ToString.Include
    private LocalDate creationDate;
}
