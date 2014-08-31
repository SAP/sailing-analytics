# Amazon EC2 for SAP Sailing Analytics

[[_TOC_]]

## Quickstart

#### Servers

- Web Server: ec2-54-229-94-254.eu-west-1.compute.amazonaws.com
- Database and Queue Server: 172.31.25.253

#### Starting an instance

- Which instance type to choose:
  - Archive: m2.2xlarge
  - Live: c1.xlarge

You may need to select "All generations" instead of "Current generation" to see these instance configurations. Of course, you may choose variations of those as you feel is appropriate for your use case.

- Using a release, set the following in the instance's user data, replacing `myspecificevent` by a unique name of the event or series you'll be running on that instance, such as `kielerwoche2014` or similar.
<pre>
INSTALL_FROM_RELEASE=`name-of-release`
USE_ENVIRONMENT=live-server
MONGODB_PORT=10202
MONGODB_NAME=myspecificevent
REPLICATION_CHANNEL=myspecificevent
SERVER_NAME=MYSPECIFICEVENT
BUILD_COMPLETE_NOTIFY=simon.marcel.pamies@sap.com
SERVER_STARTUP_NOTIFY=simon.marcel.pamies@sap.com
</pre>

Note that when you select to install an environment using the `USE_ENVIRONMENT` variable, any other variable that you specify in the user data, such as the `MONGODB_NAME` or `REPLICATION_CHANNEL` properties in the example above, these additional user data properties will override whatever comes from the environment specified by the `USE_ENVIRONMENT` parameter.

- To build from git, install and start, set the following in the instance's user data, adjusting the MONGODB_PORT and memory settings according to your needs:
<pre>
BUILD_BEFORE_START=True
BUILD_FROM=master
RUN_TESTS=False
COMPILE_GWT=True
BUILD_COMPLETE_NOTIFY=simon.marcel.pamies@sap.com
SERVER_STARTUP_NOTIFY=
SERVER_NAME=MYSPECIFICEVENT
MEMORY=2048m
REPLICATION_HOST=172.31.25.253
REPLICATION_CHANNEL=myspecificevent
TELNET_PORT=14888
SERVER_PORT=8888
MONGODB_HOST=172.31.25.253
MONGODB_PORT=10202
MONGODB_NAME=myspecificevent
EXPEDITION_PORT=2010
REPLICATE_ON_START=False
REPLICATE_MASTER_SERVLET_HOST=
REPLICATE_MASTER_SERVLET_PORT=
REPLICATE_MASTER_QUEUE_HOST=
REPLICATE_MASTER_QUEUE_PORT=
INSTALL_FROM_RELEASE=
USE_ENVIRONMENT=
</pre>

#### Receiving wind from Expedition

- To receive and forward wind with an Expedition connector, log into webserver as user trac and switch to $HOME/servers/udpmirror. Start the mirror and forward it to the instance you want. In order to receive wind through the Igtimi connector, this step is not required as the wind data is received directly from the Igtimi server.

#### Setting up Master and Replica

- Fire up a master with the following configuration. There is a preconfigured master environment at http://releases.sapsailing.com/environments/live-master-server that you should use.

<pre>
INSTALL_FROM_RELEASE=(name-of-release)
USE_ENVIRONMENT=live-master-server
BUILD_COMPLETE_NOTIFY=simon.marcel.pamies@sap.com
SERVER_STARTUP_NOTIFY=simon.marcel.pamies@sap.com
</pre>

- After your master server is ready, note the internal IP and configure your replica instances. Make sure to use the preconfigured environment from http://releases.sapsailing.com/environments/live-replica-server. Then absolutely make sure to add the line "REPLICATE_MASTER_SERVLET_HOST" to the user-data!

<pre>
INSTALL_FROM_RELEASE=(name-of-release)
USE_ENVIRONMENT=live-replica-server
REPLICATE_MASTER_SERVLET_HOST=(IP of your master server)
SERVER_STARTUP_NOTIFY=simon.marcel.pamies@sap.com
BUILD_COMPLETE_NOTIFY=
</pre>

