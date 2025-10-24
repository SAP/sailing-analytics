package com.sap.sailing.server.trackfiles.impl;

import com.sap.sailing.domain.shared.tracking.Track;
import com.sap.sse.common.Timed;

class TrackReaderImpl<E, T extends Timed> implements TrackReader<E, T>, IterableLocker {
    private final Track<T> track;

    TrackReaderImpl(Track<T> track) {
        this.track = track;
    }

    @Override
    public void lock() {
        track.lockForRead();
    }

    @Override
    public void unlock() {
        track.unlockAfterRead();
    }

    @Override
    public Iterable<T> getTrack(E e) {
        return track.getFixes();
    }

    @Override
    public Iterable<T> getRawTrack(E e) {
        return track.getRawFixes();
    }

    @Override
    public IterableLocker getLocker() {
        return new IterableLocker() {
            @Override
            public void lock() {
                track.lockForRead();
            }

            @Override
            public void unlock() {
                track.unlockAfterRead();
            }
        };
    }
}