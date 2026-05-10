package org.koppe.epub.api.epub_library_api.jpa.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.koppe.epub.api.epub_library_api.exceptions.MediaTypeException;
import org.koppe.epub.api.epub_library_api.jpa.model.Author;
import org.koppe.epub.api.epub_library_api.jpa.model.Epub;
import org.koppe.epub.api.epub_library_api.jpa.model.EpubEdition;
import org.koppe.epub.api.epub_library_api.jpa.model.EpubMetadata;
import org.koppe.epub.api.epub_library_api.jpa.model.Genre;
import org.koppe.epub.api.epub_library_api.jpa.repository.EpubEditionRepository;
import org.koppe.epub.api.epub_library_api.jpa.repository.EpubMetadataRepository;
import org.koppe.epub.api.epub_library_api.jpa.repository.EpubRepository;
import org.koppe.epub.api.epub_library_api.jpa.repository.TableOfContentLineRepository;
import org.koppe.epub.api.epub_library_api.jpa.specs.EpubSpecificationBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;

/**
 * Service layer that interacts with the jpa repository for epubs
 */
@Service
@RequiredArgsConstructor
public class EpubService {
    /**
     * Logger
     */
    private final Logger logger = LoggerFactory.getLogger(EpubService.class);
    /**
     * JPA repository
     */
    private final EpubRepository epubs;
    /**
     * JPA repository for epub editions
     */
    private final EpubEditionRepository editions;
    /**
     * Jpa repository for working with toc lines
     */
    private final TableOfContentLineRepository tocs;
    /**
     * JPA repository for working with epub metadata
     */
    private final EpubMetadataRepository metadata;

    private final SharedService shared;

    /**
     * Base file path for all epubs
     */
    private final String BASE_FILE_PATH = System.getenv("LIBRARY_DATA_PATH") != null
            ? System.getenv("LIBRARY_DATA_PATH") + "/epubs"
            : "./data/epub-library/epubs";

    // #region find all epubs
    /**
     * Finds all epubs matching the specifications
     * 
     * @param nameContains    Epubs must contain this string in the title
     * @param publishedBefore Epubs must be published before this date
     * @param publishedAfter  Epubs must be published after this date
     * @param uploadedBefore  Epubs must be uploaded before this date
     * @param uploadedAfter   Epubs must be uploaded after this date
     * @param genres          Epubs must be associated with at least one of the
     *                        given genres
     * @param authors         Epubs must be associated with at least one of the
     *                        given authors
     * @param page            Page of the resultset to be returned
     * @param pageSize        Size of the page to be returned
     * @return
     */
    @Transactional(readOnly = true)
    public Page<Epub> findAllEpubs(String nameContains, LocalDate publishedBefore, LocalDate publishedAfter,
            LocalDate uploadedBefore, LocalDate uploadedAfter, List<Genre> genres, List<Author> authors, Integer page,
            Integer pageSize) {

        Specification<Epub> spec = EpubSpecificationBuilder.titleContains(nameContains)
                .and(EpubSpecificationBuilder.publishedBefore(publishedBefore))
                .and(EpubSpecificationBuilder.publishedAfter(publishedAfter))
                .and(EpubSpecificationBuilder.uploadedBefore(uploadedBefore))
                .and(EpubSpecificationBuilder.uploadedAfter(uploadedAfter))
                .and(EpubSpecificationBuilder.genreFilter(genres))
                .and(EpubSpecificationBuilder.authorFilter(authors));

        return epubs.findAll(spec, PageRequest.of(page == null ? 0 : page, pageSize == null ? 1000 : pageSize));
    }

    // #region epub exists by id
    /**
     * Checks if an epub for the given id exists
     * 
     * @param id Id of the epub to check
     * @return True, if id for epub exists, false otherwise
     * @throws IllegalArgumentException If no id is given
     */
    @Transactional(readOnly = true)
    public boolean existsById(Long id) throws IllegalArgumentException {
        if (id == null) {
            logger.info("No id given");
            throw new IllegalArgumentException("No id given");
        }

        return epubs.existsById(id);
    }

