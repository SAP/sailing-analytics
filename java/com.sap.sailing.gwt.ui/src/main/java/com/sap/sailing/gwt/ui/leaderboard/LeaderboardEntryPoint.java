package com.sap.sailing.gwt.ui.leaderboard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.LeaderboardType;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.domain.common.dto.AbstractLeaderboardDTO;
import com.sap.sailing.gwt.ui.actions.AsyncActionsExecutor;
import com.sap.sailing.gwt.ui.client.AbstractEntryPoint;
import com.sap.sailing.gwt.ui.client.DataEntryDialog;
import com.sap.sailing.gwt.ui.client.LogoAndTitlePanel;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.Timer;
import com.sap.sailing.gwt.ui.client.Timer.PlayModes;
import com.sap.sailing.gwt.ui.client.URLEncoder;
import com.sap.sailing.gwt.ui.leaderboard.LeaderboardSettings.RaceColumnSelectionStrategies;
import com.sap.sailing.gwt.ui.raceboard.GlobalNavigationPanel;


public class LeaderboardEntryPoint extends AbstractEntryPoint {
    private static final Logger logger = Logger.getLogger(LeaderboardEntryPoint.class.getName());
    private static final String PARAM_LEADERBOARD_GROUP_NAME = "leaderboardGroupName";
    private static final String PARAM_EMBEDDED = "embedded";
    private static final String PARAM_SHOW_RACE_DETAILS = "showRaceDetails";
    private static final String PARAM_RACE_NAME = "raceName";
    private static final String PARAM_RACE_DETAIL = "raceDetail";
    private static final String PARAM_OVERALL_DETAIL = "overallDetail";
    private static final String PARAM_LEG_DETAIL = "legDetail";
    private static final String PARAM_MANEUVER_DETAIL = "maneuverDetail";
    private static final String PARAM_AUTO_EXPAND_PRESELECTED_RACE = "autoExpandPreselectedRace";
    private static final String PARAM_AUTO_EXPAND_LAST_RACE_COLUMN = "autoExpandLastRaceColumn";
    private static final String PARAM_REGATTA_NAME = "regattaName";
    private static final String PARAM_REFRESH_INTERVAL_MILLIS = "refreshIntervalMillis";
    private static final String PARAM_SHOW_RANK_CHART = "showRankChart";

    
    /**
     * Parameter to support scaling the complete page by a given factor. This works by either using the
     * CSS3 zoom property or by applying scale operation to the body element. This comes in handy
     * when having to deal with screens that have high resolutions and that can't be controlled manually.
     * It is also a very simple method of adapting the viewport to a tv resolution. This parameter works
     * with value from 0.0 to 10.0 where 1.0 denotes the unchanged level (100%).
     */
    private static final String PARAM_ZOOM_TO = "zoomTo";
    
    /**
     * Lets the client choose a different race column selection which displays only up to the last N races with N being the integer
     * number specified by the parameter.
     */
    private static final String PARAM_NAME_LAST_N = "lastN";
    private String leaderboardName;
    private String leaderboardGroupName;
    private LeaderboardType leaderboardType;
    private GlobalNavigationPanel globalNavigationPanel;
    
