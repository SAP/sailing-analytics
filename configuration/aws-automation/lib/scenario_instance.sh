#!/usr/bin/env bash

# Scenario for creating an instance
# ------------------------------------------------------

function instance_start(){
	instance_require
	instance_execute
}

# -----------------------------------------------------------
# All these variables are needed for this scenario
# If one variable is not assigned or passed by parameter
# the user will be prompted to enter a value
# -----------------------------------------------------------
function instance_require(){
	require_region
	require_instance_type
	require_instance_security_group_id
	require_image_id
	require_instance_name
	require_instance_short_name
	require_ssh_user
	require_build_version
	require_key_name
	require_key_file
	require_new_admin_password
	require_user_username
	require_user_password
}

# -----------------------------------------------------------
# Execute instance scenario
# @param $1  user data
# -----------------------------------------------------------
function instance_execute() {
	header "Instance Initialization"

	local user_data=$(build_configuration "MONGODB_HOST=$default_mongodb_host" "MONGODB_PORT=$default_mongodb_port" "MONGODB_NAME=$(alphanumeric $instance_name)" \
	"REPLICATION_CHANNEL=$(alphanumeric $instance_name)" "SERVER_NAME=$(alphanumeric $instance_name)" "USE_ENVIRONMENT=live-server" \
	"INSTALL_FROM_RELEASE=$build_version" "SERVER_STARTUP_NOTIFY=$default_server_startup_notify")

	instance_id=$(exit_on_fail create_instance "$user_data")
	public_dns_name=$(exit_on_fail query_public_dns_name $instance_id)

	wait_for_ssh_connection $ssh_user $public_dns_name

	header "Event and user creation"
	local port="8888"
	configure_application $public_dns_name $port $event_name $new_admin_password $user_username $user_pass

}
