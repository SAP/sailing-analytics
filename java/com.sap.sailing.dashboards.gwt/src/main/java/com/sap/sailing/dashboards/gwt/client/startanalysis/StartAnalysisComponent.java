package com.sap.sailing.dashboards.gwt.client.startanalysis;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Cookies;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.HasWidgets;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.dashboards.gwt.client.DashboardClientFactory;
import com.sap.sailing.dashboards.gwt.client.PollsLiveDataEvery5Seconds;
import com.sap.sailing.dashboards.gwt.client.actions.GetCompetitorInLeaderboardAction;
import com.sap.sailing.dashboards.gwt.client.actions.GetStartAnalysesAction;
import com.sap.sailing.dashboards.gwt.client.notifications.bottom.BottomNotification;
import com.sap.sailing.dashboards.gwt.client.notifications.bottom.BottomNotificationClickListener;
import com.sap.sailing.dashboards.gwt.client.notifications.bottom.BottomNotificationType;
import com.sap.sailing.dashboards.gwt.client.popups.competitorselection.CompetitorSelectionListener;
import com.sap.sailing.dashboards.gwt.client.popups.competitorselection.CompetitorSelectionPopup;
import com.sap.sailing.dashboards.gwt.client.popups.competitorselection.SettingsButtonWithSelectionIndicationLabel;
import com.sap.sailing.dashboards.gwt.shared.DashboardURLParameters;
import com.sap.sailing.dashboards.gwt.shared.dto.LeaderboardCompetitorsDTO;
import com.sap.sailing.dashboards.gwt.shared.dto.StartAnalysesDTO;
import com.sap.sailing.dashboards.gwt.shared.dto.startanalysis.StartAnalysisDTO;
import com.sap.sailing.domain.common.dto.CompetitorDTO;

/**
 * The class contains an collection of {@link StartAnalysisCard}s that are displayed in horizontal aligned pages. It
 * shows only one {@link StartAnalysisCard} at a time and enables the user to switch between
 * {@link StartAnalysisCard}s with left and right buttons represented by {@link #left_focus_panel} and
 * {@link #right_focus_panel}. When a new {@link StartAnalysisCard} gets added to the component it shows a
 * {@link BottomNotification}.
 * 
 * @author Alexander Ries (D062114)
 *
 */
public class StartAnalysisComponent extends Composite implements HasWidgets, PollsLiveDataEvery5Seconds {

    private static StartAnalysisComponentUiBinder uiBinder = GWT.create(StartAnalysisComponentUiBinder.class);

    interface StartAnalysisComponentUiBinder extends UiBinder<Widget, StartAnalysisComponent> {
    }

    interface StartlineAnalysis extends CssResource {
    }

    @UiField
    HTMLPanel header;

    @UiField
    Image rightButton;

    @UiField
    Image leftButton;

    @UiField
    FocusPanel left_focus_panel;

    @UiField
    FocusPanel right_focus_panel;

    /**
     * Gets populated with new {@link StartAnalysisCard}s when the method
     * {@link #addStartAnalysisCard(StartAnalysisDTO)} gets called.
     * */
    @UiField
    HTMLPanel startanalysis_card_container;

    /**
     * The CSS "left" property from the {@link #startanalysis_card_container}.
     * */
    private double currentScrollPosition = 0;

    /**
     * The page or {@link StartAnalysisCard} the {@link #startanalysis_card_container} displays. Begins with 0.
     * */
    private int page = 0;

    /**
     * Amount of {@link StartAnalysisCard}s in {@link #startanalysis_card_container}.
     * */
    private int numberOfStartAnalysisCards = 0;

    private boolean displaysCards;
    private List<StartAnalysisDTO> starts;
    private List<StartAnalysisCard> pageChangeListener;
    private BottomNotification bottomNotification;

    private CompetitorSelectionPopup competitorSelectionPopup;
    private SettingsButtonWithSelectionIndicationLabel settingsButtonWithSelectionIndicationLabel;
    
    private DashboardClientFactory dashboardClientFactory;

    private static final String SELECTED_COMPETITOR_ID_COOKIE_KEY = "selectedCompetitorId";
    private static final int SELECTED_COMPETITOR_ID_COOKIE_KEY_EXPIRE_TIME_IN_MILLIS = 60 * 1000 * 60 * 5;
    private static final int SCROLL_OFFSET_STARTANALYSIS_CARDS = 83;
    private static final Logger logger = Logger.getLogger(StartAnalysisComponent.class.getName());