    @Override
    protected void doOnModuleLoad() {
        super.doOnModuleLoad();
        final boolean showRaceDetails = Window.Location.getParameter(PARAM_SHOW_RACE_DETAILS) != null
                && Window.Location.getParameter(PARAM_SHOW_RACE_DETAILS).equalsIgnoreCase("true");
        final boolean embedded = Window.Location.getParameter(PARAM_EMBEDDED) != null
                && Window.Location.getParameter(PARAM_EMBEDDED).equalsIgnoreCase("true");

        leaderboardName = Window.Location.getParameter("name");
        leaderboardGroupName = Window.Location.getParameter(PARAM_LEADERBOARD_GROUP_NAME);

        if (leaderboardName != null) {
            sailingService.checkLeaderboardName(leaderboardName, new AsyncCallback<Pair<String, LeaderboardType>>() {
                @Override
                public void onSuccess(Pair<String, LeaderboardType> leaderboardNameAndType) {
                    if (leaderboardNameAndType != null && leaderboardName.equals(leaderboardNameAndType.getA())) {
                        leaderboardType = leaderboardNameAndType.getB();
                        createUI(showRaceDetails, embedded);
                    } else {
                        RootPanel.get().add(new Label(stringMessages.noSuchLeaderboard()));
                    }
                    
                    final String zoomTo = Window.Location.getParameter(PARAM_ZOOM_TO);
                    if (zoomTo != null) {
                        RootPanel.getBodyElement().setAttribute("style", "zoom: "+zoomTo+";-moz-transform: scale("+zoomTo+");-moz-transform-origin: 0 0;-o-transform: scale("+zoomTo+");-o-transform-origin: 0 0;-webkit-transform: scale("+zoomTo+");-webkit-transform-origin: 0 0;");
                    }
                }

                @Override
                public void onFailure(Throwable t) {
                    reportError("Error trying to obtain list of leaderboard names: " + t.getMessage());
                }
            });
        } else {
            RootPanel.get().add(new Label(stringMessages.noSuchLeaderboard()));
        }
        final String zoomTo = Window.Location.getParameter(PARAM_ZOOM_TO);
        if (zoomTo != null) {
            RootPanel.getBodyElement().setAttribute("style", "zoom: "+zoomTo+";-moz-transform: scale("+zoomTo+");-moz-transform-origin: 0 0;-o-transform: scale("+zoomTo+");-o-transform-origin: 0 0;-webkit-transform: scale("+zoomTo+");-webkit-transform-origin: 0 0;");
        }
    }
    
    private void createUI(boolean showRaceDetails, boolean embedded) {
        DockLayoutPanel mainPanel = new DockLayoutPanel(Unit.PX);
        RootLayoutPanel.get().add(mainPanel);
        LogoAndTitlePanel logoAndTitlePanel = null;
        if (!embedded) {
            // Hack to shorten the leaderboardName in case of overall leaderboards
            String leaderboardDisplayName = Window.Location.getParameter("displayName");
            if (leaderboardDisplayName == null || leaderboardDisplayName.isEmpty()) {
                leaderboardDisplayName = leaderboardName;
            }
            globalNavigationPanel = new GlobalNavigationPanel(stringMessages, true, null, leaderboardGroupName);
            logoAndTitlePanel = new LogoAndTitlePanel(leaderboardGroupName, leaderboardDisplayName, stringMessages, this) {
                @Override
                public void onResize() {
                    super.onResize();
                    if (isSmallWidth()) {
                        remove(globalNavigationPanel);
                    } else {
                        add(globalNavigationPanel);
                    }
                }
            };
            logoAndTitlePanel.addStyleName("LogoAndTitlePanel");
            if (!isSmallWidth()) {
                logoAndTitlePanel.add(globalNavigationPanel);
            }
            mainPanel.addNorth(logoAndTitlePanel, 68);
        }
        ScrollPanel contentScrollPanel = new ScrollPanel();
        long delayBetweenAutoAdvancesInMilliseconds = 3000l;
        final RaceIdentifier preselectedRace = getPreselectedRace(Window.Location.getParameterMap());
        Timer timer = new Timer(PlayModes.Replay, delayBetweenAutoAdvancesInMilliseconds);
        final LeaderboardSettings leaderboardSettings = createLeaderboardSettingsFromURLParameters(Window.Location.getParameterMap());
        if (leaderboardSettings.getDelayBetweenAutoAdvancesInMilliseconds() != null) {
            timer.setPlayMode(PlayModes.Live); // the leaderboard, viewed via the entry point, always goes "live"
        }
        boolean autoExpandLastRaceColumn = Window.Location.getParameterMap().containsKey(
                PARAM_AUTO_EXPAND_LAST_RACE_COLUMN) ? Boolean.valueOf(Window.Location.getParameterMap()
                .get(PARAM_AUTO_EXPAND_LAST_RACE_COLUMN).get(0)) : false;
        boolean showRankChart = Window.Location.getParameterMap().containsKey(
                PARAM_SHOW_RANK_CHART) ? Boolean.valueOf(Window.Location.getParameterMap()
                .get(PARAM_SHOW_RANK_CHART).get(0)) : false;  
                
        Widget leaderboardViewer;
        if(leaderboardType.isMetaLeaderboard()) {
            leaderboardViewer = new MetaLeaderboardViewer(sailingService, new AsyncActionsExecutor(),
                    leaderboardSettings, null, preselectedRace, leaderboardGroupName, leaderboardName, this, stringMessages, userAgent,
                    showRaceDetails, autoExpandLastRaceColumn, showRankChart);
        } else {
            leaderboardViewer = new LeaderboardViewer(sailingService, new AsyncActionsExecutor(),
                    leaderboardSettings, preselectedRace, leaderboardGroupName, leaderboardName, this, stringMessages, userAgent,
                    showRaceDetails, autoExpandLastRaceColumn, showRankChart);
        }
        
         contentScrollPanel.setWidget(leaderboardViewer);
         mainPanel.add(contentScrollPanel);
    }
   
