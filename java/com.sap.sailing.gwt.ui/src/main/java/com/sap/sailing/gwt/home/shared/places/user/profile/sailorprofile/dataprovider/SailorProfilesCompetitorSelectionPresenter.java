package com.sap.sailing.gwt.home.shared.places.user.profile.sailorprofile.dataprovider;

import java.util.Collection;
import java.util.UUID;

import com.sap.sailing.gwt.home.communication.event.SimpleCompetitorWithIdDTO;
import com.sap.sailing.gwt.home.shared.partials.editable.EditableSuggestedMultiSelection.EditModeChangeHandler;
import com.sap.sailing.gwt.home.shared.partials.multiselection.AbstractSuggestedCompetitorMultiSelectionPresenter;
import com.sap.sailing.gwt.home.shared.partials.multiselection.SuggestedMultiSelection;
import com.sap.sailing.gwt.home.shared.partials.multiselection.SuggestedMultiSelectionPresenter;

/**
 * This presenter functions as an adapter from the {@link AbstractSuggestedCompetitorMultiSelectionPresenter} to the
 * {@link SailorProfileDataProvider}. It handles competitor selection events and functions as a change handler for edit
 * mode change events. <br/>
 * <br/>
 * This presenter holds the uuid of the currently selected sailor profile and the competitors which are contained in
 * this sailor profile. Storing/Loading of these competitors is done in {@link SailorProfileDataProvider}, which also
 * sets the competitors via {@link #setCompetitorsAndUUID(Collection, UUID)}. <br/>
 * <br/>
 * The {@link #competitorDataProvider} is used as an adapter to the displayed {@link SuggestedMultiSelection} in the
 * sailor profiles.
 */
public class SailorProfilesCompetitorSelectionPresenter implements EditModeChangeHandler,
        SuggestedMultiSelectionPresenter<SimpleCompetitorWithIdDTO, SuggestedMultiSelectionPresenter.Display<SimpleCompetitorWithIdDTO>> {

    private final AbstractSuggestedCompetitorMultiSelectionPresenter<Display<SimpleCompetitorWithIdDTO>> competitorDataProvider;

    private Collection<SimpleCompetitorWithIdDTO> competitors;
    private UUID uuid;

    private final SailorProfileDataProvider sailorProfileDataProvider;

    public SailorProfilesCompetitorSelectionPresenter(
            AbstractSuggestedCompetitorMultiSelectionPresenter<Display<SimpleCompetitorWithIdDTO>> competitorDataProvider,
            SailorProfileDataProvider sailorProfileDataProvider) {
        this.competitorDataProvider = competitorDataProvider;
        this.sailorProfileDataProvider = sailorProfileDataProvider;
    }

    public void setCompetitorsAndUUID(Collection<SimpleCompetitorWithIdDTO> competitors, UUID uuid) {
        this.competitors = competitors;
        this.competitorDataProvider.clearSelection();
        competitors.forEach(c->this.competitorDataProvider.addSelection(c));
        this.uuid = uuid;
    }

    public Collection<SimpleCompetitorWithIdDTO> getCompetitors() {
        return competitors;
    }

    public UUID getUuid() {
        return uuid;
    }

    @Override
    public void onEditModeChanged(boolean edit) {
        if (!edit) {
            sailorProfileDataProvider.updateCompetitors(uuid, competitors, this);
        }
    }

    @Override
    public Object getKey(SimpleCompetitorWithIdDTO item) {
        return competitorDataProvider.getKey(item);
    }

    @Override
    public void addSelection(SimpleCompetitorWithIdDTO item) {
        competitors.add(item);
        competitorDataProvider.addSelection(item);
        sailorProfileDataProvider.updateCompetitors(uuid, competitors, this);
    }

    @Override
    public void removeSelection(SimpleCompetitorWithIdDTO item) {
        competitors.remove(item);
        competitorDataProvider.removeSelection(item);
        sailorProfileDataProvider.updateCompetitors(uuid, competitors, this);
    }

    @Override
    public void clearSelection() {
        competitors.clear();
        competitorDataProvider.clearSelection();
        sailorProfileDataProvider.updateCompetitors(uuid, competitors, this);
    }

    @Override
    public void getSuggestionItems(Iterable<String> queryTokens, int limit,
            SuggestionItemsCallback<SimpleCompetitorWithIdDTO> callback) {
        competitorDataProvider.getSuggestionItems(queryTokens, limit, callback);
    }

    @Override
    public String createSuggestionKeyString(SimpleCompetitorWithIdDTO value) {
        return competitorDataProvider.createSuggestionKeyString(value);
    }

    @Override
    public void addDisplay(SuggestedMultiSelectionPresenter.Display<SimpleCompetitorWithIdDTO> display) {
        competitorDataProvider.addDisplay(display);
    }

    @Override
    public void persist() {
    }

    @Override
    public void initSelectedItems(Collection<SimpleCompetitorWithIdDTO> selectedItems) {
    }

    @Override
    public String createSuggestionAdditionalDisplayString(SimpleCompetitorWithIdDTO value) {
        return competitorDataProvider.createSuggestionAdditionalDisplayString(value);
    }
}
