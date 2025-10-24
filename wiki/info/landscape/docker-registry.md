# Docker Registry

## `docker.sapsailing.com`

Our "legacy" self-hosted docker registry is at ``docker.sapsailing.com``. It mainly holds four repositories:

- sapsailing
- sapjvm8
- sapsailing_on_sapmachine17
- windestimationtraining

The ``sapsailing`` repository holds the images built, in particular, by the latest ``master`` branch build.
See the corresponding [Hudson Job Configuration](https://hudson.sapsailing.com/job/SAPSailingAnalytics-master/).

The files responsible for building the docker images can be found in the Git repository under
``/docker/Dockerfile.tpl`` which is a template patched with a specific release of the SAP
Sailing Analytics using the ``/docker/makeImageForLatestRelease`` script.

The Docker registry is run based on the ``registry`` and ``docker-registry-ui`` Docker images
tied together by a ``docker-compose`` file found at ``sapsailing.com:/var/log/old/cache/docker/registry/docker-compose.yml``.
The compose file launches a registry and a web UI for the registry. This works hand in hand with a corresponding
Apache httpd configuration found at ``sapsailing.com:/etc/httpd/conf.d/006-docker-registry.conf`` which maps
``docker.sapsailing.com:80`` to port 5000 on which the ``docker-registry-ui`` container is listening.

The actual Docker registry listens on port 5001 on sapsailing.com. This is additionally exposed by the Apache
reverse proxy server through ``docker-registry.sapsailing.com`` forwarding all ``/v2`` traffic to the
registry container listening on port 5001.

Both, the external UI and registry access through the Apache httpd reverse proxy require basic authentication
based on the ``/etc/httpd/conf/passwd.git`` password file. To add a user to it, use the ``htpasswd`` command
on ``sapsailing.com`` as user ``root``, e.g., as follows:
```
    htpasswd /etc/httpd/conf/passwd.git the_new_user
```
followed by entering the new user's password twice.

The actual registry configuration is found in ``sapsailing.com:/var/log/old/cache/docker/registry/registry-config.yml``.
It is mapped in the ``docker-compose.yml`` file using a corresponding volume specification.

For reference, here goes the ``docker-compose.yml`` file:
```
version: '3.7'
services:
  registry:
    image: registry:latest
    ports:
      - 5001:5001
    volumes:
      - /var/log/old/cache/docker/registry:/var/lib/registry
      - /var/log/old/cache/docker/registry/registry-config.yml:/etc/docker/registry/config.yml
    networks:
      - registry-ui-net
    restart: unless-stopped
  ui:
    image: joxit/docker-registry-ui:latest
    ports:
      - 5000:80
    environment:
      - REGISTRY_TITLE=SAP Sailing Analytics Docker Registry
      - NGINX_PROXY_PASS_URL=http://registry:5001
      # - REGISTRY_URL=http://registry:5001
      - SINGLE_REGISTRY=true
      - DELETE_IMAGES=true
    depends_on:
      - registry
    networks:
      - registry-ui-net
    restart: unless-stopped
networks:
  registry-ui-net:
```

The ``registry-config.yml`` file currently looks like this:
```
version: 0.1
log:
  fields:
    service: registry
storage:
  delete:
    enabled: true
  cache:
    blobdescriptor: inmemory
  filesystem:
    rootdirectory: /var/lib/registry
http:
  addr: :5001
  headers:
    Access-Control-Allow-Origin: ['*']
    Access-Control-Allow-Methods: ['HEAD', 'GET', 'OPTIONS', 'DELETE']
    Access-Control-Expose-Headers: ['Docker-Content-Digest']

```

The Hudson build slave AWS image (AMI) has a set of valid credentials in the ``hudson`` user's account to
push to the registry.

## Garbage-Collecting Unused Content

To run a garbage collection in the registry, try this:
```
  docker exec -it registry-registry-1 registry garbage-collect /etc/docker/registry/config.yml
```

This process is automated by adding the line

```
  0 7 2 * *       export PATH=/bin:/usr/bin:/usr/local/bin; docker exec -it registry-registry-1 registry garbage-collect /etc/docker/registry/config.yml
```

to /root/crontab and running ``crontab crontab`` as the ``root`` user. See also ``crontab -l`` for whether this has already been set up. This line can also be found in the `/configuration/crontabs/environments/crontab-application-server` file.

If you want to delete an entire repository, e.g., because you pushed images under an incorrect repository tag, try this:
```
  docker exec -it registry-registry-1 rm -rf /var/lib/registry/docker/registry/v2/repositories/{your-repository-name}
```

## `ghcr.io`

Our Github Actions-based builds produce Docker images and upload them to [https://github.com/SAP/sailing-analytics/pkgs/container/sailing-analytics](https://github.com/SAP/sailing-analytics/pkgs/container/sailing-analytics). This is currently still a private registry that may become public once the Github repository for `SAP/sailing-analytics` becomes public.
