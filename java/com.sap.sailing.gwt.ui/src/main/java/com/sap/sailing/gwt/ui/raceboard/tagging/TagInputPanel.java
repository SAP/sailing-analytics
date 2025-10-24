package com.sap.sailing.gwt.ui.raceboard.tagging;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.sap.sailing.domain.common.dto.TagDTO;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.raceboard.tagging.TaggingPanelResources.TagPanelStyle;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.adminconsole.URLFieldWithFileUpload;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;

/**
 * Panel containing input fields for tag/tag button creation and modification.
 */
public class TagInputPanel extends FlowPanel {

    private static final String DEFAULT_TAG = "";
    private static final String DEFAULT_COMMENT = "";
    private static final String DEFAULT_HIDDEN_INFO = "";
    private static final String DEFAULT_IMAGE_URL = "";
    private static final boolean DEFAULT_VISIBLE_FOR_PUBLIC = false;

    /**
     * Extends {@link DataEntryDialog} to provide input validation for tags. The dialog itself is never shown to the
     * user. Therefore methods {@link #show()} and {@link #center()} are overridden to have no effect. Instead input fields
     * can be used in other UIs as validator still has access to these fields.
     */
    private class TagEntryFields extends DataEntryDialog<TagDTO> {
        private final StringMessages stringMessages;
        private final TaggingComponent taggingComponent;
        private final TextBox tagTextBox;
        private final TextArea commentTextArea;
        private final TextArea hiddenInfoTextArea;
        private final CheckBox visibleForPublicCheckBox;
        private final URLFieldWithFileUpload imageUploadPanel;

        public TagEntryFields(StringMessages stringMessages, TaggingComponent taggingComponent,
                DialogCallback<TagDTO> callback) {
            // set empty values for parameters which are not shown to the user. okayButtonText will be set by the using
            // components.
            super(/* title */ "", /* message */ "", /* okayButtonText */ "", stringMessages.cancel(), null,
                    /* animationEnabled */ true, callback);
            this.setValidator(this::validate);
            this.stringMessages = stringMessages;
            this.taggingComponent = taggingComponent;
            tagTextBox = createTextBox(DEFAULT_TAG);
            commentTextArea = createTextArea(DEFAULT_COMMENT);
            hiddenInfoTextArea = createTextArea(DEFAULT_HIDDEN_INFO);
            visibleForPublicCheckBox = createCheckbox(stringMessages.tagVisibleForPublicCheckBox());
            visibleForPublicCheckBox.setValue(DEFAULT_VISIBLE_FOR_PUBLIC);
            imageUploadPanel = new URLFieldWithFileUpload(stringMessages, null);
            getCancelButton().addClickHandler(clickEvent-> imageUploadPanel.deleteCurrentFile());
            validateAndUpdate();
        }

        /**
         * Validates given <code>tagToValidate</code> and returns error message in case input is not valid.
         * 
         * @param tagToValidate
         *            tag to be checked
         * @return <code>error message</code> in case input is not valid, otherwise <code>null</code>
         */
        private String validate(TagDTO tagToValidate) {
            final String result;
            if (tagToValidate == null) {
                result = stringMessages.unknown();
            } else if (tagToValidate.getTag() == null || tagToValidate.getTag().isEmpty()) {
                result = stringMessages.tagNotSpecified();
            } else if (taggingComponent.tagAlreadyExists(tagToValidate.getTag(), tagToValidate.getComment(),
                    tagToValidate.getHiddenInfo(), tagToValidate.getImageURL(), tagToValidate.getResizedImageURL(),
                    tagToValidate.isVisibleForPublic(), tagToValidate.getRaceTimepoint())) {
                result = stringMessages.tagAlreadyExists();
            } else {
                result = null;
            }
            return result;
        }

        public TextBox getTagTextBox() {
            return tagTextBox;
        }

        public TextArea getCommentTextArea() {
            return commentTextArea;
        }
        
        private TextArea getHiddenInfoTextArea() {
            return hiddenInfoTextArea;
        }

        public CheckBox getVisibleForPublicCheckBox() {
            return visibleForPublicCheckBox;
        }

        public URLFieldWithFileUpload getImageUploadPanel() {
            return imageUploadPanel;
        }

        public Button getOkButton() {
            return super.getOkButton();
        }

        public Button getCancelButton() {
            return super.getCancelButton();
        }

        public Label getStatusLabel() {
            return super.getStatusLabel();
        }

