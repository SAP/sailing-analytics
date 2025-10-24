package com.sap.sailing.news;

import java.util.Collection;

import com.sap.sailing.domain.base.Event;

/**
 * Interface to be implemented by news item providers. When registering instances of classes
 * that implement this interface with the OSGi service registry, they will automatically be
 * picked up by this bundle's {@link EventNewsProviderRegistry}, when such an
 * instance is removed from the OSGi registry, e.g., because its bundle is stopped, then
 * the provider is automatically removed from the {@link EventNewsProviderRegistry}.
 *
 * @see Activator
 */
public interface EventNewsProvider {
    Collection<? extends EventNewsItem> getNews(Event event);
}
