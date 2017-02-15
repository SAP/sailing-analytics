package com.sap.sailing.server.trackfiles.test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.domain.common.sensordata.BravoSensorDataMetadata;
import com.sap.sailing.domain.common.tracking.DoubleVectorFix;
import com.sap.sailing.domain.common.tracking.impl.BravoFixImpl;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifier;
import com.sap.sailing.domain.trackimport.DoubleVectorFixImporter;
import com.sap.sailing.domain.trackimport.FormatNotSupportedException;
import com.sap.sailing.server.trackfiles.impl.BravoDataImporterImpl;
import com.sap.sailing.server.trackfiles.impl.doublefix.DownsamplerTo1HzProcessor;
import com.sap.sailing.server.trackfiles.impl.doublefix.LearningBatchProcessor;

public class BravoDataImportTest {
    
    private LearningBatchProcessor batchProcessor;
    private DownsamplerTo1HzProcessor downsampler;

    private final DoubleVectorFixImporter bravoDataImporter = new BravoDataImporterImpl() {
        protected com.sap.sailing.server.trackfiles.impl.doublefix.DoubleFixProcessor createProcessor(
                BravoSensorDataMetadata metadata,
                DoubleVectorFixImporter.Callback callback,
                TrackFileImportDeviceIdentifier trackIdentifier) {
            batchProcessor = new LearningBatchProcessor(5000, 5000, callback, trackIdentifier);
            downsampler = new DownsamplerTo1HzProcessor(metadata.trackColumnCount, batchProcessor);
            return downsampler;
        };
    };
    private int callbackCallCount = 0;
    private double sumRideHeightInMeters = 0.0;
    
    @Before
    public void setUp() {
        this.callbackCallCount = 0;
        this.sumRideHeightInMeters = 0.0;
    }
    
