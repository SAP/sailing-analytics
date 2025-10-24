package com.sap.sailing.server.trackfiles.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.sensordata.BravoExtendedSensorDataMetadata;
import com.sap.sailing.domain.common.tracking.BravoExtendedFix;
import com.sap.sailing.domain.common.tracking.BravoFix;
import com.sap.sailing.domain.common.tracking.DoubleVectorFix;
import com.sap.sailing.domain.common.tracking.impl.BravoExtendedFixImpl;
import com.sap.sailing.domain.trackimport.FormatNotSupportedException;
import com.sap.sailing.domain.tracking.DynamicBravoFixTrack;
import com.sap.sailing.domain.tracking.impl.BravoFixTrackImpl;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class BravoExtendedDataImportTest extends AbstractBravoDataImportTest {
    
    @Override
    protected Map<String, Integer> getColumnData() {
        return BravoExtendedSensorDataMetadata.getColumnNamesToIndexInDoubleFix();
    }
    
    @Override
    protected int getTrackColumnCount() {
        return BravoExtendedSensorDataMetadata.getTrackColumnCount();
    }
    
    @Test
    public void testDefaultHeaderNoDataImport() throws FormatNotSupportedException, IOException {
        testImport(ImportData.DUMMY_DEFAULT_HEADER_NO_DATA);
    }
    
    @Test
    public void testDefaultHeaderOneLineImport() throws FormatNotSupportedException, IOException {
        testImport(ImportData.DUMMY_DEAFULT_HEADER_ONE_LINE);
    }
    
    @Test
    public void testRandomHeaderOneLineImport() throws FormatNotSupportedException, IOException {
        testImport(ImportData.DUMMY_RANDOM_HAEDER_ONE_LINE);
    }
    
    @Test
    public void testReadingTwoLinesAndInterpolating() throws FormatNotSupportedException, IOException {
        DynamicBravoFixTrack<Competitor> track = new BravoFixTrackImpl<>(null, "Test Track", /* hasExtendedFixes */ true);
        try (final ByteArrayInputStream inputStream = new ByteArrayInputStream((
                HEADER_ORDER_DEFAULT+DUMMY_CONTENT+DUMMY_CONTENT2).getBytes(StandardCharsets.UTF_8))) {
            bravoDataImporter.importFixes(inputStream,
                    Charset.defaultCharset(), (fixes, device) -> {
            for (DoubleVectorFix fix : fixes) {
            track.add(new BravoExtendedFixImpl(fix));
            }
         }, "filename", "source", /* downsample */ true);
            final TimePoint t1 = new MillisecondsTimePoint(1500648308500l);
            final TimePoint t2 = new MillisecondsTimePoint(1500648309500l);
            final BravoExtendedFix bef1, bef2;
            track.lockForRead();
            try {
                final Iterator<BravoFix> i = track.getFixes().iterator();
                bef1 = (BravoExtendedFix) i.next();
                bef2 = (BravoExtendedFix) i.next();
            } finally {
                track.unlockAfterRead();
            }
            assertEquals(t1, bef1.getTimePoint());
            assertEquals(t2, bef2.getTimePoint());
            assertEquals(0.915471, bef1.getPortRudderRake(), 0.00001);
            assertEquals(0.913729, bef2.getPortRudderRake(), 0.00001);
            assertEquals(0.915471, track.getPortRudderRakeIfAvailable(t1), 0.00001);
            assertEquals(0.913729, track.getPortRudderRakeIfAvailable(t2), 0.00001);
            assertEquals((0.915471+0.913729)/2., track.getPortRudderRakeIfAvailable(t1.plus(t1.until(t2).divide(2.))), 0.00001);
            assertEquals(new DegreeBearingImpl(-46.507845).getDegrees(), track.getMastRotationIfAvailable(t1).getDegrees(), 0.00001);
            assertEquals(new DegreeBearingImpl(-46.845317).getDegrees(), track.getMastRotationIfAvailable(t2).getDegrees(), 0.00001);
            assertEquals(new DegreeBearingImpl((-46.507845-46.845317)/2.).getDegrees(), track.getMastRotationIfAvailable(t1.plus(t1.until(t2).divide(2.))).getDegrees(), 0.00001);
        }
    }
    
    private enum ImportData implements ImportDataDefinition {
        DUMMY_DEFAULT_HEADER_NO_DATA(0, 0) {
            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream(HEADER_ORDER_DEFAULT.getBytes(StandardCharsets.UTF_8));
            }
        },
        DUMMY_DEAFULT_HEADER_ONE_LINE(1, 1) {
            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream((HEADER_ORDER_DEFAULT + DUMMY_CONTENT).getBytes(StandardCharsets.UTF_8));
            }
        },
        DUMMY_RANDOM_HAEDER_ONE_LINE(1, 1) {
            @Override
            public InputStream getInputStream() {
                return new ByteArrayInputStream((HEADER_ORDER_RANDOM + DUMMY_CONTENT).getBytes(StandardCharsets.UTF_8));
            }
        };
        
        private final int expectedFixesCount;
        private final int expectedFixesConsolidated;

        private ImportData(int expectedFixesCount, int expectedFixesConsolidated) {
            this.expectedFixesCount = expectedFixesCount;
            this.expectedFixesConsolidated = expectedFixesConsolidated;
        }
        
        @Override
        public int getExpectedFixesCount() {
            return expectedFixesCount;
        }
        
        @Override
        public int getExpectedFixesConsolidated() {
            return expectedFixesConsolidated;
        }
    }
    
    private static final String HEADER_ORDER_DEFAULT = "jjlDATE\tjjlTIME\tEpoch\tMagneticVariation\tCurrentSpeed\t"
            + "CurrentDirection\tWindGradient\tLat\tLon\tCOG\tSOG\tHdg\tHeel\tTrim\tFlightPathAngle\tRollRate\t"
            + "PitchRate\tYawRate\tAccelX\tAccelY\tAccelZ\tHeaveSpeed\tHeave\tRideHeightPort\tRideHeightStbd\t"
            + "RideHeight\tBS\tLwy\tCse\tSOW\tAWA\tAWS\tTWA\tTWS\tTWD\tUpwashAngle\tUpwashSpeed\tAdjWindAngle\t"
            + "AdjWindSpeed\tVMG\tMastRotation\tDaggerBoardRakeAnglePort\tRudderRakeAnglePort\tDaggerBoardRakeAngleStbd\t"
            + "RudderRakeAngleStbd\tTargetTWA\tTargetVMG\tTargetBS\tPolarBS\tBsPercentage\tVmgPercentage\t\n";
    
    private static final String HEADER_ORDER_RANDOM = "jjlDATE\tjjlTIME\tEpoch\tMagneticVariation\tCurrentSpeed\t"
            + "CurrentDirection\tWindGradient\tLat\tLon\tCOG\tSOG\tHdg\tHeel\tTrim\tFlightPathAngle\tRollRate\t"
            + "AdjWindSpeed\tVMG\tMastRotation\tDaggerBoardRakeAnglePort\tRudderRakeAnglePort\tDaggerBoardRakeAngleStbd\t"
            + "RideHeight\tBS\tLwy\tCse\tSOW\tAWA\tAWS\tTWA\tTWS\tTWD\tUpwashAngle\tUpwashSpeed\tAdjWindAngle\t"
            + "PitchRate\tYawRate\tAccelX\tAccelY\tAccelZ\tHeaveSpeed\tHeave\tRideHeightPort\tRideHeightStbd\t"
            + "RudderRakeAngleStbd\tTargetTWA\tTargetVMG\tTargetBS\tPolarBS\tBsPercentage\tVmgPercentage\t\n";
    
    private static final String DUMMY_CONTENT = "20170721.000000\t144509.020891\t1500648308988.000000\t0.000000\t"
            + "0.000000\t0.000000\t0.100000\t4121.750774\t211.855241\t267.200000\t12.108000\t274.401591\t8.293443\t"
            + "1.381434\t-0.607992\t4.466850\t2.295866\t4.974762\t0.021349\t0.049948\t-0.003009\t-0.059050\t0.564561\t"
            + "-0.142641\t0.661195\t0.640803\t11.024831\t4.831789\t274.531789\t11.415373\t-28.235512\t12.815985\t"
            + "-88.908514\t6.063496\t183.968325\t0.000000\t0.000000\t0.000000\t0.000000\t-0.080389\t-46.507845\t"
            + "-0.301864\t0.915471\t0.737711\t5.518818\t60.000000\t4.049104\t8.098208\t11.025103\t134.646042\t0.684715\t\n";
    private static final String DUMMY_CONTENT2 = "20170721.000000\t144509.118053\t1500648309088.000000\t0.000000\t0.000000\t"
            + "0.000000\t0.100000\t4121.750782\t211.854789\t270.700000\t12.176000\t274.577803\t8.372667\t1.532401\t-0.512621\t"
            + "4.513550\t2.952788\t5.778569\t0.009470\t0.074722\t-0.023186\t-0.053617\t0.575403\t-0.142008\t0.680177\t0.661195\t"
            + "11.118556\t7.659780\t274.859780\t11.461121\t-28.065986\t12.819678\t-88.995990\t6.032477\t183.831801\t0.000000\t"
            + "0.000000\t0.000000\t0.000000\t-0.112260\t-46.845317\t-0.301864\t0.913729\t0.738945\t5.526500\t60.000000\t4.031748\t"
            + "8.063496\t10.993632\t136.139139\t1.985348\n";
}
