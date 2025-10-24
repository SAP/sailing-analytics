package com.sap.sailing.gwt.autoplay.client.nodes.base;

public abstract class BaseCompositeNode extends AutoPlayNodeBase {
    private AutoPlayNode currentNode;

    public BaseCompositeNode(String name) {
        super(name);
    }

    public AutoPlayNode getCurrentNode() {
        return currentNode;
    }

    protected void transitionTo(AutoPlayNode nextNode) {
        if (nextNode == currentNode) {
            return;
        }
        if (isStopped()) {
            return;
        }
        if (currentNode != null) {
            currentNode.stop();
        }
        currentNode = nextNode;
        nextNode.start(getBus());
    }

    @Override
    public void onStop() {
        currentNode = null;
    }
}