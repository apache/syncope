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
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.jndi.JndiObjectFactoryBean;

public class EmbeddedPostgreSQLContext {

    private static final Logger LOG = LoggerFactory.getLogger(EmbeddedPostgreSQLContext.class);

    @Value("${embedded.databases:syncope}")
    private String[] embeddedDatabases;

    @ConditionalOnClass(name = "org.postgresql.Driver")
    @Bean(name = "MasterDataSource")
    public JndiObjectFactoryBean masterDataSource() {
        LOG.info("Creating embedded databases: {}", List.of(embeddedDatabases));

        try {
            EmbeddedPostgres pg = EmbeddedPostgres.builder().setPort(5432).start();

            Stream.of(embeddedDatabases).forEach(key -> {
                try (Connection conn = pg.getPostgresDatabase().getConnection();
                        Statement stmt = conn.createStatement()) {

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
            });

            JndiObjectFactoryBean masterDataSource = new JndiObjectFactoryBean();
            masterDataSource.setJndiName("java:comp/env/jdbc/syncopeMasterDataSource");
            masterDataSource.setDefaultObject(pg.getDatabase("syncope", "syncope"));
            return masterDataSource;
        } catch (IOException e) {
            throw new IllegalStateException("Could not start embedded PostgreSQL", e);
        }
    }
}
