package kz.astyq.zimran.alert.service.impl;

import kz.astyq.zimran.alert.model.domain.AlertSettingsUpdate;
import kz.astyq.zimran.alert.model.dto.AlertPreferencesUpdateRequest;
import kz.astyq.zimran.alert.model.enums.AlertChannel;
import kz.astyq.zimran.alert.service.AlertPreferencesValidationService;
import kz.astyq.zimran.alert.util.MessageCode;
import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.city.service.SupportedCityService;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.util.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.EnumSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AlertPreferencesValidationServiceImpl implements AlertPreferencesValidationService {

    private final SupportedCityService supportedCityService;

    @Override
    public AlertSettingsUpdate validate(AlertPreferencesUpdateRequest request) {
        SupportedCity city = supportedCityService.getByName(request.getCity());
        Set<AlertChannel> channels = request.getChannels() == null
                ? EnumSet.of(AlertChannel.IN_APP) : EnumSet.copyOf(request.getChannels());
        channels.stream()
                .filter(channel -> !channel.isAvailable())
                .findFirst()
                .ifPresent(channel -> {
                    throw new ServiceValidationException(ErrorCode.CHANNEL_NOT_AVAILABLE,
                            MessageCode.CHANNEL_NOT_AVAILABLE, channel.getId())
                            .withDetail("field", "channels")
                            .withDetail("channel", channel.getId());
                });
        return AlertSettingsUpdate.builder()
                .cityId(city.getId())
                .categorySlugs(EnumSet.copyOf(request.getCategorySlugs()))
                .budgetMax(request.getBudgetMax())
                .frequency(request.getFrequency())
                .channels(channels)
                .paused(Boolean.TRUE.equals(request.getPaused()))
                .build();
    }
}
