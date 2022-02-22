package com.sap.sailing.gwt.ui.raceboard.tagging;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.raceboard.tagging.TaggingPanelResources.TagPanelStyle;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.security.ui.client.UserService;

/**
 * Panel used to show the {@link TagButton tag-buttons} of {@link UserService#getCurrentUser() current user}.
 */
public class TagButtonPanel extends FlowPanel {

    private static final String USER_STORAGE_TAG_BUTTONS_KEY = "sailing.tags.buttons";

    private final TagPanelStyle style = TaggingPanelResources.INSTANCE.style();

    private final TagFooterPanel footerPanel;
    private final TaggingComponent taggingComponent;
    private final StringMessages stringMessages;
    private final UserService userService;

    private final Label heading;
    private final Panel tagButtonsPanel;

    /**
     * Displays {@link TagButton tag-buttons} of {@link UserService#getCurrentUser() current user} at the footer of the
     * {@link TaggingComponent}.
     */
    protected TagButtonPanel(TaggingComponent taggingComponent, TagFooterPanel footerPanel, StringMessages stringMessages,
            UserService userService) {
        this.footerPanel = footerPanel;
        this.taggingComponent = taggingComponent;
        this.stringMessages = stringMessages;
        this.userService = userService;

        heading = new Label(stringMessages.tagButtons());
        heading.setStyleName(style.tagButtonPanelHeader());
        add(heading);

        tagButtonsPanel = new FlowPanel();
        tagButtonsPanel.setStyleName(style.buttonsPanel());
        tagButtonsPanel.addStyleName(style.tagButtonPanel());
        add(tagButtonsPanel);
    }

    /**
     * Loads all {@link TagButton tag-buttons} of {@link UserService#getCurrentUser() current user} from
     * {@link com.sap.sse.security.interfaces.UserStore UserStore} and displays them.
     */
    protected void loadAllTagButtons() {
        tagButtonsPanel.clear();
        if (userService.getCurrentUser() != null) {
            userService.getPreference(USER_STORAGE_TAG_BUTTONS_KEY, new AsyncCallback<String>() {
                @Override
                public void onFailure(Throwable caught) {
                    // preference does not exist for this user in user store
                    // => user did not save any tag-buttons before
                    // => ignore error
                }

                @Override
                public void onSuccess(String result) {
                    taggingComponent.getTagButtons().clear();
                    if (result != null && !result.isEmpty()) {
                        final TagButtonJsonDeSerializer deserializer = new TagButtonJsonDeSerializer();
                        final JSONValue value = JSONParser.parseStrict(result);
                        if (value.isArray() != null) {
                            for (TagButton tagButton : deserializer.deserialize((JSONArray) value)) {
                                taggingComponent.addTagButton(tagButton);
                            }
                        }
                    }
                    recalculateHeight();
                }
            });
        } else {
            taggingComponent.getTagButtons().clear();
            recalculateHeight();
        }
    }

    /**
     * Stores {@link TaggingComponent#getTagButtons() local copy} of {@link TagButton tag-buttons} of the
     * {@link UserService#getCurrentUser() current user} in {@link com.sap.sse.security.interfaces.UserStore UserStore}.
     */
    protected void storeAllTagButtons() {
        TagButtonJsonDeSerializer serializer = new TagButtonJsonDeSerializer();
        JSONArray jsonArray = serializer.serialize(taggingComponent.getTagButtons());
        userService.setPreference(USER_STORAGE_TAG_BUTTONS_KEY, jsonArray.toString(), new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                Notification.notify(stringMessages.tagButtonNotSavable(), NotificationType.WARNING);
            }

            @Override
            public void onSuccess(Void result) {
            }
        });
    }

    /**
     * If the height of the {@link #tagButtonsPanel} has changed after deleting all {@link TagButton tag-buttons} (delta
     * height does not equal 0), the {@link TaggingComponent}s
     * {@link com.google.gwt.user.client.ui.HeaderPanel#getFooterWidget() footer widget} has a different height, which
     * in this case might cause the {@link TaggingComponent#contentPanel contentWidget} to be to small. In case no
     * {@link TagButton tag-buttons} are available for the {@link UserService#getCurrentUser() current user},
     * {@link TagButtonPanel} will be hidden.
     */
    protected void recalculateHeight() {
        if (taggingComponent.getTagButtons().size() == 0) {
            tagButtonsPanel.clear();
            footerPanel.setTagButtonsVisibility(false);
        } else {
            footerPanel.setTagButtonsVisibility(true);
            final int oldHeight = getOffsetHeight();
            tagButtonsPanel.clear();
            taggingComponent.getTagButtons().forEach(button -> {
                tagButtonsPanel.add(button);
            });
            if ((getOffsetHeight() - oldHeight) != 0) {
                taggingComponent.refreshContentPanel();
            }
        }
    }
}
