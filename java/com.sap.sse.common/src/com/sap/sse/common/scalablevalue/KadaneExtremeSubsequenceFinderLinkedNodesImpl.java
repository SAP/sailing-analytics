package com.sap.sse.common.scalablevalue;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Collections;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import com.sap.sse.common.Util;
import com.sap.sse.common.impl.SerializableComparator;

/**
 * An implementation of Kadane's algorithm for "maximum sub-sequence sum" that works incrementally,
 * allows insertion and removal anywhere in the sequence, and maintains the start/end points of those
 * extreme sum sequences for both, the maximal and the minimal sum. It furthermore supports iteration,
 * also across sub-sequences such as those extreme sum sub-sequences, a {@link #size()} as well as an
 * {@link #isEmpty()} operation.<p>
 * 
 * This implementation uses a doubly-linked sequence of {@link Node}s. It is <em>not</em> thread-safe.
 * Callers must ensure that concurrent modifications are properly synchronized.
 * 
 * @author Axel Uhl (D043530)
 */
public class KadaneExtremeSubsequenceFinderLinkedNodesImpl<ValueType, AveragesTo extends Comparable<AveragesTo>, T extends ComparableScalableValueWithDistance<ValueType, AveragesTo>>
        implements KadaneExtremeSubsequenceFinder<ValueType, AveragesTo, T> {

    private static final long serialVersionUID = -8986609116472739636L;

    private transient Node<ValueType, AveragesTo, T> first;
    
    private transient Node<ValueType, AveragesTo, T> last;

    private int size;
    
    private final TreeSet<Node<ValueType, AveragesTo, T>> nodesOrderedByMinSum;
    
    private final TreeSet<Node<ValueType, AveragesTo, T>> nodesOrderedByMaxSum;
    
    private int minChangePropagationStepsSum; // for internal stats
    
    private int minChangePropagationsCount; // for internal stats
    
    private int maxChangePropagationStepsSum; // for internal stats
    
    private int maxChangePropagationsCount; // for internal stats

    private static <ValueType, AveragesTo extends Comparable<AveragesTo>, T extends ComparableScalableValueWithDistance<ValueType, AveragesTo>> Integer compare(
            final ScalableValueWithDistance<ValueType, AveragesTo> a,
            final ScalableValueWithDistance<ValueType, AveragesTo> b) {
        return a.divide(1).compareTo(b.divide(1));
    }
    
    /**
     * Nodes of this type are used to construct a doubly-linked list, with each node holding a reference to the node
     * forming the start of the sequence with the maximum sum.
     * 
     * @author Axel Uhl (d043530)
     */
    private static class Node<ValueType, AveragesTo extends Comparable<AveragesTo>, T extends ComparableScalableValueWithDistance<ValueType, AveragesTo>> implements Serializable {
        private static final long serialVersionUID = -2547142048423135013L;
        private static final AtomicInteger idCounter = new AtomicInteger(0);
        private final T value;
        private final int id;
        private transient Node<ValueType, AveragesTo, T> previous;
        private transient Node<ValueType, AveragesTo, T> next;
        private ScalableValueWithDistance<ValueType, AveragesTo> minSumEndingHere;
        private transient Node<ValueType, AveragesTo, T> startOfMinSumSubSequenceEndingHere;
        private ScalableValueWithDistance<ValueType, AveragesTo> maxSumEndingHere;
        private transient Node<ValueType, AveragesTo, T> startOfMaxSumSubSequenceEndingHere;

        private Node(Node<ValueType, AveragesTo, T> previous, Node<ValueType, AveragesTo, T> next, T value) {
            super();
            id = idCounter.getAndIncrement();
            this.previous = previous;
            this.next = next;
            this.value = value;
            this.minSumEndingHere = null;
            this.startOfMinSumSubSequenceEndingHere = null;
            this.maxSumEndingHere = null;
            this.startOfMaxSumSubSequenceEndingHere = null;
        }

        /**
         * A unique ID that can be used, e.g., for disambiguation during sorting, so as to keep two nodes with
         * {@link #getValue} values comparing equal apart.
         */
        private int getId() {
            return id;
        }
        
        private Node<ValueType, AveragesTo, T> getPrevious() {
            return previous;
        }

        private Node<ValueType, AveragesTo, T> getNext() {
            return next;
        }

        private void setPrevious(Node<ValueType, AveragesTo, T> previous) {
            this.previous = previous;
        }

        private void setNext(Node<ValueType, AveragesTo, T> next) {
            this.next = next;
        }

        private T getValue() {
            return value;
        }

        private ScalableValueWithDistance<ValueType, AveragesTo> getMinSumEndingHere() {
            return minSumEndingHere;
        }

        private Node<ValueType, AveragesTo, T> getStartOfMinSumSubSequenceEndingHere() {
            return startOfMinSumSubSequenceEndingHere;
        }

        private void setMinSumEndingHere(ScalableValueWithDistance<ValueType, AveragesTo> minSumEndingHere) {
            this.minSumEndingHere = minSumEndingHere;
        }

        private void setStartOfMinSumSubSequenceEndingHere(Node<ValueType, AveragesTo, T> startOfMinSumSubSequenceEndingHere) {
            this.startOfMinSumSubSequenceEndingHere = startOfMinSumSubSequenceEndingHere;
        }

        private void setMaxSumEndingHere(ScalableValueWithDistance<ValueType, AveragesTo> maxSumEndingHere) {
            this.maxSumEndingHere = maxSumEndingHere;
        }

        private void setStartOfMaxSumSubSequenceEndingHere(Node<ValueType, AveragesTo, T> startOfMaxSumSubSequenceEndingHere) {
            this.startOfMaxSumSubSequenceEndingHere = startOfMaxSumSubSequenceEndingHere;
        }

        private ScalableValueWithDistance<ValueType, AveragesTo> getMaxSumEndingHere() {
            return maxSumEndingHere;
        }

        private Node<ValueType, AveragesTo, T> getStartOfMaxSumSubSequenceEndingHere() {
            return startOfMaxSumSubSequenceEndingHere;
        }
        
        /**
         * Updates this node's extreme sum values of the sub-sequences ending at this node, considering the values
         * stored in the {@link #getPrevious() previous} element, if such an element exists. Furthermore, the
         * references to the nodes where these sub-sequences start are updated accordingly.
         * 
         * @return whether the node has changed during this update; this may mean a change in the min/max sum and/or the
         *         the start of the extreme sub-sequence(s) ending at this node. Any such change requires updating
         *         {@link #getNext() following nodes} too.
         */
        private boolean updateThisFromPrevious(TreeSet<Node<ValueType, AveragesTo, T>> nodesOrderedByMinSum, TreeSet<Node<ValueType, AveragesTo, T>> nodesOrderedByMaxSum) {
            final boolean changedByMin = updateMinFromPrevious(nodesOrderedByMinSum);
            final boolean changedByMax = updateMaxFromPrevious(nodesOrderedByMaxSum);
            assert nodesOrderedByMinSum.size() == nodesOrderedByMaxSum.size();
            return changedByMax || changedByMin;
        }

        /**
         * Updates this node's min sum values of the sub-sequences ending at this node, considering the values stored in
         * the {@link #getPrevious() previous} element, if such an element exists. Furthermore, the references to the
         * node where the min sum sub-sequence starts is updated accordingly.
         * 
         * @return whether the node's min sum-related properties changed during this update; this may mean a change in
         *         the min sum and/or the the start of the min sum sub-sequence(s) ending at this node. Any such change
         *         requires updating {@link #getNext() following nodes} using this method, too. It does not happen
         *         automatically by calling this method. This method updates only this node.
         */
        private boolean updateMinFromPrevious(TreeSet<Node<ValueType, AveragesTo, T>> nodesOrderedByMinSum) {
            return updateThisFromPrevious(Node::getMinSumEndingHere,
                    Node::getStartOfMinSumSubSequenceEndingHere, this::setMinSumEndingHere,
                    this::setStartOfMinSumSubSequenceEndingHere, (a, b)->compare(b, a), nodesOrderedByMinSum);
        }

        /**
         * Updates this node's max sum values of the sub-sequences ending at this node, considering the values stored in
         * the {@link #getPrevious() previous} element, if such an element exists. Furthermore, the references to the
         * node where the max sum sub-sequence starts is updated accordingly.
         * 
         * @return whether the node's max sum-related properties changed during this update; this may mean a change in
         *         the max sum and/or the the start of the max sum sub-sequence(s) ending at this node. Any such change
         *         requires updating {@link #getNext() following nodes} using this method, too. It does not happen
         *         automatically by calling this method. This method updates only this node.
         */
        private boolean updateMaxFromPrevious(TreeSet<Node<ValueType, AveragesTo, T>> nodesOrderedByMaxSum) {
            return updateThisFromPrevious(Node::getMaxSumEndingHere,
                    Node::getStartOfMaxSumSubSequenceEndingHere, this::setMaxSumEndingHere,
                    this::setStartOfMaxSumSubSequenceEndingHere, KadaneExtremeSubsequenceFinderLinkedNodesImpl::compare, nodesOrderedByMaxSum);
        }

        private boolean updateThisFromPrevious(Function<Node<ValueType, AveragesTo, T>, ScalableValueWithDistance<ValueType, AveragesTo>> getExtremeSumEndingHere,
                Function<Node<ValueType, AveragesTo, T>, Node<ValueType, AveragesTo, T>> getStartOfExtremeSumSubSequenceEndingHere,
                Consumer<ScalableValueWithDistance<ValueType, AveragesTo>> setExtremeSumEndingHere,
                Consumer<Node<ValueType, AveragesTo, T>> setStartOfExtremeSubSubSequenceEndingHere,
                BiFunction<ScalableValueWithDistance<ValueType, AveragesTo>, ScalableValueWithDistance<ValueType, AveragesTo>, Integer> comparator,
                TreeSet<Node<ValueType, AveragesTo, T>> mapOrderedByExtremeSumEndingHereToUpdate) {
            boolean changed = false;
            final ScalableValueWithDistance<ValueType, AveragesTo> newMaxSumEndingHere;
            final Node<ValueType, AveragesTo, T> newStartOfMaxSumSubSequenceEndingHere;
            final ScalableValueWithDistance<ValueType, AveragesTo> sumWithMax = getPrevious() == null ? null : getValue().add(getExtremeSumEndingHere.apply(getPrevious()));
            if (getPrevious() == null || comparator.apply(getValue(), sumWithMax) >= 0) {
                newMaxSumEndingHere = getValue(); // one-element sum consisting of element at "index" is the maximum
                newStartOfMaxSumSubSequenceEndingHere = this;
            } else {
                newMaxSumEndingHere = sumWithMax;
                newStartOfMaxSumSubSequenceEndingHere = getStartOfExtremeSumSubSequenceEndingHere.apply(getPrevious());
            }
            final ScalableValueWithDistance<ValueType, AveragesTo> oldExtremeSumEndingHere = getExtremeSumEndingHere.apply(this);
            if (!newMaxSumEndingHere.equals(oldExtremeSumEndingHere)) {
                changed = true;
                if (oldExtremeSumEndingHere != null) {
                    if (!mapOrderedByExtremeSumEndingHereToUpdate.remove(this)) {
                        throw new InternalError("This shouldn't have happened as it means the node was not present in the map, although it should have been");
                    }
                }
                setExtremeSumEndingHere.accept(newMaxSumEndingHere);
                if (newMaxSumEndingHere != null) {
                    mapOrderedByExtremeSumEndingHereToUpdate.add(this);
                }
            }
            if (newStartOfMaxSumSubSequenceEndingHere != getStartOfExtremeSumSubSequenceEndingHere.apply(this)) {
                changed = true;
                setStartOfExtremeSubSubSequenceEndingHere.accept(newStartOfMaxSumSubSequenceEndingHere);
            }
            return changed;
        }
        
        @Override
        public String toString() {
            return "["+getValue()+", "+"max: "+getMaxSumEndingHere()+", min: "+getMinSumEndingHere()+"]";
        }
    }

    public KadaneExtremeSubsequenceFinderLinkedNodesImpl() {
        this.size = 0;
        this.first = null;
        this.last = null;
        final SerializableComparator<? super Node<ValueType, AveragesTo, T>> idComparator = (n1, n2)->Integer.compare(n1.getId(), n2.getId());
        final SerializableComparator<Node<ValueType, AveragesTo, T>> minSumComparator = (n1, n2)->compare(n1.getMinSumEndingHere(), n2.getMinSumEndingHere());
        final SerializableComparator<Node<ValueType, AveragesTo, T>> maxSumComparator = (n1, n2)->compare(n1.getMaxSumEndingHere(), n2.getMaxSumEndingHere());
        final SerializableComparator<? super Node<ValueType, AveragesTo, T>> minSumOuterComparator = (n1,n2)->(n1==n2?0:minSumComparator.thenComparing(idComparator).compare(n1, n2));
        final SerializableComparator<? super Node<ValueType, AveragesTo, T>> maxSumOuterComparator = (n1,n2)->(n1==n2?0:maxSumComparator.thenComparing(idComparator).compare(n1, n2));
        this.nodesOrderedByMinSum = new TreeSet<>(minSumOuterComparator);
        this.nodesOrderedByMaxSum = new TreeSet<>(maxSumOuterComparator);
    }
    
    /**
     * Writes the sequence iteratively instead of by recursion, which would be the default
     * {@link ObjectOutputStream} behavior. The nodes need to be "wired" again by the reading end
     * regarding the {@link #previous} and {@link #next} links based on the order in which the
     * nodes are written to the stream.<p>
     * 
     * After the nodes follow two references per node to the start nodes of the extreme sum sub-sequences ending at that node,
     * as those are transient again to avoid recursion, and thus not written by the default serialization process. We can assume
     * that the {@link ObjectOutputStream} already holds those nodes, and so only references to those nodes need to be written.
     */
    private void writeObject(ObjectOutputStream oos) throws IOException {
        oos.defaultWriteObject();
        final Iterable<Node<ValueType, AveragesTo, T>> iterable = this::nodeIterator;
        for (final Node<ValueType, AveragesTo, T> node : iterable) {
            oos.writeObject(node);
        }
        // writing the start nodes of the extreme sum sub-sequences separately, as they are transient and thus not written by the default serialization process;
        // we need to write them separately as otherwise we would lose the references to those nodes, and thus the information about where the extreme sum sub-sequences start
        for (final Node<ValueType, AveragesTo, T> node : iterable) {
            oos.writeObject(node.getStartOfMinSumSubSequenceEndingHere());
            oos.writeObject(node.getStartOfMaxSumSubSequenceEndingHere());
        }
    }
    
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject(); // this is expected to read the size field
        Node<ValueType, AveragesTo, T> lastRead = null;
        for (int i=0; i<size; i++) {
            @SuppressWarnings("unchecked")
            final Node<ValueType, AveragesTo, T> node = (Node<ValueType, AveragesTo, T>) ois.readObject();
            node.setPrevious(lastRead);
            if (lastRead != null) {
                lastRead.setNext(node);
            }
            lastRead = node;
            if (first == null) {
                first = lastRead;
            }
        }
        last = lastRead;
        final Iterable<Node<ValueType, AveragesTo, T>> iterable = this::nodeIterator;
        for (final Node<ValueType, AveragesTo, T> node : iterable) {
            @SuppressWarnings("unchecked")
            final Node<ValueType, AveragesTo, T> startOfMinSubSubSequenceEndingHere = (Node<ValueType, AveragesTo, T>) ois.readObject();
            node.setStartOfMinSumSubSequenceEndingHere(startOfMinSubSubSequenceEndingHere);
            @SuppressWarnings("unchecked")
            final Node<ValueType, AveragesTo, T> startOfMaxSubSubSequenceEndingHere = (Node<ValueType, AveragesTo, T>) ois.readObject();
            node.setStartOfMaxSumSubSequenceEndingHere(startOfMaxSubSubSequenceEndingHere);
        }
    }
    
    @Override
    public int size() {
        return size;
    }
    
    @Override
    public boolean isEmpty() {
        return first == null;
    }
    
    private Iterable<Node<ValueType, AveragesTo, T>> getNodeIterable() {
        return this::nodeIterator;
    }
    
    private Iterator<Node<ValueType, AveragesTo, T>> nodeIterator() {
        return nodeIterator(first, last);
    }
    
    private Iterator<Node<ValueType, AveragesTo, T>> nodeIterator(final Node<ValueType, AveragesTo, T> firstNode, final Node<ValueType, AveragesTo, T> lastNode) {
        return new Iterator<Node<ValueType, AveragesTo, T>>() {
            private Node<ValueType, AveragesTo, T> current = firstNode;
            
            @Override
            public boolean hasNext() {
                return current != (lastNode==null ? null : lastNode.getNext());
            }

            @Override
            public Node<ValueType, AveragesTo, T> next() {
                final Node<ValueType, AveragesTo, T> result = current;
                current = current.getNext();
                return result;
            }
        };
    }
    
    @Override
    public Iterator<T> iterator() {
        return Util.map(getNodeIterable(), node->node.getValue()).iterator();
    }

    @Override
    public void add(int index, T t) {
        if (index < 0 || index > size()) {
            throw new IndexOutOfBoundsException("Trying to add at index "+index+" to a sequence of size "+size());
        }
        final Node<ValueType, AveragesTo, T> node;
        if (isEmpty()) {
            node = new Node<>(/* previous */ null, /* next */ null, t);
            first = node;
            last = node;
            node.updateThisFromPrevious(nodesOrderedByMinSum, nodesOrderedByMaxSum); // no need to consider changes; it's the only element
        } else {
            final Node<ValueType, AveragesTo, T> nodeBeforeIndex;
            final Node<ValueType, AveragesTo, T> nodeAfterIndex;
            if (index>0) {
                nodeBeforeIndex = getNode(index-1);
                nodeAfterIndex = nodeBeforeIndex.getNext();
            } else if (index<size()) {
                nodeAfterIndex = getNode(index);
                nodeBeforeIndex = nodeAfterIndex.getPrevious();
            } else {
                throw new InternalError("This shouldn't have happened as it means the sequence is empty, and we shouldn't have arrived in this branch");
            }
            node = new Node<>(nodeBeforeIndex, nodeAfterIndex, t);
            if (nodeBeforeIndex != null) {
                nodeBeforeIndex.setNext(node);
            } else {
                first = node;
            }
            if (nodeAfterIndex != null) {
                nodeAfterIndex.setPrevious(node);
            } else {
                last = node;
            }
            node.updateThisFromPrevious(nodesOrderedByMinSum, nodesOrderedByMaxSum); // manages addition to tree maps
            if (nodeBeforeIndex == null
                    || !node.getMaxSumEndingHere().equals(nodeBeforeIndex.getMaxSumEndingHere())
                    || !node.getMinSumEndingHere().equals(nodeBeforeIndex.getMinSumEndingHere())
                    || node.getStartOfMaxSumSubSequenceEndingHere() != nodeBeforeIndex.getStartOfMaxSumSubSequenceEndingHere()
                    || node.getStartOfMinSumSubSequenceEndingHere().equals(nodeBeforeIndex.getStartOfMinSumSubSequenceEndingHere())) {
                // the inserted node differs from the previous node in one of the extreme sums ending at it, and/or
                // regarding where those sub-sequences start, so we need to propagate the updates to subsequent nodes
                propagateChanges(node);
            }
        }
        size++;
    }

    /**
     * Propagates changes to the {@link Node#getNext() next} node that follows {@code node}, and all further ones up to
     * the end of the collection or until no more changes occur that need propagating. The {@link #nodesOrderedByMinSum}
     * and {@link #nodesOrderedByMaxSum} collections are updated accordingly.
     */
    private void propagateChanges(Node<ValueType, AveragesTo, T> node) {
        boolean changedMin = true;
        boolean changedMax = true;
        propagateChanges(node, changedMin, changedMax);
    }

    /**
     * Propagates changes to the {@link Node#getNext() next} node that follows {@code node}, and all further ones up to
     * the end of the collection or until no more changes occur that need propagating. The {@link #nodesOrderedByMinSum}
     * and {@link #nodesOrderedByMaxSum} collections are updated accordingly.
     * 
     * @param changedMin
     *            tells if changes to {@code node}'s minimum sum sub-sequences need propagation
     * @param changedMax
     *            tells if changes to {@code node}'s maximum sum sub-sequences need propagation
     */
    private void propagateChanges(Node<ValueType, AveragesTo, T> node, boolean changedMin, boolean changedMax) {
        int minChangeCount = 0;
        int maxChangeCount = 0;
        Node<ValueType, AveragesTo, T> current = node.getNext();
        while ((changedMin || changedMax) && current != null) {
            if (changedMin) {
                minChangeCount++;
                changedMin = current.updateMinFromPrevious(nodesOrderedByMinSum);
            }
            if (changedMax) {
                maxChangeCount++;
                changedMax = current.updateMaxFromPrevious(nodesOrderedByMaxSum);
            }
            current = current.getNext();
            assert nodesOrderedByMinSum.size() == nodesOrderedByMaxSum.size();
        }
        minChangePropagationStepsSum += minChangeCount;
        minChangePropagationsCount++;
        maxChangePropagationStepsSum += maxChangeCount;
        maxChangePropagationsCount++;
    }

    private Node<ValueType, AveragesTo, T> getNode(int index) {
        if (index < 0 || index >= size()) {
            throw new IndexOutOfBoundsException("Trying to find node at index "+index+" in a sequence of size "+size());
        }
        final Node<ValueType, AveragesTo, T> result;
        if (isEmpty()) {
            result = null;
        } else {
            if (index > size()/2) { // search from the end
                result = step(last, size()-1-index, Node::getPrevious);
            } else { // search from the beginning
                result = step(first, index, Node::getNext);
            }
        }
        return result;
    }
    
    private Node<ValueType, AveragesTo, T> step(Node<ValueType, AveragesTo, T> start, int numberOfSteps,
            Function<Node<ValueType, AveragesTo, T>, Node<ValueType, AveragesTo, T>> stepper) {
        Node<ValueType, AveragesTo, T> current = start;
        for (int i=0; i<numberOfSteps; i++) {
            current = stepper.apply(current);
        }
        return current;
    }
    
    @Override
    public void remove(int index) {
        final Node<ValueType, AveragesTo, T> node = getNode(index);
        assert node != null; // otherwise, an IndexOutOfBoundsException should have been thrown
        remove(node);
    }
    
    private void remove(Node<ValueType, AveragesTo, T> node) {
        if (node.getPrevious() != null) {
            node.getPrevious().setNext(node.getNext());
        } else {
            assert first == node;
            first = node.getNext();
        }
        if (node.getNext() != null) {
            node.getNext().setPrevious(node.getPrevious());
            final boolean changedMin = node.getPrevious() == null
                    || !node.getMinSumEndingHere().equals(node.getPrevious().getMinSumEndingHere())
                    || node.getStartOfMinSumSubSequenceEndingHere() != node.getPrevious()
                            .getStartOfMinSumSubSequenceEndingHere();
            final boolean changedMax = node.getPrevious() == null
                    || !node.getMaxSumEndingHere().equals(node.getPrevious().getMaxSumEndingHere())
                    || node.getStartOfMaxSumSubSequenceEndingHere() != node.getPrevious()
                            .getStartOfMaxSumSubSequenceEndingHere();
            propagateChanges(node, changedMin, changedMax);
        } else {
            assert last == node;
            last = node.getPrevious();
        }
        nodesOrderedByMinSum.remove(node);
        nodesOrderedByMaxSum.remove(node);
        assert nodesOrderedByMinSum.size() == nodesOrderedByMaxSum.size();
        size--;
    }

    @Override
    public void remove(T t) {
        Node<ValueType, AveragesTo, T> node = first;
        while (node != null && !node.getValue().equals(t)) {
            node = node.getNext();
        }
        if (node != null) {
            assert node.getValue().equals(t);
            remove(node);
        }
    }
    
    @Override
    public void removeFirst(int howManyNodesToRemove) {
        if (howManyNodesToRemove < 0) {
            throw new IllegalArgumentException("Cannot remove a negative number of nodes: "+howManyNodesToRemove);
        }
        if (howManyNodesToRemove > size()) {
            throw new IllegalArgumentException("Cannot remove more nodes than the sequence currently holds: "+howManyNodesToRemove+">"+size());
        }
        if (howManyNodesToRemove > 0) { // otherwise this is a no-op
            final Node<ValueType, AveragesTo, T> lastNodeToRemove = getNode(howManyNodesToRemove-1);
            first = lastNodeToRemove.getNext();
            Node<ValueType, AveragesTo, T> nodeRemoved = lastNodeToRemove;
            while (nodeRemoved != null) {
                nodesOrderedByMinSum.remove(nodeRemoved);
                nodesOrderedByMaxSum.remove(nodeRemoved);
                assert nodesOrderedByMinSum.size() == nodesOrderedByMaxSum.size();
                nodeRemoved = nodeRemoved.getPrevious();
            }
            if (first != null) {
                first.setPrevious(null);
                if (first.updateThisFromPrevious(nodesOrderedByMinSum, nodesOrderedByMaxSum)) {
                    propagateChanges(first);
                }
            } else {
                assert howManyNodesToRemove == size();
                last = null;
            }
            size -= howManyNodesToRemove;
        }
    }

    @Override
    public ScalableValueWithDistance<ValueType, AveragesTo> getMinSum() {
        return isEmpty() ? null : nodesOrderedByMinSum.first().getMinSumEndingHere();
    }

    @Override
    public ScalableValueWithDistance<ValueType, AveragesTo> getMaxSum() {
        return isEmpty() ? null : nodesOrderedByMaxSum.last().getMaxSumEndingHere();
    }

    @Override
    public int getStartIndexOfMaxSumSequence() {
        return isEmpty() ? -1 : Util.indexOf(getNodeIterable(), nodesOrderedByMaxSum.last().getStartOfMaxSumSubSequenceEndingHere());
    }

    @Override
    public int getEndIndexOfMaxSumSequence() {
        return isEmpty() ? -1 : Util.indexOf(getNodeIterable(), nodesOrderedByMaxSum.last());
    }

    @Override
    public int getStartIndexOfMinSumSequence() {
        return isEmpty() ? -1 : Util.indexOf(getNodeIterable(), nodesOrderedByMinSum.first().getStartOfMinSumSubSequenceEndingHere());
    }

    @Override
    public int getEndIndexOfMinSumSequence() {
        return isEmpty() ? -1 : Util.indexOf(getNodeIterable(), nodesOrderedByMinSum.first());
    }

    @Override
    public Iterator<T> getSubSequenceWithMaxSum() {
        final Iterator<T> result;
        if (isEmpty()) {
            result = Collections.emptyIterator();
        } else {
            final Node<ValueType, AveragesTo, T> nodeWhereBestMaxSumSubSequenceEnds = nodesOrderedByMaxSum.last();
            final Iterable<Node<ValueType, AveragesTo, T>> nodeIterable = ()->nodeIterator(nodeWhereBestMaxSumSubSequenceEnds.getStartOfMaxSumSubSequenceEndingHere(), nodeWhereBestMaxSumSubSequenceEnds);
            result = Util.map(nodeIterable, node->node.getValue()).iterator();
        }
        return result;
    }

    @Override
    public Iterator<T> getSubSequenceWithMinSum() {
        final Iterator<T> result;
        if (isEmpty()) {
            result = Collections.emptyIterator();
        } else {
            final Node<ValueType, AveragesTo, T> nodeWhereBestMinSumSubSequenceEnds = nodesOrderedByMinSum.first();
            final Iterable<Node<ValueType, AveragesTo, T>> nodeIterable = ()->nodeIterator(nodeWhereBestMinSumSubSequenceEnds.getStartOfMinSumSubSequenceEndingHere(), nodeWhereBestMinSumSubSequenceEnds);
            result = Util.map(nodeIterable, node->node.getValue()).iterator();
        }
        return result;
    }
    
    @Override
    public int getAverageMinChangePropagationSteps() {
        return minChangePropagationsCount == 0 ? 0 : minChangePropagationStepsSum / minChangePropagationsCount;
    }
    
    @Override
    public int getAverageMaxChangePropagationSteps() {
        return maxChangePropagationsCount == 0 ? 0 : maxChangePropagationStepsSum / maxChangePropagationsCount;
    }
    
    @Override
    public void resetStats() {
        minChangePropagationStepsSum = 0;
        minChangePropagationsCount = 0;
        maxChangePropagationStepsSum = 0;
        maxChangePropagationsCount = 0;
    }

    @Override
    public String toString() {
        return "KadaneExtremeSubsequenceFinderLinkedNodesImpl [size=" + size
                + ", minChangePropagationStepsAvg=" + (minChangePropagationsCount==0?null:(minChangePropagationStepsSum / minChangePropagationsCount))
                + ", maxChangePropagationStepsAvg=" + (maxChangePropagationsCount==0?null:(maxChangePropagationStepsSum / maxChangePropagationsCount)) + "]";
    }
}
