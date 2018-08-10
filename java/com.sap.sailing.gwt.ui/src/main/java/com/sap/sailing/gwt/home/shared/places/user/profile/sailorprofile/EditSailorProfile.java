package com.sap.sailing.gwt.home.shared.places.user.profile.sailorprofile;

import java.util.Collection;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.common.client.SharedResources;
import com.sap.sailing.gwt.home.shared.partials.desktopaccordion.DesktopAccordion;
import com.sap.sailing.gwt.home.shared.partials.editable.EditableSuggestedMultiSelectionCompetitor;
import com.sap.sailing.gwt.home.shared.partials.editable.InlineEditLabel;
import com.sap.sailing.gwt.home.shared.partials.listview.BoatClassListView;
import com.sap.sailing.gwt.home.shared.places.user.profile.sailorprofile.domain.ParticipatedEventDTO;
import com.sap.sailing.gwt.home.shared.places.user.profile.sailorprofile.domain.SailorProfileEntry;
import com.sap.sailing.gwt.home.shared.places.user.profile.sailorprofile.events.SailorProfileEventsTable;
import com.sap.sailing.gwt.ui.client.FlagImageResolver;

/**
 * Implementation of {@link SharedSailorProfileView} where users can change their preferred selections and
 * notifications.
 */
public class EditSailorProfile extends Composite implements SharedSailorProfileView {

    private static SharedSailorProfileUiBinder uiBinder = GWT.create(SharedSailorProfileUiBinder.class);

    interface SharedSailorProfileUiBinder extends UiBinder<Widget, EditSailorProfile> {
    }

    interface Style extends CssResource {
        String edgeToEdge();
    }

    @UiField
    Style style;
    @UiField
    SharedResources res;
    @UiField(provided = true)
    EditableSuggestedMultiSelectionCompetitor competitorSelectionUi;
    @UiField
    InlineEditLabel titleUi;
    @UiField
    BoatClassListView boatClassesUi;

    @UiField
    DesktopAccordion accordionEventsUi;
    @UiField
    DesktopAccordion accordionStatisticsUi;
    @UiField
    DesktopAccordion accordionPolarDiagramUi;

    private final SharedSailorProfileView.Presenter presenter;

    public EditSailorProfile(SharedSailorProfileView.Presenter presenter, FlagImageResolver flagImageResolver) {
        this.presenter = presenter;
        competitorSelectionUi = new EditableSuggestedMultiSelectionCompetitor(presenter, flagImageResolver);
        initWidget(uiBinder.createAndBindUi(this));
        boatClassesUi.setText("Boatclasses");
        setupAccordions();
    }

    private void setupAccordions() {
        accordionEventsUi.setTitle("Events");
        accordionStatisticsUi.setTitle("Statistics");
        accordionPolarDiagramUi.setTitle("Polar Diagram");

    }

    public void setEdgeToEdge(boolean edgeToEdge) {
        competitorSelectionUi.setStyleName(style.edgeToEdge(), edgeToEdge);
        competitorSelectionUi.getElement().getParentElement().removeClassName(res.mediaCss().column());
    }

    public void setEntry(SailorProfileEntry entry) {
        competitorSelectionUi.setSelectedItems(entry.getCompetitors());
        titleUi.setText(entry.getName());
        boatClassesUi.setItems(entry.getBoatclasses());

        presenter.getDataProvider().getEvents(entry.getKey(), new AsyncCallback<Collection<ParticipatedEventDTO>>() {

            @Override
            public void onFailure(Throwable caught) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onSuccess(Collection<ParticipatedEventDTO> result) {
                for (ParticipatedEventDTO dto : result) {
                    SailorProfileEventsTable table = new SailorProfileEventsTable();
                    table.setController(presenter.getPlaceController());
                    table.setEvent(dto);
                    table.addStyleName(SharedResources.INSTANCE.mediaCss().column());
                    table.addStyleName(SharedResources.INSTANCE.mediaCss().small12());
                    table.addStyleName(SharedResources.INSTANCE.mainCss().spacermargintopmediumsmall());
                    accordionEventsUi.addWidget(table);
                }
            }
        });
    }
}
