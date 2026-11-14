package kz.astyq.zimran.guest.view.service;

import kz.astyq.zimran.guest.model.dto.GuestSessionViewResponse;

public interface GuestViewService {

    /**
     * Creates a new guest and signs a token for it.
     *
     * @param clientAddress remote address, used for rate limiting only
     */
    GuestSessionViewResponse createSession(String clientAddress);
}
