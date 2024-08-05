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
package org.apache.syncope.core.logic;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import jakarta.persistence.EntityManager;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import org.apache.syncope.common.lib.types.EntitlementsHolder;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.jpa.MasterDomain;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.PostgreSQLContainer;

@SpringJUnitConfig(classes = { MasterDomain.class, IdRepoLogicTestContext.class })
public abstract class AbstractTest {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractTest.class);

    private static final PostgreSQLContainer<?> MASTER_DOMAIN;

    static {
        String dockerPostgreSQLVersion = null;
        try (InputStream propStream = AbstractTest.class.getResourceAsStream("/test.properties")) {
            Properties props = new Properties();
            props.load(propStream);

            dockerPostgreSQLVersion = props.getProperty("docker.postgresql.version");
        } catch (Exception e) {
            LOG.error("Could not load /test.properties", e);
        }
        assertNotNull(dockerPostgreSQLVersion);

        MASTER_DOMAIN = new PostgreSQLContainer<>("postgres:" + dockerPostgreSQLVersion).
                withTmpFs(Map.of("/var/lib/postgresql/data", "rw")).
                withDatabaseName("syncope").withPassword("syncope").withUsername("syncope").
                withUrlParam("stringtype", "unspecified").
                withReuse(true);
        MASTER_DOMAIN.start();
    }

    @DynamicPropertySource
    static void configureProperties(final DynamicPropertyRegistry registry) {
        registry.add("DB_URL", MASTER_DOMAIN::getJdbcUrl);
        registry.add("DB_USER", MASTER_DOMAIN::getUsername);
        registry.add("DB_PASSWORD", MASTER_DOMAIN::getPassword);
    }

    @BeforeAll
    public static void init() {
        EntitlementsHolder.getInstance().addAll(IdRepoEntitlement.values());
    }

    @Autowired
    protected EntityManager entityManager;

}
