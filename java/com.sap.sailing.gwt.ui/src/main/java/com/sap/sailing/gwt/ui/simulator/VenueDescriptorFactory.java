package com.sap.sailing.gwt.ui.simulator;

import java.util.ArrayList;

import com.google.gwt.maps.client.base.LatLng;
import com.sap.sailing.gwt.ui.client.shared.racemap.CoordinateSystem;
import com.sap.sailing.simulator.util.SailingSimulatorConstants;
import com.sap.sse.common.impl.DegreePosition;

public class VenueDescriptorFactory {
    public static VenueDescriptor createVenue(char event, CoordinateSystem coordinateSystem) {
        VenueDescriptor result = null;
        switch (event) {
            case SailingSimulatorConstants.EventKielerWoche: 
                result = createKielVenue(coordinateSystem);
                break;
            case SailingSimulatorConstants.EventTravemuenderWoche:
                result = createTravemuendeVenue(coordinateSystem);
                break;
        }
        return result;
    }
    
    private static VenueDescriptor createKielVenue(CoordinateSystem coordinateSystem) {
        LatLng cPos;
        ArrayList<CourseAreaDescriptor> courseAreas = new ArrayList<CourseAreaDescriptor>();

        VenueDescriptor venue = new VenueDescriptor("Kieler Bucht", courseAreas);
        // Middle of Echo and Klio
        venue.setCenterPos(coordinateSystem.toLatLng(new DegreePosition(54.477245795, 10.220622225)));
        
        // TV
        cPos = coordinateSystem.toLatLng(new DegreePosition(54.43450, 10.19559167));
        CourseAreaDescriptor defaultCourseArea = new CourseAreaDescriptor("TV", cPos, 0.5, "#EAB75A");
        courseAreas.add(defaultCourseArea);

        // Golf
        cPos = coordinateSystem.toLatLng(new DegreePosition(54.41985556, 10.19454167));
        courseAreas.add(new CourseAreaDescriptor("Golf", cPos, 0.35, "#F1F3EF", "silver"));

        // Foxtrot
        cPos = coordinateSystem.toLatLng(new DegreePosition(54.445775, 10.29223889));
        courseAreas.add(new CourseAreaDescriptor("Foxtrot", cPos, 0.65, "#DA58A6"));

        // India
        cPos = coordinateSystem.toLatLng(new DegreePosition(54.44803611, 10.20863611));
        courseAreas.add(new CourseAreaDescriptor("India", cPos, 0.40, "#B7827B"));

        // Juliett
        cPos = coordinateSystem.toLatLng(new DegreePosition(54.46183611, 10.2239));
        courseAreas.add(new CourseAreaDescriptor("Juliett", cPos, 0.55, "#979b9b"));

        // Echo
        cPos = coordinateSystem.toLatLng(new DegreePosition(54.47640278, 10.20090556));
        courseAreas.add(new CourseAreaDescriptor("Echo", cPos, 0.60, "#1CADD9"));

        // Kilo
        cPos = coordinateSystem.toLatLng(new DegreePosition(54.47808889, 10.24033889));
        courseAreas.add(new CourseAreaDescriptor("Kilo", cPos, 0.55, "#9FC269"));

        // Charlie
        cPos = coordinateSystem.toLatLng(new DegreePosition(54.49327222, 10.17525833));
        courseAreas.add(new CourseAreaDescriptor("Charlie", cPos, 0.70, "#2796f1"));

        // Delta
        cPos = coordinateSystem.toLatLng(new DegreePosition(54.49706111, 10.21921944));
        courseAreas.add(new CourseAreaDescriptor("Delta", cPos, 0.75, "#179E8B"));

        // Bravo
        cPos = coordinateSystem.toLatLng(new DegreePosition(54.50911667, 10.13973333));
        courseAreas.add(new CourseAreaDescriptor("Bravo", cPos, 0.80, "#d34547"));

        // Alfa
        cPos = coordinateSystem.toLatLng(new DegreePosition(54.52905, 10.18515278));
        courseAreas.add(new CourseAreaDescriptor("Alfa", cPos, 1.00, "#d65c93"));
        
        venue.setDefaultCourseArea(defaultCourseArea);
        return venue;
    }

    private static VenueDescriptor createTravemuendeVenue(CoordinateSystem coordinateSystem) {
        LatLng cPos;
        ArrayList<CourseAreaDescriptor> courseAreas = new ArrayList<CourseAreaDescriptor>();

        VenueDescriptor venue = new VenueDescriptor("Kieler Bucht", courseAreas);
        venue.setCenterPos(coordinateSystem.toLatLng(new DegreePosition(54.01583, 10.92583)));

        // Alfa
        cPos = coordinateSystem.toLatLng(new DegreePosition(54.015, 10.835));
        CourseAreaDescriptor defaultCourseArea = new CourseAreaDescriptor("Alfa", cPos, 0.9, "#FF8030");
        courseAreas.add(defaultCourseArea);

        // Bravo
        cPos = coordinateSystem.toLatLng(new DegreePosition(54.01666667, 11.01666667));
        courseAreas.add(new CourseAreaDescriptor("Bravo", cPos, 0.75, "#179E8B"));

        // Charlie
        cPos = coordinateSystem.toLatLng(new DegreePosition(54.0025, 10.98333333));
        courseAreas.add(new CourseAreaDescriptor("Charlie", cPos, 0.6, "#CE3032"));

        // Delta
        cPos = coordinateSystem.toLatLng(new DegreePosition(54.02166667, 10.92083333));
        courseAreas.add(new CourseAreaDescriptor("Delta", cPos, 0.75, "#B4287C"));

        // Foxtrot
        cPos = coordinateSystem.toLatLng(new DegreePosition(54.00833333, 10.88333333));
        courseAreas.add(new CourseAreaDescriptor("Foxtrot", cPos, 0.75, "#FFFFFF", "silver"));

        // Golf
        cPos = coordinateSystem.toLatLng(new DegreePosition(53.98333333, 10.9));
        courseAreas.add(new CourseAreaDescriptor("Golf", cPos, 0.50, "#1CADD9"));

        // Hotel
        cPos = coordinateSystem.toLatLng(new DegreePosition(53.99, 10.95666667));
        courseAreas.add(new CourseAreaDescriptor("Hotel", cPos, 0.50, "#FFFF30", "silver"));

        // See
        cPos = coordinateSystem.toLatLng(new DegreePosition(54.04333333, 10.875));
        courseAreas.add(new CourseAreaDescriptor("See", cPos, 1.25, "#818585"));
      
        venue.setDefaultCourseArea(defaultCourseArea);
        return venue;
    }
}