package com.sap.sailing.dashboards.gwt.server.util.actions.startlineadvantage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.dashboards.gwt.server.util.actions.startlineadvantage.precalculation.StartlineAdvantageCalculationData;
import com.sap.sailing.dashboards.gwt.server.util.actions.startlineadvantage.precalculation.StartlineAdvantageCalculationDataRetriever;
import com.sap.sailing.dashboards.gwt.shared.dispatch.DashboardDispatchContext;
import com.sap.sailing.dashboards.gwt.shared.dto.StartLineAdvantageDTO;
import com.sap.sailing.dashboards.gwt.shared.dto.StartlineAdvantagesWithMaxAndAverageDTO;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.SpeedWithConfidence;
import com.sap.sailing.domain.common.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Position;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedImpl;
import com.sap.sse.common.impl.MeterDistance;

/**
 * @author Alexander Ries (D062114)
 *
 */
public class StartlineAdvantagesByWindCalculator {

    private StartlineAdvantageCalculationDataRetriever startlineAdvantageCalculationDataRetriever;
    private DashboardDispatchContext dashboardDispatchContext;
    private DomainFactory domainFactory;
    private DefaultPolarWindAngleBoatSpeedFunction defaultPolarSpeedWindAngleFunction;

    private final Logger logger = Logger.getLogger(StartlineAdvantagesByWindCalculator.class.getName());

    public StartlineAdvantagesByWindCalculator(DashboardDispatchContext dashboardDispatchContext, DomainFactory domainFactory) {
        this.dashboardDispatchContext = dashboardDispatchContext;
        this.domainFactory = domainFactory;
        this.defaultPolarSpeedWindAngleFunction = new DefaultPolarWindAngleBoatSpeedFunction();
        this.startlineAdvantageCalculationDataRetriever = new StartlineAdvantageCalculationDataRetriever(domainFactory, dashboardDispatchContext.getPolarDataService()); 
    }

