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
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ConnBundleTO;
import org.apache.syncope.common.lib.to.ConnIdObjectClassTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.persistence.api.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.provisioning.api.ConnIdBundleManager;
import org.apache.syncope.core.provisioning.api.ConnectorFactory;
import org.apache.syncope.core.provisioning.api.data.ConnInstanceDataBinder;
import org.apache.syncope.core.provisioning.api.utils.RealmUtils;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.identityconnectors.common.l10n.CurrentLocale;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConnectorInfoManager;
import org.identityconnectors.framework.api.ConnectorKey;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
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

    protected void securityChecks(final Set<String> effectiveRealms, final String realm, final String key) {
        boolean authorized = effectiveRealms.stream().anyMatch(ownedRealm -> realm.startsWith(ownedRealm));
        if (!authorized) {
            throw new DelegatedAdministrationException(realm, ConnInstance.class.getSimpleName(), key);
        }
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.CONNECTOR_CREATE + "')")
    public ConnInstanceTO create(final ConnInstanceTO connInstanceTO) {
        if (connInstanceTO.getAdminRealm() == null) {
            throw SyncopeClientException.build(ClientExceptionType.InvalidRealm);
        }

        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(StandardEntitlement.CONNECTOR_CREATE),
                connInstanceTO.getAdminRealm());
        securityChecks(effectiveRealms, connInstanceTO.getAdminRealm(), null);

        return binder.getConnInstanceTO(connInstanceDAO.save(binder.getConnInstance(connInstanceTO)));
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.CONNECTOR_UPDATE + "')")
    public ConnInstanceTO update(final ConnInstanceTO connInstanceTO) {
        if (connInstanceTO.getAdminRealm() == null) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidConnInstance);
            sce.getElements().add("Invalid or null realm specified: " + connInstanceTO.getAdminRealm());
            throw sce;
        }

        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(StandardEntitlement.CONNECTOR_UPDATE),
                connInstanceTO.getAdminRealm());
        securityChecks(effectiveRealms, connInstanceTO.getAdminRealm(), connInstanceTO.getKey());

        return binder.getConnInstanceTO(binder.update(connInstanceTO));
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.CONNECTOR_DELETE + "')")
    public ConnInstanceTO delete(final String key) {
        ConnInstance connInstance = connInstanceDAO.authFind(key);
        if (connInstance == null) {
            throw new NotFoundException("Connector '" + key + "'");
        }

        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(StandardEntitlement.CONNECTOR_DELETE),
                connInstance.getAdminRealm().getFullPath());
        securityChecks(effectiveRealms, connInstance.getAdminRealm().getFullPath(), connInstance.getKey());

        if (!connInstance.getResources().isEmpty()) {
            SyncopeClientException associatedResources = SyncopeClientException.build(
                    ClientExceptionType.AssociatedResources);
            connInstance.getResources().forEach(resource -> {
                associatedResources.getElements().add(resource.getKey());
            });
            throw associatedResources;
        }

        ConnInstanceTO deleted = binder.getConnInstanceTO(connInstance);
        connInstanceDAO.delete(key);
        return deleted;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.CONNECTOR_LIST + "')")
    @Transactional(readOnly = true)
    public List<ConnInstanceTO> list(final String lang) {
        CurrentLocale.set(StringUtils.isBlank(lang) ? Locale.ENGLISH : new Locale(lang));

        return connInstanceDAO.findAll().stream().
                filter(connInstance -> connInstance != null).
                map(connInstance -> {
                    ConnInstanceTO result = null;
                    try {
                        result = binder.getConnInstanceTO(connInstance);
                    } catch (NotFoundException e) {
                        LOG.
                                error("Connector '{}#{}' not found", connInstance.getBundleName(), connInstance.
                                        getVersion());
                    }

                    return result;
                }).collect(Collectors.toList());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.CONNECTOR_READ + "')")
    @Transactional(readOnly = true)
    public ConnInstanceTO read(final String key, final String lang) {
        CurrentLocale.set(StringUtils.isBlank(lang) ? Locale.ENGLISH : new Locale(lang));

        ConnInstance connInstance = connInstanceDAO.authFind(key);
        if (connInstance == null) {
            throw new NotFoundException("Connector '" + key + "'");
        }

        return binder.getConnInstanceTO(connInstance);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.CONNECTOR_READ + "')")
    @Transactional(readOnly = true)
    public List<ConnBundleTO> getBundles(final String lang) {
        if (StringUtils.isBlank(lang)) {
            CurrentLocale.set(Locale.ENGLISH);
        } else {
            CurrentLocale.set(new Locale(lang));
        }

        List<ConnBundleTO> connectorBundleTOs = new ArrayList<>();
        for (Map.Entry<URI, ConnectorInfoManager> entry : connIdBundleManager.getConnInfoManagers().entrySet()) {
            entry.getValue().getConnectorInfos().stream().map(bundle -> {
                ConnBundleTO connBundleTO = new ConnBundleTO();
                connBundleTO.setDisplayName(bundle.getConnectorDisplayName());
                connBundleTO.setLocation(entry.getKey().toString());
                ConnectorKey key = bundle.getConnectorKey();
                connBundleTO.setBundleName(key.getBundleName());
                connBundleTO.setConnectorName(key.getConnectorName());
                connBundleTO.setVersion(key.getBundleVersion());
                ConfigurationProperties properties = connIdBundleManager.getConfigurationProperties(bundle);
                for (String propName : properties.getPropertyNames()) {
                    connBundleTO.getProperties().add(binder.build(properties.getProperty(propName)));
                }
                return connBundleTO;
            }).forEachOrdered(connBundleTO -> {
                connectorBundleTOs.add(connBundleTO);
            });
        }

        return connectorBundleTOs;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.CONNECTOR_READ + "')")
    public List<ConnIdObjectClassTO> buildObjectClassInfo(
            final ConnInstanceTO connInstanceTO, final boolean includeSpecial) {

        ConnInstance connInstance = connInstanceDAO.authFind(connInstanceTO.getKey());
        if (connInstance == null) {
            throw new NotFoundException("Connector '" + connInstanceTO.getKey() + "'");
        }

        Set<ObjectClassInfo> objectClassInfo = connFactory.createConnector(
                connFactory.buildConnInstanceOverride(connInstance, connInstanceTO.getConf(), null)).
                getObjectClassInfo();

        List<ConnIdObjectClassTO> result = new ArrayList<>(objectClassInfo.size());
        objectClassInfo.stream().map(info -> {
            ConnIdObjectClassTO connIdObjectClassTO = new ConnIdObjectClassTO();
            connIdObjectClassTO.setType(info.getType());
            connIdObjectClassTO.setAuxiliary(info.isAuxiliary());
            connIdObjectClassTO.setContainer(info.isContainer());
            info.getAttributeInfo().stream().
                    filter(attrInfo -> includeSpecial || !AttributeUtil.isSpecialName(attrInfo.getName())).
                    forEachOrdered(attrInfo -> {
                        connIdObjectClassTO.getAttributes().add(attrInfo.getName());
                    });
            return connIdObjectClassTO;
        }).forEachOrdered((connIdObjectClassTO) -> {
            result.add(connIdObjectClassTO);
        });

        return result;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.CONNECTOR_READ + "')")
    @Transactional(readOnly = true)
    public void check(final ConnInstanceTO connInstanceTO) {
        if (connInstanceTO.getAdminRealm() == null) {
            throw SyncopeClientException.build(ClientExceptionType.InvalidRealm);
        }

        connFactory.createConnector(binder.getConnInstance(connInstanceTO)).test();
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.CONNECTOR_READ + "')")
    @Transactional(readOnly = true)
    public ConnInstanceTO readByResource(final String resourceName, final String lang) {
        CurrentLocale.set(StringUtils.isBlank(lang) ? Locale.ENGLISH : new Locale(lang));

        ExternalResource resource = resourceDAO.find(resourceName);
        if (resource == null) {
            throw new NotFoundException("Resource '" + resourceName + "'");
        }
        ConnInstanceTO connInstance = binder.getConnInstanceTO(connFactory.getConnector(resource).getConnInstance());
        connInstance.setKey(resource.getConnector().getKey());
        return connInstance;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.CONNECTOR_RELOAD + "')")
    @Transactional(readOnly = true)
    public void reload() {
        connFactory.unload();
        connFactory.load();
    }

    @Override
    protected ConnInstanceTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof ConnInstanceTO) {
                    key = ((ConnInstanceTO) args[i]).getKey();
                }
            }
        }

        if (key != null) {
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
