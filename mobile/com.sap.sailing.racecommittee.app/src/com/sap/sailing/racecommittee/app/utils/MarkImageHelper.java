package com.sap.sailing.racecommittee.app.utils;

import java.util.List;

import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.common.MarkType;

public abstract class MarkImageHelper {
    List<MarkImageDescriptor> markImageDescriptors;
    protected MarkImageDescriptor defaultCourseMarkDescriptor;
    
    public int resolveMarkImage(Mark mark) {
        MarkImageDescriptor result = defaultCourseMarkDescriptor;
        int highestCompatibilityLevel = -1;
        
        for (MarkImageDescriptor imageDescriptor: markImageDescriptors) {
            int compatibilityLevel = imageDescriptor.getCompatibilityLevel(mark.getType(), mark.getColor(), mark.getShape(), mark.getPattern());
            if(compatibilityLevel > highestCompatibilityLevel) {
               result = imageDescriptor;
               highestCompatibilityLevel = compatibilityLevel;
               if(highestCompatibilityLevel == 3) {
                   break;
               }
            }
        }
        
        return result.getDrawableId();
    }
    
    protected MarkImageDescriptor createMarkImageDescriptor(int drawableId, MarkType type, String color, String shape, String pattern) {
        MarkImageDescriptor markIconDescriptor = new MarkImageDescriptor(drawableId, type, color, shape, pattern);
        markImageDescriptors.add(markIconDescriptor);
        
        return markIconDescriptor;
    }

}
