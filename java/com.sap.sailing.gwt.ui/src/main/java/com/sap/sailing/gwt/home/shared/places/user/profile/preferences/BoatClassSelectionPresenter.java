package com.sap.sailing.gwt.home.shared.places.user.profile.preferences;

import java.util.Collection;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.domain.common.dto.BoatClassDTO;
import com.sap.sailing.gwt.home.shared.partials.multiselection.SuggestedMultiSelectionPresenter;
import com.sap.sse.gwt.dispatch.shared.commands.VoidResult;

public interface BoatClassSelectionPresenter
        extends SuggestedMultiSelectionPresenter<BoatClassDTO, BoatClassSelectionPresenter.Display> {

    void setNotifyAboutUpcomingRaces(boolean notifyAboutUpcomingRaces, AsyncCallback<VoidResult> callback);

    void setNotifyAboutResults(boolean notifyAboutResults, AsyncCallback<VoidResult> callback);

    void initNotifications(boolean notifyAboutUpcomingRaces, boolean notifyAboutResults, Collection<BoatClassDTO> selection);

    void persistResults(boolean notifyAboutUpcomingRaces, boolean notifyAboutResults,
            AsyncCallback<VoidResult> callback, Collection<BoatClassDTO> latestSelectedItems);

    void setSelectionPersistenceCallback(AsyncCallback<VoidResult> selectionCallback);

    interface Display extends SuggestedMultiSelectionPresenter.Display<BoatClassDTO> {
        void initResults(boolean notifyAboutUpcomingRaces, boolean notifyAboutResults, Collection<BoatClassDTO> selection);

        boolean getNotifyAboutUpcomingRaces();

        boolean getNotifyAboutResults();

        Collection<BoatClassDTO> getSelection();
    }

}