    // #region find epub by id
    /**
     * Finds epub with given id and returns it or null, if no such epub exists
     * 
     * @param id Id of th epub to find and return
     * @return Epub or null, if no epub with that id exists
     * @throws IllegalArgumentException If no id is given
     */
    @Transactional(readOnly = true)
    public Epub findEpubById(Long id) throws IllegalArgumentException {
        if (id == null) {
            throw new IllegalArgumentException("No id given");
        }
        Optional<Epub> found = epubs.findById(id);
        if (found.isEmpty())
            return null;

        Epub epub = found.get();
        if (epub.getEditions() != null) {
            for (var x : epub.getEditions()) {
                if (x.getMetadata() != null) {
                    x.getMetadata().getTableOfContents();
                }
            }
        }

        epub.getAuthors();
        epub.getGenres();

        return epub;
    }

    // #region add book
    /**
     * Adds given epub to the database
     * 
     * @param epub Epub to add to the database. If id is set, it is nulled before
     *             adding.
     * @return The added epub
     * @throws IllegalArgumentException If epub is null or epub.title is null or
     *                                  blank
     */
    @Transactional
    public Epub addBook(Epub epub) throws IllegalArgumentException {
        if (epub == null || epub.getTitle() == null || epub.getTitle().isBlank()) {
            logger.info("No epub or title given");
            throw new IllegalArgumentException("No epub or title given");
        }

        epub.setId(null);
        return epubs.save(epub);
    }

    /**
     * Adds an epub with the given title to the database
     * 
     * @param title Title of the epub
     * @return Created epub
     * @throws IllegalArgumentException If title is null or blank
     */
    @Transactional
    public Epub addBook(String title) throws IllegalArgumentException {
        if (title == null || title.isBlank()) {
            logger.info("No title for new epub given");
            throw new IllegalArgumentException("No title for new epub given");
        }

        Epub epub = new Epub();
        epub.setTitle(title);
        return addBook(epub);
    }

    // #region update epub
    @Transactional
    public Epub updateEpub(Long id, Epub epub, boolean overwriteNulls) throws IllegalArgumentException {
        if (id == null || epub == null) {
            logger.info("No epub or id given");
            throw new IllegalArgumentException();
        }

        if (epub.getId() != null && !epub.getId().equals(id)) {
            logger.info("Id in epub does not match given id");
            throw new IllegalArgumentException();
        }

        if (!existsById(id)) {
            logger.info("Epub with id {} does not exist", id);
            throw new IllegalArgumentException("Epub with given id does not exist");
        }

        if (epub.getTitle() == null && overwriteNulls) {
            logger.warn("Cannot update title with null value");
            throw new IllegalArgumentException("Cannot update title with null value");
        }

        Epub entry = epubs.findById(id).get();
        entry.setPublishingDate(epub.getPublishingDate() != null || overwriteNulls ? epub.getPublishingDate()
                : entry.getPublishingDate());
        entry.setTitle(epub.getTitle() != null ? epub.getTitle() : entry.getTitle());

        return epubs.save(entry);
    }

    // #region add author
    @Transactional
    public Author addAuthor(Long id, Long authorId) throws IllegalArgumentException {
        if (id == null || authorId == null || !existsById(id) || !shared.authorExistsById(authorId)) {
            logger.info("Invalid id or author id given");
            throw new IllegalArgumentException("Invalid id or author id given");
        }

        return shared.addEpubToAuthor(authorId, id);
    }

    // #region remove author
    @Transactional
    public Author removeAuthor(long epubId, long authorId) throws IllegalArgumentException {
        if (!existsById(epubId) || !shared.authorExistsById(authorId)) {
            logger.info("Invalid epub or author id given");
            throw new IllegalArgumentException("Invalid epub or author id given");
        }
        return shared.removeEpubFromAuthor(authorId, epubId);
    }

    // #region add genre
    @Transactional
    public Epub addGenre(long id, long genreId) throws IllegalArgumentException {
        return shared.addGenreToEpub(genreId, id);
    }

    // #region delete genre from epub
    @Transactional
    public Epub deleteGenre(long id, long genreId) throws IllegalArgumentException {
        return shared.deleteGenreFromEpub(id, genreId);
    }

    // #region delete epub
    /**
     * Deletes epub with given id. Before the epub itself is deleted, all it's
     * editions are deleted.
     * 
     * @param id Id of the epub to be deleted
     * @return The deleted epub
     * @throws IllegalArgumentException If no id is given or no epub with given id
     *                                  exists
     */
    @Transactional
    public Epub deleteEpub(Long id) throws IllegalArgumentException {
        if (id == null || !existsById(id)) {
            logger.info("No epub with id {} exists", id);
            throw new IllegalArgumentException("No epub with given id exists");
        }

        Epub epub = epubs.findById(id).get();
        logger.info("Starting to delete epub {}", epub);

        for (var x : epub.getEditions()) {
            logger.debug("Deleting edition {}", x);
            deleteEdition(id, x.getId());
            File bookDirectory = new File(x.getBaseFilePath()).getParentFile();
            if (bookDirectory.exists())
                deleteDirectory(bookDirectory);
        }

        epubs.delete(epub);
        logger.info("Successfully deleted epub {}", epub);
        return epub;
    }

