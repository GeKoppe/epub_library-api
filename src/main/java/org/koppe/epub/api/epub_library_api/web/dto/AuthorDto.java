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
public class AuthorDto {
    @Include
    @lombok.ToString.Include
    private Long id;
    @Include
    @lombok.ToString.Include
    private String firstName;
    @Include
    @lombok.ToString.Include
    private String surname;
    @Include
    @lombok.ToString.Include
    private LocalDate birthDate;
    @Include
    @lombok.ToString.Include
    private LocalDate deathDate;
    @lombok.ToString.Include
    private String description;

    private Set<EpubDto> epubs;
    private Set<GenreDto> genres;
}
