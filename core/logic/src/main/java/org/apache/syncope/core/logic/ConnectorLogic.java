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
package org.apache.syncope.core.logic;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.PredicateUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ConnBundleTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.Entitlement;
import org.apache.syncope.core.persistence.api.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.provisioning.api.ConnIdBundleManager;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.ConnectorFactory;
import org.apache.syncope.core.provisioning.api.data.ConnInstanceDataBinder;
import org.identityconnectors.common.l10n.CurrentLocale;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConnectorInfo;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.identityconnectors.framework.api.ConnectorKey;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ConnectorLogic extends AbstractTransactionalLogic<ConnInstanceTO> {

    @Autowired
    private ConnIdBundleManager connIdBundleManager;

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Autowired
    private ConnInstanceDataBinder binder;

    @Autowired
    private ConnectorFactory connFactory;

    @PreAuthorize("hasRole('" + Entitlement.CONNECTOR_CREATE + "')")
    public ConnInstanceTO create(final ConnInstanceTO connInstanceTO) {
        ConnInstance connInstance = binder.getConnInstance(connInstanceTO);
        try {
            connInstance = connInstanceDAO.save(connInstance);
        } catch (SyncopeClientException e) {
            throw e;
        } catch (Exception e) {
            SyncopeClientException ex = SyncopeClientException.build(ClientExceptionType.InvalidConnInstance);
            ex.getElements().add(e.getMessage());
            throw ex;
        }

        return binder.getConnInstanceTO(connInstance);
    }

    @PreAuthorize("hasRole('" + Entitlement.CONNECTOR_UPDATE + "')")
    public ConnInstanceTO update(final ConnInstanceTO connInstanceTO) {
        ConnInstance connInstance = binder.updateConnInstance(connInstanceTO.getKey(), connInstanceTO);
        try {
            connInstance = connInstanceDAO.save(connInstance);
        } catch (SyncopeClientException e) {
            throw e;
        } catch (Exception e) {
            SyncopeClientException ex = SyncopeClientException.build(ClientExceptionType.InvalidConnInstance);
            ex.getElements().add(e.getMessage());
            throw ex;
        }

        return binder.getConnInstanceTO(connInstance);
    }

    @PreAuthorize("hasRole('" + Entitlement.CONNECTOR_DELETE + "')")
    public ConnInstanceTO delete(final Long connInstanceKey) {
        ConnInstance connInstance = connInstanceDAO.find(connInstanceKey);
        if (connInstance == null) {
            throw new NotFoundException("Connector '" + connInstanceKey + "'");
        }

        if (!connInstance.getResources().isEmpty()) {
            SyncopeClientException associatedResources = SyncopeClientException.build(
                    ClientExceptionType.AssociatedResources);
            for (ExternalResource resource : connInstance.getResources()) {
                associatedResources.getElements().add(resource.getKey());
            }
            throw associatedResources;
        }

        ConnInstanceTO connToDelete = binder.getConnInstanceTO(connInstance);

        connInstanceDAO.delete(connInstanceKey);

        return connToDelete;
    }

    @PreAuthorize("hasRole('" + Entitlement.CONNECTOR_LIST + "')")
    @Transactional(readOnly = true)
    public List<ConnInstanceTO> list(final String lang) {
        CurrentLocale.set(StringUtils.isBlank(lang) ? Locale.ENGLISH : new Locale(lang));

        List<ConnInstanceTO> result = CollectionUtils.collect(connInstanceDAO.findAll().iterator(),
                new Transformer<ConnInstance, ConnInstanceTO>() {

                    @Override
                    public ConnInstanceTO transform(final ConnInstance connInstance) {
                        ConnInstanceTO result = null;
                        try {
                            result = binder.getConnInstanceTO(connInstance);
                        } catch (NotFoundException e) {
                            LOG.error("Connector '{}#{}' not found",
                                    connInstance.getBundleName(), connInstance.getVersion());
                        }

                        return result;
                    }
                }, new ArrayList<ConnInstanceTO>());
        CollectionUtils.filter(result, PredicateUtils.notNullPredicate());
        return result;
    }

    @PreAuthorize("hasRole('" + Entitlement.CONNECTOR_READ + "')")
    @Transactional(readOnly = true)
    public ConnInstanceTO read(final Long connInstanceKey, final String lang) {
        CurrentLocale.set(StringUtils.isBlank(lang) ? Locale.ENGLISH : new Locale(lang));

        ConnInstance connInstance = connInstanceDAO.find(connInstanceKey);
        if (connInstance == null) {
            throw new NotFoundException("Connector '" + connInstanceKey + "'");
        }

        return binder.getConnInstanceTO(connInstance);
    }

    @PreAuthorize("hasRole('" + Entitlement.CONNECTOR_READ + "')")
    @Transactional(readOnly = true)
    public List<ConnBundleTO> getBundles(final String lang) {
        if (StringUtils.isBlank(lang)) {
            CurrentLocale.set(Locale.ENGLISH);
        } else {
            CurrentLocale.set(new Locale(lang));
        }

        List<ConnBundleTO> connectorBundleTOs = new ArrayList<>();
        for (Map.Entry<URI, ConnectorInfoManager> entry : connIdBundleManager.getConnInfoManagers().entrySet()) {
            for (ConnectorInfo bundle : entry.getValue().getConnectorInfos()) {
                ConnBundleTO connBundleTO = new ConnBundleTO();
                connBundleTO.setDisplayName(bundle.getConnectorDisplayName());

                connBundleTO.setLocation(entry.getKey().toString());

                ConnectorKey key = bundle.getConnectorKey();
                connBundleTO.setBundleName(key.getBundleName());
                connBundleTO.setConnectorName(key.getConnectorName());
                connBundleTO.setVersion(key.getBundleVersion());

                ConfigurationProperties properties = connIdBundleManager.getConfigurationProperties(bundle);

                for (String propName : properties.getPropertyNames()) {
                    connBundleTO.getProperties().add(binder.buildConnConfPropSchema(properties.getProperty(propName)));
                }

                connectorBundleTOs.add(connBundleTO);
            }
        }

        return connectorBundleTOs;
    }

    @PreAuthorize("hasRole('" + Entitlement.CONNECTOR_READ + "')")
    @Transactional(readOnly = true)
    public List<String> getSchemaNames(final ConnInstanceTO connInstanceTO, final boolean includeSpecial) {
        ConnInstance connInstance = connInstanceDAO.find(connInstanceTO.getKey());
        if (connInstance == null) {
            throw new NotFoundException("Connector '" + connInstanceTO.getKey() + "'");
        }

        // consider the possibility to receive overridden properties only
        Set<ConnConfProperty> conf =
                binder.mergeConnConfProperties(connInstanceTO.getConfiguration(), connInstance.getConfiguration());

        // We cannot use Spring bean because this method could be used during resource definition or modification:
        // bean couldn't exist or couldn't be updated.
        // This is the reason why we should take a "not mature" connector facade proxy to ask for schema names.
        return new ArrayList<>(connFactory.createConnector(connInstance, conf).getSchemaNames(includeSpecial));
    }

    @PreAuthorize("hasRole('" + Entitlement.CONNECTOR_READ + "')")
    @Transactional(readOnly = true)
    public List<String> getSupportedObjectClasses(final ConnInstanceTO connInstanceTO) {
        ConnInstance connInstance = connInstanceDAO.find(connInstanceTO.getKey());
        if (connInstance == null) {
            throw new NotFoundException("Connector '" + connInstanceTO.getKey() + "'");
        }

        // consider the possibility to receive overridden properties only
        Set<ConnConfProperty> conf =
                binder.mergeConnConfProperties(connInstanceTO.getConfiguration(), connInstance.getConfiguration());

        // We cannot use Spring bean because this method could be used during resource definition or modification:
        // bean couldn't exist or couldn't be updated.
        // This is the reason why we should take a "not mature" connector facade proxy to ask for object classes.
        Set<ObjectClass> objectClasses = connFactory.createConnector(connInstance, conf).getSupportedObjectClasses();

        List<String> result = new ArrayList<>(objectClasses.size());
        for (ObjectClass objectClass : objectClasses) {
            result.add(objectClass.getObjectClassValue());
        }

        return result;
    }

    @PreAuthorize("hasRole('" + Entitlement.CONNECTOR_READ + "')")
    @Transactional(readOnly = true)
    public List<ConnConfProperty> getConfigurationProperties(final Long connInstanceKey) {

        ConnInstance connInstance = connInstanceDAO.find(connInstanceKey);
        if (connInstance == null) {
            throw new NotFoundException("Connector '" + connInstanceKey + "'");
        }

        return new ArrayList<>(connInstance.getConfiguration());
    }

    @PreAuthorize("hasRole('" + Entitlement.CONNECTOR_READ + "')")
    @Transactional(readOnly = true)
    public boolean check(final ConnInstanceTO connInstanceTO) {
        final Connector connector = connFactory.createConnector(
                binder.getConnInstance(connInstanceTO), connInstanceTO.getConfiguration());

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

    @PreAuthorize("hasRole('" + Entitlement.CONNECTOR_READ + "')")
    @Transactional(readOnly = true)
    public ConnInstanceTO readByResource(final String resourceName, final String lang) {
        CurrentLocale.set(StringUtils.isBlank(lang) ? Locale.ENGLISH : new Locale(lang));

        ExternalResource resource = resourceDAO.find(resourceName);
        if (resource == null) {
            throw new NotFoundException("Resource '" + resourceName + "'");
        }
        return binder.getConnInstanceTO(connFactory.getConnector(resource).getActiveConnInstance());
    }

    @PreAuthorize("hasRole('" + Entitlement.CONNECTOR_RELOAD + "')")
    @Transactional(readOnly = true)
    public void reload() {
        connFactory.unload();
        connFactory.load();
    }

    @Override
    protected ConnInstanceTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        Long key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof Long) {
                    key = (Long) args[i];
                } else if (args[i] instanceof ConnInstanceTO) {
                    key = ((ConnInstanceTO) args[i]).getKey();
                }
            }
        }

        if ((key != null) && !key.equals(0L)) {
            try {
                return binder.getConnInstanceTO(connInstanceDAO.find(key));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
