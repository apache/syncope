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
import java.util.stream.Stream;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ConnObject;
import org.apache.syncope.common.lib.to.Item;
import org.apache.syncope.common.lib.to.Provision;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.IdMEntitlement;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.utils.RealmUtils;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.VirAttrHandler;
import org.apache.syncope.core.provisioning.api.data.ConnInstanceDataBinder;
import org.apache.syncope.core.provisioning.api.data.ResourceDataBinder;
import org.apache.syncope.core.provisioning.java.pushpull.OutboundMatcher;
import org.apache.syncope.core.provisioning.java.utils.ConnObjectUtils;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.springframework.data.domain.Sort;
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
        ExternalResource resource = Optional.ofNullable(resourceDAO.authFind(resourceTO.getKey())).
                orElseThrow(() -> new NotFoundException("Resource '" + resourceTO.getKey() + '\''));

        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(IdMEntitlement.RESOURCE_UPDATE),
                resource.getConnector().getAdminRealm().getFullPath());
        securityChecks(effectiveRealms, resource.getConnector().getAdminRealm().getFullPath(), resource.getKey());

        return binder.getResourceTO(doSave(binder.update(resource, resourceTO)));
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.RESOURCE_UPDATE + "')")
    public void setLatestSyncToken(final String key, final String anyTypeKey) {
        ExternalResource resource = Optional.ofNullable(resourceDAO.authFind(key)).
                orElseThrow(() -> new NotFoundException("Resource '" + key + '\''));

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

            resource.getOrgUnit().setSyncToken(ConnObjectUtils.toString(
                    connector.getLatestSyncToken(new ObjectClass(resource.getOrgUnit().getObjectClass()))));
        } else {
            AnyType anyType = anyTypeDAO.findById(anyTypeKey).
                    orElseThrow(() -> new NotFoundException("AnyType " + anyTypeKey));
            Provision provision = resource.getProvisionByAnyType(anyType.getKey()).
                    orElseThrow(() -> new NotFoundException(
                    "Provision for AnyType '" + anyTypeKey + "' in Resource '" + key + '\''));

            provision.setSyncToken(ConnObjectUtils.toString(
                    connector.getLatestSyncToken(new ObjectClass(provision.getObjectClass()))));
        }

        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(IdMEntitlement.RESOURCE_UPDATE),
                resource.getConnector().getAdminRealm().getFullPath());
        securityChecks(effectiveRealms, resource.getConnector().getAdminRealm().getFullPath(), resource.getKey());

        doSave(resource);
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.RESOURCE_UPDATE + "')")
    public void removeSyncToken(final String key, final String anyTypeKey) {
        ExternalResource resource = Optional.ofNullable(resourceDAO.authFind(key)).
                orElseThrow(() -> new NotFoundException("Resource '" + key + '\''));
        if (SyncopeConstants.REALM_ANYTYPE.equals(anyTypeKey)) {
            if (resource.getOrgUnit() == null) {
                throw new NotFoundException("Realm provision not enabled for Resource '" + key + '\'');
            }

            resource.getOrgUnit().setSyncToken(null);
        } else {
            AnyType anyType = anyTypeDAO.findById(anyTypeKey).
                    orElseThrow(() -> new NotFoundException("AnyType " + anyTypeKey));
            Provision provision = resource.getProvisionByAnyType(anyType.getKey()).
                    orElseThrow(() -> new NotFoundException(
                    "Provision for AnyType '" + anyTypeKey + "' in Resource '" + key + '\''));

            provision.setSyncToken(null);
        }

        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(IdMEntitlement.RESOURCE_UPDATE),
                resource.getConnector().getAdminRealm().getFullPath());
        securityChecks(effectiveRealms, resource.getConnector().getAdminRealm().getFullPath(), resource.getKey());

        doSave(resource);
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.RESOURCE_DELETE + "')")
    public ResourceTO delete(final String key) {
        ExternalResource resource = Optional.ofNullable(resourceDAO.authFind(key)).
                orElseThrow(() -> new NotFoundException("Resource '" + key + '\''));

        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(IdMEntitlement.RESOURCE_DELETE),
                resource.getConnector().getAdminRealm().getFullPath());
        securityChecks(effectiveRealms, resource.getConnector().getAdminRealm().getFullPath(), resource.getKey());

        connectorManager.unregisterConnector(resource);

        ResourceTO deleted = binder.getResourceTO(resource);
        resourceDAO.deleteById(key);
        return deleted;
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.RESOURCE_READ + "')")
    @Transactional(readOnly = true)
    public ResourceTO read(final String key) {
        ExternalResource resource = Optional.ofNullable(resourceDAO.authFind(key)).
                orElseThrow(() -> new NotFoundException("Resource '" + key + '\''));

        return binder.getResourceTO(resource);
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.RESOURCE_LIST + "')")
    @Transactional(readOnly = true)
    public List<ResourceTO> list() {
        return resourceDAO.findAll().stream().map(binder::getResourceTO).toList();
    }

    protected Triple<AnyType, ExternalResource, Provision> getProvision(
            final String anyTypeKey, final String resourceKey) {

        AnyType anyType = anyTypeDAO.findById(anyTypeKey).
                orElseThrow(() -> new NotFoundException("AnyType " + anyTypeKey));

        ExternalResource resource = Optional.ofNullable(resourceDAO.authFind(resourceKey)).
                orElseThrow(() -> new NotFoundException("Resource '" + resourceKey + '\''));
        Provision provision = resource.getProvisionByAnyType(anyType.getKey()).
                orElseThrow(() -> new NotFoundException(
                "Provision for " + anyType + " on Resource '" + resourceKey + "'"));
        if (provision.getMapping() == null) {
            throw new NotFoundException("Mapping for " + anyType + " on Resource '" + resourceKey + "'");
        }

        return Triple.of(anyType, resource, provision);
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.RESOURCE_GET_CONNOBJECT + "')")
    @Transactional(readOnly = true)
    public String getConnObjectKeyValue(
            final String key,
            final String anyTypeKey,
            final String anyKey) {

        Triple<AnyType, ExternalResource, Provision> triple = getProvision(anyTypeKey, key);

        // 1. find any
        Any any = Optional.ofNullable(anyUtilsFactory.getInstance(triple.getLeft().getKind()).
                dao().authFind(anyKey)).
                orElseThrow(() -> new NotFoundException(triple.getLeft() + " " + anyKey));

        // 2.get ConnObjectKey value
        return mappingManager.getConnObjectKeyValue(any, triple.getMiddle(), triple.getRight()).
                orElseThrow(() -> new NotFoundException(
                "No ConnObjectKey value found for " + anyTypeKey + " " + anyKey + " on resource '" + key + "'"));
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.RESOURCE_GET_CONNOBJECT + "')")
    @Transactional(readOnly = true)
    public ConnObject readConnObjectByAnyKey(
            final String key,
            final String anyTypeKey,
            final String anyKey) {

        Triple<AnyType, ExternalResource, Provision> triple = getProvision(anyTypeKey, key);

        // 1. find any
        Any any = Optional.ofNullable(anyUtilsFactory.getInstance(triple.getLeft().getKind()).
                dao().authFind(anyKey)).
                orElseThrow(() -> new NotFoundException(triple.getLeft() + " " + anyKey));

        // 2. find on resource
        List<ConnectorObject> connObjs = outboundMatcher.match(
                connectorManager.getConnector(triple.getMiddle()),
                any,
                triple.getMiddle(),
                triple.getRight(),
                Optional.empty());
        if (connObjs.isEmpty()) {
            throw new NotFoundException(
                    "Object " + any + " with class " + triple.getRight().getObjectClass()
                    + " not found on resource " + triple.getMiddle().getKey());
        }

        if (connObjs.size() > 1) {
            LOG.warn("Expected single match, found {}", connObjs);
        } else {
            virAttrHandler.setValues(any, connObjs.getFirst());
        }

        return ConnObjectUtils.getConnObjectTO(
                outboundMatcher.getFIQL(connObjs.getFirst(), triple.getMiddle(), triple.getRight()),
                connObjs.getFirst().getAttributes());
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.RESOURCE_GET_CONNOBJECT + "')")
    @Transactional(readOnly = true)
    public ConnObject readConnObjectByConnObjectKeyValue(
            final String key,
            final String anyTypeKey,
            final String connObjectKeyValue) {

        Triple<AnyType, ExternalResource, Provision> triple = getProvision(anyTypeKey, key);

        Item connObjectKeyItem = MappingUtils.getConnObjectKeyItem(triple.getRight()).
                orElseThrow(() -> new NotFoundException(
                "ConnObjectKey mapping for " + triple.getLeft().getKey()
                + " on resource '" + triple.getMiddle().getKey() + "'"));

        return outboundMatcher.matchByConnObjectKeyValue(
                connectorManager.getConnector(triple.getMiddle()),
                connObjectKeyItem,
                connObjectKeyValue,
                triple.getMiddle(),
                triple.getRight(),
                Optional.empty(),
                Optional.empty()).
                map(connectorObject -> ConnObjectUtils.getConnObjectTO(
                outboundMatcher.getFIQL(connectorObject, triple.getMiddle(), triple.getRight()),
                connectorObject.getAttributes())).
                orElseThrow(() -> new NotFoundException(
                "Object " + connObjectKeyValue + " with class " + triple.getRight().getObjectClass()
                + " not found on resource " + triple.getMiddle().getKey()));
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.RESOURCE_LIST_CONNOBJECT + "')")
    @Transactional(readOnly = true)
    public Pair<SearchResult, List<ConnObject>> searchConnObjects(
            final Filter filter,
            final Set<String> moreAttrsToGet,
            final String key,
            final String anyTypeKey,
            final int size,
            final String pagedResultsCookie,
            final List<Sort.Order> sort) {

        ExternalResource resource;
        Provision provision;
        ObjectClass objectClass;
        OperationOptions options;
        if (SyncopeConstants.REALM_ANYTYPE.equals(anyTypeKey)) {
            resource = resourceDAO.findById(key).
                    orElseThrow(() -> new NotFoundException("Resource " + key));
            if (resource.getOrgUnit() == null) {
                throw new NotFoundException("Realm provisioning for resource '" + key + '\'');
            }

            provision = null;
            objectClass = new ObjectClass(resource.getOrgUnit().getObjectClass());
            options = MappingUtils.buildOperationOptions(
                    resource.getOrgUnit().getItems().stream(), moreAttrsToGet.toArray(String[]::new));
        } else {
            Triple<AnyType, ExternalResource, Provision> triple = getProvision(anyTypeKey, key);

            provision = triple.getRight();
            resource = triple.getMiddle();
            objectClass = new ObjectClass(provision.getObjectClass());

            Stream<Item> mapItems = Stream.concat(
                    provision.getMapping().getItems().stream(),
                    virSchemaDAO.findByResourceAndAnyType(resource.getKey(), triple.getLeft().getKey()).
                            stream().map(VirSchema::asLinkingMappingItem));
            options = MappingUtils.buildOperationOptions(mapItems, moreAttrsToGet.toArray(String[]::new));
        }

        List<ConnObject> connObjects = new ArrayList<>();
        SearchResult searchResult = connectorManager.getConnector(resource).
                search(objectClass, filter, new SearchResultsHandler() {

                    private int count;

                    @Override
                    public boolean handle(final ConnectorObject connectorObject) {
                        connObjects.add(ConnObjectUtils.getConnObjectTO(
                                provision == null
                                        ? null : outboundMatcher.getFIQL(connectorObject, resource, provision),
                                connectorObject.getAttributes()));
                        // safety protection against uncontrolled result size
                        count++;
                        return count < size;
                    }

                    @Override
                    public void handleResult(final SearchResult sr) {
                        // do nothing
                    }
                }, size, pagedResultsCookie, sort, options);

        return Pair.of(searchResult, connObjects);
    }

    @PreAuthorize("hasRole('" + IdMEntitlement.CONNECTOR_READ + "')")
    @Transactional(readOnly = true)
    public void check(final ResourceTO resourceTO) {
        ConnInstance connInstance = connInstanceDAO.findById(resourceTO.getConnector()).
                orElseThrow(() -> new NotFoundException("Connector " + resourceTO.getConnector()));

        connectorManager.createConnector(
                connectorManager.buildConnInstanceOverride(
                        connInstanceDataBinder.getConnInstanceTO(connInstance),
                        resourceTO.getConfOverride(),
                        resourceTO.getCapabilitiesOverride())).
                test();
    }

    @Override
    protected ResourceTO resolveReference(final Method method, final Object... args)
            throws UnresolvedReferenceException {

        String key = null;

        if (ArrayUtils.isNotEmpty(args)) {
            for (int i = 0; key == null && i < args.length; i++) {
                if (args[i] instanceof String string) {
                    key = string;
                } else if (args[i] instanceof ResourceTO resourceTO) {
                    key = resourceTO.getKey();
                }
            }
        }

        if (key != null) {
            try {
                return binder.getResourceTO(resourceDAO.findById(key).orElseThrow());
            } catch (Throwable ignore) {
                LOG.debug("Unresolved reference", ignore);
                throw new UnresolvedReferenceException(ignore);
            }
        }

        throw new UnresolvedReferenceException();
    }
}
