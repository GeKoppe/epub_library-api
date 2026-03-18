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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode.Include;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "epubs")
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Epub {

    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Include
    @lombok.ToString.Include
    private Long id;

    @Column(name = "title", nullable = false, unique = true)
    @Include
    @lombok.ToString.Include
    private String title;

    @Column(name = "publishing_date", nullable = true)
    @Include
    @lombok.ToString.Include
    private LocalDate publishingDate;

    @Column(name = "upload_date", nullable = false)
    @Include
    @lombok.ToString.Include
    private LocalDate uploadDate;

    @ManyToMany(mappedBy = "books", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private Set<Author> authors;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "book_genres", joinColumns = @JoinColumn(name = "epub_id", nullable = false), inverseJoinColumns = @JoinColumn(name = "genre_id", nullable = false))
    private Set<Genre> genres;

    @OneToMany(mappedBy = "epub", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @lombok.ToString.Include
    private Set<EpubEdition> editions;

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinTable(name = "book_tags", joinColumns = @JoinColumn(name = "tag_id", nullable = false), inverseJoinColumns = @JoinColumn(name = "epub_id", nullable = false))
    private Set<Tag> tags;

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinTable(name = "book_series", joinColumns = @JoinColumn(name = "series_id", nullable = false), inverseJoinColumns = @JoinColumn(name = "epub_id", nullable = false))
    private Set<EpubSeries> series;

    @ManyToMany(mappedBy = "epubs", fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    private Set<Franchise> franchises;
}
