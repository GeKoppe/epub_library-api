package org.koppe.epub.api.epub_library_api.utility;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.koppe.epub.api.epub_library_api.jpa.model.Author;
import org.koppe.epub.api.epub_library_api.jpa.model.Epub;
import org.koppe.epub.api.epub_library_api.jpa.model.Genre;
import org.koppe.epub.api.epub_library_api.web.dto.AuthorDto;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({ "unused" })
public class DtoFactoryTest {
    private Author a1;
    private Author a2;
    private Author a3;

    private Epub e1;
    private Epub e2;
    private Epub e3;

    private Genre g1;
    private Genre g2;
    private Genre g3;

    @BeforeEach
    public void beforeEach() {
        e1 = new Epub(1L, "Test epub", LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 1), Set.of(), Set.of(),
                Set.of(), new HashSet<>(), new HashSet<>(), new HashSet<>());
        g1 = new Genre(1L, "Test genre", "Tests things", Set.of(), Set.of(), null, new HashSet<>());
        a1 = new Author(1L, "Test", "Testerson", "Author of test literature", LocalDate.of(2000, 1, 1), null,
                Set.of(g1), Set.of(e1));

        a2 = new Author(2L, "Test2", "Testerson2", "Author of test literature", LocalDate.of(2000, 1, 1), null,
                Set.of(), Set.of());
        e2 = new Epub(2L, "Test epub 2", LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 1), Set.of(),
                Set.of(), Set.of(), new HashSet<>(), new HashSet<>(), new HashSet<>());
        g2 = new Genre(2L, "Test genre 2", "Tests things", Set.of(e2), Set.of(a2), null, new HashSet<>());

        a3 = new Author(3L, "Test3", "Testerson3", "Author of test literature", LocalDate.of(2000, 1, 1), null,
                Set.of(), Set.of());
        g3 = new Genre(3L, "Test genre 3", "Tests things", Set.of(), Set.of(), null, new HashSet<>());
        e3 = new Epub(3L, "Test epub 3", LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 1), Set.of(a3),
                Set.of(g3), Set.of(), new HashSet<>(), new HashSet<>(), new HashSet<>());
    }

    // #region authors
    @Test
    public void testConvertSingleAuthor() {
        assertThrows(IllegalArgumentException.class, () -> DtoFactory.convertAuthorToDto(null, false, false));

        AuthorDto dtoWithoutBooksAndGenres = DtoFactory.convertAuthorToDto(a1, false, false);
        assertEquals(1L, dtoWithoutBooksAndGenres.getId());
        assertEquals(0, dtoWithoutBooksAndGenres.getEpubs().size());
        assertEquals(0, dtoWithoutBooksAndGenres.getGenres().size());

        AuthorDto dtoWithoutBooks = DtoFactory.convertAuthorToDto(a1, false, true);
        assertEquals(1L, dtoWithoutBooks.getId());
        assertEquals(0, dtoWithoutBooks.getEpubs().size());
        assertEquals(1, dtoWithoutBooks.getGenres().size());

        AuthorDto dtoWithoutGenres = DtoFactory.convertAuthorToDto(a1, true, false);
        assertEquals(1L, dtoWithoutGenres.getId());
        assertEquals(1, dtoWithoutGenres.getEpubs().size());
        assertEquals(0, dtoWithoutGenres.getGenres().size());

        AuthorDto fullDto = DtoFactory.convertAuthorToDto(a1, true, true);
        assertEquals(1L, fullDto.getId());
        assertEquals(1, fullDto.getEpubs().size());
        assertEquals(1, fullDto.getGenres().size());
    }

    @Test
    public void testConvertMultipleAuthors() {
        Set<Author> a = new HashSet<>();
        a.add(null);
        assertThrows(IllegalArgumentException.class,
                () -> DtoFactory.convertMultipleAuthorsToDto((Set<Author>) null, false, false));
        assertThrows(IllegalArgumentException.class,
                () -> DtoFactory.convertMultipleAuthorsToDto(a, false, false));

        Set<AuthorDto> dtosWithoutBooks = DtoFactory.convertMultipleAuthorsToDto(Set.of(a1, a2, a3), false,
                true);
        assertEquals(3, dtosWithoutBooks.size());

        int authorsWithGenres = 0;
        for (var x : dtosWithoutBooks) {
            assertEquals(0, x.getEpubs().size());
            if (x.getGenres().size() > 0)
                authorsWithGenres++;
        }

        assertEquals(1, authorsWithGenres);

        Set<AuthorDto> dtosWithoutGenres = DtoFactory.convertMultipleAuthorsToDto(Set.of(a1, a2, a3), true,
                false);
        assertEquals(3, dtosWithoutGenres.size());

        int authorsWithBooks = 0;
        for (var x : dtosWithoutGenres) {
            assertEquals(0, x.getGenres().size());
            if (x.getEpubs().size() > 0)
                authorsWithBooks++;
        }

        assertEquals(1, authorsWithBooks);

        Set<AuthorDto> fullDtos = DtoFactory.convertMultipleAuthorsToDto(Set.of(a1, a2, a3), true, true);
        authorsWithBooks = 0;
        authorsWithGenres = 0;
        for (var x : fullDtos) {
            if (x.getEpubs().size() > 0)
                authorsWithBooks++;
            if (x.getEpubs().size() > 0)
                authorsWithGenres++;
        }

        assertEquals(1, authorsWithBooks);
        assertEquals(1, authorsWithGenres);

        List<Author> authorList = List.of(a1, a2, a3);
        List<Author> nullList = new ArrayList<>();
        nullList.add(null);
        assertThrows(IllegalArgumentException.class,
                () -> DtoFactory.convertMultipleAuthorsToDto((List<Author>) null, false, false));
        assertThrows(IllegalArgumentException.class,
                () -> DtoFactory.convertMultipleAuthorsToDto(nullList, false, false));

        List<AuthorDto> dtoList = DtoFactory.convertMultipleAuthorsToDto(authorList, false, false);
        assertEquals(3, dtoList.size());
    }

    // #region genres
}
