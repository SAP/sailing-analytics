#!/bin/bash
# Fetches the latest trac trac urls from the Mongo archive replica set, currently residing on the central mongo instance, 
# and commits the changes to the master branch of the main Git. Note that local state is prioritised if there is a conflict.
# Parameter 1 is an optional absolute path to the Git root. If unspecified, then the default is /home/wiki/gitwiki.
if [[ $# -eq 0 ]]; then
    GIT_ROOT=/home/wiki/gitwiki
else
    GIT_ROOT=$1
fi
PATH_TO_TRAC_TRAC_URLS="configuration/tractrac-json-urls"
urls=$(ssh -o StrictHostKeyChecking=no ec2-user@dbserver.internal.sapsailing.com "mongosh --quiet \"mongodb://dbserver.internal.sapsailing.com:10201/winddb?replicaSet=archive\" --eval \"EJSON.stringify(db.TRACTRAC_CONFIGURATIONS.find({}, {TT_CONFIG_JSON_URL : 1, _id : 0}).toArray())\"" | jq -r '.[].TT_CONFIG_JSON_URL' | sort -u )
if [[ "$?" -ne 0 || "$urls" == "null" ]]; then
    echo "Mongo db returns null for tractrac url discovery" | notify-operators "MongoDB/tractrac urls issue"
    exit 1
else
    echo "${urls}" | sort -u >"${GIT_ROOT}/${PATH_TO_TRAC_TRAC_URLS}"
    cd "${GIT_ROOT}"
    git config pull.rebase false # ensure that diverging branches merge.
    branch_head=$(git rev-parse HEAD)
    stash_stack_size=$(git stash list | wc -l)
    git stash
    new_stash_stack_size=$(git stash list | wc -l)
    git pull
    if [[ "$stash_stack_size" -eq "$new_stash_stack_size" ]]; then
        echo "NO CHANGES STASHED" # This implies there were no changes to the tractrac urls or other changes in the workspace.
    else
        if git stash apply; then
            echo "STASH APPLIED CLEANLY"
            git stash drop
            git add "${GIT_ROOT}/${PATH_TO_TRAC_TRAC_URLS}"
            git commit -m "Updated tractrac-json-urls"
            git push
        else
            # prioritise existing state and local changes.
            echo "PRIORITISING LOCAL STATE, RESETTING HEAD AND APPLYING STASH."
            git reset --hard "$branch_head"
            git stash apply
            echo "The stash is still stored in the stack."
        fi
    fi
fi
