package kz.astyq.zimran.alert.service;

import kz.astyq.zimran.alert.model.domain.AlertSettingsUpdate;
import kz.astyq.zimran.alert.model.dto.AlertPreferencesUpdateRequest;

/**
 * Turns a bean-validated preferences request into an {@link AlertSettingsUpdate}.
 */
public interface AlertPreferencesValidationService {

    /**
     * Unsupported city: {@code 400 VALIDATION_ERROR} ({@code details.field=city}). A channel that is not available
     * yet (email): {@code 422 CHANNEL_NOT_AVAILABLE} ({@code details.field=channels}, {@code details.channel}).
     * Missing channels default to {@code in_app}, missing {@code paused} to false.
     */
    AlertSettingsUpdate validate(AlertPreferencesUpdateRequest request);
}
