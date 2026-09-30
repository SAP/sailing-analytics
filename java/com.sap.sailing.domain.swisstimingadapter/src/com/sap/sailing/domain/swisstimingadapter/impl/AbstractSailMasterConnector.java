package com.sap.sailing.domain.swisstimingadapter.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.common.TrackedRaceStatusEnum;
import com.sap.sailing.domain.swisstimingadapter.Competitor;
import com.sap.sailing.domain.swisstimingadapter.Course;
import com.sap.sailing.domain.swisstimingadapter.Fix;
import com.sap.sailing.domain.swisstimingadapter.Mark;
import com.sap.sailing.domain.swisstimingadapter.Mark.MarkType;
import com.sap.sailing.domain.swisstimingadapter.MessageType;
import com.sap.sailing.domain.swisstimingadapter.Race;
import com.sap.sailing.domain.swisstimingadapter.RaceStatus;
import com.sap.sailing.domain.swisstimingadapter.RacingStatus;
import com.sap.sailing.domain.swisstimingadapter.SailMasterConnector;
import com.sap.sailing.domain.swisstimingadapter.SailMasterListener;
import com.sap.sailing.domain.swisstimingadapter.SailMasterMessage;
import com.sap.sailing.domain.swisstimingadapter.StartList;
import com.sap.sailing.domain.swisstimingadapter.TrackerType;
import com.sap.sse.common.AbstractBearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Position;
import com.sap.sse.common.Speed;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.KilometersPerHourSpeedImpl;
import com.sap.sse.common.impl.KnotSpeedImpl;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.common.impl.MeterDistance;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.shared.util.impl.UUIDHelper;

