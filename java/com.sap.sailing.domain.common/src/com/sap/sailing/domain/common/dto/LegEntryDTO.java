package com.sap.sailing.domain.common.dto;

import java.io.Serializable;
import java.util.Map;

import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sse.common.Duration;

/**
 * Holds data about one competitor's performance in one leg of one race represented in the
 * {@link LeaderboardDTO leaderboard} in which this object is (indirectly, via a
 * {@link LeaderboardRowDTO} instance) embedded.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class LegEntryDTO implements Serializable {
    private static final long serialVersionUID = -1236734337344886025L;
    public LegType legType;
    public Double distanceTraveledInMeters;
    public Double distanceTraveledIncludingGateStartInMeters;
    public Double averageSpeedOverGroundInKnots;
    public int rank;
    public Double gapToLeaderInSeconds;
    public Double gapChangeSinceLegStartInSeconds;
    public NauticalSide sideToWhichMarkAtLegStartWasRounded;
    public Double estimatedTimeToNextWaypointInSeconds;

    /**
     * If the competitor has already finished the leg at the time point for which this entry was created, contains the
     * <em>average</em> speed over ground, otherwise the current speed over ground. If the competitor hasn't started the
     * leg yet, it's <code>null</code>.
     */
    public Double currentSpeedOverGroundInKnots;
    
    /**
     * If the competitor has already finished the leg at the time point for which this entry was created, contains the
     * <em>average</em> ride height (foiling), otherwise the current ride height. If the competitor hasn't started the
     * leg yet, it's <code>null</code>.
     */
    public Double currentRideHeightInMeters;

    public Double currentHeelInDegrees;
    public Double currentPitchInDegrees;
    public Double currentDistanceFoiledInMeters;
    public Double currentDurationFoiledInSeconds;
    
    /**
     * If the competitor has already finished the leg at the time point for which this entry was created, contains the
     * <em>average</em> VMG, otherwise the current VMG. If the competitor hasn't started the leg yet, it's
     * <code>null</code>.
     */
    public Double velocityMadeGoodInKnots;
    
    public Double windwardDistanceToGoInMeters;
    public Long timeInMilliseconds;
    public boolean started;
    public boolean finished;
    public Map<ManeuverType, Integer> numberOfManeuvers;
    public Map<ManeuverType, Double> averageManeuverLossInMeters;
    public Double averageAbsoluteCrossTrackErrorInMeters;
    public Double averageSignedCrossTrackErrorInMeters;
    
    /**
     * The corrected time spent since the start of the race up to the current time point or the finishing of
     * the leg, whichever comes first
     */
    public Duration correctedTotalTime;
    
    public ExpeditionLegHolder expeditionHolder;
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((averageAbsoluteCrossTrackErrorInMeters == null) ? 0
                : averageAbsoluteCrossTrackErrorInMeters.hashCode());
        result = prime * result + ((averageManeuverLossInMeters == null) ? 0 : averageManeuverLossInMeters.hashCode());
        result = prime * result + ((averageSignedCrossTrackErrorInMeters == null) ? 0
                : averageSignedCrossTrackErrorInMeters.hashCode());
        result = prime * result
                + ((averageSpeedOverGroundInKnots == null) ? 0 : averageSpeedOverGroundInKnots.hashCode());
        result = prime * result + ((correctedTotalTime == null) ? 0 : correctedTotalTime.hashCode());
        result = prime * result
                + ((currentDistanceFoiledInMeters == null) ? 0 : currentDistanceFoiledInMeters.hashCode());
        result = prime * result
                + ((currentDurationFoiledInSeconds == null) ? 0 : currentDurationFoiledInSeconds.hashCode());
        result = prime * result + ((currentHeelInDegrees == null) ? 0 : currentHeelInDegrees.hashCode());
        result = prime * result + ((currentPitchInDegrees == null) ? 0 : currentPitchInDegrees.hashCode());
        result = prime * result + ((currentRideHeightInMeters == null) ? 0 : currentRideHeightInMeters.hashCode());
        result = prime * result
                + ((currentSpeedOverGroundInKnots == null) ? 0 : currentSpeedOverGroundInKnots.hashCode());
        result = prime * result + ((distanceTraveledInMeters == null) ? 0 : distanceTraveledInMeters.hashCode());
        result = prime * result + ((distanceTraveledIncludingGateStartInMeters == null) ? 0
                : distanceTraveledIncludingGateStartInMeters.hashCode());
        result = prime * result + ((estimatedTimeToNextWaypointInSeconds == null) ? 0
                : estimatedTimeToNextWaypointInSeconds.hashCode());
        result = prime * result + ((expeditionHolder == null) ? 0 : expeditionHolder.hashCode());
        result = prime * result + (finished ? 1231 : 1237);
        result = prime * result
                + ((gapChangeSinceLegStartInSeconds == null) ? 0 : gapChangeSinceLegStartInSeconds.hashCode());
        result = prime * result + ((gapToLeaderInSeconds == null) ? 0 : gapToLeaderInSeconds.hashCode());
        result = prime * result + ((legType == null) ? 0 : legType.hashCode());
        result = prime * result + ((numberOfManeuvers == null) ? 0 : numberOfManeuvers.hashCode());
        result = prime * result + rank;
        result = prime * result
                + ((sideToWhichMarkAtLegStartWasRounded == null) ? 0 : sideToWhichMarkAtLegStartWasRounded.hashCode());
        result = prime * result + (started ? 1231 : 1237);
        result = prime * result + ((timeInMilliseconds == null) ? 0 : timeInMilliseconds.hashCode());
        result = prime * result + ((velocityMadeGoodInKnots == null) ? 0 : velocityMadeGoodInKnots.hashCode());
        result = prime * result
                + ((windwardDistanceToGoInMeters == null) ? 0 : windwardDistanceToGoInMeters.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LegEntryDTO other = (LegEntryDTO) obj;
        if (averageAbsoluteCrossTrackErrorInMeters == null) {
            if (other.averageAbsoluteCrossTrackErrorInMeters != null)
                return false;
        } else if (!averageAbsoluteCrossTrackErrorInMeters.equals(other.averageAbsoluteCrossTrackErrorInMeters))
            return false;
        if (averageManeuverLossInMeters == null) {
            if (other.averageManeuverLossInMeters != null)
                return false;
        } else if (!averageManeuverLossInMeters.equals(other.averageManeuverLossInMeters))
            return false;
        if (averageSignedCrossTrackErrorInMeters == null) {
            if (other.averageSignedCrossTrackErrorInMeters != null)
                return false;
        } else if (!averageSignedCrossTrackErrorInMeters.equals(other.averageSignedCrossTrackErrorInMeters))
            return false;
        if (averageSpeedOverGroundInKnots == null) {
            if (other.averageSpeedOverGroundInKnots != null)
                return false;
        } else if (!averageSpeedOverGroundInKnots.equals(other.averageSpeedOverGroundInKnots))
            return false;
        if (correctedTotalTime == null) {
            if (other.correctedTotalTime != null)
                return false;
        } else if (!correctedTotalTime.equals(other.correctedTotalTime))
            return false;
        if (currentDistanceFoiledInMeters == null) {
            if (other.currentDistanceFoiledInMeters != null)
                return false;
        } else if (!currentDistanceFoiledInMeters.equals(other.currentDistanceFoiledInMeters))
            return false;
        if (currentDurationFoiledInSeconds == null) {
            if (other.currentDurationFoiledInSeconds != null)
                return false;
        } else if (!currentDurationFoiledInSeconds.equals(other.currentDurationFoiledInSeconds))
            return false;
        if (currentHeelInDegrees == null) {
            if (other.currentHeelInDegrees != null)
                return false;
        } else if (!currentHeelInDegrees.equals(other.currentHeelInDegrees))
            return false;
        if (currentPitchInDegrees == null) {
            if (other.currentPitchInDegrees != null)
                return false;
        } else if (!currentPitchInDegrees.equals(other.currentPitchInDegrees))
            return false;
        if (currentRideHeightInMeters == null) {
            if (other.currentRideHeightInMeters != null)
                return false;
        } else if (!currentRideHeightInMeters.equals(other.currentRideHeightInMeters))
            return false;
        if (currentSpeedOverGroundInKnots == null) {
            if (other.currentSpeedOverGroundInKnots != null)
                return false;
        } else if (!currentSpeedOverGroundInKnots.equals(other.currentSpeedOverGroundInKnots))
            return false;
        if (distanceTraveledInMeters == null) {
            if (other.distanceTraveledInMeters != null)
                return false;
        } else if (!distanceTraveledInMeters.equals(other.distanceTraveledInMeters))
            return false;
        if (distanceTraveledIncludingGateStartInMeters == null) {
            if (other.distanceTraveledIncludingGateStartInMeters != null)
                return false;
        } else if (!distanceTraveledIncludingGateStartInMeters.equals(other.distanceTraveledIncludingGateStartInMeters))
            return false;
        if (estimatedTimeToNextWaypointInSeconds == null) {
            if (other.estimatedTimeToNextWaypointInSeconds != null)
                return false;
        } else if (!estimatedTimeToNextWaypointInSeconds.equals(other.estimatedTimeToNextWaypointInSeconds))
            return false;
        if (expeditionHolder == null) {
            if (other.expeditionHolder != null)
                return false;
        } else if (!expeditionHolder.equals(other.expeditionHolder))
            return false;
        if (finished != other.finished)
            return false;
        if (gapChangeSinceLegStartInSeconds == null) {
            if (other.gapChangeSinceLegStartInSeconds != null)
                return false;
        } else if (!gapChangeSinceLegStartInSeconds.equals(other.gapChangeSinceLegStartInSeconds))
            return false;
        if (gapToLeaderInSeconds == null) {
            if (other.gapToLeaderInSeconds != null)
                return false;
        } else if (!gapToLeaderInSeconds.equals(other.gapToLeaderInSeconds))
            return false;
        if (legType != other.legType)
            return false;
        if (numberOfManeuvers == null) {
            if (other.numberOfManeuvers != null)
                return false;
        } else if (!numberOfManeuvers.equals(other.numberOfManeuvers))
            return false;
        if (rank != other.rank)
            return false;
        if (sideToWhichMarkAtLegStartWasRounded != other.sideToWhichMarkAtLegStartWasRounded)
            return false;
        if (started != other.started)
            return false;
        if (timeInMilliseconds == null) {
            if (other.timeInMilliseconds != null)
                return false;
        } else if (!timeInMilliseconds.equals(other.timeInMilliseconds))
            return false;
        if (velocityMadeGoodInKnots == null) {
            if (other.velocityMadeGoodInKnots != null)
                return false;
        } else if (!velocityMadeGoodInKnots.equals(other.velocityMadeGoodInKnots))
            return false;
        if (windwardDistanceToGoInMeters == null) {
            if (other.windwardDistanceToGoInMeters != null)
                return false;
        } else if (!windwardDistanceToGoInMeters.equals(other.windwardDistanceToGoInMeters))
            return false;
        return true;
    }
    
    private void ensureExpeditionHolder() {
        if (expeditionHolder == null) {
            expeditionHolder = new ExpeditionLegHolder();
        }
    }

    public void setExpeditionAWA(Double expeditionAWA) {
        if (expeditionAWA != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionAWA = expeditionAWA;
        }
    }

    public void setExpeditionAWS(Double expeditionAWS) {
        if (expeditionAWS != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionAWS = expeditionAWS;
        }
    }

    public void setExpeditionTWA(Double expeditionTWA) {
        if (expeditionTWA != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionTWA = expeditionTWA;
        }
    }

    public void setExpeditionTWS(Double expeditionTWS) {
        if (expeditionTWS != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionTWS = expeditionTWS;
        }
    }

    public void setExpeditionTWD(Double expeditionTWD) {
        if (expeditionTWD != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionTWD = expeditionTWD;
        }
    }

    public void setExpeditionTargTWA(Double expeditionTWA) {
        if (expeditionTWA != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionTWA = expeditionTWA;
        }
    }

    public void setExpeditionBoatSpeed(Double expeditionBoatSpeed) {
        if (expeditionBoatSpeed != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionBoatSpeed = expeditionBoatSpeed;
        }
    }

    public void setExpeditionTargBoatSpeed(Double expeditionTargBoatSpeed) {
        if (expeditionTargBoatSpeed != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionTargBoatSpeed = expeditionTargBoatSpeed;
        }
    }

    public void setExpeditionSOG(Double expeditionSOG) {
        if (expeditionSOG != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionSOG = expeditionSOG;
        }
    }

    public void setExpeditionCOG(Double expeditionCOG) {
        if (expeditionCOG != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionCOG = expeditionCOG;
        }
    }

    public void setExpeditionForestayLoad(Double expeditionForestayLoad) {
        if (expeditionForestayLoad != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionForestayLoad = expeditionForestayLoad;
        }
    }

    public void setExpeditionRake(Double expeditionRake) {
        if (expeditionRake != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionRake = expeditionRake;
        }
    }

    public void setExpeditionCourseDetail(Double expeditionCourseDetail) {
        if (expeditionCourseDetail != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionCourseDetail = expeditionCourseDetail;
        }
    }

    public void setExpeditionHeading(Double expeditionHeading) {
        if (expeditionHeading != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionHeading = expeditionHeading;
        }
    }

    public void setExpeditionVMG(Double expeditionVMG) {
        if (expeditionVMG != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionVMG = expeditionVMG;
        }
    }

    public void setExpeditionVMGTargVMGDelta(Double expeditionVMGTargVMGDelta) {
        if (expeditionVMGTargVMGDelta != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionVMGTargVMGDelta = expeditionVMGTargVMGDelta;
        }
    }

    public void setExpeditionRateOfTurn(Double expeditionRateOfTurn) {
        if (expeditionRateOfTurn != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionRateOfTurn = expeditionRateOfTurn;
        }
    }

    public void setExpeditionRudderAngle(Double expeditionRudderAngle) {
        if (expeditionRudderAngle != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionRudderAngle = expeditionRudderAngle;
        }
    }

    public void setExpeditionHeel(Double expeditionHeel) {
        if (expeditionHeel != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionHeel = expeditionHeel;
        }
    }

    public void setExpeditionTargetHeel(Double expeditionTargetHeel) {
        if (expeditionTargetHeel != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionTargetHeel = expeditionTargetHeel;
        }
    }

    public void setExpeditionTimeToPortLayline(Double expeditionTimeToPortLayline) {
        if (expeditionTimeToPortLayline != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionTimeToPortLayline = expeditionTimeToPortLayline;
        }
    }

    public void setExpeditionTimeToStbLayline(Double expeditionTimeToStbLayline) {
        if (expeditionTimeToStbLayline != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionTimeToStbLayline = expeditionTimeToStbLayline;
        }
    }

    public void setExpeditionDistToPortLayline(Double expeditionDistToPortLayline) {
        if (expeditionDistToPortLayline != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionDistToPortLayline = expeditionDistToPortLayline;
        }
    }

    public void setExpeditionDistToStbLayline(Double expeditionDistToStbLayline) {
        if (expeditionDistToStbLayline != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionDistToStbLayline = expeditionDistToStbLayline;
        }
    }

    public void setExpeditionTimeToGUN(Double expeditionTimeToGUN) {
        if (expeditionTimeToGUN != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionTimeToGUN = expeditionTimeToGUN;
        }
    }

    public void setExpeditionTimeToCommitteeBoat(Double expeditionTimeToCommitteeBoat) {
        if (expeditionTimeToCommitteeBoat != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionTimeToCommitteeBoat = expeditionTimeToCommitteeBoat;
        }
    }

    public void setExpeditionTimeToPin(Double expeditionTimeToPin) {
        if (expeditionTimeToPin != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionTimeToPin = expeditionTimeToPin;
        }
    }

    public void setExpeditionTimeToBurnToLine(Double expeditionTimeToBurnToLine) {
        if (expeditionTimeToBurnToLine != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionTimeToBurnToLine = expeditionTimeToBurnToLine;
        }
    }

    public void setExpeditionTimeToBurnToCommitteeBoat(Double expeditionTimeToCommitteeBoat) {
        if (expeditionTimeToCommitteeBoat != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionTimeToCommitteeBoat = expeditionTimeToCommitteeBoat;
        }
    }

    public void setExpeditionTimeToBurnToPin(Double expeditionTimeToBurnToPin) {
        if (expeditionTimeToBurnToPin != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionTimeToBurnToPin = expeditionTimeToBurnToPin;
        }
    }

    public void setExpeditionDistanceToCommitteeBoat(Double expeditionDistanceToCommitteeBoat) {
        if (expeditionDistanceToCommitteeBoat != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionDistanceToCommitteeBoat = expeditionDistanceToCommitteeBoat;
        }
    }

    public void setExpeditionDistanceToPinDetail(Double expeditionDistanceToPinDetail) {
        if (expeditionDistanceToPinDetail != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionDistanceToPinDetail = expeditionDistanceToPinDetail;
        }
    }

    public void setExpeditionDistanceBelowLine(Double expeditionDistanceBelowLine) {
        if (expeditionDistanceBelowLine != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionDistanceBelowLine = expeditionDistanceBelowLine;
        }
    }

    public void setExpeditionLineSquareForWindDirection(Double expeditionLineSquareForWindDirection) {
        if (expeditionLineSquareForWindDirection != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionLineSquareForWindDirection = expeditionLineSquareForWindDirection;
        }
    }
    
    public void setExpeditionBaroIfAvailable(Double expeditionBaro) {
        if (expeditionBaro != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionBaro = expeditionBaro;
        }        
    }

    public void setExpeditionLoadSIfAvailable(Double expeditionLoadS) {
        if (expeditionLoadS != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionLoadS = expeditionLoadS;
        }        
    }
    
    public void setExpeditionLoadPIfAvailable(Double expeditionLoadP) {
        if (expeditionLoadP != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionLoadP = expeditionLoadP;
        }        
    }

    public void setExpeditionJibCarPortIfAvailable(Double expeditionJibCarPort) {
        if (expeditionJibCarPort != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionJibCarPort = expeditionJibCarPort;
        }        
    }

    public void setExpeditionJibCarStbdIfAvailable(Double expeditionJibCarStbd) {
        if (expeditionJibCarStbd != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionJibCarStbd = expeditionJibCarStbd;
        }        
    }
    
    public void setExpeditionMastButtIfAvailable(Double expeditionMastButt) {
        if (expeditionMastButt != null) {
            ensureExpeditionHolder();
            expeditionHolder.expeditionMastButt = expeditionMastButt;
        }   
    }

    public Double getExpeditionAWA() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionAWA;
    }

    public Double getExpeditionAWS() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionAWS;
    }

    public Double getExpeditionBoatSpeed() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionBoatSpeed;
    }

    public Double getExpeditionCOG() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionCOG;
    }

    public Double getExpeditionDistanceBelowLine() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionDistanceBelowLine;
    }

    public Double getExpeditionDistanceToCommitteeBoat() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionDistanceToCommitteeBoat;
    }

    public Double getExpeditionDistToPortLayline() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionDistToPortLayline;
    }

    public Double getExpeditionDistToStbLayline() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionDistToStbLayline;
    }

    public Double getExpeditionVMG() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionVMG;
    }

    public Double getExpeditionTWS() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionTWS;
    }

    public Double getExpeditionTWD() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionTWD;
    }

    public Double getExpeditionTWA() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionTWA;
    }

    public Double getExpeditionTimeToStbLayline() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionTimeToStbLayline;
    }

    public Double getExpeditionTimeToPortLayline() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionTimeToPortLayline;
    }

    public Double getExpeditionTimeToPin() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionTimeToPin;
    }

    public Double getExpeditionTimeToGUN() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionTimeToGUN;
    }

    public Double getExpeditionTimeToCommitteeBoat() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionTimeToCommitteeBoat;
    }

    public Double getExpeditionTimeToBurnToPin() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionTimeToBurnToPin;
    }

    public Double getExpeditionTargTWA() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionTargTWA;
    }

    public Double getExpeditionTimeToBurnToCommitteeBoat() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionTimeToBurnToCommitteeBoat;
    }

    public Double getExpeditionTimeToBurnToLine() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionTimeToBurnToLine;
    }

    public Double getExpeditionTargetHeel() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionTargetHeel;
    }

    public Double getExpeditionTargBoatSpeed() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionTargBoatSpeed;
    }

    public Double getExpeditionSOG() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionSOG;
    }

    public Double getExpeditionRudderAngle() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionRudderAngle;
    }

    public Double getExpeditionRateOfTurn() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionRateOfTurn;
    }

    public Double getExpeditionRake() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionRake;
    }

    public Double getExpeditionLineSquareForWindDirection() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionLineSquareForWindDirection;
    }

    public Double getExpeditionHeading() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionHeading;
    }

    public Double getExpeditionForestayLoad() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionForestayLoad;
    }

    public Double getExpeditionHeel() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionHeel;
    }

    public Double getExpeditionCourseDetail() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionCourseDetail;
    }

    public Double getExpeditionDistanceToPinDetail() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionDistanceToPinDetail;
    }

    public Double getExpeditionVMGTargVMGDelta() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionVMGTargVMGDelta;
    }

    public Double getExpeditionBaro() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionBaro;
    }

    public Double getExpeditionLoadS() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionLoadS;
    }

    public Double getExpeditionLoadP() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionLoadP;
    }

    public Double getExpeditionJibCarPort() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionJibCarPort;
    }

    public Double getExpeditionJibCarStbd() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionJibCarStbd;
    }

    public Double getExpeditionMastButt() {
        return expeditionHolder == null ? null : expeditionHolder.expeditionMastButt;
    }
    
    public static class ExpeditionLegHolder implements Serializable {
        private static final long serialVersionUID = -2507536984016949734L;
        
        public Double expeditionMastButt;
        public Double expeditionJibCarStbd;
        public Double expeditionJibCarPort;
        public Double expeditionLoadP;
        public Double expeditionLoadS;
        public Double expeditionBaro;
        public Double expeditionAWA;
        public Double expeditionAWS;
        public Double expeditionBoatSpeed;
        public Double expeditionCOG;
        public Double expeditionDistanceBelowLine;
        public Double expeditionDistanceToCommitteeBoat;
        public Double expeditionDistToPortLayline;
        public Double expeditionDistToStbLayline;
        public Double expeditionVMG;
        public Double expeditionTWS;
        public Double expeditionTWD;
        public Double expeditionTWA;
        public Double expeditionTimeToStbLayline;
        public Double expeditionTimeToPortLayline;
        public Double expeditionTimeToPin;
        public Double expeditionTimeToGUN;
        public Double expeditionTimeToCommitteeBoat;
        public Double expeditionTimeToBurnToPin;
        public Double expeditionTargTWA;
        public Double expeditionTimeToBurnToCommitteeBoat;
        public Double expeditionTimeToBurnToLine;
        public Double expeditionTargetHeel;
        public Double expeditionTargBoatSpeed;
        public Double expeditionSOG;
        public Double expeditionRudderAngle;
        public Double expeditionRateOfTurn;
        public Double expeditionRake;
        public Double expeditionLineSquareForWindDirection;
        public Double expeditionHeading;
        public Double expeditionForestayLoad;
        public Double expeditionHeel;
        public Double expeditionCourseDetail;
        public Double expeditionDistanceToPinDetail;
        public Double expeditionVMGTargVMGDelta;
        
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((expeditionAWA == null) ? 0 : expeditionAWA.hashCode());
            result = prime * result + ((expeditionAWS == null) ? 0 : expeditionAWS.hashCode());
            result = prime * result + ((expeditionBaro == null) ? 0 : expeditionBaro.hashCode());
            result = prime * result + ((expeditionBoatSpeed == null) ? 0 : expeditionBoatSpeed.hashCode());
            result = prime * result + ((expeditionCOG == null) ? 0 : expeditionCOG.hashCode());
            result = prime * result + ((expeditionCourseDetail == null) ? 0 : expeditionCourseDetail.hashCode());
            result = prime * result
                    + ((expeditionDistToPortLayline == null) ? 0 : expeditionDistToPortLayline.hashCode());
            result = prime * result
                    + ((expeditionDistToStbLayline == null) ? 0 : expeditionDistToStbLayline.hashCode());
            result = prime * result
                    + ((expeditionDistanceBelowLine == null) ? 0 : expeditionDistanceBelowLine.hashCode());
            result = prime * result
                    + ((expeditionDistanceToCommitteeBoat == null) ? 0 : expeditionDistanceToCommitteeBoat.hashCode());
            result = prime * result
                    + ((expeditionDistanceToPinDetail == null) ? 0 : expeditionDistanceToPinDetail.hashCode());
            result = prime * result + ((expeditionForestayLoad == null) ? 0 : expeditionForestayLoad.hashCode());
            result = prime * result + ((expeditionHeading == null) ? 0 : expeditionHeading.hashCode());
            result = prime * result + ((expeditionHeel == null) ? 0 : expeditionHeel.hashCode());
            result = prime * result + ((expeditionJibCarPort == null) ? 0 : expeditionJibCarPort.hashCode());
            result = prime * result + ((expeditionJibCarStbd == null) ? 0 : expeditionJibCarStbd.hashCode());
            result = prime * result + ((expeditionLineSquareForWindDirection == null) ? 0
                    : expeditionLineSquareForWindDirection.hashCode());
            result = prime * result + ((expeditionLoadP == null) ? 0 : expeditionLoadP.hashCode());
            result = prime * result + ((expeditionLoadS == null) ? 0 : expeditionLoadS.hashCode());
            result = prime * result + ((expeditionMastButt == null) ? 0 : expeditionMastButt.hashCode());
            result = prime * result + ((expeditionRake == null) ? 0 : expeditionRake.hashCode());
            result = prime * result + ((expeditionRateOfTurn == null) ? 0 : expeditionRateOfTurn.hashCode());
            result = prime * result + ((expeditionRudderAngle == null) ? 0 : expeditionRudderAngle.hashCode());
            result = prime * result + ((expeditionSOG == null) ? 0 : expeditionSOG.hashCode());
            result = prime * result + ((expeditionTWA == null) ? 0 : expeditionTWA.hashCode());
            result = prime * result + ((expeditionTWD == null) ? 0 : expeditionTWD.hashCode());
            result = prime * result + ((expeditionTWS == null) ? 0 : expeditionTWS.hashCode());
            result = prime * result + ((expeditionTargBoatSpeed == null) ? 0 : expeditionTargBoatSpeed.hashCode());
            result = prime * result + ((expeditionTargTWA == null) ? 0 : expeditionTargTWA.hashCode());
            result = prime * result + ((expeditionTargetHeel == null) ? 0 : expeditionTargetHeel.hashCode());
            result = prime * result + ((expeditionTimeToBurnToCommitteeBoat == null) ? 0
                    : expeditionTimeToBurnToCommitteeBoat.hashCode());
            result = prime * result
                    + ((expeditionTimeToBurnToLine == null) ? 0 : expeditionTimeToBurnToLine.hashCode());
            result = prime * result + ((expeditionTimeToBurnToPin == null) ? 0 : expeditionTimeToBurnToPin.hashCode());
            result = prime * result
                    + ((expeditionTimeToCommitteeBoat == null) ? 0 : expeditionTimeToCommitteeBoat.hashCode());
            result = prime * result + ((expeditionTimeToGUN == null) ? 0 : expeditionTimeToGUN.hashCode());
            result = prime * result + ((expeditionTimeToPin == null) ? 0 : expeditionTimeToPin.hashCode());
            result = prime * result
                    + ((expeditionTimeToPortLayline == null) ? 0 : expeditionTimeToPortLayline.hashCode());
            result = prime * result
                    + ((expeditionTimeToStbLayline == null) ? 0 : expeditionTimeToStbLayline.hashCode());
            result = prime * result + ((expeditionVMG == null) ? 0 : expeditionVMG.hashCode());
            result = prime * result + ((expeditionVMGTargVMGDelta == null) ? 0 : expeditionVMGTargVMGDelta.hashCode());
            return result;
        }
        
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ExpeditionLegHolder other = (ExpeditionLegHolder) obj;
            if (expeditionAWA == null) {
                if (other.expeditionAWA != null)
                    return false;
            } else if (!expeditionAWA.equals(other.expeditionAWA))
                return false;
            if (expeditionAWS == null) {
                if (other.expeditionAWS != null)
                    return false;
            } else if (!expeditionAWS.equals(other.expeditionAWS))
                return false;
            if (expeditionBaro == null) {
                if (other.expeditionBaro != null)
                    return false;
            } else if (!expeditionBaro.equals(other.expeditionBaro))
                return false;
            if (expeditionBoatSpeed == null) {
                if (other.expeditionBoatSpeed != null)
                    return false;
            } else if (!expeditionBoatSpeed.equals(other.expeditionBoatSpeed))
                return false;
            if (expeditionCOG == null) {
                if (other.expeditionCOG != null)
                    return false;
            } else if (!expeditionCOG.equals(other.expeditionCOG))
                return false;
            if (expeditionCourseDetail == null) {
                if (other.expeditionCourseDetail != null)
                    return false;
            } else if (!expeditionCourseDetail.equals(other.expeditionCourseDetail))
                return false;
            if (expeditionDistToPortLayline == null) {
                if (other.expeditionDistToPortLayline != null)
                    return false;
            } else if (!expeditionDistToPortLayline.equals(other.expeditionDistToPortLayline))
                return false;
            if (expeditionDistToStbLayline == null) {
                if (other.expeditionDistToStbLayline != null)
                    return false;
            } else if (!expeditionDistToStbLayline.equals(other.expeditionDistToStbLayline))
                return false;
            if (expeditionDistanceBelowLine == null) {
                if (other.expeditionDistanceBelowLine != null)
                    return false;
            } else if (!expeditionDistanceBelowLine.equals(other.expeditionDistanceBelowLine))
                return false;
            if (expeditionDistanceToCommitteeBoat == null) {
                if (other.expeditionDistanceToCommitteeBoat != null)
                    return false;
            } else if (!expeditionDistanceToCommitteeBoat.equals(other.expeditionDistanceToCommitteeBoat))
                return false;
            if (expeditionDistanceToPinDetail == null) {
                if (other.expeditionDistanceToPinDetail != null)
                    return false;
            } else if (!expeditionDistanceToPinDetail.equals(other.expeditionDistanceToPinDetail))
                return false;
            if (expeditionForestayLoad == null) {
                if (other.expeditionForestayLoad != null)
                    return false;
            } else if (!expeditionForestayLoad.equals(other.expeditionForestayLoad))
                return false;
            if (expeditionHeading == null) {
                if (other.expeditionHeading != null)
                    return false;
            } else if (!expeditionHeading.equals(other.expeditionHeading))
                return false;
            if (expeditionHeel == null) {
                if (other.expeditionHeel != null)
                    return false;
            } else if (!expeditionHeel.equals(other.expeditionHeel))
                return false;
            if (expeditionJibCarPort == null) {
                if (other.expeditionJibCarPort != null)
                    return false;
            } else if (!expeditionJibCarPort.equals(other.expeditionJibCarPort))
                return false;
            if (expeditionJibCarStbd == null) {
                if (other.expeditionJibCarStbd != null)
                    return false;
            } else if (!expeditionJibCarStbd.equals(other.expeditionJibCarStbd))
                return false;
            if (expeditionLineSquareForWindDirection == null) {
                if (other.expeditionLineSquareForWindDirection != null)
                    return false;
            } else if (!expeditionLineSquareForWindDirection.equals(other.expeditionLineSquareForWindDirection))
                return false;
            if (expeditionLoadP == null) {
                if (other.expeditionLoadP != null)
                    return false;
            } else if (!expeditionLoadP.equals(other.expeditionLoadP))
                return false;
            if (expeditionLoadS == null) {
                if (other.expeditionLoadS != null)
                    return false;
            } else if (!expeditionLoadS.equals(other.expeditionLoadS))
                return false;
            if (expeditionMastButt == null) {
                if (other.expeditionMastButt != null)
                    return false;
            } else if (!expeditionMastButt.equals(other.expeditionMastButt))
                return false;
            if (expeditionRake == null) {
                if (other.expeditionRake != null)
                    return false;
            } else if (!expeditionRake.equals(other.expeditionRake))
                return false;
            if (expeditionRateOfTurn == null) {
                if (other.expeditionRateOfTurn != null)
                    return false;
            } else if (!expeditionRateOfTurn.equals(other.expeditionRateOfTurn))
                return false;
            if (expeditionRudderAngle == null) {
                if (other.expeditionRudderAngle != null)
                    return false;
            } else if (!expeditionRudderAngle.equals(other.expeditionRudderAngle))
                return false;
            if (expeditionSOG == null) {
                if (other.expeditionSOG != null)
                    return false;
            } else if (!expeditionSOG.equals(other.expeditionSOG))
                return false;
            if (expeditionTWA == null) {
                if (other.expeditionTWA != null)
                    return false;
            } else if (!expeditionTWA.equals(other.expeditionTWA))
                return false;
            if (expeditionTWD == null) {
                if (other.expeditionTWD != null)
                    return false;
            } else if (!expeditionTWD.equals(other.expeditionTWD))
                return false;
            if (expeditionTWS == null) {
                if (other.expeditionTWS != null)
                    return false;
            } else if (!expeditionTWS.equals(other.expeditionTWS))
                return false;
            if (expeditionTargBoatSpeed == null) {
                if (other.expeditionTargBoatSpeed != null)
                    return false;
            } else if (!expeditionTargBoatSpeed.equals(other.expeditionTargBoatSpeed))
                return false;
            if (expeditionTargTWA == null) {
                if (other.expeditionTargTWA != null)
                    return false;
            } else if (!expeditionTargTWA.equals(other.expeditionTargTWA))
                return false;
            if (expeditionTargetHeel == null) {
                if (other.expeditionTargetHeel != null)
                    return false;
            } else if (!expeditionTargetHeel.equals(other.expeditionTargetHeel))
                return false;
            if (expeditionTimeToBurnToCommitteeBoat == null) {
                if (other.expeditionTimeToBurnToCommitteeBoat != null)
                    return false;
            } else if (!expeditionTimeToBurnToCommitteeBoat.equals(other.expeditionTimeToBurnToCommitteeBoat))
                return false;
            if (expeditionTimeToBurnToLine == null) {
                if (other.expeditionTimeToBurnToLine != null)
                    return false;
            } else if (!expeditionTimeToBurnToLine.equals(other.expeditionTimeToBurnToLine))
                return false;
            if (expeditionTimeToBurnToPin == null) {
                if (other.expeditionTimeToBurnToPin != null)
                    return false;
            } else if (!expeditionTimeToBurnToPin.equals(other.expeditionTimeToBurnToPin))
                return false;
            if (expeditionTimeToCommitteeBoat == null) {
                if (other.expeditionTimeToCommitteeBoat != null)
                    return false;
            } else if (!expeditionTimeToCommitteeBoat.equals(other.expeditionTimeToCommitteeBoat))
                return false;
            if (expeditionTimeToGUN == null) {
                if (other.expeditionTimeToGUN != null)
                    return false;
            } else if (!expeditionTimeToGUN.equals(other.expeditionTimeToGUN))
                return false;
            if (expeditionTimeToPin == null) {
                if (other.expeditionTimeToPin != null)
                    return false;
            } else if (!expeditionTimeToPin.equals(other.expeditionTimeToPin))
                return false;
            if (expeditionTimeToPortLayline == null) {
                if (other.expeditionTimeToPortLayline != null)
                    return false;
            } else if (!expeditionTimeToPortLayline.equals(other.expeditionTimeToPortLayline))
                return false;
            if (expeditionTimeToStbLayline == null) {
                if (other.expeditionTimeToStbLayline != null)
                    return false;
            } else if (!expeditionTimeToStbLayline.equals(other.expeditionTimeToStbLayline))
                return false;
            if (expeditionVMG == null) {
                if (other.expeditionVMG != null)
                    return false;
            } else if (!expeditionVMG.equals(other.expeditionVMG))
                return false;
            if (expeditionVMGTargVMGDelta == null) {
                if (other.expeditionVMGTargVMGDelta != null)
                    return false;
            } else if (!expeditionVMGTargVMGDelta.equals(other.expeditionVMGTargVMGDelta))
                return false;
            return true;
        }
    }
}
