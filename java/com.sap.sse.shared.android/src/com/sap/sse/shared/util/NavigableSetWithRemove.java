package com.sap.sse.shared.util;

import java.util.NavigableSet;

/**
 * A navigable set with additional methods for removing all elements less than, less than or equal, greater than, or
 * greater than or equal to a specified element. This may allow implementing classes to provide algorithms for these
 * that are more efficient than, e.g., {@link #removeIf(java.util.function.Predicate)}.
 * <p>
 * 
 * @author Axel Uhl (d043530)
 *
 * @param <E>
 */
public interface NavigableSetWithRemove<E> extends NavigableSet<E> {
    void removeAllLessThan(E e);
    void removeAllLessOrEqual(E e);
    void removeAllGreaterThan(E e);
    void removeAllGreaterOrEqual(E e);
    @Override
    NavigableSetWithRemove<E> descendingSet();
    @Override
    NavigableSetWithRemove<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive);
    @Override
    NavigableSetWithRemove<E> headSet(E toElement, boolean inclusive) ;
    @Override
    NavigableSetWithRemove<E> tailSet(E fromElement, boolean inclusive);
    @Override
    NavigableSetWithRemove<E> subSet(E fromElement, E toElement);
    @Override
    NavigableSetWithRemove<E> headSet(E toElement);
    @Override
    NavigableSetWithRemove<E> tailSet(E fromElement);
}
