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

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.io.InputStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jndi.JndiObjectFactoryBean;

@EnableConfigurationProperties(PersistenceProperties.class)
@Configuration(proxyBeanMethods = false)
public class MasterDomain {

    @ConditionalOnMissingBean(name = "MasterDataSource")
    @Bean(name = "MasterDataSource")
    public JndiObjectFactoryBean masterDataSource(final PersistenceProperties props) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName(props.getDomain().getFirst().getJdbcDriver());
        hikariConfig.setJdbcUrl(props.getDomain().getFirst().getJdbcURL());
        hikariConfig.setUsername(props.getDomain().getFirst().getDbUsername());
        hikariConfig.setPassword(props.getDomain().getFirst().getDbPassword());
        hikariConfig.setTransactionIsolation(props.getDomain().getFirst().getTransactionIsolation().name());
        hikariConfig.setMaximumPoolSize(props.getDomain().getFirst().getPoolMaxActive());
        hikariConfig.setMinimumIdle(props.getDomain().getFirst().getPoolMinIdle());

        JndiObjectFactoryBean masterDataSource = new JndiObjectFactoryBean();
        masterDataSource.setJndiName("java:comp/env/jdbc/syncopeMasterDataSource");
        masterDataSource.setDefaultObject(new HikariDataSource(hikariConfig));
        return masterDataSource;
    }

    @Bean(name = "MasterContentXML")
    public InputStream masterContentXML(
            final ResourceLoader resourceLoader,
            final PersistenceProperties props) throws IOException {

        return resourceLoader.getResource(props.getDomain().getFirst().getContent()).getInputStream();
    }

    @Bean(name = "MasterKeymasterConfParamsJSON")
    public InputStream masterKeymasterConfParamsJSON(
            final ResourceLoader resourceLoader,
            final PersistenceProperties props) throws IOException {

        return resourceLoader.getResource(props.getDomain().getFirst().getKeymasterConfParams()).getInputStream();
    }

    @Bean(name = "MasterDatabaseSchema")
    public String masterDatabaseSchema(final PersistenceProperties props) {
        return props.getDomain().getFirst().getDbSchema();
    }
}
