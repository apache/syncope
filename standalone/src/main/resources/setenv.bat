@echo off
rem Licensed to the Apache Software Foundation (ASF) under one or more
rem contributor license agreements.  See the NOTICE file distributed with
rem this work for additional information regarding copyright ownership.
rem The ASF licenses this file to You under the Apache License, Version 2.0
rem (the "License"); you may not use this file except in compliance with
rem the License.  You may obtain a copy of the License at
rem
rem     http://www.apache.org/licenses/LICENSE-2.0
rem
rem Unless required by applicable law or agreed to in writing, software
rem distributed under the License is distributed on an "AS IS" BASIS,
rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
rem See the License for the specific language governing permissions and
rem limitations under the License.

set JAVA_OPTS=-Djava.awt.headless=true -Dfile.encoding=UTF-8 -Djavax.net.ssl.trustStore=%CATALINA_HOME%\conf\keystore.jks -Dsyncope.connid.location=connid://${testconnectorserver.key}@localhost:${testconnectorserver.port} -Dsyncope.conf.dir=%CATALINA_HOME%\webapps\syncope\WEB-INF\classes -Dsyncope.log.dir=%CATALINA_HOME%\logs -Dspring.profiles.active=embedded,all -server -Xms2048m -Xmx2048m -XX:NewSize=256m -XX:MaxNewSize=256m -DCATALINA_HOME=%CATALINA_HOME% -Dh2.returnOffsetDateTime=true
