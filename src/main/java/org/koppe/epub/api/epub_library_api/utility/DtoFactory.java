package org.koppe.epub.api.epub_library_api.utility;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.koppe.epub.api.epub_library_api.jpa.model.Author;
import org.koppe.epub.api.epub_library_api.jpa.model.Epub;
import org.koppe.epub.api.epub_library_api.jpa.model.EpubEdition;
import org.koppe.epub.api.epub_library_api.jpa.model.EpubMetadata;
import org.koppe.epub.api.epub_library_api.jpa.model.EpubSeries;
import org.koppe.epub.api.epub_library_api.jpa.model.Franchise;
import org.koppe.epub.api.epub_library_api.jpa.model.Genre;
import org.koppe.epub.api.epub_library_api.jpa.model.TableOfContentLine;
import org.koppe.epub.api.epub_library_api.jpa.model.Tag;
import org.koppe.epub.api.epub_library_api.jpa.model.User;
import org.koppe.epub.api.epub_library_api.web.dto.AuthorDto;
import org.koppe.epub.api.epub_library_api.web.dto.EpubDto;
import org.koppe.epub.api.epub_library_api.web.dto.EpubEditionDto;
import org.koppe.epub.api.epub_library_api.web.dto.EpubMetadataDto;
import org.koppe.epub.api.epub_library_api.web.dto.EpubSeriesDto;
import org.koppe.epub.api.epub_library_api.web.dto.FranchiseDto;
import org.koppe.epub.api.epub_library_api.web.dto.GenreDto;
import org.koppe.epub.api.epub_library_api.web.dto.TableOfContentLineDto;
import org.koppe.epub.api.epub_library_api.web.dto.TagDto;
import org.koppe.epub.api.epub_library_api.web.dto.UserResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Provides static methods to convert JPA entities to web dtos
 */
public abstract class DtoFactory {
    /**
     * Logger
     */
    private static final Logger logger = LoggerFactory.getLogger(DtoFactory.class);

    // #region authors
    /**
     * Converts an author entity to an author dto
     * 
     * @param author     Entity to be converted
     * @param withBooks  If true, books will be included. MUST be preloaded in jpa
     *                   transaction.
     * @param withGenres If true, genres will be included. MUST be preloaded in jpa
     *                   transaction.
     * @return Converted dto
     */
    public static final AuthorDto convertAuthorToDto(Author author, boolean withBooks, boolean withGenres) {
        logger.info("Converting author {} {}, {}", author, withBooks ? "with books" : "without books",
                withGenres ? "with genres" : "without genres");
        AuthorDto dto = new AuthorDto();

        if (author == null) {
            logger.info("No author given");
            throw new IllegalArgumentException("No author given");
        }

        dto.setId(author.getId());
        dto.setFirstName(author.getFirstName());
        dto.setSurname(author.getSurname());
        dto.setBirthDate(author.getBirthDate());
        dto.setDeathDate(author.getDeathDate());
        dto.setDescription(author.getDescription());

        if (withBooks) {
            dto.setEpubs(convertMultipleEpubsToDto(author.getBooks(), false, false, false, false, false, false));
        } else {
            dto.setEpubs(new HashSet<>());
        }

        if (withGenres) {
            dto.setGenres(convertMultipleGenresToDtos(author.getGenres(), false, false));
        } else {
            dto.setGenres(new HashSet<>());
        }

        return dto;
    }

    /**
     * Iterates over all given authors, converts them to dtos and returns the
     * resulting dtos
     * 
     * @param authors    Author jpa objects to convert
     * @param withBooks  If true, books will be included. MUST be preloaded in jpa
     *                   transaction.
     * @param withGenres If true, genres will be included. MUST be preloaded in jpa
     *                   transaction.
     * @return Set of all converted author dtos
     * @throws IllegalArgumentException If one of the authors in the given set is
     *                                  null
     */
    public static final Set<AuthorDto> convertMultipleAuthorsToDto(Set<Author> authors, boolean withBooks,
            boolean withGenres) throws IllegalArgumentException {
        Set<AuthorDto> dtos = new HashSet<>();
        if (authors == null) {
            logger.info("No authors given");
            throw new IllegalArgumentException("No authors given");
        }
        for (var a : authors) {
            dtos.add(convertAuthorToDto(a, withBooks, withGenres));
        }
        return dtos;
    }

