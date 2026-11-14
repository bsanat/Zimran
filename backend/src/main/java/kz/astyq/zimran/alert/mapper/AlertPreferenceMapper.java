package kz.astyq.zimran.alert.mapper;

import kz.astyq.zimran.alert.model.domain.AlertSettings;
import kz.astyq.zimran.alert.model.domain.AlertSettingsUpdate;
import kz.astyq.zimran.alert.model.dto.AlertPreferencesViewResponse;
import kz.astyq.zimran.alert.model.entity.AlertPreference;
import kz.astyq.zimran.alert.model.enums.AlertChannel;
import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.event.model.enums.CategorySlug;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * {@link AlertPreference} ⇄ {@link AlertSettings}. Scope and {@code enabledAt} are managed by the preference
 * service, never here.
 */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AlertPreferenceMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "scopeId", ignore = true)
    @Mapping(target = "scopeKind", ignore = true)
    @Mapping(target = "enabledAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "deletedAt", ignore = true)
    @Mapping(target = "city", source = "cityId")
    @Mapping(target = "categorySlugs", source = "categorySlugs", qualifiedByName = "slugNames")
    @Mapping(target = "channels", source = "channels", qualifiedByName = "channelNames")
    void updateEntity(AlertSettingsUpdate update, @MappingTarget AlertPreference entity);

    @Mapping(target = "scope", expression = "java(toScope(entity))")
    @Mapping(target = "cityId", source = "city")
    @Mapping(target = "categorySlugs", source = "categorySlugs", qualifiedByName = "slugs")
    @Mapping(target = "channels", source = "channels", qualifiedByName = "channels")
    AlertSettings toDomain(AlertPreference entity);

    @Mapping(target = "city", source = "cityId")
    @Mapping(target = "categorySlugs", source = "categorySlugs", qualifiedByName = "slugList")
    @Mapping(target = "channels", source = "channels", qualifiedByName = "channelList")
    AlertPreferencesViewResponse toViewResponse(AlertSettings settings);

    default RequestScope toScope(AlertPreference entity) {
        return RequestScope.builder().scopeId(entity.getScopeId()).kind(entity.getScopeKind()).build();
    }

    /**
     * A mutable list: MapStruct updates a loaded entity's list in place.
     */
    @Named("slugNames")
    default List<String> slugNames(Set<CategorySlug> slugs) {
        return slugs.stream().sorted().map(Enum::name).collect(Collectors.toCollection(ArrayList::new));
    }

    @Named("channelNames")
    default List<String> channelNames(Set<AlertChannel> channels) {
        return channels.stream().sorted().map(Enum::name).collect(Collectors.toCollection(ArrayList::new));
    }

    /**
     * Unknown stored names are skipped, so a value removed later never breaks reads.
     */
    @Named("slugs")
    default Set<CategorySlug> slugs(Collection<String> names) {
        Set<CategorySlug> slugs = EnumSet.noneOf(CategorySlug.class);
        if (names != null) {
            names.forEach(name -> CategorySlug.fromId(name).ifPresent(slugs::add));
        }
        return slugs;
    }

    @Named("channels")
    default Set<AlertChannel> channels(Collection<String> names) {
        Set<AlertChannel> channels = EnumSet.noneOf(AlertChannel.class);
        if (names != null) {
            names.forEach(name -> AlertChannel.fromId(name).ifPresent(channels::add));
        }
        return channels;
    }

    @Named("slugList")
    default List<CategorySlug> slugList(Set<CategorySlug> slugs) {
        return slugs.stream().sorted().toList();
    }

    @Named("channelList")
    default List<AlertChannel> channelList(Set<AlertChannel> channels) {
        return channels.stream().sorted().toList();
    }
}
