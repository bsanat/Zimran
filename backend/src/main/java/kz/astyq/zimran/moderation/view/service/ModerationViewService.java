package kz.astyq.zimran.moderation.view.service;

import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.moderation.model.dto.BlockCreateRequest;
import kz.astyq.zimran.moderation.model.dto.BlockViewResponse;
import kz.astyq.zimran.moderation.model.dto.ReportCreateRequest;

/**
 * Reports and blocks; free for every caller with a scope.
 */
public interface ModerationViewService {

    /** Rate limited per scope. */
    void report(ReportCreateRequest request, RequestScope scope);

    BlockViewResponse block(BlockCreateRequest request, RequestScope scope);

    void unblock(String scopeRef, RequestScope scope);
}
