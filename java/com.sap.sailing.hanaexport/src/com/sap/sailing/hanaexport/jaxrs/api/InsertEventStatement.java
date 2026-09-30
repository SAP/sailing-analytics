package com.sap.sailing.hanaexport.jaxrs.api;

import java.sql.Connection;
import java.sql.Date;
import java.sql.SQLException;

import com.sap.sailing.domain.base.Event;
import com.sap.sse.common.Position;

public class InsertEventStatement extends AbstractPreparedInsertStatement<Event> {
    protected InsertEventStatement(Connection connection) throws SQLException {
        super(connection.prepareStatement(
                "INSERT INTO SAILING.\"Event\" (\"id\", \"name\", \"startDate\", \"endDate\", \"venue\", \"isListed\", \"description\", \"location\") "+
                "VALUES (?, ?, ?, ?, ?, ?, ?, new ST_GEOMETRY('POINT('|| ? || ' ' || ? ||')', 4326).ST_Transform(3857));"));
    }

    @Override
    public void parameterizeStatement(Event event) throws SQLException {
        getPreparedStatement().setString(1, event.getId().toString());
        getPreparedStatement().setString(2, event.getName());
        getPreparedStatement().setDate(3, event.getStartDate()==null?null:new Date(event.getStartDate().asMillis()));
        getPreparedStatement().setDate(4, event.getEndDate()==null?null:new Date(event.getEndDate().asMillis()));
        getPreparedStatement().setString(5, event.getVenue().getName());
        getPreparedStatement().setBoolean(6, event.isPublic());
        getPreparedStatement().setString(7, event.getDescription());
        final Position location = event.getLocation();
        getPreparedStatement().setString(8, location != null ? String.format("%1.8f", location.getLngDeg()) : null);
        getPreparedStatement().setString(9, location != null ? String.format("%1.8f", location.getLatDeg()) : null);
    }
}
