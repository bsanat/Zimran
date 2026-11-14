package kz.astyq.zimran.guest.view.service.impl;

import kz.astyq.zimran.core.model.enums.RateLimitAction;
import kz.astyq.zimran.core.service.RateLimitService;
import kz.astyq.zimran.guest.mapper.GuestMapper;
import kz.astyq.zimran.guest.model.dto.GuestSessionViewResponse;
import kz.astyq.zimran.guest.service.GuestScopeService;
import kz.astyq.zimran.guest.service.GuestTokenService;
import kz.astyq.zimran.guest.view.service.GuestViewService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class GuestViewServiceImpl implements GuestViewService {

    private final RateLimitService rateLimitService;
    private final GuestScopeService guestScopeService;
    private final GuestTokenService guestTokenService;
    private final GuestMapper guestMapper;

    @Override
    public GuestSessionViewResponse createSession(String clientAddress) {
        rateLimitService.consumeForClient(RateLimitAction.GUEST_SESSION, clientAddress);
        String scopeId = guestScopeService.createGuest();
        return guestMapper.toViewResponse(guestTokenService.issue(scopeId));
    }
}