    private RaceIdentifier getPreselectedRace(Map<String, List<String>> parameterMap) {
        RaceIdentifier result;
        if (parameterMap.containsKey(PARAM_RACE_NAME) && parameterMap.get(PARAM_RACE_NAME).size() == 1 &&
                parameterMap.containsKey(PARAM_REGATTA_NAME) && parameterMap.get(PARAM_REGATTA_NAME).size() == 1) {
            result = new RegattaNameAndRaceName(parameterMap.get(PARAM_REGATTA_NAME).get(0), parameterMap.get(PARAM_RACE_NAME).get(0));
        } else {
            result = null;
        }
        return result;
    }

    /**
     * Constructs {@link LeaderboardSettings} from the URL parameters found
     */
    private LeaderboardSettings createLeaderboardSettingsFromURLParameters(Map<String, List<String>> parameterMap) {
        LeaderboardSettings result;
        Long refreshIntervalMillis = parameterMap.containsKey(PARAM_REFRESH_INTERVAL_MILLIS) ? Long
                .valueOf(parameterMap.get(PARAM_REFRESH_INTERVAL_MILLIS).get(0)) : null;
        RaceColumnSelectionStrategies raceColumnSelectionStrategy;
        if (parameterMap.containsKey(PARAM_NAME_LAST_N)) {
            raceColumnSelectionStrategy = RaceColumnSelectionStrategies.LAST_N;
        } else {
            raceColumnSelectionStrategy = RaceColumnSelectionStrategies.EXPLICIT;
        }
        final Integer numberOfLastRacesToShow;
        if (parameterMap.containsKey(PARAM_NAME_LAST_N)) {
            numberOfLastRacesToShow = Integer.valueOf(parameterMap.get(PARAM_NAME_LAST_N).get(0));
        } else {
            numberOfLastRacesToShow = null;
        }
        if (parameterMap.containsKey(PARAM_RACE_NAME) || parameterMap.containsKey(PARAM_RACE_DETAIL) ||
                parameterMap.containsKey(PARAM_LEG_DETAIL) || parameterMap.containsKey(PARAM_MANEUVER_DETAIL) ||
                parameterMap.containsKey(PARAM_OVERALL_DETAIL)) {
            List<DetailType> maneuverDetails = getDetailTypeListFromParamValue(parameterMap.get(PARAM_MANEUVER_DETAIL));
            List<DetailType> raceDetails = getDetailTypeListFromParamValue(parameterMap.get(PARAM_RACE_DETAIL));
            List<DetailType> overallDetails = getDetailTypeListFromParamValue(parameterMap.get(PARAM_OVERALL_DETAIL));
            List<DetailType> legDetails = getDetailTypeListFromParamValue(parameterMap.get(PARAM_LEG_DETAIL));
            List<String> namesOfRacesToShow = getStringListFromParamValue(parameterMap.get(PARAM_RACE_NAME));
            boolean autoExpandPreSelectedRace = parameterMap.containsKey(PARAM_AUTO_EXPAND_PRESELECTED_RACE) ?
                    Boolean.valueOf(parameterMap.get(PARAM_AUTO_EXPAND_PRESELECTED_RACE).get(0)) :
                        (namesOfRacesToShow != null && namesOfRacesToShow.size() == 1);
            result = new LeaderboardSettings(maneuverDetails, legDetails, raceDetails, overallDetails,
                    /* namesOfRaceColumnsToShow */ null,
                    namesOfRacesToShow, numberOfLastRacesToShow,
                    autoExpandPreSelectedRace, refreshIntervalMillis, /* sort by column */ (namesOfRacesToShow != null && !namesOfRacesToShow.isEmpty()) ?
                                    namesOfRacesToShow.get(0) : null,
                            /* ascending */ true, /* updateUponPlayStateChange */ raceDetails.isEmpty() && legDetails.isEmpty(),
                                    raceColumnSelectionStrategy);

        } else {
            final List<DetailType> overallDetails = Collections.emptyList();
            result = LeaderboardSettingsFactory.getInstance().createNewDefaultSettings(null, null,
                    /* overallDetails */ overallDetails, null,
                    /* autoExpandFirstRace */false, refreshIntervalMillis, numberOfLastRacesToShow,
                    raceColumnSelectionStrategy);
        }
        return result;
    }

