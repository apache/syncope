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

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.Neo4jContainer;

@SpringJUnitConfig(classes = { MasterDomain.class, PersistenceTestContext.class })
@DirtiesContext
public abstract class AbstractTest {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractTest.class);

    private static final Neo4jContainer<?> MASTER_DOMAIN;

    private static final Neo4jContainer<?> TWO_DOMAIN;

    static {
        String dockerVersion = null;
        try (InputStream propStream = AbstractTest.class.getResourceAsStream("/test.properties")) {
            Properties props = new Properties();
            props.load(propStream);

            dockerVersion = props.getProperty("docker.neo4j.version");
        } catch (Exception e) {
            LOG.error("Could not load /test.properties", e);
        }
        assertNotNull(dockerVersion);

        MASTER_DOMAIN = new Neo4jContainer<>("neo4j:" + dockerVersion).
                withTmpFs(Map.of(
                        "/data", "rw",
                        "/logs", "rw",
                        "/var/lib/neo4j/data", "rw",
                        "/var/lib/neo4j/logs", "rw",
                        "/var/lib/neo4j/metrics", "rw")).
                withoutAuthentication().
                withPlugins("apoc").
                withReuse(true);
        MASTER_DOMAIN.start();
        TWO_DOMAIN = new Neo4jContainer<>("neo4j:" + dockerVersion).
                withTmpFs(Map.of(
                        "/data", "rw",
                        "/logs", "rw",
                        "/var/lib/neo4j/data", "rw",
                        "/var/lib/neo4j/logs", "rw",
                        "/var/lib/neo4j/metrics", "rw")).
                withoutAuthentication().
                withPlugins("apoc").
                withReuse(true);
        TWO_DOMAIN.start();
    }

    @DynamicPropertySource
    static void configureProperties(final DynamicPropertyRegistry registry) {
        registry.add("BOLT_URL", MASTER_DOMAIN::getBoltUrl);

        registry.add("BOLT2_URL", TWO_DOMAIN::getBoltUrl);
    }

    @Autowired
    protected EntityFactory entityFactory;

    @Autowired
    protected AnyUtilsFactory anyUtilsFactory;

}
