package com.sap.sailing.domain.base.racegroup.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.racegroup.CurrentRaceFilter;
import com.sap.sailing.domain.base.racegroup.RaceCell;
import com.sap.sailing.domain.base.racegroup.RaceGroup;
import com.sap.sailing.domain.base.racegroup.RaceGroupSeriesFleetRaceColumn;
import com.sap.sailing.domain.base.racegroup.RaceRow;
import com.sap.sailing.domain.base.racegroup.SeriesWithRows;
import com.sap.sailing.domain.common.BoatClassMasterdata;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;

public class CurrentRaceFilterImplTest {

    private CurrentRaceFilter<SimpleFilterableRace> fixture;
    private RaceGroup _505;
    private RaceGroup yes;
    private RaceGroup isaf;
    private RaceGroup league;
    private RaceGroup ess;
    private RaceGroup iQFOil;
    private Map<RaceGroupSeriesFleetRaceColumn, SimpleFilterableRace> races;

    @BeforeEach
    public void setUp() {
        races = new HashMap<>();
        // five regattas ("RaceGroup"s):
        // - simple single-series, single fleet set-up (505 style)
        // - single-series, two fleets (Yellow / Blue, as used by YES)
        // - qualification with Yellow / Blue; final with Gold / Silver; Medal
        // - league set-up with single series with three fleets Red/Green/Blue
        _505 = create505Regatta();
        races.putAll(getAllRaces(_505));
        yes = createYesRegatta();
        races.putAll(getAllRaces(yes));
        isaf = createIsafRegatta();
        races.putAll(getAllRaces(isaf));
        league = createLeagueRegatta();
        races.putAll(getAllRaces(league));
        ess = createEssRegatta();
        races.putAll(getAllRaces(ess));
        iQFOil = createiQFOilRegatta();
        races.putAll(getAllRaces(iQFOil));
        fixture = new CurrentRaceFilterImpl<>(races.values());
    }

    private RaceGroup create505Regatta() {
        final List<SeriesWithRows> _505Series = new ArrayList<>();
        final List<RaceRow> _505RaceRows = new ArrayList<>();
        final RaceRow _505RaceRow = createRaceRow(/* firstRaceColumnNumber */ 1, /* numberOfRaces */ 9, /* raceColumnNamePrefix */ "R", /* fleetName */ "Default");
        _505RaceRows.add(_505RaceRow);
        _505Series.add(new SeriesWithRowsImpl("Default", /* isMedal */ false, /* isFleetsCanRunInParallel */ false, _505RaceRows));
        final RaceGroup _505 = new RaceGroupImpl("505", /* displayName */ null,
                new BoatClassImpl(BoatClassMasterdata._5O5), /* canBoatsOfCompetitorsChangePerRace */ false,
                _505Series, /* regattaConfiguration */ null);
        return _505;
    }

    private RaceGroup createYesRegatta() {
        final List<SeriesWithRows> yesSeries = new ArrayList<>();
        final List<RaceRow> yesRaceRows = Arrays.asList(createRaceRow(1, 9, "R", "Yellow"), createRaceRow(1, 9, "R", "Blue"));
        yesSeries.add(new SeriesWithRowsImpl("Default", /* isMedal */ false, /* isFleetsCanRunInParallel */ true, yesRaceRows));
        final RaceGroup yes = new RaceGroupImpl("YES", /* displayName */ null,
                new BoatClassImpl(BoatClassMasterdata._470), /* canBoatsOfCompetitorsChangePerRace */ false,
                yesSeries, /* regattaConfiguration */ null);
        return yes;
    }

