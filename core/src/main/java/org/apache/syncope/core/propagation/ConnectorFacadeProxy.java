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
package org.apache.syncope.core.propagation;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.syncope.core.persistence.beans.AbstractMappingItem;
import org.apache.syncope.core.persistence.beans.ConnInstance;
import org.apache.syncope.core.persistence.dao.MissingConfKeyException;
import org.apache.syncope.core.util.ConnBundleManager;
import org.apache.syncope.core.util.NotFoundException;
import org.apache.syncope.types.ConnConfProperty;
import org.apache.syncope.types.ConnectorCapability;
import org.apache.syncope.types.PropagationMode;
import org.apache.syncope.types.ResourceOperation;
import org.identityconnectors.common.security.GuardedByteArray;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorKey;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeInfo;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.Schema;
import org.identityconnectors.framework.common.objects.SyncDeltaBuilder;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

/**
 * Intercept calls to ConnectorFacade's methods and check if the corresponding connector instance has been configured to
 * allow every single operation: if not, simply do nothing.
 */
public class ConnectorFacadeProxy {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(ConnectorFacadeProxy.class);

    /**
     * Connector facade wrapped instance.
     */
    private final ConnectorFacade connector;

    /**
     * Active Connector instance.
     *
     * @see org.apache.syncope.core.persistence.beans.ConnInstance
     */
    private final ConnInstance activeConnInstance;

