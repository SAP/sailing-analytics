package com.sap.sailing.xrr.resultimport.impl;

import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.sap.sailing.domain.common.BoatClassMasterdata;
import com.sap.sailing.domain.common.RegattaScoreCorrections;
import com.sap.sailing.domain.common.ScoreCorrectionProvider;
import com.sap.sailing.xrr.resultimport.Parser;
import com.sap.sailing.xrr.schema.Boat;
import com.sap.sailing.xrr.schema.Division;
import com.sap.sailing.xrr.schema.Event;
import com.sap.sailing.xrr.schema.EventGender;
import com.sap.sailing.xrr.schema.Person;
import com.sap.sailing.xrr.schema.Race;
import com.sap.sailing.xrr.schema.RegattaResults;
import com.sap.sailing.xrr.schema.TRResult;
import com.sap.sailing.xrr.schema.Team;
import com.sap.sse.util.XmlUtil;


public class ParserImpl implements Parser {
    private final Map<String, Person> personByID;
    private final Map<String, Team> teamByID;
    private final Map<String, Boat> boatByID;
    private final Map<String, Race> raceByID;
    private final InputStream inputStream;
    private final String name;
    
    public ParserImpl(InputStream inputStream, String name) {
        super();
        this.personByID = new HashMap<>();
        this.teamByID = new HashMap<>();
        this.boatByID = new HashMap<>();
        this.raceByID = new HashMap<>();
        this.inputStream = inputStream;
        this.name = name;
    }

    @Override
    public RegattaResults parse() throws JAXBException, SAXException, ParserConfigurationException {
        javax.xml.transform.Source xmlSource = XmlUtil.getXmlSourceForInputStream(inputStream);
        JAXBContext jc = JAXBContext.newInstance(TRResult.class.getPackage().getName(), ParserImpl.class.getClassLoader());
        Unmarshaller um = jc.createUnmarshaller();
        @SuppressWarnings("unchecked")
        RegattaResults regattaResults = ((JAXBElement<RegattaResults>) um.unmarshal(xmlSource)).getValue();
        for (Object o : regattaResults.getPersonOrBoatOrTeam()) {
            if (o instanceof Person) {
                Person person = (Person) o;
                personByID.put(person.getPersonID(), person);
            } else if (o instanceof Boat) {
                Boat boat = (Boat) o;
                boatByID.put(boat.getBoatID(), boat);
            } else if (o instanceof Team) {
                Team team = (Team) o;
                teamByID.put(team.getTeamID(), team);
            } else if (o instanceof Event) {
                Event event = (Event) o;
                for (Object o2 : event.getRaceOrDivisionOrRegattaSeriesResult()) {
                    if (o2 instanceof Race) {
                        Race race = (Race) o2;
                        raceByID.put(race.getRaceID(), race);
                    }
                }
            }
        }
        return regattaResults;
    }

    @Override
    public String getBoatClassName(Division division) {
        return XRRParserUtil.getBoatClassName(division);
    }

    @Override
    public Boat getBoat(String boatID) {
        return boatByID.get(boatID);
    }

    @Override
    public Team getTeam(String teamID) {
        return teamByID.get(teamID);
    }

    @Override
    public Person getPerson(String personID) {
        return personByID.get(personID);
    }
    
    @Override
    public Race getRace(String raceID) {
        return raceByID.get(raceID);
    }
    
    @Override
    public String toString() {
        return name==null?"":name;
    }

    @Override
    public RegattaScoreCorrections getRegattaScoreCorrections(RegattaResults regattaResults,
            ScoreCorrectionProvider scoreCorrectionProvider, Optional<String> eventNameFilter,
            Optional<String> boatClassNameFilter) {
        for (Object o : regattaResults.getPersonOrBoatOrTeam()) {
            if (o instanceof Event) {
                final Event event = (Event) o;
                if (!eventNameFilter.isPresent() || event.getTitle().equals(eventNameFilter.get())) {
                    for (Object eventO : event.getRaceOrDivisionOrRegattaSeriesResult()) {
                        if (eventO instanceof Division) {
                            final Division division = (Division) eventO;
                            final EventGender divisionGender = division.getGender();
                            final String divisionBoatClass = getBoatClassName(division);
                            final String divisionBoatClassAndGender;
                            if (divisionGender != null) {
                                divisionBoatClassAndGender = divisionBoatClass + ", " + divisionGender.name();  
                            } else {
                                divisionBoatClassAndGender = divisionBoatClass;
                            }
                            final String unifiedBoatClassNameFilter = boatClassNameFilter.map(n->BoatClassMasterdata.unifyBoatClassNameBasedOnExistingMasterdata(n)).orElse(null);
                            if (unifiedBoatClassNameFilter == null
                                    || unifiedBoatClassNameFilter.equals(BoatClassMasterdata.unifyBoatClassNameBasedOnExistingMasterdata(divisionBoatClass))
                                    || unifiedBoatClassNameFilter.equals(BoatClassMasterdata.unifyBoatClassNameBasedOnExistingMasterdata(divisionBoatClassAndGender))
                                    || unifiedBoatClassNameFilter.contains(BoatClassMasterdata.unifyBoatClassNameBasedOnExistingMasterdata(divisionBoatClassAndGender))) {
                                return new XRRRegattaResultsAsScoreCorrections(event, division, scoreCorrectionProvider, this);
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
    
}
