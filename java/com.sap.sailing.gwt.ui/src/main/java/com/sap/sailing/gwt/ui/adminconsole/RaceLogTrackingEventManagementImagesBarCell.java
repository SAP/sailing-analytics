package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;

import com.google.gwt.core.client.GWT;
import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.sap.sailing.domain.common.LeaderboardType;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.gwt.client.IconResources;
import com.sap.sse.gwt.client.celltable.ImagesBarCell;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;

public class RaceLogTrackingEventManagementImagesBarCell extends ImagesBarCell {
    public static final String ACTION_DENOTE_FOR_RACELOG_TRACKING = "ACTION_DENOTE_FOR_RACELOG_TRACKING";
    public final static String ACTION_COMPETITOR_REGISTRATIONS = "ACTION_COMPETITOR_REGISTRATIONS";
    public final static String ACTION_BOAT_REGISTRATIONS = "ACTION_BOAT_REGISTRATIONS";
    public final static String ACTION_MAP_DEVICES = "ACTION_MAP_DEVICES";
    public final static String ACTION_INVITE_BUOY_TENDERS = "ACTION_INVITE_BUOY_TENDERS";
    public static final String ACTION_SHOW_REGATTA_LOG = "ACTION_SHOW_REGATTA_LOG";
    public static final String ACTION_REVOKE_EXPLICIT_TRACKING_TIMES = "ACTION_REVOKE_EXPLICIT_TRACKING_TIMES";
    private static AdminConsoleResources resources = GWT.create(AdminConsoleResources.class);
    private final StringMessages stringMessages;
    
    public RaceLogTrackingEventManagementImagesBarCell(StringMessages stringMessages) {
        super();
        this.stringMessages = stringMessages;
    }

    public RaceLogTrackingEventManagementImagesBarCell(SafeHtmlRenderer<String> renderer, StringMessages stringConstants) {
        super();
        this.stringMessages = stringConstants;
    }

    @Override
    protected Iterable<ImageSpec> getImageSpecs() {
        ArrayList<ImageSpec> result = new ArrayList<ImageSpec>();
        final StrippedLeaderboardDTO selectedLeaderboard = (StrippedLeaderboardDTO) getContext().getKey();
        result.add(new ImageSpec(ACTION_DENOTE_FOR_RACELOG_TRACKING, stringMessages.denoteAllRacesForRaceLogTrackingShorctut(),
                makeImagePrototype(resources.denoteForRaceLogTracking())));
        result.add(new ImageSpec(ACTION_COMPETITOR_REGISTRATIONS, stringMessages.competitorRegistrations(),
                makeImagePrototype(resources.competitorRegistrations())));
        if (selectedLeaderboard.canBoatsOfCompetitorsChangePerRace) {
            result.add(new ImageSpec(ACTION_BOAT_REGISTRATIONS, stringMessages.boatRegistrations(),
                    makeImagePrototype(resources.boatRegistrations())));
        }
        result.add(new ImageSpec(ACTION_MAP_DEVICES, stringMessages.mapDevices(),
                makeImagePrototype(resources.mapDevices())));
        result.add(new ImageSpec(ACTION_INVITE_BUOY_TENDERS, stringMessages.inviteBuoyTenders(),
                makeImagePrototype(resources.inviteBuoyTenders())));
        result.add(new ImageSpec(ACTION_SHOW_REGATTA_LOG, stringMessages.regattaLog(),
                makeImagePrototype(resources.flagIcon())));
        if (selectedLeaderboard.type == LeaderboardType.RegattaLeaderboard) {
            result.add(new ImageSpec(ACTION_REVOKE_EXPLICIT_TRACKING_TIMES, stringMessages.revokeExplicitTrackingTimes(),
                    makeImagePrototype(resources.eraser())));
        }
        result.add(new ImageSpec(DefaultActions.CHANGE_OWNERSHIP.name(), stringMessages.actionChangeOwnership(),
                IconResources.INSTANCE.changeOwnershipIcon()));
        result.add(new ImageSpec(DefaultActions.CHANGE_ACL.name(), stringMessages.actionChangeACL(),
                IconResources.INSTANCE.changeACLIcon()));
        return result;
    }
}