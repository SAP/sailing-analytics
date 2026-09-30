package com.sap.sailing.domain.swisstimingadapter.classes.services;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import com.sap.sailing.domain.swisstimingadapter.Competitor;
import com.sap.sailing.domain.swisstimingadapter.Mark;
import com.sap.sailing.domain.swisstimingadapter.Mark.MarkType;
import com.sap.sailing.domain.swisstimingadapter.Race;
import com.sap.sailing.domain.swisstimingadapter.SailMasterMessage;
import com.sap.sailing.domain.swisstimingadapter.classes.messages.CAMMessage;
import com.sap.sailing.domain.swisstimingadapter.classes.messages.CCGMessage;
import com.sap.sailing.domain.swisstimingadapter.classes.messages.ClockAtMarkElement;
import com.sap.sailing.domain.swisstimingadapter.classes.messages.RACMessage;
import com.sap.sailing.domain.swisstimingadapter.classes.messages.RPDMessage;
import com.sap.sailing.domain.swisstimingadapter.classes.messages.RacePositionDataElement;
import com.sap.sailing.domain.swisstimingadapter.classes.messages.STLMessage;
import com.sap.sailing.domain.swisstimingadapter.classes.messages.TMDMessage;
import com.sap.sailing.domain.swisstimingadapter.classes.messages.TimingDataElement;
import com.sap.sailing.domain.swisstimingadapter.classes.services.Exceptions.MessageScriptParsingException;
import com.sap.sailing.domain.swisstimingadapter.impl.CompetitorWithoutID;
import com.sap.sailing.domain.swisstimingadapter.impl.MarkImpl;
import com.sap.sailing.domain.swisstimingadapter.impl.RaceImpl;
import com.sap.sailing.domain.swisstimingadapter.impl.SailMasterMessageImpl;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Speed;
import com.sap.sse.common.SpeedWithBearing;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedImpl;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.common.impl.MeterDistance;

public class MessageFileServiceImpl implements MessageFileService {

    private File file;
    private BufferedWriter writer;
    private BufferedReader reader;
    // private String lastTimeZoneSuffix;
    private final DateFormat dateFormat;
    private final DateFormat timeFormat;

    public MessageFileServiceImpl() {
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        timeFormat = new SimpleDateFormat("HH:mm:ss");
    }

    @Override
    public void writeListToFile(File path, List<Object> msgList) throws IOException {
        writer = new BufferedWriter(new FileWriter(path));
        String tmp = "";
        for (Object object : msgList) {
            tmp = tmp + object.toString() + "\n";
        }
        System.out.println(tmp);
        writer.write(tmp);
        writer.flush();
    }

    @Override
    public List<Object> readListFromFile(File path) throws MessageScriptParsingException, IOException, ParseException {
        List<Object> resultList = new ArrayList<Object>();

        reader = new BufferedReader(new FileReader(path));
        List<String> strList = getFileIntoArrayListString(reader);
        List<SailMasterMessage> sailMasterMessageList = getSailMasterMessageList(strList);

        // Iterate throug sailmastermessagelist
        for (SailMasterMessage sailMasterMessage : sailMasterMessageList) {
            // Switch Case for all kinds of actions, just used
            switch (sailMasterMessage.getType()) {
            case RPD:
                resultList.add(getRPDMessage(sailMasterMessage));
                break;
            case RAC: // / Used in MainTestfileService
                resultList.add(getRACMessage(sailMasterMessage));
                break;
            case CCG: // /Used in MainTestfileService
                resultList.add(getCCGMessage(sailMasterMessage));
                break;
            case STL: // / Used in MainTestfileService
                resultList.add(getSTLMessage(sailMasterMessage));
                break;
            case CAM: // / Used in MainTestfileService
                resultList.add(getCAMMessage(sailMasterMessage));
                break;
            case TMD: // / Used in MainTestfileService
                resultList.add(getTMDMessage(sailMasterMessage));
                break;
            default:
                throw new MessageScriptParsingException(sailMasterMessage.getMessage(), file);
            }
        }
        return resultList;
    }

