# Domain Switchover

## Locations that reference sapsailing.com

- The ChargeBee subscription module used for payment services has a callback method pointing to ``sapsailing.com`` where updates to the users are processed and then replicated across the landscape.

- ``SharedLandscapeConsants.DEFAULT_DOMAIN_NAME`` is set to "sapsailing.com" but can be overridden; for things to work, however, we would need to add the deviating domain as a "hosted zone" to our Route53 account so that the DNS-maintaining logic will run without exceptions.

- The certificate used in the ALBs is created for \*.sapsailing.com; to run under a different domain name, a certificate matching the domain name will be required. The ALB set-up in AwsLandscapeImpl.createLoadBalancerHttpsListener uses the default certificate domain "\*.sapsailing.com" from a constant; this would need to be made flexible, e.g., through a system property, if we were to use any form of landscape automation under a different domain name.

- Shared security configuration as in
    env.sh:#ADDITIONAL_JAVA_ARGS="$ADDITIONAL_JAVA_ARGS -Dsecurity.sharedAcrossSubdomainsOf=sapsailing.com -Dsecurity.baseUrlForCrossDomainStorage=https://security-service.sapsailing.com -Dgwt.acceptableCrossDomainStorageRequestOriginRegexp=https?://(.\*\.)?sapsailing\.com(:[0-9]\*)?$"
    
- The ``target-group-tag-route53-nfs-elasticIP-setup.sh`` script used as the last stage of the setup process of a new central reverse proxy. It queries the Route 53 ID of the "sapsailing.com" hosted zone which would have to become a parameter or would need to be changed in the script.

- Mail sending and Amazon SES: we have Amazon WorkMail accounts, particularly for support@sapsailing.com and also a few other @sapsailing.com addresses, such as marcus.baur@sapsailing.com and jan.hamann@sapsailing.com. There are also technical accounts such as noreply@sapsailing.com. The mail address used for mail sent by the application as well as the mail server to use for sending, including its authentication parameters, can be set in the ``mail.properties`` file. The secrets for this currently come from ``root@sapsailing.com:secrets``.

- ``sapsailing.com`` is hardcoded in some of the environments files currently found under ``https://releases.sapsailing.com/environments``

- The ``refreshInstance.sh`` script contains around eight references to ``releases.sapsailing.com``. Some of these are about where to download releases from and shall be replaced by logic that obtains those releases from Github. The others are concerned with environments, as referenced by the ``USE_ENVIRONMENT`` variable and are currently expected to be found under ``https://releases.sapsailing.com/environments`` which is just a static share on our central reverse proxy. If that is configured for a different domain then so has the refreshInstance.sh script.

- Igtimi Riot Server: we run the Riot server environment as an application replica set named ``wind`` under ``wind.sapsailing.com`` which is then used as a default for new servers in landscape automation

- Google Maps API is SAP provided in our current production environment, and a different API token/key is required once SAP stops its support.

- A number of bash scripts have sapsailing.com hardcoded, especially the setup scripts and imageupgrade_functions.sh. Ideally we will need to factor out into a constant that all bash scripts can access.

- Releases and p2 respository references, where "releases.sapsailing.com" will be replaced over time by Github releases which the ``refreshInstance.sh`` script can then download. The p2 repositories for the AWS SDK combined repository, as well as our base p2 repo that forms an important part of our target platform are currently expected to be found at ``https://p2.sapsailing.com/p2/aws-sdk`` and ``https://p2.sapsailing.com/p2/sailing``, respectively. Also, we have the latest SDBG installable hosted there under ``https://p2.sapsailing.com/p2/sdbg``. The scripts under ``java/com.sap.sailing.targetplatform/scripts`` and ``java/com.sap.sailing.feature.p2build/scripts`` as well as the target platform definitions under ``java/com.sap.sailing.targetplatform/definitions`` reference those p2 repositories. Should this set of p2 repositories be migrated to a different domain, the corresponding scripts and target platform definitions need to be adjusted, and the builds including the builds with an adjusted local target platform, need to be tested thoroughly.

- MongoDB references and rabbit references, or any other private IP references in route53. Such references can in particular be found in the ``environments/`` folder on ``releases.sapsailing.com`` for the default parameterization of certain application process types, such as ``security-service-master`` or ``archive-server``.

- branch.io is used for "deep linking" for the apps. We have an integration with our Route53 hosted zone "sapsailing.com" for the specific domain names, allowing smart payload transfer through from-scratch installations, e.g., from a QR code. The branch.io account would need to be transferred and would need to have the configuration changed; or a new account would be required. The apps' deep linking capabilities are expressed in their manifests, and hence the apps would need to be adjusted to work with a different domain.

- App hosting, at least for Race Manager and Buoy Pinger apps, currently runs through the SAP SE PlayStore account. If someone else was to build and deploy these apps under their own Google PlayStore account, this would affect the deep linking and QR code generation, see above on the branch.io ramifications.