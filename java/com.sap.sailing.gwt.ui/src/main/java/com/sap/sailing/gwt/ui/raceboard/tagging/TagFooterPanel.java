package com.sap.sailing.gwt.ui.raceboard.tagging;

import com.google.gwt.user.client.ui.FlowPanel;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.raceboard.tagging.TaggingComponent.State;
import com.sap.sse.security.ui.client.UserService;

/**
 * Panel containg {@link TagModificationPanel} and {@link TagButtonPanel} which is used as
 * {@link TaggingComponent#footerPanel footer} in {@link TaggingComponent}.
 */
public class TagFooterPanel extends FlowPanel {

    private final TaggingComponent taggingComponent;

    private final TagModificationPanel tagModificationPanel;
    private final TagButtonPanel tagButtonPanel;

    /**
     * Creates footer panel and loads {@link TagButton tag-buttons} from {@link com.sap.sse.security.interfaces.UserStore
     * UserStore}.
     * 
     * @param taggingComponent
     *            required to instantiate {@link TagModificationPanel} and {@link TagButtonPanel}
     */
    protected TagFooterPanel(TaggingComponent taggingComponent, SailingServiceAsync sailingService, StringMessages stringMessages, UserService userService, RaceIdentifier raceIdentifier) {
        this.taggingComponent = taggingComponent;
        tagModificationPanel = new TagModificationPanel(taggingComponent, this, sailingService, stringMessages, userService, raceIdentifier);
        tagButtonPanel = new TagButtonPanel(taggingComponent, this, stringMessages, userService);
        // Tag-buttons are only shown if amount of tag-buttons is greater then 0!
        setTagButtonsVisibility(true);
        // input fields are hidden by default
        setInputFieldsVisibility(false);
        tagButtonPanel.loadAllTagButtons();
    }

    /**
     * Sets visibility of {@link TagModificationPanel input fields} for new tag and {@link TaggingComponent#createTagsButton
     * "Edit Tag-Buttons"-button}. {@link TagModificationPanel Input fields} will not be hidden but removed completly
     * from DOM in case <code>visible</code> is set to <code>false</code>!
     * 
     * @param visible
     *            should be <code>true</code> when user is logged in and {@link TaggingComponent} is in
     *            {@link TaggingComponent#currentState EDIT-mode}, otherwise <code>false</code>
     */
    protected void setInputFieldsVisibility(boolean visible) {
        if (visible) {
            add(tagModificationPanel);
        } else {
            remove(tagModificationPanel);
        }
    }

    /**
     * Sets visibility of {@link TagButtonPanel tagButtonPanel}. {@link TagButtonPanel tagButtonPanel} will only be
     * shown if amount of {@link TaggingComponent#getTagButtons() tag-buttons} is greater then <code>0</code>!
     * 
     * @param visible
     *            should be <code>true</code> when user is logged in, otherwise <code>false</code>
     */
    protected void setTagButtonsVisibility(boolean visible) {
        if (visible && taggingComponent.getTagButtons().size() > 0) {
            add(tagButtonPanel);
        } else {
            remove(tagButtonPanel);
        }
    }

    protected void setCurrentState(State state) {
        if (state == State.CREATE_TAG) {
            setInputFieldsVisibility(true);
            setTagButtonsVisibility(true);
            tagModificationPanel.setCurrentStatus(state);
        } else if (state == State.EDIT_TAG) {
            setInputFieldsVisibility(true);
            setTagButtonsVisibility(false);
            tagModificationPanel.setCurrentStatus(state);
        } else {
            setInputFieldsVisibility(false);
            setTagButtonsVisibility(true);
            tagModificationPanel.setCurrentStatus(state);
        }
    }

    /**
     * If height of the {@link TagButtonPanel tagButtonPanel} has changed after deleting (delta height does not equal
     * 0), the {@link TaggingComponent#footerPanel footer widget} of the {@link TaggingComponent} has a different height, which
     * in this case might cause the {@link TaggingComponent#contentPanel content widget} to be to small.
     */
    protected void recalculateHeight() {
        tagButtonPanel.recalculateHeight();
    }

    /**
     * Loads all {@link TagButton tag-buttons} from {@link com.sap.sse.security.interfaces.UserStore UserStore} and displays them.
     */
    protected void loadAllTagButtons() {
        tagButtonPanel.loadAllTagButtons();
    }

    /**
     * Stores all {@link TagButton tag-buttons} into {@link com.sap.sse.security.interfaces.UserStore UserStore}.
     */
    protected void storeAllTagButtons() {
        tagButtonPanel.storeAllTagButtons();
    }
}
