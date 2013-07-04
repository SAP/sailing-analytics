package com.sap.sailing.server.trackfiles.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import com.sap.sailing.domain.common.trackfiles.TrackFilesDataSource;
import com.sap.sailing.domain.common.trackfiles.TrackFilesFormat;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.server.trackfiles.Export;
import com.sap.sailing.server.trackfiles.common.FormatNotSupportedException;

/**
 * Export data to well-known formats such as GPX, KML etc. Internally, the
 * RouteConverter Library is used.
 * 
 * Should you need to support another format, add this to
 * {@link TrackFilesFormat} and extend the writeFixes method.
 * 
 * @author Fredrik Teschke
 * 
 */
public class ExportImpl implements Export {
	private static final Logger log = Logger.getLogger(ExportImpl.class
			.toString());

	/**
	 * Writes the wanted data of all the races to the output stream. One file
	 * per race and data type.
	 * 
	 * @param data
	 * @param format
	 * @param races
	 * @param dataBeforeAfter
	 *            false: do not include data from before the race started and
	 *            after the race ended
	 * @param out
	 * @throws FormatNotSupportedException
	 */
	@Override
	public void writeAllData(List<TrackFilesDataSource> data,
			TrackFilesFormat format, List<TrackedRace> races,
			boolean dataBeforeAfter, boolean rawFixes, final ZipOutputStream out)
			throws IOException {

		WriteZipCallback callback = new WriteZipCallback() {
			@Override
			public synchronized void write(ZipEntry entry, byte[] data)
					throws IOException {
				out.putNextEntry(entry);
				out.write(data);
				out.closeEntry();
			}
		};

		List<WriteRaceDataCallable> callables = new ArrayList<>();
		ExecutorService executor = Executors.newCachedThreadPool();
		List<String> errors = new ArrayList<>();
		for (TrackedRace race : races) {
			for (TrackFilesDataSource d : data) {
				WriteRaceDataCallable callable = new WriteRaceDataCallable(
						race, d, format, dataBeforeAfter, rawFixes,
						callback);
				callables.add(callable);
			}
		}

		List<Future<Void>> results = Collections.<Future<Void>> emptyList();

		try {
			results = executor.invokeAll(callables);
		} catch (InterruptedException e) {
			errors.add(e.getMessage());
			log.log(Level.WARNING, "Error exporting race: " + e.getMessage());
			e.printStackTrace();
		}

		for (Future<Void> result : results) {
			try {
				result.get();
			} catch (ExecutionException e) {
				Throwable t = e.getCause();
				errors.add(t.getMessage());
				log.log(Level.WARNING,
						"Error exporting race: " + t.getMessage());
				t.printStackTrace();
			} catch (InterruptedException e) {
				errors.add(e.getMessage());
				log.log(Level.WARNING,
						"Error exporting race: " + e.getMessage());
				e.printStackTrace();
			}
		}

		if (errors.size() > 0) {
			StringBuilder sb = new StringBuilder();
			for (String error : errors) {
				sb.append(error);
				sb.append("\n");
			}
			try {
				callback.write(new ZipEntry("ERRORS"), sb.toString().getBytes());
			} catch (Exception e) {
				log.log(Level.WARNING,
						"Error exporting race: " + e.getMessage());
				e.printStackTrace();
			}

		}
	}
}
