package com.sap.sailing.gwt.ui.raceboard.tagging;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HeaderPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.SingleSelectionModel;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.domain.common.dto.TagDTO;
import com.sap.sailing.domain.common.security.Permission;
import com.sap.sailing.domain.common.security.Permission.Mode;
import com.sap.sailing.domain.common.security.SailingPermissionsForRoleProvider;
import com.sap.sailing.gwt.ui.client.RaceTimesInfoProvider;
import com.sap.sailing.gwt.ui.client.RaceTimesInfoProviderListener;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.raceboard.tagging.TagPanelResources.TagPanelStyle;
import com.sap.sailing.gwt.ui.shared.RaceTimesInfoDTO;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.player.TimeListener;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.shared.components.Component;
import com.sap.sse.gwt.client.shared.components.ComponentWithoutSettings;
import com.sap.sse.gwt.client.shared.settings.ComponentContext;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.UserStatusEventHandler;
import com.sap.sse.security.ui.shared.SuccessInfo;
import com.sap.sse.security.ui.shared.UserDTO;

/**
 * A view showing tags which are connected to a specific race and allowing users to add own tags to a race. This view is
 * shown at the {@link com.sap.sailing.gwt.ui.raceboard.RaceBoardPanel RaceBoard}. Tags consist of a heading and
 * optional a comment and/or image. Tag-Buttons allow to preset tags which are used more frequently by an user. Public
 * tags will be stored as an {@link com.sap.sailing.domain.abstractlog.race.RaceLogEvent RaceLogEvent}, private tags
 * will be stored in the {@link com.sap.sse.security.UserStore UserStore}.<br/>
 * <br/>
 * The TaggingPanel is also used as a data provider for all of its subcomponents like header, footer and content
 * section. Therefore the TaggingPanel provides references to important services, string messages, its current state and
 * so on.
 * Best practice: The constructor of subcomponents of the TaggingPanel contains only the TaggingPanel as a parameter.
 * Every other required shared resource (string messages, service references, ...) can be requested from the
 * TaggingPanel itself.
 */