    /**
     * Use the passed connector instance to build a ConnectorFacade that will be used to make all wrapped calls.
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
    public ConnectorFacadeProxy(final ConnInstance connInstance, final ConnBundleManager connBundleManager)
            throws NotFoundException {

        this.activeConnInstance = connInstance;

        // specify a connector.
        ConnectorKey key = new ConnectorKey(connInstance.getBundleName(), connInstance.getVersion(), connInstance.
                getConnectorName());

        if (LOG.isDebugEnabled()) {
            LOG.debug("\nBundle name: " + key.getBundleName() + "\nBundle version: " + key.getBundleVersion()
                    + "\nBundle class: " + key.getConnectorName());
        }

        // get the specified connector.
        ConnectorInfo info;
        try {
            info = connBundleManager.getConnectorManager().findConnectorInfo(key);
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
        final ConfigurationProperties properties = apiConfig.getConfigurationProperties();
        if (properties == null) {
            throw new NotFoundException("Configuration properties");
        }

        // Print out what the properties are (not necessary)
        if (LOG.isDebugEnabled()) {
            for (String propName : properties.getPropertyNames()) {
                LOG.debug("\nProperty Name: {}\nProperty Type: {}", properties.getProperty(propName).getName(),
                        properties.getProperty(propName).getType());
            }
        }

        // Set all of the ConfigurationProperties needed by the connector.
        for (ConnConfProperty property : connInstance.getConfiguration()) {
            if (property.getValues() != null && !property.getValues().isEmpty()) {
                properties.setPropertyValue(property.getSchema().getName(),
                        getPropertyValue(property.getSchema().getType(), property.getValues()));
            }
        }

        // Use the ConnectorFacadeFactory's newInstance() method to get a new connector.
        connector = ConnectorFacadeFactory.getInstance().newInstance(apiConfig);
        if (connector == null) {
            throw new NotFoundException("Connector");
        }

        // Make sure we have set up the Configuration properly
        connector.validate();
    }

    /**
     * Create user on a connector instance.
     *
     * @param propagationMode propagation mode
     * @param objectClass ConnId's object class
     * @param attrs attributes for creation
     * @param options ConnId's OperationOptions
     * @param propagationAttempted if creation is actually performed (based on connector instance's capabilities)
     * @return Uid for created user
     */
    public Uid create(final PropagationMode propagationMode, final ObjectClass objectClass, final Set<Attribute> attrs,
            final OperationOptions options, final Set<String> propagationAttempted) {

        Uid result = null;

        if (propagationMode == PropagationMode.ONE_PHASE
                ? activeConnInstance.getCapabilities().contains(ConnectorCapability.ONE_PHASE_CREATE)
                : activeConnInstance.getCapabilities().contains(ConnectorCapability.TWO_PHASES_CREATE)) {

            propagationAttempted.add("create");

            result = connector.create(objectClass, attrs, options);
        } else {
            LOG.info("Create was attempted, although the connector only has these capabilities: {}. No action.",
                    activeConnInstance.getCapabilities());
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
     * @param propagationAttempted if update is actually performed (based on connector instance's capabilities)
     * @return Uid for created user
     */
    public Uid update(final PropagationMode propagationMode, final ObjectClass objectClass, final Uid uid,
            final Set<Attribute> attrs, final OperationOptions options, final Set<String> propagationAttempted) {

        Uid result = null;

        if (propagationMode == PropagationMode.ONE_PHASE
                ? activeConnInstance.getCapabilities().contains(ConnectorCapability.ONE_PHASE_UPDATE)
                : activeConnInstance.getCapabilities().contains(ConnectorCapability.TWO_PHASES_UPDATE)) {

            propagationAttempted.add("update");

            result = connector.update(objectClass, uid, attrs, options);
        } else {
            LOG.info("Update for {} was attempted, although the "
                    + "connector only has these capabilities: {}. No action.", uid.getUidValue(), activeConnInstance.
                    getCapabilities());
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
     * @param propagationAttempted if deletion is actually performed (based on connector instance's capabilities)
     */
    public void delete(final PropagationMode propagationMode, final ObjectClass objectClass, final Uid uid,
            final OperationOptions options, final Set<String> propagationAttempted) {

        if (propagationMode == PropagationMode.ONE_PHASE
                ? activeConnInstance.getCapabilities().contains(ConnectorCapability.ONE_PHASE_DELETE)
                : activeConnInstance.getCapabilities().contains(ConnectorCapability.TWO_PHASES_DELETE)) {

            propagationAttempted.add("delete");

            connector.delete(objectClass, uid, options);
        } else {
            LOG.info("Delete for {} was attempted, although the connector only has these capabilities: {}. No action.",
                    uid.getUidValue(), activeConnInstance.getCapabilities());
        }
    }

    /**
     * Sync users from a connector instance.
     *
     * @param objectClass ConnId's object class.
     * @param token to be passed to the underlying connector
     * @param handler to be used to handle deltas.
     */
    public void sync(final ObjectClass objectClass, final SyncToken token, final SyncResultsHandler handler,
            final OperationOptions options) {

        if (activeConnInstance.getCapabilities().contains(ConnectorCapability.SYNC)) {
            connector.sync(objectClass, token, handler, options);
        } else {
            LOG.info("Sync was attempted, although the connector only has these capabilities: {}. No action.",
                    activeConnInstance.getCapabilities());
        }
    }

    /**
     * Read latest sync token from a connector instance.
     *
     * @param objectClass ConnId's object class.
     * @return latest sync token
     */
    public SyncToken getLatestSyncToken(final ObjectClass objectClass) {
        SyncToken result = null;

        if (activeConnInstance.getCapabilities().contains(ConnectorCapability.SYNC)) {
            result = connector.getLatestSyncToken(objectClass);
        } else {
            LOG.info("getLatestSyncToken was attempted, although the "
                    + "connector only has these capabilities: {}. No action.", activeConnInstance.getCapabilities());
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
    public ConnectorObject getObject(final ObjectClass objectClass, final Uid uid, final OperationOptions options) {
        return getObject(null, null, objectClass, uid, options);
    }

    /**
     * Get remote object used by the propagation manager in order to choose for a create (object doesn't exist) or an
     * update (object exists).
     *
     * @param propagationMode propagation mode
     * @param operationType resource operation type
     * @param objectClass ConnId's object class
     * @param uid ConnId's Uid
     * @param options ConnId's OperationOptions
     * @return ConnId's connector object for given uid
     */
    public ConnectorObject getObject(final PropagationMode propagationMode, final ResourceOperation operationType,
            final ObjectClass objectClass, final Uid uid, final OperationOptions options) {

        ConnectorObject result = null;

        if (activeConnInstance.getCapabilities().contains(ConnectorCapability.SEARCH)) {
            if (operationType == null) {
                result = connector.getObject(objectClass, uid, options);
            } else {
                switch (operationType) {
                    case CREATE:
                        if (propagationMode == null || (propagationMode == PropagationMode.ONE_PHASE
                                ? activeConnInstance.getCapabilities().
                                contains(ConnectorCapability.ONE_PHASE_CREATE)
                                : activeConnInstance.getCapabilities().
                                contains(ConnectorCapability.TWO_PHASES_CREATE))) {

                            result = connector.getObject(objectClass, uid, options);
                        }
                        break;
                    case UPDATE:
                        if (propagationMode == null || (propagationMode == PropagationMode.ONE_PHASE
                                ? activeConnInstance.getCapabilities().
                                contains(ConnectorCapability.ONE_PHASE_UPDATE)
                                : activeConnInstance.getCapabilities().
                                contains(ConnectorCapability.TWO_PHASES_UPDATE))) {

                            result = connector.getObject(objectClass, uid, options);
                        }
                        break;
                    default:
                        result = connector.getObject(objectClass, uid, options);
                }
            }
        } else {
            LOG.info("Search was attempted, although the connector only has these capabilities: {}. No action.",
                    activeConnInstance.getCapabilities());
        }

        return result;
    }

    public List<ConnectorObject> search(final ObjectClass objectClass, final Filter filter,
            final OperationOptions options) {

        final List<ConnectorObject> result = new ArrayList<ConnectorObject>();

        if (activeConnInstance.getCapabilities().contains(ConnectorCapability.SEARCH)) {
            connector.search(objectClass, filter, new ResultsHandler() {

                @Override
                public boolean handle(final ConnectorObject obj) {
                    return result.add(obj);
                }
            }, options);
        } else {
            LOG.info("Search was attempted, although the connector only has these capabilities: {}. No action.",
                    activeConnInstance.getCapabilities());
        }

        return result;
    }

    /**
     * Get remote object used by the propagation manager in order to choose for a create (object doesn't exist) or an
     * update (object exists).
     *
     * @param objectClass ConnId's object class.
     * @param handler to be used to handle deltas.
     * @param options ConnId's OperationOptions.
     */
    public void getAllObjects(final ObjectClass objectClass, final SyncResultsHandler handler,
            final OperationOptions options) {

        if (activeConnInstance.getCapabilities().contains(ConnectorCapability.SEARCH)) {
            connector.search(objectClass, null, new ResultsHandler() {

                @Override
                public boolean handle(final ConnectorObject obj) {
                    final SyncDeltaBuilder bld = new SyncDeltaBuilder();
                    bld.setObject(obj);
                    bld.setUid(obj.getUid());
                    bld.setDeltaType(SyncDeltaType.CREATE_OR_UPDATE);
                    bld.setToken(new SyncToken(""));

                    return handler.handle(bld.build());
                }
            }, options);

        } else {
            LOG.info("Search was attempted, although the connector only has these capabilities: {}. No action.",
                    activeConnInstance.getCapabilities());
        }
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
    public Attribute getObjectAttribute(final ObjectClass objectClass, final Uid uid, final OperationOptions options,
            final String attributeName) {

        Attribute attribute = null;

        final ConnectorObject object = connector.getObject(objectClass, uid, options);
        if (object == null) {
            LOG.debug("Object for '{}' not found", uid.getUidValue());
        } else {
            attribute = object.getAttributeByName(attributeName);
        }

        return attribute;
    }

    /**
     *
     * @param objectClass ConnId's object class
     * @param uid ConnId's Uid
     * @param options ConnId's OperationOptions
     * @return attributes (if present)
     */
    public Set<Attribute> getObjectAttributes(final ObjectClass objectClass, final Uid uid,
            final OperationOptions options) {

        final Set<Attribute> attributes = new HashSet<Attribute>();

        ConnectorObject object = connector.getObject(objectClass, uid, options);
        if (object == null) {
            LOG.debug("Object for '{}' not found", uid.getUidValue());
        } else {
            for (String attribute : options.getAttributesToGet()) {
                attributes.add(object.getAttributeByName(attribute));
            }
        }

        return attributes;
    }

    /**
     * Return resource schema names.
     *
     * @param showall return special attributes (like as __NAME__ or __PASSWORD__) if true
     * @return a list of schema names
     */
    public Set<String> getSchema(final boolean showall) {
        final Set<String> resourceSchemaNames = new HashSet<String>();

        final Schema schema = connector.schema();

        try {
            for (ObjectClassInfo info : schema.getObjectClassInfo()) {
                for (AttributeInfo attrInfo : info.getAttributeInfo()) {
                    if (showall || !isSpecialName(attrInfo.getName())) {
                        resourceSchemaNames.add(attrInfo.getName());
                    }
                }
            }
        } catch (Exception e) {
            // catch exception in order to manage unpredictable behaviors
            LOG.debug("Unsupported operation {}", e);
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

    /**
     * Getter for active connector instance.
     *
     * @return active connector instance.
     */
    public ConnInstance getActiveConnInstance() {
        return activeConnInstance;
    }

    public OperationOptions getOperationOptions(final Collection<AbstractMappingItem> mapItems) {
        // -------------------------------------
        // Ask just for mapped attributes
        // -------------------------------------
        final OperationOptionsBuilder oob = new OperationOptionsBuilder();

        final Set<String> attrsToGet = new HashSet<String>();
        attrsToGet.add(Name.NAME);
        attrsToGet.add(Uid.NAME);
        attrsToGet.add(OperationalAttributes.ENABLE_NAME);

        for (AbstractMappingItem item : mapItems) {
            attrsToGet.add(item.getExtAttrName());
        }

        oob.setAttributesToGet(attrsToGet);
        // -------------------------------------

        return oob.build();
    }

    private Object getPropertyValue(final String propType, final List<Object> values) {
        Object value = null;

        try {
            final Class propertySchemaClass = ClassUtils.forName(propType, ClassUtils.getDefaultClassLoader());

            if (GuardedString.class.equals(propertySchemaClass)) {
                value = new GuardedString(values.get(0).toString().toCharArray());
            } else if (GuardedByteArray.class.equals(propertySchemaClass)) {
                value = new GuardedByteArray((byte[]) values.get(0));
            } else if (Character.class.equals(propertySchemaClass) || Character.TYPE.equals(propertySchemaClass)) {
                value = values.get(0) == null || values.get(0).toString().isEmpty()
                        ? null : values.get(0).toString().charAt(0);
            } else if (Integer.class.equals(propertySchemaClass) || Integer.TYPE.equals(propertySchemaClass)) {
                value = Integer.parseInt(values.get(0).toString());
            } else if (Long.class.equals(propertySchemaClass) || Long.TYPE.equals(propertySchemaClass)) {
                value = Long.parseLong(values.get(0).toString());
            } else if (Float.class.equals(propertySchemaClass) || Float.TYPE.equals(propertySchemaClass)) {
                value = Float.parseFloat(values.get(0).toString());
            } else if (Double.class.equals(propertySchemaClass) || Double.TYPE.equals(propertySchemaClass)) {
                value = Double.parseDouble(values.get(0).toString());
            } else if (Boolean.class.equals(propertySchemaClass) || Boolean.TYPE.equals(propertySchemaClass)) {
                value = Boolean.parseBoolean(values.get(0).toString());
            } else if (URI.class.equals(propertySchemaClass)) {
                value = URI.create(values.get(0).toString());
            } else if (File.class.equals(propertySchemaClass)) {
                value = new File(values.get(0).toString());
            } else if (String[].class.equals(propertySchemaClass)) {
                value = values.toArray(new String[]{});
            } else {
                value = values.get(0) == null ? null : values.get(0).toString();
            }
        } catch (Exception e) {
            LOG.error("Invalid ConnConfProperty specified: {} {}", propType, values, e);
        }

        return value;
    }

    private boolean isSpecialName(final String name) {
        return (name.startsWith("__") && name.endsWith("__"));
    }

    @Override
    public String toString() {
        return "ConnectorFacadeProxy{"
                + "connector=" + connector + "\n" + "capabitilies=" + activeConnInstance.getCapabilities() + '}';
    }
}
