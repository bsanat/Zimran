package kz.astyq.zimran.guest.service.impl;

import kz.astyq.zimran.core.configuration.GuestProperties;
import kz.astyq.zimran.core.model.domain.RequestScope;
import kz.astyq.zimran.core.model.enums.ScopeKind;
import kz.astyq.zimran.core.service.ScopeActivityRecorder;
import kz.astyq.zimran.guest.model.entity.GuestScope;
import kz.astyq.zimran.guest.repository.GuestScopeRepository;
import kz.astyq.zimran.guest.service.GuestScopeService;
import kz.astyq.zimran.guest.util.GuestScopeIds;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
public class GuestScopeServiceImpl implements GuestScopeService, ScopeActivityRecorder {

    private final GuestScopeRepository guestScopeRepository;
    private final GuestProperties properties;
    private final Clock clock;

    @Override
    @Transactional
    public String createGuest() {
        GuestScope guest = guestScopeRepository.save(GuestScope.builder()
                .scopeId(GuestScopeIds.newScopeId())
                .ageConfirmed(false)
                .lastSeenAt(clock.instant())
                .build());
        return guest.getScopeId();
    }

    /**
     * Updates the guest's {@code last_seen_at}, at most once per {@code app.guest.last-seen-update-interval}.
     */
    @Override
    @Transactional
    public void recordActivity(RequestScope scope) {
        if (scope.getKind() != ScopeKind.GUEST) {
            return;
        }
        Instant now = clock.instant();
        guestScopeRepository.touchLastSeen(scope.getScopeId(), now, now.minus(properties.getLastSeenUpdateInterval()));
    }
}
