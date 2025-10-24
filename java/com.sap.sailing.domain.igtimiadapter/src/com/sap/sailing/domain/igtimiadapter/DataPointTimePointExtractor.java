package com.sap.sailing.domain.igtimiadapter;

import com.igtimi.IgtimiData.Acceleration;
import com.igtimi.IgtimiData.Angle;
import com.igtimi.IgtimiData.ApparentWindAngle;
import com.igtimi.IgtimiData.ApparentWindSpeed;
import com.igtimi.IgtimiData.Boolean;
import com.igtimi.IgtimiData.Command;
import com.igtimi.IgtimiData.Corrections;
import com.igtimi.IgtimiData.CourseOverGround;
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
import com.sap.sse.common.TimePoint;

public class DataPointTimePointExtractor implements DataPointVisitor<TimePoint> {
    @Override
    public TimePoint handleAcc(Acceleration acc) {
        return TimePoint.of(acc.getTimestamp());
    }

    @Override
    public TimePoint handleAmp(ElectricalCurrent amp) {
        return TimePoint.of(amp.getTimestamp());
    }

    @Override
    public TimePoint handleAng(Angle ang) {
        return TimePoint.of(ang.getTimestamp());
    }

    @Override
    public TimePoint handleAwa(ApparentWindAngle awa) {
        return TimePoint.of(awa.getTimestamp());
    }

    @Override
    public TimePoint handleAws(ApparentWindSpeed aws) {
        return TimePoint.of(aws.getTimestamp());
    }

    @Override
    public TimePoint handleBool(Boolean bool) {
        return TimePoint.of(bool.getTimestamp());
    }

    @Override
    public TimePoint handleCmd(Command cmd) {
        return TimePoint.of(cmd.getTimestamp());
    }

    @Override
    public TimePoint handleCog(CourseOverGround cog) {
        return TimePoint.of(cog.getTimestamp());
    }

    @Override
    public TimePoint handleCor(Corrections cor) {
        return TimePoint.of(cor.getTimestamp());
    }

    @Override
    public TimePoint handleEvent(Event event) {
        return TimePoint.of(event.getTimestamp());
    }

    @Override
    public TimePoint handleFile(File file) {
        return TimePoint.of(file.getTimestamp());
    }

    @Override
    public TimePoint handleFor(Force for1) {
        return TimePoint.of(for1.getTimestamp());
    }

    @Override
    public TimePoint handleFreq(Frequency freq) {
        return TimePoint.of(freq.getTimestamp());
    }

    @Override
    public TimePoint handleHdg(Heading hdg) {
        return TimePoint.of(hdg.getTimestamp());
    }

    @Override
    public TimePoint handleHdgm(HeadingMagnetic hdgm) {
        return TimePoint.of(hdgm.getTimestamp());
    }

    @Override
    public TimePoint handleHr(HeartRate hr) {
        return TimePoint.of(hr.getTimestamp());
    }

    @Override
    public TimePoint handleInt(Integer int1) {
        return TimePoint.of(int1.getTimestamp());
    }

    @Override
    public TimePoint handleJson(JSON json) {
        return TimePoint.of(json.getTimestamp());
    }

    @Override
    public TimePoint handleLen(Length len) {
        return TimePoint.of(len.getTimestamp());
    }

    @Override
    public TimePoint handleLog(Log log) {
        return TimePoint.of(log.getTimestamp());
    }

    @Override
    public TimePoint handleNum(Number num) {
        return TimePoint.of(num.getTimestamp());
    }

    @Override
    public TimePoint handleOri(Orientation ori) {
        return TimePoint.of(ori.getTimestamp());
    }

    @Override
    public TimePoint handlePos(GNSS_Position pos) {
        return TimePoint.of(pos.getTimestamp());
    }

    @Override
    public TimePoint handlePress(Pressure press) {
        return TimePoint.of(press.getTimestamp());
    }

    @Override
    public TimePoint handlePwr(Power pwr) {
        return TimePoint.of(pwr.getTimestamp());
    }

    @Override
    public TimePoint handleSatc(GNSS_Sat_Count satc) {
        return TimePoint.of(satc.getTimestamp());
    }

    @Override
    public TimePoint handleSatq(GNSS_Quality satq) {
        return TimePoint.of(satq.getTimestamp());
    }

    @Override
    public TimePoint handleSle(SessionLogEntry sle) {
        return TimePoint.of(sle.getTimestamp());
    }

    @Override
    public TimePoint handleSog(SpeedOverGround sog) {
        return TimePoint.of(sog.getTimestamp());
    }

    @Override
    public TimePoint handleSpd(Speed spd) {
        return TimePoint.of(spd.getTimestamp());
    }

    @Override
    public TimePoint handleStw(SpeedThroughWater stw) {
        return TimePoint.of(stw.getTimestamp());
    }

    @Override
    public TimePoint handleTemp(Temperature temp) {
        return TimePoint.of(temp.getTimestamp());
    }

    @Override
    public TimePoint handleTime(TimeInterval time) {
        return TimePoint.of(time.getTimestamp());
    }

    @Override
    public TimePoint handleTorq(Torque torq) {
        return TimePoint.of(torq.getTimestamp());
    }

    @Override
    public TimePoint handleTwd(TrueWindDirection twd) {
        return TimePoint.of(twd.getTimestamp());
    }

    @Override
    public TimePoint handleTws(TrueWindSpeed tws) {
        return TimePoint.of(tws.getTimestamp());
    }

    @Override
    public TimePoint handleTxt(Text txt) {
        return TimePoint.of(txt.getTimestamp());
    }

    @Override
    public TimePoint handleVolt(ElectricalPotential volt) {
        return TimePoint.of(volt.getTimestamp());
    }

}
