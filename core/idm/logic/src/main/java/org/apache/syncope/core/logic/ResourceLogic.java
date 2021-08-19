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
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdMEntitlement;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.data.ResourceDataBinder;
import org.apache.syncope.core.provisioning.java.utils.ConnObjectUtils;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import org.apache.syncope.core.provisioning.api.data.ConnInstanceDataBinder;
import org.apache.syncope.core.provisioning.api.utils.RealmUtils;
import org.apache.syncope.core.provisioning.java.pushpull.OutboundMatcher;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

public class ResourceLogic extends AbstractTransactionalLogic<ResourceTO> {

    protected final ExternalResourceDAO resourceDAO;

    protected final AnyTypeDAO anyTypeDAO;

    protected final ConnInstanceDAO connInstanceDAO;

    protected final VirSchemaDAO virSchemaDAO;

    protected final VirAttrHandler virAttrHandler;

    protected final ResourceDataBinder binder;

    protected final ConnInstanceDataBinder connInstanceDataBinder;

    protected final OutboundMatcher outboundMatcher;

    protected final MappingManager mappingManager;

    protected final ConnectorManager connectorManager;

    protected final AnyUtilsFactory anyUtilsFactory;

    public ResourceLogic(
            final ExternalResourceDAO resourceDAO,
            final AnyTypeDAO anyTypeDAO,
            final ConnInstanceDAO connInstanceDAO,
            final VirSchemaDAO virSchemaDAO,
            final VirAttrHandler virAttrHandler,
            final ResourceDataBinder binder,
            final ConnInstanceDataBinder connInstanceDataBinder,
            final OutboundMatcher outboundMatcher,
            final MappingManager mappingManager,
            final ConnectorManager connectorManager,
            final AnyUtilsFactory anyUtilsFactory) {

        this.resourceDAO = resourceDAO;
        this.anyTypeDAO = anyTypeDAO;
        this.connInstanceDAO = connInstanceDAO;
        this.virSchemaDAO = virSchemaDAO;
        this.virAttrHandler = virAttrHandler;
        this.binder = binder;
        this.connInstanceDataBinder = connInstanceDataBinder;
        this.outboundMatcher = outboundMatcher;
        this.mappingManager = mappingManager;
        this.connectorManager = connectorManager;
        this.anyUtilsFactory = anyUtilsFactory;
    }

    protected void securityChecks(final Set<String> effectiveRealms, final String realm, final String key) {
        boolean authorized = effectiveRealms.stream().anyMatch(realm::startsWith);
        if (!authorized) {
            throw new DelegatedAdministrationException(realm, ExternalResource.class.getSimpleName(), key);
        }
    }

