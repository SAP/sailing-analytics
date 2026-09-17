package com.sap.sailing.landscape.common;

import java.io.Serializable;

/**
 * The result of a landscape operation which either completed successfully or was prevented because live content was
 * found. Technical failures continue to be reported as exceptions by the operation.
 */
public final class LiveContentAwareOperationResult<T> implements Serializable {
    private static final long serialVersionUID = -4892697307147697172L;
    private final boolean successful;
    private final T successfulResult;
    private final LiveContentCheckResult liveContentCheckResult;

    private LiveContentAwareOperationResult(final boolean successful, final T successfulResult,
            final LiveContentCheckResult liveContentCheckResult) {
        if (successful == (liveContentCheckResult != null)) {
            throw new IllegalArgumentException("A successful result must not have a live-content conflict and a failed result must have one");
        }
        this.successful = successful;
        this.successfulResult = successfulResult;
        this.liveContentCheckResult = liveContentCheckResult;
    }

    public static <T> LiveContentAwareOperationResult<T> success(final T successfulResult) {
        return new LiveContentAwareOperationResult<>(true, successfulResult, null);
    }

    public static <T> LiveContentAwareOperationResult<T> liveContentConflict(
            final LiveContentCheckResult liveContentCheckResult) {
        return new LiveContentAwareOperationResult<>(false, null, liveContentCheckResult);
    }

    public boolean isSuccessful() {
        return successful;
    }

    public T getSuccessfulResult() {
        return successfulResult;
    }

    public LiveContentCheckResult getLiveContentCheckResult() {
        return liveContentCheckResult;
    }
}
