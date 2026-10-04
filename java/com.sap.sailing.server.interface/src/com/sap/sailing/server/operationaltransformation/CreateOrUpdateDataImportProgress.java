package com.sap.sailing.server.operationaltransformation;

import java.util.UUID;

import com.sap.sailing.domain.common.DataImportProgress;
import com.sap.sailing.domain.common.DataImportSubProgress;
import com.sap.sailing.domain.common.MasterDataImportObjectCreationCount;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.interfaces.RacingEventServiceOperation;

public class CreateOrUpdateDataImportProgress extends AbstractRacingEventServiceOperation<Void> {

    private static final long serialVersionUID = 1980532937717924118L;

    private UUID importOperationId;
    private double overallProgressPct;
    private DataImportSubProgress subProgress;
    private double subProgressPct;
    /**
     * The import result to publish on the replica once the whole import has completed, or {@code null} for the many
     * intermediate progress updates that carry no result yet. Replicated so that a client polling a replica sees
     * {@link com.sap.sailing.domain.common.DataImportProgress#getResult()} turn non-{@code null} only at true
     * completion, in step with the master (see bug6227). Kept serialization-compatible with the previous field set by
     * leaving the {@code serialVersionUID} unchanged: an older replica ignores this extra field and a newer replica
     * defaults it to {@code null}, which is exactly the pre-completion behavior.
     */
    private MasterDataImportObjectCreationCount result;

    public CreateOrUpdateDataImportProgress(UUID importOperationId, double overallProgressPct, 
            DataImportSubProgress subProgress, double subProgressPct) {
        this(importOperationId, overallProgressPct, subProgress, subProgressPct, /* result */ null);
    }

    public CreateOrUpdateDataImportProgress(UUID importOperationId, double overallProgressPct,
            DataImportSubProgress subProgress, double subProgressPct,
            final MasterDataImportObjectCreationCount result) {
        this.importOperationId = importOperationId;
        this.overallProgressPct = overallProgressPct;
        this.subProgress = subProgress;
        this.subProgressPct = subProgressPct;
        this.result = result;
    }

    @Override
    public Void internalApplyTo(RacingEventService toState) throws Exception {
        final DataImportProgress progress = toState.createOrUpdateDataImportProgressWithoutReplication(
                importOperationId, overallProgressPct, subProgress, subProgressPct);
        if (result != null) {
            progress.setResult(result);
        }
        return null;
    }

    @Override
    public RacingEventServiceOperation<?> transformClientOp(RacingEventServiceOperation<?> serverOp) {
        return null;
    }

    @Override
    public RacingEventServiceOperation<?> transformServerOp(RacingEventServiceOperation<?> clientOp) {
        return null;
    }

}
