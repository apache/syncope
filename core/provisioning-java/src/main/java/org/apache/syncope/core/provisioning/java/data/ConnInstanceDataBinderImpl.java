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

import org.apache.syncope.core.provisioning.api.data.ConnInstanceDataBinder;
import java.util.Arrays;
import java.util.Collection;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ConnPoolConfTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ConnConfPropSchema;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.core.persistence.api.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.provisioning.api.ConnIdBundleManager;
import org.apache.syncope.core.provisioning.api.utils.ConnPoolConfUtils;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConfigurationProperty;
import org.identityconnectors.framework.impl.api.ConfigurationPropertyImpl;
import org.apache.syncope.core.spring.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ConnInstanceDataBinderImpl implements ConnInstanceDataBinder {

    private static final String[] IGNORE_PROPERTIES = { "poolConf" };

    @Autowired
    private ConnIdBundleManager connIdBundleManager;

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Autowired
    private EntityFactory entityFactory;

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

        BeanUtils.copyProperties(connInstanceTO, connInstance, IGNORE_PROPERTIES);
        if (connInstanceTO.getLocation() != null) {
            connInstance.setLocation(connInstanceTO.getLocation());
        }
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
    public ConnInstance update(final String key, final ConnInstanceTO connInstanceTO) {
        SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);

        if (key == null) {
            sce.getElements().add("connector key");
        }

        ConnInstance connInstance = connInstanceDAO.find(key);
        connInstance.getCapabilities().clear();
        connInstance.getCapabilities().addAll(connInstanceTO.getCapabilities());

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

        if (!sce.isEmpty()) {
            throw sce;
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
                connConfPropSchema.getDefaultValues().addAll(Arrays.asList((Object[]) property.getValue()));
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

        BeanUtils.copyProperties(connInstance, connInstanceTO, IGNORE_PROPERTIES);

        // refresh stored properties in the given connInstance with direct information from underlying connector
        ConfigurationProperties properties =
                connIdBundleManager.getConfigurationProperties(connIdBundleManager.getConnectorInfo(connInstance));
        for (final String propName : properties.getPropertyNames()) {
            ConnConfPropSchema schema = build(properties.getProperty(propName));

            ConnConfProperty property = IterableUtils.find(connInstanceTO.getConf(),
                    new Predicate<ConnConfProperty>() {

                @Override
                public boolean evaluate(final ConnConfProperty candidate) {
                    return propName.equals(candidate.getSchema().getName());
                }
            });
            if (property == null) {
                property = new ConnConfProperty();
                connInstanceTO.getConf().add(property);
            }

            property.setSchema(schema);
        }

        // pool configuration
        if (connInstance.getPoolConf() != null) {
            ConnPoolConfTO poolConf = new ConnPoolConfTO();
            BeanUtils.copyProperties(connInstance.getPoolConf(), poolConf);
            connInstanceTO.setPoolConf(poolConf);
        }

        return connInstanceTO;
    }
}
