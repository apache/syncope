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

import jakarta.persistence.EntityManager;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.jpa.dao.JPAPlainAttrValueDAO;
import org.apache.syncope.core.persistence.jpa.dao.repo.PlainSchemaRepoExtImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.testcontainers.containers.JdbcDatabaseContainer;
import org.testcontainers.containers.MariaDBContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.oracle.OracleContainer;

@SpringJUnitConfig(classes = { MasterDomain.class, PersistenceTestContext.class })
public abstract class AbstractTest {

    private static final Logger LOG = LoggerFactory.getLogger(AbstractTest.class);

    private static String JDBC_DRIVER;

    private static String DATABASE_PLATFORM;

    private static String ORM = "META-INF/spring-orm.xml";

    private static String INDEXES = "classpath:indexes.xml";

    private static JdbcDatabaseContainer<?> MASTER_DOMAIN;

    private static JdbcDatabaseContainer<?> TWO_DOMAIN;

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
        String dockerPostgreSQLVersion = null;
        String dockerMySQLVersion = null;
        String dockerMariaDBVersion = null;
        String dockerOracleVersion = null;
        try (InputStream propStream = AbstractTest.class.getResourceAsStream("/test.properties")) {
            Properties props = new Properties();
            props.load(propStream);

            dockerPostgreSQLVersion = props.getProperty("docker.postgresql.version");
            dockerMySQLVersion = props.getProperty("docker.mysql.version");
            dockerMariaDBVersion = props.getProperty("docker.mariadb.version");
            dockerOracleVersion = props.getProperty("docker.oracle.version");
        } catch (Exception e) {
            LOG.error("Could not load /test.properties", e);
        }
        assertNotNull(dockerPostgreSQLVersion);
        assertNotNull(dockerMySQLVersion);
        assertNotNull(dockerMariaDBVersion);
        assertNotNull(dockerOracleVersion);

        MASTER_DOMAIN = null;
        TWO_DOMAIN = null;
        if (classExists("org.postgresql.Driver")) {
            JDBC_DRIVER = "org.postgresql.Driver";
            DATABASE_PLATFORM = "org.apache.openjpa.jdbc.sql.PostgresDictionary";

            MASTER_DOMAIN = new PostgreSQLContainer<>("postgres:" + dockerPostgreSQLVersion).
                    withTmpFs(Map.of("/var/lib/postgresql/data", "rw")).
                    withDatabaseName("syncope").withPassword("syncope").withUsername("syncope").
                    withUrlParam("stringtype", "unspecified").
                    withReuse(true);
            TWO_DOMAIN = new PostgreSQLContainer<>("postgres:" + dockerPostgreSQLVersion).
                    withTmpFs(Map.of("/var/lib/postgresql/data", "rw")).
                    withDatabaseName("syncope").withPassword("syncope").withUsername("syncope").
                    withUrlParam("stringtype", "unspecified").
                    withReuse(true);
        } else if (classExists("com.mysql.cj.jdbc.Driver")) {
            JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
            DATABASE_PLATFORM = "org.apache.openjpa.jdbc.sql.MySQLDictionary("
                    + "blobTypeName=LONGBLOB,dateFractionDigits=3,useSetStringForClobs=true)";

            MASTER_DOMAIN = new MySQLContainer<>("mysql:" + dockerMySQLVersion).
                    withTmpFs(Map.of("/var/lib/mysql", "rw")).
                    withDatabaseName("syncope").withPassword("syncope").withUsername("syncope").
                    withUrlParam("characterEncoding", "UTF-8").
                    withReuse(true);
            TWO_DOMAIN = new MySQLContainer<>("mysql:" + dockerMySQLVersion).
                    withTmpFs(Map.of("/var/lib/mysql", "rw")).
                    withDatabaseName("syncope").withPassword("syncope").withUsername("syncope").
                    withUrlParam("characterEncoding", "UTF-8").
                    withReuse(true);
        } else if (classExists("org.mariadb.jdbc.Driver")) {
            JDBC_DRIVER = "org.mariadb.jdbc.Driver";
            DATABASE_PLATFORM = "org.apache.openjpa.jdbc.sql.MariaDBDictionary("
                    + "blobTypeName=LONGBLOB,dateFractionDigits=3)";

            MASTER_DOMAIN = new MariaDBContainer<>("mariadb:" + dockerMariaDBVersion).
                    withTmpFs(Map.of("/var/lib/mysql", "rw")).
                    withDatabaseName("syncope").withPassword("syncope").withUsername("syncope").
                    withUrlParam("characterEncoding", "UTF-8").
                    withReuse(true);
            TWO_DOMAIN = new MariaDBContainer<>("mariadb:" + dockerMariaDBVersion).
                    withTmpFs(Map.of("/var/lib/mysql", "rw")).
                    withDatabaseName("syncope").withPassword("syncope").withUsername("syncope").
                    withUrlParam("characterEncoding", "UTF-8").
                    withReuse(true);
        } else if (classExists("oracle.jdbc.OracleDriver")) {
            JDBC_DRIVER = "oracle.jdbc.OracleDriver";
            DATABASE_PLATFORM = "org.apache.openjpa.jdbc.sql.OracleDictionary";
            ORM = "META-INF/spring-orm-oracle.xml";
            INDEXES = "classpath:oracle_indexes.xml";

            MASTER_DOMAIN = new OracleContainer("gvenzl/oracle-free:" + dockerOracleVersion).
                    withDatabaseName("syncope").withPassword("syncope").withUsername("syncope").
                    withReuse(true);
            TWO_DOMAIN = new OracleContainer("gvenzl/oracle-free:" + dockerOracleVersion).
                    withDatabaseName("syncope").withPassword("syncope").withUsername("syncope").
                    withReuse(true);
        }

        if (MASTER_DOMAIN == null) {
            throw new IllegalStateException("Could not inizialize TestContainers for domain Master");
        }
        MASTER_DOMAIN.start();
        if (TWO_DOMAIN == null) {
            throw new IllegalStateException("Could not inizialize TestContainers for domain Two");
        }
        TWO_DOMAIN.start();
    }

    @DynamicPropertySource
    static void configureProperties(final DynamicPropertyRegistry registry) {
        registry.add("JDBC_DRIVER", () -> JDBC_DRIVER);
        registry.add("DATABASE_PLATFORM", () -> DATABASE_PLATFORM);
        registry.add("ORM", () -> ORM);
        registry.add("INDEXES", () -> INDEXES);

        registry.add("DB_URL", MASTER_DOMAIN::getJdbcUrl);
        registry.add("DB_USER", MASTER_DOMAIN::getUsername);
        registry.add("DB_PASSWORD", MASTER_DOMAIN::getPassword);

        registry.add("DB2_URL", TWO_DOMAIN::getJdbcUrl);
        registry.add("DB2_USER", TWO_DOMAIN::getUsername);
        registry.add("DB2_PASSWORD", TWO_DOMAIN::getPassword);
    }

    @Autowired
    protected EntityFactory entityFactory;

    @Autowired
    protected AnyUtilsFactory anyUtilsFactory;

    @Autowired
    protected EntityManager entityManager;

    protected <T extends PlainAttr<?>> Optional<T> findPlainAttr(final String key, final Class<T> reference) {
        return Optional.ofNullable(
                reference.cast(entityManager.find(PlainSchemaRepoExtImpl.getEntityReference(reference), key)));
    }

    protected <T extends PlainAttrValue> Optional<T> findPlainAttrValue(final String key, final Class<T> reference) {
        return Optional.ofNullable(
                reference.cast(entityManager.find(JPAPlainAttrValueDAO.getEntityReference(reference), key)));
    }
}
