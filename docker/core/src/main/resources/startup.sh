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

cd /etc/apache-syncope

rm -f provisioning.properties
ln -s provisioning.properties.$DBMS provisioning.properties

rm -f persistence.properties
if [ $DBMS = "pgjsonb" ]; then
  ln -s persistence.properties.pgjsonb persistence.properties
elif [ $DBMS = "myjson" ]; then
  ln -s persistence.properties.myjson persistence.properties
else
  ln -s persistence.properties.all persistence.properties
fi

rm -f views.xml
ln -s views.xml.$DBMS views.xml

if [ $DBMS = "pgjsonb" ]; then
  ln -s indexes.xml.pgjsonb indexes.xml
elif [ $DBMS = "myjson" ]; then
  ln -s indexes.xml.myjson indexes.xml
else
  rm -f indexes.xml
fi

cd domains

if [ $DBMS = "pgjsonb" ]; then
  mv MasterContent.xml MasterContent.xml.all
  ln -s MasterContent.xml.pgjsonb MasterContent.xml
elif [ $DBMS = "myjson" ]; then
  mv MasterContent.xml MasterContent.xml.all
  ln -s MasterContent.xml.myjson MasterContent.xml
else
  rm -f MasterContent.xml
  mv MasterContent.xml.all MasterContent.xml
fi

rm -f Master.properties
ln -s Master.properties.$DBMS Master.properties

if [ $DBMS = "pgjsonb" ] || [ $DBMS = "myjson" ] ; then
  mkdir -p /tmp/WEB-INF/lib
  cp /etc/apache-syncope/syncope-core-persistence-jpa-json-*.jar /tmp/WEB-INF/lib
  cd /tmp
  jar uvf /usr/share/apache-syncope/syncope-deb-core-*.war WEB-INF/lib/
fi

/etc/init.d/tomcat8 start

xtail /var/log/apache-syncope/*.log /var/log/tomcat8/

/bin/bash
