
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

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.to.ResourceTOs;
import org.syncope.client.to.SchemaMappingTOs;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.Resource;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.dao.ConnectorInstanceDAO;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.SchemaMappingDAO;
import org.syncope.core.persistence.dao.SyncopeRoleDAO;
import org.syncope.core.rest.data.ResourceDataBinder;
import org.syncope.types.SyncopeClientExceptionType;

@Controller
@RequestMapping("/resource")
public class ResourceController extends AbstractController {

    @Autowired
    private ResourceDAO resourceDAO;

    @Autowired
    private SchemaDAO schemaDAO;

    @Autowired
    private SyncopeRoleDAO syncopeRoleDAO;

    @Autowired
    private SchemaMappingDAO schemaMappingDAO;

    @Autowired
    ConnectorInstanceDAO connectorInstanceDAO;

    @Transactional
    @RequestMapping(method = RequestMethod.POST,
    value = "/create")
    public ResourceTO create(HttpServletResponse response,
            @RequestBody ResourceTO resourceTO) throws IOException {

        if (log.isDebugEnabled()) {
            log.debug("Creation request received");
        }

        if (resourceTO == null) {
            if (log.isErrorEnabled()) {
                log.error("Missing resource.");
            }

            return throwNotFoundException("Resource not found", response);
        }

        ResourceDataBinder binder =
                new ResourceDataBinder(schemaDAO, connectorInstanceDAO);

        Resource actual = null;

        try {
            if (log.isDebugEnabled()) {
                log.debug("Verify that resource dosn't exist");
            }

            Resource resource = null;

            if (resourceDAO.find(resourceTO.getName()) != null) {
                SyncopeClientException ex = new SyncopeClientException(
                        SyncopeClientExceptionType.AlreadyExists);

                ex.addElement(resourceTO.getName());

                throw ex;
            }

            if (log.isDebugEnabled()) {
                log.debug("Resource data binder ..");
            }

            resource = binder.getResource(resourceTO);

            if (log.isInfoEnabled()) {
                log.info("Create resource " + resource.getName());
            }

            actual = resourceDAO.save(resource);

            if (actual == null) {
                if (log.isErrorEnabled()) {
                    log.error("Resource creation failed");
                }

                SyncopeClientException ex = new SyncopeClientException(
                        SyncopeClientExceptionType.Unknown);

                throw ex;
            }

        } catch (SyncopeClientCompositeErrorException e) {

            return throwCompositeException(e, response);

        } catch (SyncopeClientException ex) {

            SyncopeClientCompositeErrorException compositeErrorException =
                    new SyncopeClientCompositeErrorException(
                    HttpStatus.BAD_REQUEST);

            compositeErrorException.addException(ex);

            return throwCompositeException(compositeErrorException, response);

        } catch (Throwable t) {

            if (log.isErrorEnabled()) {
                log.error("Unknown exception", t);
            }

            SyncopeClientException ex = new SyncopeClientException(
                    SyncopeClientExceptionType.Unknown);

            SyncopeClientCompositeErrorException compositeErrorException =
                    new SyncopeClientCompositeErrorException(
                    HttpStatus.BAD_REQUEST);

            compositeErrorException.addException(ex);

            return throwCompositeException(compositeErrorException, response);
        }

        response.setStatus(HttpServletResponse.SC_CREATED);
        return binder.getResourceTO(actual);
    }

    @Transactional
    @RequestMapping(method = RequestMethod.POST,
    value = "/update")
    public ResourceTO update(HttpServletResponse response,
            @RequestBody ResourceTO resourceTO) throws IOException {

        if (log.isDebugEnabled()) {
            log.debug("Update request received");
        }

        Resource resource = null;

        if (resourceTO != null && resourceTO.getName() != null) {
            resource = resourceDAO.find(resourceTO.getName());
        }

        if (resource == null) {
            if (log.isErrorEnabled()) {
                log.error("Missing resource.");
            }

            return throwNotFoundException("Resource not found", response);
        }

        ResourceDataBinder binder =
                new ResourceDataBinder(schemaDAO, connectorInstanceDAO);

        Resource actual = null;

        try {
            if (log.isDebugEnabled()) {
                log.debug("Resource data binder ..");
            }

            resource = binder.getResource(resource, resourceTO);

            if (log.isInfoEnabled()) {
                log.info("Update resource " + resource.getName());
            }

            actual = resourceDAO.save(resource);

            if (actual == null) {
                if (log.isErrorEnabled()) {
                    log.error("Resource creation failed");
                }

                SyncopeClientException ex = new SyncopeClientException(
                        SyncopeClientExceptionType.Unknown);

                throw ex;
            }

            // remove older mappings
            Set<SchemaMapping> mappings = resource.getMappings();
            for (SchemaMapping mapping : mappings) {
                mapping.setResource(null);
                schemaMappingDAO.delete(mapping.getId());
            }

        } catch (SyncopeClientCompositeErrorException e) {

            if (log.isErrorEnabled()) {
                log.error("Could not create mappings", e);
            }

            return throwCompositeException(e, response);

        } catch (SyncopeClientException ex) {

            SyncopeClientCompositeErrorException compositeErrorException =
                    new SyncopeClientCompositeErrorException(
                    HttpStatus.BAD_REQUEST);

            compositeErrorException.addException(ex);

            return throwCompositeException(compositeErrorException, response);

        } catch (Throwable t) {

            if (log.isErrorEnabled()) {
                log.error("Unknown exception", t);
            }

            SyncopeClientException ex = new SyncopeClientException(
                    SyncopeClientExceptionType.Unknown);

            SyncopeClientCompositeErrorException compositeErrorException =
                    new SyncopeClientCompositeErrorException(
                    HttpStatus.BAD_REQUEST);

            compositeErrorException.addException(ex);

            return throwCompositeException(compositeErrorException, response);
        }

        response.setStatus(HttpServletResponse.SC_CREATED);
        return binder.getResourceTO(actual);
    }

