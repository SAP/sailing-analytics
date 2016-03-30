package com.sap.sailing.dashboards.gwt.client.dataretriever;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.dashboards.gwt.client.DashboardClientFactory;
import com.sap.sailing.dashboards.gwt.client.actions.GetIDFromRaceThatTakesWindFixesNowAction;
import com.sap.sailing.dashboards.gwt.client.widgets.PollsLiveDataEvery5Seconds;
import com.sap.sailing.dashboards.gwt.shared.DashboardURLParameters;
import com.sap.sailing.dashboards.gwt.shared.dto.RaceIdDTO;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.gwt.ui.actions.GetWindInfoAction;
import com.sap.sailing.gwt.ui.shared.WindInfoForRaceDTO;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;

public class WindBotDataRetriever implements PollsLiveDataEvery5Seconds, WindBotDataRetrieverProvider {

    private AsyncActionsExecutor asyncActionsExecutor;
    private List<NumberOfWindBotsChangeListener> numberOfWindBotsChangeListeners;
    private List<WindBotDataRetrieverListener> windBotDataRetrieverListeners;
    private List<String> windBotIDsInLiveRace;

    private final Set<String> windSourceTypeNames;
    private final int WIND_CHART_RESOLUTION_IN_MILLISECONDS = 5000;
    private final int ONE_HOUR_IN_MILLISECONDS = 1000*60*60;
    private final String LODA_WIND_CHART_DATA_CATEGORY = "loadWindChartData";
    private boolean didInitialLoading;
    
    private DashboardClientFactory dashboardClientFactory;
    
    private static final Logger logger = Logger.getLogger(WindBotDataRetriever.class.getName());
    
    public WindBotDataRetriever(DashboardClientFactory dashboardClientFactory) {
        this.dashboardClientFactory = dashboardClientFactory;
        didInitialLoading = false;
        asyncActionsExecutor = new AsyncActionsExecutor();
        numberOfWindBotsChangeListeners = new ArrayList<NumberOfWindBotsChangeListener>();
        windBotDataRetrieverListeners = new ArrayList<WindBotDataRetrieverListener>(); 
        windBotIDsInLiveRace = new ArrayList<String>();
        windSourceTypeNames = new HashSet<>();
        windSourceTypeNames.add(WindSourceType.EXPEDITION.name());
        registerForDashboardFiveSecondsTimer(dashboardClientFactory);
    }

    private void loadWindBotData(Date from, Date to, RegattaAndRaceIdentifier selectedRaceIdentifier) {
        if (!didInitialLoading) {
            didInitialLoading = true;
            from = new Date(from.getTime()-ONE_HOUR_IN_MILLISECONDS);
        }
        logger.log(Level.INFO, "Executing GetWindInfoAction with from "+from+" and to "+to);
        GetWindInfoAction getWindInfoAction = new GetWindInfoAction(dashboardClientFactory.getSailingService(), selectedRaceIdentifier, from, to,
                WIND_CHART_RESOLUTION_IN_MILLISECONDS, windSourceTypeNames, /*
                                                              * onlyUpToNewestEvent==true because we don't want to
                                                              * overshoot the evidence so far
                                                              */true);
        asyncActionsExecutor.execute(getWindInfoAction, LODA_WIND_CHART_DATA_CATEGORY,
                new AsyncCallback<WindInfoForRaceDTO>() {
                    @Override
                    public void onSuccess(WindInfoForRaceDTO result) {
                        logger.log(Level.INFO, "Received WindInfoForRaceDTO");
                        if (result != null) {
                            int numberOfReceivedExpeditionSources = getWindBotIdsFrom(result).size();
                            if (numberOfReceivedExpeditionSources > 0) {
                                if (windBotIDsInLiveRace.size() != numberOfReceivedExpeditionSources) {
                                    windBotIDsInLiveRace = getWindBotIdsFrom(result);
                                    notifyListenersAboutNumberOfWindBotChange(windBotIDsInLiveRace);
                                    logger.log(Level.INFO, "Number of Windbots changed to "+numberOfReceivedExpeditionSources);
                                }
                                notifyWindBotDataRetrieverListeners(result);
                            } else {
                                logger.log(Level.INFO, "WindInfoForRaceDTO is not containing wind from windbots");
                            }
                        } else {
                            logger.log(Level.INFO, "WindInfoForRaceDTO is null");
                        }
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        logger.log(Level.INFO, "Failed to received WindInfoForRaceDTO, "+caught.getMessage());
                    }
                });
    }

