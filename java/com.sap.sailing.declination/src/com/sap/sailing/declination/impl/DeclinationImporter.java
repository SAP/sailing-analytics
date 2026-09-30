package com.sap.sailing.declination.impl;

import java.io.IOException;
import java.text.ParseException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import com.sap.sailing.declination.Declination;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.util.ThreadPoolUtil;

public abstract class DeclinationImporter {
    private static final Logger logger = Logger.getLogger(DeclinationImporter.class.getName());
    
    public abstract Declination importRecord(Position position, TimePoint timePoint)
            throws IOException, ParserConfigurationException, SAXException;

    /**
     * Tries two things in parallel: fetch a more or less precise response from the online service and load
     * the requested year's declination values from a stored resource to look up a value that comes close.
     * The online lookup will be given preference. However, should it take longer than
     * <code>timeoutForOnlineFetchInMilliseconds</code>, then the method will return whatever it found
     * in the stored file, or <code>null</code> if no file exists for the year of <code>timePoint</code>.
     * 
     * @param timeoutForOnlineFetchInMilliseconds if 0, this means wait forever for the online result
     * @throws ParseException 
     * @throws ClassNotFoundException 
     * @throws IOException 
     */
    public Declination getDeclination(final Position position, final TimePoint timePoint,
            long timeoutForOnlineFetchInMilliseconds) throws IOException, ParseException {
        ExecutorService executorService = ThreadPoolUtil.INSTANCE.
            createBackgroundTaskThreadPoolExecutor(1, "DeclinationImporterThreadPoolExecutor");
        try {
            return executorService.submit(
                    () -> importRecord(position, timePoint)
                ).get(timeoutForOnlineFetchInMilliseconds, TimeUnit.MILLISECONDS);
        } catch (TimeoutException e) {
            logger.log(Level.INFO, "Timeout while getting declination.", e);
            // ignore; simply use value from file in this case
        } catch (InterruptedException e) {
            logger.log(Level.INFO, "InterruptedException while getting declination", e);
            // ignore; simply use value from file in this case
        } catch (ExecutionException e) {
            logger.log(Level.INFO, "Exception while trying to load magnetic declination online", e);
        }
        return null;
    }
}
