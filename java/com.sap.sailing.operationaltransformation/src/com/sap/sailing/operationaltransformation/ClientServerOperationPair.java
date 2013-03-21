package com.sap.sailing.operationaltransformation;

public class ClientServerOperationPair<O extends Operation<?>> {
    private O clientOp;
    private O serverOp;

    public ClientServerOperationPair(O clientOp, O serverOp) {
	super();
	this.clientOp = clientOp;
	this.serverOp = serverOp;
    }
    
    public O getClientOp() {
        return clientOp;
    }
    
    public O getServerOp() {
        return serverOp;
    }
    
    @SuppressWarnings("unchecked")
    public static <O extends Operation<S>, S> O getNoOp() {
        return (O) new Operation<S>() {
            @Override
            public S applyTo(S toState) {
                return toState; // don't change anything, return state unchanged
            }
        };
    }
}
