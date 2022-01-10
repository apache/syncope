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
import java.util.Map;
import java.util.Objects;

import javax.sql.DataSource;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.jpa.spring.CommonEntityManagerFactoryConf;
import org.apache.syncope.core.persistence.jpa.spring.DomainEntityManagerFactoryBean;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.datasource.init.DataSourceInitializer;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.vendor.OpenJpaVendorAdapter;
import org.springframework.transaction.PlatformTransactionManager;

@EnableConfigurationProperties(PersistenceProperties.class)
@Configuration(proxyBeanMethods = false)
public class MasterDomain {

    @ConditionalOnMissingBean(name = "MasterDataSource")
    @Bean(name = "MasterDataSource")
    public JndiObjectFactoryBean masterDataSource(final PersistenceProperties props) {
        HikariConfig hikariConfig = new HikariConfig();
        hikariConfig.setDriverClassName(props.getDomain().get(0).getJdbcDriver());
        hikariConfig.setJdbcUrl(props.getDomain().get(0).getJdbcURL());
        hikariConfig.setUsername(props.getDomain().get(0).getDbUsername());
        hikariConfig.setPassword(props.getDomain().get(0).getDbPassword());
        hikariConfig.setTransactionIsolation(props.getDomain().get(0).getTransactionIsolation().name());
        hikariConfig.setMaximumPoolSize(props.getDomain().get(0).getPoolMaxActive());
        hikariConfig.setMinimumIdle(props.getDomain().get(0).getPoolMinIdle());

        JndiObjectFactoryBean masterDataSource = new JndiObjectFactoryBean();
        masterDataSource.setJndiName("java:comp/env/jdbc/syncopeMasterDataSource");
        masterDataSource.setDefaultObject(new HikariDataSource(hikariConfig));
        return masterDataSource;
    }

    @ConditionalOnMissingBean(name = "MasterDataSourceInitializer")
    @Bean(name = "MasterDataSourceInitializer")
    public DataSourceInitializer masterDataSourceInitializer(
        final PersistenceProperties props,
        @Qualifier("MasterDataSource")
        final JndiObjectFactoryBean masterDataSource) {
        ResourceDatabasePopulator databasePopulator = new ResourceDatabasePopulator();
        databasePopulator.setContinueOnError(true);
        databasePopulator.setIgnoreFailedDrops(true);
        databasePopulator.setSqlScriptEncoding("UTF-8");
        databasePopulator.addScript(new ClassPathResource("/audit/" + props.getDomain().get(0).getAuditSql()));

        DataSourceInitializer dataSourceInitializer = new DataSourceInitializer();
        dataSourceInitializer.setDataSource((DataSource) Objects.requireNonNull(masterDataSource.getObject()));
        dataSourceInitializer.setEnabled(true);
        dataSourceInitializer.setDatabasePopulator(databasePopulator);
        return dataSourceInitializer;
    }

    @ConditionalOnMissingBean(name = "MasterEntityManagerFactory")
    @DependsOn("commonEMFConf")
    @Bean(name = "MasterEntityManagerFactory")
    public DomainEntityManagerFactoryBean masterEntityManagerFactory(
        final PersistenceProperties props,
        @Qualifier("MasterDataSource")
        final JndiObjectFactoryBean masterDataSource,
        final CommonEntityManagerFactoryConf commonEMFConf) {
        OpenJpaVendorAdapter vendorAdapter = new OpenJpaVendorAdapter();
        vendorAdapter.setShowSql(false);
        vendorAdapter.setGenerateDdl(true);
        vendorAdapter.setDatabasePlatform(props.getDomain().get(0).getDatabasePlatform());

        DomainEntityManagerFactoryBean masterEntityManagerFactory = new DomainEntityManagerFactoryBean();
        masterEntityManagerFactory.setMappingResources(props.getDomain().get(0).getOrm());
        masterEntityManagerFactory.setPersistenceUnitName(SyncopeConstants.MASTER_DOMAIN);
        masterEntityManagerFactory.setDataSource(Objects.requireNonNull((DataSource) masterDataSource.getObject()));
        masterEntityManagerFactory.setJpaVendorAdapter(vendorAdapter);
        masterEntityManagerFactory.setCommonEntityManagerFactoryConf(commonEMFConf);

        if (props.getMetaDataFactory() != null) {
            masterEntityManagerFactory.setJpaPropertyMap(Map.of(
                    "openjpa.MetaDataFactory",
                    props.getMetaDataFactory().replace("##orm##", props.getDomain().get(0).getOrm())));
        }

        return masterEntityManagerFactory;
    }

    @ConditionalOnMissingBean(name = "MasterTransactionManager")
    @Bean(name = { "MasterTransactionManager", "Master" })
    public PlatformTransactionManager transactionManager(
        @Qualifier("MasterEntityManagerFactory")
        final DomainEntityManagerFactoryBean masterEntityManagerFactory) {
        return new JpaTransactionManager(Objects.requireNonNull(masterEntityManagerFactory.getObject()));
    }

    @Bean(name = "MasterContentXML")
    public InputStream masterContentXML(final ResourceLoader resourceLoader,
                                        final PersistenceProperties props) throws IOException {
        return resourceLoader.getResource(props.getDomain().get(0).getContent()).getInputStream();
    }

    @Bean(name = "MasterKeymasterConfParamsJSON")
    public InputStream masterKeymasterConfParamsJSON(final ResourceLoader resourceLoader,
                                                     final PersistenceProperties props) throws IOException {
        return resourceLoader.getResource(props.getDomain().get(0).getKeymasterConfParams()).getInputStream();
    }

    @Bean(name = "MasterDatabaseSchema")
    public String masterDatabaseSchema(final PersistenceProperties props) {
        return props.getDomain().get(0).getDbSchema();
    }
}
