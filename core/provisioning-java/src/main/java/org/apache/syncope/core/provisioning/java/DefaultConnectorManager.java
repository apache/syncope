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
package org.apache.syncope.core.provisioning.java;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.utils.ConnPoolConfUtils;
import org.apache.syncope.core.provisioning.api.ConnIdBundleManager;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.provisioning.api.data.ConnInstanceDataBinder;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.identityconnectors.common.l10n.CurrentLocale;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.annotation.Transactional;

public class DefaultConnectorManager implements ConnectorManager {

    protected static final Logger LOG = LoggerFactory.getLogger(ConnectorManager.class);

    protected static String getBeanName(final ExternalResource resource) {
        return String.format("connInstance-%s-%S-%s",
                AuthContextUtils.getDomain(), resource.getConnector().getKey(), resource.getKey());
    }

    protected final ConnIdBundleManager connIdBundleManager;

    protected final RealmDAO realmDAO;

    protected final RealmSearchDAO realmSearchDAO;

    protected final ExternalResourceDAO resourceDAO;

    protected final ConnInstanceDataBinder connInstanceDataBinder;

    protected final AsyncConnectorFacade asyncFacade;

    protected final EntityFactory entityFactory;

    public DefaultConnectorManager(
            final ConnIdBundleManager connIdBundleManager,
            final RealmDAO realmDAO,
            final RealmSearchDAO realmSearchDAO,
            final ExternalResourceDAO resourceDAO,
            final ConnInstanceDataBinder connInstanceDataBinder,
            final AsyncConnectorFacade asyncFacade,
            final EntityFactory entityFactory) {

        this.connIdBundleManager = connIdBundleManager;
        this.realmDAO = realmDAO;
        this.realmSearchDAO = realmSearchDAO;
        this.resourceDAO = resourceDAO;
        this.connInstanceDataBinder = connInstanceDataBinder;
        this.asyncFacade = asyncFacade;
        this.entityFactory = entityFactory;
    }

    @Override
    public Optional<Connector> readConnector(final ExternalResource resource) {
        return Optional.ofNullable((Connector) ApplicationContextProvider.getBeanFactory().
                getSingleton(getBeanName(resource)));
    }

    @Override
    public Connector getConnector(final ExternalResource resource) {
        // Try to re-create connector bean from underlying resource (useful for managing failover scenarios)
        return readConnector(resource).orElseGet(() -> {
            registerConnector(resource);
            return (Connector) ApplicationContextProvider.getBeanFactory().getSingleton(getBeanName(resource));
        });
    }

    @Override
    public ConnInstance buildConnInstanceOverride(
            final ConnInstanceTO connInstance,
            final Optional<List<ConnConfProperty>> confOverride,
            final Optional<Set<ConnectorCapability>> capabilitiesOverride) {

        ConnInstance override = entityFactory.newEntity(ConnInstance.class);
        override.setAdminRealm(realmSearchDAO.findByFullPath(connInstance.getAdminRealm()).orElseGet(() -> {
            LOG.warn("Could not find admin Realm {}, reverting to {}",
                    connInstance.getAdminRealm(), SyncopeConstants.ROOT_REALM);
            return realmDAO.getRoot();
        }));
        override.setConnectorName(connInstance.getConnectorName());
        override.setDisplayName(connInstance.getDisplayName());
        override.setBundleName(connInstance.getBundleName());
        override.setVersion(connInstance.getVersion());
        override.setLocation(connInstance.getLocation());
        override.setConf(connInstance.getConf());
        override.getCapabilities().addAll(connInstance.getCapabilities());
        override.setConnRequestTimeout(connInstance.getConnRequestTimeout());

        Map<String, ConnConfProperty> overridable = new HashMap<>();
        List<ConnConfProperty> conf = new ArrayList<>();

        override.getConf().forEach(prop -> {
            if (prop.isOverridable()) {
                overridable.put(prop.getSchema().getName(), prop);
            } else {
                conf.add(prop);
            }
        });

        // add override properties
        confOverride.ifPresent(co -> co.stream().
                filter(prop -> overridable.containsKey(prop.getSchema().getName()) && !prop.getValues().isEmpty()).
                forEach(prop -> {
                    conf.add(prop);
                    overridable.remove(prop.getSchema().getName());
                }));

        // add override properties not substituted
        conf.addAll(overridable.values());

        override.setConf(conf);

        // replace capabilities
        capabilitiesOverride.ifPresent(capabilities -> {
            override.getCapabilities().clear();
            override.getCapabilities().addAll(capabilities);
        });

        Optional.ofNullable(connInstance.getPoolConf()).
                ifPresent(pc -> override.setPoolConf(ConnPoolConfUtils.getConnPoolConf(pc)));

        return override;
    }

    @Override
    public Connector createConnector(final ConnInstance connInstance) {
        return new ConnectorFacadeProxy(connInstance, asyncFacade);
    }

    @Override
    public void registerConnector(final ExternalResource resource) {
        String beanName = getBeanName(resource);

        if (ApplicationContextProvider.getBeanFactory().containsSingleton(beanName)) {
            unregisterConnector(beanName);
        }

        ConnInstance connInstance = buildConnInstanceOverride(
                connInstanceDataBinder.getConnInstanceTO(resource.getConnector()),
                resource.getConfOverride(),
                resource.getCapabilitiesOverride());
        Connector connector = createConnector(connInstance);
        LOG.debug("Connector to be registered: {}", connector);

        ApplicationContextProvider.getBeanFactory().registerSingleton(beanName, connector);
        LOG.debug("Successfully registered bean {}", beanName);
    }

    protected void unregisterConnector(final String id) {
        ApplicationContextProvider.getBeanFactory().destroySingleton(id);
    }

    @Override
    public void unregisterConnector(final ExternalResource resource) {
        String beanName = getBeanName(resource);
        if (ApplicationContextProvider.getBeanFactory().containsSingleton(beanName)) {
            unregisterConnector(beanName);
        }
    }

    @Transactional(readOnly = true)
    @Override
    public void load() {
        // This is needed in order to avoid encoding problems when sending error messages via REST
        CurrentLocale.set(Locale.ENGLISH);

        // Load all connector bundles
        connIdBundleManager.getConnManagers();

        // Load all resource-specific connectors
        int connectors = 0;
        for (ExternalResource resource : resourceDAO.findAll()) {
            LOG.info("Registering resource-connector pair {}-{}", resource, resource.getConnector());
            try {
                registerConnector(resource);

                connectors++;
            } catch (Exception e) {
                LOG.error("While registering resource-connector pair {}-{}", resource, resource.getConnector(), e);
            }
        }

        LOG.info("Done loading {} connectors", connectors);
    }

    @Transactional(readOnly = true)
    @Override
    public void unload() {
        int connectors = 0;
        for (ExternalResource resource : resourceDAO.findAll()) {
            String beanName = getBeanName(resource);
            if (ApplicationContextProvider.getBeanFactory().containsSingleton(beanName)) {
                LOG.info("Unregistering resource-connector pair {}-{}", resource, resource.getConnector());

                getConnector(resource).dispose();
                unregisterConnector(beanName);

                connectors++;
            }
        }

        LOG.info("Done unloading {} connectors", connectors);

        ConnectorFacadeFactory.getInstance().dispose();
        connIdBundleManager.resetConnManagers();
        LOG.info("All connector resources disposed");
    }
}
