package com.sap.sse.gwt.client.async;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.TimeRangeImpl;

public class TimeRangeActionsExecutorTest {

    private class DelayedTimeRangeAsyncAction<Result, Key> implements TimeRangeAsyncAction<Result, Key> {
        private final Result result;
        private final TimeRange timeRange;
        private final Key key;
        private Optional<AsyncCallback<Result>> optionalCallback = Optional.empty();
        public DelayedTimeRangeAsyncAction(Result result, TimeRange timeRange, Key key) {
            this.result = result;
            this.timeRange = timeRange;
            this.key = key;
        }
        public void returnResult() {
            optionalCallback.ifPresent(callback -> callback.onSuccess(result));
        }
        @Override
        public void execute(Map<Key,TimeRange> timeRanges, AsyncCallback<Result> callback) {
            optionalCallback = Optional.ofNullable(callback);
        }
        @Override
        public Map<Key, TimeRange> getTimeRanges() {
            final Map<Key, TimeRange> timeRangeMap = new HashMap<>(1);
            timeRangeMap.put(key, timeRange);
            return timeRangeMap;
        }
    }

    @Test
    public void testSimpleRequest() {
        TimeRangeActionsExecutor<Map<String, Integer>, Integer, String> exec = new TimeRangeActionsExecutor<>();
        final AtomicBoolean actionHasRun = new AtomicBoolean(false);
        final AtomicBoolean callbackHasRun = new AtomicBoolean(false);
        exec.execute(new TimeRangeAsyncAction<Map<String, Integer>, String>() {
            @Override
            public void execute(Map<String, TimeRange> timeRanges,
                    AsyncCallback<Map<String, Integer>> callback) {
                actionHasRun.set(true);
                Map<String, Integer> results = new HashMap<>();
                for (final String key : timeRanges.keySet()) {
                    results.put(key, key.length());
                }
                callback.onSuccess(results);
            }

            @Override
            public Map<String, TimeRange> getTimeRanges() {
                final Map<String, TimeRange> timeRangeMap = new HashMap<>();
                timeRangeMap.put("a", new MilliTimeRange(0, 1));
                timeRangeMap.put("bb", new MilliTimeRange(1, 2));
                return timeRangeMap;
            }
        }, new TimeRangeAsyncCallback<Map<String, Integer>, Integer, String>() {
            @Override
            public Map<String, Integer> unzipResult(Map<String, Integer> result) {
                return result;
            }

            @Override
            public void onSuccess(Map<String, Integer> result) {
                callbackHasRun.set(true);
                assertEquals(2, result.size());
                for (Map.Entry<String, Integer> entry : result.entrySet()) {
                    assertEquals(entry.getKey().length(), entry.getValue().intValue());
                }
            }

            @Override
            public void onFailure(Throwable caught) {
                assertTrue(false);
            }

            @Override
            public Integer joinSubResults(TimeRange timeRange, List<Pair<TimeRange, Integer>> toJoin) {
                return toJoin.get(0).getB();
            }

            @Override
            public Map<String, Integer> zipSubResults(Map<String, Integer> subResultMap) {
                return subResultMap;
            }
        });
        assertTrue(actionHasRun.get());
        assertTrue(callbackHasRun.get());
    }

    @Test
    public void testRequestCachedResults() {
        final TimeRangeActionsExecutor<Map<String, Integer>, Integer, String> exec = new TimeRangeActionsExecutor<>();
        final String key = "key";
        final TimeRange firstRange = TimeRangeImpl.create(10_000L, 20_000L);
        final TimeRange firstKeepAliveRange = TimeRangeImpl.create(8_000L, 12_000L);
        final TimeRange secondRange = TimeRangeImpl.create(15_000L, 20_000L);
        final Map<String, Integer> firstResult = new HashMap<>();
        firstResult.put(key, 1);
        final DelayedTimeRangeAsyncAction<Map<String, Integer>, String> firstAction = new DelayedTimeRangeAsyncAction<>(
                firstResult, firstRange, key);
        final DelayedTimeRangeAsyncAction<Map<String, Integer>, String> secondAction = new DelayedTimeRangeAsyncAction<>(
                null, secondRange, key);
        final AtomicBoolean callbackHasRun = new AtomicBoolean(false);
        exec.execute(firstAction, new TimeRangeAsyncCallback<Map<String, Integer>, Integer, String>() {
            @Override
            public Map<String, Integer> zipSubResults(Map<String, Integer> subResultMap) {
                return subResultMap;
            }
            @Override
            public Map<String, Integer> unzipResult(Map<String, Integer> result) {
                return result;
            }
            @Override
            public void onSuccess(Map<String, Integer> result) {
                // Nop
            }
            @Override
            public void onFailure(Throwable caught) {
                assertTrue(false);
            }
            @Override
            public Integer joinSubResults(TimeRange timeRange, List<Pair<TimeRange, Integer>> toJoin) {
                return toJoin.stream().mapToInt(Pair::getB).sum();
            }
        });
        exec.execute(new DelayedTimeRangeAsyncAction<>(null, firstKeepAliveRange, key),
                new TimeRangeAsyncCallback<Map<String, Integer>, Integer, String>() {
                    @Override
                    public Map<String, Integer> zipSubResults(Map<String, Integer> subResultMap) {
                        // Not used
                        return null;
                    }
                    @Override
                    public Map<String, Integer> unzipResult(Map<String, Integer> result) {
                        // Not used
                        return null;
                    }
                    @Override
                    public void onSuccess(Map<String, Integer> result) {
                        // Not used
                    }
                    @Override
                    public void onFailure(Throwable caught) {
                        assertTrue(false);
                    }
                    @Override
                    public Integer joinSubResults(TimeRange timeRange, List<Pair<TimeRange, Integer>> toJoin) {
                        // Not used
                        return null;
                    }
                });
        firstAction.returnResult();
        exec.execute(secondAction, new TimeRangeAsyncCallback<Map<String, Integer>, Integer, String>() {
            @Override
            public Map<String, Integer> zipSubResults(Map<String, Integer> subResultMap) {
                return subResultMap;
            }
            @Override
            public Map<String, Integer> unzipResult(Map<String, Integer> result) {
                return result;
            }
            @Override
            public void onSuccess(Map<String, Integer> result) {
                assertFalse(callbackHasRun.getAndSet(true));
            }
            @Override
            public void onFailure(Throwable caught) {
               assertTrue(false);
            }
            @Override
            public Integer joinSubResults(TimeRange timeRange, List<Pair<TimeRange, Integer>> toJoin) {
                return toJoin.stream().mapToInt(Pair::getB).sum();
            }
        });
        secondAction.returnResult();
        assertTrue(callbackHasRun.get());
    }

