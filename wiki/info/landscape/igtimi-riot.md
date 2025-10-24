# Igtimi WindBot Riot Connector and Server

[[_TOC_]]

## Architecture Overview

Igtimi used to be a company based in Dunedin, New Zealand, makers of the YachtBot and WindBot devices for boat tracking and wind and current sensing. The devices come equipped with a "Brain" that holds a battery, a SIM card-based Internet connection, an SDCard for configuration and log file storage, and an IMU (Inertial Motion Unit) with a satellite positioning sensor and accelerometers. The WindBot comes with a Gill ultrasonic wind sensor at the top of a carbon fibre pole, connected to the brain through a cable running inside the hollow pole. When switched on, the devices will try to establish a TCP connection to a server (the "Riot" server), performing a handshake during which the device authenticates to the server using a "device group token," then sending sensor and status data.

The transmission protocol is based on Protocol Buffers (protobuf), a serialization / de-serialization standard developed by Google. It works both ways, so the server can also send protobuf messages to the devices through the socket connection, once established. This can be used for acknowledgement messages but also for sending commands to the device, e.g., in order to power it down. The protocol used by the WindBot devices has been documented [here](https://support.yacht-bot.com/YachtBot%20Products/Riot%20Protocol/). The technical specification of the protobuf message formats can be found in [this Github project](https://github.com/igtimi/go-igtimi/tree/main/proto).

The default address to which the devices connect is ``www.igtimi.com:6000``. Since [firmware version 2.2.540](https://support.yacht-bot.com/YachtBot%20Products/Firmware%20Updates/#firmware-update-process) it is possible to [configure](https://github.com/igtimi/go-igtimi?tab=readme-ov-file#device-configuration) the address to which the devices connect in their ``config.ini`` configuration file on their SDCard. Using a line like

```
    riot server_address sapsailing.com 6000
```

the connection will be made to ``sapsailing.com:6000`` instead of the ``igtimi.com`` address.

Igtimi called the back-end to which the devices connect the "Riot" system. It has a REST API through which authenticated clients can discover devices, resources created from data sent from those devices, and data access windows which grant access to data recorded by a specific device for a specified time range to a specific set of users. The API then also allows clients to request the actual data that has been recorded so far, as well as connect with a web socket connection through which the server will stream data from requested devices live if the client authenticated with the web socket connection is permitted to read this data on the basis of one or more data access windows granting permission.

The device-to-Riot connections as well as the web socket connections from API clients to the Riot server implement a heart beat mechanism to notice broken connections and issue a re-connect.

The Riot back-end that receives data from devices can be configured regarding the port is listens on:

- ``IGTIMI_RIOT_PORT``

The Riot server is configured using the ``IGTIMI_RIOT_PORT`` environment variable which, if provided, is used to set the ``igtimi.riot.port`` system property. It defines the TCP port the devices connect to in order to reach the Riot server. If a Network Load Balancer (NLB) is used, this is the port the target group must use. If not provided, an arbitrary port that is available will be used for listening for incoming connections. The port can be discovered using the ``/igtimi/api/v1/server`` REST API end point. The "well-known" port for the Igtimi devices is 6000.

The Igtimi wind receiver that is used when tracking races with live wind data, and the REST API client for wind import are both configured using two system properties, mapped to two corresponding environment variables.

- ``IGTIMI_BASE_URL``

Use this variable to override the default ``https://wind.sapsailing.com`` base URL for obtaining wind data from Igtimi devices. Authentication to this service by default will assume shared security and therefore shared user bases with shared access tokens. In particular, when starting the tracking of a race, the current user's credentials will be used. During a server re-start, live races will use their owner's credentials to authenticate to the remote Riot API. If you don't use shared security, consider using ``IGTIMI_BEARER_TOKEN`` in addition (see next section).
    
- ``IGTIMI_BEARER_TOKEN``

Overrides the default authentication scheme for requests against the remote "Riot" service for Igtimi wind connectivity whose base URL may be overridden using ``IGTIMI_BASE_URL``. Specify a bearer token valid in the context of the security service of the remote Riot service.

We plan to deploy a replica set called ``wind`` so it is reachable at ``https://wind.sapsailing.com`` which is also the default for the Igtimi connection factory unless overridden with the ``IGTIMI_BASE_URL`` variable (see above). This replica set should consist of two nodes only. This way, in case the primary/master process becomes unavailable, devices and clients will "find" to each other on the replica where messages are enqueued for transmission to the primary/master once it becomes available again.

The management of the NLB target group for listening on the Riot port 6000 shall be automated. See also [bug6083](https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=6083).

## History of the Riot System at Igtimi and Riedel

Around 2020, the German company *Riedel Communications GmbH* acquired Igtimi and used some of their technology for the America's Cup at the time. The Riot system continued to operate under the Igtimi brand, though owned by Riedel now. In the years that followed, Riedel decided to move out of the sailing domain again. In 2024 they announced they would end the Riot service by the end of the year. Effective January 6, 2025, the Riot system running at ``https://www.yacht-bot.com`` and ``https://www.igtimi.com`` was shut down.

With the new firmware version 2.2.540, the remaining devices can now be made send to a new address that needs to receive the protobuf messages, handle the connection handshake and implement the heart beat mechanism accordingly. An example implementation was provided by Riedel as an [open-source project](https://github.com/igtimi/go-igtimi). With this, existing customers are now on their own when it comes to providing a new back-end for the devices still in use.

We decided to implement a Riot server replacement that comes as a few OSGi bundles which can be embedded in our existing Sailing Analytics architecture.

## Embedded Riot Server in the Sailing Analytics

We captured the requirement to build our own Riot server replacement in [bug 6059](https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=6059) and dependent tasks.

### OSGi Bundles

The solution is built around the existing Igtimi connector, mainly implemented by bundle ``com.sap.sailing.domain.igtimiadapter`` and web bundle ``com.sap.sailing.domain.igtimiadapter.gateway`` serving an API under the ``/igtimi`` URL path. The former had a client for the Riot REST API to authenticate with a client ID and a client secret, then query devices and data access windows, load stored data and connect to live data using web socket connections. The gateway bundle was used primarily as a callback interface to receive an OAuth token during the account authentication phase which was then used to authenticate API requests. A third bundle, ``com.sap.sailing.domain.igtimiadapter.persistence``, was used to store the account and token information persistently across server restarts, and a test fragment ``com.sap.sailing.domain.igtimiadapter.test`` ran a few tests for the API client.

A new bundle ``com.sap.sailing.domain.igtimiadapter.server`` was added that now implements the core Riot functionality, in particular the socket server that listens for incoming device connections. The persistence bundle was changed and now stores what the Riot server implementation needs: ``Device`` and ``DataAccessWindow`` entities as well as the messages received form the devices. This makes for three MongoDB collections managed by the bundle:

- ``IGTIMI_DATA_ACCESS_WINDOWS``
- ``IGTIMI_DEVICES``
- ``IGTIMI_MESSAGES``

The server bundle's activator loads the existing devices and data access windows from the database and keeps them in memory during its life span. It launches the socket server, using ``java.nio`` selectors for good resource utilization. Devices connecting and sending an authentication request will be sent an acknowledgement. If a device yet unknown connects for the first time, a new ``Device`` entity will be created automatically, although without any data access window. Messages received from any device will be stored in the database and will be forwarded to any live web socket client that registered for messages from that device and can ``READ`` a data access window for that device spanning the event's time point.

The gateway bundle offers API methods to create, query and update the device and data access window entities, as well as for downloading sensor data and connecting with web sockets for live sensor data. These API methods are not 100% compatible with the original Riot API but have been designed sufficiently similar, achieving the goal of reducing the need for change in the existing client connector implemented by the ``com.sap.sailing.domain.igtimiadapter`` bundle.

The ``com.sap.sailing.gwt.ui`` bundle, in particular its AdminConsole entry point and in it the ``IgtimiDevicesPanel`` as well as the corresponding ``SailingService`` GWT RPC methods have been adjusted. Instead of managing the old accounts required for accessing the Igtimi-hosted Riot API it now provides a view onto the Riot server running embedded in the Sailing Analytics server that the AdminConsole holding the Igtimi panel configures. Consequently, it shows the devices and their associated data access windows and offers basic support for sending commands to the devices as well as seeing their last known heart beat time point and last known position.

### Security Aspects

Devices and data access windows are secured entities that support the usual group and user ownerships as well as access control lists and the default actions ``CREATE``, ``READ``, ``UPDATE``, and ``DELETE``. As usual, the GWT UI's server side, here in particular ``SailingService[Write]Impl``, and the REST API methods in the ``com.sap.sailing.domain.igtimiadapter.gateway`` bundle implement the necessary permission checks for the ``Device`` and ``DataAccessWindow`` entities. Furthermore, access to device data---regardless of whether live or stored or "latest"---is controlled by the permissions as follows:

- the subject needs to have the ``READ`` permission for the ``Device`` from which the data originated
- the subject needs to have ``READ`` permission on at least one ``DataAccessWindow`` for the device such that the data point's timestamp is within the data access window's time range

When a device connects to the Riot server for the first time, a ``Device`` entity is created for it. The entity is assigned the default server group as its group owner, with no user owner being set. This way, users with ``READ`` and ``UPDATE`` permissions for objects owned by the server group can deal with the device accordingly.

Enhancing security for device authentication is described in [bug 6074](https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=6074).

### Replication

The ``RiotServerImpl`` class is a ``Replicable`` object. A Sailing Analytics replica will replicate this by default. The processes in the replica set shall be registered with the Network Load Balancer (NLB) target group to which the listener for the Riot port (usually 6000) forwards. The replicated state consists of the ``Device`` and ``DataAccessWindow`` entities. Creating, updating and deleting these entities happens through replicable operations, with the usual pattern of implementing these operations as lambda expressions that comply with the ``OperationWithResult`` interface and as such are serializable. The replicable state does *not* contain the actual messages received from the devices. Those are stored in the database, and as usual, only the database of the primary/master process is to be trusted.

Messages received from a device are turned into a replicable operation invoking the ``internalNotifyListeners`` method. This way, messages from the devices are replicated across the replica set. If a message is received by the primary/master process, it is stored in the replica set's database and propagated to all replicas. While replicas will also store the message received from the primary/master in their "phony" database, the key point on the replicas is that they, like the primary/master will also forward the message to any live web socket connection that is currently known to the replica.

If a message is originally received by a replica, it will be stored in the replica's phony database and will be forwarded to the primary/master process for persistent storage in the replica set's "real" database, as well as for forwarding to all other replicas and the primary/master's live web socket connections.

REST API requests can be of two types

- those that work with the ``Device`` and ``DataAccessWindow`` entities
- those that work with stored data

The first type of requests can be handled on either a primary or a replica process. The replication architecture will ensure eventual consistency and persistence on the primary.

The second type of request will have to use the database, and that can be trusted only on the primary/master process. Therefore, when such a request hits a replica, the replica will turn around and become a REST API client itself and request the data from the primary/master, making sure to set the ``X-SAPSSE-Forward-Request-To`` HTTP header to the value ``master`` which will instruct a load balancer to send this request to the primary and not a replica. The response is then returned by the replica to its client.

Taking this together with the connection handling that uses hand shakes and automatic re-connects in case of broken connections (both, for the devices, as well as for client-issues live data web socket connections), we get a highly available set up. Should the primary/master process become unavailable, devices and web socket clients will fail over to any remaining replica. The devices will re-connect to the NLB and will get connected to one of the replicas. Likewise, live web socket connections will fail over through the regular public target group of the ``wind`` replica set and re-connect to one of the replicas. If there is exactly one replica in the replica set, this will lead to quick recovery because devices will send to the one replica to which the live web socket clients have then also failed over to. In case of multiple replicas, in the worst case all devices send to one replica, and all live web socket connections use different replicas. The replication messages on the replica receiving data from the devices will be queued until the primary/master becomes available again; only then will the data be forwarded to the other replicas.

The failure of one out of multiple replicas is less problematic. Devices and live web socket connection clients will fail over to another replica or the primary/master, and all replication will still succeed.

With this in mind, a good configuration could be one that uses a primary/master with a single replica.

### Configuration

The Riot server is configured using the ``IGTIMI_RIOT_PORT`` environment variable which, if provided, is used to set the ``igtimi.riot.port`` system property. It defines the TCP port the devices connect to in order to reach the Riot server. If a Network Load Balancer (NLB) is used, this is the port the target group must use. If not provided, an arbitrary port that is available will be used for listening for incoming connections. The port can be discovered using the ``/igtimi/api/v1/server`` REST API end point. The "well-known" port for the Igtimi devices is 6000.

## New Riot REST API

The REST API implemented in the ``com.sap.sailing.domain.igtimiadapter.gateway`` bundle offers GET, POST and DELETE methods for the ``Device`` and ``DataAccessWindow`` entities. Furthermore, there is a ``server_listers`` end point that tells clients the web socket connection URL, and a ``server`` end point for querying the Riot port number.

### Changes Compared to Original Riot REST API

Authentication of the new API works by the principles of the Sailing Analytics security infrastructure. REST and web socket requests are authenticated using the ``Authorization`` HTTP header field with a ``Bearer`` token, or basic authentication. There is no more OAuth callback because authentication works differently now. Furthermore, the API no longer supports the concept of ``Resource`` being an entity. Our application cares about devices and their data access windows only, and the API methods for requesting the data from devices for certain time ranges has not changed in its parameters. We don't need explicit resource entities for this.

When requesting stored / latest data, instead of delivering JSON documents the API now returns Base64-encoded protobuf messages that the API client needs to decode. Accordingly, our ``FixFactory`` that turns the API responses into semantic "fix" objects for wind and GPS data has changed and parses the protobuf messages now after base64 decoding.

Likewise, the web socket connections for live data will no longer see JSON text output but instead receive binary messages that contain the protobuf messages received and forwarded from the devices. The text message handling has been left in place to keep processing the heart beat messages as before. This also leaves the re-connect management unchanged.