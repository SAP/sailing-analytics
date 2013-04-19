package com.sap.sailing.server;

import java.io.Serializable;

import com.sap.sailing.operationaltransformation.Operation;
import com.sap.sailing.operationaltransformation.Transformer;
import com.sap.sailing.server.operationaltransformation.AddColumnToLeaderboard;
import com.sap.sailing.server.operationaltransformation.CreateFlexibleLeaderboard;
import com.sap.sailing.server.operationaltransformation.CreateRegattaLeaderboard;
import com.sap.sailing.server.operationaltransformation.MoveLeaderboardColumnDown;
import com.sap.sailing.server.operationaltransformation.MoveLeaderboardColumnUp;
import com.sap.sailing.server.operationaltransformation.RemoveLeaderboard;
import com.sap.sailing.server.operationaltransformation.RemoveLeaderboardColumn;
import com.sap.sailing.server.operationaltransformation.RenameLeaderboardColumn;

public interface RacingEventServiceOperation<ResultType> extends Operation<RacingEventService>, Serializable {
    /**
     * Performs the actual operation, applying it to the <code>toState</code> service. The operation's result is
     * returned.
     */
    ResultType internalApplyTo(RacingEventService toState) throws Exception;
    
    /**
     * Tells if this operation needs to be executed in order with other operations requesting synchronous execution.
     */
    boolean requiresSynchronousExecution();
    
    /**
     * Implements the specific transformation rule for the implementing subclass for the set of possible peer operations
     * along which to transform this operation, assuming this is the client operation. See
     * {@link Transformer#transform(Operation, Operation)} for the specification.
     * 
     * @return the result of transforming <code>this</code> operation along <code>serverOp</code>
     */
    RacingEventServiceOperation<?> transformClientOp(RacingEventServiceOperation<?> serverOp);

    /**
     * Implements the specific transformation rule for the implementing subclass for the set of possible peer operations
     * along which to transform this operation, assuming this is the server operation. See
     * {@link Transformer#transform(Operation, Operation)} for the specification.
     * 
     * @return the result of transforming <code>this</code> operation along <code>clientOp</code>
     */
    RacingEventServiceOperation<?> transformServerOp(RacingEventServiceOperation<?> clientOp);

    /**
     * Assumes this is the "server" operation and transforms the client's <code>removeColumnFromLeaderboardClientOp</code> according to this
     * operation. The default implementation will probably pass on the untransformed client operation. However, if this
     * operation deals with the leaderboard column being removed by <code>removeColumnFromLeaderboardClientOp</code>,
     * the result will be <code>null</code>, meaning that this operation cannot be applied after the column has been removed.
     */
    RacingEventServiceOperation<?> transformRemoveColumnFromLeaderboardClientOp(RemoveLeaderboardColumn removeColumnFromLeaderboardClientOp);

    RacingEventServiceOperation<?> transformRemoveColumnFromLeaderboardServerOp(RemoveLeaderboardColumn removeColumnFromLeaderboardServerOp);

    RacingEventServiceOperation<?> transformRenameLeaderboardColumnClientOp(RenameLeaderboardColumn renameLeaderboardColumnClientOp);

    RacingEventServiceOperation<?> transformRenameLeaderboardColumnServerOp(RenameLeaderboardColumn renameLeaderboardColumnServerOp);

    RacingEventServiceOperation<?> transformAddFlexibleLeaderboardClientOp(CreateFlexibleLeaderboard addLeaderboard);

    RacingEventServiceOperation<?> transformAddFlexibleLeaderboardServerOp(CreateFlexibleLeaderboard addLeaderboard);

    RacingEventServiceOperation<?> transformAddRegattaLeaderboardClientOp(CreateRegattaLeaderboard addLeaderboard);

    RacingEventServiceOperation<?> transformAddRegattaLeaderboardServerOp(CreateRegattaLeaderboard addLeaderboard);

    RacingEventServiceOperation<?> transformRemoveLeaderboardClientOp(RemoveLeaderboard removeLeaderboard);

    RacingEventServiceOperation<?> transformRemoveLeaderboardServerOp(RemoveLeaderboard removeLeaderboard);

    RacingEventServiceOperation<?> transformAddColumnToLeaderboardClientOp(AddColumnToLeaderboard addColumnToLeaderboard);

    RacingEventServiceOperation<?> transformAddColumnToLeaderboardServerOp(AddColumnToLeaderboard addColumnToLeaderboard);

    RacingEventServiceOperation<?> transformMoveLeaderboardColumnDownClientOp(MoveLeaderboardColumnDown moveLeaderboardColumnDown);

    RacingEventServiceOperation<?> transformMoveLeaderboardColumnDownServerOp(MoveLeaderboardColumnDown moveLeaderboardColumnDown);

    RacingEventServiceOperation<?> transformMoveLeaderboardColumnUpClientOp(MoveLeaderboardColumnUp moveLeaderboardColumnUp);

    RacingEventServiceOperation<?> transformMoveLeaderboardColumnUpServerOp(MoveLeaderboardColumnUp moveLeaderboardColumnUp);
}
