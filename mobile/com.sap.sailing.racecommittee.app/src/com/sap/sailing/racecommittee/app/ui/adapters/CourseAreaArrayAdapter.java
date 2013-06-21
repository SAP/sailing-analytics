package com.sap.sailing.racecommittee.app.ui.adapters;

import java.util.List;

import android.content.Context;

import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.racecommittee.app.AppPreferences;

public class CourseAreaArrayAdapter extends NamedArrayAdapter<CourseArea> {

    public CourseAreaArrayAdapter(Context context, List<CourseArea> namedList) {
        super(context, namedList);
    }
    
    @Override
    public boolean isEnabled(int position) {
        CourseArea courseArea = getItem(position);
        return AppPreferences.getManagedCourseAreaNames(getContext()).contains(courseArea.getName());
    }

}
