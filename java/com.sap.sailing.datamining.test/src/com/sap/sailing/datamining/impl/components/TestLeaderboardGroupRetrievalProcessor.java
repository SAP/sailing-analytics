package com.sap.sailing.datamining.impl.components;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationInfo;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.authc.SimpleAccount;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.Permission;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.mgt.DefaultSecurityManager;
import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.apache.shiro.web.subject.support.WebDelegatingSubject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.datamining.data.HasLeaderboardGroupContext;
import com.sap.sailing.datamining.test.util.ConcurrencyTestsUtil;
import com.sap.sailing.datamining.test.util.NullProcessor;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.impl.LeaderboardGroupImpl;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.security.AbstractCompositeAuthorizingRealm;

public class TestLeaderboardGroupRetrievalProcessor {
    
    private RacingEventService service;
    private Processor<RacingEventService, HasLeaderboardGroupContext> retriever;
    
    private Collection<LeaderboardGroup> retrievedGroups;
    
    private static class DummyRealm extends AbstractCompositeAuthorizingRealm {
        public static final String REALM_NAME = "myRealm";
        private final SimplePrincipalCollection pc;
        
        public DummyRealm() {
            pc = new SimplePrincipalCollection();
            pc.add("admin", DummyRealm.REALM_NAME);
        }

        @Override
        protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
            return new SimpleAccount(pc, "admin".toCharArray(), REALM_NAME);
        }

        @Override
        public boolean isPermitted(PrincipalCollection principals, Permission perm) {
            return true;
        }

        @Override
        public boolean[] isPermitted(PrincipalCollection principals, List<Permission> permissions) {
            final boolean[] result = new boolean[permissions.size()];
            for (int i=0; i<result.length; i++) {
                result[i] = true;
            }
            return result;
        }
        
        @Override
        public boolean isPermitted(PrincipalCollection principals, String permissionString) {
            return true;
        }

        @Override
        public boolean isPermittedAll(PrincipalCollection principals, Collection<Permission> permissions) {
            return true;
        }

        @Override
        protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principals) {
            return new SimpleAuthorizationInfo(new HashSet<>(Arrays.asList(new String[] { "admin" })));
        }

        @Override
        public String getName() {
            return REALM_NAME;
        }
        
        public PrincipalCollection getPrincipalCollection() {
            return pc;
        }
    }
    
    @BeforeEach
    public void initializeComponents() {
        final DummyRealm realm = new DummyRealm();
        SecurityUtils.setSecurityManager(new DefaultSecurityManager(realm));
        final AuthenticationToken authenticationToken = new UsernamePasswordToken("admin", "admin".toCharArray());
        /**
         * PrincipalCollection principals, boolean authenticated,
                                String host, Session session,
                                ServletRequest request, ServletResponse response,
                                SecurityManager securityManager
         */
        final SecurityManager securityManager = SecurityUtils.getSecurityManager();
        final Subject subject = securityManager.login(
                new WebDelegatingSubject(realm.getPrincipalCollection(), /* authenticated */ true, "localhost", /* session */ null, /* request */ null,
                        /* response */ null, securityManager), authenticationToken);
        ThreadContext.bind(subject);
        service = mock(RacingEventService.class);
        when(service.getLeaderboardGroups()).thenReturn(getGroupsInService());
        retrievedGroups = new HashSet<>();
        Processor<HasLeaderboardGroupContext, Void> receiver = new NullProcessor<HasLeaderboardGroupContext, Void>(HasLeaderboardGroupContext.class, Void.class) {
            @Override
            public void processElement(HasLeaderboardGroupContext element) {
                retrievedGroups.add(element.getLeaderboardGroup());
            }
        };
        
        Collection<Processor<HasLeaderboardGroupContext, ?>> resultReceivers = new ArrayList<>();
        resultReceivers.add(receiver);
        retriever = new LeaderboardGroupRetrievalProcessor(ConcurrencyTestsUtil.getExecutor(), resultReceivers, 0, "");
    }

    private Map<UUID, LeaderboardGroup> getGroupsInService() {
        Map<UUID, LeaderboardGroup> groups = new HashMap<>();
        for (int i=1; i<=5; i++) {
            final LeaderboardGroupImpl lg = new LeaderboardGroupImpl("LG"+i, "", /* displayName */ null, false, new ArrayList<Leaderboard>());
            groups.put(lg.getId(), lg);
        }
        return groups;
    }

    @Test
    public void testFilteringRetrieval() throws InterruptedException {
        retriever.processElement(service);
        retriever.finish();
        final Set<LeaderboardGroup> expectedGroups = new HashSet<>(service.getLeaderboardGroups().values());
        assertThat(new HashSet<>(retrievedGroups), is(expectedGroups));
    }
}
