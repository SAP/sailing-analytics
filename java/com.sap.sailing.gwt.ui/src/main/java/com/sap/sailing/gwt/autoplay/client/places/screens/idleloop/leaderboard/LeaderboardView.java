package com.sap.sailing.gwt.autoplay.client.places.screens.idleloop.leaderboard;

import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.sap.sailing.gwt.autoplay.client.shared.leaderboard.LeaderboardWithHeaderPerspective;

public interface LeaderboardView {

    public interface Presenter {
    }

    void startingWith(Presenter p, AcceptsOneWidget panel,
            LeaderboardWithHeaderPerspective leaderboardWithHeaderPerspective);

    void onStop();
}
