package com.sap.sailing.gwt.home.shared.refresh;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.gwt.home.communication.SailingAction;
import com.sap.sailing.gwt.home.communication.SailingDispatchSystem;
import com.sap.sailing.gwt.home.shared.refresh.ActionProvider.DefaultActionProvider;
import com.sap.sse.common.Duration;
import com.sap.sse.gwt.dispatch.shared.commands.ResultWithTTL;
import com.sap.sse.gwt.shared.DTO;

/**
 * Provides automatic refresh functionality for widgets based on the {@link SailingDispatchSystem}.
 * 
 * A widget can be registered for automatic refresh if it implements {@link RefreshableWidget} and it's data is provided
 * by a dispatch action of type {@link SailingAction}. This action needs to have it's result wrapped by a
 * {@link ResultWithTTL} to control refresh intervals by the server.
 * 
 * The {@link RefreshManager} ensures that the next refresh will occur in about the time defined by the returned
 * {@link ResultWithTTL}. While doing this it tries to batch refreshes that are scheduled for a similar time point.
 * 
 * If an error occurs while loading new data on a refresh, the next refresh is automatically scheduled in 30 seconds to
 * not kill the server by immediate repetitive remote calls.
 */
public abstract class RefreshManager {
    private static final Logger LOG = Logger.getLogger(RefreshManager.class.getName());

    /**
     * When an error occurs, the next refresh is delayed by 30 seconds to not kill the server by doing immediate repetitive calls.
     */
    private static final long PAUSE_ON_ERROR = Duration.ONE_SECOND.times(30).asMillis();
    private List<RefreshHolder<DTO, SailingAction<ResultWithTTL<DTO>>>> refreshables = new ArrayList<>();

    private boolean scheduled;
    private final Timer timer = new Timer() {
        @Override
        public void run() {
            update();
        }
    };

    private final SailingDispatchSystem actionExecutor;
    
    boolean started = false;

    /**
     * @param actionExecutor the {@link SailingDispatchSystem} to use for remote communication
     */
    public RefreshManager(SailingDispatchSystem actionExecutor) {
        this.actionExecutor = actionExecutor;
    }
    
    /**
     * Initially starts the auto-refresh. Needs to be called once by sublasses when at least one {@link RefreshableWidget} is added.
     * This triggers the first refresh. Subsequent refreshes are automatically triggered until {@link #cancel()} is called.
     */
    protected void start() {
        LOG.log(Level.FINE, "Starting auto refresh");
        started = true;
        reschedule();
    }
    
    /**
     * Must be called by subclasses to stop the auto-refresh. Ensures that all resources are disposed that would
     * otherwise lead to memory leaks or zombie processes.
     */
    protected void cancel() {
        LOG.log(Level.FINE, "Cancelling auto refresh");
        started = false;
        timer.cancel();
    }
    
    /**
     * @return true if a refresh may occur in the current state, false otherwise.
     */
    protected abstract boolean canExecute();
    
    /**
     * Is called whenever a refresh was successful. Useful for subclasses that need to hook into the lifecycle of the {@link RefreshManager}.
     */
    protected void onSuccessfulUpdate() {
    }
    
    /**
     * Is called whenever a refresh failed. Useful for subclasses that need to hook into the lifecycle of the {@link RefreshManager}.
     */
    protected  void onFailedUpdate(Throwable errorCause) {
    }