    private RPDMessage getRPDMessage(SailMasterMessage message) throws ParseException {
        String[] sections = message.getSections();
        String raceID = sections[1];
        int status = Integer.valueOf(sections[2]);
        String sec3 = sections[3];
        String sdataTimeStringWithoutColon = sec3.substring(0, sec3.length() - 3)
                + sec3.substring(sec3.length() - 2, sec3.length());
        System.out.println(sdataTimeStringWithoutColon);
        final Date timePoint;
        synchronized (dateFormat) {
            timePoint = dateFormat.parse(sdataTimeStringWithoutColon);
        }
        // Date startTimeEstimatedStartTime = timeFormat.parse(sections[4]);
        final Date startTimeEstimatedStartTime;
        final Long millisecondsSinceRaceStart;
        synchronized (timeFormat) {
            startTimeEstimatedStartTime= sections[4].trim().length() == 0 ? null : timeFormat.parse(sections[4]);
            millisecondsSinceRaceStart = sections[5].trim().length() == 0 ? null : timeFormat.parse(sections[5]).getTime();
        }
        Integer nextMarkIndexForLeader = sections[6].trim().length() == 0 ? null : Integer.valueOf(sections[6]);
        Distance distanceToNextMarkForLeader = sections[7].trim().length() == 0 ? null : new MeterDistance(
                Double.valueOf(sections[7]));
        int count = Integer.valueOf(sections[8]);

        List<RacePositionDataElement> racePositionElements = new ArrayList<RacePositionDataElement>();

        for (int i = 0; i < count; i++) {
            int fixDetailIndex = 0;
            String[] fixSections = sections[9 + i].split(";");
            if (fixSections.length > 2) {
                String boatID = fixSections[fixDetailIndex++];
                int trackerTypeInt = Integer.valueOf(fixSections[fixDetailIndex++]);
                Long ageOfDataInMilliseconds = 1000l * Long.valueOf(fixSections[fixDetailIndex++]);
                double latitude = Double.parseDouble(fixSections[fixDetailIndex++]);
                double longitude = Double.parseDouble(fixSections[fixDetailIndex++]);
                Double speedOverGroundInKnots = Double.valueOf(fixSections[fixDetailIndex++]);
                Speed averageSpeedOverGround = fixSections[fixDetailIndex].trim().length() == 0 ? null
                        : new KnotSpeedImpl(Double.valueOf(fixSections[fixDetailIndex]));
                fixDetailIndex++;
                Speed velocityMadeGood = fixSections[fixDetailIndex].trim().length() == 0 ? null : new KnotSpeedImpl(
                        Double.valueOf(fixSections[fixDetailIndex]));
                fixDetailIndex++;
                SpeedWithBearing speed = new KnotSpeedWithBearingImpl(speedOverGroundInKnots, new DegreeBearingImpl(
                        Double.valueOf(fixSections[fixDetailIndex++])));

                Integer nextMarkIndex = fixSections.length <= fixDetailIndex
                        || fixSections[fixDetailIndex].trim().length() == 0 ? null : Integer
                        .valueOf(fixSections[fixDetailIndex]);
                fixDetailIndex++;
                Integer rank = fixSections.length <= fixDetailIndex || fixSections[fixDetailIndex].trim().length() == 0 ? null
                        : Integer.valueOf(fixSections[fixDetailIndex]);
                fixDetailIndex++;
                Distance distanceToLeader = fixSections.length <= fixDetailIndex
                        || fixSections[fixDetailIndex].trim().length() == 0 ? null : new MeterDistance(
                        Double.valueOf(fixSections[fixDetailIndex]));
                fixDetailIndex++;
                Distance distanceToNextMark = fixSections.length <= fixDetailIndex
                        || fixSections[fixDetailIndex].trim().length() == 0 ? null : new MeterDistance(
                        Double.valueOf(fixSections[fixDetailIndex]));
                fixDetailIndex++;

                racePositionElements.add(new RacePositionDataElement(boatID, trackerTypeInt,
                        ageOfDataInMilliseconds == null ? null : ageOfDataInMilliseconds.longValue(), 
                                latitude,
                        longitude, speedOverGroundInKnots == null ? null : speedOverGroundInKnots.doubleValue(),
                        velocityMadeGood == null ? 0.0 : velocityMadeGood.getKnots(),
                        averageSpeedOverGround == null ? 0.0 : averageSpeedOverGround.getKnots(), speed == null ? 0.0
                                : speed.getBearing().getDegrees(), nextMarkIndex == null ? 0 : nextMarkIndex
                                .intValue(), rank == null ? 0 : rank.intValue(), distanceToLeader == null ? 0
                                : distanceToLeader.getMeters(), distanceToNextMark == null ? 0 : distanceToNextMark
                                .getMeters()));
            }
        }
        return new RPDMessage(raceID, status, timePoint, startTimeEstimatedStartTime, new Date(
                millisecondsSinceRaceStart == null ? 0 : millisecondsSinceRaceStart), nextMarkIndexForLeader == null ? 0 : nextMarkIndexForLeader.intValue(),
                distanceToNextMarkForLeader == null ? 0 : distanceToNextMarkForLeader.getMeters(), racePositionElements);
    }

