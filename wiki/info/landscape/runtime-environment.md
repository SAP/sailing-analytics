# Runtime Environment

[[_TOC_]]

## Linux, Java, OSGi/Equinox
By and large, the Sailing Analytics are a web application implemented using Java technology. The application's components are OSGi bundles running in an Equinox OSGi container. Some bundles offer static web content or dynamic content in the form of servlets. Those bundles are implemented as OSGi Web Bundles which we consider a simple and elegant way to meet web standards using an OSGi-based implementation. A Web Bundle's main extension compared to a regular OSGi bundle is the presence of a web.xml descriptor in the WEB-INF top-level folder where servlets and static content can be declared and mapped to URLs.

Yet other bundles are GWT projects that also may have a backend part, making them an OSGi bundle from the backend's perspective, yet a GWT project for the GWT compiler and runtime.

The application exposes a web UI, implemented using GWT (see below), a REST API implemented largely using JAX-RS/Jersey-based OSGi web bundles, an OSGi console accessible through the telnet protocol, and one or more connectivity components, such as a socket server backend for Igtimi wind sensors or a UDP connector for the "Expedition" on-board navigation tool.

## Database
By and large, we use a database to recover from a server restart. Once started, most data managed by the application is kept in main memory. We currently use MongoDB as our database. Different DB instances belong to different server instances. This allows us to cleanly separate development and test data from production data. Replicas (see also [Replication](#replication) below) have their own "phony" MongoDB that is only a placeholder and is never read from. It's a good plan to run those replica MongoDB processes on machines separate from the primary's MongoDB. In our production environment we run those replica MongoDB processes on the same host the replica runs on, so separate replicas on separate nodes use separate MongoDBs, so there will be no single bottleneck that all replicas would try to write to.

The GPS tracking data is currently usually fetched from the tracking provider in case the provider stores it persistently. While this avoids redundancies and ensures up-to-date versions of the tracking data are used, it also creates a strong dependence upon the provider's system availability and may cause performance issues when many GPS tracks need to be reloaded after a server restart.

We therefore consider using our database also for replicated versions of at least the "archived" tracks where further changes on behalf of the tracking provider are unlikely.

## Replication
To improve at least read-only availability and to scale out with growing numbers of concurrent users, we allow for replicating a server process's state. Similar to the MongoDB architecture, we support a single "primary" and multiple "replica" nodes. The primary is where by default we want modifying transactions to be processed first. Furthermore, some aspects of connectivity to external systems are handled by the primary but not the replicas. For example, connections to the TracTrac environment from where GPS tracking data can be received, are managed only on the primary. Likewise, only the primary would have an e-mail service configured for outbound e-mail sending.

When the primary has received a modifying request, it will make sure to send a corresponding operation to all replicas. This happens through RabbitMQ, where queues and fan-out exchanges are used so that the primary sends the serialized operation as a message which gets propagated through a fan-out exchange to all registered replicas. There, the operation is de-serialized and applied. Transitive replication from replicas through another RabbitMQ / fan-out exchange to further replicas is possible.

Should a modifying request have reached a replica directly (e.g., a user session being created), the replica will apply this operation locally first, then assign an ID to the operation and send it to its primary using an HTTP request, serializing the operation into the request body. The primary will then apply the operation and propagate it to all replicas. The replica where the operation originated will recognize it based on its ID and not apply it again. With this "reverse replication" pattern, replicas may already allow for certain types of local modifications even in case their primary is currently unavailable. Reverse replication will enqueue those messages to be sent to the primary in case the primary is temporarily unavailable and send them at a later point. This will lead to eventual consistency across the replica set.

In our AWS EC2 production environment we use load balancer rules to make it likely that modifying operations are routed to the primary. See [here](https://wiki.sapsailing.com/wiki/info/landscape/amazon-ec2.md#standard-application-replica-set-with-target-groups-auto-scaling-group-and-launch-configuration) for more details.

Yet more details on replication can be found [[here|wiki/howto/misc/server-replication.md]] and [[here|wiki/info/landscape/basic-architectural-principles#scale-out-through-replication]].

## Google Web Toolkit (GWT)
The web UI is built using the Google Web Toolkit (GWT). This allows us to share code between UI and back-end and gives us the power of the regular Eclipse Java tools for code understanding, debugging and agile manipulation. We use a [[forked version of GWT|wiki/howto/development/gwt-fork]].

The GWT application communicates with the server using GWT RPC which, in the back-end, is implemented as a so-called RemoteServiceServlet which again is exposed by means of an OSGi web bundle. This servlet accesses the core application through OSGi services, such as ``RacingEventService`` or ``SecurityService`` which is hooked up in the OSGi service registry.

Our GWT RPC services offer reading and modifying operations. As laid out in [Replication](#replication), we are interested in those modifying operations to reach the primary node of a replica set first. This is why on the client side we distinguish between the reading and the modifying ``...Async`` interface for the RPC service. For example, we have ``SailingService`` and ``SailingServiceWrite``, where the latter extends the former and adds all modifying operations. The actual implementation then implements the ``...Write`` interface with all methods. Client code can then decide to obtain an instance of the reading or the modifying interface, as in

```
    final SailingServiceAsync readService = SailingServiceHelper.createSailingServiceInstance();
    final SailingServiceWriteAsync writeService = SailingServiceHelper.createSailingServiceWriteInstance();
```

The two service clients will be configured (using ``EntryPointHelper.registerASyncService(...)``) with different HTTP headers they will send when the request is made, giving a load balancer a chance to route writing requests to the primary node.

We try to keep important styling information separate in CSS resources which can be manipulated by web designers more conveniently than the Java source code. We balance this with the benefits of the Java sources' traceability which does not exist really for CSS resources where everything is just a string.

Java code can be shared between back-end and front-end. This is not only possible by using the com.sap.sailing.gwt.ui.shared package. GWT enables the sharing of an entire OSGi bundle between client and server. As a starting point for this powerful construct we have introduced the bundle com.sap.sailing.domain.common. It has a GWT module descriptor (SailingDomain.gwt.xml) which inside the <module> element has to list the packages to expose, e.g., <source path='common'/> and currently (with GWT 2.4) has to adhere to the JDK 1.6 language capabilities, in particular disallowing multi-catch clauses and inferred generics. Also, it has to adhere to the rules for GWT's JRE emulation (see, e.g., https://developers.google.com/web-toolkit/doc/latest/DevGuide). The bundle at the same time is a regular OSGi Java bundle, with a regular manifest. Other GWT bundles can use it by including an inherits clause in their <module> specification, as in <inherits name="com.sap.sailing.domain.SailingDomain"/>. With this mechanism, code can easily be shared across client and back-end.

## Security

We use Apache Shiro to implement authentication and authorization. See [[User Management|wiki/info/landscape/usermanagement.md]] and [[Permission Concept|wiki/info/security/permission-concept.md]] for details. At this point it be sufficient to understand that we check for the user having the necessary permissions in the "ingress" of requests into our back end. There are generally two types of request handlers: GWT RPC service implementations; and REST API implementations. We strive to keep those "ingress" layers lean. Mostly, they should deal with authorization, parameter handling and result assembly, and in between delegate to a service (e.g., one found from the OSGi service registry, such as ``RacingEventService`` or ``SecurityService``) where the functionality lives that is independent of how the request came in (GWT RPC or REST API). This way, the service implementations usually don't need to worry about authorization issues as this is already handled at request ingress level. Note that this can lead to very similar authorization checks in similar GWT RPC and REST API methods. Example: ``UserManagementWriteServiceImpl.deleteUser(...)`` and ``SecurityResource.deleteUser(...)``.

## Running Selenium Tests
To run Selenium Integration UI Tests please do the following:

* in local-test-environment.xml file adjust the properties according to which browser and driver you'll be using (chromedriver, geckodriver) and what their file system locations are;
* make sure no "Sailing Server ..." launch configuration is currently running;
* start the "Sailing Server (No Proxy, winddbTest)" config, it will use a test database in MongoDB so as to not overwrite anything in your local "production" ("winddb") database; it furthermore includes a bundle in the OSGi configuration that enables massive "clear" operations that we don't want to deploy to our production landscape otherwise but which is used by the tests to ensure a clean fresh state when tests start to execute;
* start the "GWT Sailing SDM" launch configuration;
* start the "Sailing Domain Test (No Proxy)" JUnit test launch configuration;

## Tracking and Wind Sensor Connectors
To receive GPS and wind data in near real time, some network programming becomes necessary. Depending on the technology and provider used, a combination of HTTP, TCP and UDP connections is required to obtain live data. Particularly the UDP connectivity was the reason why deploying our solution to SAP NetWeaver Cloud was and still is difficult.

We try to isolate connectors to a particular technology or tracking provider so that the core application doesn't depend on a particular provider. Among other things, this leads to an architecture in which separate bundles encapsulate the connectivity components for each provider. There are still a few minor pieces of code in the UI area where this separation hasn't been completed yet and where the UI component knows about the concrete GPS tracking providers currently supported. We have plans to change this such that simply by deploying a tracking provider's connectivity bundle the back-end picks it up and makes it known all the way into the front-end.

## TracTrac Connector
TracTrac offers a Java client to ease the connection to their back-end systems. This client is provided as a JAR file and is referred to as the "TracTrac Client Module" (TTCM). The client offers convenient access to both, historic ("stored") data and live data which is pushed to the client.
The TracTrac system serves JSON documents, one per event, which provides an overview of the races tracked by the system. This list is visualized in the TracTrac tab of the AdminConsole when the "List Races" button is pressed after having entered a correct JSON URL. The document also contains details about the connectivity parameters which we read and use by default.

TTCM requires access to specific TCP ports (usually germanmaster.traclive.dk:4400/4401). This is to be considered when configuring a firewall. It is generally possible to tunnel these connections through an SSH tunnel. TTCM is fairly resilient to network disruptions and keeps trying to re-connect. Once connected, the push service works pretty reliably during live events.

It is worth noting that the TracTrac architecture can lead to out-of-order delivery of messages. For example, if a tracker loses network connectivity for some time, it will send the GPS fixes recorded during the outage once it re-connects. Those fixes can affect past analyses such as the wind estimation. Also, mark rounding times can change over time. Whenever the TracTrac server computes an improved mark rounding time, it will push the update to all registered TTCM clients, possibly updating previous mark rounding time estimations. Since such updates have a major impact on many calculations, it was one of the key reasons we originally decided to store only the facts and calculate all derived figures from those facts on the fly.

We map the TracTrac domain concepts to our domain concepts in an adapter we call the DomainFactory. It keeps track of the mappings performed so as to not create duplicate domain objects in our application for the same TracTrac competitor, buoy, waypoint or race. These canonicalizing mappings are–together with the use of immutable master data objects–at the same time currently one of the annoyances in the architecture. When master data changes on TracTrac's side, our domain objects currently aren't properly updated because they are immutable, and replacing them would not be an easy task. See also http://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=596 for a more detailed discussion of this problem.

TTCM has a notion of "events" which can differ from what our domain model calls an "event." With TracTrac it is possible to track multiple sailing events and regattas within a single TracTrac event. However, in our domain model, a sailing event such as "Kieler Woche 2012" is a single event.

The connector runs a number of threads for each race tracked: one per type of message received. Those types are the race course definition together with the list of competitors, the raw competitor GPS fixes, the mark positions, start/finish times and the mark rounding times. In retrospect, this design is not ideal for a number of reasons. While it keeps each receiver for each type of information fairly clean, affording at least five threads per race is quite a lot and would not be needed if we handled the receiving of each message synchronously in the callback provided by TTCM.

A particular aspect in the connector's design to re-consider is the life cycle of the RaceDefinition objects. They are currently only created after the race course definition has been received. All callers required to access the RaceDefinition object are currently suspended until the course layout has been successfully received. If for some reasons this process times out.

## SwissTiming Connector
Other than in the case of TracTrac, the SwissTiming SailMaster system broadcasts packets containing position and mark rounding information to a pre-determined set of hosts. There is no API to obtain old, stored data. Packets missed during live transmission are lost, except for a tricky, unconfirmed and yet untested process of receiving database dumps from SwissTiming at a later point.

Therefore, we have decided to implement a database buffer for the SwissTiming connector in such a way that a very simple, hence robust Java process is solely responsible for receiving, forwarding and durably storing the information packets broadcast by SwissTiming (see class StoreAndForward). The actual connector running in our back-end then connects to this process for the forwarded live packets while loading the packets already received so far from the persistent store. Based on packet numbers, the original sequence can be restored while live packets keep coming in.

It may be worth noting that SwissTiming SailMaster systems can operate in one of two modes. Either they offer inbound connections on an IP address and port. In this case, requests can also be sent to the SailMaster instance, e.g., to find out the set of races currently managed by that instance. Or the SailMaster system runs behind a multiplexer which collects messages from several SailMaster systems and broadcasts them to a previously determined set of IP addresses and ports. In this case, no requests can be sent to any of these SailMaster systems because the multiplexer couldn't identify the individual SailMaster instance to which to forward the request. For this reason, the connector distinguishes between these two modes and won't try to send requests if the SailMaster system runs behind a multiplexer.

The current implementation offers methods on the SailMasterConnector interface, allowing clients to send requests to a SailMaster instance capable of handling them. However, the connector doesn't currently use any of those, except for the getRaces() call which is used if available to determine the set of races managed by the SailMaster instance. Future versions should consider obtaining more information by explicit requests if possible.

## Expedition Connector
We like to receive wind data live from the race course(s). For this, we can install wind measurement devices on vessels such as the start vessel or RIBs following the field or dedicated solely to the task of wind measurement. Currently, we use devices that can be hooked up to a Panasonic Toughbook on which we run a software package called Expedition (see http://www.tasmanbaynav.co.nz/). This package is capable of receiving sensor data from a variety of different devices, including a Nexus on-board controller and various sensor devices connected directly to the laptop using, e.g., USB.
If sufficient information is available for Expedition to determine the sensor speed (either by a water log or by availability of GPS data), it can infer the true wind speed and bearing from the apparent wind speed/bearing measured.

Expedition can be configured to transmit one or more of the values it received or inferred across a network connection. The data is prefixed with a so-called "boat ID" which can be configured in the Expedition software. We use Expedition's capability to send the sensor data through UDP to port 2012 on the sapsailing.com host. We use a simple UDP mirroring process implemented in Java (see class com.sap.sailing.expeditionconnector.UDPMirror) to forward the messages received to the various server instances, each listening on a different UDP port.

Those UDP messages are received and analyzed by an active instance of class UDPExpeditionReceiver. Those that can be parsed successfully are used to record wind fixes in a tracked race(s) with which the receiver is associated by means of ExpeditionWindTracker objects. These objects are managed by an ExpeditionWindTrackerFactory together with the UDPExpeditionReceiver objects of which obviously only one can exist per inbound UDP port. The default port for those Expedition UDP receivers is specified by the expedition.udp.port VM property.

## Result Importers
For the operations of many regattas, software solutions are already in place. Those are used to manage competitor lists, fleet assignments, boat class to race course area assignments and of course scoring and ranking. Usually, they are the single source of truth also for the race committee and the sailors.
The SAP Sailing Analytics primarily base their leader boards on the tracking results. However, the tracking not always reflects the actual scoring as decided by the race committees. We therefore are interested in the ability to import the official results into our solution so that in addition to the GPS tracking data we have a copy of the truth when it comes to the scoring process. This is particularly exciting during the last race of a regatta when we can augment the official scores with the live tracking of the race currently ongoing.

We have provided an interface to import official results into our leader boards. Result importers implementing the ScoreCorrectionProvider interface can register with the OSGi service registry and are dynamically discovered. Such a provider can tell for which events and boat classes and dates it has "score corrections" which is a more technical term for "official results."

We currently have ScoreCorrectionProvider implementations for the 2011 and 2012 Kieler Woche result system (see bundle com.sap.sailing.kiworesultimport), for the french "FREG" system (see bundle com.sap.sailing.freg.resultimport) and for the Extreme Sailing Series (see bundle com.sap.sailing.ess40.resultimport). In their activators, these bundles register an instance of their score correction provider implementation with the OSGi service registry. This way, more importers can easily be added, potentially even at runtime, simply by deploying and starting another result importer bundle. See also section Adding a ScoreCorrectionProvider.

## Kieler Woche Result Importer
There is no online access to the Kieler Woche regatta system. However, we were able to agree an FTP result export with the system provider, b+m. The export format is a ZIP file containing numerous XML and PDF files. The XML files are then analyzed and provide the contents for the score corrections. We created a dedicated account for the FTP export on sapsailing.com ("kiwo"). The FTP export ends up in /home/kiwo, and all server instance directories under /home/trac/servers/(dev|test|prod1|prod2) contain a link "kiworesults" pointing to /home/kiwo.

The score correction provider implementation scans the "./kiworesults" directory for the ZIP files and offers their contents through its API.

## FREG Result Importer
At the 505 Worlds in La Rochelle we were faced with an HTML export format. Unfortunately, there was no stable URL scheme from which to obtain the HTML documents exported from the regatta management system. Therefore, we decided to leave the URL configuration to the administrator of our solution and added a tab "FREG URLs" to the AdminConsole. The documents reachable through the URLs added to this page will be scanned by the FREG score correction provider.

## ESS40 Result Importer
The Extreme Sailing Series currently manages their results through sailracer.org. The series has their own iPad app to capture the finish line passings. This app produces a CSV file managed on the series' web server before it is converted and uploaded to the sailracer.org server. We get access to the CSV files and a document listing the CSV files available for the series. Those then feed into the score correction provider.

## Race Export
### Feature Description and related bundles
Using the export feature, data from tracked races (such as the fixes for competitors and buoys) can be downloaded in common track formats such as GPX or KML. Though this somewhat contradicts the proposition of SAP Sailing Analytics to be _the_ authority in analyzing tracking data, it may be handy in some cases. The export functionality is mainly encapsulated in the `com.sap.sailing.server.trackfiles` bundle, and exposed to clients via a POST servlet in the `com.sap.sailing.server.gateway` bundle (`TrackFilesExportPostServlet`).

### RouteConverter library
To support different kinds of file formats, the core code of the [RouteConverter](https://github.com/cpesch/RouteConverter) project is used. Even without the GPSBabel integration (which we cannot use due to license restrictions) RouteConverter itself can write to the most important file formats.

The core components of RouteConverter are packaged in the bundle ``slash.navigation.routeconverter``. If upstream changes from the original repository should be integrated, overwrite the direct copy in the ``routeconverter-vanilla`` branch. Afterwards, merge this branch into ``routeconverter-adapted``, where custom changes to the library (removing unused components, in-memory preferences factory, ...) live.

The diff between ``routeconverter-vanilla`` and ``routeconverter-adapted`` represents our custom changes to the library. To get new changes into a development version, merge ``routeconverter-adapted`` into ``master`` or whatever feature branch you are working on.

### Using the Export feature
Though the export functionality is theoretically accessible by everyone, the UI counterpart resides only within the admin console as an _Export_ button underneath every `TrackedRaceListComposite` (e.g. in the TracTrac panel). If races have been tracked and are selected, the button can be pressed, then the appropriate settings can be made in the popup dialog, and as a result a post request containing this configuration data and the desired races is issued automatically, which returns a ZIP file to the user.