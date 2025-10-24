# Building, Deploying, Stopping and Starting Server Instances

[[_TOC_]]

## Running a Build

Builds are generally executed in either of the following two ways:

 * Check out a branch of your liking from git and run <gitroot>/configuration/buildAndUpdateProduct.sh build (check the various options of this script by invoking it with no arguments). The build results including the p2 product repository are then located in the git workspace.

 * Ensure that [Hudson](http://hudson.sapsailing.com) has a job for your branch; simply push the branch to the central git at sapsailing.com and let Hudson do the job. This is mostly good for knowing whether everything builds and tests ok. Only if you push the special "release" tag, Hudson will build a release and upload it to [releases.sapsailing.com](http://releases.sapsailing.com).

## Deploying Build Results

When the build has been run using the `buildAndUpdateProduct.sh` script, the build results in the git workspace can be deployed to a server environment under `~/servers/<servername>` using the command

    <gitroot>/configuration/buildAndUpdateProduct.sh -s <servername> install

This will copy all necessary files, in particular the p2 product, to the server directory, including the start and stop scripts.

Again, check out the script's options for more and other possibilities including a remote deployment option and a hot deploy of individual bundles into a running server environment.

Deploying build results is generally also possible with a Hudson build, but it is not recommended because a user would need to log in to the Hudson server, know where which build workspace is located and then apply the deployment script there.

## Working with Releases

Particularly when starting an EC2 instance, it is helpful to be able to do that using a well-known release of the product. When an EC2 instance starts, it has a version of the product built into the image and its disk snapshots from which the instance got initialized. This, however, is usually not up to date. To refresh it, you could run a build from a specific git commit, or you could install a **release** previously assembled using the `release` option of the `buildAndUpdateProduct.sh` script as follows:

    <gitroot>/configuration/buildAndUpdateProduct.sh build
    <gitroot>/configuration/buildAndUpdateProduct.sh -w trac@sapsailing.com -n <release-name> release

This will ask you for a comment about the release which goes into the release notes text file that accompanies the release. The build results are packed up into a .tar.gz file which is then uploaded to [releases.sapsailing.com](http://releases.sapsailing.com), using the name optionally provided using the -n parameter with the `release` action, or---as a default---the current timestamp for the release name.

A release can be downloaded and installed to a server by changing to the server's directory, e.g., `~/servers/server` or whatever the sub-directory of the server installation is, and there executing the `refreshInstance.sh` script with the parameter `install-release <release-name>`. Afterwards, starting the instance works as after a local build.

A sample session could look like this:

<pre>
$ ssh sailing@34.250.136.229
$ cd servers/ubilabs-test
$ ./refreshInstance.sh install-release build-201712210442
$ ./stop; ./start
</pre>

Instead of building a release yourself you can let the build server to the job. There is a job that looks out for a git tag named `release`. If a new revision is found then an automatic release build is being executed. The result of that is persisted to http://releases.sapsailing.com/. You can create that tag as follows. Make sure that you're on the current main branch before executing the following commands.

<pre>
$ git tag -f release
$ git push -f origin release:release
</pre>

Give the build server some time (20-30 Minutes) until it will have the release ready.

### Example with Hudson
Deploying jobs from hudson to `releases.sapsailing.com` is quite simple.

- login as `hudson@<build server>`
- go to `/home/hudson/repo/jobs/{jobname}/workspace`
- execute `./configuration/buildAndUpdateProduct.sh -n build -w trac@sapsailing.com release` for triggering the upload

## Working with Environments

There exist a number of preconfigured environment configurations at [releases.sapsailing.com/environments](http://releases.sapsailing.com/environments). Such an environment can be automagically applied to your instance by changing to the servers directory and then executing the `refreshInstance.sh` script with the parameter `install-env <environment-name>`. This will update your env.sh. Make sure to afterwards restart your server.

## Starting, Administrating and Stopping a Java Instance

The product runs as a Java instance consisting largely of an Equinox OSGi server that load and runs various OSGi bundles constituting the product and that contains an embedded Jetty web server. It connects to a database, may serve as a master for replication through a messaging server, may be launched as a replica of some other master instance, may receive wind data in _Expedition_ format on a specific port and can listen for telnet requests to administrate the OSGi server on a specific port. These and other properties are usually configured in a file called `env.sh` which has to be located in the server directory, e.g., in `~/servers/server`, next to the `start` script. When the `start` script is executed, it first sources the env.sh file which sets the various properties which are then passed to the actual Java process, usually in the form of system properties.

After successfully having started a Java instance, it can be administrated through a telnet connection. The port on which the OSGi console listens for incoming connections can be configured in the `env.sh` file. Usually it defaults to port `14888`. Therefore, a `telnet localhost 14888` connects you to the OSGi console where an `ss` command will show you all bundles loaded. Once logged on to the OSGi console, a `disconnect` command will disconnect the telnet session from the OSGi console.

An `exit` command will **terminate** the Java instance after a confirmation. This will obviously stop all services provided by the instance, including all static and dynamic contents served by its web server. You should only trigger the `exit` command if you really know what you are doing!

Stopping a running server has---for your convenience---been wrapped into the `stop` script usually located in the server's directory. Simply executing it will use a telnet connection to the server's OSGi port and trigger an `exit` command automatically. See above for the "know what you're doing" part...

## Automatic Java Instance Start-Up After Boot

When firing up an EC2 instance it can be convenient to not having to log on to have the EC2 instance run a Java instance automatically after it has completed its boot process. This is possible using so-called _user data_. The process of firing up an instance that either builds a certain git commit, installs and starts it after server boot or that downloads and installs a release and starts it is explained [here](https://wiki.sapsailing.com/wiki/info/landscape/amazon-ec2#amazon-ec2-for-sap-sailing-analytics_howto).

## App Build Process for Android

(Also look at older versions of this document in Git as there has been a long history of different approaches for how SAP builds and releases our apps. Meanwhile, the "Sail Insight" app is no longer shipped from our primary Git repository but instead, a new version of it has been developed, using React Native and TypeScript as the technology. This app was originally shipped by d-labs, later through our partner, Marcus Baur and the developer at the time (Adrian Aioanei), before in 2023 we moved this to d-labs again.)

### Preparing the Azure Build Pipeline with Hyperspace

As of January 2024, the xMake build infrastructure will be terminated. Projects have been requested to migrate their builds to Azure Pipelines. The creation of those pipelines is managed by an SAP-internal tool called [Hyperspace](https://hyperspace.tools.sap/). Pipelines are organized into "Groups" within Hyperspace. Those groups may share, e.g., secrets stored in a common Hashicorp Vault. But secrets may as well be managed separately per pipeline. As a prerequisite, all code must be made available in a Github repository that is owned by a Github "organization," not a personal user account. For this purpose, the SAP Sailing Analytics Git repository is now also available at [https://github.tools.sap/SAP-Sailing-Analytics/sapsailing](https://github.com/SAP/sailing-analytics). Furthermore, in order to use the Hyperspace templates for mobile projects, such as Android, the group responsible for Hyperspace needs to actively enable your user account for the use of those templates. Find more Hyperspace onboarding documentation [here](https://pages.github.tools.sap/SAPMobile/Documentation/GettingStarted/hyperspace/). The set-up with all necessary steps is explained [here](https://pages.github.tools.sap/SAPMobile/Documentation/). In particular, the set-up for building mobile Android apps is then detailed further [here](https://pages.github.tools.sap/SAPMobile/Documentation/Pipelines/android/). Note that Hyperspace is accessible only from within the SAP network / VPN / Citrix Workplace; other elements of this, such as Github and Azure Pipelines can also be accessed from anywhere as long as you have your client certificate installed.

When working with Hyperspace to prepare the creation of the actual Azure Pipeline, various credentials have to be provided or obtained, such as for the [Github Enterprise repository](https://github.tools.sap), access to the "MoMa" app metadata management tool, and the [Microsoft AppCenter](https://appcenter.ms/) where a new organization must be created into which the apps get registered, configuring the optional Checkmarx and Black Duck code scanners and connecting to the Hashicorp vault for secret management. The result is a [pipeline](https://hyperspace.tools.sap/pipelines/29049/vault) configured in a pipeline group, in our case named `SAP-Sailing-Analytics`. The pipeline created by Hyperspace can then be found in [Azure DevOps](https://dev.azure.com/hyperspace-mobile/SAP-Sailing-Analytics/_build?definitionId=496):
* Pipeline Name: ``sapsailing-android-apps``
* Pipeline Description: Building the SAP Sailing Analytics Android Apps
* Pipeline Group: ``SAP-Sailing-Analytics``

The pipeline configuration is produced as a [Github pull request](https://github.tools.sap/SAP-Sailing-Analytics/sapsailing/pull/1) for the Git repository used for Hyperspace onboarding. In particular, it contains a "Fastlane" configuration under ``fastlane/Fastfile`` and the actual pipeline configuration in ``azure-pipelines/mail.yml``. The fastlane config in ``fastlane/Fastfile`` we could slighly adjust and simplify because we only build ``.apk`` files, no ``.aab`` libraries. Furthermore, we need adjustments for the location of the ``gradle.properties`` file into which a git-based technical version identifier shall be generated during the build process. Later more on the details of this.

In the ``azure-pipelines/main.yml`` file under the ``trigger:`` property we configure which Git branches and/or tags shall trigger a pipeline execution. The core part then is defined by a reference to the pipeline's template, and specific adjustments can then be made to the template pipeline in the ``extends:`` section. The ``isRelease`` boolean property controls whether the build shall be a release build that triggers code signing, in particular. The ``productiveBranchName`` property controls for buils of which branch all scanning and test tasks shall be performed. We required special adjustments of the ``jdkVersion`` property to ``11`` and the ``androidVersion`` to ``33`` for our Gradle app build to work. Furthermore, the MoMa Assembly ID must be configured in the ``momaAssemblyId`` property. Each app folder now has its own ``files2sign.json`` file in its app folder under ``mobile/`` because this allows us to limit the build output to files under the app's own folder.

Since we want to build and release two apps out of one Git repo and ideally with one pipeline, we work with Git branches, one per app that we want to release, as follows:

- ``release-buoy-pinger-app`` to release the SAP Sailing Buoy Pinger app with MoMa assembly ID 188
- ``release-race-manager-app`` to release the SAP Sailing Race Manager app with MoMa assembly ID 189

Both these branches derive from the branch ``hyperspace`` which is more like a proxy branch as its original changes with the pipeline / fastlane definition and a few changes to the top-level README and .gitignore files have now been merged fully into the main branch where they have no averse effects. However, pushing onto the ``hyperspace`` branch will trigger the Azure DevOps pipeline for a full build including "tests" and "scans." However, since we don't have any app-specific UI tests and instead focus on unit tests for the logic shared also with the back-end and GWT UI ("common" and "shared" bundle projects), we "simulate" successful tests to satisfy the build pipeline's requirement for tests to be present. See files ``dummy/TEST-dummy.xml`` and ``dummy/jacoco/jacocoTestReport.xml``. This lets the "quality" stage of the Azure Pipeline pass without complaints. The two app-specific branches each make four important adjustments compared to the ``hyperspace``/``master`` branch:

- The ``isRelease`` property is set to ``true`` for both app release branches to force the signing / releasing process
- The ``buildOutputPath`` is set to ``mobile/{app-directory}`` for the respective app to limit the number of files staged (artifacts staged are the unsigned .apk file, the files2sign.json and the zipped archive of all sources that went into the app)
- The MoMa Assembly ID is set in the ``momaAssemblyId`` property
- In ``fastlane/Fastfile`` the path to the ``gradle.properties`` file into which to patch the version code update is adjusted to the respective app folder, e.g., ``mobile/com.sap.sailing.android.buoy.positioning.app/gradle.properties``

After at most 90 days, the vault secret expire. Then, under [https://hyperspace.tools.sap/pipelines/29049/vault](https://hyperspace.tools.sap/pipelines/29049/vault) we can click the "Refresh SecretID" button. The build pipeline supposedly fetches the new secret automatically afterwards, which should then be good to go for another 90 days. Note: when your build pipeline failed for the lack of a valid secret ID, with the error message reading something like

```
Couldn't fetch secret at 'piper/PIPELINE-GROUP-5565/PIPELINE-29049/cumulus' - Error making API request
```

see also [here](https://pages.github.tools.sap/SAPMobile/Documentation/Support/faq/#my-pipeline-fails-with-invalid-secret-id-or-invalid-role-id). 

Our points of contact for the Hyperspace / Azure Pipeline migration are Marc Bormeth (marc.bormeth@sap.com), Maurice Breit (maurice.breit@sap.com) and Philipp Resch (philipp.resch@sap.com).

### Running a Build / Release / Promote Cycle

To release the apps, go to the MoMa records of the two apps ([https://moma.mo.sap.corp/#/apps/124](https://moma.mo.sap.corp/#/apps/124) and [https://moma.mo.sap.corp/#/apps/123](https://moma.mo.sap.corp/#/apps/123)) and from there into the Assembly-Data and press the "Add Release" button. Make sure the "Build System" is set to "free - Hyperspace Azure Pipelines". Then run the ``configuration/releaseAndroidApps.sh`` script, like this:

```
        ./configuration/releaseAndroidApps.sh
```

Additional options:

```
    -m Disable upgrading the versionCode and versionName
    -g Disable the final git push operation the release branches
    -r The git remote; defaults to origin
```

It starts with checking out the ``hyperspace`` branch, then increments the minor version number by one in all relevant files, in particular the ``files2sign.json`` files and the ``gradle.properties`` files in the app directories. It then commits these changes to the ``hyperspace`` branch and checks out each release branch one after another, merges the ``hyperspace`` branch with the version number updates and all other functional changes into the release branch and pushes it. This triggers the release build pipeline execution including code signing in Azure DevOps. The pipeline executions including their logs can be observed [here](https://dev.azure.com/hyperspace-mobile/SAP-Sailing-Analytics/_build?definitionId=496).

When the release pipelines ran successfully, they will have updated the MoMa assembly metadata for the respective apps, including the version numbers and the staging repositories used. Furthermore, a "Build Release Pipeline Project Link" is generated, such as [https://gkemobilepipelines.jaas-gcp.cloud.sap.corp/job/NAAS%20-%20Mobile%20Freestyle/job/com.sap.sailing.android.buoy.positioning.app-android/](https://gkemobilepipelines.jaas-gcp.cloud.sap.corp/job/NAAS%20-%20Mobile%20Freestyle/job/com.sap.sailing.android.buoy.positioning.app-android/) for the SAP Sailing Buoy Pinger app, and [https://gkemobilepipelines.jaas-gcp.cloud.sap.corp/job/NAAS%20-%20Mobile%20Freestyle/job/com.sap.sailing.racecommittee.app-android/](https://gkemobilepipelines.jaas-gcp.cloud.sap.corp/job/NAAS%20-%20Mobile%20Freestyle/job/com.sap.sailing.racecommittee.app-android/) for the SAP Sailing Race Manager app. Before using these pipelines, go to the MoMa Google Play Metadata section for each app you'd like to release and click on "Add Release" there, too. You will have to make a few adjustments to some fields, such as that you're only releasing a "Patch" with specific release notes which you then have to submit to naming@sap.com for approval before you can send them to LXLabs using the respective button on the Google Play Metadata page in MoMa.

Only once you have all approvals in place, use the "Build Release Pipeline Project Link" for the app you'd like to release and run a build there. Note that these builds have interactive steps that pause and ask you to confirm the promotion to the Play Store. Your released app(s) should then show in the Google Play Store after a few hours.