    private List<DetailType> getDetailTypeListFromParamValue(List<String> list) {
        List<DetailType> result = new ArrayList<DetailType>();
        if (list != null) {
            for (String entry : list) {
                try {
                    result.add(DetailType.valueOf(entry));
                } catch (IllegalArgumentException e) {
                    logger.info("Can't find detail type "+entry+". Ignoring.");
                }
            }
        }
        return result;
    }

    private List<String> getStringListFromParamValue(List<String> list) {
        List<String> result = new ArrayList<String>();
        if (list != null) {
            result.addAll(list);
        }
        return result;
    }
    
    public static class LeaderboardUrlSettings {
        private final LeaderboardSettings leaderboardSettings;
        private final boolean embedded;
        private final boolean showRaceDetails;
        private final boolean autoExpandLastRaceColumn;
        private final boolean autoRefresh;
        
        public LeaderboardUrlSettings(LeaderboardSettings leaderboardSettings, boolean embedded,
                boolean showRaceDetails, boolean autoRefresh, boolean autoExpandLastRaceColumn) {
            super();
            this.leaderboardSettings = leaderboardSettings;
            this.embedded = embedded;
            this.showRaceDetails = showRaceDetails;
            this.autoRefresh = autoRefresh;
            this.autoExpandLastRaceColumn = autoExpandLastRaceColumn;
        }

        public LeaderboardSettings getLeaderboardSettings() {
            return leaderboardSettings;
        }

        public boolean isEmbedded() {
            return embedded;
        }

        public boolean isShowRaceDetails() {
            return showRaceDetails;
        }

        public boolean isAutoRefresh() {
            return autoRefresh;
        }

        public boolean isAutoExpandLastRaceColumn() {
            return autoExpandLastRaceColumn;
        }
    }
    
