# Creating an Amazon AWS EC2 Image from Scratch

I started out with a clean "Amazon Linux AMI 2015.03 (HVM), SSD Volume Type - ami-a10897d6" image from Amazon and added the existing Swap and Home snapshots as new volumes. The root/system volume I left as is, to start with. This requires having access to a user key that can be selected when launching the image.

Enable the EPEL repository by issuing `yum-config-manager --enable epel/x86_64`.

I then did a `yum update` and added the following packages:

 - httpd
 - mod_proxy_html
 - tmux
 - nfs-utils
 - chrony
 - apachetop
 - goaccess

Then I created a mount point /home/sailing and copied the following lines from the /etc/fstab file from an existing SL instance:

```
UUID=a1d96e53-233f-4e44-b865-c78b862df3b8       /home/sailing   ext4    defaults,noatime,commit=30      0 0
UUID=7d7e68a3-27a1-49ef-908f-a6ebadcc55bb       none    swap    sw      0       0

# Mount the Android SDK from the Build/Dev box; use a timeout of 10s (100ds)
172.31.28.17:/home/hudson/android-sdk-linux     /opt/android-sdk-linux  nfs     tcp,intr,timeo=100,retry=0
172.31.18.15:/var/log/old       /var/log/old    nfs     tcp,intr,timeo=100,retry=0
```

This will mount the swap space partition as well as the /home/sailing partition, /var/log/old and the Android SDK stuff required for local builds.

In `/etc/ssh/sshd_config` I commented the line

```
# Only allow root to run commands over ssh, no shell
#PermitRootLogin forced-commands-only
```

and added the lines

```
PermitRootLogin without-password
PermitRootLogin Yes
```

to allow root shell login.

I copied the JDK7/JDK8 installations from an existing SL instance to /opt.

I linked /etc/init.d/sailing to /home/sailing/code/configuration/sailing and added the following links to it:

```
rc0.d/K10sailing
rc1.d/K10sailing
rc2.d/S95sailing
rc3.d/S95sailing
rc4.d/S95sailing
rc5.d/S95sailing
rc6.d/K10sailing
```

Linked /etc/profile.d/sailing.sh to /home/sailing/code/configuration/sailing.sh. As this contains a PATH entry for /opt/amazon and the new image has the Amazon scripts at /opt/aws, I aldo created a symbolic link from /opt/amazon to /opt/aws to let this same path configuration find those scripts under the old and the new images.

Added the lines

```
# number of connections the firewall can track
net.ipv4.ip_conntrac_max = 131072
```

to `/etc/sysctl.conf` in order to increase the number of connections that are possible concurrently.

Added the following two lines to `/etc/security/limits.conf`:

```
*               hard    nproc           unlimited
*               hard    nofile          65000
```

This increases the maximum number of open files allowed from the default 1024 to a more appropriate 65k.

Copied the httpd configuration files `/etc/httpd/conf/httpd.conf`, `/etc/httpd/conf.d/000-macros.conf` and the skeletal `/etc/httpd/conf.d/001-events.conf` from an existing server.

Instead of having the `ANDROID_HOME` environment variable be set in `/etc/profile` as in the old instances, I moved this statement to the `sailing.sh` script in git at `configuration/sailing.sh` and linked to by `/etc/profile.d/sailing.sh`. For old instances this will set the variable redundantly, as they also have it set by a manually adjusted `/etc/profile`, but this shouldn't hurt.

Copied /etc/logrotate.conf from an existing SL instance so that `/var/log/logrotate-target` is used to rotate logs to.