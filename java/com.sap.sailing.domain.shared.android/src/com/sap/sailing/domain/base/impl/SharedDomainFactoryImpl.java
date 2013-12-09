package com.sap.sailing.domain.base.impl;

import java.io.Serializable;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorStore;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.Gate;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Nationality;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.configuration.DeviceConfigurationMatcher;
import com.sap.sailing.domain.base.configuration.impl.DeviceConfigurationMatcherMulti;
import com.sap.sailing.domain.base.configuration.impl.DeviceConfigurationMatcherSingle;
import com.sap.sailing.domain.common.MarkType;
import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sailing.domain.common.WithID;
import com.sap.sailing.domain.common.configuration.DeviceConfigurationMatcherType;

public class SharedDomainFactoryImpl implements SharedDomainFactory {
    
    /**
     * Ensure that the <em>same</em> string is used as key that is also used to set the {@link Nationality}
     * object's {@link Nationality#getThreeLetterIOCAcronym() IOC code}.
     */
    private final Map<String, Nationality> nationalityCache;
    
    private final Map<Serializable, Mark> markCache;
    
    /**
     * For all marks ever created by this factory, the mark {@link WithID#getId() ID}'s string representation
     * is mapped here to the actual ID. This allows clients to send only the string representation to the server
     * and still be able to identify a mark uniquely this way.
     */
    private final Map<String, Serializable> markIdCache;
    
    private final Map<String, BoatClass> boatClassCache;
    
    protected final CompetitorStore competitorStore;
    
    private final Map<Serializable, CourseArea> courseAreaCache;
    
    /**
     * Weakly references the waypoints. If a waypoint is no longer strongly referenced, the corresponding reference contained
     * as value will have its referred object be <code>null</code>. In this case, the methods reading from this cache will purge
     * the record and behave as if the record hadn't existed at the time of the read operation.
     */
    private final ConcurrentHashMap<Serializable, WeakWaypointReference> waypointCache;
    
    private final ReferenceQueue<Waypoint> waypointCacheReferenceQueue;
    
    private final Map<Serializable, DeviceConfigurationMatcher> configurationMatcherCache;

    /**
     * Weak references to {@link Waypoint} objects of this type are registered with
     * {@link DomainFactoryImpl#waypointCacheReferenceQueue} upon construction so that when their referents are no
     * longer strongly referenced and the reference was nulled, they are entered into that queue.
     * Methods managing the {@link #waypointCache} can poll the queue and then remove cache entries based on
     * the {@link #id} stored in the reference.
     * 
     * @author Axel Uhl (D043530)
     * 
     */
    private class WeakWaypointReference extends WeakReference<Waypoint> {
        private final Serializable id;
        
        public WeakWaypointReference(Waypoint waypoint) {
            super(waypoint, waypointCacheReferenceQueue);
            this.id = waypoint.getId();
        }
        
        public void removeCacheEntry() {
            waypointCache.remove(id);
        }
    }

    private final Set<String> mayStartWithNoUpwindLeg;
    
    /**
     * Uses a transient competitor store
     */
    public SharedDomainFactoryImpl() {
        this(new TransientCompetitorStoreImpl());
    }
    
    public SharedDomainFactoryImpl(CompetitorStore competitorStore) {
        waypointCacheReferenceQueue = new ReferenceQueue<Waypoint>();
        nationalityCache = new HashMap<String, Nationality>();
        markCache = new HashMap<Serializable, Mark>();
        markIdCache = new HashMap<String, Serializable>();
        boatClassCache = new HashMap<String, BoatClass>();
        this.competitorStore = competitorStore;
        waypointCache = new ConcurrentHashMap<Serializable, WeakWaypointReference>();
        mayStartWithNoUpwindLeg = new HashSet<String>(Arrays.asList(new String[] { "extreme40", "ess", "ess40" }));
        courseAreaCache = new HashMap<Serializable, CourseArea>();
        configurationMatcherCache = new HashMap<Serializable, DeviceConfigurationMatcher>();
    }
    
    @Override
    public Nationality getOrCreateNationality(String threeLetterIOCCode) {
        synchronized (nationalityCache) {
            Nationality result = nationalityCache.get(threeLetterIOCCode);
            if (result == null) {
                result = new NationalityImpl(threeLetterIOCCode);
                nationalityCache.put(threeLetterIOCCode, result);
            }
            return result;
        }
    }
    
    @Override
    public Mark getOrCreateMark(String name) {
        return getOrCreateMark(name, name);
    }
    
    @Override
    public Mark getOrCreateMark(Serializable id, String name) {
        Mark result = markCache.get(id);
        if (result == null) {
            result = new MarkImpl(id, name);
            cacheMark(id, result);
        }
        return result;
    }

    @Override
    public Mark getOrCreateMark(String toStringRepresentationOfID, String name) {
        final Mark result;
        if (markIdCache.containsKey(toStringRepresentationOfID)) {
            Serializable id = markIdCache.get(toStringRepresentationOfID);
            result = getOrCreateMark(id, name);
        } else {
            result = new MarkImpl(toStringRepresentationOfID, name);
            cacheMark(toStringRepresentationOfID, result);
        }
        return result;
    }

    private void cacheMark(Serializable id, Mark result) {
        markCache.put(id, result);
        markIdCache.put(id.toString(), id);
    }
    
    
    