    // #region editions
    // #region edition exists by id
    /**
     * Check if epub edition for given epub id exists
     * 
     * @param epubId    Id of the epub
     * @param editionId Id of the edition for the epub to be checked for existence
     * @return True, if the edition exists, false otherwise
     * @throws IllegalArgumentException If epubId or editionId are not given
     */
    @Transactional(readOnly = true)
    public boolean editionExistsByid(Long epubId, Long editionId) throws IllegalArgumentException {
        if (epubId == null || editionId == null) {
            logger.info("Edition id or epub id are not given");
            throw new IllegalArgumentException("Edition id or epub id are not given");
        }

        Optional<EpubEdition> edition = editions.findById(editionId);
        if (edition.isEmpty()) {
            logger.info("Edition {} does not exist", editionId);
            return false;
        }

        EpubEdition ed = edition.get();
        if (!ed.getEpub().getId().equals(epubId)) {
            logger.info("Edition exists but does not match given epub id");
            return false;
        }

        return true;
    }

    // #region edition has metadata
    /**
     * Checks if the epub edition with the given id already has metadata associated
     * to it
     * 
     * @param editionId Edition for which to check if metadata exists
     * @return True, if metadata exists for the given edition
     * @throws IllegalArgumentException If no editionId is given
     */
    @Transactional(readOnly = true)
    public boolean editionHasMetadata(Long editionId) throws IllegalArgumentException {
        if (editionId == null || !editions.existsById(editionId)) {
            logger.info("No valid edition id given");
            throw new IllegalArgumentException("No valid edition id given");
        }

        EpubEdition ed = editions.findById(editionId).get();
        if (ed.getMetadata() == null)
            return false;
        return true;
    }

    // #region add edition to epub
    /**
     * Adds given edition to epub with given id
     * 
     * @param epubId  Epub to add the edition to
     * @param edition Edition to add to the epub
     * @return The edition entity saved in the database
     * @throws IllegalArgumentException If an invalid epub id is given, edition is
     *                                  null or has no version name
     */
    @Transactional
    public EpubEdition addEdition(Long epubId, EpubEdition edition) throws IllegalArgumentException {
        if (epubId == null || !existsById(epubId)) {
            logger.info("Invalid epub id");
            throw new IllegalArgumentException("Invalid epub id");
        }

        if (edition == null || edition.getVersionName() == null || edition.getVersionName().isBlank()) {
            logger.info("Invalid edition given");
            throw new IllegalArgumentException("Invalid edition given");
        }

        Epub e = epubs.findById(epubId).get();
        String uploadId = UUID.randomUUID().toString();
        String downloadId = UUID.randomUUID().toString();

        edition.setEpub(e);
        edition.setBaseFilePath(BASE_FILE_PATH + "/" + e.getTitle() + "/" + edition.getVersionName());
        edition.setUploadGuid(uploadId);
        edition.setDownloadGuid(downloadId);

        return editions.save(edition);
    }

    // #region find edition by upload guid
    /**
     * Finds epub edition associated with given upload guid
     * 
     * @param uploadGuid Upload guid of the edition to find
     * @return The found edition or null, if no edition with that upload guid has
     *         been found
     * @throws IllegalArgumentException If given upload guid is null or blank
     */
    @Transactional(readOnly = true)
    public EpubEdition findEditionByUploadGuid(String uploadGuid) throws IllegalArgumentException {
        if (uploadGuid == null || uploadGuid.isBlank()) {
            logger.info("No upload guid given");
            throw new IllegalArgumentException("No upload guid given");
        }

        Set<EpubEdition> edop = editions.findAllByUploadGuid(uploadGuid);
        if (edop.isEmpty()) {
            logger.info("No edition with upload guid {} exists", uploadGuid);
            return null;
        }

        return edop.toArray(EpubEdition[]::new)[0];
    }

