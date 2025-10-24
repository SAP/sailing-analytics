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

import org.apache.shiro.SecurityUtils;

import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.security.SecuredDomainType.TrackedRaceActions;
import com.sap.sailing.domain.common.trackfiles.TrackFilesDataSource;
import com.sap.sailing.domain.common.trackfiles.TrackFilesExportParameters;
import com.sap.sailing.domain.common.trackfiles.TrackFilesFormat;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.server.gateway.SailingServerHttpServlet;
import com.sap.sailing.server.trackfiles.TrackFileExporter;

public class TrackFilesExportPostServlet extends SailingServerHttpServlet {
    private static final long serialVersionUID = 1120226743039934620L;
    private static final Logger log = Logger.getLogger(TrackFilesExportPostServlet.class.toString());

    private TrackedRace getTrackedRace(String regattaString, String raceString) {
        final TrackedRace result;
        final Regatta regatta = getService().getRegattaByName(regattaString);
        if (regatta == null) {
            result = null;
        } else {
            final RaceDefinition race = regatta.getRaceByName(raceString);
            if (race == null) {
                result = null;
            } else {
                result = getService().getTrackedRace(regatta, race);
            }
        }
        return result;   
    }

    private List<TrackedRace> getTrackedRaces(String[] regattaRaces) {
        final List<TrackedRace> races = new ArrayList<TrackedRace>();
        for (String regattaRace : regattaRaces) {
            final String[] split = regattaRace.split(":");
            if (split.length != 0) {
                final String regattaString = split[0];
                final String raceString = split[1];
                final TrackedRace trackedRace = getTrackedRace(regattaString, raceString);
                if (trackedRace != null) {
                    races.add(trackedRace);
                }
            }
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
        List<TrackedRace> trackedRaces = getTrackedRaces(regattaRaces);
        for (TrackedRace trackedRace : trackedRaces) {
            SecurityUtils.getSubject()
                    .checkPermission(trackedRace.getIdentifier().getStringPermission(TrackedRaceActions.EXPORT));
        }
        ZipOutputStream out = new ZipOutputStream(resp.getOutputStream());
        boolean beforeAfter = beforeAfterString == null ? false : true;
        boolean rawFixes = rawFixesString == null ? false : true;
        TrackFilesFormat format = TrackFilesFormat.valueOf(formatString);
        List<TrackFilesDataSource> data = new ArrayList<TrackFilesDataSource>();
        for (String s : dataString) {
            data.add(TrackFilesDataSource.valueOf(s));
        }
        try {
            TrackFileExporter.INSTANCE.writeAllData(data, format, trackedRaces, beforeAfter, rawFixes, out);
        } catch (Exception e) {
            resp.sendError(HttpServletResponse.SC_BAD_REQUEST, "Error trying to export data; see server logs for details.");
            log.log(Level.WARNING, e.getMessage());
        }
        out.flush();
        out.close();
    }
}
