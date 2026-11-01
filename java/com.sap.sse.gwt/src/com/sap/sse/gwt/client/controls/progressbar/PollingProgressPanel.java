package com.sap.sse.gwt.client.controls.progressbar;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * Shared overall/sub-progress display with non-overlapping asynchronous polls. A null response allows initial
 * replication lag; transient failures are retried. Detaching stops observation, never the server-side operation.
 * The panel removes itself before invoking its terminal callback.
 */
public class PollingProgressPanel<T> extends VerticalPanel {
    private static final int POLL_INTERVAL_MILLIS = 5000;
    private static final int MAX_MISSING_RESPONSES = 12;
    private static final int MAX_CONSECUTIVE_FAILURES = 3;
    private final CustomProgressBar overallProgressBar = CustomProgressBar.determinate();
    private final Label subProgressName = new Label();
    private final CustomProgressBar subProgressBar = CustomProgressBar.determinate();
    private final Consumer<AsyncCallback<T>> fetchProgress;
    private final ToDoubleFunction<T> overallProgress;
    private final ToDoubleFunction<T> subProgress;
    private final Function<T, String> subProgressLabel;
    private final Function<T, String> failure;
    private final Predicate<T> complete;
    private final AsyncCallback<T> callback;
    private final String pollingErrorMessage;
    private final Timer timer = new Timer() {
        @Override
        public void run() {
            poll();
        }
    };
    private int generation;
    private int missingResponses;
    private int consecutiveFailures;
    private boolean running;

    public PollingProgressPanel(final String overallLabel, final String pollingErrorMessage,
            final Consumer<AsyncCallback<T>> fetchProgress, final ToDoubleFunction<T> overallProgress,
            final Function<T, String> subProgressLabel, final ToDoubleFunction<T> subProgress,
            final Function<T, String> failure, final Predicate<T> complete, final AsyncCallback<T> callback) {
        this.fetchProgress = fetchProgress;
        this.overallProgress = overallProgress;
        this.subProgressLabel = subProgressLabel;
        this.subProgress = subProgress;
        this.failure = failure;
        this.complete = complete;
        this.callback = callback;
        this.pollingErrorMessage = pollingErrorMessage;
        add(new Label(overallLabel + ":"));
        overallProgressBar.ensureDebugId("overallProgressBar");
        add(overallProgressBar);
        add(subProgressName);
        add(subProgressBar);
    }

    public void start() {
        stop();
        missingResponses = 0;
        consecutiveFailures = 0;
        running = true;
        timer.schedule(/* delayMillis */ 0);
    }

    public void stop() {
        running = false;
        generation++;
        timer.cancel();
    }

    @Override
    protected void onUnload() {
        stop();
        super.onUnload();
    }

    private void fail(final Throwable caught) {
        stop();
        removeFromParent();
        callback.onFailure(caught);
    }

    private void poll() {
        if (running) {
            final int requestGeneration = generation;
            fetchProgress.accept(new AsyncCallback<T>() {
                @Override
                public void onFailure(final Throwable caught) {
                    if (running && generation == requestGeneration) {
                        consecutiveFailures++;
                        if (consecutiveFailures >= MAX_CONSECUTIVE_FAILURES) {
                            fail(new IllegalStateException(pollingErrorMessage, caught));
                        } else {
                            timer.schedule(POLL_INTERVAL_MILLIS);
                        }
                    }
                }

                @Override
                public void onSuccess(final T progress) {
                    if (running && generation == requestGeneration) {
                        consecutiveFailures = 0;
                        if (progress == null) {
                            missingResponses++;
                            if (missingResponses >= MAX_MISSING_RESPONSES) {
                                fail(new IllegalStateException(pollingErrorMessage));
                            } else {
                                timer.schedule(POLL_INTERVAL_MILLIS);
                            }
                        } else {
                            missingResponses = 0;
                            overallProgressBar.setValue(overallProgress.applyAsDouble(progress));
                            subProgressName.setText(subProgressLabel.apply(progress));
                            subProgressBar.setValue(subProgress.applyAsDouble(progress));
                            final String error = failure.apply(progress);
                            if (error != null) {
                                fail(new IllegalStateException(error));
                            } else if (complete.test(progress)) {
                                stop();
                                removeFromParent();
                                callback.onSuccess(progress);
                            } else {
                                timer.schedule(POLL_INTERVAL_MILLIS);
                            }
                        }
                    }
                }
            });
        }
    }
}