/**
 * Implements the connector to the SwissTiming Sail Master system. It uses a host name and port number to establish the
 * connection via TCP. The connector offers a number of explicit service request methods. Additionally, the connector
 * can receive "spontaneous" events sent by the sail master system. Clients can register for those spontaneous events
 * (see {@link #addSailMasterListener}).
 * <p>
 * 
 * When the connector is used with SailMaster instances hidden behind a "bridge" / firewall, no explicit requests are
 * possible, and the connector has to rely solely on the events it receives. It may, though, load recorded race-specific
 * messages through a {@link RaceSpecificMessageLoader} object. If a non-<code>null</code> {@link RaceSpecificMessageLoader}
 * is provided to the constructor, the connector will fetch the {@link #getRace() race} from that loader.
 * Additionally, the connector will use the loader upon each {@link #trackRace(String)} to load all messages recorded
 * by the loader for the race requested so far.
 * <p>
 * 
 * Generally, the connector needs to be instructed for which races it shall handle events using calls to the
 * {@link #trackRace} and {@link #stopTrackingRace} operations. {@link MessageType#isRaceSpecific() Race-specific
 * messages} for other races are ignored and not forwarded to any listener.<p>
 * 
 * Clients that want to wait until the connector changes to {@link #isStopped()} can {@link Object#wait()} on this
 * object because it notifies all waiters when changing from !{@link #isStopped()} to {@link #isStopped()}. 
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public abstract class AbstractSailMasterConnector extends SailMasterTransceiverImpl implements SailMasterConnector, Runnable {
    private static final Logger logger = Logger.getLogger(AbstractSailMasterConnector.class.getName());
    
    private final DateFormat dateFormat;
    private final Set<SailMasterListener> listeners;
    private final Thread receiverThread;
    private boolean stopped;
    private final String raceId;
    private final String raceName;
    private final String raceDescription;
    private final BoatClass boatClass;
    
    /**
     * Currently the SwissTiming SailMaster protocol only transmits time zone information when sending
     * an {@link MessageType#RPD RPD} event. Other events, such as the {@link MessageType#STT STT} or
     * {@link MessageType#CAM CAM} events/responses also carry time stamps but in hh:mm:ss format without
     * any hint as to the time zone relative to which they are given.<p>
     * 
     * The only way known so far for how to find out the time zone relative to which the other time stamps
     * are to be interpreted is to start with the current default time zone's offset and wait for an
     * {@link MessageType#RPD RPD} event to be received. From this event, the time zone offset can be extracted
     * and applied to all other time stamps. It is stored in this field.
     */
    private String lastTimeZoneSuffix;
    
    /**
     * Some messages in the SwissTiming SailMaster protocol lack proper time stamp information. It is therefore
     * necessary to keep track of time stamps received from other messages and use them as an approximation for
     * the time point of messages received without explicit time stamp.
     */
    private TimePoint lastRPDMessageTimePoint;
    
    private TimePoint startTime;
    
    /**
     * Used for the {@link #rendevouz(SailMasterMessage)} pattern. For each {@link MessageType} there
     * is a queue to which response messages of that type are offered so that {@link #receiveMessage(MessageType)}
     * can take them from there.
     */
    private final Map<MessageType, BlockingQueue<SailMasterMessage>> unprocessedMessagesByType;
    
    private long maxSequenceNumber;

    private Long numberOfStoredMessages;
    
    /**
     * Subclasses must invoke {@link #startReceiverThread()} in their constructor before the constructor completes, but
     * usually after finishing the initialization of all their fields.
     */
    protected AbstractSailMasterConnector(String raceId, String raceName, String raceDescription, BoatClass boatClass, SwissTimingRaceTrackerImpl swissTimingRaceTracker) throws InterruptedException, ParseException {
        super();
        maxSequenceNumber = -1l;
        this.raceId = raceId; // from this time on, the connector interprets messages for raceID
        this.raceName = raceName;
        this.raceDescription = raceDescription;
        this.boatClass = boatClass;
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        this.listeners = new HashSet<>();
        this.unprocessedMessagesByType = new HashMap<>();
        this.addSailMasterListener(swissTimingRaceTracker);
        receiverThread = new Thread(this, "SwissTiming SailMaster Receiver");
    }

    protected void startReceiverThread() {
        receiverThread.start();
    }
    
    public void run() {
        try {
            while (!stopped) {
                try {
                    ensureConnected(); // the result of this may be that the connector w\s stopped in between, so another check is required
                    if (!stopped && isConnected()) {
                        String receivedMessage = receiveMessage(getInputStream());
                        if (receivedMessage == null) {
                            logger.info("Reached EOF for "+this+"; disconnecting");
                            // reached EOF; this means the socket is or can be closed
                            if (isConnected()) {
                                disconnect();
                            }
                        } else {
                            SailMasterMessage message = new SailMasterMessageImpl(receivedMessage);
                            // drop race-specific messages for non-tracked races
                            if (message.getSequenceNumber() != null) {
                                maxSequenceNumber = Math.max(maxSequenceNumber, message.getSequenceNumber());
                                if (maxSequenceNumber <= numberOfStoredMessages) {
                                    notifyListenersStoredDataProgress(raceId, (double) maxSequenceNumber
                                            / (double) numberOfStoredMessages);
                                }
                            }
                            if (message.isResponse()) {
                                // this is a response for an explicit request
                                rendevouz(message);
                            } else if (message.isEvent()) {
                                // a spontaneous event
                                logger.fine("notifying message " + message);
                                notifyListeners(message);
                            }
                            if (message.getType() == MessageType._STOPSERVER) {
                                logger.info("SailMasterConnector received " + MessageType._STOPSERVER.name());
                                stop();
                            }
                        }
                    }
                } catch (SocketException se) {
                    // This occurs if the socket was closed which may mean the connector was stopped. Check in while
                    logger.info("Caught exception "+se+" during socket operation; setting socket to null");
                    disconnect();
                    Thread.sleep(1000); // try again in 1s
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception in sail master connector "+AbstractSailMasterConnector.class.getName()+".run for "+this, e);
        }
        logger.info("Stopping Sail Master connector thread for "+this);
        stopped = true;
    }
    
    @Override
    public SailMasterMessage receiveMessage(MessageType type) throws InterruptedException {
        BlockingQueue<SailMasterMessage> blockingQueue = getBlockingQueue(type);
        SailMasterMessage result = blockingQueue.take();
        return result;
    }

    private BlockingQueue<SailMasterMessage> getBlockingQueue(MessageType type) {
        synchronized (unprocessedMessagesByType) {
            BlockingQueue<SailMasterMessage> blockingQueue;
            blockingQueue = unprocessedMessagesByType.get(type);
            if (blockingQueue == null) {
                blockingQueue = new LinkedBlockingQueue<SailMasterMessage>();
                unprocessedMessagesByType.put(type, blockingQueue);
            }
            return blockingQueue;
        }
    }
    
    private void rendevouz(SailMasterMessage message) {
        BlockingQueue<SailMasterMessage> blockingQueue = getBlockingQueue(message.getType());
        blockingQueue.offer(message);
    }

    protected void notifyListeners(SailMasterMessage message) {
        if (message.getType() != null) {
            try {
                switch (message.getType()) {
                case RPD:
                    notifyListenersRPD(message);
                    break;
                case RAC:
                    notifyListenersRAC(message);
                    break;
                case CCG:
                    notifyListenersCCG(message);
                    break;
                case STL:
                    notifyListenersSTL(message);
                    break;
                case CAM:
                    notifyListenersCAM(message);
                    break;
                case TMD:
                    notifyListenersTMD(message);
                    break;
                case WND:
                    notifyListenersWND(message);
                    break;
                default:
                    // ignore all other messages because there are no notification patterns for those
                }
            } catch (Exception e) {
                // broken messages are ignored
                logger.log(Level.WARNING, "Exception caught during parsing of message '" + message.getMessage() + "' : " + e.getMessage(), e);
            }
        }
    }
    
    private void notifyListenersStoredDataProgress(String raceID, double progress) {
        for (SailMasterListener listener : getListeners()) {
            try {
                listener.storedDataProgress(raceID, progress, getStatusAfterLoadingIsComplete());
            } catch (Exception e) {
                logger.info("Exception occurred trying to notify listener "+listener+" about progress "+progress);
                logger.throwing(AbstractSailMasterConnector.class.getName(), "notifyStoredDataProgress", e);
            }
        }
    }

    protected abstract TrackedRaceStatusEnum getStatusAfterLoadingIsComplete();
    
    private void notifyListenersWND(SailMasterMessage message) {
        // example message: WND|W4702|1|320|5.4
        String raceID = message.getSections()[1];
        int zeroBasedMarkIndex = Integer.valueOf(message.getSections()[2]);
        double windDirectionTrueDegrees = Double.valueOf(message.getSections()[3]);
        double windSpeedInKnots = Double.valueOf(message.getSections()[4]);
        for (SailMasterListener listener : getListeners()) {
            try {
                listener.receivedWindData(raceID, zeroBasedMarkIndex, windDirectionTrueDegrees, windSpeedInKnots);
            } catch (Exception e) {
                logger.info("Exception occurred trying to notify listener "+listener+" about "+message+": "+e.getMessage());
                logger.throwing(AbstractSailMasterConnector.class.getName(), "notifyListenersWND", e);
            }
        }
    }

    private void notifyListenersTMD(SailMasterMessage message) {
        // example message: TMD|W4702|NZL 75|1|4;;00:49:43
        String raceID = message.getSections()[1];
        String competitorIdAsString = message.getSections()[2];
        int count = Integer.valueOf(message.getSections()[3]);
        List<Util.Triple<Integer, Integer, Long>> markIndicesRanksAndTimesSinceStartInMilliseconds = new ArrayList<Util.Triple<Integer,Integer,Long>>();
        for (int i = 0; i < count; i++) {
            String[] details = message.getSections()[4+i].split(";");
            Integer markIndex = details.length <= 0 || details[0].trim().length() == 0 ? null : Integer.valueOf(details[0]); 
            Integer rank = details.length <= 1 || details[1].trim().length() == 0 ? null : Integer.valueOf(details[1]); 
            Long timeSinceStartInMilliseconds = details.length <= 2 || details[2].trim().length() == 0 ? null :
                parseHHMMSSToMilliseconds(details[2]);
            markIndicesRanksAndTimesSinceStartInMilliseconds.add(new Util.Triple<Integer, Integer, Long>(markIndex, rank, timeSinceStartInMilliseconds));
        }
        for (SailMasterListener listener : getListeners()) {
            try {
                listener.receivedTimingData(raceID, competitorIdAsString, markIndicesRanksAndTimesSinceStartInMilliseconds);
            } catch (Exception e) {
                logger.info("Exception occurred trying to notify listener "+listener+" about "+message+": "+e.getMessage());
                logger.throwing(AbstractSailMasterConnector.class.getName(), "notifyListenersTMD", e);
            }
        }
    }

    private void notifyListenersCAM(SailMasterMessage message) throws ParseException {
        List<Util.Triple<Integer, TimePoint, String>> clockAtMarkResults = parseClockAtMarkMessage(message);
        for (SailMasterListener listener : getListeners()) {
            try {
                listener.receivedClockAtMark(message.getSections()[1], clockAtMarkResults);
            } catch (Exception e) {
                logger.info("Exception occurred trying to notify listener "+listener+" about "+message+": "+e.getMessage());
                logger.throwing(AbstractSailMasterConnector.class.getName(), "notifyListenersCAM", e);
            }
        }
    }

    private void notifyListenersSTL(SailMasterMessage message) {
        StartList startListMessage = parseStartListMessage(message);
        for (SailMasterListener listener : getListeners()) {
            try {
                listener.receivedStartList(message.getSections()[1], startListMessage);
            } catch (Exception e) {
                logger.info("Exception occurred trying to notify listener "+listener+" about "+message+": "+e.getMessage());
                logger.throwing(AbstractSailMasterConnector.class.getName(), "notifyListenersSTL", e);
            }
        }
    }

    private void notifyListenersCCG(SailMasterMessage message) {
        Course course = parseCourseConfigurationMessage(message);
        for (SailMasterListener listener : getListeners()) {
            try {
                listener.receivedCourseConfiguration(message.getSections()[1], course);
            } catch (Exception e) {
                logger.info("Exception occurred trying to notify listener "+listener+" about "+message+": "+e.getMessage());
                logger.throwing(AbstractSailMasterConnector.class.getName(), "notifyListenersCCG", e);
            }
        }
    }

    private void notifyListenersRAC(SailMasterMessage message) {
        Iterable<Race> races = parseAvailableRacesMessage(message);
        for (SailMasterListener listener : getListeners()) {
            try {
                listener.receivedAvailableRaces(races);
            } catch (Exception e) {
                logger.info("Exception occurred trying to notify listener "+listener+" about "+message+": "+e.getMessage());
                logger.throwing(AbstractSailMasterConnector.class.getName(), "notifyListenersRAC", e);
            }
        }
        // not race specific; no need to notify any listener from raceSpecificListeners
    }

    private void notifyListenersRPD(SailMasterMessage message) throws ParseException {
        assert message.getType() == MessageType.RPD;
        String[] sections = message.getSections();
        String raceID = sections[1];
        final String[] raceStatusIntAndRacingStatusInt = sections[2].split(",");
        final String raceStatusAsString = raceStatusIntAndRacingStatusInt.length > 0 ? raceStatusIntAndRacingStatusInt[0] : null;
        final RaceStatus raceStatus = raceStatusAsString == null || raceStatusAsString.trim().isEmpty() ? null : RaceStatus.values()[Integer.valueOf(raceStatusAsString)];
        final String racingStatusAsString;
        if (raceStatusIntAndRacingStatusInt.length > 1) {
            racingStatusAsString = raceStatusIntAndRacingStatusInt[1];
        } else {
            racingStatusAsString = null;
        }
        final RacingStatus racingStatus = racingStatusAsString == null || racingStatusAsString.trim().isEmpty() ? null : RacingStatus.values()[Integer.valueOf(racingStatusAsString)];
        TimePoint timePoint = new MillisecondsTimePoint(parseTimeAndDateISO(sections[3], raceID));
        lastRPDMessageTimePoint = timePoint;
        String dateISO = sections[3].substring(0, sections[3].indexOf('T'));
        String startTimeEstimatedStartTimeISO = dateISO+"T"+sections[4]+lastTimeZoneSuffix;
        TimePoint startTimeEstimatedStartTime = sections[4].trim().length() == 0 ? null : new MillisecondsTimePoint(
                parseTimeAndDateISO(startTimeEstimatedStartTimeISO, raceID));
        if (startTimeEstimatedStartTime != null) {
            startTime = startTimeEstimatedStartTime;
        }
        Long millisecondsSinceRaceStart = sections[5].trim().length() == 0 ? null : parseHHMMSSToMilliseconds(sections[5]);
        Integer nextMarkIndexForLeader = sections[6].trim().length() == 0 ? null : Integer.valueOf(sections[6]);
        Distance distanceToNextMarkForLeader = sections[7].trim().length() == 0 ? null : new MeterDistance(Double.valueOf(sections[7]));
        int count = Integer.valueOf(sections[8]);
        Collection<Fix> fixes = new ArrayList<Fix>();
        for (int i=0; i<count; i++) {
            int fixDetailIndex = 0;
            final String[] fixSections = sections[9+i].split(";");
            final boolean postVersion1_0 = sections[9+i].split(";", -1).length >= 14;
            if (fixSections.length > 2) {
                final String trackedObjectIdAsString = fixSections[fixDetailIndex++].trim();
                if (trackedObjectIdAsString != null && !trackedObjectIdAsString.trim().isEmpty()) {
                    final String trackerTypeAsString = fixSections[fixDetailIndex++];
                    final TrackerType trackerType = trackerTypeAsString == null || trackerTypeAsString.trim().isEmpty() ? null : TrackerType.values()[Integer.valueOf(trackerTypeAsString)];
                    final String ageOfDataInMillisAsString = fixSections[fixDetailIndex++];
                    final Long ageOfDataInMilliseconds = ageOfDataInMillisAsString==null || ageOfDataInMillisAsString.trim().isEmpty() ? null : (1000l * Long.valueOf(ageOfDataInMillisAsString));
                    final String latDegAsString = fixSections[fixDetailIndex++];
                    final String lngDegAsString = fixSections[fixDetailIndex++];
                    final Position position = latDegAsString==null || latDegAsString.trim().isEmpty() || lngDegAsString==null || lngDegAsString.trim().isEmpty() ? null :
                        new DegreePosition(Double.valueOf(latDegAsString), Double.valueOf(lngDegAsString));
                    final String sogInKnotsAsString = fixSections[fixDetailIndex++];
                    final Double speedOverGroundInKnots = sogInKnotsAsString==null || sogInKnotsAsString.trim().isEmpty() ? null : Double.valueOf(sogInKnotsAsString);
                    final int alsIndex = postVersion1_0 ? fixDetailIndex+1 : fixDetailIndex;
                    final int vmgIndex = postVersion1_0 ? fixDetailIndex : fixDetailIndex+1;
                    final Speed averageSpeedOverGround = fixSections[alsIndex].trim().length() == 0 ? null
                                : new KnotSpeedImpl(Double.valueOf(fixSections[alsIndex]));
                    final Speed velocityMadeGood = fixSections[vmgIndex].trim().length() == 0 ? null : new KnotSpeedImpl(
                            Double.valueOf(fixSections[vmgIndex]));
                    fixDetailIndex += 2;
                    final String cogAsString = fixSections[fixDetailIndex++];
                    final AbstractBearing cog = cogAsString==null || cogAsString.trim().isEmpty() ? null :
                        new DegreeBearingImpl(Double.valueOf(cogAsString));
                    final SpeedWithBearing speed = speedOverGroundInKnots == null || cog == null ? null : new KnotSpeedWithBearingImpl(speedOverGroundInKnots, cog);
                    final Integer nextMarkIndex = fixSections.length <= fixDetailIndex
                            || fixSections[fixDetailIndex].trim().length() == 0 ? null : Integer
                            .valueOf(fixSections[fixDetailIndex]);
                    fixDetailIndex++;
                    final Integer rank = fixSections.length <= fixDetailIndex || fixSections[fixDetailIndex].trim().length() == 0 ? null
                            : Integer.valueOf(fixSections[fixDetailIndex]);
                    fixDetailIndex++;
                    final Distance distanceToLeader = fixSections.length <= fixDetailIndex
                            || fixSections[fixDetailIndex].trim().length() == 0 ? null : new MeterDistance(
                            Double.valueOf(fixSections[fixDetailIndex]));
                    fixDetailIndex++;
                    final Distance distanceToNextMark = fixSections.length <= fixDetailIndex
                            || fixSections[fixDetailIndex].trim().length() == 0 ? null : new MeterDistance(
                            Double.valueOf(fixSections[fixDetailIndex]));
                    fixDetailIndex++;
                    final String boatIRM; // the "disqualification" or "MaxPointReason"
                    if (postVersion1_0 && fixSections.length > fixDetailIndex) {
                        boatIRM = fixSections[fixDetailIndex++];
                    } else {
                        boatIRM = null;
                    }
                    fixes.add(new FixImpl(UUIDHelper.tryUuidConversion(trackedObjectIdAsString), trackerType, ageOfDataInMilliseconds, position, speed, nextMarkIndex,
                            rank, averageSpeedOverGround, velocityMadeGood, distanceToLeader, distanceToNextMark, boatIRM));
                }
            }
        }
        Set<SailMasterListener> allListeners = getListeners();
        for (SailMasterListener listener : allListeners) {
            try {
                listener.receivedRacePositionData(raceID, raceStatus, racingStatus, timePoint, startTimeEstimatedStartTime,
                        millisecondsSinceRaceStart, nextMarkIndexForLeader, distanceToNextMarkForLeader, fixes);
            } catch (Exception e) {
                logger.info("Exception occurred trying to notify listener "+listener+" about "+message+": "+e.getMessage());
                logger.throwing(AbstractSailMasterConnector.class.getName(), "notifyListenersRPD", e);
            }
        }
    }

    private Set<SailMasterListener> getListeners() {
        synchronized (listeners) {
            return Collections.unmodifiableSet(listeners);
        }
    }

    @Override
    public void stop() throws IOException {
        stopped = true;
        logger.info("Stopping "+this);
        disconnect();
        synchronized (this) {
            notifyAll();
        }
    }
    
    @Override
    public boolean isStopped() {
        return stopped;
    }
    
    @Override
    public void addSailMasterListener(SailMasterListener listener)  {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }
    
    @Override
    public void removeSailMasterListener(SailMasterListener listener) throws IOException {
        synchronized (listeners) {
            listeners.remove(listener);
            if (listeners.isEmpty()) {
                stop();
            }
        }
    }

    public SailMasterMessage sendRequestAndGetResponse(MessageType messageType, String... args) throws UnknownHostException, IOException, InterruptedException {
        ensureConnected();
        return sendRequestAndGetResponseAssumingSocketIsOpen(messageType, args);
    }

    private SailMasterMessage sendRequestAndGetResponseAssumingSocketIsOpen(MessageType messageType, String... args)
            throws IOException, InterruptedException {
        OutputStream os = getOutputStream();
        final SailMasterMessage sailMasterMessage = createSailMasterMessage(messageType, args);
        sendMessage(sailMasterMessage, os);
        return receiveMessage(messageType);
    }

    private SailMasterMessage createSailMasterMessage(MessageType messageType, String... args) {
        StringBuilder requestMessage = new StringBuilder();
        requestMessage.append(messageType.name());
        if (messageType != MessageType.OPN && messageType != MessageType.LSN) {
            // OPN and LSN don't have the questionmark appended
            requestMessage.append('?');
        }
        for (String arg : args) {
            requestMessage.append('|');
            requestMessage.append(arg);
        }
        final SailMasterMessage sailMasterMessage = new SailMasterMessageImpl(requestMessage.toString());
        return sailMasterMessage;
    }
    
    protected abstract OutputStream getOutputStream() throws IOException;
    protected abstract InputStream getInputStream() throws IOException;

    private final Object ensureSocketIsOpenSemaphor = new Object();
    private void ensureConnected() throws InterruptedException, NumberFormatException, IOException {
        synchronized (ensureSocketIsOpenSemaphor) {
            while (!stopped && !isConnected()) {
                try {
                    connect();
                    final OutputStream os = getOutputStream();
                    final InputStream is = getInputStream();
                    final SailMasterMessage opnRequest = createSailMasterMessage(MessageType.OPN, raceId);
                    sendMessage(opnRequest, os);
                    SailMasterMessage opnResponse = new SailMasterMessageImpl(receiveMessage(is));
                    if (opnResponse.getType() != MessageType.OPN || !"OK".equals(opnResponse.getSections()[1])) {
                        logger.info("Recevied non-OK response " + opnResponse + " in "+this+" for our request " + opnRequest
                                + ". Closing socket and stopping because we have no hope for recovery");
                        stopped = true;
                        disconnect();
                    } else {
                        logger.info("Received " + opnResponse + " in "+this+" which seems OK. Continuing with "
                                + MessageType.LSN.name() + " request...");
                        numberOfStoredMessages = Long.valueOf(opnResponse.getSections()[2]);
                        List<String> lsnArgs = new ArrayList<>();
                        lsnArgs.add("ON"); // request live messages always; why not?
                        if (maxSequenceNumber != -1) {
                            logger.info("Requesting messages starting from sequence number " + (maxSequenceNumber + 1)+" in "+this);
                            // already received a numbered message; ask only for newer messages with greater sequence number
                            lsnArgs.add(Long.valueOf(maxSequenceNumber + 1).toString());
                        } else {
                            logger.info("Requesting messages starting from the beginning in "+this);
                            lsnArgs.add("1");
                        }
                        final SailMasterMessage lsnRequest = createSailMasterMessage(MessageType.LSN,
                                lsnArgs.toArray(new String[0]));
                        sendMessage(lsnRequest, os);
                        SailMasterMessageImpl lsnResponse = new SailMasterMessageImpl(receiveMessage(is));
                        if (lsnResponse.getType() != MessageType.LSN || !"OK".equals(lsnResponse.getSections()[1])) {
                            logger.info("Received non-OK response " + lsnResponse + " for our request " + lsnRequest
                                    + " in "+this+". Closing socket and trying again in 1s...");
                            disconnectAndWaitABit();
                        } else {
                            logger.info("Received " + lsnResponse
                                    + " which seems to be OK. I think we're connected in "+this+"!");
                        }
                    }
                } catch (IOException | URISyntaxException e) {
                    logger.log(Level.INFO, "Exception trying to establish connection in "+this+". Trying again in 1s.", e);
                    disconnectAndWaitABit();
                }
            }
        }
    }

    protected abstract void connect() throws IOException, URISyntaxException;

    protected abstract boolean isConnected() throws IOException;

    protected abstract void disconnect() throws IOException;
    
    private void disconnectAndWaitABit() throws InterruptedException, IOException {
        disconnect();
        Thread.sleep(1000);
    }
    
    @Override
    public Race getRace() {
        return new RaceImpl(raceId, raceName, raceDescription, boatClass);
    }

    private List<Race> parseAvailableRacesMessage(SailMasterMessage availableRacesMessage) {
        assertMessageType(MessageType.RAC, availableRacesMessage);
        int count = Integer.valueOf(availableRacesMessage.getSections()[1]);
        List<Race> result = new ArrayList<Race>();
        for (int i=0; i<count; i++) {
            String[] idAndDescription = availableRacesMessage.getSections()[2+i].split(";");
            result.add(new RaceImpl(idAndDescription[0], idAndDescription[1], idAndDescription[1], boatClass));
        }
        return result;
    }

    @Override
    public Course getCourse(String raceID) throws UnknownHostException, IOException, InterruptedException {
        SailMasterMessage response = sendRequestAndGetResponse(MessageType.CCG, raceID);
        String[] sections = response.getSections();
        assertResponseType(MessageType.CCG, response);
        assertRaceID(raceID, sections[1]);
        return parseCourseConfigurationMessage(response);
    }

    private Course parseCourseConfigurationMessage(SailMasterMessage courseConfigurationMessage) {
        assertMessageType(MessageType.CCG, courseConfigurationMessage);
        int count = Integer.valueOf(courseConfigurationMessage.getSections()[2]);
        List<Mark> marks = new ArrayList<Mark>();
        for (int i=0; i<count; i++) {
            String[] markDetails = courseConfigurationMessage.getSections()[3+i].split(";");
            MarkType markType = null;
            final int devicesNamesStartIndex;
            if (courseConfigurationMessage.getSections()[3+i].split(";", -1).length == 5) {
                // this is the SailMaster protocol version 1.0 (May 2012) or later (see bug 1000), containing
                // a MarkType specification before the two tracker IDs:
                int markTypeIndex = Integer.valueOf(markDetails[2]);
                markType = MarkType.values()[markTypeIndex];
                devicesNamesStartIndex = 3;
            } else {
                devicesNamesStartIndex = 2;
            }
            marks.add(new MarkImpl(markDetails[1], Integer.valueOf(markDetails[0]),
                    Arrays.asList(markDetails).subList(devicesNamesStartIndex, markDetails.length).stream()
                        .filter(idAsString->Util.hasLength(idAsString))
                        .map(idAsString->UUIDHelper.tryUuidConversion(idAsString))
                        .collect(Collectors.toList()),
                    markType));
        }
        return new CourseImpl(courseConfigurationMessage.getSections()[1], marks);
    }
    
    @Override
    public TimePoint getLastRPDMessageTimePoint() {
        return lastRPDMessageTimePoint;
    }
    
    private String getLastTimeZoneSuffix(String raceID) {
        String result = lastTimeZoneSuffix;
        if (result == null) {
            int offset = TimeZone.getDefault().getOffset(System.currentTimeMillis())/1000/3600;
            result = (offset<0?"-":"+") + new DecimalFormat("00").format(offset)+"00";
            lastTimeZoneSuffix = result;
        }
        return result;
    }
    
    private String prefixTimeWithISOTodayAndSuffixWithTimezoneIndicator(String time, String raceID) {
        synchronized (dateFormat) {
            return dateFormat.format(new Date()).substring(0, "yyyy-mm-ddT".length())+time+getLastTimeZoneSuffix(raceID);
        }
    }

    private Date parseTimeAndDateISO(String timeAndDateISO, String raceID) throws ParseException {
        char timeZoneIndicator = timeAndDateISO.charAt(timeAndDateISO.length()-6);
        if ((timeZoneIndicator == '+' || timeZoneIndicator == '-') && timeAndDateISO.charAt(timeAndDateISO.length()-3) == ':') {
            timeAndDateISO = timeAndDateISO.substring(0, timeAndDateISO.length()-3)+timeAndDateISO.substring(timeAndDateISO.length()-2);
            lastTimeZoneSuffix = timeAndDateISO.substring(timeAndDateISO.length()-5);
        }
        synchronized(dateFormat) {
            return dateFormat.parse(timeAndDateISO);
        }
    }

    private void assertRaceID(String raceID, String section) {
        if (!section.equals(raceID)) {
            throw new RuntimeException("Expected race ID "+raceID+" but received "+section);
        }
    }

    private void assertMarkIndex(int markIndex, String section) {
        if (Integer.valueOf(section).intValue() != markIndex) {
            throw new RuntimeException("Expected marker index " + markIndex + " in response but received " + section);
        }
    }

    private void assertBoatID(String boatID, String section) {
        if (!section.equals(boatID)) {
            throw new RuntimeException("Expected boat ID " + boatID + " in response but received " + section);
        }
    }
    
    private void assertMessageType(MessageType expectedMessageType, SailMasterMessage message) {
        if (message.getType() != expectedMessageType) {
            throw new RuntimeException("Expected a "+expectedMessageType+" message type but got "+message.getType());
        }
    }

    private void assertResponseType(MessageType responseType, SailMasterMessage message) {
        if (!message.isResponse()) {
            throw new RuntimeException("Expected a response message but got "+message);
        }
        if (message.getType() != responseType) {
            throw new RuntimeException("Expected a "+responseType+" response for a "+responseType+" request but got "+message.getType());
        }
    }

    private void assertLeg(String leg, String section) {
        if (!section.equals(leg)) {
            throw new RuntimeException("Expected leg " + leg + " in response but received " + section);
        }
    }

    @Override
    public StartList getStartList(String raceID) throws UnknownHostException, IOException, InterruptedException {
        SailMasterMessage response = sendRequestAndGetResponse(MessageType.STL, raceID);
        String[] sections = response.getSections();
        assertResponseType(MessageType.STL, response);
        assertRaceID(raceID, sections[1]);
        return parseStartListMessage(response);
    }

    private StartList parseStartListMessage(SailMasterMessage startListMessage) {
        assertMessageType(MessageType.STL, startListMessage);
        ArrayList<Competitor> competitors = new ArrayList<Competitor>();
        int count = Integer.valueOf(startListMessage.getSections()[2]);
        for (int i=0; i<count; i++) {
            String[] competitorDetails = startListMessage.getSections()[3+i].split(";");
            competitors.add(new CompetitorWithoutID(competitorDetails[0], competitorDetails[1], competitorDetails[2]));
        }
        return new StartListImpl(startListMessage.getSections()[1], competitors);
    }

    @Override
    public TimePoint getStartTime() throws UnknownHostException, IOException, ParseException, InterruptedException {
        return startTime;
    }

    private Date parseTimePrefixedWithISOToday(String timeHHMMSS, String raceID) throws ParseException {
        synchronized(dateFormat) {
            return dateFormat.parse(prefixTimeWithISOTodayAndSuffixWithTimezoneIndicator(timeHHMMSS, raceID));
        }
    }

    @Override
    public Distance getDistanceToMark(String raceID, int markIndex, String boatID) throws UnknownHostException, IOException, InterruptedException {
        SailMasterMessage response = sendRequestAndGetResponse(MessageType.DTM, raceID, ""+markIndex, boatID);
        String[] sections = response.getSections();
        assertResponseType(MessageType.DTM, response);
        assertRaceID(raceID, sections[1]);
        assertMarkIndex(markIndex, sections[2]);
        assertBoatID(boatID, sections[3]);
        return sections.length <= 4 || sections[4].trim().length() == 0 ? null : new MeterDistance(Double.valueOf(sections[4]));
    }

    @Override
    public Speed getCurrentBoatSpeed(String raceID, String boatID) throws UnknownHostException, IOException, InterruptedException {
        SailMasterMessage response = sendRequestAndGetResponse(MessageType.CBS, raceID, boatID);
        String[] sections = response.getSections();
        assertResponseType(MessageType.CBS, response);
        assertRaceID(raceID, sections[1]);
        assertBoatID(boatID, sections[2]);
        return new KilometersPerHourSpeedImpl(3.6*Double.valueOf(sections[3]));
    }

    @Override
    public Distance getDistanceBetweenBoats(String raceID, String boatID1, String boatID2) throws UnknownHostException, IOException, InterruptedException {
        Distance result;
        if (boatID1.equals(boatID2)) {
            result = Distance.NULL;
        } else {
            SailMasterMessage response = sendRequestAndGetResponse(MessageType.DBB, raceID, boatID1, boatID2);
            String[] sections = response.getSections();
            assertResponseType(MessageType.DBB, response);
            assertRaceID(raceID, sections[1]);
            assertBoatID(boatID1, sections[2]);
            assertBoatID(boatID2, sections[3]);
            result = sections.length <= 4 || sections[4].trim().length() == 0 ? null : new MeterDistance(Double.valueOf(sections[4]));
        }
        return result;
    }

    @Override
    public Speed getAverageBoatSpeed(String raceID, String leg, String boatID) throws UnknownHostException, IOException, InterruptedException {
        SailMasterMessage response = sendRequestAndGetResponse(MessageType.ABS, raceID, leg, boatID);
        String[] sections = response.getSections();
        assertResponseType(MessageType.ABS, response);
        assertRaceID(raceID, sections[1]);
        assertLeg(leg, sections[2]);
        assertBoatID(boatID, sections[3]);
        return new KilometersPerHourSpeedImpl(3.6*Double.valueOf(sections[4]));
    }
    
    @Override
    public Map<Integer, Util.Pair<Integer, Long>> getMarkPassingTimesInMillisecondsSinceRaceStart(String raceID, String boatID)
            throws UnknownHostException, IOException, InterruptedException {
        SailMasterMessage response = sendRequestAndGetResponse(MessageType.TMD, raceID, boatID);
        String[] sections = response.getSections();
        assertResponseType(MessageType.TMD, response);
        assertRaceID(raceID, sections[1]);
        assertBoatID(boatID, sections[2]);
        int count = Integer.valueOf(sections[3]);
        Map<Integer, Util.Pair<Integer, Long>> result = new HashMap<Integer, Util.Pair<Integer, Long>>();
        for (int i=0; i<count; i++) {
            String[] markTimeDetail = sections[4+i].split(";");
            Long millisecondsSinceStart = markTimeDetail.length <= 2 || markTimeDetail[2].trim().length() == 0 ? null :
                parseHHMMSSToMilliseconds(markTimeDetail[2]);
            result.put(Integer.valueOf(markTimeDetail[0]), new Util.Pair<Integer, Long>(
                    markTimeDetail.length <= 1 || markTimeDetail[1].trim().length() == 0 ? null :
                        Integer.valueOf(markTimeDetail[1]), millisecondsSinceStart));
        }
        return result;
    }

    @Override
    public List<Util.Triple<Integer, TimePoint, String>> getClockAtMark(String raceID) throws ParseException, UnknownHostException, IOException, InterruptedException {
        SailMasterMessage response = sendRequestAndGetResponse(MessageType.CAM, raceID);
        String[] sections = response.getSections();
        assertResponseType(MessageType.CAM, response);
        assertRaceID(raceID, sections[1]);
        List<Util.Triple<Integer, TimePoint, String>> result = parseClockAtMarkMessage(response);
        return result;
    }

    private List<Util.Triple<Integer, TimePoint, String>> parseClockAtMarkMessage(SailMasterMessage clockAtMarkMessage) throws ParseException {
        assertMessageType(MessageType.CAM, clockAtMarkMessage);
        List<Util.Triple<Integer, TimePoint, String>> result = new ArrayList<Util.Triple<Integer,TimePoint,String>>();
        int count = Integer.valueOf(clockAtMarkMessage.getSections()[2]);
        for (int i=0; i<count; i++) {
            String[] clockAtMarkDetail = clockAtMarkMessage.getSections()[3+i].split(";");
            int markIndex = Integer.valueOf(clockAtMarkDetail[0]);
            TimePoint timePoint = clockAtMarkDetail.length <= 1 || clockAtMarkDetail[1].trim().length() == 0 ? null :
                new MillisecondsTimePoint(parseTimePrefixedWithISOToday(clockAtMarkDetail[1], clockAtMarkMessage.getRaceID()));
            result.add(new Util.Triple<Integer, TimePoint, String>(
                    markIndex, timePoint, clockAtMarkDetail.length <= 2 ? null : clockAtMarkDetail[2]));
        }
        return result;
    }

    private long parseHHMMSSToMilliseconds(String hhmmss) {
        String[] timeDetail = hhmmss.split(":");
        long millisecondsSinceStart = 1000 * (Long.valueOf(timeDetail[2]) + 60 * Long.valueOf(timeDetail[1]) + 3600 * Long
                .valueOf(timeDetail[0]));
        return millisecondsSinceStart;
    }

    @Override
    public void enableRacePositionData() throws UnknownHostException, IOException, InterruptedException {
        sendRequestAndGetResponse(MessageType.RPD, "1");
    }

    @Override
    public void disableRacePositionData() throws UnknownHostException, IOException, InterruptedException {
        sendRequestAndGetResponse(MessageType.RPD, "0");
    }

}
