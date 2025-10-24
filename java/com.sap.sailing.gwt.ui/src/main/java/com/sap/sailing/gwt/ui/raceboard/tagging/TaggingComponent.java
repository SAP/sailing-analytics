package com.sap.sailing.gwt.ui.raceboard.tagging;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.shared.GWT;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Style;
import com.google.gwt.user.cellview.client.CellList;
import com.google.gwt.user.cellview.client.HasKeyboardSelectionPolicy.KeyboardSelectionPolicy;
import com.google.gwt.user.client.Random;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HeaderPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.view.client.CellPreviewEvent;
import com.google.gwt.view.client.CellPreviewEvent.Handler;
import com.google.gwt.view.client.NoSelectionModel;
import com.google.gwt.view.client.SingleSelectionModel;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.domain.common.dto.TagDTO;
import com.sap.sailing.gwt.ui.client.RaceTimesInfoProvider;
import com.sap.sailing.gwt.ui.client.RaceTimesInfoProviderListener;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.raceboard.tagging.TaggingPanelResources.TagPanelStyle;
import com.sap.sailing.gwt.ui.shared.RaceTimesInfoDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.media.MediaTagConstants;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.Storage;
import com.sap.sse.gwt.client.media.ImageDTO;
import com.sap.sse.gwt.client.media.ImageResizingTaskDTO;
import com.sap.sse.gwt.client.player.TimeListener;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.shared.components.Component;
import com.sap.sse.gwt.client.shared.components.ComponentWithoutSettings;
import com.sap.sse.gwt.client.shared.settings.ComponentContext;
import com.sap.sse.gwt.client.xdstorage.CrossDomainStorage;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.dto.UserDTO;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.UserStatusEventHandler;
import com.sap.sse.security.ui.shared.SuccessInfo;

/**
 * A view showing tags which are connected to a specific race and allowing users to add own tags to a race. This view is
 * shown at the {@link com.sap.sailing.gwt.ui.raceboard.RaceBoardPanel RaceBoard}. Tags consist of a title and optional
 * a comment and/or image. Tag-Buttons allow users to preset tags which are used more frequently. Public tags will be
 * stored as an {@link com.sap.sailing.domain.abstractlog.race.RaceLogEvent RaceLogEvent}, private tags will be stored
 * in the {@link com.sap.sse.security.interfaces.UserStore UserStore}.
 * 
 * @author Julian Rendl, Henri Kohlberg
 */
