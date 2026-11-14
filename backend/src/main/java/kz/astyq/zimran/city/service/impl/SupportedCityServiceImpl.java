package kz.astyq.zimran.city.service.impl;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.city.service.SupportedCityService;
import kz.astyq.zimran.city.util.MessageCode;
import kz.astyq.zimran.core.configuration.CityProperties;
import kz.astyq.zimran.core.configuration.CityProperties.CityDefinition;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class SupportedCityServiceImpl implements SupportedCityService {

    private final CityProperties properties;

    @Override
    public List<SupportedCity> getAll() {
        return properties.getSupported().stream().map(SupportedCityServiceImpl::toDomain).toList();
    }

    @Override
    public Optional<SupportedCity> findById(String id) {
        if (!StringUtils.hasText(id)) {
            return Optional.empty();
        }
        String wanted = id.trim();
        return getAll().stream().filter(city -> city.getId().equalsIgnoreCase(wanted)).findFirst();
    }

    @Override
    public Optional<SupportedCity> findByName(String name) {
        if (!StringUtils.hasText(name)) {
            return Optional.empty();
        }
        String wanted = name.trim().toLowerCase(Locale.ROOT);
        return getAll().stream()
                .filter(city -> city.getName().toLowerCase(Locale.ROOT).equals(wanted))
                .findFirst();
    }

    @Override
    public Optional<SupportedCity> findByName(String name, String state) {
        if (!StringUtils.hasText(state)) {
            return findByName(name);
        }
        if (!StringUtils.hasText(name)) {
            return Optional.empty();
        }
        String wantedName = name.trim().toLowerCase(Locale.ROOT);
        String wantedState = state.trim();
        return getAll().stream()
                .filter(city -> city.getName().toLowerCase(Locale.ROOT).equals(wantedName))
                .filter(city -> city.getStateCode() != null && city.getStateCode().trim().equalsIgnoreCase(wantedState))
                .findFirst();
    }

    @Override
    public SupportedCity getByName(String name, String state) {
        if (!StringUtils.hasText(state)) {
            return getByName(name);
        }
        Optional<SupportedCity> byNameAndState = findByName(name, state)
                .or(() -> findById(name).filter(city -> state.trim().equalsIgnoreCase(city.getStateCode())));
        if (byNameAndState.isPresent()) {
            return byNameAndState.get();
        }
        getByName(name); // unknown city: CITY_NOT_SUPPORTED
        throw new ServiceValidationException(ErrorCode.VALIDATION_ERROR, MessageCode.STATE_NOT_SUPPORTED)
                .withDetail("field", "state");
    }

    @Override
    public SupportedCity getByName(String name) {
        return findByName(name)
                .or(() -> findById(name))
                .orElseThrow(() -> new ServiceValidationException(ErrorCode.VALIDATION_ERROR,
                        MessageCode.CITY_NOT_SUPPORTED).withDetail("field", "city"));
    }

    @Override
    public SupportedCity getDefault() {
        return findById(properties.getDefaultId())
                .orElseThrow(() -> new IllegalStateException("app.city.default-id is not a supported city"));
    }

    private static SupportedCity toDomain(CityDefinition definition) {
        return SupportedCity.builder()
                .id(definition.getId().trim())
                .name(definition.getName().trim())
                .stateCode(definition.getStateCode())
                .countryCode(definition.getCountryCode())
                .zone(definition.getZone())
                .build();
    }
}
