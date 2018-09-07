package com.sap.sailing.gwt.home.mobile.places.user.profile.sailorprofiles.details;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Display;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.dto.BoatClassDTO;
import com.sap.sailing.gwt.common.client.BoatClassImageResolver;
import com.sap.sailing.gwt.home.communication.event.SimpleCompetitorWithIdDTO;
import com.sap.sailing.gwt.home.communication.user.profile.domain.ParticipatedEventDTO;
import com.sap.sailing.gwt.home.communication.user.profile.domain.SailorProfileDTO;
import com.sap.sailing.gwt.home.communication.user.profile.domain.SailorProfileEventsDTO;
import com.sap.sailing.gwt.home.communication.user.profile.domain.SailorProfileNumericStatisticType;
import com.sap.sailing.gwt.home.communication.user.profile.domain.SailorProfileStatisticDTO;
import com.sap.sailing.gwt.home.desktop.places.user.profile.sailorprofiletab.SailingProfileOverviewPresenter;
import com.sap.sailing.gwt.home.desktop.places.user.profile.sailorprofiletab.SailorProfileView;
import com.sap.sailing.gwt.home.mobile.partials.sectionHeader.SectionHeaderContent;
import com.sap.sailing.gwt.home.mobile.partials.sectionHeader.SectionHeaderContent.AccordionExpansionListener;
import com.sap.sailing.gwt.home.mobile.partials.sectionHeader.SectionHeaderContent.InitialAccordionExpansionListener;
import com.sap.sailing.gwt.home.mobile.places.user.profile.sailorprofiles.SailorProfileMobileResources;
import com.sap.sailing.gwt.home.mobile.places.user.profile.sailorprofiles.details.events.SailorProfileEventEntry;
import com.sap.sailing.gwt.home.mobile.places.user.profile.sailorprofiles.details.statistics.SailorProfileStatisticTable;
import com.sap.sailing.gwt.home.shared.partials.editable.EditableSuggestedMultiSelectionCompetitor;
import com.sap.sailing.gwt.home.shared.partials.editable.InlineEditButton;
import com.sap.sailing.gwt.home.shared.partials.editable.InlineEditLabel;
import com.sap.sailing.gwt.home.shared.places.user.profile.sailorprofile.EditSailorProfileView;
import com.sap.sailing.gwt.home.shared.places.user.profile.sailorprofile.SharedSailorProfileResources;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.security.ui.authentication.app.NeedsAuthenticationContext;

/**
 * Implementation of {@link SailorProfileView} and {@link EditSailorProfileView} where users can change the details of
 * the sailor profiles.
 */
public class SailorProfilesDetailsImpl extends Composite implements SailorProfileView, EditSailorProfileView {

    private static MyUiBinder uiBinder = GWT.create(MyUiBinder.class);
    private static StringMessages stringMessages = GWT.create(StringMessages.class);

    interface MyUiBinder extends UiBinder<Widget, SailorProfilesDetailsImpl> {
    }

    @UiField
    InlineEditLabel profileTitleUi;

    @UiField
    FlowPanel contentUi;

    @UiField
    SectionHeaderContent competitorsUi;

    @UiField
    HTMLPanel contentContainerCompetitorsUi;
    @UiField
    SectionHeaderContent boatclassesUi;

    @UiField
    HTMLPanel contentContainerBoatclassesUi;
    @UiField
    SectionHeaderContent eventsUi;

    @UiField
    HTMLPanel contentContainerEventsUi;
    @UiField
    SectionHeaderContent statisticsUi;

    @UiField
    HTMLPanel contentContainerStatisticsUi;

    private SailingProfileOverviewPresenter presenter;

    private EditableSuggestedMultiSelectionCompetitor editableSuggestedMultiselect;

    public SailorProfilesDetailsImpl() {
        initWidget(uiBinder.createAndBindUi(this));
        SharedSailorProfileResources.INSTANCE.css().ensureInjected();
        SailorProfileMobileResources.INSTANCE.css().ensureInjected();
        competitorsUi.initCollapsibility(contentContainerCompetitorsUi.getElement(), false);
        boatclassesUi.initCollapsibility(contentContainerBoatclassesUi.getElement(), false);
        eventsUi.initCollapsibility(contentContainerEventsUi.getElement(), false);
        statisticsUi.initCollapsibility(contentContainerStatisticsUi.getElement(), false);
    }

