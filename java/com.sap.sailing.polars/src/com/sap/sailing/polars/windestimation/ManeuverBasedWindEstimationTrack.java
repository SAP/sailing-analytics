package com.sap.sailing.polars.windestimation;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.common.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.scalablevalue.ScalableDouble;
import com.sap.sse.common.scalablevalue.impl.ScalableBearing;
import com.sap.sse.util.kmeans.Cluster;

public interface ManeuverBasedWindEstimationTrack extends WindTrack {

    BoatClass getBoatClass();

    Set<Cluster<ManeuverClassification, Pair<ScalableBearing, ScalableDouble>, Pair<Bearing, Double>, ScalableBearingAndScalableDouble>> getClusters();

    List<Cluster<ManeuverClassification, Pair<ScalableBearing, ScalableDouble>, Pair<Bearing, Double>, ScalableBearingAndScalableDouble>> getTackClusters();

    List<Cluster<ManeuverClassification, Pair<ScalableBearing, ScalableDouble>, Pair<Bearing, Double>, ScalableBearingAndScalableDouble>> getJibeClusters();

    String getStringRepresentation();

    String getStringRepresentation(
            Stream<Cluster<ManeuverClassification, Pair<ScalableBearing, ScalableDouble>, Pair<Bearing, Double>, ScalableBearingAndScalableDouble>> clusters);

    void initialize() throws NotEnoughDataHasBeenAddedException;

}
