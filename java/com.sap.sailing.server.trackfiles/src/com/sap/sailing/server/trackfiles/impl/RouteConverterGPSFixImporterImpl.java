package com.sap.sailing.server.trackfiles.impl;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

import com.sap.sailing.domain.base.impl.KilometersPerHourSpeedWithBearingImpl;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

import slash.common.type.CompactCalendar;
import slash.navigation.base.BaseNavigationPosition;
import slash.navigation.base.NavigationFormat;
import slash.navigation.base.NavigationFormats;
import slash.navigation.base.Wgs84Position;
import slash.navigation.bcr.MTP0607Format;
import slash.navigation.bcr.MTP0809Format;
import slash.navigation.copilot.CoPilot6Format;
import slash.navigation.copilot.CoPilot7Format;
import slash.navigation.copilot.CoPilot8Format;
import slash.navigation.copilot.CoPilot9Format;
import slash.navigation.fpl.GarminFlightPlanFormat;
import slash.navigation.gopal.GoPal3RouteFormat;
import slash.navigation.gopal.GoPal5RouteFormat;
import slash.navigation.gpx.BrokenGpx10Format;
import slash.navigation.gpx.BrokenGpx11Format;
import slash.navigation.gpx.Gpx10Format;
import slash.navigation.gpx.Gpx11Format;
import slash.navigation.itn.TomTom5RouteFormat;
import slash.navigation.itn.TomTom8RouteFormat;
import slash.navigation.klicktel.KlickTelRouteFormat;
import slash.navigation.kml.BrokenKml21Format;
import slash.navigation.kml.BrokenKml21LittleEndianFormat;
import slash.navigation.kml.BrokenKml22BetaFormat;
import slash.navigation.kml.BrokenKml22Format;
import slash.navigation.kml.BrokenKmz21Format;
import slash.navigation.kml.BrokenKmz21LittleEndianFormat;
import slash.navigation.kml.Igo8RouteFormat;
import slash.navigation.kml.Kml20Format;
import slash.navigation.kml.Kml21Format;
import slash.navigation.kml.Kml22BetaFormat;
import slash.navigation.kml.Kml22Format;
import slash.navigation.kml.Kmz20Format;
import slash.navigation.kml.Kmz21Format;
import slash.navigation.kml.Kmz22BetaFormat;
import slash.navigation.kml.Kmz22Format;
import slash.navigation.lmx.NokiaLandmarkExchangeFormat;
import slash.navigation.mm.MagicMaps2GoFormat;
import slash.navigation.mm.MagicMapsIktFormat;
import slash.navigation.mm.MagicMapsPthFormat;
import slash.navigation.nmea.BrokenNmeaFormat;
import slash.navigation.nmea.MagellanExploristFormat;
import slash.navigation.nmea.MagellanRouteFormat;
import slash.navigation.nmea.NmeaFormat;
import slash.navigation.nmn.NavigatingPoiWarnerFormat;
import slash.navigation.nmn.Nmn4Format;
import slash.navigation.nmn.Nmn5Format;
import slash.navigation.nmn.Nmn6FavoritesFormat;
import slash.navigation.nmn.Nmn6Format;
import slash.navigation.nmn.Nmn7Format;
import slash.navigation.nmn.NmnRouteFormat;
import slash.navigation.ovl.OvlFormat;
import slash.navigation.simple.BrokenHaicomLoggerFormat;
import slash.navigation.simple.BrokenNavilinkFormat;
import slash.navigation.simple.ColumbusV900ProfessionalFormat;
import slash.navigation.simple.ColumbusV900StandardFormat;
import slash.navigation.simple.GlopusFormat;
import slash.navigation.simple.GoRiderGpsFormat;
import slash.navigation.simple.GpsTunerFormat;
import slash.navigation.simple.GroundTrackFormat;
import slash.navigation.simple.HaicomLoggerFormat;
import slash.navigation.simple.Iblue747Format;
import slash.navigation.simple.KienzleGpsFormat;
import slash.navigation.simple.KompassFormat;
import slash.navigation.simple.NavilinkFormat;
import slash.navigation.simple.OpelNaviFormat;
import slash.navigation.simple.QstarzQ1000Format;
import slash.navigation.simple.Route66Format;
import slash.navigation.simple.SygicAsciiFormat;
import slash.navigation.simple.SygicUnicodeFormat;
import slash.navigation.simple.WebPageFormat;
import slash.navigation.tcx.Tcx1Format;
import slash.navigation.tcx.Tcx2Format;
import slash.navigation.tour.TourFormat;
import slash.navigation.viamichelin.ViaMichelinFormat;
import slash.navigation.wbt.WintecWbt201Tk1Format;
import slash.navigation.wbt.WintecWbt201Tk2Format;
import slash.navigation.zip.ZipFormat;