    public List<String> getWindBotIdsFrom(WindInfoForRaceDTO windInfoForRaceDTO) {
        List<String> windBotIds = new ArrayList<String>();
        for (WindSource windSource : windInfoForRaceDTO.windTrackInfoByWindSource.keySet()) {
            if (windSource.getType().equals(WindSourceType.EXPEDITION) && windSource.getId() != null) {
                windBotIds.add(windSource.getId().toString());
            }
        }
        return windBotIds;
    }

    public void addNumberOfWindBotsChangeListeners(NumberOfWindBotsChangeListener numberOfWindBotsChangeListener) {
        numberOfWindBotsChangeListeners.add(numberOfWindBotsChangeListener);
    }

    public void removeNumberOfWindBotsChangeListeners(NumberOfWindBotsChangeListener numberOfWindBotsChangeListener) {
        numberOfWindBotsChangeListeners.remove(numberOfWindBotsChangeListener);
    }

    private void notifyListenersAboutNumberOfWindBotChange(List<String> windBotIDs) {
        for (NumberOfWindBotsChangeListener numberOfWindBotsChangeListener : numberOfWindBotsChangeListeners) {
            numberOfWindBotsChangeListener.numberOfWindBotsReceivedChanged(windBotIDs, this);
        }
    }
    
    @Override
    public void addWindBotDataRetrieverListener(WindBotDataRetrieverListener windBotDataRetrieverListener) {
        windBotDataRetrieverListeners.add(windBotDataRetrieverListener);
    }
    @Override
    public void removeWindBotDataRetrieverListener(WindBotDataRetrieverListener windBotDataRetrieverListener) {
        windBotDataRetrieverListeners.remove(windBotDataRetrieverListener);
    }
    
    @Override
    public void notifyWindBotDataRetrieverListeners(WindInfoForRaceDTO windInfoForRaceDTO) {
        logger.log(Level.INFO, "Notifing WindBotDataRetrieverListener about new WindInfoForRaceDTO");
        for (WindBotDataRetrieverListener windBotDataRetrieverListener : windBotDataRetrieverListeners) {
            logger.log(Level.INFO, "Notifying WindBotDataRetrieverListener");
            windBotDataRetrieverListener.updateWindBotUI(windInfoForRaceDTO);
        }
    }

    @Override
    public void timeChanged(Date newTime, Date oldTime) {
        final Date finalNewTime = newTime;
        final Date finaloldTime = oldTime;
        logger.log(Level.INFO, "Executing GetIDFromRaceThatTakesWindFixesNowAction");
        dashboardClientFactory.getDispatch().execute(
                new GetIDFromRaceThatTakesWindFixesNowAction(DashboardURLParameters.LEADERBOARD_NAME.getValue()),
                new AsyncCallback<RaceIdDTO>() {

                    @Override
                    public void onSuccess(RaceIdDTO result) {
                        if (result != null) {
                            RegattaAndRaceIdentifier raceId = result.getRaceId();
                            if (raceId != null) {
                                logger.log(Level.INFO, "Received RegattaAndRaceIdentifier from race that takes wind fixes now");
                                loadWindBotData(finaloldTime, finalNewTime, raceId);
                            } else {
                                logger.log(Level.INFO, "Can´t load wind data because RegattaAndRaceIdentifier for race which takes wind is null");
                            }
                        }
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        logger.log(Level.INFO, "Failed to received RegattaAndRaceIdentifier from race that takes wind fixes now");
                        logger.log(Level.INFO, caught.getMessage());
                    }
                });
    }
    
    @Override
    public void registerForDashboardFiveSecondsTimer(DashboardClientFactory dashboardClientFactory) {
        if (dashboardClientFactory != null) {
            dashboardClientFactory.getDashboardFiveSecondsTimer().addTimeListener(this);
        }
    }
}
