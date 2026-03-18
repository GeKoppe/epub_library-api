package org.koppe.epub.api.epub_library_api.jpa.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode.Include;

@Entity
@Table(name = "table_of_contents")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TableOfContentLine {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Include
    @lombok.ToString.Include
    private Long id;

    @Column(name = "chapter_number", nullable = false)
    @lombok.ToString.Include
    private String chapterNumber;

    @Column(name = "chapter_name", nullable = false)
    @lombok.ToString.Include
    private String chapterName;

    @Column(name = "page", nullable = true)
    @lombok.ToString.Include
    private Integer page;

    @Column(name = "sort_chapter", nullable = false)
    @lombok.ToString.Include
    private Integer chapterCounter;

    @ManyToOne(optional = false, fetch = FetchType.EAGER)
    @JoinColumn(name = "metadata_id", nullable = false)
    private EpubMetadata metadata;
}
