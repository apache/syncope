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

import static org.junit.jupiter.api.Assertions.fail;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import jakarta.persistence.EntityManager;
import java.util.function.Supplier;
import org.apache.syncope.common.lib.types.EntitlementsHolder;
import org.apache.syncope.common.lib.types.IdRepoEntitlement;
import org.apache.syncope.core.persistence.jpa.MasterDomain;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = { MasterDomain.class, IdRepoLogicTestContext.class })
public abstract class AbstractTest {

    private static Supplier<Object> JDBC_URL_SUPPLIER;

    private static final Supplier<Object> DB_CRED_SUPPLIER = () -> "syncope";

    static {
        try {
            EmbeddedPostgres pg = EmbeddedPostgres.builder().start();
            JdbcTemplate jdbcTemplate = new JdbcTemplate(pg.getPostgresDatabase());
            jdbcTemplate.execute("CREATE DATABASE syncope");

            jdbcTemplate.execute("CREATE USER syncope WITH PASSWORD 'syncope'");
            jdbcTemplate.execute("ALTER DATABASE syncope OWNER TO syncope");

            JDBC_URL_SUPPLIER = () -> pg.getJdbcUrl("syncope", "syncope") + "&stringtype=unspecified";
        } catch (Exception e) {
            fail("Could not setup PostgreSQL database", e);
        }
    }

    @DynamicPropertySource
    static void configureProperties(final DynamicPropertyRegistry registry) {
        registry.add("DB_URL", JDBC_URL_SUPPLIER);
        registry.add("DB_USER", DB_CRED_SUPPLIER);
        registry.add("DB_PASSWORD", DB_CRED_SUPPLIER);
    }

    @BeforeAll
    public static void init() {
        EntitlementsHolder.getInstance().addAll(IdRepoEntitlement.values());
    }

    @Autowired
    protected EntityManager entityManager;

}
