package com.sap.sailing.domain.igtimiadapter;

import com.igtimi.IgtimiData.Acceleration;
import com.igtimi.IgtimiData.Angle;
import com.igtimi.IgtimiData.ApparentWindAngle;
import com.igtimi.IgtimiData.ApparentWindSpeed;
import com.igtimi.IgtimiData.Boolean;
import com.igtimi.IgtimiData.Command;
import com.igtimi.IgtimiData.Corrections;
import com.igtimi.IgtimiData.CourseOverGround;
import com.igtimi.IgtimiData.DataPoint;
import com.igtimi.IgtimiData.ElectricalCurrent;
import com.igtimi.IgtimiData.ElectricalPotential;
import com.igtimi.IgtimiData.Event;
import com.igtimi.IgtimiData.File;
import com.igtimi.IgtimiData.Force;
import com.igtimi.IgtimiData.Frequency;
import com.igtimi.IgtimiData.GNSS_Position;
import com.igtimi.IgtimiData.GNSS_Quality;
import com.igtimi.IgtimiData.GNSS_Sat_Count;
import com.igtimi.IgtimiData.Heading;
import com.igtimi.IgtimiData.HeadingMagnetic;
import com.igtimi.IgtimiData.HeartRate;
import com.igtimi.IgtimiData.Integer;
import com.igtimi.IgtimiData.JSON;
import com.igtimi.IgtimiData.Length;
import com.igtimi.IgtimiData.Log;
import com.igtimi.IgtimiData.Number;
import com.igtimi.IgtimiData.Orientation;
import com.igtimi.IgtimiData.Power;
import com.igtimi.IgtimiData.Pressure;
import com.igtimi.IgtimiData.SessionLogEntry;
import com.igtimi.IgtimiData.Speed;
import com.igtimi.IgtimiData.SpeedOverGround;
import com.igtimi.IgtimiData.SpeedThroughWater;
import com.igtimi.IgtimiData.Temperature;
import com.igtimi.IgtimiData.Text;
import com.igtimi.IgtimiData.TimeInterval;
import com.igtimi.IgtimiData.Torque;
import com.igtimi.IgtimiData.TrueWindDirection;
import com.igtimi.IgtimiData.TrueWindSpeed;

public interface DataPointVisitor<ResultType> {
    static <ResultType> ResultType accept(DataPoint dataPoint, DataPointVisitor<ResultType> visitor) {
        switch (dataPoint.getDataCase()) {
        case ACC:
            return visitor.handleAcc(dataPoint.getAcc());
        case AMP:
            return visitor.handleAmp(dataPoint.getAmp());
        case ANG:
            return visitor.handleAng(dataPoint.getAng());
        case AWA:
            return visitor.handleAwa(dataPoint.getAwa());
        case AWS:
            return visitor.handleAws(dataPoint.getAws());
        case BOOL:
            return visitor.handleBool(dataPoint.getBool());
        case CMD:
            return visitor.handleCmd(dataPoint.getCmd());
        case COG:
            return visitor.handleCog(dataPoint.getCog());
        case COR:
            return visitor.handleCor(dataPoint.getCor());
        case DATA_NOT_SET: // ignore message
        case EVENT:
            return visitor.handleEvent(dataPoint.getEvent());
        case FILE:
            return visitor.handleFile(dataPoint.getFile());
        case FOR:
            return visitor.handleFor(dataPoint.getFor());
        case FREQ:
            return visitor.handleFreq(dataPoint.getFreq());
        case HDG:
            return visitor.handleHdg(dataPoint.getHdg());
        case HDGM:
            return visitor.handleHdgm(dataPoint.getHdgm());
        case HR:
            return visitor.handleHr(dataPoint.getHr());
        case INT:
            return visitor.handleInt(dataPoint.getInt());
        case JSON:
            return visitor.handleJson(dataPoint.getJson());
        case LEN:
            return visitor.handleLen(dataPoint.getLen());
        case LOG:
            return visitor.handleLog(dataPoint.getLog());
        case NUM:
            return visitor.handleNum(dataPoint.getNum());
        case ORI:
            return visitor.handleOri(dataPoint.getOri());
        case POS:
            return visitor.handlePos(dataPoint.getPos());
        case PRESS:
            return visitor.handlePress(dataPoint.getPress());
        case PWR:
            return visitor.handlePwr(dataPoint.getPwr());
        case SATC:
            return visitor.handleSatc(dataPoint.getSatc());
        case SATQ:
            return visitor.handleSatq(dataPoint.getSatq());
        case SLE:
            return visitor.handleSle(dataPoint.getSle());
        case SOG:
            return visitor.handleSog(dataPoint.getSog());
        case SPD:
            return visitor.handleSpd(dataPoint.getSpd());
        case STW:
            return visitor.handleStw(dataPoint.getStw());
        case TEMP:
            return visitor.handleTemp(dataPoint.getTemp());
        case TIME:
            return visitor.handleTime(dataPoint.getTime());
        case TORQ:
            return visitor.handleTorq(dataPoint.getTorq());
        case TWD:
            return visitor.handleTwd(dataPoint.getTwd());
        case TWS:
            return visitor.handleTws(dataPoint.getTws());
        case TXT:
            return visitor.handleTxt(dataPoint.getTxt());
        case VOLT:
            return visitor.handleVolt(dataPoint.getVolt());
        }
        throw new RuntimeException("Internal error; shouldn't have reached here; found unknown data case "+dataPoint.getDataCase());
    }

