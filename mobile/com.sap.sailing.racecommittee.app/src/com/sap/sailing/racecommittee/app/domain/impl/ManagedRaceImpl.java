package com.sap.sailing.racecommittee.app.domain.impl;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.FinishingTimeFinder;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.StartTimeFinder;
import com.sap.sailing.domain.abstractlog.race.state.RaceState;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.SeriesBase;
import com.sap.sailing.domain.base.racegroup.RaceGroup;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sailing.racecommittee.app.data.AndroidRaceLogResolver;
import com.sap.sailing.racecommittee.app.domain.ManagedRace;
import com.sap.sailing.racecommittee.app.domain.ManagedRaceIdentifier;
import com.sap.sailing.racecommittee.app.domain.MapMarker;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class ManagedRaceImpl implements ManagedRace {
    private static final long serialVersionUID = -4936566684992524001L;

    private RaceState state;
    private final ManagedRaceIdentifier identifier;
    private Collection<Competitor> competitors;
    private List<MapMarker> mapMarkers;
    private CourseBase courseOnServer;

    public ManagedRaceImpl(ManagedRaceIdentifier identifier, RaceState state) {
        this.state = state;
        this.identifier = identifier;
        this.competitors = new ArrayList<>();
        this.courseOnServer = null;
    }

    @Override
    public RaceState getState() {
        return state;
    }

    @Override
    public void setState(RaceState state) {
        if (this.state != null) {
            throw new IllegalStateException("RaceState can only be set once");
        }
        this.state = state;
    }

    @Override
    public String getId() {
        return identifier.getId();
    }

    @Override
    public String getName() {
        return identifier.getRaceName();
    }

    @Override
    public String getRaceName() {
        return getName();
    }

    @Override
    public Fleet getFleet() {
        return identifier.getFleet();
    }

    @Override
    public SeriesBase getSeries() {
        return identifier.getSeries();
    }

    @Override
    public RaceGroup getRaceGroup() {
        return identifier.getRaceGroup();
    }

    @Override
    public ManagedRaceIdentifier getIdentifier() {
        return identifier;
    }

    @Override
    public RaceLog getRaceLog() {
        return state.getRaceLog();
    }

    @Override
    public RaceLogRaceStatus getStatus() {
        return state.getStatus();
    }

    @Override
    public CourseBase getCourseDesign() {
        return state.getCourseDesign();
    }

    @Override
    public Collection<Competitor> getCompetitors() {
        return competitors;
    }

    @Override
    public List<MapMarker> getMapMarkers(){
        return mapMarkers;
    }

    @Override
    public CourseBase getCourseOnServer() {
        return courseOnServer;
    }

    @Override
    public void setCourseOnServer(CourseBase course) {
        courseOnServer = course;
    }

    @Override
    public void setCompetitors(Collection<Competitor> competitors) {
        this.competitors = competitors;
    }

    @Override
    public void setMapMarkers(List<MapMarker> markers) {
        this.mapMarkers = markers;
    }

    @Override
    public Result setFinishedTime(TimePoint finishedTime) {
        Result result = new Result();
        FinishingTimeFinder ftf = new FinishingTimeFinder(getRaceLog());
        if (ftf.analyze() != null) {
            if (finishedTime.after(MillisecondsTimePoint.now())) {
                result.setError(R.string.error_time_in_future);
            } else {
                if (ftf.analyze().before(finishedTime)) {
                    getState().setFinishedTime(finishedTime);
                } else {
                    result.setError(R.string.error_finished_time);
                }
            }
        }
        return result;
    }

    @Override
    public Result setFinishingTime(TimePoint finishingTime) {
        Result result = new Result();
        StartTimeFinder stf = new StartTimeFinder(new AndroidRaceLogResolver(), getRaceLog());
        if (stf.analyze() != null) {
            if (finishingTime.after(MillisecondsTimePoint.now())) {
                result.setError(R.string.error_time_in_future);
            } else {
                if (stf.analyze().getStartTime().before(finishingTime)) {
                    getState().setFinishingTime(finishingTime);
                } else {
                    result.setError(R.string.error_finishing_time);
                }
            }
        }
        return result;
    }
}
