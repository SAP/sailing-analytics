#!/bin/bash
source ./../../../configuration/correctFilePathInRelationToCurrentOs.sh
# generate a target definition pointing to the locally built p2 repo from the race-analysis-p2-remote target defintion
base="../definitions/race-analysis-p2"
remote_repo="https://download.eclipse.org/sailing-analytics/p2/sailing/"
remote_repo_aws_sdk="https://download.eclipse.org/sailing-analytics/p2/aws-sdk/"
# reading the filepath and editing it, so it fits for eclipse
# currently safely works for cygwin, gitbash and linux
local_repo=$(correct_file_path  "`readlink -f ../../com.sap.sailing.targetplatform.base/target/repository/`")
local_repo="file://$local_repo"
local_repo_aws_sdk=$(correct_file_path  "`readlink -f ../../com.amazon.aws.aws-java-api.updatesite/target/repository/`")
local_repo_aws_sdk="file://$local_repo_aws_sdk"
# replace remote p2-repo URL with local repo URL
sed -e "/^<repository location=\"/ s@$remote_repo@$local_repo@" -e "/^<repository location=\"/ s@$remote_repo_aws_sdk@$local_repo_aws_sdk@" -e 's/target name="Race Analysis Target"/target name="Race Analysis Local Target"/' $base-remote.target > $base-local.target
