package com.sap.sailing.gwt.ui.adminconsole;

import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.ImageResource;

public interface AdminConsoleResources extends ClientBundle {
    @Source("com/sap/sailing/gwt/ui/client/images/magnifier.png")
    ImageResource magnifierIcon();

    @Source("com/sap/sailing/gwt/ui/client/images/settingsActionIcon.png")
    ImageResource settingsActionIcon();

    @Source("com/sap/sailing/gwt/ui/client/images/import.png")
    ImageResource importIcon();

    @Source("com/sap/sailing/gwt/ui/client/images/reload.png")
    ImageResource reloadIcon();

    @Source("com/sap/sailing/gwt/ui/client/images/link.png")
    ImageResource linkIcon();

    @Source("com/sap/sailing/gwt/ui/client/images/link_break.png")
    ImageResource unlinkIcon();

    @Source("com/sap/sailing/gwt/ui/client/images/openBrowser.png")
    ImageResource openBrowserIcon();

    @Source("com/sap/sailing/gwt/ui/client/images/dice.png")
    ImageResource scoresIcon();

    @Source("com/sap/sailing/gwt/ui/client/images/users.png")
    ImageResource competitorsIcon();

    @Source("com/sap/sailing/gwt/ui/client/images/sailboat.png")
    ImageResource sailboatIcon();

    @Source("com/sap/sailing/gwt/ui/client/images/blackdot.png")
    ImageResource blackdotIcon();
    
    @Source("com/sap/sailing/gwt/ui/client/images/reddot.png")
    ImageResource reddotIcon();
    
    @Source("com/sap/sailing/gwt/ui/client/images/arrow_left.png")
    ImageResource arrowLeft();
    
    @Source("com/sap/sailing/gwt/ui/client/images/arrow_right.png")
    ImageResource arrowRight();
    
    @Source("com/sap/sailing/gwt/ui/client/images/help.png")
    ImageResource help();
    
    @Source("com/sap/sailing/gwt/ui/client/images/clock.png")
    ImageResource clockIcon();
    
    @Source("com/sap/sailing/gwt/ui/client/images/pairinglist.png")
    ImageResource pairingList();

    @Source("com/sap/sailing/gwt/ui/client/images/copy_pairinglist.png")
    ImageResource copyPairingList();

    @Source("com/sap/sailing/gwt/ui/client/images/print_pairinglist.png")
    ImageResource printPairingList();
    
    // Smaller variant of RegattaRaceStatesFlagsResources.flagBlue to solve layouting issues
    @Source("com/sap/sailing/gwt/ui/client/images/blue_small.png")
    ImageResource blueSmall();

    @Source("com/sap/sailing/gwt/ui/client/images/flag_blue.png")
    ImageResource flagIcon();

    @Source("com/sap/sailing/gwt/ui/client/images/opencoachdashboard.png")
    ImageResource openCoachDashboard();

    @Source("com/sap/sailing/gwt/ui/client/images/add_racelog_tracker.png")
    ImageResource addRaceLogTracker();

    @Source("com/sap/sailing/gwt/ui/client/images/denote_for_racelog_tracking.png")
    ImageResource denoteForRaceLogTracking();

    @Source("com/sap/sailing/gwt/ui/client/images/undenote_for_racelog_tracking.png")
    ImageResource unDenoteForRaceLogTracking();

    @Source("com/sap/sailing/gwt/ui/client/images/start_racelog_tracking.png")
    ImageResource startRaceLogTracking();
    
    @Source("com/sap/sailing/gwt/ui/client/images/stop_racelog_tracking.png")
    ImageResource stopRaceLogTracking();

    @Source("com/sap/sailing/gwt/ui/client/images/competitor_registrations.png")
    ImageResource competitorRegistrations();

    @Source("com/sap/sailing/gwt/ui/client/images/boat_registrations.png")
    ImageResource boatRegistrations();

    @Source("com/sap/sailing/gwt/ui/client/images/define_course.png")
    ImageResource defineCourse();

    @Source("com/sap/sailing/gwt/ui/client/images/map_devices.png")
    ImageResource mapDevices();

    @Source("com/sap/sailing/gwt/ui/client/images/set_tracking_times.png")
    ImageResource setTrackingTimes();
    
    @Source("com/sap/sailing/gwt/ui/client/images/ping.png")
    ImageResource ping();

    @Source("com/sap/sailing/gwt/ui/client/images/remove_ping.png")
    ImageResource removePing();

    @Source("com/sap/sailing/gwt/ui/client/images/copy_course.png")
    ImageResource copyCourse();

    @Source("com/sap/sailing/gwt/ui/client/images/copy.png")
    ImageResource copy();

    @Source("com/sap/sailing/gwt/ui/client/images/close_time_range.png")
    ImageResource closeTimeRange();

    @Source("com/sap/sailing/gwt/ui/client/images/compose_mail_small.png")
    ImageResource inviteBuoyTenders();
    
    @Source("com/sap/sailing/gwt/ui/client/images/ajax-loader.gif")
    ImageResource loaderGif();
    
    @Source("com/sap/sailing/gwt/ui/client/images/transparent.gif")
    ImageResource transparentGif();
    
    @Source("com/sap/sailing/gwt/ui/client/images/orc_pcs_leg.png")
    ImageResource orcPcsDefineLegIcon();

    @Source("com/sap/sailing/gwt/ui/client/images/orc_pcs_all_legs.png")
    ImageResource orcPcsDefineAllLegsIcon();

    @Source("com/sap/sailing/gwt/ui/client/images/certificates.png")
    ImageResource updateCertificatesIcon();

    @Source("com/sap/sailing/gwt/ui/client/images/scratchBoat.png")
    ImageResource scratchBoatIcon();

    @Source("com/sap/sailing/gwt/ui/client/images/impliedWind.png")
    ImageResource impliedWindIcon();
}