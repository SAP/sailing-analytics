package com.sap.sailing.gwt.common.client.help;

import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;

public class HelpButton extends Composite {
    private final Image icon;
    private final HelpButtonPopup popup;

    public HelpButton(final HelpButtonResources resources, final String description, final String url, int place) {
    	// 0 = AdminConsole
    	// 1 = DataMining
    	// Default = AdmiConsole
    	if(place == 0)
    	    resources.style().ensureInjected();
    	if(place == 1)
    	    resources.style2().ensureInjected();
    	else
    	    resources.style().ensureInjected();
		this.icon = new Image(resources.icon());
		this.icon.addStyleName(resources.style().icon());
		this.popup = new HelpButtonPopup(resources, description, url, place);
		this.icon.addClickHandler(event -> popup.showRelativeTo(icon));
		initWidget(icon);
		this.setTitle(description);
        this.setDescription(description);
        this.setLinkUrl(url);
        this.getElement().getStyle().setCursor(Cursor.POINTER); 
    }

    public void setDescription(final String description) {
        this.popup.textUi.setInnerText(description);
    }

    public void setLinkUrl(final String url) {
        this.popup.linkUi.setHref(url);
    }
}
