package com.sap.sse.mail.replication.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Test;

import com.sap.sse.common.mail.MailException;
import com.sap.sse.mail.replication.testsupport.AbstractMailServiceReplicationTest;

public class SimpleMailServiceReplicationTest extends AbstractMailServiceReplicationTest {
    @Test
    public void testReplicationOfSendMail() throws InterruptedException,
            IllegalAccessException, MailException {
        master.sendMail("test@ing.me", "oh what a beatiful mail", "to be sent on this day");
        replicaReplicator.waitUntilQueueIsEmpty();
        Thread.sleep(3000);
        assertThat("mail was not sent on replica",
                AbstractMailServiceReplicationTest.numberOfMailsSent.get(replica), equalTo(null));
        assertThat("mail was sent on master",
                AbstractMailServiceReplicationTest.numberOfMailsSent.get(master), equalTo(1));
    }
}
