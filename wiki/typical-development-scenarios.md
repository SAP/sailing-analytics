# Typical Development Scenarios

[[_TOC_]]

## Adding a Bundle
We distinguish two cases: adding a 3rd-party bundle to the target platform and adding a new development bundle as a Java project.

## Adding a Bundle to the Target Platform
Add a New Library which can not be found in any SAP Repository
*	Check if the library is already OSGi-enabled (normally this means there is a MANIFEST.MF file in the META-INF folder of the JAR file containing valid OSGi metadata.
*	In case the library is not OSGi-enabled someone has to create such a OSGi-enabled version (ask the technical lead of the project)
*	Add the library to an appropriate target folder under plugins/ in the project com.sap.sailing.targetplatform.base (e.g. target-base)
*	Add a corresponding entry to the corresponding feature.xml in the project com.sap.sailing.targetplatform.base
*	Rebuild the base target platform by running the script createLocalBaseP2repository.sh (com.sap.sailing.targetplatform/scripts). The script needs to be adjusted to your local paths for your Eclipse and GIT workspace directories.
*	Test the new overall target platform by settings the race-analysis-p2-ide-p2admin.target as target platform in the IDE
•	The admin of the central p2 repository (currently at sapsailing.com) must now replace the content of the central server /home/trac/p2-repositories/sailing with the content of the new local base p2 repository (com.sap.sailing.targetplatform/base/gen/p2)
*	Reload the target platform in the IDE

## Adding an Existing Remote p2 Repository as New Source of Libraries
*	Add the URL of the remote p2 repository to all target definition files in com.sap.sailing.targetplatform/defintions
*	Select the features of the p2 repository you want to use in the project
*	Reload the target platform

## Adding a GWT Library to the com.sap.sailing.gwt.ui Project
*	Copy the library (the jar file) to the folder /WEB-INF/lib
*	Add the library to the bundle classpath (in the META-INF/manifest.mf file)
*	Add a build dependency for the GWT compiler to the pom.xml
*	Add the library to our central maven repository /home/trac/maven-repositories by using the mvn: install:install-file command
* Command sample to add the library gwt-maps-api-3.9.0-build-17.jar: mvn install:install-file -Dfile=/home/trac/git/java/com.sap.sailing.gwt.ui/WEB-INF/lib/gwt-maps-api-3.9.0-build-17.jar -DgroupId=com.github.branflake2267 -DartifactId=gwt-maps-api -Dversion=3.9.0-build-17 -Dpackaging=jar -DlocalRepositoryPath=/home/trac/maven-repositories


## Adding an GWT Extension Library (With Source Code)
TODO (see Highcharts example)

## Adding a Self-Created p2 Repository from a Maven-Based External
Project as new source of libraries
Example: Integration of 'Atmosphere' framework (Server push technology)
TODO

## Adding a Java Project Bundle
*	Add a new Java plugin project, using <git-workspace>/java/<bundle-name> as the project directory; if you need an activator, you can let Eclipse generate one for you. Deselect "Create a plug-in using one of the templates" and press "Finish."
*	Connect the project to eGit by selecting "Share Project..." from the "Team" menu. When Eclipse suggests the git directory to connect to, select the "Use or create repository in parent folder of project" checkbox at the top of the dialog. This will then usually already suggest the correct git workspace to add to.
*	Add the project to the com.sap.sailing.feature project's feature.xml descriptor in the Plug-ins tab. This ensures the bundle will be added to the product built based on the raceanalysis.product descriptor.
*	Add a pom.xml file to integrate the bundle with the maven build. You may start by copying a pom.xml file from a similar project. Note that the pom.xml's <packaging> specification varies between test and non-test bundles. Test bundles use "eclipse-test-plugin" as their packaging type, all other bundles use "eclipse-plugin" here. Adjust the version and artifactId tags correspondingly.
*	Add an entry to the parent pom.xml in the java/ folder

## Adding a Column to the Leaderboard
It is a typical request to have a new column with a new key figure added to the leaderboard structure. A number of things need to be considered to implement this.

## Extending LeaderboardDTO Contents
The content to be displayed by the LeaderboardPanel user interface component is transmitted from the server to the client using a LeaderboardDTO object. The class describes overall measures and data for the entire leaderboard, has overviews for each race as well as all detail figures for all legs. For example, the LeaderboardDTO object contains LeaderboardRowDTO objects, one for each competitor. Each such row object, in turn, contains LeaderboardEntryDTOs, one for each race column in the leaderboard. Those contain aggregates for the respective race/competitor as well as a list of LegEntryDTOs describing key figures for the competitors performance in each leg of the race.

Depending on the level (overall, race, leg) at which to add a column, a corresponding field may need to be added to one of LeaderboardDTO, LeaderboardRowDTO, LeaderboardEntryDTO or LegEntryDTO. It may, however, at times also be possible to derive a new value from other values already fully contained in the LeaderboardDTO object. In such a case, no change is required to the LeaderboardDTO type hierarchy.

## Filling LeaderboardDTO Extensions
The LeaderboardDTO objects are constructed in SailingServiceImpl.computeLeaderboardByName and its outbound call hierarchy. Looking at the existing code, it should become clear how to extend this. Usually, if really new measures are to be introduced, a corresponding extension to TrackedRace and its key implementation class TrackedRaceImpl becomes necessary where the new measures are computed, based on the raw tracking data and the various domain APIs available there.

When a new measure is introduced such that computeLeaderboardByName needs to access data which may change over time, it is important to make sure that the leaderboard caches in LiveLeaderboardUpdater and LeaderboardDTOCache are invalidated accordingly if the data used by the computations introduced changes. For example, if the new figure depends on the structure of the leaderboard, the cache needs to be invalidated when the leaderboard's column layout changes. Fortunately, in this case, such an observer pattern already exists for the leaderboard caches and can be used to understand how such a pattern needs to be implemented.

## Adding the Column Type
The LeaderboardPanel and its dependent components are the user interface components used to display the leaderboard. The underlying GWT component used to render the leaderboard is a CellTable with Column implementations for the various different column types. In a little "micro-framework" we support expandable columns (those with a "+" button in the header allowing the user to expand more details for that column), columns with CSS styles that travel with the column as the column moves to the right or the left in the table, as well as sortable columns. A special column base class exists to represent Double values. This column type displays a colored bar, symbolizing the relative value's magnitude, compared to the other values in the column.

A column class is implemented as a subclass of SortableColumn. If the column is to display a numeric value where comparing between values makes sense, consider using a FormattedDoubleLegDetailColumn which displays a bar in the value's background, indicating the value's magnitude. To make this column type widely applicable, its constructor accepts a LegDetailField value which is responsible for computing or extracting the value to be displayed from a LeaderboardRowDTO object.

A column which may itself have details should be implemented as a subclass of ExpandableSortableColumn, implementing the getDetailColumnMap method to specify the detail columns, as explained later in Adding to Parent Column's Detail Column Map.

## Extending DetailType
The enumeration type DetailType has a literal for each column type available. The literal describes the column's precision as a number of decimals (not used for non-numeric column types) and the default sorting order.

To make the new DetailType literal displayable, DetailTypeFormatter.format needs to be extended by a corresponding case clause so that it supports the new literal.

## Adding to List of Available Columns
Mostly for the presentation in the leaderboard settings panel, the lists of available column types are maintained in class-level methods on LeaderboardPanel (getAvailableRaceDetailColumnTypes and getAvailableOverallDetailColumnTypes) ManeuverCountRaceColumn (getAvailableManeuverDetailColumnTypes) and LegColumn (getAvailableLegDetailColumnTypes). When adding a detail column to a parent column, the corresponding getAvailable...ColumnTypes method needs to be extended by returning the respective DetailsType literal so the column is offered in the settings panel.

## Adding to Parent Column's Detail Column Map
If the column added is supposed to be a detail of a parent column, the new column type needs to be returned by the parent column's getDetailColumnMap method. Check the LegColumn.getDetailColumnMap for details.

**Discussion**
The current approach to extending the leaderboard panel by another column is unnecessarily laborious and contains a number of redundancies. In particular, adding the column to both, the detail column map and the list of "available columns" only used by the settings seems highly redundant. When constructing a FormattedDoubleLegDetailColumn, the DetailType literal is used three times: once for the key of the detail column map, then for the precision and default sorting order. This should be simplified.

Generally, there are too many special cases necessitating specific handling. Instead, it would be much better to have a homogeneous hierarchy of column types which automatically leads to the necessary results in getDetailColumnMap and the settings dialogs.

## Adding a ScoreCorrectionProvider
External regatta management systems can usually provide the official scores through some electronic interface. These interfaces come with a pull or push transport protocol which may range from direct TCP, HTTP to an FTP file transfer, and a content format which can be anything from a simple CSV format to a complex XML document structure. Using such interfaces results in the capability of importing the official scores as score corrections into our leader boards, thus aligning the tracking results and the official results.

As regatta management systems vary vastly and from event to event, we use an open and flexible architecture for integrating with them. Key entry point is the ScoreCorrectionProvider interface that needs to be implemented for an integration with a regatta management system. An instance of the resulting type then has to be added to the OSGi service registry. Usually, this happens in an OSGi bundle activator of a bundle dedicated to the result import from the regatta management system to integrate. Example:

        Activator.context = bundleContext;
        final ScoreCorrectionProviderImpl service = new ScoreCorrectionProviderImpl();
        context.registerService(ScoreCorrectionProvider.class, service, /* properties */null);

When the bundle has been activated successfully in the OSGi container, then when a user triggers a result import, the importer will be asked for its results available so that the user can select them.

See the Javadocs and the existing implementations of the ScoreCorrectionProvider interface for more details.

## Adding a Maintainable Property on a Leaderboard
Using the example of the already existing property "factor" on the leaderboard columns, this section explains what needs to be done to add such a property.

## Domain Model
The structure of a leaderboard with its attached entities such as score corrections, discarding rules and the leaderboard column details are formalized by the Leaderboard interface and what is reachable from there. To capture an extension, usually one or more of those interfaces and corresponding implementation classes require extensions. For example, the factor property needed to be added to the RaceColumn interface in the form of the getFactor():double and setFactor(double) methods, implemented mainly by SimpleAbstractRaceColumn.

## Cache Invalidation
Usually, a new property along the leaderboard data model has effects on the results of SailingServiceImpl.computeLeaderboardByName method which are cached. In this case, a cached leaderboard needs to be invalidated when the new property is updated. For this purpose, the cache needs to observe the object changed, directly or transitively. The LeaderboardDTOCache class already maintains observer patterns using the ScoreCorrectionListener interface and the RaceColumnListener interface, observing each cached leaderboard's score corrections for changes, and listening for changes in the leaderboard's column structure and the column's attached races. Additionally, each tracked race associated with the leaderboard is observed already with a RaceChangeListener instance also managed by the LeaderboardDTOCache class.
It is therefore convenient if a change of the new property can be "funneled" into any of those existing observer relationships between the leaderboard and the LeaderboardDTOCache. In case of our example factor property the solution was adding a method factorChanged to the RaceColumnListener interface and letting LeaderboardDTOCache's RaceColumnListener implementation remove the leaderboard from the cache whose column had its factor changed.

## User Interface
We so far have implemented two typical styles for editing server-side properties. One uses modal pop-up dialogs, such as implemented, e.g., by the class FlexibleLeaderboardEditDialog. The class is a transitive subclass of DataEntryDialog<E> which implements a micro-framework for any type of pop-up and data capturing dialog.

In particular, with these dialogs comes some support for Enter/Esc key handling. To use it, UI data entry controls for the dialog need to be created using the create...(...) methods of the DataEntryDialog class, for example createCheckbox(String). The UI controls returned already have the keyboard interaction listeners necessary for Enter/Esc handling registered.

The micro-framework around DataEntryDialog support immediate validation and error message display. If the validator passed to the dialog's constructor considers the current values invalid, an error message constructed by the validator will be displayed in the dialog box, and the OK button will be disabled.

The second option for manipulating properties on the server is an in-place editing facility on the page displaying the data to be modified. For example, several of the tables that show server-side data such as leaderboards, leaderboard groups or leaderboard columns have a delete icon in the icon bar which is implemented by the class ImagesBarCell.

## Persistence Layer
Server state changes that need to be preserved across server restarts need to be stored persistently in the database, currently a MongoDB instance. For example, if an extension is designed for the RaceColumn interface and its implementing classes, the methods responsible for storing and loading objects of those types during storing and loading a leaderboard need to be extended accordingly.

For the domain objects which are independent of any particular tracking provider, the class com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl is responsible for storing objects to MongoDB. A leaderboard is stored by the method storeLeaderboard(Leaderboard). The database used is passed to the MongoObjectFactory's constructor which in turn is configured by the MongoFactoryImpl class, which in turn gets its properties set by the bundle activator which reads the configuration properties, particularly the hostname and port number of the MongoDB instance to connect to, from the OSGi context / system properties.

In order to store extensions, the DBObject instances that are stored to the database need to be extended accordingly. Check out, for example, the method storeColumnFactors which was added rather recently to support individual multipliers for leaderboard columns.

## Replication
Server state changes are usually relevant for replication. To ensure consistent replication to all replicas, the implementation of an Operation class describing and serializing the change is required.

The class CreateFlexibleLeaderboard may serve as a typical example of such an operation class. It holds the fields necessary to parameterize the method call to the RacingEventService object to which it can be applied (see the internalApplyTo method).

Most of the operation classes are instantiated in methods of the SailingServiceImpl class which currently handles all incoming client requests. To stay with the example, if in the client the creation of a flexible leaderboard is requested, the createFlexibleLeaderboard operation is invoked on the SailingServiceImpl remote servlet class. It creates an instance of CreateFlexibleLeaderboard based on the parameters passed to the SailingServiceImpl method call and applies the operation to the RacingEventService instance obtained through the OSGi service registry.

The RacingEventService, in turn, executes the internalApplyTo method to perform the changes described by the operation locally and then passes the operation to the replication service for propagation to all replicas registered. There, once received, the operation is again applied using the internalApplyTo method.

Usually, the internalApplyTo implementation makes use of the public methods exposed by RacingEventService to actually perform the changes. Note that there are a few cases in which invoking a method on RacingEventService triggers replication by itself, for example cacheAndReplicateDefaultRegatta which, after an implicit local state change, creates an operation solely for the purpose of replicating this local change which already took place. In those cases, the operation is not used to perform the state change locally, which should rather be the exception than the rule.

## Adding Persistence and Replication for Domain Objects
1. For persisting and loading your Domain Objects to and from MongoDB, you can have a look in `MongoObjectFactoryImpl` and `DomainObjectFactoryImpl`. Note that you may be able to reuse existing JSON serializers and deserializers to create a String representation of Domain Objects and to create Domain Objects from JSON strings. With the JSON Strings, you can interface with MongoDB via the com.mongodb.util.JSON class, e.g.
```
JSONObject json = competitorSerializer.serialize(competitor);
BasicDBObject query = new BasicDBObject(FieldNames.COMPETITOR_ID.name(), competitor.getId());
DBObject entry = (DBObject) JSON.parse(json.toString());
```
or
```
String jsonString = JSON.serialize(o);
JSONObject json = Helpers.toJSONObjectSafe(JSONValue.parseWithException(jsonString));
Competitor c = competitorDeserializer.deserialize(json);
```

2. Persisted Domain Objects should be loaded from the MongoDB after a server restart on the master instance. This is best done in the `RacingEventServiceImpl` constructor, where quite a few calls to other `load*` methods reside. If the objects are managed via a domain factory (e.g. implement `IsManagedBySharedDomainFactory`), remember to register them with that domain factory.

3. Whenever you add a Domain Object to an instance, it somehow has to be replicated to the other instances. To do so, create an Operation which you can then `apply()` to the `RacingEventService`, which will then replicate it to other intsances (the operations are basically commands as in the Command pattern, with the added difficulty of operational transformation to provide a uniform final state when operations are applied in different orders on different instances). Internally, the apply-mechanism writes to an `ObjectOutputStream`, which on the other side is evaluated by an `ObjectInputStreamResolvingAgainstDomainFactory`. For this reason, all objects implementing the `IsManagedBySharedDomainFactory` interface are resolved against the domain factory via their `resolve()` implementation. This removes the chance of duplicate instances representing the same actual object.

4. Also, whenever a replica (slave) instance registers with the master instance, it is provided with the current state of the master as an initial load. To do so, the master instance exports its state via the `serializeForInitialReplication()` method in the `RacintEventServiceImpl`, while the replica recieves the object stream output by the master via the `initiallyFillFrom()` method. Again, an `ObjectInputStreamResolvingAgainstDomainFactory` is used.

## Import Another Year of Magnetic Declination Values

Under java/com.sap.sailing.declination/resources we store magnetic declination values, using one file per year. The resolution at which we usually store those is one degree of latitude and longitude, each. When for a year those values aren't found in a file, an online request is performed to the [[www.ngdc.noaa.gov/geomag-web/]](NOAA service) which can be time and bandwidth consuming. Therefore, it is a good idea to keep a file with cached declination values around for the current year.

To produce such a file, use the main(...) method of class com.sap.sailing.declination.impl.DeclinationStore. There are pre-defined launch configurations in place in the com.sap.sailing.declination bundle project. Adjust the from/to year parameters for the current year. The process usually takes several hours to complete at a one-degree resolution. Don't forget to commit the resulting resources/declination-<year> file to git.

Experience has shown that sometimes the SAP HTTP proxy doesn't properly resolve the NOAA service. In those cases, it is more convenient to run the process from either sapsailing.com or stg.sailtracks.de, using something like following command from a server's plugins/ directory after creating the resources/ subdirectory:

java -cp com.sap.sailing.domain_*.jar:com.sap.sailing.domain.common_*.jar:com.sap.sailing.declination_*.jar:com.sap.sailing.domain.shared.android_*.jar com.sap.sailing.declination.impl.DeclinationStore 2014 2014 1

Run this inside a tmux window to be sure that logging off does not interrupt the process. After the process completes, copy the resulting declination-<year> file to your git workspace to java/com.sap.sailing.declination/resources and commit.