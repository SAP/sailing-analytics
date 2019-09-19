package com.sap.sailing.domain.base.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

import com.sap.sailing.domain.base.ControlPointWithTwoMarks;
import com.sap.sailing.domain.base.Mark;

public class ControlPointWithTwoMarksImpl implements ControlPointWithTwoMarks {
    private static final long serialVersionUID = 2807354812133070574L;
    private final Mark left;
    private final Mark right;
    private final String name;
    private final Serializable id;
    private final String shortName;
    
    /**
     * @param name also used as ID for the mark; if you have a better ID, use {@link GateImpl(Serializable, Mark, Mark, String)} instead.
     */

    /** Use {@link #ControlPointWithTwoMarksImpl(Mark, Mark, String, String)} instead. */
    @Deprecated
    public ControlPointWithTwoMarksImpl(Mark left, Mark right, String name) {
        this(/* ID */ name, left, right, name, name);
    }

    /** Use {@link #ControlPointWithTwoMarksImpl(Serializable, Mark, Mark, String, String)} instead. */
    @Deprecated
    public ControlPointWithTwoMarksImpl(Serializable id, Mark left, Mark right, String name) {
        this(id, left, right, name, name);
    }

    public ControlPointWithTwoMarksImpl(Mark left, Mark right, String name, String shortName) {
        this(/* ID */ name, left, right, name, shortName);
    }
    
    public ControlPointWithTwoMarksImpl(Serializable id, Mark left, Mark right, String name, String shortName) {
        this.id = id;
        this.left = left;
        this.right = right;
        this.name = name;
        this.shortName = shortName;
    }

    @Override
    public Mark getLeft() {
        return left;
    }

    @Override
    public Mark getRight() {
        return right;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        return getName();
    }

    @Override
    public Iterable<Mark> getMarks() {
        Collection<Mark> result = new ArrayList<Mark>(2);
        result.add(getLeft());
        result.add(getRight());
        return result;
    }

    @Override
    public Serializable getId() {
        return id;
    }

    public String getShortName() {
        return shortName;
    }
}