    @Transactional
    @RequestMapping(method = RequestMethod.DELETE,
    value = "/delete/{resourceName}")
    public void delete(HttpServletResponse response,
            @PathVariable("resourceName") String resourceName)
            throws IOException {

        Resource resource = resourceDAO.find(resourceName);

        if (resource == null) {

            if (log.isErrorEnabled()) {
                log.error("Could not find resource '" + resourceName + "'");
            }

            throwNotFoundException(String.valueOf(resourceName), response);

        } else {

            resourceDAO.delete(resourceName);

        }
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/read/{resourceName}")
    public ResourceTO read(HttpServletResponse response,
            @PathVariable("resourceName") String resourceName)
            throws IOException {

        ResourceDataBinder binder =
                new ResourceDataBinder(schemaDAO, connectorInstanceDAO);

        Resource resource = resourceDAO.find(resourceName);

        if (resource == null) {

            if (log.isErrorEnabled()) {
                log.error("Could not find resource '" + resourceName + "'");
            }

            return throwNotFoundException(resourceName, response);
        }

        return binder.getResourceTO(resource);
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/list")
    public ResourceTOs list(HttpServletResponse response)
            throws IOException {

        ResourceDataBinder binder =
                new ResourceDataBinder(schemaDAO, connectorInstanceDAO);

        List<Resource> resources = resourceDAO.findAll();

        if (resources == null) {

            if (log.isErrorEnabled()) {
                log.error("No resource found");
            }

            return throwNotFoundException("No resource found", response);
        }

        return binder.getResourceTOs(resources);
    }

    @Transactional
    @RequestMapping(method = RequestMethod.POST,
    value = "/{resourceName}/mappings/create")
    public SchemaMappingTOs createMappings(HttpServletResponse response,
            @PathVariable("resourceName") String resourceName,
            @RequestBody SchemaMappingTOs mappings) throws IOException {

        Set<SchemaMapping> actuals = new HashSet<SchemaMapping>();

        ResourceDataBinder binder =
                new ResourceDataBinder(schemaDAO, connectorInstanceDAO);

        try {

            Resource resource = null;
            if (resourceName != null) {
                resource = resourceDAO.find(resourceName);
            }

            if (resource == null) {
                if (log.isErrorEnabled()) {
                    log.error("Missing resource.");
                }

                SyncopeClientException ex = new SyncopeClientException(
                        SyncopeClientExceptionType.RequiredValueMissing);

                ex.addElement("resource");

                throw ex;
            }

            if (mappings == null || mappings.getMappings().size() == 0) {
                if (log.isErrorEnabled()) {
                    log.error("Missing mapping.");
                }

                SyncopeClientException ex = new SyncopeClientException(
                        SyncopeClientExceptionType.RequiredValueMissing);

                ex.addElement("mappings");

                throw ex;
            }

            // resource.getMappings() can never return a null value
            Set<SchemaMapping> existentMappings = resource.getMappings();

            for (SchemaMapping mapping : existentMappings) {
                schemaMappingDAO.delete(mapping.getId());
            }

            // to be sure ...
            resource.getMappings().clear();

            Set<SchemaMapping> schemaMappings =
                    binder.getSchemaMappings(resource, mappings);

            SchemaMapping actual = null;

            for (SchemaMapping schemaMapping : schemaMappings) {
                actual = schemaMappingDAO.save(schemaMapping);
                actuals.add(actual);
            }

        } catch (SyncopeClientCompositeErrorException e) {

            if (log.isErrorEnabled()) {
                log.error("Could not create mappings", e);
            }

            return throwCompositeException(e, response);

        } catch (SyncopeClientException ex) {

            SyncopeClientCompositeErrorException compositeErrorException =
                    new SyncopeClientCompositeErrorException(
                    HttpStatus.BAD_REQUEST);

            compositeErrorException.addException(ex);

            return throwCompositeException(compositeErrorException, response);

        } catch (Throwable t) {

            if (log.isErrorEnabled()) {
                log.error("Unknown exception", t);
            }

            SyncopeClientException ex = new SyncopeClientException(
                    SyncopeClientExceptionType.Unknown);

            SyncopeClientCompositeErrorException compositeErrorException =
                    new SyncopeClientCompositeErrorException(
                    HttpStatus.BAD_REQUEST);

            compositeErrorException.addException(ex);

            return throwCompositeException(compositeErrorException, response);
        }

        response.setStatus(HttpServletResponse.SC_CREATED);
        return binder.getSchemaMappingTOs(actuals);
    }

    @Transactional
    @RequestMapping(method = RequestMethod.DELETE,
    value = "/{resourceName}/mappings/delete")
    public void deleteMappings(HttpServletResponse response,
            @PathVariable("resourceName") String resourceName)
            throws IOException {

        Resource resource = resourceDAO.find(resourceName);

        if (resource == null) {

            if (log.isErrorEnabled()) {
                log.error("Could not find resource '" + resourceName + "'");
            }

            throwNotFoundException(resourceName, response);

        } else {

            Set<SchemaMapping> mappings = resource.getMappings();

            // resource.getMappings() can never return a null value

            for (SchemaMapping mapping : mappings) {
                schemaMappingDAO.delete(mapping.getId());
            }

            // to be sure ...
            resource.getMappings().clear();
        }
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/{resourceName}/mappings/list")
    public SchemaMappingTOs getResourceMapping(HttpServletResponse response,
            @PathVariable("resourceName") String resourceName)
            throws IOException {

        Resource resource = null;
        if (resourceName != null) {
            resource = resourceDAO.find(resourceName);
        }

        if (resource == null) {
            if (log.isErrorEnabled()) {
                log.error("Resource " + resourceName + " not found.");
            }

            SyncopeClientCompositeErrorException compositeErrorException =
                    new SyncopeClientCompositeErrorException(
                    HttpStatus.BAD_REQUEST);

            SyncopeClientException ex = new SyncopeClientException(
                    SyncopeClientExceptionType.RequiredValueMissing);

            ex.addElement("resource");

            compositeErrorException.addException(ex);

            return throwCompositeException(compositeErrorException, response);
        }


        Set<SchemaMapping> schemaMappings = resource.getMappings();

        // resource.getMappings() can never return a null value

        ResourceDataBinder binder =
                new ResourceDataBinder(schemaDAO, connectorInstanceDAO);

        return binder.getSchemaMappingTOs(schemaMappings);
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/{roleName}/resources/mappings/list")
    public SchemaMappingTOs getRoleResourcesMapping(HttpServletResponse response,
            @PathVariable("roleName") Long roleId)
            throws IOException {

        SyncopeRole role = null;
        if (roleId != null) {
            role = syncopeRoleDAO.find(roleId);
        }

        if (role == null) {
            if (log.isErrorEnabled()) {
                log.error("Role " + roleId + " not found.");
            }

            SyncopeClientCompositeErrorException compositeErrorException =
                    new SyncopeClientCompositeErrorException(
                    HttpStatus.BAD_REQUEST);

            SyncopeClientException ex = new SyncopeClientException(
                    SyncopeClientExceptionType.RequiredValueMissing);

            ex.addElement("resource");

            compositeErrorException.addException(ex);

            return throwCompositeException(compositeErrorException, response);
        }

        SchemaMappingTOs roleMappings = new SchemaMappingTOs();

        Set<Resource> resources = role.getResources();

        ResourceDataBinder binder =
                new ResourceDataBinder(schemaDAO, connectorInstanceDAO);

        SchemaMappingTOs resourceMappings = null;

        for (Resource resource : resources) {
            if (log.isDebugEnabled()) {
                log.debug("Ask for the mappings of '" + resource + "'");
            }

            Set<SchemaMapping> schemaMappings = resource.getMappings();

            if (log.isDebugEnabled()) {
                log.debug("The mappings of '" + resource + "' are '" +
                        schemaMappings + "'");
            }

            resourceMappings = binder.getSchemaMappingTOs(schemaMappings);

            if (log.isDebugEnabled()) {
                log.debug("The mappings TO of '" + resource + "' are '" +
                        resourceMappings.getMappings() + "'");
            }

            roleMappings.getMappings().addAll(resourceMappings.getMappings());
        }

        if (log.isDebugEnabled()) {
            log.debug("Mappings found: " + roleMappings.getMappings());
        }

        return roleMappings;
    }
}