## Costs per month

To give you a feeling about the costs you can refer to the following table. To get all details go to http://www.awsnow.info/

<table>
<tr>
<td>Server Type</td>
<td>Cost per Month</td>
<td>Cost per Month (Reserved instance for 12 months)</td>
</tr>
<tr>
<td>m2.2xlarge (Archive)</td>
<td>$800</td>
<td>$400</td>
</tr>
<tr>
<td>c1.xlarge (Build and Live)</td>
<td>$500</td>
<td>$350</td>
</tr>
</table>

## General Information and Security

Since XXX 2013 this project is using EC2 as the server provider. Amazon Elastic Compute Cloud (EC2) is a central part of Amazon.com's cloud computing platform, Amazon Web Services (AWS). EC2 allows users to rent virtual computers on which to run their own computer applications. EC2 allows scalable deployment of applications by providing a Web service through which a user can boot an Amazon Machine Image to create a virtual machine, which Amazon calls an "instance", containing any software desired. A user can create, launch, and terminate server instances as needed, paying by the hour for active servers, hence the term "elastic".

This project is associated with an SAP Sailing Analytics account that, for billing purposes, is a subsidiary of a main SAP billing account. The Analytics account number is "0173-6397-0217 (simon.marcel.pamies@sap.com)" and connected to "SAP CMC Production (hagen.stanek@sap.com)". It has "Dr. Axel Uhl (axel.uhl@sap.com)" configured as operations officer that can be contacted by Amazon in case of problems with the instances.

The main entry point for the account is https://console.aws.amazon.com/. There you can only log in using the root account. You will then have access to not only the EC2 Console but also to the main account details (including billing details).

<img src="/wiki/images/amazon/RootAccount.JPG" width="100%" height="100%"/>