    public StartlineAdvantagesWithMaxAndAverageDTO getStartLineAdvantagesAccrossLineFromTrackedRaceAtTimePoint(
            TrackedRace trackedRace, TimePoint timepoint) {
        StartlineAdvantagesWithMaxAndAverageDTO result = new StartlineAdvantagesWithMaxAndAverageDTO();
        if (trackedRace != null) {
            StartlineAdvantageCalculationData startlineAdvantageCalculationData = startlineAdvantageCalculationDataRetriever.retrieveDataForTrackedRace(trackedRace);
            if (startlineAdvantageCalculationData.getWind() != null) {
                Pair<Number[][], Number[][]> startlineAdvantagesAndConfidencesAsArray = null;
                if (isStartlineCompletelyUnderneathLaylines(startlineAdvantageCalculationData)) {
                    logger.log(Level.INFO, "Startline is completely underneath laylines");
                    Pair<Double, Double> advantagesRange = new Pair<Double, Double>(0.0, startlineAdvantageCalculationData.getStartlineLenghtInMeters());
                    List<StartLineAdvantageDTO> startlineAdvantages = calculateStartlineAdvantagesUnderneathLaylinesInRange(advantagesRange, startlineAdvantageCalculationData);
                    double maximum = getMaximumAdvantageOfStartlineAdvantageDTOs(startlineAdvantages);
                    result.maximum = maximum;
                    startlineAdvantagesAndConfidencesAsArray = convertStartLineAdvantageDTOListToPointAndConfidenceArrays(startlineAdvantages);
                } else if (isStartlineCompletelyAboveLaylines(startlineAdvantageCalculationData)) {
                    logger.log(Level.INFO, "Startline is completely above laylines");
                    Pair<Double, Double> advantagesRange = new Pair<Double, Double>(0.0, startlineAdvantageCalculationData.getStartlineLenghtInMeters());
                    List<StartLineAdvantageDTO> startlineAdvantages = calculatePolarBasedStartlineAdvantagesInRange(advantagesRange, startlineAdvantageCalculationData);
                    subtractMinimumOfAllStartlineAdvantages(startlineAdvantages);
                    double maximum = getMaximumAdvantageOfStartlineAdvantageDTOs(startlineAdvantages);
                    result.maximum = maximum;
                    subtractAgainstMaximumOfAllStartlineAdvantages(startlineAdvantages, maximum);
                    startlineAdvantagesAndConfidencesAsArray = convertStartLineAdvantageDTOListToPointAndConfidenceArrays(startlineAdvantages);
                } else {
                    logger.log(Level.INFO, "Layline(s) cross startline");
                    Position intersectionOfRightLaylineAndStartline = getIntersectionOfRightLaylineAndStartline(startlineAdvantageCalculationData);
                    Position intersectionOfleftLaylineAndStartline = getIntersectionOfLeftLaylineAndStartline(startlineAdvantageCalculationData);
                    Pair<Double, Double> polarBasedStartlineAdvatagesRange = getStartAndEndPointOfPolarBasedStartlineAdvatagesInDistancesToRCBoat(
                            intersectionOfRightLaylineAndStartline, intersectionOfleftLaylineAndStartline, startlineAdvantageCalculationData);
                    Pair<Double, Double> pinEndStartlineAdvatagesRange = getPinEndStartlineAdvantagesRangeFromPolarAdvantagesRange(polarBasedStartlineAdvatagesRange, startlineAdvantageCalculationData);
                    List<StartLineAdvantageDTO> startlineAdvantages = calculatePolarBasedStartlineAdvantagesInRange(polarBasedStartlineAdvatagesRange, startlineAdvantageCalculationData);
                    subtractMinimumOfAllStartlineAdvantages(startlineAdvantages);
                    double maximum = getMaximumAdvantageOfStartlineAdvantageDTOs(startlineAdvantages);
                    result.maximum = maximum;
                    subtractAgainstMaximumOfAllStartlineAdvantages(startlineAdvantages, maximum);
                    addClosingZeroPointToMixedAdvantages(startlineAdvantages, pinEndStartlineAdvatagesRange);
                    startlineAdvantagesAndConfidencesAsArray = convertStartLineAdvantageDTOListToPointAndConfidenceArrays(startlineAdvantages);
                }
                if (startlineAdvantagesAndConfidencesAsArray != null) {
                    result.distanceToRCBoatToStartlineAdvantage = startlineAdvantagesAndConfidencesAsArray.getA();
                    result.distanceToRCBoatToConfidence = startlineAdvantagesAndConfidencesAsArray.getB();
                }
            }
        } else {
            logger.log(Level.INFO, "No live race available for startlineadvantages calculation");
        }
        return result;
    }

    private boolean isBearingAboveAdvantageLines(Bearing bearing, StartlineAdvantageCalculationData startlineAdvantageCalculationData) {
        boolean result = false;
        Bearing bearingOfRightLaylineInDeg = new DegreeBearingImpl(startlineAdvantageCalculationData.getWind().getBearing().getDegrees()
                - startlineAdvantageCalculationData.getManeuverAngle() / 2);
        Bearing bearingOfLeftLaylineInDeg = new DegreeBearingImpl(startlineAdvantageCalculationData.getWind().getBearing().getDegrees()
                + startlineAdvantageCalculationData.getManeuverAngle() / 2);
        if (bearing.getDegrees() < bearingOfRightLaylineInDeg.getDegrees() && bearing.getDegrees() > 0
                || bearing.getDegrees() > bearingOfLeftLaylineInDeg.getDegrees() && bearing.getDegrees() < 360) {
            result = true;
        }
        return result;
    }

