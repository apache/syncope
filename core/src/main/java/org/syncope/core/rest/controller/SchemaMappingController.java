
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
import java.util.Set;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.syncope.client.to.SchemaMappingTOs;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.Resource;
import org.syncope.core.persistence.beans.SchemaMapping;
import org.syncope.core.persistence.beans.role.RoleSchema;
import org.syncope.core.persistence.beans.role.SyncopeRole;
import org.syncope.core.persistence.beans.user.UserSchema;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.persistence.dao.SchemaDAO;
import org.syncope.core.persistence.dao.SchemaMappingDAO;
import org.syncope.core.persistence.dao.SyncopeRoleDAO;
import org.syncope.core.rest.data.SchemaMappingDataBinder;
import org.syncope.types.SyncopeClientExceptionType;

@Controller
@RequestMapping("/mapping")
public class SchemaMappingController extends AbstractController {

    @Autowired
    private ResourceDAO resourceDAO;

    @Autowired
    private SchemaDAO schemaDAO;

    @Autowired
    private SyncopeRoleDAO syncopeRoleDAO;

    @Autowired
    private SchemaMappingDAO schemaMappingDAO;

    @Transactional
    @RequestMapping(method = RequestMethod.POST,
    value = "/create/{resourceName}")
    public SchemaMappingTOs create(HttpServletResponse response,
            @PathVariable("resourceName") String resourceName,
            @RequestBody SchemaMappingTOs mappings) throws IOException {

        Set<SchemaMapping> actuals = new HashSet<SchemaMapping>();

        SchemaMappingDataBinder binder =
                new SchemaMappingDataBinder(schemaDAO);

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
            UserSchema userSchema = null;
            RoleSchema roleSchema = null;

            for (SchemaMapping schemaMapping : schemaMappings) {
                resource.addMapping(schemaMapping);

                // synchronize userSchema
                userSchema = schemaMapping.getUserSchema();
                if (userSchema != null) userSchema.addMapping(schemaMapping);

                // synchronize roleSchema
                roleSchema = schemaMapping.getRoleSchema();
                if (roleSchema != null) roleSchema.addMapping(schemaMapping);

                // save schema mapping and synchronize
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
    value = "/delete/{resourceName}")
    public void delete(HttpServletResponse response,
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
    value = "/getResourceMapping/{resourceName}")
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

        SchemaMappingDataBinder binder = new SchemaMappingDataBinder(schemaDAO);

        return binder.getSchemaMappingTOs(schemaMappings);
    }

    @RequestMapping(method = RequestMethod.GET,
    value = "/getRoleResourcesMapping/{roleName}")
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

        SchemaMappingDataBinder binder = new SchemaMappingDataBinder(schemaDAO);

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

            roleMappings.addAllMappings(resourceMappings.getMappings());
        }

        if (log.isDebugEnabled()) {
            log.debug("Mappings found: " +
                    roleMappings.getMappings());
        }

        return roleMappings;
    }
}
