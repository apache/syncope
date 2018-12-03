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

FROM debian:stable-slim
MAINTAINER dev@syncope.apache.org

RUN set -x

COPY *.jar /tmp/

COPY *.deb /tmp/

COPY provisioning.properties.* /etc/apache-syncope/
COPY views.xml.* /etc/apache-syncope/
COPY *.properties /etc/apache-syncope/
COPY Master.properties.* /etc/apache-syncope/domains/

COPY build.sh /tmp
RUN chmod 755 /tmp/build.sh && sync && /tmp/build.sh && rm /tmp/build.sh

COPY startup.sh /sbin
RUN chmod 755 /sbin/startup.sh
CMD ["/sbin/startup.sh"]

EXPOSE 8080
