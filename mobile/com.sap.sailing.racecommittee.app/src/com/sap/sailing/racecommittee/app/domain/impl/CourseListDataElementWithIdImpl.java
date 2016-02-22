package com.sap.sailing.racecommittee.app.domain.impl;

import com.sap.sailing.racecommittee.app.ui.adapters.coursedesign.CourseListDataElement;

public class CourseListDataElementWithIdImpl extends CourseListDataElement {
    private static final long serialVersionUID = -6655629745339992249L;

    private long mId;

    public void setId(long id) {
        mId = id;
    }

    public long getId() {
        return mId;
    }
}