/**
 * Only supports such navigation formats for which RouteConverter implements own parser.
 * Other formats are supported by RouteConverter via GPSBabel, for which there is a licensing
 * issue (see to Bug 1933).
 * 
 * For a list of these formats, see {@link NavigationFormats#SUPPORTED_FORMATS} (specifically,
 * the part of the static block directly below labeled "//self-implemented formats".
 * 
 * @author Fredrik Teschke
 *
 */
public class RouteConverterGPSFixImporterImpl extends BaseRouteConverterGPSFixImporterImpl {    
    private final static List<Class<? extends NavigationFormat<?>>> SUPPORTED_FORMATS =
            Arrays.<Class<? extends NavigationFormat<?>>>asList(
                    // self-implemented formats
                    NmeaFormat.class,
                    MTP0809Format.class,
                    MTP0607Format.class,
                    TomTom8RouteFormat.class,
                    TomTom5RouteFormat.class,
                    Kml20Format.class,
                    Kmz20Format.class,
                    Kml21Format.class,
                    Kmz21Format.class,
                    Kml22BetaFormat.class,
                    Kmz22BetaFormat.class,
                    Igo8RouteFormat.class,
                    Kml22Format.class,
                    Kmz22Format.class,
                    Gpx10Format.class,
                    Gpx11Format.class,
                    Nmn7Format.class,
                    Nmn6FavoritesFormat.class,
                    Nmn6Format.class,
                    Nmn5Format.class,
                    Nmn4Format.class,
                    WebPageFormat.class,
                    GpsTunerFormat.class,
                    HaicomLoggerFormat.class,
                    CoPilot6Format.class,
                    CoPilot7Format.class,
                    CoPilot8Format.class,
                    CoPilot9Format.class,
                    Route66Format.class,
                    KompassFormat.class,
                    GlopusFormat.class,
                    ColumbusV900ProfessionalFormat.class,
                    ColumbusV900StandardFormat.class,
                    QstarzQ1000Format.class,
                    Iblue747Format.class,
                    SygicAsciiFormat.class,
                    SygicUnicodeFormat.class,
                    MagicMapsPthFormat.class,
                    GoPal3RouteFormat.class,
                    GoPal5RouteFormat.class,
                    OvlFormat.class,
                    TourFormat.class,
                    ViaMichelinFormat.class,
                    MagicMapsIktFormat.class,
                    MagicMaps2GoFormat.class,
                    MagellanExploristFormat.class,
                    MagellanRouteFormat.class,
                    Tcx1Format.class,
                    Tcx2Format.class,
                    NokiaLandmarkExchangeFormat.class,
                    KlickTelRouteFormat.class,
                    GarminFlightPlanFormat.class,
                    WintecWbt201Tk1Format.class,
                    WintecWbt201Tk2Format.class,
                    NavilinkFormat.class,
                    GoRiderGpsFormat.class,
                    KienzleGpsFormat.class,
                    GroundTrackFormat.class,
                    OpelNaviFormat.class,
                    NavigatingPoiWarnerFormat.class,
                    NmnRouteFormat.class,
                    ZipFormat.class,

                    // second try for broken files
                    BrokenNmeaFormat.class,
                    BrokenHaicomLoggerFormat.class,
                    BrokenGpx10Format.class,
                    BrokenGpx11Format.class,
                    BrokenKml21Format.class,
                    BrokenKml21LittleEndianFormat.class,
                    BrokenKmz21Format.class,
                    BrokenKmz21LittleEndianFormat.class,
                    BrokenKml22BetaFormat.class,
                    BrokenKml22Format.class,
                    BrokenNavilinkFormat.class
                    );
    
    public RouteConverterGPSFixImporterImpl() {
        super(SUPPORTED_FORMATS);
    }
    
    public GPSFix convertToGPSFix(BaseNavigationPosition position) throws Exception {
        final CompactCalendar time = position.getTime();
        final Date t;
        if (time == null) {
            t = new Date(); // use "now" as the time for the fix which otherwise would be lacking a time stamp
        } else {
            t = time.getTime();
        }
        Double heading = null;
        if (position instanceof Wgs84Position) {
            heading = ((Wgs84Position) position).getHeading();
        }
        final Double speedInKilometersPerHour = position.getSpeed();
        Position pos = new DegreePosition(position.getLatitude(), position.getLongitude());
        TimePoint timePoint = new MillisecondsTimePoint(t);
        final GPSFix result;
        if (speedInKilometersPerHour != null && heading != null) {
            SpeedWithBearing speedWithBearing = new KilometersPerHourSpeedWithBearingImpl(
                    speedInKilometersPerHour, new DegreeBearingImpl(heading));
            result = new GPSFixMovingImpl(pos, timePoint, speedWithBearing, /* optionalTrueHeading */ null); // we assume the Wgs84Position.getHeading() method returns COG, not HDG/HDT
        } else {
            result = new GPSFixImpl(pos, timePoint);
        }
        return result;
    };

    @Override
    public Iterable<String> getSupportedFileExtensions() {
        //TODO the list is not yet complete
        return Arrays.asList("gpx", "kml", "kmz", "txt");
    }

    @Override
    public String getType() {
        return "RouteConverter";
    }
}