    /**
     * Iterates over all given authors, converts them to dtos and returns the
     * resulting dtos
     * 
     * @param authors    Author jpa objects to convert
     * @param withBooks  If true, books will be included. MUST be preloaded in jpa
     *                   transaction.
     * @param withGenres If true, genres will be included. MUST be preloaded in jpa
     *                   transaction.
     * @return List of all converted author dtos
     * @throws IllegalArgumentException If one of the authors in the given set is
     *                                  null
     */
    public static final List<AuthorDto> convertMultipleAuthorsToDto(List<Author> authors, boolean withBooks,
            boolean withGenres) throws IllegalArgumentException {
        if (authors == null) {
            logger.info("Authors is null");
            throw new IllegalArgumentException("No authors given");
        }

        List<AuthorDto> dtos = new ArrayList<>();
        Set<Author> authorSet = new HashSet<>();

        authorSet.addAll(authors);
        Set<AuthorDto> setDtos = convertMultipleAuthorsToDto(authorSet, withBooks, withGenres);

        dtos.addAll(setDtos);

        return dtos;
    }

    // #region epubs

    public static final EpubDto convertEpubToDto(Epub book, boolean withGenres, boolean withAuthors,
            boolean withEditions, boolean withToc, boolean withSeries, boolean withTags) {
        EpubDto dto = new EpubDto();

        dto.setId(book.getId());
        dto.setTitle(book.getTitle());
        dto.setPublishDate(book.getPublishingDate());
        dto.setUploadDate(book.getUploadDate());

        if (withAuthors) {
            dto.setAuthors(convertMultipleAuthorsToDto(book.getAuthors(), false, false));
        } else {
            dto.setAuthors(new HashSet<>());
        }

        if (withGenres) {
            dto.setGenres(convertMultipleGenresToDtos(book.getGenres(), false, false));
        } else {
            dto.setGenres(new HashSet<>());
        }

        if (withEditions) {
            dto.setEditions(convertMultipleEditionsToDto(book.getEditions(), withToc));
        } else {
            dto.setEditions(new HashSet<>());
        }

        if (withTags) {
            dto.setTags(convertMultipleTagsToDtos(book.getTags(), false));
        } else {
            dto.setTags(new HashSet<>());
        }

        if (withSeries) {
            dto.setSeries(convertMultipleSeriesToDto(book.getSeries(), false));
        } else {
            dto.setSeries(new HashSet<>());
        }

        return dto;
    }

    public static final Set<EpubDto> convertMultipleEpubsToDto(Set<Epub> books, boolean withGenres,
            boolean withAuthors, boolean withEditions, boolean withToc, boolean withSeries, boolean withTags) {
        Set<EpubDto> dtos = new HashSet<>();
        for (Epub e : books) {
            dtos.add(convertEpubToDto(e, withGenres, withAuthors, withEditions, withToc, withSeries, withTags));
        }
        return dtos;
    }

    // #region genres
    public static final GenreDto convertGenreToDto(Genre genre, boolean withBooks, boolean withAuthors) {
        logger.info("Converting {} to dto {} books, {} authors.", genre, withBooks ? "with" : "without",
                withAuthors ? "with" : "without");

        GenreDto dto = new GenreDto();
        dto.setId(genre.getId());
        dto.setName(genre.getName());
        dto.setDescription(genre.getDescription());
        dto.setParentId(genre.getParent() != null ? genre.getParent().getId() : null);

        Set<GenreDto> childDtos = new HashSet<>();
        if (genre.getChildren() != null) {
            for (var x : genre.getChildren()) {
                childDtos.add(convertGenreToDto(x, withBooks, withAuthors));
            }
        } else {
            childDtos = new HashSet<>();
        }
        dto.setChildren(childDtos);

        if (withBooks) {
            dto.setBooks(convertMultipleEpubsToDto(genre.getBooks(), false, false, false, false, false, false));
        } else {
            dto.setBooks(new HashSet<>());
        }

        if (withAuthors) {
            dto.setAuthors(convertMultipleAuthorsToDto(genre.getAuthors(), false, false));
        } else {
            dto.setAuthors(new HashSet<>());
        }

        return dto;
    }

    public static final Set<GenreDto> convertMultipleGenresToDtos(Set<Genre> genres, boolean withBooks,
            boolean withAuthors) {
        Set<GenreDto> dtos = new HashSet<>();
        for (var g : genres) {
            dtos.add(convertGenreToDto(g, withBooks, withAuthors));
        }
        return dtos;
    }

    // #region editions
    public static final EpubEditionDto convertEditionToDto(EpubEdition edition, boolean withToc) {
        EpubEditionDto dto = new EpubEditionDto();

        dto.setId(edition.getId());
        dto.setVersionName(edition.getVersionName());
        dto.setEpubId(edition.getEpub().getId());
        dto.setDownloadGuid(edition.getDownloadGuid());
        dto.setUploadGuid(edition.getUploadGuid());
        dto.setMetadata(convertMetadataToDto(edition.getMetadata(), withToc));

        return dto;
    }

