package org.koppe.epub.api.epub_library_api.jpa.service;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.koppe.epub.api.epub_library_api.jpa.model.Epub;
import org.koppe.epub.api.epub_library_api.jpa.model.EpubEdition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import nl.siegmann.epublib.domain.Author;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.epub.EpubReader;

/**
 * Imports all epubs in a specified directory
 */
@Component
@RequiredArgsConstructor
public class AutoImporter {
    /**
     * Logger
     */
    private final Logger logger = LoggerFactory.getLogger(AutoImporter.class);
    /**
     * Path in which all the epubs that should be imported are
     */
    private final String IMPORT_FILE_PATH = System.getenv("IMPORT_FILE_PATH") != null
            ? System.getenv("IMPORT_FILE_PATH") + "/epubs"
            : "./import";
    /**
     * Service for querying the database for epubs
     */
    private final EpubService epubs;
    private final AuthorService authors;
    private final EpubReader reader = new EpubReader();

    /**
     * Main execution wrapper. Imports all epubs from the file system.
     */
    @Scheduled(fixedRate = 15, timeUnit = TimeUnit.MINUTES)
    public void execute() {
        int imported = 0;
        logger.debug("Start executing import run at {}", new SimpleDateFormat("HH:mm:ss").format(new Date()));

        File[] filesToImport = getAllImportFiles();
        if (filesToImport == null || filesToImport.length == 0) {
            logger.info("No files for import found.");
            return;
        }
        logger.debug("Found {} files to import", filesToImport.length);

        for (File f : filesToImport) {
            if (importFile(f)) {
                imported++;
                handleSuccess(f);
            } else {
                handleError(f);
            }
        }

        logger.debug("Ended import run at {}. Imported {} of {} files",
                new SimpleDateFormat("HH:mm:ss").format(new Date()), imported, filesToImport.length);
    }

