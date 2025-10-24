# Thoughts on Landscape Configuration for Paris 2024 / Marseille

As a baseline we'll use the [Olympic Setup From Tokyo 2020](/wiki/info/landscape/tokyo2020/olympic-setup). The major change, though, would be that instead of running a local on-site master and a local on-site replica we would run two master instances locally on site where one is the "shadow" and the other one is the "production" master.

We captured a set of scripts and configuration files in out Git repository at ``configuration/on-site-scripts``, in particular also separately for the two laptops, in ``configuration/on-site-scripts/sap-p1-1`` and ``configuration/on-site-scripts/sap-p1-2``.

Many of these scripts and configuration files contain an explicit reference to the replica set name (and therefore sub-domain name, DB name, tag values, etc.) ``tokyo2020``. With the test event up in July 2023 and the Paris Olympic Summer Games 2024 we should consider making this a parameter of these scripts so it is easy to adjust. We will need different sub-domains for the test event and the Games where the latter most likely will have ``paris2024.sapsailing.com`` as its domain name and hence ``paris2024`` as the replica set name.

## VPCs and VPC Peering

From Tokyo2020 we still have the VPCs around in five regions (``eu-west-3``, ``us-west-1``, ``us-east-1``, ``ap-northeast-1``, and ``ap-southeast-2``). They were named ``Tokyo2020`` and our scripts currently depend on this. But VPCs can easily be renamed, and with that we may save a lot of work regarding re-peering those VPCs. We will, though need routes to the new "primary" VPC ``eu-west-3`` from everywhere because the ``paris-ssh.sapsailing.com`` jump host will be based there. Note the inconsistency in capitalization: for the VPC name and as part of instance names such as ``SL Tokyo2020 (Upgrade Replica)`` we use ``Tokyo2020``, for basically everything else it's ``tokyo2020`` (lowercase). When switching to a parameterized approach we should probably harmonize this and use the lowercase name consistently throughout.

I've started with re-naming the VPCs and their routing tables from ``Tokyo2020`` to ``Paris2024``. I've also added VPC peering between Paris (``eu-west-3``) and California (``us-west-1``), Virginia (``us-east-1``), and Sydney (``ap-southeast-2``). The peering between Paris and Tokyo (``ap-northeast-1``) already existed because for Tokyo 2020, Paris hosted replicas that needed to access the jump host in the Tokyo region.

I've also copied the "SAP Sailing Analytics 1.150" image to all five regions.

## Master and Shadow Master

We will use one laptop as production master, the other as "shadow master." The reason for not using a master and a local replica is that if the local master fails, re-starting later in the event can cause significant delays until all races have loaded and replicated again.

Both laptops shall run their local RabbitMQ instance. Each of the two master processes can optionally write into its local RabbitMQ through an SSH tunnel which may instead redirect to the cloud-based RabbitMQ for an active Internet/Cloud connection.

This will require to set up two MongoDB databases (not separate processes, just different DB names), e.g., "paris2024" and "paris2024-shadow". Note that for the shadow master this means that the DB name does not follow the typical naming convention where the ``SERVER_NAME`` property ("paris2024" for both, the primary and the shadow master) also is used as the default MongoDB database name.

Note: The shadow master must have at least one registered replica because otherwise it would not send any operations into the RabbitMQ replication channel. This can be a challenge for a shadow master that has never seen any replica. We could, for example, simulate a replica registration when the shadow master is still basically empty, using, e.g., a CURL request and then ignoring and later deleting the initial load queue on the local RabbitMQ.

Furthermore, the shadow master must not send into the production RabbitMQ replication channel that is used by the production master instance while it is not in production itself, because it would duplicate the operations sent. Instead, the shadow master shall use a local RabbitMQ instance to which an SSH tunnel forwards.

We will install a cron job that regularly performs a "compareServers" between production and shadow master. Any deviation shall be notified using the e-mail notification mechanism in place for all other alerts and monitoring activities, too.