    private RaceGroup createIsafRegatta() {
        final List<SeriesWithRows> isafSeries = new ArrayList<>();
        final List<RaceRow> isafQualificationRaceRows = Arrays.asList(createRaceRow(1, 5, "Q", "Yellow"), createRaceRow(1, 5, "Q", "Blue"));
        isafSeries.add(new SeriesWithRowsImpl("Qualification", /* isMedal */ false, /* isFleetsCanRunInParallel */ true, isafQualificationRaceRows));
        final List<RaceRow> isafFinalRaceRows = Arrays.asList(createRaceRow(6, 5, "F", "Gold"), createRaceRow(6, 5, "F", "Silver"));
        isafSeries.add(new SeriesWithRowsImpl("Final", /* isMedal */ false, /* isFleetsCanRunInParallel */ true, isafFinalRaceRows));
        final List<RaceRow> isafMedalRaceRows = Arrays.asList(createRaceRow(1, 1, "M", "Medal"));
        isafSeries.add(new SeriesWithRowsImpl("Medal", /* isMedal */ true, /* isFleetsCanRunInParallel */ true, isafMedalRaceRows));
        final RaceGroup isaf = new RaceGroupImpl("ISAF", /* displayName */ null,
                new BoatClassImpl(BoatClassMasterdata._470), /* canBoatsOfCompetitorsChangePerRace */ false,
                isafSeries, /* regattaConfiguration */ null);
        return isaf;
    }

    private RaceGroup createEssRegatta() {
        final List<SeriesWithRows> essSeries = new ArrayList<>();
        final List<RaceRow> essDay1RaceRows = Arrays.asList(createRaceRow(1, 5, "R", "Default"));
        essSeries.add(new SeriesWithRowsImpl("Day 1", /* isMedal */ false, /* isFleetsCanRunInParallel */ true, essDay1RaceRows));
        final List<RaceRow> essDay2RaceRows = Arrays.asList(createRaceRow(6, 5, "R", "Default"));
        essSeries.add(new SeriesWithRowsImpl("Day 2", /* isMedal */ false, /* isFleetsCanRunInParallel */ true, essDay2RaceRows));
        final List<RaceRow> essKnockoutQualificationRaceRows = Arrays.asList(createRaceRow(1, 1, "Q", "Yellow"), createRaceRow(1, 1, "Q", "Blue"));
        essSeries.add(new SeriesWithRowsImpl("Knockout Qualification", /* isMedal */ false, /* isFleetsCanRunInParallel */ true, essKnockoutQualificationRaceRows));
        final List<RaceRow> essKnockoutScoringRaceRows = Arrays.asList(createRaceRow(1, 1, "F", "Gold"), createRaceRow(1, 1, "F", "Silver"));
        essSeries.add(new SeriesWithRowsImpl("Knockout Finals", /* isMedal */ false, /* isFleetsCanRunInParallel */ false, essKnockoutScoringRaceRows));
        final List<RaceRow> essDay3RaceRows = Arrays.asList(createRaceRow(11, 5, "R", "Default"));
        essSeries.add(new SeriesWithRowsImpl("Day 3", /* isMedal */ false, /* isFleetsCanRunInParallel */ true, essDay3RaceRows));
        final List<RaceRow> essDay4RaceRows = Arrays.asList(createRaceRow(16, 5, "R", "Default"));
        essSeries.add(new SeriesWithRowsImpl("Day 4", /* isMedal */ false, /* isFleetsCanRunInParallel */ true, essDay4RaceRows));
        final RaceGroup ess = new RaceGroupImpl("ESS", /* displayName */ null,
                new BoatClassImpl(BoatClassMasterdata.EXTREME_40), /* canBoatsOfCompetitorsChangePerRace */ false,
                essSeries, /* regattaConfiguration */ null);
        return ess;
    }

    private RaceGroup createLeagueRegatta() {
        final List<SeriesWithRows> leagueSeries = new ArrayList<>();
        final List<RaceRow> leagueRaceRows = Arrays.asList(createRaceRow(1, 15, "F", "Red"), createRaceRow(1, 15, "F", "Green"), createRaceRow(1, 15, "F", "Blue"));
        leagueSeries.add(new SeriesWithRowsImpl("Default", /* isMedal */ false, /* isFleetsCanRunInParallel */ false, leagueRaceRows));
        final RaceGroup league = new RaceGroupImpl("League", /* displayName */ null,
                new BoatClassImpl(BoatClassMasterdata.J70), /* canBoatsOfCompetitorsChangePerRace */ false,
                leagueSeries, /* regattaConfiguration */ null);
        return league;
    }

