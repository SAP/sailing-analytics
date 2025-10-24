package com.sap.sse.gwt.common;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.DataResource;
import com.google.gwt.resources.client.DataResource.MimeType;
import com.google.gwt.resources.client.ImageResource;

public interface CommonIcons extends ClientBundle {

    @Source("facebook-logo.svg")
    @MimeType("image/svg+xml")
    DataResource facebook();

    @Source("x-logo.svg")
    @MimeType("image/svg+xml")
    DataResource twitter();

    @Source("google.svg")
    @MimeType("image/svg+xml")
    DataResource google();

    @Source("search-icon.svg")
    @MimeType("image/svg+xml")
    DataResource search();

    @Source("instagram.svg")
    @MimeType("image/svg+xml")
    DataResource instagram();

    @Source("instagram_inverse.svg")
    @MimeType("image/svg+xml")
    DataResource instagramInverse();
    
    @Source("email-icon.png")
    ImageResource email();

    @Source("dropdown__check@2x.png")
    ImageResource dropdownCheck();
    
    @Source("arrow-down-filled-black.png")
    ImageResource arrowDownFilledBlack();
    
    @Source("arrow-down-filled-white.png")
    ImageResource arrowDownFilledWhite();
}