    // #region find edition by md5
    /**
     * Returns the epub edition associated with the given md5 hash or null, if no
     * such edition exists.
     * 
     * @param md5 MD5 hash of the .epub file to find in the system
     * @return The associated epub edition or null, if no such edition exists.
     * @throws IllegalArgumentException If no md5 is given
     */
    public EpubEdition findEditionByMd5Hash(@NotNull String md5) throws IllegalArgumentException {
        if (md5 == null || md5.isBlank()) {
            logger.info("No md5 to find given");
            throw new IllegalArgumentException("Missing md5 hash");
        }

        md5 = md5.toUpperCase();
        Optional<EpubEdition> edop = editions.findByMd5Hash(md5);
        if (edop.isEmpty()) {
            logger.info("No edition with given md5 exists");
            return null;
        }

        return edop.get();
    }

    // #region update edition name
    /**
     * Updates version name of an edition
     * 
     * @param epubId      Id of the epub
     * @param editionId   Id of the edition
     * @param versionName New name for the epub edition version
     * @return The new epub edition
     * @throws IllegalArgumentException if one of the arguments is not given or the
     *                                  combination of epub id and edition id does
     *                                  not exist.
     */
    @Transactional
    public EpubEdition updateEditionName(Long epubId, Long editionId, String versionName)
            throws IllegalArgumentException {
        if (epubId == null || editionId == null || versionName == null || versionName.isBlank()
                || !editionExistsByid(epubId, editionId)) {
            logger.info("Invalid arugments");
            throw new IllegalArgumentException();
        }

        EpubEdition edition = editions.findById(editionId).get();
        edition.setVersionName(versionName);
        return editions.save(edition);
    }

    // #region delete edition
    /**
     * Deletes edition of the epub irreversibly
     * 
     * @param epubId    Id of the epub
     * @param editionId Id of the edition
     * @return The deleted epub edition
     */
    @Transactional
    public EpubEdition deleteEdition(Long epubId, Long editionId) {
        if (epubId == null || editionId == null || !editionExistsByid(epubId, editionId)) {
            logger.info("Invalid arugments");
            throw new IllegalArgumentException();
        }

        EpubEdition edition = editions.findById(editionId).get();
        File editionDir = new File(edition.getBaseFilePath());
        deleteDirectory(editionDir);

        for (var x : edition.getMetadata().getTableOfContents()) {
            tocs.delete(x);
        }
        metadata.delete(edition.getMetadata());
        editions.delete(edition);

        return edition;
    }

