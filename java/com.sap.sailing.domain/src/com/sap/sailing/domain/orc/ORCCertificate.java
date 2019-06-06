package com.sap.sailing.domain.orc;

import java.util.Map;

import com.sap.sse.common.Duration;
import com.sap.sse.common.Speed;

//TODO COMMENTS!!!
/**
 * 
 * @author Daniel Lisunkin (i505543)
 *
 */
public interface ORCCertificate {

    /**
     * Returns a {@link ORCPerformanceCurve} for the competitor owning this {@link ORCCertificate}.
     * 
     * @param course
     *          equals the {@link ORCPerformanceCurveCourse} (part of the whole course) sailed by the competitor upon the point of this call.
     * @return
     */
    public ORCPerformanceCurve getPerformanceCurve(ORCPerformanceCurveCourse course);
    
    /**
     * 
     * @return
     */
    public double getGPH();
    
    // TODO getCDL f�r Gruppeneinteilung
    
    /**
     * 
     * @return
     */
    public Map<Speed, Duration> getWindwardLeewardAllowances();
    
    /**
     * 
     * @return
     */
    public Map<Speed, Duration> getCircularRandomAllowances();
    
    /**
     * 
     * @return
     */
    public Map<Speed, Duration> getLongDistanceAllowances();
    
    /**
     * 
     * @return
     */
    public Map<Speed, Duration> getNonSpinnakerAllowances();
    
}