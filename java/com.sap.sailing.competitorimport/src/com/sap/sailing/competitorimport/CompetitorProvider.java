package com.sap.sailing.competitorimport;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.sap.sailing.domain.common.CompetitorDescriptor;
import com.sap.sse.common.Named;

/**
 * Competitor data may come from various sources, such as external regatta management systems (RMS),
 * CSV files, Excel spreadsheets and the like. When creating a regatta that uses "RaceLog Tracking"
 * (also known as Smartphone Tracking)---and be it only in order to manage results for the regatta
 * with the race committee app---it is tedious having to enter all competitor data manually when
 * that data is already available in electronic form.<p>
 * 
 * In its simplest form, a provider will just produce an otherwise unstructured list of competitor
 * entries with no hint as to the event, regatta or race that the competitor pertains to. More
 * sophisticated providers may structure competitor data into events, regattas or even individual
 * races so that importing these competitors into a single fleet of one race column becomes possible.
 * This will be particularly useful for split-fleet racing.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface CompetitorProvider extends Named {
    /**
     * @return keys are event names, values are the names of the regatta in the event for which the connector has
     *         competitor names. Should the value for a key be <code>null</code>, competitors for the event may still
     *         be available, only they may not be keyed per regatta in that case.
     */
    Map<String, Set<String>> getHasCompetitorsForRegattasInEvent() throws IOException, URISyntaxException, SAXException, ParserConfigurationException;

    /**
     * Obtains competitor records from the source of import
     * 
     * @param eventName
     *            an event name as obtained as a key in {@link #getHasCompetitorsForRegattasInEvent()}'s result
     * @param regattaName
     *            <code>null</code> to get the competitors for all regattas in the event specified by
     *            <code>eventName</code>, or a regatta name as provided in the value set for the key
     *            <code>eventName</code> as returned by {@link #getHasCompetitorsForRegattasInEvent()}.
     */
    Iterable<CompetitorDescriptor> getCompetitorDescriptors(String eventName, String regattaName) throws JAXBException, IOException, URISyntaxException, SAXException, ParserConfigurationException;

    /**
     * A hint for users about this competitor provider; ideally mapped to the user's locale with i18n support.
     * 
     * @return may return {@code null}
     */
    String getHint(Locale locale);
}
