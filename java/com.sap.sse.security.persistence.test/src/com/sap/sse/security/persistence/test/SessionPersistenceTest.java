package com.sap.sse.security.persistence.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.SimpleSession;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.junit.jupiter.api.Test;

public class SessionPersistenceTest extends AbstractSecurityPersistenceTest {
    @Test
    public void testLoadAndStoreAndRemoveSimpleSession() {
        final String cacheName = "shiroSessionCache";
        final String host = "myHost";
        final String id = UUID.randomUUID().toString();
        final Date start = new Date();
        final Date last = new Date();
        final long timeout = 3600000l;
        final SimpleSession session = new SimpleSession();
        final String attr1Key = "a1";
        final String attr1Value = "a1Value";
        final String attr2Key = "a2";
        final boolean attr2Value = true;
        final String realm1Name = "rea lm1 ..,;";
        final String realm1Principal1Name = "r1.p1";
        final String realm1Principal2Name = "r1/\",,p2";
        final String realm2Name = "realm2,.\\\"'";
        final String realm2Principal1Name = "r2\\p1";
        final String realm2Principal2Name = "r2.'_p2";
        final String principalAttribute = "org.apache.shiro.subject.support.DefaultSubjectContext_PRINCIPALS_SESSION_KEY";
        final SimplePrincipalCollection principalCollection = new SimplePrincipalCollection();
        principalCollection.add(realm1Principal1Name, realm1Name);
        principalCollection.add(realm1Principal2Name, realm1Name);
        principalCollection.add(realm2Principal1Name, realm2Name);
        principalCollection.add(realm2Principal2Name, realm2Name);
        session.setId(id);
        session.setHost(host);
        session.setStartTimestamp(start);
        session.setLastAccessTime(last);
        session.setTimeout(timeout);
        session.setAttribute(attr1Key, attr1Value);
        session.setAttribute(attr2Key, attr2Value);
        session.setAttribute(principalAttribute, principalCollection);
        mof.storeSession(cacheName, session);
        final Map<String, Set<Session>> sessions = dof.loadSessionsByCacheName();
        final Session readSession = sessions.get(cacheName).iterator().next();
        assertEquals(session, readSession);
        assertEquals(id, readSession.getId());
        assertEquals(host, readSession.getHost());
        assertEquals(start, readSession.getStartTimestamp());
        assertEquals(last, readSession.getLastAccessTime());
        assertEquals(timeout, readSession.getTimeout());
        assertEquals(attr1Value, readSession.getAttribute(attr1Key));
        assertEquals(attr2Value, readSession.getAttribute(attr2Key));
        PrincipalCollection readPrincipleCollection = (PrincipalCollection) readSession.getAttribute(principalAttribute);
        assertTrue(readPrincipleCollection.fromRealm(realm1Name).contains(realm1Principal1Name));
        assertTrue(readPrincipleCollection.fromRealm(realm1Name).contains(realm1Principal2Name));
        assertTrue(readPrincipleCollection.fromRealm(realm2Name).contains(realm2Principal1Name));
        assertTrue(readPrincipleCollection.fromRealm(realm2Name).contains(realm2Principal2Name));
        
        mof.removeSession(cacheName, session);
        assertTrue(dof.loadSessionsByCacheName().isEmpty());
    }

    @Test
    public void testSessionExpiry() throws InterruptedException {
        final String cacheName = "shiroSessionCache";
        final String host = "myHost";
        final String id = UUID.randomUUID().toString();
        final Date start = new Date();
        final Date last = new Date();
        final long timeout = 5000l; // 5s
        final SimpleSession session = new SimpleSession();
        session.setId(id);
        session.setHost(host);
        session.setStartTimestamp(start);
        session.setLastAccessTime(last);
        session.setTimeout(timeout);
        session.setAttribute("a", "b");
        mof.storeSession(cacheName, session);
        final Map<String, Set<Session>> sessions = dof.loadSessionsByCacheName();
        // expecting the session to be still there because the 10s haven't expired yet:
        final Session readSession = sessions.get(cacheName).iterator().next();
        assertEquals(session, readSession);
        assertEquals(id, readSession.getId());
        assertEquals(host, readSession.getHost());
        assertEquals(start, readSession.getStartTimestamp());
        assertEquals(last, readSession.getLastAccessTime());
        assertEquals(timeout, readSession.getTimeout());
        Thread.sleep(timeout);
        final Map<String, Set<Session>> sessionsWithoutExpired = dof.loadSessionsByCacheName();
        assertTrue(sessionsWithoutExpired.isEmpty());
    }
}
