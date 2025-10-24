# Welcome to the SAP Sailing Wiki

This is the <img src="https://www.sapsailing.com/images/sap-logo_grey.png"/> Wiki where useful information regarding this project can be found.

### The Pitch

Like businesses, sailors need the latest information to make strategic decisions - but they need it even faster. One wrong tack, a false estimation of the current, or the slightest wind shift can cost the skipper the entire race. As premium sponsor of the Kieler Woche 2011, and co-sponsor of Sailing Team Germany (STG), SAP is showing how innovative IT solutions providing real time data analysis can give teams the competitive edge.

SAP is at the center of today’s technology revolution, developing innovations that not only help businesses run like never before, but also improve the lives of people everywhere. As market leader in enterprise application software, SAP SE (NYSE: SAP) helps companies of all sizes and industries run better. From back office to boardroom, warehouse to storefront, desktop to mobile device – SAP empowers people and organizations to work together more efficiently and use business insight more effectively to stay ahead of the competition. SAP applications and services enable more than 400,000 customers to operate profitably, adapt continuously, and grow sustainably.

# Table of Contents

## Information

### General

* [[Onboarding|wiki/howto/onboarding]]
* [[Information about this Wiki and HowTo|wiki/info/general/wiki]]
* [[General Project Information|wiki/info/general/general-information]]
* [[Project History|wiki/info/general/project-history]]
* [[Development Environment|wiki/info/landscape/development-environment]]

### Development

* [[Development Environment|wiki/info/landscape/development-environment]]
* [[Typical Development Scenarios|wiki/info/landscape/typical-development-scenarios]]
* [[Building and Deploying|wiki/info/landscape/building-and-deploying]]
* [[Continuous Integration with Hudson/Jenkins|wiki/howto/development/ci]]
* [[Dispatch|wiki/howto/development/dispatch]]
* [[Working with GWT UI Binder|wiki/howto/development/gwt-ui-binder]]
* [[Java De(Serialization) and Circular Dependencies|wiki/howto/development/java-de-serialization-and-circular-dependencies]]
* [[Create boat graphics for the 2D race viewer|wiki/howto/development/boatgraphicssvg]]
* [[JMX Support|wiki/howto/development/jmx]]
* [[Working with GWT Locally|wiki/howto/development/local-gwt]]
* [[UI Tests with Selenium|wiki/howto/development/selenium-ui-tests]]
* [[Profiling|wiki/howto/development/profiling]]
* [[Working with GWT Super Dev Mode|wiki/howto/development/super-dev-mode]]
* [[Training of internal Wind Estimation models|wiki/howto/windestimation]]
* [[Whitelabelling|wiki/howto/whitelabelling]]
* [[Secured Settings|wiki/howto/development/secured-settings]]
* [[Webdesign|wiki/info/landscape/webdesign]]

### Architecture and Infrastructure
  * [[Workspace, Bundles, Projects|wiki/info/general/workspace-bundles-projects-structure]]
  * [[Runtime Environment|wiki/info/landscape/runtime-environment]]
  * [[Basic architectual principles|wiki/info/landscape/basic-architectural-principles]]
  * [[User Management|wiki/info/landscape/usermanagement]]
  * [[Igtimi Windbot Riot Connector|wiki/info/landscape/igtimi-riot]]
  * [[Production Environment|wiki/info/landscape/production-environment]]
  * [[Internationalization (i18n)|wiki/howto/development/i18n]]
  * [[AI Agent|wiki/info/landscape/ai-agent]]
  * [[Malware Scanning|wiki/info/landscape/malware-scanning]]
  * [[RaceLog Tracking Server Architecture|wiki/info/landscape/server]]
  * Environment Overview [[PDF|wiki/info/mobile/event-tracking/architecture.pdf]] | [[SVG|wiki/info/mobile/event-tracking/architecture.svg]]

### Landscape

