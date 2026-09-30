package com.sap.sailing.declination.impl;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.sap.sailing.declination.Declination;
import com.sap.sailing.domain.common.quadtree.QuadTree;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * Manages resources in which declinations can be stored for off-line look-up. Time resolution is
 * one year. The declination values are expected to be provided for mid-year (June 30). The annual
 * change can be used to extrapolate to other times of year.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class DeclinationStore {
    private static final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
    
    private final DeclinationImporter importer;
    
    public DeclinationStore(DeclinationImporter importer) {
        this.importer = importer;
    }
    
    /**
     * Returns <code>null</code> if no stored declinations exist for the <code>year</code> requested.
     */
    public QuadTree<Declination> getStoredDeclinations(int year) throws IOException, ParseException {
        Declination record;
        QuadTree<Declination> result = null;
        InputStream is = getInputStreamForYear(year);
        if (is != null) {
            result = new QuadTree<Declination>();
            BufferedReader in = new BufferedReader(new InputStreamReader(is));
            boolean exceptionOccurred;
            try {
                record = readExternal(in);
                exceptionOccurred = false;
            } catch (NumberFormatException e) {
                exceptionOccurred = true;
                record = null;
            }
            while (exceptionOccurred || record != null) {
                if (record != null) {
                    synchronized (result) {
                        result.put(record.getPosition(), record);
                    }
                }
                try {
                    record = readExternal(in);
                    exceptionOccurred = false;
                } catch (NumberFormatException e) {
                    exceptionOccurred = true;
                    record = null;
                }
            }
        }
        return result;
    }
    
    private String getResourceForYear(int year) {
        String filename = getFilenameForYear(year);
        return "resources/" + filename;
    }

    private String getFilenameForYear(int year) {
        String filename = "declination-"+year;
        return filename;
    }
    
    private InputStream getInputStreamForYear(int year) {
        return getClass().getResourceAsStream("/"+getFilenameForYear(year));
    }
    
    public void writeExternal(Declination record, Writer out) throws IOException {
        synchronized (dateFormatter) {
            out.write(dateFormatter.format(record.getTimePoint().asDate()));
        }
        out.write("|"+record.getPosition().getLatDeg());
        out.write("|"+record.getPosition().getLngDeg());
        out.write("|"+record.getBearing().getDegrees());
        out.write("|"+record.getAnnualChange().getDegrees());
        out.write("\n");
    }
    
    public Declination readExternal(BufferedReader in) throws IOException, ParseException {
        Declination result = null;
        try {
            String line = in.readLine();
            if (line != null && line.length() > 0) {
                String[] fields = line.split("\\|");
                final TimePoint timePoint;
                synchronized (dateFormatter) {
                    timePoint = new MillisecondsTimePoint(dateFormatter.parse(fields[0]).getTime());
                }
                double lat = Double.valueOf(fields[1]);
                double lng = Double.valueOf(fields[2]);
                Position position = new DegreePosition(lat, lng);
                Bearing bearing = new DegreeBearingImpl(Double.valueOf(fields[3]));
                Bearing annualChange = new DegreeBearingImpl(Double.valueOf(fields[4]));
                return new DeclinationRecordImpl(position, timePoint, bearing, annualChange);
            }
        } catch (EOFException eof) {
            // leave result as null
        }
        return result;
    }
    
    private void fetchAndAppendDeclination(TimePoint timePoint, Position position, DeclinationImporter importer,
            Writer out) throws IOException {
        Declination declination = null;
        // re-try three times
        for (int i=0; i<3; i++) {
            try {
                declination = importer.importRecord(position, timePoint);
                break;
            } catch (IOException | ParserConfigurationException | SAXException ioe) {
                ioe.printStackTrace();
                if (i<2) {
                    System.out.println("re-trying");
                }
            }
        }
        if (declination != null) {
            writeExternal(declination, out);
            out.flush();
        }
    }

    private void run(String[] args) throws FileNotFoundException, IOException, ClassNotFoundException, ParseException {
        if (args.length == 0) {
            usage();
        } else {
            int fromYear = Integer.valueOf(args[0]);
            int toYear = Integer.valueOf(args[1]);
            if (toYear < fromYear) {
                usage();
            } else {
                double grid = Double.valueOf(args[2]);
                for (int year = fromYear; year <= toYear; year++) {
                    QuadTree<Declination> storedDeclinations = getStoredDeclinations(year);
                    if (storedDeclinations == null) {
                        storedDeclinations = new QuadTree<>();
                    }
                    // append if file already exists
                    File fileForYear = new File(getResourceForYear(year));
                    Writer out;
                    if (fileForYear.exists()) {
                        out = new FileWriter(getResourceForYear(year), /* append */ true);
                    } else {
                        out = new FileWriter(getResourceForYear(year));
                    }
                    int month = 6;
                    Calendar cal = new GregorianCalendar(year, month, /* dayOfMonth */ 1);
                    TimePoint timePoint = new MillisecondsTimePoint(cal.getTimeInMillis());
                    for (double lat = 0; lat < 90; lat += grid) {
                        fetchAndAppendDeclinationForLatitude(grid, importer, year, storedDeclinations, out, month, timePoint, lat);
                    }
                    for (double lat = -grid; lat > -90; lat -= grid) {
                        fetchAndAppendDeclinationForLatitude(grid, importer, year, storedDeclinations, out, month, timePoint, lat);
                    }
                    out.close();
                }
            }
        }
    }

    private void fetchAndAppendDeclinationForLatitude(double grid, DeclinationImporter importer, int year,
            QuadTree<Declination> storedDeclinations, Writer out, int month, TimePoint timePoint, double lat)
            throws IOException {
        System.out.println("Date: " + year + "/" + (month + 1) + ", Latitude: " + lat);
        for (double lng = 0; lng < 180; lng += grid) {
            fetchAndAppendDeclination(importer, storedDeclinations, out, timePoint, lat, lng);
        }
        for (double lng = -grid; lng > -180; lng -= grid) {
            fetchAndAppendDeclination(importer, storedDeclinations, out, timePoint, lat, lng);
        }
    }

    private void fetchAndAppendDeclination(DeclinationImporter importer, QuadTree<Declination> storedDeclinations, Writer out,
            TimePoint timePoint, double lat, double lng) throws IOException {
        Position point = new DegreePosition(lat, lng);
        final Declination existingDeclinationRecord;
        synchronized (storedDeclinations) {
            existingDeclinationRecord = storedDeclinations.get(point);
        }
        if (existingDeclinationRecord == null
                || DeclinationServiceImplWithStore.timeAndSpaceDistance(existingDeclinationRecord.getPosition().getDistance(point),
                timePoint, existingDeclinationRecord.getTimePoint()) > 0.1) {
            // less than ~6 nautical miles and/or ~.6 months off
            fetchAndAppendDeclination(timePoint, point, importer, out);
        }
    }

    /**
     * Launches the importer, writing to resources/declination-year.txt (where "year" represents the year for which the
     * values are stored in the file) the declinations downloaded online for the years <code>args[0]</code> to
     * <code>args[1]</code> (inclusive) for all positions with a grid of <code>args[2]</code> degrees each, starting at
     * 0&deg;0.0'N and 0&deg;0.0'E. <code>args[3]</code> can optionally be used to select a non-default declination
     * importer. By default, the {@link NOAAImporter} will be used. Using "c" here will use the {@link ColoradoImporter}
     * instead, and using "b" gets you the {@link BGSImporter}.
     * 
     * @throws ParseException 
     * @throws ClassNotFoundException 
     */
    public static void main(String[] args) throws IOException, ClassNotFoundException, ParseException {
        DeclinationStore store = new DeclinationStore(args.length > 3 && args[3].equals("c") ? new ColoradoImporter() :
            args.length > 3 && args[3].equals("b") ? new BGSImporter() : new NOAAImporter());
        store.run(args);
    }
    
    private void usage() {
        System.out.println("java " + getClass().getName() + " <fromYear> <toYear> <gridSizeInDegrees> [c|b]");
        System.out.println("The optional trailing c causes the ColoradoImporter, b the BGSImporter to be used instead of the default NOAAImporter.");
    }
}
