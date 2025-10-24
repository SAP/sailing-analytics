package com.sap.sse.mail.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.util.Arrays;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeUtility;

import com.sap.sse.common.IsManagedByCache;
import com.sap.sse.common.mail.MailException;
import com.sap.sse.mail.MailServiceResolver;
import com.sap.sse.mail.SerializableMultipartSupplier;
import com.sap.sse.mail.operations.MailServiceOperation;
import com.sap.sse.mail.operations.SendMailOperation;
import com.sap.sse.mail.operations.SendMailWithMultipartSupplierOperation;
import com.sap.sse.replication.interfaces.impl.AbstractReplicableWithObjectInputStream;
import com.sap.sse.util.ObjectInputStreamResolvingAgainstCache;

public class MailServiceImpl extends AbstractReplicableWithObjectInputStream<ReplicableMailService, MailServiceOperation<?>> implements ReplicableMailService {
    private static final Logger logger = Logger.getLogger(MailServiceImpl.class.getName());

    private Properties mailProperties;

    private final MailServiceResolver mailServiceResolver;
    
    public MailServiceImpl(Properties mailProperties, MailServiceResolver mailServiceResolver) {
        this.mailProperties = mailProperties;
        this.mailServiceResolver = mailServiceResolver;
    }

    private class SMTPAuthenticator extends javax.mail.Authenticator {
        public PasswordAuthentication getPasswordAuthentication() {
            String username = mailProperties.getProperty("mail.smtp.user");
            String password = mailProperties.getProperty("mail.smtp.password");
            return new PasswordAuthentication(username, password);
        }
    }

    protected static interface ContentSetter {
        void setContent(MimeMessage msg) throws MessagingException;
    }
    
    private boolean canSendMail() {
        return mailProperties != null && mailProperties.containsKey("mail.transport.protocol");
    }

    /**
     * Get the mail sending state based on 'mail.enabled' property from mail.properties. 
     * Replica disablement is not controlled by this key. 
     * Default if not set is false. 
     * 
     * @return true if mail.enabled is set to false (case insensitive)
     */
    private boolean isSendMailSetInactive() {
        final boolean mailInactive;
        if (mailProperties != null && mailProperties.get("mail.enabled") != null) {
            mailInactive = "false".equals(String.valueOf(mailProperties.get("mail.enabled")).toLowerCase());
        } else {
            // only disable mailing if explicit set to false, e.g. in testing cases this code need to be accessible
            mailInactive = false;
        }
        return mailInactive;
    }

    // protected for testing purposes
    protected void internalSendMail(String toAddress, String subject, ContentSetter contentSetter) throws MailException {
        if (canSendMail()) {
            if (toAddress != null) {
                final String[] toAddresses = toAddress.split(",");
                Session session = Session.getInstance(mailProperties, new SMTPAuthenticator());
                MimeMessage msg = new MimeMessage(session);
                ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
                try {
                    msg.setFrom(new InternetAddress(mailProperties.getProperty("mail.from", "root@sapsailing.com")));
                    try {
                        msg.setSubject(MimeUtility.encodeText(subject, "utf-8", "B"));
                    } catch (UnsupportedEncodingException e) {
                        msg.setSubject(subject);
                    }
                    for (final String singleToAddress : toAddresses) {
                        msg.addRecipient(RecipientType.TO, new InternetAddress(singleToAddress.trim()));
                    }
                    // this fixes the DCH MIME type error 
                    // see http://tanyamadurapperuma.blogspot.de/2014/01/struggling-with-nosuchproviderexception.html
                    Thread.currentThread().setContextClassLoader(javax.mail.Session.class.getClassLoader());
                    contentSetter.setContent(msg);
                    // for testing with gmail
                    //Transport ts = session.getTransport("smtps");
                    Transport ts = session.getTransport();
                    ts.connect();
                    ts.sendMessage(msg, msg.getRecipients(RecipientType.TO));
                    ts.close();
                    logger.info("mail sent to " + Arrays.toString(toAddresses) + " with subject " + subject);
                } catch (MessagingException e) {
                    logger.severe("Error trying to send mail to " + Arrays.toString(toAddresses)+": "+e.getMessage());
                } finally {
                    Thread.currentThread().setContextClassLoader(oldClassLoader);
                }
            }
        } else {
            logger.warning("No mail properties provided. Cannot send e-mail about " + subject + " to " + toAddress
                    + ". This could also mean that this is running on a replica server in which case this is perfectly fine. "
                    + "The master "+getMasterDescriptor()+" will handle the mail sending operation in this case.");
        }
    }

    @Override
    public Void internalSendMail(String toAddress, String subject, String body) throws MailException {
        internalSendMail(toAddress, subject, new ContentSetter() {
            @Override
            public void setContent(MimeMessage msg) throws MessagingException {
                msg.setContent(body, "text/plain; charset=UTF-8");
            }
        });
        return null;
    }

    @Override
    public void sendMail(String toAddress, String subject, String body) throws MailException {
        if (isSendMailSetInactive()) {
            logger.warning("would send email, currently disabled.");
            logger.info("toAddress: " + toAddress);
            logger.info("subject: " + subject);
            logger.info("body: " + body);
        } else {
            apply(new SendMailOperation(toAddress, subject, body));
        }
    }

    @Override
    public Void internalSendMail(String toAddress, String subject, SerializableMultipartSupplier multipartSupplier) throws MailException {
        internalSendMail(toAddress, subject, new ContentSetter() {
            @Override
            public void setContent(MimeMessage msg) throws MessagingException {
                msg.setContent(multipartSupplier.get());
            }
        });
        return null;
    }

    @Override
    public void sendMail(String toAddress, String subject, SerializableMultipartSupplier multipartSupplier) throws MailException {
        if (isSendMailSetInactive()) {
            logger.warning("would send email, currently disabled.");
            logger.info("toAddress: " + toAddress);
            logger.info("subject: " + subject);
            logger.info("multipartSupplier");
        } else {
            apply(new SendMailWithMultipartSupplierOperation(toAddress, subject, multipartSupplier));
        }
    }

    // ----------------- Replication -------------
    @Override
    public void clearReplicaState() throws MalformedURLException, IOException, InterruptedException {
        // do nothing
    }

    @Override
    public ObjectInputStream createObjectInputStreamResolvingAgainstCache(InputStream is, Map<String, Class<?>> classLoaderCache) throws IOException {
        return new ObjectInputStreamResolvingAgainstCache<MailServiceResolver>(is, mailServiceResolver, null, classLoaderCache) {
        };
    }

    @Override
    public IsManagedByCache<MailServiceResolver> resolve(MailServiceResolver cache) {
        return cache.getMailService();
    }

    @Override
    public void initiallyFillFromInternal(ObjectInputStream is) throws IOException, ClassNotFoundException,
            InterruptedException {
        // send mails only from master
        mailProperties = null;
    }

    @Override
    public void serializeForInitialReplicationInternal(ObjectOutputStream objectOutputStream) throws IOException {
        // do nothing
    }
}
