package com.sap.sailing.server.gateway.impl;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;

import com.sap.sailing.domain.base.EventData;
import com.sap.sailing.server.gateway.AbstractJsonHttpServlet;
import com.sap.sailing.server.gateway.serialization.JsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.CourseAreaJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.EventDataJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.VenueJsonSerializer;

public class EventsJsonExportServlet extends AbstractJsonHttpServlet {
    private static final long serialVersionUID = -7313766879733880267L;

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        JsonSerializer<EventData> eventSerializer = new EventDataJsonSerializer(new VenueJsonSerializer(new CourseAreaJsonSerializer()));
        JSONArray result = new JSONArray();
        for (EventData event : getService().getAllEvents()) {
            result.add(eventSerializer.serialize(event));
        }
        setJsonResponseHeader(response);
        result.writeJSONString(response.getWriter());
    }
}
