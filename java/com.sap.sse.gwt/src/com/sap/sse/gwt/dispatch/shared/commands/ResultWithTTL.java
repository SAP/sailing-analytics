package com.sap.sse.gwt.dispatch.shared.commands;

import com.sap.sse.common.Duration;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.gwt.dispatch.shared.caching.HasClientCacheTotalTimeToLive;
import com.sap.sse.gwt.shared.DTO;

/**
 * Result implementation that holds a {@link DTO} with a defined time to live expiration.
 *
 * @param <T> The type of the actual result that is wrapped by this.
 */
public class ResultWithTTL<T extends DTO> implements Result, HasClientCacheTotalTimeToLive {
    
    /** max millis the result might be loaded earlier so we do batches **/
    public static final Duration MAX_TIME_TO_LOAD_EARLIER = Duration.ONE_SECOND.times(5);

    /** Time to live. Defines when the client should ask for data the next time.*/
    private Duration ttl;
    private T dto;
    @SuppressWarnings("unused")
    private ResultWithTTL() {
    }

    public ResultWithTTL(long ttl, T dto) {
       this(new MillisecondsDurationImpl(ttl), dto);
    }
    
    public ResultWithTTL(Duration ttl, T dto) {
        this.ttl = ttl;
        this.dto = dto;
    }

    public T getDto() {
        return dto;
    }

    public Duration getTtl() {
        return ttl;
    }
    
    public long getTtlMillis() {
        return ttl.asMillis();
    }

    /**
     * Reduce the total time to live value for cache by the time needed so the refresh manager can batch calls. Result
     * will not be negative.
     */
    @Override
    public int cacheTotalTimeToLiveMillis() {
        return (int) Math.max(0, getTtlMillis() - MAX_TIME_TO_LOAD_EARLIER.asMillis());
    }

}
