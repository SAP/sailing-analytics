# Cook Book with useful recipes

[[_TOC_]]

### Export from MongoDB

To export data from MongoDB you simply have to use the monogexport command. It will export data to human readable JSON format. Make absolutely sure to use fields backed by an index in your query otherwise it can put MongoDB under heavy load and take ages.

#### Wind

`/opt/mongodb/bin/mongoexport --port 10202 -d winddb -c WIND_TRACKS -q "{'REGATTA_NAME': 'ESS 2013 Muscat (Extreme40)'}" > /tmp/ess2013-muscat-wind.json`


### Import to MongoDB

Importing requires data to be in JSON format (as exported by mongoexport). To make sure that old entries just get updated and not overwritten you must use the --upsert parameter.

#### Wind

`/opt/mongodb/bin/mongoimport --port 10202 -d winddb -c WIND_TRACKS --upsert /tmp/ess2013-muscat-wind.json`

### Migrate Master Data along with Score Corrections, etc, without touching the mongodb console

In the Admin Panel you can now find a tab called "Master Data Import". Here you can import all data corresponding to a leaderboard group from a remote server, excluding wind and the actual TrackedRaces.

#### Listing the available leaderboard groups

At first you need to enter the remote host. This could for example be "http://live2.sapsailing.com/" if you are on an archive server and you want to import the information of a recent live event that is still hosted on a live server. If you hit Enter or click the Button "Fetch Leaderboard Group List", all available leaderboard groups from the remote host will be displayed, if the connection was successful.

#### Importing all data for selected leaderboard groups

You can now select the leaderboard groups you want to import. Multi-selection is allowed. If you have already added objects like events or leaderboards where names collide with those that are to be imported, the "override" option becomes relevant. It should be checked if you want to have the same ID's, race-columns, race-log-events, score corrections as on the remote server, but it can overwrite some of the data you entered before. 
Now click "Import selected Leaderboard Groups" to start the actual import process. If successful, a pop-up will show you how many events, regattas, leaderboards and leaderboard groups where imported.

#### Media

For now, all media links are imported (even if they are not in the same time-frame as the leaderboard-grouped races). The process also looks for the override flag to decide whether it should overwrite the existing row, when there is an id-conflict.

### Hot Deploy Java Packages to a running Server

Sometimes you need to deploy code to a running OSGi server without restarting the whole process. To ease this process the script buildAndUpdateProduct.sh provides an action called _hot-deploy_. To deploy a changed package you first need to know the fully qualified name. In most cases (for com.sap* packages) this corresponds to the package name (e.g. com.sap.sailing.gwt.ui). To hot-deploy the package _com.sap.sailing.server.gateway_ one can use the following call:

`buildAndUpdateProduct.sh  -l telnetPortOfServerInstance -n com.sap.sailing.server.gateway hot-deploy`

This will first check if versions really differ and tell you the versions (installed, to-be-deployed). You can then decide to execute deployment. Deployment will not overwrite the old package but copy the new package to $SERVER_HOME/plugins/deploy. If the OSGi server is reachable on a telnet port (that you can specify with -p parameter) then you don't need to worry about the OSGi reload process it will be performed automagically. If no server can be reached then you'll get detailed instructions on how to install new package. It will then look like this:

<pre>
PROJECT_HOME is /Users/spamies/Projects/sailing/code
SERVERS_HOME is /Users/spamies/Projects/sailing/servers
OLD bundle is com.sap.sailing.monitoring with version 1.0.0.201303252301
NEW bundle is com.sap.sailing.monitoring with version 1.0.0.201303252302

Do you really want to hot-deploy bundle com.sap.sailing.monitoring to /Users/spamies/Projects/sailing/code/master? (y/N): Continuing
Copied com.sap.sailing.monitoring_1.0.0.201303252302.jar to /Users/spamies/Projects/sailing/servers/master/plugins/deploy

ERROR: Could not find any process running on port . Make sure your server has been started with -console 
I've already deployed bundle to /Users/spamies/Projects/sailing/servers/master/plugins/deploy/com.sap.sailing.monitoring_1.0.0.201303252302.jar
You can now install it yourself by issuing the following commands:

osgi> ss com.sap.sailing.monitoring
21   ACTIVE   com.sap.sailing.monitoring_1.0.0.201303252302
osgi> stop 21
osgi> uninstall 21
osgi> install file:///Users/spamies/Projects/sailing/servers/master/plugins/deploy/com.sap.sailing.monitoring_1.0.0.201303252302.jar
osgi> ss com.sap.sailing.monitoring
71   INSTALLED   com.sap.sailing.monitoring_1.0.0.201303252302
osgi> start 71
</pre>

