package org.koppe.epub.api.epub_library_api.jpa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.koppe.epub.api.epub_library_api.exceptions.MediaTypeException;
import org.koppe.epub.api.epub_library_api.jpa.model.Author;
import org.koppe.epub.api.epub_library_api.jpa.model.Epub;
import org.koppe.epub.api.epub_library_api.jpa.model.EpubEdition;
import org.koppe.epub.api.epub_library_api.jpa.model.EpubMetadata;
import org.koppe.epub.api.epub_library_api.jpa.model.TableOfContentLine;
import org.koppe.epub.api.epub_library_api.jpa.model.EpubMetadata.LanguageC;
import org.koppe.epub.api.epub_library_api.jpa.repository.EpubEditionRepository;
import org.koppe.epub.api.epub_library_api.jpa.repository.EpubMetadataRepository;
import org.koppe.epub.api.epub_library_api.jpa.repository.EpubRepository;
import org.koppe.epub.api.epub_library_api.jpa.repository.TableOfContentLineRepository;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({ "unused" })
public class EpubServiceTest {
    private final EpubRepository epubs = mock(EpubRepository.class);
    private final EpubEditionRepository editions = mock(EpubEditionRepository.class);
    private final EpubMetadataRepository metadata = mock(EpubMetadataRepository.class);
    private final TableOfContentLineRepository tocs = mock(TableOfContentLineRepository.class);

    @InjectMocks
    private EpubService srv;

    private Epub e1;
    private Author a1;
    private EpubEdition ed1;
    private EpubMetadata m1;
    private TableOfContentLine t1;
    private TableOfContentLine t2;

    private static File testEpub;

    @BeforeAll
    public static void beforeAll() throws IOException {
        testEpub = new File(EpubServiceTest.class.getClassLoader().getResource("test.epub").getPath());
        if (!testEpub.exists()) {
            throw new IOException("Test file does not exist");
        }
    }

