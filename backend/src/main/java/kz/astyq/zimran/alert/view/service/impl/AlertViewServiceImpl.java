package kz.astyq.zimran.alert.view.service.impl;

import kz.astyq.zimran.alert.mapper.AlertPreferenceMapper;
import kz.astyq.zimran.alert.model.domain.AlertSettings;
import kz.astyq.zimran.alert.model.domain.AlertSettingsUpdate;
import kz.astyq.zimran.alert.model.dto.AlertPreferencesDetailsViewResponse;
import kz.astyq.zimran.alert.model.dto.AlertPreferencesSaveViewResponse;
import kz.astyq.zimran.alert.model.dto.AlertPreferencesUpdateRequest;
import kz.astyq.zimran.alert.service.AlertPreferenceService;
import kz.astyq.zimran.alert.service.AlertPreferencesValidationService;
import kz.astyq.zimran.alert.view.service.AlertViewService;
import kz.astyq.zimran.city.service.SupportedCityService;
import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.subscription.service.EntitlementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AlertViewServiceImpl implements AlertViewService {

    private final AlertPreferenceService alertPreferenceService;
    private final AlertPreferencesValidationService alertPreferencesValidationService;
    private final EntitlementService entitlementService;
    private final SupportedCityService supportedCityService;
    private final AlertPreferenceMapper mapper;

    @Override
    public AlertPreferencesDetailsViewResponse getPreferences(RequestScope scope) {
        Optional<AlertSettings> stored = alertPreferenceService.find(scope);
        AlertSettings settings = stored.orElseGet(() ->
                AlertSettings.defaults(scope, supportedCityService.getDefault().getId()));
        return AlertPreferencesDetailsViewResponse.builder()
                .preferences(mapper.toViewResponse(settings))
                .premiumRequired(!entitlementService.isPremium(scope))
                .configured(stored.isPresent())
                .build();
    }

    @Override
    public AlertPreferencesSaveViewResponse updatePreferences(AlertPreferencesUpdateRequest request,
                                                              RequestScope scope) {
        entitlementService.requirePremium(scope);
        AlertSettingsUpdate update = alertPreferencesValidationService.validate(request);
        AlertSettings saved = alertPreferenceService.save(scope, update);
        return AlertPreferencesSaveViewResponse.builder()
                .saved(true)
                .preferences(mapper.toViewResponse(saved))
                .build();
    }
}