    private boolean isBearingUnderneathAdvantageLines(Bearing bearing, StartlineAdvantageCalculationData startlineAdvantageCalculationData) {
        boolean result = false;
        Bearing bearingOfRightLaylineInDeg = new DegreeBearingImpl(startlineAdvantageCalculationData.getWind().getBearing().getDegrees()
                - startlineAdvantageCalculationData.getManeuverAngle() / 2);
        Bearing bearingOfLeftLaylineInDeg = new DegreeBearingImpl(startlineAdvantageCalculationData.getWind().getBearing().getDegrees()
                + startlineAdvantageCalculationData.getManeuverAngle() / 2);
        logger.log(Level.INFO, "Underneath bearingOfRightLaylineInDeg?" + bearingOfRightLaylineInDeg);
        logger.log(Level.INFO, "Underneath bearingOfLeftLaylineInDeg?" + bearingOfLeftLaylineInDeg);
        if (bearing.getDegrees() > bearingOfRightLaylineInDeg.getDegrees()
                && bearing.getDegrees() < bearingOfLeftLaylineInDeg.getDegrees()) {
            result = true;
        }
        return result;
    }

    private boolean isStartlineCompletelyAboveLaylines(StartlineAdvantageCalculationData startlineAdvantageCalculationData) {
        boolean result = false;
        Bearing bearingRCBoatToFirstMark = new DegreeBearingImpl(startlineAdvantageCalculationData.getFirstMarkPosition()
                .getBearingGreatCircle(startlineAdvantageCalculationData.getStartBoatPosition()).getDegrees());
        Bearing bearingPinEndToFirstMark = new DegreeBearingImpl(startlineAdvantageCalculationData.getFirstMarkPosition()
                .getBearingGreatCircle(startlineAdvantageCalculationData.getPinEndPosition()).getDegrees());
        if (isBearingAboveAdvantageLines(bearingRCBoatToFirstMark, startlineAdvantageCalculationData)
                && isBearingAboveAdvantageLines(bearingPinEndToFirstMark, startlineAdvantageCalculationData)) {
            result = true;
        }
        return result;
    }

    private boolean isStartlineCompletelyUnderneathLaylines(StartlineAdvantageCalculationData startlineAdvantageCalculationData) {
        boolean result = false;
        Bearing bearingFirstMarkToRCBoat = new DegreeBearingImpl(startlineAdvantageCalculationData.getFirstMarkPosition()
                .getBearingGreatCircle(startlineAdvantageCalculationData.getStartBoatPosition()).getDegrees());
        Bearing bearingFirstMarkToPinEnd = new DegreeBearingImpl(startlineAdvantageCalculationData.getFirstMarkPosition()
                .getBearingGreatCircle(startlineAdvantageCalculationData.getPinEndPosition()).getDegrees());
        logger.log(Level.INFO, "Underneath RC?" + bearingFirstMarkToRCBoat);
        logger.log(Level.INFO, "Underneath PIN?" + bearingFirstMarkToPinEnd);
        if (isBearingUnderneathAdvantageLines(bearingFirstMarkToRCBoat, startlineAdvantageCalculationData)
                && isBearingUnderneathAdvantageLines(bearingFirstMarkToPinEnd, startlineAdvantageCalculationData)) {
            result = true;
        }
        return result;
    }

    private Pair<Double, Double> getStartAndEndPointOfPolarBasedStartlineAdvatagesInDistancesToRCBoat(Position rightIntersection, Position leftIntersection, StartlineAdvantageCalculationData startlineAdvantageCalculationData) {
        Pair<Double, Double> result = null;
        if (rightIntersection != null && leftIntersection == null) {
            double distanceFromIntersectionToRCBoatInMeters = rightIntersection.getDistance(startlineAdvantageCalculationData.getStartBoatPosition()).getMeters();
            result = new Pair<Double, Double>(0.0, distanceFromIntersectionToRCBoatInMeters);
        } else if (rightIntersection == null && leftIntersection != null) {
            double distanceFromIntersectionToRCBoatInMeters = leftIntersection.getDistance(startlineAdvantageCalculationData.getStartBoatPosition()).getMeters();
            result = new Pair<Double, Double>(distanceFromIntersectionToRCBoatInMeters, startlineAdvantageCalculationData.getStartlineLenghtInMeters());
        } else if (rightIntersection != null && leftIntersection != null) {
            result = new Pair<Double, Double>(0.0, startlineAdvantageCalculationData.getStartlineLenghtInMeters());
        }
        return result;
    }

