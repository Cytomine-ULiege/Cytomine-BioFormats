#!/bin/bash

# Copyright (c) 2009-2019. Authors: see NOTICE file.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

java -jar /tmp/cytomine-bioformats-wrapper.jar $BIOFORMAT_PORT > /tmp/log &

touch /tmp/crontab
echo "BIOFORMAT_PORT=$BIOFORMAT_PORT" >> /tmp/crontab
echo "*/1 * * * * /bin/bash /tmp/check-status.sh $BIOFORMAT_PORT >> /tmp/cron.out" >> /tmp/crontab
crontab /tmp/crontab
rm /tmp/crontab

service rsyslog restart
service cron restart

tail -F /tmp/log
