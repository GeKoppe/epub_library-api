package org.koppe.epub.api.epub_library_api.jpa.service;

import java.io.File;
import java.util.Optional;

import org.koppe.epub.api.epub_library_api.jpa.model.Author;
import org.koppe.epub.api.epub_library_api.jpa.model.Epub;
import org.koppe.epub.api.epub_library_api.jpa.model.EpubEdition;
import org.koppe.epub.api.epub_library_api.jpa.model.EpubSeries;
import org.koppe.epub.api.epub_library_api.jpa.model.Franchise;
import org.koppe.epub.api.epub_library_api.jpa.model.Genre;
import org.koppe.epub.api.epub_library_api.jpa.model.Tag;
import org.koppe.epub.api.epub_library_api.jpa.repository.AuthorRepository;
import org.koppe.epub.api.epub_library_api.jpa.repository.EpubEditionRepository;
import org.koppe.epub.api.epub_library_api.jpa.repository.EpubMetadataRepository;
import org.koppe.epub.api.epub_library_api.jpa.repository.EpubRepository;
import org.koppe.epub.api.epub_library_api.jpa.repository.EpubSeriesRepository;
import org.koppe.epub.api.epub_library_api.jpa.repository.FranchiseRepository;
import org.koppe.epub.api.epub_library_api.jpa.repository.GenreRepository;
import org.koppe.epub.api.epub_library_api.jpa.repository.TableOfContentLineRepository;
import org.koppe.epub.api.epub_library_api.jpa.repository.TagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SharedService {
    private final Logger logger = LoggerFactory.getLogger(SharedService.class);
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
    /**
     * JPA repository for working with authors
     */
    private final AuthorRepository authors;
    /**
     * JPA repository for working with genres
     */
    private final GenreRepository genres;
    /**
     * JPA repository for working with tags
     */
    private final TagRepository tags;
    /**
     * JPA repository for working with epub series
     */
    private final EpubSeriesRepository series;
    /**
     * JPA repository for working with franchises
     */
    private final FranchiseRepository franchises;

    // #region epubs
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
        if (id == null || !epubs.existsById(id)) {
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

    // #region epub exists by id
    /**
     * Returns true, if epub with given id exists
     * 
     * @param id Id of epub to check
     * @return True, if epub exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean epubExistsById(long id) {
        return epubs.existsById(id);
    }

    // #region find epub by id
    /**
     * Returns epub with given id
     * 
     * @param id
     * @return
     * @throws IllegalArgumentException
     */
    @Transactional(readOnly = true)
    public Epub findEpubById(long id) throws IllegalArgumentException {
        if (!epubExistsById(id)) {
            logger.info("Invalid epub id given");
            throw new IllegalArgumentException("Invalid epub id given");
        }

        Epub epub = epubs.findById(id).get();
        epub.getAuthors().size();
        epub.getGenres().size();
        epub.getEditions().size();

        return epub;
    }

    // #region add genre to epub
    /**
     * Adds genre to epub
     * 
     * @param genreId Id of the genre
     * @param epubId  Id of the epub
     * @return The updated epub
     * @throws IllegalArgumentException If genre or epub don't exist
     */
    @Transactional
    public Epub addGenreToEpub(long genreId, long epubId) throws IllegalArgumentException {
        if (!genreExistsById(genreId) || !epubExistsById(epubId)) {
            logger.info("Invalid combination of genre and epub");
            throw new IllegalArgumentException("Invalid combination of genre and epub");
        }

        Epub epub = findEpubById(epubId);
        Genre genre = findGenreById(genreId);

        if (epub.getGenres().contains(genre)) {
            logger.info("Genre {} already associated with epub {}", genre, epub);
            return null;
        }

        epub.getGenres().add(genre);
        return epubs.save(epub);
    }

    // #region delete genre from epub
    @Transactional
    public Epub deleteGenreFromEpub(long epubId, long genreId) throws IllegalArgumentException {
        if (!epubExistsById(epubId) || !genreExistsById(genreId)) {
            logger.info("Invalid combination of epub and genre given");
            throw new IllegalArgumentException("Invalid combination of epub and genre given");
        }

        Epub epub = findEpubById(epubId);
        Genre genre = findGenreById(genreId);

        if (!epub.getGenres().contains(genre)) {
            logger.info("Genre {} is not associated with epub {}", genre, epub);
            return null;
        }

        epub.getGenres().remove(genre);
        return epubs.save(epub);
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

    // #region genres

    // #region genre exists by id
    /**
     * Returns true, if a genre with that id exists
     * 
     * @param id Id of the genre to check
     * @return True, if genre exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean genreExistsById(long id) {
        return genres.existsById(id);
    }

    // #region find genre by id
    /**
     * 
     * @param id
     * @return
     * @throws IllegalArgumentException
     */
    @Transactional(readOnly = true)
    public Genre findGenreById(long id) throws IllegalArgumentException {
        if (!genreExistsById(id)) {
            throw new IllegalArgumentException("Genre with given id does not exist");
        }

        return genres.findById(id).get();
    }

    // #region authors
    // #region author exists by id
    @Transactional(readOnly = true)
    public boolean authorExistsById(long id) {
        return authors.existsById(id);
    }

    // #region find author by id
    /**
     * Returns author with given id
     * 
     * @param id Id of the author to return
     * @return Author with given id
     * @throws IllegalArgumentException If no author with given id exists
     */
    @Transactional(readOnly = true)
    public Author findAuthorById(long id) throws IllegalArgumentException {
        if (!authorExistsById(id)) {
            throw new IllegalArgumentException("Invalid author id given");
        }

        return authors.findById(id).get();
    }

    // #region add genre to author
    @Transactional
    public Author addGenreToAuthor(long authorId, long genreId) throws IllegalArgumentException {
        if (!authorExistsById(authorId) || !genreExistsById(genreId)) {
            logger.info("Invalid combnination of author and genre");
            throw new IllegalArgumentException("Invalid combnination of author and genre");
        }

        Author author = findAuthorById(authorId);
        Genre genre = findGenreById(genreId);

        if (author.getGenres().contains(genre)) {
            logger.info("Author {} already associated with genre {}", author, genre);
            return null;
        }
        logger.info("Adding genre {} to author {}", genre, author);
        author.getGenres().add(genre);

        return authors.save(author);
    }

    // #region remove genre from author
    @Transactional
    public Author removeGenreFromAuthor(long authorId, long genreId) {
        if (!authorExistsById(authorId) || !genreExistsById(genreId)) {
            logger.info("Invalid combnination of author and genre");
            throw new IllegalArgumentException("Invalid combnination of author and genre");
        }

        Author author = findAuthorById(authorId);
        Genre genre = findGenreById(genreId);

        if (!author.getGenres().contains(genre)) {
            logger.info("Author {} already associated with genre {}", author, genre);
            return null;
        }
        logger.info("Removing genre {} to author {}", genre, author);
        author.getGenres().remove(genre);

        return authors.save(author);
    }

    // #region add epub to author
    @Transactional
    public Author addEpubToAuthor(long authorId, long epubId) throws IllegalArgumentException {
        if (!authorExistsById(authorId) || !epubExistsById(epubId)) {
            logger.info("Invalid combination of author and epub");
            throw new IllegalArgumentException("Invalid combination of author and epub");
        }

        Author author = findAuthorById(authorId);
        Epub epub = findEpubById(epubId);

        if (author.getBooks().contains(epub)) {
            logger.info("Epub {} already associated with author {}", epub, author);
            return null;
        }

        logger.info("Adding epub {} to author {}", epub, author);
        author.getBooks().add(epub);

        return authors.save(author);
    }

    // #region remove epub from author
    @Transactional
    public Author removeEpubFromAuthor(long authorId, long epubId) throws IllegalArgumentException {
        if (!authorExistsById(authorId) || !epubExistsById(epubId)) {
            logger.info("Invalid combination of author and epub");
            throw new IllegalArgumentException("Invalid combination of author and epub");
        }

        Author author = findAuthorById(authorId);
        Epub epub = findEpubById(epubId);

        if (!author.getBooks().contains(epub)) {
            logger.info("Epub {} already associated with author {}", epub, author);
            return null;
        }

        logger.info("Adding epub {} to author {}", epub, author);
        author.getBooks().remove(epub);

        return authors.save(author);
    }

    // #region tags
    // #region find tag by id
    @Transactional(readOnly = true)
    public boolean tagExistsById(long id) {
        return tags.existsById(id);
    }

    // #region find tag by id
    @Transactional(readOnly = true)
    public Tag findTagById(long id) throws IllegalArgumentException {
        if (!tagExistsById(id)) {
            logger.info("Invalid tag id given");
            throw new IllegalArgumentException("Invalid tag id given");
        }

        Tag tag = tags.findById(id).get();
        tag.getEpubs().size();
        return tag;
    }

    // #region find tag by name
    @Transactional(readOnly = true)
    public Tag findTagByName(String name) throws IllegalArgumentException {
        if (name == null || name.isBlank()) {
            logger.info("No name given");
            throw new IllegalArgumentException("No tag name given");
        }

        Optional<Tag> opt = tags.findByName(name);
        if (opt.isEmpty()) {
            return null;
        }
        return opt.get();
    }

    // #region add tag to epub
    @Transactional
    public Epub addTagToEpub(long epubId, long tagId) throws IllegalArgumentException {
        if (!epubExistsById(epubId) || !tagExistsById(tagId)) {
            logger.info("Invalid epub or tag id given");
            throw new IllegalArgumentException("Invalid epub or tag id given");
        }

        Epub epub = findEpubById(epubId);
        Tag tag = findTagById(tagId);

        if (epub.getTags().contains(tag)) {
            logger.info("Epub already associated with given tag");
            return null;
        }
        logger.info("Adding tag {} to epub {}", tag, epub);
        epub.getTags().add(tag);

        return epubs.save(epub);
    }

    // #region remove tag from epub
    @Transactional
    public Epub removeTagFromEpub(long epubId, long tagId) {
        if (!epubExistsById(epubId) || !tagExistsById(tagId)) {
            logger.info("Invalid epub or tag id given");
            throw new IllegalArgumentException("Invalid epub or tag id given");
        }

        Epub epub = findEpubById(epubId);
        Tag tag = findTagById(tagId);

        if (!epub.getTags().contains(tag)) {
            logger.info("Epub are not associated");
            return null;
        }
        logger.info("Removing tag {} from epub {}", tag, epub);
        epub.getTags().remove(tag);

        return epubs.save(epub);
    }

    // #region series
    // #region series exists by id
    @Transactional(readOnly = true)
    public boolean seriesExistsById(long id) {
        return series.existsById(id);
    }

    // #region find epub series by id
    @Transactional(readOnly = true)
    public EpubSeries findSeriesById(long id) throws IllegalArgumentException {
        if (!seriesExistsById(id)) {
            logger.info("Invalid series id given");
            throw new IllegalArgumentException("Invalid series id given");
        }

        EpubSeries es = series.findById(id).get();
        es.getEpubs().size();
        return es;
    }

    // #region franchise exists by id
    @Transactional(readOnly = true)
    public boolean franchiseExistsById(long id) {
        return franchises.existsById(id);
    }

    // #region find franchise by id
    @Transactional(readOnly = true)
    public Franchise findFranchiseById(long id) throws IllegalArgumentException {
        if (!franchiseExistsById(id)) {
            logger.info("Invalid franchise id");
            throw new IllegalArgumentException("Invalid franchise id");
        }

        return franchises.findById(id).get();
    }

    // #region add epub to franchise
    @Transactional
    public Franchise addEpubToFranchise(long franchiseId, long epubId) throws IllegalArgumentException {
        if (!franchiseExistsById(franchiseId) || !epubExistsById(epubId)) {
            logger.info("Invalid combination of franchise and epub");
            throw new IllegalArgumentException("Invalid combination of franchise and epub");
        }

        Franchise franchise = findFranchiseById(franchiseId);
        Epub epub = findEpubById(epubId);

        if (franchise.getEpubs().contains(epub)) {
            logger.info("{} and {} already associated");
            return null;
        }
        franchise.getEpubs().add(epub);
        return franchises.save(franchise);
    }

    // #region remove epub from franchise
    @Transactional
    public Franchise removeEpubFromFranchise(long franchiseId, long epubId) throws IllegalArgumentException {
        if (!franchiseExistsById(franchiseId) || !epubExistsById(epubId)) {
            logger.info("Invalid combination of franchise and epub");
            throw new IllegalArgumentException("Invalid combination of franchise and epub");
        }

        Franchise franchise = findFranchiseById(franchiseId);
        Epub epub = findEpubById(epubId);

        if (!franchise.getEpubs().contains(epub)) {
            logger.info("{} and {} not associated");
            return null;
        }
        franchise.getEpubs().remove(epub);
        return franchises.save(franchise);
    }
}
