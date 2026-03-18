package org.koppe.epub.api.epub_library_api.jpa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.koppe.epub.api.epub_library_api.jpa.model.Author;
import org.koppe.epub.api.epub_library_api.jpa.model.Epub;
import org.koppe.epub.api.epub_library_api.jpa.model.Genre;
import org.koppe.epub.api.epub_library_api.jpa.repository.AuthorRepository;
import org.koppe.epub.api.epub_library_api.jpa.repository.EpubEditionRepository;
import org.koppe.epub.api.epub_library_api.jpa.repository.EpubMetadataRepository;
import org.koppe.epub.api.epub_library_api.jpa.repository.EpubRepository;
import org.koppe.epub.api.epub_library_api.jpa.repository.TableOfContentLineRepository;
import org.koppe.epub.api.epub_library_api.web.dto.AuthorDto;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({ "unused" })
public class AuthorServiceTest {

    private AuthorRepository repo = mock(AuthorRepository.class);
    private EpubRepository epubRepo = mock(EpubRepository.class);
    private EpubEditionRepository editions = mock(EpubEditionRepository.class);
    private TableOfContentLineRepository tocs = mock(TableOfContentLineRepository.class);
    private EpubMetadataRepository metadata = mock(EpubMetadataRepository.class);
    private SharedService shared = mock(SharedService.class);

    @InjectMocks
    private AuthorService srv;

    private Author a1;
    private Epub b1;
    private Genre g1;

    @BeforeEach
    public void setup() {
        a1 = new Author(1L, "Test", "Testerson", "Does test literature", LocalDate.of(2020, 1, 1), null, null, null);
        b1 = new Epub(1L, "Test", LocalDate.of(2000, 1, 1), LocalDate.now(), Set.of(a1), null, null, Set.of(),
                Set.of(), Set.of());
        g1 = new Genre(1L, "Horror", "Horror", null, Set.of(a1), null, new HashSet<>());
        a1.setBooks(Set.of(b1));
        a1.setGenres(Set.of(g1));
    }

    // #region exists by id
    @Test
    public void testExistsById() {
        assertThrows(IllegalArgumentException.class, () -> srv.existsById(null));
        when(repo.existsById(5L)).thenReturn(false);
        when(repo.existsById(1L)).thenReturn(true);

        assertTrue(srv.existsById(1L));
        assertFalse(srv.existsById(5L));
    }

    // #region find by id
    @Test
    public void testFindById() {
        assertThrows(IllegalArgumentException.class, () -> srv.findById(null));
        assertThrows(IllegalArgumentException.class, () -> srv.findById(-1L));

        when(repo.findById(1L)).thenReturn(Optional.of(a1));
        when(repo.findById(5L)).thenReturn(Optional.empty());

        assertNull(srv.findById(5L));

        Author auth = srv.findById(1L);
        assertNotNull(auth);
        assertEquals("Test", auth.getFirstName());
        assertEquals("Testerson", auth.getSurname());

        auth = srv.findById(1L, true);
        assertNotNull(auth);

        auth = srv.findById(1L, true, true);
        assertNotNull(auth);
    }

    // #region add author
    @Test
    public void testAddAuthor() {
        assertThrows(IllegalArgumentException.class, () -> srv.addAuthor(null));

        a1.setFirstName(null);
        assertThrows(IllegalArgumentException.class, () -> srv.addAuthor(a1));

        a1.setFirstName("  ");
        assertThrows(IllegalArgumentException.class, () -> srv.addAuthor(a1));

        a1.setFirstName("Test");
        a1.setSurname(null);
        assertThrows(IllegalArgumentException.class, () -> srv.addAuthor(a1));

        a1.setSurname("   ");
        assertThrows(IllegalArgumentException.class, () -> srv.addAuthor(a1));

        a1.setSurname("Testerson");

        when(repo.save(any(Author.class))).thenReturn(a1);
        assertEquals(a1, srv.addAuthor("Test", "Testerson"));
        assertEquals(a1, srv.addAuthor("Test", "Testerson", "Tests things"));
        assertEquals(a1, srv.addAuthor(a1));

        assertThrows(IllegalArgumentException.class, () -> srv.addAuthor(null, null));
        assertThrows(IllegalArgumentException.class, () -> srv.addAuthor("    ", null));
        assertThrows(IllegalArgumentException.class, () -> srv.addAuthor(null, "   "));
        assertThrows(IllegalArgumentException.class, () -> srv.addAuthor("    ", "   "));

        assertThrows(IllegalArgumentException.class, () -> srv.addAuthor(null, null, ""));
        assertThrows(IllegalArgumentException.class, () -> srv.addAuthor("    ", null, ""));
        assertThrows(IllegalArgumentException.class, () -> srv.addAuthor(null, "   ", ""));
        assertThrows(IllegalArgumentException.class, () -> srv.addAuthor("    ", "   ", ""));
    }

