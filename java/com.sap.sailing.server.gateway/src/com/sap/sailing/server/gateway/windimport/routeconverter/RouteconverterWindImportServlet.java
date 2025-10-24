package com.sap.sailing.server.gateway.windimport.routeconverter;

import com.sap.sailing.server.gateway.windimport.AbstractWindImportServlet;

public class RouteconverterWindImportServlet extends AbstractWindImportServlet {
    private static final long serialVersionUID = -4547876638456305135L;
    
    public RouteconverterWindImportServlet() {
        super(new RouteconverterWindImporter());
    }
}
