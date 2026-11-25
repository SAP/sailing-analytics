package com.sap.sailing.gwt.ui.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.gwt.ui.adminconsole.places.refresher.AbstractRefresher;
import com.sap.sailing.gwt.ui.client.Displayer;

public class AbstractRefresherTest {
    private static final List<String> INITIAL_DATA = Collections.singletonList("initial");
    private static final List<String> UPDATED_DATA = Collections.singletonList("updated");

    private static class TestRefresher extends AbstractRefresher<String> {
        private final List<AsyncCallback<Iterable<String>>> requests = new ArrayList<>();

        @Override
        public void reload(final AsyncCallback<Iterable<String>> callback) {
            requests.add(callback);
        }

        private void complete(final int requestIndex, final Iterable<String> data) {
            requests.get(requestIndex).onSuccess(data);
        }
    }

    private static class RecordingDisplayer implements Displayer<String> {
        private final List<Iterable<String>> results = new ArrayList<>();

        @Override
        public void fill(final Iterable<String> data) {
            results.add(data);
        }
    }

    @Test
    public void overlappingFillOnlyRequestsNotifyBothUnregisteredDisplayers() {
        final TestRefresher refresher = new TestRefresher();
        final RecordingDisplayer first = new RecordingDisplayer();
        final RecordingDisplayer second = new RecordingDisplayer();
        refresher.reloadAndCallFillOnly(first);
        refresher.reloadAndCallFillOnly(second);
        assertEquals(/* expected */ 1, refresher.requests.size());
        assertTrue(first.results.isEmpty());
        assertTrue(second.results.isEmpty());
        refresher.complete(/* requestIndex */ 0, INITIAL_DATA);
        assertEquals(Collections.singletonList(INITIAL_DATA), first.results);
        assertEquals(Collections.singletonList(INITIAL_DATA), second.results);
    }

    @Test
    public void initialFillJoinsInFlightLoadAndThenUsesCache() {
        final TestRefresher refresher = new TestRefresher();
        final RecordingDisplayer registered = new RecordingDisplayer();
        final RecordingDisplayer additional = new RecordingDisplayer();
        refresher.addDisplayerAndCallFillOnInit(registered);
        refresher.callFillAndReloadInitially(additional);
        refresher.callFillAndReloadInitially(additional);
        assertEquals(/* expected */ 1, refresher.requests.size());
        refresher.complete(/* requestIndex */ 0, INITIAL_DATA);
        assertEquals(Collections.singletonList(INITIAL_DATA), registered.results);
        assertEquals(Collections.singletonList(INITIAL_DATA), additional.results);
        final RecordingDisplayer cached = new RecordingDisplayer();
        refresher.callFillAndReloadInitially(cached);
        assertEquals(Collections.singletonList(INITIAL_DATA), cached.results);
        assertEquals(/* expected */ 1, refresher.requests.size());
    }

    @Test
    public void registeredAdditionalDisplayerIsIncludedInFillOnlyRequest() {
        final TestRefresher refresher = new TestRefresher();
        final RecordingDisplayer registered = new RecordingDisplayer();
        final RecordingDisplayer only = new RecordingDisplayer();
        refresher.addDisplayerAndCallFillOnInit(registered);
        refresher.complete(/* requestIndex */ 0, INITIAL_DATA);
        refresher.reloadAndCallFillOnly(only, registered);
        refresher.complete(/* requestIndex */ 1, UPDATED_DATA);
        assertEquals(Arrays.asList(INITIAL_DATA, UPDATED_DATA), registered.results);
        assertEquals(Collections.singletonList(UPDATED_DATA), only.results);
    }

    @Test
    public void sameOnlyAndAdditionalDisplayerIsNotNotifiedTwice() {
        final TestRefresher refresher = new TestRefresher();
        final RecordingDisplayer displayer = new RecordingDisplayer();
        refresher.reloadAndCallFillOnly(displayer, displayer);
        refresher.complete(/* requestIndex */ 0, INITIAL_DATA);
        assertEquals(Collections.singletonList(INITIAL_DATA), displayer.results);
    }

