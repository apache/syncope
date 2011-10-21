/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.rest.data;

import javassist.NotFoundException;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConfigurationProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.syncope.client.to.ConnInstanceTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.ConnInstance;
import org.syncope.core.persistence.dao.ConnInstanceDAO;
import org.syncope.core.util.ConnBundleManager;
import org.syncope.types.ConnConfPropSchema;
import org.syncope.types.ConnConfProperty;
import org.syncope.types.SyncopeClientExceptionType;

@Component
public class ConnInstanceDataBinder {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            ConnInstanceDataBinder.class);

    private static final String[] ignoreProperties = {
        "id", "resources", "syncToken"};

    @Autowired
    private ConnInstanceDAO connectorInstanceDAO;

    @Autowired
    private ConnBundleManager connBundleManager;

    public ConnInstance getConnInstance(
            final ConnInstanceTO connectorInstanceTO)
            throws SyncopeClientCompositeErrorException {

        SyncopeClientCompositeErrorException compositeErrorException =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        SyncopeClientException requiredValuesMissing =
                new SyncopeClientException(
                SyncopeClientExceptionType.RequiredValuesMissing);

        if (connectorInstanceTO.getBundleName() == null) {
            requiredValuesMissing.addElement("bundlename");
        }

        if (connectorInstanceTO.getVersion() == null) {
            requiredValuesMissing.addElement("bundleversion");
        }

        if (connectorInstanceTO.getConnectorName() == null) {
            requiredValuesMissing.addElement("connectorname");
        }

        if (connectorInstanceTO.getConfiguration() == null
                || connectorInstanceTO.getConfiguration().isEmpty()) {
            requiredValuesMissing.addElement("configuration");
        }

        ConnInstance connectorInstance = new ConnInstance();

        BeanUtils.copyProperties(
                connectorInstanceTO, connectorInstance, ignoreProperties);

        // Throw composite exception if there is at least one element set
        // in the composing exceptions

        if (!requiredValuesMissing.getElements().isEmpty()) {
            compositeErrorException.addException(requiredValuesMissing);
        }

        if (compositeErrorException.hasExceptions()) {
            throw compositeErrorException;
        }

        return connectorInstance;
    }

    public ConnInstance updateConnInstance(
            Long connectorInstanceId,
            ConnInstanceTO connInstanceTO)
            throws SyncopeClientCompositeErrorException {

        SyncopeClientCompositeErrorException compositeErrorException =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        SyncopeClientException requiredValuesMissing =
                new SyncopeClientException(
                SyncopeClientExceptionType.RequiredValuesMissing);

        if (connectorInstanceId == null) {
            requiredValuesMissing.addElement("connector id");
        }

        ConnInstance connInstance =
                connectorInstanceDAO.find(connectorInstanceId);

        if (connInstanceTO.getBundleName() != null) {
            connInstance.setBundleName(
                    connInstanceTO.getBundleName());
        }

        if (connInstanceTO.getVersion() != null) {
            connInstance.setVersion(connInstanceTO.getVersion());
        }

        if (connInstanceTO.getConnectorName() != null) {
            connInstance.setConnectorName(
                    connInstanceTO.getConnectorName());
        }

        if (connInstanceTO.getConfiguration() != null
                && !connInstanceTO.getConfiguration().isEmpty()) {

            connInstance.setConfiguration(
                    connInstanceTO.getConfiguration());
        }

        if (connInstanceTO.getDisplayName() != null) {
            connInstance.setDisplayName(
                    connInstanceTO.getDisplayName());
        }

        connInstance.setCapabilities(
                connInstanceTO.getCapabilities());

        if (connInstanceTO.getSyncToken() == null) {
            connInstance.setSerializedSyncToken(null);
        }

        if (!requiredValuesMissing.getElements().isEmpty()) {
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
        connInstanceTO.setId(connInstance.getId());

        // retrieve the ConfigurationProperties.
        ConfigurationProperties properties =
                connBundleManager.getConfigurationProperties(
                connInstance.getBundleName(),
                connInstance.getVersion(),
                connInstance.getConnectorName());

        BeanUtils.copyProperties(
                connInstance, connInstanceTO, ignoreProperties);

        connInstanceTO.setSyncToken(
                connInstance.getSerializedSyncToken());

        ConnConfPropSchema connConfPropSchema;
        ConfigurationProperty configurationProperty;

        for (String propName : properties.getPropertyNames()) {

            if (!connInstanceTO.isPropertyPresent(propName)) {

                connConfPropSchema = new ConnConfPropSchema();
                configurationProperty = properties.getProperty(propName);
                connConfPropSchema.setName(
                        configurationProperty.getName());
                connConfPropSchema.setDisplayName(
                        configurationProperty.getDisplayName(propName));
                connConfPropSchema.setHelpMessage(
                        configurationProperty.getHelpMessage(propName));
                connConfPropSchema.setRequired(
                        configurationProperty.isRequired());
                connConfPropSchema.setType(
                        configurationProperty.getType().getName());

                ConnConfProperty property = new ConnConfProperty();
                property.setSchema(connConfPropSchema);
                connInstanceTO.addConfiguration(property);
            }
        }
        return connInstanceTO;
    }
}
