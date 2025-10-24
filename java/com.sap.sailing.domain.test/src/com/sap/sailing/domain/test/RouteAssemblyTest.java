package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.sap.sailing.domain.tractracadapter.LoadingQueueDoneCallBack;
import com.sap.sailing.domain.tractracadapter.Receiver;
import com.sap.sse.common.Util;
import com.tractrac.model.lib.api.route.IControlRoute;
import com.tractrac.model.lib.api.route.IPathRoute;
import com.tractrac.subscription.lib.api.control.IControlRouteChangeListener;

public class RouteAssemblyTest extends AbstractTracTracLiveTest {

    public RouteAssemblyTest() throws URISyntaxException,
            MalformedURLException {
        super();
    }

    @Test
    public void testReceiveRouteData() {
        final IControlRoute[] firstRoute = new IControlRoute[1];
        final Object semaphor = new Object();
        
        Receiver receiver = new Receiver() {
            @Override
            public void stopPreemptively() {
            }

            @Override
            public void stopAfterProcessingQueuedEvents() {
            }

            @Override
            public void join() {
            }

            @Override
            public void join(long timeoutInMilliseconds) {
            }

            @Override
            public void stopAfterNotReceivingEventsForSomeTime(long timeoutInMilliseconds) {
            }

            @Override
            public void subscribe() {
                getRaceSubscriber().subscribeRouteChanges(new IControlRouteChangeListener() {
                    private boolean first = true;

                    @Override
                    public void gotRouteChange(IControlRoute controlRoute, long timeStamp) {
                        if (first) {
                            synchronized (semaphor) {
                                firstRoute[0] = controlRoute;
                                semaphor.notifyAll();
                                getRaceSubscriber().unsubscribeRouteChanges(this);
                            }
                            first = false;
                        }
                    }

                    @Override
                    public void gotRouteChange(IPathRoute pathRoute, long timeStamp) {
                        // will never be invoked for sailing events
                    }
                });
            }

            @Override
            public void callBackWhenLoadingQueueIsDone(LoadingQueueDoneCallBack callback) { 
            }
        };
        addListenersForStoredDataAndStartController(Collections.singleton(receiver));
        synchronized (semaphor) {
            while (firstRoute[0] == null) {
                try {
                    semaphor.wait();
                } catch (InterruptedException e) {
                    // print, ignore, wait on
                    e.printStackTrace();
                }
            }
        }
        assertNotNull(firstRoute[0]);
        Course course = DomainFactory.INSTANCE.createCourse(firstRoute[0].getName(), getTracTracControlPointsWithPassingInstructions(firstRoute[0].getControls()));
        assertNotNull(course);
        assertEquals("windward-leeward training", course.getName());
        assertEquals(3, Util.size(course.getWaypoints()));
        assertEquals(Util.size(course.getWaypoints())-1, Util.size(course.getLegs()));
    }

}
