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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.ConnectorCapability;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.provisioning.api.ConnIdBundleManager;
import org.apache.syncope.core.provisioning.api.ConnPoolConfUtils;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.TimeoutException;
import org.apache.syncope.core.misc.spring.ApplicationContextProvider;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.identityconnectors.common.security.GuardedByteArray;
import org.identityconnectors.common.security.GuardedString;
import org.identityconnectors.framework.api.APIConfiguration;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConnectorFacade;
import org.identityconnectors.framework.api.ConnectorFacadeFactory;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.OperationOptionsBuilder;
import org.identityconnectors.framework.common.objects.OperationalAttributes;
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
    private final ConnInstance activeConnInstance;

    @Autowired
    private AsyncConnectorFacade asyncFacade;

    /**
     * Use the passed connector instance to build a ConnectorFacade that will be used to make all wrapped calls.
     *
     * @param connInstance the connector instance configuration
     * @see ConnectorInfo
     * @see APIConfiguration
     * @see ConfigurationProperties
     * @see ConnectorFacade
     */
    public ConnectorFacadeProxy(final ConnInstance connInstance) {
        this.activeConnInstance = connInstance;

        ConnIdBundleManager connIdBundleManager =
                ApplicationContextProvider.getBeanFactory().getBean(ConnIdBundleManager.class);
        ConnectorInfo info = connIdBundleManager.getConnectorInfo(connInstance);

        // create default configuration
        APIConfiguration apiConfig = info.createDefaultAPIConfiguration();
        // enable filtered results handler in validation mode
        apiConfig.getResultsHandlerConfiguration().setFilteredResultsHandlerInValidationMode(true);

        // set connector configuration according to conninstance's
        ConfigurationProperties properties = apiConfig.getConfigurationProperties();
        for (ConnConfProperty property : connInstance.getConfiguration()) {
            if (property.getValues() != null && !property.getValues().isEmpty()) {
                properties.setPropertyValue(property.getSchema().getName(),
                        getPropertyValue(property.getSchema().getType(), property.getValues()));
            }
        }

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

        if (activeConnInstance.getCapabilities().contains(ConnectorCapability.AUTHENTICATE)) {
            Future<Uid> future = asyncFacade.authenticate(
                    connector, username, new GuardedString(password.toCharArray()), options);
            try {
                result = future.get(activeConnInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
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
                    activeConnInstance.getCapabilities());
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

        if (activeConnInstance.getCapabilities().contains(ConnectorCapability.CREATE)) {
            propagationAttempted[0] = true;

            Future<Uid> future = asyncFacade.create(connector, objectClass, attrs, options);
            try {
                result = future.get(activeConnInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
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

    @Override
    public Uid update(
            final ObjectClass objectClass,
            final Uid uid,
            final Set<Attribute> attrs,
            final OperationOptions options,
            final Boolean[] propagationAttempted) {

        Uid result = null;

        if (activeConnInstance.getCapabilities().contains(ConnectorCapability.UPDATE)) {
            propagationAttempted[0] = true;

            Future<Uid> future = asyncFacade.update(connector, objectClass, uid, attrs, options);

            try {
                result = future.get(activeConnInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
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

    @Override
    public void delete(
            final ObjectClass objectClass,
            final Uid uid,
            final OperationOptions options,
            final Boolean[] propagationAttempted) {

        if (activeConnInstance.getCapabilities().contains(ConnectorCapability.DELETE)) {
            propagationAttempted[0] = true;

            Future<Uid> future = asyncFacade.delete(connector, objectClass, uid, options);

            try {
                future.get(activeConnInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
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

    @Override
    public SyncToken getLatestSyncToken(final ObjectClass objectClass) {
        SyncToken result = null;

        if (activeConnInstance.getCapabilities().contains(ConnectorCapability.SYNC)) {
            Future<SyncToken> future = asyncFacade.getLatestSyncToken(connector, objectClass);

            try {
                result = future.get(activeConnInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
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

    @Override
    public ConnectorObject getObject(final ObjectClass objectClass, final Uid uid, final OperationOptions options) {
        return getObject(null, objectClass, uid, options);
    }

    @Override
    public ConnectorObject getObject(
            final ResourceOperation operationType,
            final ObjectClass objectClass,
            final Uid uid,
            final OperationOptions options) {

        boolean hasCapablities = false;

        if (activeConnInstance.getCapabilities().contains(ConnectorCapability.SEARCH)) {
            if (operationType == null) {
                hasCapablities = true;
            } else {
                switch (operationType) {
                    case CREATE:
                        hasCapablities = activeConnInstance.getCapabilities().contains(ConnectorCapability.CREATE);
                        break;

                    case UPDATE:
                        hasCapablities = activeConnInstance.getCapabilities().contains(ConnectorCapability.UPDATE);
                        break;

                    default:
                        hasCapablities = true;
                }
            }
        }

        Future<ConnectorObject> future = null;
        if (hasCapablities) {
            future = asyncFacade.getObject(connector, objectClass, uid, options);
        } else {
            LOG.info("Search was attempted, although the connector only has these capabilities: {}. No action.",
                    activeConnInstance.getCapabilities());
        }

        try {
            return future == null ? null : future.get(activeConnInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
        } catch (java.util.concurrent.TimeoutException e) {
            if (future != null) {
                future.cancel(true);
            }
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
    public void getAllObjects(
            final ObjectClass objectClass, final SyncResultsHandler handler, final OperationOptions options) {

        search(objectClass, null, new ResultsHandler() {

            @Override
            public boolean handle(final ConnectorObject obj) {
                return handler.handle(new SyncDeltaBuilder().
                        setObject(obj).
                        setUid(obj.getUid()).
                        setDeltaType(SyncDeltaType.CREATE_OR_UPDATE).
                        setToken(new SyncToken("")).
                        build());
            }
        }, options);
    }

    @Override
    public Attribute getObjectAttribute(final ObjectClass objectClass, final Uid uid, final OperationOptions options,
            final String attributeName) {

        Future<Attribute> future = asyncFacade.getObjectAttribute(
                connector, objectClass, uid, options, attributeName);
        try {
            return future.get(activeConnInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
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
    public Set<Attribute> getObjectAttributes(final ObjectClass objectClass, final Uid uid,
            final OperationOptions options) {

        Future<Set<Attribute>> future = asyncFacade.getObjectAttributes(connector, objectClass, uid, options);
        try {
            return future.get(activeConnInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
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
    public Set<String> getSchemaNames(final boolean includeSpecial) {
        Future<Set<String>> future = asyncFacade.getSchemaNames(connector, includeSpecial);
        try {
            return future.get(activeConnInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
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
    public Set<ObjectClass> getSupportedObjectClasses() {
        Future<Set<ObjectClass>> future = asyncFacade.getSupportedObjectClasses(connector);
        try {
            return future.get(activeConnInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
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
            future.get(activeConnInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
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
            future.get(activeConnInstance.getConnRequestTimeout(), TimeUnit.SECONDS);
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
    public void search(
            final ObjectClass objectClass,
            final Filter filter,
            final ResultsHandler handler,
            final OperationOptions options) {

        if (activeConnInstance.getCapabilities().contains(ConnectorCapability.SEARCH)) {
            if (options.getPageSize() == null && options.getPagedResultsCookie() == null) {
                OperationOptionsBuilder builder = new OperationOptionsBuilder(options);
                builder.setPageSize(DEFAULT_PAGE_SIZE);

                final String[] cookies = new String[] { null };
                do {
                    if (cookies[0] != null) {
                        builder.setPagedResultsCookie(cookies[0]);
                    }

                    connector.search(objectClass, filter, new SearchResultsHandler() {

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
                connector.search(objectClass, filter, handler, options);
            }
        } else {
            LOG.info("Search was attempted, although the connector only has these capabilities: {}. No action.",
                    activeConnInstance.getCapabilities());
        }
    }

    @Override
    public void search(
            final ObjectClass objectClass,
            final Filter filter,
            final ResultsHandler handler,
            final int pageSize,
            final String pagedResultsCookie,
            final List<OrderByClause> orderBy,
            final Iterator<? extends MappingItem> mapItems) {

        OperationOptionsBuilder builder = new OperationOptionsBuilder().setPageSize(pageSize);
        if (pagedResultsCookie != null) {
            builder.setPagedResultsCookie(pagedResultsCookie);
        }
        builder.setSortKeys(CollectionUtils.collect(orderBy, new Transformer<OrderByClause, SortKey>() {

            @Override
            public SortKey transform(final OrderByClause clause) {
                return new SortKey(clause.getField(), clause.getDirection() == OrderByClause.Direction.ASC);
            }
        }, new ArrayList<SortKey>(orderBy.size())));

        builder.setAttributesToGet(getOperationOptions(mapItems).getAttributesToGet());

        search(objectClass, filter, handler, builder.build());
    }

    @Override
    public ConnInstance getActiveConnInstance() {
        return activeConnInstance;
    }

    @Override
    public OperationOptions getOperationOptions(final Iterator<? extends MappingItem> mapItems) {
        // -------------------------------------
        // Ask just for mapped attributes
        // -------------------------------------
        OperationOptionsBuilder builder = new OperationOptionsBuilder();

        Set<String> attrsToGet = new HashSet<>();
        attrsToGet.add(Name.NAME);
        attrsToGet.add(Uid.NAME);
        attrsToGet.add(OperationalAttributes.ENABLE_NAME);

        while (mapItems.hasNext()) {
            MappingItem mapItem = mapItems.next();
            if (mapItem.getPurpose() != MappingPurpose.NONE) {
                attrsToGet.add(mapItem.getExtAttrName());
            }
        }

        builder.setAttributesToGet(attrsToGet);
        // -------------------------------------

        return builder.build();
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
                + "connector=" + connector + "\n" + "capabitilies=" + activeConnInstance.getCapabilities() + '}';
    }
}
