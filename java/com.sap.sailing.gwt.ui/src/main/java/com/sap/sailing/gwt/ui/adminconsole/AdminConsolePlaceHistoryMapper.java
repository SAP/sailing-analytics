package com.sap.sailing.gwt.ui.adminconsole;

import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.place.shared.WithTokenizers;
import com.sap.sailing.gwt.ui.adminconsole.places.advanced.FileStoragePlace;
import com.sap.sailing.gwt.ui.adminconsole.places.advanced.LocalServerPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.advanced.MasterDataImportPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.advanced.RemoteServerInstancesPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.advanced.ReplicationPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.advanced.RolesPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.advanced.UserGroupManagementPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.advanced.UserManagementPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.aiagent.AIAgentConfigurationPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.connectors.ExpeditionDeviceConfigurationsPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.connectors.IgtimiDevicesPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.connectors.Manage2SailRegattaStructureImportPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.connectors.ResultImportUrlsPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.connectors.SmartphoneTrackingPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.connectors.SwissTimingArchivedEventsPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.connectors.SwissTimingEventsPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.connectors.TracTracEventsPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.connectors.YellowBrickEventsPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.coursecreation.CourseTemplatesPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.coursecreation.MarkPropertiesPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.coursecreation.MarkRolesPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.coursecreation.MarkTemplatesPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.events.EventsPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.leaderboards.LeaderboardGroupsPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.leaderboards.LeaderboardsPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.racemanager.DeviceConfigurationPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.regattas.RegattasPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.trackedraces.AudioAndVideoPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.trackedraces.BoatsPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.trackedraces.CompetitorsPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.trackedraces.CourseLayoutPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.trackedraces.TrackedRacesPlace;
import com.sap.sailing.gwt.ui.adminconsole.places.trackedraces.WindPlace;
import com.sap.sailing.landscape.ui.client.LandscapeManagementPlace;

@WithTokenizers({EventsPlace.Tokenizer.class, RegattasPlace.Tokenizer.class, 
    LeaderboardsPlace.Tokenizer.class, LeaderboardGroupsPlace.Tokenizer.class, TrackedRacesPlace.Tokenizer.class, CompetitorsPlace.Tokenizer.class,
    BoatsPlace.Tokenizer.class, CourseLayoutPlace.Tokenizer.class, WindPlace.Tokenizer.class, AudioAndVideoPlace.Tokenizer.class, 
    TracTracEventsPlace.Tokenizer.class, YellowBrickEventsPlace.Tokenizer.class, SwissTimingArchivedEventsPlace.Tokenizer.class, SwissTimingEventsPlace.Tokenizer.class, 
    SmartphoneTrackingPlace.Tokenizer.class, IgtimiDevicesPlace.Tokenizer.class, ExpeditionDeviceConfigurationsPlace.Tokenizer.class,
    ResultImportUrlsPlace.Tokenizer.class, Manage2SailRegattaStructureImportPlace.Tokenizer.class, DeviceConfigurationPlace.Tokenizer.class,
    ReplicationPlace.Tokenizer.class, MasterDataImportPlace.Tokenizer.class, RemoteServerInstancesPlace.Tokenizer.class, LocalServerPlace.Tokenizer.class,
    UserManagementPlace.Tokenizer.class, RolesPlace.Tokenizer.class, UserGroupManagementPlace.Tokenizer.class, FileStoragePlace.Tokenizer.class, 
    MarkTemplatesPlace.Tokenizer.class, MarkPropertiesPlace.Tokenizer.class, CourseTemplatesPlace.Tokenizer.class, MarkRolesPlace.Tokenizer.class,
    LandscapeManagementPlace.Tokenizer.class, AIAgentConfigurationPlace.Tokenizer.class })
public interface AdminConsolePlaceHistoryMapper extends PlaceHistoryMapper {

}