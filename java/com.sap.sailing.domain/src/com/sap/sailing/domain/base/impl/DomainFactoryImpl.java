package com.sap.sailing.domain.base.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorAndBoatStore;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Placemark;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.dto.BoatDTO;
import com.sap.sailing.domain.common.dto.CompetitorAndBoatDTO;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.CompetitorWithBoatDTO;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.PlacemarkDTO;
import com.sap.sailing.domain.common.dto.PlacemarkOrderDTO;
import com.sap.sailing.domain.common.dto.RaceDTO;
import com.sap.sailing.domain.common.dto.RaceStatusDTO;
import com.sap.sailing.domain.common.dto.TrackedRaceDTO;
import com.sap.sailing.domain.common.dto.TrackedRaceStatisticsDTO;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.media.MediaTrack;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.leaderboard.impl.HighPoint;
import com.sap.sailing.domain.leaderboard.impl.HighPointByWinsTiesLastlyBrokenByOtherLeaderboard;
import com.sap.sailing.domain.leaderboard.impl.HighPointExtremeSailingSeriesOverall;
import com.sap.sailing.domain.leaderboard.impl.HighPointExtremeSailingSeriesOverall12PointsMax;
import com.sap.sailing.domain.leaderboard.impl.HighPointFirstGets10LastBreaksTie;
import com.sap.sailing.domain.leaderboard.impl.HighPointFirstGets10Or8AndLastBreaksTie;
import com.sap.sailing.domain.leaderboard.impl.HighPointFirstGets12Or8AndLastBreaksTie;
import com.sap.sailing.domain.leaderboard.impl.HighPointFirstGets12Or8AndLastBreaksTie2017;
import com.sap.sailing.domain.leaderboard.impl.HighPointFirstGets1LastBreaksTie;
import com.sap.sailing.domain.leaderboard.impl.HighPointLastBreaksTie;
import com.sap.sailing.domain.leaderboard.impl.HighPointMatchRacing;
import com.sap.sailing.domain.leaderboard.impl.HighPointWinnerGetsEight;
import com.sap.sailing.domain.leaderboard.impl.HighPointWinnerGetsEightAndInterpolation;
import com.sap.sailing.domain.leaderboard.impl.HighPointWinnerGetsFive;
import com.sap.sailing.domain.leaderboard.impl.HighPointWinnerGetsSix;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.leaderboard.impl.LowPointA82Only;
import com.sap.sailing.domain.leaderboard.impl.LowPointFirstToWinThreeRaces;
import com.sap.sailing.domain.leaderboard.impl.LowPointFirstToWinThreeRacesA82Only;
import com.sap.sailing.domain.leaderboard.impl.LowPointFirstToWinTwoRaces;
import com.sap.sailing.domain.leaderboard.impl.LowPointForLeagueOverallLeaderboard;
import com.sap.sailing.domain.leaderboard.impl.LowPointForOverallUsingNetPoints;
import com.sap.sailing.domain.leaderboard.impl.LowPointTieBreakBasedOnLastSeriesOnly;
import com.sap.sailing.domain.leaderboard.impl.LowPointWinnerGetsZero;
import com.sap.sailing.domain.leaderboard.impl.LowPointWithAutomaticRDG;
import com.sap.sailing.domain.leaderboard.impl.LowPointWithEliminatingMedalSeriesPromotingOneToFinalAndTwoToSemifinal;
import com.sap.sailing.domain.leaderboard.impl.LowPointWithEliminatingMedalSeriesPromotingTwoToFinalAndTwoToSemifinal;
import com.sap.sailing.domain.leaderboard.impl.LowPointWithEliminationsAndRoundsWinnerGets07;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sailing.domain.tracking.impl.CourseDesignUpdateHandler;
import com.sap.sailing.domain.tracking.impl.FinishTimeUpdateHandler;
import com.sap.sailing.domain.tracking.impl.MarkPassingImpl;
import com.sap.sailing.domain.tracking.impl.RaceAbortedHandler;
import com.sap.sailing.domain.tracking.impl.StartTimeUpdateHandler;
import com.sap.sailing.domain.tracking.impl.TrackedRaceImpl;
import com.sap.sailing.geocoding.ReverseGeocoder;
import com.sap.sse.common.Distance;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.util.ObjectInputStreamResolvingAgainstCache;
import com.sap.sse.util.ObjectInputStreamResolvingAgainstCache.ResolveListener;

public class DomainFactoryImpl extends SharedDomainFactoryImpl<RaceLogAndTrackedRaceResolver> implements DomainFactory {
    private static Logger logger = Logger.getLogger(DomainFactoryImpl.class.getName());
    
