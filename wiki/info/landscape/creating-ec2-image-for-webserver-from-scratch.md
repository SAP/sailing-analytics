# Setting up an image for the www.sapsailing.com web server (For the disposables, scroll to the bottom.)
 
This is an add-on to the regular EC2 image set-up described [here](https://wiki.sapsailing.com/wiki/info/landscape/creating-ec2-image-from-scratch), but leave out the following packages during installation because they are not needed on the webserver:

* libstdc++48.i686 (for Android builds)
* glibc.i686 (for Android builds)
* libzip.i686 (for Android builds)
* telnet
* chrony (ntp is used now instead)

Then carry out these steps:

* install additional packages:
```
  yum install fail2ban git mod24_perl perl perl-CGI perl-Template-Toolkit perl-HTML-Template perl-CPAN perl-DBD-MySQL \
              mod24_ssl php71 php71-mysqlnd mod24-ldap ruby24 ruby24-devel rubygems24 rubygems24-devel icu libicu-devel \
              gcc-c++ ncurses-devel geoip-devel perl-autodie docker
```

* activate NFS by calling `systemctl enable nfs-server`; ensure that `/var/log/old` and `/home/scores` are exposed in `/etc/exports` as follows:
```
/var/log/old 172.31.0.0/16(rw,nohide,no_root_squash)
/home/scores 172.31.0.0/16(rw,nohide,no_root_squash)
```
rw = read/write allowed
nohide = means sub-mounted volumes are included in the parent nfs share
no_root_squash = Allows remote root users to act as root on the nfs share

* launch the NFS service once using `service nfs start`
* run the following command in order to obtain this feature required by Bugzilla:
```
cpan install Date::Parse Email::Address Email::Send DBI Geo::IP::PurePerl Math::Random::ISAAC IO::Socket::SSL
```
The libraries end up under `/root/perl5/lib/perl5`. For use by AWStats, read access to this path is required for the Apache web server. In particular, ensure that `/root` has read permissions for all.
* run the following commands to install missing Perl modules:
```
/usr/bin/perl install-module.pl DateTime::TimeZone
/usr/bin/perl install-module.pl Email::Sender
/usr/bin/perl install-module.pl GD
/usr/bin/perl install-module.pl Chart::Lines
/usr/bin/perl install-module.pl Template::Plugin::GD::Image
/usr/bin/perl install-module.pl GD::Text
/usr/bin/perl install-module.pl GD::Graph
/usr/bin/perl install-module.pl PatchReader
/usr/bin/perl install-module.pl Authen::Radius
/usr/bin/perl install-module.pl JSON::RPC
/usr/bin/perl install-module.pl TheSchwartz
/usr/bin/perl install-module.pl Daemon::Generic
/usr/bin/perl install-module.pl File::MimeInfo::Magic
/usr/bin/perl install-module.pl File::Copy::Recursive
```
Those modules were installed to `/root/perl5/lib/perl5` but for some reason any `SetEnv PERL5LIB` directive in the Apache configuration for the bugzilla `VirtualHost` section seemd to remain ignored. Therefore, after installing all modules required, I copied all contents of `/root/perl5/lib/perl5` to `/usr/local/share/perl5` to make them found through the `@INC` variable.
* Ensure that `/root/perl5/lib/perl5` is part of the `PERL5LIB` variable setting in the AWStats virtual host configuration in `/etc/httpd/conf.d/awstats.conf` as follows:
```
        <IfModule mod_env.c>
            SetEnv PERL5LIB /usr/share/awstats/lib:/usr/share/awstats/plugins:/root/perl5/lib/perl5
        </IfModule>
```
* make sure `/etc/alternatives/ruby` and `/etc/alternatives/gem` point to `/usr/bin/[ruby|gem]2.4`
* run the following commands to install gollum and uninstall a too current rack version 2.0.3:
```
gem install gollum
gem uninstall rack
Select gem to uninstall:
 1. rack-1.6.8
 2. rack-2.0.3
 3. All versions
> 2

You have requested to uninstall the gem:
        rack-2.0.3

sinatra-2.0.0 depends on rack (~> 2.0)
If you remove this gem, these dependencies will not be met.
Continue with Uninstall? [yN]  y
Successfully uninstalled rack-2.0.3
```
* ensure there are users and groups for `wiki`, `scores`, `trac` that match up with their /home directory owners / groups
* ensure the Wiki startup script `serve.sh` configured for port 4567 and `config.ru` as well as the entire Gollum installation under /home/wiki are present, as well as the `users.yml` file
* clone ``ssh://trac@sapsailing.com/home/trac/git`` into ``/home/wiki/gitwiki``
* ensure there is a reasonable `/root/.goaccess` file
* Configure goaccess by adjusting `/etc/goaccess.conf` such that it contains the following lines:
```
...
time-format %H:%M:%S
...
date-format %d/%b/%Y
...
# NCSA Combined with virtual host name as prefix:
log-format %v %h %^[%d:%t %^] "%r" %s %b "%R" "%u"
```
Note that the `log-format` piece is slightly different from the regular NCSA Combined Log Format in so far as it adds `%v` at the beginning which is capturing the virtual host name that our Apache servers are configured to log as the first field in each line.
* ensure there is the `/etc/tmux.conf` file that maps your hotkeys (Ctrl-a vs. Ctrl-b, for example)
* rename the `welcome.conf` file of the Apache configuration because it harms directory index presentation:
```
cd /etc/httpd/conf.d
mv welcome.conf welcome.conf.org
```
* install bugzilla to `/usr/share/bugzilla` and `/var/lib/bugzilla`
* create `/etc/bugzilla/localconfig`
* install scripts such as ``update_authorized_keys_for_landscape_managers_if_changed`` to ``/usr/local/bin``:
```
lrwxrwxrwx  1 root root       62 Jan 29  2022 awsmfalogon.sh -> /home/wiki/gitwiki/configuration/aws-automation/awsmfalogon.sh
-r-xr-xr-x  1 root root     1465 Jan 11  2018 dbilogstrip
-r-xr-xr-x  1 root root     6291 Jan 11  2018 dbiprof
-r-xr-xr-x  1 root root     5479 Jan 11  2018 dbiproxy
-rwxr-xr-x  1 root root 24707072 Jan 16  2022 docker-compose
-r-xr-xr-x  1 root root    42043 Jan 11  2018 enc2xs
-r-xr-xr-x  1 root root     3065 Jan 11  2018 encguess
-rwxr-xr-x  1 root root      640 Jan 11  2018 github-markup
-rwxr-xr-x  1 root root      598 Jan 11  2018 gollum
-rwxr-xr-x  1 root root      613 Jan 11  2018 htmldiff
-rwxr-xr-x  1 root root      610 Jan 11  2018 kramdown
-rwxr-xr-x  1 root root      607 Jan 11  2018 ldiff
-rwxr-xr-x  1 root root      352 Nov  1  2021 mail-events-on-my
-rwxr-xr-x  1 root root      610 Jan 11  2018 mustache
-rwxrwxr-x  1 trac trac    18992 Jun 16  2020 netio
-rwxr-xr-x  1 root root      610 Jan 11  2018 nokogiri
lrwxrwxrwx  1 root root       75 Oct 20 09:00 notify-operators -> /home/wiki/gitwiki/configuration/on-site-scripts/paris2024/notify-operators
-r-xr-xr-x  1 root root     8356 Jan 11  2018 piconv
-rwxr-xr-x  1 root root      648 Jan 11  2018 posix-spawn-benchmark
-rwxr-xr-x  1 root root      590 Jan 11  2018 rackup
-rwxr-xr-x  1 root root      596 Jan 11  2018 rougify
-rwxr-xr-x  1 root root      616 Jan 11  2018 ruby-prof
-rwxr-xr-x  1 root root      640 Jan 11  2018 ruby-prof-check-trace
-rwxr-xr-x  1 root root      586 Jan 11  2018 tilt
lrwxrwxrwx  1 root root       78 Feb  8  2021 update_authorized_keys_for_landscape_managers -> /home/wiki/gitwiki/configuration/update_authorized_keys_for_landscape_managers
lrwxrwxrwx  1 root root       89 Feb  8  2021 update_authorized_keys_for_landscape_managers_if_changed -> /home/wiki/gitwiki/configuration/update_authorized_keys_for_landscape_managers_if_changed
```
* set up ``crontab`` for ``root`` user (remove the symbolic link to ``/home/sailing/code/configuration/crontab`` if that had been created earlier). Note that ``configuration/crontabs`` contains a selection of crontab files for different use cases, including the ``environments/crontab-reverse-proxy-instance``, which should be pointed to by a symbolic link in /root.
```
0 10 1 * *  export PATH=/bin:/usr/bin:/usr/local/bin; mail-events-on-my >/dev/null 2>/dev/null
* * * * *   export PATH=/bin:/usr/bin:/usr/local/bin; sleep $(( $RANDOM * 60 / 32768 )); update_authorized_keys_for_landscape_managers_if_changed $( cat /root/ssh-key-reader.token ) https://security-service.sapsailing.com /root 2>&1 >>/var/log/sailing.err
0 7 2 * *   export PATH=/bin:/usr/bin:/usr/local/bin; docker exec -it registry-registry-1 registry garbage-collect /etc/docker/registry/config.yml
```
* set up crontab for user `wiki` as a symbolic link to /configuration/crontabs/users/crontab-wiki.
* ensure that ``/var/log/old/cache/docker`` makes it across from any previous installation to the new one; it contains the docker registry contents. See in particular ``/var/log/old/cache/docker/registry/docker/registry/v2/repositories``.
* [install docker registry](https://wiki.sapsailing.com/wiki/info/landscape/docker-registry) so that the following containers are up and running:
```
CONTAINER ID   IMAGE                             COMMAND                  CREATED        STATUS        PORTS                                                 NAMES
cd8086eb6361   joxit/docker-registry-ui:latest   "/docker-entrypoint.…"   6 months ago   Up 6 months   0.0.0.0:5000->80/tcp, :::5000->80/tcp                 registry-ui-1
bcf1e278ecd7   registry:latest                   "/entrypoint.sh /etc…"   6 months ago   Up 6 months   5000/tcp, 0.0.0.0:5001->5001/tcp, :::5001->5001/tcp   registry-registry-1
```
* ensure that `https://git.sapsailing.com/git` delivers the git content, with password credentials defined in `/etc/httpd/conf/passwd.git`. Sasa Zivkov (sasa.zivkov@sap.com) has been our point of contact of the SAP Gerrit group helping us with replicating our Git repository to the SAP-internal git.wdf.sap.corp one.
* comment `lbmethod_heartbeat_module` in /etc/httpd/conf.modules.d/00-proxy.conf because we don't need this sort of load balancing across origin servers and it causes a warning message in error_log
* install awstats to `/usr/share/awstats`, establish `/etc/httpd/conf/passwd.awstats`, establish a configuration under `/etc/awstats`, establish AWStats data directory under `/var/lib/awstats` and create /etc/cron.weekly/awstats as follows:
```
#!/bin/bash
su -l -c '/usr/share/awstats/tools/awstats_updateall.pl now         -configdir="/etc/awstats"         -awstatsprog="/usr/share/awstats/wwwroot/cgi-bin/awstats.pl" >>/var/log/awstats-cron.out 2>>/var/log/awstats-cron.err'
#exec /usr/share/awstats/tools/awstats_updateall.pl now         -configdir="/etc/awstats"         -awstatsprog="/usr/share/awstats/wwwroot/cgi-bin/awstats.pl" >>/var/log/awstats-cron.out 2>>/var/log/awstats-cron.err
exit 0
```
* Follow the [mail setup](https://wiki.sapsailing.com/wiki/info/landscape/mail-relaying#setup-central-mail-server-instance-webserver) instructions
* Install gollum Wiki
* Copy git contents of ssh://trac@sapsailing.com/home/trac/git to /home/trac/git
* Ensure there is a /home/scores directory with subdirectories `barbados`, `kiwo`, `sailwave`, `scores`, `velum`, and `xrrftp`.
* Check that the sail-insight.com website is hosted correctly (See [here](https://wiki.sapsailing.com/wiki/info/landscape/sail-insight.com-website))
* Establish the Apache web server configuration, in particular ensure that the SSL certificates are in place (see [here](https://wiki.sapsailing.com/wiki/info/security/ssl-support)) and the following files are set up: `/etc/httpd/conf/httpd.conf`, `/etc/httpd/conf/passwd.awstats`, `/etc/httpd/conf/passwd.git`, and `/etc/httpd/conf/conf.d/*.conf`.
* Update the hostname in `/etc/sysconfig/network`: `HOSTNAME=analytics-webserver`
* Run `chkconfig sendmail off; chkconfig postfix on` to make sure that the postfix mail server is the one that will be launched during boot
* Reboot the system, among other things for the hostname change to take effect, and in addition to see whether all services start properly
* configure fail2ban by editing `/etc/fail2ban/jail.conf`, entering reasonable e-mail configuration for the `ssh-iptables` filter as follows:
```
[ssh-iptables]

enabled  = true
filter   = sshd
action   = iptables[name=SSH, port=ssh, protocol=tcp]
           sendmail-whois[name=SSH, dest=axel.uhl@sap.com, sender=fail2ban@sapsailing.com]
logpath  = /var/log/secure
maxretry = 5
```
* Ensure that fail2ban will be started automatically when the instance starts: `chkconfig --level 23 fail2ban on` and start it right away with `service fail2ban start`. You can see which filters are active using `service fail2ban status`.
* Ensure you have EC2 / EBS snapshot backups for the volumes by tagging them as follows: ``WeeklySailingInfrastructureBackup=Yes`` for ``/var/www/static``, ``/var/log``, ``/var/log/old`` and ``/var/log/old/cache``, ``DailySailingBackup=Yes`` for ``/home``.

## Basic setup for disposable reverse proxy instance

From a fresh Amazon Linux 2023 instance (HVM), run the `configuration/environments_scripts/reverse_proxy/setup-disposable-reverse-proxy.sh` script, passing the IP address of the instance and the ssh-key-reader.token (needed for accessing the landscape without mfa).

The script sets up nfs/nvme mounts, installs/updates httpd + software for scripts, sets up the httpd, sets up crontabs and copies files (via the`configuration\environments_scripts\build-crontab-and-cp-files`), enables service units, makes the ssh connections more resilient, sets up logrotation, configures fail2ban and alters postfix to enable mail sending.

## Read Also

Check out the details in [amazon-ec2](https://wiki.sapsailing.com/wiki/info/landscape/amazon-ec2#amazon-ec2-for-sap-sailing-analytics_landscape-overview_apache-httpd-the-central-reverse-proxy-webserver-and-disposable-reverse-proxies) on the disposables and central: namely the target group healthcheck and shared httpd configuration Git repo. Also, look over the key_vault and the ``build_crontab_and_setup_files`` detailed there.