    private RaceGroup createiQFOilRegatta() {
        final List<SeriesWithRows> iQFOilSeries = new ArrayList<>();
        final List<RaceRow> iQFOilOpeningSeries1RaceRows = Arrays.asList(createRaceRow(1, 1, "R", "Default"));
        iQFOilSeries.add(new SeriesWithRowsImpl("Opening (1)", /* isMedal */ false, /* isFleetsCanRunInParallel */ true, iQFOilOpeningSeries1RaceRows));
        final List<RaceRow> iQFOilMarathonRaceRows = Arrays.asList(createRaceRow(2, 0, "R", "Default"));
        iQFOilSeries.add(new SeriesWithRowsImpl("Marathon", /* isMedal */ false, /* isFleetsCanRunInParallel */ true, iQFOilMarathonRaceRows));
        final List<RaceRow> iQFOilOpeningSeries2RaceRows = Arrays.asList(createRaceRow(16, 0, "R", "Default"));
        iQFOilSeries.add(new SeriesWithRowsImpl("Opening (2)", /* isMedal */ false, /* isFleetsCanRunInParallel */ true, iQFOilOpeningSeries2RaceRows));
        final List<RaceRow> iQFOilQuarterFinalRaceRows = Arrays.asList(createRaceRow(1, 1, "QF", "Grand Final"), createRaceRow(1, 1, "QF", "Semi Final"), createRaceRow(1, 1, "QF", "Quarter Final"), createRaceRow(1, 1, "QF", "Not Qualified"));
        iQFOilSeries.add(new SeriesWithRowsImpl("Quarter Final", /* isMedal */ false, /* isFleetsCanRunInParallel */ true, iQFOilQuarterFinalRaceRows));
        final List<RaceRow> iQFOilSemiFinalRaceRows = Arrays.asList(createRaceRow(1, 1, "SF", "Grand Final"), createRaceRow(1, 1, "SF", "Semi Final"), createRaceRow(1, 1, "SF", "Not Qualified for Semi Final"), createRaceRow(1, 1, "SF", "Not Qualified for Quarter Final"));
        iQFOilSeries.add(new SeriesWithRowsImpl("Quarter Final", /* isMedal */ false, /* isFleetsCanRunInParallel */ true, iQFOilSemiFinalRaceRows));
        final List<RaceRow> iQFOilGrandFinalRaceRows = Arrays.asList(createRaceRow(1, 1, "QF", "Grand Final"));
        iQFOilSeries.add(new SeriesWithRowsImpl("Quarter Final", /* isMedal */ true, /* isFleetsCanRunInParallel */ false, iQFOilGrandFinalRaceRows));
        final RaceGroup iQFOil = new RaceGroupImpl("iQFOil", /* displayName */ null,
                new BoatClassImpl(BoatClassMasterdata.IQFOIL_MEN), /* canBoatsOfCompetitorsChangePerRace */ false,
                iQFOilSeries, /* regattaConfiguration */ null);
        return iQFOil;
    }

    private Map<RaceGroupSeriesFleetRaceColumn, SimpleFilterableRace> getAllRaces(RaceGroup raceGroup) {
        final Map<RaceGroupSeriesFleetRaceColumn, SimpleFilterableRace> result = new HashMap<>();
        int zeroBasedSeriesIndex = 0;
        for (SeriesWithRows series : raceGroup.getSeries()) {
            for (final RaceRow raceRow : series.getRaceRows()) {
                int zeroBasedIndexInFleet = 0;
                for (final RaceCell raceCell : raceRow.getCells()) {
                    final SimpleFilterableRace race = new SimpleFilterableRace(raceGroup, series, raceRow.getFleet(), zeroBasedIndexInFleet, zeroBasedSeriesIndex, raceCell.getName());
                    result.put(new RaceGroupSeriesFleetRaceColumn(race), race);
                    zeroBasedIndexInFleet++;
                }
            }
            zeroBasedSeriesIndex++;
        }
        return result;
    }

