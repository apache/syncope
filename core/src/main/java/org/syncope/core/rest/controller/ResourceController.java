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
package org.syncope.core.rest.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javassist.NotFoundException;
import javax.servlet.http.HttpServletResponse;
import org.identityconnectors.framework.common.objects.Attribute;
import org.identityconnectors.framework.common.objects.AttributeUtil;
import org.identityconnectors.framework.common.objects.ConnectorObject;
import org.identityconnectors.framework.common.objects.Name;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.identityconnectors.framework.common.objects.Uid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.syncope.client.to.ConnObjectTO;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.to.SchemaMappingTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.core.audit.AuditManager;
import org.syncope.core.init.ConnInstanceLoader;
import org.syncope.core.persistence.beans.ConnInstance;
import org.syncope.core.persistence.beans.ExternalResource;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.dao.ConnInstanceDAO;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.RoleDAO;
import org.syncope.core.propagation.ConnectorFacadeProxy;
import org.syncope.core.rest.data.ResourceDataBinder;
import org.syncope.core.util.ConnObjectUtil;
import org.syncope.types.AuditElements.Category;
import org.syncope.types.AuditElements.ResourceSubCategory;
import org.syncope.types.AuditElements.Result;

@Controller
@RequestMapping("/resource")
public class ResourceController extends AbstractController {

    @Autowired
    private AuditManager auditManager;

    @Autowired
    private ResourceDAO resourceDAO;

    @Autowired
    private ConnInstanceDAO connInstanceDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private ResourceDataBinder binder;

    /**
     * ConnectorObject util.
     */
    @Autowired
    private ConnObjectUtil connObjectUtil;

    @Autowired
    private ConnInstanceLoader connLoader;

    @PreAuthorize("hasRole('RESOURCE_CREATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/create")
    public ResourceTO create(final HttpServletResponse response, @RequestBody final ResourceTO resourceTO)
            throws SyncopeClientCompositeErrorException, NotFoundException {

        LOG.debug("Resource creation: {}", resourceTO);

        ExternalResource resource = resourceDAO.save(binder.create(resourceTO));

        auditManager.audit(Category.resource, ResourceSubCategory.create, Result.success,
                "Successfully created resource: " + resource.getName());

        response.setStatus(HttpServletResponse.SC_CREATED);
        return binder.getResourceTO(resource);
    }

    @PreAuthorize("hasRole('RESOURCE_UPDATE')")
    @RequestMapping(method = RequestMethod.POST, value = "/update")
    public ResourceTO update(@RequestBody final ResourceTO resourceTO)
            throws SyncopeClientCompositeErrorException, NotFoundException {

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
    @RequestMapping(method = RequestMethod.DELETE, value = "/delete/{resourceName}")
    public void delete(@PathVariable("resourceName") final String resourceName)
            throws NotFoundException {

        ExternalResource resource = resourceDAO.find(resourceName);
        if (resource == null) {
            throw new NotFoundException("Resource '" + resourceName + "'");
        }

        auditManager.audit(Category.resource, ResourceSubCategory.delete, Result.success,
                "Successfully deleted resource: " + resource.getName());

        resourceDAO.delete(resourceName);
    }

    @PreAuthorize("hasRole('RESOURCE_READ')")
    @Transactional(readOnly = true)
    @RequestMapping(method = RequestMethod.GET, value = "/read/{resourceName}")
    public ResourceTO read(@PathVariable("resourceName") final String resourceName)
            throws NotFoundException {

        ExternalResource resource = resourceDAO.find(resourceName);
        if (resource == null) {
            throw new NotFoundException("Resource '" + resourceName + "'");
        }

        auditManager.audit(Category.resource, ResourceSubCategory.read, Result.success,
                "Successfully read resource: " + resource.getName());

        return binder.getResourceTO(resource);
    }

    @Transactional(readOnly = true)
    @RequestMapping(method = RequestMethod.GET, value = "/list")
    public List<ResourceTO> list(@RequestParam(required = false, value = "connInstanceId") final Long connInstanceId)
            throws NotFoundException {

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

    @PreAuthorize("hasRole('RESOURCE_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/{roleId}/mappings")
    public List<SchemaMappingTO> getRoleResourcesMapping(@PathVariable("roleId") final Long roleId)
            throws NotFoundException {

        SyncopeRole role = roleDAO.find(roleId);
        if (role == null) {
            throw new NotFoundException("Role '" + roleId + "'");
        }

        List<SchemaMappingTO> roleMappings = new ArrayList<SchemaMappingTO>();

        for (ExternalResource resource : role.getResources()) {
            roleMappings.addAll(binder.getSchemaMappingTOs(resource.getMappings()));
        }

        auditManager.audit(Category.resource, ResourceSubCategory.getRoleResourcesMapping, Result.success,
                "Found " + roleMappings.size() + " mappings for role " + roleId);

        return roleMappings;
    }

    @PreAuthorize("hasRole('RESOURCE_GETOBJECT')")
    @Transactional(readOnly = true)
    @RequestMapping(method = RequestMethod.GET, value = "/{resourceName}/read/{objectId}")
    public ConnObjectTO getObject(@PathVariable("resourceName") final String resourceName,
            @PathVariable("objectId") final String objectId)
            throws NotFoundException {

        ExternalResource resource = resourceDAO.find(resourceName);
        if (resource == null) {
            throw new NotFoundException("Resource '" + resourceName + "'");
        }

        final ConnectorFacadeProxy connector = connLoader.getConnector(resource);

        final ConnectorObject connectorObject = connector.getObject(ObjectClass.ACCOUNT, new Uid(objectId), connector.
                getOperationOptions(resource));

        if (connectorObject == null) {
            throw new NotFoundException("Object " + objectId + " not found on resource " + resourceName);
        }

        final Set<Attribute> attributes = connectorObject.getAttributes();

        if (AttributeUtil.find(Uid.NAME, attributes) == null) {
            attributes.add(connectorObject.getUid());
        }

        if (AttributeUtil.find(Name.NAME, attributes) == null) {
            attributes.add(connectorObject.getName());
        }

        auditManager.audit(Category.resource, ResourceSubCategory.getObject, Result.success,
                "Successfully read object " + objectId + " from resource " + resourceName);

        return connObjectUtil.getConnObjectTO(connectorObject);
    }
}