public class TaggingComponent extends ComponentWithoutSettings
        implements RaceTimesInfoProviderListener, UserStatusEventHandler, TimeListener {

    /**
     * Describes the {@link TaggingComponent#currentState current state} of the {@link TaggingComponent}.
     */
    protected enum State {
        VIEW, // default
        CREATE_TAG, EDIT_TAG
    }

    private static final int FOOTERPANEL_ANIMATION_PERIOD_MS = 500;

    // HTML5 Storage for notifying other instances of tag changes
    private static final String LOCAL_STORAGE_UPDATE_KEY = "private-tags-changed";

    // styling
    private final TagPanelStyle style;

    // required to display tags
    private final CellList<TagDTO> tagCellList;
    private final SingleSelectionModel<TagDTO> tagSelectionModel;
    private final TagListProvider tagListProvider;

    // custom tag buttons of current user
    private final List<TagButton> tagButtons;

    // UI elements
    //        taggingPanel
    // +--------------------------+
    // |    -content / center-    |
    // | filterbarAndContentPanel |
    // | +----------------------+ |
    // | |       -header-       | |
    // | |    filterBarPanel    | |
    // | |    +============+    | |
    // | +----------------------+ |
    // | |  -content / center-  | |
    // | |     contentPanel     | |
    // | |     +==========+     | |
    // | +----------------------+ |
    // +--------------------------+
    // |         -south-          |
    // |       footerPanel        |
    // |    +----------------+    |
    // |    | tagFooterPanel |    |
    // |    | +============+ |    |
    // |    +----------------+    |
    // +--------------------------+
    private final DockLayoutPanel taggingPanel;
    private final HeaderPanel filterbarAndContentPanel;
    private final TagFilterPanel filterbarPanel;
    private final Panel contentPanel;
    private final ScrollPanel footerPanel;
    private final TagFooterPanel tagFooterPanel;
    private final Button createTagsButton;

    // misc. elements
    private final StringMessages stringMessages;
    private final SailingServiceAsync sailingService;
    private final SailingServiceWriteAsync sailingServiceWrite;
    private final UserService userService;
    private final Timer timer;
    private final RaceTimesInfoProvider raceTimesInfoProvider;

    // race log identifiers
    private String leaderboardName = null;
    private RaceColumnDTO raceColumn = null;
    private FleetDTO fleet = null;

    // current state of the Tagging-Panel
    private State currentState;

    // Needed for sharing Tags
    private boolean tagHasNotBeenHighlightedYet = true;
    protected final TimePoint timePointToHighlight;
    protected final String tagToHighlight;

    // ID for inter-instance communication
    private String id;

    /**
     * This boolean prevents the timer from jumping when other users create or delete tags. The timer change event is
     * called by the selection change event and the other way around. When:<br/>
     * 1) the timer is in <i>PLAY</i> mode and<br/>
     * 2) the current timer position is set after the last received tag and<br/>
     * 3) another user adds/deletes/changes any tag between the latest received tag and the current timer position<br/>
     * consecutively, the timer would jump to this new tag as the selection would change automatically as the latest tag
     * changed. This selection change would also trigger the timer to jump to the latest tag, which is not intended in
     * this case. Therefor any received changes on any tags will set this boolean to true which will ignore the time
     * jump at the selection change event and prevent this wrong behavior.
     * 
     * @see #raceTimesInfosReceived(Map, long, Date, long)
     */
    private boolean preventTimeJumpAtSelectionChangeForOnce = false;

    private StrippedLeaderboardDTO leaderboardDTO;

    public TaggingComponent(Component<?> parent, ComponentContext<?> context, StringMessages stringMessages,
            SailingServiceAsync sailingService, UserService userService, Timer timer,
            RaceTimesInfoProvider raceTimesInfoProvider, TimePoint timePointToHighlight, String tagToHighlight,
            StrippedLeaderboardDTO leaderboardDTO, SailingServiceWriteAsync sailingServiceWrite, RaceIdentifier raceIdentifier) {
        super(parent, context);
        this.stringMessages = stringMessages;
        this.sailingService = sailingService;
        this.sailingServiceWrite = sailingServiceWrite;
        this.userService = userService;
        this.timer = timer;
        this.raceTimesInfoProvider = raceTimesInfoProvider;
        this.timePointToHighlight = timePointToHighlight;
        this.tagToHighlight = tagToHighlight;
        this.leaderboardDTO = leaderboardDTO;
        style = TaggingPanelResources.INSTANCE.style();
        style.ensureInjected();
        TaggingPanelResources.INSTANCE.cellListStyle().ensureInjected();
        TaggingPanelResources.INSTANCE.cellTableStyle().ensureInjected();
        tagCellList = new CellList<TagDTO>(new TagCell(this, stringMessages, userService, false, raceIdentifier),
                TaggingPanelResources.INSTANCE);
        tagSelectionModel = new SingleSelectionModel<TagDTO>();
        tagListProvider = new TagListProvider();
        tagButtons = new ArrayList<TagButton>();
        taggingPanel = new DockLayoutPanel(Style.Unit.PX) {
            @Override
            public void onResize() {
                super.onResize();
                if (currentState != State.VIEW) {
                    taggingPanel.setWidgetSize(footerPanel, calculateFooterPanelHeight());
                }
            }
        };
        filterbarAndContentPanel = new HeaderPanel();
        tagFooterPanel = new TagFooterPanel(this, sailingService, stringMessages, userService, raceIdentifier);
        footerPanel = new ScrollPanel(tagFooterPanel);
        filterbarPanel = new TagFilterPanel(this, stringMessages, userService);
        contentPanel = new FlowPanel();
        createTagsButton = new Button();
        userService.addUserStatusEventHandler(this);
        raceTimesInfoProvider.addRaceTimesInfoProviderListener(this);
        generateRandomId();
        registerStorageEventHandler();
        setCurrentState(State.VIEW);
        initializePanel();
    }

    /**
     * Initializes UI of {@link TaggingComponent}.
     */
    private void initializePanel() {
        taggingPanel.setStyleName(style.taggingPanel());

        // taggingPanel content / center
        filterbarAndContentPanel.setHeaderWidget(filterbarPanel);
        filterbarAndContentPanel.setContentWidget(contentPanel);

        // taggingPanel footer
        taggingPanel.addSouth(footerPanel, 0);

        // contentPanel (tags)
        contentPanel.addStyleName(style.tagCellListPanel());
        contentPanel.add(tagCellList);
        contentPanel.add(createTagsButton);
        tagListProvider.addDataDisplay(tagCellList);
        tagCellList.setEmptyListWidget(new Label(stringMessages.tagNoTagsFound()));
        tagCellList.setKeyboardSelectionPolicy(KeyboardSelectionPolicy.DISABLED);
        tagCellList.setSelectionModel(tagSelectionModel);
        tagCellList.addCellPreviewHandler(new Handler<TagDTO>() {
            @Override
            public void onCellPreview(CellPreviewEvent<TagDTO> event) {
                if (BrowserEvents.CLICK.equals(event.getNativeEvent().getType())) {
                    // set time slider to corresponding position
                    TagDTO selectedTag = event.getValue();
                    if (selectedTag != null) {
                        /**
                         * Do not set time of timer when {@link #preventTimeJumpAtSelectionChangeForOnce} is set to
                         * <code>true</code>. In this case set {@link #preventTimeJumpAtSelectionChangeForOnce} to
                         * <code>false</code> as selection change is ignored once.
                         * 
                         * @see #preventTimeJumpAtSelectionChangeForOnce
                         */
                        if (preventTimeJumpAtSelectionChangeForOnce) {
                            preventTimeJumpAtSelectionChangeForOnce = false;
                        } else {
                            // remove time change listener when manually selecting tag cells as this could end in an
                            // infinite loop
                            // of timer change -> automatic selection change -> timer change -> ...
                            timer.removeTimeListener(TaggingComponent.this);
                            timer.setTime(selectedTag.getRaceTimepoint().asMillis());
                            // adding time change listener again
                            timer.addTimeListener(TaggingComponent.this);
                        }
                    }
                }
            }
        });
        createTagsButton.setTitle(stringMessages.tagAddTags());
        createTagsButton.setStyleName(style.toggleEditState());
        createTagsButton.addStyleName(style.imagePusTransparent());
        createTagsButton.addClickHandler(event -> {
            setCurrentState(State.CREATE_TAG);
        });
        taggingPanel.add(filterbarAndContentPanel);
        taggingPanel.forceLayout();
        updateContent();
    }

    /**
     * Registers a storage event handler that reloads the private tags if another instance has updated them.
     */
    private void registerStorageEventHandler() {
        Storage.addStorageEventHandler(event -> {
            if (LOCAL_STORAGE_UPDATE_KEY.equals(event.getKey()) && event.getNewValue() != null
                    && !event.getNewValue().isEmpty() && !event.getNewValue().equals(id.toString())) {
                reloadPrivateTags();
            }
        });
    }

    /**
     * Notifies other instances that the private tags have changed.
     */
    private void firePrivateTagUpdateEvent(CrossDomainStorage storage) {
        storage.getItem(LOCAL_STORAGE_UPDATE_KEY, value->{
            if (value.equals(id)) {
                // This instance fired the last update. To fire another one we have to change our id
                generateRandomId();
            }
            storage.setItem(LOCAL_STORAGE_UPDATE_KEY, id, null);
        });
    }

    /**
     * Generates a new random ID
     */
    private void generateRandomId() {
        id = Long.toString(System.currentTimeMillis() * Random.nextInt());
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
     * @see #saveTag(String, Sting, String, String, boolean, TimePoint, boolean)
     */
    protected void saveTag(String tag, String comment, String hiddenInfo, String imageURL, boolean visibleForPublic) {
        getResizedImageURLForImageURL(imageURL, new AsyncCallback<String>() {
            @Override
            public void onFailure(Throwable caught) {
                saveTag(tag, comment, hiddenInfo, imageURL, /* resizedImageURL */ null, visibleForPublic, null);
                GWT.log(caught.getMessage());
            }

            @Override
            public void onSuccess(String resizedImageURL) {
                saveTag(tag, comment, hiddenInfo, imageURL, resizedImageURL, visibleForPublic, null);
            }
        });
    }

    /**
     * Returns image URL of resized image for given image URL.
     * 
     * @param imageURL
     *            URL of image which needs to be resized
     * @param callback
     *            An asynchronous callback containing the resized image URL, <code>empty string</code> in case of empty
     *            given image URL
     */
    protected void getResizedImageURLForImageURL(String imageURL, AsyncCallback<String> callback) {
        if (imageURL == null || imageURL.isEmpty()) {
            callback.onSuccess("");
        } else {
            sailingService.resolveImageDimensions(imageURL, new AsyncCallback<Pair<Integer, Integer>>() {
                @Override
                public void onFailure(Throwable caught) {
                    callback.onFailure(caught);
                }

                @Override
                public void onSuccess(Pair<Integer, Integer> result) {
                    if (result == null || result.getA() == null || result.getB() == null) {
                        callback.onFailure(new IllegalArgumentException("Size of image could not be determined!"));
                    } else {
                        int imageWidth = result.getA();
                        int imageHeight = result.getB();
                        if (imageWidth < MediaTagConstants.TAGGING_IMAGE.getMinWidth()
                                || imageHeight < MediaTagConstants.TAGGING_IMAGE.getMinHeight()) {
                            callback.onFailure(new IllegalArgumentException("Image is too small for resizing!"));
                        } else {
                            if (imageWidth > MediaTagConstants.TAGGING_IMAGE.getMaxWidth()
                                    || imageHeight > MediaTagConstants.TAGGING_IMAGE.getMaxHeight()) {
                                ArrayList<MediaTagConstants> tags = new ArrayList<MediaTagConstants>();
                                tags.add(MediaTagConstants.TAGGING_IMAGE);
                                sailingServiceWrite.resizeImage(new ImageResizingTaskDTO(imageURL, new Date(), tags),
                                        new AsyncCallback<Set<ImageDTO>>() {
                                            @Override
                                            public void onFailure(Throwable caught) {
                                                callback.onFailure(caught);
                                            }

                                            @Override
                                            public void onSuccess(Set<ImageDTO> result) {
                                                String resizedImageURL = null;
                                                if (result.size() != 0) {
                                                    resizedImageURL = result.iterator().next().getSourceRef();
                                                }
                                                callback.onSuccess(resizedImageURL);
                                            }
                                        });
                            } else {
                                callback.onSuccess(imageURL);
                            }
                        }
                    }

                }
            });
        }
    }

    /**
     * Sends request to {@link SailingServiceAsync SailingService} to add the given tag to the
     * {@link com.sap.sailing.domain.abstractlog.race.RaceLog RaceLog} if the parameter <code>isVisibleForPublic</code>
     * is set to <code>true</code>. Otherwise tag will be stored in the {@link com.sap.sse.security.interfaces.UserStore
     * UserStore}. <br/>
     * Checks parameters for valid values and replaces optional parameters with value <code>null</code> by default
     * values: <code>comment</code> and <code>imageURL</code> will be replaced by an empty string,
     * <code>raceTimePoint</code> by current {@link #getTimerTime() timer position}.
     * @param hiddenInfo TODO
     */
    protected void saveTag(String tag, String comment, String hiddenInfo, String imageURL,
            String resizedImageURL, boolean visibleForPublic, TimePoint raceTimePoint) {
        if (tagAlreadyExists(tag, comment, hiddenInfo, imageURL, resizedImageURL, visibleForPublic, raceTimePoint)) {
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
            final TimePoint saveRaceTimePoint = (raceTimePoint == null ? new MillisecondsTimePoint(getTimerTime())
                    : raceTimePoint);
            sailingServiceWrite.addTag(leaderboardName, raceColumn.getName(), fleet.getName(), tag, saveComment, hiddenInfo,
                    imageURL, resizedImageURL, visibleForPublic, saveRaceTimePoint, new AsyncCallback<SuccessInfo>() {
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
                                    firePrivateTagUpdateEvent(userService.getStorage());
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
        sailingServiceWrite.removeTag(leaderboardName, raceColumn.getName(), fleet.getName(), tag,
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
                            firePrivateTagUpdateEvent(userService.getStorage());
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
    protected void updateTag(TagDTO tagToUpdate, String tag, String comment, String hiddenInfo, String imageURL, boolean visibleForPublic) {
        // A new resized image gets created every time a tag is updated (if tag shall contain an image)
        getResizedImageURLForImageURL(imageURL, new AsyncCallback<String>() {
            @Override
            public void onFailure(Throwable caught) {
                Notification.notify(caught.getMessage(), NotificationType.WARNING);
                updateTag(tagToUpdate, tag, comment, hiddenInfo, imageURL, visibleForPublic, /* resizedImageURL */ null);
                GWT.log(caught.getMessage());
            }

            @Override
            public void onSuccess(String resizedImageURL) {
                updateTag(tagToUpdate, tag, comment, hiddenInfo, imageURL, visibleForPublic, resizedImageURL);
            }
        });
    }

    protected void updateTag(TagDTO tagToUpdate, String tag, String comment, String hiddenInfo, String imageURL,
            boolean visibleForPublic, String resizedImageURL) {
        sailingServiceWrite.updateTag(leaderboardName, raceColumn.getName(), fleet.getName(), tagToUpdate, tag,
                comment, hiddenInfo, imageURL, resizedImageURL, visibleForPublic, new AsyncCallback<SuccessInfo>() {
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
                            // If old tag was or new tag is private, reload all private tags. Otherwise just
                            // refresh UI.
                            if (!tagToUpdate.isVisibleForPublic() || !visibleForPublic) {
                                reloadPrivateTags();
                                firePrivateTagUpdateEvent(userService.getStorage());
                            } else {
                                updateContent();
                            }
                            Notification.notify(stringMessages.tagSavedSuccessfully(),
                                    NotificationType.SUCCESS);
                        } else {
                            Notification.notify(stringMessages.tagNotSavedReason(result.getMessage()),
                                    NotificationType.ERROR);
                        }
                    }
                });
    }

    /**
     * Returns whether given tag already exists.
     * 
     * @return <code>true</code> if tag already exists (only checked client side), otherwise <code>false</code>
     */
    protected boolean tagAlreadyExists(String tag, String comment, String hiddenInfo, String imageURL,
            String resizedImageURL, boolean visibleForPublic, TimePoint raceTimePoint) {
        for (TagDTO tagDTO : tagListProvider.getAllTags()) {
            if (tagDTO.equals(tag, comment, hiddenInfo, imageURL, resizedImageURL,
                    visibleForPublic, userService.getCurrentUser().getName(), new MillisecondsTimePoint(getTimerTime()))) {
                return true;
            }
        }
        return false;
    }

    /**
     * Removes all private tags from {@link TagListProvider}, loads all private tags from {@link SailingServiceAsync
     * SailingService}, adds them to the {@link TagListProvider} and updates the UI via {@link #updateContent()}.
     */
    private void reloadPrivateTags() {
        tagListProvider.removePrivateTags();
        if (userService.getCurrentUser() != null) {
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
    }

    /**
     * Controls the visibility of UI elements in case the content or {@link #currentState} changes.
     */
    protected void updateContent() {
        ensureFooterPanelVisibility();
        createTagsButton.setVisible(userService.getCurrentUser() != null && currentState.equals(State.VIEW));
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
        filterbarAndContentPanel.setContentWidget(contentPanel);
    }

    /**
     * Adds {@link TagButton} to {@link #tagButtons list} of all {@link TagButton tag-buttons} and applies
     * {@link com.google.gwt.event.dom.client.ClickHandler ClickHandler} on it which allows saving of tags.
     */
    protected void addTagButton(TagButton tagButton) {
        tagButton.addClickHandler(event -> {
            saveTag(tagButton.getTag(), tagButton.getComment(), /* hidden info */ null,
                    tagButton.getImageURL(), tagButton.isVisibleForPublic());
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
     * component of the {@link TaggingComponent}.
     */
    protected TagListProvider getTagListProvider() {
        return tagListProvider;
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
     * Returns current state of {@link TaggingComponent}.
     * 
     * @return current state
     */
    protected State getCurrentState() {
        return currentState;
    }

    /**
     * Updates the visibility of the {@link #footerPanel} and it's components. Input fields will not get displayed if
     * user is not logged in. {@link TagButton}s can't be hidden and will get displayed automatically when
     * {@link UserService#getCurrentUser() current user} is logged in.
     */
    private void ensureFooterPanelVisibility() {
        if (currentState != null && (!currentState.equals(State.VIEW)
                || (currentState.equals(State.VIEW) && !getTagButtons().isEmpty()))) {
            if (taggingPanel.getWidgetIndex(footerPanel) != -1) {
                // Expand panel to 1 px which causes the browser to calculate its size
                taggingPanel.setWidgetSize(footerPanel, 1);
                // The scheduled command will execute after the browser has determined the size
                Scheduler.get().scheduleFinally(new RepeatingCommand() {
                    @Override
                    public boolean execute() {
                        taggingPanel.setWidgetSize(footerPanel, calculateFooterPanelHeight());
                        taggingPanel.animate(FOOTERPANEL_ANIMATION_PERIOD_MS);
                        return false;
                    }
                });
            }
            tagFooterPanel.setCurrentState(currentState);
        } else {
            if (taggingPanel.getWidgetIndex(footerPanel) != -1) {
                taggingPanel.setWidgetSize(footerPanel, 0);
                taggingPanel.animate(FOOTERPANEL_ANIMATION_PERIOD_MS);
            }
        }
    }

    private int calculateFooterPanelHeight() {
        // Start with footerPanel content height
        int height = footerPanel.getWidget().getElement().getOffsetHeight();
        // Limit size if we take up the entire height of the viewing area
        // to make sure we don't get pushed out of it at the top making parts of the footerPanel inaccessible
        height = Math.min(height, taggingPanel.getOffsetHeight());
        return height;
    }

    /**
     * Checks whether current user has permission to modify public tags.
     * 
     * @return <code>true</code> if user has {@link Mode#UPDATE update permissions} on {@link #leaderboardName current
     *         leaderboard}, otherwise <code>false</code>
     */
    protected boolean hasPermissionToModifyPublicTags() {
        boolean hasPermission = false;
        if (leaderboardName != null && userService.hasPermission(leaderboardDTO, DefaultActions.UPDATE)) {
            hasPermission = true;
        }
        return hasPermission;
    }

    /**
     * Clears local list of tags and reloads settings for the current user (private tags, tag buttons and filter).
     */
    protected void clearCache() {
        tagListProvider.clear();
        raceTimesInfoProvider.getRaceIdentifiers().forEach((raceIdentifier) -> {
            raceTimesInfoProvider.setLatestReceivedTagTime(raceIdentifier, null);
        });
        reloadPrivateTags();
        filterbarPanel.loadTagFilterSets();
        tagFooterPanel.loadAllTagButtons();
    }

    /**
     * Updates {@link TagListProvider#getAllTags() local list of tags} when response of {@link SailingServiceAsync
     * SailingService} gets dispatched to all listeners by {@link RaceTimesInfoProvider} and highlights shared tag in
     * case a tag is shared via URL. {@link SailingServiceAsync SailingService} sends only difference of tags in
     * comparison based on the <code>createdAt</code>-timestamp of the
     * {@link RaceTimesInfoProvider#latestReceivedTagTimes latest received tag events}. <br/>
     * <br/>
     * <b>Notice for shared tags:</b><br/>
     * There is a mismatch with the key of tags in general in comparison to shared tags which get highlighted by this
     * method. Tags use every attribute except for createdAt and revokedAt time points to identify them as the same tag
     * without using a generated custom ID (see also {@link TagDTO#equals(Object) equals()}). Appending these parameters
     * to the URL is unnecessary and may create a pretty long URL when sharing a tag. To prevent this, only title and
     * race time point are part of the URL parameter. The problem with this solution is that a wrong tag might be
     * highlighted instead of the shared one when multiple tags have the same title and race time point. We decided to
     * ignore this corner case and highlight the first matching tag (see bug 4104, comment 34).
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
                    preventTimeJumpAtSelectionChangeForOnce = true;
                    updateContent();
                }
                // Search for tag which matches the URL Parameter "tag", highlight the corresponding tag and jump to its
                // logical race time point after tags have been loaded for the first time.
                if (tagHasNotBeenHighlightedYet && raceInfo.getTags() != null) {
                    tagHasNotBeenHighlightedYet = false;
                    if (timePointToHighlight != null) {
                        timer.setTime(timePointToHighlight.asMillis());
                        if (tagToHighlight != null) {
                            TagDTO matchingTag = null;
                            for (TagDTO tag : tagListProvider.getAllTags()) {
                                if (tag.getRaceTimepoint().equals(timePointToHighlight)
                                        && tag.getTag().equals(tagToHighlight)) {
                                    matchingTag = tag;
                                    break;
                                }
                            }
                            if (matchingTag != null) {
                                tagSelectionModel.clear();
                                tagSelectionModel.setSelected(matchingTag, true);
                            } else {
                                Notification.notify(stringMessages.tagNotFound(), NotificationType.WARNING);
                            }
                        }

                    }
                }
            });
        }
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
        clearCache();
        setCurrentState(State.VIEW);
    }

    /**
     * Highlights most current tag when timer changes.
     */
    @Override
    public void timeChanged(Date newTime, Date oldTime) {
        // When reopening Tagging-Panel after it was opened and closed by the user, the TouchSplitLayoutPanel will
        // set the timer to the current time and not the race time, oldTime will be null in this case. This would cause
        // a jump in time as TaggingComponent selects most current tag when time changes. When time continues while timer is
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
        return "TaggingComponent";
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
     * Requests tags from server only if {@link TaggingComponent} is visible.
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