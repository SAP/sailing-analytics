package com.sap.sse.mail.replication.testsupport;

import java.util.HashMap;
import java.util.Map;

import org.junit.Before;

import com.sap.sse.common.mail.MailException;
import com.sap.sse.mail.MailService;
import com.sap.sse.mail.MailServiceResolver;
import com.sap.sse.mail.SerializableMultipartSupplier;
import com.sap.sse.mail.impl.MailServiceImpl;
import com.sap.sse.replication.testsupport.AbstractServerReplicationTestSetUp;
import com.sap.sse.replication.testsupport.AbstractServerWithSingleServiceReplicationTest;

public class AbstractMailServiceReplicationTest extends AbstractServerWithSingleServiceReplicationTest<MailService, MailServiceImpl> {
    public AbstractMailServiceReplicationTest() {
        super(new MailServerReplicationTestSetUp());
    }

    public static final Map<MailServiceImpl, Integer> numberOfMailsSent = new HashMap<>();
    
    @Before
    public void clearNumberOfMailsSent() {
        numberOfMailsSent.clear();
    }
    
    public static MailServiceImpl createMailCountingService(final boolean canSendMail) {
        final MailServiceImpl[] mailService = new MailServiceImpl[1];
        MailServiceResolver mailServiceResolver = new MailServiceResolver() {
            @Override
            public MailService getMailService() {
                return mailService[0];
            }
        };
        mailService[0] = new MailServiceImpl(null, mailServiceResolver) {
            @Override
            public Void internalSendMail(String toAddress, String subject, SerializableMultipartSupplier multipartSupplier)
                    throws MailException {
                countOneIfCanSendMail(canSendMail);
                return null;
            }

            private void countOneIfCanSendMail(final boolean canSendMail) {
                if (canSendMail) {
                    Integer old = numberOfMailsSent.get(this);
                    numberOfMailsSent.put(this, old == null ? 1 : old + 1);
                }
            }

            @Override
            public Void internalSendMail(String toAddress, String subject, String body) throws MailException {
                countOneIfCanSendMail(canSendMail);
                return null;
            }
        };
        return mailService[0];
    }

    public static class MailServerReplicationTestSetUp extends AbstractServerReplicationTestSetUp<MailService, MailServiceImpl> {
        @Override
        protected MailServiceImpl createNewMaster() throws MailException {
            return createMailCountingService(true);
        }

        @Override
        protected MailServiceImpl createNewReplica() throws MailException {
            return createMailCountingService(false);
        }
    }

}