    @Test
    public void testRaceAccessAndStatusUpdate() {
        SimpleFilterableRace isafQ4Yellow = get(isaf, "F7", "Gold");
        assertNotNull(isafQ4Yellow);
        assertEquals("F7", isafQ4Yellow.getRaceColumnName());
        assertEquals("Gold", isafQ4Yellow.getFleet().getName());
        assertEquals(1, isafQ4Yellow.getZeroBasedIndexInFleet());
        assertEquals(1, isafQ4Yellow.getZeroBasedSeriesIndex());
        assertEquals(RaceLogRaceStatus.UNSCHEDULED, isafQ4Yellow.getStatus());
        isafQ4Yellow.setStatus(RaceLogRaceStatus.SCHEDULED);
        assertEquals(RaceLogRaceStatus.SCHEDULED, isafQ4Yellow.getStatus());
    }
    
    @Test
    public void testBasicFilteringForFirstUnscheduledRaces() {
        // with all races unscheduled we can expect the first race of each regatta's first series to show
        final Set<SimpleFilterableRace> currentRaces = fixture.getCurrentRaces();

        {
        final Set<SimpleFilterableRace> current505Races = currentRaces.stream().filter(r->r.getRaceGroup() == _505).collect(Collectors.toSet());
        assertEquals(1, current505Races.size());
        final SimpleFilterableRace firstRace = current505Races.iterator().next();
        assertEquals("R1", firstRace.getRaceColumnName());
        }
        {
        final Set<SimpleFilterableRace> currentYesRaces = currentRaces.stream().filter(r->r.getRaceGroup() == yes).collect(Collectors.toSet());
        assertEquals(2, currentYesRaces.size());
        for (final SimpleFilterableRace currentYesRace : currentYesRaces) {
            assertEquals("R1", currentYesRace.getRaceColumnName());
        }
        }
        {
        final Set<SimpleFilterableRace> currentIsafRaces = currentRaces.stream().filter(r->r.getRaceGroup() == isaf).collect(Collectors.toSet());
        assertEquals(2, currentIsafRaces.size());
        for (final SimpleFilterableRace currentYesRace : currentIsafRaces) {
            assertEquals("Q1", currentYesRace.getRaceColumnName());
        }
        }
        {
        final Set<SimpleFilterableRace> currentLeagueRaces = currentRaces.stream().filter(r->r.getRaceGroup() == league).collect(Collectors.toSet());
        assertEquals(1, currentLeagueRaces.size());
        final SimpleFilterableRace firstRace = currentLeagueRaces.iterator().next();
        assertEquals("Red", firstRace.getFleet().getName());
        assertEquals("F1", firstRace.getRaceColumnName());
        }
        {
        final Set<SimpleFilterableRace> currentEssRaces = currentRaces.stream().filter(r->r.getRaceGroup() == ess).collect(Collectors.toSet());
        assertEquals(1, currentEssRaces.size());
        final SimpleFilterableRace firstRace = currentEssRaces.iterator().next();
        assertEquals("Default", firstRace.getFleet().getName());
        assertEquals("R1", firstRace.getRaceColumnName());
        }
    }

    @Test
    public void testSchedulingFirst505Race() {
        // with all races unscheduled we can expect the first race of each regatta's first series to show
        get(_505, "R1", "Default").setStatus(RaceLogRaceStatus.SCHEDULED);
        final Set<SimpleFilterableRace> currentRaces = fixture.getCurrentRaces();
        final Set<SimpleFilterableRace> current505Races = currentRaces.stream().filter(r->r.getRaceGroup() == _505).collect(Collectors.toSet());
        assertEquals(2, current505Races.size());
        assertTrue(current505Races.contains(get(_505, "R1", "Default"))); // it's scheduled, so it shows
        assertTrue(current505Races.contains(get(_505, "R2", "Default"))); // it's unscheduled and has a scheduled immediate predecessor
    }

