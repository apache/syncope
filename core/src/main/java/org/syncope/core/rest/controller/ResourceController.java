
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
import java.util.List;
import javassist.NotFoundException;
import javax.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.syncope.client.to.ResourceTO;
import org.syncope.client.to.ResourceTOs;
import org.syncope.client.validation.SyncopeClientCompositeErrorException;
import org.syncope.client.validation.SyncopeClientException;
import org.syncope.core.persistence.beans.TargetResource;
import org.syncope.core.persistence.dao.ResourceDAO;
import org.syncope.core.rest.data.ResourceDataBinder;
import org.syncope.types.SyncopeClientExceptionType;

@Controller
@RequestMapping("/resource")
public class ResourceController extends AbstractController {

    @Autowired
    private ResourceDAO resourceDAO;
    @Autowired
    private ResourceDataBinder binder;

    @RequestMapping(method = RequestMethod.POST,
    value = "/create")
    public ResourceTO create(HttpServletResponse response,
            @RequestBody ResourceTO resourceTO)
            throws SyncopeClientCompositeErrorException, NotFoundException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Creation request received");
        }

        SyncopeClientCompositeErrorException compositeErrorException =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        if (resourceTO == null) {
            LOG.error("Missing resource");

            throw new NotFoundException("Missing resource");
        }

        TargetResource resource = null;

        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Verify that resource dosn't exist");
            }

            if (resourceDAO.find(resourceTO.getName()) != null) {
                SyncopeClientException ex = new SyncopeClientException(
                        SyncopeClientExceptionType.AlreadyExists);

                ex.addElement(resourceTO.getName());
                compositeErrorException.addException(ex);

                throw compositeErrorException;
            }

            if (LOG.isDebugEnabled()) {
                LOG.debug("Resource data binder ..");
            }

            resource = binder.getResource(resourceTO);
            if (resource == null) {
                LOG.error("Resource creation failed");

                SyncopeClientException ex = new SyncopeClientException(
                        SyncopeClientExceptionType.Unknown);

                compositeErrorException.addException(ex);

                throw compositeErrorException;
            }
        } catch (Throwable t) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Unknown exception", t);
            }

            SyncopeClientException ex = new SyncopeClientException(
                    SyncopeClientExceptionType.Unknown);

            compositeErrorException.addException(ex);

            throw compositeErrorException;
        }

        resource = resourceDAO.save(resource);

        response.setStatus(HttpServletResponse.SC_CREATED);
        return binder.getResourceTO(resource);
    }

    @RequestMapping(method = RequestMethod.POST,
    value = "/update")
    public ResourceTO update(HttpServletResponse response,
            @RequestBody ResourceTO resourceTO)
            throws SyncopeClientCompositeErrorException, NotFoundException {

        if (LOG.isDebugEnabled()) {
            LOG.debug("Update request received");
        }

        TargetResource resource = null;

        if (resourceTO != null && resourceTO.getName() != null) {
            resource = resourceDAO.find(resourceTO.getName());
        }

        if (resource == null) {
            LOG.error("Missing resource");

            throw new NotFoundException(resourceTO.getName());
        }

        SyncopeClientCompositeErrorException compositeErrorException =
                new SyncopeClientCompositeErrorException(
                HttpStatus.BAD_REQUEST);

        try {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Remove old mappings ..");
            }

            // remove older mappings
            resource.getMappings().clear();

            if (LOG.isDebugEnabled()) {
                LOG.debug("Resource data binder ..");
            }

            resource = binder.getResource(resource, resourceTO);
            if (resource == null) {
                if (LOG.isErrorEnabled()) {
                    LOG.error("Resource creation failed");
                }

                SyncopeClientException ex = new SyncopeClientException(
                        SyncopeClientExceptionType.Unknown);

                compositeErrorException.addException(ex);
                throw compositeErrorException;
            }
        } catch (Throwable t) {
            LOG.error("Unknown exception", t);

            SyncopeClientException ex = new SyncopeClientException(
                    SyncopeClientExceptionType.Unknown);

            compositeErrorException.addException(ex);

            throw compositeErrorException;
        }

        resource = resourceDAO.save(resource);
        return binder.getResourceTO(resource);
    }

    @RequestMapping(method = RequestMethod.DELETE,
    value = "/delete/{resourceName}")
    public void delete(HttpServletResponse response,
            @PathVariable("resourceName") String resourceName)
            throws NotFoundException {

        TargetResource resource = resourceDAO.find(resourceName);

        if (resource == null) {
            if (LOG.isErrorEnabled()) {
                LOG.error("Could not find resource '" + resourceName + "'");
            }
            throw new NotFoundException(resourceName);
        } else {
            resourceDAO.delete(resourceName);
        }
    }

    @Transactional(readOnly = true)
    @RequestMapping(method = RequestMethod.GET,
    value = "/read/{resourceName}")
    public ResourceTO read(HttpServletResponse response,
            @PathVariable("resourceName") String resourceName)
            throws NotFoundException {

        TargetResource resource = resourceDAO.find(resourceName);
        if (resource == null) {
            LOG.error("Could not find resource '" + resourceName + "'");

            throw new NotFoundException(resourceName);
        }

        return binder.getResourceTO(resource);
    }

    @Transactional(readOnly = true)
    @RequestMapping(method = RequestMethod.GET,
    value = "/list")
    public ResourceTOs list(HttpServletResponse response)
            throws NotFoundException {

        List<TargetResource> resources = resourceDAO.findAll();

        if (resources == null) {
            LOG.error("No resource found");

            throw new NotFoundException("No resource found");
        }

        return binder.getResourceTOs(resources);
    }
}
