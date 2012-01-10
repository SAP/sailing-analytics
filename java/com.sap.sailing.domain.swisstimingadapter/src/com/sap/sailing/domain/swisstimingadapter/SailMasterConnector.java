package com.sap.sailing.domain.swisstimingadapter;

import java.io.IOException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.Speed;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.Util.Pair;
import com.sap.sailing.domain.common.Util.Triple;

public interface SailMasterConnector {
    SailMasterMessage sendRequestAndGetResponse(MessageType messageType, String... args) throws UnknownHostException, IOException, InterruptedException;
    
    Iterable<Race> getRaces() throws UnknownHostException, IOException, InterruptedException;
    
    Course getCourse(String raceID) throws UnknownHostException, IOException, InterruptedException;
    
    StartList getStartList(String raceID) throws UnknownHostException, IOException, InterruptedException;
    
    TimePoint getStartTime(String raceID) throws UnknownHostException, IOException, ParseException, InterruptedException;
    
    Distance getDistanceToMark(String raceID, int markIndex, String boatID) throws UnknownHostException, IOException, InterruptedException;
    
    Speed getCurrentBoatSpeed(String raceID, String boatID) throws UnknownHostException, IOException, InterruptedException;
    
    Speed getAverageBoatSpeed(String raceID, String leg, String boatID) throws UnknownHostException, IOException, InterruptedException;
    
    Distance getDistanceBetweenBoats(String raceID, String boatID1, String boatID2) throws UnknownHostException, IOException, InterruptedException;

    /**
     * Retrieves the mark passing times for a boat ID within a race specified by <code>raceID</code>.
     * 
     * @return keys are the mark indices; values are the pairs telling the requested boat's rank at the key mark and the
     *         number of milliseconds that passed between the race start and the point in time at which the boat with ID
     *         <code>boatID</code> passed the mark.
     * @throws InterruptedException 
     */
    Map<Integer, Pair<Integer, Long>> getMarkPassingTimesInMillisecondsSinceRaceStart(String raceID, String boatID)
            throws UnknownHostException, IOException, InterruptedException;
    
    /**
     * Adds the listener and ensures that the connector is actually connected, even if no request has explicitly
     * been sent, so that the connector will at least receive spontaneous events.
     */
    void addSailMasterListener(SailMasterListener listener) throws UnknownHostException, IOException;
    
    /**
     * Like {@link #addSailMasterListener(SailMasterListener)}, but only forwards race-specific events for the
     * race identified by <code>raceID</code> to the <code>listener</code>. Clients still need to call
     * {@link #trackRace(String)} with the <code>raceID</code> to make the connector actually receive the
     * events for that race.
     */
    void addSailMasterListener(String raceID, SailMasterListener listener) throws UnknownHostException, IOException;
    
    void removeSailMasterListener(SailMasterListener listener);

    SailMasterMessage receiveMessage(MessageType type) throws InterruptedException;
    
    /**
     * Enables receiving RPD (Race Position Data) events to be emitted by the server. If such events are received, they
     * are forwarded to {@link #addSailMasterListener(SailMasterListener) registered} listeners by calling their
     * {@link SailMasterListener#receivedRacePositionData(String, RaceStatus, TimePoint, TimePoint, Long, Integer, com.sap.sailing.domain.common.Distance, java.util.Collection)}
     * method.
     */
    void enableRacePositionData() throws UnknownHostException, IOException, InterruptedException;
    
    /**
     * Stops the server from emitting RPD (Race Position Data) events.
     */
    void disableRacePositionData() throws UnknownHostException, IOException, InterruptedException;

    /**
     * @return the list of mark index / mark time / sail number triplets telling when which leader
     * first passed the mark with the respective index
     */
    List<Triple<Integer, TimePoint, String>> getClockAtMark(String raceID) throws ParseException, UnknownHostException, IOException, InterruptedException;
    
    /**
     * Stops this connector's thread that is started upon creation and responsible for receiving spontaneous events
     * @throws IOException 
     */
    void stop() throws IOException;

    void trackRace(String raceID) throws ParseException;

    void stopTrackingRace(String raceID);

    void removeSailMasterListener(String raceID, SailMasterListener listener);

    boolean isStopped();
}
