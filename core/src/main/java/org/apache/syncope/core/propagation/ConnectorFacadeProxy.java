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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import javassist.NotFoundException;
import org.apache.syncope.core.persistence.beans.ConnInstance;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.beans.SchemaMapping;
import org.apache.syncope.core.persistence.dao.ConfDAO;
import org.apache.syncope.core.persistence.dao.MissingConfKeyException;
import org.apache.syncope.core.util.ConnBundleManager;
import org.apache.syncope.core.util.SchemaMappingUtil;
import org.apache.syncope.types.ConnConfProperty;
import org.apache.syncope.types.ConnectorCapability;
import org.apache.syncope.types.PropagationMode;
import org.apache.syncope.types.PropagationOperation;
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
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

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

    private int timeout;

    @Autowired
    private AsyncConnectorFacade asyncFacade;

    @Autowired
    private ConfDAO confDAO;

    @Autowired
    private void setTimeout() {
        try {
            timeout = Integer.parseInt(confDAO.find("connectorRequest.timeout", "60").getValue());
        } catch (Throwable t) {
            timeout = 60;
        }
    }

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
            final Object propertyValue = getPropertyValue(property);
            if (propertyValue != null) {
                properties.setPropertyValue(property.getSchema().getName(), propertyValue);
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

            final Future<Uid> future = asyncFacade.create(connector, objectClass, attrs, options);
            try {
                result = future.get(timeout, TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                throw new TimeoutException("Request timeout");
            } catch (Exception e) {
                LOG.error("Connector request execution failure", e);
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new IllegalArgumentException(e.getCause());
                }
            }

        } else {
            LOG.info("Create was attempted, although the " + "connector only has these capabilities: {}. No action.",
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

            final Future<Uid> future = asyncFacade.update(connector, objectClass, uid, attrs, options);

            try {
                result = future.get(timeout, TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                throw new TimeoutException("Request timeout");
            } catch (Exception e) {
                LOG.error("Connector request execution failure", e);
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new IllegalArgumentException(e.getCause());
                }
            }

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

            final Future<Uid> future = asyncFacade.delete(connector, objectClass, uid, options);

            try {
                future.get(timeout, TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                throw new TimeoutException("Request timeout");
            } catch (Exception e) {
                LOG.error("Connector request execution failure", e);
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new IllegalArgumentException(e.getCause());
                }
            }
        } else {
            LOG.info("Delete for {} was attempted, although the connector only has these capabilities: {}. No action.",
                    uid.getUidValue(), activeConnInstance.getCapabilities());
        }
    }

    /**
     * Sync users from a connector instance.
     *
     * @param token to be passed to the underlying connector
     * @param handler to be used to handle deltas.
     */
    public void sync(final SyncToken token, final SyncResultsHandler handler, final OperationOptions options) {

        if (activeConnInstance.getCapabilities().contains(ConnectorCapability.SYNC)) {
            final Future<SyncResultsHandler> future =
                    asyncFacade.sync(connector, token, handler, options);

            try {
                // no timeout
                future.get();
            } catch (Exception e) {
                LOG.error("Connector request execution failure", e);
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new IllegalArgumentException(e.getCause());
                }
            }

        } else {
            LOG.info("Sync was attempted, although the connector only has these capabilities: {}. No action.",
                    activeConnInstance.getCapabilities());
        }
    }

    /**
     * Read latest sync token from a connector instance.
     *
     * @return latest sync token
     */
    public SyncToken getLatestSyncToken() {
        SyncToken result = null;

        if (activeConnInstance.getCapabilities().contains(ConnectorCapability.SYNC)) {
            final Future<SyncToken> future = asyncFacade.getLatestSyncToken(connector);

            try {
                result = future.get(timeout, TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                throw new TimeoutException("Request timeout");
            } catch (Exception e) {
                LOG.error("Connector request execution failure", e);
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new IllegalArgumentException(e.getCause());
                }
            }
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
    public ConnectorObject getObject(final PropagationMode propagationMode, final PropagationOperation operationType,
            final ObjectClass objectClass, final Uid uid, final OperationOptions options) {

        Future<ConnectorObject> future = null;

        if (activeConnInstance.getCapabilities().contains(ConnectorCapability.SEARCH)) {
            if (operationType == null) {
                future = asyncFacade.getObject(connector, objectClass, uid, options);
            } else {
                switch (operationType) {
                    case CREATE:
                        if (propagationMode == null || (propagationMode == PropagationMode.ONE_PHASE
                                ? activeConnInstance.getCapabilities().contains(ConnectorCapability.ONE_PHASE_CREATE)
                                : activeConnInstance.getCapabilities().contains(ConnectorCapability.TWO_PHASES_CREATE))) {

                            future = asyncFacade.getObject(connector, objectClass, uid, options);
                        }
                        break;
                    case UPDATE:
                        if (propagationMode == null || (propagationMode == PropagationMode.ONE_PHASE
                                ? activeConnInstance.getCapabilities().contains(ConnectorCapability.ONE_PHASE_UPDATE)
                                : activeConnInstance.getCapabilities().contains(ConnectorCapability.TWO_PHASES_UPDATE))) {

                            future = asyncFacade.getObject(connector, objectClass, uid, options);
                        }
                        break;
                    default:
                        future = asyncFacade.getObject(connector, objectClass, uid, options);
                }
            }
        } else {
            LOG.info("Search was attempted, although the connector only has these capabilities: {}. No action.",
                    activeConnInstance.getCapabilities());
        }

        try {
            return future == null ? null : future.get(timeout, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new TimeoutException("Request timeout");
        } catch (Exception e) {
            LOG.error("Connector request execution failure", e);
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw new IllegalArgumentException(e.getCause());
            }
        }
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
            final Future<SyncResultsHandler> future = asyncFacade.getAllObjects(connector, objectClass, handler, options);

            try {
                // no timeout
                future.get();
            } catch (Exception e) {
                LOG.error("Connector request execution failure", e);
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new IllegalArgumentException(e.getCause());
                }
            }

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
        try {
            return asyncFacade.getObjectAttribute(connector, objectClass, uid, options, attributeName).
                    get(timeout, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new TimeoutException("Request timeout");
        } catch (Exception e) {
            LOG.error("Connector request execution failure", e);
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw new IllegalArgumentException(e.getCause());
            }
        }
    }

    /**
     *
     * @param objectClass ConnId's object class
     * @param uid ConnId's Uid
     * @param options ConnId's OperationOptions
     * @param attributeNames attributes to read
     * @return attributes (if present)
     */
    public Set<Attribute> getObjectAttributes(final ObjectClass objectClass, final Uid uid,
            final OperationOptions options) {

        try {
            return asyncFacade.getObjectAttributes(connector, objectClass, uid, options).get(timeout, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new TimeoutException("Request timeout");
        } catch (Exception e) {
            LOG.error("Connector request execution failure", e);
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw new IllegalArgumentException(e.getCause());
            }
        }
    }

    /**
     * Return resource schema names.
     *
     * @param showall return __NAME__ and __PASSWORD__ attribute if true.
     * @return a list of schema names.
     */
    public Set<String> getSchema(final boolean showall) {
        try {
            return asyncFacade.getSchema(connector, showall).get(timeout, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new TimeoutException("Request timeout");
        } catch (Exception e) {
            LOG.error("Connector request execution failure", e);
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw new IllegalArgumentException(e.getCause());
            }
        }
    }

    /**
     * Validate a connector instance.
     */
    public void validate() {
        try {
            asyncFacade.test(connector).get(timeout, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new TimeoutException("Request timeout");
        } catch (Exception e) {
            LOG.error("Connector request execution failure", e);
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw new IllegalArgumentException(e.getCause());
            }
        }
    }

    /**
     * Check connection to resource.
     */
    public void test() {
        try {
            asyncFacade.test(connector).get(timeout, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            throw new TimeoutException("Request timeout");
        } catch (Exception e) {
            LOG.error("Connector request execution failure", e);
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw new IllegalArgumentException(e.getCause());
            }
        }
    }

    @Override
    public String toString() {
        return "ConnectorFacadeProxy{connector=" + connector + "capabitilies="
                + activeConnInstance.getCapabilities() + '}';
    }

    /**
     * Getter for active connector instance.
     *
     * @return active connector instance.
     */
    public ConnInstance getActiveConnInstance() {
        return activeConnInstance;
    }

    public OperationOptions getOperationOptions(final ExternalResource resource) {

        // -------------------------------------
        // Ask just for mapped attributes
        // -------------------------------------
        final OperationOptionsBuilder oob = new OperationOptionsBuilder();

        final Set<String> attributesToGet = new HashSet<String>(Arrays.asList(new String[]{Name.NAME, Uid.NAME,
                    OperationalAttributes.ENABLE_NAME}));

        for (SchemaMapping mapping : resource.getMappings()) {
            final String extAttrName = SchemaMappingUtil.getExtAttrName(mapping);

            if (StringUtils.hasText(extAttrName)) {
                attributesToGet.add(extAttrName);
            }
        }

        oob.setAttributesToGet(attributesToGet);
        // -------------------------------------

        return oob.build();
    }

    private Object getPropertyValue(final ConnConfProperty property) {
        Object value = null;

        final List<Object> values = property.getValues();

        if (values != null && !values.isEmpty()) {
            try {
                final Class propertySchemaClass = ClassUtils.forName(property.getSchema().getType(), ClassUtils.
                        getDefaultClassLoader());

                if (GuardedString.class.equals(propertySchemaClass)) {
                    value = new GuardedString((values.get(0).toString()).toCharArray());
                } else if (GuardedByteArray.class.equals(propertySchemaClass)) {
                    value = new GuardedByteArray((byte[]) values.get(0));
                } else if (Character.class.equals(propertySchemaClass) || Character.TYPE.equals(propertySchemaClass)) {
                    value = values.get(0) != null && !values.get(0).toString().isEmpty()
                            ? values.get(0).toString().charAt(0)
                            : null;
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
                    value = values.get(0).toString();
                }
            } catch (Exception e) {
                LOG.error("Invalid ConnConfProperty specified: {}", property, e);
            }
        }

        return value;
    }
}
