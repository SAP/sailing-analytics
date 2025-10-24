# Continuous Integration with Hudson/Jenkins and Github Actions

## Access

Our default Hudson runs on https://hudson.sapsailing.com. If you need access, please contact axel.uhl@sap.com or petr.janacek@sap.com. Usernames typically are {firstname}{lastname}, all lowercase.

Our use of a CI tool such as Hudson (could also be Jenkins, see [[below|#in-case-you-d-like-to-set-up-your-own-hudson-jenkins]]) is special in so far as the actual build is not run under its control. Instead, we use it only as an aggregator for build logs, test results and measurements collected during the builds. While historically we _did_ use Hudson to run our builds, also in conjunction with a fleet of AWS build slaves that allowed us to parallelize builds and configure hardware resources for it as needed, with the move to Github we also migrated our build to Github Actions, defining the [release](https://github.com/SAP/sailing-analytics/actions/workflows/release.yml) workflow which by and large takes over the heavy lifting of our build environment.

It turns out, though, that Github Actions is not particularly good at managing build logs that exceed tens of thousands of lines (like ours), and it is not good at managing test results and measurements, or presenting and preserving Selenium-provided screenshots recorded from headless browsers used for UI testing. That's why we decided to keep a more "classic" CI around, but instead of using it for actually _running_ the builds, we would instead only forward the build outputs produced by Github Actions to the CI where it then gets evaluated, stored, evaluated statistically and presented in an easy-to-access way.

The key to this type of integration is that at the end of the ``release`` workflow a Hudson job is triggered through an HTTP request made by the workflow. The scripts which are then used by the Hudson job to obtain the build results and if needed copy a Github release to https://releases.sapsailing.com can be found under [configuration/github-download-workflow-artifacts.sh](https://github.com/SAP/sailing-analytics/blob/main/configuration/github-download-workflow-artifacts.sh) and [configuration/github-copy-release-to-sapsailing-com.sh](https://github.com/SAP/sailing-analytics/blob/main/configuration/github-copy-release-to-sapsailing-com.sh), respectively.

Access to the Github repository is granted to the Hudson jobs through a Github personal access token (PAT) to an account with read-only permissions to the repository.

## Build Jobs

There are a number of standard build jobs:
* [SAPSailingAnalytics-master](https://hudson.sapsailing.com/job/SAPSailingAnalytics-master/): builds the ``main`` branch; if it shows "green" then a release and docker images will have been built
* [SAPSailingAnalytics-master-fasttrack-no-tests](https://hudson.sapsailing.com/job/SAPSailingAnalytics-master-fasttrack-no-tests/): gets triggered by builds that were run with no tests being executed, e.g., by manually invoking the [Github Release Workflow](https://github.com/SAP/sailing-analytics/actions/workflows/release.yml) with the build parameter "skip all tests" set to ``true``
* [SDBG](https://hudson.sapsailing.com/job/SDBG/) runs in the unlikely case that still some committer makes a change to the Eclipse GWT Super Dev Mode debugger; it would build a new release of the corresponding Plugin p2 repository and upload it to [https://p2.sapsailing.com/p2/sdbg](https://p2.sapsailing.com/p2/sdbg)
* [translation](https://hudson.sapsailing.com/job/translation/) is triggered by the corresponding ``translation`` branch, and as such follows the general pattern of branch names equalling the build job names; however, at its end, if the build was successful, the ``translation`` branch will be merged into the ``main`` branch by the build job
* ``bugXXXX`` jobs are the ones that correspond with branches in Git, just like a few for special branches such as ``management-console``; they are [[triggered by pushing to them in Git|wiki/howto/development/ci.md#github-webhook-on-push]]
* [CopyTemplate](https://hudson.sapsailing.com/view/archived%20jobs%20/job/CopyTemplate/) is a disabled job that serves as the template used by the ``configuration/createdHudsonJobForBug.sh`` script, so don't delete this, and don't enable this!

## How Jobs are Usually Triggered

### Github Actions ``release`` Workflow

The [release](https://github.com/SAP/sailing-analytics/actions/workflows/release.yml) workflow in Github is defined such that at its end it makes an HTTP request to https://hudson.sapsailing.com/job/${JOB}/build, passing a secret access token as defined in the Hudson build jobs (taken from the CopyTemplate job), which will trigger the build job that corresponds to the branch the ``release`` workflow just tried to build. No rule without exceptions: the ``main`` branch is mapped to ``SAPSailingAnalytics-master``, and if ``main`` was built without test execution, then ``SAPSailingAnalytics-master-fasttrack-no-tests``. Furthermore, branches named ``releases/...`` then the ``releases/`` is stripped from the branch name to obtain the name of the build job to trigger (see also [[Building with Release|wiki/info/landscape/development-environment.md#exceptionally-building-without-running-tests-more-fewer-cpus-and-with-release]]).

### Manually Triggering a Job

The ``release`` workflow can be dispatched manually. It has a few build parameters you can use to configure the build. If you skip tests but build a branch producing a release, that release will be named "untested-..." for clarity. You can also configure the number of CPUs within certain limits and steps. Should you have made changes to the [[target platform|wiki/info/general/workspace-bundles-projects-structure.md#target-platform]] on your branch that would require a different set of bundles in our base p2 repository under https://p2.sapsailing.com/p2/sailing, you can also explicitly ask for a build here which will first construct a temporary base p2 repository during the build and use that instead of the regular one.

### Github Webhook on Push

When commits are pushed to our Github repository, a [webhook](https://github.com/SAP/sailing-analytics/settings/hooks/480929064) is triggered which sends an HTTP request to [https://git.sapsailing.com/hooks/github.cgi](https://git.sapsailing.com/hooks/github.cgi). This, in turn, is backed by the script ``/var/www/cgi-bin/github.cgi`` which is installed there when the central reverse proxy is set up from [configuration/environments_scripts/central_reverse_proxy/files/var/www/cgi-bin/github.cgi](https://github.com/SAP/sailing-analytics/blob/main/configuration/environments_scripts/central_reverse_proxy/files/var/www/cgi-bin/github.cgi). Currently, that script's only task is to check whether the push originated from the translators making contributions to the ``translation`` branch and if so, push those changes also to the ``translation`` branch of our internal Git repository at ssh://trac@sapsailing.com/home/trac/git.

## Disabling a Job

When done with a branch for the time being or for good, you can disable the corresponding Hudson job that was created for it. The job's page has a corresponding "Disable job" button on it.

Disabling (rather than deleting) jobs has the benefit of all data (logs, test runs, measurements) of those builds that are kept (usually the latest ten builds) will be preserved. This way it is usually safe to reference build and test results in your Bugzilla comments.

Disabled jobs will not show directly on the landing page at [https://hudson.sapsailing.com](https://hudson.sapsailing.com) but instead can be found under the [archived jobs](https://hudson.sapsailing.com/view/archived%20jobs%20/) tab. This way, the landing page stays clean and tidy.

## Collecting measurements using Hudson/Jenkins

If you have a test case that measures something, such as performance or level of fulfillment or any other numeric measure, you can have Hudson/Jenkins plot it. In your test case, use the class `com.sap.sailing.domain.test.measurements.MeasurementXMLFile` and add performance cases to which you add measurements, e.g., as follows:
<pre>
        MeasurementXMLFile performanceReport = new MeasurementXMLFile(getClass());
        MeasurementCase performanceReportCase = performanceReport.addCase(getClass().getSimpleName());
        performanceReportCase.addMeasurement(new Measurement("My Measurement", theNumberIMeasured));
        performanceReport.write();
</pre>

## In case you'd like to set up your own Hudson/Jenkins

Initially we had trouble with Jenkins and the GIT plug-in. However, https://issues.jenkins-ci.org/browse/JENKINS-13381?focusedCommentId=196689&page=com.atlassian.jira.plugin.system.issuetabpanels:comment-tabpanel#comment-196689 explains that installing the Credentials plugin into Jenkins may help. Basically, what is needed over and above a plain Jenkins installation are the following plug-ins:

* Credentials
* Jenkins GIT
* Jenkins GIT Client
* Measurement Plots
* SSH Credentials
* Xvnc

Note, though that the Xvnc plug-in in version 1.16 seems to be causing some trouble (https://issues.jenkins-ci.org/browse/JENKINS-22105). Downgrading to 1.14 helps. The 1.14 .hpi file can be obtained, e.g., here: http://www.filewatcher.com/m/xvnc.hpi.21882-0.html.

Make sure that the environment used to run Hudson/Jenkins uses a UTF-8 locale. Under Linux, it's a good idea to set the environment variables
<pre>
    export LC_ALL=en_US.UTF-8
    export LANG=us_US.UTF-8
</pre>
which can, depending on how your Hudson/Jenkins is started, be included, e.g., in `/etc/init/jenkins` which then should have a section that looks like this:
<pre>
script
    [ -r /etc/default/jenkins ] && . /etc/default/jenkins
    export JENKINS_HOME
    export LC_ALL=en_US.UTF-8
    export LANG=us_US.UTF-8
    exec start-stop-daemon --start -c $JENKINS_USER --exec $JAVA --name jenkins \
        -- $JAVA_ARGS -jar $JENKINS_WAR $JENKINS_ARGS --logfile=$JENKINS_LOG
end script
</pre>
Other options for setting the locale include adding the LC_ALL and LANG variables to the `/etc/environment` file.

The basic idea of setting up a build job is to create a so-called "free-style software project" which then executes our `configuration/buildAndUpdateProduct.sh` script using the `build` parameter. Top-down, the following adjustments to a default free-style job that are required for a successful build are these:

* select "Git"
* enter `ssh://trac@sapsailing.com/home/trac/git` as the Repository URL
* create credentials using the `Add` button, e.g., pasting your private key and providing Jenkins with the password
* enter `master` for "Branches to build"
* under "Build Triggers" check "Poll SCM" and enter `H/1 * * * *` for the schedule which will check for updates in git every minute
* under "Build Environment" check "Run Xvnc during build"
* under "Build" select "Add build step" --> "Execute Shell" and paste as command something like this: `ANDROID_HOME=/usr/local/android-sdk-linux configuration/buildAndUpdateProduct.sh build`. Adjust the location of the Android SDK accordingly and install it if not already present.
* as Post-build Action, select "Publish JUnit test result report" and as Test report XMLs provide `**/TEST-*.xml` as the file pattern for the test reports.
* check the "Additional test reports features / Measurement Plots" box
* provide e-mail notification settings as you see fit

## Hudson Master/Slave Set-Up

In order to elastically scale our build / CI infrastructure, we use AWS to provide Hudson build slaves on demand. The Hudson Master (https://hudson.sapsailing.com) has a script obtained from our git at ``./configuration/launchhudsonslave`` which takes an Amazon Machine Image (AMI), launches it in our default region (eu-west-1) and connects to it. The AWS credentials are stored in the ``root`` account on ``hudson.sapsailing.com``, and the ``hudson`` user is granted access to the script by means of an ``/etc/sudoers.d`` entry.

The image has been crafted specifically to contain the tools required for the build. In order to set up such an image based on Ubuntu, consider running the following commands as root on a fresh Ubuntu 20.04 instance with a 100GB root partition, starting as the "ubuntu" user:

```
   scp -o StrictHostKeyChecking=false trac@sapsailing.com:/home/wiki/gitwiki/configuration/imageupgrade_functions.sh /tmp
   scp -o StrictHostKeyChecking=false trac@sapsailing.com:/home/wiki/gitwiki/configuration/hudson_slave_setup/* /tmp
   sudo -i
   # For Ubuntu 22.x install libssl1.1:
   wget http://archive.ubuntu.com/ubuntu/pool/main/o/openssl/libssl1.1_1.1.1f-1ubuntu2_amd64.deb
   dpkg -i libssl1.1_1.1.1f-1ubuntu2_amd64.deb
   rm libssl1.1_1.1.1f-1ubuntu2_amd64.deb
   # For all O/S versions:
   dd if=/dev/zero of=/var/cache/swapfile bs=1G count=20
   chmod 600 /var/cache/swapfile
   mkswap /var/cache/swapfile
   echo "/var/cache/swapfile none swap sw 0 0" >>/etc/fstab
   swapon -a
   mkdir /opt/android-sdk-linux
   echo "dev.internal.sapsailing.com:/home/hudson/android-sdk-linux /opt/android-sdk-linux nfs tcp,intr,timeo=100,retry=0" >>/etc/fstab
   apt-get update
   apt-get install -y unzip xvfb libxi6 libgconf-2-4 nfs-common gnupg
   curl -sS -o - https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add
   wget -qO - https://www.mongodb.org/static/pgp/server-4.4.asc | sudo apt-key add -
   echo "deb https://repo.mongodb.org/apt/ubuntu focal/mongodb-org/4.4 multiverse" >/etc/apt/sources.list.d/mongodb-org-4.4.list
   apt-get -y update
   apt-get -y upgrade
   # The following work well on Ubuntu 20.04 but not on Ubuntu 22.04 or Debian 11 because
   # the firefox package may be "firefox-esr" and firefox-geckodriver and the linux*aws packages may not be available at
   # all, in which case a direct download, e.g., from https://github.com/mozilla/geckodriver/releases/download/v0.31.0/geckodriver-v0.31.0-linux64.tar.gz
   # is an alternative; unpack to /usr/local/bin.
   cd /usr/local/bin
   wget -O - "https://github.com/mozilla/geckodriver/releases/download/v0.31.0/geckodriver-v0.31.0-linux64.tar.gz" | tar xzvpf -
   apt-get -y install jq cloud-guest-utils maven rabbitmq-server mongodb-org firefox fwupd linux-aws linux-headers-aws linux-image-aws docker.io
   apt-get -y autoremove
   cd /tmp
   mv /tmp/imageupgrade /usr/local/bin
   mv /tmp/imageupgrade_functions.sh /usr/local/bin
   mv /tmp/mounthudsonworkspace /usr/local/bin
   mv /tmp/*.service /etc/systemd/system/
   source /usr/local/bin/imageupgrade_functions.sh
   download_and_install_latest_sap_jvm_8
   systemctl daemon-reload
   systemctl enable imageupgrade.service
   systemctl enable mounthudsonworkspace.service
   systemctl enable mongod.service
   systemctl enable rabbitmq-server.service
   adduser --system --shell /bin/bash --quiet --group --disabled-password sailing
   adduser --system --shell /bin/bash --quiet --group --disabled-password hudson
   adduser hudson docker
   # Now log in to the docker registry at docker.sapsailing.com:443 with a valid user account for local user "hudson"
   sudo -u hudson docker login docker.sapsailing.com:443
   sudo -u sailing mkdir /home/sailing/.ssh
   sudo -u sailing chmod 700 /home/sailing/.ssh
   sudo -u hudson mkdir /home/hudson/.ssh
   sudo -u hudson chmod 700 /home/hudson/.ssh
   sudo -u hudson mkdir /home/hudson/workspace
   sudo -u hudson git config --global user.email "hudson@sapsailing.com"
   sudo -u hudson git config --global user.name "Hudson CI SAP Sailing Analytics"
   # Now add a password-less private ssh key "id_rsa" to /home/sailing/.ssh and make sure it is eligible to access trac@sapsailing.com
   chown sailing /home/sailing/.ssh/id_*
   chgrp sailing /home/sailing/.ssh/id_*
   chmod 600 /home/sailing/.ssh/id_*
   cp /home/sailing/.ssh/id_* /home/hudson/.ssh
   chown hudson /home/hudson/.ssh/*
   chgrp hudson /home/hudson/.ssh/*
   chmod 600 /home/hudson/.ssh/id_*
   # ensure the host key of sapsailing.com is accepted:
   sudo -u sailing ssh -o StrictHostKeyChecking=false trac@sapsailing.com ls >/dev/null
   sudo -u sailing git clone trac@sapsailing.com:/home/trac/git /home/sailing/code
   sudo -u hudson mkdir /home/hudson/.m2
   chmod a+r /home/sailing
   chmod a+x /home/sailing
   sudo -u hudson cp /home/sailing/code/toolchains.xml /home/hudson/.m2
   sudo -u hudson ssh -o StrictHostKeyChecking=false trac@sapsailing.com ls >/dev/null
   echo "export JAVA_HOME=/opt/sapjvm_8" >/etc/profile.d/sapjvm.sh
   chmod a+x /etc/profile.d/sapjvm.sh
   echo "ssh-rsa AAAAB3NzaC1yc2EAAAABIwAAAQEA6TjveiR+KkEbQQcAEcme6PCUHZLLU5ENRCXnXKaFolWrBj77xEMf3RrlLJ1TINepuwydHDtN5of0D1kjykAIlgZPeMYf9zq3mx0dQk/B2IEFSW8Mbj74mYDpQoUULwosSmWz3yAhfLRgE83C7Wvdb0ToBGVHeHba2IFsupnxU6gcInz8SfX3lP78mh4KzVkNmQdXkfEC2Qe/HUeDLdI8gqVtAOd0NKY8yv/LUf4JX8wlZb6rU9Y4nWDGbgcv/k8h67xYRI4YbtEDVkPBqCZux66JuwKF4uZ2q+rPZTYRYJWT8/0x1jz5W5DQtuDVITT1jb1YsriegOZgp9LfS11B7w== hudson@ip-172-31-28-17" >/home/hudson/.ssh/authorized_keys
   sudo -u hudson wget -O /home/hudson/slave.jar "https://hudson.sapsailing.com/jnlpJars/slave.jar"
```

Furthermore, the ephemeral storage is partitioned with a ``gpt`` label into a swap partition with 8GB and the remainder as an ``ext4`` partition mounted under ``/ephemeral/data`` with is then bound with a "bind" mount to ``/home/hudson/workspace``. See the ``/etc/systemd/system/mounthudsonworkspace.service`` systemd service definition on the slave instances. The ``launchhudsonslave`` script launches the instance, checks for it to enter the ``running`` state, then tries to connect using SSH with user ``hudson``. The respective keys are baked into the image and match up with the key stored in ``hudson@hudson.sapsailing.com:.ssh``.

The ``launchhudsonslave`` script will then establish the SSH connection, launching the ``slave.jar`` connector. When the Hudson Master disconnects, the Java VM running ``slave.jar`` will terminate, and the next script command of ``launchhudsonslave`` will shutdown the host. This is possible for user ``hudson`` due to corresponding entries under ``/etc/sudoers.d``. The hosts are launched such that shutting them down will terminate the Amazon EC2 instance.

## Hudson patch for mail-1.6.2

With JDKs around 1.8.0_291 an original Hudson installation faces trouble when sending out e-mails through TLS-secured SMTP servers such as Amazon Simple Email Service (SES). The problem can be solved by replacing ``WEB-INF/lib/mail-1.4.4.jar`` in the ``/usr/lib/hudson/hudson.war`` file by a newer copy, such as ``mail-1.6.2.jar``, sometimes also referred to as ``com.sun.mail-1.6.2.jar`` or ``javax.mail-1.6.2.jar``. A correspondingly patched version can be found at [https://static.sapsailing.com/hudson.war.patched-with-mail-1.6.2](https://static.sapsailing.com/hudson.war.patched-with-mail-1.6.2).
