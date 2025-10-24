Setting up an EC2 Image for a MongoDB Replica Set from Scratch

## Image and Basic Packages

Start with an Amazon Linux 2 AMI (HVM), SSD Volume Type - ami-0fad7378adf284ce0 (64-bit x86) image.

Add the yum repository for MongoDB 3.6 by creating ``/etc/yum.repos.d/mongodb-org-3.6.repo`` as follow:

```
[mongodb-org-3.6]
name=MongoDB Repository
baseurl=https://repo.mongodb.org/yum/amazon/2013.03/mongodb-org/3.6/x86_64/
gpgcheck=1
enabled=1
gpgkey=https://www.mongodb.org/static/pgp/server-3.6.asc
```

Then:

```
yum install -y mongodb-org-server mongodb-org-mongos mongodb-org-shell mongodb-org-tools
mkfs.xfs /dev/nvme0n1
mount /dev/nvme0n1 /var/lib/mongo
```

## MongoDB Set-Up

In file ``/etc/mongod.conf`` comment the line
```
#  bindIp: 127.0.0.1  # Listen to local interface only, comment to listen on all interfaces.
```
and instead add
```
  bindIp: 0.0.0.0
```

so that MongoDB listens on all interfaces, not only localhost. Furthermore, set the ``directoryPerDB`` property to ``true`` and provide a replication set name using the ``replication.replSetName`` property. The MongoDB default would be ``rs0``. In our landscape we're using ``archive`` for the ``winddb`` database on ``dbserver.internal.sapsailing.com:10201`` and ``live`` for all live events with a hidden replica at ``dbserver.internal.sapsailing.com:10203``. 

Here is the full ``/etc/mongod.conf`` file:

```
# mongod.conf

# for documentation of all options, see:
#   http://docs.mongodb.org/manual/reference/configuration-options/

# where to write logging data.
systemLog:
  destination: file
  logAppend: true
  path: /var/log/mongodb/mongod.log

# Where and how to store data.
storage:
  dbPath: /var/lib/mongo
  journal:
    enabled: true
  directoryPerDB: true
#  engine:
#  wiredTiger:

# how the process runs
processManagement:
  fork: true  # fork and run in background
  pidFilePath: /var/run/mongodb/mongod.pid  # location of pidfile
  timeZoneInfo: /usr/share/zoneinfo

# network interfaces
net:
  port: 27017
#  bindIp: 127.0.0.1  # Listen to local interface only, comment to listen on all interfaces.
  bindIp: 0.0.0.0

#security:

#operationProfiling:

replication:
  replSetName: "live"

#sharding:

## Enterprise-Only Options

#auditLog:

#snmp:
```

To deal with the set-up of the ephemeral volume, there are a number of ``systemd`` units that can be found in the git repository at ``configuration/mongo_instance_setup``. Copy the ``*.service`` files to ``/etc/systemd/system`` and the remaining scripts to ``/usr/local/bin``.

The ``ephemeralvolume.service`` ensures that the volume exists and is formatted with the XFS file system. It looks for the volume at ``/dev/nvme0n1`` and on ``/dev/xvdb``, ensures XFS formatting and then mounts the partition to ``/var/lib/mongo``. This service uses the ``/usr/local/bin/ephemeralvolume`` script to do so. The ownerships are adjusted after that by the ``chownvarlibmongo.service`` unit to the ``mongod`` user. This is then also the last unit required to run before the ``mongod.service`` can start. After that, the replica set hook-up needs to take place. For that, the unit ``mongo-replica-set.service`` is provided, together with the two scripts ``/usr/local/bin/add-as-replica`` and ``/usr/local/bin/remove-as-replica``. The service unit is configured so that it is "wanted" by ``multi-user.target``, making it start automatically when the system comes up. It depends on ``mongod.service``, hence waits until the MongoDB process has started successfully. It then registers as a replica in a replica set. The following user data variables can be used to configure the behavior:
- ``REPLICA_SET_NAME``: the replica set name to which to subscribe; this name will also be patched into the ``/etc/mongod.conf`` config file; defaults to ``live``
- ``REPLICA_SET_PRIMARY``: the hostname and port, separated by a colon ``:``, of the replica set's primary to which to connect; defaults to ``mongo0.internal.sapsailing.com:27017``
- ``REPLICA_SET_PRIORITY``: the priority with which the new replica shall become a master; defaults to ``1`` which lets the new replica become master. Set it to ``0`` to avoid ever promoting the new replica to a primary.

Then execute ``systemctl enable mongod.service`` and ``systemctl enable mongo-replica-set.service`` to launch the MongoDB process and ensure it is always launched when the instance (re-)boots and becomes a replica in a replica set according to the user data and the defaults described above.

## Manual MongoDB Replica Set Configuration

In case you decide to skip the benefits of ``mongo-replica-set.service``, you can also establish the connection with the replica set as follows. Connect to the MongoDB on the instance. If it is the first instance of a replica set, issue the command ``rs.initiate()`` in order to turn the instance into the "seed" of a replica set. You can then, for the time being, ``quit()`` the mongo shell. Re-connecting, e.g., with ``mongo "mongodb://localhost:27017/?replicaSet=live&readPreference=nearest"``, will show the ``PRIMARY`` of the new replica set. Don't issue these commands if you only want to add another replica to an existing replica set. 

Connected to the PRIMARY using the ``mongo`` shell, a replica can be added using ``rs.add({host: "hostname:port"})``. For additional options see [here](https://docs.mongodb.com/manual/reference/method/rs.add/). To add a hidden replica on ``dbserver.internal.sapsailing.com:10203``, use ``rs.add({host: "dbserver.internal.sapsailing.com:10203", buildIndexes: false, hidden: true, priority: 0})``.

## Launch Script for a MongoDB Replica

Under ``configuration/aws-automation/launch-mongodb-replica.sh`` there is a script that can be used for quickly launching a MongoDB replica. Usage hints by calling without arguments.

This can, e.g., be used to fire up a replica for the ``winddb`` instance on ``dbserver.internal.sapsailing.com:10201`` which is configured to run on a slow, inexpensive but large disk and on a host that has only little RAM configured because the only load it has to serve may be occasional master data import activity and burst-loads during archive server re-starts. To handle the latter, an instance with more RAM and fast disk would be preferable because otherwise indices won't fit into RAM, letting MongoDB read indices from the slow disk. This lets the archive server take many days to load.

Instead, the ``launch-mongodb-replica.sh`` script can be used to fire up a replica with a very fast NMVe SSD attached to the instance. A good instance type for this may be ``i2.xlarge`` which has 30GB of RAM, 4 vCPUs and 800GB of NVMe storage. Launch like this:

``configuration/aws-automation/launch-mongodb-replica.sh -r archive -p dbserver.internal.sapsailing.com:10201 -P 0 -t i2.xlarge -k <your-aws-key-pair-name>``

You can then check the replication status by connecting your ``mongo`` client to ``dbserver.internal.sapsailing.com:10201`` and check the output of ``rs.status()``. As long as the new instance is listed in status ``STARTUP2`` it is still synchronizing from the primary. Eventually (took 55min during the last test) it transitions to status ``SECONDARY``. You can then use a connection string such as ``mongodb://dbserver.internal.sapsailing.com:10201/winddb?replicaSet=archive&retryWrites=true&readPreference=secondary`` for the ``MONGODB_URI`` in your archive server Java instance which will then preferably connect to your new fast secondary replica. Consider that the new secondary replica will have to build indices when first queried. This can take another 20 minutes. When done loading the archive server, terminate the instance which will properly remove the replica from the replica set.
