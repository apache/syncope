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
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.provisioning.api.ConnIdBundleManager;
import org.apache.syncope.core.provisioning.api.utils.ConnPoolConfUtils;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.TimeoutException;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.provisioning.api.pushpull.ReconciliationFilterBuilder;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.identityconnectors.common.security.GuardedByteArray;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.ResultsHandler;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.SortKey;
import org.identityconnectors.framework.common.objects.SyncDeltaBuilder;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.identityconnectors.framework.common.objects.SyncResultsHandler;
import org.identityconnectors.framework.common.objects.SyncToken;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
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

    @Autowired
    private AsyncConnectorFacade asyncFacade;

    /**
     * Use the passed connector instance to build a ConnectorFacade that will be used to make all wrapped calls.
     *
     * @param connInstance the connector instance
     * @see ConnectorInfo
     * @see APIConfiguration
     * @see ConfigurationProperties
     * @see ConnectorFacade
     */
    public ConnectorFacadeProxy(final ConnInstance connInstance) {
        this.connInstance = connInstance;

        ConnIdBundleManager connIdBundleManager = ApplicationContextProvider.getBeanFactory().getBean(
                ConnIdBundleManager.class);
        ConnectorInfo info = connIdBundleManager.getConnectorInfo(connInstance).getRight();

        // create default configuration
        APIConfiguration apiConfig = info.createDefaultAPIConfiguration();
        // enable filtered results handler in validation mode
        apiConfig.getResultsHandlerConfiguration().setFilteredResultsHandlerInValidationMode(true);

        // set connector configuration according to conninstance's
        ConfigurationProperties properties = apiConfig.getConfigurationProperties();
        connInstance.getConf().stream().
                filter(property -> (property.getValues() != null && !property.getValues().isEmpty())).
                forEachOrdered(property -> {
                    properties.setPropertyValue(property.getSchema().getName(),
                            getPropertyValue(property.getSchema().getType(), property.getValues()));
                });

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
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new IllegalArgumentException(e.getCause());
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
            final Boolean[] propagationAttempted) {

        Uid result = null;

        if (connInstance.getCapabilities().contains(ConnectorCapability.CREATE)) {
            propagationAttempted[0] = true;

            Future<Uid> future = asyncFacade.create(connector, objectClass, attrs, options);
            try {
                result = future.get(connInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
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
            final Boolean[] propagationAttempted) {

        Uid result = null;

        if (connInstance.getCapabilities().contains(ConnectorCapability.UPDATE)) {
            propagationAttempted[0] = true;

            Future<Uid> future = asyncFacade.update(connector, objectClass, uid, attrs, options);

            try {
                result = future.get(connInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
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
            final Boolean[] propagationAttempted) {

        if (connInstance.getCapabilities().contains(ConnectorCapability.DELETE)) {
            propagationAttempted[0] = true;

            Future<Uid> future = asyncFacade.delete(connector, objectClass, uid, options);

            try {
                future.get(connInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
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
                    uid.getUidValue(), connInstance.getCapabilities());
        }
    }

    @Transactional
    @Override
    public void sync(final ObjectClass objectClass, final SyncToken token, final SyncResultsHandler handler,
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
                if (e.getCause() instanceof RuntimeException) {
                    throw (RuntimeException) e.getCause();
                } else {
                    throw new IllegalArgumentException(e.getCause());
                }
            }
        } else {
            LOG.info("getLatestSyncToken was attempted, although the "
                    + "connector only has these capabilities: {}. No action.", connInstance.getCapabilities());
        }

        return result;
    }

    @Transactional
    @Override
    public void fullReconciliation(
            final ObjectClass objectClass,
            final SyncResultsHandler handler,
            final OperationOptions options) {

        filteredReconciliation(objectClass, null, handler, options);
    }

    @Transactional
    @Override
    public void filteredReconciliation(
            final ObjectClass objectClass,
            final ReconciliationFilterBuilder filterBuilder,
            final SyncResultsHandler handler,
            final OperationOptions options) {

        search(objectClass, filterBuilder == null ? null : filterBuilder.build(), object
                -> handler.handle(new SyncDeltaBuilder().
                        setObject(object).
                        setUid(object.getUid()).
                        setDeltaType(SyncDeltaType.CREATE_OR_UPDATE).
                        setToken(new SyncToken("")).
                        build()), options);
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
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw new IllegalArgumentException(e.getCause());
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
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw new IllegalArgumentException(e.getCause());
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
            if (e.getCause() instanceof RuntimeException) {
                throw (RuntimeException) e.getCause();
            } else {
                throw new IllegalArgumentException(e.getCause());
            }
        }
    }

    @Override
    public ConnectorObject getObject(
            final ObjectClass objectClass,
            final Attribute connObjectKey,
            final OperationOptions options) {

        Future<ConnectorObject> future = null;

        if (connInstance.getCapabilities().contains(ConnectorCapability.SEARCH)) {
            future = asyncFacade.getObject(connector, objectClass, connObjectKey, options);
        } else {
            LOG.info("Search was attempted, although the connector only has these capabilities: {}. No action.",
                    connInstance.getCapabilities());
        }

        try {
            return future == null ? null : future.get(connInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
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

    @Override
    public SearchResult search(
            final ObjectClass objectClass,
            final Filter filter,
            final ResultsHandler handler,
            final OperationOptions options) {

        SearchResult result = null;

        if (connInstance.getCapabilities().contains(ConnectorCapability.SEARCH)) {
            if (options.getPageSize() == null && options.getPagedResultsCookie() == null) {
                OperationOptionsBuilder builder = new OperationOptionsBuilder(options).
                        setPageSize(DEFAULT_PAGE_SIZE).setPagedResultsOffset(-1);

                final String[] cookies = new String[] { null };
                do {
                    if (cookies[0] != null) {
                        builder.setPagedResultsCookie(cookies[0]);
                    }

                    result = connector.search(objectClass, filter, new SearchResultsHandler() {

                        @Override
                        public void handleResult(final SearchResult result) {
                            if (handler instanceof SearchResultsHandler) {
                                SearchResultsHandler.class.cast(handler).handleResult(result);
                            }
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
    public SearchResult search(
            final ObjectClass objectClass,
            final Filter filter,
            final ResultsHandler handler,
            final int pageSize,
            final String pagedResultsCookie,
            final List<OrderByClause> orderBy,
            final OperationOptions options) {

        OperationOptionsBuilder builder = new OperationOptionsBuilder().setPageSize(pageSize).setPagedResultsOffset(-1);
        if (pagedResultsCookie != null) {
            builder.setPagedResultsCookie(pagedResultsCookie);
        }
        builder.setSortKeys(orderBy.stream().map(clause
                -> new SortKey(clause.getField(), clause.getDirection() == OrderByClause.Direction.ASC)).
                collect(Collectors.toList()));

        builder.setAttributesToGet(options.getAttributesToGet());

        return search(objectClass, filter, handler, builder.build());
    }

    @Override
    public ConnInstance getConnInstance() {
        return connInstance;
    }

    private Object getPropertyValue(final String propType, final List<?> values) {
        Object value = null;

        try {
            Class<?> propertySchemaClass = ClassUtils.forName(propType, ClassUtils.getDefaultClassLoader());

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
                value = values.toArray(new String[] {});
            } else {
                value = values.get(0) == null ? null : values.get(0).toString();
            }
        } catch (Exception e) {
            LOG.error("Invalid ConnConfProperty specified: {} {}", propType, values, e);
        }

        return value;
    }

    @Override
    public String toString() {
        return "ConnectorFacadeProxy{"
                + "connector=" + connector + "\n" + "capabitilies=" + connInstance.getCapabilities() + '}';
    }
}