    private RACMessage getRACMessage(SailMasterMessage message) {
        int count = Integer.valueOf(message.getSections()[1]);
        List<Race> result = new ArrayList<Race>();
        for (int i = 0; i < count; i++) {
            String[] idAndDescription = message.getSections()[2 + i].split(";");
            result.add(new RaceImpl(idAndDescription[0], idAndDescription[1], null /* boat class */));
        }
        return new RACMessage(result);
    }

    private CCGMessage getCCGMessage(SailMasterMessage message) {
        String raceId = message.getSections()[1];
        int count = Integer.valueOf(message.getSections()[2]);
        List<Mark> marks = new ArrayList<Mark>();
        for (int i = 0; i < count; i++) {
            String[] markDetails = message.getSections()[3 + i].split(";");
            MarkType markType = null;
            final int devicesNamesStartIndex;
            if (message.getSections()[3+i].split(";", -1).length == 5) {
                // this is the SailMaster protocol version 1.0 (May 2012) or later (see bug 1000), containing
                // a MarkType specification before the two tracker IDs:
                int markTypeIndex = Integer.valueOf(markDetails[2]);
                markType = MarkType.values()[markTypeIndex];
                devicesNamesStartIndex = 3;
            } else {
                devicesNamesStartIndex = 2;
            }
            List<Serializable> markIds = Arrays.asList(markDetails).subList(devicesNamesStartIndex, markDetails.length).stream().map(idAsString->UUID.fromString(idAsString)).collect(Collectors.toList());
            marks.add(new MarkImpl(markDetails[1], Integer.valueOf(markDetails[0]), markIds, markType));
        }
        return new CCGMessage(raceId, marks);
    }

    private STLMessage getSTLMessage(SailMasterMessage message) {
        String raceId = message.getSections()[1];
        ArrayList<Competitor> competitors = new ArrayList<Competitor>();
        int count = Integer.valueOf(message.getSections()[2]);
        for (int i = 0; i < count; i++) {
            String[] competitorDetails = message.getSections()[3 + i].split(";");
            competitors.add(new CompetitorWithoutID(competitorDetails[0], competitorDetails[1], competitorDetails[2]));
        }
        return new STLMessage(raceId, competitors);
    }

    private CAMMessage getCAMMessage(SailMasterMessage message) throws ParseException {
        String raceId = message.getSections()[1];
        List<ClockAtMarkElement> result = new ArrayList<ClockAtMarkElement>();
        int count = Integer.valueOf(message.getSections()[2]);
        for (int i = 0; i < count; i++) {
            String[] clockAtMarkDetail = message.getSections()[3 + i].split(";");
            int markIndex = Integer.valueOf(clockAtMarkDetail[0]);
            final Date timePoint;
            synchronized (timeFormat) {
                timePoint = clockAtMarkDetail.length <= 1 || clockAtMarkDetail[1].trim().length() == 0 ? null
                    : timeFormat.parse(clockAtMarkDetail[1]);
            }
            result.add(new ClockAtMarkElement(markIndex, timePoint, clockAtMarkDetail.length <= 2 ? null
                    : clockAtMarkDetail[2]));
        }
        return new CAMMessage(raceId, result);
    }

