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

import java.util.Map;
import javassist.NotFoundException;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.apache.syncope.client.to.ConnInstanceTO;
import org.apache.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.apache.syncope.client.validation.SyncopeClientException;
import org.apache.syncope.core.persistence.beans.ConnInstance;
import org.apache.syncope.core.persistence.dao.ConnInstanceDAO;
import org.apache.syncope.core.util.ConnBundleManager;
import org.apache.syncope.types.ConnConfPropSchema;
import org.apache.syncope.types.ConnConfProperty;
import org.apache.syncope.types.SyncopeClientExceptionType;
import org.identityconnectors.framework.impl.api.ConfigurationPropertyImpl;

@Component
public class ConnInstanceDataBinder {

    private static final String[] IGNORE_PROPERTIES = {"id", "resources"};

    @Autowired
    private ConnInstanceDAO connectorInstanceDAO;

    @Autowired
    private ConnBundleManager connBundleManager;

    public ConnInstance getConnInstance(final ConnInstanceTO connInstanceTO)
            throws SyncopeClientCompositeErrorException {

        SyncopeClientCompositeErrorException compositeErrorException = new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        SyncopeClientException requiredValuesMissing = new SyncopeClientException(
                SyncopeClientExceptionType.RequiredValuesMissing);

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

        ConnInstance connectorInstance = new ConnInstance();

        BeanUtils.copyProperties(connInstanceTO, connectorInstance, IGNORE_PROPERTIES);

        // Throw composite exception if there is at least one element set
        // in the composing exceptions

        if (!requiredValuesMissing.isEmpty()) {
            compositeErrorException.addException(requiredValuesMissing);
        }

        if (compositeErrorException.hasExceptions()) {
            throw compositeErrorException;
        }

        return connectorInstance;
    }

    public ConnInstance updateConnInstance(final long connInstanceId, final ConnInstanceTO connInstanceTO)
            throws SyncopeClientCompositeErrorException {

        SyncopeClientCompositeErrorException compositeErrorException = new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        SyncopeClientException requiredValuesMissing = new SyncopeClientException(
                SyncopeClientExceptionType.RequiredValuesMissing);

        if (connInstanceId == 0) {
            requiredValuesMissing.addElement("connector id");
        }

        ConnInstance connInstance = connectorInstanceDAO.find(connInstanceId);

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

        connInstance.setCapabilities(connInstanceTO.getCapabilities());

        if (!requiredValuesMissing.isEmpty()) {
            compositeErrorException.addException(requiredValuesMissing);
        }

        // Throw composite exception if there is at least one element set
        // in the composing exceptions
        if (compositeErrorException.hasExceptions()) {
            throw compositeErrorException;
        }

        return connInstance;
    }

    public ConnInstanceTO getConnInstanceTO(final ConnInstance connInstance)
            throws NotFoundException {

        ConnInstanceTO connInstanceTO = new ConnInstanceTO();
        connInstanceTO.setId(connInstance.getId() != null ? connInstance.getId().longValue() : 0L);

        // retrieve the ConfigurationProperties.
        ConfigurationProperties properties = connBundleManager.getConfigurationProperties(
                connInstance.getBundleName(), connInstance.getVersion(), connInstance.getConnectorName());

        BeanUtils.copyProperties(connInstance, connInstanceTO, IGNORE_PROPERTIES);

        final Map<String, ConnConfProperty> connInstanceToConfMap = connInstanceTO.getConfigurationMap();

        for (String propName : properties.getPropertyNames()) {
            ConfigurationPropertyImpl configurationProperty =
                    (ConfigurationPropertyImpl) properties.getProperty(propName);

            if (!connInstanceToConfMap.containsKey(propName)) {
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
            } else {
                connInstanceToConfMap.get(propName).getSchema().setDisplayName(
                        configurationProperty.getDisplayName(propName));
            }
        }
        return connInstanceTO;
    }
}
