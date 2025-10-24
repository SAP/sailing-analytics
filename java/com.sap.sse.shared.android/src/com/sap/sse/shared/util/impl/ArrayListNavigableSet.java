package com.sap.sse.shared.util.impl;

import java.io.Serializable;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.NoSuchElementException;

import com.sap.sse.common.ReverseRandomAccessList;
import com.sap.sse.shared.util.NavigableSetWithRemove;

/**
 * A {@link NavigableSet} implementation that internally uses an {@link ArrayList} to represent the data structure.
 * The elements are ordered by a mandatory {@link Comparator}, meaning that the natural element order is not considered.
 * {@link Collections#binarySearch(List, Object, Comparator) Binary search} is used to locate elements with logarithmic
 * effort which also results in logarithmic effort for computing subsets, head and tail sets, etc.<p>
 * 
 * The {@link #add} operation handles the special case that elements are added in ascending order specifically fast (O(1)),
 * at the expense of always performing one comparison in case the collection is not empty, before resorting to
 * binary search.<p>
 * 
 * Computing subsets is mapped to computing sub-lists (see {@link #subList(int, int)}), with all the same effects
 * on how the resulting collection is backed by the original collection.<p>
 * 
 * The iterators returned by {@link #iterator()} and {@link #descendingIterator()} are special in that they do not
 * perform any checks for concurrent modifications. They consist of a single integer index pointing into the
 * underlying list structure which is incremented/decremented when moving the iterator. The {@link Iterator#hasNext()}
 * operation does a simple bounds check when it is invoked. This way, the iterators may skip elements, visit the same
 * element twice or run into {@link ArrayIndexOutOfBoundsException} in case an element is removed between a
 * {@link Iterator#hasNext()} check and the call to {@link Iterator#next()}. On the other hand, this makes the iterators
 * very robust against the usual appending for which this entire data structure is tuned since no further
 * synchronization is necessary. Clients even typically expect to read elements added after an iterator started through
 * such an iterator.
 * 
 * @author Axel Uhl (d043530)
 */
public class ArrayListNavigableSet<E> extends AbstractSet<E> implements NavigableSetWithRemove<E>, Serializable {
    private static final long serialVersionUID = 6923963699509907975L;
    private final List<E> list;
    private final Comparator<? super E> comparator;

    public ArrayListNavigableSet(Comparator<? super E> comparator) {
        list = new ArrayList<E>();
        this.comparator = comparator;
    }
    
    public ArrayListNavigableSet(int initialSize, Comparator<? super E> comparator) {
        list = new ArrayList<E>(initialSize);
        this.comparator = comparator;
    }
    
    private ArrayListNavigableSet(List<E> list, Comparator<? super E> comparator) {
        this.list = list;
        this.comparator = comparator;
    }
    
    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @Override
    public E first() {
        try {
            return list.get(0);
        } catch (IndexOutOfBoundsException e) {
            throw new NoSuchElementException(e.getMessage());
        }
    }

    @Override
    public E last() {
        try {
            return list.get(list.size()-1);
        } catch (IndexOutOfBoundsException e) {
            throw new NoSuchElementException(e.getMessage());
        }
    }

