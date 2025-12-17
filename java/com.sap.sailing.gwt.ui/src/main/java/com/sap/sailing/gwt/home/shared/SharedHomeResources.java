package com.sap.sailing.gwt.home.shared;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.resources.client.DataResource.MimeType;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.resources.client.ImageResource.ImageOptions;
import com.sap.sse.gwt.common.CommonIcons;

public interface SharedHomeResources extends CommonIcons {

    public static final SharedHomeResources INSTANCE = GWT.create(SharedHomeResources.class);

    @Source("SharedHome.gss")
    LocalCss sharedHomeCss();

    public interface LocalCss extends CssResource {
        String primary();

        String headerButton();

        String right();

        String label();

        String subTitle();

        String input();

        String inputGroup();

        String buttonGroup();

        String overlay();

        String uploadButton();

        String loading();

        String popup();

        String select();

        @ClassName("progress-overlay")
        String progressOverlay();

        @ClassName("progress-spinner")
        String progressSpinner();
    }

    @Source("default_event_logo.jpg")
    @ImageOptions(preventInlining = true)
    ImageResource defaultEventLogoImage();

    @Source("default_event_photo.jpg")
    @ImageOptions(preventInlining = true)
    ImageResource defaultEventPhotoImage();

    @Source("default_stage_event_teaser.jpg")
    @ImageOptions(preventInlining = true)
    ImageResource defaultStageEventTeaserImage();

    @Source("default_video_preview.jpg")
    @ImageOptions(preventInlining = true)
    ImageResource defaultVideoPreviewImage();

    @Source("arrow-down-grey.png")
    ImageResource arrowDownGrey();

    @Source("arrow-down-white.png")
    ImageResource arrowDownWhite();

    @Source("news-i.png")
    ImageResource news();

    @Source("close.svg")
    @MimeType("image/svg+xml")
    DataResource close();

    @Source("reload.svg")
    @MimeType("image/svg+xml")
    DataResource reload();

    @Source("settings.svg")
    @MimeType("image/svg+xml")
    DataResource settings();

    @Source("fullscreen.svg")
    @MimeType("image/svg+xml")
    DataResource fullscreen();

    @Source("icon-green-check.svg")
    @MimeType("image/svg+xml")
    DataResource greenCheck();

    @Source("icon-red-dash.svg")
    @MimeType("image/svg+xml")
    DataResource redDash();

    @Source("launch-loupe.svg")
    @MimeType("image/svg+xml")
    DataResource launchLoupe();

    @Source("launch-play.svg")
    @MimeType("image/svg+xml")
    DataResource launchPlay();

    @Source("icon-audio.png")
    ImageResource audio();

    @Source("icon-video.png")
    ImageResource video();

    @Source("icon-wind.png")
    ImageResource wind();

    @Source("raw_gps_fixes.png")
    ImageResource gpsFixes();

    @Source("add-media.svg")
    @MimeType("image/svg+xml")
    DataResource addMedia();

    @Source("edit_white.svg")
    @MimeType("image/svg+xml")
    DataResource editWhite();

    @Source("edit_black.svg")
    @MimeType("image/svg+xml")
    DataResource editBlack();

    @Source("trash.svg")
    @MimeType("image/svg+xml")
    DataResource trash();

    @Source("folder_black.svg")
    @MimeType("image/svg+xml")
    DataResource folderBlack();

    @Source("folder_white.svg")
    @MimeType("image/svg+xml")
    DataResource folderWhite();

    @Source("plus.svg")
    @MimeType("image/svg+xml")
    DataResource plus();
    
    @Source("github-white.png")
    ImageResource githubWhite();

}
