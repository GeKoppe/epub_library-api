package org.koppe.epub.api.epub_library_api.web.dto;

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
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class EpubEditionDto {
    @Include
    @lombok.ToString.Include
    private Long id;
    @Include
    @lombok.ToString.Include
    private String versionName;
    @lombok.ToString.Include
    private Long epubId;
    @lombok.ToString.Include
    private String downloadGuid;
    @lombok.ToString.Include
    private String uploadGuid;
    @lombok.ToString.Include
    private EpubMetadataDto metadata;
}
