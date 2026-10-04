package com.sap.sailing.gwt.home.shared.places.user.profile.preferences;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.common.client.SharedResources;
import com.sap.sailing.gwt.home.shared.partials.labeledbox.LabeledBox;
import com.sap.sailing.gwt.home.shared.partials.multiselection.BoatClassDisplayImpl;
import com.sap.sailing.gwt.home.shared.partials.multiselection.CompetitorDisplayImpl;
import com.sap.sailing.gwt.home.shared.partials.multiselection.MiscellaneousDisplayImpl;
import com.sap.sailing.gwt.ui.client.FlagImageResolver;

/**
 * Implementation of {@link UserPreferencesView} where users can change their preferred selections and notifications.
 */
public class UserPreferences extends Composite implements UserPreferencesView {

    private static UserPreferencesUiBinder uiBinder = GWT.create(UserPreferencesUiBinder.class);

    interface UserPreferencesUiBinder extends UiBinder<Widget, UserPreferences> {
    }

    interface Style extends CssResource {
        String edgeToEdge();
    }

    @UiField
    Style style;
    @UiField
    SharedResources res;
    @UiField(provided = true)
    LabeledBox favoriteCompetitorsSelctionUi;
    @UiField(provided = true)
    LabeledBox favoriteBoatClassesSelctionUi;
    @UiField(provided = true)
    LabeledBox miscUi;
    @UiField
    DivElement notificationsTextUi;
    final UserPreferencesView.Presenter presenter;

    public UserPreferences(UserPreferencesView.Presenter presenter, FlagImageResolver flagImageResolver) {
        this.presenter = presenter;
        favoriteCompetitorsSelctionUi = new CompetitorDisplayImpl(presenter.getFavoriteCompetitorsDataProvider(),
                flagImageResolver).selectionUi;
        favoriteBoatClassesSelctionUi = new BoatClassDisplayImpl(
                presenter.getFavoriteBoatClassesDataProvider()).selectionUi;
        miscUi = (new MiscellaneousDisplayImpl(presenter.getMiscPresenter())).selectionUi;
        initWidget(uiBinder.createAndBindUi(this));
        // TODO hide notificationsTextUi if the user's mail address is already verified
    }

    public void setEdgeToEdge(boolean edgeToEdge) {
        favoriteCompetitorsSelctionUi.setStyleName(style.edgeToEdge(), edgeToEdge);
        favoriteCompetitorsSelctionUi.getElement().getParentElement().removeClassName(res.mediaCss().column());
    }
}
