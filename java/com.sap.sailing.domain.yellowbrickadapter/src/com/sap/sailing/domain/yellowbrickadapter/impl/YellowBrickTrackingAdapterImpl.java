package com.sap.sailing.domain.yellowbrickadapter.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.json.simple.parser.ParseException;

import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.regattalog.RegattaLogStore;
import com.sap.sailing.domain.yellowbrickadapter.YellowBrickConfiguration;
import com.sap.sailing.domain.yellowbrickadapter.YellowBrickConfigurationListener;
import com.sap.sailing.domain.yellowbrickadapter.YellowBrickRace;
import com.sap.sailing.domain.yellowbrickadapter.YellowBrickRaceTrackingConnectivityParams;
import com.sap.sailing.domain.yellowbrickadapter.YellowBrickTrackingAdapter;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.util.HttpUrlConnectionHelper;

public class YellowBrickTrackingAdapterImpl implements YellowBrickTrackingAdapter {
    private final static Duration TIMEOUT_FOR_RACE_LOADING = Duration.ONE_MINUTE.times(2);
    final static Duration DEFAULT_POLLING_INTERVAL = Duration.ONE_MINUTE.times(1);
    
    /**
     * The base URL template with a string parameter for the race URL
     */
    private final static String BASE_URL_TEMPLATE = "https://yb.tl/API3/Race/%s/GetPositions";
    
    /**
     * Template to construct the URL from the race URL parameter and the number of latest fixes per team to obtain
     */
    private final static String NUMBER_OF_POSITIONS_URL_TEMPLATE = BASE_URL_TEMPLATE + "?n=%d";
    
    /**
     * Template to construct the URL from the race URL parameter and the date (as Unix time stamp, seconds
     * since the epoch (not milliseconds!), 1970-01-01T00:00:00Z) after which to fetch positions.
     */
    private final static String POSITIONS_SINCE_DATE_URL_TEMPLATE = BASE_URL_TEMPLATE + "?t=%d";
    
    private final DomainFactory baseDomainFactory;
    
    /**
     * Keyed by pairs consisting of {@link YellowBrickConfiguration#getCreatorName() creator name} and
     * {@link YellowBrickConfiguration#getRaceUrl() race URL}.
     */
    private final ConcurrentMap<Pair<String, String>, YellowBrickConfiguration> yellowBrickConfigurations;
    
    /**
     * A {@link Collections#synchronizedSet(Set) synchronized set} for the listeners
     */
    private final Set<YellowBrickConfigurationListener> yellowBrickConfigurationListeners;
    
    public YellowBrickTrackingAdapterImpl(DomainFactory baseDomainFactory) {
        this.baseDomainFactory = baseDomainFactory;
        this.yellowBrickConfigurations = new ConcurrentHashMap<>();
        this.yellowBrickConfigurationListeners = Collections.synchronizedSet(new HashSet<>());
    }

    @Override
    public void addYellowBrickRace(RacingEventService service, RegattaIdentifier regattaToAddTo,
            String yellowBrickRaceUrl, RaceLogStore raceLogStore, RegattaLogStore regattaLogStore,
            String creatorUsername, String raceUrl, boolean trackWind, boolean correctWindByDeclination)
            throws Exception {
        final Pair<String, String> configKey = getConfigKey(raceUrl, creatorUsername);
        final YellowBrickConfiguration configuration = yellowBrickConfigurations.get(configKey);
        service.addRace(regattaToAddTo,
                new YellowBrickRaceTrackingConnectivityParams(yellowBrickRaceUrl, configuration==null?null:configuration.getUsername(),
                        configuration==null?null:configuration.getPassword(), trackWind, correctWindByDeclination, raceLogStore, regattaLogStore,
                        baseDomainFactory, this),
                /* timeout */ TIMEOUT_FOR_RACE_LOADING.asMillis());
    }
    
    String getUrlForLatestFix(String raceUrl, Optional<String> username, Optional<String> password) {
        return appendUsernameAndPasswordParameters(String.format(NUMBER_OF_POSITIONS_URL_TEMPLATE, raceUrl, 1), username, password);
    }

    private String appendUsernameAndPasswordParameters(String url, Optional<String> username, Optional<String> password) {
        final StringBuilder sb = new StringBuilder(url);
        appendOptionalParameter(sb, "username", username);
        appendOptionalParameter(sb, "password", password);
        return sb.toString();
    }

