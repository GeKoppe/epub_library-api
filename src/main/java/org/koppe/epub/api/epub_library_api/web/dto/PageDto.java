package org.koppe.epub.api.epub_library_api.web.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode.Include;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class PageDto <T> {
    @Include
    private Integer number;
    @Include
    private List<T> content;
    @Include
    private Long itemCount;
    @Include
    private Integer pageSize;
}
