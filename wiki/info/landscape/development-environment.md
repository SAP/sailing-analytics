# Development Environment

[[_TOC_]]

Here, we describe the process for doing simple standard development for the Sailing Analytics project, with a focus on how we handle Git w.r.t. branches, Bugzilla, Hudson-CI, and Github Actions. Other development scenarios you'll find described in more depth [[here|wiki/info/landscape/typical-development-scenarios]].

## Git, Issues (formerly Bugzilla), and Our Branches
Being an Eclipse Foundation project (Eclipse Azimuth Sailing Analytics), the main Git repository lives at [https://github.com/eclipse-sailing-analytics/sailing-analytics](https://github.com/eclipse-sailing-analytics/sailing-analytics). A "downstream" repository with SAP specifics exists at [https://github.com/SAP/sailing-analytics](https://github.com/SAP/sailing-analytics). Its ``main`` branch is also mirrored to ssh://trac@sapsailing.com/home/trac/git periodically and powers our Wiki at [https://wiki.sapsailing.com](https://wiki.sapsailing.com).

Changes should usually happen in the Eclipse upstream repository. They can then be pulled by the downstream repo on a regular basis. Should changes be truly SAP-specific, they may happen in the downstream repository only.

Issue tracking happens primarily in the upstream repositry as [Github Issues](https://github.com/eclipse-sailing-analytics/sailing-analytics/issues). (Historic note: We used Bugzilla for issue tracking before we migrated to Github. The latest issue natively created there was number 6248. The old Bugzilla content is still [available read-only](https://old-bugzilla.sapsailing.com) for the time being but may disappear in the future. Migrated issues carry a link to the original Bugzilla item. For now there is an automatic redirect for https://bugzilla.sapsailing.com which points to the migrated Github issues.) 

Small, minor, obvious and non-disruptive developments are usually carried out immediately on our ``main`` branch.

Everything else should follow the pattern
- create Github issue (e.g., issue #12345)
- create branch for issue ``bug12345``, typically branching from latest ``main`` tip
- create Jenkins job for branch using ``configuration/createJenkinsJobForIssue.sh 12345``
- make your changes on your branch and commit and push regularly
- pushing triggers the [release workflow](https://github.com/eclipse-sailing-analytics/sailing-analytics/actions/workflows/release.yml) which runs a build with tests
- when the workflow has finished, it triggers your Jenkins job which collects the [build and test results](https://ci.eclipse.org/sailing-analytics)
- be verbose and document your changes, progress, hold-ups and problems on your Github issue
- when build is "green," create a Pull Request (PR) and assign reviewers as appropriate
- after your branch has been merged into ``main``, delete your Jenkins build job for your branch, comment about the merge on the issue and close the issue
- the ``main`` branch will then build a new release that you can roll out into the production landscape
- in case of changes to i18n-related message properties files, merge ``main`` into ``translation`` and push ``translation`` to the downstream repository, which triggers the translation process; the completed translations will arrive as pushes to the ``translations`` branch, triggering another ``release`` workflow, and---if successful---an automated push to upstream ``translation`` with the corresponding build/release process happens, based on the [translation Hudson job](https://hudson.sapsailing.com/job/translation/configure)'s and upstream [translation Jenkins job](https://ci.eclipse.org/sailing-analytics/view/Active%20Jobs/job/translation/) special logic
- a successful ``main`` build (still on Java 8) will lead to an automatic merge into one or more branches for newer Java releases (such as ``docker-25``) with the corresponding build/release process

Be eager to equip your features and functions with tests. There should be enough examples to learn from. For UI testing, use Selenium (see the ``java/com.sap.sailing.selenium.test`` project).

### Exceptionally Building Without Running Tests, More/Fewer CPUs, and With Release
Ideally, the build should be run including the test cases. However, for exceptional cases you can trigger a build using the ``release`` workflow in Github Actions manually on the SAP downstream repo and can choose to ignore tests, change the number of CPUs to use for the build, and run the build with an OSGi target platform built according to the specifications of the branch you're building from.

Furthermore, if you push your branch, say ``bug12345`` to ``releases/bug12345`` then the Github Actions build triggered by the push will also build and publish a release (see [https://github.com/eclipse-sailing-analytics/sailing-analytics/releases](https://github.com/eclipse-sailing-analytics/sailing-analytics/releases) for upstream and [https://github.com/SAP/sailing-analytics/releases](https://github.com/SAP/sailing-analytics/releases) for downstream releases). You can use such as release, e.g., to deploy it to a staging server such as [https://dev.sapsailing.com](https://dev.sapsailing.com) or roll it out to a production landscape. Note, that ``sapsailing.com`` currently uses downstream releases.

## Eclipse Setup, Required Plug-Ins
The Eclipse setup is explained in our [[Onboarding|wiki/howto/onboarding]] description.

## Maven Build and Tests
We recommend using a local Maven-based build only if you try to understand or reproduce issues with the Github Actions build. In most other cases you should be fine using Eclipse with its local build/run/debug cycle.

If you still feel you want to run a local Maven build, make sure again (see also [[Onboarding|wiki/howto/onboarding]]) to get your ``maven-settings.xml`` and ``toolchains.xml`` files in ``~/.m2`` in good shape first.

We have a top-level Maven pom.xml configuration file in the root folder of our Git workspace. It delegates to the pom.xml file in the java/ folder where all the bundle projects are defined. We rely on the Tycho Maven plug-in to build our OSGi bundles, also known as the "manifest-first approach." The key idea is to mainly develop using Eclipse means, including its OSGi manifest editing capabilities, and keep the Maven infrastructure as simple as possible, deriving component dependencies from the OSGi manifests. See the various pom.xml files in the projects to see the project-specific settings. By and large, a pom.xml file for a bundle needs to have the bundle name and version defined (we currently have most bundles at version 1.0.0.qualifier in the manifest or 1.0.0.SNAPSHOT in Maven), and whether the bundle is a test or non-test bundle, expressed as the packaging type which here can be one of eclipse-plugin or ecplise-test-plugin.

Test plugins automatically have their tests executed during a Maven build unless the command-line option -Dmaven.test.skip=true argument is specified. It is generally a good idea to launch the Maven command using the -fae option which asks Maven to continue until the end, even if errors or failures occurred on the way, failing at the end if any failures occurred. This can save numerous round trips and is useful in case of known and temporarily acceptable test failures.

The Maven plug-in for the GWT compilation doesn't reliably perform a dependency check. It is therefore recommended to remove all contents of the java/com.sap.sailing.gwt.ui/com.sap.sailing.* folders (basically, all GWT compiler output) before launching the Maven build. A good command line for the Maven build from the java/ subdirectory in your local environment when outside the SAP VPN is this:

    buildAndUpdateProduct.sh build

which basically does something like

    rm -rf com.sap.sailing.gwt.ui/com.sap.sailing.*; mvn -fae -P debug.without-proxy clean install 2>&1 | tee log

Inside the SAP VPN you may want to use a different profile which accounts for the proxies that have to be used:

    buildAndUpdateProduct.sh -p build

The buildAndUpdateProduct.sh script can be found in the top-level configuration/ directory in git. It has been used successfully in Linux and Cygwin environments.

When building on sapsailing.com you should stick with the buildAndUpdateProduct.sh script. It makes a lot of settings that are necessary, such as specifying the settings.xml file to use for the Maven build. For the Selenium tests to succeed you have to make sure the DISPLAY environment variable is set to ":2.0" to send test browsers to a VNC display. Should the GWT build fail because it cannot open enough files, ensure the "ulimit -n" output is at least 4096 to enable the GWT compiler to assemble the resource sets which consist of many files that all need to be opened concurrently. Currently, the maximum value for "ulimit -n" is configured in /etc/security/limits.conf and is set to 16384. This specified the maximum amount to which a user's shell can set this value. The ~trac/.bash_profile contains a "ulimit -n 4096" command, but when running "screen" the shells usually are no login shells. You need to make sure you run ~trac/.bash_profile in the build shell to set the limit of open files to at least 4096. Then issue the respective buildAndUpdateProduct.sh command line.

All these build lines also creates a log file with all error messages, just in case the screen buffer is not sufficient to hold all scrolling error messages.

When you're done with your local Maven build, finally run "mvn clean" to clean up the artifacts produced by the Maven build. Without this, remnants and outputs from the Maven build may collide with the local Eclipse build, such as JAR files that ended up in projects' ``bin/`` folders.

## Plotting test results with the Measurement Plugin

By default the duration of each test is published and can be viewed in comparison with older builds. It is possible to publish other values using the Measurement Plugin, which reads them out of a `MeasurementXMLFile`. 

```
MeasurementXMLFile performanceReport = new MeasurementXMLFile("TEST-" + getClass().getSimpleName() + ".xml", getClass().getSimpleName(), getClass().getName());
MeasurementCase performanceReportCase = performanceReport.addCase(getClass().getSimpleName());
performanceReportCase.addMeasurement(new Measurement("MeasurementName", measurementValue));
```
