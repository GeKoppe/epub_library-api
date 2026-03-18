package org.koppe.epub.api.epub_library_api.web.dto;

import java.util.Set;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode.Include;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString(onlyExplicitlyIncluded = true)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FranchiseDto {
    @Include
    @lombok.ToString.Include
    private Long id;
    @Include
    @lombok.ToString.Include
    private String name;
    private Set<EpubDto> epubs;
}
