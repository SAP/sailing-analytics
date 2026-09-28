package com.sap.sailing.gwt.ui.server;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.management.ManagementFactory;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.MediaType;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.mongodb.connection.ClusterDescription;
import com.mongodb.connection.ServerDescription;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sse.ServerInfo;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Util;
import com.sap.sse.mongodb.MongoDBService;
import com.sap.sse.replication.ReplicationService;
import com.sap.sse.replication.ReplicationStatus;
import com.sap.sse.util.ThreadPoolUtil;

public class StatusServlet extends HttpServlet {
    private static final String WAIT_UNTIL_RACES_LOADED = "waitUntilRacesLoaded";
    private static final long serialVersionUID = -8896724182560416457L;

    protected <T> T getService(Class<T> clazz) {
        BundleContext context = Activator.getDefault();
        ServiceTracker<T, T> tracker = new ServiceTracker<T, T>(context, clazz, null);
        tracker.open();
        T service = tracker.getService();
        tracker.close();
        return service;
    }

    private RacingEventService getService(ServletContext servletContext) {
        return getService(RacingEventService.class);
    }
    
    private ReplicationService getReplicationService(ServletContext servletContext) {
        return getService(ReplicationService.class);
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        final ServletContext servletContext = req.getServletContext();
        final JSONObject result = new JSONObject();
        final RacingEventService service = getService(servletContext);
        final String waitUntilRacesLoadedString = req.getParameter(WAIT_UNTIL_RACES_LOADED);
        boolean waitUntilRacesLoaded = Boolean.valueOf(waitUntilRacesLoadedString);
        result.put("servername", ServerInfo.getName());
        result.put("serverdirectory", ServerInfo.getServerDirectory().getAbsolutePath());
        result.put("buildversion", ServerInfo.getBuildVersion());
        try {
            final JSONObject versionAsJson = ServerInfo.getBuildVersionJson();
            result.putAll(versionAsJson);
            final ScheduledExecutorService defaultBackgroundTaskThreadPoolExecutor = ThreadPoolUtil.INSTANCE.getDefaultBackgroundTaskThreadPoolExecutor();
            if (defaultBackgroundTaskThreadPoolExecutor instanceof ThreadPoolExecutor) {
                final long queueLengthDefaultBackgroundThreadPoolExecutor = ((ThreadPoolExecutor) defaultBackgroundTaskThreadPoolExecutor).getQueue().size();
                result.put("defaultbackgroundthreadpoolexecutorqueuelength", queueLengthDefaultBackgroundThreadPoolExecutor);
                final long nonDelayedQueueLengthDefaultBackgroundThreadPoolExecutor = Util.size(ThreadPoolUtil.INSTANCE.getTasksDelayedByLessThan((ThreadPoolExecutor) defaultBackgroundTaskThreadPoolExecutor, Duration.ONE_SECOND));
                result.put("defaultbackgroundthreadpoolexecutorqueuelengthnondelayed", nonDelayedQueueLengthDefaultBackgroundThreadPoolExecutor);
            }
            final ScheduledExecutorService defaultForegroundTaskThreadPoolExecutor = ThreadPoolUtil.INSTANCE.getDefaultForegroundTaskThreadPoolExecutor();
            if (defaultForegroundTaskThreadPoolExecutor instanceof ThreadPoolExecutor) {
                final long queueLengthDefaultForegroundThreadPoolExecutor = ((ThreadPoolExecutor) defaultForegroundTaskThreadPoolExecutor).getQueue().size();
                result.put("defaultforegroundthreadpoolexecutorqueuelength", queueLengthDefaultForegroundThreadPoolExecutor);
                final long nonDelayedQueueLengthDefaultForegroundThreadPoolExecutor = Util.size(ThreadPoolUtil.INSTANCE.getTasksDelayedByLessThan((ThreadPoolExecutor) defaultForegroundTaskThreadPoolExecutor, Duration.ONE_SECOND));
                result.put("defaultforegroundthreadpoolexecutorqueuelengthnondelayed", nonDelayedQueueLengthDefaultForegroundThreadPoolExecutor);
            }
            final double systemLoadAverageLastMinute = ManagementFactory.getOperatingSystemMXBean().getSystemLoadAverage();
            result.put("systemloadaveragelastminute", systemLoadAverageLastMinute);
            final long numberOfTrackedRacesToRestore = service.getNumberOfTrackedRacesToRestore();
            result.put("numberofracestorestore", numberOfTrackedRacesToRestore);
            final int numberOfTrackedRacesRestored = service.getNumberOfTrackedRacesRestored();
            result.put("numberofracesrestored", numberOfTrackedRacesRestored);
            final int numberOfTrackedRacesRestoredDoneLoading = service.getNumberOfTrackedRacesRestoredDoneLoading();
            result.put("numberofracesrestoreddoneloading", numberOfTrackedRacesRestoredDoneLoading);
            final int numberOfTrackedRacesStillLoading = service.getNumberOfTrackedRacesStillLoading();
            result.put("numberofracesstillloading", numberOfTrackedRacesStillLoading);
            result.put("mongoDbConfiguration", getMongoDBReplicaSetNodes());
            final ReplicationService replicationService = getReplicationService(servletContext);
            final ReplicationStatus replicationStatus = replicationService == null ? null : replicationService.getStatus();
            if (replicationStatus != null) {
                result.put("replication", replicationStatus.toJSONObject());
            }
            boolean available = numberOfTrackedRacesRestored >= numberOfTrackedRacesToRestore
                    && (replicationStatus == null || replicationStatus.isAvailable());
            if (waitUntilRacesLoaded) {
                // interestingly, we sometimes see servers where numberOfTrackedRacesRestoredDoneLoading > numberOfTrackedRacesToRestore...
                available = available && numberOfTrackedRacesRestoredDoneLoading >= numberOfTrackedRacesToRestore;
            }
            result.put("available", available);
            resp.setStatus(available ? HttpServletResponse.SC_OK : HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            resp.setContentType(MediaType.APPLICATION_JSON + ";charset=UTF-8");
            OutputStreamWriter out = new OutputStreamWriter(resp.getOutputStream());
            result.writeJSONString(out);
            out.close();
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }
    }

    private JSONObject getMongoDBReplicaSetNodes() {
        final JSONObject result = new JSONObject();
        final ClusterDescription clusterDescription = MongoDBService.INSTANCE.getMongoClient().getClusterDescription();
        result.put("connectionMode", clusterDescription.getConnectionMode().name());
        result.put("replicaSet", clusterDescription.getClusterSettings().getRequiredReplicaSetName());
        result.put("database", MongoDBService.INSTANCE.getMongoClientURI().getDatabase());
        final JSONArray servers = new JSONArray();
        for (final ServerDescription serverDescription : clusterDescription.getServerDescriptions()) {
            final JSONObject serverHostAndPort = new JSONObject();
            serverHostAndPort.put("host", serverDescription.getAddress().getHost());
            serverHostAndPort.put("port", serverDescription.getAddress().getPort());
            serverHostAndPort.put("type", serverDescription.getType().name());
            servers.add(serverHostAndPort);
        }
        result.put("servers", servers);
        return result;
    }
}
