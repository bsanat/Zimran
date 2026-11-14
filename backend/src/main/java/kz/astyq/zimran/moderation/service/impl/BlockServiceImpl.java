package kz.astyq.zimran.moderation.service.impl;

import kz.astyq.zimran.core.exception.ServiceValidationException;
import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.core.util.ErrorCode;
import kz.astyq.zimran.core.util.PublicIds;
import kz.astyq.zimran.moderation.model.domain.ScopeBlockedEvent;
import kz.astyq.zimran.moderation.model.entity.Block;
import kz.astyq.zimran.moderation.model.enums.ModerationTargetKind;
import kz.astyq.zimran.moderation.repository.BlockRepository;
import kz.astyq.zimran.moderation.service.BlockService;
import kz.astyq.zimran.moderation.service.ModerationTargetResolver;
import kz.astyq.zimran.moderation.util.MessageCode;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class BlockServiceImpl implements BlockService {

    static final String PUBLIC_ID_PREFIX = "blk_";

    private final BlockRepository blockRepository;
    private final ModerationTargetResolver moderationTargetResolver;
    private final ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional
    public Block block(RequestScope scope, ModerationTargetKind kind, String targetId) {
        String blocked = moderationTargetResolver.authorScopeId(kind, targetId)
                .orElseThrow(() -> new ServiceValidationException(ErrorCode.NOT_FOUND, MessageCode.TARGET_NOT_FOUND).withDetail("field", "targetId"));
        String blocker = scope.getScopeId();
        if (blocked.equals(blocker)) {
            throw new ServiceValidationException(ErrorCode.VALIDATION_ERROR, MessageCode.CANNOT_BLOCK_SELF)
                    .withDetail("field", "targetId");
        }
        return blockRepository.findByBlockerScopeIdAndBlockedScopeId(blocker, blocked).orElseGet(() -> {
            Block block = blockRepository.save(Block.builder()
                    .publicId(PublicIds.newId(PUBLIC_ID_PREFIX))
                    .blockerScopeId(blocker)
                    .blockedScopeId(blocked)
                    .build());
            eventPublisher.publishEvent(new ScopeBlockedEvent(blocker, blocked));
            return block;
        });
    }

    @Override
    @Transactional
    public void unblock(RequestScope scope, String scopeRef) {
        Block block = blockRepository.findByPublicId(scopeRef)
                .filter(found -> found.getBlockerScopeId().equals(scope.getScopeId()))
                .orElseThrow(() -> new ServiceValidationException(ErrorCode.NOT_FOUND, MessageCode.BLOCK_NOT_FOUND));
        blockRepository.delete(block);
    }

    @Override
    public boolean isBlockedEitherWay(String firstScopeId, String secondScopeId) {
        return blockRepository.existsEitherWay(firstScopeId, secondScopeId);
    }

    @Override
    public Set<String> blockedEitherWay(String scopeId) {
        return Set.copyOf(blockRepository.findCounterpartsOf(scopeId));
    }
}