    /**
     * Uses a transient competitor and boat store
     */
    public DomainFactoryImpl(RaceLogAndTrackedRaceResolver raceLogResolver) {
        super(new TransientCompetitorAndBoatStoreImpl(), raceLogResolver);
    }
    
    public DomainFactoryImpl(CompetitorAndBoatStore competitorStore, RaceLogAndTrackedRaceResolver raceLogResolver) {
        super(competitorStore, raceLogResolver);
    }

    @Override
    public MarkPassing createMarkPassing(TimePoint timePoint, Waypoint waypoint, Competitor competitor) {
        return new MarkPassingImpl(timePoint, waypoint, competitor);
    }

    @Override
    public ObjectInputStreamResolvingAgainstCache<DomainFactory> createObjectInputStreamResolvingAgainstThisFactory(
            InputStream inputStream, ResolveListener resolveListener, Map<String, Class<?>> classLoaderCache) throws IOException {
        return new ObjectInputStreamResolvingAgainstDomainFactoryImpl(inputStream, this, resolveListener, classLoaderCache);
    }

    @Override
    public ScoringScheme createScoringScheme(ScoringSchemeType scoringSchemeType) {
        switch (scoringSchemeType) {
        case LOW_POINT:
            return new LowPoint();
        case HIGH_POINT:
            return new HighPoint();
        case HIGH_POINT_ESS_OVERALL:
            return new HighPointExtremeSailingSeriesOverall();
        case HIGH_POINT_ESS_OVERALL_12:
            return new HighPointExtremeSailingSeriesOverall12PointsMax();
        case HIGH_POINT_LAST_BREAKS_TIE:
            return new HighPointLastBreaksTie();
        case HIGH_POINT_FIRST_GETS_ONE:
            return new HighPointFirstGets1LastBreaksTie();
        case HIGH_POINT_FIRST_GETS_TEN:
            return new HighPointFirstGets10LastBreaksTie();
        case LOW_POINT_WINNER_GETS_ZERO:
            return new LowPointWinnerGetsZero();
        case HIGH_POINT_WINNER_GETS_FIVE:
            return new HighPointWinnerGetsFive();
        case HIGH_POINT_WINNER_GETS_SIX:
            return new HighPointWinnerGetsSix();
        case HIGH_POINT_WINNER_GETS_EIGHT:
            return new HighPointWinnerGetsEight();
        case HIGH_POINT_MATCH_RACING:
            return new HighPointMatchRacing();
        case HIGH_POINT_WINNER_GETS_EIGHT_AND_INTERPOLATION:
            return new HighPointWinnerGetsEightAndInterpolation();
        case HIGH_POINT_FIRST_GETS_TEN_OR_EIGHT:
            return new HighPointFirstGets10Or8AndLastBreaksTie();
        case HIGH_POINT_FIRST_GETS_TWELVE_OR_EIGHT:
            return new HighPointFirstGets12Or8AndLastBreaksTie();
        case HIGH_POINT_FIRST_GETS_TWELVE_OR_EIGHT_2017:
            return new HighPointFirstGets12Or8AndLastBreaksTie2017();
        case LOW_POINT_WITH_ELIMINATIONS_AND_ROUNDS_WINNER_GETS_07:
            return new LowPointWithEliminationsAndRoundsWinnerGets07();
        case LOW_POINT_LEAGUE_OVERALL:
            return new LowPointForLeagueOverallLeaderboard();
        case LOW_POINT_TIE_BREAK_BASED_ON_LAST_SERIES_ONLY:
            return new LowPointTieBreakBasedOnLastSeriesOnly();
        case LOW_POINT_WITH_AUTOMATIC_RDG:
            return new LowPointWithAutomaticRDG();
        case LOW_POINT_FIRST_TO_WIN_TWO_RACES:
            return new LowPointFirstToWinTwoRaces();
        case LOW_POINT_FIRST_TO_WIN_THREE_RACES:
            return new LowPointFirstToWinThreeRaces();
        case HIGH_POINT_BY_WINS_TIES_LASTLY_BROKEN_BY_OTHER_LEADERBOARD:
            return new HighPointByWinsTiesLastlyBrokenByOtherLeaderboard();
        case LOW_POINT_A82_ONLY:
            return new LowPointA82Only();
        case LOW_POINT_FIRST_TO_WIN_THREE_RACES_A82_ONLY:
            return new LowPointFirstToWinThreeRacesA82Only();
        case LOW_POINT_WITH_ELIMINATING_MEDAL_SERIES_PROMOTING_ONE_TO_FINAL_AND_TWO_TO_SEMIFINAL:
            return new LowPointWithEliminatingMedalSeriesPromotingOneToFinalAndTwoToSemifinal();
        case LOW_POINT_OVERALL_USING_NET_POINTS:
            return new LowPointForOverallUsingNetPoints();
        case LOW_POINT_WITH_ELIMINATING_MEDAL_SERIES_PROMOTING_TWO_TO_FINAL_AND_TWO_TO_SEMIFINAL:
            return new LowPointWithEliminatingMedalSeriesPromotingTwoToFinalAndTwoToSemifinal();
        }
        throw new RuntimeException("Unknown scoring scheme type "+scoringSchemeType.name());
    }

