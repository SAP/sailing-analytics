package com.sap.sailing.domain.common.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.domain.base.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.Cloner;
import com.sap.sailing.domain.common.dto.BoatClassDTO;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.CompetitorDTOImpl;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.IncrementalLeaderboardDTO;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.domain.common.dto.LeaderboardEntryDTO;
import com.sap.sailing.domain.common.dto.LeaderboardRowDTO;
import com.sap.sailing.domain.common.dto.LegEntryDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.domain.common.dto.RaceDTO;
import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.domain.test.StoredTrackBasedTest;
import com.sap.sailing.util.ClonerImpl;

/**
 * Tests the compressing / de-compressing functionality of {@link LeaderboardDTO} and {@link IncrementalLeaderboardDTO}.
 * See also bug 1417.<p>
 * 
 * The data of a meaningful and non-trivial {@link LeaderboardDTO} is obtained by using an instrumented version of
 * <code>SailingServiceImpl.getLeaderboardByName(...)</code> which serializes the leaderboard at the end of the method
 * to a file used by this test. The leaderboard that this test wants to use is that of the 505 Worlds 2013, obtained
 * for an expanded Race R9 at time 2013-05-03T17:21:40Z.  
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class LeaderboardDTODiffingTest {
    private LeaderboardDTO previousVersion;
    private IncrementalLeaderboardDTO newVersion;
    private final Cloner cloner = new ClonerImpl();
    
    @Before
    public void setUp() throws FileNotFoundException, IOException, ClassNotFoundException {
        ObjectInputStream ois = StoredTrackBasedTest.getObjectInputStream("IncrementalLeaderboardDTO.ser");
        previousVersion = (LeaderboardDTO) ois.readObject();
        ois.close();
        newVersion = new IncrementalLeaderboardDTO("12345", cloner);
        cloner.clone(previousVersion, newVersion);
    }
    
    private CompetitorDTO getPreviousCompetitorByName(String name) {
        for (CompetitorDTO competitor : previousVersion.competitors) {
            if (competitor.getName().equals(name)) {
                return competitor;
            }
        }
        return null;
    }
    
    @Test
    public void testLeaderboardSuccessfullyRead() {
        assertNotNull(previousVersion);
        assertNotNull(newVersion);
    }

    @Test
    public void testPreviousAndNewVersionHaveEqualRows() {
        assertEquals(previousVersion.rows, newVersion.rows);
    }
    
    @Test
    public void testTotalStripping() {
        newVersion.strip(previousVersion);
        assertAllRowsKeysAreIdenticalToAllLeaderboardRowDTOCompetitors(newVersion);
        assertNull(newVersion.rows);
    }

    @Test
    public void testMajorStripping() {
        newVersion.rows = new HashMap<CompetitorDTO, LeaderboardRowDTO>(newVersion.rows);
        List<RaceColumnDTO> raceListBeforeStripping = new ArrayList<RaceColumnDTO>(newVersion.getRaceList());
        CompetitorDTO wolfgang = getPreviousCompetitorByName("HUNGER +JESS");
        assertNotNull(wolfgang);
        LeaderboardRowDTO wolfgangsRow = new LeaderboardRowDTO();
        cloner.clone(newVersion.rows.get(wolfgang), wolfgangsRow);
        newVersion.rows.put(wolfgang, wolfgangsRow);
        wolfgangsRow.totalDistanceTraveledInMeters += 1;
        Map<CompetitorDTO, LeaderboardRowDTO> rowsBeforeStripping = newVersion.rows;
        newVersion.strip(previousVersion);
        assertAllRowsKeysAreIdenticalToAllLeaderboardRowDTOCompetitors(newVersion);
        assertNotNull(newVersion.rows);
        assertEquals(1, newVersion.rows.size()); // only wolfgang's row should show
        assertEquals(wolfgang, newVersion.rows.keySet().iterator().next().getCompetitorFromPrevious(previousVersion));
        assertTrue(newVersion.rows.values().iterator().next().fieldsByRaceColumnName.isEmpty());
        for (RaceColumnDTO nullRaceColumn : newVersion.getRaceList()) {
            assertNull(nullRaceColumn); // there were no changes to the columns; expect all of them to have been eliminiated
        }
        LeaderboardDTO applied = newVersion.getLeaderboardDTO(previousVersion);
        assertEquals(rowsBeforeStripping, applied.rows);
        assertNotSame(raceListBeforeStripping, applied.getRaceList());
        assertEquals(raceListBeforeStripping, applied.getRaceList());
    }

    @Test
    public void testStrippingExceptOneColumnInOneRace() {
        HashMap<CompetitorDTO, LeaderboardRowDTO> newRows = new HashMap<CompetitorDTO, LeaderboardRowDTO>(newVersion.rows);
        double newDistance = 1234;
        String nameOfRaceColumnToChange = "R9";
        int indexOfLegToChange = 7; // ( zero based: ) 8th leg is the last leg
        for (Map.Entry<CompetitorDTO, LeaderboardRowDTO> e : newVersion.rows.entrySet()) {
            LeaderboardRowDTO newRow = new LeaderboardRowDTO();
            cloner.clone(e.getValue(), newRow);
            newRows.put(e.getKey(), newRow);
            newRow.fieldsByRaceColumnName = new HashMap<String, LeaderboardEntryDTO>(newRow.fieldsByRaceColumnName); // clone entry map
            LeaderboardEntryDTO newEntry = new LeaderboardEntryDTO();
            cloner.clone(newRow.fieldsByRaceColumnName.get(nameOfRaceColumnToChange), newEntry);
            newRow.fieldsByRaceColumnName.put(nameOfRaceColumnToChange, newEntry);
            if (newEntry.legDetails != null) {
                newEntry.legDetails = new ArrayList<LegEntryDTO>(newEntry.legDetails); // clone leg details list
                if (newEntry.legDetails.get(indexOfLegToChange) != null) {
                    LegEntryDTO newLegDetail = new LegEntryDTO();
                    cloner.clone(newEntry.legDetails.get(indexOfLegToChange), newLegDetail);
                    newEntry.legDetails.set(indexOfLegToChange, newLegDetail);
                    newLegDetail.distanceTraveledInMeters = newDistance;
                    newDistance += 1;
                }
            }
        }
        newVersion.rows = newRows;
        Map<CompetitorDTO, LeaderboardRowDTO> rowsBeforeStripping = newVersion.rows;
        Map<Pair<CompetitorDTO, String>, List<LegEntryDTO>> previousLegDetailsBeforeStripping = new HashMap<>();
        for (Map.Entry<CompetitorDTO, LeaderboardRowDTO> e : rowsBeforeStripping.entrySet()) {
            for (Map.Entry<String, LeaderboardEntryDTO> e2 : e.getValue().fieldsByRaceColumnName.entrySet()) {
                if (e2.getValue().legDetails != null) {
                    previousLegDetailsBeforeStripping.put(new Pair<CompetitorDTO, String>(e.getKey(), e2.getKey()),
                            new ArrayList<>(e2.getValue().legDetails));
                }
            }
        }
        newVersion.strip(previousVersion);
        // see bug 1455; check that the stripping doesn't kill the previous leaderboard's legDetails lists
        for (Map.Entry<Pair<CompetitorDTO, String>, List<LegEntryDTO>> e : previousLegDetailsBeforeStripping.entrySet()) {
            final LeaderboardRowDTO leaderboardRowDTO = rowsBeforeStripping.get(e.getKey().getA());
            if (leaderboardRowDTO != null) {
                List<LegEntryDTO> newLegDetails = leaderboardRowDTO.fieldsByRaceColumnName.get(e.getKey().getB()).legDetails;
                assertEquals(e.getValue(), newLegDetails);
            }
        }
        assertAllRowsKeysAreIdenticalToAllLeaderboardRowDTOCompetitors(newVersion);
        assertNotNull(newVersion.rows);
        assertEquals(previousVersion.rows.size()-17, newVersion.rows.size()); // all rows have changed except for 17 that have no leg details in leg 8
        // now assert that for all rows there is no leaderboard entry for all races but R9 and
        // for R9 there either are no leg details or all leg details for all legs other than L8 are null
        for (Map.Entry<CompetitorDTO, LeaderboardRowDTO> e : newVersion.rows.entrySet()) {
            if (e.getValue().fieldsByRaceColumnName != null) {
                assertTrue(e.getValue().fieldsByRaceColumnName.size() <= 1);
                for (Map.Entry<String, LeaderboardEntryDTO> e2 : e.getValue().fieldsByRaceColumnName.entrySet()) {
                    if (e2.getKey().equals(nameOfRaceColumnToChange)) {
                        List<LegEntryDTO> r9LegDetails = e2.getValue().legDetails;
                        if (r9LegDetails != null) {
                            for (int i=0; i<r9LegDetails.size(); i++) {
                                if (i != indexOfLegToChange) {
                                    assertNull(r9LegDetails.get(i));
                                } else {
                                    assertNotNull(r9LegDetails.get(i));
                                }
                            }
                        }
                    } else {
                        // if there is an entry for any column other than R9 (which is not really expected) then the entry is expected to be null
                        assertNull(e2.getValue());
                    }
                }
            }
        }
        LeaderboardDTO applied = newVersion.getLeaderboardDTO(previousVersion);
        assertEquals(rowsBeforeStripping, applied.rows);
    }
    
    @Test
    public void testCompetitorListChange() {
        newVersion.competitors = new ArrayList<CompetitorDTO>(newVersion.competitors); // clone competitor list so it's not identical to that of previous version
        CompetitorDTO somebodyNew = new CompetitorDTOImpl("Someone New", "DE", "GER", "Germany", "GER 1234", "912p09871203987",
                new BoatClassDTO("505", 5.05));
        newVersion.competitors.add(13, somebodyNew); // insert a competitor; this should mess up all others' indexes; check if this works
        CompetitorDTO wolfgang = getPreviousCompetitorByName("HUNGER +JESS");
        newVersion.competitors.remove(wolfgang);
        newVersion.rows.remove(wolfgang); // remove another competitor
        List<CompetitorDTO> newCompetitorsBeforeStripping = new ArrayList<CompetitorDTO>(newVersion.competitors);
        newVersion.strip(previousVersion);
        assertAllRowsKeysAreIdenticalToAllLeaderboardRowDTOCompetitors(newVersion);
        assertNull(newVersion.competitors); // but there should be an added competitor that we can't see through the public interface
        LeaderboardDTO applied = newVersion.getLeaderboardDTO(previousVersion);
        assertEquals(newCompetitorsBeforeStripping, applied.competitors);
    }

    @Test
    public void testSuppressionChange() {
        final List<CompetitorDTO> newSuppressedCompetitors = new ArrayList<CompetitorDTO>();
        Util.addAll(newVersion.getSuppressedCompetitors(), newSuppressedCompetitors);
        newVersion.setSuppressedCompetitors(newSuppressedCompetitors);
        newVersion.competitors = new ArrayList<CompetitorDTO>(newVersion.competitors); // clone competitor list so it's not identical to that of previous version
        CompetitorDTO somebodyNew = new CompetitorDTOImpl("Someone New", "DE", "GER", "Germany", "GER 1234", "912p09871203987",
                new BoatClassDTO("505", 5.05));
        newVersion.setSuppressed(newVersion.competitors.get(13), true); // suppress an existing competitor; compaction should reduce this to a single number only
        newVersion.setSuppressed(somebodyNew, true); // check that mixed mode with existing and new competitors works as well
        List<CompetitorDTO> newSuppressedCompetitorsBeforeStripping = new ArrayList<CompetitorDTO>();
        Util.addAll(newVersion.getSuppressedCompetitors(), newSuppressedCompetitorsBeforeStripping);
        newVersion.strip(previousVersion);
        assertAllRowsKeysAreIdenticalToAllLeaderboardRowDTOCompetitors(newVersion);
        assertEquals(2, Util.size(newVersion.getSuppressedCompetitors()));
        assertTrue(Util.contains(newVersion.getSuppressedCompetitors(), somebodyNew));
        for (CompetitorDTO compactSuppressedCompetitor : newVersion.getSuppressedCompetitors()) {
            if (compactSuppressedCompetitor != somebodyNew) {
                assertFalse(compactSuppressedCompetitor instanceof CompetitorDTOImpl); // assert that the existing competitor was compacted
            }
        }
        LeaderboardDTO applied = newVersion.getLeaderboardDTO(previousVersion);
        assertEquals(newSuppressedCompetitorsBeforeStripping, applied.getSuppressedCompetitors());
    }

    @Test
    public void testDisplayNameChange() {
        newVersion.competitors = new ArrayList<CompetitorDTO>(newVersion.competitors); // clone competitor list so it's not identical to that of previous version
        CompetitorDTO somebodyNew = new CompetitorDTOImpl("Someone New", "DE", "GER", "Germany", "GER 1234", "912p09871203987",
                new BoatClassDTO("505", 5.05));
        newVersion.competitors.add(somebodyNew);
        newVersion.competitorDisplayNames = new HashMap<CompetitorDTO, String>(newVersion.competitorDisplayNames);
        newVersion.competitorDisplayNames.put(newVersion.competitors.get(13), "Humba");
        newVersion.competitorDisplayNames.put(somebodyNew, "Trala");
        final HashMap<CompetitorDTO, String> newDisplayNamesBeforeStripping = new HashMap<CompetitorDTO, String>();
        newDisplayNamesBeforeStripping.putAll(newVersion.competitorDisplayNames);
        newVersion.strip(previousVersion);
        assertAllRowsKeysAreIdenticalToAllLeaderboardRowDTOCompetitors(newVersion);
        assertEquals(2, newVersion.competitorDisplayNames.size());
        assertTrue(newVersion.competitorDisplayNames.keySet().contains(somebodyNew));
        for (CompetitorDTO compactSuppressedCompetitor : newVersion.competitorDisplayNames.keySet()) {
            if (compactSuppressedCompetitor != somebodyNew) {
                assertFalse(compactSuppressedCompetitor instanceof CompetitorDTOImpl); // assert that the existing competitor was compacted
            }
        }
        LeaderboardDTO applied = newVersion.getLeaderboardDTO(previousVersion);
        assertEquals(newDisplayNamesBeforeStripping, applied.competitorDisplayNames);
    }

    @Test
    public void testCompetitorOrderingInRaceChange() {
        RaceColumnDTO r9 = newVersion.getRaceColumnByName("R9");
        Map<String, List<CompetitorDTO>> newCompetitorOrderingPerRace = new HashMap<String, List<CompetitorDTO>>(newVersion.getCompetitorOrderingPerRaceColumnName());
        newVersion.setCompetitorOrderingPerRace(newCompetitorOrderingPerRace);
        List<CompetitorDTO> newOrdering = new ArrayList<CompetitorDTO>(newVersion.getCompetitorsFromBestToWorst(r9));
        newVersion.setCompetitorsFromBestToWorst(r9, newOrdering);
        newVersion.competitors = new ArrayList<CompetitorDTO>(newVersion.competitors); // clone competitor list so it's not identical to that of previous version
        CompetitorDTO somebodyNew = new CompetitorDTOImpl("Someone New", "DE", "GER", "Germany", "GER 1234", "912p09871203987",
                new BoatClassDTO("505", 5.05));
        newVersion.competitors.add(somebodyNew);
        newOrdering.add(somebodyNew);
        CompetitorDTO formerRank13 = newOrdering.remove(13);
        newOrdering.add(12, formerRank13);
        List<CompetitorDTO> newOrderBeforeStripping = new ArrayList<CompetitorDTO>(newOrdering);
        newVersion.strip(previousVersion);
        assertAllRowsKeysAreIdenticalToAllLeaderboardRowDTOCompetitors(newVersion);
        for (int i=1; i<9; i++) {
            assertNull(newVersion.getCompetitorsFromBestToWorst("R"+i));
        }
        assertEquals(newVersion.getCompetitorsFromBestToWorst(r9).size()-1, newVersion.getCompetitorsFromBestToWorst(r9).indexOf(somebodyNew));
        for (CompetitorDTO compactSuppressedCompetitor : newVersion.getCompetitorsFromBestToWorst(r9)) {
            if (compactSuppressedCompetitor != somebodyNew) {
                assertFalse(compactSuppressedCompetitor instanceof CompetitorDTOImpl); // assert that the existing competitor was compacted
            }
        }
        LeaderboardDTO applied = newVersion.getLeaderboardDTO(previousVersion);
        assertEquals(newOrderBeforeStripping, applied.getCompetitorsFromBestToWorst(applied.getRaceColumnByName("R9")));
    }
    
    private void assertAllRowsKeysAreIdenticalToAllLeaderboardRowDTOCompetitors(LeaderboardDTO leaderboard) {
        if (leaderboard.rows != null) {
            for (Map.Entry<CompetitorDTO, LeaderboardRowDTO> e : leaderboard.rows.entrySet()) {
                assertSame(e.getKey(), e.getValue().competitor);
            }
        }
    }

    @Test
    public void testPartialRaceColumnDTOCompaction() throws IllegalArgumentException, IllegalAccessException, NoSuchFieldException, SecurityException {
        // create a modified R9 RaceDTO clone in newVersion to make sure that even changing a property in the RaceDTO will keep the RaceColumnDTO from being omitted
        RaceColumnDTO r9 = newVersion.getRaceColumnByName("R9");
        RaceColumnDTO clonedR9 = new RaceColumnDTO(r9.isValidInTotalScore());
        cloner.clone(r9, clonedR9);
        // also clone the racesPerFleet map, or else we'd be modifying the previous version's one too
        final Field racesPerFleetField = clonedR9.getClass().getDeclaredField("racesPerFleet");
        racesPerFleetField.setAccessible(true);
        @SuppressWarnings("unchecked")
        final Map<FleetDTO, RaceDTO> m = (Map<FleetDTO, RaceDTO>) racesPerFleetField.get(clonedR9);
        racesPerFleetField.set(clonedR9, new HashMap<FleetDTO, RaceDTO>(m));
        final FleetDTO defaultFleet = r9.getFleets().iterator().next();
        RaceDTO r9Race = r9.getRace(defaultFleet);
        RaceDTO clonedR9Race = new RaceDTO();
        cloner.clone(r9Race, clonedR9Race);
        clonedR9.setRace(defaultFleet, clonedR9Race);
        clonedR9Race.endOfRace = new MillisecondsTimePoint(r9Race.endOfRace).plus(1234).asDate();
        List<RaceColumnDTO> clonedRaceList = new ArrayList<RaceColumnDTO>(newVersion.getRaceList());
        clonedRaceList.set(clonedRaceList.indexOf(r9), clonedR9);
        newVersion.setRaceList(clonedRaceList);
        List<RaceColumnDTO> raceListBeforeStripping = new ArrayList<RaceColumnDTO>(clonedRaceList);
        newVersion.strip(previousVersion);
        assertAllRowsKeysAreIdenticalToAllLeaderboardRowDTOCompetitors(newVersion);
        for (int i=0; i<8; i++) {
            assertNull(newVersion.getRaceList().get(i));
        }
        assertNotNull(newVersion.getRaceList().get(8));
        LeaderboardDTO applied = newVersion.getLeaderboardDTO(previousVersion);
        assertEquals(raceListBeforeStripping, applied.getRaceList());
    }
}