    private void appendOptionalParameter(StringBuilder urlBuilder, String parameterName, Optional<String> parameterValue) {
        if (parameterValue.isPresent() && Util.hasLength(parameterValue.get())) {
            urlBuilder.append("&");
            urlBuilder.append(parameterName);
            urlBuilder.append("=");
            try {
                urlBuilder.append(URLEncoder.encode(parameterValue.get(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
    }
    
    String getUrlForAllData(String raceUrl, Optional<String> username, Optional<String> password) {
        return getUrlForDataSinceTimePoint(raceUrl, TimePoint.BeginningOfTime, username, password);
    }

    private String getUrlForDataSinceTimePoint(String raceUrl, final TimePoint since, Optional<String> username,
            Optional<String> password) {
        return appendUsernameAndPasswordParameters(String.format(POSITIONS_SINCE_DATE_URL_TEMPLATE, raceUrl, since.asMillis()/1000l), username, password);
    }

    @Override
    public YellowBrickRace getRaceMetadata(String raceUrl, Optional<String> username, Optional<String> password) throws IOException, ParseException {
        final String url = getUrlForLatestFix(raceUrl, username, password);
        final PositionsDocument doc = getPositionsDocumentForUrl(url);
        return new YellowBrickRaceImpl(raceUrl, doc.getTimePointOfLastFix(), doc.getTeams());
    }

    private PositionsDocument getPositionsDocumentForUrl(final String url)
            throws MalformedURLException, IOException, ParseException {
        final URLConnection result = HttpUrlConnectionHelper.redirectConnectionWithBearerToken(new URL(url), TIMEOUT_FOR_RACE_LOADING, /* bearer token */ null);
        final Charset charset = HttpUrlConnectionHelper.getCharsetFromConnectionOrDefault(result, "UTF-8");
        final InputStream inputStream = (InputStream) result.getContent();
        final PositionsDocument doc = new GetPositionsParser().parse(new InputStreamReader(inputStream, charset), /* inferSpeedAndBearing */ true);
        return doc;
    }
    
    @Override
    public PositionsDocument getPositionsSince(String raceUrl, TimePoint since, Optional<String> username, Optional<String> password) throws MalformedURLException, IOException, ParseException {
        return getPositionsDocumentForUrl(getUrlForDataSinceTimePoint(raceUrl, since, username, password));
    }

    @Override
    public PositionsDocument getStoredData(String raceUrl, Optional<String> username, Optional<String> password) throws MalformedURLException, IOException, ParseException {
        return getPositionsDocumentForUrl(getUrlForAllData(raceUrl, username, password));
    }

    @Override
    public YellowBrickConfiguration createYellowBrickConfiguration(String name, String raceUrl, String username, String password, String creatorName) {
        final YellowBrickConfiguration config = new YellowBrickConfigurationImpl(name, raceUrl, username, password, creatorName);
        addYellowBrickConfiguration(config);
        return config;
    }
    
    @Override
    public void addYellowBrickConfiguration(YellowBrickConfiguration yellowBrickConfig) {
        yellowBrickConfigurations.put(getConfigKey(yellowBrickConfig), yellowBrickConfig);
        synchronized (yellowBrickConfigurationListeners) {
            for (final YellowBrickConfigurationListener listener : yellowBrickConfigurationListeners) {
                listener.yellowBrickConfigurationAdded(yellowBrickConfig);
            }
        }
    }

    @Override
    public void removeYellowBrickConfiguration(String raceUrl, String creatorName) {
        final YellowBrickConfiguration removedConfig = yellowBrickConfigurations.remove(getConfigKey(raceUrl, creatorName));
        if (removedConfig != null) {
            synchronized (yellowBrickConfigurationListeners) {
                for (final YellowBrickConfigurationListener listener : yellowBrickConfigurationListeners) {
                    listener.yellowBrickConfigurationRemoved(removedConfig);
                }
            }
        }
    }
    
    private Pair<String, String> getConfigKey(YellowBrickConfiguration config) {
        return new Pair<>(config.getCreatorName(), config.getRaceUrl());
    }
    
    private Pair<String, String> getConfigKey(String raceUrl, String creatorName) {
        return new Pair<>(creatorName, raceUrl);
    }

    @Override
    public void updateYellowBrickConfiguration(String name, String raceUrl, String username,
            String password, String creatorName) {
        final Pair<String, String> configKey = getConfigKey(raceUrl, creatorName);
        final YellowBrickConfiguration oldConfig = yellowBrickConfigurations.get(configKey);
        final YellowBrickConfigurationImpl updatedConfig = new YellowBrickConfigurationImpl(name, raceUrl, username,
                password == null ? oldConfig == null ? null : oldConfig.getPassword() : password,
                creatorName);
        yellowBrickConfigurations.put(configKey, updatedConfig);
        synchronized (yellowBrickConfigurationListeners) {
            for (final YellowBrickConfigurationListener listener : yellowBrickConfigurationListeners) {
                listener.yellowBrickConfigurationUpdated(updatedConfig);
            }
        }
    }

    @Override
    public void addYellowBrickConfigurationListener(YellowBrickConfigurationListener listener) {
        yellowBrickConfigurationListeners.add(listener);
    }

    @Override
    public void removeYellowBrickConfigurationListener(YellowBrickConfigurationListener listener) {
        yellowBrickConfigurationListeners.remove(listener);
    }

    @Override
    public Iterable<YellowBrickConfiguration> getYellowBrickConfigurations() {
        return yellowBrickConfigurations.values();
    }
}