    @Test
    public void testSchedulingRaceAfterEmptySeries() {
        {
        final Set<SimpleFilterableRace> currentRaces = fixture.getCurrentRaces();
        // with all races unscheduled we can expect the first race of each regatta's first series to show
        final Set<SimpleFilterableRace> currentiQFOilRaces = currentRaces.stream().filter(r->r.getRaceGroup() == iQFOil).collect(Collectors.toSet());
        assertEquals(1, currentiQFOilRaces.size());
        final SimpleFilterableRace firstRace = currentiQFOilRaces.iterator().next();
        assertEquals("R1", firstRace.getRaceColumnName());
        }
        get(iQFOil, "R1", "Default").setStatus(RaceLogRaceStatus.SCHEDULED);
        {
        final Set<SimpleFilterableRace> currentRaces = fixture.getCurrentRaces();
        final Set<SimpleFilterableRace> currentiQFOilRaces = currentRaces.stream().filter(r->r.getRaceGroup() == iQFOil).collect(Collectors.toSet());
        assertEquals(5, currentiQFOilRaces.size());
        assertTrue(currentiQFOilRaces.contains(get(iQFOil, "R1", "Default"))); // it's scheduled, so it shows
        assertTrue(currentiQFOilRaces.contains(get(iQFOil, "QF1", "Quarter Final")));
        assertTrue(currentiQFOilRaces.contains(get(iQFOil, "QF1", "Semi Final")));
        assertTrue(currentiQFOilRaces.contains(get(iQFOil, "QF1", "Quarter Final")));
        assertTrue(currentiQFOilRaces.contains(get(iQFOil, "QF1", "Semi Final")));
        }
    }


    @Test
    public void testSchedulingFirstIsafRace() {
        // with all races unscheduled we can expect the first race of each regatta's first series to show
        get(isaf, "Q1", "Yellow").setStatus(RaceLogRaceStatus.SCHEDULED);
        final Set<SimpleFilterableRace> currentRaces = fixture.getCurrentRaces();
        final Set<SimpleFilterableRace> currentIsafRaces = currentRaces.stream().filter(r->r.getRaceGroup() == isaf).collect(Collectors.toSet());
        assertEquals(3, currentIsafRaces.size());
        assertTrue(currentIsafRaces.contains(get(isaf, "Q1", "Yellow"))); // it's scheduled, so it shows
        assertTrue(currentIsafRaces.contains(get(isaf, "Q1", "Blue")));   // it's unscheduled, first in fleet
        assertTrue(currentIsafRaces.contains(get(isaf, "Q2", "Yellow"))); // predecessor Q1/Yellow is scheduled, so show this as next in fleet
    }

    @Test
    public void testSchedulingFirstEssRace() {
        // with all races unscheduled we can expect the first race of each regatta's first series to show
        get(ess, "R1", "Default").setStatus(RaceLogRaceStatus.SCHEDULED);
        final Set<SimpleFilterableRace> currentRaces = fixture.getCurrentRaces();
        final Set<SimpleFilterableRace> currentEssRaces = currentRaces.stream().filter(r->r.getRaceGroup() == ess).collect(Collectors.toSet());
        assertEquals(2, currentEssRaces.size());
        assertTrue(currentEssRaces.contains(get(ess, "R1", "Default"))); // it's scheduled, so it shows
        assertTrue(currentEssRaces.contains(get(ess, "R2", "Default")));   // it's unscheduled, first after last scheduled
    }

    @Test
    public void testFinishingAllEssRacesBeforeKnockoutQualification() {
        for (int i=1; i<=10; i++) {
            get(ess, "R"+i, "Default").setStatus(RaceLogRaceStatus.FINISHED);
        }
        final Set<SimpleFilterableRace> currentRaces = fixture.getCurrentRaces();
        final Set<SimpleFilterableRace> currentEssRaces = currentRaces.stream().filter(r->r.getRaceGroup() == ess).collect(Collectors.toSet());
        assertEquals(2, currentEssRaces.size());
        assertTrue(currentEssRaces.contains(get(ess, "Q1", "Yellow")));
        assertTrue(currentEssRaces.contains(get(ess, "Q1", "Blue")));
    }

