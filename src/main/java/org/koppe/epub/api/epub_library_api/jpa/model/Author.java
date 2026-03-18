package org.koppe.epub.api.epub_library_api.jpa.model;

import java.time.LocalDate;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode.Include;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "authors")
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Author {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Include
    @lombok.ToString.Include
    private Long id;

    @Column(name = "first_name", nullable = false)
    @Include
    @lombok.ToString.Include
    private String firstName;

    @Column(name = "surname", nullable = false)
    @Include
    @lombok.ToString.Include
    private String surname;

    @Column(name = "description", nullable = true)
    @lombok.ToString.Include
    private String description;

    @Column(name = "birth_date", nullable = true)
    @Include
    @lombok.ToString.Include
    private LocalDate birthDate;

    @Column(name = "death_date", nullable = true)
    @Include
    @lombok.ToString.Include
    private LocalDate deathDate;

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinTable(name = "author_genres", joinColumns = @JoinColumn(name = "author_id", nullable = false), inverseJoinColumns = @JoinColumn(name = "genre_id", nullable = false))
    private Set<Genre> genres;

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinTable(name = "author_books", joinColumns = @JoinColumn(name = "author_id", nullable = false), inverseJoinColumns = @JoinColumn(name = "book_id", nullable = false))
    private Set<Epub> books;
}
