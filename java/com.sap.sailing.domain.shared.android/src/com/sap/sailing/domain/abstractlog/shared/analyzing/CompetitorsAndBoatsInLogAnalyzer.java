package com.sap.sailing.domain.abstractlog.shared.analyzing;

import java.util.HashMap;
import java.util.Map;

import com.sap.sailing.domain.abstractlog.AbstractLog;
import com.sap.sailing.domain.abstractlog.AbstractLogEvent;
import com.sap.sailing.domain.abstractlog.BaseLogAnalyzer;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogRegisteredCompetitorsAnalyzer;
import com.sap.sailing.domain.abstractlog.race.tracking.RaceLogUseCompetitorsFromRaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.tracking.analyzing.impl.RegisteredCompetitorsAnalyzer;
import com.sap.sailing.domain.abstractlog.shared.events.RegisterCompetitorAndBoatEvent;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;

/**
 * This class searches for RegisterCompetitorEvents in the given log.
 * 
 * TLDR: It's likely, that you shouldn't use this, but you might want to use {@link RegisteredCompetitorsAnalyzer} or {@link RaceLogRegisteredCompetitorsAnalyzer}
 * 
 * Note that solemnly analyzing a certain race or RegattaLog my not lead the correct registered competitors for the race
 * corresponding to the RaceLog/the races corresponding to the RegattaLog.
 * 
 * This is the case as by default the competitors registered on the RegattaLog are used for each race of the regatta
 * ignoring the ones in the RaceLog (if present). The RaceLog may override this behavior with the
 * {@link RaceLogUseCompetitorsFromRaceLogEvent}. Is an event of this type present, the competitors written into the
 * RaceLog are used instead of the competitors in the RegattaLog.
 *
 */
public class CompetitorsAndBoatsInLogAnalyzer<LogT extends AbstractLog<EventT, VisitorT>, EventT extends AbstractLogEvent<VisitorT>, VisitorT>
        extends BaseLogAnalyzer<LogT, EventT, VisitorT, Map<Competitor, Boat>> {

    public CompetitorsAndBoatsInLogAnalyzer(LogT log) {
        super(log);
    }

    @Override
    protected Map<Competitor, Boat> performAnalysis() {
        Map<Competitor, Boat> result = new HashMap<>();

        for (EventT event : getLog().getUnrevokedEvents()) {
            if (event instanceof RegisterCompetitorAndBoatEvent) {
                RegisterCompetitorAndBoatEvent<?> competitorAndBoatEvent = (RegisterCompetitorAndBoatEvent<?>) event;
                result.put(competitorAndBoatEvent.getCompetitor(), competitorAndBoatEvent.getBoat());
            }
        }

        return result;
    }
}