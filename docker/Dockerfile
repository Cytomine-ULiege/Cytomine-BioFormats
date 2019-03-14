#
# Copyright (c) 2009-2018. Authors: see NOTICE file.
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
#

ARG FROM_NAMESPACE
ARG FROM_VERSION
FROM ${FROM_NAMESPACE}/java8:$FROM_VERSION

ARG VERSION
RUN cd /tmp/ && \
    wget -q https://github.com/Cytomine-ULiege/Cytomine-BioFormats/releases/download/$VERSION/Cytomine_BioFormats.tar.gz -O Cytomine_BioFormats.tar.gz && \
    tar -zxvf Cytomine_BioFormats.tar.gz

#ADD Cytomine_BioFormats.tar.gz /tmp

COPY crontab /tmp/crontab
COPY check_bioformat.py /tmp/check_bioformat.py
COPY deploy.sh /tmp/deploy.sh
RUN chmod +x /tmp/deploy.sh

ENTRYPOINT ["/tmp/deploy.sh"]
