package kz.astyq.zimran.event.model.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import kz.astyq.zimran.event.model.enums.Interest;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Stores interests as their ids, comma separated in declaration order ({@code music,art}). Unknown ids read back
 * are dropped.
 */
@Converter
public class InterestSetConverter implements AttributeConverter<Set<Interest>, String> {

    private static final String SEPARATOR = ",";

    @Override
    public String convertToDatabaseColumn(Set<Interest> interests) {
        if (interests == null || interests.isEmpty()) {
            return "";
        }
        return EnumSet.copyOf(interests).stream().map(Interest::getId).collect(Collectors.joining(SEPARATOR));
    }

    @Override
    public Set<Interest> convertToEntityAttribute(String value) {
        Set<Interest> interests = EnumSet.noneOf(Interest.class);
        if (value != null) {
            Arrays.stream(value.split(SEPARATOR)).map(Interest::fromId).flatMap(Optional::stream).forEach(interests::add);
        }
        return Collections.unmodifiableSet(interests);
    }
}
