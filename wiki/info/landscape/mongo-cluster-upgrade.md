# MongoDB Cluster Upgrades

In our production environment on AWS, we currently (2026-02-10) run three MongoDB replica sets:

- ``live``: holds all databases for live operations and consists of three nodes: two i3.large instances with fast NVMe storage used for the ``/var/lib/mongo`` partition, and a hidden instance with an EBS volume that is backed up on a daily basis
- ``archive``: holds the ``winddb`` database used for the ARCHIVE server
- ``slow``: used for backing up databases when removing them from the ``live`` replica set, e.g., when shutting down an application replica set after an event

The ``archive`` and ``slow`` replica sets usually have only a single instance running on ``dbserver.internal.sapsailing.com``, and this is also where the hidden replica of the ``live`` replica set runs. The other two ``live`` nodes have internal DNS names set for them: ``mongo[01].internal.sapsailing.com``.

Upgrades may affect the packages installed on the nodes, or may affect the major version of MongoDB being run. Both upgrade procedures are described in the following two sections.

## Upgrade Using Package Manager

With Amazon Linux 2023, ``dnf`` is the package manager used. When logging on to an instance, a message like

```
A newer release of "Amazon Linux" is available.
  Version 2023.10.20260202:
Run "/usr/bin/dnf check-release-update" for full release and version update info
```

may be shown. In this case, run

```
dnf --releasever=latest upgrade
```

and watch closely what the package manager suggests. As soon as you see a kernel update about to install, displayed in red color (if your terminal supports colored output), a reboot will be required after completing the installation. This can also be checked using the following command:

```
needs-restarting -r
```

It will output a message like

```
No core libraries or services have been updated since boot-up.
Reboot should not be necessary.
```

and exits with code ``0`` if no reboot is required; otherwise, it will exit with ``1`` and display a corresponding message.

To avoid interrupting user-facing services, rebooting the MongoDB nodes shall follow a certain procedure:

- Ensure that no ARCHIVE candidate is currently launching; such a candidate would read from the ``archive`` replica set, so that rebooting the ``dbserver.internal.sapsailing.com`` node would interrupt this loading process. If an ARCHIVE candidate is launching, wait for the launch to finish.
- Ensure that no application replica set is currently being shut down with backing up its database. This backup would fail if the ``dbserver.internal.sapsailing.com`` node were restarted as it hosts the ``slow`` replica set used for the backup.
- ssh into ``ec2-user@dbserver.internal.sapsailing.com``
- There, run ``sudo dnf --releasever=latest upgrade`` and confirm with "yes"
- Assuming an update was installed that now requires a reboot, run ``sudo reboot``
- Wait until the instance is back up and running, you can ssh into it again, and ``pgrep mongod`` shows the three process IDs of the three running ``mongod`` processes
- ssh into ``ec2-user@mongo0.internal.sapsailing.com``
- run ``mongosh`` to see if ``mongo0`` is currently primary or secondary in the ``live`` replica set
- if you see "secondary", you're all set; if you see "primary", enter ``rs.stepDown()`` and see how the prompt changes from "primary" to "secondary"
- use ``quit()`` to exit the ``mongosh`` shell
- run ``sudo dnf --releasever=latest upgrade`` and confirm with "yes"
- if a reboot is required, run ``sudo reboot``
- wait for the instance and its ``mongod`` process to become available again; you may probe, e.g., by ssh-ing into the instance and checking with ``mongosh``
- repeat the process described for ``mongo0`` for ``mongo1.internal.sapsailing.com``

Hint: You can choose the order between ``mongo0`` and ``mongo1`` as you wish. If you start with the "secondary" instance, you will save one ``rs.stepDown()`` command.

## MongoDB Major Version Upgrade

Upgrading a MongoDB replica set that has more than one node can work without client noticing any interruption of service. This is in particular important for our ``live`` replica set used by all running application replica sets other than ``ARCHIVE``. For the single-node replica sets ``archive`` and ``slow`` it again comes down to timing an upgrade such that no ``ARCHIVE`` candidate launch is ongoing, and that no application replica set is currently being shut down with its database getting backed up to the ``slow`` replica set.

The [MongoDB online documentation](https://www.mongodb.com/docs/manual/release-notes/8.0-upgrade-replica-set/#std-label-8.0-upgrade-replica-set) contains a useful description of the steps necessary. The key to understanding those steps is that MongoDB replica sets can distinguish between the actual version of ``mongod`` that is running, and the "protocol version" the nodes use to talk to each other in a replica set. Newer ``mongod`` versions can always still work with the "protocol version" of the previous major release. For example, ``mongod`` in version 8 can still work with protocol version "7.0".

Therefore, upgrading a replica set with multiple nodes will work along these steps:

- Ensure all ``mongod`` processes in the replica set run the same (old) version
- Ensure all ``mongod`` processes use the protocol version that matches their own ``mongod`` version
- Upgrade the binaries and restart the ``mongod`` processes with the new version for all nodes, properly having the primary step down before restarting its process
- Set the new protocol version for the replica set

The sequence in which to work with the different nodes and processes resembles that for reboots after upgrades with the package manager. Here are the steps in detail:

- Ensure that no ARCHIVE candidate is currently launching; such a candidate would read from the ``archive`` replica set, so that rebooting the ``dbserver.internal.sapsailing.com`` node would interrupt this loading process. If an ARCHIVE candidate is launching, wait for the launch to finish.
- Ensure that no application replica set is currently being shut down with backing up its database. This backup would fail if the ``dbserver.internal.sapsailing.com`` node were restarted as it hosts the ``slow`` replica set used for the backup.
- ssh into ``ec2-user@dbserver.internal.sapsailing.com``
- Ensure all ``mongod`` processes on the host run the same (old) version, using ``mongosh`` for all three replica sets (``live``, ``archive``, ``slow``)
- In ``mongosh``, display the protocol version using ``db.adminCommand( { getParameter: 1, featureCompatibilityVersion: 1 } )``. Should you find a deviation, set the protocol version using ``db.adminCommand( { setFeatureCompatibilityVersion: "7.0" , confirm: true } )`` (of course with the "7.0" replaced by whichever protocol version you have to set this to).
- In ``/etc/yum.repos.d/`` find the ``mongodb-org.{major.minor}.repo`` file that controls where the MongoDB packages are currently obtained from. Rename the current ``mongodb-org.{major.minor}.repo`` by appending, e.g., ``.bak`` to its name and create a new ``.repo`` file for the MongoDB version you'd like to upgrade to. Then run ``dnf --releasever=latest upgrade``. This should automatically restart the ``mongod`` processes now upgraded to the new release.
- ssh into ``ec2-user@mongo0.internal.sapsailing.com``
- check ``mongod`` and protocol version using ``mongosh``; adjust protocol version if necessary (see above)
- if on the "primary", use ``rs.stepDown()`` to make it a "secondary"
- run the binaries upgrade as explained for ``dbserver.internal.sapsailing.com`` above, adjusting the ``.repo`` file under ``/etc/yum.repos.d``, followed by ``dnf --releasever=latest upgrade``
- repeat the last four steps for ``mongo1.internal.sapsailing.com``
- use ``mongosh`` to connect to the primaries of all three replica sets (``live``, ``archive``, ``slow``) and on each one issue the command ``db.adminCommand( { setFeatureCompatibilityVersion: "8.0",  confirm: true }`` with the "8.0" replaced by the protocol version you want to upgrade to, so usually the major/minor version of the binaries to which you have upgraded.

Done :-)