# Archive Server Upgrade

## TL;DR

- Launch MongoDB replica for ``archive`` replica set (see [here](https://security-service.sapsailing.com/gwt/AdminConsole.html#LandscapeManagementPlace:))
- Wait until MongoDB replica is in ``SECONDARY`` state
- Launch "more like this" based on existing primary archive, adjusting the ``INSTALL_FROM_RELEASE`` user data entry to the release of choice and the ``Name`` tag to "SL Archive (New Candidate)"
- Wait until the new instance is done with its background tasks and CPU utilization goes to 0% (approximately 24h)
- Compare server contents, either with ``compareServers`` script or through REST API, and fix any differences
- Do some spot checks on the new instance
- Switch reverse proxy by adjusting ``ArchiveRewrite`` macro under ``root@sapsailing.com:/etc/httpd/conf.d/000-macros.conf`` followed by ``service httpd reload``
- Terminate old fail-over EC2 instance
- adjust Name tags for what is now the fail-over and what is now the primary archive server in EC2 console

[[_TOC_]]

## Details

### Landscape Overview

We currently afford a primary archive server that holds all historic "featured" events (other than club-level and self-serviced events that may continue to be hosted in specific club servers or the "my" environment). Additionally, there is a fail-over archive server that we maintain to be one release behind the primary archive server. The archive server implements the landing page for [sapsailing.com](https://sapsailing.com) and hence should have high availability. Reloading the entire archive takes several hours, so losing the primary archive server without having any sort of fail-over would cause a downtime that long.

Keeping the fail-over archive server one or more releases behind the primary archive is intended to cover the case where a new release introduces a defect that went unnoticed during quality assurance measures such as the automated tests run during the build process and the mandatory archive server content comparison before switching to a new primary archive, as well as manual spot checks on one or more events. Again, if such a defect turns out to be a show stopper, having to start up a new archive server would keep the show stopper situation for several hours.

The archive servers (primary and fail-over) are sized based on how we have observed the workload over the past years. Except for the archiving process of a new event, the archive server's workload is a read-mostly workload. No continuous data ingestion is happening, caches will be stable and well-filled after a while for the popular events, and user contention is low. At the time of this writing (2022-01-06) we see approximately 2,000 unique visitors per day browsing the archived events.

This type of workload doesn't require all data to be held in physical memory at all times. Instead, a memory architecture with plentiful fast (NVMe-based) swap space and an amount of physical memory that can easily fit a few "hot" events seems sufficient. This is currently accomodated by an ``i3.2xlarge`` instance type that comes with 62GB physical RAM and close to 2TB of available NVMe disk space used as swap area.

With this, we can size the Java VM such that it can easily hold the archive content. Currently, 300GB of heap size are sufficient, and this shouldn't be overprovisioned as garbage collection processes would take unnecessarily long.

The [archive server environment](https://releases.sapsailing.com/environments/archive-server) specifies most of what the server configuration needs. In addition to that, only the release to install and the bearer token for a user account that allows the archive server to replicate ``security-service.sapsailing.com`` need to be provided in the user data:

```
    USE_ENVIRONMENT=archive-server
    INSTALL_FROM_RELEASE=build-202112281809
    REPLICATE_MASTER_BEARER_TOKEN="..."
```

### Launching a MongoDB Replica for Replica Set ``archive``

The archive servers use a dedicated MongoDB replica set ``mongodb://dbserver.internal.sapsailing.com:10201/winddb?replicaSet=archive&retryWrites=true&readPreference=secondaryPreferred``. Obviously, loading an archive server will put some stress on this replica set which by default runs only a single MongoDB instance on the ``dbserver.internal.sapsailing.com`` host. To accelerate the loading process it is a good idea to fire up a MongoDB replica for this replica set. Based on the ``secondaryPreferred`` read preference the new archive server candidate will read from a ``SECONDARY`` replica which can be set up to have enough memory and CPUs to make the loading process quicker. Again ``i3.2xlarge`` is a good choice for now.

To check the state of your new MongoDB replica, try this:
```
  $ ssh -A trac@sapsailing.com
  $ ssh -A ec2-user@dbserver.internal.sapsailing.com
  $ mongo "mongodb://localhost:10201/?replicaSet=archive&retryWrites=true"
  archive:PRIMARY> rs.status()
```
This will show you the replica set configuration from MongoDB's perspective. In the ``members`` array you should see two instances, one of which being the ``dbserver.internal.sapsailing.com:10201`` primary. Your to-be secondary server will likely be listed as the second member (index 1) in that array, and you will continue to see a ``stateStr`` of ``STARTUP2`` for a few hours until it changes to ``SECONDARY``.

### Launching the EC2 Instance

When the MongoDB replica is available (in state ``SECONDARY`` after going through the lengthy ``STARTUP2`` phase) a new archive server candidate can be launched. A quick approach is to select the existing primary archive server in the [EC2 instances list](https://eu-west-1.console.aws.amazon.com/ec2/v2/home?region=eu-west-1#Instances:instanceState=running;search=SL%20Archive;sort=tag:Name) and choose "Launch more like this" from the context menu, then adjust the instance's user data for the correct release and set the ``Name`` tag from "SL Archive" to something like "SL Archive (New Candidate)" so you can discern it from the production and fail-over archive and launch the new instance.

The instance will start up, install the release as specified and launch the Java VM which then goes about replicating ``security-service.sapsailing.com`` and loading all archived content from the MongoDB ``archive`` replica set. This will take a few hours. During the process you can monitor the loading progress under the new archive candidate's ``/gwt/status`` URL. Use the instance's external IP and make it into a URL such as ``http://1.2.3.4:8888/gwt/status``. Watch out for the fields ``numberofracestorestore`` and ``numberofracesrestored``. The latter will converge towards the former which finally should put the overall ``available`` field to ``true``.

But note: having launched the loading of all races doesn't make the new archive candidate present all content in the proper form yet. Many background processes will keep running for several more hours, computing maneuvers and from them updating wind estimations. You should let the process finish before running the mandatory archive server content comparison. One way to track these background processes is by looking at the EC2 console's instance monitoring and there the CPU Utilization chart. You will clearly see when the CPU utilization drops from 100% to 0% when the background processes are done. Instead, you may choose to ssh into the instance and run a ``top`` command or similar and track CPU load. Alternatively, track the ``/home/sailing/servers/server/logs/sailing0.log.0`` file on the new archive server candidate. It will contain lines of the sort
```
INFO: Thread[MarkPassingCalculator for race R14 initialization,4,main]: Timeout waiting for future task com.sap.sse.util.impl.ThreadPoolAwareFutureTask@39f29211 (retrying); scheduled with executor com.sap.sse.util.impl.NamedTracingScheduledThreadPoolExecutor@51517746[Running, pool size = 7, active threads = 7, queued tasks = 352521, completed tasks = 610095][name=Default background executor]
```
that will keep repeating. Watch out for the ``queued tasks`` count. It should be decreasing, and when done it should go down to 0 eventually.

### Comparing Contents with Primary ARCHIVE Server

#### ``compareServers`` Script

This is when you can move on to comparing the new candidate's contents with the primary archive server. Two ways are currently possible. The first uses the ``compareServers`` script that you find in the Git repository in the ``java/target/`` directory. Start it like this:
```
    compareServers -el https://www.sapsailing.com http://1.2.3.4:8888
```
where ``1.2.3.4`` is assumed to be the external IP address of your new archive server candidate. The script will fetch the leaderboard groups available on both servers and then compare their contents one by one. Should a difference be observed, the difference is printed to the standard error stream and the comparison stops. You must then fix the problem and continue the comparison, starting at the previously different leaderboard group, with the following command, adding the ``-c`` option ("continue"):
```
    compareServers -cel https://www.sapsailing.com http://1.2.3.4:8888
```
Repeat until no differences are found anymore.

#### REST API ``/sailingserver/api/v1/compareservers``

Alternatively, you can use the [REST API for comparing server contents](https://www.sapsailing.com/sailingserver/webservices/api/v1/compareServers.html). Example:
```
curl https://www.sapsailing.com/sailingserver/api/v1/compareservers -d "server2=1.2.3.4:8888"
```
Differences are reported in a JSON response document. If no differences are found, you will see a response document like this:
```
        {
         "www.sapsailing.com:443": [],
         "1.2.3.4:8888": []
        }
```
The response status will be ``200`` if the comparison was ok, and ``409`` otherwise. Handle differences are described for the ``compareServers`` script above.

### Manual Spot Checks

Following the mandatory automated content comparison you should do a few spot checks on the new archive server candidate. Go to ``http://1.2.3.4:8888/gwt/Home.html`` if ``1.2.3.4`` is the public IP address of your new archive server candidate and browse through a few events. Note that clicking on a link to show all events will get you back to ``www.sapsailing.com``. In this case, replace ``www.sapsailing.com`` by your candidate server's public IP address again and continue browsing.

### Switching in Reverse Proxy

Once you are content with the quality of the new archive server candidate's contents it's time to switch. Technically, switching archive servers is done by adjusting the corresponding configuration in the central Apache reverse proxy server. You find this in ``root@sapsailing.com:/etc/httpd/conf.d/000-macros.conf`` in the definition of the macro ``ArchiveRewrite`` defined usually at the top of the file. You'll find a macro definition that looks like this:
```<Macro ArchiveRewrite>
# ARCHIVE, based on i3.2xlarge, 64GB RAM and 1.9TB swap
#       Use Rewrite 172.31.4.106 8888
        Use Rewrite 172.31.9.176 8888
</Macro>
```
Copy the uncommented ``Use Rewrite`` line and in the new copy adjust the internal IP to match the internal IP of your new archive server candidate. Then comment the line that pointed to the currently active primary archive server. Your macro definition then would look something like this, assuming that ``172.31.8.7`` were the IP address of your new archive server candidate:
```<Macro ArchiveRewrite>
# ARCHIVE, based on i3.2xlarge, 64GB RAM and 1.9TB swap
#       Use Rewrite 172.31.4.106 8888
#        Use Rewrite 172.31.9.176 8888
        Use Rewrite 172.31.8.7 8888
</Macro>
```
Exit your editor and reload the reverse proxy configuration by issuing the following command:
```
    # service httpd reload
```
Check that the new archive service is now active, e.g., by looking at [sapsailing.com/gwt/status](https://sapsailing.com/gwt/status). It should reflect the new release in its ``release`` field.

### Clean up EC2 Names and Instances

Next, you should terminate the previous fail-over archive server instance, and you need to adjust the ``Name`` tags in the EC2 console of the old primary to show that it's now the fail-over, and for the candidate to show that it's now the primary. Select the old fail-over instance and terminate it. Then change the name tag of "SL Archive" to "SL Archive (Failover)", then change that of "SL Archive (New Candidate)" to "SL Archive", and you're done.
