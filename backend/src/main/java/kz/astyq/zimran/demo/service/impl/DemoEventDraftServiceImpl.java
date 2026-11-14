package kz.astyq.zimran.demo.service.impl;

import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.city.service.SupportedCityService;
import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.i18n.MessageService;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.demo.model.dto.DemoEventCreateRequest;
import kz.astyq.zimran.demo.service.DemoEventDraftService;
import kz.astyq.zimran.demo.util.MessageCode;
import kz.astyq.zimran.event.model.domain.DemoEventDraft;
import kz.astyq.zimran.event.model.enums.CategorySlug;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class DemoEventDraftServiceImpl implements DemoEventDraftService {

    static final LocalTime DEFAULT_START_TIME = LocalTime.of(19, 0);

    private final SupportedCityService supportedCityService;
    private final MessageService messageService;
    private final Clock clock;

    @Override
    public DemoEventDraft resolve(DemoEventCreateRequest request) {
        SupportedCity city = StringUtils.hasText(request.getCity())
                ? supportedCityService.getByName(request.getCity()) : supportedCityService.getDefault();
        Instant now = clock.instant();
        return DemoEventDraft.builder()
                .categorySlug(request.getCategorySlug())
                .city(city)
                .title(title(request))
                .startAt(startAt(request.getStartAt(), city, now))
                .priceMax(request.getPriceMax())
                .build();
    }

    private String title(DemoEventCreateRequest request) {
        if (StringUtils.hasText(request.getTitle())) {
            return request.getTitle().strip();
        }
        CategorySlug slug = request.getCategorySlug();
        String category = messageService.getMessage(kz.astyq.zimran.meta.util.MessageCode.category(slug));
        return messageService.getMessage(MessageCode.DEMO_EVENT_TITLE, category);
    }

    private static OffsetDateTime startAt(OffsetDateTime requested, SupportedCity city, Instant now) {
        if (requested == null) {
            LocalDate tomorrow = LocalDate.ofInstant(now, city.getZone()).plusDays(1);
            return tomorrow.atTime(DEFAULT_START_TIME).atZone(city.getZone()).toOffsetDateTime();
        }
        if (!requested.toInstant().isAfter(now)) {
            throw new ServiceValidationException(ErrorCode.VALIDATION_ERROR, MessageCode.DEMO_START_IN_PAST)
                    .withDetail("field", "startAt");
        }
        return requested.atZoneSameInstant(city.getZone()).toOffsetDateTime();
    }
}
