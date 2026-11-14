package kz.astyq.zimran.alert.service.impl;

import kz.astyq.zimran.alert.mapper.AlertPreferenceMapper;
import kz.astyq.zimran.alert.model.domain.AlertSettings;
import kz.astyq.zimran.alert.model.domain.AlertSettingsUpdate;
import kz.astyq.zimran.alert.model.entity.AlertPreference;
import kz.astyq.zimran.alert.repository.AlertPreferenceRepository;
import kz.astyq.zimran.alert.service.AlertPreferenceService;
import kz.astyq.zimran.core.model.domain.RequestScope;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class AlertPreferenceServiceImpl implements AlertPreferenceService {

    private final AlertPreferenceRepository repository;
    private final AlertPreferenceMapper mapper;
    private final Clock clock;

    @Override
    public Optional<AlertSettings> find(RequestScope scope) {
        return repository.findByScopeId(scope.getScopeId()).map(mapper::toDomain);
    }

    @Override
    @Transactional
    public AlertSettings save(RequestScope scope, AlertSettingsUpdate update) {
        AlertPreference entity = repository.findByScopeId(scope.getScopeId())
                .orElseGet(() -> AlertPreference.builder()
                        .scopeId(scope.getScopeId())
                        .scopeKind(scope.getKind())
                        .build());
        boolean wasEnabled = entity.getId() != null && !entity.isPaused() && entity.getEnabledAt() != null;
        mapper.updateEntity(update, entity);
        if (!update.isPaused() && !wasEnabled) {
            entity.setEnabledAt(clock.instant());
        }
        return mapper.toDomain(repository.save(entity));
    }

    @Override
    public List<AlertSettings> findActiveInCities(Collection<String> cityIds) {
        if (cityIds == null || cityIds.isEmpty()) {
            return List.of();
        }
        return repository.findActiveInCities(cityIds).stream().map(mapper::toDomain).toList();
    }
}
