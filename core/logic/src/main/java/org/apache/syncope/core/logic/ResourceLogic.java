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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.ImmutableTriple;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.common.lib.collections.IteratorChain;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.StandardEntitlement;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.provisioning.api.Connector;
import org.apache.syncope.core.provisioning.api.ConnectorFactory;
import org.apache.syncope.core.provisioning.api.data.ResourceDataBinder;
import org.apache.syncope.core.provisioning.java.utils.ConnObjectUtils;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.provisioning.api.MappingManager;
import org.apache.syncope.core.provisioning.api.utils.RealmUtils;
import org.apache.syncope.core.provisioning.java.utils.MappingUtils;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.DelegatedAdministrationException;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeBuilder;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.OperationOptions;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.Uid;
import org.identityconnectors.framework.spi.SearchResultsHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ResourceLogic extends AbstractTransactionalLogic<ResourceTO> {

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private VirSchemaDAO virSchemaDAO;

    @Autowired
    private ResourceDataBinder binder;

    @Autowired
    private ConnObjectUtils connObjectUtils;

    @Autowired
    private MappingManager mappingManager;

    @Autowired
    private ConnectorFactory connFactory;

    protected void securityChecks(final Set<String> effectiveRealms, final String realm, final String key) {
        effectiveRealms.stream().anyMatch(ownedRealm -> realm.startsWith(ownedRealm));
        boolean authorized = effectiveRealms.stream().anyMatch(ownedRealm -> realm.startsWith(ownedRealm));
        if (!authorized) {
            throw new DelegatedAdministrationException(realm, ExternalResource.class.getSimpleName(), key);
        }
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.RESOURCE_CREATE + "')")
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
                AuthContextUtils.getAuthorizations().get(StandardEntitlement.RESOURCE_CREATE),
                connInstance.getAdminRealm().getFullPath());
        securityChecks(effectiveRealms, connInstance.getAdminRealm().getFullPath(), null);

        if (resourceDAO.authFind(resourceTO.getKey()) != null) {
            throw new DuplicateException(resourceTO.getKey());
        }

        return binder.getResourceTO(resourceDAO.save(binder.create(resourceTO)));
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.RESOURCE_UPDATE + "')")
    public ResourceTO update(final ResourceTO resourceTO) {
        ExternalResource resource = resourceDAO.authFind(resourceTO.getKey());
        if (resource == null) {
            throw new NotFoundException("Resource '" + resourceTO.getKey() + "'");
        }

        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(StandardEntitlement.RESOURCE_UPDATE),
                resource.getConnector().getAdminRealm().getFullPath());
        securityChecks(effectiveRealms, resource.getConnector().getAdminRealm().getFullPath(), resource.getKey());

        return binder.getResourceTO(resourceDAO.save(binder.update(resource, resourceTO)));
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.RESOURCE_UPDATE + "')")
    public void setLatestSyncToken(final String key, final String anyTypeKey) {
        ExternalResource resource = resourceDAO.authFind(key);
        if (resource == null) {
            throw new NotFoundException("Resource '" + key + "'");
        }

        Connector connector;
        try {
            connector = connFactory.getConnector(resource);
        } catch (Exception e) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidConnInstance);
            sce.getElements().add(e.getMessage());
            throw sce;
        }

        if (SyncopeConstants.REALM_ANYTYPE.equals(anyTypeKey)) {
            if (resource.getOrgUnit() == null) {
                throw new NotFoundException("Realm provision not enabled for Resource '" + key + "'");
            }

            resource.getOrgUnit().setSyncToken(connector.getLatestSyncToken(resource.getOrgUnit().getObjectClass()));
        } else {
            AnyType anyType = anyTypeDAO.find(anyTypeKey);
            if (anyType == null) {
                throw new NotFoundException("AnyType '" + anyTypeKey + "'");
            }
            Optional<? extends Provision> provision = resource.getProvision(anyType);
            if (!provision.isPresent()) {
                throw new NotFoundException("Provision for AnyType '" + anyTypeKey + "' in Resource '" + key + "'");
            }

            provision.get().setSyncToken(connector.getLatestSyncToken(provision.get().getObjectClass()));
        }

        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(StandardEntitlement.RESOURCE_UPDATE),
                resource.getConnector().getAdminRealm().getFullPath());
        securityChecks(effectiveRealms, resource.getConnector().getAdminRealm().getFullPath(), resource.getKey());

        resourceDAO.save(resource);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.RESOURCE_UPDATE + "')")
    public void removeSyncToken(final String key, final String anyTypeKey) {
        ExternalResource resource = resourceDAO.authFind(key);
        if (resource == null) {
            throw new NotFoundException("Resource '" + key + "'");
        }
        if (SyncopeConstants.REALM_ANYTYPE.equals(anyTypeKey)) {
            if (resource.getOrgUnit() == null) {
                throw new NotFoundException("Realm provision not enabled for Resource '" + key + "'");
            }

            resource.getOrgUnit().setSyncToken(null);
        } else {
            AnyType anyType = anyTypeDAO.find(anyTypeKey);
            if (anyType == null) {
                throw new NotFoundException("AnyType '" + anyTypeKey + "'");
            }
            Optional<? extends Provision> provision = resource.getProvision(anyType);
            if (!provision.isPresent()) {
                throw new NotFoundException("Provision for AnyType '" + anyTypeKey + "' in Resource '" + key + "'");
            }

            provision.get().setSyncToken(null);
        }

        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(StandardEntitlement.RESOURCE_UPDATE),
                resource.getConnector().getAdminRealm().getFullPath());
        securityChecks(effectiveRealms, resource.getConnector().getAdminRealm().getFullPath(), resource.getKey());

        resourceDAO.save(resource);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.RESOURCE_DELETE + "')")
    public ResourceTO delete(final String key) {
        ExternalResource resource = resourceDAO.authFind(key);
        if (resource == null) {
            throw new NotFoundException("Resource '" + key + "'");
        }

        Set<String> effectiveRealms = RealmUtils.getEffective(
                AuthContextUtils.getAuthorizations().get(StandardEntitlement.RESOURCE_DELETE),
                resource.getConnector().getAdminRealm().getFullPath());
        securityChecks(effectiveRealms, resource.getConnector().getAdminRealm().getFullPath(), resource.getKey());

        ResourceTO resourceToDelete = binder.getResourceTO(resource);

        resourceDAO.delete(key);

        return resourceToDelete;
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.RESOURCE_READ + "')")
    @Transactional(readOnly = true)
    public ResourceTO read(final String key) {
        ExternalResource resource = resourceDAO.authFind(key);
        if (resource == null) {
            throw new NotFoundException("Resource '" + key + "'");
        }

        return binder.getResourceTO(resource);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.RESOURCE_LIST + "')")
    @Transactional(readOnly = true)
    public List<ResourceTO> list() {
        return resourceDAO.findAll().stream().
                map(resource -> binder.getResourceTO(resource)).collect(Collectors.toList());
    }

    private Triple<ExternalResource, AnyType, Provision> connObjectInit(
            final String resourceKey, final String anyTypeKey) {

        ExternalResource resource = resourceDAO.authFind(resourceKey);
        if (resource == null) {
            throw new NotFoundException("Resource '" + resourceKey + "'");
        }
        AnyType anyType = anyTypeDAO.find(anyTypeKey);
        if (anyType == null) {
            throw new NotFoundException("AnyType '" + anyTypeKey + "'");
        }
        Optional<? extends Provision> provision = resource.getProvision(anyType);
        if (!provision.isPresent()) {
            throw new NotFoundException("Provision on resource '" + resourceKey + "' for type '" + anyTypeKey + "'");
        }

        return ImmutableTriple.of(resource, anyType, provision.get());
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.RESOURCE_GET_CONNOBJECT + "')")
    @Transactional(readOnly = true)
    public ConnObjectTO readConnObject(final String key, final String anyTypeKey, final String anyKey) {
        Triple<ExternalResource, AnyType, Provision> init = connObjectInit(key, anyTypeKey);

        // 1. find any
        Any<?> any = init.getMiddle().getKind() == AnyTypeKind.USER
                ? userDAO.find(anyKey)
                : init.getMiddle().getKind() == AnyTypeKind.ANY_OBJECT
                ? anyObjectDAO.find(anyKey)
                : groupDAO.find(anyKey);
        if (any == null) {
            throw new NotFoundException(init.getMiddle() + " " + anyKey);
        }

        // 2. build connObjectKeyItem
        Optional<MappingItem> connObjectKeyItem = MappingUtils.getConnObjectKeyItem(init.getRight());
        if (!connObjectKeyItem.isPresent()) {
            throw new NotFoundException(
                    "ConnObjectKey mapping for " + init.getMiddle() + " " + anyKey + " on resource '" + key + "'");
        }
        Optional<String> connObjectKeyValue = mappingManager.getConnObjectKeyValue(any, init.getRight());

        // 3. determine attributes to query
        Set<MappingItem> linkinMappingItems = new HashSet<>();
        virSchemaDAO.findByProvision(init.getRight()).forEach(virSchema -> {
            linkinMappingItems.add(virSchema.asLinkingMappingItem());
        });
        Iterator<MappingItem> mapItems = new IteratorChain<>(
                init.getRight().getMapping().getItems().iterator(),
                linkinMappingItems.iterator());

        // 4. read from the underlying connector
        Connector connector = connFactory.getConnector(init.getLeft());
        ConnectorObject connectorObject = connector.getObject(
                init.getRight().getObjectClass(),
                AttributeBuilder.build(connObjectKeyItem.get().getExtAttrName(), connObjectKeyValue.get()),
                MappingUtils.buildOperationOptions(mapItems));
        if (connectorObject == null) {
            throw new NotFoundException(
                    "Object " + connObjectKeyValue.get() + " with class " + init.getRight().getObjectClass()
                    + " not found on resource " + key);
        }

        // 5. build result
        Set<Attribute> attributes = connectorObject.getAttributes();
        if (AttributeUtil.find(Uid.NAME, attributes) == null) {
            attributes.add(connectorObject.getUid());
        }
        if (AttributeUtil.find(Name.NAME, attributes) == null) {
            attributes.add(connectorObject.getName());
        }

        return connObjectUtils.getConnObjectTO(connectorObject);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.RESOURCE_LIST_CONNOBJECT + "')")
    @Transactional(readOnly = true)
    public Pair<SearchResult, List<ConnObjectTO>> listConnObjects(final String key, final String anyTypeKey,
            final int size, final String pagedResultsCookie, final List<OrderByClause> orderBy) {

        ExternalResource resource;
        ObjectClass objectClass;
        OperationOptions options;
        if (SyncopeConstants.REALM_ANYTYPE.equals(anyTypeKey)) {
            resource = resourceDAO.authFind(key);
            if (resource == null) {
                throw new NotFoundException("Resource '" + key + "'");
            }
            if (resource.getOrgUnit() == null) {
                throw new NotFoundException("Realm provisioning for resource '" + key + "'");
            }

            objectClass = resource.getOrgUnit().getObjectClass();
            options = MappingUtils.buildOperationOptions(
                    MappingUtils.getPropagationItems(resource.getOrgUnit().getItems()).iterator());
        } else {
            Triple<ExternalResource, AnyType, Provision> init = connObjectInit(key, anyTypeKey);
            resource = init.getLeft();
            objectClass = init.getRight().getObjectClass();
            init.getRight().getMapping().getItems();

            Set<MappingItem> linkinMappingItems = new HashSet<>();
            virSchemaDAO.findByProvision(init.getRight()).forEach(virSchema -> {
                linkinMappingItems.add(virSchema.asLinkingMappingItem());
            });
            Iterator<MappingItem> mapItems = new IteratorChain<>(
                    init.getRight().getMapping().getItems().iterator(),
                    linkinMappingItems.iterator());
            options = MappingUtils.buildOperationOptions(mapItems);
        }

        final SearchResult[] searchResult = new SearchResult[1];
        final List<ConnObjectTO> connObjects = new ArrayList<>();

        connFactory.getConnector(resource).search(objectClass, null, new SearchResultsHandler() {

            private int count;

            @Override
            public void handleResult(final SearchResult result) {
                searchResult[0] = result;
            }

            @Override
            public boolean handle(final ConnectorObject connectorObject) {
                connObjects.add(connObjectUtils.getConnObjectTO(connectorObject));
                // safety protection against uncontrolled result size
                count++;
                return count < size;
            }
        }, size, pagedResultsCookie, orderBy, options);

        return ImmutablePair.of(searchResult[0], connObjects);
    }

    @PreAuthorize("hasRole('" + StandardEntitlement.CONNECTOR_READ + "')")
    @Transactional(readOnly = true)
    public void check(final ResourceTO resourceTO) {
        ConnInstance connInstance = connInstanceDAO.find(resourceTO.getConnector());
        if (connInstance == null) {
            throw new NotFoundException("Connector '" + resourceTO.getConnector() + "'");
        }

        connFactory.createConnector(
                connFactory.buildConnInstanceOverride(
                        connInstance,
                        resourceTO.getConfOverride(),
                        resourceTO.isOverrideCapabilities() ? resourceTO.getCapabilitiesOverride() : null)).
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
