[TOC]

# Rank computation
The following diagram describes how the ranks are calculated. Note the notes and the constraints they contain.
![Sequenz diagram of the rank calculation](SequenzDiagram%20rank%20calculation.png)

Or as text:

- AbstractLeaderboardWithCache.computeDTO
    - for every RaceColumn:
        - AbstractSimpleLeaderboardImpl.getCompetitorsFromBestToWorst(RaceColumn)
            - for each Competitor:
                - AbstractSimpleLeaderboardImpl.getTotalPoints(competitor, raceColumn)
                    - ScoreCorrection.get[Un]CorrectedScore(...)
                        - AbstractLeaderboardImpl.getTrackedRank(competitor, raceColumn)
                            - TrackedRaceImpl.getCompetitorsFromBestToWorstAndRankAndRankComparable() (CACHING 10 results by time point)
                                - RankingMetric.getRaceRankingComparator()
                                - sort competitors with race ranking comparator (O(n*log(n)) with partly expensive computations per competitor such as windward distance to go)
                                - build result list of Competitor/RankAndRankComparable
                            - AbstractLeaderboardImpl.getRankImprovedByDisqualificationsOfBetterRankedCompetitors(...) (O(n), finding competitor in list from best to worst)
    - for every leg (if leg details are active):
        - TrackedLegImpl.getRanks(...)
            - TrackedLegImpl.getCompetitorTracksOrderedByRank
                - RankingMetric.getLegRankingComparator(trackedLeg)
                - sort competitors in leg based on leg RankingMetric's ranking comparator (O(n*log(n))
                TODO: this leg rank part is missing a merging across fleets if requested; again, a RankComparable would be required.
    - AbstractSimpleLeaderboardImpl.getCompetitorsFromBestToWorst()
        - AbstractSimpleLeaderboardImpl.getLeaderboardTotalRankComparator()
            - for all Competitors / RaceColumns:
                - AbstractSimpleLeaderboardImpl.getTotalPoints(competitor, raceColumn)
                    - ScoreCorrection.get[Un]CorrectedScore(...)
                        - AbstractLeaderboardImpl.getTrackedRank(competitor, raceColumn)
                            - TrackedRaceImpl.getCompetitorsFromBestToWorstAndRankAndRankComparable() (CACHING 10 results by time point)
                                - RankingMetric.getRaceRankingComparator()
                                - sort competitors with race ranking comparator (O(n*log(n)) with partly expensive computations per competitor such as windward distance to go)
                                - build result list of Competitor/RankAndRankComparable
                            - AbstractLeaderboardImpl.getRankImprovedByDisqualificationsOfBetterRankedCompetitors(...) (O(n), finding competitor in list from best to worst)
                - AbstractSimpleLeaderboardImpl.getNetPoints(competitor, raceColumn)
                    - AbstractSimpleLeaderboardImpl.getTotalPoints(competitor, raceColumn)
                        - ScoreCorrection.get[Un]CorrectedScore(...)
                            - AbstractLeaderboardImpl.getTrackedRank(competitor, raceColumn)
                                - TrackedRaceImpl.getCompetitorsFromBestToWorstAndRankAndRankComparable() (CACHING 10 results by time point)
                                    - RankingMetric.getRaceRankingComparator()
                                    - sort competitors with race ranking comparator (O(n*log(n)) with partly expensive computations per competitor such as windward distance to go)
                                    - build result list of Competitor/RankAndRankComparable
                                - AbstractLeaderboardImpl.getRankImprovedByDisqualificationsOfBetterRankedCompetitors(...) (O(n), finding competitor in list from best to worst)
            - sort by total rank comparator
    - for each competitor from the getCompetitorsFromBestToWorst() result:
        - if race details requested then RankingMetric.getRankingInfo()
        - AbstractSimpleLeaderboardImpl.getEntry(competitor, raceColumn)
            - ScoreCorrection.getCorrectedScore(...)
                - AbstractLeaderboardImpl.getTrackedRank(competitor, raceColumn)
                    - TrackedRaceImpl.getCompetitorsFromBestToWorstAndRankAndRankComparable() (CACHING 10 results by time point)
                        - RankingMetric.getRaceRankingComparator()
                        - sort competitors with race ranking comparator (O(n*log(n)) with partly expensive computations per competitor such as windward distance to go)
                        - build result list of Competitor/RankAndRankComparable
                    - AbstractLeaderboardImpl.getRankImprovedByDisqualificationsOfBetterRankedCompetitors(...) (O(n), finding competitor in list from best to worst)

It is obvious that the `getTotalPoints(competitor, raceColumn)` sequence is invoked four times in the worst case if there are no score corrections:

