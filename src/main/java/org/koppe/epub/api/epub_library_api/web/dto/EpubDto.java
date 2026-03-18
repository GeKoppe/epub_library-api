package org.koppe.epub.api.epub_library_api.web.dto;

import java.time.LocalDate;
import java.util.Set;

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
public class EpubDto {
    @Include
    @lombok.ToString.Include
    private Long id;
    @Include
    @lombok.ToString.Include
    private String title;
    @Include
    @lombok.ToString.Include
    private LocalDate publishDate;
    @Include
    @lombok.ToString.Include
    private LocalDate uploadDate;
    private Set<AuthorDto> authors;
    private Set<GenreDto> genres;
    private Set<EpubEditionDto> editions;
    private Set<TagDto> tags;
    private Set<EpubSeriesDto> series;
}