    private Position getIntersectionOfRightLaylineAndStartline(StartlineAdvantageCalculationData startlineAdvantageCalculationData) {
        Position result = null;
        Bearing bearingOfRightLaylineInDeg = new DegreeBearingImpl(startlineAdvantageCalculationData.getWind().getBearing().getDegrees()
                - startlineAdvantageCalculationData.getManeuverAngle() / 2);
        result = calculateIntersectionPointsOfStartlineAndLaylineWithBearing(bearingOfRightLaylineInDeg, startlineAdvantageCalculationData);
        return result;
    }

    private Position getIntersectionOfLeftLaylineAndStartline(StartlineAdvantageCalculationData startlineAdvantageCalculationData) {
        Position result = null;
        Bearing bearingOfRightLaylineInDeg = new DegreeBearingImpl(startlineAdvantageCalculationData.getWind().getBearing().getDegrees()
                + startlineAdvantageCalculationData.getManeuverAngle() / 2);
        result = calculateIntersectionPointsOfStartlineAndLaylineWithBearing(bearingOfRightLaylineInDeg, startlineAdvantageCalculationData);
        return result;
    }

    private Position calculateIntersectionPointsOfStartlineAndLaylineWithBearing(Bearing bearing, StartlineAdvantageCalculationData startlineAdvantageCalculationData) {
        Position result = null;
        Bearing bearingOfStartlineInRad = startlineAdvantageCalculationData.getStartBoatPosition()
                .getBearingGreatCircle(startlineAdvantageCalculationData.getPinEndPosition());
        Bearing bearingOfStartlineInDeg = new DegreeBearingImpl(bearingOfStartlineInRad.getDegrees());
        logger.log(Level.INFO, "bearingOfStartlineInDeg " + bearingOfStartlineInDeg);
        logger.log(Level.INFO, "bearingOfLaylineInDeg " + bearing);
        Position intersectionPointLaylineStartline = startlineAdvantageCalculationData.getFirstMarkPosition().getIntersection(
                bearing, startlineAdvantageCalculationData.getPinEndPosition(), bearingOfStartlineInDeg);
        logger.log(Level.INFO, "rightIntersectionPointLaylineStartline " + intersectionPointLaylineStartline);
        Bearing bearingIntersectionPointToFirstMark = new DegreeBearingImpl(
                startlineAdvantageCalculationData.getFirstMarkPosition().getBearingGreatCircle(
                        intersectionPointLaylineStartline).getDegrees());
        if (bearingIntersectionPointToFirstMark.getDegrees() < bearing.getDegrees() + 1
                && bearingIntersectionPointToFirstMark.getDegrees() > bearing.getDegrees() - 1
                && isPositionOnStartline(intersectionPointLaylineStartline, startlineAdvantageCalculationData)) {
            result = intersectionPointLaylineStartline;
            logger.log(Level.INFO, "Layline crosses startline");
        }
        logger.log(Level.INFO, "bearingLeftIntersectionPointToFirstMark.getDegrees() "
                + bearingIntersectionPointToFirstMark.getDegrees());
        return result;
    }

    /**
     * On startline mean less than one meter away
     * */
    private boolean isPositionOnStartline(Position position, StartlineAdvantageCalculationData startlineAdvantageCalculationData) {
        boolean result = false;
        Distance distanceToStartline = position.getDistanceToLine(startlineAdvantageCalculationData.getStartBoatPosition(), startlineAdvantageCalculationData.getPinEndPosition());
        if (distanceToStartline.getMeters() < 1) {
            result = true;
        }
        return result;
    }

