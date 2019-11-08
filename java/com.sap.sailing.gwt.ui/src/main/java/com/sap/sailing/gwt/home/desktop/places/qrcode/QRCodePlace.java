package com.sap.sailing.gwt.home.desktop.places.qrcode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.google.gwt.user.client.Window;
import com.sap.sailing.domain.common.BranchIOConstants;
import com.sap.sailing.domain.common.racelog.tracking.DeviceMappingConstants;
import com.sap.sailing.gwt.common.client.AbstractBasePlace;
import com.sap.sse.common.Util.Pair;

/**
 * A place that can be used to display QR codes that direct users to branch.io using deep links which then point to an
 * app, together with configuration parameters that branch.io maps into the app runtime even through an installation
 * process. See
 * <a href="https://dashboard.branch.io/link-settings/general">https://dashboard.branch.io/link-settings/general</a>
 * for details. There, as the "Default URL", this QRCodePlace is configured, using URLs looking like this:<p>
 * 
 * <pre>
 *     https://my.sapsailing.com/gwt/Home.html#QRCodePlace:mode=COMPETITOR
 * </pre>
 * The URL fragment's {@code mode} parameter is used to configure the kind of link that is rendered in the QR code.
 *
 * @author Axel Uhl (D043530)
 *
 */
public class QRCodePlace extends AbstractBasePlace {

    public static final String PARAM_REGATTA_NAME = "regatta_name";
    public static final String PARAM_REGATTA_SECRET = "secret";
    private static final String PARAM_MODE = "mode";
    public static final String PARAM_SERVER = "server";

    private UUID eventId;
    private UUID competitorId;
    private UUID boatId;
    private UUID markId;
    private String leaderboardName;
    private String publicRegattaName;
    private String regattaRegistrationLinkSecret;
    private InvitationMode mode;
    private String rawCheckInUrl;
    private String targetServer;

    public enum InvitationMode {
        COMPETITOR_2,
        COMPETITOR,
        PUBLIC_INVITE,
        BOUY_TENDER
    }

    public QRCodePlace(String token) {
        super(token);
        try {
            mode = InvitationMode.valueOf(getParameter(PARAM_MODE));
            if (mode == InvitationMode.PUBLIC_INVITE) {
                // alternative direct link version
                targetServer = Window.Location.getParameter(PARAM_SERVER);
                publicRegattaName = Window.Location.getParameter(PARAM_REGATTA_NAME);
                regattaRegistrationLinkSecret = Window.Location.getParameter(PARAM_REGATTA_SECRET);
                if (publicRegattaName == null || regattaRegistrationLinkSecret == null || targetServer == null) {
                    GWT.log("Missing parameter for regatta, secret or server");
                }
            } else {
                rawCheckInUrl = Window.Location.getParameter(DeviceMappingConstants.URL_CHECKIN_URL);
                if (rawCheckInUrl != null) {
                    parseUrl(rawCheckInUrl);
                    if (leaderboardName == null) {
                        GWT.log("No parameter " + DeviceMappingConstants.URL_LEADERBOARD_NAME + " found!");
                    }
                    if (competitorId == null
                            && (mode == InvitationMode.COMPETITOR || mode == InvitationMode.COMPETITOR_2)) {
                        GWT.log("No parameter " + DeviceMappingConstants.URL_COMPETITOR_ID_AS_STRING + " found!");
                    }
                    if (boatId == null && (mode == InvitationMode.COMPETITOR || mode == InvitationMode.COMPETITOR_2)) {
                        GWT.log("No parameter " + DeviceMappingConstants.URL_BOAT_ID_AS_STRING + " found!");
                    }
                    if (markId == null && (mode == InvitationMode.COMPETITOR || mode == InvitationMode.COMPETITOR_2)) {
                        GWT.log("No parameter " + DeviceMappingConstants.URL_MARK_ID_AS_STRING + " found!");
                    }
                    if ((competitorId != null || boatId != null || markId != null)
                            && mode == InvitationMode.BOUY_TENDER) {
                        GWT.log("Found parameter " + DeviceMappingConstants.URL_COMPETITOR_ID_AS_STRING
                                + " will be ignored in bouy tender mode!");
                    }
                    if (eventId == null) {
                        GWT.log("No parameter " + DeviceMappingConstants.URL_EVENT_ID + " found!");
                    }
                } else {
                    GWT.log("No parameter " + DeviceMappingConstants.URL_CHECKIN_URL + " found!");
                }
            }
        } catch (Exception e) {
            GWT.log("No parameter " + PARAM_MODE + " found, or value not valid");
        }
    }

    private native String decodeUrl(String url)/*-{
        return decodeURIComponent(url)
    }-*/;

    private native String encodeUrl(String url)/*-{
        return encodeURIComponent(url)
    }-*/;

