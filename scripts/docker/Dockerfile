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

ARG VERSION_NUMBER
ARG GRADLE_TAG=4.10.3-jdk8

FROM gradle:${GRADLE_TAG} AS dependencies
USER root

# We first copy the pom.xml file and the binaries stored in the source repository.
# This way, we retrieve all maven dependencies at the beginning.
# All these steps will be cached by Docker unless pom.xml or libs/ has been updated.
# This means that we only retrieve all dependencies if we modify the dependencies definition.
RUN mkdir -p /opt/gradle/.gradle
ENV GRADLE_USER_HOME=/opt/gradle/.gradle

WORKDIR /app
COPY ./build.gradle .
RUN gradle clean build --no-daemon --console=verbose || return 0


FROM gradle:${GRADLE_TAG} AS builder
USER root

COPY --from=dependencies /opt/gradle/.gradle /opt/gradle/.gradle

WORKDIR /app
COPY . .

RUN sed -i -- 's/version '0.0.0'/version '$VERSION_NUMBER'/g' build.gradle \
    && gradle jar --console=verbose


FROM openjdk:8 AS production

WORKDIR /app
COPY --from=builder /app/build/libs/cytomine-bioformats-wrapper.jar .

ENV BIOFORMAT_POOL_SIZE 4
EXPOSE 4321

# We use CMD shell form to allow variable substitution
CMD java -jar /app/cytomine-bioformats-wrapper.jar 4321 ${BIOFORMAT_POOL_SIZE}
