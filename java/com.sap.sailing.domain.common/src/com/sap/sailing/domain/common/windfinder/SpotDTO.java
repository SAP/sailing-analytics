package com.sap.sailing.domain.common.windfinder;

import com.sap.sse.common.Duration;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.impl.NamedImpl;

public class SpotDTO extends NamedImpl implements SpotBase {
    private static final long serialVersionUID = 1642900468710612984L;
    private static final String BASE_URL = "https://www.windfinder.com";
    private static final String BASE_REPORT_URL = BASE_URL + "/report";
    private static final String BASE_FORECAST_URL = BASE_URL + "/forecast";
    private static final String BASE_STATISTICS_URL = BASE_URL + "/windstatistics";
    
    private static final Duration FORECAST_DURATION = Duration.ONE_DAY.times(9);
    private static final Duration REPORT_LOOKBACK = Duration.ONE_DAY.times(7);

    private final String id;
    private final String keyword;
    private final String englishCountryName;
    private final Position position;
    
    public SpotDTO(String name, String id, String keyword, String englishCountryName, Position position) {
        super(name);
        this.id = id;
        this.keyword = keyword;
        this.englishCountryName = englishCountryName;
        this.position = position;
    }
    
    /**
     * Copy constructor
     */
    public SpotDTO(SpotBase spot) {
        this(spot.getName(), spot.getId(), spot.getKeyword(), spot.getEnglishCountryName(), spot.getPosition());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SpotDTO other = (SpotDTO) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    @Override
    public Position getPosition() {
        return position;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getKeyword() {
        return keyword;
    }

    @Override
    public String getEnglishCountryName() {
        return englishCountryName;
    }

    @Override
    public String getReportUrl() {
        return BASE_REPORT_URL+"/"+getKeyword();
    }

    @Override
    public String getForecastUrl() {
        return BASE_FORECAST_URL+"/"+getKeyword();
    }

    @Override
    public String getStatisticsUrl() {
        return BASE_STATISTICS_URL+"/"+getKeyword();
    }
    
    @Override
    public String getCurrentlyMostAppropriateUrl(TimePoint timePoint) {
        final String result;
        final TimePoint now = MillisecondsTimePoint.now();
        if (timePoint.compareTo(now.plus(Duration.ONE_MINUTE))<=0) {  // up to one minute into the future
            if (timePoint.compareTo(now.minus(REPORT_LOOKBACK))>=0) { // and up to seven days back
                result = getReportUrl();                              // is covered by the report page;
            } else { // we're looking further into the past; so it's the statistics page
                result = getStatisticsUrl();
            }
        } else { // we're looking into the future
            if (timePoint.compareTo(now.plus(FORECAST_DURATION))<=0) { // and it's still covered by the forecast
                result = getForecastUrl();
            } else { // otherwise we again resort to the stats page
                result = getStatisticsUrl();
            }
        }
        return result;
    }
}
