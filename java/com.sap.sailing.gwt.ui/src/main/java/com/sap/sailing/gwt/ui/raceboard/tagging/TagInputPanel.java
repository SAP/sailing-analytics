package com.sap.sailing.gwt.ui.raceboard.tagging;

import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.sap.sailing.domain.common.dto.TagDTO;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.raceboard.tagging.TagPanelResources.TagPanelStyle;

/**
 * Panel containing input fields for tag/tag button creation and modification.
 */
// TODO: use DataEntryDialog
public class TagInputPanel extends FlowPanel {

    private final TagPanelStyle style = TagPanelResources.INSTANCE.style();

    // default value for "Visible for public" checkbox
    private static final String DEFAULT_TAG = "";
    private static final String DEFAULT_COMMENT = "";
    private static final String DEFAULT_IMAGE_URL = "";
    private static final boolean DEFAULT_VISIBLE_FOR_PUBLIC = false;

    private final TextBox tagTextBox;
    private final TagImageUploaderPanel imageUploadPanel;
    private final TextArea commentTextArea;
    private final SimplePanel checkboxWrapper;
    private final CheckBox visibleForPublicCheckBox;
    private final Label noPermissionForPublicTagsLabel;

    private final TaggingPanel taggingPanel;
    private final StringMessages stringMessages;
    private final SailingServiceAsync sailingService;

    /**
     * Creates view allowing users to input values for tags and {@link TagButton tag-buttons}.
     */
    protected TagInputPanel(TaggingPanel taggingPanel, SailingServiceAsync sailingService) {
        setStyleName(style.tagInputPanel());
        this.taggingPanel = taggingPanel;
        this.stringMessages = taggingPanel.getStringMessages();
        this.sailingService = sailingService;

        tagTextBox = new TextBox();
        tagTextBox.setStyleName(style.tagInputPanelTag());
        tagTextBox.setTitle(stringMessages.tagLabelTag());
        tagTextBox.getElement().setPropertyString("placeholder", stringMessages.tagLabelTag());
        add(tagTextBox);

        imageUploadPanel = new TagImageUploaderPanel(taggingPanel, sailingService);
        add(imageUploadPanel);

        commentTextArea = new TextArea();
        commentTextArea.setStyleName(style.tagInputPanelComment());
        commentTextArea.setVisibleLines(4);
        commentTextArea.setTitle(stringMessages.tagLabelComment());
        commentTextArea.getElement().setPropertyString("placeholder", stringMessages.tagLabelComment());
        add(commentTextArea);

        checkboxWrapper = new SimplePanel();
        checkboxWrapper.setStyleName(style.tagInputPanelIsVisibleForPublic());
        visibleForPublicCheckBox = new CheckBox(stringMessages.tagVisibleForPublicCheckBox());
        visibleForPublicCheckBox.setValue(DEFAULT_VISIBLE_FOR_PUBLIC);
        checkboxWrapper.setWidget(visibleForPublicCheckBox);
        add(checkboxWrapper);

        noPermissionForPublicTagsLabel = new Label(stringMessages.tagPublicModificationPermissionMissing());
        noPermissionForPublicTagsLabel.setStyleName(style.tagInputPanelNoPermissionLabel());
        add(noPermissionForPublicTagsLabel);
        clearAllValues();
    }

    public String getImageURL() {
        return imageUploadPanel.getImageURL();
    }

    public int getImageWidth() {
        return imageUploadPanel.getImageWidth();
    }

    public int getImageHeight() {
        return imageUploadPanel.getImageHeight();
    }

    /**
     * Clears all input fields.
     */
    protected void clearAllValues() {
        setTag(DEFAULT_TAG);
        setComment(DEFAULT_COMMENT);
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
        return getTag().equals(tag.getTag()) && getComment().equals(tag.getComment())
                && getImageURL().equals(tag.getImageURL()) && isVisibleForPublic() == tag.isVisibleForPublic();
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
        return getTag().equals(tagButton.getTag()) && getComment().equals(tagButton.getComment())
                && getImageURL().equals(tagButton.getImageURL())
                && isVisibleForPublic() == tagButton.isVisibleForPublic();
    }

    /**
     * Returns whether input fields are equal to default values.
     * 
     * @return <code>true</code> if all input fields are equal to default values, otherwise <code>false</code>
     */
    protected boolean isInputEmpty() {
        return getTag().equals(DEFAULT_TAG) && getComment().equals(DEFAULT_COMMENT)
                && getImageURL().equals(DEFAULT_IMAGE_URL) && isVisibleForPublic() == DEFAULT_VISIBLE_FOR_PUBLIC;
    }

    protected String getTag() {
        return tagTextBox.getValue();
    }

    protected String getComment() {
        return commentTextArea.getValue();
    }

    protected boolean isVisibleForPublic() {
        return visibleForPublicCheckBox.getValue();
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
        setImageURL(tag.getImageURL());
        setVisibleForPublic(tag.isVisibleForPublic());
        setCurrentStatus();
    }

    protected void setTag(String tag) {
        tagTextBox.setValue(tag);
    }

    protected void setComment(String comment) {
        commentTextArea.setValue(comment);
    }

    protected void setImageURL(String imageURL) {
        imageUploadPanel.setValue(imageURL);
    }

    protected void setVisibleForPublic(boolean visibleForPublic) {
        visibleForPublicCheckBox.setValue(visibleForPublic);
    }

    protected TextBox getTagTextBox() {
        return tagTextBox;
    }

    protected TagImageUploaderPanel getImageURLTextBox() {
        return imageUploadPanel;
    }

    protected TextArea getCommentTextArea() {
        return commentTextArea;
    }

    protected CheckBox getVisibleForPublicCheckBox() {
        return visibleForPublicCheckBox;
    }

    protected void setCurrentStatus() {
        if (taggingPanel.hasPermissionToModifyPublicTags()) {
            checkboxWrapper.setVisible(true);
            noPermissionForPublicTagsLabel.setVisible(false);
        } else {
            checkboxWrapper.setVisible(false);
            noPermissionForPublicTagsLabel.setVisible(true);
        }
    }
}