package org.koppe.epub.api.epub_library_api.jpa.model;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode.Include;

@Entity
@Table(name = "epub_editions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EpubEdition {
    @Id
    @Column(name = "id", nullable = false)
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Include
    @lombok.ToString.Include
    private Long id;

    @Column(name = "version_name", nullable = false)
    @Include
    @lombok.ToString.Include
    private String versionName;

    @Column(name = "base_file_path", nullable = false)
    @Include
    @lombok.ToString.Include
    private String baseFilePath;

    @Column(name = "upload_guid", nullable = false)
    private String uploadGuid;

    @Column(name = "download_guid", nullable = false)
    private String downloadGuid;

    @Column(name = "original_file_name", nullable = true)
    @lombok.ToString.Include
    private String originalFileName;

    @ManyToOne(optional = false)
    @JoinColumn(name = "epub_id", nullable = false)
    private Epub epub;

    @OneToOne(optional = true, mappedBy = "edition", fetch = FetchType.EAGER, cascade = CascadeType.REMOVE)
    @lombok.ToString.Include
    private EpubMetadata metadata;
}
