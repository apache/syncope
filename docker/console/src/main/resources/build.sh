#!/bin/bash

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

set -x

#####
# Disable suggests/recommends
#####
echo APT::Install-Recommends "0"\; > /etc/apt/apt.conf.d/10disableextras
echo APT::Install-Suggests "0"\; >>  /etc/apt/apt.conf.d/10disableextras

export DEBIAN_FRONTEND noninteractive
export DEBCONF_TERSE true

# needed by debian:stable-slim
mkdir -p /usr/share/man/man1/

######
# Additional APT repositories
######
apt-get -q update && apt-get -q install -y wget

wget -q -O - http://repos.azulsystems.com/RPM-GPG-KEY-azulsystems | tee /etc/apt/trusted.gpg.d/zulu.asc
echo 'deb http://repos.azulsystems.com/debian stable main' > /etc/apt/sources.list.d/zulu.list

######
# Install packages
######
apt-get -q update \
  && apt-get -q install -y \
    xtail \
    zulu-8 \
    tomcat8 \
    libservlet3.1-java \
  && apt-get clean

dpkg -i /tmp/*.deb && rm /tmp/*deb

######
# Setup Tomcat
######

sed -i 's/--exec/--startas/' /etc/init.d/tomcat8

echo "JAVA_OPTS=\"\${JAVA_OPTS} -Dfile.encoding=UTF-8 -server \
-Xms1536m -Xmx1536m -XX:NewSize=256m -XX:MaxNewSize=256m \
-XX:PermSize=256m -XX:MaxPermSize=256m -XX:+DisableExplicitGC \
-Djava.security.egd=file:/dev/./urandom\"" >> /etc/default/tomcat8

echo "JAVA_HOME=/usr/lib/jvm/zulu-8-amd64" >> /etc/default/tomcat8
