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
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.syncope.client.to.ConnObjectTO;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.to.SchemaMappingTO;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.init.ConnInstanceLoader;
import org.syncope.core.persistence.beans.ExternalResource;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.RoleDAO;
import org.syncope.core.propagation.ConnectorFacadeProxy;
import org.syncope.core.rest.data.ConnInstanceDataBinder;
import org.syncope.core.rest.data.ResourceDataBinder;
import org.syncope.types.SyncopeClientExceptionType;

@Controller
@RequestMapping("/resource")
public class ResourceController extends AbstractController {

    @Autowired
    private ResourceDAO resourceDAO;

    @Autowired
    private RoleDAO roleDAO;

    @Autowired
    private ResourceDataBinder binder;

    @Autowired
    private ConnInstanceDataBinder connInstanceDataBinder;

    @Autowired
    private ConnInstanceLoader connLoader;

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
                    SyncopeClientExceptionType.DataIntegrityViolation);

            ex.addElement("Existing " + resourceTO.getName());
            scce.addException(ex);

            throw scce;
        }

        ExternalResource resource = binder.create(resourceTO);
        if (resource == null) {
            LOG.error("Resource creation failed");

            SyncopeClientException ex = new SyncopeClientException(
                    SyncopeClientExceptionType.Unknown);

            scce.addException(ex);

            throw scce;
        }

        resource = resourceDAO.save(resource);

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

        ExternalResource resource = null;
        if (resourceTO != null && resourceTO.getName() != null) {
            resource = resourceDAO.find(resourceTO.getName());
        }
        if (resource == null) {
            LOG.error("Missing resource: {}", resourceTO.getName());
            throw new NotFoundException(
                    "Resource '" + resourceTO.getName() + "'");
        }

        resource = binder.update(resource, resourceTO);
        if (resource == null) {
            LOG.error("Resource update failed");

            SyncopeClientCompositeErrorException scce =
                    new SyncopeClientCompositeErrorException(
                    HttpStatus.BAD_REQUEST);
            SyncopeClientException ex = new SyncopeClientException(
                    SyncopeClientExceptionType.Unknown);
            scce.addException(ex);
            throw scce;
        }

        resource = resourceDAO.save(resource);

        return binder.getResourceTO(resource);
    }

    @PreAuthorize("hasRole('RESOURCE_DELETE')")
    @RequestMapping(method = RequestMethod.DELETE,
    value = "/delete/{resourceName}")
    public void delete(final HttpServletResponse response,
            final @PathVariable("resourceName") String resourceName)
            throws NotFoundException {

        ExternalResource resource = resourceDAO.find(resourceName);

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

        ExternalResource resource = resourceDAO.find(resourceName);
        if (resource == null) {
            LOG.error("Could not find resource '" + resourceName + "'");
            throw new NotFoundException("Resource '" + resourceName + "'");
        }

        return binder.getResourceTO(resource);
    }

    @Transactional(readOnly = true)
    @RequestMapping(method = RequestMethod.GET,
    value = "/list")
    public List<ResourceTO> list(HttpServletResponse response)
            throws NotFoundException {

        List<ExternalResource> resources = resourceDAO.findAll();
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
            final HttpServletResponse response,
            @PathVariable("roleName") final Long roleId)
            throws SyncopeClientCompositeErrorException {

        SyncopeRole role = null;
        if (roleId != null) {
            role = roleDAO.find(roleId);
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

        for (ExternalResource resource : role.getResources()) {
            roleMappings.addAll(
                    binder.getSchemaMappingTOs(resource.getMappings()));
        }

        LOG.debug("Mappings found: {} ", roleMappings);

        return roleMappings;
    }

    @PreAuthorize("hasRole('RESOURCE_GETOBJECT')")
    @Transactional(readOnly = true)
    @RequestMapping(method = RequestMethod.GET,
    value = "/{resourceName}/read/{objectId}")
    public ConnObjectTO getObject(final HttpServletResponse response,
            @PathVariable("resourceName") String resourceName,
            @PathVariable("objectId") final String objectId)
            throws NotFoundException {

        ExternalResource resource = resourceDAO.find(resourceName);
        if (resource == null) {
            LOG.error("Could not find resource '" + resourceName + "'");
            throw new NotFoundException("Resource '" + resourceName + "'");
        }

        final ConnectorFacadeProxy connector = connLoader.getConnector(resource);

        final ConnectorObject connectorObject =
                connector.getObject(ObjectClass.ACCOUNT, new Uid(objectId), null);

        if (connectorObject == null) {
            throw new NotFoundException(
                    "Object " + objectId + " not found on resource "
                    + resourceName);
        }

        final Set<Attribute> attributes = connectorObject.getAttributes();

        if (AttributeUtil.find(Uid.NAME, attributes) == null) {
            attributes.add(connectorObject.getUid());
        }

        if (AttributeUtil.find(Name.NAME, attributes) == null) {
            attributes.add(connectorObject.getName());
        }

        return connInstanceDataBinder.getConnObjectTO(connectorObject);
    }
}