    @Override
    public void setPresenter(SailingProfileOverviewPresenter presenter) {
        this.presenter = presenter;
        presenter.getSharedSailorProfilePresenter().getDataProvider().setView(this);
        createMultiSelection();
        setupTitleChangeHandler();
    }

    @Override
    public NeedsAuthenticationContext authentificationContextConsumer() {
        return null;
    }

    @Override
    public void setEntry(SailorProfileDTO entry) {
        profileTitleUi.setText(entry.getName());

        setCompetitors(entry.getCompetitors());
        setBoatclasses(entry.getBoatclasses());

        // Get events
        presenter.getSharedSailorProfilePresenter().getDataProvider().getEvents(entry.getKey(),
                new AsyncCallback<SailorProfileEventsDTO>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        GWT.log(caught.getMessage(), caught);
                    }

                    @Override
                    public void onSuccess(SailorProfileEventsDTO result) {
                        setEvents(result.getParticipatedEvents());
                    }

                });

        statisticsUi.addAccordionListener(new InitialAccordionExpansionListener() {
            @Override
            public void onFirstExpansion() {
                // Get statistics
                for (SailorProfileNumericStatisticType type : SailorProfileNumericStatisticType.values()) {
                    presenter.getSharedSailorProfilePresenter().getDataProvider().getStatisticFor(entry.getKey(), type,
                            new AsyncCallback<SailorProfileStatisticDTO>() {
                                @Override
                                public void onFailure(Throwable caught) {
                                    GWT.log(caught.getMessage(), caught);
                                }

                                @Override
                                public void onSuccess(SailorProfileStatisticDTO result) {
                                    setStatistic(type, result);
                                }
                            });
                }
            }
        });
    }

    private void setupTitleChangeHandler() {
        profileTitleUi.addTextChangeHandler(
                text -> presenter.getSharedSailorProfilePresenter().getDataProvider().updateTitle(text));
    }

    private void createMultiSelection() {
        editableSuggestedMultiselect = new EditableSuggestedMultiSelectionCompetitor(
                presenter.getSharedSailorProfilePresenter().getDataProvider(), presenter.getFlagImageResolver(), true);
        contentContainerCompetitorsUi.add(editableSuggestedMultiselect);
        final InlineEditButton editButton = editableSuggestedMultiselect.getEditButton();
        editButton.setVisible(false);
        editButton.getElement().getStyle().setTop(-0.5, Unit.EM);
        editButton.getElement().getStyle().setRight(3.1, Unit.EM);
        competitorsUi.setHeaderElement(editButton);

        competitorsUi.addAccordionListener(new AccordionExpansionListener() {
            @Override
            public void onExpansionChange(boolean collapsed) {
                editButton.setVisible(!collapsed);
            }
        });
    }

    private void setCompetitors(Iterable<SimpleCompetitorWithIdDTO> competitors) {
        editableSuggestedMultiselect.setSelectedItems(competitors);
    }

    private void setBoatclasses(Iterable<BoatClassDTO> boatclasses) {
        contentContainerBoatclassesUi.getElement().removeAllChildren();
        for (BoatClassDTO boatclass : boatclasses) {
            Element elem = DOM.createDiv();
            elem.setInnerSafeHtml(SharedSailorProfileResources.TEMPLATES.buildBoatclassIcon(
                    BoatClassImageResolver.getBoatClassIconResource(boatclass.getName()).getSafeUri().asString()));
            elem.getStyle().setDisplay(Display.INLINE_BLOCK);
            contentContainerBoatclassesUi.getElement().appendChild(elem);
        }
    }

    private void setEvents(Iterable<ParticipatedEventDTO> participatedEvents) {
        contentContainerEventsUi.clear();
        for (ParticipatedEventDTO event : participatedEvents) {
            contentContainerEventsUi.add(
                    new SailorProfileEventEntry(event, presenter.getSharedSailorProfilePresenter().getPlaceController(),
                            presenter.getFlagImageResolver()));
        }
    }

    private void setStatistic(SailorProfileNumericStatisticType type, SailorProfileStatisticDTO statistic) {
        contentContainerStatisticsUi.clear();
        contentContainerStatisticsUi.add(
                new SailorProfileStatisticTable(type, statistic, presenter.getFlagImageResolver(), stringMessages));
    }
}
