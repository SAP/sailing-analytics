package com.sap.sailing.domain.racelog;

import java.util.List;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.SimpleRaceLogIdentifier;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogResolver;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.impl.DelegatingRegattaLeaderboardWithCompetitorElimination;
import com.sap.sailing.domain.regattalike.IsRegattaLike;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.Util.Triple;

/**
 * In addition to being able to resolve a race log from a {@link SimpleRaceLogIdentifier}, this specialization can
 * additionally look for a {@link TrackedRace} linked to the "slot" identified by a {@link SimpleRaceLogIdentifier}.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public interface RaceLogAndTrackedRaceResolver extends RaceLogResolver {
    /**
     * The identifier is assumed to reference a {@link RaceColumn} in a {@link IsRegattaLike} object, plus a
     * {@link Fleet} object that can be used as an index into the {@link RaceColumn} object. With this, both, the
     * {@link RaceLog} as well as a {@link TrackedRace} can be looked up.
     */
    TrackedRace resolveTrackedRace(SimpleRaceLogIdentifier identifier);

    /**
     * Determines those {@link RaceColumn}/{@link Fleet} combinations ("slots") from all {@link Leaderboard}s managed by
     * this resolver that the tracked race identified by {@code trackedRaceIdentifier} shall be linked to when loaded.
     * Duplicate {@link RaceColumn}s are ignored; such duplicates may occur, e.g., if one {@link Leaderboard} is a view
     * of another one, as is the case for a {@link DelegatingRegattaLeaderboardWithCompetitorElimination}. In such
     * scenarios, the leaderboard returned as the {@link Triple#getA() first} component of the resulting triple is
     * randomly picked from those leaderboards {@link Leaderboard#getRaceColumns() listing} this column.
     * <p>
     * 
     * The information returned is relevant, e.g., after having created a {@link TrackedRace} that previously was
     * attached to one or more such "slots" and shall now be re-connected to those same slots again. It is also helpful,
     * e.g., when trying to figure out which race logs will be
     * {@link TrackedRace#attachRegattaLog(com.sap.sailing.domain.abstractlog.regatta.RegattaLog) attached} to that
     * {@link TrackedRace} because usually each "slot" comes with its own {@link RaceLog}, so that attaching to multiple
     * slots will result in multiple race logs being attached to the tracked race.
     */
    List<Triple<Leaderboard, RaceColumn, Fleet>> getColumnsWithRaceLogForTrackedRace(
            RegattaAndRaceIdentifier trackedRaceIdentifier);

}
