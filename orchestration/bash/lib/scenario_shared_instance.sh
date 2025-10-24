#!/usr/bin/env bash

# Scenario for creating a shared instance
# ------------------------------------------------------

function shared_instance_start(){
	shared_instance_require
	shared_instance_check_preconditions
	shared_instance_execute
}

# -----------------------------------------------------------
# All these variables are needed for this scenario
# If one variable is not assigned or passed by parameter
# the user will be prompted to enter a value
# -----------------------------------------------------------
function shared_instance_require(){
	require_super_instance
	require_load_balancer

	require_instance_name
	require_instance_short_name
	require_mongodb_host
	require_mongodb_port
	require_build_version
	require_key_file
	require_event_name
	require_new_admin_password
	require_ssh_user
	require_contact_person
	require_contact_email
	require_description

	disable_aws_success_output
	local instance_id=$(get_resource_id $super_instance)
	super_instance=$(get_public_dns_name $instance_id)
	enable_aws_success_output
}

function shared_instance_check_preconditions(){
	sailing_dir="/home/sailing"
	servers_dir="$sailing_dir/servers"
	server_dir="$servers_dir/$instance_short_name"

	header "Checking preconditions"

	local instance_id=$(get_instance_id $super_instance)

	# echo "Checking if instance has tag: 'ssh_user'."
	# ssh_user=$(exit_on_fail get_tag_value_for_key $instance_id "ssh_user")

	echo "Checking if directory $server_dir exists already..."
	exit_on_fail execute_remote "[ ! -d $server_dir ]"

	echo "Checking if ssh connection $ssh_user@$super_instance is working..."
	exit_on_fail execute_remote ls

	# echo "Checking if super instance has user data.\
	# The instance should not have any user data set, otherwise the refreshInstance script will not work properly."
	# exit_on_fail [ $(get_user_data_from_instance $instance_id) == "None" ]
}

function shared_instance_execute() {
	local server_env_file="$server_dir/env.sh"
	local readme_file="$servers_dir/README"
	local comment_out_line_in_env_with_pattern='JAVA_HOME=\/opt\/jdk1.8.0_20'
	local comment_in_line_in_env_with_pattern='sapjvm_gc'
	local refreshInstance_file='/usr/local/bin/refreshInstance.sh'

	header "Sub instance setup"

	# create sub instance server directory
	local_echo "Creating directory $server_dir ..."
	exit_on_fail execute_remote mkdir $server_dir

	local_echo "Getting next unused SERVER_PORT..."
	local server_port=$(find_first_unused_port "SERVER_PORT" "8888")

	local_echo "Getting next unused TELNET_PORT..."
	local telnet_port=$(find_first_unused_port "TELNET_PORT" "14900")

	local_echo "Getting next unused EXPEDITION_PORT..."
	local expedition_port=$(find_first_unused_port "EXPEDITION_PORT" "2000")

	# copy refreshInstance.sh from /servers/server to sub instance directory
	local_echo "Copying $refreshInstance_file to $server_dir..."
	exit_on_fail execute_remote cp $refreshInstance_file $server_dir

	# Execute refreshInstance.sh
	local_echo "Executing refreshInstance.sh with build version $build_version ..."
	exit_on_fail execute_remote "set -o pipefail;export DEPLOY_TO=$instance_short_name;cd $server_dir;./refreshInstance.sh install-release $build_version > /dev/null 2>&1;"

	# uncommenting lines containing pattern
	local_echo "Commenting in $comment_in_line_in_env_with_pattern inside $server_env_file..."
	execute_remote "sed -i '/$comment_in_line_in_env_with_pattern/s/^#//g' $server_env_file"

	# commenting out lines containing pattern
	local_echo "Commenting out $comment_out_line_in_env_with_pattern inside $server_env_file..."
	execute_remote "sed -i '/$comment_out_line_in_env_with_pattern/s/^/#/g' $server_env_file"

	local MEMORY="10000m"
	exit_on_fail execute_remote "echo -e \"MEMORY=$MEMORY\" >> $server_env_file"

	append_environment_to_env_sh "live-server" $ssh_user $super_instance $server_env_file $env_file

	local env_patch=$(build_configuration "# START Script $script_start_time" "SERVER_NAME=$(alphanumeric $instance_name)" "TELNET_PORT=$telnet_port" \
	"SERVER_PORT=$server_port" "EXPEDITION_PORT=$expedition_port" "MONGODB_NAME=$(alphanumeric $instance_name)" "MONGODB_HOST=$default_mongodb_host" \
	"MONGODB_PORT=$default_mongodb_port" "DEPLOY_TO=$instance_short_name" "SERVER_STARTUP_NOTIFY=${default_server_startup_notify:-" "}" \
	"BUILD_COMPLETE_NOTIFY=${default_build_complete_notifiy:-" "}" "# END Script $script_start_time")

	exit_on_fail execute_remote "echo -e \"$env_patch\" >> $server_env_file"



	local_echo "Creating README file if it does not exist already..."
	execute_remote touch $readme_file

	local_echo "Appending patch to README file..."
	execute_remote "echo -e \"\n# $instance_short_name (${description:-"Unknown"}, ${contact_person:-"Unknown"},${contact_email:-"Unknown"})\n$env_patch\" >> $readme_file"

	# start server and redirect both stderr and stdout to /dev/null (&>/dev/null). Send command to background by &. Do this to avoid blocking.
	local_echo "Starting server..."
	exit_on_fail execute_remote -f "set -o pipefail;sh -c \"cd $server_dir; nohup ./start > /dev/null 2>&1 &\""

	header "Event and user creation"

	event_id=$(configure_application $super_instance $server_port $event_name $new_admin_password $user_username $user_password)

	header "Configuring ALB"

	listener_arn=$(get_first_https_listener $load_balancer)
	local target_group_arn=$(exit_on_fail create_target_group "S-shared-$instance_short_name")
	set_target_group_health_check "$target_group_arn" "HTTP" "/index.html" "$server_port" "5" "4" "2" "2"

	register_targets $target_group_arn $(get_instance_id $super_instance)

	local domain=$(create_rule $listener_arn $instance_short_name $target_group_arn)

	header "Configuring Apache"

	execute_remote_root "echo -e \"\n# $instance_short_name (${description:-"Unknown"}, ${contact_person:-"Unknown"}, ${contact_email:-"Unknown"})\" >> $events_conf"
	append_macro_to_001_events_conf "$domain" "$event_id" "root" "$super_instance" "$server_port"
	reload_httpd "root" $super_instance
	header "Conclusion"

	success "Sub instance should be reachable through https://$domain."
}

# -----------------------------------------------------------
# For a given variable name, find first number that is not already used within all env.sh files inside /home/sailing/servers.
# If no number is found, return $2
# @param $1  variable name (e.g. SERVER_PORT)
# @param $2  default number (e.g. 8880)
# -----------------------------------------------------------
function find_first_unused_port(){
	last_port=($(get_last_used_port "$1"))
	if [ $? -ne 0 ]; then
		port_to_use=$2
	else
		port_to_use=$(($last_port+1))
	fi
	success $port_to_use
	echo $port_to_use
}

function execute_remote(){
	ssh_wrapper $ssh_user@$super_instance "$@"
}

function execute_remote_root(){
	ssh_wrapper "root"@$super_instance "$@"
}

function get_last_used_port(){
	ssh_prewrapper $ssh_user@$super_instance "set -o pipefail; for i in /home/sailing/servers/*/env.sh; do cat \$i | grep '^ *$1=' | tr -d '$1='; done | sort -n | tail -1"
}
