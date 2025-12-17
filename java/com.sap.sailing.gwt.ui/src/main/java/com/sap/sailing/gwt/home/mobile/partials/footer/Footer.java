package com.sap.sailing.gwt.home.mobile.partials.footer;

import static com.google.gwt.dom.client.Style.Display.NONE;

import java.util.Optional;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.desktop.places.whatsnew.WhatsNewPlace;
import com.sap.sailing.gwt.home.mobile.app.MobilePlacesNavigator;
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
    
    private ClientConfiguration cfg = ClientConfiguration.getInstance();

    interface FooterPanelUiBinder extends UiBinder<Widget, Footer> {
    }
    
    @UiField Anchor whatsNewLinkUi;
    @UiField AnchorElement supportAnchor;
    @UiField LanguageSelector languageSelector;
    @UiField DivElement copyrightDiv;
    @UiField AnchorElement imprintAnchorLink;
    @UiField AnchorElement desktopUi;
    @UiField AnchorElement jobsAnchor;
    @UiField AnchorElement sourceCodeAnchor;
    @UiField AnchorElement privacyAnchor;
    @UiField SpanElement pipe;
    
    private final MobilePlacesNavigator placeNavigator;

    public Footer(MobilePlacesNavigator placeNavigator) {
        this.placeNavigator = placeNavigator;
        FooterResources.INSTANCE.css().ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));
        placeNavigator.getImprintNavigation().configureAnchorElement(imprintAnchorLink);
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
        if (!ClientConfiguration.getInstance().isBrandingActive()) {
            copyrightDiv.getStyle().setDisplay(NONE);
            languageSelector.setLabelText(StringMessages.INSTANCE.whitelabelFooterLanguage());
            supportAnchor.getStyle().setDisplay(Display.NONE);
            jobsAnchor.getStyle().setDisplay(Display.NONE);
            privacyAnchor.getStyle().setDisplay(Display.NONE);
            sourceCodeAnchor.setHref("https://github.com/SAP/sailing-analytics");
        } else {
            pipe.setInnerText("|");
            if (!hideIfBlank(copyrightDiv, cfg.getFooterCopyright())) {
                copyrightDiv.setInnerText(cfg.getFooterCopyright());
            }
            languageSelector.setLabelText(cfg.getBrandTitle(Optional.empty()) + " " + StringMessages.INSTANCE.whitelabelFooterLanguage());
            setHrefOrHide(privacyAnchor, cfg.getFooterPrivacyLink());
            setHrefOrHide(jobsAnchor, cfg.getFooterJobsLink());
            setHrefOrHide(sourceCodeAnchor, cfg.getGitHubLink());
            setHrefOrHide(supportAnchor, cfg.getFooterSupportLink());
            if (!hideIfBlank(copyrightDiv, cfg.getFooterCopyright())) {
                copyrightDiv.setInnerText(cfg.getFooterCopyright());
            }
        }
    }
    
    @UiHandler("whatsNewLinkUi")
    void onWhatsNew(ClickEvent e) {
        placeNavigator.getWhatsNewNavigation(WhatsNewPlace.WhatsNewNavigationTabs.SailingAnalytics).goToPlace();
    }
    
    private static boolean hideIfBlank(DivElement el, String text) {
        final boolean hidden = !Util.hasLength(text);
        if (hidden) {
            el.getStyle().setDisplay(Display.NONE);
        }
        return hidden;
    }
    
    private static void setHrefOrHide(AnchorElement el, String url) {
        if (!Util.hasLength(url)) {
          el.getStyle().setDisplay(Display.NONE);
        } else {
          el.setHref(url);
        }
    }
}