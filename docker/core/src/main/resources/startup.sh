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
rm -f views.xml
ln -s views.xml.$DBMS views.xml

cd domains
rm -f Master.properties
ln -s Master.properties.$DBMS Master.properties

/etc/init.d/tomcat8 start

xtail /var/log/apache-syncope/*.log /var/log/tomcat8/

/bin/bash
