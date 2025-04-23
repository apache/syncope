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
package org.apache.syncope.core.provisioning.java;

import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.syncope.core.persistence.api.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.utils.ConnPoolConfUtils;
import org.apache.syncope.core.provisioning.api.ConnIdBundleManager;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.TimeoutException;
import org.apache.syncope.core.provisioning.api.pushpull.ReconFilterBuilder;
import org.identityconnectors.common.CollectionUtil;
import org.identityconnectors.common.security.GuardedByteArray;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeDelta;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.LiveSyncResultsHandler;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.ClassUtils;

public class ConnectorFacadeProxy implements Connector {

    private static final Logger LOG = LoggerFactory.getLogger(ConnectorFacadeProxy.class);

    private static final Integer DEFAULT_PAGE_SIZE = 100;

    /**
     * Connector facade wrapped instance.
     */
    private final ConnectorFacade connector;

    /**
     * Active connector instance.
     */
    private final ConnInstance connInstance;

    private final AsyncConnectorFacade asyncFacade;

    /**
     * Use the passed connector instance to build a ConnectorFacade that will be used to make all wrapped calls.
     *
     * @param connInstance the connector instance
     * @param asyncFacade the async connectot facade
     * @see ConnectorInfo
     * @see APIConfiguration
     * @see ConfigurationProperties
     * @see ConnectorFacade
     */
    public ConnectorFacadeProxy(final ConnInstance connInstance, final AsyncConnectorFacade asyncFacade) {
        this.connInstance = connInstance;
        this.asyncFacade = asyncFacade;

        ConnIdBundleManager connIdBundleManager =
                ApplicationContextProvider.getBeanFactory().getBean(ConnIdBundleManager.class);
        ConnectorInfo info = connIdBundleManager.getConnectorInfo(connInstance).getRight();

        // create default configuration
        APIConfiguration apiConfig = info.createDefaultAPIConfiguration();
        if (connInstance.getDisplayName() != null) {
            apiConfig.setInstanceName(connInstance.getDisplayName());
        }
        // enable filtered results handler in validation mode
        apiConfig.getResultsHandlerConfiguration().setFilteredResultsHandlerInValidationMode(true);

        // set connector configuration according to conninstance's
        ConfigurationProperties properties = apiConfig.getConfigurationProperties();
        connInstance.getConf().stream().
                filter(property -> !CollectionUtil.isEmpty(property.getValues())).
                forEach(property -> properties.setPropertyValue(
                property.getSchema().getName(),
                getPropertyValue(property.getSchema().getType(), property.getValues())));

        // set pooling configuration (if supported) according to conninstance's
        if (connInstance.getPoolConf() != null) {
            if (apiConfig.isConnectorPoolingSupported()) {
                ConnPoolConfUtils.updateObjectPoolConfiguration(
                        apiConfig.getConnectorPoolConfiguration(), connInstance.getPoolConf());
            } else {
                LOG.warn("Connector pooling not supported for {}", info);
            }
        }

        // gets new connector, with the given configuration
        connector = ConnectorFacadeFactory.getInstance().newInstance(apiConfig);

        // make sure we have set up the Configuration properly
        connector.validate();
    }