    /**
     * Component that contains handles, displays and loads startanalysis cards.
     * */
    public StartAnalysisComponent(DashboardClientFactory dashboardClientFactory) {
        StartAnalysisResources.INSTANCE.gss().ensureInjected();
        StartAnalysisResources.INSTANCE.combinedWindPanelStyle().ensureInjected();
        this.dashboardClientFactory = dashboardClientFactory;
        pageChangeListener = new ArrayList<StartAnalysisCard>();
        starts = new ArrayList<StartAnalysisDTO>();
        initWidget(uiBinder.createAndBindUi(this));
        initCompetitorSelectionPopupAndAddCompetitorSelectionListener();
        initLeftRightButtons();
        initAndAddBottomNotification();
        getCachedSelectedCompetitorOrAskForWithPopup();
        initAndAddSettingsButtonWithSelectionIndicationLabel();
        registerForDashboardFiveSecondsTimer(dashboardClientFactory);
    }

    private void initLeftRightButtons() {
        leftButton.setResource(StartAnalysisResources.INSTANCE.leftdisabled());
        left_focus_panel.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                clickedArrowButton(false);
            }
        });
        rightButton.setResource(StartAnalysisResources.INSTANCE.rightdisabled());
        right_focus_panel.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                clickedArrowButton(true);
            }
        });
    }

    private void loadStartAnalysisDTOsForCompetitorID(String competitorIdAsString){
        logger.log(Level.INFO, "Loading startanalysis for competitor id " + competitorIdAsString);
        dashboardClientFactory.getDispatch().execute(new GetStartAnalysesAction(DashboardURLParameters.LEADERBOARD_NAME.getValue(), competitorIdAsString), new AsyncCallback<StartAnalysesDTO>() {

            @Override
            public void onFailure(Throwable caught) {
                logger.log(Level.INFO, "Failed to received startanalysis list, "+caught.getMessage());
            }

            @Override
            public void onSuccess(StartAnalysesDTO result) {
              logger.log(Level.INFO, "Received startanalysis list");
              if (result != null && !result.getStartAnalyses().isEmpty()) {
                  if (displayedStartAnalysisCompetitorDifferentToRequestedOne()) {
                      removeAllStartAnalysisCards();
                  }
                  if (result.getStartAnalyses().size() != starts.size()) {
                      showNotificationForNewStartAnalysis();
                  }
                  logger.log(Level.INFO, "Updating UI with startanalysis list");
                  addNewStartAnalysisCards(result.getStartAnalyses());
                  settingsButtonWithSelectionIndicationLabel.setSelectionIndicationTextOnLabel(result.getStartAnalyses().get(0).competitor.getName());
              } else {
                  logger.log(Level.INFO, "Received startanalysis list is null or empty");
              }
            }
        });
    }

    private void initAndAddBottomNotification() {
        bottomNotification = new BottomNotification();
        bottomNotification.addBottomNotificationClickListener(new BottomNotificationClickListener() {

            @Override
            public void bottomNotificationClicked() {
                scrollToLast();
            }
        });
        RootPanel.get().add(bottomNotification);
    }

    private void initCompetitorSelectionPopupAndAddCompetitorSelectionListener() {
        competitorSelectionPopup = new CompetitorSelectionPopup();
        competitorSelectionPopup.addListener(new CompetitorSelectionListener() {

            @Override
            public void didClickOKWithSelectedCompetitor(CompetitorDTO competitor) {
                if (competitor != null) {
                    Cookies.removeCookie(SELECTED_COMPETITOR_ID_COOKIE_KEY);
                    Cookies.setCookie(SELECTED_COMPETITOR_ID_COOKIE_KEY, competitor.getIdAsString(), new Date(
                            new Date().getTime() + SELECTED_COMPETITOR_ID_COOKIE_KEY_EXPIRE_TIME_IN_MILLIS));
                    if (settingsButtonWithSelectionIndicationLabel != null) {
                        settingsButtonWithSelectionIndicationLabel.setSelectionIndicationTextOnLabel(competitor
                                .getName());
                    }
                    loadStartAnalysisDTOsForCompetitorID(competitor.getIdAsString());
                }
            }
        });
    }

    private void getCachedSelectedCompetitorOrAskForWithPopup() {
        String selectedCompetitorIdFromCookie = Cookies.getCookie(SELECTED_COMPETITOR_ID_COOKIE_KEY);
        if (selectedCompetitorIdFromCookie == null) {
            loadCompetitorsAndShowCompetitorSelectionPopup();
        }
    }

    private void loadCompetitorsAndShowCompetitorSelectionPopup() {
        logger.log(Level.INFO, "Requesting Competitors in Leaderboard");
        dashboardClientFactory.getDispatch().execute(new GetCompetitorInLeaderboardAction(DashboardURLParameters.LEADERBOARD_NAME.getValue()), new AsyncCallback<LeaderboardCompetitorsDTO>() {

                    @Override
                    public void onSuccess(LeaderboardCompetitorsDTO result) {
                        logger.log(Level.INFO, "Received competitors for leaderboard name");
                        if (result.getCompetitors() != null && result.getCompetitors().size() > 0) {
                            if (!competitorSelectionPopup.isShown()) {
                                logger.log(Level.INFO, "Showing CompetitorSelectionPopup with competitors");
                                competitorSelectionPopup.show(result.getCompetitors());
                            } else {
                                logger.log(Level.INFO, "CompetitorSelectionPopup aleady shown");
                            }
                        } else {
                            logger.log(Level.INFO, "Received competitors are null or empty");
                        }
                    }
                    
                    @Override
                    public void onFailure(Throwable caught) {
                        logger.log(Level.INFO, "Failed to received competitors for leaderboard name, "+caught.getMessage());
                    }
                });
    }

    private void initAndAddSettingsButtonWithSelectionIndicationLabel() {
        settingsButtonWithSelectionIndicationLabel = new SettingsButtonWithSelectionIndicationLabel();
        settingsButtonWithSelectionIndicationLabel.addClickHandlerToSettingsButton(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                loadCompetitorsAndShowCompetitorSelectionPopup();
            }
        });
        RootPanel.get().add(settingsButtonWithSelectionIndicationLabel);
    }

    /**
     * <param>clickedLeft</param> if clicked left is true, the left arrow button was pressed, a false represent the
     * right button. The buttons are represented by the member variables {@link #left_focus_panel} and
     * {@link #right_focus_panel}.
     * */
    private void clickedArrowButton(boolean clickedLeft) {
        if (displaysCards == true) {
            if (clickedLeft) {
                if (page != 0) {
                    currentScrollPosition += SCROLL_OFFSET_STARTANALYSIS_CARDS;
                    page--;
                    startanalysis_card_container.getElement().getStyle().setLeft(currentScrollPosition, Unit.PCT);
                    notifyStartAnalysisPageChangeListener(page);
                    int displayPage = page + 1;
                    header.getElement().setInnerHTML("Start " + starts.get(displayPage - 1).raceName);
                    if (page != numberOfStartAnalysisCards - 1) {
                        rightButton.setResource(StartAnalysisResources.INSTANCE.right());
                        rightButton.getElement().getStyle().setProperty("disabled", "false");
                    }
                    if (page == 0) {
                        leftButton.setResource(StartAnalysisResources.INSTANCE.leftdisabled());
                        leftButton.getElement().getStyle().setProperty("disabled", "true");
                    }
                }
            } else {
                if (page != numberOfStartAnalysisCards - 1) {
                    currentScrollPosition -= SCROLL_OFFSET_STARTANALYSIS_CARDS;
                    page++;
                    startanalysis_card_container.getElement().getStyle().setLeft(currentScrollPosition, Unit.PCT);
                    notifyStartAnalysisPageChangeListener(page);
                    int displayPage = page + 1;
                    header.getElement().setInnerHTML("Start " + starts.get(displayPage - 1).raceName);
                    if (page >= 0) {
                        leftButton.setResource(StartAnalysisResources.INSTANCE.left());
                        leftButton.getElement().getStyle().setProperty("disabled", "false");
                    }
                    if (page == numberOfStartAnalysisCards - 1) {
                        rightButton.setResource(StartAnalysisResources.INSTANCE.rightdisabled());
                        rightButton.getElement().getStyle().setProperty("disabled", "true");
                    }
                }
            }
        }
    }

    private void scrollToLast() {
        if (numberOfStartAnalysisCards > 1) {
            currentScrollPosition = currentScrollPosition - (numberOfStartAnalysisCards - page - 1)
                    * SCROLL_OFFSET_STARTANALYSIS_CARDS;
            page = numberOfStartAnalysisCards - 1;
            startanalysis_card_container.getElement().getStyle().setLeft(currentScrollPosition, Unit.PCT);
            header.getElement().setInnerHTML("Start " + starts.get(page).raceName);
            notifyStartAnalysisPageChangeListener(page);
            rightButton.setResource(StartAnalysisResources.INSTANCE.rightdisabled());
            rightButton.getElement().getStyle().setProperty("disabled", "true");
            leftButton.setResource(StartAnalysisResources.INSTANCE.left());
            leftButton.getElement().getStyle().setProperty("disabled", "false");
        }
    }

    private void addStartAnalysisCard(final StartAnalysisDTO startAnalysisDTO) {
        logger.log(Level.INFO, "Adding Startanalysis Card");
        if (displaysCards == false) {
            displaysCards = true;
            header.getElement().setInnerHTML("Start " + startAnalysisDTO.raceName);
        }
        if (numberOfStartAnalysisCards > 0) {
            rightButton.setResource(StartAnalysisResources.INSTANCE.right());
        }
        final StartAnalysisCard startlineAnalysisCard = new StartAnalysisCard(numberOfStartAnalysisCards
                * SCROLL_OFFSET_STARTANALYSIS_CARDS + 10, numberOfStartAnalysisCards, startAnalysisDTO,
                dashboardClientFactory.getSailingService());
        startanalysis_card_container.add(startlineAnalysisCard);
        startlineAnalysisCard.startAnalysisComponentPageChangedToIndexAndStartAnalysis(page, startAnalysisDTO);
        registerPageChangeListener(startlineAnalysisCard);
        numberOfStartAnalysisCards++;
    }

    private void removeAllStartAnalysisCards() {
        startanalysis_card_container.clear();
        numberOfStartAnalysisCards = 0;
        starts.clear();
    }

    private void showNotificationForNewStartAnalysis() {
        this.bottomNotification.show(BottomNotificationType.NEW_STARTANALYSIS_AVAILABLE);
    }

    public void registerPageChangeListener(StartAnalysisCard s) {
        pageChangeListener.add(s);
    }

    public void removeDataObserver(StartAnalysisPageChangeListener s) {
        pageChangeListener.remove(s);
    }

    public void notifyStartAnalysisPageChangeListener(int newPageIndex) {
        for (StartAnalysisPageChangeListener sO : pageChangeListener) {
            if(starts.get(newPageIndex) != null)
            sO.startAnalysisComponentPageChangedToIndexAndStartAnalysis(newPageIndex, starts.get(newPageIndex));
        }
    }

    private void addNewStartAnalysisCards(List<StartAnalysisDTO> startAnalysisDTOs) {
        for (int i = numberOfStartAnalysisCards; i <= startAnalysisDTOs.size() - 1; i++) {
            addStartAnalysisCard(startAnalysisDTOs.get(i));
            starts.add(startAnalysisDTOs.get(i));
        }
    }

    private String getCompetitorIdAsStringFromDisplayedStartAnalysisDTOs() {
        String result = "";
        if (starts != null && starts.size() > 0) {
            CompetitorDTO competitor = starts.get(0).competitor;
            if (competitor != null)
                result = starts.get(0).competitor.getIdAsString();
        }
        return result;
    }

    private boolean displayedStartAnalysisCompetitorDifferentToRequestedOne() {
        String competitorIDFromDispalyedStartAnalysisDTOs = getCompetitorIdAsStringFromDisplayedStartAnalysisDTOs();
        if (competitorIDFromDispalyedStartAnalysisDTOs != null
                && !(competitorIDFromDispalyedStartAnalysisDTOs.equals(Cookies
                        .getCookie(SELECTED_COMPETITOR_ID_COOKIE_KEY)))) {
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public void timeChanged(Date newTime, Date oldTime) {
        String selectedCompetitorId = Cookies.getCookie(SELECTED_COMPETITOR_ID_COOKIE_KEY);
        if (selectedCompetitorId != null) {
            loadStartAnalysisDTOsForCompetitorID(selectedCompetitorId);
        } else {
            loadCompetitorsAndShowCompetitorSelectionPopup();
        }
    }
    
    @Override
    public void registerForDashboardFiveSecondsTimer(DashboardClientFactory dashboardClientFactory) {
        if (dashboardClientFactory != null) {
            dashboardClientFactory.getDashboardFiveSecondsTimer().addTimeListener(this);
        }
    }

    @Override
    public void add(Widget w) {
        throw new UnsupportedOperationException("The method add(Widget w) is not supported.");
    }

    @Override
    public void clear() {
        throw new UnsupportedOperationException("The method clear() is not supported.");
    }

    @Override
    public Iterator<Widget> iterator() {
        return null;
    }

    @Override
    public boolean remove(Widget w) {
        return false;
    }
}
