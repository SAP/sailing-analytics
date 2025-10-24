#!/bin/bash
# Usage: ${0} [ -b {bugs-password] ] [ -r {root-password} ] {instance-ip}
# Deploy with Amazon Linux 2023 with a 16GB root volume

# Read options and assign to variables:
options='b:r:'
while getopts $options option
do
    case $option in
        b) BUGS_PW="${OPTARG}" ;;
        r) ROOT_PW="${OPTARG}" ;;
        \?) echo "Invalid option"
            exit 4;;
    esac
done
if [ -z "${ROOT_PW}" ]; then
  echo -n "MySQL password for user root: "
  read -s ROOT_PW
  echo
fi
if [ -z "${BUGS_PW}" ]; then
  echo -n "MySQL password for user bugs: "
  read -s BUGS_PW
  echo
fi
shift $((OPTIND-1))
if [ $# != 0 ]; then
  SERVER=$1
  scp -o StrictHostKeyChecking=false "${0}" ec2-user@${SERVER}:
  ssh -o StrictHostKeyChecking=false -A ec2-user@${SERVER} "./$( basename "${0}" ) -r \"${ROOT_PW}\" -b \"${BUGS_PW}\""
else
  BACKUP_FILE=/home/ec2-user/backupdb.sql
  backupdbNOLOCK=/home/ec2-user/backupdbNOLOCK.sql
  # Install cron job for ssh key update for landscape managers
  scp -o StrictHostKeyChecking=no -p root@sapsailing.com:/home/wiki/gitwiki/configuration/environments_scripts/repo/usr/local/bin/imageupgrade_functions.sh /home/ec2-user
  sudo mv /home/ec2-user/imageupgrade_functions.sh /usr/local/bin/imageupgrade_functions.sh
  scp -o StrictHostKeyChecking=false root@sapsailing.com:ssh-key-reader.token /home/ec2-user
  sudo chown ec2-user /home/ec2-user/ssh-key-reader.token
  sudo chgrp ec2-user /home/ec2-user/ssh-key-reader.token
  sudo chmod 600 /home/ec2-user/ssh-key-reader.token
  # Install packages for MariaDB and cron/anacron/crontab:
  sudo dnf -y --best --allowerasing --releasever=latest upgrade
  sudo dnf -y install mariadb105-server cronie
  sudo su -c "printf '\n[mysqld]\nlog_bin = /var/log/mariadb/mysql-bin.log\n' >> /etc/my.cnf.d/mariadb-server.cnf"
  sudo systemctl enable mariadb.service
  sudo systemctl start mariadb.service
  sudo systemctl enable crond.service
  sudo systemctl start crond.service
  . imageupgrade_functions.sh
  if ! build_crontab_and_setup_files mysql_instance_setup; then
    exit 1
  fi    
  setup_sshd_resilience
  setup_mail_sending
  setup_fail2ban
  sudo chown root:root /usr/local/bin/imageupgrade_functions.sh
  echo "Creating backup through mysql client on sapsailing.com..."
  ssh -o StrictHostKeyChecking=false root@sapsailing.com "mysqldump --all-databases -h mysql.internal.sapsailing.com --user=root --password=${ROOT_PW} --master-data  --skip-lock-tables  --lock-tables=0" >> ${BACKUP_FILE}
  # the two lock options are supposed to ignore table locks, but the following removes a problematic exception.
  echo "Removing lock on log table which causes failures"
  cat ${BACKUP_FILE} | sed  "/LOCK TABLES \`transaction_registry\`/,/UNLOCK TABLES;/d" >${backupdbNOLOCK}
  echo "Importing backup locally..."
  sudo mysql -u root -h localhost <${backupdbNOLOCK}
  sudo mysql -u root -p${ROOT_PW} -e "FLUSH PRIVILEGES;"
  rm ${BACKUP_FILE}
  rm ${backupdbNOLOCK}
  sudo systemctl stop mariadb.service
  sudo systemctl start mariadb.service
  echo "Showing bug count:"
  sudo mysql -u root -p${ROOT_PW} -e "select count(bug_id) from bugs.bugs;"
  echo 'Test your DB, e.g., by counting bugs: sudo mysql -u root -p -e "use bugs; select count(*) from bugs;"'
  echo "If you like what you see, switch to the new DB by updating the mysql.internal.sapsailing.com DNS record to this instance,"
  echo "make sure the instance has the \"Database and Messaging\" security group set,"
  echo "and tag the instance's root volume with the WeeklySailingInfrastructureBackup=Yes tag."
fi
