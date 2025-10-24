package com.sap.sailing.gwt.autoplay.client.nodes.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import com.google.gwt.user.client.Timer;

public class AutoPlayLoopNode extends BaseCompositeNode {
    private List<AutoPlayNode> nodes = new ArrayList<>();
    private int loopTimePerNodeInSeconds;
    private int currentPos = -1;
    
    public AutoPlayLoopNode(String name, int loopTimePerNodeInSeconds, AutoPlayNode... nodes) {
        super(name);
        this.loopTimePerNodeInSeconds = loopTimePerNodeInSeconds;
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
            currentPos = 0;
        }
        transitionTo(nodes.get(currentPos));
    }

    @Override
    protected void transitionTo(AutoPlayNode nextNode) {
        if (!isStopped()) {
            // default loop transition, might be overridden by hook
            transitionTimer.schedule(loopTimePerNodeInSeconds * 1000);
            nextNode.customDurationHook(new Consumer<Integer>() {
                @Override
                public void accept(Integer durationInSeconds) {
                    if (!isStopped()) {
                        if (durationInSeconds == 0) {
                            //the node just told us that it is not required and we should switch asap
                            transitionTimer.cancel();
                            gotoNext();
                        } else {
                            transitionTimer.schedule(durationInSeconds * 1000);
                        }
                    }
                }
            });
            super.transitionTo(nextNode);
        }
    }

    @Override
    public void onStart() {
        log("Start sequence " + getName());
        currentPos = -1;
        gotoNext();
    }

    @Override
    public void onStop() {
        log("Stop sequence " + getName());
        super.onStop();
        for (AutoPlayNode autoPlayNode : nodes) {
            autoPlayNode.stop();
        }
        transitionTimer.cancel();
    }
}