    private List<StartLineAdvantageDTO> addClosingZeroPointToMixedAdvantages(List<StartLineAdvantageDTO> advantages,
            Pair<Double, Double> rangePinEndStartlineAdvantage) {
        StartLineAdvantageDTO startLineAdvantageDTO = new StartLineAdvantageDTO();
        startLineAdvantageDTO.confidence = 1.0;
        if (rangePinEndStartlineAdvantage.getA() > 0) {
            startLineAdvantageDTO.distanceToRCBoatInMeters = rangePinEndStartlineAdvantage.getB();
            startLineAdvantageDTO.startLineAdvantage = 0.0;
            advantages.add(startLineAdvantageDTO);
        } else {
            startLineAdvantageDTO.distanceToRCBoatInMeters = rangePinEndStartlineAdvantage.getA();
            startLineAdvantageDTO.startLineAdvantage = 0.0;
            advantages.add(0, startLineAdvantageDTO);
        }
        return advantages;
    }

    private Pair<Double, Double> getPinEndStartlineAdvantagesRangeFromPolarAdvantagesRange(Pair<Double, Double> rangePolarBasedStartlineAdvatages, StartlineAdvantageCalculationData startlineAdvantageCalculationData) {
        Pair<Double, Double> result = null;
        double pinEndStartlineAdvantagesStart;
        double pinEndStartlineAdvantagesEnd;
        if (rangePolarBasedStartlineAdvatages.getA().doubleValue() == 0.0
                && rangePolarBasedStartlineAdvatages.getB().doubleValue() != startlineAdvantageCalculationData.getStartlineLenghtInMeters()) {
            pinEndStartlineAdvantagesStart = rangePolarBasedStartlineAdvatages.getB().doubleValue();
            pinEndStartlineAdvantagesEnd = startlineAdvantageCalculationData.getStartlineLenghtInMeters();
            result = new Pair<Double, Double>(pinEndStartlineAdvantagesStart, pinEndStartlineAdvantagesEnd);
        } else if (rangePolarBasedStartlineAdvatages.getA().doubleValue() != 0.0
                && rangePolarBasedStartlineAdvatages.getB().doubleValue() == startlineAdvantageCalculationData.getStartlineLenghtInMeters()) {
            pinEndStartlineAdvantagesStart = 0.0;
            pinEndStartlineAdvantagesEnd = rangePolarBasedStartlineAdvatages.getB().doubleValue();
            result = new Pair<Double, Double>(pinEndStartlineAdvantagesStart, pinEndStartlineAdvantagesEnd);
        }
        return result;
    }

    private List<StartLineAdvantageDTO> calculateStartlineAdvantagesUnderneathLaylinesInRange(Pair<Double, Double> rangePinEndStartlineAdvantage, StartlineAdvantageCalculationData startlineAdvantageCalculationData) {
        List<StartLineAdvantageDTO> result = new ArrayList<StartLineAdvantageDTO>();
        if (rangePinEndStartlineAdvantage != null) {
            logger.log(Level.INFO, "PinEnd startline advantages range " + rangePinEndStartlineAdvantage.getA() + " - "
                    + rangePinEndStartlineAdvantage.getB());
            StartLineAdvantageDTO rightEdgeAdvantage = new StartLineAdvantageDTO();
            rightEdgeAdvantage.confidence = 1.0;
            rightEdgeAdvantage.distanceToRCBoatInMeters = rangePinEndStartlineAdvantage.getA();
            StartLineAdvantageDTO leftEdgeAdvantage = new StartLineAdvantageDTO();
            leftEdgeAdvantage.confidence = 1.0;
            leftEdgeAdvantage.distanceToRCBoatInMeters = rangePinEndStartlineAdvantage.getB();
            if (startlineAdvantageCalculationData.getStartlineAdvantageAtPinEndInMeters() >= 0) {
                leftEdgeAdvantage.startLineAdvantage = startlineAdvantageCalculationData.getStartlineAdvantageAtPinEndInMeters();
                rightEdgeAdvantage.startLineAdvantage = 0.0;
            } else {
                leftEdgeAdvantage.startLineAdvantage = 0.0;
                rightEdgeAdvantage.startLineAdvantage = Math.abs(startlineAdvantageCalculationData.getStartlineAdvantageAtPinEndInMeters());
            }
            result.add(rightEdgeAdvantage);
            result.add(leftEdgeAdvantage);
        } else {
            logger.log(Level.INFO, "PinEnd startline advantages range null");
        }
        return result;
    }

