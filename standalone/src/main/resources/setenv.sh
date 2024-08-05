# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

JAVA_OPTS="-Djava.awt.headless=true -Dfile.encoding=UTF-8 -Djavax.net.ssl.trustStore=$CATALINA_HOME/conf/keystore.jks -Djavax.net.ssl.trustStorePassword=password -Dsyncope.conf.dir=$CATALINA_HOME/webapps/syncope/WEB-INF/classes -Dsyncope.connid.location=connid://${testconnectorserver.key}@localhost:${testconnectorserver.port} -Dsyncope.log.dir=$CATALINA_HOME/logs -Dspring.profiles.active=embedded,all -server -Xms2048m -Xmx2048m -XX:NewSize=256m -XX:MaxNewSize=256m -DCATALINA_HOME=$CATALINA_HOME"
