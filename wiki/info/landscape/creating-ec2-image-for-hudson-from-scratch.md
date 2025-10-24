# Setting up an image for the hudson.sapsailing.com server

Like when setting up a regular sailing application server instance, start with a fresh Amazon Linux 2 image and create an instance with a 32GB root volume. Use the "Sailing Analytics Server" security group and something like a ``t3.medium`` instance type. Then, invoke the script
```
    configuration/hudson_instance_setup/setup-hudson-server.sh {external-IP-of-new-instance}
```
This will first run the regular sailing server set-up which allows the instance to run the ``dev.sapsailing.com`` Sailing Analytics instance later. Then, the script will continue to obtain the Hudson WAR file from ``https://static.sapsailing.com/hudson.war.patched-with-mail-1.6.2`` and deploy it to ``/usr/lib/hudson``, obtain and install the default system-wide Hudson configuration, get adjusted dev server secrets from ``ssh://root@sapsailing.com/root/dev-secrets`` as well as ``mail.properties``, and install a ``hudson.service`` unit under ``/etc/systemd/system``. A ``hudson`` user is created, and its ``/home/hudson`` home directory is emptied so it can act as a mount point. A latest version of the SAP Sailing Analytics is installed to ``/home/sailing/servers/DEV``. 

The ``/home/hudson/android-sdk-linux`` folder that is later expected to be mounted into the ``/home/hudson`` mount point is exported through NFS by appending a corresponding entry to ``/etc/exports``. The script will also allow the ``hudson`` user to run the ``/usr/local/bin/launchhudsonslave`` script with ``sudo``. In order to elastically scale our build / CI infrastructure, we use AWS to provide Hudson build slaves on demand. The Hudson Master (https://hudson.sapsailing.com) has a script obtained from our git at ``./configuration/launchhudsonslave`` which takes an Amazon Machine Image (AMI), launches it in our default region (eu-west-1) and connects to it. The AWS credentials are stored in the ``root`` account on ``hudson.sapsailing.com``, and the ``hudson`` user is granted access to the script by means of an ``/etc/sudoers.d`` entry.

When the script has finished, proceed as follows:

* make a volume available that holds the ``/home/hudson`` content, including the ``android-sdk-linux`` folder. This can happen, e.g., by creating a snapshot of the existing ``/home/hudson`` volume of the running "Build/Dev/Test" server, or you may search the weekly backup snapshots for the latest version to start with, or you may even stop the running Hudson, unmount and detach the /home/hudson volume and attach/mount on the new instance as long as the new instance runs in the same availability zone (AZ). Make sure to create the volume in the same availability zone (AZ) as your instance is running in.
* Based on how full the volume already is, consider re-sizing it by creating the volume larger than the snapshot.
* Attach it.
* In the instance, as ``root`` user, call ``dmesg``. This will show the new volume that just got attached, as well as its partition names.
* If you chose the volume bigger than the snapshot, use ``resize2fs`` to resize accordingly.
* Enter an ``/etc/fstab`` entry for the volume, e.g., like this:
```
UUID={the-UUID-of-the-partition-to-mount}       /home/hudson    ext4    defaults,noatime,commit=30      0 0
```
To find out ``{the-UUID-of-the-partition-to-mount}``, use ``blkid``.
* Mount with ``mount -a``
* Adjust ownerships in case the new instance's ``hudson`` user/group IDs have changed compared to the old instance:
```
  sudo chmod -R hudson:hudson /home/hudson
```
* If you'd like to keep in sync with the latest version of a still running live Hudson environment, keep copying its ``/home/hudson`` contents with ``rsync -av root@dev.internal.sapsailing.com:/home/hudson/ /home/hudson/`` until you switch
* Ensure you have EC2 / EBS snapshot backups for the volumes by tagging them as follows: ``WeeklySailingInfrastructureBackup=Yes`` for ``/`` and ``/home/hudson``.
* Then run the following to correctly configure the aws keys and ssh keys. Make sure to add the correct key to your ssh agent and use agent forwarding via the -A ssh option, as it will try to connect to root@sapsailing.com in order to access the key_vault.
```
. imageupgrade_functions.sh
setup_keys -p build_server # -p preserves existing ownerships and permissions.
```
## In-Place Start-Up

You can then either use the instance right away by starting the two essential services, as follows:
```
  sudo systemctl start hudson.service
  sudo systemctl start sailing.service
```
## Creating an AMI to Launch

Or you stop the instance, either from within using ``shutdown -h now`` or from the AWS Console, then create an image (AMI) that you can use to create a new instance at any time. Keep in mind, though, that keeping such an image around incurs cost for the relatively large ``/home/hudson`` volume's snapshot. As the volume is part of the weekly backup strategy anyhow, and due to the fair degree of automation during producing a new version of this instance type, this may not be necessary.

The AMI should be labeled accordingly, and so should the snapshots. For allowing for automated image upgrades, ensure the AMI and snapshots follow a common naming and versioning pattern. Name your AMI something like "Build/Dev/Test x.y" with x and y being major and minor version numbers, such as 2.0. Then, your snapshots shall be named "Build/Dev/Test x.y ({volume-name})" where {volume-name} can be any human-readable identifier that lets you recognize the volume. Examples for {volume-name} may be "Root" or "Home" or "HudsonHome" or similar.

Then terminate your instance used only for image creation and launch from the AMI, then, as in "In-Place Start-Up", place the instance into the "Hudson" target group and adjust the elastic IP to point to the new instance.

## Steps to Perform to Activate the New Server

* Tag the new instance with the `sailing-analytics-server` tag and value `DEV`
* Replace the old "Build/Test/Dev" instance in the "Hudson" and "S-DEV" and "S-DEV-m" target groups with the new one
* Change the ``dev.internal.sapsailing.com`` Route53 DNS entry from the old instance's internal IP address to the new instance's internal IP address

## Testing the New and Terminating the Old Instance

After a while of testing the new environment successfully you may choose to terminate the old instance. Make sure the large ``/home/hudson`` volume is deleted with it, and it not, delete it manually.