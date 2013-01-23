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
package org.apache.syncope.core.propagation.impl;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.syncope.common.types.ConnConfProperty;
import org.apache.syncope.common.types.ConnectorCapability;
import org.apache.syncope.common.types.PropagationMode;
import org.apache.syncope.common.types.ResourceOperation;
import org.apache.syncope.core.persistence.beans.AbstractMappingItem;
import org.apache.syncope.core.persistence.beans.ConnInstance;
import org.apache.syncope.core.persistence.dao.ConfDAO;
import org.apache.syncope.core.persistence.dao.MissingConfKeyException;
import org.apache.syncope.core.propagation.SyncopeConnector;
import org.apache.syncope.core.propagation.TimeoutException;
import org.apache.syncope.core.util.ConnBundleManager;
import org.apache.syncope.core.util.NotFoundException;
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
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.SyncDeltaBuilder;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ClassUtils;

/**
 * Intercept calls to ConnectorFacade's methods and check if the corresponding connector instance has been configured to
 * allow every single operation: if not, simply do nothing.
 */
public class ConnectorFacadeProxy implements SyncopeConnector {

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
     * @see ConnInstance
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

    /* (non-Javadoc)
     * @see SyncopeConnector#create(PropagationMode, ObjectClass, Set, OperationOptions, Set)
     */
    @Override
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
                future.cancel(true);
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
            LOG.info("Create was attempted, although the connector only has these capabilities: {}. No action.",
                    activeConnInstance.getCapabilities());
        }

        return result;
    }

    /* (non-Javadoc)
     * @see SyncopeConnector#update(PropagationMode, ObjectClass, Uid, Set, OperationOptions, Set)
     */
    @Override
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
                future.cancel(true);
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

    /* (non-Javadoc)
     * @see SyncopeConnector#delete(PropagationMode, ObjectClass, Uid, OperationOptions, Set)
     */
    @Override
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
                future.cancel(true);
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

    /* (non-Javadoc)
     * @see SyncopeConnector#sync(ObjectClass, SyncToken, SyncResultsHandler, OperationOptions)
     */
    @Override
    public void sync(final ObjectClass objectClass, final SyncToken token, final SyncResultsHandler handler,
            final OperationOptions options) {

        if (activeConnInstance.getCapabilities().contains(ConnectorCapability.SYNC)) {
            connector.sync(objectClass, token, handler, options);
        } else {
            LOG.info("Sync was attempted, although the connector only has these capabilities: {}. No action.",
                    activeConnInstance.getCapabilities());
        }
    }

    /* (non-Javadoc)
     * @see SyncopeConnector#getLatestSyncToken(ObjectClass)
     */
    @Override
    public SyncToken getLatestSyncToken(final ObjectClass objectClass) {
        SyncToken result = null;

        if (activeConnInstance.getCapabilities().contains(ConnectorCapability.SYNC)) {
            final Future<SyncToken> future = asyncFacade.getLatestSyncToken(connector, objectClass);

            try {
                result = future.get(timeout, TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                future.cancel(true);
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

    /* (non-Javadoc)
     * @see SyncopeConnector#getObject(ObjectClass, Uid, OperationOptions)
     */
    @Override
    public ConnectorObject getObject(final ObjectClass objectClass, final Uid uid, final OperationOptions options) {
        return getObject(null, null, objectClass, uid, options);
    }

    /* (non-Javadoc)
     * @see SyncopeConnector#getObject(PropagationMode, ResourceOperation, ObjectClass, Uid, OperationOptions)
     */
    @Override
    public ConnectorObject getObject(final PropagationMode propagationMode, final ResourceOperation operationType,
            final ObjectClass objectClass, final Uid uid, final OperationOptions options) {

        Future<ConnectorObject> future = null;

        if (activeConnInstance.getCapabilities().contains(ConnectorCapability.SEARCH)) {
            if (operationType == null) {
                future = asyncFacade.getObject(connector, objectClass, uid, options);
            } else {
                switch (operationType) {
                    case CREATE:
                        if (propagationMode == null || (propagationMode == PropagationMode.ONE_PHASE
                                ? activeConnInstance.getCapabilities().
                                contains(ConnectorCapability.ONE_PHASE_CREATE)
                                : activeConnInstance.getCapabilities().
                                contains(ConnectorCapability.TWO_PHASES_CREATE))) {

                            future = asyncFacade.getObject(connector, objectClass, uid, options);
                        }
                        break;
                    case UPDATE:
                        if (propagationMode == null || (propagationMode == PropagationMode.ONE_PHASE
                                ? activeConnInstance.getCapabilities().
                                contains(ConnectorCapability.ONE_PHASE_UPDATE)
                                : activeConnInstance.getCapabilities().
                                contains(ConnectorCapability.TWO_PHASES_UPDATE))) {

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
            future.cancel(true);
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

    /* (non-Javadoc)
     * @see SyncopeConnector#search(ObjectClass, filter.Filter, OperationOptions)
     */
    @Override
    public List<ConnectorObject> search(
            final ObjectClass objectClass, final Filter filter, final OperationOptions options) {

        final List<ConnectorObject> result = new ArrayList<ConnectorObject>();

        search(objectClass, filter, new ResultsHandler() {

            @Override
            public boolean handle(final ConnectorObject obj) {
                return result.add(obj);
            }
        }, options);

        return result;
    }

    /* (non-Javadoc)
     * @see SyncopeConnector#getAllObjects(ObjectClass, SyncResultsHandler, OperationOptions)
     */
    @Override
    public void getAllObjects(
            final ObjectClass objectClass, final SyncResultsHandler handler, final OperationOptions options) {

        search(objectClass, null, new ResultsHandler() {

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
    }

    /* (non-Javadoc)
     * @see SyncopeConnector#getObjectAttribute(ObjectClass, Uid, OperationOptions, String)
     */
    @Override
    public Attribute getObjectAttribute(final ObjectClass objectClass, final Uid uid, final OperationOptions options,
            final String attributeName) {
        final Future<Attribute> future = asyncFacade.getObjectAttribute(connector, objectClass, uid, options,
                attributeName);
        try {
            return future.get(timeout, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
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

    /* (non-Javadoc)
     * @see SyncopeConnector#getObjectAttributes(ObjectClass, Uid, OperationOptions)
     */
    @Override
    public Set<Attribute> getObjectAttributes(final ObjectClass objectClass, final Uid uid,
            final OperationOptions options) {
        final Future<Set<Attribute>> future = asyncFacade.getObjectAttributes(connector, objectClass, uid, options);
        try {
            return future.get(timeout, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
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

    /* (non-Javadoc)
     * @see SyncopeConnector#getSchema(boolean)
     */
    @Override
    public Set<String> getSchema(final boolean showall) {
        final Future<Set<String>> future = asyncFacade.getSchema(connector, showall);
        try {
            return future.get(timeout, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
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

    /* (non-Javadoc)
     * @see SyncopeConnector#validate()
     */
    @Override
    public void validate() {
        final Future<String> future = asyncFacade.test(connector);
        try {
            future.get(timeout, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
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

    /* (non-Javadoc)
     * @see SyncopeConnector#test()
     */
    @Override
    public void test() {
        final Future<String> future = asyncFacade.test(connector);
        try {
            future.get(timeout, TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
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

    private void search(
            final ObjectClass objectClass,
            final Filter filter,
            final ResultsHandler handler,
            final OperationOptions options) {

        if (activeConnInstance.getCapabilities().contains(ConnectorCapability.SEARCH)) {
            connector.search(objectClass, filter, handler, options);

        } else {
            LOG.info("Search was attempted, although the connector only has these capabilities: {}. No action.",
                    activeConnInstance.getCapabilities());
        }
    }

    /* (non-Javadoc)
     * @see SyncopeConnector#getActiveConnInstance()
     */
    @Override
    public ConnInstance getActiveConnInstance() {
        return activeConnInstance;
    }

    /* (non-Javadoc)
     * @see SyncopeConnector#getOperationOptions(Collection)
     */
    @Override
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

    private Object getPropertyValue(final String propType, final List<?> values) {
        Object value = null;

        try {
            final Class<?> propertySchemaClass = ClassUtils.forName(propType, ClassUtils.getDefaultClassLoader());

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

    /* (non-Javadoc)
     * @see SyncopeConnector#toString()
     */
    @Override
    public String toString() {
        return "ConnectorFacadeProxy{"
                + "connector=" + connector + "\n" + "capabitilies=" + activeConnInstance.getCapabilities() + '}';
    }
}
