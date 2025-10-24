package com.sap.sailing.sailwave.html.resultimport.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.sap.sailing.domain.common.RegattaScoreCorrections;
import com.sap.sailing.domain.common.ScoreCorrectionProvider;
import com.sap.sailing.domain.resultimport.ResultUrlProvider;
import com.sap.sailing.resultimport.AbstractResultUrlProvider;
import com.sap.sailing.resultimport.RegattaResults;
import com.sap.sailing.resultimport.ResultUrlRegistry;
import com.sap.sailing.resultimport.impl.RegattaScoreCorrectionsImpl;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class ScoreCorrectionProviderImpl extends AbstractResultUrlProvider implements ScoreCorrectionProvider, ResultUrlProvider {
    private static final long serialVersionUID = 5853404150107387702L;
    public static final String PROVIDER_NAME = "Sailwave HTML Score Importer";
    
    public ScoreCorrectionProviderImpl(ResultUrlRegistry resultUrlRegistry) {
        super(resultUrlRegistry);
    }

    @Override
    public String getName() {
        return PROVIDER_NAME;
    }
    
    @Override
    public Map<String, Set<Util.Pair<String, TimePoint>>> getHasResultsForBoatClassFromDateByEventName() {
        Map<String, Set<Util.Pair<String, TimePoint>>> result = new HashMap<String, Set<Util.Pair<String,TimePoint>>>();
        SailwaveHtmlParser parser = new SailwaveHtmlParser();
        for (URL url : getReadableUrls()) {
            URLConnection conn;
            try {
                conn = url.openConnection();
                TimePoint lastModified = new MillisecondsTimePoint(conn.getLastModified());
                RegattaResults regattaResult = parser.getRegattaResults((InputStream) conn.getContent());
                final String boatClassName = getBoatClassName(regattaResult);
                result.put(boatClassName, Collections.singleton(new Util.Pair<String, TimePoint>(boatClassName, lastModified)));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    /**
     * @return the first non-empty string of the list of metadata, hoping it's something pointing at the boat class at least...
     */
    private String getBoatClassName(RegattaResults regattaResult) {
        Map<String, String> metadata = regattaResult.getMetadata();
        if (metadata.containsKey(SailwaveHtmlParser.CLASS_METADATA)) {
            return metadata.get(SailwaveHtmlParser.CLASS_METADATA);
        } else {
            for (String metadatum : metadata.values()) {
                if (metadatum != null && metadatum.length() > 0) {
                    return metadatum;
                }
            }
            return null;
        }
    }

    @Override
    public URL resolveUrl(String url) throws MalformedURLException {
        return new URL(url);
    }

    @Override
    public RegattaScoreCorrections getScoreCorrections(String eventName, String boatClassName,
            TimePoint timePoint) throws Exception {
        final SailwaveHtmlParser parser = new SailwaveHtmlParser();
        for (URL url : getReadableUrls()) {
            final URLConnection conn = url.openConnection();
            final RegattaResults regattaResult = parser.getRegattaResults((InputStream) conn.getContent());
            if ((boatClassName == null && getBoatClassName(regattaResult) == null) ||
                    boatClassName.equals(getBoatClassName(regattaResult))) {
                return new RegattaScoreCorrectionsImpl(this, regattaResult);
            }
        }
        return null;
    }

    @Override
    public RegattaScoreCorrections getScoreCorrections(InputStream inputStream) throws Exception {
        return new RegattaScoreCorrectionsImpl(this, new SailwaveHtmlParser().getRegattaResults(inputStream));
    }

    @Override
    public String getOptionalSampleURL() {
        return null;
    }
}
