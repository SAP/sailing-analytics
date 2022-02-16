# OnBoarding Information

This document describes the onboarding process for a new team member (developer)

First of all, make sure you've looked at [http://www.amazon.de/Patterns-Elements-Reusable-Object-Oriented-Software/dp/0201633612](http://www.amazon.de/Patterns-Elements-Reusable-Object-Oriented-Software/dp/0201633612). That's a great book, and knowing at least some of it will help you a great deal finding your way around our solution.

### SAP Sailing Analytics Development Setup

#### Installations

1. Eclipse (Eclipse IDE for Eclipse Committers, version 4.15.0 ["2021-03"](https://www.eclipse.org/downloads/packages/release/2021-03/r/eclipse-ide-eclipse-committers)), [http://www.eclipse.org](http://www.eclipse.org)
2. Get the content of the git repository (see
Steps to build and run the Race Analysis Suite below)
3. Install the eclipse plugins (see Automatic Eclipse plugin installation below)
4. Git (e.g. Git for Windows v2.18), [http://git-scm.com](http://git-scm.com) / [https://git-for-windows.github.io](https://git-for-windows.github.io)
5. MongoDB (at least Release 4.4), download: [https://www.mongodb.com/](https://www.mongodb.com/)
6. RabbitMQ, download from [http://www.rabbitmq.com](http://www.rabbitmq.com). Requires Erlang to be installed. RabbitMQ installer will assist in installing Erlang. Some sources report that there may be trouble with latest versions of RabbitMQ. In some cases, McAffee seems to block the installation of the latest version on SAP hardware; in other cases connection problems to newest versions have been reported. We know that version 3.6.8 works well. [https://github.com/rabbitmq/rabbitmq-server/releases/tag/rabbitmq_v3_6_8](https://github.com/rabbitmq/rabbitmq-server/releases/tag/rabbitmq_v3_6_8) is the link.
7. JDK 1.8 (Java SE 8), [http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) --- Alternatively you can use the SAPJVM 1.8: Go to [http://sapjvm.wdf.sap.corp:1080/downloads](http://sapjvm.wdf.sap.corp:1080/downloads), select JVM 1.8, extract the downloaded .zip into desired location (e.g. C:\Program Files\Java), then Go to Window -> Preferences -> Java -> Installed JREs and add the VM.
8. Maven 3.1.1 (or higher), [http://maven.apache.org](http://maven.apache.org)
9. GWT SDK 2.9.0 ([http://www.gwtproject.org/download.html](http://www.gwtproject.org/download.html))
10. Standalone Android SDK (see section "Additional steps required for Android projects"). OPTIONALLY: You may additionally install Android Studio ([https://developer.android.com/tools/studio/index.html](https://developer.android.com/tools/studio/index.html)) or IntelliJ IDEA ([https://www.jetbrains.com/idea/download/](https://www.jetbrains.com/idea/download/)).

#### Automatic Eclipse plugin installation

The necessary Eclipse plugins described above can be automatically be installed into a newly unzipped version of [Eclipse IDE for Eclipse Committers "2020-06"](https://www.eclipse.org/downloads/packages/release/2020-06/r/eclipse-ide-eclipse-committers) by using the script "configuration/installPluginsForEclipse2020-06.sh". In addition, the script applies some updates to plugins packaged with Eclipse itself. To start the plugin installation, run the following command using your Eclipse installation directory as command line parameter for the script:

    ./installPluginsForEclipse2021-03.sh "/some/path/on/my/computer/eclipse"

Be aware that with this script it's not possible to update the plugins to newer versions. Instead you can install a new version by unpacking the base package and executing the script.

###### On Windows
You need a Git Bash or Cygwin shell to run the script. In addition you need to replace all backslashes with forward slashes.

###### On Mac OS:
- Before running the script, start eclipse and install **GWT Plugin** manually from repository [http://storage.googleapis.com/gwt-eclipse-plugin/v3/release](http://storage.googleapis.com/gwt-eclipse-plugin/v3/release)
- It's not sufficient to provide the path to the app, instead you need to get the path to the directory inside of the app package hosting the "eclipse" binary (.../Eclipse.app/Contents/MacOS).

Be aware hat the installation may take several minutes depending on your Internet connection. When the script finished running, please check that no errors occurred (the installation process only logs errors but doesn't fail).

The script will install the following plugins for your convenience:

* GWT Plugin ([https://github.com/gwt-plugins/gwt-eclipse-plugin](https://github.com/gwt-plugins/gwt-eclipse-plugin))
* GWT SDM Debug Bridge ([http://sdbg.github.io/](http://sdbg.github.io/))
* Easy Shell ([https://anb0s.github.io/EasyShell/](https://anb0s.github.io/EasyShell/))
* Memory Analyzer ([https://www.eclipse.org/mat/](https://www.eclipse.org/mat/))
* SAP JVM Profiler ([https://tools.hana.ondemand.com](https://tools.hana.ondemand.com))
* UMLet ([https://www.umlet.com/](https://www.umlet.com/))
* various updates to preinstalled plugins

#### Tuning the Eclipse Installation

Out of the box, two settings in Eclipse avoid a clean workspace. Go to Window - Preferences and change the following two settings:

* Set the "Missing GWT SDK" warning to "Ignore". See screenshot below.

![](ignore-missing-gwt-sdk-warning.png)

* Set the "Plug-in does not export all packages" warning to "Ignore". See screenshot below.

![](ignore-package-not-exported-warning.png)

#### Further optional but recommended installations

1. Cygwin, [http://www.cygwin.com/](http://www.cygwin.com/)
2. Eclipse Mylyn Bugzilla extension
3. kdiff3 (git tool)
4. Firebug (javascript & .css debugging, included in Firefox Developer Tools in newer versions of Firefox by default)

#### Accounts

1. Git Account
The primary Git repository for the project is hosted on sapsailing.com. It is mirrored on an hourly basis into SAP's internal Git/Gerrit repository, but branches from the external Git end up under the remote ``sapsailing.com`` in the internal repository, thus do not automatically merge into their branch counterparts. Conversely, commits pushed onto branches of the SAP-internal Gerrit will not by themselves end up on the external Git at sapsailing.com.
  * For access to the external git at ``ssh://trac@sapsailing.com/home/trac/git`` please send your SSH public key to Axel Uhl or Simon Marcel Pamies, requesting git access. Make sure to NOT generate the key using Putty. Putty keys don't work reliably under Linux and on Windows/Cygwin environments. Use ssh-keygen in a Cygwin or Linux or MacOS/X environment instead.
  * Alternatively, for access to the SAP-internal Git/Gerrit repository register yourself as a Git user in the SAP-Git under: [https://git.wdf.sap.corp:8080/](https://git.wdf.sap.corp:8080/); ask the Git administrator (Axel Uhl) to get on the list of enabled committers
2. Bugzilla
  * Create an account at https://bugzilla.sapsailing.com
  * Ask the Bugzilla administrator (axel.uhl@sap.com) to enable your account for editing bugs
  * Bugzilla url: [https://bugzilla.sapsailing.com](https://bugzilla.sapsailing.com)
3. Wiki
  * Send a request to Axel Uhl or Simon Marcel Pamies that includes the SHA1 hash of your desired password. Obtain such an SHA1 hash for your password here: [http://www.sha1-online.com/](http://www.sha1-online.com/).
4. Hudson
  * Request a [Hudson](https://hudson.sapsailing.com) user by sending e-mail to Axel Uhl or Simon Marcel Pamies.

#### Steps to build and run the Race Analysis Suite

1. Get the content of the git repository
  * Generate SSH Keys with "ssh-keygen -t rsa -C "" " command in Cygwin Terminal (Not with Putty!!!)
  * Clone the repository to your local file system from `ssh://[SAP-User]@git.wdf.sap.corp:29418/SAPSail/sapsailingcapture.git`  or `ssh://[user]@sapsailing.com/home/trac/git`  User "trac" has all public ssh keys.
  * Please note that when using one of the newer versions of Cygwin, your Cygwin home folder setting might differ from your Windows home folder. This will likely lead to problems when issuing certain commands. For troubleshooting, take a look at the following thread: [https://stackoverflow.com/questions/1494658/how-can-i-change-my-cygwin-home-folder-after-installation](https://stackoverflow.com/questions/1494658/how-can-i-change-my-cygwin-home-folder-after-installation)
2. Check out the 'master' branch from the git repository. The 'master' branch is the main development branch. Please check that you start your work on this branch.
2. Configure your local git repository
  * Execute the command `git config core.autocrlf false` in the git repository
  * Ensure that your git username and email is set properly: In case you are unsure, use the commands `git config user.name "My Name"` and `git config user.email my.email@sap.com` in the git repository.
3. Setup and configure Eclipse
  * Make absolutely sure to import CodeFormatter.xml (from $GIT_HOME/java) into your Eclipse preferences (Preferences->Java->Code Style->Formatter)
  * It is also strongly recommended to import CodeFormatter\_JavaScript.xml (from $GIT_HOME/java) into your Eclipse preferences (Preferences->JavaScript->Code Style->Formatter) to ensure correct formatting of JavaScript Native Interface (JSNI) implementations.
  * Install the required plugins using the script provided above. Further configuration steps depend on the plugins being installed successfully.
  * In Eclipse go to "Window->Preferences->Java->Build Path->Classpath Variables" and create a new classpath variable called ``ANDROID_HOME``. Set its value to the install location of your Android SDK, e.g., ``c:\apps\android-sdk-windows`` or ``/usr/local/android-sdk-linux``.
  * Install GWT SDK and add the SDK in Eclipse (Preferences -> GWT -> GWT Settings -> Add...)
  * In "Window->Preferences->GWT->Errors/Warnings, set "Missing SDK" to "Ignore" (If not done earlier, see: Tuning the Eclipse Installation)
  * In "Window->Preferences->General->Editors->TextEditors" check Insert Spaces for Tabs
  * In "Window->Preferences->Web->HTML Files->Editor" indent using Spaces
  * In "Window->Preferences->General->Content Types" select on the right side CSS, now add in the lower file association list *.gss to get limited syntax highlighting and content assist in GSS files
  * In "Window->Preferences->XML(Wild Web Developer)->Validation & Resolution->Enable Validation" Disable the Checkbox
  * Install Eclipse debugger for GWT SuperDevMode
  * Install Eclipse eGit (optional)
  * Check that JDK 1.8 is available and has been set for compilation in Eclipse
  * Check that JDK 1.8 has been matched to JavaSE-1.8 (...>Installed JREs>Execution Environments)
  * It is also possible to match the SAPJVM 8 to JavaSE-1.8 (for profiling purposes)
  * Import all Race Analysis projects from the `java/` subdirectory of the git main folder (make sure to import via the wizard <del>"Git->Projects from Git"</del> "Import->General->Projects from Folder or Archive" in Eclipse, and additionally make sure to scan for nested projects!)
  * Import all projects from the `mobile/` subdirectory of the git main folder; this in particular contains the race committee app projects
  * Set the Eclipse target platform to race-analysis-p2-remote.target (located in com.sap.sailing.targetplatform/definitions)
  * Wait until the target platform has been resolved completely
  * Rebuild all projects
4. On clear workspace additional steps should be performed once:
  1. Run "GWT Dashboards SDM" launch configuration. After successful start, launch configuration can be stopped.
  2. Run "GWT Security SDM" launch configuration. After successful start, launch configuration can be stopped.
  3. Run "GWT xdStorage Sample SDM" launch configuration. After successful start, launch configuration can be stopped.
5. Run the Race Analysis Suite
  * Start the MongoDB (cd /somePathTo MongoDB/mongodb/bin; rm c:/data/SAP/sailing/mongodb/mongod.lock; ./mongod --dbpath c:/data/SAP/sailing/mongodb)  
  * Start the appropriate Eclipse launch configuration (e.g. 'Sailing Server (no Proxy)') You´ll find this in the debug dropdown
  * <del>Run "Security UI sdm" in the debug dropdown</del> (obsolete)
  * Run "GWT Sailing SDM" in the debug dropdown
6. Import races within the Race Analysis Suite
  * <del>Choose "Security UI sdm" in the upper left corner of the "Development Mode" Tab in Eclipse and open "...Login.html" in your browser</del> (obsolete)
  * Choose "GWT Sailing SDM" in the "Development Mode" Tab and open "...AdminConsole.html..." (It is normal that the first try fails. Reload the page after the first try)
  * Default Login: user "admin", password "admin"
  * In the list on the left, click on "Connectors"
  * For TracTrac Events: In the "TracTrac Connections" Form, fill in the JSON URL [http://germanmaster.traclive.dk/events/event_20120905_erEuropean/jsonservice.php](http://germanmaster.traclive.dk/events/event_20120905_erEuropean/jsonservice.php)(all other required information will be filled in automatically)
  * Press "List Races"

#### Git repository configuration essentials
The project has some configuration of line endings for specific file types in ".gitattributes". To make this work as intended, you need to ensure that the git attribute "core.autocrlf" is set to "false". This can be done by navigating to your local repository in a Bash/Git Bash/Cygwin instance and executing the command `git config core.autocrlf false`.

If you are first time git user, don't forget to specify your user metadata. Use the commands `git config user.name "My Name"` and `git config user.email my.email@sap.com` to tell git your name and email address.

Depending on the location of your local repository, it's filepaths might be too long for the default settings to handle. Excecute the command `git config --system core.longpaths true` to enable your system wide git installation to handle long file paths.

#### Maven Setup
Copy the settings.xml **and** the toolchains.xml from the top-level git folder to your ~/.m2 directory. Adjust the proxy settings in settings.xml accordingly (suggested settings for corporate network inside). Set the paths inside of toolchains.xml to your JDKs depending on where you installed them (this is like setting the compiler for your IDE, but for Maven; This makes it possible to build with the same Maven configuration on every system). Make sure the mvn executable you installed above is in your path. Open a shell (preferrably a git bash or a cygwin bash), cd to the git workspace's root folder and issue "./configuration/buildAndUpdateProduct.sh build". This should build the software and run all the tests. If you want to avoid the tests being executed, use the -t option. If you only want to build one GWT permutation (Chrome/English), use the -b option. When inside the SAP VPN, add the -p option for proxy use. Run the build script without arguments to get usage hints.

#### Further hints
- Configure Eclipse to use Chrome or Firefox as the default browser
- Install the GWT Browser Plugin for the GWT Development mode <del>(Chrome or Firefox; as of this writing (2013-11-05), Firefox is the only platform where the plug-in runs stably)</del> (As of 2016-08-31 Firefox is the only browser supporting the GWT plugin, you have to download Firefox version 24 for it to work)
- Use SAP JVM Profiler. If you used the script above and installed the SAPJVM instead of the jdk, you can now open the profiling perspective by clicking on Window -> Perspective -> Open Perspective -> Profiling)
- Make sure you run Eclipse with a JRE containing javax/xml/bind, otherwise you will not be able to use the SDM. See [https://github.com/sdbg/sdbg/pull/162](https://github.com/sdbg/sdbg/pull/162). To start Eclipse with Java 8 add the following two lines above the `-vmargs` in your eclipse.ini:    
`-vm`    
`<path/to/sapjvm8/jre/bin/java>`    

#### Git usage troubleshooting

There are some inconsistencies regarding line endings (unix vs windows) in our git repository. There is a configuration named ".gitattributes" committed to the root folder of the repository that helps to prevent inconsistencies of line endings when committing files. Files that existed before are partially using unix (LF) or windows (CRLF) line endings. When committing files, git will ensure unix line endings for e.g. *.java files. This can lead to big diffs that hurt when trying to merge/diff.

When merging branches that potentially have conflicts regarding line endings, you can specifically parameterize the git command line to not produce a big bunch of conflicts. Using the parameter `-Xignore-space-at-eol` while doing a merge will drastically reduce such conflicts. Using this, the commandline to merge "master" into your current branch is `git merge -Xignore-space-at-eol master`.

In cases where code was reformatted, you can also ignore all whitespace changes by using the parameter `-Xignore-space-change`.

When doing a diff, you can also use the parameters `--ignore-space-at-eol` and `--ignore-space-change`.

When doing a merge in Eclipse, you can tell the merge editor to do a similar thing by right clicking and selecting "Ignore White Space". Other merge/diff tools also provide such a functionality.

When a file has "wrong line endings" (line endings are different to what is configured in ".gitattributes" file) and you touch those files without changing the contents, git will potentially show that these files are changed. To get rid of those phantom changes, you can do a "git checkout HEAD path/toFile/in/repository".

#### Further hints

If you are working with a linux-system and you get the error message `error while loading shared libraries: libz.so.1: cannot open shared object file: No such file or directory` try to install  lib32z1 and lib32stdc++6.

#### Steps to consider for using other modules
1. For Eclipse Build
   * MANIFEST.MF , add module names unter dependencies
   * *.gwt.xml , add `<inherits name="-modulename-.-gwt.xml file name-" />`
   * In DebugConfigurations => Classpaths, Add Sourcefolder where classes are you want to user from the module
2. For Maven Build
   * pom.xml , Add Dependency to used module ie.    
`<dependency>`    
`<groupId>com.sap.sailing</groupId>`    
`<artifactId>com.sap.sailing.domain.common</artifactId>`    
`<version>1.0.0-SNAPSHOT</version>`    
`<classifier>sources</classifier>`    
`</dependency>`

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
    * Install from section "Tools" (hint: carefull not to update
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
