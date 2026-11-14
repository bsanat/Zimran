package kz.astyq.zimran.moderation.service.impl;

import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.moderation.model.entity.Report;
import kz.astyq.zimran.moderation.model.enums.ModerationTargetKind;
import kz.astyq.zimran.moderation.model.enums.ReportReason;
import kz.astyq.zimran.moderation.model.enums.ReportTargetType;
import kz.astyq.zimran.moderation.repository.ReportRepository;
import kz.astyq.zimran.moderation.service.ModerationTargetResolver;
import kz.astyq.zimran.moderation.service.ReportService;
import kz.astyq.zimran.moderation.util.MessageCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final ModerationTargetResolver moderationTargetResolver;

    @Override
    @Transactional
    public void report(RequestScope scope, ReportTargetType targetType, String targetId, ReportReason reason,
                       String comment) {
        String reported = author(targetType, targetId).orElseThrow(() -> new ServiceValidationException(ErrorCode.NOT_FOUND, MessageCode.TARGET_NOT_FOUND).withDetail("field", "targetId"));
        if (reported.equals(scope.getScopeId())) {
            throw new ServiceValidationException(ErrorCode.VALIDATION_ERROR, MessageCode.CANNOT_REPORT_SELF)
                    .withDetail("field", "targetId");
        }
        reportRepository.save(Report.builder()
                .reporterScopeId(scope.getScopeId())
                .targetType(targetType)
                .targetId(targetId)
                .reportedScopeId(reported)
                .reason(reason)
                .comment(StringUtils.hasText(comment) ? comment.strip() : null)
                .build());
    }

    /** A SCOPE report names one of the person's posts or requests. */
    private Optional<String> author(ReportTargetType targetType, String targetId) {
        return switch (targetType) {
            case POST -> moderationTargetResolver.authorScopeId(ModerationTargetKind.POST, targetId);
            case REQUEST -> moderationTargetResolver.authorScopeId(ModerationTargetKind.REQUEST, targetId);
            case SCOPE -> moderationTargetResolver.authorScopeId(ModerationTargetKind.POST, targetId)
                    .or(() -> moderationTargetResolver.authorScopeId(ModerationTargetKind.REQUEST, targetId));
        };
    }
}
