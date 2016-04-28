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
import com.sap.sse.gwt.dispatch.shared.commands.DTO;
import com.sap.sse.gwt.dispatch.shared.commands.ResultWithTTL;

public abstract class RefreshManager {
    private static final Logger LOG = Logger.getLogger(RefreshManager.class.getName());

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

    public RefreshManager(SailingDispatchSystem actionExecutor) {
        this.actionExecutor = actionExecutor;
    }
    
    protected void start() {
        LOG.log(Level.FINE, "Starting auto refresh");
        started = true;
        reschedule();
    }
    
    protected void cancel() {
        LOG.log(Level.FINE, "Cancelling auto refresh");
        started = false;
        timer.cancel();
    }
    
    protected abstract boolean canExecute();
    
    protected void onSuccessfulUpdate() {
    }
    
    protected  void onFailedUpdate(Throwable errorCause) {
    }

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

    private void reschedule() {
        if (scheduled) {
            return;
        }
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
                if(nextUpdate == null) {
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
    
    public void forceReschedule() {
        scheduled = false;
        timer.cancel();
        reschedule();
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    public <D extends DTO, A extends SailingAction<ResultWithTTL<D>>> void add(RefreshableWidget<? super D> widget,
            ActionProvider<A> provider) {
        refreshables.add(new RefreshHolder(widget, provider));
        reschedule();
    }

    public <D extends DTO, A extends SailingAction<ResultWithTTL<D>>> void add(RefreshableWidget<? super D> widget, A action) {
        add(widget, new DefaultActionProvider<>(action));
    }
    
    public SailingDispatchSystem getDispatchSystem() {
        return actionExecutor;
    }

    private static class RefreshHolder<D extends DTO, A extends SailingAction<ResultWithTTL<D>>> {
        private final RefreshableWidget<D> widget;
        private final ActionProvider<A> provider;

        // initial update is now
        private long timeout = System.currentTimeMillis();
        private boolean callRunning = false;

        public RefreshHolder(RefreshableWidget<D> widget, ActionProvider<A> provider) {
            this.widget = widget;
            this.provider = provider;
        }
    }
}