1. for the `getCompetitorsFromBestToWorst(RaceColumn)` part to fill a corresponding DTO structure for the client
2. for the `LeaderboardTotalRankComparator` that requires all points, e.g., for tie-breaking
3. for the net points calculation triggered by `LeaderboardTotalRankComparator`, required for tie-breaking rules based on non-discarded points
4. when building the `Entry` objects in `AbstractSimpleLeaderboardImpl` which again contain the total points

The problem probably doesn't create too much overhead currently because of the caching that happens inside TrackedRaceImpl.getCompetitorsFromBestToWorstAndRankAndRankComparable(). Yet, it would obviously be better to compute the total points only once for all Competitors/RaceColumns and share the results along the path.

## Comment on the Diagram
In the ScoreCorrection the Ranks of the races are transferred to scores by calling the scoring scheme. Within the scoring schemes the contigousScoring is handled if it is enabled. This place was chosen, because the scores need to be adapted in different ways for the different scoring schemes (HighPoint, LowPoint). The case of disabled contigousScoring is handled in the leaderboard because it can be done based on the fleets alone.

If crossFleetMergedRanking is enabled this case is handeled in the getTrackedRank method of the AbstractLeaderboardImpl class.

# Fleet Handling

It is possible to create series within a regatta, which in turn can consist of several fleets. The scoring of
the two settings that influence the ranking with multiple fleets are described below.
Relevant for this scoring is the rank of a fleet, which must be specified when creating the fleet.

## contigousScoring

ContigousScoring affects the behavior of ranking Competitors that are in Fleets of different ranks.

### Enabled

When contigousScoring is enabled, a competitor's rank is adjusted as if all competitors from better fleets were ahead of him. Example for LowPoint: effectiveRank = rankInFleet + numberOfCompetitorsInBetterFleets. A fleet with a low rank is always better than a fleet with a high rank. Except for fleets with rank 0, which are excluded from the ContigousScoring setting and are not affected themselves nor do they affect other fleets.

Example for contigousScoring = true and crossFleetMergeRanking = false. Each fleet consists of 10 competitors. Vertical dots indicate that the competitors of the upper fleets come next.

| Rank of Fleet | Fleet      | Competitor | R1         | R2         | Total      |
| ------------- | ---------- | ---------- | ---------- | ---------- | ---------- |
| 0             | Blue       | B1         | 1          | 1          | 2          |
| 0             | Yellow     | Y1         | 1          | 1          | 2          |
| 1             | Orange     | O1         | 1          | 1          | 2          |
| 1             | Orange     | O2         | 2          | 2          | 4          |
| 0             | Blue       | B2         | 2          | 3          | 5          |
| 1             | Green      | G1         | 4          | 2          | 6          |
| 1             | Green      | G2         | 3          | 3          | 6          |
| 0             | Blue       | B3         | 3          | 4          | 7          |
| $$\vdots$$    | $$\vdots$$ | $$\vdots$$ | $$\vdots$$ | $$\vdots$$ | $$\vdots$$ |
| 2             | Pink       | P1         | 21         | 23         | 44         |
| 2             | Red        | R1         | 23         | 22         | 45         |
| 2             | Red        | R2         | 22         | 25         | 47         |
| 2             | Pink       | P2         | 22         | 25         | 47         |

Note: Pink and Red get +20 points each, because the Competitors from Green and Orange are in fleets with lower rank. Thus numberOfCompetitorsInBetterFleets = 20. The 0 fleets (Blue, Yellow) are ignored.

### Disabled

If the option ContigousScoring is not activated, the fleets are ranked according to the Fleet, where 0 is the best Fleet and then sorted in ascending order. The points are not adjusted, so there are several times the same number of points.

Beispiel ContigousScoring= False crossFleetMergedRanking=False

| Rank of Fleet | Fleet      | Competitor | R1         | R2         | Total      |
| ------------- | ---------- | ---------- | ---------- | ---------- | ---------- |
| 0             | Blue       | B1         | 1          | 1          | 2          |
| 0             | Yellow     | Y1         | 1          | 1          | 2          |
| 0             | Blue       | B2         | 2          | 3          | 5          |
| 0             | Blue       | B3         | 3          | 4          | 7          |
| $$\vdots$$    | $$\vdots$$ | $$\vdots$$ | $$\vdots$$ | $$\vdots$$ | $$\vdots$$ |
| 1             | Orange     | O1         | 1          | 1          | 2          |
| 1             | Orange     | O2         | 2          | 2          | 4          |
| 1             | Green      | G1         | 4          | 2          | 6          |
| 1             | Green      | G2         | 3          | 3          | 6          |
| $$\vdots$$    | $$\vdots$$ | $$\vdots$$ | $$\vdots$$ | $$\vdots$$ | $$\vdots$$ |
| 2             | Pink       | P1         | 1          | 3          | 4          |
| 2             | Red        | R1         | 3          | 2          | 5          |
| 2             | Red        | R2         | 2          | 5          | 7          |
| 2             | Pink       | P2         | 2          | 5          | 7          |

