package com.sap.sailing.server.gateway.jaxrs.sharing;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import javax.ws.rs.core.UriInfo;

import com.sap.sse.common.Base64Utils;

public class TokenizedHomePlaceUrlBuilder {
    private static final String EVENT_PATH = "/event";
    private static final String SERIES_PATH = "/series";
    private static final String REGATTA_OVERVIEW_PATH = "/regatta/overview";
    private static final String GWT_PREFIX = "/gwt";
    private static final String HOME_HTML = "/Home.html";
    private static final String STARTOFPARAMS = "/:";
    private static final String STARTOFTOKENPATH = "#";

    /**
     * See also {@code AbstractEventPlace.Tokenizer.PARAM_EVENTID}
     */
    private static final String EVENT_ID_PARAM = "eventId";
    
    /**
     * See also {@code AbstractEventPlace.Tokenizer.PARAM_REGATTAID}
     */
    private static final String REGATTA_ID_PARAM = "regattaId";
    private static final String LEADERBOARD_GROUP_ID_PARAM = "leaderboardGroupId";

    private URL baseUrl;
    private boolean tokenPresent;

    public TokenizedHomePlaceUrlBuilder(UriInfo uri) {
        URI baseUri = uri.getBaseUri();
        String cleanedUri = baseUri.toString().replace(baseUri.getPath(), "");
        try {
            baseUrl = new URL(cleanedUri + GWT_PREFIX + HOME_HTML);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
    }

    public String asEventPlaceLink(String eventId) {
        String url = new String(baseUrl.toString());
        url += STARTOFTOKENPATH + EVENT_PATH;
        url += buildToken(eventId, EVENT_ID_PARAM);
        return url;
    }

    public String asSeriesPlaceLink(String eventId) {
        String url = new String(baseUrl.toString());
        url += STARTOFTOKENPATH + SERIES_PATH;
        url += buildToken(eventId, LEADERBOARD_GROUP_ID_PARAM);
        return url;
    }

    public String asRegattaPlaceLink(String eventId, String regattaId) {
        String url = new String(baseUrl.toString());
        url += STARTOFTOKENPATH + REGATTA_OVERVIEW_PATH;
        url += buildToken(eventId, EVENT_ID_PARAM);
        url += buildToken(Base64Utils.toBase64(regattaId.getBytes()), REGATTA_ID_PARAM);
        return url;
    }

    private String buildToken(String tokenId, String tokenKey) {
        String url = new String();
        if (!tokenPresent) {
            url += STARTOFPARAMS;
            tokenPresent = true;
        } else {
            url += "&";
        }
        url += tokenKey + "=" + tokenId;
        return url;
    }

    public URL getBaseUrl() {
        return baseUrl;
    }
}