    @Override
    public Uid authenticate(final String username, final String password, final OperationOptions options) {
        Uid result = null;

        if (connInstance.getCapabilities().contains(ConnectorCapability.AUTHENTICATE)) {
            Future<Uid> future = asyncFacade.authenticate(
                    connector, username, new GuardedString(password.toCharArray()), options);
            try {
                result = future.get(connInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                future.cancel(true);
                throw new TimeoutException("Request timeout");
            } catch (Exception e) {
                LOG.error("Connector request execution failure", e);
                if (e.getCause() instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                } else {
                    throw new RuntimeException(e.getCause());
                }
            }
        } else {
            LOG.info("Authenticate was attempted, although the connector only has these capabilities: {}. No action.",
                    connInstance.getCapabilities());
        }

        return result;
    }

    @Override
    public Uid create(
            final ObjectClass objectClass,
            final Set<Attribute> attrs,
            final OperationOptions options,
            final Mutable<Boolean> propagationAttempted) {

        Uid result = null;

        if (connInstance.getCapabilities().contains(ConnectorCapability.CREATE)) {
            propagationAttempted.setValue(true);

            Future<Uid> future = asyncFacade.create(connector, objectClass, attrs, options);
            try {
                result = future.get(connInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                future.cancel(true);
                throw new TimeoutException("Request timeout");
            } catch (Exception e) {
                LOG.error("Connector request execution failure", e);
                if (e.getCause() instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                } else {
                    throw new RuntimeException(e.getCause());
                }
            }
        } else {
            LOG.info("Create was attempted, although the connector only has these capabilities: {}. No action.",
                    connInstance.getCapabilities());
        }

        return result;
    }

    @Override
    public Uid update(
            final ObjectClass objectClass,
            final Uid uid,
            final Set<Attribute> attrs,
            final OperationOptions options,
            final Mutable<Boolean> propagationAttempted) {

        Uid result = null;

        if (connInstance.getCapabilities().contains(ConnectorCapability.UPDATE)) {
            propagationAttempted.setValue(true);

            Future<Uid> future = asyncFacade.update(connector, objectClass, uid, attrs, options);

            try {
                result = future.get(connInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                future.cancel(true);
                throw new TimeoutException("Request timeout");
            } catch (Exception e) {
                LOG.error("Connector request execution failure", e);
                if (e.getCause() instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                } else {
                    throw new RuntimeException(e.getCause());
                }
            }
        } else {
            LOG.info("Update for {} was attempted, although the "
                    + "connector only has these capabilities: {}. No action.",
                    uid.getUidValue(), connInstance.getCapabilities());
        }

        return result;
    }

    @Override
    public Set<AttributeDelta> updateDelta(
            final ObjectClass objectClass,
            final Uid uid,
            final Set<AttributeDelta> modifications,
            final OperationOptions options,
            final Mutable<Boolean> propagationAttempted) {

        Set<AttributeDelta> result = null;

        if (connInstance.getCapabilities().contains(ConnectorCapability.UPDATE_DELTA)) {
            propagationAttempted.setValue(true);

            Future<Set<AttributeDelta>> future =
                    asyncFacade.updateDelta(connector, objectClass, uid, modifications, options);

            try {
                result = future.get(connInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                future.cancel(true);
                throw new TimeoutException("Request timeout");
            } catch (Exception e) {
                LOG.error("Connector request execution failure", e);
                if (e.getCause() instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                } else {
                    throw new RuntimeException(e.getCause());
                }
            }
        } else {
            LOG.info("UpdateDelta for {} was attempted, although the "
                    + "connector only has these capabilities: {}. No action.",
                    uid.getUidValue(), connInstance.getCapabilities());
        }

        return result;
    }

    @Override
    public void delete(
            final ObjectClass objectClass,
            final Uid uid,
            final OperationOptions options,
            final Mutable<Boolean> propagationAttempted) {

        if (connInstance.getCapabilities().contains(ConnectorCapability.DELETE)) {
            propagationAttempted.setValue(true);

            Future<Uid> future = asyncFacade.delete(connector, objectClass, uid, options);

            try {
                future.get(connInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                future.cancel(true);
                throw new TimeoutException("Request timeout");
            } catch (Exception e) {
                LOG.error("Connector request execution failure", e);
                if (e.getCause() instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                } else {
                    throw new RuntimeException(e.getCause());
                }
            }
        } else {
            LOG.info("Delete for {} was attempted, although the connector only has these capabilities: {}. No action.",
                    uid.getUidValue(), connInstance.getCapabilities());
        }
    }

    @Override
    public void sync(
            final ObjectClass objectClass,
            final SyncToken token,
            final SyncResultsHandler handler,
            final OperationOptions options) {

        if (connInstance.getCapabilities().contains(ConnectorCapability.SYNC)) {
            connector.sync(objectClass, token, handler, options);
        } else {
            LOG.info("Sync was attempted, although the connector only has these capabilities: {}. No action.",
                    connInstance.getCapabilities());
        }
    }

    @Override
    public SyncToken getLatestSyncToken(final ObjectClass objectClass) {
        SyncToken result = null;

        if (connInstance.getCapabilities().contains(ConnectorCapability.SYNC)) {
            Future<SyncToken> future = asyncFacade.getLatestSyncToken(connector, objectClass);

            try {
                result = future.get(connInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
            } catch (java.util.concurrent.TimeoutException e) {
                future.cancel(true);
                throw new TimeoutException("Request timeout");
            } catch (Exception e) {
                LOG.error("Connector request execution failure", e);
                if (e.getCause() instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                } else {
                    throw new RuntimeException(e.getCause());
                }
            }
        } else {
            LOG.info("getLatestSyncToken was attempted, although the "
                    + "connector only has these capabilities: {}. No action.", connInstance.getCapabilities());
        }

        return result;
    }

    @Override
    public void livesync(
            final ObjectClass objectClass,
            final LiveSyncResultsHandler handler,
            final OperationOptions options) {

        if (connInstance.getCapabilities().contains(ConnectorCapability.LIVE_SYNC)) {
            connector.livesync(objectClass, handler, options);
        } else {
            LOG.info("livesync was attempted, although the connector only has these capabilities: {}. No action.",
                    connInstance.getCapabilities());
        }
    }

    @Override
    public void fullReconciliation(
            final ObjectClass objectClass,
            final SyncResultsHandler handler,
            final OperationOptions options) {

        Connector.super.fullReconciliation(objectClass, handler, options);
    }

    @Override
    public void filteredReconciliation(
            final ObjectClass objectClass,
            final ReconFilterBuilder filterBuilder,
            final SyncResultsHandler handler,
            final OperationOptions options) {

        Connector.super.filteredReconciliation(objectClass, filterBuilder, handler, options);
    }

    @Override
    public Set<ObjectClassInfo> getObjectClassInfo() {
        Future<Set<ObjectClassInfo>> future = asyncFacade.getObjectClassInfo(connector);
        try {
            return future.get(connInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            throw new TimeoutException("Request timeout");
        } catch (Exception e) {
            LOG.error("Connector request execution failure", e);
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            } else {
                throw new RuntimeException(e.getCause());
            }
        }
    }

    @Override
    public void validate() {
        Future<String> future = asyncFacade.test(connector);
        try {
            future.get(connInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            throw new TimeoutException("Request timeout");
        } catch (Exception e) {
            LOG.error("Connector request execution failure", e);
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            } else {
                throw new RuntimeException(e.getCause());
            }
        }
    }

    @Override
    public void test() {
        Future<String> future = asyncFacade.test(connector);
        try {
            future.get(connInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            throw new TimeoutException("Request timeout");
        } catch (Exception e) {
            LOG.error("Connector request execution failure", e);
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            } else {
                throw new RuntimeException(e.getCause());
            }
        }
    }

    @Override
    public ConnectorObject getObject(
            final ObjectClass objectClass,
            final Attribute connObjectKey,
            final boolean ignoreCaseMatch,
            final OperationOptions options) {

        Future<ConnectorObject> future;

        if (connInstance.getCapabilities().contains(ConnectorCapability.SEARCH)) {
            future = asyncFacade.getObject(connector, objectClass, connObjectKey, ignoreCaseMatch, options);
        } else {
            LOG.info("Search was attempted, although the connector only has these capabilities: {}. No action.",
                    connInstance.getCapabilities());
            return null;
        }

        try {
            return future.get(connInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            future.cancel(true);
            throw new TimeoutException("Request timeout");
        } catch (Exception e) {
            LOG.error("Connector request execution failure", e);
            if (e.getCause() instanceof RuntimeException runtimeException) {
                throw runtimeException;
            } else {
                throw new RuntimeException(e.getCause());
            }
        }
    }

    @Override
    public SearchResult search(
            final ObjectClass objectClass,
            final Filter filter,
            final SearchResultsHandler handler,
            final OperationOptions options) {

        SearchResult result = null;

        if (connInstance.getCapabilities().contains(ConnectorCapability.SEARCH)) {
            if (options.getPageSize() == null && options.getPagedResultsCookie() == null) {
                OperationOptionsBuilder builder = new OperationOptionsBuilder(options).
                        setPageSize(DEFAULT_PAGE_SIZE).setPagedResultsOffset(-1);

                final String[] cookies = { null };
                do {
                    if (cookies[0] != null) {
                        builder.setPagedResultsCookie(cookies[0]);
                    }

                    result = connector.search(objectClass, filter, new SearchResultsHandler() {

                        @Override
                        public void handleResult(final SearchResult result) {
                            handler.handleResult(result);
                            cookies[0] = result.getPagedResultsCookie();
                        }

                        @Override
                        public boolean handle(final ConnectorObject connectorObject) {
                            return handler.handle(connectorObject);
                        }
                    }, builder.build());
                } while (cookies[0] != null);
            } else {
                result = connector.search(objectClass, filter, handler, options);
            }
        } else {
            LOG.info("Search was attempted, although the connector only has these capabilities: {}. No action.",
                    connInstance.getCapabilities());
        }

        return result;
    }

    @Override
    public void dispose() {
        connector.dispose();
    }

    @Override
    public ConnInstance getConnInstance() {
        return connInstance;
    }

    private static Object getPropertyValue(final String propType, final List<?> values) {
        Object value = null;

        try {
            Class<?> propertySchemaClass = ClassUtils.forName(propType, ClassUtils.getDefaultClassLoader());

            if (GuardedString.class.equals(propertySchemaClass)) {
                value = new GuardedString(values.getFirst().toString().toCharArray());
            } else if (GuardedByteArray.class.equals(propertySchemaClass)) {
                value = new GuardedByteArray((byte[]) values.getFirst());
            } else if (Character.class.equals(propertySchemaClass) || Character.TYPE.equals(propertySchemaClass)) {
                value = values.getFirst() == null || values.getFirst().toString().isEmpty()
                        ? null : values.getFirst().toString().charAt(0);
            } else if (Integer.class.equals(propertySchemaClass) || Integer.TYPE.equals(propertySchemaClass)) {
                value = Integer.valueOf(values.getFirst().toString());
            } else if (Long.class.equals(propertySchemaClass) || Long.TYPE.equals(propertySchemaClass)) {
                value = Long.valueOf(values.getFirst().toString());
            } else if (Float.class.equals(propertySchemaClass) || Float.TYPE.equals(propertySchemaClass)) {
                value = Float.valueOf(values.getFirst().toString());
            } else if (Double.class.equals(propertySchemaClass) || Double.TYPE.equals(propertySchemaClass)) {
                value = Double.valueOf(values.getFirst().toString());
            } else if (Boolean.class.equals(propertySchemaClass) || Boolean.TYPE.equals(propertySchemaClass)) {
                value = Boolean.valueOf(values.getFirst().toString());
            } else if (URI.class.equals(propertySchemaClass)) {
                value = URI.create(values.getFirst().toString());
            } else if (File.class.equals(propertySchemaClass)) {
                value = Path.of(values.getFirst().toString()).toFile();
            } else if (String[].class.equals(propertySchemaClass)) {
                value = values.toArray(String[]::new);
            } else {
                value = values.getFirst() == null ? null : values.getFirst().toString();
            }
        } catch (Exception e) {
            LOG.error("Invalid ConnConfProperty specified: {} {}", propType, values, e);
        }

        return value;
    }

    @Override
    public String toString() {
        return "ConnectorFacadeProxy{"
                + "connector=" + connector + '\n' + "capabitilies=" + connInstance.getCapabilities() + '}';
    }
}
