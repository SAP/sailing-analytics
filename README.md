# Sailing Analytics

## About this Project

The Sailing Analytics, formerly known as the "SAP Sailing Analytics," are a solution for portraying and analyzing sailing regattas, supporting training scenarios, and powering the vast archive at https://sapsailing.com. The solution consists of a cloud application with a web-based user interface, as well as three companion apps that integrate with the cloud application. This repository has the code for the cloud-based web application, and two of the three mobile apps (Buoy Pinger and Race Manager). The third companion app (Sail Insight) is found in [another repository](https://github.com/SAP/sailing-analytics-sail-insight).

## Description

This is the software running the SAP Sailing Analytics platform as seen on [sapsailing.com](https://sapsailing.com). By having this under an open-source license, all interested parties can use this software, extend or modify it, host it in their cloud or on-premise environments, use it to run beautiful events with it, and keep it available to the sailing community.

Sailing provided the perfect platform for SAP to showcase solutions and help the sport run like never before. SAP’s involvement in the sport has transformed the sailing experience by providing tools, which:

- Help sailors analyze performance and optimize strategy
- Bring fans closer to the action
- Provide the media with information and insights to deliver a greater informed commentary

SAP has a longstanding involvement with sailing and has established a portfolio spanning across teams and regattas. Highlights were two Olympic Summer Games (Tokyo/Enoshima 2020/2021, Paris/Marseille 2024) where the solution saw close to a million unique visitors.

More background information is available in the project's Wiki which is currently hosted at [https://wiki.sapsailing.com](https://wiki.sapsailing.com). The Wiki's contents can also be found in the ``wiki/`` folder in the root of this repository.

## Quick Start with Docker Compose

If you have built or obtained the ``ghcr.io/sap/sailing-analytics:latest`` multi-platform image (currently available for linux/amd64 and linux/arm64 architectures), try this:
```
    cd docker
    docker-compose up
```
Based on the ``docker/docker-compose.yml`` definition you should end up with three running Docker containers:
- a MongoDB server, listening on default port 27017
- a RabbitMQ server, listening on default port
- a Sailing Analytics server, listening for HTTP requests on port 8888 and for telnet connections to the OSGi console on port 14888

Try a request to [``http://127.0.0.1:8888/index.html``](http://127.0.0.1:8888/index.html) or [``http://127.0.0.1:8888/gwt/status``](http://127.0.0.1:8888/gwt/status) to see if things worked.

To use Java 17, use the ``docker-compose-17.yml`` file instead:

```
    cd docker
    docker-compose -f docker-compose-17.yml up
```

## Requirements

The software can be run on any Linux or Windows machine with ``bash`` installed; it has also been compiled successfully for the ARM platform and was deployed to a Raspberry Pi computer. As a database, MongoDB is required, tested with releases 4.4, 5.0, 6.0, and 7.0. For use in a replicated scenario (scale-out, high availability), RabbitMQ is required. A simple Docker Compose set-up can be used to tie these three components together, e.g., for a quick local test and to familiarize yourself with the application, as Docker images are produced on a regular basis.

Compute node and database sizing depends on several aspects of your workloads, such as whether live or replay data is to be served, how many different classes with separate leaderboard are racing concurrently, how many competitors are racing in each class, or how many concurrent viewers produce how many requests and which type (e.g., analytical, data mining, or watching a live race).

To quantify this at least approximately, here are a few examples for typical node sizes that can handle such types of events reasonably well:

- National sailing league event; six boats, 18 competitors, a single live leaderboard: 8GB of RAM and 4-8 CPUs
- Large multi-class event with 15 classes with their separate leaderboards, concurrently racing on six course areas: 16GB of RAM, 16 CPUs
- Archive of 30,000 races with a few thousand visitors per day with varying analytical and replay workloads: 64GB RAM, 2TB NVMe swap, 8 CPUs

A single node typically handles up to 500-1000 concurrent viewers for live events. You will want to scale out accordingly, using the replication pattern offered by the solution which uses RabbitMQ for transaction log shipping.

## Contributing

To start contributing, read the onboarding document at the following URL: [https://wiki.sapsailing.com/wiki/howto/onboarding](https://wiki.sapsailing.com/wiki/howto/onboarding). Further documentation can be found in our Wiki at [https://wiki.sapsailing.com](https://wiki.sapsailing.com), serving the content from this repo's ``wiki/`` folder.

The project welcomes contributions in the form of pull requests, for example, enhancements of the Data Mining functionality, including any sailing-specific metric or dimension you may think of and that you find is still missing so far; or new connectors to exciting new tracking systems; or additional features for the race viewer; or a map visualization that does not require a Google Map but uses Open Street Map / Open Layers; landscape automation; improved start sequence analytics; major UI improvements for the administrative layer ("AdminConsole"), etc.

The issue tracker at [https://bugzilla.sapsailing.com](https://bugzilla.sapsailing.com) is currently used for any sort of issue and enhancement request tracking. Help to migrate this smoothly to Github Issues would be much appreciated, ideally keeping issue numbers stable due to many references to those Bugzilla bug numbers, be it in the source code, the Wiki, or the build infrastructure.


## Code of Conduct

We as members, contributors, and leaders pledge to make participation in our community a harassment-free experience for everyone. By participating in this project, you agree to abide by its [Code of Conduct](https://github.com/SAP/.github/blob/main/CODE_OF_CONDUCT.md) at all times.

## Licensing

Copyright 2024 SAP SE or an SAP affiliate company and sailing-analytics contributors. Please see our [LICENSE](LICENSE) for copyright and license information. Detailed information including third-party components and their licensing/copyright information is available [via the REUSE tool](https://api.reuse.software/info/github.com/SAP/sailing-analytics).

See [here](https://www.sapsailing.com/gwt/Home.html#/imprint/:) for a list of components used by the project, as well as their licenses, also to be found in the file ``java/com.sap.sailing.gwt.ui/imprint.json``.

## Building and Running

This assumes you have completed the onboarding (see again [here](https://wiki.sapsailing.com/wiki/howto/onboarding)) successfully. To build, then invoke

```
    configuration/buildAndUpdateProduct.sh build
```
This will build the Android companion apps first, then the web application. If the build was successful you can install the product locally by invoking

```
    configuration/buildAndUpdateProduct.sh install [ -s <server-name> ]
```

The default server name is taken to be your current branch name, e.g., ``master``. The install goes to ``${HOME}/servers/{server-name}``. You will find a ``start`` script there which you can use to launch the product.

Or you create a release by running

```
    configuration/buildAndUpdateProduct.sh -L -u -n <release-name> release
```
which produces a tarball under ``dist/{release-name}-{timestamp}/{release-name}-{timestamp}.tar.gz`` which can then be used for ``scp``-based downloads with the ``refreshInstance.sh`` script under ``java/target``.

Run the ``buildAndUpdateProduct.sh`` without any arguments to see the sub-commands and options available.


## Downloading, Installing and Running an Official Release

You need to have Java 8 installed. Get one from [here](https://tools.eu1.hana.ondemand.com/#cloud). Either ensure that this JVM's ``java`` executable in on the ``PATH`` or set ``JAVA_HOME`` appropriately.

At [https://releases.sapsailing.com](https://releases.sapsailing.com) you find official product builds. To fetch and install one of them, make an empty directory, change into it and run the ``refreshInstance.sh`` command, e.g., like this:
```
    mkdir sailinganalytics
    cd sailinganalytics
    echo "MONGODB_URI=mongodb://localhost/winddb" | ${GIT_ROOT}/java/target/refreshInstance.sh auto-install-from-stdin
```

This will download and install the latest release and configure it such that it will connect to a MongoDB server running locally (``localhost``) and listening on the default port ``27017``, using the database called ``winddb``.

In addition to the necessary ``MONGODB_URI`` variable you may need to inject a few secrets into your runtime environment:

- ``MANAGE2SAIL_ACCESS_TOKEN`` access token for result and regatta structure import from the Manage2Sail regatta management system
- ``IGTIMI_CLIENT_ID`` / ``IGTIMI_CLIENT_SECRET`` credentials for ``igtimi.com`` in case you have one or more WindBot devices that you would like to integrate with
- ``GOOGLE_MAPS_AUTHENTICATION_PARAMS`` as in ``"key=..."`` or ``"client=..."``, required to display the Google Map in the race viewer. Obtain a Google Maps key from the Google Cloud Developer console, e.g., [here](https://console.cloud.google.com/apis/dashboard).
- ``YOUTUBE_API_KEY`` as in ``"key=..."``, required to analyze time stamps and durations of YouTube videos when linking to races. Obtain a YouTube API key from the Google Cloud Developer console, e.g., [here](https://console.cloud.google.com/apis/dashboard).

In the ``configuration/mail.properties`` file make the necessary adjustments in case you would like to enable the application to send out e-mails, e.g., for user notifications or invitations.

Launch the server from the directory to which you just installed it:
```
    ./start
```
See the server logs like this:
```
    tail -f logs/sailing0.log.0
```
Connect to your server at ``http://localhost:8888`` and find its administration console at ``http://localhost:8888/gwt/AdminConsole.html``. The first-time default login user is ``admin`` with default password ``admin`` (please change).

## Docker

To build a docker image, try ``docker/makeImageForLatestRelease``. The upload to the default Github Package Registry (``ghcr.io``) will usually fail unless you are a collaborator for that repository, but you should see a local image tagged ``ghcr.io/sap/sailing-analytics:...`` resulting from the build. To run that docker image, try something like
```
    docker run -d -e "MEMORY=4g" -e "MONGODB_URI=mongodb://my.mongohost.org?replicaSet=rs0&retryWrites=true" -P <yourimage>
```

As explained above for the non-Docker scenario, add any variable assignments for secrets you need to pass to the runtime by adding more ``-e`` arguments.

Do a "docker ps" to figure out the port exposing the web application:

```
CONTAINER ID IMAGE COMMAND CREATED STATUS PORTS NAMES
79f6faf19b6a docker.sapsailing.com/sapsailing:latest "/home/sailing/serve…" 33 seconds ago Up 32 seconds
0.0.0.0:32782->6666/tcp,
0.0.0.0:32781->7091/tcp,
0.0.0.0:32780->8000/tcp,
0.0.0.0:32779->8888/tcp,
0.0.0.0:32778->14888/tcp
modest_dhawan
```

In this example, find your web application at http://localhost:32779 which is where the port 8888 exposed by the application is exposed at on your host. In the example with telnet port 14888 mapped to localhost:32788 do a
```
    telnet localhost 32778
```
to connect to the server's OSGi console.

Alternatively, use an "environment" definition that sets useful defaults, e.g., like this:
```
    docker run -P -it --rm -e "SERVER_NAME=test77" -e "USE_ENVIRONMENT=live-master-server" -e "REPLICATE_MASTER_BEARER_TOKEN=BRxGpF0nr68Z4m/f13/MgiYhdRB3xoDCYd+rLc17rTs=" ghcr.io/sap/sailing-analytics:latest \
        bash -c "rm env.sh; echo \"
SERVER_NAME=test77
USE_ENVIRONMENT=live-master-server
REPLICATE_MASTER_BEARER_TOKEN=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx\" | ./refreshInstance.sh auto-install-from-stdin; ./start fg"
```

This will download and install the latest release and set up a primary/master instance, here as an example for server name "test77" and synchronize security data from ``security-service.sapsailing.com`` using the bearer token specified as ``REPLICA_MASTER_BEARER_TOKEN``.

The default Docker image built by ``docker/makeImageForLatestRelease`` uses the ``eclipse-temurin:8-jdk`` base image. If you'd like to use the SAP JVM 8 which gives you great profiling and reversible on-the-fly debugging capabilities, you may build a Docker image for it, using ``docker/Dockerfile_sapjvm``. Using it to build an image will accept version 3.2 of the SAP tools developer license on your behalf. Build the image, e.g., like this:

```
    cd ${GITROOT}/docker
    docker build --build-arg SAPJVM_VERSION=8.1.099 -t sapjvm8:8.1.099 -f Dockerfile_sapjvm .
```

Then patch or copy ``docker/Dockerfile`` so that it has

```
   FROM sapjvm8:8.1.099
```

(of course with the version tag adjusted to what you used above when you built/tagged the image). This will give you an SAP JVM 8 under ``/opt/sapjvm_8`` in the container which in particular includes the useful ``jvmmon`` utility specific to the SAP JVM 8.

## Configuration Options, Environment Variables

The server process can be configured in various ways. The corresponding environment variables you may use during installation with ``refreshInstance.sh`` and for setting up your Docker environment can be found in the following files:
- [DefaultProcessConfigurationVariables.java](java/com.sap.sse.landscape/src/com/sap/sse/landscape/DefaultProcessConfigurationVariables.java)
- [SailingProcessConfigurationVariables.java](java/com.sap.sailing.landscape/src/com/sap/sailing/landscape/procedures/SailingProcessConfigurationVariables.java)
