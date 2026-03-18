package org.koppe.epub.api.epub_library_api.jpa.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.koppe.epub.api.epub_library_api.exceptions.DuplicateKeyException;
import org.koppe.epub.api.epub_library_api.exceptions.MediaTypeException;
import org.koppe.epub.api.epub_library_api.jpa.model.Franchise;
import org.koppe.epub.api.epub_library_api.jpa.repository.FranchiseRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FranchiseService {
    /**
     * Logger
     */
    private final Logger logger = LoggerFactory.getLogger(FranchiseService.class);
    /**
     * JPA repository for working with franchises
     */
    private final FranchiseRepository franchises;
    /**
     * Shared service
     */
    private final SharedService shared;
    /**
     * Base file path for images
     */
    private final String BASE_FILE_PATH = (System.getenv("LIBRARY_DATA_PATH") != null
            ? System.getenv("LIBRARY_DATA_PATH")
            : "./data/epub-library") + "/franchises";

    public boolean existsById(long id) {
        return franchises.existsById(id);
    }

    public Franchise findById(long id) {
        return shared.findFranchiseById(id);
    }

    // #region add franchise
    /**
     * Adds franchise to database
     * 
     * @param name Name of the new franchise
     * @return Added franchise
     * @throws IllegalArgumentException If no name is given
     * @throws DuplicateKeyException    If the given name already exists in the
     *                                  database
     */
    @Transactional
    public Franchise addFranchise(String name) throws IllegalArgumentException, DuplicateKeyException {
        if (name == null || name.isBlank()) {
            logger.info("No vaid name given");
            throw new IllegalArgumentException("No vaid name given");
        }

        if (franchises.findByName(name).isPresent()) {
            logger.info("Name {} already taken", name);
            throw new DuplicateKeyException(name);
        }

        Franchise franchise = new Franchise();
        franchise.setName(name);
        return franchises.save(franchise);
    }

    // #region update franchise
    /**
     * Updates name of franchise with given id
     * 
     * @param id   Id of the franchise to udpate
     * @param name New name for the franchise1
     * @return Updated ranchise
     * @throws IllegalArgumentException Id does not exist in the database or no name
     *                                  given
     * @throws DuplicateKeyException    Given name already exists in the system
     */
    @Transactional
    public Franchise updateFranchise(long id, String name) throws IllegalArgumentException, DuplicateKeyException {
        if (!franchises.existsById(id)) {
            logger.info("Franchise with id {} does not exist", id);
            throw new IllegalArgumentException();
        }

        if (name == null || name.isBlank()) {
            logger.info("No valid name given");
            throw new IllegalArgumentException("No valid name given");
        }

        if (franchises.findByName(name).isPresent()) {
            logger.info("Name {} already taken", name);
            throw new DuplicateKeyException(name);
        }

        Franchise franchise = franchises.findById(id).get();
        franchise.setName(name);
        return franchises.save(franchise);
    }

    // #region delete by id
    @Transactional
    public Franchise deleteFranchise(long id) {
        if (!franchises.existsById(id)) {
            logger.info("Franchise with id {} does not exist", id);
            throw new IllegalArgumentException();
        }

        Franchise franchise = franchises.findById(id).get();
        franchises.delete(franchise);
        return franchise;
    }

    // #region add epub to franchise
    public Franchise addEpubToFranchise(long franchiseId, long epubId) throws IllegalArgumentException {
        return shared.addEpubToFranchise(franchiseId, epubId);
    }

    // #region remove epub from franchise
    public Franchise removeEpubFromFranchise(long franchiseId, long epubId) throws IllegalArgumentException {
        return shared.removeEpubFromFranchise(franchiseId, epubId);
    }

    // #region download image
    public void uploadImage(long franchiseId, MultipartFile file, boolean overwrite)
            throws IOException, MediaTypeException {
        if (!existsById(franchiseId)) {
            logger.info("Invalid franchise id given");
            throw new IllegalArgumentException("Invalid franchise id given");
        }

        if (file == null || file.isEmpty() || file.getSize() == 0) {
            logger.info("No valid file given");
            throw new IOException("No file given");
        }

        MediaType uploadMediaType = MediaType.parseMediaType(file.getContentType());
        if (!List.of(MediaType.IMAGE_PNG, MediaType.IMAGE_JPEG).contains(uploadMediaType)) {
            logger.info("Invalid media type {}", uploadMediaType);
            throw new MediaTypeException("Invalid mediatype", null,
                    List.of(MediaType.IMAGE_PNG, MediaType.IMAGE_JPEG).toArray(MediaType[]::new), uploadMediaType);
        }

        File directory = prepareDataPath(franchiseId);
        if (!directory.exists()) {
            logger.info("Data path for franchise images does not exist");
            throw new IOException("Data path for franchise images does not exist");
        }

        File img = new File(directory.toPath().toString() + "/cover"
                + file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf(".")));

        if (!img.exists()) {
            img.createNewFile();
        } else {
            if (!overwrite) {
                throw new IllegalStateException("Cover for given  franchise already exists");
            }
        }
        OutputStream os = new FileOutputStream(img);
        os.write(file.getBytes());
        IOUtils.closeQuietly(os);

        return;
    }

    // #region prepare data path
    private File prepareDataPath(long id) throws IOException {
        File dir = new File(BASE_FILE_PATH + "/" + id);
        if (dir.exists()) {
            return dir;
        }

        logger.info("Creating data path for franchise images");
        boolean result = dir.mkdirs();
        if (!result) {
            logger.info("Could not create data path");
            throw new IOException("Could not create data path");
        }
        return dir;
    }

    public File downloadImage(long id) throws IOException {
        if (!existsById(id)) {
            throw new IllegalArgumentException();
        }

        File dir = prepareDataPath(id);
        if (!dir.exists() || !dir.isDirectory()) {
            return null;
        }

        for (var x : dir.listFiles()) {
            if (x.getName().contains("cover"))
                return x;
        }
        return null;
    }
}
