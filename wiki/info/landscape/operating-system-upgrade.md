# Operating System Upgrade Across Landscape

[[_TOC_]]

Mainly for security reasons we strive to keep the operating systems on which our EC2 instances are running up to date. This includes running the latest Linux kernels and having all packages updated to their latest versions as per the Amazon Linux or other Linux versions used. While doing so, we aim to keep service interruptions to a minimum and in particular keep services available at least in read-only mode also during upgrades.

We distinguish between in-place upgrades without the need to re-boot, in-place upgrade requiring a reboot (e.g., due to Linux kernel updates), and upgrades that replace EC2 instances by new EC2 instances. The latter case can be sub-divided into cases where an incremental image upgrade can be used to produce a new version of an Amazon Machine Image (AMI) used for that instance type, and cases where a new from-scratch AMI set-up will be required. Also, the procedures to use depend on the type of service run on the instance that requires an upgrade.

## Approaches for Operating System Updates

### Using AdminConsole Landscape Management Panel

The AdminConsole offers the Landscape Management panel (see, e.g., [https://security-service.sapsailing.com/gwt/AdminConsole.html#LandscapeManagementPlace:](https://security-service.sapsailing.com/gwt/AdminConsole.html#LandscapeManagementPlace:)) with a table entitled "Amazon Machine Images (AMIs)." It shows the different AMIs in use, among them the ``sailing-analytics-server``, the ``mongodb-server`` and the ``disposable-reverse-proxy`` images. Each of them have an "Upgrade" action icon in the "Actions" column that can be used to launch an instance off the image and then apply the steps necessary to upgrade the image to the latest version of kernel, all packages, and Java VM (if installed), then creates a new version of the AMI.

See below for how to proceed with the upgraded images for the different image types.

### Log On with SSH and Use Package Manager for Live Upgrade

Instead of or in addition to upgrading the AMIs to new package and kernel versions, you can also log in to a running instance using SSH, and as root (using, e.g., ``sudo``) upgrade packages and kernel in place. Should a reboot be required, however, it depends on the particular instance you have been applying this to. Some instances should not simply be rebooted as this may unnecessarily reduce availability of some services and may not always lead to a clean recovery of all services after the reboot.

For example, when rebooting an instance that runs one or more primary application processes for which replica processes run on other instances, inconsistencies between primary and replicas may result by a brute-force restart of the primary in some cases. See below for cleaner ways to do this.

#### Amazon Linux

We use Amazon Linux as the default for most instance types and hence most AMIs, particularly those for running the Sailing Analytics application, the MongoDB instances, the reverse proxy instances, and MariaDB for our Bugzilla service.

Amazon Linux 2023 uses ``dnf`` as its package manager. An operating system upgrade is performed by running, as ``root`` (e.g., by logging in as ``ec2-user`` and then using ``sudo``):
```
    dnf --releasever=latest upgrade
```
This will upgrade all packages installed as well as the kernel. When run interactively, upgrade requiring a reboot will be displayed in the update list in red color. For scripted use, consider the ``needs-restarting -r`` command, delivering an exit status of ``1`` if a reboot is required.

#### Debian

Our use of Debian is currently restricted to running RabbitMQ which is a lot harder to install and configure on Amazon Linux.

Debian uses ``apt`` as its package manager. Its default login user differs from Amazon Linux, where it is ``ec2-user`` and is called ``admin`` instead. Like the ``ec2-user`` on Amazon Linux, ``admin`` is eligible to use ``sudo`` to run commands with root privileges.

Executing an update with apt works like this:
```
   apt-get update
   apt-get upgrade
```
If this creates a file ``/var/run/reboot-required`` then the instance must be rebooted for all changes to take effect.

## Upgrading the Different Instance Types

### ``security-service.sapsailing.com`` Primary

The corresponding ``security_service`` replica set usually has a single instance running only the primary application service. It offers a few ``Replicable``s that all other replica sets (except ``DEV``) replicate, such as the ``SecurityService`` and ``SharedSailingData`` services. It acts as a hub in particular for user, group, role and permission management. Other instances have their replicated versions of the service and can make decisions locally, sign in/up and authenticate users and manage their sessions locally. Replication through the ``security_service`` replica set serves the purpose of letting users roam about the landscape. Temporary outages of the ``security_service`` replica set will delay replication of these aspects across the landscape. However, transactions will not be lost but will be queued and applied when the service becomes available again.

With this in mind, a restart of either the Java VM (in order to upgrade the application to a new version) or even a reboot of the EC2 instance, both typically done in less than 60s, will rarely cause effects noticeable to users. Therefore, we typically afford to upgrade the instance running the single primary process for the ``security_service`` replica set "in place:"

- log on with ssh as ``ec2-user``
- run ``dnf --releasever=latest upgrade``
- if a reboot is required, reboot the instance

It is useful to wait with the reboot until at least no known Sailing Analytics process start-up is happening which is in the middle of obtaining an initial load from the ``security_service`` replica set because this would be aborted and hence fail upon the reboot. Other than that, existing replicas will synchronize with the rebooted instance and the freshly started service once available again.

Should you find good reasons against an in-place upgrade, make sure you have an upgraded ``sailing-analytics-server`` AMI, remove the running instance from the ``S-security-service`` and ``S-security-service-m`` target groups, launch a new instance off the upgraded AMI with the user data copied from the running instance, with only the ``INSTALL_FROM_RELEASE`` parameter upgrade to the latest release:
```
INSTALL_FROM_RELEASE=main-202502181141
SERVER_NAME=security_service
USE_ENVIRONMENT=security-service-master
```

Then add the new instance to the ``S-security-service`` and ``S-security-service-m`` target groups and terminate the old instance.

### ``DEV``

The ``DEV`` replica set is for testing only. Other than that, the instance runs our Hudson CI environment. Both are not expected to be highly available. Therefore, the same in-place update as for the ``security_service`` replica set is possible. For a clean Hudson shut-down, consider using [this link](https://hudson.sapsailing.com/quietDown).

### ``ARCHIVE``

Make sure you have an up-to-date ``sailing-analytics-server`` AMI. Then, see [[Upgrading ARCHIVE server|wiki/info/landscape/archive-server-upgrade]] for how to launch a new ARCHIVE candidate with that new AMI and how to switch to it once the loading of all races has finished successfully.

### ``my``

You can try an [in-place upgrade](#log-on-with-ssh-and-use-package-manager-for-live-upgrade) for these. Should this, however, require a reboot, you should then apply the following procedure:

To start with, make sure you have an up-to-date ``sailing-analytics-server`` AMI (see above). Also make sure the auto-scaling group for the ``my`` replica set is set to use this latest AMI for any replicas launched by the auto-scaling group.

The ``my`` replica set is special in comparison to most other replica sets. It runs its primary process on a dedicated instance and requires an instance type with at least 500GB of swap space. A good default is an ``i3.2xlarge`` instance type. The application settings, as of this writing, require 350GB of heap size, indicated by ``MEMORY="350000m"`` in the user data section for the instance.

In order to move the ``my`` primary process to a new instance with a new operating system, use the AdminConsole's Landscape Management panel, and there the "Move master process to another instance" action. Make sure to select an appropricate ``i3....`` instance type with sufficient swap space, *not* the default ``C5_2_XLARGE`` suggestion. Explicitly enter the amount of memory you'd like to assign to the process, such as "350000" into the "Memory (MB)" field of the pop-up dialog, then confirm using the "OK" button.

This will detach all running replicas (usually exactly one) from the primary process, remove the primary process from the ``S-my`` and ``S-my-m`` target groups, then stop and remove the primary process, which will also lead to the instance being terminated as this was the last (only) application process running on it. Then, a new instance off the latest AMI will be launched, deploying and starting a new primary process for the ``my`` replica set. Once this has loaded all contents from the DB and reports a healthy status, an explicit "Upgrade Replica" is launched which uses the explicit primary instance's IP address instead of the DNS host name to obtain an initial load. This works around the fact that so far the new primary hasn't been added to any target groups yet and hence isn't reachable under the ``my.sapsailing.com `` domain name.

When the upgrade replica has reported a healthy status, the primary is added to the ``S-my`` and ``S-my-m`` target groups, and the upgrade replica is added to the ``S-my`` target group. Then, the old auto-replica which is expected to have been launched using the auto-scaling group will be terminated, causing the launching of a new instance to which a ``my`` replica is deployed and started. Once the auto-replica is healthy, the upgrade replica will be terminated which removes it from the ``S-my`` target group.

### Sailing Analytics Multi-Servers

You can try an [in-place upgrade](#log-on-with-ssh-and-use-package-manager-for-live-upgrade) for these. Should this, however, require a reboot, you should then apply the following procedure:

To start with, make sure you have an up-to-date ``sailing-analytics-server`` AMI (see above). Also make sure that all auto-scaling groups for the application replica sets are set to use this latest AMI for any replicas launched by the auto-scaling group. (This can be achieved, e.g., using the AdminConsole's Landscape Management panel, and there the "Update machine image for auto-scaling replicas" button above the replica sets table.)

Then, sort the replica sets table by the "Master Instance ID" column and identify the instances configured as "Multi-Server." Should this not be obvious, compare with the instances in the AWS EC2 console instance list named "SL Multi-Server". Click the "Move all application processes away from this replica set's master host to a new host" button. This will launch a new instance and move all application processes away from the old host, one by one. All replication aspects are handled automatically. The duration of migration varies depending on the content volumes hosted by the respective replica set. An empty replica sets migrates in a few minutes. Large replica sets may take an hour or more to migrate. The AdminConsole process may run into a timeout, but don't worry, the migration continues all the way to the end regardess of the web UI timeout.

Still, should something take suspiciously long, maybe check the server logs of the server you used the Landscape Management panel on (usually ``security-service.sapsailing.com``). The ``logs/sailing.log.0`` file may show details of what went wrong or is taking long. Sometimes it may be the loading of one or more races that fails and doesn't let the instance report a healthy status. In such cases, a manual restart of that process may help, cd'ing into its folder and running ``./stop; ./start`` explicitly.

### MariaDB

This is a clear candidate for an in-place upgrade. Should a reboot be required, just reboot. It only takes about 10s, and for Bugzilla as system used mostly internally we can afford a 10s unavailability period.

### RabbitMQ

This is Debian-based. Try to go for an in-place upgrade. Should a reboot be required, ideally choose a time outside of major events and ongoing instance upgrades as those will require the RabbitMQ service to succeed.

### MongoDB Replica Sets

We currently have three MongoDB instances running in our EC2 landscape: ``[dbserver|mongo0|mongo1].internal.sapsailing.com``. The first hosts three ``mongod`` processes, for three separate replica sets: the single primary of the ``archive`` and the ``slow`` replica sets, and a hidden replica of the ``live`` replica set. The two other instance have primary/secondary ``mongod`` processes for the ``live`` replica set. Try an in-place upgrade first. If that doesn't require a reboot, you're done.

If a reboot is required after an in-place upgrade, be gentle in how you carry out those reboots. The ``dbserver`` instance can be rebooted as long as no ARCHIVE server start-up is currently going on. During an ARCHIVE server start-up, failure to reach the database may lead to an incomplete state in the new ARCHIVE candidate which may require you to start over with the anyhow very time-consuming ARCHIVE start-up. The ``slow`` replica set and the hidden ``live`` replica, however, pose no obstacles regarding a reboot.

For ``mongo0`` and ``mongo``, log on as ``ec2-user`` using SSH and use ``mongosh`` to see whether that instance is currently PRIMARY or SECONDARY. Reboot the SECONDARY first. When that has completed, SSH into the PRIMARY and in ``mongosh`` issue the command ``rs.stepDown()`` so that the PRIMARY becomes a SECONDARY, and the other instance that previously was a SECONDARY takes over as the new PRIMARY. With this courtesy, ongoing writing transactions will not even have to go through a re-try as you reboot the now SECONDAY instance.

Should you choose to work with an upgraded ``mongodb-server`` AMI and the AdminConsole's Landscape Management panel, use the "Scale in/out" action on the respective MongoDB replica set to add new instances launched off the new AMI, then, once healthy, scale in to delete the old instances. This way you can handle the ``mongo0`` and ``mongo1`` instances. You should, however, have to adjust the DNS records in Route53 for ``mongo[01].internal.sapsailing.com`` to reflect the new instances because despite all tags-based resource discovery there are still some older configuration and environments files around that bootstrap new application instances by explicitly referring to ``mongo0.internal.sapsailing.com`` and ``mongo1.internal.sapsailing.com`` as their MongoDB instances to use.

Should you need to upgrade the central ``dbserver.internal.sapsailing.com`` instance without an in-place upgrade, use the ``configuration/environments_scripts/central_mongo_setup/setup-central-mongo-instance.sh`` script to produce a new instance. When it has launched the new instance, it prints detailed instructions to its standard output for how to unmount the data volumes from the old and mount them to the new instance, as well as which DNS actions to take in Route53 and how to name and tag the new instance.

### Central Reverse Proxy

The central reverse proxy, currently running as ``sapsailing.com``, can typically be upgraded in-place. Should a re-boot be required, launch a disposable reverse proxy in the same availability zone as the central reverse proxy first, using the AdminConsole's Landscape Management panel with its "Reverse proxies" table and the corresponding "Add" button. Once that new disposable reverse proxy is shown as healthy in the corresponding ``CentralWebServerHTTP-Dyn`` target group you can reboot the central reverse proxy. It will lead to less than 30s of downtime regarding [https://bugzilla.sapsailing.com](https://bugzilla.sapsailing.com) and [https://wiki.sapsailing.com](https://wiki.sapsailing.com), as well as our self-hosted Git repository which is still used for back-up and cross-synchronization with the checked-out workspace for our Wiki.

Should an in-place upgrade not be what you want, look into the ``configuration/environments_scripts/central_reverse_proxy`` folder with its setup scripts. They automate most parts of providing a new central reverse proxy instance that has been set up with the latest Amazon Linux from scratch. You will have to carry out a few steps manually, e.g., in the AWS Console, and the scripts will tell you in their standard output what these are.

When done, terminate the additional disposable reverse proxy that you launched in the central reverse proxy's availability zone.

### Disposable Reverse Proxy

Make sure you have an upgraded ``disposable-reverse-proxy`` AMI, then use the AdminConsole Landscape Management panel and its "Reverse proxies" section to launch one or more disposable reverse proxies with the "Add" button. The default instance type suggested is usually a good fit. Make sure to launch one per availability zone in which you'd like to replace an old reverse proxy. When your new reverse proxy is healthy (this includes an error code 503 which only indicates that the reverse proxy is not in the same availability zone as the currently active production ARCHIVE server), terminate the corresponding old reverse proxy.