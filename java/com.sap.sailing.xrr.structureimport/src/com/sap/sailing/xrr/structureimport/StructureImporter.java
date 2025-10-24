package com.sap.sailing.xrr.structureimport;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Nationality;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.DynamicBoat;
import com.sap.sailing.domain.base.impl.DynamicPerson;
import com.sap.sailing.domain.base.impl.DynamicTeam;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.common.BoatClassMasterdata;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.FleetColors;
import com.sap.sailing.domain.common.LeaderboardNameConstants;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.xrr.resultimport.ParserFactory;
import com.sap.sailing.xrr.schema.Boat;
import com.sap.sailing.xrr.schema.Crew;
import com.sap.sailing.xrr.schema.Division;
import com.sap.sailing.xrr.schema.Event;
import com.sap.sailing.xrr.schema.Person;
import com.sap.sailing.xrr.schema.Race;
import com.sap.sailing.xrr.schema.RegattaResults;
import com.sap.sailing.xrr.schema.Team;
import com.sap.sailing.xrr.structureimport.buildstructure.BuildStructure;
import com.sap.sailing.xrr.structureimport.buildstructure.Fleet;
import com.sap.sailing.xrr.structureimport.buildstructure.GuessFleetOrderingFromFleetName;
import com.sap.sailing.xrr.structureimport.buildstructure.GuessFleetOrderingStrategy;
import com.sap.sailing.xrr.structureimport.buildstructure.RegattaStructure;
import com.sap.sailing.xrr.structureimport.buildstructure.Series;
import com.sap.sailing.xrr.structureimport.buildstructure.SetRacenumberStrategy;
import com.sap.sse.common.Color;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.AbstractColor;
import com.sap.sse.util.HttpUrlConnectionHelper;
import com.sapsailing.xrr.structureimport.eventimport.EventImport;
import com.sapsailing.xrr.structureimport.eventimport.RegattaJSON;

public class StructureImporter {

    private static final Logger logger = Logger.getLogger(StructureImporter.class.getName());
    private LinkedHashMap<String, Boat> boatForPerson;
    private final DomainFactory baseDomainFactory;
    private final SetRacenumberStrategy setRacenumberStrategy;
    private final static String UNKNOWN_BOATCLASS_NAME = "?";

    public StructureImporter(SetRacenumberStrategy setRacenumber, DomainFactory baseDomainFactory) {
        this.setRacenumberStrategy = setRacenumber;
        this.baseDomainFactory = baseDomainFactory;
    }

    public StructureImporter(DomainFactory baseDomainFactory) {
        this(null, baseDomainFactory);
    }

    public StructureImporter() {
        this(null, null);
    }

    public Iterable<RegattaJSON> parseEvent(String url) {
        return new EventImport().getRegattas(url);
    }

    public Iterable<Regatta> getRegattas(Iterable<RegattaJSON> regattas) {
        Iterable<Pair<RegattaJSON, RegattaResults>> parsedRegattas = parseRegattas(regattas);
        Set<Regatta> addSpecificRegattas = new HashSet<Regatta>();
        for (Pair<RegattaJSON, RegattaResults> result : parsedRegattas) {
            List<Race> races = new ArrayList<Race>();
            // assuming that the last element in getPersonOrBoatOrTeam is an event
            Event event = (Event) result.getB().getPersonOrBoatOrTeam().get(result.getB().getPersonOrBoatOrTeam().size() - 1);
            Iterable<Object> raceOrDivisionOrRegattaSeriesResults = event.getRaceOrDivisionOrRegattaSeriesResult();
            for (Object raceOrDivisionOrRegattaSeriesResult : raceOrDivisionOrRegattaSeriesResults) {
                if (raceOrDivisionOrRegattaSeriesResult instanceof Race) {
                    races.add((Race) raceOrDivisionOrRegattaSeriesResult);
                }
            }
            BuildStructure buildStructure = new BuildStructure(races);
            final TimePoint startDate = null; // TODO can regatta start time be inferred from XRR document?
            final TimePoint endDate = null; // TODO can regatta end time be inferred from XRR document?
            final String regattaName = RegattaImpl.getDefaultName(event.getTitle(), ((Division) event
                    .getRaceOrDivisionOrRegattaSeriesResult().get(0)).getTitle());
            final String boatClassName;
            if (result.getA().getBoatClass() != null) {
                boatClassName = result.getA().getBoatClass();
            } else {
                // we need a boat class; use some default
                logger.warning("No boat class set in regatta to import: "+regattaName+". Setting to \""+UNKNOWN_BOATCLASS_NAME+"\"");
                boatClassName = UNKNOWN_BOATCLASS_NAME;
            }
            RegattaImpl regatta = new RegattaImpl(regattaName,
                    baseDomainFactory.getOrCreateBoatClass(boatClassName), 
                    /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                    startDate, endDate, getSeries(buildStructure), false,
                    this.baseDomainFactory.createScoringScheme(ScoringSchemeType.LOW_POINT), event.getEventID(), null,
                    OneDesignRankingMetric::new, /* registrationLinkSecret */ UUID.randomUUID().toString());
            addSpecificRegattas.add(regatta);
        }
        return addSpecificRegattas;
    }