    private void parseUrl(String checkInUrl) {
        Iterable<Pair<String, String>> parameters = parseServerNameAndReturnUrlParameters(checkInUrl);
        for (Pair<String, String> parameter : parameters) {
            if (DeviceMappingConstants.URL_EVENT_ID.equals(parameter.getA())) {
                try {
                    eventId = UUID.fromString(parameter.getB());
                } catch (IllegalArgumentException e) {
                    GWT.log("Invalid " + DeviceMappingConstants.URL_EVENT_ID);
                    eventId = null;
                }
            } else if (DeviceMappingConstants.URL_COMPETITOR_ID_AS_STRING.equals(parameter.getA())) {
                try {
                    competitorId = UUID.fromString(parameter.getB());
                } catch (IllegalArgumentException e) {
                    GWT.log("Invalid " + DeviceMappingConstants.URL_COMPETITOR_ID_AS_STRING);
                    competitorId = null;
                }
            } else if (DeviceMappingConstants.URL_BOAT_ID_AS_STRING.equals(parameter.getA())) {
                try {
                    boatId = UUID.fromString(parameter.getB());
                } catch (IllegalArgumentException e) {
                    GWT.log("Invalid " + DeviceMappingConstants.URL_BOAT_ID_AS_STRING);
                    boatId = null;
                }
            } else if (DeviceMappingConstants.URL_MARK_ID_AS_STRING.equals(parameter.getA())) {
                try {
                    markId = UUID.fromString(parameter.getB());
                } catch (IllegalArgumentException e) {
                    GWT.log("Invalid " + DeviceMappingConstants.URL_MARK_ID_AS_STRING);
                    boatId = null;
                }
            } else if (DeviceMappingConstants.URL_LEADERBOARD_NAME.equals(parameter.getA())) {
                leaderboardName = parameter.getB();
            } else if (PARAM_REGATTA_SECRET.equals(parameter.getA())) {
                regattaRegistrationLinkSecret = parameter.getB();
            }
        }
    }

    private Iterable<Pair<String, String>> parseServerNameAndReturnUrlParameters(String checkInUrl) {
        String[] urlArguments = checkInUrl.split("\\?");
        String targetServerWithPath = urlArguments[0];
        targetServer = targetServerWithPath.replace("/tracking/checkin", "");
        targetServer = targetServer.replace("/buoy-tender/checkin", "");

        Collection<Pair<String, String>> pairs = new ArrayList<Pair<String, String>>();
        if (urlArguments.length < 2) {
            GWT.log("No parameters found!");
        } else {
            String[] urlParams = urlArguments[1].split("&");
            for (String urlParam : urlParams) {
                String[] param = urlParam.split("=");
                Pair<String, String> pair = new Pair<String, String>(param[0], decodeUrl(param[1].replace("+", "%20")));
                pairs.add(pair);
            }
        }
        return pairs;
    }

    public String getPublicInviteBranchIOUrl() {
        return BranchIOConstants.OPEN_REGATTA_2_APP_BRANCHIO + "?" + QRCodePlace.PARAM_REGATTA_NAME + "="
                + encodeUrl(publicRegattaName) + "&" + QRCodePlace.PARAM_REGATTA_SECRET + "="
                + encodeUrl(regattaRegistrationLinkSecret) + "&" + QRCodePlace.PARAM_SERVER + "="
                + encodeUrl(targetServer);
    }

    public InvitationMode getMode() {
        return mode;
    }

    public String getEncodedCheckInUrl() {
        return encodeUrl(rawCheckInUrl);
    }

    public UUID getEventId() {
        return eventId;
    }

    public UUID getCompetitorId() {
        return competitorId;
    }

    public UUID getBoatId() {
        return boatId;
    }

    public String getLeaderboardName() {
        return leaderboardName;
    }

    public String getPublicRegattaName() {
        return publicRegattaName;
    }

    public UUID getMarkId() {
        return markId;
    }

    public String getRegattaRegistrationLinkSecret() {
        return regattaRegistrationLinkSecret;
    }

    public String getTargetServer() {
        return targetServer;
    }

    @Override
    public String toString() {
        return "QRCodePlace [eventId=" + eventId + ", competitorId=" + competitorId + ", boatId=" + boatId + ", markId="
                + markId + ", leaderboardName=" + leaderboardName + ", publicRegattaName=" + publicRegattaName
                + ", regattaRegistrationLinkSecret=" + regattaRegistrationLinkSecret + ", mode=" + mode
                + ", rawCheckInUrl=" + rawCheckInUrl + ", targetServer=" + targetServer + "]";
    }

    public static class Tokenizer implements PlaceTokenizer<QRCodePlace> {
        @Override
        public String getToken(QRCodePlace place) {
            return place.getParametersAsToken();
        }

        @Override
        public QRCodePlace getPlace(String token) {
            return new QRCodePlace(token);
        }
    }
}
