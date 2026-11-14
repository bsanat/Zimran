package kz.astyq.zimran.guest.service;

/**
 * Guest records.
 */
public interface GuestScopeService {

    /**
     * Stores a new, independent guest and returns its scope id.
     */
    String createGuest();
}