    /**
     * Triggers an update for all {@link RefreshableWidget}s that will have a timeout in the next 5 seconds.
     */
    private void update() {
        for (final RefreshHolder<DTO, SailingAction<ResultWithTTL<DTO>>> refreshable : refreshables) {
            // Everything that needs refresh within the next 5000ms will be refreshed now.
            // This makes it possible to use batching resulting in less requests.
            if (refreshable.provider.isActive() && !refreshable.callRunning
                    && refreshable.timeout < System.currentTimeMillis() + ResultWithTTL.MAX_TIME_TO_LOAD_EARLIER.asMillis()) {
                refreshable.callRunning = true;
                final SailingAction<ResultWithTTL<DTO>> action = refreshable.provider.getAction();
                actionExecutor.execute(action, new AsyncCallback<ResultWithTTL<DTO>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        LOG.log(Level.FINE, "Error on auto refresh");
                        refreshable.callRunning = false;
                        refreshable.timeout = System.currentTimeMillis() + PAUSE_ON_ERROR;
                        reschedule();
                        onFailedUpdate(caught);
                    }

                    @Override
                    public void onSuccess(ResultWithTTL<DTO> result) {
                        refreshable.callRunning = false;
                        refreshable.timeout = System.currentTimeMillis() + result.getTtlMillis();
                        try {
                            refreshable.widget.setData(result.getDto());
                            onSuccessfulUpdate();
                        } catch(Throwable error) {
                            LOG.log(Level.SEVERE, "Error while refreshing content with action " + action.getClass().getName(), error);
                        }
                        reschedule();
                    }
                });
            }
        }
    }

    /**
     * Ensures that the next update is scheduled by a timer.
     */
    private void reschedule() {
        if (!scheduled) {
            scheduled = true;
            Scheduler.get().scheduleFinally(new ScheduledCommand() {
                @Override
                public void execute() {
                    scheduled = false;
                    if (refreshables.isEmpty()) {
                        LOG.log(Level.FINE, "No refreshables found -> skipping refresh");
                        return;
                    }
                    if (!started) {
                        LOG.log(Level.FINE, "Refresh not started yet -> skipping refresh");
                        return;
                    }
                    if (!canExecute()) {
                        LOG.log(Level.FINE, "Refresh not allowed to execute -> skipping refresh");
                        return;
                    }
                    Long nextUpdate = null;
                    for (final RefreshHolder<DTO, SailingAction<ResultWithTTL<DTO>>> refreshable : refreshables) {
                        if (refreshable.callRunning || !refreshable.provider.isActive()) {
                            continue;
                        }
                        if (nextUpdate == null) {
                            nextUpdate = refreshable.timeout;
                        } else {
                            nextUpdate = Math.min(nextUpdate, refreshable.timeout);
                        }
                    }
                    if (nextUpdate == null) {
                        // This can occur if there is already a call running for all RefreshableWidgets 
                        LOG.log(Level.FINE, "Nothing to auto update");
                    } else {
                        int delayMillis = (int) (nextUpdate - System.currentTimeMillis());
                        if (delayMillis <= 0) {
                            LOG.log(Level.FINE, "Auto updating immediately");
                            update();
                        } else {
                            LOG.log(Level.FINE, "Scheduling auto refresh in " + delayMillis + "ms");
                            timer.schedule(delayMillis);
                        }
                    }
                }
            });
        }
    }
    
    public void forceReschedule() {
        scheduled = false;
        timer.cancel();
        reschedule();
    }

    /**
     * Add a {@link RefreshableWidget} to the set of refreshables controlled by this {@link RefreshManager}.
     * The refresh intervals are controlled by the ResultWithTTL returned from the server.
     * 
     * On each refresh, the given {@link ActionProvider} is called to get the action to send to the server.
     * This is useful if some state-dependent data needs to be sent so that the action can't be a fix instance.
     * 
     * @param widget the {@link RefreshableWidget} to start auto refresh for
     * @param provider the {@link ActionProvider} to get the {@link SailingAction} instance from when fetching new data for a refresh
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <D extends DTO, A extends SailingAction<ResultWithTTL<D>>> void add(RefreshableWidget<? super D> widget,
            ActionProvider<A> provider) {
        refreshables.add(new RefreshHolder(widget, provider));
        reschedule();
    }

    /**
     * Add a {@link RefreshableWidget} to the set of refreshables controlled by this {@link RefreshManager}.
     * The refresh intervals are controlled by the ResultWithTTL returned from the server.
     * 
     * On each refresh, the given action is sent to the server to get new data.
     * 
     * @param widget the {@link RefreshableWidget} to start auto refresh for
     * @param action the {@link SailingAction} instance to use when fetching new data for a refresh
     */
    public <D extends DTO, A extends SailingAction<ResultWithTTL<D>>> void add(RefreshableWidget<? super D> widget, A action) {
        add(widget, new DefaultActionProvider<>(action));
    }
    
    public SailingDispatchSystem getDispatchSystem() {
        return actionExecutor;
    }

    /**
     * Internal structure for holding the state for a {@link RefreshableWidget} controlled by the {@link RefreshManager}.
     *
     * @param <D> The Type of the Data returned from the server (wrapped by a {@link ResultWithTTL})
     * @param <A> The type of the dispatch action for fetching new data 
     */
    private static class RefreshHolder<D extends DTO, A extends SailingAction<ResultWithTTL<D>>> {
        private final RefreshableWidget<D> widget;
        private final ActionProvider<A> provider;

        /**
         * Defines the timeout when the next refresh will occur.
         * Initial update is now to trigger an immediate update.
         */
        private long timeout = System.currentTimeMillis();
        /**
         * When there is currently a call running for refreshing this {@link RefreshableWidget}, callRunning is set to true.
         */
        private boolean callRunning = false;

        public RefreshHolder(RefreshableWidget<D> widget, ActionProvider<A> provider) {
            this.widget = widget;
            this.provider = provider;
        }
    }
}
