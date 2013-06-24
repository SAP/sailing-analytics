package com.sap.sailing.gwt.ui.client;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;

/**
 * Manages a timer and can auto-advance it at a given acceleration/deceleration rate, with a given delay compared to
 * real time. It can be {@link #pause() paused} and {@link #resume() resumed}. It can be in one of two play modes:
 * {@link PlayModes#Live} and {@link PlayModes#Replay}. In live play mode the timer is always in state {@link PlayStates#Playing}
 * and adjusts the time to now-{@link #livePlayDelayInMillis}. As soon as it's paused or stopped it enters
 * {@link PlayModes#Replay}. By entering {@link PlayModes#Live} with {@link #setPlayMode(PlayModes)}, the timer starts
 * playing right away.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public class Timer {
    /**
     * Used by {@link #quantizeTimeStamp(long)}; currently set to 1s (1000ms)
     */
    private static final long LIVE_CLOCK_QUANTUM = 1000;
    
    /**
     * The time point represented by this timer
     */
    private Date time;

    /**
     * The time delay to the current point in time in milliseconds which will only be used in {@link PlayModes#Live}
     * play mode in order to infer an approximate, server-based time point to display on the client that is independent
     * of the client clock. Not set explicitly by the user but controlled by responses coming from the server.
     */
    private long livePlayDelayInMillis;
    
    /**
     * Listeners interested in changes of {@link #time}
     */
    private final Set<TimeListener> timeListeners;
    
    /**
     * Listeners interested in changes of {@link #playing}
     */
    private final Set<PlayStateListener> playStateListeners;

    /**
     * If <code>Playing</code>, an auto-refreshing command is running with {@link #refreshInterval}
     * as its refresh interval.
     */
    private PlayStates playState;
    
    /**
     * The current play mode of the timer: live or replay. In live mode the timer will adjust to now-delay each time it
     * refreshes. In replay mode it will advance time by the refresh interval times the play speed factor.
     */
    private PlayModes playMode;
    
    /**
     * The refresh interval in milliseconds for auto-updating this timer
     */
    private long refreshInterval;
    
    /**
     * Set to <code>true</code> if the refreshInterval changed while {@link #playing}. This forces the auto-refreshing command
     * to re-schedule itself with the new interval, then terminate.
     */
    private boolean refreshIntervalChanged;
    
    /**
     * Factor by which the timer runs faster than real time. 1.0 means real-time, 2.0 means twice as fast as real time, and so on.
     * Applies only if the timer is in replay mode.
     */
    private double playSpeedFactor;

    /**
     * Set to <code>true</code> to enable the timer to advance the max time automatically if the current time gets greater than the max time 
     */
    private boolean autoAdvance;

    /**
     * The clocks of a client cannot be expected to be synchronized to the same time source as the server clock.
     * Therefore, there is a good chance for a significant time difference between client and server. While this does
     * not matter in replay mode where the client requests data for some arbitrary point in time, in live mode the
     * client at least should display the server time reasonably correct. Times in requests to the server in live mode
     * shall always be encoded as <code>null</code> to tell the server to use its server time. This will also help
     * improve the chances for cache hits.<p>
     * 
     * The time difference is maintained by applying an exponential moving average across the time differences announced through
     * {@link #setMillisecondsClientIsCurrentlyBehindServer}. The field starts out with 0 as default value, implying the client
     * clock has exactly the same time as the server clock.
     */
    private long millisecondsClientIsBehindServer;
    
    private boolean clientServerOffsetHasAtLeastBeenSetOnce;
    
    /**
     * The timer can run in two different modes: Live and Replay
     * 'Live' means the timer is used for a real time event
     * 'Replay' means the timer is used for an already finished event in the past 
     */
    public enum PlayModes { Live, Replay }; 

    public enum PlayStates { Stopped, Playing, Paused }; 

    /**
     * The timer is created in stopped state unless created with <code>playMode==PlayModes.Live</code>, using the
     * client's "now" as its current time, 1.0 as its {@link #playSpeedFactor play speed factor} and 1 second (1000ms)
     * as the {@link #refreshInterval delay between automatic updates} should the timer be {@link #resume() started}.
     * The {@link #millisecondsClientIsBehindServer offset} to the server time is initially left at 0ms until
     * updated by a call to {@link #adjustClientServerOffset(long, Date, long)}. 
     */
    public Timer(PlayModes playMode) {
        this(playMode, 1000);
    }
    
    /**
     * The timer is created in stopped state, using "now" as its current time, 1.0 as its {@link #playSpeedFactor
     * acceleration factor} and <code>delayBetweenAutoAdvancesInMilliseconds</code> as the
     * {@link #refreshInterval delay between automatic updates} should the timer be
     * {@link #resume() started}. The {@link #livePlayDelayInMillis} is set to zero seconds.
     */
    public Timer(PlayModes playMode, long refreshInterval) {
        this.refreshInterval = refreshInterval;
        // Using the client's clock is only a default; the time offset between client and server is adjusted when
        // information about the server time is present; see adjustClientServerOffset(...)
        time = new Date();
        timeListeners = new HashSet<TimeListener>();
        playStateListeners = new HashSet<PlayStateListener>();
        playState = PlayStates.Stopped;
        setPlaySpeedFactor(1.0);
        livePlayDelayInMillis = 0l;
        autoAdvance = true;
        setPlayMode(playMode);
    }
    
    public void addTimeListener(TimeListener listener) {
        timeListeners.add(listener);
    }
    
    public void removeTimeListener(TimeListener listener) {
        timeListeners.remove(listener);
    }
    
    public void addPlayStateListener(PlayStateListener listener) {
        playStateListeners.add(listener);
    }
    
    public void removePlayStateListener(PlayStateListener listener) {
        playStateListeners.remove(listener);
    }
    
    /**
     * Assumes that a service request was sent to the server at <code>timePointWhenRequestWasSent</code> for which the
     * response was received at <code>timePointWhenResponseWasReceived</code> and where the response tells
     * <code>currentServerTime</code> as the server time during processing the request. Based on this, calculates the
     * difference between the client's <code>System.currentTimeMillis()</code> and the current server time and, removing
     * half the time between send and receive time point to approximate the network latency. This gives a good
     * indication for how far client and server clock differ.
     * 
     * @param serverTimeDuringRequest may be <code>null</code> in which case no adjustment is performed
     */
    public void adjustClientServerOffset(long clientTimeWhenRequestWasSent, Date serverTimeDuringRequest, long clientTimeWhenResponseWasReceived) {
        if (serverTimeDuringRequest != null) {
            // Let's assume the calculation of the RaceTimesInfoDTO objects during the request takes almost no time compared
            // to network latency. Then the difference between the client's current time and the time when the request was sent
            // can be considered network latency. If we furthermore assume that the network latency is roughly symmetrical for
            // request and response, dividing the total latency by two will approximately tell us the time that passed between
            // when the server set the RaceTimesInfoDTO.currentServerTime field and the current time.
            long responseNetworkLatencyInMillis = (clientTimeWhenResponseWasReceived-clientTimeWhenRequestWasSent)/2l;
            long offset = serverTimeDuringRequest.getTime() + responseNetworkLatencyInMillis - clientTimeWhenResponseWasReceived;
            if (clientServerOffsetHasAtLeastBeenSetOnce) {
                final double exponentialMovingAverageFactor = 0.5;
                // TODO are all casts in place here to avoid strange rounding effects?
                millisecondsClientIsBehindServer = (long) (millisecondsClientIsBehindServer
                        * exponentialMovingAverageFactor + (1. - exponentialMovingAverageFactor) * offset);
            } else {
                millisecondsClientIsBehindServer = offset;
                clientServerOffsetHasAtLeastBeenSetOnce = true;
            }
        }
    }
    
    public void setTime(long timePointAsMillis) {
        Date oldTime = time;
        time = new Date(timePointAsMillis);
        if ((oldTime == null) != (time == null) || (oldTime != null && !oldTime.equals(time))) {
            for (TimeListener listener : timeListeners) {
                listener.timeChanged(time);
            }
        }
    }
    
    public void setPlaySpeedFactor(double playSpeedFactor) {
        this.playSpeedFactor = playSpeedFactor;
        for (PlayStateListener playStateListener : playStateListeners) {
            playStateListener.playSpeedFactorChanged(this.playSpeedFactor);
        }
    }
    
    public double getPlaySpeedFactor() {
        return playSpeedFactor;
    }
    
    public void setRefreshInterval(long refreshIntervalInMillis) {
        this.refreshInterval = refreshIntervalInMillis;
        if (playState == PlayStates.Playing) {
            refreshIntervalChanged = true;
        }
    }
    
    public long getRefreshInterval() {
        return refreshInterval;
    }

    public Date getTime() {
        return time;
    }

    /**
     * When setting the play mode to live, the timer will automatically be put into play state playing.
     */
    public void setPlayMode(PlayModes newPlayMode) {
        if (this.playMode != newPlayMode) {
            this.playMode = newPlayMode;
            for (PlayStateListener playStateListener : playStateListeners) {
                playStateListener.playStateChanged(playState, playMode);
            }
            if (newPlayMode == PlayModes.Live) {
                play();
            }
        }
    }    

    /**
     * Pauses this timer after the next time advance. {@link #playing} is set to <code>false</code> if not already
     * paused, and registered {@link PlayStateListener}s will be notified. If the play mode was live, it'll be set to
     * replay.
     */
    public void pause() {
        if (playState == PlayStates.Playing) {
            playState = PlayStates.Paused; // this will cause the repeating command to stop executing
            for (PlayStateListener playStateListener : playStateListeners) {
                playStateListener.playStateChanged(playState, playMode);
            }
            setPlayMode(PlayModes.Replay);
        }
    }

    /**
     * Puts the timer into the stopped state. If it was in live mode, puts it into replay mode.
     */
    public void stop() {
        if (playState == PlayStates.Playing) {
            playState = PlayStates.Stopped;
            for (PlayStateListener playStateListener : playStateListeners) {
                playStateListener.playStateChanged(playState, playMode);
            }
            setPlayMode(PlayModes.Replay);
        }
    }

    public void play() {
        if (playState != PlayStates.Playing) {
            playState = PlayStates.Playing;
            if (playMode == PlayModes.Live) {
                setTime(getLiveTimePointInMillis());
            }
            if (autoAdvance) {
                startAutoAdvance();
            }
            for (PlayStateListener playStateListener : playStateListeners) {
                playStateListener.playStateChanged(playState, playMode);
            }
        }
    }

    private void startAutoAdvance() {
        RepeatingCommand command = new RepeatingCommand() {
            @Override
            public boolean execute() {
                if (time != null && playState == PlayStates.Playing) {
                    long newTime = time.getTime();
                    if (playMode == PlayModes.Replay) {
                        newTime += (long) playSpeedFactor * refreshInterval;
                    } else {
                        // play mode is Live; quantize to make cache hits more likely
                        newTime = quantizeTimeStamp(getLiveTimePointInMillis()); 
                    }
                    setTime(newTime);
                }
                if (refreshIntervalChanged) {
                    refreshIntervalChanged = false;
                    scheduleAdvancerCommand(this);
                    return false; // stop this command; use the newly-scheduled one instead that uses the new frequency
                } else {
                    return playState == PlayStates.Playing ? true: false;
                }
            }

        };
        scheduleAdvancerCommand(command);
    }

    /**
     * In {@link PlayModes#Live live mode}, time stamps are quantized to make it more likely for the back-end to achieve
     * a cache hit. Quantization is controlled by the {@link #LIVE_CLOCK_QUANTUM} constant.
     */
    private long quantizeTimeStamp(long millis) {
        return millis - (millis % LIVE_CLOCK_QUANTUM);
    }

    private void scheduleAdvancerCommand(RepeatingCommand command) {
        Scheduler.get().scheduleFixedPeriod(command, (int) refreshInterval);
    }
    
    public void setLivePlayDelayInMillis(long delayInMilliseconds) {
        if (this.livePlayDelayInMillis != delayInMilliseconds) {
            this.livePlayDelayInMillis = delayInMilliseconds;
            if (getPlayMode() == PlayModes.Live) {
                setTime(getLiveTimePointInMillis());
            }
        }
    }
    
    public long getLivePlayDelayInMillis() {
        return livePlayDelayInMillis;
    }

    public PlayStates getPlayState() {
        return playState;
    }

    public PlayModes getPlayMode() {
        return playMode;
    }

    public boolean isAutoAdvance() {
        return autoAdvance;
    }

    public void setAutoAdvance(boolean autoAdvance) {
        this.autoAdvance = autoAdvance;
    }

    public long getLiveTimePointInMillis() {
        return System.currentTimeMillis() - getLivePlayDelayInMillis() + millisecondsClientIsBehindServer;
    }
    
    public Date getLiveTimePointAsDate() {
        return new Date(getLiveTimePointInMillis());
    }

}