    @Test
    public void fillAllJoinsFillOnlyAndUnionsBothParametersOfLaterRequests() {
        final TestRefresher refresher = new TestRefresher();
        final RecordingDisplayer registered = new RecordingDisplayer();
        final RecordingDisplayer first = new RecordingDisplayer();
        final RecordingDisplayer second = new RecordingDisplayer();
        final RecordingDisplayer additional = new RecordingDisplayer();
        refresher.addDisplayerAndCallFillOnInit(registered);
        refresher.complete(/* requestIndex */ 0, INITIAL_DATA);
        refresher.reloadAndCallFillOnly(first);
        refresher.reloadAndCallFillOnly(second, additional);
        refresher.reloadAndCallFillAll();
        refresher.reloadAndCallFillOnly(registered, first);
        assertEquals(/* expected */ 2, refresher.requests.size());
        refresher.complete(/* requestIndex */ 1, UPDATED_DATA);
        assertEquals(Arrays.asList(INITIAL_DATA, UPDATED_DATA), registered.results);
        assertEquals(Collections.singletonList(UPDATED_DATA), first.results);
        assertEquals(Collections.singletonList(UPDATED_DATA), second.results);
        assertEquals(Collections.singletonList(UPDATED_DATA), additional.results);
    }

    @Test
    public void fillAllCapturesRegisteredDisplayersAtRequestTime() {
        final TestRefresher refresher = new TestRefresher();
        final RecordingDisplayer removed = new RecordingDisplayer();
        final RecordingDisplayer added = new RecordingDisplayer();
        refresher.addDisplayerAndCallFillOnInit(removed);
        refresher.complete(/* requestIndex */ 0, INITIAL_DATA);
        refresher.reloadAndCallFillAll();
        refresher.removeDisplayer(removed);
        refresher.addDisplayerAndCallFillOnInit(added);
        refresher.complete(/* requestIndex */ 1, UPDATED_DATA);
        assertEquals(Arrays.asList(INITIAL_DATA, UPDATED_DATA), removed.results);
        assertEquals(Collections.singletonList(INITIAL_DATA), added.results);
    }

    @Test
    public void reentrantRefreshKeepsItsRecipientsForTheNextCompletion() {
        final TestRefresher refresher = new TestRefresher();
        final RecordingDisplayer next = new RecordingDisplayer();
        final RecordingDisplayer additional = new RecordingDisplayer();
        final List<Iterable<String>> firstResults = new ArrayList<>();
        final Displayer<String> first = data -> {
            firstResults.add(data);
            refresher.reloadAndCallFillOnly(next);
        };
        refresher.reloadAndCallFillOnly(first, additional);
        refresher.complete(/* requestIndex */ 0, INITIAL_DATA);
        assertEquals(/* expected */ 2, refresher.requests.size());
        assertTrue(next.results.isEmpty());
        assertEquals(Collections.singletonList(INITIAL_DATA), additional.results);
        refresher.complete(/* requestIndex */ 1, UPDATED_DATA);
        assertEquals(Collections.singletonList(UPDATED_DATA), next.results);
        assertEquals(Collections.singletonList(INITIAL_DATA), firstResults);
        assertEquals(Collections.singletonList(INITIAL_DATA), additional.results);
    }

    @Test
    public void failedLoadCanBeRetriedWithoutNotifyingAbandonedOneShotDisplayers() {
        final TestRefresher refresher = new TestRefresher();
        final RecordingDisplayer registered = new RecordingDisplayer();
        final RecordingDisplayer abandoned = new RecordingDisplayer();
        final RecordingDisplayer retry = new RecordingDisplayer();
        refresher.addDisplayerAndCallFillOnInit(registered);
        refresher.callFillAndReloadInitially(abandoned);
        refresher.requests.get(/* index */ 0).onFailure(new IllegalStateException("Load failed"));
        refresher.callFillAndReloadInitially(retry);
        assertEquals(/* expected */ 2, refresher.requests.size());
        refresher.complete(/* requestIndex */ 1, UPDATED_DATA);
        assertEquals(Collections.singletonList(UPDATED_DATA), registered.results);
        assertEquals(Collections.singletonList(UPDATED_DATA), retry.results);
        assertTrue(abandoned.results.isEmpty());
    }
}
