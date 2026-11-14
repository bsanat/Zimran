package kz.astyq.zimran.buddy.mapper;

import kz.astyq.zimran.buddy.model.domain.BuddyPostDraft;
import kz.astyq.zimran.buddy.model.domain.BuddyRequestDraft;
import kz.astyq.zimran.buddy.model.dto.BuddyIncomingRequestViewResponse;
import kz.astyq.zimran.buddy.model.dto.BuddyOutgoingRequestViewResponse;
import kz.astyq.zimran.buddy.model.dto.BuddyPostCreateRequest;
import kz.astyq.zimran.buddy.model.dto.BuddyPostViewResponse;
import kz.astyq.zimran.buddy.model.dto.BuddyRequestCreateRequest;
import kz.astyq.zimran.buddy.model.entity.BuddyPost;
import kz.astyq.zimran.buddy.model.entity.BuddyRequest;
import kz.astyq.zimran.buddy.model.enums.BuddyPostStatus;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.springframework.util.StringUtils;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface BuddyMapper {

    /** Every text stripped; a blank optional text becomes null. */
    default BuddyPostDraft toDraft(BuddyPostCreateRequest request) {
        return BuddyPostDraft.builder()
                .eventId(request.getEventId().strip())
                .displayName(request.getDisplayName().strip())
                .shortBio(request.getShortBio().strip())
                .language(optional(request.getLanguage()))
                .preferences(optional(request.getPreferences()))
                .ageMin(request.getAgeMin())
                .ageMax(request.getAgeMax())
                .build();
    }

    default BuddyRequestDraft toDraft(BuddyRequestCreateRequest request) {
        return BuddyRequestDraft.builder()
                .displayName(request.getDisplayName().strip())
                .message(request.getMessage().strip())
                .build();
    }

    @Mapping(target = "id", source = "post.publicId")
    @Mapping(target = "status", source = "status")
    @Mapping(target = "own", source = "own")
    BuddyPostViewResponse toViewResponse(BuddyPost post, BuddyPostStatus status, boolean own);

    @Mapping(target = "id", source = "request.publicId")
    @Mapping(target = "displayName", source = "request.senderDisplayName")
    @Mapping(target = "safetyNotice", source = "safetyNotice")
    BuddyIncomingRequestViewResponse toIncomingViewResponse(BuddyRequest request, String safetyNotice);

    @Mapping(target = "id", source = "request.publicId")
    @Mapping(target = "displayName", source = "request.senderDisplayName")
    @Mapping(target = "message", source = "request.message")
    @Mapping(target = "status", source = "request.status")
    @Mapping(target = "createdAt", source = "request.createdAt")
    @Mapping(target = "resolvedAt", source = "request.resolvedAt")
    @Mapping(target = "post", source = "post")
    @Mapping(target = "safetyNotice", source = "safetyNotice")
    BuddyOutgoingRequestViewResponse toOutgoingViewResponse(BuddyRequest request, BuddyPostViewResponse post,
                                                            String safetyNotice);

    private static String optional(String text) {
        return StringUtils.hasText(text) ? text.strip() : null;
    }
}
