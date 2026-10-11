package com.sap.sailing.racecommittee.app.utils;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import androidx.annotation.Nullable;

import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.concurrent.CopyOnWriteHashMap;

import java.util.Calendar;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

/**
 * Reports a tick every second as a singleton.
 */
public enum TickSingleton {
    INSTANCE;

    private static final long DEFAULT_INTERVAL = 1000L;

    private final Map<Integer, Set<TickListener>> listenersByMillisecond = new CopyOnWriteHashMap<>();

    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(Message msg) {
            TimePoint now = MillisecondsTimePoint.now();
            final long delay = now.asMillis() / 1000L * 1000L + msg.what + DEFAULT_INTERVAL - now.asMillis();
            handler.sendEmptyMessageDelayed(msg.what, delay);
            //Notify all listeners
            final Set<TickListener> listeners = listenersByMillisecond.get(msg.what);
            if (listeners != null) {
                for (TickListener listener : listeners) {
                    listener.notifyTick(now);
                }
            }
        }
    };

    /**
     * If the {@link TickListener} has been registered, a new tick will be invoked immediately
     * to ensure that {@link TickListener#notifyTick(TimePoint)} is called as soon as possible.
     *
     * @param toRegister The listener to register
     */
    public void registerListener(TickListener toRegister) {
        registerListenerForMillisecond(toRegister, 0);
    }

    /**
     * @param toRegister The listener to register
     * @param timePoint The time point from which the millisecond is extracted and the tick occurs at
     * @see TickSingleton#registerListener(TickListener toRegister)
     */
    public void registerListener(TickListener toRegister, @Nullable TimePoint timePoint) {
        if (timePoint == null) {
            registerListener(toRegister);
            return;
        }
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(timePoint.asDate());
        final int millisecond = calendar.get(Calendar.MILLISECOND);
        registerListenerForMillisecond(toRegister, millisecond);
    }

    /**
     * Unregisters a listener.
     *
     * @param toUnregister The listener to unregister
     */
    public void unregisterListener(TickListener toUnregister) {
        for (Map.Entry<Integer, Set<TickListener>> entry : listenersByMillisecond.entrySet()) {
            final Set<TickListener> listeners = entry.getValue();
            if (listeners.remove(toUnregister)) {
                if (listeners.isEmpty()) {
                    listenersByMillisecond.remove(entry.getKey());
                    handler.removeMessages(0);
                }
            }
        }
    }

    private void registerListenerForMillisecond(TickListener listener, int millis) {
        Set<TickListener> listeners = listenersByMillisecond.get(millis);
        if (listeners == null) {
            listeners = new CopyOnWriteArraySet<>();
            listenersByMillisecond.put(millis, listeners);
        }
        if (listeners.add(listener)) {
            handler.removeMessages(millis);
            handler.sendEmptyMessage(millis);
        }
    }
}
