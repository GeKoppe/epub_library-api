package org.koppe.epub.api.epub_library_api.jpa.service;

import java.util.Optional;

import org.koppe.epub.api.epub_library_api.exceptions.DuplicateKeyException;
import org.koppe.epub.api.epub_library_api.jpa.model.EpubSeries;
import org.koppe.epub.api.epub_library_api.jpa.repository.EpubSeriesRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SeriesService {
    private final Logger logger = LoggerFactory.getLogger(SeriesService.class);
    private final EpubSeriesRepository series;
    private final SharedService shared;

    // #region exists by id
    public boolean existsById(long id) {
        return shared.seriesExistsById(id);
    }

    // #region find by id
    public EpubSeries findById(long id) throws IllegalArgumentException {
        return shared.findSeriesById(id);
    }

    // #region find series
    @Transactional
    public EpubSeries addEpubSeries(String name) throws DuplicateKeyException {
        if (name == null || name.isBlank()) {
            logger.info("No name given");
            throw new IllegalArgumentException("Invalid series id given");
        }

        if (series.findByName(name).isPresent()) {
            logger.info("Name {} already taken");
            throw new DuplicateKeyException(name);
        }
        EpubSeries es = new EpubSeries();
        es.setName(name);
        return series.save(es);
    }

    // #region update series
    @Transactional
    public EpubSeries updateSeries(long id, String name) throws IllegalArgumentException, DuplicateKeyException {
        if (!existsById(id) || name == null || name.isBlank()) {
            logger.info("Invalid name or id given");
            throw new IllegalArgumentException("Invalid name or id given");
        }

        EpubSeries es = findById(id);
        Optional<EpubSeries> esOpt = series.findByName(name);
        if (esOpt.isPresent() && !esOpt.get().equals(es)) {
            logger.info("Name {} already taken", name);
            throw new DuplicateKeyException(name);
        }

        es.setName(name);
        return series.save(es);
    }

    // #region delete
    @Transactional
    public EpubSeries delete(long id) throws IllegalArgumentException {
        if (!existsById(id)) {
            logger.info("Invalid id given");
            throw new IllegalArgumentException("Invalid id given");
        }

        EpubSeries es = findById(id);
        logger.info("Deleting {}", es);
        series.delete(es);
        return es;
    }

    
}
