package kz.astyq.zimran.moderation.view.service.impl;

import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.core.model.enums.RateLimitAction;
import kz.astyq.zimran.core.service.RateLimitService;
import kz.astyq.zimran.moderation.model.dto.BlockCreateRequest;
import kz.astyq.zimran.moderation.model.dto.BlockViewResponse;
import kz.astyq.zimran.moderation.model.dto.ReportCreateRequest;
import kz.astyq.zimran.moderation.model.entity.Block;
import kz.astyq.zimran.moderation.service.BlockService;
import kz.astyq.zimran.moderation.service.ReportService;
import kz.astyq.zimran.moderation.view.service.ModerationViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ModerationViewServiceImpl implements ModerationViewService {

    private final RateLimitService rateLimitService;
    private final ReportService reportService;
    private final BlockService blockService;

    @Override
    public void report(ReportCreateRequest request, RequestScope scope) {
        rateLimitService.consume(RateLimitAction.REPORT_CREATE, scope);
        reportService.report(scope, request.getTargetType(), request.getTargetId().strip(), request.getReason(),
                request.getComment());
    }

    @Override
    public BlockViewResponse block(BlockCreateRequest request, RequestScope scope) {
        Block block = blockService.block(scope, request.getTargetType(), request.getTargetId().strip());
        return BlockViewResponse.builder().scopeRef(block.getPublicId()).createdAt(block.getCreatedAt()).build();
    }

    @Override
    public void unblock(String scopeRef, RequestScope scope) {
        blockService.unblock(scope, scopeRef);
    }
}
