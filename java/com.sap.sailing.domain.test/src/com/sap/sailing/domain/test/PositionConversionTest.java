package com.sap.sailing.domain.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Collections;

import org.junit.Test;

import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.sap.sailing.domain.tractracadapter.LoadingQueueDoneCallBack;
import com.sap.sailing.domain.tractracadapter.Receiver;
import com.tractrac.model.lib.api.data.IPosition;
import com.tractrac.model.lib.api.route.IControl;
import com.tractrac.subscription.lib.api.control.IControlPointPositionListener;

public class PositionConversionTest extends AbstractTracTracLiveTest {

    public PositionConversionTest() throws URISyntaxException,
            MalformedURLException {
        super();
    }

    @Test
    public void testConnectivity() {
        // does nothing but test that set-up and tear-down works
    }

    @Test
    public void testReceiptOfControlPointPosition() {
        final IControl[] firstTracked = new IControl[1];
        final IPosition[] firstData = new IPosition[1];
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
                getRaceSubscriber().subscribeControlPositions(new IControlPointPositionListener() {
                    private boolean first = true;
                    
                    @Override
                    public void gotControlPointPosition(IControl control, IPosition position, int controlPointNumber) {
                        if (first) {
                            synchronized (semaphor) {
                                firstTracked[0] = control;
                                firstData[0] = position;
                                semaphor.notifyAll();
                            }
                            first = false;
                        }
                    }
                });
            }

            @Override
            public void callBackWhenLoadingQueueIsDone(LoadingQueueDoneCallBack callback) {
            }
        };
        addListenersForStoredDataAndStartController(Collections.singleton(receiver));
        synchronized (semaphor) {
            while (firstTracked[0] == null) {
                try {
                    semaphor.wait();
                } catch (InterruptedException e) {
                    // print, ignore, wait on
                    e.printStackTrace();
                }
            }
        }
        assertNotNull(firstTracked[0]);
        assertNotNull(firstData[0]);
        Position pos = DomainFactory.INSTANCE.createPosition(firstData[0]);
        assertNotNull(pos);
        assertEquals(firstData[0].getLatitude(), pos.getLatDeg(), /* epsilon */ 0.00000001);
        assertEquals(firstData[0].getLongitude(), pos.getLngDeg(), /* epsilon */ 0.00000001);
    }

}
