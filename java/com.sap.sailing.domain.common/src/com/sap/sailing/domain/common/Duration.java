package com.sap.sailing.domain.common;

import java.io.Serializable;

import com.sap.sailing.domain.common.impl.MillisecondsDurationImpl;

/**
 * A time duration that can be converted to various time units and that interoperates with {@link TimePoint}.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public interface Duration extends Serializable, Comparable<Duration> {
    
    static final Duration NULL = new MillisecondsDurationImpl(0);
    static final Duration ONE_SECOND = new MillisecondsDurationImpl(1000);
    static final Duration ONE_MINUTE = ONE_SECOND.times(60);
    static final Duration ONE_HOUR = ONE_MINUTE.times(60);
    static final Duration ONE_DAY = ONE_HOUR.times(24);
    
    long asMillis();
    
    double asSeconds();
    
    double asMinutes();
    
    double asHours();
    
    double asDays();
    
    Duration divide(long divisor);
    Duration times(long factor);
    
    Duration minus(Duration duration);
    Duration minus(long milliseconds);
    
    Duration plus(long milliseconds);
    Duration plus(Duration duration);
}
