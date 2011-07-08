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

import java.io.File;
import java.net.URI;
import java.util.Collection;
import java.util.HashSet;
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
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;
import org.syncope.core.init.ConnInstanceLoader;
import org.syncope.core.persistence.dao.MissingConfKeyException;
import org.syncope.types.ConnConfProperty;
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
    public ConnectorFacadeProxy(final ConnInstance connInstance,
            final ConnInstanceLoader connInstanceLoader)
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
        ConnectorInfo info;
        try {
            info = connInstanceLoader.getConnectorManager().
                    findConnectorInfo(key);
            if (info == null) {
                throw new NotFoundException("Connector Info for key " + key);
            }
        } catch (MissingConfKeyException e) {
            throw new NotFoundException("Connector Info for key " + key, e);
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
                    } else if (Character.class.equals(propertySchemaClass)
                            || char.class.equals(propertySchemaClass)) {

                        propertyValue = property.getValue().toCharArray()[0];
                    } else if (Integer.class.equals(propertySchemaClass)
                            || int.class.equals(propertySchemaClass)) {

                        propertyValue = Integer.valueOf(property.getValue());
                    } else if (Long.class.equals(propertySchemaClass)
                            || long.class.equals(propertySchemaClass)) {

                        propertyValue = Long.valueOf(property.getValue());
                    } else if (Float.class.equals(propertySchemaClass)
                            || float.class.equals(propertySchemaClass)) {

                        propertyValue = Float.valueOf(property.getValue());
                    } else if (Double.class.equals(propertySchemaClass)
                            || double.class.equals(propertySchemaClass)) {

                        propertyValue = Double.valueOf(property.getValue());
                    } else if (Boolean.class.equals(propertySchemaClass)
                            || boolean.class.equals(propertySchemaClass)) {

                        propertyValue = Boolean.valueOf(property.getValue());
                    } else if (URI.class.equals(propertySchemaClass)) {
                        propertyValue = URI.create(property.getValue());
                    } else if (File.class.equals(propertySchemaClass)) {
                        propertyValue = new File(property.getValue());
                    } else if (String[].class.equals(propertySchemaClass)) {
                        propertyValue = property.getValue().split(" ");
                    } else {
                        propertyValue = property.getValue();
                    }

                    properties.setPropertyValue(
                            property.getSchema().getName(), propertyValue);
                } catch (Throwable t) {
                    LOG.error("Invalid ConnConfProperty specified: {}",
                            property, t);
                }
            }
        }

        // Use the ConnectorFacadeFactory's newInstance() method to get
        // a new connector.
        connector = ConnectorFacadeFactory.getInstance().newInstance(apiConfig);
        if (connector == null) {
            throw new NotFoundException("Connector");
        }

        // Make sure we have set up the Configuration properly
        connector.validate();

        this.capabitilies = connInstance.getCapabilities();
    }

    public Uid resolveUsername(
            final ObjectClass objectClass,
            final String username,
            final OperationOptions options) {

        Uid result = null;

        if (capabitilies.contains(ConnectorCapability.RESOLVE)) {

            result = connector.resolveUsername(
                    objectClass, username, options);
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

    /**
     * Get remote object.
     */
    public ConnectorObject getObject(
            final ObjectClass objectClass,
            final Uid uid,
            final OperationOptions options) {

        return getObject(null, null, objectClass, uid, options);
    }

    /**
     * Get remote object used by the propagation manager in order to choose
     * for a create (object doesn't exist) or an update (object exists)
     */
    public ConnectorObject getObject(
            final PropagationMode propagationMode,
            final ResourceOperationType operationType,
            final ObjectClass objectClass,
            final Uid uid,
            final OperationOptions options) {

        ConnectorObject result = null;

        if (capabitilies.contains(ConnectorCapability.SEARCH)) {
            if (operationType == null) {
                result = connector.getObject(objectClass, uid, options);
            } else {
                switch (operationType) {
                    case CREATE:
                        if (propagationMode == null
                                || (propagationMode == PropagationMode.SYNC
                                ? capabitilies.contains(
                                ConnectorCapability.SYNC_CREATE)
                                : capabitilies.contains(
                                ConnectorCapability.ASYNC_CREATE))) {

                            result = connector.getObject(
                                    objectClass, uid, options);
                        }
                        break;
                    case UPDATE:
                        if (propagationMode == null
                                || (propagationMode == PropagationMode.SYNC
                                ? capabitilies.contains(
                                ConnectorCapability.SYNC_UPDATE)
                                : capabitilies.contains(
                                ConnectorCapability.ASYNC_UPDATE))) {

                            result = connector.getObject(
                                    objectClass, uid, options);
                        }
                        break;
                    default:
                        result = connector.getObject(objectClass, uid, options);
                }
            }
        }

        return result;
    }

    public void validate() {
        connector.validate();
    }

    public Attribute getObjectAttribute(
            final ObjectClass objClass,
            final Uid uid,
            final OperationOptions options,
            final String attributeName) {

        Attribute attribute = null;

        try {
            final ConnectorObject object =
                    connector.getObject(objClass, uid, options);

            attribute = object.getAttributeByName(attributeName);

        } catch (NullPointerException e) {
            // ignore exception
            LOG.debug("Object for '{}' not found", uid.getUidValue());
        }

        return attribute;
    }

    public Set<Attribute> getObjectAttributes(
            final ObjectClass objClass,
            final Uid uid,
            final OperationOptions options,
            final Collection<String> attributeNames) {

        final Set<Attribute> attributes = new HashSet<Attribute>();

        try {

            final ConnectorObject object = connector.getObject(objClass, uid,
                    options);

            for (String attribute : attributeNames) {
                attributes.add(object.getAttributeByName(attribute));
            }

        } catch (NullPointerException e) {
            // ignore exception
            LOG.debug("Object for '{}' not found", uid.getUidValue());
        }

        return attributes;
    }

    @Override
    public String toString() {
        return "ConnectorFacadeProxy{"
                + "connector=" + connector
                + "capabitilies=" + capabitilies + '}';
    }
}
