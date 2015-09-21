package com.sap.sailing.gwt.home.desktop.places.solutions;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.sap.sailing.gwt.common.client.i18n.TextMessages;

public class SolutionsActivity extends AbstractActivity {
    private final SolutionsPlace solutionsPlace;
    private final SolutionsClientFactory clientFactory;
    
    public SolutionsActivity(SolutionsPlace place, SolutionsClientFactory clientFactory) {
        this.solutionsPlace = place;
        this.clientFactory = clientFactory;
    }

    @Override
    public void start(AcceptsOneWidget panel, EventBus eventBus) {
        SolutionsView solutionsView = clientFactory.createSolutionsView(solutionsPlace.getNavigationTab());
        panel.setWidget(solutionsView.asWidget());
        Window.setTitle(TextMessages.INSTANCE.sapSailing() + " - " + TextMessages.INSTANCE.solutions());
    }
}
