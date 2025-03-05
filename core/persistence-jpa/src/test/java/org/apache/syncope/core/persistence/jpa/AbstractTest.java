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
package org.apache.syncope.core.persistence.jpa;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import jakarta.persistence.EntityManager;
import java.io.InputStream;
import java.util.Map;
import java.util.Properties;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.oracle.OracleContainer;

@SpringJUnitConfig(classes = { MasterDomain.class, PersistenceTestContext.class })
@TestPropertySource("classpath:core-test.properties")
public abstract class AbstractTest {

    private static String JDBC_DRIVER;

    private static String DATABASE_PLATFORM;

    private static String ORM = "META-INF/spring-orm.xml";

    private static String INDEXES = "classpath:META-INF/indexes.xml";

    private static String VIEWS = "classpath:META-INF/views.xml";

    private static Supplier<Object> JDBC_URL_SUPPLIER;

    private static Supplier<Object> JDBC2_URL_SUPPLIER;

    private static Supplier<Object> DB_USER_SUPPLIER = () -> "syncope";

    private static Supplier<Object> DB_PWD_SUPPLIER = () -> "syncope";

    private static Supplier<Object> DB2_USER_SUPPLIER = () -> "syncope";

    private static Supplier<Object> DB2_PWD_SUPPLIER = () -> "syncope";

    private static boolean classExists(final String name) {
        try {
            Class.forName(name, false, AbstractTest.class.getClassLoader());
            return true;
        } catch (ClassNotFoundException e) {
            // ignore
            return false;
        }
    }

