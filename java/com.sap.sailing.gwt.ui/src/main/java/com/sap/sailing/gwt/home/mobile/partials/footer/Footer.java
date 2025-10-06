package com.sap.sailing.gwt.home.mobile.partials.footer;

import static com.google.gwt.dom.client.Style.Display.NONE;

import java.util.Optional;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.shared.SwitchingEntryPoint;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.controls.languageselect.LanguageSelector;
import com.sap.sse.gwt.shared.ClientConfiguration;

/**
 * Mobile page footer with several links and the ability to switch the language.
 */
public class Footer extends Composite {
    private static FooterPanelUiBinder uiBinder = GWT.create(FooterPanelUiBinder.class);
    ClientConfiguration cfg = ClientConfiguration.getInstance();

    interface FooterPanelUiBinder extends UiBinder<Widget, Footer> {
    }
    
    @UiField Anchor whatsNewLinkUi;
    @UiField AnchorElement supportAnchor;
    @UiField LanguageSelector languageSelector;
    @UiField DivElement copyrightDiv;
    @UiField AnchorElement imprintAnchorLink;
    @UiField AnchorElement desktopUi;
    @UiField AnchorElement jobsAnchor;
    @UiField AnchorElement privacyAnchorLink;

    public Footer() {
        FooterResources.INSTANCE.css().ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));
        DOM.sinkEvents(desktopUi, Event.ONCLICK);
        DOM.setEventListener(desktopUi, new EventListener() {
            @Override
            public void onBrowserEvent(Event event) {
                if (event.getTypeInt() == Event.ONCLICK) {
                    event.preventDefault();
                    SwitchingEntryPoint.switchToDesktop();
                }
            }
        });
        if (!cfg.isBrandingActive()) {
            copyrightDiv.getStyle().setDisplay(NONE);
            languageSelector.setLabelText(StringMessages.INSTANCE.whitelabelFooterLanguage());
            supportAnchor.getStyle().setDisplay(Display.NONE);
            whatsNewLinkUi.getElement().getStyle().setDisplay(Display.NONE);
            imprintAnchorLink.getStyle().setDisplay(Display.NONE);
            jobsAnchor.getStyle().setDisplay(Display.NONE);
            privacyAnchorLink.getStyle().setDisplay(NONE);
        } else {
            languageSelector.setLabelText(cfg.getBrandTitle(Optional.empty()) + " " + StringMessages.INSTANCE.whitelabelFooterLanguage());
            setHrefOrHide(privacyAnchorLink, cfg.getFooterPrivacyLink());
            setHrefOrHide(jobsAnchor, cfg.getFooterJobsLink());
            setHrefOrHide(supportAnchor, cfg.getFooterSupportLink());
            setHrefOrHide(imprintAnchorLink, cfg.getFooterLegalLink());
            if (!Util.hasLength(cfg.getFooterWhatsNewLink())) {
                whatsNewLinkUi.getElement().getStyle().setDisplay(Display.NONE);
            } else {
                whatsNewLinkUi.setHref(cfg.getFooterWhatsNewLink());
            }
            if (!hideIfBlank(copyrightDiv, cfg.getFooterCopyright())) {
                copyrightDiv.setInnerText(cfg.getFooterCopyright());
            }
        }
    }
    
    private static boolean hideIfBlank(DivElement el, String text) {
        final boolean result;
        if (!Util.hasLength(text)) {
            el.getStyle().setDisplay(Display.NONE);
            result = true;
        } else {
            result = false;
        }
        return result;
    }
    
    private static void setHrefOrHide(AnchorElement el, String url) {
        if (!Util.hasLength(url)) {
            el.getStyle().setDisplay(Display.NONE);
        } else {
            el.setHref(url);
        }
    }
}
