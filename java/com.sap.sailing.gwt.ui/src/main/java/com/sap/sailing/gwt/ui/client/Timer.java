package com.sap.sailing.gwt.ui.client;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;

/**
 * Manages a timer and can auto-advance it at a given acceleration/deceleration rate, with a given delay compared to
 * real time. It can be {@link #pause() paused} and {@link #resume() resumed}.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public class Timer {
    /**
     * The time point represented by this timer
     */
    private Date time;

    /**
     * The time delay to the current point in time in millisseconds which
     * will only be used in {@link PlayModes#Live} play mode.  
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
     * The current play mode of the timer
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
     */
    private double playSpeedFactor;
    
    /**
     * The timer can run in two different modes: Live and Replay
     * 'Live' means the timer is used for a real time event
     * 'Replay' means the timer is used for an already finished event in the past 
     */
    public enum PlayModes { Live, Replay }; 

    public enum PlayStates { Stopped, Playing, Paused }; 

    /**
     * The timer is created in stopped state, using "now" as its current time, 1.0 as its {@link #playSpeedFactor play speed factor} and
     * 1 second (1000ms) as the {@link #refreshInterval delay between automatic updates} should the timer be
     * {@link #resume() started}.
     */
    public Timer(PlayModes playMode) {
        this(playMode, 1000);
    }
    
    /**
     * The timer is created in stopped state, using "now" as its current time, 1.0 as its {@link #playSpeedFactor
     * acceleration factor} and <code>delayBetweenAutoAdvancesInMilliseconds</code> as the
     * {@link #refreshInterval delay between automatic updates} should the timer be
     * {@link #resume() started}. The {@link #livePlayDelayInMillis} is set to five seconds (5000ms).
     */
    public Timer(PlayModes playMode, long refreshInterval) {
        this.refreshInterval = refreshInterval;
        this.playMode = playMode;
        time = new Date();
        timeListeners = new HashSet<TimeListener>();
        playStateListeners = new HashSet<PlayStateListener>();
        playState = PlayStates.Stopped;
        playSpeedFactor = 1.0;
        livePlayDelayInMillis = 5000l;
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
    }
    
    public void setRefreshInterval(long refreshInterval) {
        this.refreshInterval = refreshInterval;
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

    public void setPlayMode(PlayModes newPlayMode) {
        if (this.playMode != newPlayMode) {
            this.playMode = newPlayMode;
            for (PlayStateListener playStateListener : playStateListeners) {
                playStateListener.playStateChanged(playState, playMode);
            }
        }
    }    

    /**
     * Pauses this timer after the next time advance. {@link #playing} is set to <code>false</code> if not already
     * paused, and registered {@link PlayStateListener}s will be notified.
     */
    public void pause() {
        if (playState == PlayStates.Playing) {
            playState = PlayStates.Paused; // this will cause the repeating command to stop executing
            
            for (PlayStateListener playStateListener : playStateListeners) {
                playStateListener.playStateChanged(playState, playMode);
            }
        }
    }

    public void stop() {
        if (playState == PlayStates.Playing) {
            playState = PlayStates.Stopped;

            for (PlayStateListener playStateListener : playStateListeners) {
                playStateListener.playStateChanged(playState, playMode);
            }
        }
    }

    public void play() {
        if (playState != PlayStates.Playing) {
            playState = PlayStates.Playing;
            if (playMode == PlayModes.Live) {
                setTime(System.currentTimeMillis()-livePlayDelayInMillis);
            }
            startAutoAdvance();
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
                    if (playMode == PlayModes.Replay)
                        newTime += (long) playSpeedFactor * refreshInterval;
                    else
                        newTime += refreshInterval;
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

    private void scheduleAdvancerCommand(RepeatingCommand command) {
        Scheduler.get().scheduleFixedPeriod(command, (int) refreshInterval);
    }
    
    public void setDelay(long delayInMilliseconds) {
        this.livePlayDelayInMillis = delayInMilliseconds;
        if (getPlayState() == PlayStates.Playing) {
            setTime(new Date().getTime() - delayInMilliseconds);
        }
    }

    public long getDelay() {
        return livePlayDelayInMillis;
    }
    
    /**
     * Tells how much the timer is currently behind "now." Note that this is not the same as asking
     * {@link #getLivePlayDelayInMillis()} which tells the delay that will be established if the timer is put
     * into live mode.
     */
    public long getCurrentDelayInMillis() {
        return System.currentTimeMillis() - getTime().getTime();
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
    
}
