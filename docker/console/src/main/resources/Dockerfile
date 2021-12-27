# Licensed to the Apache Software Foundation (ASF) under one
# or more contributor license agreements.  See the NOTICE file
# distributed with this work for additional information
# regarding copyright ownership.  The ASF licenses this file
# to you under the Apache License, Version 2.0 (the
# "License"); you may not use this file except in compliance
# with the License.  You may obtain a copy of the License at
#
#   http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing,
# software distributed under the License is distributed on an
# "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
# KIND, either express or implied.  See the License for the
# specific language governing permissions and limitations
# under the License.

FROM eclipse-temurin:11-focal
MAINTAINER dev@syncope.apache.org

RUN set -x

RUN mkdir /opt/syncope
RUN mkdir /opt/syncope/bin
RUN mkdir /opt/syncope/conf
RUN mkdir /opt/syncope/lib
RUN mkdir /opt/syncope/log

COPY *.properties /opt/syncope/conf/
COPY log4j2.xml /opt/syncope/conf/

COPY syncope-docker-console-*war /opt/syncope/lib/syncope-console.war

ENV SPRING_PROFILES_ACTIVE=docker
ENV LOADER_PATH="/opt/syncope/conf,/opt/syncope/lib"

COPY startup.sh /opt/syncope/bin
RUN chmod 755 /opt/syncope/bin/startup.sh
CMD ["/opt/syncope/bin/startup.sh"]

EXPOSE 8080