    /**
     * Assembles a dialog that other parts of the application can use to let the user parameterize a leaderboard and
     * obtain the according URL for it. This keeps the "secrets" of which URL parameters have which meaning encapsulated
     * within this class.<p>
     * 
     * The implementation by and large uses the {@link LeaderboardSettingsDialogComponent}'s widget and adds to it a checkbox
     * for driving the {@link #PARAM_EMBEDDED} field.
     * 
     * @see LeaderboardEntryPoint#getUrl(String, LeaderboardSettings, boolean)
     */
    public static DataEntryDialog<LeaderboardUrlSettings> getUrlConfigurationDialog(final AbstractLeaderboardDTO leaderboard,
            final StringMessages stringMessages) {
        return new LeaderboardUrlConfigurationDialog(stringMessages, leaderboard);
    }

    /**
     * Assembles a URL for a leaderboard that displays with the <code>settings</code> and <code>embedded</code> mode
     * as specified by the parameters.
     */
    public static String getUrl(String leaderboardName, String leaderboardDisplayName, LeaderboardUrlSettings settings) {
        StringBuilder overallDetails = new StringBuilder();
        for (DetailType overallDetail : settings.getLeaderboardSettings().getOverallDetailsToShow()) {
            overallDetails.append('&');
            overallDetails.append(PARAM_OVERALL_DETAIL);
            overallDetails.append('=');
            overallDetails.append(overallDetail.name());
        }
        StringBuilder legDetails = new StringBuilder();
        for (DetailType legDetail : settings.getLeaderboardSettings().getLegDetailsToShow()) {
            legDetails.append('&');
            legDetails.append(PARAM_LEG_DETAIL);
            legDetails.append('=');
            legDetails.append(legDetail.name());
        }
        StringBuilder raceDetails = new StringBuilder();
        for (DetailType raceDetail : settings.getLeaderboardSettings().getRaceDetailsToShow()) {
            raceDetails.append('&');
            raceDetails.append(PARAM_RACE_DETAIL);
            raceDetails.append('=');
            raceDetails.append(raceDetail.name());
        }
        StringBuilder maneuverDetails = new StringBuilder();
        for (DetailType maneuverDetail : settings.getLeaderboardSettings().getManeuverDetailsToShow()) {
            maneuverDetails.append('&');
            maneuverDetails.append(PARAM_MANEUVER_DETAIL);
            maneuverDetails.append('=');
            maneuverDetails.append(maneuverDetail.name());
        }
        String debugParam = Window.Location.getParameter("gwt.codesvr");
        String link = URLEncoder.encode("/gwt/Leaderboard.html?name=" + leaderboardName
                + (settings.isShowRaceDetails() ? "&"+PARAM_SHOW_RACE_DETAILS+"=true" : "")
                + (leaderboardDisplayName != null ? "&displayName="+leaderboardDisplayName : "")
                + (settings.isEmbedded() ? "&"+PARAM_EMBEDDED+"=true" : "")

                + (!settings.isAutoRefresh() || (settings.getLeaderboardSettings().getDelayBetweenAutoAdvancesInMilliseconds() == null &&
                   settings.getLeaderboardSettings().getDelayBetweenAutoAdvancesInMilliseconds() != 0) ? "" :
                    "&"+PARAM_REFRESH_INTERVAL_MILLIS+"="+settings.getLeaderboardSettings().getDelayBetweenAutoAdvancesInMilliseconds())
                + legDetails.toString()
                + raceDetails.toString()
                + overallDetails.toString()
                + maneuverDetails.toString()
                + (settings.isAutoExpandLastRaceColumn() ? "&"+PARAM_AUTO_EXPAND_LAST_RACE_COLUMN+"=true" : "")
                + (settings.getLeaderboardSettings().getNumberOfLastRacesToShow() == null ? "" :
                    "&"+PARAM_NAME_LAST_N+"="+settings.getLeaderboardSettings().getNumberOfLastRacesToShow())
                + (debugParam != null && !debugParam.isEmpty() ? "&gwt.codesvr=" + debugParam : ""));
        return link;
    }
}
