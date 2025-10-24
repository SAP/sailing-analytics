package com.sap.sailing.domain.swisstimingadapter.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.regattalog.RegattaLogStore;
import com.sap.sailing.domain.swisstimingadapter.Competitor;
import com.sap.sailing.domain.swisstimingadapter.CrewMember;
import com.sap.sailing.domain.swisstimingadapter.StartList;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingAdapter;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingFactory;
import com.sap.sailing.domain.tracking.RaceHandle;
import com.sap.sailing.domain.tracking.TrackerManager;
import com.sap.sailing.xrr.resultimport.ParserFactory;
import com.sap.sailing.xrr.schema.Boat;
import com.sap.sailing.xrr.schema.Crew;
import com.sap.sailing.xrr.schema.Division;
import com.sap.sailing.xrr.schema.Event;
import com.sap.sailing.xrr.schema.Person;
import com.sap.sailing.xrr.schema.RaceResult;
import com.sap.sailing.xrr.schema.RegattaResults;
import com.sap.sailing.xrr.schema.Team;
import com.sap.sse.util.HttpUrlConnectionHelper;

public class SwissTimingAdapterImpl implements SwissTimingAdapter {
    private final SwissTimingFactory swissTimingFactory;

    private final com.sap.sailing.domain.swisstimingadapter.DomainFactory swissTimingDomainFactory;

    private final long DEFAULT_SWISSTIMING_LIVE_DELAY_IN_MILLISECONDS = 6000;

    public SwissTimingAdapterImpl(DomainFactory baseDomainFactory) {
        swissTimingFactory = SwissTimingFactory.INSTANCE;
        swissTimingDomainFactory = new DomainFactoryImpl(baseDomainFactory);
    }

    @Override
    public com.sap.sailing.domain.swisstimingadapter.DomainFactory getSwissTimingDomainFactory() {
        return swissTimingDomainFactory;
    }

    @Override
    public SwissTimingFactory getSwissTimingFactory() {
        return swissTimingFactory;
    }

    @Override
    public RaceHandle addSwissTimingRace(TrackerManager trackerManager, RegattaIdentifier regattaToAddTo, String raceID,
            String raceName, String raceDescription, BoatClass boatClass, String hostname, int port,
            StartList startList, RaceLogStore raceLogStore, RegattaLogStore regattaLogStore, long timeoutInMilliseconds,
            boolean useInternalMarkPassingAlgorithm, boolean trackWind, boolean correctWindDirectionByMagneticDeclination,
            String updateURL, String updateUsername, String updatePassword, String eventName, String manage2SailEventUrl) throws Exception {
        return trackerManager.addRace(regattaToAddTo,
                swissTimingDomainFactory.createTrackingConnectivityParameters(hostname, port, raceID, raceName,
                        raceDescription, boatClass, startList, DEFAULT_SWISSTIMING_LIVE_DELAY_IN_MILLISECONDS,
                        swissTimingFactory, swissTimingDomainFactory, raceLogStore, regattaLogStore,
                        useInternalMarkPassingAlgorithm, trackWind, correctWindDirectionByMagneticDeclination,
                        updateURL, updateUsername, updatePassword, eventName, manage2SailEventUrl),
                timeoutInMilliseconds);
    }

    @Override
    public StartList readStartListForRace(String raceId, RegattaResults regattaResults) {
        StartList result = null;
        List<Competitor> competitors = new ArrayList<Competitor>();

        Map<String, Person> persons = new HashMap<String, Person>();
        Map<String, Boat> boats = new HashMap<String, Boat>();
        Map<String, Team> teams = new HashMap<String, Team>();
        // collect boats, persons and teams
        for (Object o : regattaResults.getPersonOrBoatOrTeam()) {
            if (o instanceof Person) {
                Person p = (Person) o;
                persons.put(p.getPersonID(), p);
            } else if (o instanceof Boat) {
                Boat b = (Boat) o;
                boats.put(b.getBoatID(), b);
            } else if (o instanceof Team) {
                Team t = (Team) o;
                teams.put(t.getTeamID(), t);
            }
        }
        for (Object o : regattaResults.getPersonOrBoatOrTeam()) {
            if (o instanceof Event) {
                Event event = (Event) o;
                for (Object eventO : event.getRaceOrDivisionOrRegattaSeriesResult()) {
                    if (eventO instanceof Division) {
                        Division division = (Division) eventO;
                        for (Object seriesResultOrRaceResultOrTRResult : division.getSeriesResultOrRaceResultOrTRResult()) {
                            if (seriesResultOrRaceResultOrTRResult instanceof RaceResult) {
                                RaceResult raceResult = (RaceResult) seriesResultOrRaceResultOrTRResult;
                                if (raceResult.getRaceID().equals(raceId)) {
                                    Team team = teams.get(raceResult.getTeamID());
                                    if (team != null) {
                                        Boat boat = boats.get(team.getBoatID());
                                        if (boat != null) {
                                            List<CrewMember> crew = new ArrayList<CrewMember>();
                                            for (Crew crewMember : team.getCrew()) {
                                                Person person = persons.get(crewMember.getPersonID());
                                                crew.add(new CrewMemberImpl(
                                                        person.getGivenName() + " " + person.getFamilyName(),
                                                        (person.getNOC()==null)?null:(person.getNOC().name()), crewMember.getPosition().name()));
                                            }
                                            String nationality = team.getNOC()==null?null:team.getNOC().name();
                                            CompetitorWithID competitor = new CompetitorWithID(team.getTeamID(),
                                                    boat.getSailNumber(), nationality, team.getTeamName(), crew);
                                            competitors.add(competitor);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        if (!competitors.isEmpty()) {
            result = new StartListImpl(raceId, competitors);
        }
        return result;
    }

    @Override
    public RegattaResults readRegattaEntryListFromXrrUrl(String xrrEntryListUrl) throws IOException, JAXBException, SAXException, ParserConfigurationException {
        // try to read the entry list from manage2sail
        URL regattaEntryListUrl = new URL(xrrEntryListUrl);
        URLConnection regattaEntryListConn = HttpUrlConnectionHelper.redirectConnection(regattaEntryListUrl);
        RegattaResults regattaResults = ParserFactory.INSTANCE
                .createParser((InputStream) regattaEntryListConn.getContent(), xrrEntryListUrl).parse();
        return regattaResults;
    }
}