Associated to the root account are _n_ users that can be configured using the IAM (User Management, https://console.aws.amazon.com/iam/home). Each of these users can belong to different groups that have different rights associated. Currently two groups exist:

* **Administrators**: Users belonging to this group have access to all EC2 services (including IAM). They do not have the right to manage main account information (like billing).

* **Seniors**: Everyone belonging to this group can not access IAM but everything else.

Users configured in the IAM and at least belonging to the group Seniors can log in using the following url https://017363970217.signin.aws.amazon.com/console. All users that belong to one of these groups absolutely need to have MFA activated. MFA (Multi-Factor-Authentication) can be compared to the RSA token that needs to be input every time one wants to access the SAP network. After activation users need to synchronize their device using a barcode that is displayed in IAM. The device can be a software (Google Authenticator for iOS and Android) or a physical device.

<img src="/wiki/images/amazon/IAMUsers.JPG" width="100%" height="100%"/>

In addition to having a password and MFA set for one user one can activate "Access Keys". These keys are a combination of hashed username ("ID") and a password ("Key"). These are needed in case of API related access (e.g. S3 uploader scripts). One user should not have more than 1 access key active because of security concerns and never distribute them over insecure channels.

## EC2 Server Architecture for Sailing Analytics

The architecture is divided into logical tiers. These are represented by firewall configurations (Security Groups) that can be associated to Instances. Each tier can contain one or more instances. The following image depicts the parts of the architecture.

<img src="/wiki/images/amazon/EC2Architecture.JPG" width="100%" height="100%"/>

### Tiers

* **Webserver**: Holds one or more webserver instances that represent the public facing part of the architecture. Only instances running in this tier should have an Elastic IP assigned. In the image you can see one configured instance that delivers content for sapsailing.com. It has some services running on it like an Apache, the GIT repository and the UDP mirror. The Apache is configured to proxy HTTP(S) connections to an Archive or Live server.
* **Balancer**: Features an Elastic Load Balancer. Such balancers can be configured to distribute traffic among many other running instances. Internally an ELB consists of multiple balancing instances on which load is distributed by a DNS round robin so that bandwidth is not a limiting factor.
* **Database**: Instances handling all operations related to persistence. Must be reachable by the "Instance" and "Balancer+Group" tier. In the standard setup this tier only contains one database server that handles connections to MongoDB, MySQL and RabbitMQ.
* **Instances**: Space where all instances, that are not logically grouped, live. In the image one can see three running instances. One serving archived data, one serving a live event and one for build and test purposes.
* **Balancer+Group**: Analytics instances grouped and managed by an Elastic Load Balancer. A group is just a term describing multiple instances replicating from one master instance. The word "group" does in this context not refer to the so called "Placement Groups".

### Instances

<table>
<tr>
<td><b>Name</b></td>
<td><b>Access Key(s)</b></td>
<td><b>Security Group</b></td>
<td><b>Services</b></td>
<td><b>Description</b></td>
</tr>
<tr>
<td>Webserver (Elastic IP: 54.229.94.254)</td>
<td>Administrator</td>
<td>Webserver</td>
<td>Apache, GIT, Piwik, Bugzilla, Wiki</td>
<td>This tier holds one instance that has one public Elastic IP associated. This instance manages all domains and subdomains associated with this project. It also contains the public GIT repository.</td>
</tr>
<tr>
<td>DB & Messaging</td>
<td>Administrator</td>
<td>Database and Messaging</td>
<td>MongoDB, MySQL, RabbitMQ</td>
<td>All databases needed by either the Analytics applications or tools like Piwik and Bugzilla are managed by this instance.</td>
</tr>
<tr>
<td>Archive</td>
<td>Administrator, Sailing User</td>
<td>Sailing Analytics App</td>
<td>Java App</td>
<td>Instance handling the access to all historical races.</td>
</tr>
<tr>
<td>Build and Test</td>
<td>Administrator, Sailing User</td>
<td>Sailing Analytics App</td>
<td>X11,Firefox,Hudson</td>
<td>Instance that can be used to run tests</td>
</tr>
</table>

## HowTo

### Create a new Analytics application instance ready for production

Create a new Analytics instance as described in detail here [[wiki/amazon-ec2-create-new-app-instance]]. You should use a configuration like the following. You have two possibilities of making sure that the server uses code from a specific branch.

- First you can use a release file. These files can be usually found at http://releases.sapsailing.com/ and represent a certain point in time. These files can be built by using the buildAndUpdateProduct.sh with the parameter release. In addition to the release file you can specify an environment configuration. These usually can be found here http://releases.sapsailing.com/environments. A configuration then could look like this:

<pre>
INSTALL_FROM_RELEASE=master-201311062138
USE_ENVIRONMENT=live-server
BUILD_COMPLETE_NOTIFY=simon.marcel.pamies@sap.com
SERVER_STARTUP_NOTIFY=simon.marcel.pamies@sap.com
</pre>

- The second option is to let the instance build itself from a specified branch. It is currently not supported to then specify an environment file. Attention: You can not start the building process on t1.micro instances having less than 1.5 GB of RAM! The configuration then looks like this:

<pre>
BUILD_BEFORE_START=True
BUILD_FROM=master
RUN_TESTS=False
COMPILE_GWT=True
BUILD_COMPLETE_NOTIFY=simon.marcel.pamies@sap.com
SERVER_STARTUP_NOTIFY=
SERVER_NAME=LIVE1
MEMORY=2048m
REPLICATION_HOST=172.31.25.253
REPLICATION_CHANNEL=sapsailinganalytics-live
TELNET_PORT=14888
SERVER_PORT=8888
MONGODB_HOST=172.31.25.253
MONGODB_PORT=10202
EXPEDITION_PORT=2010
REPLICATE_ON_START=False
REPLICATE_MASTER_SERVLET_HOST=
REPLICATE_MASTER_SERVLET_PORT=
REPLICATE_MASTER_QUEUE_HOST=
REPLICATE_MASTER_QUEUE_PORT=
INSTALL_FROM_RELEASE=
USE_ENVIRONMENT=
</pre>

After your instance has been started (and build and tests are through) it will be publicly reachable if you chose a port between 8090 and 8099. If you filled the BUILD_COMPLETE_NOTIFY field then you will get an email once the server has been built. You can also add your email address to the field SERVER_STARTUP_NOTIFY to get an email whenever the server has been started.

You can now access this instance by either using the Administrator key (for root User) or the Sailing User key (for user sailing):

<pre>
ssh -i .ssh/Administrator.pem root@ec2-54-246-247-194.eu-west-1.compute.amazonaws.com
</pre>

or

<pre>
ssh -i .ssh/SailingUser.pem sailing@ec2-54-246-247-194.eu-west-1.compute.amazonaws.com
</pre>

If you want to connect your instance to a subdomain then log onto the main webserver with the Administrator key as root, open the file `/etc/httpd/conf.d/001-events.conf` and put something like this there. As you can see you have to specify the IP address and the port the java server is running on. Make sure to always use the internal IP.

<pre>
Use Spectator idm.sapsailing.com "505 IDM 2013" 172.31.22.12 8888
</pre>

### Testing code on a server

Starting a test is as easy as starting up a new instance. Just make sure that you fill the field RUN_TESTS and set it to `True`. Also set the field BUILD_FROM to a gitspec that matches the code branch that you want to test. After tests has been run and the server has been started you will get an email giving you all the details. You can then access your instance or simply shut it down.

### Build, deploy, start and stop an instance

Log on to the instance using ssh as user `sailing`. Change to the `~/code` directory and fetch the latest git branch, e.g., using `git fetch origin; git merge origin/master`. Make sure you have a MongoDB instance running on the default port 27017 (see [here](http://wiki.sapsailing.com/wiki/amazon-ec2#Access-MongoDB-database)). You can then trigger a build. It may be a good idea to do this in  a `tmux` session because this will allow you to log off or get disconnected while the build is running. To start a tmux session, simply enter the command `tmux` at the console and you will get a new tmux session. Check the man page of tmux for more details and note that the usual Ctrl-B shortcut has been redefined in our instances to Ctrl-A to mimic the behavior of the old `screen` tool we used before we switched to tmux. Suffice it to say that you can detach from the tmux session by pressing `Ctrl-A d` which will keep the session running. To reconnect, enter `tmux attach` which works as long as you only have one tmux session running. In other cases, refer to the tmux man page again.

To launch the build, enter `configuration/buildAndUpdateProduct.sh build` as usual. Used without options, the build script will tell the options available. After the build has completed, use `configuration/buildAndUpdateProduct.sh -s server install` to install the product to ~/servers/server.

To launch the instance, change to the `~/servers/server` directory and enter `./start`. The instance will by default launch on port 8888. See `~/servers/server/env.sh` for the instance's settings. To stop the instance again, from that same directory enter `./stop`.

To access your instance externally, make sure to use the external Amazon DNS name, such as `ec2-54-72-6-31.eu-west-1.compute.amazonaws.com`. This is required for our Google Maps API key to recognize the domain. The map won't work when you simply use the IP number as the URL. A good URL to start with would therefore look something like `http://ec2-54-72-6-31.eu-west-1.compute.amazonaws.com:8888/gwt/AdminConsole.html`.

### Setup replicated instances with ELB

The main concept behind ELB is that there is one instance that you configure in the "Load Balancers" tab that serves as the main entry point for all requests going to your application. This instance can be told to pass through requests from one port to another. In order to make this ELB instance aware of the Analytics EC2 Instances it should balance over you need to add all instances that should be part of the setup to the ELB instance.

A closer look reveals that an ELB instance consists itself of many other invisible instances. These are behind a DNS round robin configuration that redirects each incoming request to one of these instances. These invisible instances then decide upon the rules you've created how and where to distribute this request to one of the associated instances.

Here are the steps to create a load balanced setup:

- Create a master instance holding all data (see http://wiki.sapsailing.com/wiki/amazon-ec2#Setting-up-Master-and-Replica)
- When using the Race Committee App (RCApp), make sure the app is configured to send its data to the master instance and not the ELB (otherwise, write requests may end up at replicas which they wouldn't know how to handle). You may want to configure a separate URL for the master server for this purpose, so you don't have to re-configure the RCApp devices when switching to a different master server.
- Create `n` instances that are configured to connect to the master server
- Create a load balancer that redirects everything from HTTP port 8888 to HTTP port 8888 and leave the other switches and checkboxes on their default value
- As "Ping Port" enter HTTP port 8888 and use /index.html as the "Ping Path." Leave the other values on their defaults again.
- Put the ELB into the "Sailing Analytics App" security group as it will appear in the landscape as a regular sailing analytics application server.
- Associate all your instances
- Connect your domain with the IP of the load balancer. It could be a good idea to use an Elastic IP that always stays the same for the domain and associate it with your load balancer. That way you can also easily switch between a load balancer and a single instance setup. Again, remember not to let the RCApp devices point to the ELB domain as their updates could hit a replica which wouldn't know how to handle!

Amazon ELB is designed to handle unlimited concurrent requests per second with “gradually increasing” load pattern (although it's initial capacity is described to reach 20k requests/secs). It is not designed to handle heavy sudden spike of load or flash traffic because of its internal structure where it needs to fire up more instances when load increases. ELB's can be pre-warmed though by writing to the AWS Support Team.

### Access MongoDB database

To launch a local MongoDB instance on the default port, enter a command such as `mkdir /home/sailing/mongodb; /opt/mongodb-linux-x86_64-1.8.1/bin/mongod --dbpath /home/sailing/mongodb`. To make sure the process survives log-off, you may want to launch it in a tmux session. See above for how to create a tmux session. In an existing tmux session, a new window can be created using `Ctrl-a c`.

### Upgrade the Sailing Analytics App AMI image

There are a number of use cases that suggest an upgrade of the AMI that we use to start new SAP Sailing Analytics instances. One can be that we want to add or modify the set of SSH keys authorized for access to the root and sailing accounts. Another can be that we would like to upgrade the git contents under `~sailing/code` for an update to the `/etc/init.d/sailing` or the `/home/sailing/code/java/target/refreshInstance.sh` script that is central to the automated launch process of a new instance. Another use case can be applying an upgrade to the underlying operating system (currently CentOS).

Follow these steps to upgrade the AMI:

* Launch a new instance based on the existing AMI
* Log in as user `root`
* Run `yum update` to update the operating system
* Remove any obsolete logs from `/home/sailing/servers/server/logs`
* Update the git contents (essential for up-to-date versions of `/etc/init.d/sailing` which links to the git, and the `refreshInstance.sh` script used during automatic instance launch), and clean any build artifacts by doing <pre>
    su - sailing
    cd code
    mvn clean
    git fetch
    git merge origin/master</pre>
* Then, still as user `sailing`, edit `~sailing/servers/server/env.sh` and remove everything after the line `# **** Overwritten environment variables ****` as this will then be the place where any downloaded environment and the EC2 user data variables will be appended later during automatic installation upon reboot.
* Check the sizes of the mounted partitions by doing `df; swapon -s`. These will come in handy after creating the new AMI in order to tag the new volume snapshots accordingly
* Update any keys in `/root/.ssh/authorized_keys` and `/home/sailing/.ssh/authorized_keys`
* In the EC2 administration console go to the "Instances" tab, select your running instance and from the "Actions" drop-down select "Create Image". Give the image the name "SAP Sailing Analytics App x.y" where "x.y" is the updated version number of the image. Just make sure it's greater than the previous one. If you feel like it, you may provide a short description telling the most important features of the image.
* Once the image creation has completed, go to the Snapshots list in the "Elastic Block Store" category and name the new snapshots appropriately. Now the information about the device sizes obtained earlier from the `df` and `swapon` commands will help you to identify which snapshot is which. Usually, the three snapshots would be something like AMI Analytics Home x.y, AMI Analytics System x.y and AMI Analytics Swap x.y with "x.y" being the version number matching that of your image.
* Now you can remove any earlier Sailing Server AMI version and the corresponding snapshots.

## Migration Checklist

### Before switching sapsailing.com to the EC2 webserver
- fire up archive server and load it (DONE)
- configure 001-events.conf starting with a copy from old sapsailing.com, using test URLs (rombalur.de) (DONE)
- clone entire MongoDB content (DONE)
- migrate MySQL for Bugzilla
- ensure that all users have access; either solicit their public keys and enter to ~trac/.ssh/authorized_keys or migrate /etc/passwd and /etc/group settings for access to trac group (DONE)
- run test build and deploy (DONE)
- fire up a live server and test it (DONE)
- fire up a replica and check that it works correctly (ERROR!)
- check that UDP mirror is working (DONE)
- check that SwissTiming StoreAndForward is working
- check that we can fire up a live2 / archive2 server and switch transparently

### Just before the migration on Sunday evening
- check that sapsailing.com is entered everywhere a hostname / domain name is required, particularly in /etc/httpd/conf.d/001-events.conf and /opt/piwik-scripts and all of /etc - also have a look at piwik and bugzilla configuration (DONE)
- disable bugzilla on old.sapsailing.com because Nameserver switch can take up to 48 hours for everyone (DONE)
- copy /home/trac/releases to webserver (DONE)
- import bugzilla to mysql (DONE)
- git fetch --all on webserver (DONE)
- tell SAP hostmaster to point old.sapsailing.com to 195.227.10.246

### Immediately after switching the sapsailing.com domain to the EC2 webserver on Sunday evening
- check that old.sapsailing.com points to 195.227.10.246
- check that EC2 web server is responding to sapsailing.com now
- fetch all git branches from what is now old.sapsailing.com; also sync gollum wiki git
- ask people (including internal Git team) to update their known_hosts files according to the new web server's key
- check if build server can access new sapsailing.com
- check why swisstiminglistener doesn't receive connections and fix

## Glossary

<table>
<tr>
<td><b>Term</b></td>
<td><b>Description</b></td>
</tr>
<tr><td>Instance</td><td>Virtual machine that runs on a Xen host. Such an instance runs forever until it is stopped. It will be billed by hours it ran. Each start will be billed by a full hour.</td></tr>
<tr><td>Spot Instance</td><td>Instances that run whenever there are free resources. It is not possible to control when or where these instances run. These instances are much cheaper than normal instances.</td></tr>
<tr><td>Amazon Machine Image (AMI)</td><td>Amazon Machine Image: Image file that contains a filesystem and a preinstalled operating system. One can create AMIs very easily from a stopped Instance by first creating a snapshot and then converting it to an AMI.</td></tr>
<tr><td>Volume</td><td>An active harddisk that can be associated to one Instance.</td></tr>
<tr><td>IOPS</td><td>Input/Output operations per second. Metric used to denote the performance of a volume. The higher the IOPS value the better the speed. Be aware of the fact that IOPS is metered by IOPS/h and is very expensive. Use with care!</td></tr>
<tr><td>Snapshot</td><td>Snapshot of a Volume</td></tr>
<tr><td>Elastic IP</td><td>IP address that can be associated to an instance. Any Elastic-IP not associated to a running Instance costs some amount of money per hour.</td></tr>
<tr><td>Security Group</td><td>Firewall configuration that can be associated to an instance. There is no need of configuring iptables or such. One can associate many instances the the same Security Group.</td></tr>
<tr><td>Elastic Load Balancer (ELB)</td><td>Service that makes it possible to balance over services running on different instances.</td></tr>
<tr><td>Network Interfaces</td><td>Virtual network interfaces that are mapped to physical network interfaces on instances. </td></tr>
<tr><td>Placement Groups</td><td>Enables applications to get the full-bisection bandwidth and low-latency network performance required for tightly coupled, node-to-node communication. Placement Groups can only contain HVM instance and have other limitations described here: https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/using_cluster_computing.html</td></tr>
</table>