    @Test
    public void testFileImport() throws FormatNotSupportedException, IOException {
        testImport(ImportData.FILE_UNDEFINED_RACE_BRAVO);
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
    
    private void testImport(ImportData importData) throws FormatNotSupportedException, IOException {

        bravoDataImporter.importFixes(importData.getInputStream(), (fixes, device) -> {
            for (DoubleVectorFix fix : fixes) {
                callbackCallCount++;
                sumRideHeightInMeters += new BravoFixImpl(fix).getRideHeight().getMeters();
            }
        }, "filename", "source");
        Assert.assertEquals(importData.expectedFixesCount, downsampler.getCountSourceTtl());
        Assert.assertEquals(importData.expectedFixesConsolidated, downsampler.getCountImportedTtl());
        Assert.assertEquals(importData.expectedFixesConsolidated, callbackCallCount);
    }
    
    private enum ImportData {
        // find out the number of fixes using the following bash line:
        //    tail -n +5 Undefined\ Race\ -\ BRAVO.txt | awk '{if ($5<$6) print v=$5; else v=$6; sum+=v; count++; } END {print "Sum: ", sum, "Count: ", count, "Average:", sum/count;}'
        FILE_UNDEFINED_RACE_BRAVO(870, 89) {
            @Override
            protected InputStream getInputStream() {
                return getClass().getResourceAsStream("/Undefined Race - BRAVO.txt");
            }
        },
        DUMMY_DEFAULT_HEADER_NO_DATA(0, 0) {
            @Override
            protected InputStream getInputStream() {
                return new ByteArrayInputStream(HEADER_ORDER_DEFAULT.getBytes(StandardCharsets.UTF_8));
            }
        },
        DUMMY_DEAFULT_HEADER_ONE_LINE(1, 1) {
            @Override
            protected InputStream getInputStream() {
                return new ByteArrayInputStream((HEADER_ORDER_DEFAULT + DUMMY_CONTENT).getBytes(StandardCharsets.UTF_8));
            }
        },
        DUMMY_RANDOM_HAEDER_ONE_LINE(1, 1) {
            @Override
            protected InputStream getInputStream() {
                return new ByteArrayInputStream((HEADER_ORDER_RANDOM + DUMMY_CONTENT).getBytes(StandardCharsets.UTF_8));
            }
        };
        
        private final int expectedFixesCount;
        private final int expectedFixesConsolidated;

        private ImportData(int expectedFixesCount, int expectedFixesConsolidated) {
            this.expectedFixesCount = expectedFixesCount;
            this.expectedFixesConsolidated = expectedFixesConsolidated;
        }
        
        protected abstract InputStream getInputStream();
    }
    
    private static final String HEADER_ORDER_DEFAULT = "jjlDATE\tjjlTIME\tEpoch\tRideHeight\tRideHeightPortHull\t"
            + "RideHeightStbdHull\tHeel\tTrim\tImuSensor_GyroX\tImuSensor_GyroY\tImuSensor_GyroZ\tImuSensor_Pitch\t"
            + "ImuSensor_Roll\tImuSensor_Yaw\tImuSensor_LinearAccX\tImuSensor_LinearAccY\tImuSensor_LinearAccZ\t"
            + "Hb_Z\tDn_Z\tDb_Z\tLKF_ride_hgh\tLKF_ride_hgh_Position\tLKF_ride_hgh_Velocity\t"
            + "LKF_ride_hgh_Acceleration\tLKF_ride_hgh_PositionError\tLKF_ride_hgh_VelocityError\t"
            + "LKF_ride_hgh_AccelerationError\tGps_Gga_PosFixTime\tGps_Gga_Lat\tGps_Gga_Lon\tGps_Gga_QI\t"
            + "Gps_Gga_HDOP\tGps_Gga_AntHeight\tGps_Vtg_TMG\tGps_Vtg_SOGKnots\tGps_Rmc_MagVar\t"
            + "Gps_EastVelocity\tGps_NorthVelocity\tGps_UpVelocity\tBravoNet_Node0x0A0_Ch0\t"
            + "BravoNet_Node0x0A0_Voltage\tBravoNet_Node0x0A0_Temperature\tBravoNet_Node0x0A0_Current\n";
    
    private static final String HEADER_ORDER_RANDOM = "jjlDATE\tjjlTIME\tEpoch\tRideHeightPortHull\t"
            + "ImuSensor_Roll\tImuSensor_Yaw\tImuSensor_LinearAccX\tImuSensor_LinearAccY\tImuSensor_LinearAccZ\t"
            + "RideHeightStbdHull\tHeel\tTrim\tImuSensor_GyroX\tImuSensor_GyroY\tImuSensor_GyroZ\tImuSensor_Pitch\t"
            + "Gps_EastVelocity\tGps_NorthVelocity\tGps_UpVelocity\tBravoNet_Node0x0A0_Ch0\tRideHeight\t"
            + "LKF_ride_hgh_Acceleration\tLKF_ride_hgh_PositionError\tLKF_ride_hgh_VelocityError\t"
            + "Hb_Z\tDn_Z\tDb_Z\tLKF_ride_hgh\tLKF_ride_hgh_Position\tLKF_ride_hgh_Velocity\t"
            + "BravoNet_Node0x0A0_Voltage\tBravoNet_Node0x0A0_Temperature\tBravoNet_Node0x0A0_Current\t"
            + "LKF_ride_hgh_AccelerationError\tGps_Gga_PosFixTime\tGps_Gga_Lat\tGps_Gga_Lon\tGps_Gga_QI\t"
            + "Gps_Gga_HDOP\tGps_Gga_AntHeight\tGps_Vtg_TMG\tGps_Vtg_SOGKnots\tGps_Rmc_MagVar\n";
    
    private static final String DUMMY_CONTENT = "20160409.000000\t222806.293147\t1460240885279.000000\t0.771505\t"
            + "0.661044\t0.881965\t2.146488\t-10.559542\t0.019611\t0.092727\t-0.291055\t-10.559542\t-2.146488\t"
            + "354.122419\t-0.009347\t-0.004219\t-0.015309\t-0.673131\t-0.757522\t-0.743131\t0.013457\t-0.771505\t"
            + "-0.014370\t-0.006752\t0.029250\t0.023496\t0.014289\t212806.200000\t5232.274010\t1325.491430\t"
            + "1.000000\t1.200000\t73.100000\t306.100000\t0.060000\t3.200000\t-0.020000\t0.010000\t-0.000000\t"
            + "0.733643\t24.000978\t36.640236\t0.054741\n";

}
