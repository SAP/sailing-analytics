package com.sap.sailing.gwt.ui.adminconsole;

import java.util.Collection;
import java.util.HashSet;
import java.util.function.Consumer;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.security.ui.client.UserService;


public abstract class RegattaLeaderboardWithEliminationsDialog extends AbstractLeaderboardDialog<LeaderboardDescriptorWithEliminations> {
    protected final ListBox regattaLeaderboardsListBox;
    protected final Collection<RegattaDTO> existingRegattas;
    protected final FlowPanel competitorEliminationPanelHolder;
    protected final SailingServiceWriteAsync sailingServiceWrite;
    protected final UserService userService;
    private final Collection<StrippedLeaderboardDTO> existingLeaderboards;
    protected final ErrorReporter errorReporter;
    protected final StringMessages stringMessages;
    
    /**
     * {@link null} until the {@link #getEliminatedCompetitorsRetriever()} method has been called for
     * the first time. Then, a local cache of the competitors to eliminate is created and assigned to
     * this field. For an existing leaderboard with eliminations it is initially filled from the
     * server's copy of that leaderboard. For new leaderboards it starts out empty.
     */
    protected Collection<CompetitorDTO> eliminatedCompetitors;

    protected static class LeaderboardParameterValidator implements Validator<LeaderboardDescriptorWithEliminations> {
        protected final StringMessages stringMessages;
        protected final Collection<StrippedLeaderboardDTO> existingLeaderboards;

        public LeaderboardParameterValidator(StringMessages stringMessages, Collection<StrippedLeaderboardDTO> existingLeaderboards) {
            super();
            this.stringMessages = stringMessages;
            this.existingLeaderboards = existingLeaderboards;
        }

        @Override
        public String getErrorMessage(LeaderboardDescriptorWithEliminations leaderboardToValidate) {
            String errorMessage;
            boolean unique = true;
            for (StrippedLeaderboardDTO dao : existingLeaderboards) {
                if (dao.getName().equals(leaderboardToValidate.getName())) {
                    unique = false;
                }
            }
            boolean regattaSelected = leaderboardToValidate.getRegattaName() != null ? true : false;
            if (!regattaSelected) {
                errorMessage = stringMessages.pleaseSelectARegatta();
            } else if (!unique) {
                errorMessage = stringMessages.leaderboardWithThisNameAlreadyExists();
            } else {
                String discardThresholdErrorMessage = DiscardThresholdBoxes.getErrorMessage(
                        leaderboardToValidate.getDiscardThresholds(), stringMessages);
                if (discardThresholdErrorMessage != null) {
                    errorMessage = discardThresholdErrorMessage;
                } else {
                    errorMessage = null;
                }
            }
            return errorMessage;
        }
    }

    public RegattaLeaderboardWithEliminationsDialog(SailingServiceWriteAsync sailingServiceWrite, UserService userService, String title,
            LeaderboardDescriptorWithEliminations leaderboardDTO, Collection<RegattaDTO> existingRegattas,
            final Collection<StrippedLeaderboardDTO> existingLeaderboards, final StringMessages stringMessages,
            final ErrorReporter errorReporter, LeaderboardParameterValidator validator,
            DialogCallback<LeaderboardDescriptorWithEliminations> callback) {
        super(title, leaderboardDTO, stringMessages, validator, callback);
        this.sailingServiceWrite = sailingServiceWrite;
        this.userService = userService;
        this.stringMessages = stringMessages;
        this.errorReporter = errorReporter;
        this.existingRegattas = existingRegattas;
        this.existingLeaderboards = existingLeaderboards;
        displayNameTextBox = createTextBox(null);
        displayNameTextBox.ensureDebugId("DisplayNameTextBox");
        displayNameTextBox.setVisibleLength(50);
        this.competitorEliminationPanelHolder = new FlowPanel();
        regattaLeaderboardsListBox = createSortedRegattaLeaderboardsListBox(existingLeaderboards, null, stringMessages, this);
        regattaLeaderboardsListBox.ensureDebugId("RegattaListBox");
        regattaLeaderboardsListBox.addChangeHandler(new ChangeHandler() {
            public void onChange(ChangeEvent event) {
                int selectedIndex = regattaLeaderboardsListBox.getSelectedIndex();
                if (selectedIndex > 0) {
                    if (nameTextBox.getText().isEmpty()) { // don't overwrite a value entered by the user
                        nameTextBox.setText(regattaLeaderboardsListBox.getValue(selectedIndex));
                    }
                    validateAndUpdate();
                    createAndFillCompetitorEliminationPanel();
                } else {
                    competitorEliminationPanelHolder.clear();
                }
            }
        });
    }
    
