package com.sap.sailing.gwt.autoplay.client.nodes.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;

public class AutoPlaySequenceNode extends BaseCompositeNode {
    private List<AutoPlayNode> nodes = new ArrayList<>();
    private int sequenceTimePerNodeInSeconds;
    private int currentPos = -1;
    private Command onLoopEnd;

    public AutoPlaySequenceNode(String name, int sequenceTimePerNodeInSeconds, AutoPlayNode... nodes) {
        super(name);
        this.sequenceTimePerNodeInSeconds = sequenceTimePerNodeInSeconds;
        this.nodes.addAll(Arrays.asList(nodes));
    }
    
    private Timer transitionTimer = new Timer() {
        @Override
        public void run() {
            gotoNext();
        }
    };

    private void gotoNext() {
        currentPos++;
        if (currentPos > nodes.size() - 1) {
            if (onLoopEnd != null) {
                onLoopEnd.execute();
            }
            //do nothing we stay at the end position
        } else {
            transitionTo(nodes.get(currentPos));
        }
    }
    
    @Override
    protected void transitionTo(AutoPlayNode nextNode) {
        log("Sequence transition to " + nextNode);
        super.transitionTo(nextNode);
        if (!isStopped()) {
            transitionTimer.schedule(sequenceTimePerNodeInSeconds * 2000);
        }
    }
    
    public void setOnSequenceEnd(Command onLoopEnd) {
        this.onLoopEnd = onLoopEnd;
    }

    @Override
    public void onStart() {
        currentPos = -1;
        gotoNext();
    }

    @Override
    public void onStop() {
        super.onStop();
        for (AutoPlayNode autoPlayNode : nodes) {
            autoPlayNode.stop();
        }
        transitionTimer.cancel();
    }


}