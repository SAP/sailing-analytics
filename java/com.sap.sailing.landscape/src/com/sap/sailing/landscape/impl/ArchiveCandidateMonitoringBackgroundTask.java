package com.sap.sailing.landscape.impl;

import java.net.URL;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.sap.sailing.landscape.LandscapeService;
import com.sap.sailing.landscape.SailingAnalyticsMetrics;
import com.sap.sailing.landscape.SailingAnalyticsProcess;
import com.sap.sailing.server.gateway.interfaces.CompareServersResult;
import com.sap.sailing.server.gateway.interfaces.SailingServer;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Named;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.NamedImpl;
import com.sap.sse.common.mail.MailException;
import com.sap.sse.landscape.Landscape;
import com.sap.sse.landscape.aws.AwsApplicationReplicaSet;
import com.sap.sse.security.shared.impl.User;

/**
 * A stateful monitoring task that can be {@link #run run} to observe an {@code ARCHIVE} candidate process and wait for
 * it to be ready for comparing its contents with a production {@code ARCHIVE} instance. When run for the first time,
 * the task check the {@code /gwt/status} end point on {@code candidateHostname} to see whether it is already serving
 * requests. If not, it will re-schedule itself after some delay to check again until either the candidate becomes
 * healthy or a timeout is reached.
 * <p>
 * 
 * If the candidate was seen serving a {@code /gwt/status} request, this task changes state and now looks at the
 * contents of the status response. Four conditions must be fulfilled for the candidate to be considered ready for
 * comparison:
 * 
 * <ol>
 * <li>the overall status must be {@code available: true}.</li>
 * <li>the one-minute system load average must be below 2 (per cent)</li>
 * <li>the default foreground thread pool queue must contain less than 10 tasks</li>
 * <li>the default background thread pool queue must contain less than 10 tasks</li>
 * <li>the old and new ARCHIVE must compare equal with the {@link SailingServer#compareServers(Optional, SailingServer, Optional)} method</li>
 * </ol>
 * 
 * When any of these conditions is not fulfilled, the task will re-schedule itself after some delay to check again until
 * either the candidate fulfills all conditions or a timeout is reached.
 * <p>
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class ArchiveCandidateMonitoringBackgroundTask implements Runnable {
    private interface Check extends Named {
        boolean runCheck() throws Exception;
        void setLastFailureMessage(String lastFailureMessage);
        boolean hasTimedOut();
        Duration getDelayAfterFailure();
        String getLastFailureMessage();
    }
    
    private abstract class AbstractCheck extends NamedImpl implements Check {
        private static final long serialVersionUID = -8809199091635882129L;
        private final TimePoint creationTime;
        private final Duration timeout;
        private final Duration delayAfterFailure;
        private String lastFailureMessage;

        public AbstractCheck(String name, Duration timeout, Duration delayAfterFailure) {
            super(name);
            this.creationTime = TimePoint.now();
            this.timeout = timeout;
            this.delayAfterFailure = delayAfterFailure;
        }

        @Override
        public boolean hasTimedOut() {
            return creationTime.until(TimePoint.now()).compareTo(timeout) > 0;
        }
        
        @Override
        public Duration getDelayAfterFailure() {
            return delayAfterFailure;
        }

        @Override
        public String getLastFailureMessage() {
            return lastFailureMessage;
        }
        
        @Override
        public void setLastFailureMessage(String lastFailureMessage) {
            this.lastFailureMessage = lastFailureMessage;
        }
    }
    
    private static final Logger logger = Logger.getLogger(ArchiveCandidateMonitoringBackgroundTask.class.getName());

    private final static Duration DELAY_BETWEEN_CHECKS = Duration.ONE_MINUTE.times(5);
    private final static Duration LONG_TIMEOUT = Duration.ONE_DAY.times(3);
    private final static double MAXIMUM_ONE_MINUTE_SYSTEM_LOAD_AVERAGE = 2.0;
    private final static int MAXIMUM_THREAD_POOL_QUEUE_SIZE = 10;
    private final static Optional<Duration> TIMEOUT_FIRST_CONTACT = Optional.of(Landscape.WAIT_FOR_PROCESS_TIMEOUT.get().plus(Landscape.WAIT_FOR_HOST_TIMEOUT.get()));
    private final static Duration SERVER_COMPARISON_TIMEOUT = Duration.ONE_MINUTE.times(10); // good for two or three attempts, usually
    private final static Duration DELAY_BETWEEN_COMPARISON_CHECKS = Duration.ONE_MINUTE;
    
    /**
     * The user on whose behalf the monitoring is performed; this is used for sending notifications about the monitoring
     * result to the user and for performing the monitoring with the same permissions as the user (e.g. when accessing
     * the candidate's REST API)
     */
    private final User currentUser;
    private final String candidateHostname;
    private final LandscapeService landscapeService;
    private final AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet;
    private final URL continuationBaseURL;
    private final ScheduledExecutorService executor;
    
    /**
     * A bearer token expected to authenticate the {@link #currentUser} against the candidate and production ARCHIVE
     * servers
     */
    private final String effectiveBearerToken;
    
    private final Iterable<Check> checks;
    private final Iterator<Check> checksIterator;
    private Check currentCheck;
    
    public ArchiveCandidateMonitoringBackgroundTask(User currentUser, LandscapeService landscapeService,
            AwsApplicationReplicaSet<String, SailingAnalyticsMetrics, SailingAnalyticsProcess<String>> replicaSet,
            String candidateHostname,
            ScheduledExecutorService executor,
            String effectiveBearerToken, URL continuationBaseURL) {
        this.currentUser = currentUser;
        this.landscapeService = landscapeService;
        this.continuationBaseURL = continuationBaseURL;
        this.replicaSet = replicaSet;
        this.candidateHostname = candidateHostname;
        this.executor = executor;
        this.effectiveBearerToken = effectiveBearerToken;
        this.checks = Arrays.asList(
                new IsAlive(),
                new IsReady(),
                new HasLowEnoughSystemLoad(),
                new HasShortEnoughDefaultBackgroundThreadPoolExecutorQueue(),
                new HasShortEnoughDefaultForegroundThreadPoolExecutorQueue(),
                new CompareServersWithRestAPI());
        this.checksIterator = this.checks.iterator();
        this.currentCheck = checksIterator.next();
    }

    @Override
    public void run() {
        try {
            if (currentCheck.runCheck()) {
                logger.info("Check \""+currentCheck+"\" passed.");
                // the check passed; proceed to next check, if any
                currentCheck = checksIterator.hasNext() ? checksIterator.next() : null;
                if (currentCheck != null) {
                    logger.info("More checks to do; re-scheduling to run next check \""+currentCheck+"\"");
                    // re-schedule this task to run next check immediately
                    executor.submit(this);
                } else {
                    // all checks passed; candidate is ready for production; nothing more to do here
                    logger.info("Done with all checks; candidate is ready for production.");
                    notifyProcessOwnerCandidateIsReadyForSpotChecksAndRotation(); // this ends the re-scheduling loop
                }
            } else {
                rescheduleCurrentCheckAfterFailureOrTimeout();
            }
        } catch (Throwable e) {
            logger.warning("Exception while running check \"" + currentCheck + "\" for candidate " + replicaSet.getMaster().getHost().getHostname() + ": " + e.getMessage());
            currentCheck.setLastFailureMessage(e.getMessage()==null?e.getClass().getSimpleName():e.getMessage());
            try {
                rescheduleCurrentCheckAfterFailureOrTimeout();
            } catch (MailException e1) {
                logger.severe("Issue while trying to send mail: "+e.getMessage()+"; user may not know what to do next!");
            }
        }
    }

    private void rescheduleCurrentCheckAfterFailureOrTimeout() throws MailException {
        if (currentCheck.hasTimedOut()) {
            logger.severe("Check \""+currentCheck+"\" failed and has timed out; giving up on candidate "+replicaSet.getMaster().getHost().getHostname());
            notifyProcessOwnerCandidateFailedToBecomeReadyForProduction(); // this ends the re-scheduling loop
        } else {
            logger.info("Check \"" + currentCheck + "\" failed with message \"" + currentCheck.getLastFailureMessage()
                    + "\" but has not yet timed out; re-scheduling to check again after "
                    + currentCheck.getDelayAfterFailure());
            executor.schedule(this, currentCheck.getDelayAfterFailure().asMillis(), TimeUnit.MILLISECONDS);
        }
    }

    private class IsAlive extends AbstractCheck {
        private static final long serialVersionUID = -4265303532881568290L;

        private IsAlive() {
            super("is alive", TIMEOUT_FIRST_CONTACT.get(), DELAY_BETWEEN_CHECKS);
        }

        @Override
        public boolean runCheck() throws Exception {
            final boolean result = replicaSet.getMaster().isAlive(Landscape.WAIT_FOR_PROCESS_TIMEOUT);
            if (!result) {
                setLastFailureMessage("Candidate at "+replicaSet.getMaster().getHost().getPrivateAddress()+" not alive yet");
            }
            return result;
        }
    }

    private class IsReady extends AbstractCheck {
        private static final long serialVersionUID = -4265303532881568290L;

        private IsReady() {
            super("is ready", LONG_TIMEOUT, DELAY_BETWEEN_CHECKS);
        }

        @Override
        public boolean runCheck() throws Exception {
            final boolean result = replicaSet.getMaster().isReady(Landscape.WAIT_FOR_PROCESS_TIMEOUT);
            if (!result) {
                setLastFailureMessage("Candidate at "+replicaSet.getMaster().getHost().getPrivateAddress()+" not ready yet");
            }
            return result;
        }
    }

    private class HasLowEnoughSystemLoad extends AbstractCheck {
        private static final long serialVersionUID = -7931266212387969287L;

        public HasLowEnoughSystemLoad() {
            super("has low enough system load", LONG_TIMEOUT, DELAY_BETWEEN_CHECKS);
        }

        @Override
        public boolean runCheck() throws Exception {
            final double lastMinuteSystemLoadAverage = replicaSet.getMaster().getLastMinuteSystemLoadAverage(Landscape.WAIT_FOR_PROCESS_TIMEOUT);
            final boolean result = lastMinuteSystemLoadAverage < MAXIMUM_ONE_MINUTE_SYSTEM_LOAD_AVERAGE;
            if (!result) {
                setLastFailureMessage("Candidate at " + replicaSet.getMaster().getHost().getPrivateAddress()
                        + " has too high system load average of " + lastMinuteSystemLoadAverage
                        + " which is still above the maximum of " + MAXIMUM_ONE_MINUTE_SYSTEM_LOAD_AVERAGE);
            }
            return result;
        }
    }
    
    private class HasShortEnoughDefaultBackgroundThreadPoolExecutorQueue extends AbstractCheck {
        private static final long serialVersionUID = 3482148861663152178L;

        public HasShortEnoughDefaultBackgroundThreadPoolExecutorQueue() {
            super("has short enough default background thread pool executor queue", LONG_TIMEOUT, DELAY_BETWEEN_CHECKS);
        }

        @Override
        public boolean runCheck() throws Exception {
            final int defaultBackgroundThreadPoolExecutorQueueSize = replicaSet.getMaster().getDefaultBackgroundThreadPoolExecutorQueueSizeNondelayed(Landscape.WAIT_FOR_PROCESS_TIMEOUT);
            final boolean result = defaultBackgroundThreadPoolExecutorQueueSize < MAXIMUM_THREAD_POOL_QUEUE_SIZE;
            if (!result) {
                setLastFailureMessage("Candidate at " + replicaSet.getMaster().getHost().getPrivateAddress()
                        + " has too many tasks in default background thread pool executor queue: "+defaultBackgroundThreadPoolExecutorQueueSize+
                        " which is still above the maximum of "+MAXIMUM_THREAD_POOL_QUEUE_SIZE);
            }
            return result;
        }
    }

    private class HasShortEnoughDefaultForegroundThreadPoolExecutorQueue extends AbstractCheck {
        private static final long serialVersionUID = 5194383164577435150L;

        public HasShortEnoughDefaultForegroundThreadPoolExecutorQueue() {
            super("has short enough default foreground thread pool executor queue", LONG_TIMEOUT, DELAY_BETWEEN_CHECKS);
        }

        @Override
        public boolean runCheck() throws Exception {
            final int defaultForegroundThreadPoolExecutorQueueSize = replicaSet.getMaster().getDefaultForegroundThreadPoolExecutorQueueSizeNondelayed(Landscape.WAIT_FOR_PROCESS_TIMEOUT);
            final boolean result = defaultForegroundThreadPoolExecutorQueueSize < MAXIMUM_THREAD_POOL_QUEUE_SIZE;
            if (!result) {
                setLastFailureMessage("Candidate at "+replicaSet.getMaster().getHost().getPrivateAddress()
                        + " has too many tasks in default foreground thread pool executor queue: "+defaultForegroundThreadPoolExecutorQueueSize+
                        " which is still above the maximum of "+MAXIMUM_THREAD_POOL_QUEUE_SIZE);
            }
            return result;
        }
    }
    
    private class CompareServersWithRestAPI extends AbstractCheck {
        private static final long serialVersionUID = -5271988056894947109L;

        public CompareServersWithRestAPI() {
            super("compare servers with REST API", SERVER_COMPARISON_TIMEOUT, DELAY_BETWEEN_COMPARISON_CHECKS);
        }

        @Override
        public boolean runCheck() throws Exception {
            final SailingServer productionServer = landscapeService.getSailingServerFactory().getSailingServer(new URL("https", replicaSet.getHostname(), "/"), effectiveBearerToken);
            final SailingServer candidateServer = landscapeService.getSailingServerFactory().getSailingServer(new URL("https", candidateHostname, "/"), effectiveBearerToken);
            final CompareServersResult comparisonResult = candidateServer.compareServers(Optional.empty(), productionServer, Optional.empty());
            if (comparisonResult.hasDiffs()) {
                setLastFailureMessage(
                        "Candidate server does not match production server according to REST API comparison."
                                + "\nDifferences on candidate side: " + comparisonResult.getADiffs()
                                + "\nDifferences on production side: " + comparisonResult.getBDiffs()
                                + "\nNot proceeding further. You need to resolve the issues manually."
                                + "\nCheck https://"+candidateHostname+"/sailingserver/v1/compareservers?server2="+replicaSet.getHostname()
                                + "\nafter you have tried to resolve the differences."
                                + "\nThen, run your smoke checks and trigger the rotation if everything looks good.");
            }
            return !comparisonResult.hasDiffs();
        }
    }
    
    private void notifyProcessOwnerCandidateFailedToBecomeReadyForProduction() throws MailException {
        landscapeService.sendMailToUser(currentUser, "NewArchiveCandidateFailedSubject",
                "NewArchiveCandidateFailedBody", replicaSet.getServerName(), currentCheck.getName(),
                currentCheck.getLastFailureMessage());
    }

    private void notifyProcessOwnerCandidateIsReadyForSpotChecksAndRotation() throws MailException, InterruptedException, ExecutionException {
        landscapeService.sendMailToUser(currentUser, "NewArchiveCandidateReadyForSpotChecksAndRotationSubject",
                "NewArchiveCandidateReadyForSpotChecksAndRotationBody", replicaSet.getName(), candidateHostname,
                replicaSet.getHostname(), " - "+Util.joinStrings("\n - ", Util.map(checks, Check::getName)),
                continuationBaseURL.toString());
    }
}
