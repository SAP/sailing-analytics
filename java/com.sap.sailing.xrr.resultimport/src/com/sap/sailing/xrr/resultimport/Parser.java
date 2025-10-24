package com.sap.sailing.xrr.resultimport;

import java.util.Optional;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.sap.sailing.domain.common.RegattaScoreCorrections;
import com.sap.sailing.domain.common.ScoreCorrectionProvider;
import com.sap.sailing.xrr.schema.Boat;
import com.sap.sailing.xrr.schema.Division;
import com.sap.sailing.xrr.schema.Person;
import com.sap.sailing.xrr.schema.Race;
import com.sap.sailing.xrr.schema.RegattaResults;
import com.sap.sailing.xrr.schema.Team;

public interface Parser {
    RegattaResults parse() throws JAXBException, SAXException, ParserConfigurationException;
    
    String getBoatClassName(Division division); 
    
    Boat getBoat(String boatID);
    
    Team getTeam(String teamID);
    
    Person getPerson(String personID);

    Race getRace(String raceID);
    
    /**
     * Turns the parsing results delivered by {@link #parse()} into a {@link RegattaScoreCorrections} object by
     * traversing the {@link RegattaResults} object, looking for events and divisions and optionally requiring the event
     * name to match {@code eventNameFilter} and/or the boat class name to match the {@code boatClassNameFilter}. If no
     * such result is found, {@code null} is returned.
     */
    RegattaScoreCorrections getRegattaScoreCorrections(RegattaResults regattaResults,
            ScoreCorrectionProvider scoreCorrectionProvider, Optional<String> eventNameFilter,
            Optional<String> boatClassNameFilter);
}