    private Pair<Number[][], Number[][]> convertStartLineAdvantageDTOListToPointAndConfidenceArrays(
            List<StartLineAdvantageDTO> startLineAdvantageDTOList) {
        Pair<Number[][], Number[][]> result = null;
        if (startLineAdvantageDTOList != null && startLineAdvantageDTOList.size() > 0) {
            Number[][] distanceToRCBoatToStartlineAdvantage = new Number[startLineAdvantageDTOList.size()][2];
            Number[][] distanceToRCBoatToConfidence = new Number[startLineAdvantageDTOList.size()][2];
            for (int i = 0; i < startLineAdvantageDTOList.size(); i++) {
                StartLineAdvantageDTO startLineAdvantageDTO = startLineAdvantageDTOList.get(i);
                distanceToRCBoatToStartlineAdvantage[i][0] = startLineAdvantageDTO.distanceToRCBoatInMeters;
                distanceToRCBoatToStartlineAdvantage[i][1] = startLineAdvantageDTO.startLineAdvantage;
                distanceToRCBoatToConfidence[i][0] = startLineAdvantageDTO.distanceToRCBoatInMeters;
                distanceToRCBoatToConfidence[i][1] = startLineAdvantageDTO.confidence;
            }
            result = new Pair<Number[][], Number[][]>(distanceToRCBoatToStartlineAdvantage,
                    distanceToRCBoatToConfidence);
        }
        return result;
    }

    private List<StartLineAdvantageDTO> calculatePolarBasedStartlineAdvantagesInRange(Pair<Double, Double> rangePinEndStartlineAdvantage, StartlineAdvantageCalculationData startlineAdvantageCalculationData) {
        List<StartLineAdvantageDTO> result = new ArrayList<StartLineAdvantageDTO>();
        if (rangePinEndStartlineAdvantage != null) {
            logger.log(Level.INFO, "PolarBased startline advantages range " + rangePinEndStartlineAdvantage.getA()
                    + " - " + rangePinEndStartlineAdvantage.getB());
            Bearing bearingOfStartlineInRad = startlineAdvantageCalculationData.getStartBoatPosition()
                    .getBearingGreatCircle(startlineAdvantageCalculationData.getPinEndPosition());
            Bearing bearingOfStartlineInDeg = new DegreeBearingImpl(bearingOfStartlineInRad.getDegrees());
            for (double i = rangePinEndStartlineAdvantage.getA().doubleValue(); i < rangePinEndStartlineAdvantage
                    .getB().doubleValue() - 1; i++) {
                StartLineAdvantageDTO startlineAdvantage = new StartLineAdvantageDTO();
                startlineAdvantage.confidence = 0.5;
                startlineAdvantage.distanceToRCBoatInMeters = i;
                Position startingPosition = startlineAdvantageCalculationData.getStartBoatPosition().translateRhumb(bearingOfStartlineInDeg, new MeterDistance(i));
                Distance startingPositionToFirstMarkDistance = startingPosition.getDistance(startlineAdvantageCalculationData.getFirstMarkPosition());
                Bearing bearingOfFirstMarkToStartPositionPositionInRad = startlineAdvantageCalculationData.getFirstMarkPosition().getBearingGreatCircle(startingPosition);
                Bearing bearingOfFirstMarkToStartPositionPositionInDeg = new DegreeBearingImpl(bearingOfFirstMarkToStartPositionPositionInRad.getDegrees());
                DegreeBearingImpl angleToWind = new DegreeBearingImpl(Math.abs(startlineAdvantageCalculationData.getWind().getBearing().getDifferenceTo(bearingOfFirstMarkToStartPositionPositionInDeg).getDegrees()));
                logger.log(Level.INFO, "angleToWind" + angleToWind);
                SpeedWithConfidence<Void> speedWithConfidence = getBoatSpeedWithConfidenceForWindAngleAndStrength(angleToWind, startlineAdvantageCalculationData); 
                Speed speed = speedWithConfidence.getObject();
                logger.log(Level.INFO, "Speed: " + speed.getKnots()+" Confidence: "+speedWithConfidence.getConfidence());
                logger.log(Level.INFO, "startingPositionToFirstMarkDistance.getMeters()" + startingPositionToFirstMarkDistance.getMeters());
                startlineAdvantage.startLineAdvantage = Math
                        .abs((startingPositionToFirstMarkDistance.getMeters() / speed.getMetersPerSecond())
                                * speed.getMetersPerSecond());
                result.add(startlineAdvantage);
            }
        } else {
            logger.log(Level.INFO, "PinEnd startline advantages range null");
        }
        return result;
    }

