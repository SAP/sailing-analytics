package com.sap.sailing.gwt.home.communication.start;

import com.google.gwt.core.shared.GwtIncompatible;
import com.sap.sailing.gwt.home.communication.SailingAction;
import com.sap.sailing.gwt.home.communication.SailingDispatchContext;
import com.sap.sailing.gwt.server.EventHolder;
import com.sap.sailing.gwt.server.EventStageCandidateCalculator;
import com.sap.sailing.gwt.server.HomeServiceUtil;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.gwt.dispatch.client.system.caching.IsClientCacheable;
import com.sap.sse.gwt.dispatch.shared.commands.ListResult;
import com.sap.sse.gwt.dispatch.shared.exceptions.DispatchException;

public class GetStagedEventsAction implements SailingAction<ListResult<EventStageDTO>>, IsClientCacheable {
    private boolean useTeaserImage;

    protected GetStagedEventsAction() {
    }

    public GetStagedEventsAction(boolean useTeaserImage) {
        this.useTeaserImage = useTeaserImage;
    }

    @GwtIncompatible
    public ListResult<EventStageDTO> execute(final SailingDispatchContext context) throws DispatchException {
        EventStageCandidateCalculator stageCandidateCalculator = new EventStageCandidateCalculator();
        HomeServiceUtil.forAllPublicEvents(context.getRacingEventService(), context.getRequest(),
                stageCandidateCalculator);
        ListResult<EventStageDTO> result = new ListResult<>();
        int count = 0;
        for (Pair<StageEventType, EventHolder> pair : stageCandidateCalculator.getFeaturedEvents()) {
            count++;
            if (count > EventStageCandidateCalculator.MAX_STAGE_EVENTS) {
                break;
            }
            StageEventType stageType = pair.getA();
            EventHolder holder = pair.getB();
            result.addValue(HomeServiceUtil.convertToEventStageDTO(holder.event, holder.baseURL, holder.onRemoteServer,
                    stageType, context.getRacingEventService(), useTeaserImage));
        }
        return result;
    }

    @Override
    public void cacheInstanceKey(StringBuilder key) {
        key.append(useTeaserImage);
    }
}