    static {
        String dockerMySQLVersion = null;
        String dockerMariaDBVersion = null;
        String dockerOracleVersion = null;
        try (InputStream propStream = AbstractTest.class.getResourceAsStream("/test.properties")) {
            Properties props = new Properties();
            props.load(propStream);

            dockerMySQLVersion = props.getProperty("docker.mysql.version");
            dockerMariaDBVersion = props.getProperty("docker.mariadb.version");
            dockerOracleVersion = props.getProperty("docker.oracle.version");
        } catch (Exception e) {
            fail("Could not load /test.properties", e);
        }
        assertNotNull(dockerMySQLVersion);
        assertNotNull(dockerMariaDBVersion);
        assertNotNull(dockerOracleVersion);

        if (classExists("org.postgresql.Driver")) {
            JDBC_DRIVER = "org.postgresql.Driver";
            DATABASE_PLATFORM = "org.apache.openjpa.jdbc.sql.PostgresDictionary";
            ORM = "META-INF/spring-orm.xml";
            INDEXES = "classpath:META-INF/indexes.xml";
            VIEWS = "classpath:META-INF/views.xml";

            try {
                EmbeddedPostgres pg = EmbeddedPostgres.builder().start();
                JdbcTemplate jdbcTemplate = new JdbcTemplate(pg.getPostgresDatabase());
                Stream.of("syncope", "syncopetwo").forEach(key -> {
                    jdbcTemplate.execute("CREATE DATABASE " + key);

                    jdbcTemplate.execute("CREATE USER " + key + " WITH PASSWORD '" + key + "'");
                    jdbcTemplate.execute("ALTER DATABASE " + key + " OWNER TO " + key);
                });

                JDBC_URL_SUPPLIER = () -> pg.getJdbcUrl("syncope", "syncope") + "&stringtype=unspecified";
                JDBC2_URL_SUPPLIER = () -> pg.getJdbcUrl("syncopetwo", "syncopetwo") + "&stringtype=unspecified";
                DB2_USER_SUPPLIER = () -> "syncopetwo";
                DB2_PWD_SUPPLIER = () -> "syncopetwo";
            } catch (Exception e) {
                fail("Could not setup PostgreSQL databases", e);
            }
        } else if (classExists("com.mysql.cj.jdbc.Driver")) {
            JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
            DATABASE_PLATFORM = "org.apache.openjpa.jdbc.sql.MySQLDictionary("
                    + "blobTypeName=LONGBLOB,dateFractionDigits=3,useSetStringForClobs=true)";
            ORM = "META-INF/mysql/spring-orm.xml";
            INDEXES = "classpath:META-INF/mysql/indexes.xml";
            VIEWS = "classpath:META-INF/mysql/views.xml";

            MySQLContainer<?> masterDomain = new MySQLContainer<>("mysql:" + dockerMySQLVersion).
                    withTmpFs(Map.of("/var/lib/mysql", "rw")).
                    withDatabaseName("syncope").withPassword("syncope").withUsername("syncope").
                    withUrlParam("characterEncoding", "UTF-8").
                    withReuse(true);
            masterDomain.start();
            JDBC_URL_SUPPLIER = masterDomain::getJdbcUrl;

            MySQLContainer<?> twoDomain = new MySQLContainer<>("mysql:" + dockerMySQLVersion).
                    withTmpFs(Map.of("/var/lib/mysql", "rw")).
                    withDatabaseName("syncope").withPassword("syncope").withUsername("syncope").
                    withUrlParam("characterEncoding", "UTF-8").
                    withReuse(true);
            twoDomain.start();
            JDBC2_URL_SUPPLIER = twoDomain::getJdbcUrl;
        } else if (classExists("org.mariadb.jdbc.Driver")) {
            JDBC_DRIVER = "org.mariadb.jdbc.Driver";
            DATABASE_PLATFORM = "org.apache.openjpa.jdbc.sql.MariaDBDictionary("
                    + "blobTypeName=LONGBLOB,dateFractionDigits=3)";
            ORM = "META-INF/mariadb/spring-orm.xml";
            INDEXES = "classpath:META-INF/mariadb/indexes.xml";
            VIEWS = "classpath:META-INF/mariadb/views.xml";

            MariaDBContainer<?> masterDomain = new MariaDBContainer<>("mariadb:" + dockerMariaDBVersion).
                    withTmpFs(Map.of("/var/lib/mysql", "rw")).
                    withDatabaseName("syncope").withPassword("syncope").withUsername("syncope").
                    withUrlParam("characterEncoding", "UTF-8").
                    withReuse(true);
            masterDomain.start();
            JDBC_URL_SUPPLIER = masterDomain::getJdbcUrl;

            MariaDBContainer<?> twoDomain = new MariaDBContainer<>("mariadb:" + dockerMariaDBVersion).
                    withTmpFs(Map.of("/var/lib/mysql", "rw")).
                    withDatabaseName("syncope").withPassword("syncope").withUsername("syncope").
                    withUrlParam("characterEncoding", "UTF-8").
                    withReuse(true);
            twoDomain.start();
            JDBC2_URL_SUPPLIER = twoDomain::getJdbcUrl;

            // https://jira.mariadb.org/browse/MDEV-27898
            DB_USER_SUPPLIER = () -> "root";
            DB_PWD_SUPPLIER = () -> "syncope";
            DB2_USER_SUPPLIER = () -> "root";
            DB2_PWD_SUPPLIER = () -> "syncope";
        } else if (classExists("oracle.jdbc.OracleDriver")) {
            JDBC_DRIVER = "oracle.jdbc.OracleDriver";
            DATABASE_PLATFORM = "org.apache.openjpa.jdbc.sql.OracleDictionary";
            ORM = "META-INF/oracle/spring-orm.xml";
            INDEXES = "classpath:META-INF/oracle/indexes.xml";
            VIEWS = "classpath:META-INF/oracle/views.xml";

            OracleContainer masterDomain = new OracleContainer("gvenzl/oracle-free:" + dockerOracleVersion).
                    withDatabaseName("syncope").withPassword("syncope").withUsername("syncope").
                    withReuse(true);
            masterDomain.start();
            JDBC_URL_SUPPLIER = masterDomain::getJdbcUrl;

            OracleContainer twoDomain = new OracleContainer("gvenzl/oracle-free:" + dockerOracleVersion).
                    withDatabaseName("syncope").withPassword("syncope").withUsername("syncope").
                    withReuse(true);
            twoDomain.start();
            JDBC2_URL_SUPPLIER = twoDomain::getJdbcUrl;
        }
    }

    @DynamicPropertySource
    static void configureProperties(final DynamicPropertyRegistry registry) {
        registry.add("JDBC_DRIVER", () -> JDBC_DRIVER);
        registry.add("DATABASE_PLATFORM", () -> DATABASE_PLATFORM);
        registry.add("ORM", () -> ORM);
        registry.add("INDEXES", () -> INDEXES);
        registry.add("VIEWS", () -> VIEWS);

        registry.add("DB_URL", JDBC_URL_SUPPLIER::get);
        registry.add("DB_USER", DB_USER_SUPPLIER::get);
        registry.add("DB_PASSWORD", DB_PWD_SUPPLIER::get);

        registry.add("DB2_URL", JDBC2_URL_SUPPLIER::get);
        registry.add("DB2_USER", DB2_USER_SUPPLIER::get);
        registry.add("DB2_PASSWORD", DB2_PWD_SUPPLIER::get);
    }

    @Autowired
    protected EntityFactory entityFactory;

    @Autowired
    protected AnyUtilsFactory anyUtilsFactory;

    @Autowired
    protected EntityManager entityManager;

}
