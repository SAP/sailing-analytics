package com.sap.sailing.gwt.home.shared.partials.editable;

import com.sap.sailing.gwt.home.communication.event.SimpleCompetitorWithIdDTO;
import com.sap.sailing.gwt.home.shared.partials.multiselection.HeadlessSuggestedMultiSelection;
import com.sap.sailing.gwt.home.shared.partials.multiselection.SuggestedMultiSelection;
import com.sap.sailing.gwt.home.shared.partials.multiselection.SuggestedMultiSelectionCompetitorItemDescription;
import com.sap.sailing.gwt.home.shared.partials.multiselection.SuggestedMultiSelectionPresenter;
import com.sap.sailing.gwt.home.shared.partials.multiselection.SuggestedMultiSelectionView;
import com.sap.sailing.gwt.home.shared.places.user.profile.sailorprofile.EditSailorProfileView;
import com.sap.sailing.gwt.ui.client.FlagImageResolver;
import com.sap.sailing.gwt.ui.client.StringMessages;

public class EditableSuggestedMultiSelectionCompetitor
        extends EditableSuggestedMultiSelection<SimpleCompetitorWithIdDTO> {

    public EditableSuggestedMultiSelectionCompetitor(final EditSailorProfileView.Presenter presenter,
            final FlagImageResolver flagImageResolver) {
        this(presenter, flagImageResolver, false);
    }

    public EditableSuggestedMultiSelectionCompetitor(final EditSailorProfileView.Presenter presenter,
            final FlagImageResolver flagImageResolver, final boolean headless) {
        super(competitor -> new SuggestedMultiSelectionCompetitorItemDescription(competitor, flagImageResolver),
                new CompetitorDisplayImpl(presenter.getDataProvider(), flagImageResolver, headless).selectionUi,
                presenter.getDataProvider(), headless);
        super.setText(i18n.competitors());
    }

    private static class CompetitorDisplayImpl
            implements SuggestedMultiSelectionPresenter.Display<SimpleCompetitorWithIdDTO> {
        private final SuggestedMultiSelectionView<SimpleCompetitorWithIdDTO> selectionUi;

        private CompetitorDisplayImpl(
                final SuggestedMultiSelectionPresenter<SimpleCompetitorWithIdDTO, SuggestedMultiSelectionPresenter.Display<SimpleCompetitorWithIdDTO>> dataProvider,
                FlagImageResolver flagImageResolver, boolean headless) {
            if (headless) {
                selectionUi = HeadlessSuggestedMultiSelection.forCompetitors(dataProvider,
                        StringMessages.INSTANCE.competitors(), flagImageResolver);
            } else {
                selectionUi = SuggestedMultiSelection.forCompetitors(dataProvider,
                        StringMessages.INSTANCE.competitors(), flagImageResolver);
            }
            dataProvider.addDisplay(this);
        }

        @Override
        public void setSelectedItems(Iterable<SimpleCompetitorWithIdDTO> selectedItems) {
            selectionUi.setSelectedItems(selectedItems);
        }
    }

}
