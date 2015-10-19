package com.sap.sailing.gwt.home.desktop.places.searchresult;

import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;
import com.sap.sailing.gwt.common.client.i18n.TextMessages;
import com.sap.sailing.gwt.home.shared.app.HasMobileVersion;

public class SearchResultPlace extends Place implements HasMobileVersion {
    private final String searchText;
    
    public SearchResultPlace(String searchText) {
        super();
        this.searchText = searchText;
    }

    public String getTitle() {
        return TextMessages.INSTANCE.sapSailing() + " - " + TextMessages.INSTANCE.search();
    }
    
    public String getSearchText() {
        return searchText;
    }

    public static class Tokenizer implements PlaceTokenizer<SearchResultPlace> {
        @Override
        public String getToken(SearchResultPlace place) {
            return place.getSearchText();
        }

        @Override
        public SearchResultPlace getPlace(String searchQuery) {
            return new SearchResultPlace(searchQuery);
        }
    }
}