    /**
     * A valid regatta leaderboard has been selected; show a competitor dialog that allows the user to compile
     * a set of competitors to eliminate from the wrapping leaderboard
     */
    protected void createAndFillCompetitorEliminationPanel() {
        competitorEliminationPanelHolder.clear();
        StrippedLeaderboardDTO selectedRegattaLeaderboard = getSelectedLeaderboard();
        final CompetitorRegistrationsPanel[] competitorEliminationPanel = new CompetitorRegistrationsPanel[1];
        competitorEliminationPanel[0] = new CompetitorRegistrationsPanel(sailingServiceWrite, userService,
                /* competitorsRefresher not required; competitor set is limited to those in leaderboard */ null,
                /* boatsRefresher not needed */ null, stringMessages, errorReporter, /* editable */ true,
                regattaLeaderboardsListBox.getValue(regattaLeaderboardsListBox.getSelectedIndex()),
                selectedRegattaLeaderboard.canBoatsOfCompetitorsChangePerRace, selectedRegattaLeaderboard.boatClassName,
                /* "validator" updates eliminatedCompetitors */ () -> eliminatedCompetitors = competitorEliminationPanel[0]
                        .getResult(),
                getEliminatedCompetitorsRetriever(), /* restrictPoolToLeaderboard */ true,
                /* additionalWidgetsBeforeTables */ new Label(stringMessages.selectCompetitorsToEliminate()));
        competitorEliminationPanelHolder.add(competitorEliminationPanel[0]);
    }

    /**
     * Implementations are expected to return a function that takes a callback and makes an effort to obtain the
     * competitors eliminated in the leaderboard currently being created / edited. For the creation of a new leaderboard
     * with eliminations, the respective state will solely exist here, in the client, and no call to the server is
     * attempted. When editing an existing leaderboard with eliminations, a first call to the server is issued if
     * this hasn't happened before, and the result is cached locally. In the {@link #getResult()} implementation the
     * eliminations are then taken from the local state.
     */
    protected abstract Consumer<Pair<CompetitorRegistrationsPanel, AsyncCallback<Collection<CompetitorDTO>>>> getEliminatedCompetitorsRetriever();

    /**
     * Based on the contents of {@link #regattaLeaderboardsListBox} obtains the leaderboard from
     * {@code existingLeaderboards} whose name matches with the selected list item.
     */
    protected StrippedLeaderboardDTO getSelectedLeaderboard() {
        final String selectedRegattaLeaderboardName = regattaLeaderboardsListBox.getSelectedValue();
        for (final StrippedLeaderboardDTO l : existingLeaderboards) {
            if (l.getName().equals(selectedRegattaLeaderboardName)) {
                return l;
            }
        }
        return null;
    }

    @Override
    protected LeaderboardDescriptorWithEliminations getResult() {
        LeaderboardDescriptor leaderboard = super.getResult();
        leaderboard.setRegattaName(getNameOfSelectedRegattaLeaderboard());
        LeaderboardDescriptorWithEliminations result = new LeaderboardDescriptorWithEliminations(leaderboard,
                eliminatedCompetitors == null ? null : new HashSet<>(eliminatedCompetitors));
        return result;
    }

    @Override
    protected Widget getAdditionalWidget() {
        VerticalPanel mainPanel = new VerticalPanel();
        Grid formGrid = new Grid(4, 3);
        formGrid.setCellSpacing(3);
        formGrid.setWidget(0, 0, createLabel(stringMessages.regattaLeaderboards()));
        formGrid.setWidget(0, 1, regattaLeaderboardsListBox);
        formGrid.setWidget(1,  0, createLabel(stringMessages.name()));
        formGrid.setWidget(1, 1, nameTextBox);
        formGrid.setWidget(2,  0, createLabel(stringMessages.displayName()));
        formGrid.setWidget(2, 1, displayNameTextBox);
        formGrid.setWidget(3, 1, competitorEliminationPanelHolder);
        mainPanel.add(formGrid);
        return mainPanel;
    }

    public String getNameOfSelectedRegattaLeaderboard() {
        final String result;
        int selIndex = regattaLeaderboardsListBox.getSelectedIndex();
        if (selIndex > 0) { // the zero index represents the 'no selection' text
            result = regattaLeaderboardsListBox.getValue(selIndex);
        } else {
            result = null;
        }
        return result;
    }

}
