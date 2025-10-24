# AWS Automation

This page describes the functionality of a bash script that automates the setup of SAP Sailing Analytics instances.

## Importance

- Avoiding misconfiguration of instances due to human mistakes
- Allowing fast reaction times to external needs (e.g. horizontal scaling)
- Saving time 


## Scenarios

- SAP instance on a dedicated EC2 instance
- SAP instance on a shared EC2 instance
- SAP instance on a dedicated EC2 instance as a master
- SAP instance on a dedicated EC2 instance as a replica

## Basics 

### 1. Example setup: SAP instance on a dedicated EC2 instance

Login to the [https://aws.amazon.com/console/](AWS Web Console). 
Account number: 017363970217. 

Parameters necessary for EC2 instance: 

- Keypair 
- Instance type (e.g. t2.medium)
- Security group 
- Image
- User Data

Example of content for parameter User Data:

<pre>
MONGODB_HOST=123.123.123.123
MONGODB_PORT=27017
MONGODB_NAME=wcsantander2017 
SERVER_NAME=wcsantander2017
USE_ENVIRONMENT=live-server
INSTALL_FROM_RELEASE=build-201803302246
SERVER_STARTUP_NOTIFY=leon.radeck@sap.com
</pre>

### 2. SAP instance configuration

<img style="float: right" src="https://wiki.sapsailing.com/wiki/images/aws_automation/activity_diagram.png" />

Necessary configuration steps:

- Create event in admin console
- Create new user account with permissions for that event
- Change admin password

If instance home page or event page should be reachable by a specific URL:

Add one of the following lines to /etc/httpd/conf.d/001-events.conf:
<pre>
Use Home-SSL [instance name].sapsailing.com 127.0.0.1 8888" 
</pre> 
<pre>
Use Event-SSL [instance name].sapsailing.com “[event id]“ 127.0.0.1 8888
</pre>

Then check and reload apache configuration by entering the commands:
<pre>
apachectl configtest
sudo service httpd reload
</pre>

### 3. Load Balancer configuration

To reach the SAP instance by a specific URL (e.g. wcsantander2017.sapsailing.com), follow these steps:

- Create target group with name "S-dedicated-wcsantander2017"
- Create rule within HTTPS listener of load balancer. Enter "wcsantander2017.sapsailing.com" as a host name matching rule. Choose target group created in step one.
- Configure the health check of the target group
- Register instance within the target group

<img style="float: right" src="https://wiki.sapsailing.com/wiki/images/aws_automation/alb_overview.png" />

Translation: Way of a HTTP/HTTPS request from the load balancer to the sap instance (simplified)

### AWS Command Line Interface (AWS CLI)

Information about installation and configuration of the AWS CLI can be found here https://aws.amazon.com/de/cli/.

Example command and response of the AWS CLI to get information about all existing EC2 instances of a region:

Command: aws --region eu-west-2 ec2 describe-instances

Response: 
<pre>
{
“Reservations”: [
 {
“Instances”: [
 {
“Monitoring”: {
“State”: “disabled”
 },
“PublicDnsName”: “ec2-35-178-117-16.eu-west-2.compute.amazonaws.com”,
“State”: {
“Code”: 16,
“Name”: “running”
 },
“EbsOptimized”: false,
“LaunchTime”: “2018-04-06T04:09:29.000Z”,
“PublicIpAddress”: “35.178.117.16”,
“PrivateIpAddress”: “172.31.38.162”,
“ProductCodes”: [],
“VpcId”: “vpc-e5ba568c”,
“StateTransitionReason”: “”,
“InstanceId”: “i-066952116fe71fa65”,
“ImageId”: “ami-39f3e25d”,
“PrivateDnsName”: “ip-172-31-38-162.eu-west-2.compute.internal”,
“KeyName”: “leonradeck-keypair”,
[...]
</pre>

##### Filtering 
Limit instances that are returned by passing a parameter:

aws ec2 describe-instances _--instance-ids i-066952116fe71fa65_

##### Querying
Get value of specific attribute:

aws ec2 describe-instances --instance-ids i-066952116fe71fa65 _--query ‘Reservations[*].Instances[*].
PublicDnsName’_

aws ec2 describe-vpcs _--query ‘Vpcs[?IsDefault==`true`].VpcId’_

More syntax information can be found here http://jmespath.org/.

##### Formatting
Use text as output format:

public_dns_name=$(aws ec2 describe-instances --instance-ids i-066952116fe71fa65 --query ‘Reservations[*].
Instances[*].PublicDnsName’ _--output text_)

## Script

### Files

aws-setup.sh: 

- Parameter processing
- Sourcing of utils.sh, ~/aws-automation/confi g, ~/aws-automation/confi g-[region].sh
- Start of scenarios
- Helper methods

lib/build-config.sh:

- GitHub script to write and read user configuration variables

lib/functions_app.sh: 

- Functions that relate to SAP instance configuration 

lib/functions_ec2.sh:

-  Functions that relate to EC2 instance configuration 

lib/functions_elb.sh:

-  Functions that relate to Elastic Load Balancing

lib/functions_io.sh: 

- Input processing (optional input, passwords, value proposals, default initialization)
- Creation of user configuration files

lib/functions_wrapper.sh:

- Wrapper functions with error handling logic

lib/require_variables.sh:

- Declare input variable attributes (optional, required, password, default value, user question)

lib/scenario_associate_alb.sh:

- Associate instance with load balancer (target group, health check, rule, apache configuration)

lib/scenario_instance.sh:

- Create event
- Change admin password
- Associate with load balancer (target group, health check, rule, apache configuration)

lib/scenario_master_instance.sh:

- Create instance with correct user data to be used as master
- Create launch template for replica with matching user data to master

lib/scenario_replica_instance.sh:

- Starting of a launch template

lib/scenario_shared_instance.sh:

- Creation of a SAP instance on an existing EC2 instance
- Check preconditions
- Associate SAP instance with load balancer

lib/util_functions.sh:

- Helper functions

lib/utils.sh:

- Sourcing logic

lib/validation.sh:

- Validation functions 

lib/variables.sh:

- Variables

### Preconditions

- AWS CLI (configured)
- Cygwin with following packages: jq, openssh, wget, curl

### User input 

Every scenario requires user input for specific variables. For example the setup of a dedicated SAP instance on an EC2 instance requires the following variables:

- region
- load balancer
- instance type (e.g. t2.medium)
- security group
- image
- instance name
- instanz short name (e.g. for subdomains)
- mongodb host and port
- alternative SSH user (default: root)
- build version
- keypair name
- keypair path
- event name (optional)
- new admin password (optional)

The script is built for the processing of parameters but the functionality is currently not used.

User input flow example: 

<img style="float: right" src="https://wiki.sapsailing.com/wiki/images/aws_automation/sequence_diagram_1.png" />

Translation comment (1): 

If the parameter for the region was not passed ($region_param is empty) then the user has to do the input by himself after being prompted. The default value equals to $default_region. The value is found inside the file ~/aws-automation/config and was sourced at the start of the script. The input is required because the variable NOT_OPTIONAL was passed. The input is shown ($SHOW_INPUT).

Translation comment (2): 

The user input is assigned to the global variable region.


If the input variable is not a text but a type of resource from AWS (e.g. load balancer) the following mechanism will take effect:

<img style="float: right" src="https://wiki.sapsailing.com/wiki/images/aws_automation/sequence_diagram_2.png" />

Translation comment (1): 

Fills the variable RESOURCE_MAP with all available resources of type of the parameter. Keys are the ids and values are the names of the resources. The name of a resources equals to the value of a tag with key "name". 

Translation comment (2):

Likewise.

Translation comment (3):

Displays all resources to the user. Assigns the selected resource id to the variable load balancer.


### Error handling

To inform the user about problems during script execution, return values of some commands (e.g. aws, curl or ssh) will be tested for validity. The logic is encapsulated in the wrapper functions (e.g. aws_wrapper, curl_wrapper or ssh_wrapper) inside the file functions_wrapper.sh.

### Configuration management

At the first start of the script, the following files are created: 

- ~/aws-automation/config
- ~/aws-automation/config-[region]

The file ~/aws-automation/config contains all user variables that are region independent. 

<pre>
default_region=
default_server_startup_notify=
default_build_complete_notify=
</pre>

The file ~/aws-automation/config-[region] contains all user variables for that specific region.

<pre>
default_instance_type=
default_ssh_user=
default_key_name=
default_key_fi le=
default_mongodb_host=
default_mongodb_port=
default_new_admin_password=
</pre>

The user can assign values to that variable that are then used as default proposals.

### Scenarios

- SAP instance on a dedicated EC2 instance
- SAP instance on a shared EC2 instance
- SAP instance on a dedicated EC2 instance as a master
- SAP instance on a dedicated EC2 instance as a replica

#### SAP instance on a dedicated EC2 instance 

1. Start EC2 instance 
2. Query for its dns name for later ssh connection 
3. Wait until ssh connection is established 
4. Create event and change admin password if necessary
5. Query for https listener of load balancer
6. Create target group with name "S-dedicated-instanceshortname"
7. Configure target group health check
8. Register instance within target group
9. Create new rule within https listener that points to the correct target group
10. Append „Use Event-SSL \[domain\] \[eventId\] 127.0.0.1 8888“ or „Use Home-SSL \[domain\] 127.0.0.1 8888“ to etc/httpd/conf.d/001-events.conf

#### SAP instance on a shared EC2 instance

1. Check if folder already 
2. Check if ssh connection to super instance is working
3. Create folder within /home/sailing/servers 
4. Search for next free server port, telnet port and expedition port
5. Copy /home/sailing/code/java/target/refreshInstance.sh to the new folder
6. Execute refreshInstance.sh 
7. Comment out and in specific lines of the env.sh file
8. Append environment definition from releases.sapsailing.com/environments to env.sh 
9. Append MEMORY variable to env.sh
10. Append user data to env.sh
11. Append informaton to README file (/home/sailing/servers)
12. Start server 
13. Create event and change admin password if necessary
14. Query for https listener of the load balancer
15. Create target group with name „S-hared-instanceshortname“
16. Configuration of the target group health check with the server port of the sap instance
17. Create new rule within https listener that points to the correct target group
18. Append „Use Event-SSL \[domain\] \[eventId\] 127.0.0.1 8888“ or „Use Home-SSL \[domain\] 127.0.0.1 8888“ to etc/httpd/conf.d/001-events.conf
19. Check apache configuration with "apachectl configtest" and reload with "sudo service httpd reload“

#### SAP instance on a dedicated EC2 instance as a master

1. Start EC2 instance 
2. Query for its dns name for later ssh connection 
3. Wait until ssh connection is established
4. Create event and change admin password if necessary
5. Create launch template for replica with user data variable "REPLICATION_CHANNEL" matching to its master

#### SAP instance on a dedicated EC2 instance as a replica

1. Start launch template 
2. Check if ssh connection is working

## Improvements

- Extraction of AWS resources has to be improved 
- Show user detailed usageof a variable within the execution process of a scenario
- Execute dry-run of scenario to check if conditions are met and to avoid rollback. 

## Extensions 

- Preserve log files of instance before shutdown ([bug4503](https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=4503))
- Automatic configuration of load balancer when using a replication scenario
- Add and remove replicas dynamically ([bug4504](https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=4504))
- Usage of auto scaling groups ([bug4506](https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=4506))













