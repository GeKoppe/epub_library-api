package org.koppe.epub.api.epub_library_api.web.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class TableOfContentLineDto {
    private Long id;
    private Long metadataId;
    private String chapterName;
    private String chapterNumber;
    private Integer page;
}