* Amazon
  * [[Amazon EC2|wiki/info/landscape/amazon-ec2]]
  * [[Upgrading ARCHIVE server|wiki/info/landscape/archive-server-upgrade]]
  * [[EC2 Backup Strategy|wiki/info/landscape/amazon-ec2-backup-strategy]]
  * [[Creating an EC2 image from scratch|wiki/info/landscape/creating-ec2-image-from-scratch]]
  * [[Upgrading an EC2 image|wiki/info/landscape/upgrading-ec2-image]]
  * [[Creating a webserver EC2 image from scratch|wiki/info/landscape/creating-ec2-image-for-webserver-from-scratch]]
  * [[Upgrading Operating System Across Landscape|wiki/info/landscape/operating-system-upgrade]]
  * [[EC2 mail relaying vs. Amazon Simple E-Mail Service (SES)|wiki/info/landscape/mail-relaying]]
  * [[Establishing support@sapsailing.com with AWS SES, SNS, and Lambda|wiki/info/landscape/support-email]]
  * [[Creating an EC2 image for a MongoDB Replica Set from scratch|wiki/info/landscape/creating-ec2-mongodb-image-from-scratch]]
  * [[Setting up dedicated S3 buckets|wiki/info/landscape/s3-bucket-setup]]
  * [[Large-Scale Set-Ups, e.g., Olympic Games|wiki/info/landscape/tokyo2020/olympic-setup]]
* [[Log File Analysis|wiki/howto/development/Log-File-Analysis]]
* [[Old Log Compression|wiki/howto/development/Log-File-Compression]]
* [[Downloading and Archiving TracTrac Events|wiki/howto/downloading-and-archiving-tractrac-events]]
* [[Data Mining Architecture|wiki/info/landscape/data-mining-architecture]]
* [[Typical Data Mining Scenarios|wiki/info/landscape/typical-data-mining-scenarios]]
* [[sail-insight.com website|wiki/info/landscape/sail-insight.com-website]]
* [[Docker Registry|wiki/info/landscape/docker-registry]]

### Mobile

* [[Mobile Development|wiki/info/mobile/mobile-development]]
* Tracking App
  * [[Tracking App Specification|wiki/info/mobile/app-spec/app-spec]]
  * [[Event Tracking|wiki/info/mobile/event-tracking/event-tracking]]
  * [[Steps for setting up Smartphone Tracking|wiki/info/mobile/smartphone-tracking-steps]]
  * [[Tracking App Prototype Architecture|wiki/info/mobile/app]]
* Racecommittee App
  * [[Racecommittee App|wiki/info/mobile/racecommittee-app]]
  * [[Environment|wiki/info/mobile/racecommittee-app-environment]]
  * [[Administrator|wiki/info/mobile/racecommittee-app-administrator]]
  * [[User|wiki/info/mobile/racecommittee-app-user]]
* [[Android and Release Build|wiki/info/mobile/android-and-release-build]]
* [[Energy consumption of mobile apps|wiki/info/mobile/energy-consumption]]
* [[Data consumption of mobile apps|wiki/info/mobile/data-consumption]]
* [[Mobile Sailing Analytics|wiki/info/mobile/mobilesailinganalytics]]
* [[Push Notifications|wiki/info/mobile/push-notifications]]
* [[NMEA|wiki/info/mobile/NMEA]]

### API

* [[Web Services API|wiki/info/api/sailing-webservices]]
* [[API v1|wiki/info/api/api-v1]]
* [[Tracking App API|wiki/info/api/tracking-app-api]]
* [[Training API v1 draft|wiki/info/api/training-api-v1-draft]]

### Security
  * [[Fortify Tests|wiki/info/security/fortify]]
  * [[SSL / HTTPS Support|wiki/info/security/ssl-support]]
  * [[Permission Concept|wiki/info/security/permission-concept]]
  * [[Permission Vertical Migration|wiki/info/security/permission-migration-tests]]

### Miscellaneous

* [[Data Quality|wiki/info/misc/data-quality]]
* [[Sailing Domain Algorithms|wiki/info/misc/sailing-domain-algorithms]]
* [[Google Analytics (Web Page Tracking)|wiki/info/misc/ganalytics]]
* [[S3 Development Sample|wiki/info/misc/s3-sample]]
* [[Subscriptions|wiki/info/misc/subscriptions]]

