package kz.astyq.zimran.jsonsource.service;

import kz.astyq.zimran.event.model.enums.Interest;

import java.util.Collection;
import java.util.List;

/**
 * Maps the JSON source's category names to {@link Interest}s ({@code app.events-source.category-mapping}).
 */
public interface CategoryMappingService {

    /**
     * Distinct interests of {@code categories}, in the order the categories come; the first is the event's
     * primary category. A category equal to an interest id maps to it directly; unknown categories are ignored.
     */
    List<Interest> interestsFor(Collection<String> categories);
}