### Display Line Endings for File

Sometimes different line endings get mixed. To display all lin endings for each line of a given file use the following command:

`perl -p -e 's[\r\n][WIN\n]; s[(?<!WIN)\n][UNIX\n]; s[\r][MAC\n];' <FILE>`

### Finding Packages for Deployment in Target Platform

Sometimes you want to add libraries to the target platform but you don't have correctly formatted JAR files. In this case you can find some files here: [Eclipse ORBIT](http://download.eclipse.org/tools/orbit/downloads/drops/R20130118183705/)

### Debug Jetty

To gather further information regarding this bug I finally got the sourcecode of org.eclipse.jetty.* imported that way that one can debug w/o touching the OSGi environment. It is a simple as this:

- Add jetty.source package to target (already included in jetty.bundle location)
- Add MANIFEST.MF based dependency to com.sap.sailing.server
- Sourcecode is now available for debugging

I now know why starting and stopping a second server destroy the web context of the first one. This happens because every JAR file that is registered as a context in Jetty is being extracted to java.io.tmpdir/jetty-<ip>-<port>/ and loaded from there. The code doing this is hidden in WebInfConfiguration and triggered by an instance of the DeploymentManager.

I ran some load testing based on apache bench tool (ab) with 10000 requests (from which 1000 were concurrent) to /gwt/AdminConsole.html to check if this leads to a change in socket count or memory consumption. It didn't and therefore Jetty has no apparent leaks. I also tested with 404 (/gwt/ThisYieldsA404) because there had been comments about Jetty leaking with 404 but no negative results here either.

I will update start scripts for each server to use $SERVER_DIR/tmp as temporary directory thus making sure that deployed binaries can not overwrite themselves.

One can find more information about the Jetty architecture here: http://docs.codehaus.org/display/JETTY/Architecture

### Ignore GIT line endings during merge

Following http://stackoverflow.com/questions/861995/is-it-possible-for-git-merge-to-ignore-line-ending-differences one can use `git config merge.renormalize true`

### Remove association between local and remote branch in GIT

To remove the association between the local and remote branch, and delete the local branch, run:

<pre>
git config --unset branch.&lt;branch&gt;.remote
git config --unset branch.&lt;branch&gt;.merge
</pre>

### Tunnel UDP packets over SSH tunnel (TCP)

Assume you want to forward UDP packets from machine A (port 2014) to machine B (port 2014) over an SSH tunnel. This is not that easy because an SSH tunnel only works for TCP packets. So let's use socat for this purpose:

- Open a tunnel from machine A to machine B: <pre>A$ ssh -L 6667:localhost:6667 user@B</pre>
- Once opened activate a TCP to UDP forwarding on machine B: <pre>B$ socat tcp4-listen:6667,reuseaddr,fork UDP:localhost:2014</pre>
- Activate UDP to TCP forwarding on machine A: <pre>A$ socat -T15 udp4-recvfrom:2014,reuseaddr,fork tcp:localhost:6667</pre>

Now also UDP packets get transmitted through.

### Set limits (ulimit) for a running process

<pre>
[root@ip-172-31-26-232 1388]# cd /proc/1388
[root@ip-172-31-26-232 1388]# echo -n "Max processes=150000:150000" > limits 
[root@ip-172-31-26-232 1388]# cat limits 
Limit                     Soft Limit           Hard Limit           Units     
Max cpu time              unlimited            unlimited            seconds   
Max file size             unlimited            unlimited            bytes     
Max data size             unlimited            unlimited            bytes     
Max stack size            10485760             unlimited            bytes     
Max core file size        0                    unlimited            bytes     
Max resident set          unlimited            unlimited            bytes     
Max processes             150000               150000               processes 
Max open files            30000                30000                files     
Max locked memory         65536                65536                bytes     
Max address space         unlimited            unlimited            bytes     
Max file locks            unlimited            unlimited            locks     
Max pending signals       273211               273211               signals   
Max msgqueue size         819200               819200               bytes     
Max nice priority         0                    0                    
Max realtime priority     0                    0                    
Max realtime timeout      unlimited            unlimited            us        
</pre>

### Show threads consuming most CPU time for a Java process

Assume that you want to know which threads exactly eat up all the CPU time. This can be done by using JConsole and the JTop plugin. You can download the plugin here `wget http://arnhem.luminis.eu/wp-content/uploads/2013/10/topthreads-1.1.jar`. The you can open jconsole like so:

<pre>
/opt/jdk1.7.0_02/bin/jconsole -pluginpath topthreads-1.1.jar
</pre>