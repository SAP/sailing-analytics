The sail-insight micro site is hosted statically in `/home/trac/sail-insight-website`.

# Docker Certbot

The SSL certificate is provided by LetsEncrypt. Since `certbot` is not available on Amazon AMI Linux a docker container with symlinks to the three relevant folders is spun up to obtain the SSL certificates:

```
docker run -it --rm --name certbot -p 80:80 -v "/etc/letsencrypt:/etc/letsencrypt" -v "/var/lib/letsencrypt:/var/lib/letsencrypt" -v "/var/log/letsencrypt:/var/log/letsencrypt" -v "/home/trac/sail-insight-website/:/home/trac/sail-insight-website" certbot/certbot certonly
```

The same docker container is spun up once a week to check whether the certificate needs renewing:

```
docker run -it --rm --name certbot -v "/etc/letsencrypt:/etc/letsencrypt" -v "/var/lib/letsencrypt:/var/lib/letsencrypt" -v "/var/log/letsencrypt:/var/log/letsencrypt" -v "/home/trac/sail-insight-website/:/home/trac/sail-insight-website" certbot/certbot renew
```

The docker container should delete itself after it ran. (`--rm` flag)

The resulting certificates are placed int the folder `/etc/letsencrypt/live/sail-insight.com/`. Access to `/home/trac/sail-insight-website` is needed to verify ownership of the domain.

Certbot logs are written to `/var/log/letsencrypt/letsencrypt.log`

# Cron Job

To spin up the docker container (above) once a week to renew the SSL cert a cronjob runs the renew script as user 'certbot' once a week.

```
# Check if sail-insight.com cert needs renewing
0 1 * * Thu sudo -u certbot docker run --rm --name certbot -v "/etc/letsencrypt:/etc/letsencrypt" -v "/var/lib/letsencrypt:/var/lib/letsencrypt" -v "/var/log/letsencrypt:/var/log/letsencrypt" -v "/home/trac/sail-insight-website/:/home/trac/sail-insight-website" certbot/certbot renew && service httpd reload
```

# Apache

Apache config is kept in `/etc/httpd/conf.d/000-main.conf` and `/etc/httpd/conf.d/000-macros.conf`.


# TODO
* Move cronjob in own file in `/home/certbot` symlinked to git repo (`configuration` folder).
* Move sail-insight.com contents into git repo and provide instructions on how to build dist files. symlink into  `/home/trac/sail-insight-website`. tbd.