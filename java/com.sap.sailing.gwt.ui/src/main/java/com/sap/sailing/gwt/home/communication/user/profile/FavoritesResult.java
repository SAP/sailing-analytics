package com.sap.sailing.gwt.home.communication.user.profile;

import com.sap.sse.gwt.dispatch.shared.commands.Result;

public class FavoritesResult implements Result {
    
    private FavoriteBoatClassesDTO favoriteBoatClasses;
    private FavoriteCompetitorsDTO favoriteCompetitors;
    private boolean didOptOutOfFeatureAndCommunityEmails;

    protected FavoritesResult() {}
    
    public FavoritesResult(FavoriteBoatClassesDTO favoriteBoatClasses, FavoriteCompetitorsDTO favoriteCompetitors, boolean didOptOutOfFeatureAndCommunityEmails) {
        this.favoriteBoatClasses = favoriteBoatClasses;
        this.favoriteCompetitors = favoriteCompetitors;
        this.didOptOutOfFeatureAndCommunityEmails = didOptOutOfFeatureAndCommunityEmails;
    }
    
    public FavoriteBoatClassesDTO getFavoriteBoatClasses() {
        return favoriteBoatClasses;
    }
    
    public FavoriteCompetitorsDTO getFavoriteCompetitors() {
        return favoriteCompetitors;
    }

    public boolean getDidOptOutOfFeatureAndCommunityEmails() {
        return didOptOutOfFeatureAndCommunityEmails;
    }
}
