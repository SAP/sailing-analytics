package com.sap.sailing.racecommittee.app.ui.views;

import android.content.Context;
import androidx.annotation.Nullable;
import android.util.AttributeSet;

import com.sap.sailing.domain.abstractlog.race.state.RaceState;
import com.sap.sailing.racecommittee.app.utils.TickListener;
import com.sap.sailing.racecommittee.app.utils.TickSingleton;
import com.sap.sailing.racecommittee.app.utils.TimeUtils;
import com.sap.sse.common.TimePoint;

public class RaceTimeView extends androidx.appcompat.widget.AppCompatTextView {

    private TimePoint startTime;
    private TickListener listener;

    public RaceTimeView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        if (listener != null) {
            TickSingleton.INSTANCE.registerListener(listener, startTime);
        }
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        unregisterListener();
    }

    public void setRaceState(RaceState state) {
        startTime = state.getStartTime();
        unregisterListener();
        switch (state.getStatus()) {
            case SCHEDULED:
            case STARTPHASE:
            case RUNNING:
            case FINISHING:
                if (startTime == null) {
                    setVisibility(GONE);
                    setText(null);
                    return;
                }
                setVisibility(VISIBLE);
                listener = now -> setText(startTime != null ? TimeUtils.formatDuration(now, startTime) : null);
                TickSingleton.INSTANCE.registerListener(listener, startTime);
                break;
            default:
                setVisibility(GONE);
                setText(null);

        }
    }

    private void unregisterListener() {
        if (listener != null) {
            TickSingleton.INSTANCE.unregisterListener(listener);
            listener = null;
        }
    }
}