## crossFleetMergedRanking

If there are multiple fleets with the same rank, crossFleetMergedRanking decides how to rank within that group.

### Enabled

If the option is enabled, the ranks for participants in fleets with the same rank are calculated using RankComparables. The RankComparables contain information about a competitor's performance in a race and implement the compareTo function. This approach results in each rank being assigned only once within a group of fleets.

Example ContigousScoring= False crossFleetMergedRanking=True

| Rank of Fleet | Fleet      | Competitor | R1         | R2         | Total      |
| ------------- | ---------- | ---------- | ---------- | ---------- | ---------- |
| 0             | Blue       | B1         | 1          | 1          | 2          |
| 0             | Blue       | B2         | 2          | 3          | 5          |
| 0             | Yellow     | Y1         | 4          | 2          | 6          |
| 0             | Blue       | B3         | 3          | 4          | 7          |
| $$\vdots$$    | $$\vdots$$ | $$\vdots$$ | $$\vdots$$ | $$\vdots$$ | $$\vdots$$ |
| 1             | Orange     | O1         | 1          | 1          | 2          |
| 1             | Orange     | O2         | 2          | 2          | 4          |
| 1             | Green      | G1         | 4          | 3          | 7          |
| 1             | Green      | G2         | 3          | 5          | 8          |
| $$\vdots$$    | $$\vdots$$ | $$\vdots$$ | $$\vdots$$ | $$\vdots$$ | $$\vdots$$ |
| 2             | Pink       | P1         | 1          | 3          | 4          |
| 2             | Red        | R1         | 3          | 2          | 5          |
| 2             | Red        | R2         | 2          | 4          | 6          |
| 2             | Pink       | P2         | 4          | 5          | 9          |


Example ContigousScoring= True crossFleetMergedRanking=True

| Rank of Fleet | Fleet | Competitor | R1 | R2 | Total |
| ------------- | ---------- | ---------- | ---------- | ---------- | ---------- |
| 0 | Blue | B1 | 1 | 1 | 2 |
| 1 | Orange | O1 | 1 | 1 | 2 |
| 1 | Orange | O2 | 2 | 2 | 4 |
| 0 | Blue | B2 | 2 | 3 | 5 |
| 0 | Yellow | G1 | 4 | 2 | 6 |
| 1 | Green | G1 | 4 | 3 | 7 |
| 0 | Blue | B3 | 3 | 4 | 7 |
| 1 | Green | G1 | 3 | 5 | 8 |
| $$\vdots$$ | $$\vdots$$ | $$\vdots$$ | $$\vdots$$ | $$\vdots$$ | $$\vdots$$ |
| 2 | Pink | P1 | 21 | 23 | 44 |
| 2 | Red | R1 | 23 | 22 | 45 |
| 2 | Red | R2 | 22 | 25 | 46 |
| 2 | Pink | P2 | 24 | 25 | 49 |

### Disabled

If crossFleetMergedRanking is deactivated, the ranks in a fleet are assigned twice. It is then sorted by the order of the sum of the points, using the points of the single race. For Examples look at contigousScoring

## Explanation of RankComparables

In order to make the comparison across fleets possible that are not based on the rank within a race RankComparables are used. In order to work the following assumptions must be met:

- The competitors are sailing on similar courses / course Layouts
- The conditions (wind, waves, tide, drift) are the same (or do not differ so much that a comparison is invalid).
- All fleets sail the same number of races and have equal discarding rules

Therefore the order of the RankComparables for one Fleet is guaranteed to result in the same ordering as the Ordering that is returned by getCompetitorsFromBestToWorst on a race basis. This also holds when looking at a Leaderboard with multiple Fleets of the same rank and crossFleetMergedRanking set to true. (the ordering within one fleet on the leaderboard is the same as if just a leaderboard for one fleet would have been created)

The following describes how the comparisons are made for the different ranking metrics:

### One-Design

Three alternatives:

1. Pull back fleet
   1. Calculate a VMG for each fleet
      1. Min
      2. Max
      3. Avg
      4. Median
   2. Get the minimum of time sailed and time between the start of the fleets. Called timeOffset from now on.
   3. Compute a fleet-specific distance by $$ OffsetDistance = fleetVMG \cdot timeOffset $$
   4. Pull back every competitor for the calculated distance. Also around the marks.
   5. Compare all competitors by their newly calculated Position
