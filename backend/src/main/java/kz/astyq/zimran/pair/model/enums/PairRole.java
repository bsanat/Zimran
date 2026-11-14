package kz.astyq.zimran.pair.model.enums;

/**
 * Side of a Pair Planning session: {@link #A} created it, {@link #B} joined with the invite.
 */
public enum PairRole {
    A,
    B;

    public PairRole other() {
        return this == A ? B : A;
    }
}