public class TaggingPanel extends ComponentWithoutSettings
        implements RaceTimesInfoProviderListener, UserStatusEventHandler, TimeListener {

    /**
     * Describes the {@link TaggingPanel#currentState current state} of the {@link TaggingPanel}.
     */
    protected enum State {
        VIEW, // default
        CREATE_TAG,
        EDIT_TAG
    }

    // styling
    private final TagPanelStyle style;

    // required to display tags
    private final CellList<TagDTO> tagCellList;
    private final SingleSelectionModel<TagDTO> tagSelectionModel;
    private final TagListProvider tagListProvider;

    // custom tag buttons of current user
    private final List<TagButton> tagButtons;

    // UI elements
    private final HeaderPanel taggingPanel;
    private final TagFilterPanel filterbarPanel;
    private final Panel contentPanel;
    private final TagFooterPanel footerPanel;
    private final Button createTagsButton;

    // misc. elements
    private final StringMessages stringMessages;
    private final SailingServiceAsync sailingService;
    private final UserService userService;
    private final Timer timer;
    private final RaceTimesInfoProvider raceTimesInfoProvider;

    // race log identifiers
    private String leaderboardName = null;
    private RaceColumnDTO raceColumn = null;
    private FleetDTO fleet = null;

    // current state of the Tagging-Panel
    private State currentState;
    
    //Needed for sharing Tags 
    private boolean firstTimePublicTagsLoaded = true;
    private final TimePoint sharedTimePoint;
    private final String sharedTag;

    public TaggingPanel(Component<?> parent, ComponentContext<?> context, StringMessages stringMessages,
            SailingServiceAsync sailingService, UserService userService, Timer timer,
            RaceTimesInfoProvider raceTimesInfoProvider) {
        super(parent, context);

        this.stringMessages = stringMessages;
        this.sailingService = sailingService;
        this.userService = userService;
        this.timer = timer;
        this.raceTimesInfoProvider = raceTimesInfoProvider;

        style = TagPanelResources.INSTANCE.style();
        style.ensureInjected();
        TagCellListResources.INSTANCE.cellListStyle().ensureInjected();

        tagCellList = new CellList<TagDTO>(new TagCell(this, false), TagCellListResources.INSTANCE);
        tagSelectionModel = new SingleSelectionModel<TagDTO>();
        tagListProvider = new TagListProvider();

        tagButtons = new ArrayList<TagButton>();

        taggingPanel = new HeaderPanel();
        footerPanel = new TagFooterPanel(this);
        filterbarPanel = new TagFilterPanel(this);
        contentPanel = new FlowPanel();
        createTagsButton = new Button();

        userService.addUserStatusEventHandler(this);
        raceTimesInfoProvider.addRaceTimesInfoProviderListener(this);

        setCurrentState(State.VIEW);
        
        //Get the url parameter "tag" and divide it into the logical timepoint and the tag title
        String urlParameter = Window.Location.getParameter("tag");
        if(urlParameter != null) {
            if(urlParameter.length() > 13) {
                String timeMillisString = urlParameter.substring(0, 13);//Works till "Nov 20 2286", afterwards timeMillis length increases from 13 to 14 chars
                String tagString = urlParameter.substring(13, urlParameter.length()); 
                sharedTimePoint = new MillisecondsTimePoint(Long.parseLong(timeMillisString));
                sharedTag = tagString; 
            }
            else {
                Notification.notify(stringMessages.tagInvalidURL(), NotificationType.WARNING);
                sharedTimePoint = null;
                sharedTag = null; 
            }  
        }
        else {
            sharedTimePoint = null;
            sharedTag = null; 
        }

        initializePanel();
    }

    /**
     * Initializes UI of {@link TaggingPanel}.
     */
    private void initializePanel() {
        taggingPanel.setStyleName(style.taggingPanel());

        // header
        taggingPanel.setHeaderWidget(filterbarPanel);

        // footer
        taggingPanel.setFooterWidget(footerPanel);

        // content (tags)
        contentPanel.addStyleName(style.tagCellListPanel());
        contentPanel.add(tagCellList);
        contentPanel.add(createTagsButton);

        tagListProvider.addDataDisplay(tagCellList);
        tagCellList.setEmptyListWidget(new Label(stringMessages.tagNoTagsFound()));
        tagCellList.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
        tagCellList.setSelectionModel(tagSelectionModel);
        tagSelectionModel.addSelectionChangeHandler(event -> {
            // set time slider to corresponding position
            TagDTO selectedTag = tagSelectionModel.getSelectedObject();
            if (selectedTag != null) {
                // remove time change listener when manual selecting tag cells as this could end in an infinite loop of
                // timer change -> automatic selection change -> timer change -> ...
                timer.removeTimeListener(this);
                timer.setTime(selectedTag.getRaceTimepoint().asMillis());
                // adding time change listener again
                timer.addTimeListener(this);
            }
        });

        createTagsButton.setTitle(stringMessages.tagAddTags());
        createTagsButton.setStyleName(style.toggleEditState());
        createTagsButton.addStyleName(style.imagePusTransparent());
        createTagsButton.addClickHandler(event -> {
            setCurrentState(State.CREATE_TAG);
        });

        taggingPanel.setContentWidget(contentPanel);
        updateContent();
    }

    /**
     * Updates parameters required to save/revoke {@link com.sap.sailing.domain.abstractlog.race.RaceLogEvent events}
     * to/from {@link com.sap.sailing.domain.abstractlog.race.RaceLog RaceLog}.
     */
    public void updateRace(String leaderboardName, RaceColumnDTO raceColumn, FleetDTO fleet) {
        if (leaderboardName != null && !leaderboardName.equals(this.leaderboardName)) {
            this.leaderboardName = leaderboardName;
        }
        if (fleet != null && !fleet.equals(this.fleet)) {
            this.fleet = fleet;
        }
        if (raceColumn != null && !raceColumn.equals(this.raceColumn)) {
            this.raceColumn = raceColumn;
        }
        reloadPrivateTags();
    }

    /**
     * Checks if {@link UserService#getCurrentUser() current user} is non-<code>null</code> and if
     * {@link #leaderboardName}, {@link #raceColumn} and {@link #fleet} are non-<code>null</code>.
     * 
     * @return <code>true</code> if current user, {@link #leaderboardName}, {@link #raceColumn} and {@link #fleet} are
     *         non-<code>null</code>, otherwise <code>false</code>.
     */
    protected boolean isLoggedInAndRaceLogAvailable() {
        return userService.getCurrentUser() != null && leaderboardName != null && raceColumn != null && fleet != null;
    }

    /**
     * Saves tag at current timer position.
     * 
     * @see #saveTag(String, Sting, String, boolean, TimePoint, boolean)
     */
    protected void saveTag(String tag, String comment, String imageURL, boolean visibleForPublic) {
        saveTag(tag, comment, imageURL, visibleForPublic, null);
    }

    /**
     * Sends request to {@link SailingServiceAsync SailingService} to add the given tag to the
     * {@link com.sap.sailing.domain.abstractlog.race.RaceLog RaceLog} if the parameter <code>isVisibleForPublic</code>
     * is set to <code>true</code>. Otherwise tag will be stored in the {@link com.sap.sse.security.UserStore
     * UserStore}. <br/>
     * Checks parameters for valid values and replaces optional parameters with value <code>null</code> by default
     * values: <code>comment</code> and <code>imageURL</code> will be replaced by an empty string,
     * <code>raceTimePoint</code> by current {@link #getTimerTime() timer position}.
     */
    protected void saveTag(String tag, String comment, String imageURL, boolean visibleForPublic,
            TimePoint raceTimePoint) {
        boolean tagIsNewTag = true;
        // check if tag already exists
        for (TagDTO tagDTO : tagListProvider.getAllTags()) {
            if (tagDTO.equals(tag, comment, imageURL, userService.getCurrentUser().getName(), visibleForPublic,
                    new MillisecondsTimePoint(getTimerTime()))) {
                tagIsNewTag = false;
                break;
            }
        }
        if (!tagIsNewTag) {
            // tag does already exist
            Notification.notify(stringMessages.tagNotSavedReason(" " + stringMessages.tagAlreadyExists()),
                    NotificationType.WARNING);

        } else if (!isLoggedInAndRaceLogAvailable()) {
            // User is not logged in or race can not be identified because regatta, race column or fleet are missing.
            Notification.notify(stringMessages.tagNotSaved(), NotificationType.ERROR);

        } else if (tag.isEmpty()) {
            // Tag heading is empty. Empty tags are not allowed.
            Notification.notify(stringMessages.tagNotSpecified(), NotificationType.WARNING);

        } else {
            // replace null values with default values
            final String saveComment = (comment == null ? "" : comment);
            final String saveImageURL = (imageURL == null ? "" : imageURL);
            final TimePoint saveRaceTimePoint = (raceTimePoint == null ? new MillisecondsTimePoint(getTimerTime())
                    : raceTimePoint);

            sailingService.addTag(leaderboardName, raceColumn.getName(), fleet.getName(), tag, saveComment,
                    saveImageURL, visibleForPublic, saveRaceTimePoint, new AsyncCallback<SuccessInfo>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            Notification.notify(stringMessages.tagNotSavedReason(caught.toString()),
                                    NotificationType.ERROR);
                        }

                        @Override
                        public void onSuccess(SuccessInfo result) {
                            if (result.isSuccessful()) {
                                Notification.notify(stringMessages.tagSavedSuccessfully(), NotificationType.INFO);
                                // reload private tags if added tag is private
                                if (!visibleForPublic) {
                                    reloadPrivateTags();
                                }
                            } else {
                                Notification.notify(stringMessages.tagNotSavedReason(result.getMessage()),
                                        NotificationType.ERROR);
                            }
                        }
                    });
        }
    }

    /**
     * Removes tag in non-<code>silent</code> mode.
     * 
     * @param tag
     *            tag to remove
     * 
     * @see #removeTag(TagDTO, boolean)
     */
    protected void removeTag(TagDTO tag) {
        removeTag(tag, false);
    }

    /**
     * Sends request to {@link SailingServiceAsync SailingService} to remove the given <code>tag</code>.
     * 
     * @param tag
     *            tag to remove
     * @param silent
     *            when set to <code>true</code>, only error messages will get displayed to user
     */
    protected void removeTag(TagDTO tag, boolean silent) {
        sailingService.removeTag(leaderboardName, raceColumn.getName(), fleet.getName(), tag,
                new AsyncCallback<SuccessInfo>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        Notification.notify(stringMessages.tagNotRemoved(), NotificationType.ERROR);
                        GWT.log(caught.getMessage());
                    }

                    @Override
                    public void onSuccess(SuccessInfo result) {
                        if (result.isSuccessful()) {
                            tagListProvider.remove(tag);
                            updateContent();
                            if (!silent) {
                                Notification.notify(stringMessages.tagRemovedSuccessfully(), NotificationType.SUCCESS);
                            }
                        } else {
                            Notification.notify(stringMessages.tagNotRemoved() + " " + result.getMessage(),
                                    NotificationType.ERROR);
                        }
                    }
                });
    }

    /**
     * Updates given <code>tagToUpdate</code> with the given parameters <code>tag</code>, <code>comment</code>,
     * <code>imageURL</code> and <code>isPublic</code>.
     * 
     * @see TagDTO
     */
    protected void updateTag(TagDTO tagToUpdate, String tag, String comment, String imageURL,
            boolean visibleForPublic) {
        sailingService.updateTag(leaderboardName, raceColumn.getName(), fleet.getName(), tagToUpdate, tag, comment,
                imageURL, visibleForPublic, new AsyncCallback<SuccessInfo>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        Notification.notify(stringMessages.tagNotSavedReason(caught.getMessage()),
                                NotificationType.ERROR);
                        GWT.log(caught.getMessage());
                    }

                    @Override
                    public void onSuccess(SuccessInfo result) {
                        if (result.isSuccessful()) {
                            tagListProvider.remove(tagToUpdate);
                            // If old tag was or new tag is private, reload all private tags. Otherwise just refresh UI.
                            if (!tagToUpdate.isVisibleForPublic() || !visibleForPublic) {
                                reloadPrivateTags();
                            } else {
                                updateContent();
                            }
                            Notification.notify(stringMessages.tagSavedSuccessfully(), NotificationType.SUCCESS);
                        } else {
                            Notification.notify(stringMessages.tagNotSavedReason(result.getMessage()),
                                    NotificationType.ERROR);
                        }
                    }
                });
    }

    /**
     * Removes all private tags from {@link TagListProvider}, loads all private tags from {@link SailingServiceAsync
     * SailingService}, adds them to the {@link TagListProvider} and updates the UI via {@link #updateContent()}.
     */
    private void reloadPrivateTags() {
        sailingService.getPrivateTags(leaderboardName, raceColumn.getName(), fleet.getName(),
                new AsyncCallback<List<TagDTO>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        GWT.log(caught.getMessage());
                    }

                    @Override
                    public void onSuccess(List<TagDTO> result) {
                        tagListProvider.removePrivateTags();
                        if (result != null && !result.isEmpty()) {
                            tagListProvider.addAll(result);
                        }
                        updateContent();
                    }
                });
    }

    /**
     * Controls the visibility of UI elements in case the content or {@link #currentState} changes.
     */
    protected void updateContent() {
        ensureFooterPanelVisibility();
        setCreateTagsButtonVisibility(currentState.equals(State.VIEW));
        if (currentState.equals(State.EDIT_TAG)) {
            taggingPanel.addStyleName(style.taggingPanelDisabled());
            // disable selection of tags when another tags gets edited (currentState == EDIT_TAG)
            tagCellList.setSelectionModel(new NoSelectionModel<TagDTO>());
        } else {
            taggingPanel.removeStyleName(style.taggingPanelDisabled());
            tagCellList.setSelectionModel(tagSelectionModel);
        }
        tagListProvider.updateFilteredTags();
        tagCellList.setVisibleRange(0, tagListProvider.getFilteredTags().size());
        tagListProvider.refresh();
    }

    /**
     * Forces {@link #contentPanel} to rerender.
     */
    protected void refreshContentPanel() {
        taggingPanel.setContentWidget(contentPanel);
    }

    /**
     * Adds {@link TagButton} to {@link #tagButtons list} of all {@link TagButton tag-buttons} and applies
     * {@link com.google.gwt.event.dom.client.ClickHandler ClickHandler} on it which allows saving of tags.
     */
    protected void addTagButton(TagButton tagButton) {
        tagButton.addClickHandler(event -> {
            saveTag(tagButton.getTag(), tagButton.getComment(), tagButton.getImageURL(),
                    tagButton.isVisibleForPublic());
        });
        tagButtons.add(tagButton);
    }

    /**
     * Returns time of {@link #timer}.
     */
    protected Date getTimerTime() {
        return timer.getTime();
    }

    /**
     * Returns the {@link SingleSelectionModel#getSelectedObject() current selected} {@link TagDTO tag}.
     */
    protected TagDTO getSelectedTag() {
        return tagSelectionModel.getSelectedObject();
    }

    /**
     * Returns {@link #tagButtons list} of all {@link TagButton tag-buttons} of the {@link UserService#getCurrentUser()
     * current user}.
     */
    protected List<TagButton> getTagButtons() {
        return tagButtons;
    }

    /**
     * Returns instance of {@link TagListProvider} so it does not have to be a constructor parameter of every sub
     * component of the {@link TaggingPanel}.
     */
    protected TagListProvider getTagListProvider() {
        return tagListProvider;
    }

    /**
     * Returns instance of {@link UserService} so it does not have to be a constructor parameter of every sub component
     * of the {@link TaggingPanel}.
     */
    protected UserService getUserSerivce() {
        return userService;
    }

    /**
     * Returns instance of {@link StringMessages} so it does not have to be a constructor parameter of every sub
     * component of the {@link TaggingPanel}.
     */
    protected StringMessages getStringMessages() {
        return stringMessages;
    }

    /**
     * Returns current state of {@link TaggingPanel}.
     * 
     * @return current state
     */
    protected State getCurrentState() {
        return currentState;
    }

    /**
     * Sets the {@link #currentState} to the given {@link State state} and updates the UI.
     * 
     * @param state
     *            new state
     */
    protected void setCurrentState(State state) {
        currentState = state;
        updateContent();
    }

    /**
     * Updates the visibility of the {@link #footerPanel} and it's components. Input fields will not get displayed if
     * user is not logged in. {@link TagButton}s can't be hidden and will get displayed automatically when
     * {@link UserService#getCurrentUser() current user} is logged in.
     */
    private void ensureFooterPanelVisibility() {
        // Setting footerPanel.setVisible(false) is not sufficient as panel would still be
        // rendered as 20px high white space instead of being hidden.
        // Fix: remove panel completely from footer.
        if (currentState != null && (!currentState.equals(State.VIEW)
                || (currentState.equals(State.VIEW) && !getTagButtons().isEmpty()))) {
            taggingPanel.setFooterWidget(footerPanel);
            footerPanel.setCurrentState(currentState);
        } else {
            taggingPanel.setFooterWidget(null);
        }
    }

    /**
     * Updates the visibility of the {@link #createTagsButton "Add Tags"-button}. {@link #createTagsButton Button} will
     * NOT get displayed if {@link UserService#getCurrentUser() current user} is not logged in, even if
     * <code>showButton</code> is set to <code>true</code>!
     */
    private void setCreateTagsButtonVisibility(boolean showButton) {
        createTagsButton.setVisible(userService.getCurrentUser() != null && showButton);
    }

    /**
     * Updates {@link TagListProvider#getAllTags() local list of tags} when response of {@link SailingServiceAsync
     * SailingService} gets dispatched to all listeners by {@link RaceTimesInfoProvider}. {@link SailingServiceAsync
     * SailingService} sends only difference of tags in comparison based on the <code>createdAt</code>-timestamp of the
     * {@link RaceTimesInfoProvider#latestReceivedTagTimes latest received tag events}.
     */
    @Override
    public void raceTimesInfosReceived(Map<RegattaAndRaceIdentifier, RaceTimesInfoDTO> raceTimesInfo,
            long clientTimeWhenRequestWasSent, Date serverTimeDuringRequest, long clientTimeWhenResponseWasReceived) {
        if (raceTimesInfo != null) {
            raceTimesInfo.forEach((raceIdentifier, raceInfo) -> {
                // Will be true if local list of tags get modified with new received tags, otherwise false.
                boolean modifiedTags = false;
                // Will be true if latestReceivedTagTime needs to be updated in raceTimesInfoprovider, otherwise false.
                boolean updatedLatestTag = false;
                // createdAt or revokedAt timepoint of latest received tag
                TimePoint latestReceivedTagTime = raceTimesInfoProvider.getLatestReceivedTagTime(raceIdentifier);
                // get difference in tags since latestReceivedTagTime
                if (raceInfo.getTags() != null) {
                    for (TagDTO tag : raceInfo.getTags()) {
                        if (tag.getRevokedAt() != null) {
                            // received tag is revoked => latestReceivedTagTime will be revokedAt if revoke event
                            // occured before latestReceivedTagTime
                            tagListProvider.remove(tag);
                            modifiedTags = true;
                            if (latestReceivedTagTime == null || (latestReceivedTagTime != null
                                    && latestReceivedTagTime.before(tag.getRevokedAt()))) {
                                latestReceivedTagTime = tag.getRevokedAt();
                                updatedLatestTag = true;
                            }
                        } else if (!tagListProvider.getAllTags().contains(tag)) {
                            // received tag is NOT revoked => latestReceivedTagTime will be createdAt if tag event
                            // occured before latestReceivedTagTime
                            tagListProvider.add(tag);
                            modifiedTags = true;
                            if (latestReceivedTagTime == null || (latestReceivedTagTime != null
                                    && latestReceivedTagTime.before(tag.getCreatedAt()))) {
                                latestReceivedTagTime = tag.getCreatedAt();
                                updatedLatestTag = true;
                            }
                        }
                    }
                }
                // set new latestReceivedTagTime for next data request
                if (updatedLatestTag) {
                    raceTimesInfoProvider.setLatestReceivedTagTime(raceIdentifier, latestReceivedTagTime);
                }
                // refresh UI if tags did change
                if (modifiedTags) {
                    updateContent();
                }
                //After tags were added for the first time, find tag which matches the URL Parameter "tag", hightlight it and jump to its logical timepoint
                if(firstTimePublicTagsLoaded && raceInfo.getTags() != null) {
                    firstTimePublicTagsLoaded = false;
                    if(sharedTimePoint != null) {
                        timer.setTime(sharedTimePoint.asMillis());
                        if(sharedTag != null) {
                            TagDTO matchingTag = null;
                            for(TagDTO tag: tagListProvider.getAllTags()) {
                                if(tag.getRaceTimepoint().equals(sharedTimePoint) && tag.getTag().equals(sharedTag)) {
                                    matchingTag = tag;
                                    break;
                                }
                            }
                            if(matchingTag != null) {
                                tagSelectionModel.clear();
                                tagSelectionModel.setSelected(matchingTag, true);
                            }
                            else {
                                Notification.notify(stringMessages.tagNotFound(), NotificationType.WARNING);
                            }
                        }
                        
                    }
                }
            });
        }
    }

    protected boolean hasPermissionToModifyPublicTags() {
        boolean hasPermission = false;
        if (leaderboardName != null && userService.getCurrentUser().hasPermission(
                Permission.LEADERBOARD.getStringPermissionForObjects(Mode.UPDATE, leaderboardName),
                SailingPermissionsForRoleProvider.INSTANCE)) {
            hasPermission = true;
        }
        return hasPermission;
    }

    /**
     * When the {@link UserService#getCurrentUser() current user} logs in or out the {@link #contentPanel content} needs
     * to be reset to hide private tags of the previous {@link UserService#getCurrentUser() current user}. This gets
     * achieved by resetting the {@link TagListProvider#getAllTags() local list of tags} and resetting all
     * {@link RaceTimesInfoProvider#latestReceivedTagTimes latest received tag events} at the
     * {@link RaceTimesInfoProvider}.
     */
    @Override
    public void onUserStatusChange(UserDTO user, boolean preAuthenticated) {
        // clear list of local tags to hide private tags of previous user and reset cache
        tagListProvider.clear();
        raceTimesInfoProvider.getRaceIdentifiers().forEach((raceIdentifier) -> {
            raceTimesInfoProvider.setLatestReceivedTagTime(raceIdentifier, null);
        });

        // load content for new user
        reloadPrivateTags();
        filterbarPanel.loadTagFilterSets();
        footerPanel.loadAllTagButtons();

        // update UI
        setCurrentState(State.VIEW);
    }

    /**
     * Highlights most current tag when timer changes.
     */
    @Override
    public void timeChanged(Date newTime, Date oldTime) {
        // When reopening Tagging-Panel after it was opened and closed by the user, the TouchSplitLayoutPanel will
        // set the timer to the current time and not the race time, oldTime will be null in this case. This would cause
        // a jump in time as TaggingPanel selects most current tag when time changes. When time continues while timer is
        // in "play"-mode, oldValue won't be null. This can be used as a workaround to discover this "false" time
        // change.
        // => workaround: Check if oldValue is not null to avoid jumps in time
        if (oldTime != null) {
            TagDTO toHighlight = null;
            for (TagDTO tag : tagListProvider.getAllTags()) {
                if (tag.getRaceTimepoint().asDate().getTime() <= newTime.getTime()) {
                    toHighlight = tag;
                } else if (tag.getRaceTimepoint().asDate().getTime() > newTime.getTime()) {
                    break;
                }
            }
            tagSelectionModel.clear();
            if (toHighlight != null) {
                tagSelectionModel.setSelected(toHighlight, true);
            }
        }
    }

    @Override
    public String getId() {
        return "TaggingPanel";
    }

    @Override
    public String getLocalizedShortName() {
        return stringMessages.tagPanel();
    }

    @Override
    public Widget getEntryWidget() {
        return taggingPanel;
    }

    @Override
    public boolean isVisible() {
        return taggingPanel.isVisible();
    }

    /**
     * Requests tags from server only if {@link TaggingPanel} is visible.
     */
    @Override
    public void setVisible(boolean visible) {
        if (raceTimesInfoProvider != null) {
            if (visible) {
                raceTimesInfoProvider.enableTagRequests();
                timer.addTimeListener(this);
            } else {
                raceTimesInfoProvider.disableTagRequests();
                timer.removeTimeListener(this);
            }
        }
        taggingPanel.setVisible(visible);
    }

    @Override
    public String getDependentCssClassName() {
        return "tags";
    }

}