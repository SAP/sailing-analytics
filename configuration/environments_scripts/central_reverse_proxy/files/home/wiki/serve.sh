#!/bin/bash
cd /home/wiki
. ~/.bashrc
. secrets
#nohup rackup -p 4567 /home/wiki/config.ru 1>nohup.out 2>&1  &
exec bundle exec rackup -p 4567 /home/wiki/config.ru
