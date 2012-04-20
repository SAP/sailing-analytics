package com.sap.sailing.server.operationaltransformation;

import com.sap.sailing.server.RacingEventService;



/**
 * Performs identical transformations for all operation types by simply returning the operation passed. Subclasses need
 * to override the transformation methods for those operation types that have an impact for them.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public abstract class AbstractRacingEventServiceOperation<ResultType> implements RacingEventServiceOperation<ResultType> {
    private static final long serialVersionUID = 3888231857034991271L;

    /**
     * Ignores the actual result of {@link #internalApplyTo(RacingEventService)} and returns <code>toState</code> which
     * for the operational transformation algorithm is the "next state reached."
     */
    @Override
    public RacingEventService applyTo(RacingEventService toState) {
        try {
            internalApplyTo(toState);
            return toState;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    @Override
    public RacingEventServiceOperation<?> transformRemoveLeaderboardClientOp(RemoveLeaderboard removeLeaderboard) {
        return removeLeaderboard;
    }

    @Override
    public RacingEventServiceOperation<?> transformRemoveLeaderboardServerOp(RemoveLeaderboard removeLeaderboard) {
        return removeLeaderboard;
    }

    @Override
    public RacingEventServiceOperation<?> transformAddLeaderboardClientOp(CreateLeaderboard addLeaderboard) {
        return addLeaderboard;
    }

    @Override
    public RacingEventServiceOperation<?> transformAddLeaderboardServerOp(CreateLeaderboard addLeaderboard) {
        return addLeaderboard;
    }

    @Override
    public RacingEventServiceOperation<?> transformRenameLeaderboardColumnClientOp(
            RenameLeaderboardColumn renameLeaderboardColumnClientOp) {
        return renameLeaderboardColumnClientOp;
    }

    @Override
    public RacingEventServiceOperation<?> transformRenameLeaderboardColumnServerOp(
            RenameLeaderboardColumn renameLeaderboardColumnServerOp) {
        return renameLeaderboardColumnServerOp;
    }

    @Override
    public RacingEventServiceOperation<?> transformRemoveColumnFromLeaderboardServerOp(RemoveLeaderboardColumn removeColumnFromLeaderboardServerOp) {
        return removeColumnFromLeaderboardServerOp;
    }

    @Override
    public RacingEventServiceOperation<?> transformRemoveColumnFromLeaderboardClientOp(RemoveLeaderboardColumn removeColumnFromLeaderboardClientOp) {
        return removeColumnFromLeaderboardClientOp;
    }
    
    @Override
    public RacingEventServiceOperation<?> transformAddColumnToLeaderboardClientOp(
            AddColumnToLeaderboard addColumnToLeaderboard) {
        return addColumnToLeaderboard;
    }

    @Override
    public RacingEventServiceOperation<?> transformAddColumnToLeaderboardServerOp(
            AddColumnToLeaderboard addColumnToLeaderboard) {
        return addColumnToLeaderboard;
    }

    @Override
    public RacingEventServiceOperation<?> transformMoveLeaderboardColumnDownClientOp(
            MoveLeaderboardColumnDown moveLeaderboardColumnDown) {
        return moveLeaderboardColumnDown;
    }

    @Override
    public RacingEventServiceOperation<?> transformMoveLeaderboardColumnDownServerOp(
            MoveLeaderboardColumnDown moveLeaderboardColumnDown) {
        return moveLeaderboardColumnDown;
    }

    @Override
    public RacingEventServiceOperation<?> transformMoveLeaderboardColumnUpClientOp(
            MoveLeaderboardColumnUp moveLeaderboardColumnUp) {
        return moveLeaderboardColumnUp;
    }

    @Override
    public RacingEventServiceOperation<?> transformMoveLeaderboardColumnUpServerOp(
            MoveLeaderboardColumnUp moveLeaderboardColumnUp) {
        return moveLeaderboardColumnUp;
    }

    public static RacingEventServiceOperation<Void> getNoOp() {
        return new AbstractRacingEventServiceOperation<Void>() {
            private static final long serialVersionUID = -7203280393485688834L;

            @Override
            public Void internalApplyTo(RacingEventService toState) {
                return null;
            }

            @Override
            public RacingEventServiceOperation<Void> transformClientOp(RacingEventServiceOperation<?> serverOp) {
                return this;
            }

            @Override
            public RacingEventServiceOperation<Void> transformServerOp(RacingEventServiceOperation<?> clientOp) {
                return this;
            }
            
            @Override
            public String toString() {
                return "noop";
            }
        };
    }
}
