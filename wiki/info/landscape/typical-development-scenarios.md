# Typical Development Scenarios

[[_TOC_]]

## Adding a New Boat Class

Support for different boat classes comes with the following aspects:
* Name and possible alias names for the boat class
* hull dimensions
* separate downwind sail configuration for display
* boat class symbol / icon
* map visualization

If you'd like to add a boat class that the system does not yet support with its own boat class symbol or even a separate, new boat visualization on the map, the necessary extensions currently need to me implemented as code.

To start with, add an enumeration literal for the new class in [BoatClassMasterdata](https://github.com/SAP/sailing-analytics/blob/main/java/com.sap.sailing.domain.common/src/com/sap/sailing/domain/common/BoatClassMasterdata.java). The constructor needs to tell the default boat class name, whether races in this class typically have an upwind start (in which case we'll be more confident in guessing wind direction for races in this class based on the first leg's bearing), the hull length and beam in meters, the hull type, whether or not boats of that class fly a separate downwind sail, and optionally a list of alternative boat class names that can help users find the class, or to reflect name changes over time (Laser becoming ILCA, for example).

To understand the next steps, you may also search your workspace for usages of other ``BoatClassMasterdata`` enumeration literals. You will typically find at least two more: one in [BoatClassImageResolver](https://github.com/SAP/sailing-analytics/blob/main/java/com.sap.sailing.gwt.ui/src/main/java/com/sap/sailing/gwt/common/client/BoatClassImageResolver.java) which helps define the boat class symbol / icon to be displayed in the UI and made available through the REST API, e.g., for the companion apps; and another in [BoatClassVectorGraphicsResolver](https://github.com/SAP/sailing-analytics/blob/main/java/com.sap.sailing.gwt.ui/src/main/java/com/sap/sailing/gwt/ui/client/shared/racemap/BoatClassVectorGraphicsResolver.java) which defines how boats of this class are to be visualized on the map.

For the boat class symbol you have to create a transparent 140x140 pixel PNG file and place it in the ``java/com.sap.sailing.gwt.ui/src/main/resources/com/sap/sailing/gwt/ui/client/images/boatclass`` folder. The file's base name must match the ``BoatClassMasterdata`` enumeration literal you created _exactly_. The file extension shall be ``.png``. Then, define a GWT image resource in [BoatClassImageResolver](https://github.com/SAP/sailing-analytics/blob/main/java/com.sap.sailing.gwt.ui/src/main/java/com/sap/sailing/gwt/common/client/BoatClassImageResolver.java), using one of the other resource definitions in that class as an example:
```
    @Source("com/sap/sailing/gwt/ui/client/images/boatclass/FINN.png")
    @ImageOptions(preventInlining = true)
    ImageResource FinnIcon();
```
In ``BoatClassImageResolver`` then use this image resource, e.g., as follows:
```
        boatClassIconsMap.put(BoatClassMasterdata.FINN.getDisplayName(), imageResources.FinnIcon());
```
If any of the already existing boat class map visualizations work for your new class, simply add your new enumeration literal to the ``BoatClassVectorGraphics`` constructor in ``BoatClassVectorGraphicsResolver``'s static initializer that best matches your boat class. If you feel you need a better, more specific boat class visualization on the map, you'll have to [[implement your own|wiki/howto/development/boatgraphicssvg.md]] ``BoatClassVectorGraphics`` class and use that in the static initializer of ``BoatClassVectorGraphicsResolver``.

Mention the new boat class in the release notes, and don't forget to add all new files to your Git index before committing ;-).

## Adding an OSGi Bundle
Make sure you have read [[Workspace, Bundles, Projects|wiki/info/general/workspace-bundles-projects-structure]] before continuing. We distinguish two cases: adding a 3rd-party bundle to the target platform and adding a new development bundle as a Java project.

## Adding a Bundle to the Target Platform
* Add a New Library which can not be found in any SAP Repository
* Check if the library is already OSGi-enabled (normally this means there is a MANIFEST.MF file in the META-INF folder of the JAR file containing valid OSGi metadata.
* If you happen to have a corresponding source JAR, make sure it also has a MANIFEST.MF file in its META-INF folder. This manifest has to contain the ``Eclipse-SourceBundle`` header, as in the following example:
```
Manifest-Version: 1.0
Bundle-ManifestVersion: 2
Bundle-Name: mongo-java-driver-source
Bundle-SymbolicName: org.mongodb.mongo-java-driver.source
Eclipse-SourceBundle: org.mongodb.mongo-java-driver;version="3.6.4";roots:="."
Bundle-Version: 3.6.4
```
* In case the library is not OSGi-enabled someone has to create such a OSGi-enabled version (ask the technical lead of the project)
* Add the library to an appropriate target folder under plugins/ in the project com.sap.sailing.targetplatform.base (e.g. target-base)
* Add a corresponding entry to the corresponding feature.xml in the project com.sap.sailing.targetplatform.base
* Go to directory 'java/com.sap.sailing.targetplatform/scripts'
 * Rebuild the base target platform by running the script 'createLocalBaseP2repository.sh'
 * Generate the target definition for this local repository by running the script 'createLocalTargetDef.sh'
 * Currently this scripts work for the cygwin shell and the git bash, if the path to your files in the target definition is incorrect, this may be the reason.
* Test the new overall target platform
 * by setting the race-analysis-p2-local.target as target platform in the IDE
 * by running the ``release`` workflow on Github using the "Build with a local target platform (set to "true" to use local target platform)" switch, or
 * run the local maven build via ''buildAndUpdateProduct.sh -v build'' (the ''-v'' switch builds and uses the local p2 repository)
* The admin of the central p2 repository (currently at sapsailing.com) must now replace the content of the central server /home/trac/p2-repositories/sailing with the content of the new local base p2 repository (com.sap.sailing.targetplatform.base/target/repository), using the 'uploadRepositoryToServer.sh' script
* Reload the target platform in the IDE

## Adding or Upgrading Bundles from a p2 Repository

The Eclipse p2 handling is sometimes mysterious. Listing the plugins in a repository doesn't seem to be supported by the Eclipse UI. However, not always do we want to add entire features; sometimes, just a list of bundles would do. The following command, executed on the command line, executed from your Eclipse installation directory, can help you find the right bundles and their versions to add textually to the *.target definition file:

  'java -jar plugins/org.eclipse.equinox.launcher_1.3.0.v20140415-2008.jar -debug -consolelog -application org.eclipse.equinox.p2.director -repository http://download.eclipse.org/releases/luna/ -list'

Note that you may have to adjust the exact file name and version number of the equinox launcher JAR file according to your installed Eclipse version.

From the list shown, pick the bundle you need and add it to the '*.target' file, as in

  '<unit id="org.apache.felix.gogo.runtime" version="0.10.0.v201209301036"/>'

## Adding an Existing Remote p2 Repository as New Source of Libraries
* Add the URL of the remote p2 repository to all target definition files in com.sap.sailing.targetplatform/defintions
* Select the features of the p2 repository you want to use in the project
* Reload the target platform

## Working with the Amazon AWS SDK Java API

A Java API exists for the Amazon Web Services (AWS) in the form of a modular Software Development Kit (SDK). It can be found, e.g., on Maven Central, under the group ID ``software.amazon.awssdk``. A core module and various service-specific modules exist, e.g., for load balancing, Route 53 DNS handling, or the EC2 base infrastructure. Source JARs are available for the API, too, and the API has further dependencies to other artifacts. None of these artifacts is OSGi-enabled.

In order to use the SDK in our OSGi context, we decided to build a wrapper bundle that aggregates all the artifacts we need in a ``lib/`` folder and exports all ``software.amazon.awssdk`` packages. In order not to have to add all current and future versions of these JAR files as binaries to our Git repository we decided to make the wrapper bundle part of our target platform and offer a dedicated update site for it which can be upgraded quickly and without blowing up the Git repository size.

This works very similar how we build and promote the update site holding all other bundles for which we didn't find public P2 update sites serving them, only that in addition we use an additional project to build the actual wrapper bundle which then becomes part of the update site (which so far serves exactly only this one bundle). The following elements are relevant in this process:

- java/com.amazon.aws.aws-java-api: the project folder for building the wrapper bundle

- java/com.amazon.aws.aws-java-api/build.gradle: specifies which version and which components of the SDK to wrap, and for which components to also include the source JARs

- java/com.amazon.aws.aws-java-api/createLocalAwsApiP2Repository.sh: script to evaluate the ``build.gradle`` file, download the SDK JARs, generate all Eclipse project structures, update the version specification in other relevant artifacts such as the target platform definition, the feature.xml specification in the update site configuration, and the pom.xml Maven file, build the wrapper bundle, copy it to the update site project's ``plugins`` folder and build the update site.

- java/com.amazon.aws.aws-java-api.updatesite: the project used to build the local P2 update site to which the SDK wrapper bundle is moved after a successful build; running the project's Maven build constructs the local update site under the folder ``target/repository``

### Using the Local Wrapper Bundle Project Instead of the Bundle from the Central Target Platform
If you want to make modifications to the SDK wrapper bundle that you would like to work with in your local Eclipse environment before upgrading the central update site, you can prepare the wrapper bundle project for import into your Eclipse workspace as follows.
In a Bash shell environment, change directory to ``java/com.amazon.aws.aws-java-api``. There, run

```
    ./createLocalAwsApiP2Repository.sh
```

This will make the current folder an Eclipse-importable project, with a ``.project`` file, a valid ``META-INF/MANIFEST.MF`` OSGi manifest, an according ``build.properties`` and ``.classpath`` file reflecting the set of JAR files and source attachment JARs constituting the part of the SDK that the wrapper bundle is to expose. During the process, the version specifier entered in ``build.gradle`` (e.g., 2.13.50) will be copied to all other relevant places, such as the ``race-analysis-p2-remote.target`` target platform definition file, the update site specification's ``feature.xml`` file and the ``pom.xml`` file of the wrapper bundle project that you're currently in. A Maven build will start, building the wrapper bundle JAR file which will then be moved to the update site sibling project at ``java/com.amazon.aws.aws-java-api.updatesite`` where it ends in the ``plugins/aws-sdk`` folder. Then, a Maven build of the update site project is triggered, which effectively runs a Tycho update site assembly, producing a local update site repository under ``java/com.amazon.aws.aws-java-api.updatesite/target/repository``.

Now you have two ways of consuming the local SDK wrapper bundle

- You can import the ``java/com.amazon.aws.aws-java-api`` project into your workspace where the OSGi bundle it represents will take precedence over the equal-named bundle coming from the target platform pointing at the central update site. This way you can keep making adjustments relatively easy, e.g., by adjusting ``build.gradle``, running ``./createLocalAwsApiP2Repository.sh`` again and simply refreshing the project as the script has updated your OSGi manifest, build.properties and .classpath accordingly.
- You can create and use an adjusted local target platform definition by running the script ``java/com.sap.sailing.targetplatform/scripts/createLocalTargetDef.sh`` which will generate a copy of ``race-analysis-p2-remote.target`` called ``race-analysis-p2-local.target`` which uses the two local update sites under ``java/com.sap.sailing.targetplatform.base/target/repository`` and ``java/com.amazon.aws.aws-java-api.updatesite/target/repository`` instead of their central counterparts from ``http://p2.sapsailing.com/p2``. This approach is a bit more heavy-weight as particularly in Eclipse target platform modifications feel a bit brittle and in some cases require Eclipse restarts or other tricks to get reflected.

### Building and Uploading the Update Site for the AWS SDK

After having run the ``java/com.amazon.aws.aws-java-api/createLocalAwsApiP2Repository.sh`` script you have a full-fledged update site locally under ``java/com.amazon.aws.aws-java-api.updatesite/target/repository``. To upload it into the central P2 repository under ``http://p2.sapsailing.com/p2/aws-sdk`` use the script ``java/com.sap.sailing.targetplatform/scripts/uploadAwsApiRepositoryToServer.sh``. It will create a backup copy of the existing update site and then replace it with the one you have created locally.

### Upgrading the SDK to a New Release or Adding Components

Adjust the version number in the file ``java/com.amazon.aws.aws-java-api/build.gradle`` and/or adjust the list of components you'd like to include in the wrapper bundle. You should be able to find a line of this form:

```gradle
    implementation platform('software.amazon.awssdk:bom:2.13.58')
```

Adjust the version specifier at the end to what you'd like to have.

Add more components by adding more lines, such as

    implementation 'software.amazon.awssdk:ec2'

and repeat the process as needed. To cause the download of source JARs which can then be attached, add lines such as the following to the dependencies section:

    implementation group: 'software.amazon.awssdk', name: 's3', classifier: 'sources'

This example will fetch the source JAR corresponding to the s3 code JAR. 

Then run the ``java/com.amazon.aws.aws-java-api/createLocalAwsApiP2Repository.sh`` script again to produce an updated local target platform. You can then, as described in the previous sections, work with ``java/com.amazon.aws.aws-java-api`` project in Eclipse, or you generate and use a local target platform definition file that uses your locally-generated P2 update site containing your upgraded SDK wrapper bundle.

If you want to make your changes available to others, commit the changes that the ``createLocalAwsApiP2Repository.sh`` script has facilitated (this does on purpose not include the manifest, build.properties or .classpath file of the wrapper bundle project which are always generated from scratch by the script, but does include the version specifier changes in other files) and upload the P2 update site to the central update site web server using the ``java/com.sap.sailing.targetplatform/scripts/uploadAwsApiRepositoryToServer.sh`` script. Then inform fellow developers about the change because unless they now merge your changes, their target platforms will fail to resolve the SDK wrapper bundle which now has a different version.

### Using Hudson to Validate Changes to the SDK Wrapper Bundle not yet Pushed to the Update Site

The build script ``configuration/buildAndUpdateProduct.sh`` has a ``-v`` option that can be used to run the build using a target platform that references the P2 update sites built according to the specification in the workspace instead of the central P2 update sites at ``http://p2.sapsailing.com/p2``. Besides the AWS SDK update site there is our default base P2 update site that is built by the project ``java/com.sap.sailing.targetplatform.base``.

If you plan to test any changes to the AWS SDK with a full CI build before updating the P2 update site on ``http://p2.sapsailing.com/p2/aws-sdk`` you can make your adjustments to ``java/com.amazon.aws.aws-java-api/build.gradle`` and commit them *without* running the ``java/com.amazon.aws.aws-java-api/createLocalAwsApiP2Repository.sh`` script first. This way, when building with the ``-v`` option, the build script will run the ``createLocalAwsApiP2Repository.sh`` script during the build, constructing the update site in the build's workspace and using it for the build going forward.

**_Note_**: Should you have made a change to the version specifier in ``build.gradle`` and should you have run the ``createLocalAwsApiP2Repository.sh`` script locally to test the changes locally before submitting your changes for CI testing, make sure to **_not_** commit the changes to the ``race-analysis-p2-remote.target`` file. It would otherwise lead to a failure when trying to resolve the target platform during the build of the SDK wrapper bundle itself because that new version cannot yet be resolved in the existing P2 repositories.

## Adding a GWT RPC Service

We use a few GWT RPC services which offer easy serialization and asynchronous callback across the wire in a type-safe way. Historically, there was a single service covering a lot of ground: the SailingService. Over time, this service was split into several, including UserManagementService and MediaService.

One challenge we faced was that the default URL under which a client-side service would expect the servlet is simply constructed by appending the relative service URL provided in the '@RemoteServiceRelativePath' annotation to the URL that hosts the current .html document that loaded the entry point. This, however, does not work for servlets exposed by OSGi web bundles such as the 'com.sap.sailing.gwt.ui' bundle. Web bundles define in their MANIFEST.MF a Web-ContextRoot which is used as a prefix to all servlet URLs. In the case of the 'com.sap.sailing.gwt.ui' bundle this is '/gwt'.

To change the URL that a client-side service uses to construct the requests to the servlet, a dirty trick is required. The implementation of the service proxy generated by using something like 'GWT.create(SailingService.class)' also implements the 'com.google.gwt.user.client.rpc.ServiceDefTarget' interface. Casting the service proxy returned by the 'GWT.create(...)' call to this interface allows us to set the service's entry point. Here is an excerpt of the method 'AbstractEntryPoint.doOnModuleLoad()':

<pre>
                EntryPointHelper.registerASyncService((ServiceDefTarget) sailingService, RemoteServiceMappingConstants.sailingServiceRemotePath);
</pre>

Whenever you add another GWT RPC service, make sure to add it to the list of services whose service entry point URL is adjusted in 'AbstractEntryPoint.doOnModuleLoad()'.

### Adding a GWT RPC service that is implemented in a different bundle

When the RPC service implementation "lives" in a bundle different from the calling one, you'll need to also register the servlet in the calling bundle's 'web.xml'. This is essential because during resolving the serialization policy, the client assumes that the service has the same base URL as the module using it. Adding it to your bundle's 'web.xml' is no problem, even if it is declared in one or more other 'web.xml' files of other bundles.

## Adding a GWT Library to the com.sap.sailing.gwt.ui Project
* Copy the library (the jar file) to the folder /WEB-INF/lib
* Add the library to the bundle classpath (in the META-INF/manifest.mf file)
* Add a build dependency for the GWT compiler to the pom.xml
* Add the library to our central maven repository /home/trac/maven-repositories by using the mvn: install:install-file command
* Command sample to add the library gwt-maps-api-3.9.0-build-17.jar: mvn install:install-file -Dfile=/home/trac/git/java/com.sap.sailing.gwt.ui/WEB-INF/lib/gwt-maps-api-3.9.0-build-17.jar -DgroupId=com.github.branflake2267 -DartifactId=gwt-maps-api -Dversion=3.9.0-build-17 -Dpackaging=jar -DlocalRepositoryPath=/home/trac/maven-repositories

## Adding a shared GWT library bundle

Sometimes we need a bundle containing code and resources which can be used by other GWT bundles (GWT library).
To create and use such a bundle from another GWT bundle (e.g. com.sap.sailing.gwt.ui) some additional steps compared to the normal Java bundle creation are required.

GWT library bundle project creation

* Add a new Java plugin project, using <git-workspace>/java/<bundle-name> as the project directory
* Add the GWT-SDK dependency to the project 
* Add a GWT module file to project (ModuleName.gwt.xml)
* Add the bundle to the main pom.xml in /git/java

Use the GWT library project in a GWT UI project

* Add the src folder of the GWT library bundle to classpath of all GWT-Launch configurations (required to let the GWT dev mode find the sources of java classes) -> Open GWT launch config, select tab 'classpath', select 'User entries', click 'Advanced' button, choose 'add folders' option and add the src folder of the GWT library project
* Add the bundle the pom.xml of the GWT UI project (for the maven build)


## Adding an GWT Extension Library (With Source Code)
TODO (see Highcharts example)

## Adding a Self-Created p2 Repository from a Maven-Based External
Project as new source of libraries
Example: Integration of 'Atmosphere' framework (Server push technology)
TODO

## Adding a Java Project Bundle
* Add a new Java plugin project, using <git-workspace>/java/<bundle-name> as the project directory; if you need an activator, you can let Eclipse generate one for you. Deselect "Create a plug-in using one of the templates" and press "Finish."
* Connect the project to eGit by selecting "Share Project..." from the "Team" menu. When Eclipse suggests the git directory to connect to, select the "Use or create repository in parent folder of project" checkbox at the top of the dialog. This will then usually already suggest the correct git workspace to add to.
* Add the project to the com.sap.sailing.feature project's feature.xml descriptor in the Plug-ins tab. This ensures the bundle will be added to the product built based on the raceanalysis.product descriptor.
* Add a pom.xml file to integrate the bundle with the maven build. You may start by copying a pom.xml file from a similar project. Note that the pom.xml's <packaging> specification varies between test and non-test bundles. Test bundles use "eclipse-test-plugin" as their packaging type, all other bundles use "eclipse-plugin" here. Adjust the version and artifactId tags correspondingly.
* Add an entry to the parent pom.xml in the java/ folder
* If you need the bundle to get started explicitly during OSGi container start-up (e.g., a web bundle that wouldn't start otherwise), add the bundle to the ``raceanalysis.product`` specification with the appropriate start level, then run ``java/com.sap.sailing.feature.p2build/scripts/updateAll.sh`` to adjust all launch configurations and the Selenium product start-up configuration accordingly.

## Adding a Column to the Leaderboard
It is a typical request to have a new column with a new key figure added to the leaderboard structure. A number of things need to be considered to implement this.

### Extending LeaderboardDTO Contents
The content to be displayed by the LeaderboardPanel user interface component is transmitted from the server to the client using a LeaderboardDTO object. The class describes overall measures and data for the entire leaderboard, has overviews for each race as well as all detail figures for all legs. For example, the LeaderboardDTO object contains LeaderboardRowDTO objects, one for each competitor. Each such row object, in turn, contains LeaderboardEntryDTOs, one for each race column in the leaderboard. Those contain aggregates for the respective race/competitor as well as a list of LegEntryDTOs describing key figures for the competitors performance in each leg of the race.

Depending on the level (overall, race, leg) at which to add a column, a corresponding field may need to be added to one of LeaderboardDTO, LeaderboardRowDTO, LeaderboardEntryDTO or LegEntryDTO. It may, however, at times also be possible to derive a new value from other values already fully contained in the LeaderboardDTO object. In such a case, no change is required to the LeaderboardDTO type hierarchy.

### Filling LeaderboardDTO Extensions
The LeaderboardDTO objects are constructed in SailingServiceImpl.computeLeaderboardByName and its outbound call hierarchy. Looking at the existing code, it should become clear how to extend this. Usually, if really new measures are to be introduced, a corresponding extension to TrackedRace and its key implementation class TrackedRaceImpl becomes necessary where the new measures are computed, based on the raw tracking data and the various domain APIs available there.

When a new measure is introduced such that computeLeaderboardByName needs to access data which may change over time, it is important to make sure that the leaderboard caches in LiveLeaderboardUpdater and LeaderboardDTOCache are invalidated accordingly if the data used by the computations introduced changes. For example, if the new figure depends on the structure of the leaderboard, the cache needs to be invalidated when the leaderboard's column layout changes. Fortunately, in this case, such an observer pattern already exists for the leaderboard caches and can be used to understand how such a pattern needs to be implemented.

### Adding the Column Type
The LeaderboardPanel and its dependent components are the user interface components used to display the leaderboard. The underlying GWT component used to render the leaderboard is a CellTable with Column implementations for the various different column types. In a little "micro-framework" we support expandable columns (those with a "+" button in the header allowing the user to expand more details for that column), columns with CSS styles that travel with the column as the column moves to the right or the left in the table, as well as sortable columns. A special column base class exists to represent Double values. This column type displays a colored bar, symbolizing the relative value's magnitude, compared to the other values in the column.

A column class is implemented as a subclass of SortableColumn. If the column is to display a numeric value where comparing between values makes sense, consider using a FormattedDoubleLegDetailColumn which displays a bar in the value's background, indicating the value's magnitude. To make this column type widely applicable, its constructor accepts a LegDetailField value which is responsible for computing or extracting the value to be displayed from a LeaderboardRowDTO object.

A column which may itself have details should be implemented as a subclass of ExpandableSortableColumn, implementing the getDetailColumnMap method to specify the detail columns, as explained later in Adding to Parent Column's Detail Column Map.

### Extending DetailType
The enumeration type DetailType has a literal for each column type available. The literal describes the column's precision as a number of decimals (not used for non-numeric column types) and the default sorting order.

To make the new DetailType literal displayable, DetailTypeFormatter.format needs to be extended by a corresponding case clause so that it supports the new literal.

### Adding to List of Available Columns
Mostly for the presentation in the leaderboard settings panel, the lists of available column types are maintained in class-level methods on LeaderboardPanel (getAvailableRaceDetailColumnTypes and getAvailableOverallDetailColumnTypes) ManeuverCountRaceColumn (getAvailableManeuverDetailColumnTypes) and LegColumn (getAvailableLegDetailColumnTypes). When adding a detail column to a parent column, the corresponding getAvailable...ColumnTypes method needs to be extended by returning the respective DetailsType literal so the column is offered in the settings panel.

### Adding to Parent Column's Detail Column Map
If the column added is supposed to be a detail of a parent column, the new column type needs to be returned by the parent column's getDetailColumnMap method. Check the LegColumn.getDetailColumnMap for details.

**Discussion**
The current approach to extending the leaderboard panel by another column is unnecessarily laborious and contains a number of redundancies. In particular, adding the column to both, the detail column map and the list of "available columns" only used by the settings seems highly redundant. When constructing a FormattedDoubleLegDetailColumn, the DetailType literal is used three times: once for the key of the detail column map, then for the precision and default sorting order. This should be simplified.

Generally, there are too many special cases necessitating specific handling. Instead, it would be much better to have a homogeneous hierarchy of column types which automatically leads to the necessary results in getDetailColumnMap and the settings dialogs.

## Adding new available information to the competitor chart

The user can choose the information to be displayed in the competitor chart by choosing from the available information. 
This information is a subset of the information available as defined in the DetailType enumeration.

For a given information to be available for the Competitor-Chart, it must:

* be defined as a enum element in DetailType
* be added to the list of available items in the constructor of MultiCompetitorRaceChartSettingsComponent

Further,

* the DetailTypeFormatter is used to properly render the information label
* the SailingService.getCompetitorRaceDataEntry(...) Method is used to load the specified information



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
1. For persisting and loading your Domain Objects to and from MongoDB, you can have a look in 'MongoObjectFactoryImpl' and 'DomainObjectFactoryImpl'. Note that you may be able to reuse existing JSON serializers and deserializers to create a String representation of Domain Objects and to create Domain Objects from JSON strings. With the JSON Strings, you can interface with MongoDB via the com.mongodb.util.JSON class, e.g.
'''
JSONObject json = competitorSerializer.serialize(competitor);
BasicDBObject query = new BasicDBObject(FieldNames.COMPETITOR_ID.name(), competitor.getId());
DBObject entry = (DBObject) JSON.parse(json.toString());
'''
or
'''
String jsonString = JSON.serialize(o);
JSONObject json = Helpers.toJSONObjectSafe(JSONValue.parseWithException(jsonString));
Competitor c = competitorDeserializer.deserialize(json);
'''

2. Persisted Domain Objects should be loaded from the MongoDB after a server restart on the master instance. This is best done in the 'RacingEventServiceImpl' constructor, where quite a few calls to other 'load*' methods reside. If the objects are managed via a domain factory (e.g. implement 'IsManagedBySharedDomainFactory'), remember to register them with that domain factory.

3. Whenever you add a Domain Object to an instance, it somehow has to be replicated to the other instances. To do so, create an Operation which you can then 'apply()' to the 'RacingEventService', which will then replicate it to other intsances (the operations are basically commands as in the Command pattern, with the added difficulty of operational transformation to provide a uniform final state when operations are applied in different orders on different instances). Internally, the apply-mechanism writes to an 'ObjectOutputStream', which on the other side is evaluated by an 'ObjectInputStreamResolvingAgainstDomainFactory'. For this reason, all objects implementing the 'IsManagedBySharedDomainFactory' interface are resolved against the domain factory via their 'resolve()' implementation. This removes the chance of duplicate instances representing the same actual object.

4. Also, whenever a replica (slave) instance registers with the master instance, it is provided with the current state of the master as an initial load. To do so, the master instance exports its state via the 'serializeForInitialReplication()' method in the 'RacintEventServiceImpl', while the replica recieves the object stream output by the master via the 'initiallyFillFrom()' method. Again, an 'ObjectInputStreamResolvingAgainstDomainFactory' is used.

## Import Another Year of Magnetic Declination Values

Under java/com.sap.sailing.declination/resources we store magnetic declination values, using one file per year. The resolution at which we usually store those is one degree of latitude and longitude, each. When for a year those values aren't found in a file, an online request is performed to the [NOAA Service](http://www.ngdc.noaa.gov/geomag-web/) which can be time and bandwidth consuming. Therefore, it is a good idea to keep a file with cached declination values around for the current year.

To produce such a file, use the 'main(...)' method of class 'com.sap.sailing.declination.impl.DeclinationStore'. There are pre-defined launch configurations in place in the com.sap.sailing.declination bundle project. Adjust the from/to year parameters for the current year. The process usually takes several hours to complete at a one-degree resolution. Don't forget to commit the resulting resources/declination-<year> file to git.

Experience has shown that sometimes the SAP HTTP proxy doesn't properly resolve the NOAA service. In those cases, it is more convenient to run the process from either sapsailing.com or stg.sailtracks.de, using something like following command from a server's plugins/ directory after creating the resources/ subdirectory:

'java -cp com.sap.sailing.domain_*.jar:com.sap.sailing.domain.common_*.jar:com.sap.sailing.declination_*.jar:com.sap.sailing.domain.shared.android_*.jar com.sap.sailing.declination.impl.DeclinationStore 2014 2014 1'

Run this inside a tmux window to be sure that logging off does not interrupt the process. After the process completes, copy the resulting declination-<year> file to your git workspace to 'java/com.sap.sailing.declination/resources' and commit.

There is also a script java/target/importdeclination that automates these steps.

## Dynamic Remote Debugging using the SAP JVM

The SAP JVM (see [http://sapjvm:1080](here)) offers a nice feature. It can be switched into debug mode during runtime, using the ``jvmmon`` tool from the ``bin`` folder of the VM distribution, next to the ``java`` binary. Imagine you have started a VM in production mode and it starts misbehaving. You isolate the instance, e.g., by removing it from the load balancer (ELB) or by replacing it in the central Apache configuration by a new one. Now you have time to investigate the VM's state more closely. But attaching a jconsole often isn't sufficient to understand what has gone wrong. With the SAP JVM you can proceed as follows:

<pre>
[sailing@ip-172-31-28-55 ~]$ /opt/sapjvm_8/bin/jvmmon &lt;java PID&gt;
$ start debugging
$ print debugging information
State : Debugging back is waiting for debugger to connect
Port  : 8000
$ exit
</pre>

Now connect an Eclipse debugger to that VM's port 8000. This may require an SSH tunnel that forwards the port accordingly. When done, use ``jvmmon`` again to stop debugging, just like you started it:

<pre>
[sailing@ip-172-31-28-55 ~]$ /opt/sapjvm_8/bin/jvmmon &lt;java PID&gt;
$ stop debugging
$ exit
</pre>

## Upgrade TracTrac TracAPI Release

The TracAPI is a Java API provided by TracTrac that allows applications to interact with TracTrac's tracking services. It comes as a binary JAR file, a JAR file containing the source code of the API interfaces, as well as a JAR file containing the Javadocs. The SAP Sailing Analytics wrap all of this in an OSGi bundle called ``com.tractrac.clientmodule``. We keep this bundle's version in sync with the TracAPI version. The version is encoded in the ``META-INF/MANIFEST.MF`` file as well as the Maven ``pom.xml`` file and needs to be adjusted during an upgrade.

When TracTrac publishes a TracAPI upgrade, they usually notify us by e-mail and provide download locations for the new release pointing to TracTrac's Maven site, such as http://tracdev.dk/maven-sites-clients/releases/TracAPI-3.6.3.tar.gz. You will need credentials for that site.

After downloading, extract the ``lib/TracAPI.jar`` and ``src/TracAPI-src.jar`` from the ``TracAPI-x.y.z.tar.gz`` to ``java/com.tractrac.clientmodule/lib``. Unpack the ``TracAPI-x.y.z-javadoc.tar.gz`` into ``java/com.tractrac.clientmodule/javadoc`` and adjust the versions in ``META-INF/MANIFEST.MF`` and ``pom.xml`` accordingly. Unpack the ``Readme.txt`` file from ``TracAPI-x.y.z.tar.gz`` to ``java/com.tractrac.clientmodule``. Optionally, unpack the sample sources from the ``src/com`` folder contained in ``TracAPI-x.y.z.tar.gz`` into ``java/com.tractrac.clientmodule/src``.

For non-trivial upgrades use of a git branch and dedicated build job is recommended. Otherwise, committing and pushing to the ``main`` branch with the ``SAPSailingAnalytics-master`` build job picking up and testing the changes should suffice.