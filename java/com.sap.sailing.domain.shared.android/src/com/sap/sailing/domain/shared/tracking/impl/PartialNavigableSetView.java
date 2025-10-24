package com.sap.sailing.domain.shared.tracking.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * A view on a {@link NavigableSet} which suppresses some entries based on some configurable rule.
 * The {@link #size()} operation is expensive because it requires a full scan. {@link #isEmpty()} is
 * much cheaper because it suffices to find one element passing the filter rule. The filtering rule
 * has to be expressed by subclsses implementing the {@link #isValid(Object)} method.
 * 
 * @author Axel Uhl (d043530)
 *
 * @param <E>
 */
public abstract class PartialNavigableSetView<E> implements NavigableSet<E> {
    private final NavigableSet<E> set;

    private class PartialNavigableSetViewWithSameValidityAsEnclosing extends PartialNavigableSetView<E> {
        public PartialNavigableSetViewWithSameValidityAsEnclosing(NavigableSet<E> set) {
            super(set);
        }

        @Override
        protected boolean isValid(E e) {
            return PartialNavigableSetView.this.isValid(e);
        }
    }
    
    private class FilteringIterator implements Iterator<E> {
        /**
         * The iterator is always kept one step "ahead" in order to know whether there really is a next element. The
         * next valid element is fetched and stored in {@link #nextValid} and {@link #hasNext} is set to
         * <code>true</code>.
         */
        private Iterator<E> iter;
        
        private E nextValid;
        
        private boolean hasNext;
        
        private boolean hasLastNext;
        
        private E lastNext;
        
        public FilteringIterator() {
            iter = getSet().iterator();
            hasLastNext = false;
            advance();
        }

        private void advance() {
            if (iter.hasNext()) {
                E next = iter.next();
                while (!isValid(next) && iter.hasNext()) {
                    next = iter.next();
                }
                if (isValid(next)) {
                    nextValid = next;
                    hasNext = true;
                } else {
                    hasNext = false;
                }
            } else {
                hasNext = false;
            }
        }
        
        @Override
        public boolean hasNext() {
            return hasNext;
        }

        @Override
        public E next() {
            if (hasNext) {
                E result = nextValid;
                advance();
                hasLastNext = true;
                lastNext = result;
                return result;
            } else {
                throw new NoSuchElementException();
            }
        }

        @Override
        public void remove() {
            if (!hasLastNext) {
                throw new IllegalStateException("next() was not called before remove()");
            } else {
                PartialNavigableSetView.this.remove(lastNext);
                hasLastNext = false;
            }
        }
    }
    
    public PartialNavigableSetView(NavigableSet<E> set) {
        this.set = set;
    }
    
    /**
     * Subclasses need to implement this method. For elements to be eliminated from the view represented by this
     * object, return <code>false</code> for such an element being passed to this method.
     */
    abstract protected boolean isValid(E e);
    
    @Override
    public Comparator<? super E> comparator() {
        return getSet().comparator();
    }

    public NavigableSet<E> descendingSet() {
        return new PartialNavigableSetViewWithSameValidityAsEnclosing(getSet().descendingSet());
    }

    @Override
    public Iterator<E> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public E first() {
        E first = getSet().first();
        while (first != null && !isValid(first)) {
            first = getSet().higher(first);
        }
        if (first == null) {
            throw new NoSuchElementException();
        } else {
            return first;
        }
    }

    @Override
    public E last() {
        E last = getSet().last();
        while (last != null && !isValid(last)) {
            last = getSet().lower(last);
        }
        if (last == null) {
            throw new NoSuchElementException();
        } else {
            return last;
        }
    }

    @Override
    public int size() {
        int size = 0;
        for (E e : getSet()) {
            if (isValid(e)) {
                size++;
            }
        }
        return size;
    }

    @Override
    public boolean isEmpty() {
        for (E e : getSet()) {
            if (isValid(e)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object o) {
        return getSet().contains(o) && isValid((E) o);
    }

    @Override
    public Object[] toArray() {
        List<E> l = new ArrayList<E>();
        for (E e : getSet()) {
            if (isValid(e)) {
                l.add(e);
            }
        }
        return l.toArray();
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T[] toArray(T[] a) {
        List<T> l = new ArrayList<T>();
        for (E e : getSet()) {
            if (isValid(e)) {
                l.add((T) e);
            }
        }
        return l.toArray(a);
        
    }

    @Override
    public boolean add(E e) {
        return getSet().add(e);
    }

    @Override
    public boolean remove(Object o) {
        return getSet().remove(o);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean containsAll(Collection<?> c) {
        for (Object o : c) {
            if (!isValid((E) o) || !getSet().contains(o)) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        return getSet().addAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return getSet().retainAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return getSet().removeAll(c);
    }

    @Override
    public void clear() {
        getSet().clear();
    }

    @Override
    public E lower(E e) {
        E result = getSet().lower(e);
        while (result != null && !isValid(result)) {
            result = getSet().lower(result);
        }
        return result;
    }
    
    /**
     * goes one left on the raw, unfiltered set and therefore may return fixes that have {@link #isValid(Object)}==
     * <code>false</code>
     */
    protected E lowerInternal(E e) {
        return getSet().lower(e);
    }

    /**
     * goes one right on the raw, unfiltered set and therefore may return fixes that have {@link #isValid(Object)}==
     * <code>false</code>
     */
    protected E higherInternal(E e) {
        return getSet().higher(e);
    }

    @Override
    public E floor(E e) {
        E result = getSet().floor(e);
        while (result != null && !isValid(result)) {
            result = getSet().lower(result);
        }
        return result;
    }

    @Override
    public E ceiling(E e) {
        E result = getSet().ceiling(e);
        while (result != null && !isValid(result)) {
            result = getSet().higher(result);
        }
        return result;
    }

    @Override
    public E higher(E e) {
        E result = getSet().higher(e);
        while (result != null && !isValid(result)) {
            result = getSet().higher(result);
        }
        return result;
    }

    /**
     * Removes all raw fixes that have {@link #isValid(Object)}==<code>false</code> and the first element to have
     * {@link #isValid(Object)}==<code>true</code>. This latter element is returned. If no such element exists,
     * <code>null</code> is returned. It is hence possible that invalid raw fixes are removed but stil <code>null</code>
     * is returned.
     */
    @Override
    public E pollFirst() {
        E result = getSet().first();
        while (result != null && !isValid(result)) {
            getSet().remove(result);
            result = getSet().first();
        }
        return result;
    }

    @Override
    public E pollLast() {
        E result = getSet().last();
        while (result != null && !isValid(result)) {
            getSet().remove(result);
            result = getSet().last();
        }
        return result;
    }
    
    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        return new PartialNavigableSetViewWithSameValidityAsEnclosing(getSet().subSet(fromElement, fromInclusive, toElement, toInclusive));
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return new PartialNavigableSetViewWithSameValidityAsEnclosing(getSet().headSet(toElement, inclusive));
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return new PartialNavigableSetViewWithSameValidityAsEnclosing(getSet().tailSet(fromElement, inclusive));
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, E toElement) {
        SortedSet<E> subSet = set.subSet(fromElement, toElement);
        if (subSet instanceof NavigableSet<?>) {
            return new PartialNavigableSetViewWithSameValidityAsEnclosing((NavigableSet<E>) subSet);
        } else {
            TreeSet<E> result = new TreeSet<E>(subSet);
            return new PartialNavigableSetViewWithSameValidityAsEnclosing(result);
        }
    }

    @Override
    public NavigableSet<E> headSet(E toElement) {
        SortedSet<E> headSet = set.headSet(toElement);
        if (headSet instanceof NavigableSet<?>) {
            return new PartialNavigableSetViewWithSameValidityAsEnclosing((NavigableSet<E>) headSet);
        } else {
            TreeSet<E> result = new TreeSet<E>(headSet);
            return new PartialNavigableSetViewWithSameValidityAsEnclosing(result);
        }
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement) {
        SortedSet<E> tailSet = set.tailSet(fromElement);
        if (tailSet instanceof NavigableSet<?>) {
            return new PartialNavigableSetViewWithSameValidityAsEnclosing((NavigableSet<E>) tailSet);
        } else {
            TreeSet<E> result = new TreeSet<E>(tailSet);
            return new PartialNavigableSetViewWithSameValidityAsEnclosing(result);
        }
    }


    @Override
    public Iterator<E> iterator() {
        return new FilteringIterator();
    }

    @Override
    public String toString() {
        return new ArrayList<E>(this).toString();
    }

    protected NavigableSet<E> getSet() {
        return set;
    }
}
