package com.sap.sailing.server.security;

import java.io.Serializable;
import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Callable;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectThreadState;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorAndBoatStore;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.impl.DynamicBoat;
import com.sap.sailing.domain.base.impl.DynamicCompetitor;
import com.sap.sailing.domain.base.impl.DynamicCompetitorWithBoat;
import com.sap.sailing.domain.base.impl.DynamicTeam;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.domain.markpassinghash.MarkPassingRaceFingerprintRegistry;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.shared.tracking.TrackingConnectorInfo;
import com.sap.sailing.domain.tracking.DynamicRaceDefinitionSet;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.RaceTrackingHandler;
import com.sap.sailing.domain.tracking.RaceTrackingHandler.DefaultRaceTrackingHandler;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sse.common.Color;
import com.sap.sse.common.Duration;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.impl.Ownership;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.util.ThreadLocalTransporter;

/**
 * {@link RaceTrackingHandler} that handles permission checks and ownership creation. Due to the fact that the creation
 * of {@link TrackedRace TrackedRaces} is potentially done asynchronously, the current authentication is recognized when
 * calling the constructor. On {@link TrackedRace} creation this authentication is restored temporarily to allow proper
 * creation of {@link Ownership} and permission checks. If the user is not permitted, the {@link TrackedRace} creation
 * will fail.
 */
public class PermissionAwareRaceTrackingHandler extends DefaultRaceTrackingHandler {

    private final Subject subject;
    private final UserGroup defaultTenant;
    private final SecurityService securityService;

    public PermissionAwareRaceTrackingHandler(SecurityService securityService) {
        this.securityService = securityService;
        subject = SecurityUtils.getSubject();
        defaultTenant = securityService.getDefaultTenantForCurrentUser();
    }
    
    /**
     * Sets the ownership for a {@link SecuredDomainType#TRACKED_RACE} object identified by
     * {@code regattaAndRaceIdentifier} if no ownership exists for it yet; then, the permission to
     * create the object is checked. If granted, the {@code raceCreationAction} is executed and its
     * result is returned. Otherwise, the action is not executed, and if the ownership was set here,
     * it is removed again.
     */
    private <T> T setOwnershipForRace(RegattaAndRaceIdentifier regattaAndRaceIdentifier, Callable<T> raceCreationAction) {
        SubjectThreadState subjectThreadState = new SubjectThreadState(subject);
        subjectThreadState.bind();
        try {
            return securityService.doWithTemporaryDefaultTenant(defaultTenant, () -> {
                return securityService.setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                        SecuredDomainType.TRACKED_RACE, regattaAndRaceIdentifier.getTypeRelativeObjectIdentifier(),
                        regattaAndRaceIdentifier.toString(), raceCreationAction);
            });
        } finally {
            subjectThreadState.restore();
        }
    }

    /**
     * Check if there is already an ownership associated to the object identified by the
     * {@link QualifiedObjectIdentifier}. If no ownership exists, one will be created in the context of the current
     * user.
     */
    private void createOwnershipIfMissing(final QualifiedObjectIdentifier identifier) {
        if (securityService.getOwnership(identifier) == null) {
            SubjectThreadState subjectThreadState = new SubjectThreadState(subject);
            subjectThreadState.bind();
            try {
                securityService.setOwnership(identifier, securityService.getCurrentUser(), defaultTenant);
            } finally {
                subjectThreadState.restore();
            }
        }
    }

    @Override
    public DynamicTrackedRace createTrackedRace(TrackedRegatta trackedRegatta, RaceDefinition raceDefinition,
            Iterable<Sideline> sidelines, WindStore windStore, long delayToLiveInMillis,
            long millisecondsOverWhichToAverageWind, long millisecondsOverWhichToAverageSpeed,
            DynamicRaceDefinitionSet raceDefinitionSetToUpdate, boolean useMarkPassingCalculator,
            RaceLogAndTrackedRaceResolver raceLogResolver, Optional<ThreadLocalTransporter> threadLocalTransporter,
            TrackingConnectorInfo trackingConnectorInfo, MarkPassingRaceFingerprintRegistry markPassingRaceFingerprintRegistry) {
        return setOwnershipForRace(
                new RegattaNameAndRaceName(trackedRegatta.getRegatta().getName(), raceDefinition.getName()),
                () -> super.createTrackedRace(trackedRegatta, raceDefinition, sidelines, windStore, delayToLiveInMillis,
                        millisecondsOverWhichToAverageWind, millisecondsOverWhichToAverageSpeed,
                        raceDefinitionSetToUpdate, useMarkPassingCalculator, raceLogResolver, threadLocalTransporter,
                        trackingConnectorInfo, markPassingRaceFingerprintRegistry));
    }

    @Override
    public RaceDefinition createRaceDefinition(Regatta regatta, String name, Course course, BoatClass boatClass,
            Map<Competitor, Boat> competitorsAndTheirBoats, Serializable id) {
        return setOwnershipForRace(new RegattaNameAndRaceName(regatta.getName(), name),
                () -> super.createRaceDefinition(regatta, name, course, boatClass, competitorsAndTheirBoats, id));
    }

    /** Gets or creates the competitor and sets the ownership correctly. */
    @Override
    public DynamicCompetitor getOrCreateCompetitor(CompetitorAndBoatStore competitorStore, Serializable competitorId,
            String name, String shortName, Color displayColor, String email, URI flagImageURI, DynamicTeam team,
            Double timeOnTimeFactor, Duration timeOnDistanceAllowancePerNauticalMile, String searchTag) {
        final DynamicCompetitor competitor = competitorStore.getOrCreateCompetitor(competitorId, name, shortName,
                displayColor, email, flagImageURI, team, timeOnTimeFactor, timeOnDistanceAllowancePerNauticalMile,
                searchTag, /* storePersistently */ true);
        createOwnershipIfMissing(competitor.getIdentifier());
        return competitor;
    }

    @Override
    public DynamicCompetitorWithBoat getOrCreateCompetitorWithBoat(CompetitorAndBoatStore competitorStore,
            Serializable competitorId, String name, String shortName, Color displayColor, String email,
            URI flagImageURI, DynamicTeam team, Double timeOnTimeFactor,
            Duration timeOnDistanceAllowancePerNauticalMile, String searchTag, DynamicBoat boat) {
        final DynamicCompetitorWithBoat competitorWithBoat = competitorStore.getOrCreateCompetitorWithBoat(competitorId,
                name, shortName, displayColor, email, flagImageURI,
                team, timeOnTimeFactor, timeOnDistanceAllowancePerNauticalMile, searchTag, boat, /* storePersistently */ true);
        createOwnershipIfMissing(competitorWithBoat.getIdentifier());
        createOwnershipIfMissing(competitorWithBoat.getBoat().getIdentifier());
        return competitorWithBoat;
    }

    @Override
    public DynamicBoat getOrCreateBoat(CompetitorAndBoatStore competitorAndBoatStore, Serializable id, String name,
            BoatClass boatClass, String sailId, Color color) {
        final DynamicBoat boat = competitorAndBoatStore.getOrCreateBoat(id, name, boatClass, sailId, color, /* storePersistently */ true);
        createOwnershipIfMissing(boat.getIdentifier());
        return boat;
    }
}
