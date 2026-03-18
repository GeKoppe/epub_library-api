package org.koppe.epub.api.epub_library_api.web.dto;

import java.util.List;

import org.koppe.epub.api.epub_library_api.jpa.model.EpubMetadata.LanguageC;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class EpubMetadataDto {
    private Long id;
    private Long editionId;
    private Integer numberOfPages;
    private List<TableOfContentLineDto> tableOfContents;
    private LanguageC language;
}
