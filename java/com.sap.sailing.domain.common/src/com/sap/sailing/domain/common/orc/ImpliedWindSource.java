package com.sap.sailing.domain.common.orc;

import java.io.Serializable;

/**
 * Tells for a race using ORC Polar Curve Scoring for its ranking metric, where that race is supposed to obtain
 * its implied wind from. The default is that the race reports the maximum implied wind across all competitors as the
 * implied wind to use for ranking, expressed by the {@link OwnMaxImpliedWind} source. This can be overridden by race
 * log events that either specify a constant wind speed to use as that race's implied wind
 * ({@link FixedSpeedImpliedWind}), or that specify another race by naming its leaderboard name, race column name and
 * fleet name, so that this other race's implied wind is used ({@link OtherRaceAsImpliedWindSource}).
 * <p>
 * 
 * A visitor pattern can be implemented for this type using {@link ImpliedWindSourceVisitor}.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public interface ImpliedWindSource extends Serializable {
    <T> T accept(ImpliedWindSourceVisitor<T> visitor);
}