    default ResultType handleAcc(Acceleration acc) {
        return null;
    }

    default ResultType handleAmp(ElectricalCurrent amp) {
        return null;
    }

    default ResultType handleAng(Angle ang) {
        return null;
    }

    default ResultType handleAwa(ApparentWindAngle awa) {
        return null;
    }

    default ResultType handleAws(ApparentWindSpeed aws) {
        return null;
    }

    default ResultType handleBool(Boolean bool) {
        return null;
    }

    default ResultType handleCmd(Command cmd) {
        return null;
    }

    default ResultType handleCog(CourseOverGround cog) {
        return null;
    }

    default ResultType handleCor(Corrections cor) {
        return null;
    }

    default ResultType handleEvent(Event event) {
        return null;
    }

    default ResultType handleFile(File file) {
        return null;
    }

    default ResultType handleFor(Force for1) {
        return null;
    }

    default ResultType handleFreq(Frequency freq) {
        return null;
    }

    default ResultType handleHdg(Heading hdg) {
        return null;
    }

    default ResultType handleHdgm(HeadingMagnetic hdgm) {
        return null;
    }

    default ResultType handleHr(HeartRate hr) {
        return null;
    }

    default ResultType handleInt(Integer int1) {
        return null;
    }

    default ResultType handleJson(JSON json) {
        return null;
    }

    default ResultType handleLen(Length len) {
        return null;
    }

    default ResultType handleLog(Log log) {
        return null;
    }

    default ResultType handleNum(Number num) {
        return null;
    }

    default ResultType handleOri(Orientation ori) {
        return null;
    }

    default ResultType handlePos(GNSS_Position pos) {
        return null;
    }

    default ResultType handlePress(Pressure press) {
        return null;
    }

    default ResultType handlePwr(Power pwr) {
        return null;
    }

    default ResultType handleSatc(GNSS_Sat_Count satc) {
        return null;
    }

    default ResultType handleSatq(GNSS_Quality satq) {
        return null;
    }

    default ResultType handleSle(SessionLogEntry sle) {
        return null;
    }

    default ResultType handleSog(SpeedOverGround sog) {
        return null;
    }

    default ResultType handleSpd(Speed spd) {
        return null;
    }

    default ResultType handleStw(SpeedThroughWater stw) {
        return null;
    }

    default ResultType handleTemp(Temperature temp) {
        return null;
    }

    default ResultType handleTime(TimeInterval time) {
        return null;
    }

    default ResultType handleTorq(Torque torq) {
        return null;
    }

    default ResultType handleTwd(TrueWindDirection twd) {
        return null;
    }

    default ResultType handleTws(TrueWindSpeed tws) {
        return null;
    }

    default ResultType handleTxt(Text txt) {
        return null;
    }

    default ResultType handleVolt(ElectricalPotential volt) {
        return null;
    }

}