    @Override
    public int size() {
        return list.size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean contains(Object o) {
        return binarySearch((E) o) >= 0;
    }

    @Override
    public Object[] toArray() {
        return list.toArray(); 
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return list.toArray(a); 
    }

    @Override
    public boolean add(E e) {
        final boolean result;
        int pos = binarySearch(e);
        if (pos >= 0) {
            result = false;
        } else {
            list.add(-pos-1, e);
            result = true;
        }
        return result;
    }

    /**
     * Special handling for searching for elements less than the first or greater than the last to
     * speed up these probable cases in comparison to a binary search. Other than that, semantic-wise
     * identical to {@link Collections#binarySearch(List, Object, Comparator)}.
     */
    private int binarySearch(E e) {
        final int result;
        if (list.isEmpty() || comparator().compare(e, first()) < 0) {
            result = -1;
        } else if (comparator().compare(e, last()) > 0) {
            result = -size()-1;
        } else {
            result = Collections.binarySearch(list, e, comparator());
        }
        return result;
    }
    
    @Override
    public boolean remove(Object o) {
        @SuppressWarnings("unchecked")
        int pos = binarySearch((E) o);
        if (pos >= 0) {
            list.remove(pos);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return list.containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends E> c) {
        boolean result = false;
        for (E e : c) {
            result = add(e) || result;
        }
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return list.retainAll(c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return list.removeAll(c);
    }

    @Override
    public void clear() {
        list.clear();
    }

    @Override
    public E lower(E e) {
        int pos = binarySearch(e);
        E result;
        if (pos >= 0) {
            if (pos > 0) {
                result = list.get(pos-1);
            } else {
                result = null; // exact match for lowest element; there is no lower element
            }
        } else {
            // A negative binarySearch result represents -insertPosition-1 because no exact match for timePoint was found.
            // Therefore, -pos = insertionPoint+1
            if (-pos-2 >= 0) {
                result = list.get(-pos-2);
            } else {
                result = null;
            }
        }
        return result;
    }

    @Override
    public E floor(E e) {
        int pos = binarySearch(e);
        E result;
        if (pos >= 0) {
            // found element equal to e
            result = list.get(pos);
        } else {
            // A negative binarySearch result represents -insertPosition-1 because no exact match for timePoint was found.
            // Therefore, -pos = insertionPoint+1
            if (-pos-2 >= 0) {
                result = list.get(-pos-2);
            } else {
                result = null;
            }
        }
        return result;
    }

    @Override
    public E ceiling(E e) {
        int pos = binarySearch(e);
        E result;
        if (pos >= 0) {
            result = list.get(pos);
        } else {
            // A negative binarySearch result represents -insertPosition-1 because no exact match for timePoint was found.
            // Therefore, -pos = insertionPoint+1
            if (-pos-1 < list.size()) {
                result = list.get(-pos-1);
            } else {
                result = null;
            }
        }
        return result;
    }

    @Override
    public E higher(E e) {
        int pos = binarySearch(e);
        E result;
        if (pos >= 0) {
            if (pos < list.size()-1) {
                result = list.get(pos+1);
            } else {
                result = null;
            }
        } else {
            // A negative binarySearch result represents -insertPosition-1 because no exact match for timePoint was found.
            // Therefore, -pos = insertionPoint+1
            if (-pos-1 < list.size()) {
                result = list.get(-pos-1);
            } else {
                result = null;
            }
        }
        return result;
    }

    @Override
    public E pollFirst() {
        if (isEmpty()) {
            return null;
        } else {
            return list.remove(0);
        }
    }

    @Override
    public E pollLast() {
        if (isEmpty()) {
            return null;
        } else {
            return list.remove(list.size()-1);
        }
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            private int i=0;
            @Override
            public boolean hasNext() {
                return i<list.size();
            }
            @Override
            public E next() {
                if (i >= list.size()) {
                    throw new NoSuchElementException();
                }
                return list.get(i++);
            }
            @Override
            public void remove() {
                if (i == 0) {
                    throw new IllegalStateException();
                }
                list.remove(--i);
            }
        };
    }

    @Override
    public NavigableSetWithRemove<E> descendingSet() {
        return new ArrayListNavigableSet<E>(new ReverseRandomAccessList<E>(list), comparator());
    }

    @Override
    public Iterator<E> descendingIterator() {
        return new Iterator<E>() {
            private int i=list.size()-1;
            @Override
            public boolean hasNext() {
                return i>=0;
            }
            @Override
            public E next() {
                if (i < 0) {
                    throw new NoSuchElementException();
                }
                return list.get(i--);
            }
            @Override
            public void remove() {
                if (i == list.size()-1) {
                    throw new IllegalStateException();
                }
                list.remove(i);
            }
        };
    }

    @Override
    public NavigableSetWithRemove<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        int from = binarySearch(fromElement);
        if (from < 0) {
            from = -from-1;
        } else {
            if (!fromInclusive) {
                from++;
            }
        }
        int to = binarySearch(toElement);
        if (to < 0) {
            to = -to-1;
        } else {
            if (toInclusive) {
                to++;
            }
        }
        if (to < from) {
            to = from; // will still produce an empty list instead of failing with an exception
        }
        return new ArrayListNavigableSet<E>(subList(from, to), comparator());
    }

    @Override
    public NavigableSetWithRemove<E> headSet(E toElement, boolean inclusive) {
        int to = binarySearch(toElement);
        if (to < 0) {
            to = -to-1;
        } else {
            if (inclusive) {
                to++;
            }
        }
        if (to < 0) {
            to = 0; // will still produce an empty list instead of failing with an exception
        }
        return new ArrayListNavigableSet<E>(subList(0, to), comparator());
    }

    @Override
    public NavigableSetWithRemove<E> tailSet(E fromElement, boolean inclusive) {
        int from = binarySearch(fromElement);
        if (from < 0) {
            from = -from-1;
        } else {
            if (!inclusive) {
                from++;
            }
        }
        if (from > list.size()) {
            from = list.size(); // will still produce an empty list instead of failing with an exception
        }
        return new ArrayListNavigableSet<E>(subList(from, list.size()), comparator());
    }
    
    private List<E> subList(int from, int to) {
        return list.subList(from, to);
    }

    @Override
    public NavigableSetWithRemove<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public NavigableSetWithRemove<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public NavigableSetWithRemove<E> tailSet(E fromElement) {
        return tailSet(fromElement, false);
    }
    
    @Override
    public void removeAllLessThan(E e) {
        final int i = binarySearch(e);
        if (i < 0) {
            // A negative binarySearch result represents -insertPosition-1 because no exact match for e was found.
            // Therefore, -i = insertionPoint+1
            list.subList(0, -i - 1).clear(); // uses ArrayList.removeRange internally which is as efficient as can be
        } else {
            list.subList(0, i).clear();
        }
    }

    @Override
    public void removeAllLessOrEqual(E e) {
        final int i = binarySearch(e);
        if (i < 0) {
            // A negative binarySearch result represents -insertPosition-1 because no exact match for e was found.
            // Therefore, -i = insertionPoint+1
            list.subList(0, -i - 1).clear(); // uses ArrayList.removeRange internally which is as efficient as can be
        } else {
            list.subList(0, i + 1).clear();
        }
    }

    @Override
    public void removeAllGreaterThan(E e) {
        final int i = binarySearch(e);
        if (i < 0) {
            // A negative binarySearch result represents -insertPosition-1 because no exact match for e was found.
            // Therefore, -i = insertionPoint+1
            list.subList(-i - 1, list.size()).clear(); // uses ArrayList.removeRange internally which is as efficient as can be
        } else {
            list.subList(i + 1, list.size()).clear();
        }
        
    }

    @Override
    public void removeAllGreaterOrEqual(E e) {
        final int i = binarySearch(e);
        if (i < 0) {
            // A negative binarySearch result represents -insertPosition-1 because no exact match for e was found.
            // Therefore, -i = insertionPoint+1
            list.subList(-i - 1, list.size()).clear(); // uses ArrayList.removeRange internally which is as efficient as can be
        } else {
            list.subList(i, list.size()).clear();
        }
    }

    @Override
    public String toString() {
        return list.toString();
    }
}
