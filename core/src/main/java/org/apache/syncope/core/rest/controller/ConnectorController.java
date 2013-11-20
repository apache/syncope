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
package org.apache.syncope.core.rest.controller;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.to.BulkAction;
import org.apache.syncope.common.to.BulkActionRes;
import org.apache.syncope.common.to.ConnBundleTO;
import org.apache.syncope.common.to.ConnInstanceTO;
import org.apache.syncope.common.types.ConnConfPropSchema;
import org.apache.syncope.common.types.ConnConfProperty;
import org.apache.syncope.common.types.ClientExceptionType;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.apache.syncope.core.persistence.beans.ConnInstance;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.persistence.dao.ResourceDAO;
import org.apache.syncope.core.persistence.validation.entity.InvalidEntityException;
import org.apache.syncope.core.propagation.Connector;
import org.apache.syncope.core.propagation.ConnectorFactory;
import org.apache.syncope.core.rest.data.ConnInstanceDataBinder;
import org.apache.syncope.core.util.ConnIdBundleManager;
import org.identityconnectors.common.l10n.CurrentLocale;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorKey;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.impl.api.ConfigurationPropertyImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ConnectorController extends AbstractTransactionalController<ConnInstanceTO> {

    @Autowired
    private ResourceDAO resourceDAO;

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Autowired
    private ConnInstanceDataBinder binder;

    @Autowired
    private ConnectorFactory connFactory;

    @PreAuthorize("hasRole('CONNECTOR_CREATE')")
    public ConnInstanceTO create(final ConnInstanceTO connInstanceTO) {
        LOG.debug("ConnInstance create called with configuration {}", connInstanceTO);

        ConnInstance connInstance = binder.getConnInstance(connInstanceTO);
        try {
            connInstance = connInstanceDAO.save(connInstance);
        } catch (InvalidEntityException e) {
            SyncopeClientException invalidConnInstance = SyncopeClientException.build(
                    ClientExceptionType.InvalidConnInstance);
            invalidConnInstance.getElements().add(e.getMessage());
            throw invalidConnInstance;
        }

        return binder.getConnInstanceTO(connInstance);
    }

    @PreAuthorize("hasRole('CONNECTOR_UPDATE')")
    public ConnInstanceTO update(final ConnInstanceTO connInstanceTO) {
        LOG.debug("Connector update called with configuration {}", connInstanceTO);

        ConnInstance connInstance = binder.updateConnInstance(connInstanceTO.getId(), connInstanceTO);
        try {
            connInstance = connInstanceDAO.save(connInstance);
        } catch (InvalidEntityException e) {
            SyncopeClientException invalidConnInstance = SyncopeClientException.build(
                    ClientExceptionType.InvalidConnInstance);
            invalidConnInstance.getElements().add(e.getMessage());
            throw invalidConnInstance;
        }

        return binder.getConnInstanceTO(connInstance);
    }

    @PreAuthorize("hasRole('CONNECTOR_DELETE')")
    public ConnInstanceTO delete(final Long connInstanceId) {
        ConnInstance connInstance = connInstanceDAO.find(connInstanceId);
        if (connInstance == null) {
            throw new NotFoundException("Connector '" + connInstanceId + "'");
        }

        if (!connInstance.getResources().isEmpty()) {
            SyncopeClientException associatedResources = SyncopeClientException.build(
                    ClientExceptionType.AssociatedResources);
            for (ExternalResource resource : connInstance.getResources()) {
                associatedResources.getElements().add(resource.getName());
            }
            throw associatedResources;
        }

        ConnInstanceTO connToDelete = binder.getConnInstanceTO(connInstance);

        connInstanceDAO.delete(connInstanceId);

        return connToDelete;
    }

    @PreAuthorize("hasRole('CONNECTOR_LIST')")
    @Transactional(readOnly = true)
    public List<ConnInstanceTO> list(final String lang) {
        if (StringUtils.isBlank(lang)) {
            CurrentLocale.set(Locale.ENGLISH);
        } else {
            CurrentLocale.set(new Locale(lang));
        }

        List<ConnInstance> connInstances = connInstanceDAO.findAll();

        final List<ConnInstanceTO> connInstanceTOs = new ArrayList<ConnInstanceTO>();

        for (ConnInstance connector : connInstances) {
            try {
                connInstanceTOs.add(binder.getConnInstanceTO(connector));
            } catch (NotFoundException e) {
                LOG.error("Connector '{}#{}' not found", connector.getBundleName(), connector.getVersion());
            }
        }

        return connInstanceTOs;
    }

    @PreAuthorize("hasRole('CONNECTOR_READ')")
    @Transactional(readOnly = true)
    public ConnInstanceTO read(final Long connInstanceId) {
        ConnInstance connInstance = connInstanceDAO.find(connInstanceId);
        if (connInstance == null) {
            throw new NotFoundException("Connector '" + connInstanceId + "'");
        }

        return binder.getConnInstanceTO(connInstance);
    }

    @PreAuthorize("hasRole('CONNECTOR_READ')")
    @Transactional(readOnly = true)
    public List<ConnBundleTO> getBundles(final String lang) {
        if (StringUtils.isBlank(lang)) {
            CurrentLocale.set(Locale.ENGLISH);
        } else {
            CurrentLocale.set(new Locale(lang));
        }

        List<ConnBundleTO> connectorBundleTOs = new ArrayList<ConnBundleTO>();
        for (Map.Entry<String, List<ConnectorInfo>> entry : ConnIdBundleManager.getConnectorInfos().entrySet()) {
            for (ConnectorInfo bundle : entry.getValue()) {
                ConnBundleTO connBundleTO = new ConnBundleTO();
                connBundleTO.setDisplayName(bundle.getConnectorDisplayName());

                connBundleTO.setLocation(entry.getKey());

                ConnectorKey key = bundle.getConnectorKey();
                connBundleTO.setBundleName(key.getBundleName());
                connBundleTO.setConnectorName(key.getConnectorName());
                connBundleTO.setVersion(key.getBundleVersion());

                ConfigurationProperties properties = ConnIdBundleManager.getConfigurationProperties(bundle);

                for (String propName : properties.getPropertyNames()) {
                    ConnConfPropSchema connConfPropSchema = new ConnConfPropSchema();

                    ConfigurationPropertyImpl configurationProperty = (ConfigurationPropertyImpl) properties.
                            getProperty(propName);

                    connConfPropSchema.setName(configurationProperty.getName());
                    connConfPropSchema.setDisplayName(configurationProperty.getDisplayName(propName));
                    connConfPropSchema.setHelpMessage(configurationProperty.getHelpMessage(propName));
                    connConfPropSchema.setRequired(configurationProperty.isRequired());
                    connConfPropSchema.setType(configurationProperty.getType().getName());
                    connConfPropSchema.setOrder(configurationProperty.getOrder());
                    connConfPropSchema.setConfidential(configurationProperty.isConfidential());

                    connBundleTO.getProperties().add(connConfPropSchema);
                }

                LOG.debug("Connector bundle: {}", connBundleTO);

                connectorBundleTOs.add(connBundleTO);
            }
        }

        return connectorBundleTOs;
    }

    @PreAuthorize("hasRole('CONNECTOR_READ')")
    @Transactional(readOnly = true)
    public List<String> getSchemaNames(final ConnInstanceTO connInstanceTO, final boolean includeSpecial) {
        final ConnInstance connInstance = connInstanceDAO.find(connInstanceTO.getId());
        if (connInstance == null) {
            throw new NotFoundException("Connector '" + connInstanceTO.getId() + "'");
        }

        // consider the possibility to receive overridden properties only
        final Set<ConnConfProperty> conf = binder.mergeConnConfProperties(connInstanceTO.getConfiguration(),
                connInstance.getConfiguration());

        // We cannot use Spring bean because this method could be used during resource definition or modification:
        // bean couldn't exist or couldn't be updated.
        // This is the reason why we should take a "not mature" connector facade proxy to ask for schema names.
        final List<String> result = new ArrayList<String>(connFactory.createConnector(connInstance, conf).
                getSchemaNames(includeSpecial));

        return result;
    }

    @PreAuthorize("hasRole('CONNECTOR_READ')")
    @Transactional(readOnly = true)
    public List<String> getSupportedObjectClasses(final ConnInstanceTO connInstanceTO) {
        final ConnInstance connInstance = connInstanceDAO.find(connInstanceTO.getId());
        if (connInstance == null) {
            throw new NotFoundException("Connector '" + connInstanceTO.getId() + "'");
        }

        // consider the possibility to receive overridden properties only
        final Set<ConnConfProperty> conf = binder.mergeConnConfProperties(connInstanceTO.getConfiguration(),
                connInstance.getConfiguration());

        // We cannot use Spring bean because this method could be used during resource definition or modification:
        // bean couldn't exist or couldn't be updated.
        // This is the reason why we should take a "not mature" connector facade proxy to ask for object classes.
        Set<ObjectClass> objectClasses = connFactory.createConnector(connInstance, conf).getSupportedObjectClasses();

        List<String> result = new ArrayList<String>(objectClasses.size());
        for (ObjectClass objectClass : objectClasses) {
            result.add(objectClass.getObjectClassValue());
        }

        return result;
    }

    @PreAuthorize("hasRole('CONNECTOR_READ')")
    @Transactional(readOnly = true)
    public List<ConnConfProperty> getConfigurationProperties(final Long connInstanceId) {

        final ConnInstance connInstance = connInstanceDAO.find(connInstanceId);
        if (connInstance == null) {
            throw new NotFoundException("Connector '" + connInstanceId + "'");
        }

        return new ArrayList<ConnConfProperty>(connInstance.getConfiguration());
    }

    @PreAuthorize("hasRole('CONNECTOR_READ')")
    @Transactional(readOnly = true)
    public boolean check(final ConnInstanceTO connInstanceTO) {
        final Connector connector = connFactory.createConnector(binder.getConnInstance(connInstanceTO), connInstanceTO.
                getConfiguration());

        boolean result;
        try {
            connector.test();
            result = true;
        } catch (Exception ex) {
            LOG.error("Test connection failure {}", ex);
            result = false;
        }

        return result;
    }

    @PreAuthorize("hasRole('CONNECTOR_READ')")
    @Transactional(readOnly = true)
    public ConnInstanceTO readByResource(final String resourceName) {
        ExternalResource resource = resourceDAO.find(resourceName);
        if (resource == null) {
            throw new NotFoundException("Resource '" + resourceName + "'");
        }
        return binder.getConnInstanceTO(connFactory.getConnector(resource).getActiveConnInstance());
    }

    @PreAuthorize("hasRole('CONNECTOR_RELOAD')")
    @Transactional(readOnly = true)
    public void reload() {
        connFactory.unload();
        connFactory.load();
    }

    @PreAuthorize("hasRole('CONNECTOR_DELETE') and #bulkAction.operation == #bulkAction.operation.DELETE")
    public BulkActionRes bulk(final BulkAction bulkAction) {
        LOG.debug("Bulk '{}' called on '{}'", bulkAction.getOperation(), bulkAction.getTargets());

        BulkActionRes res = new BulkActionRes();

        if (bulkAction.getOperation() == BulkAction.Type.DELETE) {
            for (String id : bulkAction.getTargets()) {
                try {
                    res.add(delete(Long.valueOf(id)).getId(), BulkActionRes.Status.SUCCESS);
                } catch (Exception e) {
                    LOG.error("Error performing delete for connector {}", id, e);
                    res.add(id, BulkActionRes.Status.FAILURE);
                }
            }
        }

        return res;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected ConnInstanceTO resolveReference(final Method method, final Object... args) throws
            UnresolvedReferenceException {
        Long id = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; id == null && i < args.length; i++) {
                if (args[i] instanceof Long) {
                    id = (Long) args[i];
                } else if (args[i] instanceof ConnInstanceTO) {
                    id = ((ConnInstanceTO) args[i]).getId();
                }
            }
        }

        if (id != null) {
            try {
                return binder.getConnInstanceTO(connInstanceDAO.find(id));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
