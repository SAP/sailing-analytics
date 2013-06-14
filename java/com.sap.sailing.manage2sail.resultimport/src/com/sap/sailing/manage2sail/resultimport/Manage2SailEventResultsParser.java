package com.sap.sailing.manage2sail.resultimport;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sap.sailing.util.DateParser;
import com.sap.sailing.util.InvalidDateException;

public class Manage2SailEventResultsParser {

    /**
     * @param is closed before the method returns, also in case of exception
     * @throws IOException 
     */
    public EventResultDescriptor getEventResult(InputStream is) throws IOException {
        EventResultDescriptor result = null;
        try {
            JSONObject jsonRoot = (JSONObject) new JSONParser().parse(new InputStreamReader(is));
            result = new EventResultDescriptor();
            result.setId((String) jsonRoot.get("Id"));
            result.setIsafId((String) jsonRoot.get("IsafId"));
            result.setName((String) jsonRoot.get("Name"));
            result.setXrrUrl(parseURL(jsonRoot, "XrrUrl"));
            
            JSONArray jsonRegattas = (JSONArray) jsonRoot.get("Regattas");
            for (Object regattaObject: jsonRegattas) {
                RegattaResultDescriptor regattaResult = new RegattaResultDescriptor(); 
                JSONObject jsonRegatta = (JSONObject) regattaObject;
                regattaResult.setId((String) jsonRegatta.get("Id"));
                regattaResult.setIsafId((String) jsonRegatta.get("IsafId"));
                regattaResult.setExternalId((String) jsonRegatta.get("ExternalId"));
                regattaResult.setName((String) jsonRegatta.get("Name"));
                regattaResult.setClassName((String) jsonRegatta.get("ClassName"));
                regattaResult.setPdfUrl(parseURL(jsonRegatta, "PdfUrl"));
                regattaResult.setXrrPreliminaryUrl(parseURL(jsonRegatta, "XrrPreliminaryUrl"));
                regattaResult.setXrrFinalUrl(parseURL(jsonRegatta, "XrrFinalUrl"));
                regattaResult.setHtmlUrl(parseURL(jsonRegatta, "HtmlUrl"));
                regattaResult.setPublishedAt(parseDate(jsonRegatta, "Published"));
                regattaResult.setIsFinal((Boolean) jsonRegatta.get("Final"));
                result.getRegattaResults().add(regattaResult);
            }
            is.close();
        } catch(ParseException e) {
            e.printStackTrace();
        } finally { 
            is.close();
        }
        return result;
    }
    
    private Date parseDate(JSONObject jsonDate, String attributeName) {
        Date result = null;
        String dateAsString = (String) jsonDate.get(attributeName);
        if(dateAsString != null) {
            try {
                result = DateParser.parseUTC(dateAsString);
            } catch (InvalidDateException e) {
                e.printStackTrace();
            } 
        }
        return result;
    }

    private URL parseURL(JSONObject jsonURL, String attributeName) {
        URL result = null;
        String urlAsString = (String) jsonURL.get(attributeName);
        if(urlAsString != null) {
            try {
                result = new URL(urlAsString);
            } catch (MalformedURLException e) {
                e.printStackTrace();
            }
        }
        return result;
    }
}
