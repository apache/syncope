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
package org.apache.syncope.core.rest.data;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.apache.syncope.common.to.ConnInstanceTO;
import org.apache.syncope.common.types.ConnConfPropSchema;
import org.apache.syncope.common.types.ConnConfProperty;
import org.apache.syncope.common.types.SyncopeClientExceptionType;
import org.apache.syncope.common.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.apache.syncope.core.persistence.beans.ConnInstance;
import org.apache.syncope.core.persistence.dao.ConnInstanceDAO;
import org.apache.syncope.core.util.ConnIdBundleManager;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.impl.api.ConfigurationPropertyImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ConnInstanceDataBinder {

    private static final String[] IGNORE_PROPERTIES = {"id"};

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    /**
     * Merge connector configuration properties avoiding repetition but giving priority to primary set.
     *
     * @param primary primary set.
     * @param secondary secondary set.
     * @return merged set.
     */
    public Set<ConnConfProperty> mergeConnConfProperties(final Set<ConnConfProperty> primary,
            final Set<ConnConfProperty> secondary) {

        final Set<ConnConfProperty> conf = new HashSet<ConnConfProperty>();

        // to be used to control managed prop (needed by overridden mechanism)
        final Set<String> propertyNames = new HashSet<String>();

        // get overridden connector configuration properties
        for (ConnConfProperty prop : primary) {
            if (!propertyNames.contains(prop.getSchema().getName())) {
                conf.add(prop);
                propertyNames.add(prop.getSchema().getName());
            }
        }

        // get connector configuration properties
        for (ConnConfProperty prop : secondary) {
            if (!propertyNames.contains(prop.getSchema().getName())) {
                conf.add(prop);
                propertyNames.add(prop.getSchema().getName());
            }
        }

        return conf;
    }

    public ConnInstance getConnInstance(final ConnInstanceTO connInstanceTO) {
        SyncopeClientCompositeErrorException scee = new SyncopeClientCompositeErrorException(HttpStatus.BAD_REQUEST);

        SyncopeClientException requiredValuesMissing = new SyncopeClientException(
                SyncopeClientExceptionType.RequiredValuesMissing);

        if (connInstanceTO.getLocation() == null) {
            requiredValuesMissing.addElement("location");
        }

        if (connInstanceTO.getBundleName() == null) {
            requiredValuesMissing.addElement("bundlename");
        }

        if (connInstanceTO.getVersion() == null) {
            requiredValuesMissing.addElement("bundleversion");
        }

        if (connInstanceTO.getConnectorName() == null) {
            requiredValuesMissing.addElement("connectorname");
        }

        if (connInstanceTO.getConfiguration() == null || connInstanceTO.getConfiguration().isEmpty()) {
            requiredValuesMissing.addElement("configuration");
        }

        ConnInstance connInstance = new ConnInstance();

        BeanUtils.copyProperties(connInstanceTO, connInstance, IGNORE_PROPERTIES);
        if (connInstanceTO.getLocation() != null) {
            connInstance.setLocation(connInstanceTO.getLocation().toString());
        }

        // Throw composite exception if there is at least one element set
        // in the composing exceptions

        if (!requiredValuesMissing.isEmpty()) {
            scee.addException(requiredValuesMissing);
        }

        if (scee.hasExceptions()) {
            throw scee;
        }

        return connInstance;
    }

    public ConnInstance updateConnInstance(final long connInstanceId, final ConnInstanceTO connInstanceTO) {
        SyncopeClientCompositeErrorException scce = new SyncopeClientCompositeErrorException(HttpStatus.BAD_REQUEST);

        SyncopeClientException requiredValuesMissing = new SyncopeClientException(
                SyncopeClientExceptionType.RequiredValuesMissing);

        if (connInstanceId == 0) {
            requiredValuesMissing.addElement("connector id");
        }

        ConnInstance connInstance = connInstanceDAO.find(connInstanceId);

        if (connInstanceTO.getLocation() != null) {
            connInstance.setLocation(connInstanceTO.getLocation().toString());
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

        if (connInstanceTO.getConfiguration() != null && !connInstanceTO.getConfiguration().isEmpty()) {
            connInstance.setConfiguration(connInstanceTO.getConfiguration());
        }

        if (connInstanceTO.getDisplayName() != null) {
            connInstance.setDisplayName(connInstanceTO.getDisplayName());
        }

        if (connInstanceTO.getConnRequestTimeout() != null) {
            connInstance.setConnRequestTimeout(connInstanceTO.getConnRequestTimeout());
        }

        connInstance.setCapabilities(connInstanceTO.getCapabilities());

        if (!requiredValuesMissing.isEmpty()) {
            scce.addException(requiredValuesMissing);
        }

        // Throw composite exception if there is at least one element set
        // in the composing exceptions
        if (scce.hasExceptions()) {
            throw scce;
        }

        return connInstance;
    }

    public ConnInstanceTO getConnInstanceTO(final ConnInstance connInstance) {
        ConnInstanceTO connInstanceTO = new ConnInstanceTO();
        connInstanceTO.setId(connInstance.getId() == null ? 0L : connInstance.getId().longValue());

        // retrieve the ConfigurationProperties
        ConfigurationProperties properties = ConnIdBundleManager.getConfigurationProperties(
                ConnIdBundleManager.getConnectorInfo(connInstance.getLocation(),
                connInstance.getBundleName(), connInstance.getVersion(), connInstance.getConnectorName()));

        BeanUtils.copyProperties(connInstance, connInstanceTO, IGNORE_PROPERTIES);

        final Map<String, ConnConfProperty> connInstanceToConfMap = connInstanceTO.getConfigurationMap();

        for (String propName : properties.getPropertyNames()) {
            ConfigurationPropertyImpl configurationProperty =
                    (ConfigurationPropertyImpl) properties.getProperty(propName);

            if (connInstanceToConfMap.containsKey(propName)) {
                connInstanceToConfMap.get(propName).getSchema().setDisplayName(
                        configurationProperty.getDisplayName(propName));
            } else {
                ConnConfPropSchema connConfPropSchema = new ConnConfPropSchema();
                connConfPropSchema.setName(configurationProperty.getName());
                connConfPropSchema.setDisplayName(configurationProperty.getDisplayName(propName));
                connConfPropSchema.setHelpMessage(configurationProperty.getHelpMessage(propName));
                connConfPropSchema.setRequired(configurationProperty.isRequired());
                connConfPropSchema.setType(configurationProperty.getType().getName());
                connConfPropSchema.setConfidential(configurationProperty.isConfidential());
                connConfPropSchema.setOrder(configurationProperty.getOrder());

                ConnConfProperty property = new ConnConfProperty();
                property.setSchema(connConfPropSchema);
                connInstanceTO.addConfiguration(property);
            }
        }
        return connInstanceTO;
    }
}
