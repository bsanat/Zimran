package kz.astyq.zimran.notification.mapper;

import kz.astyq.zimran.event.mapper.EventMapper;
import kz.astyq.zimran.event.model.domain.Event;
import kz.astyq.zimran.notification.model.dto.NotificationViewResponse;
import kz.astyq.zimran.notification.model.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

/**
 * {@link Notification} → view. The title is rendered by the caller (i18n); {@code event} is the catalog event the
 * notification points to, or null when it left the catalog.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, uses = EventMapper.class)
public interface NotificationMapper {

    @Mapping(target = "id", source = "notification.publicId")
    @Mapping(target = "eventId", source = "event.id")
    @Mapping(target = "title", source = "title")
    @Mapping(target = "categorySlug", source = "notification.categorySlug")
    @Mapping(target = "createdAt", source = "notification.createdAt")
    @Mapping(target = "readAt", source = "notification.readAt")
    @Mapping(target = "event", source = "event")
    NotificationViewResponse toViewResponse(Notification notification, String title, Event event);
}
