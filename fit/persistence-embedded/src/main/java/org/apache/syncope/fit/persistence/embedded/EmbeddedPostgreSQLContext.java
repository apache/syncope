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
package org.apache.syncope.fit.persistence.embedded;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import java.io.IOException;
import java.net.Socket;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.util.ReflectionUtils;

public class EmbeddedPostgreSQLContext {

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedPostgreSQLContext.class);

    private static final String DEFAULT_POSTGRES_HOST = "localhost";

    private static final int DEFAULT_POSTGRES_PORT = 5432;

    private static final String DEFAULT_POSTGRES_USER = "postgres";

    private static final String DEFAULT_POSTGRES_PASSWORD = "root";

    @Value("${embedded.databases:syncope}")
    private String[] embeddedDatabases;

    private void initDatabases(final Connection conn) throws SQLException {
        LOG.info("Creating embedded databases: {}", List.of(embeddedDatabases));

        try {
            for (String key : embeddedDatabases) {
                try (Statement stmt = conn.createStatement()) {
                    ResultSet resultSet = stmt.executeQuery(
                            "SELECT COUNT(*) FROM pg_database WHERE datname = '" + key + "'");
                    resultSet.next();
                    if (resultSet.getInt(1) <= 0) {
                        stmt.execute("CREATE DATABASE " + key);
                    } else {
                        LOG.info("Database {} exists", key);
                    }

                    resultSet = stmt.executeQuery("SELECT COUNT(*) FROM pg_user WHERE usename = '" + key + "'");
                    resultSet.next();
                    if (resultSet.getInt(1) <= 0) {
                        stmt.execute("CREATE USER " + key + " WITH PASSWORD '" + key + "'");
                        stmt.execute("ALTER DATABASE " + key + " OWNER TO " + key);
                    } else {
                        LOG.info("User {} exists", key);
                    }
                } catch (SQLException e) {
                    LOG.error("While creating database {}", key, e);
                }
            }
        } finally {
            if (conn != null) {
                conn.close();
            }
        }
    }

    @ConditionalOnProperty(prefix = "persistence", name = "db-type", havingValue = "POSTGRESQL", matchIfMissing = true)
    @Bean(name = "MasterDataSource")
    public JndiObjectFactoryBean masterDataSource(final Environment env) throws SQLException {
        String dbhost = env.getProperty("POSTGRES_HOST", DEFAULT_POSTGRES_HOST);
        int dbport = env.getProperty("POSTGRES_PORT", int.class, DEFAULT_POSTGRES_PORT);

        Connection conn;
        DataSource defaultMasterDS;
        try (Socket s = new Socket(dbhost, dbport)) {
            LOG.info("PostgreSQL instance found");

            Class.forName("org.postgresql.Driver");
            conn = DriverManager.getConnection(
                    "jdbc:postgresql://" + dbhost + ":" + dbport + "/postgres",
                    env.getProperty("POSTGRES_USER", DEFAULT_POSTGRES_USER),
                    env.getProperty("POSTGRES_PASSWORD", DEFAULT_POSTGRES_PASSWORD));

            initDatabases(conn);

            Class<?> clazz = Class.forName("org.postgresql.ds.PGSimpleDataSource");
            defaultMasterDS = (DataSource) clazz.getConstructor().newInstance();
            ReflectionUtils.findMethod(clazz, "setUrl", String.class).invoke(
                    defaultMasterDS,
                    "jdbc:postgresql://" + dbhost + ":" + dbport + "/syncope?stringtype=unspecified");
            ReflectionUtils.findMethod(clazz, "setUser", String.class).invoke(defaultMasterDS, "syncope");
            ReflectionUtils.findMethod(clazz, "setPassword", String.class).invoke(defaultMasterDS, "syncope");
        } catch (IOException ioe) {
            LOG.info("Starting embedded PostgreSQL");

            try {
                EmbeddedPostgres pg = EmbeddedPostgres.builder().setPort(dbport).start();
                conn = pg.getPostgresDatabase().getConnection();

                initDatabases(conn);

                defaultMasterDS = pg.getDatabase("syncope", "syncope", Map.of("stringtype", "unspecified"));
            } catch (IOException e) {
                throw new IllegalStateException("Could not start embedded PostgreSQL", e);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unexpected error while setting up embedded persistence", e);
        }

        JndiObjectFactoryBean masterDataSource = new JndiObjectFactoryBean();
        masterDataSource.setJndiName("java:comp/env/jdbc/syncopeMasterDataSource");
        masterDataSource.setDefaultObject(defaultMasterDS);
        return masterDataSource;
    }
}