    // #region delete directory
    /**
     * Deletes a directory and all it's contents recursively
     * 
     * @param dir Directory to delete
     */
    private void deleteDirectory(File dir) {
        if (dir == null || !dir.exists())
            return;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    deleteDirectory(f);
                } else {
                    logger.info("Deleting file {}", f);
                    f.delete();
                }
            }
        }
        dir.delete();
    }

    // #region find edition by download guid
    /**
     * Finds edition associated with given download guid
     * 
     * @param downlaodGuid Download guid of the edition to find
     * @return The found edition
     * @throws IllegalArgumentException If no download guid is given
     */
    @Transactional(readOnly = true)
    public EpubEdition findEditionByDownloadGuid(String downlaodGuid) throws IllegalArgumentException {
        if (downlaodGuid == null || downlaodGuid.isBlank()) {
            logger.info("No download guid given");
            throw new IllegalArgumentException();
        }

        Set<EpubEdition> edop = editions.findAllByDownloadGuid(downlaodGuid);
        if (edop.isEmpty()) {
            logger.info("No edition with download guid {} exists", downlaodGuid);
            return null;
        }

        return edop.toArray(EpubEdition[]::new)[0];
    }

    // #region upload epub
    /**
     * Upload given file as epub for given upload guid
     * 
     * @param uploadGuid Guid of the edition to which the epub file belongs
     * @param file       File to upload
     * @return True if upload worked, false otherwise
     * @throws IllegalArgumentException
     * @throws IOException
     * @throws MediaTypeException
     */
    public boolean uploadEpub(String uploadGuid, MultipartFile file)
            throws IllegalArgumentException, IOException, MediaTypeException {
        if (uploadGuid == null || uploadGuid.isBlank() || file == null) {
            logger.warn("No upload guid or file given");
            throw new IllegalArgumentException("No upload guid or file given");
        }

        EpubEdition edition = findEditionByUploadGuid(uploadGuid);
        if (edition == null) {
            logger.info("No edition for given upload guid exists");
            throw new IllegalArgumentException("No edition for given upload guid exists");
        }
        return upload(edition, file);
    }

    // #region upload
    /**
     * Uploads given multipart file to the directory of the given edition
     * 
     * @param edition Edition the file should be uploaded for
     * @param file    File to be uploaded
     * @return True, if upload succeded, false otherwise
     * @throws IllegalArgumentException If no edition or file is given
     * @throws IOException              If creating the new files failed
     * @throws MediaTypeException
     */
    @Transactional
    public boolean upload(EpubEdition edition, MultipartFile file)
            throws IllegalArgumentException, IOException, MediaTypeException {
        if (edition == null || file == null) {
            logger.info("No edition or file given");
            throw new IllegalArgumentException("No edition or file given");
        }

        if (!file.getOriginalFilename().toLowerCase().matches("^.*\\.epub$")) {
            logger.info("Invalid media type {}", file.getContentType());
            throw new MediaTypeException();
        }

        File directory = new File(edition.getBaseFilePath());
        if (directory.exists()) {
            if (directory.list().length > 0) {
                logger.info("Epub already uploaded");
                return false;
            }
        } else {
            logger.info("Directory {} does not yet exist, creating it", directory);
            directory.mkdirs();
        }

        File epub = new File(directory.getAbsolutePath() + "/book.epub");
        if (!epub.exists())
            epub.createNewFile();

        OutputStream os = new FileOutputStream(epub);
        os.write(file.getBytes());

        IOUtils.closeQuietly(os);

        edition.setOriginalFileName(file.getOriginalFilename());

        // Executors.newFixedThreadPool(1).submit(() -> {
        EpubExtractor ex = new EpubExtractor(epub, edition);
        EpubMetadata extractedMetadata = ex.extract();

        if (metadata == null) {
            logger.info("Metadata could not be extracted");
            cleanup(edition);
            return false;
        }

        edition.setMd5Hash(DigestUtils.md5DigestAsHex(file.getBytes()).toUpperCase());
        editions.save(edition);

        metadata.save(extractedMetadata);
        tocs.saveAll(extractedMetadata.getTableOfContents());
        // });

        return true;
    }

    // #region cleanup
    /**
     * Cleans up files from a failed epub extraction attempt
     * 
     * @param edition Edition for which to clean upß the files
     */
    private void cleanup(EpubEdition edition) {
        File directory = new File(edition.getBaseFilePath());
        for (var x : directory.list())
            new File(directory.getPath() + "/" + x).delete();
        directory.delete();
    }

    // #region get epub for edition
    @Transactional(readOnly = true)
    public File getEpubForEdition(EpubEdition edition) throws IllegalArgumentException, IOException {
        if (edition == null) {
            logger.warn("No edition given");
            throw new IllegalArgumentException("No edition given");
        }

        File directory = new File(edition.getBaseFilePath());
        if (!directory.exists()) {
            logger.warn("Directory in edition does not exist");
            throw new IOException("Directory in edition does not exist");
        }

        File book = null;
        for (String x : directory.list()) {
            if (x.contains("book")) {
                book = new File(edition.getBaseFilePath() + "/" + x);
                break;
            }
        }

        if (book == null || !book.exists()) {
            logger.info("Could not find book");
            return null;
        }
        logger.info("Found book {} for download edition {}", book, edition);
        return book;
    }

    // #region get cover image
    /**
     * Returns cover image for given epub edition
     * 
     * @param edition Edition to get the cover image for
     * @return Cover image of the edition
     * @throws IllegalArgumentException If no edition is given
     * @throws IOException              If no directory exists for the given edition
     */
    public File getCoverForEdition(EpubEdition edition) throws IllegalArgumentException, IOException {
        if (edition == null) {
            logger.warn("No edition given");
            throw new IllegalArgumentException("No edition given");
        }

        File directory = new File(edition.getBaseFilePath());
        if (!directory.exists()) {
            logger.warn("Directory in edition does not exist");
            throw new IOException("Directory in edition does not exist");
        }

        File cover = null;
        for (String x : directory.list()) {
            if (x.contains("cover")) {
                cover = new File(edition.getBaseFilePath() + "/" + x);
                break;
            }
        }

        if (cover == null || !cover.exists()) {
            logger.info("Could not find book");
            return null;
        }
        logger.info("Found book {} for download edition {}", cover, edition);
        return cover;
    }

    public Epub addTag(long epubId, long tagId) throws IllegalArgumentException {
        return shared.addTagToEpub(epubId, tagId);
    }

    public Epub removeTag(long epubId, long tagId) throws IllegalArgumentException {
        return shared.removeTagFromEpub(epubId, tagId);
    }
}
