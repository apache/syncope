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
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ConnConfPropSchema;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.core.persistence.api.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.utils.ConnPoolConfUtils;
import org.apache.syncope.core.provisioning.api.ConnIdBundleManager;
import org.apache.syncope.core.provisioning.api.data.ConnInstanceDataBinder;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConfigurationProperty;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.impl.api.ConfigurationPropertyImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConnInstanceDataBinderImpl implements ConnInstanceDataBinder {

    protected static final Logger LOG = LoggerFactory.getLogger(ConnInstanceDataBinder.class);

    protected final ConnIdBundleManager connIdBundleManager;

    protected final ConnInstanceDAO connInstanceDAO;

    protected final RealmSearchDAO realmSearchDAO;

    protected final EntityFactory entityFactory;

    public ConnInstanceDataBinderImpl(
            final ConnIdBundleManager connIdBundleManager,
            final ConnInstanceDAO connInstanceDAO,
            final RealmSearchDAO realmSearchDAO,
            final EntityFactory entityFactory) {

        this.connIdBundleManager = connIdBundleManager;
        this.connInstanceDAO = connInstanceDAO;
        this.realmSearchDAO = realmSearchDAO;
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

        Optional.ofNullable(connInstanceTO.getAdminRealm()).
                ifPresent(r -> connInstance.setAdminRealm(realmSearchDAO.findByFullPath(r).orElse(null)));
        if (connInstance.getAdminRealm() == null) {
            sce.getElements().add("Invalid or null realm specified: " + connInstanceTO.getAdminRealm());
        }

        Optional.ofNullable(connInstanceTO.getLocation()).ifPresent(connInstance::setLocation);
        connInstance.setConf(connInstanceTO.getConf());
        Optional.ofNullable(connInstanceTO.getPoolConf()).
                ifPresent(conf -> connInstance.setPoolConf(ConnPoolConfUtils.getConnPoolConf(conf)));

        // Throw exception if there is at least one element set
        if (!sce.isEmpty()) {
            throw sce;
        }

        return connInstance;
    }

    @Override
    public ConnInstance update(final ConnInstanceTO connInstanceTO) {
        ConnInstance connInstance = Optional.ofNullable(connInstanceDAO.authFind(connInstanceTO.getKey())).
                orElseThrow(() -> new NotFoundException("Connector '" + connInstanceTO.getKey() + '\''));

        connInstance.getCapabilities().clear();
        connInstance.getCapabilities().addAll(connInstanceTO.getCapabilities());

        Optional.ofNullable(connInstanceTO.getAdminRealm()).
                ifPresent(r -> connInstance.setAdminRealm(realmSearchDAO.findByFullPath(r).
                orElseThrow(() -> {
                    SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRealm);
                    sce.getElements().add("Invalid or null realm specified: " + connInstanceTO.getAdminRealm());
                    return sce;
                })));

        Optional.ofNullable(connInstanceTO.getLocation()).ifPresent(connInstance::setLocation);
        Optional.ofNullable(connInstanceTO.getBundleName()).ifPresent(connInstance::setBundleName);
        Optional.ofNullable(connInstanceTO.getVersion()).ifPresent(connInstance::setVersion);
        Optional.ofNullable(connInstanceTO.getConnectorName()).ifPresent(connInstance::setConnectorName);
        Optional.ofNullable(connInstanceTO.getDisplayName()).ifPresent(connInstance::setDisplayName);
        Optional.ofNullable(connInstanceTO.getConf()).
                filter(Predicate.not(Collection::isEmpty)).
                ifPresent(connInstance::setConf);
        Optional.ofNullable(connInstanceTO.getConnRequestTimeout()).ifPresent(connInstance::setConnRequestTimeout);
        Optional.ofNullable(connInstanceTO.getPoolConf()).ifPresentOrElse(
                conf -> connInstance.setPoolConf(ConnPoolConfUtils.getConnPoolConf(conf)),
                () -> connInstance.setPoolConf(null));

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
            } else if (property.getValue() instanceof Collection<?> collection) {
                connConfPropSchema.getDefaultValues().addAll(collection);
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

                ConnConfProperty property = connInstanceTO.getConf(propName).
                        orElseGet(() -> {
                            ConnConfProperty p = new ConnConfProperty();
                            connInstanceTO.getConf().add(p);
                            return p;
                        });
                property.setSchema(schema);
            });
        } catch (Exception e) {
            LOG.error("Could not get ConnId information for {} / {}#{}#{}",
                    connInstance.getLocation(), connInstance.getBundleName(), connInstance.getConnectorName(),
                    connInstance.getVersion(), e);

            connInstanceTO.setErrored(true);
            connInstanceTO.setLocation(connInstance.getLocation());
        }

        connInstanceTO.setPoolConf(connInstance.getPoolConf());

        return connInstanceTO;
    }
}