## HowTo

* [[Onboarding|wiki/howto/onboarding]]
* [[Managing Events with the AdminConsole|wiki/howto/adminconsoleinstructions]]
* [[Importing Sessions from Expedition|wiki/howto/expeditionimport]]
* [[Checking our DBs for a user record by e-mail|wiki/howto/privacy]]
* [[Managing ORC Polar Curve Regattas (formerly "Performance Curve Scoring")|wiki/howto/setup-orc-regatta]]
* [[Qualtrics Surveys|wiki/howto/qualtrics/qualtrics]]
* [[Paywall|wiki/howto/paywall]]
* [[Subscriptions|wiki/howto/subscriptions]]
* [[Building and Using a Forked GWT Version|wiki/howto/development/gwt-fork]]

### For Event Managers

* [[Set up local network with replication server|wiki/howto/eventmanagers/event-network-with-replica]]
* [[Operating Igtimi WindBots|wiki/howto/eventmanagers/windbot-operations]]
* [[Linking Race Videos|wiki/howto/eventmanagers/linking-race-videos]]
* [[Import official results|wiki/howto/eventmanagers/results-import]]
* [[Pairing lists|wiki/howto/eventmanagers/Pairing-Lists]]
* [[Manage media content|wiki/howto/eventmanagers/Manage-media-content]]
* [[Import Event into Archive|wiki/howto/eventmanagers/import-into-archive]]

### Setup

* [[Configure Races on Server|wiki/howto/setup/configure-races-on-server]]
* [[Setup local webserver to serve 360° videos|wiki/howto/setup/webserver/nginx-webserver]]
* [[Setting up internal Jenkins on SAP Monsoon|wiki/howto/setup/setting-up-jenkins-on-sap-monsoon]]

### Miscellaneous

* [[Cook Book|wiki/howto/misc/cook-book]]
* [[Polars|wiki/howto/misc/polars]]
* [[QR-Codes|wiki/howto/misc/qr-codes]]
* [[Wind Estimation - Core concepts|wiki/misc/windestimation-core-concepts]]
* [[Server Replication|wiki/howto/misc/server-replication]]
* [[TracTrac|wiki/howto/misc/tractrac-lifecycle]]
* [[UI Tests|wiki/howto/misc/ui-tests-tutorial]]
* [[Uploading Media Content|wiki/howto/misc/uploading-media-content]]
* [[Monitoring Apache and RabbitMQ|wiki/misc/monitoring-apache-and-rabbitmq]]

## Projects
* [[Management Console for Easier Administration|wiki/howto/development/management-console]]
* [[Cloud Infrastructure Orchestration|wiki/projects/cloud-orchestrator]]

## Events and Planning
* [[Project Planning (bigger development)|wiki/events/planning]]
* [[General Event Planning|wiki/events/general-event-planning]]
* [[Information about Extreme Sailing Series|wiki/events/extreme-sailing-series]]
* [[Travem&uuml;nder Woche 2014 event page|wiki/events/tw2014]]
* [[505 worlds Kiel 2014 event page|wiki/events/505-worlds-kiel-2014]]
* [[Kieler Woche event page|wiki/events/kieler-woche-2015]]
* [[Charleston Race Week 2016|wiki/events/Charleston-Race-Week-2016]]
* [[Sailing Leagues 2016|wiki/events/Sailing-Leagues-2016]]
* [[Media Content|wiki/events/Sailing-events-media-content]]

## Planning
* [[Overview|https://wiki.sapsailing.com/pages/wiki/planning/]]

## Internal services (not related to wiki but useful)

* [Bugzilla Issue Tracking System](https://bugzilla.sapsailing.com/bugzilla/)
* [GIT Repository (SAP)](ssh://git.wdf.sap.corp:29418/SAPSail/sapsailingcapture.git)
* [Maven Repository Browser](https://maven.sapsailing.com/maven/) (see [[how to setup repository for Android builds|wiki/info/mobile/racecommittee-app-environment]])
* [Main Sailing Website](https://www.sapsailing.com)
