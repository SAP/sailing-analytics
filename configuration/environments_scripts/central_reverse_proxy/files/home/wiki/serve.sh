#!/bin/bash
# kill any running gollums
kill -9 `ps axlw | grep rackup | grep wiki | awk '{ print $3; }'`
rm /home/wiki/wiki_log.txt
# start gollum as a background process
# you can pipe output to /dev/null instead, if you don't want a log
cd /home/wiki
. secrets
#nohup rackup -p 4567 /home/wiki/config.ru 1>nohup.out 2>&1  &
nohup bundle exec rackup -p 4567 /home/wiki/config.ru &
