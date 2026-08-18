package com.sap.sailing.gwt.home.shared.places.user.profile.preferences;

import java.util.Collection;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.gwt.home.communication.event.SimpleCompetitorWithIdDTO;
import com.sap.sailing.gwt.home.shared.partials.multiselection.SuggestedMultiSelectionPresenter;
import com.sap.sse.gwt.dispatch.shared.commands.VoidResult;

public interface CompetitorSelectionPresenter
        extends SuggestedMultiSelectionPresenter<SimpleCompetitorWithIdDTO, CompetitorSelectionPresenter.Display> {
    void persistResults(boolean notifyAboutResults, AsyncCallback<VoidResult> callback,
            Collection<SimpleCompetitorWithIdDTO> latestSelectedItems);

    public void initResults(boolean notifyAboutResults, Collection<SimpleCompetitorWithIdDTO> latestSelectedItems);
    
    public void setSelectionPersistenceCallback(AsyncCallback<VoidResult> selectionCallback);

    public static interface Display extends SuggestedMultiSelectionPresenter.Display<SimpleCompetitorWithIdDTO> {
        void initResults(boolean notifyAboutResults, Collection<SimpleCompetitorWithIdDTO> latestSelectedItems);
        
        public boolean getIsNotify();
    }
}
