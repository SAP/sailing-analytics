package com.sap.sailing.gwt.autoplay.client.app;

import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.dto.AbstractLeaderboardDTO;
import com.sap.sailing.domain.common.dto.RaceDTO;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.domain.common.subscription.PremiumRole;
import com.sap.sailing.gwt.autoplay.client.shared.leaderboard.LeaderboardWithZoomingPerspectiveLifecycle;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.raceboard.RaceBoardPerspectiveLifecycle;
import com.sap.sse.gwt.client.shared.components.SettingsDialogComponent;
import com.sap.sse.gwt.client.shared.perspective.AbstractPerspectiveLifecycle;
import com.sap.sse.security.shared.dto.SecuredDTO;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.premium.PaywallResolver;
import com.sap.sse.security.ui.client.premium.PaywallResolverImpl;
import com.sap.sse.security.ui.client.subscription.SubscriptionServiceFactory;
import com.sap.sse.security.ui.shared.EssentialSecuredDTO;

/**
 * This lifecycle contains the necessary child lifecycles to allow AutoPlay to create and use both, a Leaderboard for
 * non life, and a RaceBoard for life races.
 */
public class AutoplayPerspectiveLifecycle extends AbstractPerspectiveLifecycle<AutoplayPerspectiveOwnSettings> {
    public static final String ID = "ap";
    private LeaderboardWithZoomingPerspectiveLifecycle leaderboardLifecycle;
    private RaceBoardPerspectiveLifecycle raceboardLifecycle;

    public AutoplayPerspectiveLifecycle(AbstractLeaderboardDTO leaderboard, UserService userService,
            SubscriptionServiceFactory subscriptionServiceFactory, Iterable<DetailType> availableDetailTypes) {
        PaywallResolver paywallResolver = new PaywallResolverImpl(userService, subscriptionServiceFactory);
        leaderboardLifecycle = new LeaderboardWithZoomingPerspectiveLifecycle(leaderboard, StringMessages.INSTANCE,
                availableDetailTypes, paywallResolver);
        raceboardLifecycle = new RaceBoardPerspectiveLifecycle(leaderboard, StringMessages.INSTANCE,
                DetailType.getAutoplayDetailTypesForChart(), userService,
                paywallResolver, availableDetailTypes, getEssentialRaceDTOWithPremiumActionACLs());
        addLifeCycle(leaderboardLifecycle);
        addLifeCycle(raceboardLifecycle);
    }

    public AutoplayPerspectiveLifecycle(AbstractLeaderboardDTO leaderboard, UserService userService,
            PaywallResolver paywallResolver, Iterable<DetailType> availableDetailTypes) {
        leaderboardLifecycle = new LeaderboardWithZoomingPerspectiveLifecycle(leaderboard, StringMessages.INSTANCE,
                availableDetailTypes, paywallResolver);
        raceboardLifecycle = new RaceBoardPerspectiveLifecycle(leaderboard, StringMessages.INSTANCE,
                DetailType.getAutoplayDetailTypesForChart(), userService, paywallResolver, availableDetailTypes, getEssentialRaceDTOWithPremiumActionACLs());
        addLifeCycle(leaderboardLifecycle);
        addLifeCycle(raceboardLifecycle);
    }

    /**
     * Creates a dummy race DTO with security accepting all premium actions which are based on RaceDTO type
     * {@link SecuredDomainType}.TRACKED_RACE. In this case it is used to enable all features based on a raceDTO because
     * the specific raceDTO is not available now. The autoplay view later will check against a real raceDTO.
     * 
     * @return a secured DTO based on the {@link EssentialSecuredDTO} but with ACL granting race premium functions
     *         regardless user group participation.
     */
    private SecuredDTO getEssentialRaceDTOWithPremiumActionACLs() {
        return EssentialSecuredDTO.getInstanceByPermissionTypeFromPermissionSet(RaceDTO.getPermissionTypeForClass(),
                PremiumRole.getInstance().getPermissions());
    }
    
    @Override
    public AutoplayPerspectiveOwnSettings createPerspectiveOwnDefaultSettings() {
        return new AutoplayPerspectiveOwnSettings();
    }

    @Override
    public SettingsDialogComponent<AutoplayPerspectiveOwnSettings> getPerspectiveOwnSettingsDialogComponent(
            AutoplayPerspectiveOwnSettings settings) {
        return new AutoplaySettingsDialogComponent(settings);
    }

    public LeaderboardWithZoomingPerspectiveLifecycle getLeaderboardLifecycle() {
        return leaderboardLifecycle;
    }

    public RaceBoardPerspectiveLifecycle getRaceboardLifecycle() {
        return raceboardLifecycle;
    }

    @Override
    public String getLocalizedShortName() {
        return StringMessages.INSTANCE.autoplayConfiguration();
    }

    @Override
    public String getComponentId() {
        return ID;
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    protected AutoplayPerspectiveOwnSettings extractOwnUserSettings(AutoplayPerspectiveOwnSettings settings) {
        return settings;
    }

    @Override
    protected AutoplayPerspectiveOwnSettings extractOwnDocumentSettings(AutoplayPerspectiveOwnSettings settings) {
        return settings;
    }

}
