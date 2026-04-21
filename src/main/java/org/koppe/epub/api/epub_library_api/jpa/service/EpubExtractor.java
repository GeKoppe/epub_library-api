package org.koppe.epub.api.epub_library_api.jpa.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.koppe.epub.api.epub_library_api.jpa.model.EpubEdition;
import org.koppe.epub.api.epub_library_api.jpa.model.EpubMetadata;
import org.koppe.epub.api.epub_library_api.jpa.model.TableOfContentLine;
import org.koppe.epub.api.epub_library_api.jpa.model.EpubMetadata.LanguageC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import lombok.RequiredArgsConstructor;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.Metadata;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.TOCReference;
import nl.siegmann.epublib.domain.TableOfContents;
import nl.siegmann.epublib.epub.EpubReader;

/**
 * Class for extracting metadta from a given epub.
 */
@RequiredArgsConstructor
public class EpubExtractor {
    /**
     * Logger
     */
    private final Logger logger = LoggerFactory.getLogger(EpubExtractor.class);
    /**
     * Epub file
     */
    private final File epubFile;
    /**
     * Epub edition for which the extraction should take place
     */
    private final EpubEdition edition;
    /**
     * Metadata object to be filled
     */
    private EpubMetadata metadata = new EpubMetadata();
    /**
     * Counter that orders the chapters and subchapters
     */
    private Integer chapterCounter = 0;

    // #region extract
    /**
     * Central logic method. Extracts all necessary information from the epub
     * 
     * @return Extracted metadata or null, if an error occurred
     */
    public EpubMetadata extract() {
        logger.info("Startin extraction of metadata from {}", epubFile);
        metadata.setEdition(edition);
        return extractAutomatically();
    }

    /**
     * Extracts the metadata of the book via nl.siegmann epub lib
     * 
     * @return Extracted metadata
     */
    private EpubMetadata extractAutomatically() {
        Book book = null;
        try {
            book = new EpubReader().readEpub(new FileInputStream(epubFile));
        } catch (IOException e) {
            logger.warn("Exception occurred while reading the epub", e);
            return null;
        }

        logger.info("Epub successfully read, starting extraction of basic metadata");
        Metadata meta = book.getMetadata();
        metadata.setBookId(meta.getIdentifiers().get(0).getValue());
        logger.debug("Extracted book id {}", metadata.getBookId());

        try {
            metadata.setInternalTitle(meta.getFirstTitle());
            logger.debug("Extracted internal title {}", metadata.getInternalTitle());
        } catch (Exception ex) {
            logger.info("Could not extract internal title");
        }

        try {
            metadata.setPublisher(meta.getPublishers().get(0));
            logger.debug("Extracted publisher {}", metadata.getPublisher());
        } catch (Exception ex) {
            logger.info("Could not get publisher from epub");
        }

        if (meta.getLanguage().contains("de"))
            metadata.setLanguage(LanguageC.GERMAN);
        else if (meta.getLanguage().contains("en"))
            metadata.setLanguage(LanguageC.ENGLISH);
        else if (meta.getLanguage().contains("es"))
            metadata.setLanguage(LanguageC.SPANISH);
        else
            metadata.setLanguage(LanguageC.OTHER);

        logger.debug("Book is written in {}", metadata.getLanguage());
        logger.info("Successfully extracted basic metadata {}", metadata);

        TableOfContents toc = book.getTableOfContents();
        List<TOCReference> tocRefs = toc.getTocReferences();
        List<TableOfContentLine> tocLines = new ArrayList<>();

        int currentChapter = 1;
        logger.info("Starting to analyse table of contents");
        for (TOCReference ref : tocRefs) {
            analyseTocRef(tocLines, ref, 0, new int[] { currentChapter });
            currentChapter++;
        }
        metadata.setTableOfContents(tocLines);
        logger.info("Successfully analysed table of contents {}", tocLines);

        try {
            downloadCoverImage(book);
        } catch (Exception ex) {
            logger.warn("Exception occurred while downloading cover image", ex);
        }
        return metadata;
    }

    // #region analyse toc ref
    /**
     * Analyses table of content references recursively
     * 
     * @param lines   List of toc lines to add the analysed references to
     * @param ref     Current reference to analyse
     * @param level   Level of the recursion
     * @param chapter Current chapter
     */
    private void analyseTocRef(List<TableOfContentLine> lines, TOCReference ref, int level, int[] chapter) {
        chapterCounter++;
        logger.debug("Analysing toc reference {} at depth {}", ref, level);
        TableOfContentLine current = new TableOfContentLine();
        current.setChapterName(ref.getTitle());
        current.setChapterCounter(chapterCounter);

        String chapterString = "";
        for (int i = 0; i <= level; i++) {
            if (i != 0)
                chapterString += ".";
            chapterString += chapter[i];
        }
        current.setChapterNumber(chapterString);
        current.setMetadata(metadata);
        lines.add(current);
        logger.debug("Added toc line {}", current);

        int subchapter = 1;
        for (TOCReference child : ref.getChildren()) {
            int[] newChapters = new int[chapter.length + 1];
            for (int j = 0; j < chapter.length; j++)
                newChapters[j] = chapter[j];
            newChapters[chapter.length] = subchapter;
            analyseTocRef(lines, child, level + 1, newChapters);
            subchapter++;
        }
    }

    // #region download cover image
    /**
     * Downloads cover image to the same path as the epub
     * 
     * @param book Book to download cover image from
     * @throws IOException If creating the file or copying the input stream failed
     */
    private void downloadCoverImage(Book book) throws IOException {
        Resource rec = book.getCoverImage();
        File cover = new File(
                epubFile.getParentFile().getAbsolutePath() + "/cover" + rec.getMediaType().getDefaultExtension());

        logger.info("Trying to download cover image to {}", cover);
        if (!cover.exists()) {
            logger.debug("Cover file does not yet exist, creating");
            cover.createNewFile();
        } else {
            cover.delete();
            cover.createNewFile();
        }

        try (OutputStream os = new FileOutputStream(cover)) {
            IOUtils.copy(rec.getInputStream(), os);
        }
    }
}
