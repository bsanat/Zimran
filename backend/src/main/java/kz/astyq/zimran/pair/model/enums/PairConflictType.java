package kz.astyq.zimran.pair.model.enums;

/**
 * Why a session ended with {@code NO_MATCH}, and which constraint to relax.
 */
public enum PairConflictType {
    /** The dates (or times of day) of both sides do not overlap, or removed the most events. */
    DATES,
    /** The strict (lower) budget removed the most events. */
    BUDGET,
    /** The interests removed the most events. */
    INTERESTS,
    /** Both sides voted on every suggestion without a common LIKE. */
    VOTES
}