        public void validateAndUpdate() {
            super.validateAndUpdate();
        }

        /**
         * Returns {@link TagDTO} created by input fields which does only contain values entered explicitly by user.
         */
        @Override
        protected TagDTO getResult() {
            return new TagDTO(getTagTextBox().getValue(), getCommentTextArea().getValue(),
                    getHiddenInfoTextArea().getValue(), getImageUploadPanel().getUri(), null, getVisibleForPublicCheckBox().getValue(), null, null, null);
        }

        @Override
        protected Focusable getInitialFocusWidget() {
            return null;
        }

        @Override
        public void show() {
        }

        @Override
        public void center() {
        }
    }

    private final TagPanelStyle style = TaggingPanelResources.INSTANCE.style();
    private final TaggingComponent taggingComponent;
    private final TagEntryFields tagEntryFields;
    private final Grid grid;
    private final FlowPanel checkboxWrapper;
    private final Label noPermissionForPublicTagsLabel;

    /**
     * Creates view allowing users to input values for tags and {@link TagButton tag-buttons}.
     */
    protected TagInputPanel(TaggingComponent taggingComponent, SailingServiceAsync sailingService,
            StringMessages stringMessages, DialogCallback<TagDTO> callback) {
        this.taggingComponent = taggingComponent;
        tagEntryFields = new TagEntryFields(stringMessages, taggingComponent, callback);
        setStyleName(style.tagInputPanel());
        grid = new Grid(4, 2);
        // tag
        Label tagLabel = new Label(stringMessages.tagLabelTag() + ":");
        tagEntryFields.getTagTextBox().setTitle(stringMessages.tagLabelTag());
        tagEntryFields.getTagTextBox().setStyleName(style.tagInputPanelTag());
        tagEntryFields.getTagTextBox().getElement().setPropertyString("placeholder", stringMessages.tagLabelTag());
        grid.setWidget(0, 0, tagLabel);
        grid.setWidget(0, 1, tagEntryFields.getTagTextBox());
        // image upload
        Label imageLabel = new Label(stringMessages.tagLabelImage() + ":");
        grid.setWidget(1, 0, imageLabel);
        grid.setWidget(1, 1, tagEntryFields.getImageUploadPanel());
        // comment
        Label commentLabel = new Label(stringMessages.tagLabelComment() + ":");
        tagEntryFields.getCommentTextArea().setTitle(stringMessages.tagLabelComment());
        tagEntryFields.getCommentTextArea().setStyleName(style.tagInputPanelComment());
        tagEntryFields.getCommentTextArea().setVisibleLines(4);
        tagEntryFields.getCommentTextArea().getElement().setPropertyString("placeholder",
                stringMessages.tagLabelComment());
        grid.setWidget(2, 0, commentLabel);
        grid.setWidget(2, 1, tagEntryFields.getCommentTextArea());
        // visibility
        Label visibilityLabel = new Label(stringMessages.tagLabelVisibility() + ":");
        noPermissionForPublicTagsLabel = new Label(stringMessages.tagPublicModificationPermissionMissing());
        checkboxWrapper = new FlowPanel();
        checkboxWrapper.add(tagEntryFields.getVisibleForPublicCheckBox());
        checkboxWrapper.add(noPermissionForPublicTagsLabel);
        grid.setWidget(3, 0, visibilityLabel);
        grid.setWidget(3, 1, checkboxWrapper);
        add(grid);
        clearAllValues();
    }

    protected Button getOkButton() {
        return tagEntryFields.getOkButton();
    }

    protected Button getCancelButton() {
        return tagEntryFields.getCancelButton();
    }

    protected Label getStatusLabel() {
        return tagEntryFields.getStatusLabel();
    }

    protected void validateAndUpdate() {
        tagEntryFields.validateAndUpdate();
    }

    protected TextBox getTagTextBox() {
        return tagEntryFields.getTagTextBox();
    }

    protected TextArea getCommentTextArea() {
        return tagEntryFields.getCommentTextArea();
    }
    
    protected TextArea getHiddenInfoTextArea() {
        return tagEntryFields.getHiddenInfoTextArea();
    }

    protected URLFieldWithFileUpload getImageURLTextBox() {
        return tagEntryFields.getImageUploadPanel();
    }

    protected CheckBox getVisibleForPublicCheckBox() {
        return tagEntryFields.getVisibleForPublicCheckBox();
    }

    protected String getTag() {
        return getTagTextBox().getValue();
    }

