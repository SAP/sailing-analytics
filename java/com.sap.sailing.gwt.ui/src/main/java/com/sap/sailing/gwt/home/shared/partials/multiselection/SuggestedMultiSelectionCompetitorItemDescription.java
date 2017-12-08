package com.sap.sailing.gwt.home.shared.partials.multiselection;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.home.communication.event.SimpleCompetitorDTO;
import com.sap.sailing.gwt.ui.client.FlagImageResolver;

class SuggestedMultiSelectionCompetitorItemDescription extends Widget {

    private static LocalUiBinder uiBinder = GWT.create(LocalUiBinder.class);

    interface LocalUiBinder extends UiBinder<Element, SuggestedMultiSelectionCompetitorItemDescription> {
    }
    
    @UiField DivElement flagImageUi;
    @UiField SpanElement sailIdUi;
    @UiField SpanElement nameUi;
    
    SuggestedMultiSelectionCompetitorItemDescription(SimpleCompetitorDTO competitor, FlagImageResolver flagImageResolver) {
        setElement(uiBinder.createAndBindUi(this));
        flagImageUi.getStyle().setBackgroundImage("url('" + flagImageResolver.getFlagImageUri(
                competitor.getFlagImageURL(), competitor.getTwoLetterIsoCountryCode()).asString() + "')");
        sailIdUi.setInnerText(competitor.getShortInfo());
        nameUi.setInnerText(competitor.getName());
    }
    
}
