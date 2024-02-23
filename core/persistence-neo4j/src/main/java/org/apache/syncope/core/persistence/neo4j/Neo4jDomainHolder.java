/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.persistence.neo4j;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.neo4j.driver.Driver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Neo4jDomainHolder implements DomainHolder<Driver> {

    private static final Logger LOG = LoggerFactory.getLogger(DomainHolder.class);

    private final Map<String, Driver> domains = new ConcurrentHashMap<>();

    @Override
    public Map<String, Driver> getDomains() {
        return domains;
    }

    @Override
    public Map<String, Boolean> getHealthInfo() {
        Map<String, Boolean> healthInfo = new HashMap<>(domains.size());

        domains.forEach((domain, driver) -> {
            try {
                driver.verifyConnectivity();
                healthInfo.put(domain, true);
            } catch (Exception e) {
                healthInfo.put(domain, false);
                LOG.debug("When attempting to connect to Domain {}", domain, e);
            }
        });

        return healthInfo;
    }
}
