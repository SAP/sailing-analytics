package com.sap.sailing.domain.abstractlog.race.impl;

import java.io.Serializable;
import java.util.NavigableSet;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.AbstractLogImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogEventVisitor;
import com.sap.sailing.domain.shared.tracking.Track;
import com.sap.sailing.domain.shared.tracking.impl.TrackImpl;

/**
 * {@link Track} implementation for {@link RaceLogEvent}s.
 *
 * <p>
 * "Fix" validity is decided based on the {@link #getCurrentPassId() current pass}. The validity is not cached.
 * </p>
 *
 * <p>
 * {@link TrackImpl#getDummyFix(com.sap.sailing.domain.common.TimePoint)} is not overridden, see
 * {@link RaceLogEventComparator} for sorting when interface methods like
 * {@link Track#getFirstFixAfter(com.sap.sailing.domain.common.TimePoint)} are used.
 * </p>
 *
 */
public class RaceLogImpl extends AbstractLogImpl<RaceLogEvent, RaceLogEventVisitor> implements RaceLog {
    private static final long serialVersionUID = 98032278604708475L;

    public RaceLogImpl(Serializable identifier) {
        super(identifier, new RaceLogEventComparator());
    }

    public RaceLogImpl(String nameForReadWriteLock, Serializable identifier) {
        super(nameForReadWriteLock, identifier, new RaceLogEventComparator());
    }

    @Override
    public int getCurrentPassId() {
        lockForRead();
        try {
            // return pass id of last event, as pass is the top-level sorting criterion in RaceLogEventComparator
            final NavigableSet<RaceLogEvent> unrevokedEvents = getUnrevokedEvents();
            if (!unrevokedEvents.isEmpty()) {
                return unrevokedEvents.last().getPassId();
            } else {
                return DefaultPassId;
            }
        } finally {
            unlockAfterRead();
        }
    }

    @Override
    protected RaceLogEvent createRevokeEvent(AbstractLogEventAuthor author, RaceLogEvent toRevoke, String reason) {
        return new RaceLogRevokeEventImpl(author, getCurrentPassId(), toRevoke, reason);
    }

    @Override
    protected NavigableSet<RaceLogEvent> getInternalFixes() {
        return new FilteredPartialNavigableSetView<>(super.getInternalFixes(), new PassValidator(getCurrentPassId()));
    }
}
