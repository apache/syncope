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

import java.util.List;
import java.util.Set;
import javax.persistence.EntityExistsException;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.to.BulkAction;
import org.apache.syncope.common.to.BulkActionRes;
import org.apache.syncope.common.to.ConnObjectTO;
import org.apache.syncope.common.to.ResourceTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.AuditElements;
import org.apache.syncope.common.types.AuditElements.Category;
import org.apache.syncope.common.types.AuditElements.ResourceSubCategory;
import org.apache.syncope.common.types.AuditElements.Result;
import org.apache.syncope.common.types.MappingPurpose;
import org.apache.syncope.common.types.ClientExceptionType;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.connid.ConnObjectUtil;
import org.apache.syncope.core.init.ImplementationClassNamesLoader;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractMappingItem;
import org.apache.syncope.core.persistence.beans.ConnInstance;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.persistence.dao.ResourceDAO;
import org.apache.syncope.core.persistence.dao.RoleDAO;
import org.apache.syncope.core.persistence.dao.UserDAO;
import org.apache.syncope.core.propagation.ConnectorFactory;
import org.apache.syncope.core.propagation.Connector;
import org.apache.syncope.core.rest.data.ResourceDataBinder;
import org.apache.syncope.core.util.AttributableUtil;
import org.apache.syncope.core.util.MappingUtil;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ResourceController extends AbstractController {

    @Autowired
    private AuditManager auditManager;

    @Autowired
    private ResourceDAO resourceDAO;

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private ResourceDataBinder binder;

    @Autowired
    private ImplementationClassNamesLoader classNamesLoader;

    /**
     * ConnectorObject util.
     */
    @Autowired
    private ConnObjectUtil connObjectUtil;

    @Autowired
    private ConnectorFactory connFactory;

    @PreAuthorize("hasRole('RESOURCE_CREATE')")
    public ResourceTO create(final ResourceTO resourceTO) {
        LOG.debug("Resource creation: {}", resourceTO);

        if (StringUtils.isBlank(resourceTO.getName())) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);
            sce.getElements().add("Resource name");
            throw sce;
        }

        if (resourceDAO.find(resourceTO.getName()) != null) {
            throw new EntityExistsException("Resource '" + resourceTO.getName() + "'");
        }

        ExternalResource resource = resourceDAO.save(binder.create(resourceTO));

        auditManager.audit(Category.resource, ResourceSubCategory.create, Result.success,
                "Successfully created resource: " + resource.getName());

        return binder.getResourceTO(resource);
    }

    @PreAuthorize("hasRole('RESOURCE_UPDATE')")
    public ResourceTO update(final ResourceTO resourceTO) {
        LOG.debug("Role update request: {}", resourceTO);

        ExternalResource resource = resourceDAO.find(resourceTO.getName());
        if (resource == null) {
            throw new NotFoundException("Resource '" + resourceTO.getName() + "'");
        }

        resource = binder.update(resource, resourceTO);
        resource = resourceDAO.save(resource);

        auditManager.audit(Category.resource, ResourceSubCategory.update, Result.success,
                "Successfully updated resource: " + resource.getName());

        return binder.getResourceTO(resource);
    }

    @PreAuthorize("hasRole('RESOURCE_DELETE')")
    public ResourceTO delete(final String resourceName) {
        ExternalResource resource = resourceDAO.find(resourceName);
        if (resource == null) {
            throw new NotFoundException("Resource '" + resourceName + "'");
        }

        ResourceTO resourceToDelete = binder.getResourceTO(resource);

        auditManager.audit(Category.resource, ResourceSubCategory.delete, Result.success,
                "Successfully deleted resource: " + resource.getName());

        resourceDAO.delete(resourceName);

        return resourceToDelete;
    }

    @PreAuthorize("hasRole('RESOURCE_READ')")
    @Transactional(readOnly = true)
    public ResourceTO read(final String resourceName) {
        ExternalResource resource = resourceDAO.find(resourceName);
        if (resource == null) {
            throw new NotFoundException("Resource '" + resourceName + "'");
        }

        auditManager.audit(Category.resource, ResourceSubCategory.read, Result.success,
                "Successfully read resource: " + resource.getName());

        return binder.getResourceTO(resource);
    }

    @PreAuthorize("hasRole('RESOURCE_READ')")
    public Set<String> getPropagationActionsClasses() {
        Set<String> actionsClasses = classNamesLoader.getClassNames(
                ImplementationClassNamesLoader.Type.PROPAGATION_ACTIONS);

        auditManager.audit(Category.resource, AuditElements.ResourceSubCategory.getPropagationActionsClasses,
                Result.success, "Successfully listed all PropagationActions classes: " + actionsClasses.size());

        return actionsClasses;
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<ResourceTO> list(final Long connInstanceId) {
        List<ExternalResource> resources;

        if (connInstanceId == null) {
            resources = resourceDAO.findAll();
        } else {
            ConnInstance connInstance = connInstanceDAO.find(connInstanceId);
            resources = connInstance.getResources();
        }

        List<ResourceTO> result = binder.getResourceTOs(resources);

        auditManager.audit(Category.resource, ResourceSubCategory.list, Result.success,
                connInstanceId == null
                ? "Successfully listed all resources: " + result.size()
                : "Successfully listed resources for connector " + connInstanceId + ": " + result.size());

        return result;
    }

    @PreAuthorize("hasRole('RESOURCE_GETCONNECTOROBJECT')")
    @Transactional(readOnly = true)
    public ConnObjectTO getConnectorObject(final String resourceName, final AttributableType type, final Long id) {
        ExternalResource resource = resourceDAO.find(resourceName);
        if (resource == null) {
            throw new NotFoundException("Resource '" + resourceName + "'");
        }

        AbstractAttributable attributable = null;
        switch (type) {
            case USER:
                attributable = userDAO.find(id);
                break;

            case ROLE:
                attributable = roleDAO.find(id);
                break;

            case MEMBERSHIP:
            default:
                throw new IllegalArgumentException("Not supported for MEMBERSHIP");
        }
        if (attributable == null) {
            throw new NotFoundException(type + " " + id);
        }

        final AttributableUtil attrUtil = AttributableUtil.getInstance(type);

        AbstractMappingItem accountIdItem = attrUtil.getAccountIdItem(resource);
        if (accountIdItem == null) {
            throw new NotFoundException("AccountId mapping for " + type + " " + id + " on resource '" + resourceName
                    + "'");
        }
        final String accountIdValue = MappingUtil.getAccountIdValue(
                attributable, resource, attrUtil.getAccountIdItem(resource));

        final ObjectClass objectClass = AttributableType.USER == type ? ObjectClass.ACCOUNT : ObjectClass.GROUP;

        final Connector connector = connFactory.getConnector(resource);
        final ConnectorObject connectorObject = connector.getObject(objectClass, new Uid(accountIdValue),
                connector.getOperationOptions(attrUtil.getMappingItems(resource, MappingPurpose.BOTH)));
        if (connectorObject == null) {
            throw new NotFoundException("Object " + accountIdValue + " with class " + objectClass
                    + "not found on resource " + resourceName);
        }

        final Set<Attribute> attributes = connectorObject.getAttributes();
        if (AttributeUtil.find(Uid.NAME, attributes) == null) {
            attributes.add(connectorObject.getUid());
        }
        if (AttributeUtil.find(Name.NAME, attributes) == null) {
            attributes.add(connectorObject.getName());
        }

        auditManager.audit(Category.resource, ResourceSubCategory.getObject, Result.success,
                "Successfully read object " + accountIdValue + " with class " + objectClass
                + " from resource " + resourceName);

        return connObjectUtil.getConnObjectTO(connectorObject);
    }

    @PreAuthorize("hasRole('CONNECTOR_READ')")
    @Transactional(readOnly = true)
    public boolean check(final ResourceTO resourceTO) {
        final ConnInstance connInstance = binder.getConnInstance(resourceTO);

        final Connector connector = connFactory.createConnector(connInstance, connInstance.getConfiguration());

        boolean result;
        try {
            connector.test();
            result = true;

            auditManager.audit(Category.connector, AuditElements.ConnectorSubCategory.check, Result.success,
                    "Successfully checked connector: " + resourceTO);
        } catch (Exception e) {
            auditManager.audit(Category.connector, AuditElements.ConnectorSubCategory.check, Result.failure,
                    "Unsuccessful check for connector: " + resourceTO, e);

            LOG.error("Test connection failure {}", e);
            result = false;
        }

        return result;
    }

    @PreAuthorize("hasRole('RESOURCE_DELETE') and #bulkAction.operation == #bulkAction.operation.DELETE")
    public BulkActionRes bulk(final BulkAction bulkAction) {
        LOG.debug("Bulk '{}' called on '{}'", bulkAction.getOperation(), bulkAction.getTargets());

        BulkActionRes res = new BulkActionRes();

        if (bulkAction.getOperation() == BulkAction.Type.DELETE) {
            for (String name : bulkAction.getTargets()) {
                try {
                    res.add(delete(name).getName(), BulkActionRes.Status.SUCCESS);
                } catch (Exception e) {
                    LOG.error("Error performing delete for resource {}", name, e);
                    res.add(name, BulkActionRes.Status.FAILURE);
                }
            }
        }

        return res;
    }
}
