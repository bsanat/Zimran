package kz.astyq.zimran.moderation.service;

import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.moderation.model.enums.ReportReason;
import kz.astyq.zimran.moderation.model.enums.ReportTargetType;

/**
 * User reports, stored for moderation.
 */
public interface ReportService {

    /**
     * Stores a report about the item {@code targetId} (for {@code SCOPE}: about the author of that post or
     * request). Unknown item: {@code 404 NOT_FOUND}; own item: {@code 400 VALIDATION_ERROR}.
     */
    void report(RequestScope scope, ReportTargetType targetType, String targetId, ReportReason reason, String comment);
}