## Cloud RabbitMQ

We will use ``rabbit-eu-west-3.sapsailing.com`` pointing to the internal IP address of the RabbitMQ installation in ``eu-west-3`` that is used as the default for the on-site master processes as well as for all cloud replicas.

## ALB and Target Group Set-Up

Like for Tokyo2020, a separate ALB for the Paris2024 event will be set up in each of the regions supported. They will all be registered with the Global Accelerator to whose anycast-IP adresses the DNS alias record for ``paris2024.sapsailing.com`` will point. Different from Tokyo2020 where we used a static "404 - Not Found" rule as the default rule for all of these ALBs, we can and should use an IP-based target group for the default rule's forwarding and should registed the ``eu-west-1`` "Webserver" (Central Reverse Proxy)'s internal IP address in these target groups. This way, when archiving the event, cached DNS records can still resolve to the Global Accelerator and from there to the ALB(s) and from there, via these default rules, back to the central reverse proxy which then should now where to find the ``paris2024.sapsailing.com`` content in the archive.

Target group naming conventions have changed slightly since Tokyo2020: instead of ``S-ded-tokyo2020`` we will use only ``S-paris2024`` for the public target group containing all the cloud replicas.

## Cloud Replica Set-Up

Based on the cloud replica set-up for Tokyo2020 we can derive the following user data for Paris2024 cloud replicas:

```
INSTALL_FROM_RELEASE=build-.............
SERVER_NAME=paris2024
MONGODB_URI="mongodb://localhost/paris2024-replica?replicaSet=replica&retryWrites=true&readPreference=nearest"
USE_ENVIRONMENT=live-replica-server
REPLICATION_CHANNEL=paris2024-replica
REPLICATION_HOST=rabbit-eu-west-3.sapsailing.com
REPLICATE_MASTER_SERVLET_HOST=paris-ssh.internal.sapsailing.com
REPLICATE_MASTER_SERVLET_PORT=8888
REPLICATE_MASTER_EXCHANGE_NAME=paris2024
REPLICATE_MASTER_QUEUE_HOST=rabbit-eu-west-3.sapsailing.com
REPLICATE_MASTER_BEARER_TOKEN="***"
ADDITIONAL_JAVA_ARGS="${ADDITIONAL_JAVA_ARGS} -Dcom.sap.sse.debranding=true"
```

Make sure to align the ``INSTALL_FROM_RELEASE`` parameter to match up with the release used on site.

## SSH Tunnels

The baseline is again the Tokyo 2020 set-up. Besides the jump host's re-naming from ``tokyo-ssh.sapsailing.com`` to ``paris-ssh.sapsailing.com``. The tunnel scripts for ``sap-p1-2`` that assume ``sap-p1-2`` is (primary) master seem to be faulty. At least, they don't establish a reverse port forward for port 8888 which, however, seems necessary to let cloud replicas reach the on-site master. ``sap-p1-2`` becoming (primary) on-site master means that ``sap-p1-1`` has failed. This can be a problem with the application process but could even be a hardware issue where the entire machine has crashed and has become unavailable. Therefore, ``sap-p1-2`` must take over at least the application and become primary master, and this requires the reverse port forward like this: ``-R '*:8888:localhost:8888'``

The ports and their semantics:

*   443: HTTPS port of security-service.sapsailing.com (or its local replacement through NGINX)
*  5673: Outbound RabbitMQ to use by on-site master (regularly to RabbitMQ in eu-west-3, local replacement as fallback)
*  5675: Inbound RabbitMQ (rabbit.internal.sapsailing.com) for replication from security-service.sapsailing.com (or local replacement)
*  9443: NGINX HTTP port on sap-p1-1 (also reverse-forwarded from paris-ssh.sapsailing.com)
*  9444: NGINX HTTP port on sap-p1-2 (also reverse-forwarded from paris-ssh.sapsailing.com)
* 10201: MongoDB on sap-p1-1
* 10202: MongoDB on sap-p1-2
* 10203: MongoDB on paris-ssh.sapsailing.com
* 15673: HTTP to RabbitMQ administration UI of the RabbitMQ server reached on port 5673
* 15675: HTTP to RabbitMQ administration UI of the RabbitMQ server reached on port 5675
* 22222: SSH access to sapsailing.com:22, e.g., for Git access through ``ssh://trac@localhost:22222/home/trac/git``
* 22443: HTTPS access to sapsailing.com:443, e.g., for trying to download a release, although chances are slim this works without local ``/etc/hosts`` magic, e.g., for ``releases.sapsailing.com``

``/etc/hosts`` must map ``security-service.sapsailing.com`` to ``localhost`` so that local port 443 can be forwarded to different targets based on needs.

### Regular Operations

