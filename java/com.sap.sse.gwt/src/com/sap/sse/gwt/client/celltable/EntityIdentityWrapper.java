package com.sap.sse.gwt.client.celltable;


/**
 * Wraps an object such that its {@link #equals(Object)} and {@link #hashCode()} implementations are based on the
 * {@link RefreshableMultiSelectionModel#comp} comparator's
 * {@link EntityIdentityComparator#representSameEntity(Object, Object)} and
 * {@link EntityIdentityComparator#hashCode(Object)} methods, respectively.
 * 
 * @author Axel Uhl (D043530)
 *
 * @param <T>
 */
class EntityIdentityWrapper<T> {
    private final T t;
    private final EntityIdentityComparator<T> comp;
    
    public EntityIdentityWrapper(T t, EntityIdentityComparator<T> comp) {
        super();
        this.t = t;
        this.comp = comp;
    }

    @Override
    public int hashCode() {
        return comp == null ? t.hashCode() : comp.hashCode(t);
    }
    
    private T getT() {
        return t;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof EntityIdentityWrapper<?>)) {
            throw new ClassCastException("Can only compare EntityIdentityWrapper with other objects of same class");
        }
        @SuppressWarnings("unchecked") // need to cast to generic type argument T
        EntityIdentityWrapper<T> objAsT = ((EntityIdentityWrapper<T>) obj);
        final boolean result;
        if (comp == null) {
            result = t.equals(objAsT.getT());
        } else {
            result = comp.representSameEntity(t, objAsT.getT());
        }
        return result;
    }
}