    @Test
    public void testOutOfOrderDelivery() {
        TimeRangeActionsExecutor<Map<String, Integer>, Integer, String> exec = new TimeRangeActionsExecutor<>();
        final AtomicBoolean callbackHasRun = new AtomicBoolean(false);
        AtomicReference<AsyncCallback<Map<String, Integer>>> lateCallback = new AtomicReference<>();
        // Fire off first request which will simulate a long round trip time
        exec.execute(new TimeRangeAsyncAction<Map<String, Integer>, String>() {
            @Override
            public void execute(Map<String, TimeRange> timeRanges,
                    AsyncCallback<Map<String, Integer>> callback) {
                // Don't call callback now
                lateCallback.set(callback);
            }

            @Override
            public Map<String, TimeRange> getTimeRanges() {
                final Map<String, TimeRange> timeRangeMap = new HashMap<>(1);
                timeRangeMap.put("x", TimeRangeImpl.create(100_000L, 120_000L));
                return timeRangeMap;
            }
        }, new TimeRangeAsyncCallback<Map<String, Integer>, Integer, String>() {

            @Override
            public Map<String, Integer> unzipResult(Map<String, Integer> result) {
                return result;
            }

            @Override
            public void onSuccess(Map<String, Integer> result) {
                assertEquals(Integer.valueOf(1), result.get("x"));
            }

            @Override
            public void onFailure(Throwable caught) {
                assertTrue(false); // Fail test
            }

            @Override
            public Integer joinSubResults(TimeRange timeRange, List<Pair<TimeRange, Integer>> toJoin) {
                return toJoin.stream().mapToInt(Pair::getB).sum();
            }

            @Override
            public Map<String, Integer> zipSubResults(Map<String, Integer> subResultMap) {
                return subResultMap;
            }
        });
        // Fire off second request which requires the results from the first request but that one is still in transit
        final TimeRange timeRange = TimeRangeImpl.create(110_000L, 130_000L);
        exec.execute(new TimeRangeAsyncAction<Map<String, Integer>, String>() {
            @Override
            public void execute(Map<String, TimeRange> timeRanges,
                    AsyncCallback<Map<String, Integer>> callback) {
                Map<String, Integer> result = new HashMap<>();
                result.put("x", 2);
                callback.onSuccess(result);
            }

            @Override
            public Map<String, TimeRange> getTimeRanges() {
                final Map<String, TimeRange> timeRangeMap = new HashMap<>();
                timeRangeMap.put("x", timeRange);
                return timeRangeMap;
            }
        }, new TimeRangeAsyncCallback<Map<String, Integer>, Integer, String>() {

            @Override
            public Map<String, Integer> unzipResult(Map<String, Integer> result) {
                return result;
            }

            @Override
            public void onSuccess(Map<String, Integer> result) {
                assertNotNull(result.get("x"));
                assertEquals(Integer.valueOf(3), result.get("x"));
                assertFalse(callbackHasRun.getAndSet(true));
            }

            @Override
            public void onFailure(Throwable caught) {
                assertTrue(false); // Fail test
            }

            @Override
            public Integer joinSubResults(TimeRange timeRange, List<Pair<TimeRange, Integer>> toJoin) {
                return toJoin.stream().mapToInt(Pair::getB).sum();
            }

            @Override
            public Map<String, Integer> zipSubResults(Map<String, Integer> subResultMap) {
                return subResultMap;
            }
        });
        // After the second request has finished the first one now gets answered
        Map<String, Integer> resultsFromFirstRequest = new HashMap<>();
        resultsFromFirstRequest.put("x", 1);
        lateCallback.get().onSuccess(resultsFromFirstRequest);
        assertTrue(callbackHasRun.get());
    }
}
