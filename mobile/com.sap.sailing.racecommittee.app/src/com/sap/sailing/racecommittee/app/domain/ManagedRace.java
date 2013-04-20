package com.sap.sailing.racecommittee.app.domain;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.common.Named;
import com.sap.sailing.domain.common.WithID;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.domain.racelog.RaceLog;
import com.sap.sailing.racecommittee.app.domain.state.RaceState;

public interface ManagedRace extends ManagedRaceIdentifier, Named, WithID {
	
	/**
	 * @return the identifier of the race.
	 */
	public ManagedRaceIdentifier getIdentifier();
	

	/**
	 * @return the state of the race.
	 */
	public RaceState getState();

	/**
	 * Shortcut to {@link RaceState#getRaceLog()} of {@link ManagedRace#getState()}.
	 * @return the log of the race.
	 */
	public RaceLog getRaceLog();
	
	/**
	 * Shortcut to {@link RaceState#getStatus()} of {@link ManagedRace#getState()}.
	 * @return the status of the race's state.
	 */
	public RaceLogRaceStatus getStatus();
	
	/**
         * the current course of the race
         * @return the course of the race
         */
        public CourseBase getCourseDesign();
        
        
        /**
         * returns the list of competitors for this race
         * @return list of competitors
         */
        public Iterable<Competitor> getCompetitors();

}