# Training of internal Wind Estimation models

[[_TOC_]]

This document describes the generation process of Machine Learning (ML) models which are used internally by the maneuver-based wind estimation. It is highly recommended to process this howto step by step considering the order of sections. At the end of this howto, you will generate a file containing the representation of internal models used by ``com.sap.sailing.windestimation`` bundle. You can use this file to update the wind estimation models of a running server instance. If you are interested in a more advanced tutorial which requires all the execution steps contained in ``SimpleModelsTrainingPart...`` classes to be executed manually, then you might be interested in [Advanced Guide for training of internal Wind Estimation models](./windestimationAdvanced.md)

## Prerequisites

To complete the training process successfully, you need to make sure that you have the following stuff:

* A complete onboarding setup for SAP Sailing Analytics development
* MongoDB (**3.4 or higher!**) is up and running (can be the same MongoDB instance as required in onboarding howto)
* At least 100 GB free space on the partition where MongoDB is operating in case you're using the WiredTiger storage engine (the default for MongoDB 3.4 and upwards), otherwise at least 300GB
* Installed graphical MongoDB client such as MongoDB Compass (Community version), or you're skilled with the ``mongo`` command line tool
* 16 GB RAM
* 24+ operating hours of a computer that has a Java 8 VM installed

## Model training process

### Docker-Based

In the docker registry under ``ghcr.io/eclipse-sailing-analytics`` there is a repository called ``windestimationtraining`` where images can be found to run the training process in a mostly automated way. All you need is an account on ``sapsailing.com`` that has the ``TRACKED_RACE:EXPORT`` permission for all races in the archive server (see the ``raw-data`` role). Furthermore, you need a MongoDB with approximately 100GB of available space. This can be a MongoDB replica set, of course. All you need is the URI to establish the connection.

If you want to try the following on a plain Amazon Linux instance, try to start with an instance type that has fast SSD storage attached (NVMe) and 16GB of RAM, such as ``c5d.2xlarge``. SSH into it (probably with the ``ec2-user`` account), then try this:

```
   sudo -i
   # Launch cfdisk for the NVMe volume, give it a gpt partition table and create a single partition spanning the entire disk:
   cfdisk /dev/nvme1n1
   mkfs.xfs /dev/nvme1n1p1
   mkdir /var/lib/mongo
   mount /dev/nvme1n1p1 /var/lib/mongo
   echo "[mongodb-org-4.4]
name=MongoDB Repository
baseurl=https://repo.mongodb.org/yum/amazon/2013.03/mongodb-org/4.4/x86_64/
gpgcheck=1
enabled=1
gpgkey=https://www.mongodb.org/static/pgp/server-4.4.asc" >/etc/yum.repos.d/mongodb-org-4.4.repo
   yum update
   yum install -y mongodb-org-server mongodb-org-tools mongodb-org-shell docker
   chown mongod /var/lib/mongo
   chgrp mongod /var/lib/mongo
```
Now adjust the ``/etc/mongod.conf`` file in two places; one to restrict the cache size to limit MongoDB's memory use, and another one to expose MongoDB's default port ``27017`` also to the Docker network:

```
storage:
  dbPath: /var/lib/mongo
  journal:
    enabled: true
+ wiredTiger:
+   engineConfig:
+     cacheSizeGB: 4
...
net:
  port: 27017
- bindIp: 127.0.0.1
+ bindIp: "172.17.0.1,127.0.0.1"
```

Then continue as follows:

```
   systemctl start docker.service
   systemctl start mongod
   systemctl start docker
   touch /tmp/windEstimationModels.dat
```

For your account that is equipped with the ``TRACKED_RACE:EXPORT`` permission you'll need the bearer token which you can obtain, when logged in on the web site, from [https://security-service.sapsailing.com/security/api/restsecurity/access_token](https://security-service.sapsailing.com/security/api/restsecurity/access_token). The value of the ``access_token`` attribute is what you will need in the following command:

```
   docker run --mount type=bind,source=/tmp/windEstimationModels.dat,target=/home/sailing/windEstimationModels.dat \
              -m 12g --rm -d \
              -e MONGODB_URI="mongodb://172.17.0.1/windestimation?retryWrites=true" \
              -e BEARER_TOKEN="{your-bearer-token-here}" \
              -e MEMORY=-Xmx8g \
              ghcr.io/eclipse-sailing-analytics/windestimationtraining:latest
```
If successful (and you may want to remove the ``--rm`` option otherwise to allow you to inspect logs after unsuccessful execution) you will find the output under ``/tmp/windEstimationModels.dat`` which you can upload as usual, e.g., as in