* Three MongoDB nodes form the ``paris2024`` replica set: ``sap-p1-1:10201``, ``sap-p1-2:10202``, and ``paris-ssh.sapsailing.com:10203``, where SSH tunnels forward ports 10201..10203 such that everywhere on the three hosts involved the replica set can be addressed as ``mongodb://localhost:10201,localhost:10202,localhost:10203/?replicaSet=paris2024&retryWrites=true&readPreference=nearest``
* ``sap-p1-1`` runs the ``paris2024`` production master from ``/home/sailing/servers/paris2024`` against local database ``paris2024:paris2024``, replicating from ``security-service.sapsailing.com`` through SSH tunnel from local port 443 pointing to ``security-service.sapsailing.com`` (which actually forwards to the ALB hosting the rules for ``security-service.sapsailing.com`` and RabbitMQ ``rabbit.internal.sapsailing.com`` tunneled through port 5675, with the RabbitMQ admin UI tunneled through port 15675; outbound replication goes to local port 5673 which tunnels to ``rabbit-eu-west-3.sapsailing.com`` whose admin UI is reached through port 15673 which tunnels to ``rabbit-eu-west-3.sapsailing.com:15672``
* ``sap-p1-2`` runs the ``paris2024`` shadow master from ``/home/sailing/servers/paris2024`` against local database ``paris2024:paris2024-shadow``, replicating from ``security-service.sapsailing.com`` through SSH tunnel from local port 443 pointing to ``security-service.sapsailing.com`` (which actually forwards to the ALB hosting the rules for ``security-service.sapsailing.com`` and RabbitMQ ``rabbit.internal.sapsailing.com`` tunneled through port 5675, with the RabbitMQ admin UI tunneled through port 15675; outbound replication goes to local port 5673 which tunnels to the RabbitMQ running locally on ``sap-p1-2``, port 5672 whose admin UI is then reached through port 15673 which tunnels to ``sap-p1-2:15672``
* The database ``mongodb://mongo0.internal.sapsailing.com,mongo1.internal.sapsailing.com/security_service?replicaSet=live`` is backed up on a regular basis (nightly) to the local MongoDB replica set ``paris2024`` DB named ``security_service`` which makes it visible especially in the two MongoDB replicas running on ``sap-p1-1`` and ``sap-p1-2``

### Production Master Failure

Situation: production master fails, e.g., because of a Java VM crash or a deadlock or user issues such as killing the wrong process...

Approach: Switch to previous shadow master on ``sap-p1-2``, re-configuring all SSH tunnels accordingly; this includes the 8888 reverse forward from the cloud to the local on-site master, as well as the RabbitMQ forward which needs to switch from the local RabbitMQ running on the shadow master's host to the cloud-based RabbitMQ. Clients such as SwissTiming clients need to switch to the shadow master. To remedy gaps in replication due to the SSH tunnel switch we may want to circulate the replica instances, rolling over to a new set of replicas that fetch a new initial load. If ``sap-p1-1``'s operating system is still alive, its SSH tunnel especially for port 8888 reverse forwarding from ``paris-ssh.sapsailing.com`` must be terminated because otherwise ``sap-p1-2`` may not be able to establish its according reverse forward of port 8888.

Here are the major changes:

* ``sap-p1-2`` runs the ``paris2024`` shadow master from ``/home/sailing/servers/paris2024`` against local database ``paris2024:paris2024-shadow``, replicating from ``security-service.sapsailing.com`` through SSH tunnel from local port 443 pointing to ``security-service.sapsailing.com`` (which actually forwards to the ALB hosting the rules for ``security-service.sapsailing.com`` and RabbitMQ ``rabbit.internal.sapsailing.com`` tunneled through port 5675, with the RabbitMQ admin UI tunneled through port 15675; *outbound replication goes to local port 5673 which tunnels to* ``rabbit-eu-west-3.sapsailing.com`` *whose admin UI is reached through port 15673 which tunnels to* ``rabbit-eu-west-3.sapsailing.com:15672``

### Internet Failure

While cloud replicas and hence the ALBs and Global Accelerator will remain reachable with the latest data snapshot at the time the connection is lost, we will then lose the following capabilities:

* replicate the official ``security-service.sapsailing.com`` service, both, from an HTTP as well as a RabbitMQ perspective; ``rabbit.internal.sapsailing.com`` will then no longer be reachable from the on-site network
* keep the cloud MongoDB instance on ``paris-ssh.sapsailing.com`` synchronized; it will fall behind
* outbound replication to ``rabbit-eu-west-3.sapsailing.com`` and from there on to the cloud replicas in all regions supported will stop
* inbound "reverse" replication from the cloud replicas to the on-site master through the reverse forward of ``paris-ssh.sapsailing.com:8888`` will stop working; the cloud replicas will start buffering the operations to send to their master and will keep re-trying in growing time intervals

To recover with as little disruption as possible, switching to a local copy of the ``security-service`` and to a local RabbitMQ for "outbound" replication is required. Of course, no replicas will be listening on that local RabbitMQ, but in order to not stop working, the application server will need a RabbitMQ that can be reached on the outbound port 5673. This is achieved by switching the SSH tunnel such that port 5673 will then forward to a RabbitMQ running locally.

We will then start ``sap-p1-1:/home/sailing/servers/security_service`` on port 8889 which will connect to the local MongoDB replica set still consisting of the two on-site nodes, using the database ``security_service`` that has been obtained as a copy of the ``live`` MongoDB replica set in our default region. This local security service uses the local RabbitMQ running on the same host for its outbound replication. On both on-site laptops the port 443 then needs to forward to the NGINX instance running locally as a reverse proxy for the local security service. On ``sap-p1-1`` this is port 9443, on ``sap-p1-2`` this is port 9444. Furthermore, the port forward from port 5675 and 15675 on both laptops then must point to the local RabbitMQ used outbound by the security service running locally. This will usually be the RabbitMQ running on ``sap-p1-1``, so ``sap-p1-1:5672``, or ``sap-p1-1:15672``, respectively, for the admin port.

This makes for the following set-up:

* Only two MongoDB nodes remain available on site from the ``paris2024`` replica set: ``sap-p1-1:10201`` and ``sap-p1-2:10202``, where SSH tunnels forward ports 10201..10203 such that everywhere on the three hosts involved the replica set can be addressed as ``mongodb://localhost:10201,localhost:10202,localhost:10203/?replicaSet=paris2024&retryWrites=true&readPreference=nearest``
* ``sap-p1-1`` runs the ``paris2024`` production master from ``/home/sailing/servers/paris2024`` against local database ``paris2024:paris2024``, replicating from ``security-service.sapsailing.com`` through SSH tunnel from local port 443 pointing to ``sap-p1-1:9443`` which is the port of the local NGINX acting as an SSL-offloading reverse proxy for the security service running locally on port 8889; port 5675 forwards to ``sap-p1-1:5672`` where the local RabbitMQ runs, with the local ``sap-p1-1`` RabbitMQ admin UI tunneled through port 15675; outbound replication goes to local port 5673 which then also tunnels to the local RabbitMQ on ``sap-p1-1:5672``, whose admin UI is reached through port 15673 which tunnels to ``sap-p1-1:15672``
* ``sap-p1-2`` runs the ``paris2024`` shadow master from ``/home/sailing/servers/paris2024`` against local database ``paris2024:paris2024-shadow``, replicating from ``security-service.sapsailing.com`` through SSH tunnel from local port 443 pointing to ``sap-p1-1:9443`` which is the reverse proxy for the security service running on ``sap-p1-1:8889``, and RabbitMQ tunneled through port 5675 to ``sap-p1-1:5672``, with the RabbitMQ admin UI tunneled through port 15675 to ``sap-p1-1:15672``; outbound replication still goes to local port 5673 which tunnels to the RabbitMQ running locally on ``sap-p1-2``, port 5672 whose admin UI is then reached through port 15673 which tunnels to ``sap-p1-2:15672`` which keeps the shadow master's outbound replication from interfering with the production master's outbound replication.

### Internet Failure Using Shadow Master

TODO

## Checklist After Event

The experience during "Tokyo 2020" has shown that after the last race of the last day everybody gets in a rush, and the on-site infrastructure starts to get dismantled quickly. For us this means that we need to prepare well for switching to cloud-only operations. The approach in Enoshima worked well, although we were caught a bit by surprise regarding the speed at which infrastructure was taken down.

### Cleanly Remove On-Site MongoDB Replicas from ``paris2024`` MongoDB Replica Set

Connecting to the ``paris2024`` MongoDB replica set, first we need to make sure that the cloud replica can become primary. The production configuration was such that by assigning a priority and votes of 0 the cloud replica never would become primary. Now it shall, so we need to change its priority and votes value in the configuration first. For this, issue the following command in the MongoDB shell while connected to the ``paris2024`` replica set:

```
  cfg=rs.config()
```

Then find the member using port number ``10203`` which is the cloud replica. Typically, this would be the first element (index 0) in the ``members`` array of the ``cfg`` object. Assuming it *is* at index 0, issue the following commands (replacing the 0 index by the actual index of the ``10203`` port member):

```
  cfg.members[0].priority=1
  cfg.members[0].votes=1
  rs.reconfig()
  rs.remove("localhost:10201")
  rs.remove("localhost:10202")
```

This will make the MongoDB cloud replica running on ``paris-ssh.sapsailing.com`` the single primary of the now single-element replica set. The MongoDB processes running on the on-site laptops can then be stopped.

### Stop Replication in Cloud Replicas

Then, all cloud replicas need to stop replicating because soon the on-site master will be stopped. See script ``configuration/on-site-scripts/paris2024/stop-all-cloud-replicas.sh``.

### Stop On-Site Master and Launch Cloud Master on ``paris-ssh.sapsailing.com``

Next, an application master for the ``paris2024`` application replica set needs to be launched on ``paris-ssh.sapsailing.com``. It uses the MongoDB URI ``mongodb://localhost:10203/paris2024?replicaSet=paris2024&retryWrites=true&readPreference=nearest``, hence connecting to the single-instance MongoDB "replica set" running on the same host. Other than this the instance uses a standard configuration for a live master. This configuration can already be prepared before the event. All that then needs to be done is to adjust the release to the one that all cloud replicas are using.

## Test Plan for Test Event Marseille July 2023

### Test Internet Failure

We shall emulate the lack of a working Internet connection and practice and test the procedures for switching to a local security-service.sapsailing.com installation as well as a local RabbitMQ standing in for the RabbitMQ deployed in the cloud.

### Test Primary Master Hardware Failure

This will require switching entirely to the shadow master. Depending on the state of the reverse port forward of the 8888 HTTP port from the cloud we may or may not have to try to terminate a hanging connection in order to be able to establish a new reverse port forward pointing from the cloud to the shadow master. The shadow master also then needs to use the cloud-based RabbitMQ instead of its local one. As a fine-tuning, we can practice the rolling re-sync of all cloud replicas which will likely have missed operations in the meantime.

### Test Primary Master Java VM Failure

This can be caused by a deadlock, VM crash, Full GC phase, massive performance degradation or other faulty behavior. We then need to actively close the reverse SSH port forward from the cloud to the production master's 8888 HTTP port, as a precaution switch the RabbitMQ tunnel from the cloud-based to the local RabbitMQ instance so that in case the production master "wakes up" again, e.g., after a Full GC, it does not start to interfere with the now active shadow master on the RabbitMQ fan-out exchange. On the shadow master we need to re-configure the SSH tunnels, particularly to target the cloud-based RabbitMQ and have the reverse port forward on port 8888 target the shadow master on site now.

### Test Primary Master Failures with no Internet Connection

Combine the above scenarios: a failing production master (hardware or VM-only) will require different tunnel re-configurations, especially regarding the then local security-service.sapsailing.com environment which may need to move to the shadow laptop.

## TODO Before / During On-Site Set-Up (Both, Test Event and OSG2024)

* Set up Global Accelerator and have the already established DNS record ``paris2024.sapsailing.com`` (placeholder that points to the Dynamic ALB in the default region ``eu-west-1`` to effectively forward to the central reverse proxy and ultimately the archive server's landing page) become an alias pointing to this Global Accelerator
* Set up logging buckets for ALBs in all supported regions
* Set up ALBs in all supported regions, define their three rules (redirect for ``paris2024.sapsailing.com/`` path; forward to public target group for all other ``paris2024.sapsailing.com`` traffic; default rule forwarding to IP-based target group containing the ``eu-west-1`` central reverse proxy) and register them with the Global Accelerator
* Add SSH public keys for password-less private keys of ``sap-p1-1`` and ``sap-p1-2`` to ``ec2-user@paris-ssh.sapsailing.com:.ssh/authorized_keys.org`` so that when the authorized_keys file is updated automatically, the on-site keys are still preserved.
* Create LetsEncrypt certificates for the NGINX installations for paris2024.sapsailing.com and security-service.sapsailing.com and install to the two on-site laptops' NGINX environments
* Ensure the MongoDB installations on both laptops use the ``paris2024`` replica set
* Adjust Athena queries to include all ALB logging buckets from all regions

## Other TODOs, Open Questions

* master set-up on sap-p1-2 must be configured in "failover" mode by default; this means it sends to the local RabbitMQ and the security_service MongoDB replica set that does not replicate into the cloud, to keep traffic on the SSH tunnel to the cloud as low as possible
* create a "primary master" configuration on sap-p1-2 in case sap-p1-1 fails for a longer time and we need to switch to sap-p1-2 for a longer time; in that case we would like to have DB replication into the cloud, so use the localhost:[10201|10202|10203] "paris2024" MongoDB replica set and send to the RabbitMQ in the cloud (rabbit-eu-west-3); these failover scenarios should be manageable by corresponding scripts
* Shall we obtain the tunnel scripts via symbolic links from the respective git repo at /home/sailing/code, there then under configuration/on-site-scripts/paris2024/sap-p1-[12]? Currently, they are copies that evolve independently from the git repo.

## Replacing an Access Token Accidentally Revoked in security-service DB

SSH into the security-service instance, then run

```
  mongo "mongodb://dbserver.internal.sapsailing.com:10203/security_service?replicaSet=live"
  > db.PREFERENCES.update({"USERNAME": "username", "KEYS_AND_VALUES.KEY": "___access_token___"}, { $set: { 'KEYS_AND_VALUES.$': { "KEY" : "___access_token___", "VALUE" : "asfdasdfasdfasfdasfdasdfsadfasfdasdfdsaf=" } } })
  > quit()
```