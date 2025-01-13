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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.conf.JDBCConfigurationImpl;
import org.apache.openjpa.jdbc.schema.FileSchemaFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class PersistenceUpgraderContext {

    @Bean
    public DataSource syncopeDataSource(
            final @Value("${db.jdbcDriverClassName}") String jdbcDriver,
            final @Value("${db.jdbcURL}") String jdbcURL,
            final @Value("${db.username}") String dbUser,
            final @Value("${db.password}") String dbPassword) {

        HikariConfig config = new HikariConfig();
        config.setDriverClassName(jdbcDriver);
        config.setJdbcUrl(jdbcURL);
        config.setUsername(dbUser);
        config.setPassword(dbPassword);
        return new HikariDataSource(config);
    }

    @Bean
    public JDBCConfiguration jdbcConf(
            final DataSource syncopeDataSource,
            final @Value("${db.jdbcDriverClassName}") String jdbcDriver,
            final @Value("${db.dictionary}") String dbDictionary) {

        JDBCConfiguration jdbcConf = new JDBCConfigurationImpl();
        jdbcConf.setConnection2DriverName(jdbcDriver);
        jdbcConf.setDBDictionary(dbDictionary);
        jdbcConf.setConnectionFactory2(syncopeDataSource);

        FileSchemaFactory schemaFactory = new FileSchemaFactory();
        schemaFactory.setConfiguration(jdbcConf);
        schemaFactory.setFile("schema.xml");
        jdbcConf.setSchemaFactory(schemaFactory);

        return jdbcConf;
    }

    @Bean
    public GenerateUpgradeSQL generateUpgradeSQL(final JDBCConfiguration jdbcConf) {
        return new GenerateUpgradeSQL(jdbcConf);
    }
}