    @BeforeEach
    public void BeforeEach() {
        a1 = new Author(1L, "Test", "Testerson", "Tests literature", LocalDate.of(2000, 1, 1), null, new HashSet<>(),
                new HashSet<>());
        t1 = new TableOfContentLine(1L, "1", "Test", 1, 0, null);
        t2 = new TableOfContentLine(2L, "1.1", "Test2", 2, 1, null);

        m1 = new EpubMetadata(1L, 1, "Test", "Test", "Test", LanguageC.ENGLISH, List.of(t1, t2), null);
        ed1 = new EpubEdition(1L, "First edition", testEpub.getParentFile().getAbsolutePath() + "/test", "123", "321",
                "Original file name", null, m1);

        e1 = new Epub(1L, "Test", LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 1), null, null, Set.of(ed1),
                new HashSet<>(), new HashSet<>(), new HashSet<>());
    }

    // #region epub exists by id
    @Test
    public void testExistsById() {
        assertThrows(IllegalArgumentException.class, () -> srv.existsById(null));

        when(epubs.existsById(1L)).thenReturn(true);
        when(epubs.existsById(5L)).thenReturn(false);

        assertTrue(srv.existsById(1L));
        assertFalse(srv.existsById(5L));
    }

    // #region find epub by id
    @Test
    public void testFindEpubById() {
        assertThrows(IllegalArgumentException.class, () -> srv.findEpubById(null));

        when(epubs.findById(1L)).thenReturn(Optional.of(e1));
        when(epubs.findById(5L)).thenReturn(Optional.empty());

        assertNull(srv.findEpubById(5L));
        Epub epub = srv.findEpubById(1L);

        assertEquals("Test", epub.getTitle());
        assertTrue(epub.getEditions().toArray(EpubEdition[]::new)[0] != null);

        EpubEdition ed = epub.getEditions().toArray(EpubEdition[]::new)[0];
        assertEquals(ed1, ed);
    }

    // #region add book
    @Test
    public void testAddBook() {
        Epub epub = new Epub();
        assertThrows(IllegalArgumentException.class, () -> srv.addBook((Epub) null));
        assertThrows(IllegalArgumentException.class, () -> srv.addBook((String) null));

        assertThrows(IllegalArgumentException.class, () -> srv.addBook(epub));

        epub.setTitle("");
        assertThrows(IllegalArgumentException.class, () -> srv.addBook(epub));
        assertThrows(IllegalArgumentException.class, () -> srv.addBook(""));

        epub.setTitle("    ");
        assertThrows(IllegalArgumentException.class, () -> srv.addBook(epub));
        assertThrows(IllegalArgumentException.class, () -> srv.addBook("    "));

        when(epubs.save(any(Epub.class))).thenReturn(e1);
        epub.setTitle("Test");

        Epub saved = srv.addBook("Test");
        assertNotNull(saved);

        saved = srv.addBook(epub);
        assertNotNull(saved);
    }

    // #region edition exists by id
    @Test
    public void testEditionExistsById() {
        assertThrows(IllegalArgumentException.class, () -> srv.editionExistsByid(null, null));
        assertThrows(IllegalArgumentException.class, () -> srv.editionExistsByid(null, 1L));
        assertThrows(IllegalArgumentException.class, () -> srv.editionExistsByid(1L, null));

        ed1.setEpub(e1);
        when(editions.findById(1L)).thenReturn(Optional.of(ed1));
        when(editions.findById(5L)).thenReturn(Optional.empty());

        assertFalse(srv.editionExistsByid(1L, 5L));
        assertFalse(srv.editionExistsByid(5L, 1L));
        assertTrue(srv.editionExistsByid(1L, 1L));
    }

    // #region edition has metadata
    @Test
    public void testEditionHasMetadata() {
        assertThrows(IllegalArgumentException.class, () -> srv.editionHasMetadata(null));

        when(editions.findById(1L)).thenReturn(Optional.of(ed1));
        when(editions.existsById(5L)).thenReturn(false);
        when(editions.existsById(1L)).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> srv.editionHasMetadata(5L));

        assertTrue(srv.editionHasMetadata(1L));
        ed1.setMetadata(null);
        assertFalse(srv.editionHasMetadata(1L));
    }

    // #region add edition
    @Test
    public void testAddEdition() {
        when(epubs.existsById(1L)).thenReturn(true);
        when(epubs.existsById(5L)).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> srv.addEdition(null, null));
        assertThrows(IllegalArgumentException.class, () -> srv.addEdition(5L, null));
        assertThrows(IllegalArgumentException.class, () -> srv.addEdition(1L, null));

        EpubEdition ed = new EpubEdition();
        assertThrows(IllegalArgumentException.class, () -> srv.addEdition(1L, ed));

        ed.setVersionName("");
        assertThrows(IllegalArgumentException.class, () -> srv.addEdition(1L, ed));

        ed.setVersionName("     ");
        assertThrows(IllegalArgumentException.class, () -> srv.addEdition(1L, ed));

        when(epubs.findById(1L)).thenReturn(Optional.of(e1));
        when(editions.save(any(EpubEdition.class))).thenReturn(ed1);

        ed.setVersionName("Test");
        assertNotNull(srv.addEdition(1L, ed));
    }

    // #region find by upload guid
    @Test
    public void findByUploadGuid() {
        assertThrows(IllegalArgumentException.class, () -> srv.findEditionByUploadGuid(null));
        assertThrows(IllegalArgumentException.class, () -> srv.findEditionByUploadGuid(""));
        assertThrows(IllegalArgumentException.class, () -> srv.findEditionByUploadGuid("     "));

        when(editions.findAllByUploadGuid("123")).thenReturn(Set.of(ed1));
        when(editions.findAllByUploadGuid("321")).thenReturn(new HashSet<>());

        assertNull(srv.findEditionByUploadGuid("321"));
        assertEquals(ed1, srv.findEditionByUploadGuid("123"));
    }

    // #region find by download guid
    @Test
    public void testFindByDownloadGuid() {
        assertThrows(IllegalArgumentException.class, () -> srv.findEditionByDownloadGuid(null));
        assertThrows(IllegalArgumentException.class, () -> srv.findEditionByDownloadGuid(""));
        assertThrows(IllegalArgumentException.class, () -> srv.findEditionByDownloadGuid("     "));

        when(editions.findAllByDownloadGuid("123")).thenReturn(Set.of(ed1));
        when(editions.findAllByDownloadGuid("321")).thenReturn(new HashSet<>());

        assertNull(srv.findEditionByDownloadGuid("321"));
        assertEquals(ed1, srv.findEditionByDownloadGuid("123"));
    }

    @Test
    public void testUploadEpub() throws FileNotFoundException, IOException {
        assertThrows(IllegalArgumentException.class, () -> srv.uploadEpub(null, null));
        assertThrows(IllegalArgumentException.class, () -> srv.uploadEpub("", null));
        assertThrows(IllegalArgumentException.class, () -> srv.uploadEpub("    ", null));
        assertThrows(IllegalArgumentException.class, () -> srv.uploadEpub("Test", null));

        when(editions.findAllByUploadGuid("123")).thenReturn(Set.of(ed1));
        when(editions.findAllByUploadGuid("321")).thenReturn(new HashSet<>());

        try (InputStream is = new FileInputStream(testEpub)) {
            MultipartFile mpf = new MockMultipartFile("test.epub", is);

            assertThrows(IllegalArgumentException.class, () -> srv.uploadEpub("321", mpf));

            assertTrue(srv.uploadEpub("123", mpf));
            assertTrue(new File(ed1.getBaseFilePath() + "/cover.jpg").exists());
            assertTrue(new File(ed1.getBaseFilePath() + "/book.epub").exists());

            File dir = new File(ed1.getBaseFilePath());
            if (dir.exists()) {
                for (var x : dir.list()) {
                    new File(ed1.getBaseFilePath() + "/" + x).delete();
                }
                dir.delete();
            }
        } catch (IllegalArgumentException e) {
            fail();
        } catch (MediaTypeException e) {
            fail();
        }
    }
}
