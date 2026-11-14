package kz.astyq.zimran.core.model.domain;

import kz.astyq.zimran.core.model.enums.ScopeKind;
import lombok.Builder;
import lombok.NonNull;
import lombok.Value;

/**
 * The verified identity of the caller. Every per-caller record (entitlements, quotas, ...) is keyed by
 * {@code scopeId}, whatever the {@code kind}, so guests and future signed-in users share one model.
 */
@Value
@Builder
public class RequestScope {

    @NonNull
    String scopeId;
    @NonNull
    ScopeKind kind;
}