    public static final Set<EpubEditionDto> convertMultipleEditionsToDto(Set<EpubEdition> editions, boolean withToc) {
        Set<EpubEditionDto> dtos = new HashSet<>();

        for (var x : editions) {
            dtos.add(convertEditionToDto(x, withToc));
        }

        return dtos;
    }

    // #region metadata
    public static final EpubMetadataDto convertMetadataToDto(EpubMetadata metadata, boolean withToc) {
        if (metadata == null) {
            logger.info("No metadata obhect given");
            return null;
        }
        EpubMetadataDto dto = new EpubMetadataDto();
        dto.setEditionId(metadata.getEdition().getId());
        dto.setId(metadata.getId());
        dto.setNumberOfPages(metadata.getNumberOfPages());
        dto.setTableOfContents(withToc ? convertMultipleTocLinesToDto(metadata.getTableOfContents(), metadata.getId())
                : new ArrayList<>());
        return dto;
    }

    // #region table of content lines
    public static final TableOfContentLineDto convertTocLineToDto(TableOfContentLine tocLine, Long metadataId) {
        if (tocLine == null) {
            logger.info("No tocline given");
            return null;
        }
        TableOfContentLineDto dto = new TableOfContentLineDto();

        dto.setId(tocLine.getId());
        dto.setChapterName(tocLine.getChapterName());
        dto.setChapterNumber(tocLine.getChapterNumber());
        dto.setMetadataId(metadataId);
        dto.setPage(tocLine.getPage());

        return dto;
    }

    public static final List<TableOfContentLineDto> convertMultipleTocLinesToDto(List<TableOfContentLine> lines,
            Long metadataId) {
        List<TableOfContentLineDto> dtos = new ArrayList<>();

        for (var x : lines) {
            dtos.add(convertTocLineToDto(x, metadataId));
        }

        return dtos;
    }

    // #region users
    public static final UserResponseDto convertUserToResponseDto(User user) {
        UserResponseDto dto = new UserResponseDto();
        dto.setId(user.getId());
        dto.setName(user.getName());
        dto.setCreationDate(user.getCreationDate());

        return dto;
    }

    // #region tags
    public static final TagDto convertTagToDto(Tag tag, boolean withBooks) {
        TagDto dto = new TagDto();
        dto.setId(tag.getId());
        dto.setName(tag.getName());
        dto.setColour(tag.getColour());

        if (withBooks) {
            dto.setEpubs(convertMultipleEpubsToDto(tag.getEpubs(), false, false, false, false, false, false));
        } else {
            dto.setEpubs(new HashSet<>());
        }

        return dto;
    }

    public static final Set<TagDto> convertMultipleTagsToDtos(Set<Tag> tags, boolean withEpubs) {
        Set<TagDto> dtos = new HashSet<>();
        for (var x : tags) {
            dtos.add(convertTagToDto(x, withEpubs));
        }
        return dtos;
    }

    // #region series
    public static final EpubSeriesDto convertSeriesToDto(EpubSeries series, boolean withEpubs) {
        EpubSeriesDto dto = new EpubSeriesDto();
        dto.setId(series.getId());
        dto.setName(series.getName());

        if (withEpubs) {
            dto.setEpubs(convertMultipleEpubsToDto(series.getEpubs(), false, false, false, false, false, false));
        } else {
            dto.setEpubs(new HashSet<>());
        }

        return dto;
    }

    public static final Set<EpubSeriesDto> convertMultipleSeriesToDto(Set<EpubSeries> series, boolean withEpubs) {
        Set<EpubSeriesDto> dtos = new HashSet<>();
        for (var x : series) {
            dtos.add(convertSeriesToDto(x, withEpubs));
        }
        return dtos;
    }

    public static final FranchiseDto convertFranchiseToDto(Franchise franchise, boolean withEpubs) {
        FranchiseDto dto = new FranchiseDto();
        dto.setId(franchise.getId());
        dto.setName(franchise.getName());

        if (withEpubs) {
            dto.setEpubs(convertMultipleEpubsToDto(franchise.getEpubs(), false, false, false, false, false, false));
        } else {
            dto.setEpubs(new HashSet<>());
        }

        return dto;
    }

    public static final Set<FranchiseDto> convertMultipleFranchisesToDto(Set<Franchise> franchises, boolean withEpubs) {
        Set<FranchiseDto> dtos = new HashSet<>();
        for (var x : franchises) {
            dtos.add(convertFranchiseToDto(x, withEpubs));
        }
        return dtos;
    }
}
