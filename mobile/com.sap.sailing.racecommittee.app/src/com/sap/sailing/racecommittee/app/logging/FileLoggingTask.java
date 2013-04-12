package com.sap.sailing.racecommittee.app.logging;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;

import com.sap.sailing.racecommittee.app.AppConstants;

import android.os.Environment;
import android.util.Log;

public class FileLoggingTask implements Runnable {
	
	private final static String TAG = FileLoggingTask.class.getName();
	
	private final static String logFileTemplate = "sap_rc_log_%s.txt";
	private final static String logFileDateFormat = "yyyyMMdd";
	
	private final BlockingQueue<String> queue;
	
	private String logFileName;
	private File logFile;
	private BufferedWriter logWriter;
	
	public FileLoggingTask(BlockingQueue<String> queue) {
		this.queue = queue;
	}
	
	public FileLoggingTask() {
		this.queue = null;
	}
	
	public boolean tryStartFileLogging() {
		return tryStartFileLogging(FileLoggingTask.logFileTemplate);
	}
	
	public boolean tryStartFileLogging(String template) {
		try {
			Format format = new SimpleDateFormat(logFileDateFormat, Locale.US);
			logFileName = String.format(template, format.format(new Date()));
			
			File loggingDirectory = new File(Environment.getExternalStorageDirectory() + AppConstants.LogFolder);
			loggingDirectory.mkdirs();
			logFile = new File(loggingDirectory, logFileName);
			
			if (prepareFile(logFile)) {
				try {
					logWriter = new BufferedWriter(new FileWriter(logFile, true));
					return true;
				} catch (IOException e) {
					Log.w(TAG, String.format("Unable to open writer on file %s: %s", logFile.getAbsolutePath(), e.getMessage()));
					return false;
				}
			}
		}
		catch (Throwable e) {
			Log.w(TAG, "Error trying to setup file logging: " + e.getMessage());
		}
		return false;
	}
	
	public String getLogFilePath() {
		return logFile.getAbsolutePath();
	}
	
	public void run() {
		while (true) {
				String logMessage;
				try {
					logMessage = queue.take();
					log(logMessage);
				} catch (InterruptedException e) { }
		}
	}
	
	public void log(String logMessage) {
		try {
			logWriter.append(logMessage);
			logWriter.newLine();
			logWriter.flush();
		} catch (Exception e) { }
	}
	
	public void logException(Throwable ex) {
		PrintWriter writer = new PrintWriter(logWriter);
		ex.printStackTrace(writer);
		writer.println();
		writer.flush();
	}
	
	@Override
	protected void finalize() throws Throwable {
		super.finalize();
		try {
			logWriter.close();
		} catch (Throwable e) { }
	}
	

	private static boolean prepareFile(File file) {
		if (file == null) {
			Log.i(TAG, "Log file couldn't be opened.");
			return false;
		}
		
		// create the file (if not exists)
		try {
			file.createNewFile();
		} catch (IOException e) {
			Log.w(TAG, String.format("Couldn't create file %s : %s", file.getAbsolutePath(), e.getMessage()));
			return false;
		}
		
		return file.canWrite();
	}
	
}
