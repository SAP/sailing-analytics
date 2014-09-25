package eventimport;

import java.io.IOException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class EventParser {
    
    public EventResults parseEvent(String url){
        
        EventResults eventResults = null;
        
        JSONObject jsonRoot;
        try {
            InputStreamReader streamReader = getStreamReader(url);
            
            jsonRoot = (JSONObject) new JSONParser().parse(streamReader);
            String id = (String) jsonRoot.get("Id");
            String name = (String) jsonRoot.get("Name");
            String xrrUrl = (String) jsonRoot.get("XrrUrl");
            eventResults = new EventResults(id, name, xrrUrl);
            
            JSONArray jsonRegattas = (JSONArray) jsonRoot.get("Regattas");
            for (Object regattaObject: jsonRegattas) {
                RegattaJSON regatta = new RegattaJSON();
                JSONObject jsonRegatta = (JSONObject) regattaObject;
                regatta.setId((String) jsonRegatta.get("Id"));
                regatta.setName((String) jsonRegatta.get("Name"));
                regatta.setBoatClass((String) jsonRegatta.get("ClassName"));
                regatta.setGender((String) jsonRegatta.get("Gender"));
                regatta.setXrrEntriesUrl((String) jsonRegatta.get("XrrEntriesUrl"));
                regatta.setXrrPreliminaryUrl((String) jsonRegatta.get("XrrPreliminaryUrl"));
                regatta.setXrrFinalUrl((String) jsonRegatta.get("XrrFinalUrl"));
                regatta.setHtmlUrl((String) jsonRegatta.get("HtmlUrl"));
                
                eventResults.addRegatta(regatta);
            
            }
            
            streamReader.close();
            
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ParseException e) {
            e.printStackTrace();
        }
        
        return eventResults;
    }
    
    private InputStreamReader getStreamReader(String url) throws UnsupportedEncodingException{
        
        InputStream is = null;
        
        URLConnection connection = null;
        try {
            connection = new URL(url).openConnection();
            is = connection.getInputStream();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        return new InputStreamReader(is, "UTF-8");
        
    }
}
