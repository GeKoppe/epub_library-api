package org.koppe.epub.api.epub_library_api.jpa.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.HashSet;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.koppe.epub.api.epub_library_api.jpa.model.Epub;
import org.koppe.epub.api.epub_library_api.jpa.model.EpubEdition;
import org.koppe.epub.api.epub_library_api.jpa.model.EpubMetadata;
import org.koppe.epub.api.epub_library_api.jpa.model.EpubMetadata.LanguageC;

public class EpubExtractorTest {
    private static File file;
    private static EpubEdition ed;

    @BeforeAll
    public static void beforeAll() throws IOException {
        file = new File(EpubExtractorTest.class.getClassLoader().getResource("test.epub").getPath());
        if (!file.exists()) {
            throw new IOException("Test file does not exist");
        }

        Epub epub = new Epub(1L, "Test", LocalDate.of(2000, 1, 1), LocalDate.of(2000, 1, 1), new HashSet<>(),
                new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashSet<>());
        ed = new EpubEdition(1L, "Test edition", file.getParent(), "1234", "4321", "test.epub", "", epub, null);
    }

    @AfterAll
    public static void afterAll() {
        File parent = file.getParentFile();

        for (var x : parent.list()) {
            if (x.contains("cover")) {
                File coverImage = new File(parent.getAbsolutePath() + "/" + x);
                if (coverImage.exists())
                    coverImage.delete();
                break;
            }
        }
    }

    // TODO build a test epub from which the toc can be read
    @Test
    public void test() {
        EpubExtractor ex = new EpubExtractor(file, ed);
        EpubMetadata m = ex.extract();

        assertEquals("TEST", m.getInternalTitle());
        assertEquals("Not Published", m.getPublisher());
        assertEquals(ed, m.getEdition());
        assertEquals(LanguageC.ENGLISH, m.getLanguage());
        assertEquals(0, m.getTableOfContents().size());
    }
}
