package kz.astyq.zimran.alert.view.service;

import kz.astyq.zimran.alert.model.dto.AlertPreferencesDetailsViewResponse;
import kz.astyq.zimran.alert.model.dto.AlertPreferencesSaveViewResponse;
import kz.astyq.zimran.alert.model.dto.AlertPreferencesUpdateRequest;
import kz.astyq.zimran.core.model.domain.RequestScope;

public interface AlertViewService {

    /**
     * The caller's preferences, or the defaults when none are saved; {@code premiumRequired} for a free scope.
     */
    AlertPreferencesDetailsViewResponse getPreferences(RequestScope scope);

    /**
     * Saves the caller's preferences. Free scope: {@code 403 PREMIUM_REQUIRED}.
     */
    AlertPreferencesSaveViewResponse updatePreferences(AlertPreferencesUpdateRequest request, RequestScope scope);
}
