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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.ConnIdBundle;
import org.apache.syncope.common.lib.to.ConnIdObjectClass;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.PlainSchemaTO;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdMEntitlement;
import org.apache.syncope.core.persistence.api.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.utils.RealmUtils;
import org.apache.syncope.core.provisioning.api.ConnIdBundleManager;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.provisioning.api.data.ConnInstanceDataBinder;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.identityconnectors.common.l10n.CurrentLocale;
import org.identityconnectors.framework.api.ConfigurationProperties;
import org.identityconnectors.framework.api.ConnectorKey;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ObjectClassInfo;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class ConnectorLogic extends AbstractTransactionalLogic<ConnInstanceTO> {

    protected final ConnIdBundleManager connIdBundleManager;

    protected final ConnectorManager connectorManager;

    protected final ExternalResourceDAO resourceDAO;

    protected final ConnInstanceDAO connInstanceDAO;

    protected final ConnInstanceDataBinder binder;

    public ConnectorLogic(
            final ConnIdBundleManager connIdBundleManager,
            final ConnectorManager connectorManager,
            final ExternalResourceDAO resourceDAO,
            final ConnInstanceDAO connInstanceDAO,
            final ConnInstanceDataBinder binder) {

        this.connIdBundleManager = connIdBundleManager;
        this.connectorManager = connectorManager;
        this.resourceDAO = resourceDAO;
        this.connInstanceDAO = connInstanceDAO;
        this.binder = binder;
    }

    protected void securityChecks(final Set<String> effectiveRealms, final String realm, final String key) {
        if (effectiveRealms.stream().noneMatch(realm::startsWith)) {
            throw new DelegatedAdministrationException(realm, ConnInstance.class.getSimpleName(), key);
        }
    }

    protected ConnInstance doSave(final ConnInstance connInstance) {
        ConnInstance merged = connInstanceDAO.save(connInstance);
        merged.getResources().forEach(resource -> {
            try {
                connectorManager.registerConnector(resource);
            } catch (NotFoundException e) {
                LOG.error("While registering connector {} for resource {}", merged, resource, e);
            }
        });
        return merged;
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.CONNECTOR_CREATE + "')")
    public ConnInstanceTO create(final ConnInstanceTO connInstanceTO) {
        if (connInstanceTO.getAdminRealm() == null) {
            throw SyncopeClientException.build(ClientExceptionType.InvalidRealm);
        }

        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(IdMEntitlement.CONNECTOR_CREATE),
                connInstanceTO.getAdminRealm());
        securityChecks(effectiveRealms, connInstanceTO.getAdminRealm(), null);

        return binder.getConnInstanceTO(doSave(binder.getConnInstance(connInstanceTO)));
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.CONNECTOR_UPDATE + "')")
    public ConnInstanceTO update(final ConnInstanceTO connInstanceTO) {
        if (connInstanceTO.getAdminRealm() == null) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidConnInstance);
            sce.getElements().add("Invalid or null realm specified: " + connInstanceTO.getAdminRealm());
            throw sce;
        }

        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(IdMEntitlement.CONNECTOR_UPDATE),
                connInstanceTO.getAdminRealm());
        securityChecks(effectiveRealms, connInstanceTO.getAdminRealm(), connInstanceTO.getKey());

        return binder.getConnInstanceTO(doSave(binder.update(connInstanceTO)));
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.CONNECTOR_DELETE + "')")
    public ConnInstanceTO delete(final String key) {
        ConnInstance connInstance = Optional.ofNullable(connInstanceDAO.authFind(key)).
                orElseThrow(() -> new NotFoundException("Connector '" + key + '\''));

        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(IdMEntitlement.CONNECTOR_DELETE),
                connInstance.getAdminRealm().getFullPath());
        securityChecks(effectiveRealms, connInstance.getAdminRealm().getFullPath(), connInstance.getKey());

        if (!connInstance.getResources().isEmpty()) {
            SyncopeClientException associatedResources = SyncopeClientException.build(
                    ClientExceptionType.AssociatedResources);
            connInstance.getResources().forEach(resource -> associatedResources.getElements().add(resource.getKey()));
            throw associatedResources;
        }

        ConnInstanceTO deleted = binder.getConnInstanceTO(connInstance);
        connInstanceDAO.deleteById(key);
        return deleted;
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.CONNECTOR_LIST + "')")
    @Transactional(readOnly = true)
    public List<ConnInstanceTO> list(final String lang) {
        CurrentLocale.set(StringUtils.isBlank(lang) ? Locale.ENGLISH : Locale.of(lang));

        return connInstanceDAO.findAll().stream().map(binder::getConnInstanceTO).toList();
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.CONNECTOR_READ + "')")
    @Transactional(readOnly = true)
    public ConnInstanceTO read(final String key, final String lang) {
        CurrentLocale.set(StringUtils.isBlank(lang) ? Locale.ENGLISH : Locale.of(lang));

        ConnInstance connInstance = connInstanceDAO.authFind(key);
        if (connInstance == null) {
            throw new NotFoundException("Connector '" + key + '\'');
        }

        return binder.getConnInstanceTO(connInstance);
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.CONNECTOR_READ + "')")
    @Transactional(readOnly = true)
    public List<ConnIdBundle> getBundles(final String lang) {
        if (StringUtils.isBlank(lang)) {
            CurrentLocale.set(Locale.ENGLISH);
        } else {
            CurrentLocale.set(Locale.of(lang));
        }

        List<ConnIdBundle> connectorBundleTOs = new ArrayList<>();
        connIdBundleManager.getConnInfoManagers().forEach((uri, cim) -> connectorBundleTOs.addAll(
                cim.getConnectorInfos().stream().map(bundle -> {
                    ConnIdBundle connBundleTO = new ConnIdBundle();
                    connBundleTO.setDisplayName(bundle.getConnectorDisplayName());

                    connBundleTO.setLocation(uri.toString());

                    ConnectorKey key = bundle.getConnectorKey();
                    connBundleTO.setBundleName(key.getBundleName());
                    connBundleTO.setConnectorName(key.getConnectorName());
                    connBundleTO.setVersion(key.getBundleVersion());

                    ConfigurationProperties properties = connIdBundleManager.getConfigurationProperties(bundle);
                    connBundleTO.getProperties().addAll(properties.getPropertyNames().stream().
                            map(propName -> binder.build(properties.getProperty(propName))).
                            toList());

                    return connBundleTO;
                }).toList()));

        return connectorBundleTOs;
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.CONNECTOR_READ + "')")
    public List<ConnIdObjectClass> buildObjectClassInfo(
            final ConnInstanceTO connInstanceTO, final boolean includeSpecial) {

        ConnInstanceTO actual = connInstanceDAO.findById(connInstanceTO.getKey()).
                map(binder::getConnInstanceTO).
                orElse(connInstanceTO);

        Set<ObjectClassInfo> objectClassInfo = connectorManager.createConnector(
                connectorManager.buildConnInstanceOverride(
                        actual, Optional.of(connInstanceTO.getConf()), Optional.empty())).
                getObjectClassInfo();

        return objectClassInfo.stream().map(info -> {
            ConnIdObjectClass connIdObjectClassTO = new ConnIdObjectClass();
            connIdObjectClassTO.setType(info.getType());
            connIdObjectClassTO.setAuxiliary(info.isAuxiliary());
            connIdObjectClassTO.setContainer(info.isContainer());
            connIdObjectClassTO.getAttributes().addAll(info.getAttributeInfo().stream().
                    filter(attrInfo -> includeSpecial || !AttributeUtil.isSpecialName(attrInfo.getName())).
                    map(attrInfo -> {
                        PlainSchemaTO schema = new PlainSchemaTO();
                        schema.setKey(attrInfo.getName());
                        schema.setMandatoryCondition(BooleanUtils.toStringTrueFalse(attrInfo.isRequired()));
                        schema.setMultivalue(attrInfo.isMultiValued());
                        schema.setReadonly(!attrInfo.isUpdateable());
                        schema.setType(AttrSchemaType.getAttrSchemaTypeByClass(attrInfo.getType()));
                        return schema;
                    }).
                    toList());

            return connIdObjectClassTO;
        }).toList();
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.CONNECTOR_READ + "')")
    @Transactional(readOnly = true)
    public void check(final ConnInstanceTO connInstanceTO) {
        if (connInstanceTO.getAdminRealm() == null) {
            throw SyncopeClientException.build(ClientExceptionType.InvalidRealm);
        }

        connectorManager.createConnector(binder.getConnInstance(connInstanceTO)).test();
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.CONNECTOR_READ + "')")
    @Transactional(readOnly = true)
    public ConnInstanceTO readByResource(final String resourceName, final String lang) {
        CurrentLocale.set(StringUtils.isBlank(lang) ? Locale.ENGLISH : Locale.of(lang));

        ExternalResource resource = resourceDAO.findById(resourceName).
                orElseThrow(() -> new NotFoundException("Resource " + resourceName));
        ConnInstanceTO connInstance = binder.getConnInstanceTO(
                connectorManager.getConnector(resource).getConnInstance());
        connInstance.setKey(resource.getConnector().getKey());
        return connInstance;
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.CONNECTOR_RELOAD + "')")
    @Transactional(readOnly = true)
    public void reload() {
        connectorManager.unload();
        connectorManager.load();
    }

    @Override
    protected ConnInstanceTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String string) {
                    key = string;
                } else if (args[i] instanceof ConnInstanceTO connInstanceTO) {
                    key = connInstanceTO.getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getConnInstanceTO(connInstanceDAO.findById(key).orElseThrow());
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
