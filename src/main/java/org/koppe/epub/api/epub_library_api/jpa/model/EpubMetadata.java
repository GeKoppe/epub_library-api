package org.koppe.epub.api.epub_library_api.jpa.model;

import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode.Include;

@Entity
@Table(name = "epub_metadata")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EpubMetadata {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Include
    @lombok.ToString.Include
    private Long id;

    @Column(name = "number_of_pages", nullable = true)
    @lombok.ToString.Include
    private Integer numberOfPages;

    @Column(name = "publisher", nullable = true)
    @lombok.ToString.Include
    private String publisher;

    @Column(name = "book_id", nullable = true)
    @lombok.ToString.Include
    private String bookId;

    @Column(name = "internal_title", nullable = true)
    @lombok.ToString.Include
    private String internalTitle;

    @Column(name = "language", nullable = true)
    @lombok.ToString.Include
    private LanguageC language;

    @OneToMany(mappedBy = "metadata", fetch = FetchType.LAZY, cascade = CascadeType.REMOVE)
    @OrderBy("sort_chapter asc")
    @lombok.ToString.Include
    private List<TableOfContentLine> tableOfContents;

    @OneToOne(optional = false)
    @JoinColumn(name = "edition_id", unique = true, nullable = true)
    private EpubEdition edition;

    @RequiredArgsConstructor
    @Getter
    public static enum LanguageC {
        GERMAN("de"),
        ENGLISH("en"),
        SPANISH("es"),
        OTHER("OTHER");

        private final String languageCode;
    }
}