    @Override
    public Mark getOrCreateMark(Serializable id, String name, MarkType type, String color, String shape, String pattern) {
        Mark result = markCache.get(id);
        if (result == null) {
            result = new MarkImpl(id, name, type, color, shape, pattern);
            cacheMark(id, result);
        }
        return result;
    }

    @Override
    public Gate createGate(Mark left, Mark right, String name) {
       return new GateImpl(left, right, name);
    }

    @Override
    public Gate createGate(Serializable id, Mark left, Mark right, String name) {
       return new GateImpl(id, left, right, name);
    }

    @Override
    public Waypoint createWaypoint(ControlPoint controlPoint, NauticalSide passingSide) {
        synchronized (waypointCache) {
            expungeStaleWaypointCacheEntries();
            Waypoint result = new WaypointImpl(controlPoint, passingSide);
            waypointCache.put(result.getId(), new WeakWaypointReference(result));
            return result;
        }
    }

    @Override
    public Waypoint getExistingWaypointById(Waypoint waypointPrototype) {
        synchronized (waypointCache) {
            expungeStaleWaypointCacheEntries();
            Waypoint result = null;
            Reference<Waypoint> ref = waypointCache.get(waypointPrototype.getId());
            if (ref != null) {
                result = ref.get();
                if (result == null) {
                    // waypoint was finalized; remove entry from cache
                    waypointCache.remove(waypointPrototype.getId());
                }
            }
            return result;
        }
    }

    @Override
    public Waypoint getExistingWaypointByIdOrCache(Waypoint waypoint) {
        synchronized (waypointCache) {
            expungeStaleWaypointCacheEntries();
            Waypoint result = null;
            Reference<Waypoint> ref = waypointCache.get(waypoint.getId());
            if (ref != null) {
                result = ref.get();
                if (result == null) {
                    // waypoint was finalized; remove entry from cache and add anew
                    result = waypoint;
                    waypointCache.put(waypoint.getId(), new WeakWaypointReference(waypoint));
                } // else, result is the waypoint found in the cache; return it
            } else {
                // No entry found in the cache; not even a stale, finalized one. Create a new entry:
                result = waypoint;
                waypointCache.put(waypoint.getId(), new WeakWaypointReference(waypoint));
            }
            return result;
        }
    }

    private void expungeStaleWaypointCacheEntries() {
        Reference<? extends Waypoint> ref;
        while ((ref=waypointCacheReferenceQueue.poll()) != null) {
            ((WeakWaypointReference) ref).removeCacheEntry();
        }
    }

    @Override
    public BoatClass getOrCreateBoatClass(String name, boolean typicallyStartsUpwind) {
        synchronized (boatClassCache) {
            BoatClass result = boatClassCache.get(name);
            if (result == null) {
                result = new BoatClassImpl(name, typicallyStartsUpwind);
                boatClassCache.put(name, result);
            }
            return result;
        }
    }
    
    @Override
    public BoatClass getOrCreateBoatClass(String name) {
        return getOrCreateBoatClass(name, /* typicallyStartsUpwind */!mayStartWithNoUpwindLeg.contains(name.toLowerCase()));
    }
    
    @Override
    public CourseArea getOrCreateCourseArea(UUID courseAreaId, String name) {
        CourseArea result = getExistingCourseAreaById(courseAreaId);
        if (result == null) {
            result = new CourseAreaImpl(name, courseAreaId);
            courseAreaCache.put(courseAreaId, result);
        }
        return result;
    }

    @Override
    public CourseArea getExistingCourseAreaById(Serializable courseAreaId) {
        return courseAreaCache.get(courseAreaId);
    }

    @Override
    public CompetitorStore getCompetitorStore() {
        return competitorStore;
    }

    @Override
    public Competitor getExistingCompetitorById(Serializable competitorId) {
        return getCompetitorStore().getExistingCompetitorById(competitorId);
    }

    @Override
    public boolean isCompetitorToUpdateDuringGetOrCreate(Competitor result) {
        return getCompetitorStore().isCompetitorToUpdateDuringGetOrCreate(result);
    }

    @Override
    public Competitor getOrCreateCompetitor(Serializable competitorId, String name, String rgbDisplayColor, DynamicTeam team, DynamicBoat boat) {
        return getCompetitorStore().getOrCreateCompetitor(competitorId, name, rgbDisplayColor, team, boat);
    }

    @Override
    public DeviceConfigurationMatcher getOrCreateDeviceConfigurationMatcher(DeviceConfigurationMatcherType type, 
            List<String> clientIdentifiers) {
        DeviceConfigurationMatcher probe = createMatcher(type, clientIdentifiers);
        DeviceConfigurationMatcher matcher = configurationMatcherCache.get(probe.getMatcherIdentifier());
        if (matcher == null) {
            configurationMatcherCache.put(probe.getMatcherIdentifier(), probe);
            matcher = probe;
        }
        return matcher;
    }
    
    private DeviceConfigurationMatcher createMatcher(DeviceConfigurationMatcherType type, List<String> clientIdentifiers) {
        DeviceConfigurationMatcher matcher = null;
        switch (type) {
        case SINGLE:
            matcher = new DeviceConfigurationMatcherSingle(clientIdentifiers.get(0));
            break;
        case MULTI:
            matcher = new DeviceConfigurationMatcherMulti(clientIdentifiers);
            break;
        default:
            throw new IllegalArgumentException("Unknown matcher type: " + type);
        }
        return matcher;
    }

}
