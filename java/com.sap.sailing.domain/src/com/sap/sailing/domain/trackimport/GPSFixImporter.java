package com.sap.sailing.domain.trackimport;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.Map;

import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.trackfiles.TrackFileImportDeviceIdentifier;

/**
 * Importer for extracting GPS fixes from an {@link InputStream}. Importers are picked up via the OSGi service registry,
 * so make sure to register them (the {@link GPSFixImporterRegistration} class can help with this).
 * <p>
 * 
 * Implementers need to {@link #getType() provide a unique type name} for the importer and may
 * {@link #getSupportedFileExtensions() specify the file name extensions} for the file types supported.
 * The actual work is done by the implementation of {@link #importFixes(InputStream, Callback, boolean)} which
 * receives the input stream that the importer shall analyze. When the start of a track is found, the importer must call the
 * {@link Callback#startTrack(String, Map)} method. From that point onwards, all fixes that the importer recognizes in the
 * stream and that it passes to {@link Callback#addFix(GPSFix)} are then assigned to that track which means that
 * for each track an artificial {@link DeviceIdentifier} is created which can later be used to associate the track
 * with a tracked object such as a competitor or a mark. Note that this means that all fixes of a single track need
 * to be passed to the callback before fixes of any other track can be passed to the callback.<p>
 * 
 * If the data source to import from does not deliver course and speed over ground (COG/SOG) but only time-stamped
 * latitude and longitude values, consider using <code>BaseGPSFixImporterImpl</code> which infers COG and SOG values
 * from the positions and time stamps.
 * 
 * @author Fredrik Teschke
 * 
 */
public interface GPSFixImporter {
    public static final String EXPEDITION_TYPE = "Expedition";
    
    String FILE_EXTENSION_PROPERTY = "fileExt";
    
    /**
     * Callback through which fixes found in the source stream are passed back. Implementing classes may choose to
     * override {@link #addFixes(Iterable, TrackFileImportDeviceIdentifier)} in case they have a better way to process a
     * whole set of fixes from the same device than adding one by one.
     * 
     * @author Fredrik Teschke
     *
     */
    interface Callback {
        void addFix(GPSFix fix, TrackFileImportDeviceIdentifier device);
        
        default void addFixes(Iterable<GPSFix> fixes, TrackFileImportDeviceIdentifier device) {
            fixes.forEach(fix->addFix(fix, device));
        }
    }

    /**
     * Retrieves the fixes from the {@code inputStream}, and calls the {@code callback} with every new fix.
     * @param inferSpeedAndBearing
     *            Should speed and bearing be inferred by looking at the previous fix, if that data is not directly
     *            present within the file?
     * @param sourceName
     *            some name that identifies the source, e.g. the file name if a file
     * 
     * @throws FormatNotSupportedException
     *             If the input format cannot be read. The import process might then decide to try attempt importing
     *             fixes using the next suitable importer.
     * 
     * @return returns if import was succesful or not
     */
    boolean importFixes(InputStream inputStream, Charset charset, Callback callback, boolean inferSpeedAndBearing, String sourceName) throws Exception;

    /**
     * Return the file extensions supported by this importer. If the importer is not intended
     * for file-based input, this may return an empty iterable. Expects only the extension without
     * any leading period, e.g., "gpx" or "kml".
     */
    Iterable<String> getSupportedFileExtensions();
    
    /**
     * Return a unique type name, that should be human readable.
     * @return
     */
    String getType();
}
