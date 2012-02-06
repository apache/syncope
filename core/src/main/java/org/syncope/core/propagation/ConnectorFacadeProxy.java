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
package org.syncope.core.propagation;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
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
import org.identityconnectors.framework.common.objects.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;
import org.syncope.core.util.ConnBundleManager;
import org.syncope.core.persistence.beans.ConnInstance;
import org.syncope.core.persistence.dao.MissingConfKeyException;
import org.syncope.types.ConnConfProperty;
import org.syncope.types.ConnectorCapability;
import org.syncope.types.PropagationMode;
import org.syncope.types.PropagationOperation;

/**
 * Intercept calls to ConnectorFacade's methods and check if the correspondant
 * connector instance has been configured to allow every single operation: if
 * not, simply do nothig.
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
     *
     * @see org.syncope.core.persistence.beans.ConnInstance
     */
    private final Set<ConnectorCapability> capabitilies;

    /**
     * Use the passed connector instance to build a ConnectorFacade that will be
     * used to make all wrapped calls.
     *
     * @param connInstance the connector instance configuration
     * @param connBundleManager connector bundle loader
     * @throws NotFoundException when not able to fetch all the required data
     * @see ConnectorKey
     * @see ConnectorInfo
     * @see APIConfiguration
     * @see ConfigurationProperties
     * @see ConnectorFacade
     */
    public ConnectorFacadeProxy(
            final ConnInstance connInstance,
            final ConnBundleManager connBundleManager)
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
            info = connBundleManager.getConnectorManager().
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
            if (property.getValues() != null && !property.getValues().isEmpty()) {
                try {
                    propertySchemaClass = ClassUtils.forName(
                            property.getSchema().getType(),
                            ClassUtils.getDefaultClassLoader());

                    if (GuardedString.class.equals(propertySchemaClass)) {
                        propertyValue = new GuardedString(
                                ((String) property.getValues().iterator().next()).toCharArray());
                    } else if (GuardedByteArray.class.equals(
                            propertySchemaClass)) {

                        propertyValue = new GuardedByteArray(
                                (byte[]) property.getValues().iterator().next());
                    } else if (Character.class.equals(propertySchemaClass)
                            || char.class.equals(propertySchemaClass)) {

                        propertyValue =
                                (Character) property.getValues().iterator().next();
                    } else if (Integer.class.equals(propertySchemaClass)
                            || int.class.equals(propertySchemaClass)) {

                        propertyValue =
                                Integer.parseInt(
                                property.getValues().iterator().next().toString());

                    } else if (Long.class.equals(propertySchemaClass)
                            || long.class.equals(propertySchemaClass)) {

                        propertyValue =
                                Long.parseLong(
                                property.getValues().iterator().next().toString());

                    } else if (Float.class.equals(propertySchemaClass)
                            || float.class.equals(propertySchemaClass)) {

                        propertyValue =
                                Float.parseFloat(
                                property.getValues().iterator().next().toString());

                    } else if (Double.class.equals(propertySchemaClass)
                            || double.class.equals(propertySchemaClass)) {

                        propertyValue =
                                Double.parseDouble(
                                property.getValues().iterator().next().toString());

                    } else if (Boolean.class.equals(propertySchemaClass)
                            || boolean.class.equals(propertySchemaClass)) {

                        propertyValue =
                                Boolean.parseBoolean(
                                property.getValues().iterator().next().toString());

                    } else if (URI.class.equals(propertySchemaClass)) {
                        propertyValue = URI.create(
                                (String) property.getValues().iterator().next());
                    } else if (File.class.equals(propertySchemaClass)) {
                        propertyValue = new File(
                                (String) property.getValues().iterator().next());
                    } else if (String[].class.equals(propertySchemaClass)) {
                        propertyValue =
                                ((List<String>) property.getValues()).toArray(
                                new String[]{});
                    } else {
                        propertyValue =
                                (String) property.getValues().iterator().next();
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

    /**
     * Resolve username to ConnId's Uid.
     *
     * @param objectClass ConnId's object class
     * @param username to resolve
     * @param options ConnId's OperationOptions
     * @return the resolved Uid (if connector instance is capable); can be null
     * if not found
     */
    public Uid resolveUsername(
            final ObjectClass objectClass,
            final String username,
            final OperationOptions options) {

        Uid result = null;
        if (capabitilies.contains(ConnectorCapability.RESOLVE)) {
            result = connector.resolveUsername(
                    objectClass, username, options);
        } else {
            LOG.info("Resolve for {} was attempted, although the "
                    + "connector only has these capabilities: {}. No action.",
                    username, capabitilies);
        }

        return result;
    }

    /**
     * Create user on a connector instance.
     *
     * @param propagationMode propagation mode
     * @param objectClass ConnId's object class
     * @param attrs attributes for creation
     * @param options ConnId's OperationOptions
     * @param propagationAttempted if creation is actually performed (based on
     * connector instance's capabilities)
     * @return Uid for created user
     */
    public Uid create(
            final PropagationMode propagationMode,
            final ObjectClass objectClass,
            final Set<Attribute> attrs,
            final OperationOptions options,
            final Set<String> propagationAttempted) {

        Uid result = null;

        if (propagationMode == PropagationMode.ONE_PHASE
                ? capabitilies.contains(
                ConnectorCapability.ONE_PHASE_CREATE)
                : capabitilies.contains(
                ConnectorCapability.TWO_PHASES_CREATE)) {

            propagationAttempted.add("create");

            result = connector.create(objectClass, attrs, options);
        } else {
            LOG.info("Create was attempted, although the "
                    + "connector only has these capabilities: {}. No action.",
                    capabitilies);
        }

        return result;
    }

    /**
     * Update user on a connector instance.
     *
     * @param propagationMode propagation mode
     * @param objectClass ConnId's object class
     * @param uid user to be updated
     * @param attrs attributes for update
     * @param options ConnId's OperationOptions
     * @param propagationAttempted if update is actually performed (based on
     * connector instance's capabilities)
     * @return Uid for created user
     */
    public Uid update(final PropagationMode propagationMode,
            final ObjectClass objectClass,
            final Uid uid,
            final Set<Attribute> attrs,
            final OperationOptions options,
            final Set<String> propagationAttempted) {

        Uid result = null;

        if (propagationMode == PropagationMode.ONE_PHASE
                ? capabitilies.contains(
                ConnectorCapability.ONE_PHASE_UPDATE)
                : capabitilies.contains(
                ConnectorCapability.TWO_PHASES_UPDATE)) {

            propagationAttempted.add("update");

            result = connector.update(
                    objectClass, uid, attrs, options);
        } else {
            LOG.info("Update for {} was attempted, although the "
                    + "connector only has these capabilities: {}. No action.",
                    uid.getUidValue(), capabitilies);
        }

        return result;
    }

    /**
     * Delete user on a connector instance.
     *
     * @param propagationMode propagation mode
     * @param objectClass ConnId's object class
     * @param uid user to be deleted
     * @param options ConnId's OperationOptions
     * @param propagationAttempted if deletion is actually performed (based on
     * connector instance's capabilities)
     */
    public void delete(final PropagationMode propagationMode,
            final ObjectClass objectClass,
            final Uid uid,
            final OperationOptions options,
            final Set<String> propagationAttempted) {

        if (propagationMode == PropagationMode.ONE_PHASE
                ? capabitilies.contains(
                ConnectorCapability.ONE_PHASE_DELETE)
                : capabitilies.contains(
                ConnectorCapability.TWO_PHASES_DELETE)) {

            propagationAttempted.add("delete");

            connector.delete(objectClass, uid, options);
        } else {
            LOG.info("Delete for {} was attempted, although the "
                    + "connector only has these capabilities: {}. No action.",
                    uid.getUidValue(), capabitilies);
        }
    }

    /**
     * Sync users from a connector instance.
     *
     * @param token to be passed to the underlying connector
     * @return list of sync operations to be performed
     */
    public List<SyncDelta> sync(final SyncToken token) {
        final List<SyncDelta> result = new ArrayList<SyncDelta>();

        if (capabitilies.contains(ConnectorCapability.SYNC)) {
            connector.sync(ObjectClass.ACCOUNT, token,
                    new SyncResultsHandler() {

                        @Override
                        public boolean handle(final SyncDelta delta) {
                            return result.add(delta);
                        }
                    }, null);
        } else {
            LOG.info("Sync was attempted, although the "
                    + "connector only has these capabilities: {}. No action.",
                    capabitilies);
        }

        return result;
    }

    /**
     * Read latest sync token from a connector instance.
     *
     * @return latest sync token
     */
    public SyncToken getLatestSyncToken() {
        SyncToken result = null;

        if (capabitilies.contains(ConnectorCapability.SYNC)) {
            result = connector.getLatestSyncToken(ObjectClass.ACCOUNT);
        } else {
            LOG.info("getLatestSyncToken was attempted, although the "
                    + "connector only has these capabilities: {}. No action.",
                    capabitilies);
        }

        return result;
    }

    /**
     * Get remote object.
     *
     * @param objectClass ConnId's object class
     * @param uid ConnId's Uid
     * @param options ConnId's OperationOptions
     * @return ConnId's connector object for given uid
     */
    public ConnectorObject getObject(
            final ObjectClass objectClass,
            final Uid uid,
            final OperationOptions options) {

        return getObject(null, null, objectClass, uid, options);
    }

    /**
     * Get remote object used by the propagation manager in order to choose for
     * a create (object doesn't exist) or an update (object exists).
     *
     * @param propagationMode propagation mode
     * @param operationType resource operation type
     * @param objectClass ConnId's object class
     * @param uid ConnId's Uid
     * @param options ConnId's OperationOptions
     * @return ConnId's connector object for given uid
     */
    public ConnectorObject getObject(
            final PropagationMode propagationMode,
            final PropagationOperation operationType,
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
                                || (propagationMode == PropagationMode.ONE_PHASE
                                ? capabitilies.contains(
                                ConnectorCapability.ONE_PHASE_CREATE)
                                : capabitilies.contains(
                                ConnectorCapability.TWO_PHASES_CREATE))) {

                            result = connector.getObject(
                                    objectClass, uid, options);
                        }
                        break;
                    case UPDATE:
                        if (propagationMode == null
                                || (propagationMode == PropagationMode.ONE_PHASE
                                ? capabitilies.contains(
                                ConnectorCapability.ONE_PHASE_UPDATE)
                                : capabitilies.contains(
                                ConnectorCapability.TWO_PHASES_UPDATE))) {

                            result = connector.getObject(
                                    objectClass, uid, options);
                        }
                        break;
                    default:
                        result = connector.getObject(objectClass, uid, options);
                }
            }
        } else {
            LOG.info("Search was attempted, although the "
                    + "connector only has these capabilities: {}. No action.",
                    capabitilies);
        }

        return result;
    }

    /**
     * Get remote object used by the propagation manager in order to choose for
     * a create (object doesn't exist) or an update (object exists).
     *
     * @param objectClass ConnId's object class
     * @param options ConnId's OperationOptions
     * @return ConnId's connector object for given uid
     */
    public List<SyncDelta> getAllObjects(
            final ObjectClass objectClass,
            final OperationOptions options) {

        final List<SyncDelta> result = new ArrayList<SyncDelta>();

        if (capabitilies.contains(ConnectorCapability.SEARCH)) {
            connector.search(objectClass, null,
                    new ResultsHandler() {

                        @Override
                        public boolean handle(final ConnectorObject obj) {
                            final SyncDeltaBuilder bld = new SyncDeltaBuilder();
                            bld.setObject(obj);
                            bld.setUid(obj.getUid());
                            bld.setDeltaType(SyncDeltaType.CREATE_OR_UPDATE);
                            bld.setToken(new SyncToken(""));

                            return result.add(bld.build());
                        }
                    }, options);

        } else {
            LOG.info("Search was attempted, although the "
                    + "connector only has these capabilities: {}. No action.",
                    capabitilies);
        }

        return result;
    }

    /**
     * Read attribute for a given connector object.
     *
     * @param objectClass ConnId's object class
     * @param uid ConnId's Uid
     * @param options ConnId's OperationOptions
     * @param attributeName attribute to read
     * @return attribute (if present)
     */
    public Attribute getObjectAttribute(
            final ObjectClass objectClass,
            final Uid uid,
            final OperationOptions options,
            final String attributeName) {

        Attribute attribute = null;

        try {
            final ConnectorObject object =
                    connector.getObject(objectClass, uid, options);

            attribute = object.getAttributeByName(attributeName);
        } catch (NullPointerException e) {
            // ignore exception
            LOG.debug("Object for '{}' not found", uid.getUidValue());
        }

        return attribute;
    }

    /**
     *
     * @param objectClass ConnId's object class
     * @param uid ConnId's Uid
     * @param options ConnId's OperationOptions
     * @param attributeNames attributes to read
     * @return attributes (if present)
     */
    public Set<Attribute> getObjectAttributes(
            final ObjectClass objectClass,
            final Uid uid,
            final OperationOptions options,
            final Collection<String> attributeNames) {

        final Set<Attribute> attributes = new HashSet<Attribute>();

        try {
            final ConnectorObject object =
                    connector.getObject(objectClass, uid, options);

            for (String attribute : attributeNames) {
                attributes.add(object.getAttributeByName(attribute));
            }
        } catch (NullPointerException e) {
            // ignore exception
            LOG.debug("Object for '{}' not found", uid.getUidValue());
        }

        return attributes;
    }

    /**
     * Return resource schema names.
     *
     * @param showall return __NAME__ and __PASSWORD__ attribute if true.
     * @return a list of schema names.
     */
    public Set<String> getSchema(final boolean showall) {
        final Set<String> resourceSchemaNames = new HashSet<String>();

        final Schema schema = connector.schema();

        try {
            for (ObjectClassInfo info : schema.getObjectClassInfo()) {
                for (AttributeInfo attrInfo : info.getAttributeInfo()) {
                    if (showall || (!Name.NAME.equals(attrInfo.getName())
                            && !OperationalAttributes.PASSWORD_NAME.equals(
                            attrInfo.getName())
                            && !OperationalAttributes.ENABLE_NAME.equals(
                            attrInfo.getName()))) {

                        resourceSchemaNames.add(attrInfo.getName());
                    }
                }
            }
        } catch (Throwable t) {
            // catch throwable in order to manage unpredictable behaviors
            LOG.debug("Unsupported operation {}", t);
        }

        return resourceSchemaNames;
    }

    /**
     * Validate a connector instance.
     */
    public void validate() {
        connector.validate();
    }

    /**
     * Check connection to resource.
     */
    public void test() {
        connector.test();
    }

    @Override
    public String toString() {
        return "ConnectorFacadeProxy{"
                + "connector=" + connector
                + "capabitilies=" + capabitilies + '}';
    }
}
