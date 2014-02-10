package com.sap.sailing.domain.markpassingcalculation;

import java.util.LinkedHashMap;
import java.util.List;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.domain.tracking.MarkPassing;

/**
 * Takes the {@link Candidate}s from an {@link AbstractCandidateFinder} and calculates any new {@link MarkPassing}s. The
 * race should then be notified about these new MarkPassings.
 * 
 * @author Nicolas Klose
 * 
 */

public interface AbstractCandidateChooser {

    public void calculateMarkPassDeltas(Competitor c, Pair<List<Candidate>, List<Candidate>> candidateDeltas);

    public LinkedHashMap<Competitor, LinkedHashMap<Waypoint, MarkPassing>> getAllPasses();
}
