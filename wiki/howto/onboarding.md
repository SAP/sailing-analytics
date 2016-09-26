# OnBoarding Information

This document describes the onboarding process for a new team member (developer)

First of all, make sure you've looked at http://www.amazon.de/Patterns-Elements-Reusable-Object-Oriented-Software/dp/0201633612. That's a great book, and knowing at least some of it will help you a great deal finding your way around our solution.

### Race Analysis Development Setup

#### Installations

1. Eclipse (Eclipse IDE for Eclipse Committers, e.g. version 4.5.2 "Mars SR2"), http://www.eclipse.org
2. Eclipse Extensions
  * Install GWT Eclipse plugin for Eclipse (http://gwt-plugins.github.io/documentation/gwt-eclipse-plugin/Download.html)
  * Install Eclipse debugger for GWT SuperDevMode (master version: http://p2.sapsailing.com/p2/sdbg; public release: http://sdbg.github.io/p2)
3. Git (e.g. msysGit for Windows v2.9.2), http://git-scm.com
4. MongoDB (e.g. Production Release 2.6.7), download: http://www.mongodb.org/
5. RabbitMQ, download from http://www.rabbitmq.com/. Requires Erlang to be installed. RabbitMQ installer will assist in installing Erlang.
6. JDK 1.7 (Java SE 7), http://jdk7.java.net
7. JDK 1.8 (Java SE 8), http://jdk8.java.net
8. Maven 3.1.1 (or higher), http://maven.apache.org
9. GWT SDK 2.7.0 (http://www.gwtproject.org/download.html)
10. Android Studio (https://developer.android.com/tools/studio/index.html) or IntelliJ IDEA (https://www.jetbrains.com/idea/download/)

#### Automatic Eclipse plugin installation

The necessary Eclipse plugins described above can be automatically be installed into a newly unzipped version of [Eclipse IDE for Eclipse Committers 4.5.2 "Mars SR2"](http://www.eclipse.org/downloads/packages/eclipse-ide-eclipse-committers-452/mars2) by using the script "configuration/installPluginsForEclipseMars.sh". In addition, the script applies some updates to plugins packaged with Eclipse itself. To start the plugin installation, run the following command using your Eclipse installation directory as command line parameter for the script:

    ./installPluginsForEclipseMars.sh "/some/path/on/my/computer/eclipse"

Be aware that with this script it's not possible to update the plugins to newer versions. Instead you can install a new version by unpacking the base package and executing the script.

On Windows you need a Git Bash or Cygwin shell to run the script. In addition you need to replace all backslashes with forward slashes.

On Mac OS, it's not sufficient to provide the path to the app, instead you need to get the path to the directory inside of the app package hosting the "eclipse" binary (.../Eclipse.app/Contents/MacOS).

Be aware hat the installation may take several minutes depending on your Internet connection. When the script finished running, please check that no errors occurred (the installation process only logs errors but doesn't fail).

__NOTE:__
Beside the installation script for Eclipse Mars, there is also one for the new Eclipse Neon release (4.6.0). Due to a ECJ/JDT regression, this version will brake project functionality. If you've already installed Eclipse Neon, you can easily downgrade to Mars SR2, reusing your existing workspace. You just have to accept the warning you'll get during first startup and perform a workspace cleanup (`Project > Clean... > Clean all projects`). In rare cases, the target platform must be set again.

#### Further optional but recommended installations

1. Cygwin, http://www.cygwin.com/
2. Eclipse Mylyn Bugzilla extension
3. kdiff3 (git tool)
4. Firebug (javascript & .css debugging)

#### Accounts

1. Git Account
  * For access to the external git at ssh://trac@sapsailing.com/home/trac/git please send your SSH public key to Axel Uhl or Simon Marcel Pamies, requesting git access. Make sure to NOT generate the key using Putty. Putty keys don't work reliably under Linux and on Windows/Cygwin environments. Use ssh-keygen in a Cygwin or Linux or MacOS/X environment instead.
  * Register yourself as a Git user in the SAP-Git under: https://git.wdf.sap.corp:8080/
  * Ask the Git administrator (Axel Uhl) to get on the list of enabled committers
2. Bugzilla
  * Ask the Bugzilla administrator (Frank Mittag, Axel Uhl) to create a bugzilla account for you.
  * Bugzilla url: http://bugzilla.sapsailing.com/bugzilla/
3. Wiki
  * Send a request to Axel Uhl or Simon Marcel Pamies that includes the SHA1 hash of your desired password. Obtain such an SHA1 hash for your password here: http://www.sha1-online.com/.
4. Hudson
  * Request a Hudson user by sending e-mail to Axel Uhl, Frank Mittag or Simon Marcel Pamies.

#### Steps to build and run the Race Analysis Suite

1. Get the content of the git repository
  * Generate SSH Keys with "ssh-keygen -t rsa -C "" " command in Cygwin Terminal (Not with Putty!!!)
  * Clone the repository to your local file system from `ssh://[SAP-User]@git.wdf.sap.corp:29418/SAPSail/sapsailingcapture.git`  or `ssh://[user]@sapsailing.com/home/trac/git`  User "trac" has all public ssh keys.
2. Check out the 'master' branch from the git repository. The 'master' branch is the main development branch. Please check that you start your work on this branch.
3. Setup and configure Eclipse
  * Make absolutely sure to import CodeFormatter.xml (from $GIT_HOME/java) into your Eclipse preferences (Preferences->Java->Code Style->Formatter)
  * Install the Eclipse GWT-Plugin (now called Google Plugin for Eclipse)
  * Install the Google Android SDK from the same Google Plugin for Eclipse update site
  * In Eclipse go to "Window->Preferences->Java->Build Path->Classpath Variables" and create a new classpath variable called ``ANDROID_HOME``. Set its value to the install location of your Android SDK, e.g., ``c:\apps\android-sdk-windows`` or ``/usr/local/android-sdk-linux``.
  * Install GWT SDK and add the SDK in Eclipse (Preferences -> Google -> Web Toolkit -> Add...)
  * Install Eclipse debugger for GWT SuperDevMode
  * Install Eclipse eGit (optional)
  * Check that JDK 1.8 is available and has been set for compilation in Eclipse
  * Check that the both JDKs are available (Windows->Preferences->Java->Installed JREs)
  * Check that JDK 1.7 has been matched to JavaSE-1.7 and that JDK 1.8 has been matched to JavaSE-1.8 (...>Installed JREs>Execution Environments)
  * It is also possible to match the SAPJVM 7 or 8 to JavaSE-1.7 / JavaSE-1.8 (for profiling purposes)
  * Go to Windows->Preferences->Google->Errors/Warnings and set "Missing SDK" to "Ignore"
  * Import all Race Analysis projects from the `java/` subdirectory of the git main folder (make sure to import as a git project in eclipse)
  * Import all projects from the `mobile/` subdirectory of the git main folder; this in particular contains the race committee app projects
  * Set the Eclipse target platform to race-analysis-p2-remote.target (located in com.sap.sailing.targetplatform/definitions)
  * Wait until the target platform has been resolved completely
  * Rebuild all projects
4. Run the Race Analysis Suite
  * Start the MongoDB
  * Start the appropriate Eclipse launch configuration (e.g. 'Sailing Server (Proxy)') You´ll find this in the debug dropdown
  * Run "Security UI sdm" in the debug dropdown
  * Run "SailingGWT" in the debug dropdown
5. Import races within the Race Analysis Suite
  * Choose "Security UI sdm" in the upper left corner of the "Development Mode" Tab in Eclipse and open "...Login.html" in your browser
  * Default Login: user "admin", password "admin"
  * Choose "Sailing GWT" in the "Development Mode" Tab and open "...AdminConsole.html..." (It is normal that the first try fails. Reload the page after the first try)
  * For TracTrac Events: (Date 27.11.2012) Use Live URI tcp://10.18.22.156:4412, Stored URI tcp://10.18.22.156:4413, JSON URL  http://germanmaster.traclive.dk/events/event_20120905_erEuropean/jsonservice.php
  * Press List Races

#### Maven Setup
Copy the settings.xml **and** the toolchains.xml from the top-level git folder to your ~/.m2 directory. Adjust the proxy settings in settings.xml accordingly (suggested settings for corporate network inside). Set the paths inside of toolchains.xml to your JDKs depending on where you installed them (this is like setting the compiler for your IDE, but for Maven; This makes it possible to build with the same Maven configuration on every system). Make sure the mvn executable you installed above is in your path. Open a shell (preferrably a git bash or a cygwin bash), cd to the git workspace's root folder and issue "./configuration/buildAndUpdateProduct.sh build". This should build the software and run all the tests. If you want to avoid the tests being executed, use the -t option. If you only want to build one GWT permutation (Chrome/English), use the -b option. When inside the SAP VPN, add the -p option for proxy use. Run the build script without arguments to get usage hints.

#### Further hints
- Configure Eclipse to use Chrome or Firefox as the default browser
- Install the GWT Browser Plugin for the GWT Development mode <del>(Chrome or Firefox; as of this writing (2013-11-05), Firefox is the only platform where the plug-in runs stably)</del> (As of 2016-08-31 Firefox is the only browser supporting the GWT plugin, you have to download Firefox version 24 for it to work)

#### Additional steps required for Android projects

To ensure that all components of the Analysis Suite are working, you should also import all Android projects (mobile/) into your workspace. There are some additional requirements to enable the build process of these projects.

1. Add the Android Development Tools (ADT) plugin to your Eclipse IDE
  - In Eclipse click Help -> Install New Software -> Add and enter https://dl-ssl.google.com/android/eclipse/
  - Select the Developer Tools and install
  - After restarting Eclipse the "Welcome to Android Development" window should help you with installing the Android SDK
  - It is also possible to download the Android SDK separately from http://developer.android.com/sdk/index.html ("Use an existing IDE")
2. Setup the Android SDK
  * In Eclipse press Window -> Android SDK Manager
  * Ensure that everything of "Tools" is installed
  * Install everything of "Android 3.2 API 13"
  * Optional: it's a good idea to install the newest API Version
  * Install "Android Support Library" (Extras), "Google Play Services" (Extras) and "Google USB Driver" (Extras)
3. Import the Android projects into your workspace
  * Android projects can be found in the /mobile subdirectory

To deploy an Android project (for example com.sap.sailing.racecommittee.app) to a real device:

1. Plug-in the device
  * Development mode must be enabled on the device
  * For certain device/OS combinations additional device drivers are needed
  * You can check if the device is detected correctly by checking the "Devices" tab of the "DDMS" Eclipse perspective.
2. Start a run configuration of the project
3. Select your attached device in the device selection screen
4. The app should be started after deployment

#### Further hints

If you are working with a linux-system and you get the error message `error while loading shared libraries: libz.so.1: cannot open shared object file: No such file or directory` try to install  lib32z1 and lib32stdc++6.

####Steps to consider for using other modules
1. For Eclipse Build
   * MANIFEST.MF , add module names unter dependencies
   * *.gwt.xml , add `<inherits name="-modulename-.-gwt.xml file name-" />`
   * In DebugConfigurations => Classpaths, Add Sourcefolder where classes are you want to user from the module
2. For Maven Build
   * pom.xml , Add Dependency to used module ie.     
`<dependency>
<groupId>com.sap.sailing</groupId>
<artifactId>com.sap.sailing.domain.common</artifactId>
<version>1.0.0-SNAPSHOT</version>
<classifier>sources</classifier>
</dependency>`

#### Using Android Studio for Development

The Android Apps can be built in Android Studio or gradle command line. Android Studio is built on top of IntelliJ IDEA, so it is possible to use IntelliJ IDEA as well.

1. On the "Welcome Screen" choose "Import Project (Eclipse, ADT, Gradle, etc.)"
2. Navigate to the project root folder and select the "build.gradle" file (all used modules are defined in "settings.gradle")
    * it will download all necessary gradle runtime files
    * you will see floating messages at the top right
        * ignore "non-managed pom.xml file..."
        * choose "add root" from "Unregistered VCS root detected" (you can add this later, if you missed it)
3. Setup the Android SDK
    * in Android Studio press Tools -> Android -> SDK Manager
    * Install from section "Tools"
        * Android SDK Tools
        * Android SDK Platform-tools
        * Android SDK Build-tools 22.0.1 (latest version at the time of writing)
    * Install everything of "Android 5.1.1 (API 22)" (latest API at the time of writing)
        * the "System Images" are optional and only needed for the emulators, which can't be fully used because of the missing Google Play Services (needed for location detection in the wind fragment)
    * Install from section "Extras"
        * Android Support Repository
        * Google Repository
        * Google USB Driver (only on Windows)
4. To edit all (not only Android) modules, open the left window "1: Project" and switch the view from "Android" to "Project" (folder view)
5. At the top is a drop down, where you can switch the mobile projects (com.sap.sailing.*) and start with buttons right to it
    * Run (starts the app on a real device or emulator)
    * Debug (starts the app with an attached debugger)
    * Attach Debugger (useful, if the app is currently running and you want to start debugging against the correspond sources)

If git is not in the Path system environment variable, the gradle build will not work.

##### To enable missing git integration

1. navigate to VCS -> Enable Version Control Integration
2. choose git in the drop down
3. if everything is correct, you'll see the current branch at the bottom line

See [RaceCommittee App](/wiki/info/mobile/racecommittee-app) for more information regarding the mobile app.
