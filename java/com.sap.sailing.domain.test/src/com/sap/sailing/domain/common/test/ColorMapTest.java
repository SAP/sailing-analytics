package com.sap.sailing.domain.common.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.impl.ColorMapImpl;
import com.sap.sse.common.Color;
import com.sap.sse.common.impl.RGBColor;

public class ColorMapTest {
    // this is a duplicate variable to RaceMap.WATER_COLOR, as the RaceMap class cannot be loaded without a GWT context,
    // due to the GWT.create(), this would fail the junit test
    public static final Color WATER_COLOR = new RGBColor(0, 67, 125);

    @Test
    public void testHundredDistinctColors() {
        ColorMapImpl<Integer> colorMap = new ColorMapImpl<Integer>(WATER_COLOR);
        List<String> existingColors = new ArrayList<String>();
        int amountOfDistinctColorsToCreate = 100;
        for (int i = 1; i <= amountOfDistinctColorsToCreate; i++) {
            String colorByID = colorMap.getColorByID(i).getAsHtml();
            if (!existingColors.contains(colorByID)) {
                existingColors.add(colorByID);
            }
        }
        assertEquals(amountOfDistinctColorsToCreate, existingColors.size());
    }

    /**
     * A function only useful for visual tests. It creates an HTML file (as string) with 100 distinct colors on top of
     * the water color of the google map.
     */
    public void createColorMapAsHtml() {
        ColorMapImpl<Integer> colorMap = new ColorMapImpl<Integer>(WATER_COLOR);
        int amountOfDistinctColorsToCreate = 100;

        String colorMapAsHtml = "<html><head></head><body style='background-color: " + WATER_COLOR.getAsHtml() + "'>";

        for (int i = 1; i <= amountOfDistinctColorsToCreate; i++) {
            colorMapAsHtml += "<div style='height:3px; background-color:" + colorMap.getColorByID(i).getAsHtml()
                    + "'></div><br/>";
        }

        colorMapAsHtml += "</body></html>";
        System.out.println(colorMapAsHtml);
    }

    public static void main(String[] args) {
        new ColorMapTest().createColorMapAsHtml();
    }
}