```
    curl -X POST -H "Content-Type: application/octet-stream" --data-binary @windEstimationModels.dat \
                 -H "Authorization: Bearer 987235098w0t98yw409857098745=" \
                 https://host.sapsailing.com/windestimation/api/windestimation_data
```

### Creating the Docker Image for Model Training

Under ``docker/Dockerfile_windestimation`` there is a docker file that can be used to produce the Docker image, given that a ``WindEstimationModelsTraining.jar`` exists under [https://static.sapsailing.com/WindEstimationModelsTraining.jar](https://static.sapsailing.com/WindEstimationModelsTraining.jar). Producing an image works like this:

```
    docker build --no-cache -f Dockerfile_windestimation -t ghcr.io/eclipse-sailing-analytics/windestimationtraining:0.0.4 .
```

To produce the JAR file used for the Docker image creation, run an "Export" command in Eclipse, using "File - Export - Runnable JAR File" with the ``SimpleModelsTrainingPart1`` launch configuration with the option "Package required libraries into generated JAR" selected. This will export a JAR that you can then upload to ``trac@sapsailing.com:static`` using a command such as
```
    scp WindEstimationModelsTraining.jar trac@sapsailing.com:static
```

### AI Core

In the "Canary - Public Cloud [Feature Set B]" landscape (Europe (Frankfurt) - Canary - AWS) we have a Global Account named "SAP Sailing Analytics" which is also used for our SAP Analytics Cloud (SAP) showcase. It is configured through the SAP BTP Control Center which can be reached at [https://cp-control-client-uc2.cfapps.sap.hana.ondemand.com/index.html](https://cp-control-client-uc2.cfapps.sap.hana.ondemand.com/index.html). There, in the Entitlements section you need the "AI Cockpit," "SAP AI Launchpad," and the "SAP AI Core" elements.

When managing this account in the SAP BTP Control Center (e.g., [here](https://cockpit.sap.hana.ondemand.com/cockpit/#/globalaccount/6e49f5f8-1822-44f0-b669-9d0d5e79abc5/accountModel&//?section=SubaccountsSection&view=TilesView)) then under Entitlements these same services need to be present. In the "SAC Showcase" sub-account these entitlements then need to be configured. Afterwards, under "Instances and Subscriptions" the "SAP AI Core" and "SAP AI Launchpad" services can be subscribed to. Once the services are deployed, you'll see the "SAP AI Launchpad" subscription where you can use the "..." button to go to the application. This will take you to [https://sac4sapsailing.ai-launchpad.prodintern.eu-central-1.aws.apps.ml.hana.ondemand.com/aic/index.html#/workspaces](https://sac4sapsailing.ai-launchpad.prodintern.eu-central-1.aws.apps.ml.hana.ondemand.com/aic/index.html#/workspaces).

Obtain your API access key from the "Instances and Subscriptions" page from your "SAP AI Core" instance. See also [https://help.sap.com/docs/AI_CORE/2d6c5984063c40a59eda62f4a9135bee/7323ff4e37ba41c198b06e9669b80920.html](https://help.sap.com/docs/AI_CORE/2d6c5984063c40a59eda62f4a9135bee/7323ff4e37ba41c198b06e9669b80920.html) for a description. However, the way the descriptions there continues to explain how to set up the environment for API calls, e.g., with ``curl``, didn't work for me. Instead, I started to create a bunch of scripts that help me with obtaining ``curl``-based access, as well as establish the basic environment including Git repository access, access to a private Docker registry, an AWS S3 object store secret, a SAP Sailing Analytics bearer token secret, as well as the "Application" object that tells AI Core where to find workflow templates in the Git repository.

Create a service key using the "..." menu on your "SAP AI Core" instance and download the credentials file. This will can later be used as an argument to a little script ``aicore-credentials.sh`` that helps you set your shell variables for easy ``curl`` API access. Call like this:

```
  eval `./aicore-credentials.sh YOUR-KEY-FILE.json`
```

This will establish, in particular, a ``TOKEN`` and the ``AI_API_URL`` environment variable which can then be used by subsequent scripts and API calls which basically follow this pattern:

```
    curl --location --request POST "$AI_API_URL/v2/admin/repositories" \
         --header "Authorization: Bearer $TOKEN" \
         --header 'Content-Type: application/json' \
         ...
```

The next step is to establish a connection for the Docker registry from which Docker images are to be pulled. Log on to the registry you'd like to use, e.g., with a command such as

```
    docker login docker.sapsailing.com
```

The credentials you provide are encoded in ``~/.docker/config.json``. You can then use the script ``configureDockerRegistry.sh`` to establish the image pull secret. Either provide the name of your docker registry (e.g., ``docker.sapsailing.com`` as the single argument to the script or call without argument to have the script prompt you for it. The script will then read your ``~/.docker/config.json`` and extract the corresponding authorization information to pass it on as the AI Core registry secret.

Then, you can create the GIT connection. Use script ``createGitConfig.sh``, either with three arguments (GIT URL, user email, personal access token (PAT)), or have the script prompt for these. I have created a Git repository with the necessary workflow definition under ``https://github.tools.sap/D043530/aicore-sailing``. In the ``workflows`` folder there is a ``workflow-template.yaml`` file which contains the specification of an Argo workflow template. It runs the ``docker.sapsailing.com/windestimation:latest``.

### Traditional

1. Run ``com.sap.sailing.windestimation.model.SimpleModelsTrainingPart1`` as a normal Java Application. If you would like to run this outside of your development environment, use "Export as..." in Eclipse, pick the launch configuration for ``SimpleModelsTrainingPart1`` and let the exporter pack all required dependencies into the single executable JAR file that you can send anywhere you would like to execute it and then run ``java -jar SimpleModelsTrainingPar1.jar`` or however you called the JAR file produced by the export. After this, all the necessary maneuver and wind data will be downloaded, pre-processed and maneuver classifiers get trained. You can use the usual MongoDB system properties to configure the database connection, such as ``-Dmongo.dbName=windestimation -Dmongo.port=10202 -Dmongo.host=dbserver.internal.sapsailing.com`` or ``"-Dmongo.uri=mongodb://mongo0.internal.sapsailing.com,mongo1.internal.sapsailing.com/windestimation?replicaSet=live&retryWrites=true"``. You have to provide the VM at least 16GB of RAM. Use ``-Xms16g -Xmx16g`` as VM arguments to accomplish this. A full command line could, e.g., look like this:
```
  java -Dmongo.dbName=windestimation -Dmongo.port=10202 -Dmongo.host=dbserver.internal.sapsailing.com -Xms16g -Xmx16g -jar SimpleModelsTrainingPart1.jar
```
If you run this in a "headless" server environment, make sure the Java VM can show a dialog somewhere, e.g., on a VNC server. Example:
```
  vncserver -depth 24 -geometry 1600x900 :2
  export DISPLAY=:2.0
``` 
2. Make sure that the launched program does not get terminated by an uncaught exception. Wait until a graphical info dialog shows up which asks you to perform data cleansing for the duration dimension.

   ![Screenshot of graphical info dialog requesting to perform data cleansing for duration dimension](../images/windestimation/dialogRequestingDataCleansingForDurationDimension.jpg "Screenshot of graphical info dialog requesting to perform data cleansing for duration dimension")

   Press OK. Afterwards, a graphical window will open with two charts. The top chart is an XY-chart where the x-axis represents **seconds** and the y-axis represents various TWD delta-based measures (e.g. standard deviation or mean). Below the XY-chart, a histogram for the data points of the XY-chart is provided. You can zoom-in and zoom-out in each of the charts by mouse dragging. Be aware that currently the zoom level of both charts is not synchronizing.
   
   ![Screenshot of graphical wind data visualization tool for duration dimension](../images/windestimation/aggregatedDurationBasedTwdDeltaTransitionBeforeDataCleansing.jpg "Screenshot of duration-based TWD delta visualization tool before data cleansing")
3. Open your graphical MongoDB client and connect to the MongoDB you configured with the system properties above. Open the collection named ``aggregatedDurationTwdTransition``. Within the collection you will see all the instances/data points visualized in the previous step. The attribute used for the x-axis is ``value``. Its corresponding metrics plotted in y-axis are the other attributes. ``std`` represents standard deviation (``Sigma`` curve in XY-chart) and ``std0`` represents standard deviation with zero as mean value (``Zero mean sigma`` curve in XY-chart).

   ![Screenshot of MongoDB Compass with opened aggregatedDurationTwdTransition collection](../images/windestimation/mongoDbCompassWithOpenedAggregatedDurationTwdTransitionCollection.jpg "Screenshot of MongoDB Compass with opened aggregatedDurationTwdTransition collection")
4. Delete all the instances within the collection which do not make sense. For this, use the data visualization tool from step 2 to identify such instances. Some of the instances are not representative due to the small number of supporting instances which is visualized in the histogram. Such instances can produce unreasonable bumps in the XY-chart. The desired output of this step is that the curve ``Zero mean sigma`` looks smooth and always growing, e.g. as depicted below:
   ![Screenshot of graphical visualization tool of duration dimension after data cleansing](../images/windestimation/aggregatedDurationBasedTwdDeltaTransitionAfterDataCleansing.jpg "Screenshot of duration-based TWD delta visualization tool after data cleansing")
   
   Use the ``Refresh charts`` button as often as needed to update the charts with the modified data in MongoDB. Close the graphical visualization tool window after you are done with data cleansing to resume the training process. A confirmation dialog shows up. Confirm it by pressing *"Continue with model training"* button.
   
   ![Screenshot of confirmation dialog for finishing the data cleansing](../images/windestimation/confirmationDialogAfterDataCleansingDurationDimension.jpg "Screenshot of confirmation dialog for finishing the data cleansing")
5. A new information dialog shows up requesting you to open the source code of the class ``com.sap.sailing.windestimation.model.regressor.twdtransition.DurationBasedTwdTransitionRegressorModelContext``. Open it and scroll down to the definition of the inner enum ``DurationValueRange``. The enum defines the intervals for which a separate regressor model will be trained. Read the Javadoc of ``DurationValueRange`` and adjust the intervals accordingly in order to allow the regressor model to learn the ``Zero mean sigma`` curve with minimal error. You can also configure the polynomial which will be used as regressor function. Make sure that there are at least 2 data points contained within each configured interval. The data point with x = 0, y = 0 will be created automatically within the model training procedure. Press OK in information dialog after you are done.
6. A graphical info dialog shows up which requests you to perform data cleansing for the *distance* dimension. Press OK. All steps for data cleansing for the distance dimension are analogous to the steps of the duration dimension described from step 2. until step 5. Thus, consult these steps in order to complete the data cleansing for the distance dimension. The unit used for the distance representation is **meters**. The collection name required in step 3. is ``aggregatedDistanceTwdTransition``. The class required in step 5. is ``com.sap.sailing.windestimation.model.regressor.twdtransition.DistanceBasedTwdTransitionRegressorModelContext`` and its inner enum is ``DistanceValueRange``.
7. Run ``com.sap.sailing.windestimation.model.SimpleModelsTrainingPart2`` as a normal Java Application. Wait until the model training finishes and the program terminates normally. A new file with serialized representation of internal wind estimation models should be located in ``./windEstimationModels.dat``. The absolute path of the file must be printed in the console output of the program. You can upload the file via HTTP POST to http://sapsailing.com/windestimation/api/windestimation_data (see ``com.sap.sailing.windestimation.jaxrs.api.WindEstimationDataResource``) to update the wind estimation of a server instance. You may want to store a recent copy of the result of this training process by uploading the ``windEstimationModels.dat`` file to ``trac@sapsailing.com:static`` for later use. The file should not be stored in the git repository because it's more than 30MB in size. If you changed the source files of ``DurationValueRange`` or ``DistanceValueRange``, then you will need to update ``com.sap.sailing.windestimation`` bundle of the server instance which is meant to receive the new wind estimation models.
8. Optionally, run ``com.sap.sailing.windestimation.evaluation.WindEstimatorManeuverNumberDependentEvaluationRunner`` as a normal Java Application to evaluate the wind estimation with the new trained models. The evaluation score will be stored as CSV in ``./maneuverNumberDependentEvaluation.csv``.
9. To upload the models that resulted from the training to a server, use the following ``curl`` command, assuming you have the ``windEstimationModels.dat`` file in your current directory:
<pre>
    curl -X POST -H "Content-Type: application/octet-stream" --data-binary @windEstimationModels.dat \
                  https://username:password@host.sapsailing.com/windestimation/api/windestimation_data
</pre>
You should see a response code of 200, and a success message such as "Wind estimation models accepted".
