# Training of internal Wind Estimation models

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

In our docker registry under ``docker.sapsailing.com:443`` there is a repository called ``windestimationtraining`` where images can be found to run the training process in a mostly automated way. All you need is an account for ``docker.sapsailing.com:443`` and an account on ``sapsailing.com`` that has the ``TRACKED_RACE:EXPORT`` permission for all races in the archive server (see the ``raw-data`` role). Furthermore, you need a MongoDB with approximately 100GB of available space. This can be a MongoDB replica set, of course. All you need is the URI to establish the connection.

For your account that is equipped with the ``TRACKED_RACE:EXPORT`` permission you'll need the bearer token which you can obtain, when logged in on the web site, from [https://security-service.sapsailing.com/security/api/restsecurity/access_token](https://security-service.sapsailing.com/security/api/restsecurity/access_token). The value of the ``access_token`` attribute is what you will need in the following command:

```
   docker run --mount type=bind,source=/tmp/windEstimationModels.dat,target=/home/sailing/windEstimationModels.dat \
              -m 10g --rm -it \
              -e MONGODB_URI="mongodb://172.17.0.1/windestimation?retryWrites=true" \
              -e BEARER_TOKEN="{your-bearer-token-here}" \
              -e MEMORY=-Xmx8g \
              docker.sapsailing.com:443/windestimationtraining:latest
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
    docker build --no-cache -f Dockerfile_windestimation -t docker.sapsailing.com:443/windestimationtraining:0.0.4,docker.sapsailing.com:443/windestimationtraining:latest
```

To produce the JAR file used for the Docker image creation, run an "Export" command in Eclipse, using "File - Export - Runnable JAR File" with the ``SimpleModelsTrainingPart1`` launch configuration. This will export a JAR that you can then upload to ``trac@sapsailing.com:static`` using a command such as
```
    scp WindEstimationModelsTraining.jar trac@sapsailing.com:static
```

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