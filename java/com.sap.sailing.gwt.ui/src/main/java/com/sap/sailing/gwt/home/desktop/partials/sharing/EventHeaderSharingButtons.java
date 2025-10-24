package com.sap.sailing.gwt.home.desktop.partials.sharing;

import static com.google.gwt.dom.client.Style.Display.NONE;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.http.client.UrlBuilder;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.common.client.NavigatorUtil;
import com.sap.sailing.gwt.home.shared.partials.shared.SharingMetadataProvider;
import com.sap.sailing.gwt.home.shared.places.ShareablePlaceContext;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.shared.ClientConfiguration;

public class EventHeaderSharingButtons extends Composite {

    private static EventHeaderSharingButtonsUiBinder uiBinder = GWT.create(EventHeaderSharingButtonsUiBinder.class);
    
    private static String SHARING_URL_PREFIX = "/sailingserver/shared/home";

    interface EventHeaderSharingButtonsUiBinder extends UiBinder<Widget, EventHeaderSharingButtons> {
    }
    
    @UiField HTMLPanel htmlPanel;
    @UiField AnchorElement mail;
    @UiField AnchorElement twitter;
    @UiField AnchorElement facebook;
    @UiField Button copyToClipBoard;

    public EventHeaderSharingButtons() {
        EventHeaderSharingButtonsResources.INSTANCE.css().ensureInjected();
        initWidget(uiBinder.createAndBindUi(this));
        if (!ClientConfiguration.getInstance().isBrandingActive()) {
            htmlPanel.getElement().getStyle().setDisplay(NONE);
        } 
    }
    
    public void setUp(SharingMetadataProvider provider) {
        if (!ClientConfiguration.getInstance().isBrandingActive()) {
            return;
        }
        final ShareablePlaceContext context = provider.getContext();
        final String urlToShare = Window.Location.createUrlBuilder()
                .setPath(SHARING_URL_PREFIX + context.getContextAsPathParameters())
                .setHash(null)
                .buildString();
        final String shortText = provider.getShortText();
        final UrlBuilder mailtoLink = new UrlBuilder().setProtocol("mailto").setParameter("subject", shortText).setParameter("body", urlToShare);
        // URLBuilder encodes spaces in parameters using "+" instead of "%20". This causes problems in Mail programs that do not decode "+" as space.
        mail.setHref(mailtoLink.buildString().replace("+", "%20"));
        final UrlBuilder twitterLink = new UrlBuilder().setProtocol("https").setHost("x.com").setPath("intent/tweet").setParameter("text", shortText).setParameter("url", urlToShare).setParameter("short_url_length", "8");
        twitter.setHref(twitterLink.buildString());
        final UrlBuilder facebookLink = new UrlBuilder().setProtocol("https").setHost("www.facebook.com")
                .setPath("sharer/sharer.php").setParameter("u", urlToShare);
        facebook.setHref(facebookLink.buildString());
        if (NavigatorUtil.clientHasNavigatorCopyToClipboardSupport()) {
            copyToClipBoard.removeStyleDependentName("gwt-button");
            copyToClipBoard.removeStyleDependentName("gwt-Button:visited");
            copyToClipBoard.removeStyleName("button");
            copyToClipBoard.addClickHandler(new ClickHandler() {
                @Override
                public void onClick(ClickEvent event) {
                    NavigatorUtil.copyToClipboard(urlToShare);
                    Notification.notify(StringMessages.INSTANCE.sharingLinkCopied(), NotificationType.INFO);
                }
            });
        } else {
            copyToClipBoard.setVisible(false);
        }
    }
}
