package com.sap.sailing.gwt.ui.raceboard.tagging;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Panel;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.dto.TagDTO;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.raceboard.tagging.TaggingComponent.State;
import com.sap.sailing.gwt.ui.raceboard.tagging.TaggingPanelResources.TagPanelStyle;
import com.sap.sse.gwt.client.dialog.ConfirmationDialog;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.security.ui.client.UserService;

/**
 * Panel used to create and edit tags and {@link TagButton tag-buttons} at the {@link TaggingComponent#footerPanel footer}
 * of the {@link TaggingComponent}.
 */
public class TagModificationPanel extends FlowPanel {

    private final TagPanelStyle style = TaggingPanelResources.INSTANCE.style();

    private final TaggingComponent taggingComponent;
    private final StringMessages stringMessages;

    private final Label heading;
    private final Panel buttonsPanel;
    private final TagInputPanel inputPanel;
    private final Button editCustomTagButtons;

    /**
     * Creates panel used to create tags and {@link TagButton tag-buttons}.
     * 
     * @param taggingComponent
     *            provides reference to {@link StringMessages}
     * @param tagFooterPanel
     *            required for creation of {@link TagButton tag-buttons}
     */
    protected TagModificationPanel(TaggingComponent taggingComponent, TagFooterPanel tagFooterPanel,
            SailingServiceAsync sailingService, StringMessages stringMessages, UserService userService, RaceIdentifier raceIdentifier) {
        this.taggingComponent = taggingComponent;
        this.stringMessages = stringMessages;
        setStyleName(style.tagModificationPanel());
        inputPanel = new TagInputPanel(taggingComponent, sailingService, stringMessages, new DialogCallback<TagDTO>() {
            @Override
            public void ok(TagDTO editedObject) {
                if (taggingComponent.getCurrentState().equals(State.EDIT_TAG)) {
                    taggingComponent.updateTag(taggingComponent.getSelectedTag(), inputPanel.getTag(), inputPanel.getComment(),
                            inputPanel.getHiddenInfo(), inputPanel.getImageURL(), inputPanel.isVisibleForPublic());
                    resetState();
                } else {
                    if (taggingComponent.isLoggedInAndRaceLogAvailable()) {
                        taggingComponent.saveTag(inputPanel.getTag(), inputPanel.getComment(), inputPanel.getHiddenInfo(),
                                inputPanel.getImageURL(), inputPanel.isVisibleForPublic());
                        // TODO: Add callback to saveTag() to clear fields only if tag got really added
                        inputPanel.clearAllValues();
                    }
                }
            }

            @Override
            public void cancel() {
                if ((taggingComponent.getCurrentState().equals(State.CREATE_TAG) && !inputPanel.isInputEmpty())
                        || (taggingComponent.getCurrentState().equals(State.EDIT_TAG)
                                && !inputPanel.compareFieldsToTag(taggingComponent.getSelectedTag()))) {
                    ConfirmationDialog.create(stringMessages.tagDiscardChangesHeading(),
                            stringMessages.tagDiscardChanges(), stringMessages.confirm(), stringMessages.cancel(),
                            TagModificationPanel.this::resetState).center();
                } else {
                    resetState();
                }
            }
        });
        editCustomTagButtons = new Button(stringMessages.tagEditCustomTagButtons());
        editCustomTagButtons.setStyleName(style.tagDialogButton());
        editCustomTagButtons.addStyleName("gwt-Button");
        editCustomTagButtons.addClickHandler(event -> {
            if (taggingComponent.isLoggedInAndRaceLogAvailable()) {
                new TagButtonDialog(taggingComponent, tagFooterPanel, sailingService, stringMessages, userService, raceIdentifier);
            }
        });
        inputPanel.getOkButton().setText(stringMessages.tagAddTag());
        inputPanel.getOkButton().setStyleName(style.tagDialogButton());
        inputPanel.getOkButton().addStyleName("gwt-Button");
        inputPanel.getCancelButton().setText(stringMessages.cancel());
        inputPanel.getCancelButton().setStyleName(style.tagDialogButton());
        inputPanel.getCancelButton().addStyleName("gwt-Button");
        buttonsPanel = new FlowPanel();
        buttonsPanel.setStyleName(style.buttonsPanel());
        buttonsPanel.add(editCustomTagButtons);
        buttonsPanel.add(inputPanel.getOkButton());
        buttonsPanel.add(inputPanel.getCancelButton());
        buttonsPanel.setVisible(true);
        Panel headerPanel = new FlowPanel();
        headerPanel.setStyleName(style.tagModificationPanelHeader());
        heading = new Label(stringMessages.tagAddTags());
        heading.setStyleName(style.tagModificationPanelHeaderLabel());
        headerPanel.add(heading);
        Button closeFooterButton = new Button("X");
        closeFooterButton.setStyleName(style.tagModificationPanelHeaderButton());
        closeFooterButton.setTitle(stringMessages.close());
        closeFooterButton.addClickHandler(event -> {
            if ((taggingComponent.getCurrentState().equals(State.CREATE_TAG) && !inputPanel.isInputEmpty())
                    || (taggingComponent.getCurrentState().equals(State.EDIT_TAG)
                            && !inputPanel.compareFieldsToTag(taggingComponent.getSelectedTag()))) {
                ConfirmationDialog.create(stringMessages.tagDiscardChangesHeading(), stringMessages.tagDiscardChanges(),
                        stringMessages.confirm(), stringMessages.cancel(), TagModificationPanel.this::resetState)
                        .center();
            } else {
                resetState();
            }
        });
        headerPanel.add(closeFooterButton);
        add(headerPanel);
        add(inputPanel.getStatusLabel());
        add(inputPanel);
        add(buttonsPanel);
    }

    /**
     * Updates UI to match {@link TaggingComponent#getCurrentState() current state}.
     * 
     * @param state
     *            new state
     */
    protected void setCurrentStatus(State state) {
        if (state.equals(State.EDIT_TAG)) {
            heading.setText(stringMessages.tagEditTag());
            editCustomTagButtons.setVisible(false);
            inputPanel.getOkButton().setText(stringMessages.save());
            inputPanel.getCancelButton().setVisible(true);
            inputPanel.setTag(taggingComponent.getSelectedTag());
            inputPanel.validateAndUpdate();
        } else {
            heading.setText(stringMessages.tagAddTags());
            editCustomTagButtons.setVisible(true);
            inputPanel.getOkButton().setText(stringMessages.tagAddTag());
            inputPanel.getCancelButton().setVisible(false);
        }
        inputPanel.setCurrentStatus();
    }

    /**
     * Resets input fields and {@link TaggingComponent#setCurrentState(State) TaggingPanels currentState}.
     */
    private void resetState() {
        inputPanel.clearAllValues();
        taggingComponent.setCurrentState(State.VIEW);
    }
}
