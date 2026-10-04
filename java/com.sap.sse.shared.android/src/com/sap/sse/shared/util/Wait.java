package com.sap.sse.shared.util;

import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;

public class Wait {
    private static final Logger logger = Logger.getLogger(Wait.class.getName());
    
    /**
     * Like {@link #wait(Callable, Optional, Duration, Level, String)}, but not producing log output
     */
    public static boolean wait(Callable<Boolean> condition, Optional<Duration> optionalTimeoutEmptyMeansForever, Duration sleepBetweenAttempts) throws Exception {
        return wait(condition, optionalTimeoutEmptyMeansForever, sleepBetweenAttempts, /* no logging */ null, /* no log message */ null);
    }
    
    /**
     * Keeps evaluating {@code condition} until the result is {@code true} or the timeout, if provided in
     * {@code optionalTimeoutEmptyMeansForever}, has expired. Sleeps {@code sleepBetweenAttempts} between subsequent
     * attempts. Note that once decided for starting another evaluation, that evaluation may surpass the timeout; but no
     * new evaluation will be started after the timeout has expired. The {@code condition} will be evaluated at least
     * once, even if a timeout of zero seconds has been specified. If no timeout is provided, the method will keep
     * evaluating the condition until it returns {@code true}.<p>
     * 
     * Any exception thrown during the evaluation of {@code condition} will pass the exception on to the caller.<p>
     * 
     * If a non-{@code null} {@code logLevel} and {@code retryLogMessage} are provided, upon failure and if the timeout permits
     * for another round of sleep/retry, a message will be logged telling that {@code retryLogMessage} failed and that
     * another attempt will be made in {@code sleepBetweenAttempts} until the timeout has been reached.
     * 
     * @return the last evaluation result of {@code condition}
     */
    public static boolean wait(Callable<Boolean> condition, Optional<Duration> optionalTimeoutEmptyMeansForever, Duration sleepBetweenAttempts,
            Level logLevel, String retryLogMessage) throws Exception, TimeoutException {
        return wait(condition, result->result, /* retryOnException */ false, optionalTimeoutEmptyMeansForever, sleepBetweenAttempts, logLevel, retryLogMessage);
    }

    /**
     * Keeps evaluating {@code callable} until the result is tested successfully by the {@code successPredicate}, or the
     * timeout, if provided in {@code optionalTimeoutEmptyMeansForever}, has expired. At least one attempt is made,
     * regardless how small the timeout. Sleeps {@code sleepBetweenAttempts} between subsequent attempts. Note that once
     * decided for starting another evaluation, that evaluation may surpass the timeout; but no new evaluation will be
     * started after the timeout has expired. The {@code condition} will be evaluated at least once, even if a timeout
     * of zero seconds has been specified. If no timeout is provided, the method will keep evaluating the
     * {@code callable} until it passes the {@code successPredicate}.
     * <p>
     * 
     * If {@code retryOnException} is set to {@code true}, any exception thrown during the evaluation of
     * {@code callable} will be considered a failed attempt that leads to a retry if there is still time; if
     * {@code retryOnException} is {@code false}, the exception will be thrown by this method.
     * <p>
     * 
     * If a non-{@code null} {@code logLevel} and {@code retryLogMessage} are provided, upon failure and if the timeout
     * permits for another round of sleep/retry, a message will be logged telling that {@code retryLogMessage} failed
     * and that another attempt will be made in {@code sleepBetweenAttempts} until the timeout has been reached.
     * <p>
     * 
     * @return the last evaluation result of {@code callable}; this one has successfully passed the
     *         {@code successPredicate}
     * @throws TimeoutException
     *             if a {@code optionalTimeoutEmptyMeansForever} was provided and within this time no execution of the
     *             {@code callable} was considered a success by the {@code successPredicate}
     */
    public static <T> T wait(Callable<T> callable, Predicate<T> successPredicate, boolean retryOnException,
            Optional<Duration> optionalTimeoutEmptyMeansForever, Duration sleepBetweenAttempts, Level logLevel,
            String retryLogMessage) throws Exception, TimeoutException {
        final TimePoint start = TimePoint.now();
        T result = null;
        boolean success;
        boolean timeForAnotherAttempt;
        do {
            try {
                result = callable.call();
                success = successPredicate.test(result);
                if (success && logLevel != null && retryLogMessage != null) {
                    logger.log(logLevel, retryLogMessage+" succeeded.");
                }
            } catch (Exception e) {
                if (retryOnException) {
                    success = false;
                    if (logLevel != null && retryLogMessage != null) {
                        logger.log(logLevel, retryLogMessage+" threw exception "+e.getMessage());
                    }
                } else {
                    throw e;
                }
            }
            timeForAnotherAttempt = optionalTimeoutEmptyMeansForever.map(timeout->TimePoint.now().plus(sleepBetweenAttempts).before(start.plus(timeout))).orElse(true);
            if (!success) {
                if (logLevel != null && retryLogMessage != null) {
                    logger.log(logLevel, retryLogMessage+" failed."+(timeForAnotherAttempt?
                            optionalTimeoutEmptyMeansForever.map(timeout->" Will time out in "+timeout.minus(start.until(TimePoint.now())).toString()).orElse("")
                            +" Sleeping for "+sleepBetweenAttempts+" now...":""));
                }
                if (timeForAnotherAttempt) {
                    Thread.sleep(sleepBetweenAttempts.asMillis());
                }
            }
        } while (!success && timeForAnotherAttempt);
        if (!success) {
            if (logLevel != null && retryLogMessage != null) {
                logger.log(logLevel, retryLogMessage+" timed out.");
            }
            throw new TimeoutException();
        }
        return result;
    }
}