    @Override
    public CompetitorAndBoatDTO convertToCompetitorAndBoatDTO(Competitor competitor, Boat boat) {
        return new CompetitorAndBoatDTO(convertToCompetitorDTO(competitor), competitorAndBoatStore.convertToBoatDTO(boat));
    }

    @Override
    public CompetitorDTO convertToCompetitorDTO(Competitor competitor) {
        return competitorAndBoatStore.convertToCompetitorWithOptionalBoatDTO(competitor);
    }

    @Override
    public CompetitorWithBoatDTO convertToCompetitorWithBoatDTO(CompetitorWithBoat competitor) {
        return competitorAndBoatStore.convertToCompetitorWithBoatDTO(competitor);
    }

    @Override
    public Map<CompetitorDTO, BoatDTO> convertToCompetitorAndBoatDTOs(Map<Competitor, ? extends Boat> competitorsAndBoats) {
        return competitorAndBoatStore.convertToCompetitorAndBoatDTOs(competitorsAndBoats);
    }

    @Override
    public BoatDTO convertToBoatDTO(Boat boat) {
        return competitorAndBoatStore.convertToBoatDTO(boat);
    }

    @Override
    public FleetDTO convertToFleetDTO(Fleet fleet) {
        return new FleetDTO(fleet.getName(), fleet.getOrdering(), fleet.getColor());
    }

    @Override
    public RaceDTO createRaceDTO(TrackedRegattaRegistry trackedRegattaRegistry, boolean withGeoLocationData, RegattaAndRaceIdentifier raceIdentifier, TrackedRace trackedRace) {
        assert trackedRace != null;
        // Optional: Getting the places of the race
        PlacemarkOrderDTO racePlaces = withGeoLocationData ? getRacePlaces(trackedRace) : null;
        TrackedRaceDTO trackedRaceDTO = createTrackedRaceDTO(trackedRace); 
        RaceDTO raceDTO = new RaceDTO(raceIdentifier, trackedRaceDTO, trackedRegattaRegistry.isRaceBeingTracked(
                trackedRace.getTrackedRegatta().getRegatta(), trackedRace.getRace()), trackedRace.getRankingMetric()==null?null:trackedRace.getRankingMetric().getType());
        raceDTO.places = racePlaces;
        updateRaceDTOWithTrackedRaceData(trackedRace, raceDTO);
        return raceDTO;
    }

    @Override
    public void updateRaceDTOWithTrackedRaceData(TrackedRace trackedRace, RaceDTO raceDTO) {
        assert trackedRace != null;
        raceDTO.startOfRace = trackedRace.getStartOfRace() == null ? null : trackedRace.getStartOfRace().asDate();
        raceDTO.endOfRace = trackedRace.getEndOfRace() == null ? null : trackedRace.getEndOfRace().asDate();
        raceDTO.raceFinishingTime = trackedRace.getFinishingTime() == null ? null : trackedRace.getFinishingTime().asDate();
        raceDTO.raceFinishedTime = trackedRace.getFinishedTime() == null ? null : trackedRace.getFinishedTime().asDate();
        raceDTO.status = new RaceStatusDTO();
        raceDTO.status.status = trackedRace.getStatus() == null ? null : trackedRace.getStatus().getStatus();
        raceDTO.status.loadingProgress = trackedRace.getStatus() == null ? 0.0 : trackedRace.getStatus().getLoadingProgress();
    }
    
    @Override
    public TrackedRaceDTO createTrackedRaceDTO(TrackedRace trackedRace) {
        TrackedRaceDTO trackedRaceDTO = new TrackedRaceDTO();
        trackedRaceDTO.startOfTracking = trackedRace.getStartOfTracking() == null ? null : trackedRace.getStartOfTracking().asDate();
        trackedRaceDTO.endOfTracking = trackedRace.getEndOfTracking() == null ? null : trackedRace.getEndOfTracking().asDate();
        trackedRaceDTO.timePointOfNewestEvent = trackedRace.getTimePointOfNewestEvent() == null ? null : trackedRace.getTimePointOfNewestEvent().asDate();
        trackedRaceDTO.hasWindData = trackedRace.hasWindData();
        trackedRaceDTO.hasGPSData = trackedRace.hasGPSData();
        trackedRaceDTO.delayToLiveInMs = trackedRace.getDelayToLiveInMillis();
        return trackedRaceDTO;
    }

