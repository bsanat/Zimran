package kz.astyq.zimran.buddy.listener;

import kz.astyq.zimran.buddy.service.BuddyRequestService;
import kz.astyq.zimran.moderation.model.domain.ScopeBlockedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * A block ends what two scopes share in Event Buddy: their open requests to each other are cancelled, in the
 * blocking transaction.
 */
@Component
@RequiredArgsConstructor
public class ScopeBlockedListener {

    private final BuddyRequestService buddyRequestService;

    @EventListener
    public void onScopeBlocked(ScopeBlockedEvent blocked) {
        buddyRequestService.cancelBetween(blocked.getBlockerScopeId(), blocked.getBlockedScopeId());
    }
}
