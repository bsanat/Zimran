package kz.astyq.zimran.event.model.entity.converter;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import kz.astyq.zimran.event.model.domain.EventSchedule;
import tools.jackson.databind.json.JsonMapper;

/**
 * Stores an {@link EventSchedule} as JSON text (ISO dates, enum names).
 */
@Converter
public class EventScheduleConverter implements AttributeConverter<EventSchedule, String> {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Override
    public String convertToDatabaseColumn(EventSchedule schedule) {
        return schedule != null ? JSON.writeValueAsString(schedule) : null;
    }

    @Override
    public EventSchedule convertToEntityAttribute(String value) {
        return value != null && !value.isBlank() ? JSON.readValue(value, EventSchedule.class) : null;
    }
}
