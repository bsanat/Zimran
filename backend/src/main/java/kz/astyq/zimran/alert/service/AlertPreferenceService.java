package kz.astyq.zimran.alert.service;

import kz.astyq.zimran.alert.model.domain.AlertSettings;
import kz.astyq.zimran.alert.model.domain.AlertSettingsUpdate;
import kz.astyq.zimran.core.model.domain.RequestScope;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Stored alert preferences. Entitlement checks are the caller's job.
 */
public interface AlertPreferenceService {

    Optional<AlertSettings> find(RequestScope scope);

    /**
     * Creates or replaces the scope's preferences. {@code enabledAt} is set to now when alerts become enabled: on
     * the first save that is not paused, and on every un-pause.
     */
    AlertSettings save(RequestScope scope, AlertSettingsUpdate update);

    /**
     * Active preferences ({@link AlertSettings#isActive()}) in any of {@code cityIds}.
     */
    List<AlertSettings> findActiveInCities(Collection<String> cityIds);
}