    @Override
    public TrackedRaceStatisticsDTO createTrackedRaceStatisticsDTO(TrackedRace trackedRace, Leaderboard leaderboard,
            RaceColumn raceColumn, Fleet fleet, Iterable<MediaTrack> mediaTracks) {
        TrackedRaceStatisticsDTO statisticsDTO = new TrackedRaceStatisticsDTO();
        // GPS data
        statisticsDTO.hasGPSData = trackedRace.hasGPSData();
        Competitor leaderOrWinner = null;
        TimePoint now = MillisecondsTimePoint.now();
        try {
            if (trackedRace.isLive(now)) {
                leaderOrWinner = trackedRace.getOverallLeader(now);
            } else if (trackedRace.getEndOfRace() != null) {
                for (Competitor competitor : leaderboard.getCompetitorsFromBestToWorst(raceColumn, now)) {
                    Fleet fleetOfCompetitor = raceColumn.getFleetOfCompetitor(competitor);
                    if (fleetOfCompetitor != null && fleetOfCompetitor.equals(fleet)) {
                        leaderOrWinner = competitor;
                        break;
                    }
                }
            }
            if (leaderOrWinner != null) {
                final Boat leaderOrWinnerBoat = trackedRace.getBoatOfCompetitor(leaderOrWinner);
                statisticsDTO.hasLeaderOrWinnerData = true;
                statisticsDTO.leaderOrWinner = convertToCompetitorAndBoatDTO(leaderOrWinner, leaderOrWinnerBoat);
                GPSFixTrack<Competitor, GPSFixMoving> track = trackedRace.getTrack(leaderOrWinner);
                if (track != null) {
                    statisticsDTO.averageGPSDataSampleInterval = track.getAverageIntervalBetweenRawFixes();
                }
            }
        } catch (NoWindException e) {
        }

        // Measured wind sources data
        statisticsDTO.measuredWindSourcesCount = Util.size(trackedRace.getWindSources(WindSourceType.EXPEDITION));
        statisticsDTO.hasMeasuredWindData = statisticsDTO.measuredWindSourcesCount > 0;

        // leg progress data
        RaceDefinition race = trackedRace.getRace();
        if (race.getCourse() != null) {
            statisticsDTO.hasLegProgressData = true;
            statisticsDTO.totalLegsCount = race.getCourse().getLegs().size();
            statisticsDTO.currentLegNo = trackedRace.getLastLegStarted(MillisecondsTimePoint.now());
        }

        // media data
        if (mediaTracks != null) {
            for (MediaTrack track : mediaTracks) {
                if (track.mimeType != null) {
                    switch (track.mimeType.mediaType) {
                    case audio:
                        statisticsDTO.hasAudioData = true;
                        statisticsDTO.audioTracksCount = statisticsDTO.audioTracksCount == null ? 1
                                : statisticsDTO.audioTracksCount++;
                        break;
                    case video:
                        statisticsDTO.hasVideoData = true;
                        statisticsDTO.videoTracksCount = statisticsDTO.videoTracksCount == null ? 1
                                : statisticsDTO.videoTracksCount++;
                        break;
                    case image: // TODO should this add to an image count?
                        break;
                    case unknown: // TODO should this add to an "unknown media" count? Probably not
                        break;
                    default:
                        break;
                    }
                }
            }
        }

        return statisticsDTO;
    }

    private PlacemarkOrderDTO getRacePlaces(TrackedRace trackedRace) {
        Util.Pair<Placemark, Placemark> startAndFinish = getStartFinishPlacemarksForTrackedRace(trackedRace);
        PlacemarkOrderDTO racePlaces = new PlacemarkOrderDTO();
        if (startAndFinish.getA() != null) {
            racePlaces.getPlacemarks().add(convertToPlacemarkDTO(startAndFinish.getA()));
        }
        if (startAndFinish.getB() != null) {
            racePlaces.getPlacemarks().add(convertToPlacemarkDTO(startAndFinish.getB()));
        }
        if (racePlaces.isEmpty()) {
            racePlaces = null;
        }
        return racePlaces;
    }