    @Test
    public void testFinishingAllEssRacesBeforeKnockoutQualificationAndSchedulingFirstKnockout() {
        for (int i=1; i<=10; i++) {
            get(ess, "R"+i, "Default").setStatus(RaceLogRaceStatus.FINISHED);
        }
        get(ess, "Q1", "Yellow").setStatus(RaceLogRaceStatus.SCHEDULED);
        final Set<SimpleFilterableRace> currentRaces = fixture.getCurrentRaces();
        final Set<SimpleFilterableRace> currentEssRaces = currentRaces.stream().filter(r->r.getRaceGroup() == ess).collect(Collectors.toSet());
        assertEquals(3, currentEssRaces.size());
        assertTrue(currentEssRaces.contains(get(ess, "Q1", "Yellow"))); // scheduled
        assertTrue(currentEssRaces.contains(get(ess, "Q1", "Blue")));   // unscheduled
        assertTrue(currentEssRaces.contains(get(ess, "F1", "Gold")));   // first unscheduled in series; last column in preceding series has scheduled race
        // F1/Silver remains invisible because the knockout finals runs its fleets in sequence
    }

    @Test
    public void testFinishingAllEssRacesBeforeKnockoutFinals() {
        for (int i=1; i<=10; i++) {
            get(ess, "R"+i, "Default").setStatus(RaceLogRaceStatus.FINISHED);
        }
        get(ess, "Q1", "Yellow").setStatus(RaceLogRaceStatus.FINISHED);
        get(ess, "Q1", "Blue").setStatus(RaceLogRaceStatus.FINISHED);
        final Set<SimpleFilterableRace> currentRaces = fixture.getCurrentRaces();
        final Set<SimpleFilterableRace> currentEssRaces = currentRaces.stream().filter(r->r.getRaceGroup() == ess).collect(Collectors.toSet());
        assertEquals(1, currentEssRaces.size());
        assertTrue(currentEssRaces.contains(get(ess, "F1", "Gold")));   // first unscheduled in series; last column in preceding series has scheduled race
        // F1/Silver remains invisible because the knockout finals runs its fleets in sequence
    }

    @Test
    public void testFinishingAllEssRacesBeforeKnockoutFinalsAndGoldRaceStarted() {
        for (int i=1; i<=10; i++) {
            get(ess, "R"+i, "Default").setStatus(RaceLogRaceStatus.FINISHED);
        }
        get(ess, "Q1", "Yellow").setStatus(RaceLogRaceStatus.FINISHED);
        get(ess, "Q1", "Blue").setStatus(RaceLogRaceStatus.FINISHED);
        get(ess, "F1", "Gold").setStatus(RaceLogRaceStatus.RUNNING);
        final Set<SimpleFilterableRace> currentRaces = fixture.getCurrentRaces();
        final Set<SimpleFilterableRace> currentEssRaces = currentRaces.stream().filter(r->r.getRaceGroup() == ess).collect(Collectors.toSet());
        assertEquals(2, currentEssRaces.size());
        assertTrue(currentEssRaces.contains(get(ess, "F1", "Gold")));   // running
        assertTrue(currentEssRaces.contains(get(ess, "F1", "Silver"))); // first unscheduled with a predecessor that's running
    }

    @Test
    public void testFinishingAllEssRacesBeforeKnockoutFinalsAndGoldRaceFinishedAndSilverRaceScheduled() {
        for (int i=1; i<=10; i++) {
            get(ess, "R"+i, "Default").setStatus(RaceLogRaceStatus.FINISHED);
        }
        get(ess, "Q1", "Yellow").setStatus(RaceLogRaceStatus.FINISHED);
        get(ess, "Q1", "Blue").setStatus(RaceLogRaceStatus.FINISHED);
        get(ess, "F1", "Gold").setStatus(RaceLogRaceStatus.FINISHED);
        get(ess, "F1", "Silver").setStatus(RaceLogRaceStatus.SCHEDULED);
        final Set<SimpleFilterableRace> currentRaces = fixture.getCurrentRaces();
        final Set<SimpleFilterableRace> currentEssRaces = currentRaces.stream().filter(r->r.getRaceGroup() == ess).collect(Collectors.toSet());
        assertEquals(2, currentEssRaces.size());
        assertTrue(currentEssRaces.contains(get(ess, "F1", "Silver")));   // scheduled
        assertTrue(currentEssRaces.contains(get(ess, "R11", "Default"))); // first unscheduled in series with the last race in the preceding series scheduled
    }

