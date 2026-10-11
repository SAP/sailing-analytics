#!/bin/bash
# Usage: Launch an Amazon EC2 m6gd.large instance from an Amazon Linux 2023 AMI with
# 200GB of root partition size and the "SSH" and "MapTiles from Sailing App on Port 80"
# security groups using an SSH key for which you have a working private key available.
# Then, run this script on your local computer, using the external IP address
# of the instance you just launched in AWS as only argument. This will then
# turn the instance into a MapLibre maptiles server.
# When the script is done you may log in to look around and check
# things. When done, shut down the instance (Stop, not Terminate) and create
# an image off of it, naming it, e.g., "OpenFreeMapTile Server 1.6" and
# also tagging its root volume snapshot as, e.g., "OpenFreeMap Tile Server 1.6 (Root)".
# If you want to use the resulting image in production, also tag it with
# tag key "image-type" and tag value "maptiles".
if [ $# != 0 ]; then
  SERVER=$1
  OPENFREEMAP_GIT=$( mktemp -d openfreemapXXXX.git )
  git clone https://github.com/axeluhl/openfreemap "${OPENFREEMAP_GIT}"
  cp $( dirname "${0}" )/bake.jsonc "${OPENFREEMAP_GIT}/config/linux_host"
  if ! command -v uv >/dev/null 2>&1; then
    echo "The 'uv' tool is required to run ./linux_host/deploy_linux_host.py but was not found." >&2
    read -r -p "Install it now via 'curl -LsSf https://astral.sh/uv/install.sh | sh'? [y/N] " REPLY
    if [[ "${REPLY}" =~ ^[Yy]$ ]]; then
      curl -LsSf https://astral.sh/uv/install.sh | sh
      # Make uv available in the current shell without requiring a new login session:
      if [ -f "${HOME}/.local/bin/env" ]; then
        . "${HOME}/.local/bin/env"
      fi
      if ! command -v uv >/dev/null 2>&1; then
        echo "'uv' still not found on PATH after installation; aborting." >&2
        exit 3
      fi
    else
      echo "Cannot continue without 'uv'; aborting." >&2
      exit 3
    fi
  fi
  pushd "${OPENFREEMAP_GIT}"
  ./linux_host/deploy_linux_host.py --config bake --host ${SERVER} --user ec2-user
  popd
  rm -rf "${OPENFREEMAP_GIT}"
  scp "${0}" ec2-user@${SERVER}:
  ssh -A ec2-user@${SERVER} ./$( basename "${0}" )
  ssh -A ec2-user@${SERVER} "tail -f /data/ofm/linux_host/logs/sync.log"
else
  if ec2-metadata | grep -q instance-id; then
    echo "Running on an AWS EC2 instance as user ${USER} / $(whoami), starting setup..."
    # Install standard packages:
    sudo dnf -y --best --allowerasing --releasever=latest upgrade
    sudo dnf -y install nvme-cli chrony cronie cronie-anacron jq mailx whois iptables
    # Copy imageupgrade_function.sh
    scp -o StrictHostKeyChecking=no -p root@sapsailing.com:/home/wiki/gitwiki/configuration/environments_scripts/repo/usr/local/bin/imageupgrade_functions.sh .
    sudo mv imageupgrade_functions.sh /usr/local/bin
    # build-crontab
    . imageupgrade_functions.sh
    build_crontab_and_setup_files maptiles_server
    setup_fail2ban
    # obtain root SSH key from key vault:
    setup_keys "maptiles_server"
    scp root@sapsailing.com:ssh-key-reader.token /tmp
    sudo mv /tmp/ssh-key-reader.token /root
    sudo chown root:root /root/ssh-key-reader.token
    sudo chmod 600 /root/ssh-key-reader.token
    setup_sshd_resilience
  else
    echo "Not running on an AWS instance; refusing to run setup!" >&2
    echo "To prepare an instance running in AWS, provide its external IP as argument to this script." >&2
    exit 2
  fi
fi
