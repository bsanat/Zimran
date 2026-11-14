package kz.astyq.zimran.assistant.service;

import kz.astyq.zimran.ai.model.domain.AiExtraction;
import kz.astyq.zimran.assistant.model.domain.AssistantFilters;
import kz.astyq.zimran.city.model.domain.SupportedCity;

/**
 * Server-side validation of filters the model read from a guest message. Lenient: an invalid value never fails
 * the request, it becomes null and is reported in {@code missingFields}.
 */
public interface AssistantFilterResolutionService {

    /**
     * The supported city {@code requestedCity} names (id or name), else the default city. Its timezone defines
     * "today" for the model.
     */
    SupportedCity referenceCity(String requestedCity);

    /**
     * Checks every field:
     * <ul>
     *   <li>city: the city the message names, if supported (an unsupported one is not replaced); when the message
     *       names none, {@code requestedCity} if supported;</li>
     *   <li>interests: known category slugs only, unknown ones dropped;</li>
     *   <li>dates: real {@code YYYY-MM-DD} days, {@code dateFrom} today or later in the city's timezone,
     *       {@code dateTo} not before {@code dateFrom} and within {@code app.planner.max-range-days}; a missing
     *       {@code dateTo} means the single day {@code dateFrom};</li>
     *   <li>time of day: {@code any}, {@code morning}, {@code afternoon} or {@code evening};</li>
     *   <li>budget: {@code >= 0}.</li>
     * </ul>
     * City, interests and both dates are required; time of day and budget are optional (null = any / no limit)
     * and only listed as missing when the model gave an invalid value.
     */
    AssistantFilters resolve(AiExtraction extraction, String requestedCity);
}