    private Iterable<Pair<RegattaJSON, RegattaResults>> parseRegattas(final Iterable<RegattaJSON> selectedRegattas) {
        final Set<Pair<RegattaJSON, RegattaResults>> result = Collections.synchronizedSet(new HashSet<Pair<RegattaJSON, RegattaResults>>());
        Set<Thread> threads = new HashSet<Thread>();
        for (final RegattaJSON selectedRegatta : selectedRegattas) {
            Thread thread = new Thread("XRR Importer " + selectedRegatta.getName()) {
                @Override
                public void run() {
                    try {
                        result.add(new Pair<>(selectedRegatta, parseRegattaXML(selectedRegatta.getXrrEntriesUrl())));
                    } catch (JAXBException | IOException | SAXException | ParserConfigurationException e) {
                        logger.info("Parse error during XRR import. Ignoring document " + selectedRegatta.getName());
                    }
                }
            };
            thread.start();
            threads.add(thread);
        }
        while (true) {
            try {
                for (Thread thread : threads) {
                    thread.join();
                }
                break;
            } catch (InterruptedException e) {
                // waiting for the other threads
            }
        }
        return result;
    }
    
    private Iterable<SeriesImpl> getSeries(BuildStructure structure) {
        List<SeriesImpl> series = new ArrayList<>(); 
        RegattaStructure regattaStructure = structure.getRegattaStructure();
        if (regattaStructure != null) {
            int index = 0;
            for (Series raceType : regattaStructure.getSeries()) {
                List<com.sap.sailing.domain.base.Fleet> fleets = getFleets(raceType.getFleets());
                setRaceNames(index, raceType, raceType.getFleets());
                series.add(new SeriesImpl(raceType.getSeries(), raceType.isMedal(), true, fleets, raceType.getRaceNames(), null));
            }
        }
        return series;
    }

    private void setRaceNames(int i, Series raceType, List<Fleet> fleets) {
        Iterable<Race> races = fleets.get(raceType.getMaxIndex()).getRaces();
        for (Race race : races) {
            setRacenumberStrategy.setRacenumber(race, raceType, i);
        }
    }
    
    private List<com.sap.sailing.domain.base.Fleet> getFleets(List<Fleet> fleets) {
        List<com.sap.sailing.domain.base.Fleet> fleetsImpl = new ArrayList<com.sap.sailing.domain.base.Fleet>();
        GuessFleetOrderingStrategy fleetOrderingStrategy = new GuessFleetOrderingFromFleetName();
        String fleetColor = "";
        if (fleets.size() <= 1) {
            fleetColor = LeaderboardNameConstants.DEFAULT_FLEET_NAME;
            FleetImpl fleetImpl = new FleetImpl(fleetColor, 0, getColorFromString(fleetColor));
            fleetsImpl.add(fleetImpl);
        } else {
            for (Fleet fleet : fleets) {
                fleetColor = fleet.getColor();
                FleetImpl fleetImpl = new FleetImpl(fleetColor, fleetOrderingStrategy.guessOrder(fleet.getColor()), getColorFromString(fleetColor));
                fleetsImpl.add(fleetImpl);
            }
        }
        return fleetsImpl;
    }

    private Color getColorFromString(final String colorString) {
        Color result = null;
        for (FleetColors fleetColor : FleetColors.values()) {
            if (fleetColor.name().equalsIgnoreCase(colorString)) {
                result = fleetColor.getColor();
                break;
            }
        }
        if (result == null) {
            result = AbstractColor.getColorByLowercaseNameStatic(colorString.toLowerCase());
        }
        return result;
    }

