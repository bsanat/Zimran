package kz.astyq.zimran.jsonsource.service.impl;

import kz.astyq.zimran.core.configuration.EventSourceProperties;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.jsonsource.service.CategoryMappingService;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

@Service
public class CategoryMappingServiceImpl implements CategoryMappingService {

    /** Normalized source category name -> interests it counts as, in configuration order. */
    private final Map<String, List<Interest>> byCategory = new HashMap<>();

    public CategoryMappingServiceImpl(EventSourceProperties properties) {
        for (Interest interest : Interest.values()) {
            for (String name : properties.categoriesFor(interest)) {
                if (StringUtils.hasText(name)) {
                    byCategory.computeIfAbsent(normalize(name), key -> new ArrayList<>()).add(interest);
                }
            }
        }
    }

    @Override
    public List<Interest> interestsFor(Collection<String> categories) {
        if (categories == null) {
            return List.of();
        }
        Set<Interest> interests = new LinkedHashSet<>();
        categories.stream().filter(Objects::nonNull).filter(StringUtils::hasText).forEach(category -> {
            Optional<Interest> direct = Interest.fromId(category);
            direct.ifPresent(interests::add);
            interests.addAll(byCategory.getOrDefault(normalize(category), List.of()));
        });
        return List.copyOf(interests);
    }

    private static String normalize(String name) {
        return name.strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
    }
}
