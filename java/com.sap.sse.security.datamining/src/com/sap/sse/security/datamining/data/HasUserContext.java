package com.sap.sse.security.datamining.data;

import java.util.Collection;

import org.apache.shiro.cache.Cache;
import org.apache.shiro.cache.CacheManager;
import org.apache.shiro.session.Session;
import org.apache.shiro.session.mgt.eis.CachingSessionDAO;
import org.apache.shiro.subject.PrincipalCollection;

import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.datamining.annotations.Dimension;
import com.sap.sse.datamining.annotations.Statistic;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.impl.User;

public interface HasUserContext {
    User getUser();
    
    SecurityService getSecurityService();

    @Dimension(messageKey="Name")
    default String getName() {
        return getUser().getName();
    }
    
    @Dimension(messageKey="Locale")
    default String getLocale() {
        return getUser().getLocale() != null ? getUser().getLocale().getDisplayName() : "";
    }
    
    @Dimension(messageKey="HasFullName")
    default boolean hasFullName() {
        return Util.hasLength(getUser().getFullName());
    }
    
    @Dimension(messageKey="FullName")
    default String getFullName() {
        return getUser().getFullName();
    }
    
    @Dimension(messageKey="EMail")
    default String getEMail() {
        return getUser().getEmail();
    }
    
    @Dimension(messageKey="HasCompany")
    default boolean hasCompany() {
        return Util.hasLength(getUser().getCompany());
    }
    
    @Dimension(messageKey="Company")
    default String getCompany() {
        return getUser().getCompany();
    }
    
    @Dimension(messageKey="HasEmail")
    default boolean hasEmail() {
        return Util.hasLength(getUser().getEmail());
    }
    
    @Dimension(messageKey="IsEmailValidated")
    default boolean isEmailValidated() {
        return getUser().isEmailValidated();
    }
    
    @Statistic(messageKey="NumberOfPermissions")
    default int getNumberOfPermissions() {
        return Util.size(getUser().getPermissions());
    }

    @Statistic(messageKey="NumberOfPreferences")
    default int getNumberOfPreferences() {
        return getSecurityService().getAllPreferences(getUser().getName()).size();
    }
    
    default Session getSession() {
        final CacheManager cacheManager = getSecurityService().getCacheManager();
        final Cache<?, ?> activeSessionCache = cacheManager.getCache(CachingSessionDAO.ACTIVE_SESSION_CACHE_NAME);
        for (final Object i : activeSessionCache.values()) {
            if (i instanceof Session) {
                final Session session = (Session) i;
                for (final Object attributeKey : session.getAttributeKeys()) {
                    final Object attributeValue = session.getAttribute(attributeKey);
                    if (attributeValue instanceof PrincipalCollection) {
                        final PrincipalCollection pc = (PrincipalCollection) attributeValue;
                        final Collection<?> principalList = pc.fromRealm(getUser().getName());
                        if (principalList != null && !principalList.isEmpty()) {
                            return session;
                        }
                    }
                }
            }
        }
        return null;
    }
    
    @Statistic(messageKey="DurationSinceLastAccess")
    default Duration getDurationSinceLastAccess() {
        final Session session = getSession();
        return session == null ? null : TimePoint.of(session.getLastAccessTime()).until(TimePoint.now());
    }
    
    @Statistic(messageKey="DurationUntilSessionExpiry")
    default Duration getDurationUntilSessionExpiry() {
        final Session session = getSession();
        return session == null ? null : TimePoint.now().until(TimePoint.of(session.getLastAccessTime()).plus(Duration.ofMillis(session.getTimeout())));
    }
    
    @Dimension(messageKey="DidOptOutOfFeatureAndCommunityEmails")
    default boolean didOptOutOfFeatureAndCommunityEmails() {
        return getUser().getDidOptOutOfFeatureAndCommunityEmails();
    }
}