    public void setCompetitors(Set<RegattaResults> results, String boatClassName) {
        for (RegattaResults result : results) {
            BoatClass boatClass = null;
            boatClass = getBoatClass(boatClassName);
            Iterable<Object> personOrBoatOrTeam = result.getPersonOrBoatOrTeam();
            setBoatsAndTeamsForPerson(personOrBoatOrTeam);
            for (Object obj : personOrBoatOrTeam) {
                if (obj instanceof Person) {
                    Person person = (Person) obj;
                    String idAsString = person.getPersonID();
                    String name = person.getGivenName() + " " + person.getFamilyName();
                    String shortName = null; // Can we get a short name from Manage2Sail?
                    Color color = null;
                    String email = null;
                    URI flagImage = null;
                    Nationality nationality = (person.getNOC() == null) ? null : getNationality(person.getNOC()
                            .toString());
                    BoatAndTeam boatAndTeam = getBoatAndTeam(idAsString, name, nationality, boatClass);
                    this.baseDomainFactory.convertToCompetitorDTO(this.baseDomainFactory.getOrCreateCompetitor(
                            UUID.fromString(idAsString), name, shortName, color, email, flagImage, boatAndTeam.getTeam(),
                            /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null, /* storePersistently */ true));
                } else {
                    break;
                }
            }
        }
    }

    private BoatClass getBoatClass(String boatClassName) {
        BoatClass boatClass;
        if (boatClassName != null && !boatClassName.equals("")) {
            boatClass = baseDomainFactory.getOrCreateBoatClass(boatClassName);
        } else {
            boatClass = baseDomainFactory.getOrCreateBoatClass("default");
        }
        return boatClass;
    }

    private Nationality getNationality(String country) {
        return baseDomainFactory.getOrCreateNationality(country);
    }

    private void setBoatsAndTeamsForPerson(Iterable<Object> personOrBoatOrTeam) {
        boatForPerson = new LinkedHashMap<String, Boat>();
        LinkedHashMap<String, Team> teamForBoat = new LinkedHashMap<String, Team>();
        for (Object obj : personOrBoatOrTeam) {
            if (obj instanceof Team) {
                teamForBoat.put(((Team) obj).getBoatID(), (Team) obj);
            }
        }
        for (Object obj : personOrBoatOrTeam) {
            if (obj instanceof Boat) {
                Team team = teamForBoat.get(((Boat) obj).getBoatID());
                Iterable<Crew> crew = team.getCrew();
                for (Crew person : crew) {
                    boatForPerson.put(person.getPersonID(), (Boat) obj);
                }
            }
        }
    }

    private BoatAndTeam getBoatAndTeam(String idAsString, String name, Nationality nationality, BoatClass boatClass) {
        DynamicBoat boat = createBoat(name, boatForPerson.get(idAsString), boatClass);
        if (nationality == null) {
            nationality = createNationalityFromSailID(boat.getSailID());
        }
        DynamicTeam team = createTeam(name, nationality);
        return new BoatAndTeam(boat, team);
    }

    private Nationality createNationalityFromSailID(String sailID) {
        String country = "";
        if (sailID.length() >= 3) {
            for (int i = 0; i < 3; i++) {
                country += sailID.charAt(i);
            }
            return getNationality(country);
        }
        return null;
    }

    private DynamicBoat createBoat(String name, Boat boat, BoatClass boatClass) {
        DynamicBoat boat1 = new BoatImpl(name + " boat", name + " boat", boatClass, boat.getSailNumber());
        return boat1;
    }

    private DynamicTeam createTeam(String name, Nationality nationality) {
        DynamicPerson sailor = new PersonImpl(name, nationality, null, null);
        DynamicTeam team = new TeamImpl(name + " team", Collections.singleton(sailor), null);
        return team;
    }

    private RegattaResults parseRegattaXML(String url) throws FileNotFoundException, JAXBException, IOException, SAXException, ParserConfigurationException {
        return ParserFactory.INSTANCE.createParser(getInputStream(url), "").parse();
    }

    private InputStream getInputStream(String url) throws FileNotFoundException, IOException {
        URLConnection connection = HttpUrlConnectionHelper.redirectConnection(new URL(url));
        return connection.getInputStream();
    }
}
