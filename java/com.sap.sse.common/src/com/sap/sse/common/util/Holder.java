package com.sap.sse.common.util;

/**
 * A trivial mutable holder for a single reference, mirroring {@link IntHolder} and {@link DoubleHolder} for
 * arbitrary object types. Useful, e.g., to give a lambda access to a value that can only be assigned after the
 * lambda has been constructed, such as a recursively self-referencing lambda: the holder is declared first, the
 * lambda captures the (effectively final) holder, and the lambda's own value is stored into {@link #value}
 * afterwards. This avoids the {@code T[]} one-element-array idiom that would otherwise require an unchecked cast.
 */
public class Holder<T> {
    public T value;

    public Holder() {
        super();
    }

    public Holder(T value) {
        super();
        this.value = value;
    }
}
