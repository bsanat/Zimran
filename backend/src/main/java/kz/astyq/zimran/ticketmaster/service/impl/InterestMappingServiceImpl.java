package kz.astyq.zimran.ticketmaster.service.impl;

import kz.astyq.zimran.core.configuration.InterestMappingProperties;
import kz.astyq.zimran.core.configuration.InterestMappingProperties.ClassificationRule;
import kz.astyq.zimran.event.model.enums.Interest;
import kz.astyq.zimran.ticketmaster.model.dto.TmClassification;
import kz.astyq.zimran.ticketmaster.model.dto.TmNamed;
import kz.astyq.zimran.ticketmaster.service.InterestMappingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InterestMappingServiceImpl implements InterestMappingService {

    private final InterestMappingProperties properties;

    @Override
    public List<Interest> resolve(List<TmClassification> classifications) {
        if (classifications == null || classifications.isEmpty()) {
            return List.of();
        }
        List<TmClassification> present = classifications.stream().filter(Objects::nonNull).toList();
        List<Interest> interests = new ArrayList<>();
        for (Interest interest : Interest.values()) {
            boolean matched = properties.rulesFor(interest).stream()
                    .anyMatch(rule -> present.stream().anyMatch(c -> matches(rule, c)));
            if (matched) {
                interests.add(interest);
            }
        }
        return List.copyOf(interests);
    }

    @Override
    public List<String> classificationNamesFor(Interest interest) {
        Set<String> names = new LinkedHashSet<>();
        for (ClassificationRule rule : properties.rulesFor(interest)) {
            String name = firstText(rule.getSubGenre(), rule.getGenre(), rule.getSegment());
            if (name != null) {
                names.add(name.trim());
            }
        }
        return List.copyOf(names);
    }

    @Override
    public boolean hasCoverage(Interest interest) {
        return !properties.rulesFor(interest).isEmpty();
    }

    private static boolean matches(ClassificationRule rule, TmClassification classification) {
        return fieldMatches(rule.getSegment(), classification.getSegment())
                && fieldMatches(rule.getGenre(), classification.getGenre())
                && fieldMatches(rule.getSubGenre(), classification.getSubGenre());
    }

    private static boolean fieldMatches(String expected, TmNamed actual) {
        if (!StringUtils.hasText(expected)) {
            return true;
        }
        return actual != null && actual.getName() != null && expected.trim().equalsIgnoreCase(actual.getName().trim());
    }

    private static String firstText(String... values) {
        for (String value : values) {
            if (StringUtils.hasText(value)) {
                return value;
            }
        }
        return null;
    }
}
