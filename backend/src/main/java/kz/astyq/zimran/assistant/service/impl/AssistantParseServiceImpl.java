package kz.astyq.zimran.assistant.service.impl;

import kz.astyq.zimran.ai.model.domain.AiExtraction;
import kz.astyq.zimran.ai.model.domain.AiExtractionRequest;
import kz.astyq.zimran.ai.service.AiFilterExtractionService;
import kz.astyq.zimran.assistant.model.domain.AssistantFilters;
import kz.astyq.zimran.assistant.service.AssistantFilterResolutionService;
import kz.astyq.zimran.assistant.service.AssistantParseService;
import kz.astyq.zimran.city.model.domain.SupportedCity;
import kz.astyq.zimran.event.model.enums.CategorySlug;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AssistantParseServiceImpl implements AssistantParseService {

    static final List<String> CATEGORY_IDS = Arrays.stream(CategorySlug.values()).map(CategorySlug::getId).toList();

    private final AiFilterExtractionService aiFilterExtractionService;
    private final AssistantFilterResolutionService assistantFilterResolutionService;
    private final Clock clock;

    @Override
    public AssistantFilters parse(String message, String requestedCity) {
        SupportedCity referenceCity = assistantFilterResolutionService.referenceCity(requestedCity);
        AiExtraction extraction = aiFilterExtractionService.extract(AiExtractionRequest.builder()
                .message(message)
                .today(LocalDate.now(clock.withZone(referenceCity.getZone())))
                .categories(CATEGORY_IDS)
                .build());
        return assistantFilterResolutionService.resolve(extraction, requestedCity);
    }
}