    // #region delete by id
    @Test
    public void testDeleteById() {
        assertThrows(IllegalArgumentException.class, () -> srv.deleteById(null, false));

        when(repo.existsById(5L)).thenReturn(false);
        when(repo.existsById(1L)).thenReturn(true);
        assertNull(srv.deleteById(5L, false));

        when(repo.findById(1L)).thenReturn(Optional.of(a1));
        when(shared.deleteEpub(anyLong())).thenReturn(b1);
        doNothing().when(repo).delete(any(Author.class));

        Author deletedWithBooks = srv.deleteById(1L, true);
        assertEquals(a1, deletedWithBooks);
        assertEquals(1, deletedWithBooks.getBooks().size());

        Author deletedWithoutBooks = srv.deleteById(1L, false);
        assertEquals(a1, deletedWithoutBooks);
        assertEquals(0, deletedWithoutBooks.getBooks().size());
    }

    // #region update author
    @Test
    public void testUpdateAuthor() {
        AuthorDto dto = new AuthorDto(1L, "Test", "Test", LocalDate.of(2000, 2, 2), LocalDate.of(2000, 2, 2), "Test",
                null, null);

        when(repo.existsById(1L)).thenReturn(true);
        when(repo.existsById(5L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> srv.updateAuthor(null, false));

        dto.setId(null);
        assertThrows(IllegalArgumentException.class, () -> srv.updateAuthor(dto, false));

        dto.setId(5L);
        assertThrows(IllegalArgumentException.class, () -> srv.updateAuthor(dto, false));

        dto.setId(1L);
        dto.setFirstName(null);
        assertThrows(IllegalArgumentException.class, () -> srv.updateAuthor(dto, true));

        dto.setFirstName("");
        assertThrows(IllegalArgumentException.class, () -> srv.updateAuthor(dto, true));

        dto.setFirstName("   ");
        assertThrows(IllegalArgumentException.class, () -> srv.updateAuthor(dto, true));

        dto.setFirstName("Test");
        dto.setSurname(null);
        assertThrows(IllegalArgumentException.class, () -> srv.updateAuthor(dto, true));

        dto.setSurname("");
        assertThrows(IllegalArgumentException.class, () -> srv.updateAuthor(dto, true));

        dto.setSurname("   ");
        assertThrows(IllegalArgumentException.class, () -> srv.updateAuthor(dto, true));

        dto.setSurname("Test");
        when(repo.findById(1L)).thenReturn(Optional.of(a1));
    }

    @Test
    public void testAddBook() {
        when(repo.existsById(1L)).thenReturn(true);
        when(repo.existsById(5L)).thenReturn(false);
        when(shared.epubExistsById(1L)).thenReturn(true);
        when(shared.epubExistsById(5L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> srv.addBook(5L, 1L));
        assertThrows(IllegalArgumentException.class, () -> srv.addBook(5L, 5L));
        assertThrows(IllegalArgumentException.class, () -> srv.addBook(1L, 5L));

        when(shared.addEpubToAuthor(1L, 1L)).thenReturn(a1);
        assertNotNull(srv.addBook(1L, 1L));
    }

    @Test
    public void testRemoveEpubFromAuthor() {
        when(repo.existsById(1L)).thenReturn(true);
        when(repo.existsById(5L)).thenReturn(false);
        when(shared.epubExistsById(1L)).thenReturn(true);
        when(shared.epubExistsById(5L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> srv.addBook(5L, 1L));
        assertThrows(IllegalArgumentException.class, () -> srv.addBook(5L, 5L));
        assertThrows(IllegalArgumentException.class, () -> srv.addBook(1L, 5L));

        when(shared.removeEpubFromAuthor(1L, 1L)).thenReturn(a1);
        assertNotNull(srv.removeEpubFromAuthor(1L, 1L));
    }
}
