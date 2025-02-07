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
package org.apache.syncope.core.persistence.jpa.upgrade;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.fail;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.function.Supplier;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.DatabasePopulatorUtils;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = PersistenceUpgraderContext.class)
class GenerateUpgradeSQLTest {

    protected static final Logger LOG = LoggerFactory.getLogger(GenerateUpgradeSQLTest.class);

    protected static Supplier<Object> JDBC_URL_SUPPLIER;

    protected static DataSource SYNCOPE_DS;

    protected static final Supplier<Object> DB_CRED_SUPPLIER = () -> "syncope";

    static {
        try {
            EmbeddedPostgres pg = EmbeddedPostgres.builder().start();
            JdbcTemplate jdbcTemplate = new JdbcTemplate(pg.getPostgresDatabase());
            jdbcTemplate.execute("CREATE DATABASE syncope");

            jdbcTemplate.execute("CREATE USER syncope WITH PASSWORD 'syncope'");
            jdbcTemplate.execute("ALTER DATABASE syncope OWNER TO syncope");

            JDBC_URL_SUPPLIER = () -> pg.getJdbcUrl("syncope", "syncope") + "&stringtype=unspecified";

            SYNCOPE_DS = pg.getDatabase("syncope", "syncope");

            DatabasePopulatorUtils.execute(
                    new ResourceDatabasePopulator(new ClassPathResource("syncope30.pgjsonb.sql")),
                    SYNCOPE_DS);
        } catch (Exception e) {
            fail("Could not setup PostgreSQL database", e);
        }
    }

    @DynamicPropertySource
    static void registerTestProperties(final DynamicPropertyRegistry registry) {
        registry.add("db.jdbcDriverClassName", () -> "org.postgresql.Driver");
        registry.add("db.dictionary", () -> "org.apache.openjpa.jdbc.sql.PostgresDictionary");

        registry.add("db.jdbcURL", JDBC_URL_SUPPLIER);
        registry.add("db.username", DB_CRED_SUPPLIER);
        registry.add("db.password", DB_CRED_SUPPLIER);
    }

    @Autowired
    private GenerateUpgradeSQL generateUpgradeSQL;

    @Test
    void run() throws IOException, SQLException {
        StringWriter out = new StringWriter();

        assertDoesNotThrow(() -> generateUpgradeSQL.run(out));

        LOG.info("SQL upgrade statements:\n{}", out);

        DatabasePopulatorUtils.execute(
                new ResourceDatabasePopulator(new ByteArrayResource(out.toString().getBytes())),
                SYNCOPE_DS);
    }
}
