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
package org.apache.syncope.core.provisioning.java.data;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ConnPoolConfTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ConnConfPropSchema;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.core.persistence.api.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.provisioning.api.ConnIdBundleManager;
import org.apache.syncope.core.provisioning.api.data.ConnInstanceDataBinder;
import org.apache.syncope.core.provisioning.api.utils.ConnPoolConfUtils;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConfigurationProperty;
import org.identityconnectors.framework.impl.api.ConfigurationPropertyImpl;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnInstanceDataBinderImpl implements ConnInstanceDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(ConnInstanceDataBinder.class);

    protected final ConnIdBundleManager connIdBundleManager;

    protected final ConnInstanceDAO connInstanceDAO;

    protected final RealmDAO realmDAO;

    protected final EntityFactory entityFactory;

    public ConnInstanceDataBinderImpl(
            final ConnIdBundleManager connIdBundleManager,
            final ConnInstanceDAO connInstanceDAO,
            final RealmDAO realmDAO,
            final EntityFactory entityFactory) {

        this.connIdBundleManager = connIdBundleManager;
        this.connInstanceDAO = connInstanceDAO;
        this.realmDAO = realmDAO;
        this.entityFactory = entityFactory;
    }

    @Override
    public ConnInstance getConnInstance(final ConnInstanceTO connInstanceTO) {
        SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);

        if (connInstanceTO.getLocation() == null) {
            sce.getElements().add("location");
        }

        if (connInstanceTO.getBundleName() == null) {
            sce.getElements().add("bundlename");
        }

        if (connInstanceTO.getVersion() == null) {
            sce.getElements().add("bundleversion");
        }

        if (connInstanceTO.getConnectorName() == null) {
            sce.getElements().add("connectorname");
        }

        if (connInstanceTO.getConf().isEmpty()) {
            sce.getElements().add("configuration");
        }

        ConnInstance connInstance = entityFactory.newEntity(ConnInstance.class);

        connInstance.setBundleName(connInstanceTO.getBundleName());
        connInstance.setConnectorName(connInstanceTO.getConnectorName());
        connInstance.setVersion(connInstanceTO.getVersion());
        connInstance.setDisplayName(connInstanceTO.getDisplayName());
        connInstance.setConnRequestTimeout(connInstanceTO.getConnRequestTimeout());
        connInstance.getCapabilities().addAll(connInstanceTO.getCapabilities());

        if (connInstanceTO.getAdminRealm() != null) {
            connInstance.setAdminRealm(realmDAO.findByFullPath(connInstanceTO.getAdminRealm()));
        }
        if (connInstance.getAdminRealm() == null) {
            sce.getElements().add("Invalid or null realm specified: " + connInstanceTO.getAdminRealm());
        }
        if (connInstanceTO.getLocation() != null) {
            connInstance.setLocation(connInstanceTO.getLocation());
        }
        connInstance.setConf(connInstanceTO.getConf());
        if (connInstanceTO.getPoolConf() != null) {
            connInstance.setPoolConf(
                    ConnPoolConfUtils.getConnPoolConf(connInstanceTO.getPoolConf(), entityFactory.newConnPoolConf()));
        }

        // Throw exception if there is at least one element set
        if (!sce.isEmpty()) {
            throw sce;
        }

        return connInstance;
    }

    @Override
    public ConnInstance update(final ConnInstanceTO connInstanceTO) {
        ConnInstance connInstance = connInstanceDAO.authFind(connInstanceTO.getKey());
        if (connInstance == null) {
            throw new NotFoundException("Connector '" + connInstanceTO.getKey() + '\'');
        }

        connInstance.getCapabilities().clear();
        connInstance.getCapabilities().addAll(connInstanceTO.getCapabilities());

        if (connInstanceTO.getAdminRealm() != null) {
            Realm realm = realmDAO.findByFullPath(connInstanceTO.getAdminRealm());
            if (realm == null) {
                SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRealm);
                sce.getElements().add("Invalid or null realm specified: " + connInstanceTO.getAdminRealm());
                throw sce;
            }
            connInstance.setAdminRealm(realm);
        }

        if (connInstanceTO.getLocation() != null) {
            connInstance.setLocation(connInstanceTO.getLocation());
        }

        if (connInstanceTO.getBundleName() != null) {
            connInstance.setBundleName(connInstanceTO.getBundleName());
        }

        if (connInstanceTO.getVersion() != null) {
            connInstance.setVersion(connInstanceTO.getVersion());
        }

        if (connInstanceTO.getConnectorName() != null) {
            connInstance.setConnectorName(connInstanceTO.getConnectorName());
        }

        if (connInstanceTO.getConf() != null && !connInstanceTO.getConf().isEmpty()) {
            connInstance.setConf(connInstanceTO.getConf());
        }

        if (connInstanceTO.getDisplayName() != null) {
            connInstance.setDisplayName(connInstanceTO.getDisplayName());
        }

        if (connInstanceTO.getConnRequestTimeout() != null) {
            connInstance.setConnRequestTimeout(connInstanceTO.getConnRequestTimeout());
        }

        if (connInstanceTO.getPoolConf() == null) {
            connInstance.setPoolConf(null);
        } else {
            connInstance.setPoolConf(
                    ConnPoolConfUtils.getConnPoolConf(connInstanceTO.getPoolConf(), entityFactory.newConnPoolConf()));
        }

        return connInstance;
    }

    @Override
    public ConnConfPropSchema build(final ConfigurationProperty property) {
        ConnConfPropSchema connConfPropSchema = new ConnConfPropSchema();

        connConfPropSchema.setName(property.getName());
        connConfPropSchema.setDisplayName(property.getDisplayName(property.getName()));
        connConfPropSchema.setHelpMessage(property.getHelpMessage(property.getName()));
        connConfPropSchema.setRequired(property.isRequired());
        connConfPropSchema.setType(property.getType().getName());
        connConfPropSchema.setOrder(((ConfigurationPropertyImpl) property).getOrder());
        connConfPropSchema.setConfidential(property.isConfidential());

        if (property.getValue() != null) {
            if (property.getValue().getClass().isArray()) {
                connConfPropSchema.getDefaultValues().addAll(List.of((Object[]) property.getValue()));
            } else if (property.getValue() instanceof Collection<?>) {
                connConfPropSchema.getDefaultValues().addAll((Collection<?>) property.getValue());
            } else {
                connConfPropSchema.getDefaultValues().add(property.getValue());
            }
        }

        return connConfPropSchema;
    }

    @Override
    public ConnInstanceTO getConnInstanceTO(final ConnInstance connInstance) {
        ConnInstanceTO connInstanceTO = new ConnInstanceTO();
        connInstanceTO.setKey(connInstance.getKey());
        connInstanceTO.setBundleName(connInstance.getBundleName());
        connInstanceTO.setConnectorName(connInstance.getConnectorName());
        connInstanceTO.setVersion(connInstance.getVersion());
        connInstanceTO.setDisplayName(connInstance.getDisplayName());
        connInstanceTO.setConnRequestTimeout(connInstance.getConnRequestTimeout());
        connInstanceTO.setAdminRealm(connInstance.getAdminRealm().getFullPath());
        connInstanceTO.getCapabilities().addAll(connInstance.getCapabilities());
        connInstanceTO.getConf().addAll(connInstance.getConf());

        try {
            Pair<URI, ConnectorInfo> info = connIdBundleManager.getConnectorInfo(connInstance);

            connInstanceTO.setLocation(info.getLeft().toASCIIString());

            // refresh stored properties in the given connInstance with direct information from underlying connector
            ConfigurationProperties properties = connIdBundleManager.getConfigurationProperties(info.getRight());
            properties.getPropertyNames().forEach(propName -> {
                ConnConfPropSchema schema = build(properties.getProperty(propName));

                Optional<ConnConfProperty> property = connInstanceTO.getConf(propName);
                if (property.isEmpty()) {
                    property = Optional.of(new ConnConfProperty());
                    connInstanceTO.getConf().add(property.get());
                }
                property.get().setSchema(schema);
            });
        } catch (Exception e) {
            LOG.error("Could not get ConnId information for {} / {}#{}#{}",
                    connInstance.getLocation(), connInstance.getBundleName(), connInstance.getConnectorName(),
                    connInstance.getVersion(), e);

            connInstanceTO.setErrored(true);
            connInstanceTO.setLocation(connInstance.getLocation());
        }

        Collections.sort(connInstanceTO.getConf());

        // pool configuration
        if (connInstance.getPoolConf() != null
                && (connInstance.getPoolConf().getMaxIdle() != null
                || connInstance.getPoolConf().getMaxObjects() != null
                || connInstance.getPoolConf().getMaxWait() != null
                || connInstance.getPoolConf().getMinEvictableIdleTimeMillis() != null
                || connInstance.getPoolConf().getMinIdle() != null)) {

            ConnPoolConfTO poolConf = new ConnPoolConfTO();
            poolConf.setMaxIdle(connInstance.getPoolConf().getMaxIdle());
            poolConf.setMaxObjects(connInstance.getPoolConf().getMaxObjects());
            poolConf.setMaxWait(connInstance.getPoolConf().getMaxWait());
            poolConf.setMinEvictableIdleTimeMillis(connInstance.getPoolConf().getMinEvictableIdleTimeMillis());
            poolConf.setMinIdle(connInstance.getPoolConf().getMinIdle());
            connInstanceTO.setPoolConf(poolConf);
        }

        return connInstanceTO;
    }
}
