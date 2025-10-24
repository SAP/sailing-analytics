package com.sap.sailing.domain.racelogtracking.test.manual;

import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.racelogtracking.impl.SmartphoneImeiIdentifierImpl;
import com.sap.sailing.domain.racelogtracking.impl.SmartphoneImeiJsonHandler;
import com.sap.sailing.domain.racelogtracking.test.AbstractJsonOverHttpTest;
import com.sap.sailing.server.gateway.serialization.impl.DeviceAndSessionIdentifierWithGPSFixesSerializer;
import com.sap.sailing.server.gateway.serialization.impl.DeviceIdentifierJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.GPSFixMovingJsonSerializer;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class PostFixes extends AbstractJsonOverHttpTest {
    protected static final SmartphoneImeiIdentifierImpl device = new SmartphoneImeiIdentifierImpl("a");

    private final DeviceAndSessionIdentifierWithGPSFixesSerializer<SmartphoneImeiIdentifierImpl, GPSFixMoving> fixSerializer =
            new DeviceAndSessionIdentifierWithGPSFixesSerializer<>(DeviceIdentifierJsonSerializer.create(
                    new SmartphoneImeiJsonHandler(), SmartphoneImeiIdentifierImpl.TYPE), new GPSFixMovingJsonSerializer());

            private void recordFix(GPSFixMoving... fix) throws IOException {
                String request = fixSerializer.serialize(
                        new Util.Triple<SmartphoneImeiIdentifierImpl, Serializable, List<GPSFixMoving>>(
                                device, null, Arrays.asList(fix))).toJSONString();

                executeRequest("POST", getUrl(URL_TR + "/recordFixes"), request);
            }

            private void recordFixes() {
                final Timer timer = new Timer();
                final Position p1 = new DegreePosition(54.427119, 10.182583);
                final Position p2 = new DegreePosition(54.435707, 10.200779);
                final Position p3 = new DegreePosition(54.425921, 10.188248);

                final int nFirstLeg = 100;
                final int nSecondLeg = 100;
                final int n = nFirstLeg + nSecondLeg;
                long period = 1000;

                timer.scheduleAtFixedRate(new TimerTask() {
                    int i = 0;
                    GPSFix lastFix;

                    @Override
                    public void run() {
                        if (i++ > n)
                            timer.cancel();

                        Position start;
                        Position end;
                        int iLeg = i;
                        int nLeg;

                        if (i < nFirstLeg) {
                            start = p1;
                            end = p2;
                            nLeg = nFirstLeg;
                        } else {
                            start = p2;
                            end = p3;
                            iLeg -= nFirstLeg;
                            nLeg = nSecondLeg;
                        }

                        Distance distance = start.getDistance(end);
                        Bearing bearing = start.getBearingGreatCircle(end);
                        Position boat = start.translateGreatCircle(bearing, distance.scale(((double) iLeg) / nLeg));
                        GPSFix fix = new GPSFixImpl(boat, MillisecondsTimePoint.now());
                        SpeedWithBearing speed = lastFix == null ? new KnotSpeedWithBearingImpl(0, new DegreeBearingImpl(0))
                        : lastFix.getSpeedAndBearingRequiredToReach(fix);
                        lastFix = fix;

                        try {
                            recordFix(new GPSFixMovingImpl(fix.getPosition(), fix.getTimePoint(), speed, /* optionalTrueHeading */ null));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }, 0, period);
            }

            public void run() throws IOException, IllegalStateException, ParseException, InterruptedException {
                recordFixes();
            }

            public static void main(String[] args) throws IllegalStateException, IOException, ParseException,
            InterruptedException {
                new PostFixes().run();
            }
}