    private Util.Pair<Placemark, Placemark> getStartFinishPlacemarksForTrackedRace(TrackedRace race) {
        double radiusCalculationFactor = 10.0;
        Placemark startBest = null;
        Placemark finishBest = null;
        Position startPosition = null;
        // Get start position
        final Waypoint firstWaypoint = race.getRace().getCourse().getFirstWaypoint();
        if (firstWaypoint != null) {
            Iterator<Mark> startMarks = firstWaypoint.getMarks().iterator();
            GPSFix startMarkFix = startMarks.hasNext() ? race.getOrCreateTrack(startMarks.next()).getLastRawFix() : null;
            startPosition = startMarkFix != null ? startMarkFix.getPosition() : null;
            if (startPosition != null) {
                try {
                    // Get distance to nearest placemark and calculate the search radius
                    Placemark startNearest = ReverseGeocoder.INSTANCE.getPlacemarkNearest(startPosition);
                    if (startNearest != null) {
                        Distance startNearestDistance = startNearest.distanceFrom(startPosition);
                        double startRadius = startNearestDistance.getKilometers() * radiusCalculationFactor;
    
                        // Get the estimated best start place
                        startBest = ReverseGeocoder.INSTANCE.getPlacemarkLast(startPosition, startRadius,
                                new Placemark.ByPopulationDistanceRatio(startPosition));
                    }
                } catch (IOException e) {
                    logger.throwing(TrackedRaceImpl.class.getName(), "getPlaceOrder()", e);
                } catch (org.json.simple.parser.ParseException e) {
                    logger.throwing(TrackedRaceImpl.class.getName(), "getPlaceOrder()", e);
                }
            }
        }

        // Get finish position
        final Waypoint lastWaypoint = race.getRace().getCourse().getLastWaypoint();
        if (lastWaypoint != null) {
            Iterator<Mark> finishMarks = firstWaypoint.getMarks().iterator();
            GPSFix finishMarkFix = finishMarks.hasNext() ? race.getOrCreateTrack(finishMarks.next()).getLastRawFix() : null;
            Position finishPosition = finishMarkFix != null ? finishMarkFix.getPosition() : null;
            if (startPosition != null && finishPosition != null) {
                if (startPosition.getDistance(finishPosition).getKilometers() <= ReverseGeocoder.POSITION_CACHE_DISTANCE_LIMIT_IN_KM) {
                    finishBest = startBest;
                } else {
                    try {
                        // Get distance to nearest placemark and calculate the search radius
                        Placemark finishNearest = ReverseGeocoder.INSTANCE.getPlacemarkNearest(finishPosition);
                        Distance finishNearestDistance = finishNearest.distanceFrom(finishPosition);
                        double finishRadius = finishNearestDistance.getKilometers() * radiusCalculationFactor;
    
                        // Get the estimated best finish place
                        finishBest = ReverseGeocoder.INSTANCE.getPlacemarkLast(finishPosition, finishRadius,
                                new Placemark.ByPopulationDistanceRatio(finishPosition));
                    } catch (IOException e) {
                        logger.throwing(TrackedRaceImpl.class.getName(), "getPlaceOrder()", e);
                    } catch (org.json.simple.parser.ParseException e) {
                        logger.throwing(TrackedRaceImpl.class.getName(), "getPlaceOrder()", e);
                    }
                }
            }
        }
        Util.Pair<Placemark, Placemark> placemarks = new Util.Pair<Placemark, Placemark>(startBest, finishBest);
        return placemarks;
    }

    @Override
    public PlacemarkDTO convertToPlacemarkDTO(Placemark placemark) {
        Position position = placemark.getPosition();
        return new PlacemarkDTO(placemark.getName(), placemark.getCountryCode(), new DegreePosition(position.getLatDeg(),
                position.getLngDeg()), placemark.getPopulation());
    }

    @Override
    public List<CompetitorDTO> getCompetitorDTOList(Iterable<Competitor> competitors) {
        List<CompetitorDTO> result = new ArrayList<>();
        for (Competitor competitor : competitors) {
            result.add(convertToCompetitorDTO(competitor));
        }
        return result;
    }
    
    @Override
    public void addUpdateHandlers(DynamicTrackedRace trackedRace, CourseDesignUpdateHandler courseDesignHandler,
            StartTimeUpdateHandler startTimeHandler, RaceAbortedHandler raceAbortedHandler,
            final FinishTimeUpdateHandler finishTimeUpdateHandler) {
        trackedRace.addCourseDesignChangedListener(courseDesignHandler);
        trackedRace.addStartTimeChangedListener(startTimeHandler);
        trackedRace.addRaceAbortedListener(raceAbortedHandler);
        trackedRace.addListener(finishTimeUpdateHandler.getListener());
    }
}
