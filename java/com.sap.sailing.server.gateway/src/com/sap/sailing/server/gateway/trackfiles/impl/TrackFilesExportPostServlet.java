package com.sap.sailing.server.gateway.trackfiles.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipOutputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.trackfiles.TrackFilesDataSource;
import com.sap.sailing.domain.common.trackfiles.TrackFilesExportParameters;
import com.sap.sailing.domain.common.trackfiles.TrackFilesFormat;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.server.gateway.SailingServerHttpServlet;
import com.sap.sailing.server.trackfiles.Export;

public class TrackFilesExportPostServlet extends SailingServerHttpServlet {
    private static final long serialVersionUID = 1120226743039934620L;
    private static final Logger log = Logger.getLogger(TrackFilesExportPostServlet.class.toString());

    private TrackedRace getTrackedRace(String regattaString, String raceString) {
        Regatta regatta = getService().getRegattaByName(regattaString);
        if (regatta == null)
            return null;
        RaceDefinition race = regatta.getRaceByName(raceString);
        if (race == null)
            return null;

        return getService().getTrackedRace(regatta, race);
    }

    private List<TrackedRace> getTrackedRaces(String[] regattaRaces) {
        List<TrackedRace> races = new ArrayList<TrackedRace>();

        for (String regattaRace : regattaRaces) {
            String[] split = regattaRace.split(":");
            if (split.length == 0)
                continue;
            String regattaString = split[0];
            String raceString = split[1];
            TrackedRace trackedRace = getTrackedRace(regattaString, raceString);
            if (trackedRace == null)
                continue;

            races.add(trackedRace);
        }

        return races;
    }

    private boolean isParamValid(HttpServletResponse resp, Object param, String name) throws IOException {
        if (param == null) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "POST Parameter not passed: " + name);
            return false;
        }
        return true;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        // TODO better error handling: check if whole file is generated, then start outputting to client

        String[] regattaRaces = req.getParameterValues(TrackFilesExportParameters.REGATTARACES);
        String formatString = req.getParameter(TrackFilesExportParameters.FORMAT);
        String[] dataString = req.getParameterValues(TrackFilesExportParameters.DATA);
        String beforeAfterString = req.getParameter(TrackFilesExportParameters.BEFORE_AFTER);
        String rawFixesString = req.getParameter(TrackFilesExportParameters.RAW_FIXES);

        if (!(isParamValid(resp, regattaRaces, TrackFilesExportParameters.REGATTARACES)
                && isParamValid(resp, formatString, TrackFilesExportParameters.FORMAT) && isParamValid(resp,
                    dataString, TrackFilesExportParameters.DATA))) {
            return;
        }
        resp.setHeader("Content-Disposition", "attachment; filename=\"tracked-races.zip\"");
        resp.setContentType("application/zip");

        ZipOutputStream out = new ZipOutputStream(resp.getOutputStream());
        List<TrackedRace> trackedRaces = getTrackedRaces(regattaRaces);
        boolean beforeAfter = beforeAfterString == null ? false : true;
        boolean rawFixes = rawFixesString == null ? false : true;
        TrackFilesFormat format = TrackFilesFormat.valueOf(formatString);
        List<TrackFilesDataSource> data = new ArrayList<TrackFilesDataSource>();
        for (String s : dataString) {
            data.add(TrackFilesDataSource.valueOf(s));
        }
        try {
            Export.INSTANCE.writeAllData(data, format, trackedRaces, beforeAfter, rawFixes, out);
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            log.log(Level.WARNING, e.getMessage());
        }

        out.flush();
        out.close();
    }
}
