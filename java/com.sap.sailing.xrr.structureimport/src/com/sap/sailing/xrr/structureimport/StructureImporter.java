package com.sap.sailing.xrr.structureimport;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import javax.xml.bind.JAXBException;

import buildstructure.BuildStructure;
import buildstructure.Fleet;
import buildstructure.RegattaStructure;
import buildstructure.Series;
import buildstructure.SetRacenumberStrategy;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Nationality;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.DynamicBoat;
import com.sap.sailing.domain.base.impl.DynamicPerson;
import com.sap.sailing.domain.base.impl.DynamicTeam;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.common.Color;
import com.sap.sailing.domain.common.FleetColors;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.SeriesCreationParametersDTO;
import com.sap.sailing.xrr.resultimport.ParserFactory;
import com.sap.sailing.xrr.schema.Boat;
import com.sap.sailing.xrr.schema.Crew;
import com.sap.sailing.xrr.schema.Division;
import com.sap.sailing.xrr.schema.Event;
import com.sap.sailing.xrr.schema.Person;
import com.sap.sailing.xrr.schema.Race;
import com.sap.sailing.xrr.schema.RegattaResults;
import com.sap.sailing.xrr.schema.Team;

import eventimport.EventImport;
import eventimport.RegattaJSON;

public class StructureImporter {

    private static final Logger logger = Logger.getLogger(StructureImporter.class.getName());
    private LinkedHashMap<String, Boat> boatForPerson;
    private final DomainFactory baseDomainFactory;
    private final SetRacenumberStrategy setRacenumberStrategy;

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
        Iterable<RegattaResults> parsedRegattas = parseRegattas(regattas);
        Set<Regatta> addSpecificRegattas = new HashSet<Regatta>();
        for (RegattaResults result : parsedRegattas) {
            Set<Race> races = new HashSet<Race>();
            // assuming that the last element in getPersonOrBoatOrTeam is an event
            Event event = (Event) result.getPersonOrBoatOrTeam().get(result.getPersonOrBoatOrTeam().size() - 1);
            Iterable<Object> raceOrDivisionOrRegattaSeriesResults = event.getRaceOrDivisionOrRegattaSeriesResult();
            for (Object raceOrDivisionOrRegattaSeriesResult : raceOrDivisionOrRegattaSeriesResults) {
                if (raceOrDivisionOrRegattaSeriesResult instanceof Race) {
                    races.add((Race) raceOrDivisionOrRegattaSeriesResult);
                }
            }
            BuildStructure buildStructure = new BuildStructure(races);
            RegattaImpl regatta = new RegattaImpl(RegattaImpl.getDefaultName(event.getTitle(),
                    ((Division) event.getRaceOrDivisionOrRegattaSeriesResult().get(0)).getTitle()), new BoatClassImpl(((Division) event
                    .getRaceOrDivisionOrRegattaSeriesResult().get(0)).getTitle(),true), setSeries(buildStructure), false, this.baseDomainFactory
                            .createScoringScheme(ScoringSchemeType.LOW_POINT), event.getEventID(), null);
            addSpecificRegattas.add(regatta);
        }
        return addSpecificRegattas;
    }

    private Iterable<RegattaResults> parseRegattas(final Iterable<RegattaJSON> selectedRegattas) {
        final Set<RegattaResults> result = Collections.synchronizedSet(new HashSet<RegattaResults>());
        Set<Thread> threads = new HashSet<Thread>();
        for (final RegattaJSON selectedRegatta : selectedRegattas) {
            Thread thread = new Thread("XRR Importer " + selectedRegatta.getName()) {
                @Override
                public void run() {
                    try {
                        result.add(parseRegattaXML(selectedRegatta.getXrrEntriesUrl()));
                    } catch (JAXBException | IOException e) {
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
    
    private Iterable<SeriesImpl> setSeries(BuildStructure structure) {
        Set<SeriesImpl> series = new HashSet<SeriesImpl>(); 
        RegattaStructure regattaStructure = structure.getRegattaStructure();
        if (regattaStructure != null) {
            int index = 0;
            for (Series raceType : regattaStructure.getSeries()) {
                List<com.sap.sailing.domain.base.Fleet> fleets = getFleets(raceType.getFleets());
                setRaceNames(index, raceType, raceType.getFleets());
                series.add(new SeriesImpl(raceType.getSeries(), raceType.isMedal(), fleets, raceType.getRaceNames(), null));
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
        String fleetColor = "";
        if (fleets.size() <= 1) {
            fleetColor = "Default";
            FleetImpl fleetImpl = new FleetImpl(fleetColor, 0, getColorFromString(fleetColor));
            fleetsImpl.add(fleetImpl);
        } else {
            int index = 0;
            for (Fleet fleet : fleets) {
                fleetColor = fleet.getColor();
                FleetImpl fleetImpl = new FleetImpl(fleetColor, index, getColorFromString(fleetColor));
                fleetsImpl.add(fleetImpl);
                index++;
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
            result = Color.BLACK;
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
                    Color color = null;
                    Nationality nationality = (person.getNOC() == null) ? null : getNationality(person.getNOC()
                            .toString());
                    BoatAndTeam boatAndTeam = getBoatAndTeam(idAsString, name, nationality, boatClass);
                    this.baseDomainFactory.convertToCompetitorDTO(this.baseDomainFactory.getOrCreateCompetitor(
                            UUID.fromString(idAsString), name, color, boatAndTeam.getTeam(), boatAndTeam.getBoat()));
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
        DynamicBoat boat1 = new BoatImpl(name + " boat", boatClass, boat.getSailNumber());
        return boat1;
    }

    private DynamicTeam createTeam(String name, Nationality nationality) {
        DynamicPerson sailor = new PersonImpl(name, nationality, null, null);
        DynamicTeam team = new TeamImpl(name + " team", Collections.singleton(sailor), null);
        return team;
    }

    private RegattaResults parseRegattaXML(String url) throws FileNotFoundException, JAXBException, IOException {
        return ParserFactory.INSTANCE.createParser(getInputStream(url), "").parse();
    }

    private InputStream getInputStream(String url) throws FileNotFoundException, IOException {
        URLConnection connection = new URL(url).openConnection();
        return connection.getInputStream();
    }
}
