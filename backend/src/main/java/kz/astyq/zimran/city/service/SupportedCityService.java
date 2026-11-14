package kz.astyq.zimran.city.service;

import kz.astyq.zimran.city.model.domain.SupportedCity;

import java.util.List;
import java.util.Optional;

/**
 * Cities the planner supports (configured in {@code app.city}).
 */
public interface SupportedCityService {

    /**
     * All supported cities, in configuration order.
     */
    List<SupportedCity> getAll();

    Optional<SupportedCity> findById(String id);

    /**
     * City whose name equals {@code name}, ignoring case and surrounding whitespace.
     */
    Optional<SupportedCity> findByName(String name);

    /**
     * City by name or id (both ignoring case). Unknown or blank:
     * {@code ServiceValidationException(VALIDATION_ERROR, CITY_NOT_SUPPORTED)} with detail {@code field=city}.
     */
    SupportedCity getByName(String name);

    /**
     * The pilot city; always present.
     */
    SupportedCity getDefault();
}