    private TMDMessage getTMDMessage(SailMasterMessage message) throws ParseException {
        String raceID = message.getSections()[1];
        String boatID = message.getSections()[2];
        int count = Integer.valueOf(message.getSections()[3]);
        List<TimingDataElement> timingDataelementList = new ArrayList<TimingDataElement>();
        for (int i = 0; i < count; i++) {
            String[] details = message.getSections()[4 + i].split(";");
            Integer markIndex = details.length <= 0 || details[0].trim().length() == 0 ? null : Integer
                    .valueOf(details[0]);
            Integer rank = details.length <= 1 || details[1].trim().length() == 0 ? null : Integer.valueOf(details[1]);
            final Date timeSinceStart;
            synchronized (timeFormat) {
                timeSinceStart = details[2].trim().length() == 0 ? null : timeFormat.parse(details[2]);
            }
            timingDataelementList.add(new TimingDataElement(markIndex == null ? 0 : markIndex, rank == null ? 0 : rank, timeSinceStart));
        }
        return new TMDMessage(raceID, boatID, timingDataelementList);
    }

    private List<SailMasterMessage> getSailMasterMessageList(List<String> strList) {
        // Add Strings of stringmessageList into a ArrayList of type SailMasterMessage
        List<SailMasterMessage> sailMasterMessageList = new ArrayList<SailMasterMessage>();
        for (String s : strList) {
            sailMasterMessageList.add(new SailMasterMessageImpl(s));
        }
        return sailMasterMessageList;
    }

    private List<String> getFileIntoArrayListString(BufferedReader reader) throws IOException {
        List<String> strList = new ArrayList<String>();
        String tmp = "";
        while ((tmp = reader.readLine()) != null) {
            strList.add(tmp);
        }
        return strList;
    }
    //
    // private Date parseTimeAndDateISO(String timeAndDateISO) throws ParseException {
    // char timeZoneIndicator = timeAndDateISO.charAt(timeAndDateISO.length() - 6);
    // /*if ((timeZoneIndicator == '+' || timeZoneIndicator == '-')
    // && timeAndDateISO.charAt(timeAndDateISO.length() - 3) == ':') {
    // timeAndDateISO = timeAndDateISO.substring(0, timeAndDateISO.length() - 3)
    // + timeAndDateISO.substring(timeAndDateISO.length() - 2);
    // //lastTimeZoneSuffix = timeAndDateISO.substring(timeAndDateISO.length() - 5);
    // }*/
    // synchronized (dateFormat) {
    // return dateFormat.parse(timeAndDateISO);
    // }
    // }
    //
    // private Date parseTimePrefixedWithISOToday(String timeHHMMSS) throws ParseException {
    // synchronized (dateFormat) {
    // return dateFormat.parse(prefixTimeWithISOTodayAndSuffixWithTimezoneIndicator(timeHHMMSS));
    // }
    // }
    //
    // private String prefixTimeWithISOTodayAndSuffixWithTimezoneIndicator(String time) {
    // synchronized (dateFormat) {
    // return dateFormat.format(new Date()).substring(0, "yyyy-mm-ddT".length()) + time /*+ lastTimeZoneSuffix*/;
    // }
    // }
    //
    // private long parseHHMMSSToMilliseconds(String hhmmss) {
    // String[] timeDetail = hhmmss.split(":");
    // long millisecondsSinceStart = 1000 * (Long.valueOf(timeDetail[2]) + 60 * Long.valueOf(timeDetail[1]) + 3600 *
    // Long
    // .valueOf(timeDetail[0]));
    // return millisecondsSinceStart;
    // }

    // private Date parseHHMMSSToDate(String hhmmss){
    // String[] timeDetail = hhmmss.split(":");
    // long millisecondsSinceStart = 1000 * (Long.valueOf(timeDetail[2]) + 60 * Long.valueOf(timeDetail[1]) + 3600 *
    // Long
    // .valueOf(timeDetail[0]));
    // return new Date(millisecondsSinceStart);
    // }
}
