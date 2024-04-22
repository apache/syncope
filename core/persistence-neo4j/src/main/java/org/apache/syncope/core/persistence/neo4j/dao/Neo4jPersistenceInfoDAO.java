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
package org.apache.syncope.core.persistence.neo4j.dao;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.syncope.core.persistence.api.dao.PersistenceInfoDAO;
import org.neo4j.driver.Driver;
import org.neo4j.driver.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Neo4jPersistenceInfoDAO implements PersistenceInfoDAO {

    protected static final Logger LOG = LoggerFactory.getLogger(PersistenceInfoDAO.class);

    protected static final String CYPHER =
            """
CALL dbms.components() YIELD versions, name, edition WHERE name = 'Neo4j Kernel' RETURN edition, versions[0] as version
""";

    protected final Driver driver;

    public Neo4jPersistenceInfoDAO(final Driver driver) {
        this.driver = driver;
    }

    @Override
    public Map<String, Object> info() {
        Map<String, Object> result = new LinkedHashMap<>();

        try (Session session = driver.session()) {
            result.putAll(session.run(CYPHER).next().asMap());
        }

        if (driver.isMetricsEnabled()) {
            List<Map<String, Object>> metrics = new ArrayList<>();
            driver.metrics().connectionPoolMetrics().forEach(m -> {
                Map<String, Object> metric = new LinkedHashMap<>();
                metric.put("id", m.id());
                metric.put("inUse", m.inUse());
                metric.put("idle", m.idle());
                metric.put("created", m.created());
                metric.put("failedToCreate", m.failedToCreate());
                metric.put("closed", m.closed());
                metric.put("acquiring", m.acquiring());
                metric.put("acquired", m.acquired());
                metric.put("timedOutToAcquire", m.timedOutToAcquire());
                metric.put("totalAcquisitionTime", m.totalAcquisitionTime());
                metric.put("totalConnectionTime", m.totalConnectionTime());
                metric.put("totalInUseTime", m.totalInUseTime());
                metric.put("totalInUseCount", m.totalInUseCount());
                metrics.add(metric);
            });

            result.put("metrics", metrics);
        }

        return result;
    }
}
