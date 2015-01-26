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
package org.apache.syncope.server.logic;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.lib.types.MappingPurpose;
import org.apache.syncope.common.lib.types.SubjectType;
import org.apache.syncope.server.persistence.api.dao.ConnInstanceDAO;
import org.apache.syncope.server.persistence.api.dao.DuplicateException;
import org.apache.syncope.server.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.server.persistence.api.dao.NotFoundException;
import org.apache.syncope.server.persistence.api.dao.RoleDAO;
import org.apache.syncope.server.persistence.api.dao.UserDAO;
import org.apache.syncope.server.persistence.api.entity.AttributableUtil;
import org.apache.syncope.server.persistence.api.entity.AttributableUtilFactory;
import org.apache.syncope.server.persistence.api.entity.ConnInstance;
import org.apache.syncope.server.persistence.api.entity.ExternalResource;
import org.apache.syncope.server.persistence.api.entity.MappingItem;
import org.apache.syncope.server.persistence.api.entity.Subject;
import org.apache.syncope.server.provisioning.api.Connector;
import org.apache.syncope.server.provisioning.api.ConnectorFactory;
import org.apache.syncope.server.provisioning.api.data.ResourceDataBinder;
import org.apache.syncope.server.misc.ConnObjectUtil;
import org.apache.syncope.server.misc.MappingUtil;
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
public class ResourceLogic extends AbstractTransactionalLogic<ResourceTO> {

    @Autowired
    private ExternalResourceDAO resourceDAO;

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private ResourceDataBinder binder;

    @Autowired
    private ConnObjectUtil connObjectUtil;

    @Autowired
    private ConnectorFactory connFactory;

    @Autowired
    private AttributableUtilFactory attrUtilFactory;

    @PreAuthorize("hasRole('RESOURCE_CREATE')")
    public ResourceTO create(final ResourceTO resourceTO) {
        if (StringUtils.isBlank(resourceTO.getKey())) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.RequiredValuesMissing);
            sce.getElements().add("Resource name");
            throw sce;
        }

        if (resourceDAO.find(resourceTO.getKey()) != null) {
            throw new DuplicateException("Resource '" + resourceTO.getKey() + "'");
        }

        ExternalResource resource = null;
        try {
            resource = resourceDAO.save(binder.create(resourceTO));
        } catch (SyncopeClientException e) {
            throw e;
        } catch (Exception e) {
            SyncopeClientException ex = SyncopeClientException.build(ClientExceptionType.InvalidExternalResource);
            ex.getElements().add(e.getMessage());
            throw ex;
        }

        return binder.getResourceTO(resource);
    }

    @PreAuthorize("hasRole('RESOURCE_UPDATE')")
    public ResourceTO update(final ResourceTO resourceTO) {
        ExternalResource resource = resourceDAO.find(resourceTO.getKey());
        if (resource == null) {
            throw new NotFoundException("Resource '" + resourceTO.getKey() + "'");
        }

        resource = binder.update(resource, resourceTO);
        try {
            resource = resourceDAO.save(resource);
        } catch (SyncopeClientException e) {
            throw e;
        } catch (Exception e) {
            SyncopeClientException ex = SyncopeClientException.build(ClientExceptionType.InvalidExternalResource);
            ex.getElements().add(e.getMessage());
            throw ex;
        }

        return binder.getResourceTO(resource);
    }

    @PreAuthorize("hasRole('RESOURCE_DELETE')")
    public ResourceTO delete(final String resourceName) {
        ExternalResource resource = resourceDAO.find(resourceName);
        if (resource == null) {
            throw new NotFoundException("Resource '" + resourceName + "'");
        }

        ResourceTO resourceToDelete = binder.getResourceTO(resource);

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

        return binder.getResourceTO(resource);
    }

    @PreAuthorize("isAuthenticated()")
    @Transactional(readOnly = true)
    public List<ResourceTO> list(final Long connInstanceId) {
        List<? extends ExternalResource> resources;

        if (connInstanceId == null) {
            resources = resourceDAO.findAll();
        } else {
            ConnInstance connInstance = connInstanceDAO.find(connInstanceId);
            resources = connInstance.getResources();
        }

        return binder.getResourceTOs(resources);
    }

    @PreAuthorize("hasRole('RESOURCE_GETCONNECTOROBJECT')")
    @Transactional(readOnly = true)
    public ConnObjectTO getConnectorObject(final String resourceName, final SubjectType type, final Long id) {
        ExternalResource resource = resourceDAO.find(resourceName);
        if (resource == null) {
            throw new NotFoundException("Resource '" + resourceName + "'");
        }

        Subject<?, ?, ?> subject = type == SubjectType.USER
                ? userDAO.find(id)
                : roleDAO.find(id);
        if (subject == null) {
            throw new NotFoundException(type + " " + id);
        }

        final AttributableUtil attrUtil = attrUtilFactory.getInstance(type.asAttributableType());

        MappingItem accountIdItem = attrUtil.getAccountIdItem(resource);
        if (accountIdItem == null) {
            throw new NotFoundException("AccountId mapping for " + type + " " + id + " on resource '" + resourceName
                    + "'");
        }
        final String accountIdValue = MappingUtil.getAccountIdValue(
                subject, resource, attrUtil.getAccountIdItem(resource));

        final ObjectClass objectClass = SubjectType.USER == type ? ObjectClass.ACCOUNT : ObjectClass.GROUP;

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
        } catch (Exception e) {
            LOG.error("Test connection failure {}", e);
            result = false;
        }

        return result;
    }

    @PreAuthorize("hasRole('RESOURCE_DELETE') and #bulkAction.operation == #bulkAction.operation.DELETE")
    public BulkActionResult bulk(final BulkAction bulkAction) {
        BulkActionResult res = new BulkActionResult();

        if (bulkAction.getOperation() == BulkAction.Type.DELETE) {
            for (String name : bulkAction.getTargets()) {
                try {
                    res.add(delete(name).getKey(), BulkActionResult.Status.SUCCESS);
                } catch (Exception e) {
                    LOG.error("Error performing delete for resource {}", name, e);
                    res.add(name, BulkActionResult.Status.FAILURE);
                }
            }
        }

        return res;
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
