package kz.astyq.zimran.core.service;

import kz.astyq.zimran.core.model.domain.RequestScope;

/**
 * Notified when a verified scope uses an endpoint that needs it (e.g. to update "last seen").
 */
public interface ScopeActivityRecorder {

    void recordActivity(RequestScope scope);
}