    protected String getComment() {
        return getCommentTextArea().getValue();
    }
    
    protected String getHiddenInfo() {
        return getHiddenInfoTextArea().getValue();
    }

    protected boolean isVisibleForPublic() {
        return getVisibleForPublicCheckBox().getValue();
    }

    public String getImageURL() {
        return tagEntryFields.getImageUploadPanel().getUri();
    }

    protected void setTag(String tag) {
        getTagTextBox().setValue(tag);
    }

    protected void setComment(String comment) {
        getCommentTextArea().setValue(comment);
    }
    
    protected void setHiddenInfo(String hiddenInfo) {
        getHiddenInfoTextArea().setValue(hiddenInfo);
    }

    protected void setImageURL(String imageURL) {
        getImageURLTextBox().setUri(imageURL);
    }

    protected void setVisibleForPublic(boolean visibleForPublic) {
        getVisibleForPublicCheckBox().setValue(visibleForPublic);
    }

    /**
     * Clears all input fields.
     */
    protected void clearAllValues() {
        setTag(DEFAULT_TAG);
        setComment(DEFAULT_COMMENT);
        setHiddenInfo(DEFAULT_HIDDEN_INFO);
        setImageURL(DEFAULT_IMAGE_URL);
        setVisibleForPublic(DEFAULT_VISIBLE_FOR_PUBLIC);
        setCurrentStatus();
    }

    /**
     * Returns whether values of given <code>tag</code> are different from input fields.
     * 
     * @param tag
     *            {@link TagDTO} to compare to input fields
     * @return <code>true</code> if title, comment, imageURL and visibility are equal to input fields, otherwise
     *         <code>false</code>
     */
    protected boolean compareFieldsToTag(TagDTO tag) {
        return tag != null && getTag().equals(tag.getTag()) && Util.equalsWithNull(getComment(), tag.getComment()) && Util.equalsWithNull(getHiddenInfo(), tag.getHiddenInfo())
                && (Util.equalsWithNull(getImageURL(), tag.getImageURL())
                        || (getImageURL() == null && tag.getImageURL() == ""))
                && isVisibleForPublic() == tag.isVisibleForPublic();
    }

    /**
     * Returns whether values of given <code>tagButton</code> are different from input fields.
     * 
     * @param tagButton
     *            {@link TagButton} to compare to input fields
     * @return <code>true</code> if title, comment, imageURL and visibility are equal to input fields, otherwise
     *         <code>false</code>
     */
    protected boolean compareFieldsToTagButton(TagButton tagButton) {
        return tagButton != null && getTag().equals(tagButton.getTag()) && Util.equalsWithNull(getComment(), tagButton.getComment())
                && (Util.equalsWithNull(getImageURL(), tagButton.getImageURL())
                        || (getImageURL() == null && tagButton.getImageURL() == ""))
                && isVisibleForPublic() == tagButton.isVisibleForPublic();
    }

    /**
     * Returns whether input fields are equal to default values.
     * 
     * @return <code>true</code> if all input fields are equal to default values, otherwise <code>false</code>
     */
    protected boolean isInputEmpty() {
        return (getTag() == null || getTag().equals(DEFAULT_TAG))
                && (getComment() == null || getComment().equals(DEFAULT_COMMENT))
                && (getImageURL() == null || getImageURL().equals(DEFAULT_IMAGE_URL))
                && isVisibleForPublic() == DEFAULT_VISIBLE_FOR_PUBLIC;
    }

    /**
     * Sets all input fields to values of given {@link TagDTO tag}.
     * 
     * @param tag
     *            tag
     */
    protected void setTag(TagDTO tag) {
        setTag(tag.getTag());
        setComment(tag.getComment());
        setHiddenInfo(tag.getHiddenInfo());
        setImageURL(tag.getImageURL());
        setVisibleForPublic(tag.isVisibleForPublic());
        setCurrentStatus();
    }

    /**
     * Sets visibility of checkbox for visibility of tag. Users without permission for public tag creation will see
     * information label instead.
     */
    protected void setCurrentStatus() {
        if (taggingComponent.hasPermissionToModifyPublicTags()) {
            getVisibleForPublicCheckBox().setVisible(true);
            noPermissionForPublicTagsLabel.setVisible(false);
        } else {
            getVisibleForPublicCheckBox().setVisible(false);
            noPermissionForPublicTagsLabel.setVisible(true);
        }
    }
}