    @Test
    public void testSchedulingAllIsafQualificationRacesButOne() {
        for (int i=1; i<=4; i++) {
            get(isaf, "Q"+i, "Yellow").setStatus(RaceLogRaceStatus.FINISHED);
            get(isaf, "Q"+i, "Blue").setStatus(RaceLogRaceStatus.FINISHED);
        }
        get(isaf, "Q5", "Blue").setStatus(RaceLogRaceStatus.SCHEDULED);
        final Set<SimpleFilterableRace> currentRaces = fixture.getCurrentRaces();
        final Set<SimpleFilterableRace> currentIsafRaces = currentRaces.stream().filter(r->r.getRaceGroup() == isaf).collect(Collectors.toSet());
        assertEquals(4, currentIsafRaces.size());
        assertTrue(currentIsafRaces.contains(get(isaf, "Q5", "Yellow"))); // it's unscheduled; its predecessor Q4/Yellow is FINISHED; show it
        assertTrue(currentIsafRaces.contains(get(isaf, "Q5", "Blue")));   // it's scheduled
        assertTrue(currentIsafRaces.contains(get(isaf, "F6", "Gold")));   // Q5/Blue is a scheduled immediate predecessor
        assertTrue(currentIsafRaces.contains(get(isaf, "F6", "Silver")));   // Q5/Blue is a scheduled immediate predecessor
    }

    @Test
    public void testFinishingAllIsafQualificationAndFinalRaces() {
        // the medal race shall be the only one remaining
        for (int i=1; i<=5; i++) {
            get(isaf, "Q"+i, "Yellow").setStatus(RaceLogRaceStatus.FINISHED);
            get(isaf, "Q"+i, "Blue").setStatus(RaceLogRaceStatus.FINISHED);
            get(isaf, "F"+(i+5), "Gold").setStatus(RaceLogRaceStatus.FINISHED);
            get(isaf, "F"+(i+5), "Silver").setStatus(RaceLogRaceStatus.FINISHED);
        }
        final Set<SimpleFilterableRace> currentRaces = fixture.getCurrentRaces();
        final Set<SimpleFilterableRace> currentIsafRaces = currentRaces.stream().filter(r->r.getRaceGroup() == isaf).collect(Collectors.toSet());
        assertEquals(1, currentIsafRaces.size());
        assertTrue(currentIsafRaces.contains(get(isaf, "M1", "Medal")));
    }

    @Test
    public void testFinishingFirstLeagueRace() {
        // with all races unscheduled we can expect the first race of each regatta's first series to show
        get(league, "F1", "Red").setStatus(RaceLogRaceStatus.FINISHED);
        final Set<SimpleFilterableRace> currentRaces = fixture.getCurrentRaces();
        final Set<SimpleFilterableRace> currentLeagueRaces = currentRaces.stream().filter(r->r.getRaceGroup() == league).collect(Collectors.toSet());
        assertEquals(1, currentLeagueRaces.size());
        assertTrue(currentLeagueRaces.contains(get(league, "F1", "Green"))); // Red is finished, don't show it anymore
    }
    
    @Test
    public void testSkippingSecondLeagueRace() {
        // with all races unscheduled we can expect the first race of each regatta's first series to show
        get(league, "F1", "Red").setStatus(RaceLogRaceStatus.FINISHED);
        get(league, "F1", "Blue").setStatus(RaceLogRaceStatus.SCHEDULED);
        final Set<SimpleFilterableRace> currentRaces = fixture.getCurrentRaces();
        final Set<SimpleFilterableRace> currentLeagueRaces = currentRaces.stream().filter(r->r.getRaceGroup() == league).collect(Collectors.toSet());
        assertEquals(3, currentLeagueRaces.size());
        assertTrue(currentLeagueRaces.contains(get(league, "F1", "Green"))); // Red is finished, don't show it anymore; show the next one instead which is Green
        assertTrue(currentLeagueRaces.contains(get(league, "F1", "Blue")));  // show a scheduled race
        assertTrue(currentLeagueRaces.contains(get(league, "F2", "Red")));   // This is the next one after the scheduled F1/Blue
    }