2. Pull ahead fleet
   1. Calculate a VMG for each fleet
      1. Min
      2. Max
      3. Avg
      4. Median
   2. Get the minimum of timesailed and time between the start of the fleets. Called timeOffset from now on.
   3. Compute a fleet specific distance by $$ OffsetDistance = fleetVMG \cdot timeOffset $$
   4. Pull ahead every competitor for the calculated distance. Also around the marks.
   5. Compare all competitors by their newly calculated Position
3. Time on Time and Distance like
   1. For every competitor in a fleet compute the time needed to reach the leader in the fleet. When passing a mark anticipate same performance, by just adding the time the faster competitor spent on the leg to the calculated time.
   2. Use this calculated Time as calcTime@fastest and apply the model of TimeOnTimeAndDistance.

### Time-on-Time/Time-on-Distance

For the scoring of Time on Time and Time on Distance races the following steps are performed:

1. For each fleet compute the leaderboard for the current race.
2. From the calculated leaderboards extract the calcTime@Fastest (Fastest references the competitor furthest ahead in the current race of the fleet) for each competitor in the fleet.
3. For every competitor (currentComp) compute a stretch factor as follows:
   $$ stretchFactor = \frac{calcTime@FastestOfCurrentComp}{timeSailedByFastestCompetitor} $$
4. For each fleet determine the fastest Competitor (the fastest competitor is the competitor where following statement holds: sailedTime = calcTime@fastest)
5. Order the fastest competitors by their windward Distance sailed. If multiple competitors sailed the same windward distance order them by their time sailed. In the following the fastest competitor of all competitors is called absoluteFastestCompetitor.
6. For each fastest competitor compute the time that is needed to reach the position of absoluteFastestCompetitor. This time is named timeToAdd.
7. Compute calcTime@absolouteFurthest ahead for each competitor as follows:
   $$ calcTime@absolouteFurthest = calcTime@furthestAhead + (timeToAdd \cdot stretchFactor)$$
8. Compute the leaderboard by ordering the competitors by their calcTime@absolouteFurthest

#### Justification for the stretch factor

The stretch factor is needed to map the current performance of a competitor to the distance between the fastest comp in fleet and absoluteFastestCompetitor.
A simpler justification for the stretching factor is possible if it is omitted first:
$$ calcTime@absolouteFurthest = calcTime@furthestAhead + timeToAdd $$
In this calculation it is assumed that from the fastest competitors position onwards every competitor is sailing with the performance of the fastest competitor in a fleet. If we proceed with this assumption the performance of the fastest competitor could heavely influence the assuemed performance of the hole fleet. Espespially if the gap between the fastest competitor and the rest of the fleet is rather big. This would lead to a non representetive order of precedence on the Leaderboard across fleets.
To overcome this issue the stretchFactor is introduced wich is supposed to reflect the performance of a competitor in relation to the fastest competitor within a fleet. Therfor the ratio between the calcTime@fastest and timeSailedByFastestComp is computed. By this procedure the ToT and ToD Factor of the competitors are considerd.

#### Further ideas:

- instead of computing the time to the absoluteFastestCompetitor based on the fastestCompetitor in a race compute it based on the median competitor or the slowest competitor (or offer these types as options). In order to offer these opton the following steps would need to change:
  - Step 3. Instead of using the timeSailedByFastestCompetitor use the clacTime@fastest of the median or slowest competitor.
  - Step 6. From the position of the fastest competitor in the race compute the timeToAdd based on the slowest or median competitor.

### ORC Polar Curve Scoring (PCS) >= 2015 (formerly "Performance Curve Scoring")

For PCS >= 2015, the delta between allowance and sailed time is formed for every competitor. Subsequently, the percentage of the course that the competitor has already sailed is recorded. Finally, it is determined how large the delta would be if the competitor continued sailing with his current performance until he reaches the finish. These computed deltas are then used for comparison in the leaderboard.

### ORC Polar Curve Scoring (PCS) < 2015 (formerly "Performance Curve Scoring")

Just order the competitors across all fleets by their implied wind. In order to have calculated times reflect the merged order then a common scratch boat would have to be chosen for converting the implied wind values to times. But this would lead to different calculated times in the individual races compared to the merged view, and this makes things more complicated than necessary. Also, a common scratch boat can still be chosen for those races manually, providing a clean solution to this problem. With the implied wind as the sorting criteria it is also easy for users to select this column and validate and comprehend the ordering.
