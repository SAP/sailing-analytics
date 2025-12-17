package com.sap.sailing.gwt.home.mobile.partials.socialfooter;

import static com.sap.sse.gwt.shared.DebugConstants.DEBUG_ID_ATTRIBUTE;

import java.util.Optional;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.HeadingElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.shared.ClientConfiguration;

public class SocialFooter extends Composite {

    interface SocialFooterUiBinder extends UiBinder<Widget, SocialFooter> {
    }
    
    private static SocialFooterUiBinder uiBinder = GWT.create(SocialFooterUiBinder.class);
    
    @UiField HTMLPanel htmlPanel;
    @UiField HeadingElement socialHeading;
    @UiField DivElement xItem;
    @UiField DivElement gitHubItem;
    @UiField DivElement facebookItem;
    @UiField DivElement instagramItem;
    @UiField AnchorElement xLink;
    @UiField AnchorElement gitHubLink;
    @UiField AnchorElement facebookLink;
    @UiField AnchorElement instagramLink;
    @UiField DivElement xTopText;
    @UiField DivElement gitHubTopText;
    @UiField DivElement facebookTopText;
    @UiField DivElement instagramTopText;

    public SocialFooter() {
        ClientConfiguration cfg = ClientConfiguration.getInstance();
        SocialFooterResources.INSTANCE.css().ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));
        if (!cfg.isBrandingActive() || !Util.hasLength(cfg.getFollowSports(Optional.empty()))) {
            htmlPanel.getElement().getStyle().setDisplay(Display.NONE);
        } else {
            socialHeading.setInnerText(cfg.getFollowSports(Optional.empty()));
            setHrefOrHide(xItem, xLink, xTopText, cfg.getxLink(), cfg.getSportsOn(Optional.empty()));
            setHrefOrHide(facebookItem, facebookLink, facebookTopText, cfg.getFacebookLink(), cfg.getSportsOn(Optional.empty()));
            setHrefOrHide(instagramItem, instagramLink, instagramTopText, cfg.getInstagramLink(), cfg.getSportsOn(Optional.empty()));
            setHrefOrHide(gitHubItem, gitHubLink, gitHubTopText, cfg.getGitHubLink(), cfg.getFollowGitHub(Optional.empty()));
        }
        htmlPanel.getElement().setAttribute(DEBUG_ID_ATTRIBUTE, "socialFooter");
    }

    private static void setHrefOrHide(DivElement el1, AnchorElement el2, DivElement topTextElement, String url,
            String text) {
        if (!Util.hasLength(text)) {
            el1.getStyle().setDisplay(Display.NONE);
        } else {
            topTextElement.setInnerText(text);
            el2.setHref(url);
        }
    }
}
