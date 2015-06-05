package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.ui.Label;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.media.VideoDTO;

public class VideoEditDialog extends VideoDialog {
    public VideoEditDialog(VideoDTO video, StringMessages stringMessages, DialogCallback<VideoDTO> callback) {
        super(new VideoParameterValidator(stringMessages), stringMessages, callback);
        createdAtLabel = new Label(video.getCreatedAtDate().toString());
        videoURLAndUploadComposite.setURL(video.getSourceRef());
        titleTextBox = createTextBox(video.getTitle());
        titleTextBox.setVisibleLength(50);
        subtitleTextBox = createTextBox(video.getSubtitle());
        subtitleTextBox.setVisibleLength(50);
        copyrightTextBox = createTextBox(video.getCopyright());
        copyrightTextBox.setVisibleLength(50);
        lengthIntegerBox = createIntegerBox(video.getLengthInSeconds(), 10);
        List<String> tags = new ArrayList<String>();
        tags.addAll(video.getTags());
        tagsListEditor.setValue(tags);
    }
}
