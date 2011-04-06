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
package org.syncope.core.persistence.propagation;

import java.util.Set;
import javassist.NotFoundException;
import org.identityconnectors.common.security.GuardedByteArray;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorKey;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;
import org.syncope.types.ConnConfProperty;
import org.syncope.core.persistence.ConnInstanceLoader;
import org.syncope.core.persistence.beans.ConnInstance;
import org.syncope.types.ConnectorCapability;
import org.syncope.types.PropagationMode;
import org.syncope.types.ResourceOperationType;

/**
 * Intercept calls to ConnectorFacade's methods and check if the correspondant
 * connector instance has been configured to allow every single operation:
 * if not, simply do nothig.
 */
public class ConnectorFacadeProxy {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(
            ConnectorFacadeProxy.class);

    /**
     * Connector facade wrapped instance.
     */
    private final ConnectorFacade connector;

    /**
     * Set of configure connecto instance capabilities.
     * @see org.syncope.core.persistence.beans.ConnInstance
     */
    private final Set<ConnectorCapability> capabitilies;

    /**
     * Use the passed connector instance to build a ConnectorFacade that will
     * be used to make all wrapped calls.
     *
     * @param connInstance the connector instance configuration
     * @throws NotFoundException when not able to fetch all the required data
     * @see ConnectorKey
     * @see ConnectorInfo
     * @see APIConfiguration
     * @see ConfigurationProperties
     * @see ConnectorFacade
     */
    public ConnectorFacadeProxy(final ConnInstance connInstance)
            throws NotFoundException {

        // specify a connector.
        ConnectorKey key = new ConnectorKey(
                connInstance.getBundleName(),
                connInstance.getVersion(),
                connInstance.getConnectorName());

        if (key == null) {
            throw new NotFoundException("Connector Key");
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug("\nBundle name: " + key.getBundleName()
                    + "\nBundle version: " + key.getBundleVersion()
                    + "\nBundle class: " + key.getConnectorName());
        }

        // get the specified connector.
        ConnectorInfo info = ConnInstanceLoader.getConnectorManager().
                findConnectorInfo(key);
        if (info == null) {
            throw new NotFoundException("Connector Info");
        }

        // create default configuration
        APIConfiguration apiConfig = info.createDefaultAPIConfiguration();

        if (apiConfig == null) {
            throw new NotFoundException("Default API configuration");
        }

        // retrieve the ConfigurationProperties.
        ConfigurationProperties properties =
                apiConfig.getConfigurationProperties();

        if (properties == null) {
            throw new NotFoundException("Configuration properties");
        }

        // Print out what the properties are (not necessary)
        if (LOG.isDebugEnabled()) {
            for (String propName : properties.getPropertyNames()) {
                LOG.debug("\nProperty Name: "
                        + properties.getProperty(propName).getName()
                        + "\nProperty Type: "
                        + properties.getProperty(propName).getType());
            }
        }

        // Set all of the ConfigurationProperties needed by the connector.
        Class propertySchemaClass;
        Object propertyValue;
        for (ConnConfProperty property : connInstance.getConfiguration()) {
            if (property.getValue() != null) {
                try {
                    propertySchemaClass = ClassUtils.forName(
                            property.getSchema().getType(),
                            ClassUtils.getDefaultClassLoader());

                    if (GuardedString.class.equals(propertySchemaClass)) {
                        propertyValue = new GuardedString(
                                property.getValue().toCharArray());
                    } else if (GuardedByteArray.class.equals(
                            propertySchemaClass)) {
                        propertyValue = new GuardedByteArray(
                                property.getValue().getBytes());
                    } else {
                        propertyValue = property.getValue();
                    }

                    properties.setPropertyValue(
                            property.getSchema().getName(), propertyValue);
                } catch (ClassNotFoundException e) {
                    LOG.error("Invalid configType specified for "
                            + property.getSchema(), e);
                }
            }
        }

        // Use the ConnectorFacadeFactory's newInstance() method to get
        // a new connector.
        ConnectorFacade connector =
                ConnectorFacadeFactory.getInstance().newInstance(apiConfig);

        if (connector == null) {
            throw new NotFoundException("Connector");
        }

        // Make sure we have set up the Configuration properly
        try {
            connector.validate();
        } catch (RuntimeException e) {
            LOG.error("Connector configuration not valid: " + apiConfig, e);
        }

        this.connector = connector;
        this.capabitilies = connInstance.getCapabilities();
    }

    public Uid resolveUsername(
            final PropagationMode propagationMode,
            final ResourceOperationType operationType,
            final ObjectClass objectClass,
            final String username,
            final OperationOptions options) {

        Uid result = null;

        if (capabitilies.contains(ConnectorCapability.RESOLVE)) {
            switch (operationType) {
                case CREATE:
                    if (propagationMode == PropagationMode.SYNC
                            ? capabitilies.contains(
                            ConnectorCapability.SYNC_CREATE)
                            : capabitilies.contains(
                            ConnectorCapability.ASYNC_CREATE)) {

                        result = connector.resolveUsername(
                                objectClass, username, options);
                    }
                    break;
                case UPDATE:
                    if (propagationMode == PropagationMode.SYNC
                            ? capabitilies.contains(
                            ConnectorCapability.SYNC_UPDATE)
                            : capabitilies.contains(
                            ConnectorCapability.ASYNC_UPDATE)) {

                        result = connector.resolveUsername(
                                objectClass, username, options);
                    }
                    break;
                default:
                    result = connector.resolveUsername(
                            objectClass, username, options);
            }
        }

        return result;
    }

    public Uid create(
            final PropagationMode propagationMode,
            final ObjectClass oclass,
            final Set<Attribute> attrs,
            final OperationOptions options,
            final Set<String> triedPropagationRequests) {

        Uid result = null;

        if (propagationMode == PropagationMode.SYNC
                ? capabitilies.contains(
                ConnectorCapability.SYNC_CREATE)
                : capabitilies.contains(
                ConnectorCapability.ASYNC_CREATE)) {

            triedPropagationRequests.add("create");

            result = connector.create(oclass, attrs, options);
        }

        return result;
    }

    public Uid update(final PropagationMode propagationMode,
            final ObjectClass objclass,
            final Uid uid,
            final Set<Attribute> replaceAttributes,
            final OperationOptions options,
            final Set<String> triedPropagationRequests) {

        Uid result = null;

        if (propagationMode == PropagationMode.SYNC
                ? capabitilies.contains(
                ConnectorCapability.SYNC_UPDATE)
                : capabitilies.contains(
                ConnectorCapability.ASYNC_UPDATE)) {

            triedPropagationRequests.add("update");

            result = connector.update(
                    objclass, uid, replaceAttributes, options);
        }

        return result;
    }

    public void delete(final PropagationMode propagationMode,
            final ObjectClass objClass,
            final Uid uid,
            final OperationOptions options,
            final Set<String> triedPropagationRequests) {

        if (propagationMode == PropagationMode.SYNC
                ? capabitilies.contains(
                ConnectorCapability.SYNC_DELETE)
                : capabitilies.contains(
                ConnectorCapability.ASYNC_DELETE)) {

            triedPropagationRequests.add("delete");

            connector.delete(objClass, uid, options);
        }
    }

    public void validate() {
        connector.validate();
    }

    @Override
    public String toString() {
        return "ConnectorFacadeProxy{"
                + "connector=" + connector
                + "capabitilies=" + capabitilies + '}';
    }
}
