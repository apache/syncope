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

FROM adoptopenjdk/openjdk11-openj9:alpine-slim
MAINTAINER dev@syncope.apache.org

RUN set -x

RUN mkdir /opt/syncope
RUN mkdir /opt/syncope/bin
RUN mkdir /opt/syncope/bundles
RUN mkdir /opt/syncope/conf
RUN mkdir /opt/syncope/conf/domains
RUN mkdir /opt/syncope/lib
RUN mkdir /opt/syncope/jpa-json
RUN mkdir /opt/syncope/log

COPY *.properties* /opt/syncope/conf/
COPY *.xml* /opt/syncope/conf/
COPY keystore /opt/syncope/conf/
COPY domains/* /opt/syncope/conf/domains/

COPY bundles/*.jar /opt/syncope/bundles/
COPY lib/*.jar /opt/syncope/lib/
COPY jpa-json/*.jar /opt/syncope/jpa-json/

COPY lib/syncope-docker-core-*war /opt/syncope/lib/syncope.war

COPY startup.sh /opt/syncope/bin
RUN chmod 755 /opt/syncope/bin/startup.sh
CMD ["/opt/syncope/bin/startup.sh"]

EXPOSE 8080