    @Test
    public void testStartInSecondSeriesOf49erRaces() {
        // with all races unscheduled we start with the first race of the second series
        // we can expect the first race of each regatta's series to show except the first series 
        get(isaf, "F6", "Gold").setStatus(RaceLogRaceStatus.SCHEDULED);
        final Set<SimpleFilterableRace> currentRaces = fixture.getCurrentRaces();
        final Set<SimpleFilterableRace> current49erRaces = currentRaces.stream().filter(r->r.getRaceGroup() == isaf).collect(Collectors.toSet());
        assertEquals(3, current49erRaces.size());
        assertTrue(current49erRaces.contains(get(isaf, "F6", "Gold"))); // it's scheduled, so it shows
        assertTrue(current49erRaces.contains(get(isaf, "F6", "Silver")));   // it's unscheduled, first in fleet
        assertTrue(current49erRaces.contains(get(isaf, "F7", "Gold"))); // predecessor F7/Gold is scheduled, so show this as next in fleet
    }

    @Test
    public void testSchedulingSilverFleetKnockoutFinalRaceAlsoShowsGoldRace() {
        // with nothing scheduled in earlier fleets, F1 Gold shall be scheduled when (erroneously or on purpose)
        // the later race F1 Silver is already scheduled.
        get(ess, "F1", "Silver").setStatus(RaceLogRaceStatus.SCHEDULED);
        final Set<SimpleFilterableRace> currentRaces = fixture.getCurrentRaces();
        final Set<SimpleFilterableRace> current49erRaces = currentRaces.stream().filter(r->r.getRaceGroup() == ess).collect(Collectors.toSet());
        assertEquals(3, current49erRaces.size());
        assertTrue(current49erRaces.contains(get(ess, "F1", "Gold"))); // it's unscheduled but the first in an already started series
        assertTrue(current49erRaces.contains(get(ess, "F1", "Silver")));   // it's scheduled
        assertTrue(current49erRaces.contains(get(ess, "R11", "Default"))); // preceding series' last column has a scheduled race
    }

    private SimpleFilterableRace get(RaceGroup raceGroup, String raceColumnName, String fleetName) {
        for (final SeriesWithRows series : raceGroup.getSeries()) {
            final Fleet fleet = getFleet(series, fleetName);
            if (fleet != null) {
                final RaceRow raceRow = series.getRaceRow(fleet);
                if (raceRow != null) {
                    for (final RaceCell raceCell : raceRow.getCells()) {
                        if (raceCell.getName().equals(raceColumnName)) {
                            return races.get(new RaceGroupSeriesFleetRaceColumn(raceGroup, series, fleet, raceColumnName));
                        }
                    }
                }
            }
        }
        return null;
    }

    private Fleet getFleet(SeriesWithRows series, String fleetName) {
        for (final Fleet fleet : series.getFleets()) {
            if (fleet.getName().equals(fleetName)) {
                return fleet;
            }
        }
        return null;
    }

    private RaceRow createRaceRow(final int firstRaceColumnNumber, final int numberOfRaces,
            final String raceColumnNamePrefix, final String fleetName) {
        final List<RaceCell> _505RaceCells = new ArrayList<>(); 
        for (int i=0; i < numberOfRaces; i++) {
            _505RaceCells.add(new RaceCellImpl(raceColumnNamePrefix+(i+firstRaceColumnNumber),
                    /* race log */ null, /* factor */ firstRaceColumnNumber, /* explicitFactor */ null,
                    /* zeroBasedIndexInFleet */ i, /* targetTime */ null));
        }
        final RaceRowImpl _505RaceRow = new RaceRowImpl(new FleetImpl(fleetName), _505RaceCells);
        return _505RaceRow;
    }
}
