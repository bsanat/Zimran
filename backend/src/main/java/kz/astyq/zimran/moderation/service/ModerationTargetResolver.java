package kz.astyq.zimran.moderation.service;

import kz.astyq.zimran.moderation.model.enums.ModerationTargetKind;

import java.util.Optional;

/**
 * Finds the author of a reportable item. Implemented by the feature that owns the items (Event Buddy), so
 * moderation does not depend on it.
 */
public interface ModerationTargetResolver {

    /**
     * Scope id of the author of the {@code kind} item with this public id; empty when there is none.
     */
    Optional<String> authorScopeId(ModerationTargetKind kind, String publicId);
}