    // #region get all import files
    /**
     * Checks import directory for all new .epub files and returns them.
     * 
     * @return Array of all epub files in the import directory.
     */
    private @Nullable File[] getAllImportFiles() {
        File importDir = new File(IMPORT_FILE_PATH);
        if (!importDir.exists() || !importDir.isDirectory()) {
            logger.info("Import directory does not exist, creating it for use in next update run");
            if (!importDir.mkdir()) {
                logger.warn("Could not create import directory");
                return null;
            }
        }

        return importDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return !pathname.isDirectory()
                        && pathname.getName().contains(".")
                        && pathname.getName().substring(pathname.getName().lastIndexOf(".")).equals(".epub");
            }
        });
    }

    // #region import file
    private boolean importFile(@NotNull File file) {
        logger.info("Starting upload of file {}", file);
        if (!checkUploadPrerequisites(file)) {
            logger.info("One or more prerequisites for uploading have not been met by file {}");
            return false;
        }

        Book book = getBook(file);
        if (book == null) {
            logger.info("Could not read epub {}", file);
            return false;
        }

        Epub epub = getEpub(book);
        if (epub == null) {
            logger.info("Could not get or create new epub");
            return false;
        }

        EpubEdition edition = createEdition(epub);
        if (edition == null) {
            logger.info("Could not create an edition");
            return false;
        }

        return true;
    }

    private @NotNull EpubEdition createEdition(@NotNull Epub epub) {
        EpubEdition edition = new EpubEdition();
        edition.setEpub(epub);
        edition.setVersionName("Automatic import " + UUID.randomUUID().toString());
        return epubs.addEdition(epub.getId(), edition);
    }

    // #region get epub
    private @Nullable Epub getEpub(@NotNull Book book) {
        if (book == null) {
            logger.info("No book given");
            return null;
        }

        Epub epub = getOrCreateEpub(book.getTitle());
        if (epub == null) {
            logger.info("Could not get an epub");
            return null;
        }

        if (epub.getAuthors() == null || epub.getAuthors().isEmpty()) {
            logger.debug("Epub has no authors associated with it");
            List<org.koppe.epub.api.epub_library_api.jpa.model.Author> authors = getOrCreateAuthors(
                    book.getMetadata().getAuthors());
            Set<org.koppe.epub.api.epub_library_api.jpa.model.Author> a = new HashSet<>();
            a.addAll(authors);
            epub.setAuthors(a);
        }

        logger.info("Saving epub");
        if (epub.getId() != null)
            this.epubs.addBook(epub);
        else
            this.epubs.updateEpub(epub.getId(), epub, false);

        return epub;
    }

    // #region check prereqs
    /**
     * Checks, if the file meets prerequisites for upload to the system. Those are:
     * 
     * - File exists
     * - File is a .epub
     * - File is not in the system already
     * 
     * @param file File to be checked
     * @return True, if file meets prerequisites, false otherwise.
     */
    private boolean checkUploadPrerequisites(@NotNull File file) {
        if (file == null || !file.exists()) {
            logger.info("File {} does not exist", file);
            return false;
        }

        if (!(file.getName().contains(".")
                && file.getName().substring(file.getName().lastIndexOf(".")).equals(".epub"))) {
            logger.info("File {} is not an epub");
            return false;
        }

        if (checkExisting(file)) {
            logger.info("File {} already exists in the system, not uploading", file);
            return false;
        }
        return true;
    }

    // #region check existing
    /**
     * Checks, if a given file already exists in the system by comparing the md5
     * hash of the file with the database.
     * 
     * @param f File to check for in the system.
     * @return True, if file does not exist in the system, false otherwise
     */
    private boolean checkExisting(@NotNull File f) {
        logger.info("Checking if file {} already exists in the system", f);
        try {
            byte[] bytes = Files.readAllBytes(f.toPath());
            String md5 = DigestUtils.md5DigestAsHex(bytes).toUpperCase();

            logger.debug("Checking md5 {} against database", md5);
            EpubEdition ed = epubs.findEditionByMd5Hash(md5);
            logger.debug("Found edition {}", ed);

            return ed != null;
        } catch (Exception ex) {
            logger.warn("Exception occurred while checking if file {} already exists in the system", f, ex);
            return false;
        }
    }

    // #region get book
    /**
     * Reads the file into a usable book object
     * 
     * @param file
     * @return
     */
    private @Nullable Book getBook(@NotNull File file) {
        Book book = null;
        try {
            book = reader.readEpub(new FileInputStream(file));
        } catch (FileNotFoundException e) {
            logger.info("File {} does not exist", file);
            return null;
        } catch (IOException e) {
            logger.info("Exception occurred while reading the epub", e);
            return null;
        }

        return book;
    }

    // #region get or create epub
    private @Nullable Epub getOrCreateEpub(@NotNull String title) {
        Epub epub = null;
        if (title == null || title.isBlank()) {
            logger.info("No title given");
            return null;
        }

        List<Epub> epubs = this.epubs.findAllEpubs(title, null, null, null, null, null, null, null, null)
                .getContent();

        // TODO will definitely mismatch books. Change to matching algorithm
        if (epubs.size() == 0) {
            logger.debug("No epub with title {} exists in the system, creating a new one");
            epub = new Epub();
            epub.setTitle(title);
        } else {
            logger.info("An epub with the name {} exists");
            epub = epubs.get(0);
        }
        return epub;
    }

    private @Nullable List<org.koppe.epub.api.epub_library_api.jpa.model.Author> getOrCreateAuthors(
            @NotNull List<Author> authors) {
        if (authors == null || authors.isEmpty()) {
            logger.info("No authors given");
            return null;
        }

        List<org.koppe.epub.api.epub_library_api.jpa.model.Author> createdAuthors = new ArrayList<>();
        for (var x : authors) {
            org.koppe.epub.api.epub_library_api.jpa.model.Author created = null;
            List<org.koppe.epub.api.epub_library_api.jpa.model.Author> found = this.authors
                    .findAll(x.getFirstname(), x.getLastname(), null, null, null, null).getContent();

            if (found.isEmpty()) {
                logger.debug("No author for {} found, creating a new one", x);
                created = new org.koppe.epub.api.epub_library_api.jpa.model.Author();
                created.setFirstName(x.getFirstname());
                created.setSurname(x.getLastname());
            } else {
                logger.info("Found author {} for {}", found.get(0), x);
                created = found.get(0);
            }
            createdAuthors.add(created);
        }

        return createdAuthors;
    }

    // #region handle error
    /**
     * 
     * @param f
     */
    private void handleError(@NotNull File f) {
        logger.info("Failed to import file {}, moving to error", f);
        if (f == null || !f.exists()) {
            logger.info("File {} does not exist");
            return;
        }

        File errorDir = new File(IMPORT_FILE_PATH + "/error");
        if (!errorDir.exists() || !errorDir.isDirectory()) {
            logger.debug("Error directory does not exist, start creation");
            if (!errorDir.mkdir()) {
                logger.info("Could not create error direcotry");
                return;
            }
            logger.debug("Created error directory {}", errorDir);
        }
        logger.debug("Moving {} to directory {}", f, errorDir);

        f.renameTo(new File(errorDir.getAbsolutePath() + "/" + f.getName()));
        logger.info("Renamed {} to {}", f, new File(errorDir.getAbsolutePath() + "/" + f.getName()));
    }

    // #region handle success
    private void handleSuccess(@NotNull File f) {
        logger.info("Successfully imported {}, deleting the file", f);
        if (f == null || !f.exists()) {
            logger.info("File {} does not exist");
            return;
        }

        if (!f.delete()) {
            logger.info("Cannot delete file {}, trying to move it to error", f);
            handleError(f);
        }
    }
}
