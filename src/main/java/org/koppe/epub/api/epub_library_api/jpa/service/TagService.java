package org.koppe.epub.api.epub_library_api.jpa.service;

import org.koppe.epub.api.epub_library_api.exceptions.DuplicateKeyException;
import org.koppe.epub.api.epub_library_api.jpa.model.Epub;
import org.koppe.epub.api.epub_library_api.jpa.model.Tag;
import org.koppe.epub.api.epub_library_api.jpa.repository.TagRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TagService {
    /**
     * Logger
     */
    private final Logger logger = LoggerFactory.getLogger(TagService.class);
    /**
     * Service for shared operations
     */
    private final SharedService shared;
    /**
     * JPA Repository for working with tags in the database
     */
    private final TagRepository tags;

    /**
     * Add a tag to the database
     * 
     * @param name Name of the new tag
     * @return The new tag
     * @throws IllegalArgumentException If no name is given
     * @throws DuplicateKeyException    If the given name is already taken
     */
    @Transactional
    public Tag addTag(String name) throws IllegalArgumentException, DuplicateKeyException {
        return addTag(name, null);
    }

    /**
     * Adds tag to the database
     * 
     * @param name   Name of the new tag
     * @param colour Colour of the new tag
     * @return The new tag
     * @throws IllegalArgumentException If no name is given
     * @throws DuplicateKeyException    If the given name is already taken
     */
    @Transactional
    public Tag addTag(String name, String colour) throws IllegalArgumentException, DuplicateKeyException {
        return addTag(new Tag(null, name, colour, null));
    }

    /**
     * Adds a tag to the database
     * 
     * @param tag Tag to add
     * @return The added tag
     * @throws IllegalArgumentException If no name in the tag is given
     * @throws DuplicateKeyException    If the given name is already taken
     */
    @Transactional
    public Tag addTag(Tag tag) throws IllegalArgumentException, DuplicateKeyException {
        if (tag == null || tag.getName() == null || tag.getName().isBlank()) {
            logger.info("Invalid tag given");
            throw new IllegalArgumentException("Invalid Tag given");
        }

        if (shared.findTagByName(tag.getName()) != null) {
            logger.info("Tag name {} already taken", tag.getName());
            throw new DuplicateKeyException("Tag name " + tag.getName() + " already taken");
        }

        tag.setId(null);
        tag.setEpubs(null);

        return tags.save(tag);
    }

    // #region exists by id
    @Transactional(readOnly = true)
    public boolean existsById(long id) {
        return shared.tagExistsById(id);
    }

    // #region find by id
    @Transactional(readOnly = true)
    public Tag findById(long id) throws IllegalArgumentException {
        if (!existsById(id)) {
            throw new IllegalArgumentException("Invalid tag id given");
        }

        return shared.findTagById(id);
    }

    // #region update tag
    @Transactional
    public Tag updateTag(long id, String name, String colour, boolean overwriteNulls)
            throws IllegalArgumentException, DuplicateKeyException {
        if (!existsById(id)) {
            logger.info("Invalid tag id given");
            throw new IllegalArgumentException("Invalid tag id given");
        }

        if (name != null && !name.isBlank() && shared.findTagByName(name) != null) {
            logger.info("Trying to overwrite name with value that already exists");
            throw new DuplicateKeyException(name);
        }

        Tag tag = shared.findTagById(id);
        logger.info("Updating tag {} with name {} and colour {}", tag, name, colour);
        if (name != null && !name.isBlank()) {
            tag.setName(name);
        }

        if (!(colour == null || colour.isBlank()) || overwriteNulls) {
            tag.setColour(colour);
        }

        return tags.save(tag);
    }

    // #region delete tag
    @Transactional
    public Tag deleteById(long id) {
        if (!existsById(id)) {
            logger.info("Invalid tag id given");
            throw new IllegalArgumentException("Invalid tag id givenm");
        }

        Tag tag = findById(id);
        logger.info("Deleting tag {}", tag);
        tags.delete(tag);

        return tag;
    }

    // #region add epub to tag
    public Tag addEpubToTag(long tagId, long epubId) throws IllegalArgumentException {
        Epub epub = shared.addTagToEpub(epubId, tagId);
        if (epub == null) {
            logger.info("Tag and epub already associated");
            return null;
        }

        Tag tag = findById(tagId);
        return tag;
    }

    public Tag removeEpubFromTag(long tagId, long epubId) throws IllegalArgumentException {
        Epub epub = shared.removeTagFromEpub(epubId, tagId);
        if (epub == null) {
            logger.info("Tag and epub are not associated");
            return null;
        }

        Tag tag = findById(tagId);
        return tag;
    }
}