    private List<StartLineAdvantageDTO> subtractAgainstMaximumOfAllStartlineAdvantages(
            List<StartLineAdvantageDTO> advantages, double maximum) {
        for (StartLineAdvantageDTO startLineAdvantageDTO : advantages) {
            startLineAdvantageDTO.startLineAdvantage = maximum - startLineAdvantageDTO.startLineAdvantage;
        }
        return advantages;
    }

    private List<StartLineAdvantageDTO> subtractMinimumOfAllStartlineAdvantages(List<StartLineAdvantageDTO> advantages) {
        double minimum = getMinimumAdvantageOfStartlineAdvantageDTOs(advantages);
        for (StartLineAdvantageDTO startLineAdvantageDTO : advantages) {
            startLineAdvantageDTO.startLineAdvantage = startLineAdvantageDTO.startLineAdvantage - minimum;
        }
        return advantages;
    }

    private Double getMaximumAdvantageOfStartlineAdvantageDTOs(List<StartLineAdvantageDTO> advantages) {
        Double result = null;
        List<StartLineAdvantageDTO> sortedAdvantages = new ArrayList<StartLineAdvantageDTO>();
        sortedAdvantages.addAll(advantages);
        if (sortedAdvantages != null && sortedAdvantages.size() > 0) {
            Collections.sort(sortedAdvantages, StartLineAdvantageDTO.startlineAdvantageComparatorByAdvantageDesc);
            result = Double.valueOf(sortedAdvantages.get(0).startLineAdvantage);
        }
        return result;
    }

    private double getMinimumAdvantageOfStartlineAdvantageDTOs(List<StartLineAdvantageDTO> advantages) {
        double result = 0;
        List<StartLineAdvantageDTO> sortedAdvantages = new ArrayList<StartLineAdvantageDTO>();
        sortedAdvantages.addAll(advantages);
        if (sortedAdvantages != null && sortedAdvantages.size() > 0) {
            Collections.sort(sortedAdvantages, StartLineAdvantageDTO.startlineAdvantageComparatorByAdvantageDesc);
            result = sortedAdvantages.get(sortedAdvantages.size() - 1).startLineAdvantage;
        }
        return result;
    }
    
    private SpeedWithConfidence<Void> getBoatSpeedWithConfidenceForWindAngleAndStrength(Bearing angleToWind, StartlineAdvantageCalculationData startlineAdvantageCalculationData) {
        SpeedWithConfidence<Void> result = null;
        try {
            result = dashboardDispatchContext.getPolarDataService().getSpeed(domainFactory.getOrCreateBoatClass("Extreme 40"), new KnotSpeedImpl(startlineAdvantageCalculationData.getWind().getBeaufort()), angleToWind);
        } catch (NotEnoughDataHasBeenAddedException e) {
            result = defaultPolarSpeedWindAngleFunction.getBoatSpeedForWindAngleAndSpeed(angleToWind, new KnotSpeedImpl(startlineAdvantageCalculationData.getWind().getBeaufort()));
            e.printStackTrace();
        }
        return result;
        
    }
}
