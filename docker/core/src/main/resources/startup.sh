#!/bin/sh

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

cd /opt/syncope/cache
if [ $JCACHE = "ehcache" ]; then
  mkdir ehcache && cd ehcache && unzip ../syncope-core-cache-ehcache-wrap.zip
  LOADER_PATH="$LOADER_PATH,/opt/syncope/cache/ehcache"
elif [[ $JCACHE == hazelcast* ]]; then
  mkdir hazelcast && cd hazelcast && unzip ../syncope-core-cache-hazelcast-wrap.zip
  LOADER_PATH="$LOADER_PATH,/opt/syncope/cache/hazelcast"

  PROVIDER_TYPE="member"
  if [ $JCACHE = "hazelcast_client" ]; then
    PROVIDER_TYPE="client"
  fi
  JAVA_OPTS="$JAVA_OPTS -Dhazelcast.jcache.provider.type=$PROVIDER_TYPE -Dhazelcast.logging.type=slf4j --add-modules java.se --add-exports java.base/jdk.internal.ref=ALL-UNNAMED --add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/sun.nio.ch=ALL-UNNAMED --add-opens java.management/sun.management=ALL-UNNAMED --add-opens jdk.management/com.sun.management.internal=ALL-UNNAMED"
elif [ $JCACHE = "infinispan" ]; then
  mkdir infinispan && cd infinispan && unzip ../syncope-core-cache-infinispan-wrap.zip
  LOADER_PATH="$LOADER_PATH,/opt/syncope/cache/infinispan"
else
  mkdir caffeine && cd caffeine && unzip ../syncope-core-cache-caffeine-wrap.zip
  LOADER_PATH="$LOADER_PATH,/opt/syncope/cache/caffeine"
fi
cd -

exec java $JAVA_OPTS -jar /opt/syncope/lib/syncope.jar
