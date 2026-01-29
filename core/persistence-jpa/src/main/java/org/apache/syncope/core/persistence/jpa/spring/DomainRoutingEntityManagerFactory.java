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
import jakarta.persistence.PersistenceUnitTransactionType;
import jakarta.persistence.PersistenceUnitUtil;
import jakarta.persistence.Query;
import jakarta.persistence.SchemaManager;
import jakarta.persistence.SynchronizationType;
import jakarta.persistence.TypedQueryReference;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.metamodel.Metamodel;
import java.io.Closeable;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Function;
import javax.sql.DataSource;
import org.apache.syncope.common.keymaster.client.api.model.JPADomain;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.jpa.ConnectorManagerRemoteCommitListener;
import org.apache.syncope.core.persistence.jpa.PersistenceProperties;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.orm.jpa.JpaVendorAdapter;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

public class DomainRoutingEntityManagerFactory implements EntityManagerFactory, Closeable {

    protected static final Logger LOG = LoggerFactory.getLogger(DomainRoutingEntityManagerFactory.class);

    protected final CommonEntityManagerFactoryConf commonEMFConf;

    protected final ConnectorManager connectorManager;

    protected final ExternalResourceDAO resourceDAO;

    public DomainRoutingEntityManagerFactory(
            final CommonEntityManagerFactoryConf commonEMFConf,
            final ConnectorManager connectorManager,
            final ExternalResourceDAO resourceDAO) {

        this.commonEMFConf = commonEMFConf;
        this.connectorManager = connectorManager;
        this.resourceDAO = resourceDAO;
    }

    protected final Map<String, EntityManagerFactory> delegates = new ConcurrentHashMap<>();

    protected void addToJpaPropertyMap(
            final DomainEntityManagerFactoryBean emf,
            final JpaVendorAdapter vendorAdapter,
            final String dbSchema,
            final String domain) {

        emf.getJpaPropertyMap().putAll(vendorAdapter.getJpaPropertyMap());

        Optional.ofNullable(dbSchema).
                ifPresent(s -> emf.getJpaPropertyMap().put("hibernate.default_schema", s));

        emf.getJpaPropertyMap().put("hibernate.cache.region_prefix", domain);
    }

    public void master(
            final PersistenceProperties props,
            final JndiObjectFactoryBean dataSource) {

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setShowSql(false);
        vendorAdapter.setGenerateDdl(true);
        vendorAdapter.setDatabasePlatform(props.getDomain().getFirst().getDatabasePlatform());

        DomainEntityManagerFactoryBean emf = new DomainEntityManagerFactoryBean();
        emf.setPersistenceUnitName(SyncopeConstants.MASTER_DOMAIN);
        emf.setMappingResources(props.getDomain().getFirst().getOrm());
        emf.setDataSource(Objects.requireNonNull((DataSource) dataSource.getObject()));
        emf.setJpaVendorAdapter(vendorAdapter);
        emf.setCommonEntityManagerFactoryConf(commonEMFConf);
        emf.setConnectorManagerRemoteCommitListener(new ConnectorManagerRemoteCommitListener(
                connectorManager, resourceDAO, SyncopeConstants.MASTER_DOMAIN));

        addToJpaPropertyMap(
                emf,
                vendorAdapter,
                props.getDomain().getFirst().getDbSchema(),
                SyncopeConstants.MASTER_DOMAIN);

        emf.afterPropertiesSet();

        delegates.put(SyncopeConstants.MASTER_DOMAIN, emf.getObject());
    }

    public void domain(
            final JPADomain domain,
            final DataSource dataSource) {

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        vendorAdapter.setShowSql(false);
        vendorAdapter.setGenerateDdl(true);
        vendorAdapter.setDatabasePlatform(domain.getDatabasePlatform());

        DomainEntityManagerFactoryBean emf = new DomainEntityManagerFactoryBean();
        emf.setPersistenceUnitName(domain.getKey());
        emf.setMappingResources(domain.getOrm());
        emf.setDataSource(dataSource);
        emf.setJpaVendorAdapter(vendorAdapter);
        emf.setCommonEntityManagerFactoryConf(commonEMFConf);
        emf.setConnectorManagerRemoteCommitListener(new ConnectorManagerRemoteCommitListener(
                connectorManager, resourceDAO, domain.getKey()));

        addToJpaPropertyMap(emf, vendorAdapter, domain.getDbSchema(), domain.getKey());

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
        delegates.forEach(this::close);
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

    @Override
    public String getName() {
        return delegate().getName();
    }

    @Override
    public PersistenceUnitTransactionType getTransactionType() {
        return delegate().getTransactionType();
    }

    @Override
    public SchemaManager getSchemaManager() {
        return delegate().getSchemaManager();
    }

    @Override
    public <R> Map<String, TypedQueryReference<R>> getNamedQueries(final Class<R> type) {
        return delegate().getNamedQueries(type);
    }

    @Override
    public <E> Map<String, EntityGraph<? extends E>> getNamedEntityGraphs(final Class<E> type) {
        return delegate().getNamedEntityGraphs(type);
    }

    @Override
    public void runInTransaction(final Consumer<EntityManager> cnsmr) {
        delegate().runInTransaction(cnsmr);
    }

    @Override
    public <R> R callInTransaction(final Function<EntityManager, R> fnctn) {
        return delegate().callInTransaction(fnctn);
    }
}