    protected ExternalResource doSave(final ExternalResource resource) {
        ExternalResource merged = resourceDAO.save(resource);
        try {
            connectorManager.registerConnector(merged);
        } catch (NotFoundException e) {
            LOG.error("While registering connector for resource", e);
        }
        return merged;
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.RESOURCE_CREATE + "')")
    public ResourceTO create(final ResourceTO resourceTO) {
        if (StringUtils.isBlank(resourceTO.getKey())) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);
            sce.getElements().add("Resource key");
            throw sce;
        }

        ConnInstance connInstance = connInstanceDAO.authFind(resourceTO.getConnector());
        if (connInstance == null) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidExternalResource);
            sce.getElements().add("Connector " + resourceTO.getConnector());
            throw sce;
        }

        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(IdMEntitlement.RESOURCE_CREATE),
                connInstance.getAdminRealm().getFullPath());
        securityChecks(effectiveRealms, connInstance.getAdminRealm().getFullPath(), null);

        if (resourceDAO.authFind(resourceTO.getKey()) != null) {
            throw new DuplicateException(resourceTO.getKey());
        }

        return binder.getResourceTO(doSave(binder.create(resourceTO)));
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.RESOURCE_UPDATE + "')")
    public ResourceTO update(final ResourceTO resourceTO) {
        ExternalResource resource = resourceDAO.authFind(resourceTO.getKey());
        if (resource == null) {
            throw new NotFoundException("Resource '" + resourceTO.getKey() + '\'');
        }

        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(IdMEntitlement.RESOURCE_UPDATE),
                resource.getConnector().getAdminRealm().getFullPath());
        securityChecks(effectiveRealms, resource.getConnector().getAdminRealm().getFullPath(), resource.getKey());

        return binder.getResourceTO(doSave(binder.update(resource, resourceTO)));
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.RESOURCE_UPDATE + "')")
    public void setLatestSyncToken(final String key, final String anyTypeKey) {
        ExternalResource resource = resourceDAO.authFind(key);
        if (resource == null) {
            throw new NotFoundException("Resource '" + key + '\'');
        }

        Connector connector;
        try {
            connector = connectorManager.getConnector(resource);
        } catch (Exception e) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidConnInstance);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        if (SyncopeConstants.REALM_ANYTYPE.equals(anyTypeKey)) {
            if (resource.getOrgUnit() == null) {
                throw new NotFoundException("Realm provision not enabled for Resource '" + key + '\'');
            }

            resource.getOrgUnit().setSyncToken(connector.getLatestSyncToken(resource.getOrgUnit().getObjectClass()));
        } else {
            AnyType anyType = anyTypeDAO.find(anyTypeKey);
            if (anyType == null) {
                throw new NotFoundException("AnyType '" + anyTypeKey + '\'');
            }
            Optional<? extends Provision> provision = resource.getProvision(anyType);
            if (provision.isEmpty()) {
                throw new NotFoundException("Provision for AnyType '" + anyTypeKey + "' in Resource '" + key + '\'');
            }

            provision.get().setSyncToken(connector.getLatestSyncToken(provision.get().getObjectClass()));
        }

        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(IdMEntitlement.RESOURCE_UPDATE),
                resource.getConnector().getAdminRealm().getFullPath());
        securityChecks(effectiveRealms, resource.getConnector().getAdminRealm().getFullPath(), resource.getKey());

        doSave(resource);
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.RESOURCE_UPDATE + "')")
    public void removeSyncToken(final String key, final String anyTypeKey) {
        ExternalResource resource = resourceDAO.authFind(key);
        if (resource == null) {
            throw new NotFoundException("Resource '" + key + '\'');
        }
        if (SyncopeConstants.REALM_ANYTYPE.equals(anyTypeKey)) {
            if (resource.getOrgUnit() == null) {
                throw new NotFoundException("Realm provision not enabled for Resource '" + key + '\'');
            }

            resource.getOrgUnit().setSyncToken(null);
        } else {
            AnyType anyType = anyTypeDAO.find(anyTypeKey);
            if (anyType == null) {
                throw new NotFoundException("AnyType '" + anyTypeKey + '\'');
            }
            Optional<? extends Provision> provision = resource.getProvision(anyType);
            if (provision.isEmpty()) {
                throw new NotFoundException("Provision for AnyType '" + anyTypeKey + "' in Resource '" + key + '\'');
            }

            provision.get().setSyncToken(null);
        }

        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(IdMEntitlement.RESOURCE_UPDATE),
                resource.getConnector().getAdminRealm().getFullPath());
        securityChecks(effectiveRealms, resource.getConnector().getAdminRealm().getFullPath(), resource.getKey());

        doSave(resource);
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.RESOURCE_DELETE + "')")
    public ResourceTO delete(final String key) {
        ExternalResource resource = resourceDAO.authFind(key);
        if (resource == null) {
            throw new NotFoundException("Resource '" + key + '\'');
        }

        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(IdMEntitlement.RESOURCE_DELETE),
                resource.getConnector().getAdminRealm().getFullPath());
        securityChecks(effectiveRealms, resource.getConnector().getAdminRealm().getFullPath(), resource.getKey());

        ResourceTO resourceToDelete = binder.getResourceTO(resource);

        resourceDAO.delete(key);

        return resourceToDelete;
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.RESOURCE_READ + "')")
    @Transactional(readOnly = true)
    public ResourceTO read(final String key) {
        ExternalResource resource = resourceDAO.authFind(key);
        if (resource == null) {
            throw new NotFoundException("Resource '" + key + '\'');
        }

        return binder.getResourceTO(resource);
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.RESOURCE_LIST + "')")
    @Transactional(readOnly = true)
    public List<ResourceTO> list() {
        return resourceDAO.findAll().stream().map(binder::getResourceTO).collect(Collectors.toList());
    }

    protected Provision getProvision(final String resourceKey, final String anyTypeKey) {
        ExternalResource resource = resourceDAO.find(resourceKey);
        if (resource == null) {
            throw new NotFoundException("Resource '" + resourceKey + '\'');
        }

        AnyType anyType = anyTypeDAO.find(anyTypeKey);
        if (anyType == null) {
            throw new NotFoundException("AnyType '" + anyTypeKey + '\'');
        }

        return resource.getProvision(anyType).
                orElseThrow(() -> new NotFoundException(
                "Provision on resource '" + resourceKey + "' for type '" + anyTypeKey + "'"));
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.RESOURCE_GET_CONNOBJECT + "')")
    @Transactional(readOnly = true)
    public String getConnObjectKeyValue(
            final String key,
            final String anyTypeKey,
            final String anyKey) {

        Provision provision = getProvision(key, anyTypeKey);

        // 1. find any
        Any<?> any = anyUtilsFactory.getInstance(provision.getAnyType().getKind()).dao().authFind(anyKey);
        if (any == null) {
            throw new NotFoundException(provision.getAnyType() + " " + anyKey);
        }

        // 2.get ConnObjectKey value
        return mappingManager.getConnObjectKeyValue(any, provision).
                orElseThrow(() -> new NotFoundException(
                "No ConnObjectKey value found for " + anyTypeKey + " " + anyKey + " on resource '" + key + "'"));
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.RESOURCE_GET_CONNOBJECT + "')")
    @Transactional(readOnly = true)
    public ConnObjectTO readConnObjectByAnyKey(
            final String key,
            final String anyTypeKey,
            final String anyKey) {

        Provision provision = getProvision(key, anyTypeKey);

        // 1. find any
        Any<?> any = anyUtilsFactory.getInstance(provision.getAnyType().getKind()).dao().authFind(anyKey);
        if (any == null) {
            throw new NotFoundException(provision.getAnyType() + " " + anyKey);
        }

        // 2. find on resource
        List<ConnectorObject> connObjs = outboundMatcher.match(
                connectorManager.getConnector(provision.getResource()), any, provision, Optional.empty());
        if (connObjs.isEmpty()) {
            throw new NotFoundException(
                    "Object " + any + " with class " + provision.getObjectClass()
                    + " not found on resource " + provision.getResource().getKey());
        }

        if (connObjs.size() > 1) {
            LOG.warn("Expected single match, found {}", connObjs);
        } else {
            virAttrHandler.setValues(any, connObjs.get(0));
        }

        return ConnObjectUtils.getConnObjectTO(
                outboundMatcher.getFIQL(connObjs.get(0), provision), connObjs.get(0).getAttributes());
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.RESOURCE_GET_CONNOBJECT + "')")
    @Transactional(readOnly = true)
    public ConnObjectTO readConnObjectByConnObjectKeyValue(
            final String key,
            final String anyTypeKey,
            final String connObjectKeyValue) {

        Provision provision = getProvision(key, anyTypeKey);

        MappingItem connObjectKeyItem = MappingUtils.getConnObjectKeyItem(provision).
                orElseThrow(() -> new NotFoundException(
                "ConnObjectKey mapping for " + provision.getAnyType().getKey()
                + " on resource '" + provision.getResource().getKey() + "'"));

        return outboundMatcher.matchByConnObjectKeyValue(
                connectorManager.getConnector(provision.getResource()),
                connObjectKeyItem,
                connObjectKeyValue,
                provision,
                Optional.empty(),
                Optional.empty()).
                map(connectorObject -> ConnObjectUtils.getConnObjectTO(
                outboundMatcher.getFIQL(connectorObject, provision), connectorObject.getAttributes())).
                orElseThrow(() -> new NotFoundException(
                "Object " + connObjectKeyValue + " with class " + provision.getObjectClass()
                + " not found on resource " + provision.getResource().getKey()));
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.RESOURCE_LIST_CONNOBJECT + "')")
    @Transactional(readOnly = true)
    public Pair<SearchResult, List<ConnObjectTO>> searchConnObjects(
            final Filter filter,
            final Set<String> moreAttrsToGet,
            final String key,
            final String anyTypeKey,
            final int size,
            final String pagedResultsCookie,
            final List<OrderByClause> orderBy) {

        ExternalResource resource;
        Provision provision;
        ObjectClass objectClass;
        OperationOptions options;
        if (SyncopeConstants.REALM_ANYTYPE.equals(anyTypeKey)) {
            resource = resourceDAO.find(key);
            if (resource == null) {
                throw new NotFoundException("Resource '" + key + '\'');
            }
            if (resource.getOrgUnit() == null) {
                throw new NotFoundException("Realm provisioning for resource '" + key + '\'');
            }

            provision = null;
            objectClass = resource.getOrgUnit().getObjectClass();
            options = MappingUtils.buildOperationOptions(
                    resource.getOrgUnit().getItems().stream(), moreAttrsToGet.toArray(new String[0]));
        } else {
            provision = getProvision(key, anyTypeKey);
            resource = provision.getResource();
            objectClass = provision.getObjectClass();

            Stream<MappingItem> mapItems = Stream.concat(
                    provision.getMapping().getItems().stream(),
                    virSchemaDAO.findByProvision(provision).stream().map(VirSchema::asLinkingMappingItem));
            options = MappingUtils.buildOperationOptions(mapItems, moreAttrsToGet.toArray(new String[0]));
        }

        List<ConnObjectTO> connObjects = new ArrayList<>();
        SearchResult searchResult = connectorManager.getConnector(resource).
                search(objectClass, filter, new SearchResultsHandler() {

                    private int count;

                    @Override
                    public boolean handle(final ConnectorObject connectorObject) {
                        connObjects.add(ConnObjectUtils.getConnObjectTO(
                                provision == null ? null : outboundMatcher.getFIQL(connectorObject, provision),
                                connectorObject.getAttributes()));
                        // safety protection against uncontrolled result size
                        count++;
                        return count < size;
                    }

                    @Override
                    public void handleResult(final SearchResult sr) {
                        // do nothing
                    }
                }, size, pagedResultsCookie, orderBy, options);

        return Pair.of(searchResult, connObjects);
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.CONNECTOR_READ + "')")
    @Transactional(readOnly = true)
    public void check(final ResourceTO resourceTO) {
        ConnInstance connInstance = connInstanceDAO.find(resourceTO.getConnector());
        if (connInstance == null) {
            throw new NotFoundException("Connector '" + resourceTO.getConnector() + '\'');
        }

        connectorManager.createConnector(
                connectorManager.buildConnInstanceOverride(
                        connInstanceDataBinder.getConnInstanceTO(connInstance),
                        resourceTO.getConfOverride(),
                        resourceTO.isOverrideCapabilities()
                        ? Optional.of(resourceTO.getCapabilitiesOverride()) : Optional.empty())).
                test();
    }

    @Override
    protected ResourceTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String) {
                    key = (String) args[i];
                } else if (args[i] instanceof ResourceTO) {
                    key = ((ResourceTO) args[i]).getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getResourceTO(resourceDAO.find(key));
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
