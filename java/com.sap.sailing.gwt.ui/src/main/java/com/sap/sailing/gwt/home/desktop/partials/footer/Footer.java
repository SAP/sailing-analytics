package com.sap.sailing.gwt.home.desktop.partials.footer;

import static com.google.gwt.dom.client.Style.Display.NONE;
import static com.sap.sse.gwt.shared.DebugConstants.DEBUG_ID_ATTRIBUTE;

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
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.google.web.bindery.event.shared.EventBus;
import com.sap.sailing.gwt.home.desktop.app.DesktopPlacesNavigator;
import com.sap.sailing.gwt.home.desktop.places.whatsnew.WhatsNewPlace;
import com.sap.sailing.gwt.home.desktop.places.whatsnew.WhatsNewPlace.WhatsNewNavigationTabs;
import com.sap.sailing.gwt.home.shared.SwitchingEntryPoint;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.client.controls.languageselect.LanguageSelector;
import com.sap.sse.gwt.shared.ClientConfiguration;
import com.sap.sse.gwt.shared.DebugConstants;

public class Footer extends Composite {
    private static FooterPanelUiBinder uiBinder = GWT.create(FooterPanelUiBinder.class);
    private ClientConfiguration cfg = ClientConfiguration.getInstance();

    interface FooterPanelUiBinder extends UiBinder<Widget, Footer> {
    }
    
    @UiField AnchorElement whatsNewAnchor;
    @UiField AnchorElement supportAnchor;
    @UiField LanguageSelector languageSelector;
    @UiField DivElement copyrightDiv;
    @UiField AnchorElement imprintAnchorLink;
    @UiField AnchorElement privacyAnchorLink;
    @UiField AnchorElement mobileUi;
    @UiField AnchorElement jobsAnchor;
    @UiField AnchorElement sourceCodeAnchor;
    @UiField(provided = true)
    final PlaceNavigation<WhatsNewPlace> releaseNotesNavigation;

    public Footer(final DesktopPlacesNavigator navigator, EventBus eventBus) {
        FooterResources.INSTANCE.css().ensureInjected();
        releaseNotesNavigation = navigator.getWhatsNewNavigation(WhatsNewNavigationTabs.SailingAnalytics);
        initWidget(uiBinder.createAndBindUi(this));
        navigator.getImprintNavigation().configureAnchorElement(imprintAnchorLink);
        DOM.sinkEvents(mobileUi, Event.ONCLICK);
        DOM.setEventListener(mobileUi, new EventListener() {
            @Override
            public void onBrowserEvent(Event event) {
                if (event.getTypeInt() == Event.ONCLICK) {
                    event.preventDefault();
                    SwitchingEntryPoint.switchToMobile();
                }
            }
        });
        if (!ClientConfiguration.getInstance().isBrandingActive()) {
            copyrightDiv.getStyle().setDisplay(NONE);
            languageSelector.setLabelText(StringMessages.INSTANCE.whitelabelFooterLanguage());
            supportAnchor.getStyle().setDisplay(Display.NONE);
            privacyAnchorLink.getStyle().setDisplay(Display.NONE);
            jobsAnchor.getStyle().setDisplay(Display.NONE);
            sourceCodeAnchor.setHref("https://github.com/SAP/sailing-analytics");
        } else {
            hideIfBlank(copyrightDiv, cfg.getFooterCopyright());
            setHrefOrHide(privacyAnchorLink, cfg.getFooterPrivacyLink());
            setHrefOrHide(jobsAnchor, cfg.getFooterJobsLink());
            setHrefOrHide(sourceCodeAnchor, cfg.getGitHubLink());
            setHrefOrHide(supportAnchor, cfg.getFooterSupportLink());
            languageSelector.setLabelText(cfg.getBrandTitle(Optional.empty()) + " " + StringMessages.INSTANCE.whitelabelFooterLanguage());
            if (!hideIfBlank(copyrightDiv, cfg.getFooterCopyright())) {
                copyrightDiv.setInnerText(cfg.getFooterCopyright());
            }
        }
        copyrightDiv.setAttribute(DebugConstants.DEBUG_ID_ATTRIBUTE, "copyrightDiv");
        supportAnchor.setAttribute(DEBUG_ID_ATTRIBUTE, "supportAnchor");
        jobsAnchor.setAttribute(DEBUG_ID_ATTRIBUTE, "jobsAnchor");
        sourceCodeAnchor.setAttribute(DEBUG_ID_ATTRIBUTE, "sourceCodeAnchor");
        whatsNewAnchor.setAttribute(DEBUG_ID_ATTRIBUTE, "whatsNewAnchor");
        imprintAnchorLink.setAttribute(DEBUG_ID_ATTRIBUTE, "imprintAnchorLink");
        privacyAnchorLink.setAttribute(DEBUG_ID_ATTRIBUTE, "privacyAnchorLink");
        languageSelector.getElement().setAttribute(DEBUG_ID_ATTRIBUTE, "languageSelector");
    }
    
    private static boolean hideIfBlank(DivElement el, String text) {
        boolean flag = false;
        if (!Util.hasLength(text)) {
            el.getStyle().setDisplay(Display.NONE);
            flag = true;
        }
        return flag;
    }
    
    private static void setHrefOrHide(AnchorElement el, String url) {
        if (!Util.hasLength(url)) {
          el.getStyle().setDisplay(Display.NONE);
        } else if (!url.equals("nothing")) {
          el.setHref(url);
        }
    }
    
}