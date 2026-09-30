package com.sap.sailing.domain.common.windfinder;

import java.net.URL;

import com.sap.sse.common.NamedWithID;
import com.sap.sse.common.Positioned;
import com.sap.sse.common.TimePoint;

/**
 * A measurement spot for which a report and / or a forecast may exist. Equality and
 * hash code are based on the {@link #getId() ID} only.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public interface SpotBase extends NamedWithID, Positioned {
    /**
     * A spot's ID is always a {@link String}
     */
    @Override
    String getId();
    
    /**
     * A keyword that is used, in particular, when constructing the URLs for {@link #getReportUrl() report} and
     * {@link #getForecastUrl() forecast} web pages.
     */
    String getKeyword();
    
    /**
     * Name of the country this spot is in, provided in the English language
     */
    String getEnglishCountryName();
    
    /**
     * @return the URL of a web page showing a current weather report for this spot
     */
    String getReportUrl();
    
    /**
     * @return the URL of a web page showing a weather forecast for this spot
     */
    String getForecastUrl();
    
    /**
     * @return the URL of a web page showing general wind statistics for this spot
     */
    String getStatisticsUrl();
    
    /**
     * For this spot provides a {@link URL} that a web UI can use to link
     * to the WindFinder web site with the content most appropriate given the time point.
     * This could be the report page if the time is about now; the forecast page if the time point
     * is up to ten days in the future, or the statistics page if the time point is out of any
     * of the scopes above.
     */
    String getCurrentlyMostAppropriateUrl(TimePoint timePoint);
}
