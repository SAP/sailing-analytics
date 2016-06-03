## Setup central mail server (instance "Webserver")
- you need to add the local subnet (which the instance which should relay) is in
  - edit `/etc/postfix/main.cf` and edit `mynetworks = x.y.z.w/v` to match the subnet. Currently all subnets from VPC Ireland and US East are in here, this means adding an instance to existing subnets / zones will not require a change here
  - restart postfix with `service postfix restart`
  - check the log if everything is working with `tailf /var/log/maillog`
# Setup standalone instance for relaying
- on each Sailing App instance a local sendmail client will be running, which is in default using its own binaries to send out mails directly
- the problem here is, that external mail servers will check actually the domain "sapsailing.com" and its MX and A-Record. IF this does not match (it does not, because A/MX goes to central webserver) most correctly mailservers will deferr or even reject the incoming mail as it looks like hard spam
- to prevent this you need to relay mails send from any instance to the central postfix (with the correct A and MX records), so that other mail servers dont reject mails
  - install package `sendmail-cf` via `yum install`
  - edit the config `/etc/mail/sendmail.mc` and insert the line 
```
define(`SMART_HOST', `smtp.internalsapsailing.com')dnl
``` and save
  - regenerate the sendmail config with `. /etc/mail/make` 
  - restart sendmail with `service sendmail restart`
- test the setup by tailing both `/var/log/maillog`'s and send a mail from the relayed instance via `echo "test" | sendmail yourmail@domain.tld`

=> all this shall be included to a next HVM version 

## USSailing mail relaying
- on server dev, there are two postfix processes running
  - main process is doing the normal relaying stuff as before
  - slave process is listening externally on port `1025` to accept sasl auth users for sending mails
- add A-Record in Route53 and let point `uss.sapsailing.com` towards external IP of dev `52.17.217.83`
  - responding to incoming mails is not needed and will also not work on port 1025, as we _MUST_ use the standard 25 port then and create an corresponding MX-Record towards the A-Record
- enable multi instance mode of postfix via
  - `postmulti -e init`
  - `postmulti -I postfix-ussailing -G outgoing -e create`
  - comment out the following lines in file `/etc/postfix-ussailing/main.cf`
    ```
     #master_service_disable = inet
     #authorized_submit_users =
    ```
  - change listen port of slave postfix to something other than 25 via `/etc/postfix-ussailing/master.cf`
    - `1025      inet  n       -       n       -       -       smtpd`
  - restart posfix via `/etc/init.d/postfix restart`
- adjust configuration of slave postfix to allow sasl authenticated users to login
  - edit `/etc/postfix-ussailing/main.cf` and modify/add following lines
     - modify `myhostname = uss.sapsailing.com`, `mydomain = uss.sapsailing.com`, `myorigin = $mydomain`, `inet_interfaces = all`
     - go to the end of the config and set some parameters for authentication and alias lookup according to:
       ```
       # SUPPORT FOR CLIENT LOGIN
       alias_database = hash:/etc/aliases
       alias_maps = hash:/etc/aliases
       smtpd_sasl_auth_enable = yes
       smtpd_sasl_security_options = noanonymous
       smtpd_sasl_local_domain = $myhostname
       smtpd_use_tls = no
       broken_sasl_auth_clients = yes
       smtpd_recipient_restrictions =
          permit_sasl_authenticated,
          permit_mynetworks,
          check_relay_domains
       ```
- create a normal linux user via `useradd <name>` and give it a password via `passwd <password>`
- create a destination alias where mails shall go (currently internally only, see above) to, if someone responds to the mail <name>@<myhostname> via `/etc/aliases` and add additional destination mails comma-seperated
  ```
  # USSailing Mailing
  uss:		steffen.tobias.wagner@sap.com
  ```
- testing the setup is possible via http://smtper.nanogenesis.fr

- sources
  - http://steam.io/2013/11/05/postfix-multi-instance-configuration/
  - http://sharadchhetri.com/2013/03/06/how-to-change-smtp-port-number-25-in-postfix/
  - http://postfix.state-of-mind.de/patrick.koetter/smtpauth/smtp_auth_mailclients.html
  - https://www.df.eu/de/support/df-faq/cloudserver/anleitungen/smtp-authentifizierung-mit-postfix-debian/