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
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorStore;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.ControlPointWithTwoMarks;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Nationality;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.configuration.DeviceConfigurationMatcher;
import com.sap.sailing.domain.base.configuration.impl.DeviceConfigurationMatcherMulti;
import com.sap.sailing.domain.base.configuration.impl.DeviceConfigurationMatcherSingle;
import com.sap.sailing.domain.common.BoatClassMasterdata;
import com.sap.sailing.domain.common.MarkType;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.common.configuration.DeviceConfigurationMatcherType;
import com.sap.sse.common.Color;
import com.sap.sse.common.WithID;

public class SharedDomainFactoryImpl implements SharedDomainFactory {
    private static final Logger logger = Logger.getLogger(SharedDomainFactoryImpl.class.getName());
    
    /**
     * Ensure that the <em>same</em> string is used as key that is also used to set the {@link Nationality}
     * object's {@link Nationality#getThreeLetterIOCAcronym() IOC code}.
     */
    private final Map<String, Nationality> nationalityCache;
    
    private final Map<Serializable, Mark> markCache;
    
    private final Map<Serializable, ControlPointWithTwoMarks> controlPointWithTwoMarksCache;
    
    /**
     * For all marks ever created by this factory, the mark {@link WithID#getId() ID}'s string representation
     * is mapped here to the actual ID. This allows clients to send only the string representation to the server
     * and still be able to identify a mark uniquely this way.
     */
    private final Map<String, Serializable> markIdCache;
    
    /**
     * For all ControlPoints ever created by this factory, the mark {@link WithID#getId() ID}'s string representation
     * is mapped here to the actual ID. This allows clients to send only the string representation to the server
     * and still be able to identify a ControlPoint uniquely this way.
     */
    private final Map<String, Serializable> controlPointWithTwoMarksIdCache;
    
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
        controlPointWithTwoMarksCache = new HashMap<Serializable, ControlPointWithTwoMarks>();
        controlPointWithTwoMarksIdCache = new HashMap<String, Serializable>();
        boatClassCache = new HashMap<String, BoatClass>();
        this.competitorStore = competitorStore;
        waypointCache = new ConcurrentHashMap<Serializable, WeakWaypointReference>();
        mayStartWithNoUpwindLeg = new HashSet<String>(Arrays.asList(new String[] { "extreme40", "ess", "ess40" }));
        courseAreaCache = new HashMap<Serializable, CourseArea>();
        configurationMatcherCache = new HashMap<Serializable, DeviceConfigurationMatcher>();
    }
    
    @Override
    public Nationality getOrCreateNationality(String threeLetterIOCCode) {
        if (threeLetterIOCCode == null) {
            threeLetterIOCCode = "   ";
        }
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
        return getOrCreateMark(id, name, null, null, null, null);
    }

    @Override
    public Mark getOrCreateMark(String toStringRepresentationOfID, String name) {
        return getOrCreateMark(toStringRepresentationOfID, name, null, null, null, null);
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
    public Mark getOrCreateMark(String toStringRepresentationOfID, String name, MarkType type,
            String color, String shape, String pattern) {
        Serializable id = toStringRepresentationOfID;
        if (markIdCache.containsKey(toStringRepresentationOfID)) {
            id = markIdCache.get(toStringRepresentationOfID);
        }
        return getOrCreateMark(id, name, type, color, shape, pattern);
    }

    @Override
    public ControlPointWithTwoMarks getOrCreateControlPointWithTwoMarks(Serializable id, String name, Mark left, Mark right) {
        if (controlPointWithTwoMarksCache.containsKey(id)) {
            return controlPointWithTwoMarksCache.get(id);
        }
        return createControlPointWithTwoMarks(id, left, right, name);
    }

    @Override
    public ControlPointWithTwoMarks getOrCreateControlPointWithTwoMarks(
            String toStringRepresentationOfID, String name, Mark left, Mark right) {
        Serializable id = toStringRepresentationOfID;
        if (controlPointWithTwoMarksIdCache.containsKey(toStringRepresentationOfID)) {
            id = controlPointWithTwoMarksIdCache.get(toStringRepresentationOfID);
        }
        return getOrCreateControlPointWithTwoMarks(id, name, left, right);
    }

    private void cacheMark(Serializable id, Mark result) {
        markCache.put(id, result);
        markIdCache.put(id.toString(), id);
    }

    @Override
    public ControlPointWithTwoMarks createControlPointWithTwoMarks(Mark left, Mark right, String name) {
       return createControlPointWithTwoMarks(name, left, right, name);
    }

    @Override
    public ControlPointWithTwoMarks createControlPointWithTwoMarks(Serializable id, Mark left, Mark right, String name) {
        ControlPointWithTwoMarks result = new ControlPointWithTwoMarksImpl(id, left, right, name);
        controlPointWithTwoMarksCache.put(id, result);
        controlPointWithTwoMarksIdCache.put(id.toString(), id);
        return result;
    }

    @Override
    public Waypoint createWaypoint(ControlPoint controlPoint, PassingInstruction passingInstruction) {
        synchronized (waypointCache) {
            expungeStaleWaypointCacheEntries();
            Waypoint result = passingInstruction==null?new WaypointImpl(controlPoint):new WaypointImpl(controlPoint, passingInstruction);
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
        final String unifiedBoatClassName = BoatClassMasterdata.unifyBoatClassName(name);
        synchronized (boatClassCache) {
            BoatClass result = boatClassCache.get(name);
            if (result == null) {
                result = boatClassCache.get(unifiedBoatClassName);
            }
            if (result == null && unifiedBoatClassName != null) {
                BoatClassMasterdata boatClassMasterdata = BoatClassMasterdata.resolveBoatClass(name);
                if (boatClassMasterdata != null) {
                    result = new BoatClassImpl(boatClassMasterdata.getDisplayName(), boatClassMasterdata);
                    boatClassCache.put(name, result);
                    boatClassCache.put(unifiedBoatClassName, result);
                    boatClassCache.put(result.getName(), result);
                    for (String alternativeName : boatClassMasterdata.getAlternativeNames()) {
                        boatClassCache.put(alternativeName, result);
                    }
                }
            }
            if (result == null) {
                result = new BoatClassImpl(unifiedBoatClassName, typicallyStartsUpwind);
                boatClassCache.put(unifiedBoatClassName, result);
            }
            return result;
        }
    }
    
    @Override
    public BoatClass getOrCreateBoatClass(String name) {
        return getOrCreateBoatClass(name, name == null || /* typicallyStartsUpwind */!mayStartWithNoUpwindLeg.contains(name.toLowerCase()));
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
    public Competitor getOrCreateCompetitor(Serializable competitorId, String name, Color displayColor, DynamicTeam team, DynamicBoat boat) {
        if (logger.isLoggable(Level.FINEST)) {
            logger.log(Level.FINEST, "getting or creating competitor "+name+" with ID "+competitorId+" in domain factory "+this);
        }
        return getCompetitorStore().getOrCreateCompetitor(competitorId, name, displayColor, team, boat);
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
    
    @Override
    public Mark getExistingMarkById(Serializable id) {
        return markCache.get(id);
    }
    
    @Override
    public Mark getExistingMarkByIdAsString(String toStringRepresentationOfID) {
        return markCache.get(markIdCache.get(toStringRepresentationOfID));
    }

}
