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
package org.apache.syncope.core.persistence.jpa.spring;

import jakarta.persistence.Cache;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.Query;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.metamodel.Metamodel;
import java.io.Closeable;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import javax.sql.DataSource;
import org.apache.openjpa.jdbc.conf.JDBCConfiguration;
import org.apache.openjpa.jdbc.meta.MappingRepository;
import org.apache.openjpa.jdbc.meta.MappingTool;
import org.apache.openjpa.lib.conf.Configurations;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.syncope.common.keymaster.client.api.model.Domain;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.jpa.PersistenceProperties;
import org.apache.syncope.core.persistence.jpa.openjpa.ConnectorManagerRemoteCommitListener;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jndi.JndiObjectFactoryBean;

public class DomainRoutingEntityManagerFactory implements EntityManagerFactory, Closeable {

    protected static final Logger LOG = LoggerFactory.getLogger(DomainRoutingEntityManagerFactory.class);

    protected final CommonEntityManagerFactoryConf commonEMFConf;

    public DomainRoutingEntityManagerFactory(final CommonEntityManagerFactoryConf commonEMFConf) {
        this.commonEMFConf = commonEMFConf;
    }

    protected final Map<String, EntityManagerFactory> delegates = new ConcurrentHashMap<>();

    public void master(
            final PersistenceProperties props,
            final JndiObjectFactoryBean dataSource) {

        OpenJpaVendorAdapter vendorAdapter = new OpenJpaVendorAdapter();
        vendorAdapter.setShowSql(false);
        vendorAdapter.setGenerateDdl(true);
        vendorAdapter.setDatabasePlatform(props.getDomain().get(0).getDatabasePlatform());

        DomainEntityManagerFactoryBean emf = new DomainEntityManagerFactoryBean();
        emf.setMappingResources(props.getDomain().get(0).getOrm());
        emf.setPersistenceUnitName(SyncopeConstants.MASTER_DOMAIN);
        emf.setDataSource(Objects.requireNonNull((DataSource) dataSource.getObject()));
        emf.setJpaVendorAdapter(vendorAdapter);
        emf.setCommonEntityManagerFactoryConf(commonEMFConf);
        emf.setConnectorManagerRemoteCommitListener(
                new ConnectorManagerRemoteCommitListener(SyncopeConstants.MASTER_DOMAIN));

        if (props.getMetaDataFactory() != null) {
            emf.setJpaPropertyMap(Map.of(
                    "openjpa.MetaDataFactory",
                    props.getMetaDataFactory().replace("##orm##", props.getDomain().get(0).getOrm())));
        }

        emf.afterPropertiesSet();

        delegates.put(SyncopeConstants.MASTER_DOMAIN, emf.getObject());
    }

    public void domain(
            final Domain domain,
            final DataSource dataSource,
            final String metadataFactory) {

        OpenJpaVendorAdapter vendorAdapter = new OpenJpaVendorAdapter();
        vendorAdapter.setShowSql(false);
        vendorAdapter.setGenerateDdl(true);
        vendorAdapter.setDatabasePlatform(domain.getDatabasePlatform());

        DomainEntityManagerFactoryBean emf = new DomainEntityManagerFactoryBean();
        emf.setMappingResources(domain.getOrm());
        emf.setPersistenceUnitName(domain.getKey());
        emf.setDataSource(dataSource);
        emf.setJpaVendorAdapter(vendorAdapter);
        emf.setCommonEntityManagerFactoryConf(commonEMFConf);
        emf.setConnectorManagerRemoteCommitListener(new ConnectorManagerRemoteCommitListener(domain.getKey()));

        if (metadataFactory != null) {
            emf.setJpaPropertyMap(Map.of(
                    "openjpa.MetaDataFactory",
                    metadataFactory.replace("##orm##", domain.getOrm())));
        }

        emf.afterPropertiesSet();

        delegates.put(domain.getKey(), emf.getObject());
    }

    public void remove(final String domain) {
        EntityManagerFactory emf = delegates.remove(domain);
        close(domain, emf);
    }

    protected EntityManagerFactory delegate() {
        return delegates.computeIfAbsent(AuthContextUtils.getDomain(), domain -> {
            throw new IllegalStateException("Could not find EntityManagerFactory for domain " + domain);
        });
    }

    public void initJPASchema() {
        OpenJPAEntityManagerFactorySPI emfspi = delegate().unwrap(OpenJPAEntityManagerFactorySPI.class);
        JDBCConfiguration jdbcConf = (JDBCConfiguration) emfspi.getConfiguration();

        MappingRepository mappingRepo = jdbcConf.getMappingRepositoryInstance();
        Collection<Class<?>> classes = mappingRepo.loadPersistentTypes(false, getClass().getClassLoader());

        String action = "buildSchema(ForeignKeys=true)";
        String props = Configurations.getProperties(action);
        action = Configurations.getClassName(action);
        MappingTool mappingTool = new MappingTool(jdbcConf, action, false, getClass().getClassLoader());
        Configurations.configureInstance(mappingTool, jdbcConf, props, "SynchronizeMappings");

        // initialize the schema
        classes.forEach(mappingTool::run);

        mappingTool.record();
    }

    @Override
    public EntityManager createEntityManager() {
        return delegate().createEntityManager();
    }

    @SuppressWarnings("rawtypes")
    @Override
    public EntityManager createEntityManager(final Map map) {
        return delegate().createEntityManager(map);
    }

    @Override
    public EntityManager createEntityManager(final SynchronizationType synchronizationType) {
        return delegate().createEntityManager(synchronizationType);
    }

    @SuppressWarnings("rawtypes")
    @Override
    public EntityManager createEntityManager(final SynchronizationType synchronizationType, final Map map) {
        return delegate().createEntityManager(synchronizationType, map);
    }

    @Override
    public CriteriaBuilder getCriteriaBuilder() {
        return delegate().getCriteriaBuilder();
    }

    @Override
    public Metamodel getMetamodel() {
        return delegate().getMetamodel();
    }

    @Override
    public boolean isOpen() {
        return delegate().isOpen();
    }

    protected void close(final String domain, final EntityManagerFactory emf) {
        LOG.info("Closing JPA EntityManagerFactory for persistence unit '{}'", domain);
        try {
            emf.close();
        } catch (Exception e) {
            LOG.error("While closing EntityManagerFactory for persistence unit '{}'", domain, e);
        }
    }

    @Override
    public void close() {
        delegates.forEach((domain, emf) -> close(domain, emf));
    }

    @Override
    public Map<String, Object> getProperties() {
        return delegate().getProperties();
    }

    @Override
    public Cache getCache() {
        return delegate().getCache();
    }

    @Override
    public PersistenceUnitUtil getPersistenceUnitUtil() {
        return delegate().getPersistenceUnitUtil();
    }

    @Override
    public void addNamedQuery(final String name, final Query query) {
        delegate().addNamedQuery(name, query);
    }

    @Override
    public <T> T unwrap(final Class<T> cls) {
        return delegate().unwrap(cls);
    }

    @Override
    public <T> void addNamedEntityGraph(final String graphName, final EntityGraph<T> entityGraph) {
        delegate().addNamedEntityGraph(graphName, entityGraph);
    }
}
