package com.sap.sailing.domain.common.dto;

import java.io.Serializable;
import java.util.List;

import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Speed;
import com.sap.sse.common.impl.MillisecondsDurationImpl;

/**
 * Holds a single competitor's scoring details for a single race. It may optionally contain
 * a list of {@link LegEntryDTO} objects providing details about the individual legs sailed
 * during the race.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class LeaderboardEntryDTO implements Serializable {
    private static final long serialVersionUID = -4678693781217869837L;

    /**
     * Identifies the race in which the competitor achieved this score. This makes it possible to find out in which
     * fleet the competitor started in this column.
     */
    public RaceIdentifier race;

    /** the boat of the competitor in case of regattas where boat can change during racing, null otherwise */
    public BoatDTO boat;

    /**
     * Either <code>null</code> in case no max points, or one of "DNS", "DNF", "OCS", "DND", "RAF", "BFD", "DNC", or "DSQ"
     */
    public MaxPointsReason reasonForMaxPoints;
    
    public Double totalPoints;
    public Double totalPointsUncorrected;
    
    public Double incrementalScoreCorrectionInPoints;
    
    /**
     * Tells if the total points have been overridden by a score correction. Can be used to render differently in editing environment.
     */
    public boolean totalPointsCorrected;
    
    public Double netPoints;
    
    public boolean discarded;
    
    public Double windwardDistanceToCompetitorFarthestAheadInMeters;
    
    public Double averageAbsoluteCrossTrackErrorInMeters;
    
    public Double averageSignedCrossTrackErrorInMeters;
    
    public Double distanceToStartLineFiveSecondsBeforeStartInMeters;
    
    public Double speedOverGroundFiveSecondsBeforeStartInKnots;
    
    public Double distanceToStartLineAtStartOfRaceInMeters;
    
    public Double speedOverGroundAtStartOfRaceInKnots;
    
    public Double speedOverGroundAtPassingStartWaypointInKnots;
    
    public Double timeBetweenRaceStartAndCompetitorStartInSeconds;
    
    public Double distanceToStarboardSideOfStartLineInMeters;
    
    public Tack startTack;
    
    /**
     * If we have GPS data for the competitor for whom this is a leaderboard entry, tells the time since the last
     * non-extrapolated GPS fix that was really received from the tracking device at or before the time point for which
     * the leaderboard was queried. The user interface may---particularly in live mode---choose to visualize the time
     * that passed between the last fix and the query time point for which this entry was created.
     */
    public Double timeSinceLastPositionFixInSeconds;
    
    /**
     * For the competitor's track in the race represented by this object, if a track is present and the track has more
     * than one fix, this field tells the average duration between two fixes on the competitor's track.
     */
    public Duration averageSamplingInterval;
    
    public Double averageRideHeightInMeters;
    
    /**
     * The time gap to the competitor leading the race; for one-design races this is the time the competitor is expected
     * to need to reach the leader's (windward) position (if in the same leg) or the difference between the time at
     * which the competitor is expected to arrive at the next mark and the time when the leader had passed that mark.
     * <p>
     * 
     * For handicap ranking, things are more complicated. All competitors are projected to the boat farthest ahead (the
     * "fastest" boat), using their average VMG in the current leg, and if the fastest boat is no longer in the same
     * leg, using the same handicapped performance as the fastest boat for subsequent legs. This gives an actual arrival
     * time at the fastest boat's current (windward) position, and the corrected time can be computed for all
     * competitors, using the same total windward distance traveled (namely that leading up to the fastest boat's
     * current position). This enables sorting the competitors by these corrected times, yielding a "leader" of the
     * race. This field then describes the actual duration that this competitor would have had to be earlier where she
     * is now in order to rank equal in corrected time with the leader at the fastest boat's current position.
     * <p>
     * 
     * Note that for handicap ranking, this metric can differ from the leg-specific
     * {@link LegEntryDTO#gapToLeaderInSeconds} even for the current leg because the leg's gap metric only considers the
     * race up to the leg's end, regardless of where the leading and fastest boat are.
     */
    public Duration gapToLeaderInOwnTime;
    
    /**
     * The time sailed since the race start time. When the competitor represented by this record has finished the
     * race, this will be the duration between race start time and the time when the competitor finished the race. 
     */
    public Duration timeSailedSinceRaceStart;

    /**
     * The corrected time spent in the race; usually based on the current time and distance, calculated by the {@link RankingMetric}.
     * For one-design classes this equals the time spent in the race.
     */
    public Duration calculatedTime;
    
    /**
     * If this is a leaderboard entry for an ORC Polar Curve Scoring (PCS) race,
     * the field holds the so-called "implied wind" as a speed. It is the wind speed with which,
     * according to its polar, the competitor would have sailed the part of the course sailed
     * so far in the time elapsed so far. For ORC PCS before 2015 this was the primary ranking
     * criterion. Since 2015, however, implied wind has lost in significance because it is
     * used only to compute a default wind speed by maximizing all implied wind values of all
     * competitors in the race, and then using this wind speed to determine the time allowance
     * for each competitor so that a different between time elapsed and the allowance can then
     * be used for ranking.
     */
    public Speed impliedWind;
    
    /**
     * The corrections applied to the time and distance sailed when the competitor would have reached the
     * competitor farthest ahead, based on average VMG on the current leg and equal performance to the boat
     * farthest ahead for all subsequent legs.
     */
    public Duration calculatedTimeAtEstimatedArrivalAtCompetitorFarthestAhead;
    
    public SpeedWithBearing currentSpeedAndCourseOverGround;

    /**
     * If <code>null</code>, no leg details are known yet, the race is not being tracked or the details
     * haven't been requested from the server yet. Otherwise, the list holds one entry per <code>Leg</code> of the
     * <code>Course</code> being sailed in the race for which this object holds the scoring details.
     */
    public List<LegEntryDTO> legDetails;

    /**
     * <code>null</code>, if the fleet couldn't be determined, e.g., because the tracked race isn't known and therefore
     * the link to the fleet is not known; otherwise the description of the fleet in which the competitor scored this
     * entry
     */
    public FleetDTO fleet;
    
    public ExpeditionDataHolder expeditionDataHolder;

    public Bearing heel;

    public Bearing pitch;

    public Double percentTargetBoatSpeed;

    public LeaderboardEntryDTO() { }
    
    public boolean hasScoreCorrection() {
        return totalPointsCorrected || (reasonForMaxPoints != null && reasonForMaxPoints != MaxPointsReason.NONE);
    }
    
    public Duration getTimeSailed() {
        final Duration result;
        if (legDetails != null) {
            long timeInMilliseconds = 0;
            for (LegEntryDTO legDetail : legDetails) {
                if (legDetail != null) {
                    if (legDetail.distanceTraveledInMeters != null && legDetail.timeInMilliseconds != null) {
                        timeInMilliseconds += legDetail.timeInMilliseconds;
                    } else {
                        timeInMilliseconds = 0;
                        break;
                    }
                }
            }
            result = new MillisecondsDurationImpl(timeInMilliseconds);
        } else {
            result = null;
        }
        return result;
    }
    
    public Distance getDistanceTraveled() {
        Distance result = null;
        if (legDetails != null) {
            for (LegEntryDTO legDetail : legDetails) {
                if (legDetail != null) {
                    if (legDetail.distanceTraveledInMeters != null) {
                        if (result == null) {
                            result = Distance.NULL;
                        }
                        result = result.add(new MeterDistance(legDetail.distanceTraveledInMeters));
                    }
                }
            }
        }
        return result;
    }
    
    public Duration getDurationFoiled() {
        final Duration result;
        if (legDetails != null) {
            Double acc = null;
            for (LegEntryDTO legDetail : legDetails) {
                if (legDetail != null) {
                    if (legDetail.currentDurationFoiledInSeconds != null) {
                        if (acc == null) {
                            acc = 0.0;
                        }
                        acc += legDetail.currentDurationFoiledInSeconds;
                    } else {
                        acc = null;
                        break;
                    }
                }
            }
            result = acc == null ? null : new MillisecondsDurationImpl((long) (acc*1000.));
        } else {
            result = null;
        }
        return result;
    }
    
    public Distance getDistanceFoiled() {
        Distance result = null;
        if (legDetails != null) {
            for (LegEntryDTO legDetail : legDetails) {
                if (legDetail != null) {
                    if (legDetail.currentDistanceFoiledInMeters != null) {
                        if (result == null) {
                            result = Distance.NULL;
                        }
                        result = result.add(new MeterDistance(legDetail.currentDistanceFoiledInMeters));
                    }
                }
            }
        }
        return result;
    }
    
    public int getOneBasedCurrentLegNumber() {
        int result = 0;
        if (legDetails != null && !legDetails.isEmpty()) {
            for (LegEntryDTO legDetail : legDetails) {
                if (legDetail != null && legDetail.started) {
                    result++;
                } else {
                    break;
                }
            }
        }
        return result;
    }
    
    private void ensureExpeditionHolder() {
        if (expeditionDataHolder == null) {
            expeditionDataHolder = new ExpeditionDataHolder();
        }
    }

    public void setExpeditionAWA(Double expeditionAWA) {
        if (expeditionAWA != null) {
            ensureExpeditionHolder();
            expeditionDataHolder.expeditionAWA = expeditionAWA;
        }
    }

    public void setExpeditionAWS(Double expeditionAWS) {
        if (expeditionAWS != null) {
            ensureExpeditionHolder();
            expeditionDataHolder.expeditionAWS = expeditionAWS;
        }
    }

    public void setExpeditionTWA(Double expeditionTWA) {
        if (expeditionTWA != null) {
            ensureExpeditionHolder();
            expeditionDataHolder.expeditionTWA = expeditionTWA;
        }
    }

    public void setExpeditionTWS(Double expeditionTWS) {
        if (expeditionTWS != null) {
            ensureExpeditionHolder();
            expeditionDataHolder.expeditionTWS = expeditionTWS;
        }
    }

    public void setExpeditionTWD(Double expeditionTWD) {
        if (expeditionTWD != null) {
            ensureExpeditionHolder();
            expeditionDataHolder.expeditionTWD = expeditionTWD;
        }
    }

    public void setExpeditionBoatSpeed(Double expeditionBSP) {
        if (expeditionBSP != null) {
            ensureExpeditionHolder();
            expeditionDataHolder.expeditionBoatSpeed = expeditionBSP;
        }
    }

    public void setExpeditionTargBoatSpeed(Double expeditionBSP_TR) {
        if (expeditionBSP_TR != null) {
            ensureExpeditionHolder();
            expeditionDataHolder.expeditionTargBoatSpeed = expeditionBSP_TR;
        }
    }

    public void setExpeditionSOG(Double expeditionSOG) {
        if (expeditionSOG != null) {
            ensureExpeditionHolder();
            expeditionDataHolder.expeditionSOG = expeditionSOG;
        }
    }

    public void setExpeditionCOG(Double expeditionCOG) {
        if (expeditionCOG != null) {
            ensureExpeditionHolder();
            expeditionDataHolder.expeditionCOG = expeditionCOG;
        }
    }

    public void setExpeditionForestayLoad(Double expeditionForestayLoad) {
        if (expeditionForestayLoad != null) {
            ensureExpeditionHolder();
            expeditionDataHolder.expeditionForestayLoad = expeditionForestayLoad;
        }
    }

    public void setExpeditionRake(Double expeditionRake) {
        if (expeditionRake != null) {
            ensureExpeditionHolder();
            expeditionDataHolder.expeditionRake = expeditionRake;
        }
    }

    public void setExpeditionHeading(Double expeditionHDG) {
        if (expeditionHDG != null) {
            ensureExpeditionHolder();
            expeditionDataHolder.expeditionHeading = expeditionHDG;
        }
    }

    public void setExpeditionHeel(Double expeditionHeel) {
        if (expeditionHeel != null) {
            ensureExpeditionHolder();
            expeditionDataHolder.expeditionHeel = expeditionHeel;
        }
    }

    public void setExpeditionTargetHeel(Double expeditionTG_Heell) {
        if (expeditionTG_Heell != null) {
            ensureExpeditionHolder();
            expeditionDataHolder.expeditionTargetHeel = expeditionTG_Heell;
        }
    }

    public void setExpeditionTimeToGunInSeconds(Double expeditionTmToGunInSeconds) {
        if (expeditionTmToGunInSeconds != null) {
            ensureExpeditionHolder();
            expeditionDataHolder.expeditionTimeToGunInSeconds = expeditionTmToGunInSeconds;
        }
    }

    public void setExpeditionTimeToBurnToLineInSeconds(Double expeditionTmToBurnInSeconds) {
        if (expeditionTmToBurnInSeconds != null) {
            ensureExpeditionHolder();
            expeditionDataHolder.expeditionTimeToBurnToLineInSeconds = expeditionTmToBurnInSeconds;
        }
    }

    public void setExpeditionDistanceBelowLineInMeters(Double expeditionBelowLnInMeters) {
        if (expeditionBelowLnInMeters != null) {
            ensureExpeditionHolder();
            expeditionDataHolder.expeditionDistanceBelowLine = expeditionBelowLnInMeters;
        }
    }

    public void setExpeditionCourseDetail(Double expeditionCourse) {
        if (expeditionCourse != null) {
            ensureExpeditionHolder();
            expeditionDataHolder.expeditionCourseDetail = expeditionCourse;
        }
    }
    
    public void setExpeditionBaro(Double expeditionBARO) {
        if (expeditionBARO != null) {
            ensureExpeditionHolder();
            expeditionDataHolder.expeditionBARO = expeditionBARO;
        }        
    }

    public void setExpeditionLoadP(Double expeditionLoadP) {
        if (expeditionLoadP != null) {
            ensureExpeditionHolder();
            expeditionDataHolder.expeditionLoadP = expeditionLoadP;
        }        
    }

    public void setExpeditionLoadS(Double expeditionLoadS) {
        if (expeditionLoadS != null) {
            ensureExpeditionHolder();
            expeditionDataHolder.expeditionLoadS = expeditionLoadS;
        }        
    }

    public void setExpeditionJibCarPort(Double expeditionJibCarPort) {
        if (expeditionJibCarPort != null) {
            ensureExpeditionHolder();
            expeditionDataHolder.expeditionJibCarPort = expeditionJibCarPort;
        }        
    }

    public void setExpeditionJibCarStbd(Double expeditionJibCarStbd) {
        if (expeditionJibCarStbd != null) {
            ensureExpeditionHolder();
            expeditionDataHolder.expeditionJibCarStbd = expeditionJibCarStbd;
        }        
    }
    

    public void setExpeditionMastButt(Double expeditionMastButt) {
        if (expeditionMastButt != null) {
            ensureExpeditionHolder();
            expeditionDataHolder.expeditionMastButt = expeditionMastButt;
        }           
    }
    
    public void setExpeditionRateOfTurn(Double expeditionRateOfTurn) {
        if (expeditionRateOfTurn != null) {
            ensureExpeditionHolder();
            expeditionDataHolder.expeditionRateOfTurn = expeditionRateOfTurn;
        }           
    }
    
    public Double getExpeditionAWA() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionAWA;
    }

    public Double getExpeditionAWS() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionAWS;
    }

    public Double getExpeditionBoatSpeed() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionBoatSpeed;
    }

    public Double getExpeditionCOG() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionCOG;
    }

    public Double getExpeditionDistanceBelowLine() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionDistanceBelowLine;
    }

    public Double getExpeditionDistanceToCommitteeBoat() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionDistanceToCommitteeBoat;
    }

    public Double getExpeditionDistToPortLayline() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionDistToPortLayline;
    }

    public Double getExpeditionDistToStbLayline() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionDistToStbLayline;
    }

    public Double getExpeditionVMG() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionVMG;
    }

    public Double getExpeditionTWS() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionTWS;
    }

    public Double getExpeditionTWD() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionTWD;
    }

    public Double getExpeditionTWA() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionTWA;
    }

    public Double getExpeditionTimeToStbLayline() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionTimeToStbLayline;
    }

    public Double getExpeditionTimeToPortLayline() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionTimeToPortLayline;
    }

    public Double getExpeditionTimeToPin() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionTimeToPin;
    }

    public Double getExpeditionTimeToGun() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionTimeToGunInSeconds;
    }

    public Double getExpeditionTimeToCommitteeBoat() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionTimeToCommitteeBoat;
    }

    public Double getExpeditionTimeToBurnToPin() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionTimeToBurnToPin;
    }

    public Double getExpeditionTargTWA() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionTargTWA;
    }

    public Double getExpeditionTimeToBurnToCommitteeBoat() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionTimeToBurnToCommitteeBoat;
    }

    public Double getExpeditionTimeToBurnToLine() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionTimeToBurnToLineInSeconds;
    }

    public Double getExpeditionTargetHeel() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionTargetHeel;
    }

    public Double getExpeditionTargBoatSpeed() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionTargBoatSpeed;
    }

    public Double getExpeditionSOG() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionSOG;
    }

    public Double getExpeditionRudderAngle() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionRudderAngle;
    }

    public Double getExpeditionRateOfTurn() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionRateOfTurn;
    }

    public Double getExpeditionRake() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionRake;
    }

    public Double getExpeditionLineSquareForWindDirection() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionLineSquareForWindDirection;
    }

    public Double getExpeditionHeading() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionHeading;
    }

    public Double getExpeditionForestayLoad() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionForestayLoad;
    }

    public Double getExpeditionHeel() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionHeel;
    }

    public Double getExpeditionCourseDetail() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionCourseDetail;
    }

    public Double getExpeditionDistanceToPinDetail() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionDistanceToPinDetail;
    }

    public Double getExpeditionVMGTargVMGDelta() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionVMGTargVMGDelta;
    }

    public Double getExpeditionBaro() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionBARO;
    }

    public Double getExpeditionLoadP() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionLoadP;
    }

    public Double getExpeditionLoadS() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionLoadS;
    }

    public Double getExpeditionJibCarPort() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionJibCarPort;
    }

    public Double getExpeditionJibCarStbd() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionJibCarStbd;
    }

    public Double getExpeditionMastButt() {
        return expeditionDataHolder == null ? null : expeditionDataHolder.expeditionMastButt;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((averageAbsoluteCrossTrackErrorInMeters == null) ? 0
                : averageAbsoluteCrossTrackErrorInMeters.hashCode());
        result = prime * result + ((averageRideHeightInMeters == null) ? 0 : averageRideHeightInMeters.hashCode());
        result = prime * result + ((averageSamplingInterval == null) ? 0 : averageSamplingInterval.hashCode());
        result = prime * result + ((averageSignedCrossTrackErrorInMeters == null) ? 0
                : averageSignedCrossTrackErrorInMeters.hashCode());
        result = prime * result + ((boat == null) ? 0 : boat.hashCode());
        result = prime * result + ((timeSailedSinceRaceStart == null) ? 0 : timeSailedSinceRaceStart.hashCode());
        result = prime * result + ((calculatedTime == null) ? 0 : calculatedTime.hashCode());
        result = prime * result + ((calculatedTimeAtEstimatedArrivalAtCompetitorFarthestAhead == null) ? 0
                : calculatedTimeAtEstimatedArrivalAtCompetitorFarthestAhead.hashCode());
        result = prime * result + ((currentSpeedAndCourseOverGround == null) ? 0
                : currentSpeedAndCourseOverGround.hashCode());
        result = prime * result + (discarded ? 1231 : 1237);
        result = prime * result + ((distanceToStarboardSideOfStartLineInMeters == null) ? 0
                : distanceToStarboardSideOfStartLineInMeters.hashCode());
        result = prime * result + ((distanceToStartLineAtStartOfRaceInMeters == null) ? 0
                : distanceToStartLineAtStartOfRaceInMeters.hashCode());
        result = prime * result + ((distanceToStartLineFiveSecondsBeforeStartInMeters == null) ? 0
                : distanceToStartLineFiveSecondsBeforeStartInMeters.hashCode());
        result = prime * result + ((expeditionDataHolder == null) ? 0 : expeditionDataHolder.hashCode());
        result = prime * result + ((fleet == null) ? 0 : fleet.hashCode());
        result = prime * result + ((gapToLeaderInOwnTime == null) ? 0 : gapToLeaderInOwnTime.hashCode());
        result = prime * result + ((heel == null) ? 0 : heel.hashCode());
        result = prime * result + ((legDetails == null) ? 0 : legDetails.hashCode());
        result = prime * result + ((netPoints == null) ? 0 : netPoints.hashCode());
        result = prime * result + ((pitch == null) ? 0 : pitch.hashCode());
        result = prime * result + ((percentTargetBoatSpeed == null) ? 0 : percentTargetBoatSpeed.hashCode());
        result = prime * result + ((race == null) ? 0 : race.hashCode());
        result = prime * result + ((reasonForMaxPoints == null) ? 0 : reasonForMaxPoints.hashCode());
        result = prime * result + ((speedOverGroundAtPassingStartWaypointInKnots == null) ? 0
                : speedOverGroundAtPassingStartWaypointInKnots.hashCode());
        result = prime * result
                + ((speedOverGroundAtStartOfRaceInKnots == null) ? 0 : speedOverGroundAtStartOfRaceInKnots.hashCode());
        result = prime * result + ((speedOverGroundFiveSecondsBeforeStartInKnots == null) ? 0
                : speedOverGroundFiveSecondsBeforeStartInKnots.hashCode());
        result = prime * result + ((startTack == null) ? 0 : startTack.hashCode());
        result = prime * result + ((timeBetweenRaceStartAndCompetitorStartInSeconds == null) ? 0
                : timeBetweenRaceStartAndCompetitorStartInSeconds.hashCode());
        result = prime * result
                + ((timeSinceLastPositionFixInSeconds == null) ? 0 : timeSinceLastPositionFixInSeconds.hashCode());
        result = prime * result + ((totalPoints == null) ? 0 : totalPoints.hashCode());
        result = prime * result + (totalPointsCorrected ? 1231 : 1237);
        result = prime * result + ((totalPointsUncorrected == null) ? 0 : totalPointsUncorrected.hashCode());
        result = prime * result + ((incrementalScoreCorrectionInPoints == null) ? 0 : incrementalScoreCorrectionInPoints.hashCode());
        result = prime * result + ((windwardDistanceToCompetitorFarthestAheadInMeters == null) ? 0
                : windwardDistanceToCompetitorFarthestAheadInMeters.hashCode());
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
        LeaderboardEntryDTO other = (LeaderboardEntryDTO) obj;
        if (averageAbsoluteCrossTrackErrorInMeters == null) {
            if (other.averageAbsoluteCrossTrackErrorInMeters != null)
                return false;
        } else if (!averageAbsoluteCrossTrackErrorInMeters.equals(other.averageAbsoluteCrossTrackErrorInMeters))
            return false;
        if (averageRideHeightInMeters == null) {
            if (other.averageRideHeightInMeters != null)
                return false;
        } else if (!averageRideHeightInMeters.equals(other.averageRideHeightInMeters))
            return false;
        if (averageSamplingInterval == null) {
            if (other.averageSamplingInterval != null)
                return false;
        } else if (!averageSamplingInterval.equals(other.averageSamplingInterval))
            return false;
        if (averageSignedCrossTrackErrorInMeters == null) {
            if (other.averageSignedCrossTrackErrorInMeters != null)
                return false;
        } else if (!averageSignedCrossTrackErrorInMeters.equals(other.averageSignedCrossTrackErrorInMeters))
            return false;
        if (boat == null) {
            if (other.boat != null)
                return false;
        } else if (!boat.equals(other.boat))
            return false;
        if (timeSailedSinceRaceStart == null) {
            if (other.timeSailedSinceRaceStart != null)
                return false;
        } else if (!timeSailedSinceRaceStart.equals(other.timeSailedSinceRaceStart))
            return false;
        if (calculatedTime == null) {
            if (other.calculatedTime != null)
                return false;
        } else if (!calculatedTime.equals(other.calculatedTime))
            return false;
        if (calculatedTimeAtEstimatedArrivalAtCompetitorFarthestAhead == null) {
            if (other.calculatedTimeAtEstimatedArrivalAtCompetitorFarthestAhead != null)
                return false;
        } else if (!calculatedTimeAtEstimatedArrivalAtCompetitorFarthestAhead
                .equals(other.calculatedTimeAtEstimatedArrivalAtCompetitorFarthestAhead))
            return false;
        if (currentSpeedAndCourseOverGround == null) {
            if (other.currentSpeedAndCourseOverGround != null)
                return false;
        } else if (!currentSpeedAndCourseOverGround
                .equals(other.currentSpeedAndCourseOverGround))
            return false;
        if (discarded != other.discarded)
            return false;
        if (distanceToStarboardSideOfStartLineInMeters == null) {
            if (other.distanceToStarboardSideOfStartLineInMeters != null)
                return false;
        } else if (!distanceToStarboardSideOfStartLineInMeters.equals(other.distanceToStarboardSideOfStartLineInMeters))
            return false;
        if (distanceToStartLineAtStartOfRaceInMeters == null) {
            if (other.distanceToStartLineAtStartOfRaceInMeters != null)
                return false;
        } else if (!distanceToStartLineAtStartOfRaceInMeters.equals(other.distanceToStartLineAtStartOfRaceInMeters))
            return false;
        if (distanceToStartLineFiveSecondsBeforeStartInMeters == null) {
            if (other.distanceToStartLineFiveSecondsBeforeStartInMeters != null)
                return false;
        } else if (!distanceToStartLineFiveSecondsBeforeStartInMeters
                .equals(other.distanceToStartLineFiveSecondsBeforeStartInMeters))
            return false;
        if (expeditionDataHolder == null) {
            if (other.expeditionDataHolder != null)
                return false;
        } else if (!expeditionDataHolder.equals(other.expeditionDataHolder))
            return false;
        if (fleet == null) {
            if (other.fleet != null)
                return false;
        } else if (!fleet.equals(other.fleet))
            return false;
        if (gapToLeaderInOwnTime == null) {
            if (other.gapToLeaderInOwnTime != null)
                return false;
        } else if (!gapToLeaderInOwnTime.equals(other.gapToLeaderInOwnTime))
            return false;
        if (heel == null) {
            if (other.heel != null)
                return false;
        } else if (!heel.equals(other.heel))
            return false;
        if (legDetails == null) {
            if (other.legDetails != null)
                return false;
        } else if (!legDetails.equals(other.legDetails))
            return false;
        if (netPoints == null) {
            if (other.netPoints != null)
                return false;
        } else if (!netPoints.equals(other.netPoints))
            return false;
        if (pitch == null) {
            if (other.pitch != null)
                return false;
        } else if (!pitch.equals(other.pitch))
            return false;
        if (percentTargetBoatSpeed == null) {
            if (other.percentTargetBoatSpeed != null)
                return false;
        } else if (!percentTargetBoatSpeed.equals(other.percentTargetBoatSpeed))
            return false;
        if (race == null) {
            if (other.race != null)
                return false;
        } else if (!race.equals(other.race))
            return false;
        if (reasonForMaxPoints != other.reasonForMaxPoints)
            return false;
        if (speedOverGroundAtPassingStartWaypointInKnots == null) {
            if (other.speedOverGroundAtPassingStartWaypointInKnots != null)
                return false;
        } else if (!speedOverGroundAtPassingStartWaypointInKnots
                .equals(other.speedOverGroundAtPassingStartWaypointInKnots))
            return false;
        if (speedOverGroundAtStartOfRaceInKnots == null) {
            if (other.speedOverGroundAtStartOfRaceInKnots != null)
                return false;
        } else if (!speedOverGroundAtStartOfRaceInKnots.equals(other.speedOverGroundAtStartOfRaceInKnots))
            return false;
        if (speedOverGroundFiveSecondsBeforeStartInKnots == null) {
            if (other.speedOverGroundFiveSecondsBeforeStartInKnots != null)
                return false;
        } else if (!speedOverGroundFiveSecondsBeforeStartInKnots
                .equals(other.speedOverGroundFiveSecondsBeforeStartInKnots))
            return false;
        if (startTack != other.startTack)
            return false;
        if (timeBetweenRaceStartAndCompetitorStartInSeconds == null) {
            if (other.timeBetweenRaceStartAndCompetitorStartInSeconds != null)
                return false;
        } else if (!timeBetweenRaceStartAndCompetitorStartInSeconds
                .equals(other.timeBetweenRaceStartAndCompetitorStartInSeconds))
            return false;
        if (timeSinceLastPositionFixInSeconds == null) {
            if (other.timeSinceLastPositionFixInSeconds != null)
                return false;
        } else if (!timeSinceLastPositionFixInSeconds.equals(other.timeSinceLastPositionFixInSeconds))
            return false;
        if (totalPoints == null) {
            if (other.totalPoints != null)
                return false;
        } else if (!totalPoints.equals(other.totalPoints))
            return false;
        if (incrementalScoreCorrectionInPoints == null) {
            if (other.incrementalScoreCorrectionInPoints != null)
                return false;
        } else if (!incrementalScoreCorrectionInPoints.equals(other.incrementalScoreCorrectionInPoints))
            return false;
        if (totalPointsCorrected != other.totalPointsCorrected)
            return false;
        if (totalPointsUncorrected == null) {
            if (other.totalPointsUncorrected != null)
                return false;
        } else if (!totalPointsUncorrected.equals(other.totalPointsUncorrected))
            return false;
        if (windwardDistanceToCompetitorFarthestAheadInMeters == null) {
            if (other.windwardDistanceToCompetitorFarthestAheadInMeters != null)
                return false;
        } else if (!windwardDistanceToCompetitorFarthestAheadInMeters
                .equals(other.windwardDistanceToCompetitorFarthestAheadInMeters))
            return false;
        return true;
    }
    
    /**
     * Used to ensure that only one null value needs to be transmitted, if no expedition data exists 
     */
    public static class ExpeditionDataHolder implements Serializable {
        private static final long serialVersionUID = 1L;
        
        public Double expeditionMastButt;
        public Double expeditionJibCarStbd;
        public Double expeditionJibCarPort;
        public Double expeditionLoadS;
        public Double expeditionLoadP;
        public Double expeditionBARO;
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
        public Double expeditionTimeToGunInSeconds;
        public Double expeditionTimeToCommitteeBoat;
        public Double expeditionTimeToBurnToPin;
        public Double expeditionTargTWA;
        public Double expeditionTimeToBurnToCommitteeBoat;
        public Double expeditionTimeToBurnToLineInSeconds;
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
            result = prime * result + ((expeditionBARO == null) ? 0 : expeditionBARO.hashCode());
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
                    + ((expeditionTimeToBurnToLineInSeconds == null) ? 0 : expeditionTimeToBurnToLineInSeconds.hashCode());
            result = prime * result + ((expeditionTimeToBurnToPin == null) ? 0 : expeditionTimeToBurnToPin.hashCode());
            result = prime * result
                    + ((expeditionTimeToCommitteeBoat == null) ? 0 : expeditionTimeToCommitteeBoat.hashCode());
            result = prime * result + ((expeditionTimeToGunInSeconds == null) ? 0 : expeditionTimeToGunInSeconds.hashCode());
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
            ExpeditionDataHolder other = (ExpeditionDataHolder) obj;
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
            if (expeditionBARO == null) {
                if (other.expeditionBARO != null)
                    return false;
            } else if (!expeditionBARO.equals(other.expeditionBARO))
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
            if (expeditionTimeToBurnToLineInSeconds == null) {
                if (other.expeditionTimeToBurnToLineInSeconds != null)
                    return false;
            } else if (!expeditionTimeToBurnToLineInSeconds.equals(other.expeditionTimeToBurnToLineInSeconds))
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
            if (expeditionTimeToGunInSeconds == null) {
                if (other.expeditionTimeToGunInSeconds != null)
                    return false;
            } else if (!expeditionTimeToGunInSeconds.equals(other.expeditionTimeToGunInSeconds))
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
