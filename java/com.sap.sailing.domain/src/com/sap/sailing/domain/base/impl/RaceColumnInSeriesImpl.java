package com.sap.sailing.domain.base.impl;


import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumnInSeries;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;

/**
 * Obtains flees and medal information from the {@link Series} to which it is connected at construction time.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class RaceColumnInSeriesImpl extends AbstractRaceColumn implements RaceColumnInSeries {
    private static final long serialVersionUID = -2199678838624406645L;
    private final Series series;
    private final String name;

    /**
     * Making this transient will leave it <code>null</code> on a replica. This, however, is OK because
     * no check for matching regatta is required, assuming replication happens on a consistent, correct state.
     */
    private transient final TrackedRegattaRegistry trackedRegattaRegistry;

    /**
     * @param trackedRegattaRegistry
     *            used to find the {@link TrackedRegatta} for this column's series' {@link Series#getRegatta() regatta}
     *            in order to re-associate a {@link TrackedRace} passed to {@link #setTrackedRace(Fleet, TrackedRace)}
     *            with this column's series' {@link TrackedRegatta}, and the tracked race's {@link RaceDefinition} with
     *            this column's series {@link Regatta}, respectively. If <code>null</code>, the re-association won't be
     *            carried out.
     */
    public RaceColumnInSeriesImpl(String name, Series series, TrackedRegattaRegistry trackedRegattaRegistry) {
        super();
        this.name = name;
        this.series = series;
        this.trackedRegattaRegistry = trackedRegattaRegistry;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Regatta getRegatta() {
        return getSeries().getRegatta();
    }

    @Override
    public Iterable<? extends Fleet> getFleets() {
        return series.getFleets();
    }

    @Override
    public boolean isMedalRace() {
        return series.isMedal();
    }

    @Override
    public Series getSeries() {
        return series;
    }

    /**
     * In addition to associating the tracked race to this column for the fleet specified, if this column has a non-
     * <code>null</code> {@link #trackedRegattaRegistry}, this method also verifies that <code>trackedRace</code>
     * belongs to the {@link TrackedRegatta} of this column's series' {@link Series#getRegatta() regatta} and that the
     * corresponding {@link TrackedRace#getRace() RaceDefinition} is owned by this column's series'
     * {@link Series#getRegatta() regatta}. If not, an {@link IllegalArgumentException} is thrown.
     */
    @Override
    public void setTrackedRace(Fleet fleet, TrackedRace trackedRace) {
        if (trackedRegattaRegistry != null) {
            if (trackedRace != null) {
                TrackedRegatta trackedRegatta = trackedRace.getTrackedRegatta();
                Regatta regatta = trackedRegatta.getRegatta();
                if (regatta != getRegatta()) {
                    throw new IllegalArgumentException("Trying to associate tracked race of regatta "
                            + regatta.getName() + " to a race column of regatta " + getRegatta().getName());
                }
            }
        }
        // re-associating the TrackedRace needs to happen before the super call because the RaceIdentifier may have changed
        super.setTrackedRace(fleet, trackedRace);
    }

}
