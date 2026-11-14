package kz.astyq.zimran.buddy.service;

public interface BuddyExpiryService {

    /**
     * Sets EXPIRED on every OPEN or MATCHED post whose event has started.
     *
     * @return the number of posts expired
     */
    int expireDue();
}
