/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.core.rest.controller;

import java.util.ArrayList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import java.util.List;
import java.util.Set;
import javassist.NotFoundException;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.to.SchemaMappingTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.RoleDAO;
import org.syncope.core.persistence.validation.entity.InvalidEntityException;
import org.syncope.core.rest.data.ResourceDataBinder;
import org.syncope.types.SyncopeClientExceptionType;

@Controller
@RequestMapping("/resource")
public class ResourceController extends AbstractController {

    @Autowired
    private ResourceDAO resourceDAO;

    @Autowired
    private RoleDAO syncopeRoleDAO;

    @Autowired
    private ResourceDataBinder binder;

    @PreAuthorize("hasRole('RESOURCE_CREATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/create")
    public ResourceTO create(final HttpServletResponse response,
            final @RequestBody ResourceTO resourceTO)
            throws SyncopeClientCompositeErrorException, NotFoundException {

        LOG.debug("Resource creation: {}", resourceTO);

        if (resourceTO == null) {
            LOG.error("Missing resource");

            throw new NotFoundException("Missing resource");
        }

        SyncopeClientCompositeErrorException scce =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        LOG.debug("Verify that resource doesn't exist yet");
        if (resourceTO.getName() != null
                && resourceDAO.find(resourceTO.getName()) != null) {
            SyncopeClientException ex = new SyncopeClientException(
                    SyncopeClientExceptionType.DuplicateUniqueValue);

            ex.addElement(resourceTO.getName());
            scce.addException(ex);

            throw scce;
        }

        TargetResource resource = binder.getResource(resourceTO);
        if (resource == null) {
            LOG.error("Resource creation failed");

            SyncopeClientException ex = new SyncopeClientException(
                    SyncopeClientExceptionType.Unknown);

            scce.addException(ex);

            throw scce;
        }

        try {
            resource = resourceDAO.save(resource);
        } catch (InvalidEntityException e) {
            SyncopeClientException ex = new SyncopeClientException(
                    SyncopeClientExceptionType.InvalidSchemaMapping);
            scce.addException(ex);
            throw scce;
        }

        response.setStatus(HttpServletResponse.SC_CREATED);
        return binder.getResourceTO(resource);
    }

    @PreAuthorize("hasRole('RESOURCE_UPDATE')")
    @RequestMapping(method = RequestMethod.POST,
    value = "/update")
    public ResourceTO update(final HttpServletResponse response,
            final @RequestBody ResourceTO resourceTO)
            throws SyncopeClientCompositeErrorException, NotFoundException {

        LOG.debug("Role update request: {}", resourceTO);

        TargetResource resource = null;
        if (resourceTO != null && resourceTO.getName() != null) {
            resource = resourceDAO.find(resourceTO.getName());
        }
        if (resource == null) {
            LOG.error("Missing resource: " + resourceTO.getName());
            throw new NotFoundException(
                    "Resource '" + resourceTO.getName() + "'");
        }

        SyncopeClientCompositeErrorException scce =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        LOG.debug("Removing old mappings ..");
        // remove old mappings
        resourceDAO.deleteAllMappings(resource);

        resource = binder.getResource(resource, resourceTO);
        if (resource == null) {
            LOG.error("Resource update failed");

            SyncopeClientException ex = new SyncopeClientException(
                    SyncopeClientExceptionType.Unknown);
            scce.addException(ex);
            throw scce;
        }

        try {
            resource = resourceDAO.save(resource);
        } catch (InvalidEntityException e) {
            SyncopeClientException ex = new SyncopeClientException(
                    SyncopeClientExceptionType.InvalidSchemaMapping);
            scce.addException(ex);
            throw scce;
        }

        return binder.getResourceTO(resource);
    }

    @PreAuthorize("hasRole('RESOURCE_DELETE')")
    @RequestMapping(method = RequestMethod.DELETE,
    value = "/delete/{resourceName}")
    public void delete(final HttpServletResponse response,
            final @PathVariable("resourceName") String resourceName)
            throws NotFoundException {

        TargetResource resource = resourceDAO.find(resourceName);

        if (resource == null) {
            LOG.error("Could not find resource '" + resourceName + "'");
            throw new NotFoundException("Resource '" + resourceName + "'");
        }

        resourceDAO.delete(resourceName);
    }

    @PreAuthorize("hasRole('RESOURCE_READ')")
    @Transactional(readOnly = true)
    @RequestMapping(method = RequestMethod.GET,
    value = "/read/{resourceName}")
    public ResourceTO read(final HttpServletResponse response,
            final @PathVariable("resourceName") String resourceName)
            throws NotFoundException {

        TargetResource resource = resourceDAO.find(resourceName);
        if (resource == null) {
            LOG.error("Could not find resource '" + resourceName + "'");
            throw new NotFoundException("Resource '" + resourceName + "'");
        }

        return binder.getResourceTO(resource);
    }

    @PreAuthorize("hasRole('RESOURCE_LIST')")
    @Transactional(readOnly = true)
    @RequestMapping(method = RequestMethod.GET,
    value = "/list")
    public List<ResourceTO> list(HttpServletResponse response)
            throws NotFoundException {

        List<TargetResource> resources = resourceDAO.findAll();
        if (resources == null) {
            LOG.error("No resources found");
            throw new NotFoundException("No resources found");
        }

        return binder.getResourceTOs(resources);
    }

    @PreAuthorize("hasRole('RESOURCE_READ')")
    @RequestMapping(method = RequestMethod.GET,
    value = "/{roleName}/mappings")
    public List<SchemaMappingTO> getRoleResourcesMapping(
            HttpServletResponse response,
            @PathVariable("roleName") Long roleId)
            throws SyncopeClientCompositeErrorException {

        SyncopeRole role = null;
        if (roleId != null) {
            role = syncopeRoleDAO.find(roleId);
        }

        if (role == null) {
            LOG.error("Role " + roleId + " not found.");

            SyncopeClientCompositeErrorException compositeErrorException =
                    new SyncopeClientCompositeErrorException(
                    HttpStatus.BAD_REQUEST);

            SyncopeClientException ex = new SyncopeClientException(
                    SyncopeClientExceptionType.RequiredValuesMissing);

            ex.addElement("resource");

            compositeErrorException.addException(ex);

            throw compositeErrorException;
        }

        List<SchemaMappingTO> roleMappings = new ArrayList<SchemaMappingTO>();

        Set<TargetResource> resources = role.getTargetResources();

        List<SchemaMappingTO> resourceMappings;
        for (TargetResource resource : resources) {
            LOG.debug("Ask for the mappings of {}", resource);

            List<SchemaMapping> schemaMappings = resource.getMappings();
            LOG.debug("The mappings of {} are {}",
                    resource, schemaMappings);

            resourceMappings = binder.getSchemaMappingTOs(schemaMappings);
            LOG.debug("The mappings TO of {} are {}",
                    resource, resourceMappings);

            roleMappings.addAll(resourceMappings);
        }

        LOG.debug("Mappings found: {} ", roleMappings);

        return roleMappings;
    }
}
