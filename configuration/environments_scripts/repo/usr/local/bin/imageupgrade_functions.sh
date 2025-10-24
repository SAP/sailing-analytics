#!/bin/bash

# Upgrades the AWS EC2 instance that this script is assumed to be executed on.
# The steps are as follows:

REBOOT_INDICATOR=/var/lib/sailing/is-rebooted
LOGON_USER_HOME=/root

run_yum_update() {
  echo "Updating packages using yum"
  yum -y update
}

run_dnf_upgrade() {
  echo "Upgrading using dnf"
  dnf -y upgrade --releasever=latest
}

run_apt_update_upgrade() {
  echo "Updating packages using apt"
  apt-get -y update; apt-get -y upgrade
  apt-get -y install linux-image-cloud-amd64
  apt-get -y autoremove
}

run_git_pull() {
  echo "Pulling git to /home/sailing/code"
  su - sailing -c "cd code; git pull"
}

download_and_install_latest_sap_jvm_8() {
  echo "Downloading and installing latest SAP JVM 8 to /opt/sapjvm_8"
  vmpath=$( curl -s --cookie eula_3_2_agreed=tools.hana.ondemand.com/developer-license-3_2.txt https://tools.hana.ondemand.com | grep additional/sapjvm-8\..*-linux-x64.zip | head -1 | sed -e 's/^.*a href="\(additional\/sapjvm-8\..*-linux-x64\.zip\)".*/\1/' )
  if [ -n "${vmpath}" ]; then
    echo "Found VM version ${vmpath}; upgrading installation at /opt/sapjvm_8"
    if [ -z "${TMP}" ]; then
      TMP=/tmp
    fi
    echo "Downloading SAP JVM 8 as ZIP file to ${TMP}/sapjvm8-linux-x64.zip"
    curl --cookie eula_3_2_agreed=tools.hana.ondemand.com/developer-license-3_2.txt "https://tools.hana.ondemand.com/${vmpath}" > ${TMP}/sapjvm8-linux-x64.zip
    cd /opt
    rm -rf sapjvm_8
    if [ -f SIGNATURE.SMF ]; then
      rm -f SIGNATURE.SMF
    fi
    unzip ${TMP}/sapjvm8-linux-x64.zip
    rm -f ${TMP}/sapjvm8-linux-x64.zip
    rm -f SIGNATURE.SMF
  else
    echo "Did not find SAP JVM 8 at tools.hana.ondemand.com; not trying to upgrade"
  fi
}

clean_logrotate_target() {
  echo "Clearing logrorate-targets"
  rm -rf /var/log/logrotate-target/*
}

clean_httpd_logs() {
  echo "Clearing httpd logs"
  service httpd stop
  rm -rf /var/log/httpd/*
  rm -f /etc/httpd/conf.d/001-internals.conf
}

clean_startup_logs() {
  echo "Clearing bootstrap logs"
  rm -f /var/log/sailing*
  # Ensure that upon the next boot the reboot indicator is not present, indicating that it's the first boot
  rm -f "${REBOOT_INDICATOR}"
}

clean_servers_dir() {
  rm -rf /home/sailing/servers/*
}

build_crontab_and_setup_files() {
    if [[ "$#" -eq 0 ]]; then
        echo "Number of arguments is invalid. Please use the options and arguments as follows."
        echo "Options for this function:"
        echo "  -h is the hostname to fetch the configuration/environments_scripts from."
        echo "The remaining (optional) args and options, if correct, are passed to the build_crontab_and_setup_files script."
        echo "  -c means no crontab file is created"
        echo "  -n means that if a crontab has been created, it isn't actually installed. This is useful for testing."
        echo "  -f means no files are copied over, which is useful if you have already copied files accross or don't want to override existing files"
        echo "Then there are the arguments, where the order matters:"
        echo "  ENVIRONMENT_TYPE - the directory name in environments_scripts which will be used."
    else
        TEMP=$(getopt -o fnch: -n 'options checker' -- "$@")
        [[ "$?" -eq 0 ]] || return 2
        eval set -- "$TEMP"
        PASS_OPTIONS=()
        HOSTNAME="sapsailing.com"
        while :; do
            case "$1" in
                -c|-f|-n)
                    PASS_OPTIONS+=("$1")
                    ;;
                -h)
                    if [[ "$2" ]]; then
                        HOSTNAME="$2"
                        shift
                    else
                        echo "hostname option requires argument"
                    fi
                    ;;
                --)
                    shift
                    break
                    ;;
                *)
                    echo "no more options"
                    break
            esac
            shift
        done
        TEMP_ENVIRONMENTS_SCRIPTS=$(mktemp -d /var/tmp/environments_scripts_XXX)
        echo "Attempting access to the wiki, typically used by image upgrade. Otherwise, try root. NOTE: If the first command fails, there will be a warning message."
        # During image upgrades, no environment type should have root access to the central server, but they do need 
        # access to the wiki copy. Therefore, the various keys for different environment types are in the authorized_keys
        # of the wiki user. So, during image upgrade or to get the latest changes, the following command should succeed.
        # The wiki user's authorized_keys is not updated automatically with landscape managers, so 
        # the below command may fail during initial image setup. In this scenario, the root user is instead the target 
        # user of the scp command (as seen in the second command below).
        scp -o StrictHostKeyChecking=no -pr wiki@"$HOSTNAME":~/gitwiki/configuration/environments_scripts/* "${TEMP_ENVIRONMENTS_SCRIPTS}"
        [[ "$?" -eq 0 ]] || scp -o StrictHostKeyChecking=no -pr root@"$HOSTNAME":/home/wiki/gitwiki/configuration/environments_scripts/* "${TEMP_ENVIRONMENTS_SCRIPTS}" # For initial setup as not all landscape managers have direct wiki access.
        sudo chown root:root "${TEMP_ENVIRONMENTS_SCRIPTS}"
        sudo chmod 777 "${TEMP_ENVIRONMENTS_SCRIPTS}"
        cd "${TEMP_ENVIRONMENTS_SCRIPTS}"
        # Add all args to array, otherwise, if PASS_OPTIONS is empty, and we also pass $@ then argument $1 is in fact null, which would cause errors.
        for option in "$@"; do
          PASS_OPTIONS+=( "$option" )
        done
        if ! sudo ./build-crontab-and-cp-files "${PASS_OPTIONS[@]}"; then
          return 1
        fi
        cd ..
        sudo rm -rf "$TEMP_ENVIRONMENTS_SCRIPTS"
    fi
}

# Assumes a filename following the pattern "crontab*[@{param}={value} ...]"
# It prints an "sed" command on the standard output that callers can use to
# obtain a piping command through which to send the file's contents to achieve
# the text pattern replacements requested by the @-parts in the file/link
# name.
# If a value shall contain the @ literal, it needs to be doubled, as, e.g., in
#     crontab-some-mail-thing@EMAIL_ADDRESS=john.doe@@example.com
# Multiple replacements may be requested by adding more @-phrases:
#     crontab-something@A=x@B=y
# Example usage:
#   echo "The user USER has e-mail address EMAIL_ADDRESS and home directory HOME_DIR" | eval $( get_replacements_from_crontab_filename_as_sed_command crontab-humba@USER=uhl@EMAIL_ADDRESS=axel.uhl@@sap.com@HOME_DIR=_home_axel__uhl_ )
# will output
#   The user uhl has e-mail address axel.uhl@sap.com and home directory /home/axel_uhl/
get_replacements_from_crontab_filename_as_sed_command() {
    LINK_NAME="${1}"
    AT_SIGN_REPLACEMENT=`mktemp -u XXXXXXXXXXXXXXXX`
    UNDERSCORE_REPLACEMENT=`mktemp -u XXXXXXXXXXXXXXXX`
    LINK_NAME_WITH_UNDERSCORES_REPLACED=$( echo "${LINK_NAME}" | sed -e 's|__|'${UNDERSCORE_REPLACEMENT}'|g' )
    LINK_NAME_WITH_AT_LITERALS_REPLACED=$( echo "${LINK_NAME_WITH_UNDERSCORES_REPLACED}" | sed -e 's|@@|'${AT_SIGN_REPLACEMENT}'|g' )
    IFS='@' read -r -a BASENAME_AND_SUBSTITUTIONS <<<"${LINK_NAME_WITH_AT_LITERALS_REPLACED}"
    # Now, BASENAME_AND_SUBSTITUTIONS[0] is the link's base name, 1..n are the replacements
    SED_COMMAND="sed -e 's| | |'"  # start with a no-op so that in case of no replacements we still have a valid sed command 
    for REPLACEMENT in ${BASENAME_AND_SUBSTITUTIONS[@]:1}; do
        IFS='=' read -r -a PARAM_AND_VALUE <<<"${REPLACEMENT}"
        PARAM="${PARAM_AND_VALUE[0]}"
        VALUE_WITH_SLASHES=$( echo "${PARAM_AND_VALUE[1]}" | sed -e 's|_|/|g' )
        VALUE_WITH_UNDERSCORES=$( echo "${VALUE_WITH_SLASHES}" | sed -e 's|'${UNDERSCORE_REPLACEMENT}'|_|g' )
        VALUE=$( echo "${VALUE_WITH_UNDERSCORES}" | sed -e 's|'${AT_SIGN_REPLACEMENT}'|@|g' )
        SED_COMMAND="${SED_COMMAND} -e 's|${PARAM}|${VALUE}|g'"
    done
    echo "${SED_COMMAND}"
}

setup_keys() {
    # Installs the necessary aws and ssh keys for a given environment type, by copying from the key vault.
    # $1: Environment type.
    # Optional parameter is -p which indicates that no permissions will be set or overwritten.
    TEMP=$(getopt -o p -n 'options' -- "$@")
    [[ "$?" -eq 0 ]] || return 2
    SET_PERMISSIONS="true"
    eval set -- "$TEMP"
    while true; do
        case "$1" in
            -p)
                SET_PERMISSIONS="false"
                ;;
            --)
                shift
                break
                ;;
            *)
                echo "Option not recognised"
                return 2
                ;;
        esac
        shift
    done
    if [[ "$#" -ne 1 ]]; then
        echo "Please specify the environment type and use the optional -p flag to indicate that no permissions will be set or overwritten."
        return 2
    fi
    pushd .
    TEMP_KEY_DIR=$(mktemp  -d /var/tmp/keysXXXXX)
    scp -o StrictHostKeyChecking=no -pr root@sapsailing.com:/root/key_vault/"${1}"/* "${TEMP_KEY_DIR}"
    sudo su - -c "source imageupgrade_functions.sh; __setup_keys_using_local_copy $TEMP_KEY_DIR $SET_PERMISSIONS"
    popd
    rm -rf "${TEMP_KEY_DIR}"
}
__setup_keys_using_local_copy() {
    # $1 the local location of the key_vault subdirectory corresponding to the image type this is run on.
    # $2 a "true" or "false" string, indicating whether to override existing permissions. 
    # "true" indicates the permissions and ownership of the .ssh and .aws folders will not be set.
    TEMP_KEY_DIR="$1"
    SET_PERMISSIONS="$2"
    REGION="$( ec2-metadata | grep "^placement:" | sed -e 's/^.*: \(.*\).$/\1/')"
    cd "${TEMP_KEY_DIR}"
    for user in *; do
        [[ -e "$user" ]] || continue
        if id -u "$user" > /dev/null; then
            user_home_dir=$(getent passwd $(id -u "$user") | cut -d: -f6) # getent searches for passwd based on user id, which the "id" command supplies.
            # aws setup
            if [[ -d "${user}/aws" ]]; then 
                mkdir --parents "${user_home_dir}/.aws"
                # Setup credentials
                if [[ -d "${user}/aws/credentials" && ! -e "${user_home_dir}/.aws/credentials" ]]; then
                    > "${user_home_dir}"/.aws/credentials
                    for credentials in "${user}"/aws/credentials/*; do
                        [[ -f "$credentials" ]] || continue
                        cat "$credentials" >> "${user_home_dir}"/.aws/credentials
                        echo "" >> "${user_home_dir}"/.aws/credentials
                    done
                fi
                # Setup config
                if [[ ! -e "${user_home_dir}/.aws/config" ]]; then
                    echo "[default]" >> "${user_home_dir}/.aws/config"
                    echo "region = ${REGION}" >> "${user_home_dir}"/.aws/config
                    echo "" >> "${user_home_dir}"/.aws/config
                    if [[ -d "${user}/aws/config" ]]; then
                        for config in "${user}"/aws/config/*; do
                            [[ -f "$config" ]] || continue
                            cat "$config" >> "${user_home_dir}"/.aws/config
                            echo "region = ${REGION}" >> "${user_home_dir}"/.aws/config
                            echo "" >> "${user_home_dir}"/.aws/config
                        done
                    fi
                fi
                if [[ "$SET_PERMISSIONS" == "true" ]]; then
                    chmod 755 "${user_home_dir}"/.aws
                    chown -R  ${user}:${user} "${user_home_dir}/.aws"
                    chmod 600 "${user_home_dir}"/.aws/*
                fi
            fi
            # ssh setup
            if [[ -d "${user}/ssh" ]]; then
                mkdir --parents "${user_home_dir}/.ssh"
                for key in "${user}"/ssh/*; do
                    [[ -f "$key" ]] || continue
                    [[ ! -f "$user_home_dir"/.ssh/"$(basename "$key")" ]] || { echo "$key not touched, as it is already present in ssh dir of $user. You may want to override it." && continue; }
                    \cp --preserve --dereference "$key" "$user_home_dir"/.ssh
                done
                for key in "${user}"/ssh/authorized_keys/*; do
                    [[ -f "$key" ]] || continue
                    if ! grep -q "$(cat "$key")" "${user_home_dir}"/.ssh/authorized_keys; then
                        cat "${key}" >>  "${user_home_dir}"/.ssh/authorized_keys
                    fi
                done
                if [[ "$SET_PERMISSIONS" == "true" ]]; then
                    chmod 700 "${user_home_dir}/.ssh"
                    chown -R  ${user}:${user} "${user_home_dir}/.ssh"
                    chmod 600 "${user_home_dir}"/.ssh/*
                fi
            fi
        fi
    done
}

clean_root_ssh_dir_and_tmp() {
  echo "Cleaning up ${LOGON_USER_HOME}/.ssh"
  rm -rf ${LOGON_USER_HOME}/.ssh/authorized_keys
  rm -rf ${LOGON_USER_HOME}/.ssh/known_hosts
  rm -f /var/run/last_change_aws_landscape_managers_ssh_keys*
  rm -rf /tmp/image-upgrade-finished
}

get_ec2_user_data() {
  ec2-metadata -d | sed -e 's/^user-data: //'
}

finalize() {
  # Finally, shut down the node unless "no-shutdown" was provided in the user data, so that a new AMI can be constructed cleanly
  if get_ec2_user_data | grep "^no-shutdown$"; then
    echo "Shutdown disabled by no-shutdown option in user data. Remember to clean /root/.ssh when done."
    touch /tmp/image-upgrade-finished
  else
    # Only clean ${LOGON_USER_HOME}/.ssh directory and /tmp/image-upgrade-finished if the next step is shutdown / image creation
    clean_root_ssh_dir_and_tmp
    rm -f /var/log/sailing.err
    shutdown -h now &
  fi
}

setup_cloud_cfg_and_root_login() {
    sudo sed -i 's/#PermitRootLogin yes/PermitRootLogin without-password\nPermitRootLogin yes/' /etc/ssh/sshd_config
    sudo sed -i 's/^disable_root: *true$/disable_root: false/' /etc/cloud/cloud.cfg
}

setup_fail2ban() {
    # Expects setup_mail_sending to have been invoked for fail2ban e-mails being sent properly
    sudo dnf install -y fail2ban whois
    sudo sed -i 's|^backend *= *auto *$|backend = systemd|' /etc/fail2ban/jail.conf
    # The fail2ban service may depend on firewalld which then gets installed and
    # injects all sorts of unwanted nftables/iptables rules that can make our services
    # unreachable (MongoDB port 27017, MariaDB port 3306, application server port 8888, etc.).
    # We use security groups in our VPC to control which ports can be reached from where.
    # Therefore, we disable firewalld, should it have been installed
    sudo systemctl disable firewalld
    sudo systemctl enable fail2ban
    # the /etc/fail2ban/jail.d/ contents are expected to be provided by the files/etc/fail2ban/jail.d
    # folders in the respective environments_scripts sub-folder; use, e.g., a symbolic link to
    # configuration/environments_scripts/repo/etc/fail2ban/jail.d/customisation.local for a
    # systemd-based sshd-iptables filter.
    sudo touch /var/log/fail2ban.log
    sudo systemctl start fail2ban
}

setup_mail_sending() {
    # Sets up mail sending using Amazon Simple Email Service by configuring postfix.
    #
    # $1 is an optional argument which is prepended to the myorigin variable in the postfix conf.
    # This variable controls the sender's email address.
    # eg. "setup_mail_sending disposable" will result in mail from disposable.sapsailing.com. 
    # DO NOT include spaces in the parameter, even if you use quotes.
    # The default is the local hostname.
    if sudo grep "^relayhost\>" /etc/postfix/main.cf &>/dev/null; then   #& here redirects stdout and stderr. \> means there must be a word boundary.
        echo "Postfix is already installed and has some custom configuration. Please check it is configured correctly at /etc/postfix/main.cf"
        echo "If you wish to change the sender's email address then edit the myorigin variable in /etc/postfix/main.cf"
        return 0
    fi
    subdomain_of_sender_address="$( ec2-metadata --local-hostname | sed "s/local-hostname: *//")"
    if [[ -n "$1" ]]; then
        subdomain_of_sender_address="$1"
    fi
    sudo dnf install -y mailx postfix
    sudo systemctl enable postfix
    temp_mail_properties_location=$(mktemp /var/tmp/mail.properties_XXX)
    scp -o StrictHostKeyChecking=no  -p root@sapsailing.com:mail.properties "${temp_mail_properties_location}"
    cd $(dirname "${temp_mail_properties_location}")
    local smtp_host="$(sed -n "s/mail.smtp.host \?= \?\(.*\)/\1/p" ${temp_mail_properties_location})"
    local smtp_port="$(sed -n "s/mail.smtp.port \?= \?\(.*\)/\1/p" ${temp_mail_properties_location})"
    local smtp_user="$(sed -n "s/mail.smtp.user \?= \?\(.*\)/\1/p" ${temp_mail_properties_location})"
    local smtp_pass="$(sed -n "s/mail.smtp.password \?= \?\(.*\)/\1/p" ${temp_mail_properties_location})"
    local password_file_location="/etc/postfix/sasl_passwd"
    sudo su -c "echo \"relayhost = [${smtp_host}]:${smtp_port}
smtp_sasl_auth_enable = yes
smtp_sasl_security_options = noanonymous
smtp_sasl_password_maps = hash:${password_file_location}
smtp_use_tls = yes
smtp_tls_security_level = encrypt
smtp_tls_note_starttls_offer = yes

myorigin =${subdomain_of_sender_address}.sapsailing.com
\" >> /etc/postfix/main.cf"
    sudo sed -i  "/smtp_tls_security_level = may/d" /etc/postfix/main.cf
    sudo su -c "echo \"[${smtp_host}]:${smtp_port} ${smtp_user}:${smtp_pass}\" >> ${password_file_location}"
    sudo postmap hash:${password_file_location}
    sudo systemctl restart postfix
    rm -f "${temp_mail_properties_location}"
}

setup_sshd_resilience() {
    sudo su -c "echo 'ClientAliveInterval 3
ClientAliveCountMax 3
GatewayPorts yes
MaxStartups 100' >> /etc/ssh/sshd_config && systemctl reload sshd.service"
}

identify_suitable_partition_for_ephemeral_volume() {
    EPHEMERAL_VOLUME_NAME=$(
    # List all block devices and find those named nvme...
    for i in $(lsblk | grep -o "nvme[0-9][0-9]\?n[0-9]" | sort -u); do
        # If they don't have any partitions, then...
        if ! lsblk | grep -o "${i}p[0-9]\+" 2>&1 >/dev/null; then
            # ...check whether they are EBS devices
            /sbin/ebsnvme-id -u "/dev/$i" >/dev/null
            # If not, list their name because then they must be ephemeral instance storage
            if [[ $? -ne 0 ]]; then
                echo "${i}"
            fi
        fi
    done 2>/dev/null | head -n 1 )
    echo $EPHEMERAL_VOLUME_NAME
}

setup_goaccess() {
    # Compatible with Amazon Linux 2023
    pushd .
    cd /usr/local/src
    wget https://tar.goaccess.io/goaccess-1.9.1.tar.gz
    tar -xzvf goaccess-1.9.1.tar.gz
    cd goaccess-1.9.1/
    dnf install -y gcc-c++
    dnf install -y libmaxminddb-devel ncurses-devel
    ./configure --enable-utf8
    make
    make install
    # old location:
    # scp root@sapsailing.com:/etc/goaccess.conf /usr/local/etc/goaccess/goaccess.conf
    # once we switch from amazon linux 1:
    scp root@sapsailing.com:/usr/local/etc/goaccess/goaccess.conf /usr/local/etc/goaccess/goaccess.conf
    popd
}
setup_apachetop() {
    # Compatible with Amazon Linux 2023
    pushd .
    dnf install -y gcc-c++
    dnf install -y ncurses-devel readline-devel
    cd /usr/local/src
    wget https://github.com/tessus/apachetop/releases/download/0.23.2/apachetop-0.23.2.tar.gz
    tar -xvzf apachetop-0.23.2.tar.gz
    cd apachetop-0.23.2
    ./configure
    make
    make install
    popd
}

setup_swap() {
    # $1: size of swapspace in megabytes.
    echo "Creating swapspace of $1 MBs"
    local swapfile_location=/var/cache/swapfile
    pushd .
    sudo dd if=/dev/zero of="$swapfile_location" bs=1M count="$1"
    sudo chmod 600 "$swapfile_location"
    sudo chown root:root "$swapfile_location"
    sudo mkswap "$swapfile_location"
    sudo su - -c "echo \"$swapfile_location       none    swap    pri=0      0       0\" >> /etc/fstab"
    sudo swapon -a
    popd
}

setup_mongo_7_0_on_AL2023() {
    # Install MongoDB 7.0 on Amazon Linux 2023
    sudo su - -c "cat << EOF >/etc/yum.repos.d/mongodb-org.7.0.repo
[mongodb-org-7.0]
name=MongoDB Repository
baseurl=https://repo.mongodb.org/yum/amazon/2023/mongodb-org/7.0/x86_64/
gpgcheck=1
enabled=1
gpgkey=https://www.mongodb.org/static/pgp/server-7.0.asc
EOF
"
    sudo dnf -y update
    sudo dnf -y install mongodb-org-server mongodb-org-tools mongodb-mongosh-shared-openssl3
    # ensure that logrotate can work nicely with SIGUSR1:
    if ! grep "logRotate: reopen" /etc/mongod.conf; then
      sudo sed -i -e 's/^  logAppend: true/  logAppend: true\n  logRotate: reopen/' /etc/mongod.conf
    fi
}

# Copies the /root/secrets and /root/mail.properties file to the local instance, ensuring only root can read it
install_secrets() {
    # Install secrets
    scp -o StrictHostKeyChecking=no root@sapsailing.com:secrets /tmp
    scp -o StrictHostKeyChecking=no root@sapsailing.com:mail.properties /tmp
    sudo mv /tmp/secrets /root
    sudo mv /tmp/mail.properties /root
    sudo chown root /root/secrets
    sudo chgrp root /root/secrets
    sudo chmod 600 /root/secrets
    sudo chown root /root/mail.properties
    sudo chgrp root /root/mail.properties
    sudo chmod 600 /root/mail.properties
}
