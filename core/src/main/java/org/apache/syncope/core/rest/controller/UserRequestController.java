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

import java.util.ArrayList;
import java.util.List;
import javax.persistence.RollbackException;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.persistence.beans.SyncopeConf;
import org.apache.syncope.core.persistence.beans.UserRequest;
import org.apache.syncope.core.persistence.dao.ConfDAO;
import org.apache.syncope.core.persistence.dao.UserRequestDAO;
import org.apache.syncope.core.rest.data.UserRequestDataBinder;
import org.apache.syncope.core.util.NotFoundException;
import org.apache.syncope.mod.UserMod;
import org.apache.syncope.to.UserRequestTO;
import org.apache.syncope.to.UserTO;
import org.apache.syncope.types.AuditElements.Category;
import org.apache.syncope.types.AuditElements.Result;
import org.apache.syncope.types.AuditElements.UserRequestSubCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/user/request")
public class UserRequestController {

    /**
     * Logger.
     */
    private static final Logger LOG = LoggerFactory.getLogger(UserRequestController.class);

    @Autowired
    private AuditManager auditManager;

    @Autowired
    private ConfDAO confDAO;

    @Autowired
    private UserRequestDAO userRequestDAO;

    @Autowired
    private UserRequestDataBinder dataBinder;

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(method = RequestMethod.GET, value = "/read/self")
    @Transactional(readOnly = true)
    public UserTO read() throws NotFoundException {

        UserTO userTO = dataBinder.getAuthUserTO();

        auditManager.audit(Category.userRequest, UserRequestSubCategory.read, Result.success,
                "Successfully read self data");

        return userTO;
    }

    private Boolean isCreateAllowedByConf() {
        SyncopeConf createRequestAllowed = confDAO.find("createRequest.allowed", "false");

        return Boolean.valueOf(createRequestAllowed.getValue());
    }

    @RequestMapping(method = RequestMethod.GET, value = "/create/allowed")
    @Transactional(readOnly = true)
    public ModelAndView isCreateAllowed() {

        auditManager.audit(Category.userRequest, UserRequestSubCategory.isCreateAllowed, Result.success,
                "Successfully checked whether self create is allowed");

        return new ModelAndView().addObject(isCreateAllowedByConf());
    }

    @RequestMapping(method = RequestMethod.POST, value = "/create")
    public UserRequestTO create(@RequestBody final UserTO userTO) throws UnauthorizedRoleException {

        if (!isCreateAllowedByConf()) {
            LOG.error("Create requests are not allowed");

            throw new UnauthorizedRoleException(-1L);
        }

        LOG.debug("Request user create called with {}", userTO);

        try {
            dataBinder.testCreate(userTO);
        } catch (RollbackException e) {
            LOG.debug("Testing create - ignore exception");
        }

        UserRequest request = new UserRequest();
        request.setUserTO(userTO);
        request = userRequestDAO.save(request);

        auditManager.audit(Category.userRequest, UserRequestSubCategory.create, Result.success,
                "Successfully created user request for " + request.getUserTO().getUsername());

        return dataBinder.getUserRequestTO(request);
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(method = RequestMethod.POST, value = "/update")
    public UserRequestTO update(@RequestBody final UserMod userMod)
            throws NotFoundException, UnauthorizedRoleException {

        LOG.debug("Request user update called with {}", userMod);

        try {
            dataBinder.testUpdate(userMod);
        } catch (RollbackException e) {
            LOG.debug("Testing update - ignore exception");
        }

        UserRequest request = new UserRequest();
        request.setUserMod(userMod);
        request = userRequestDAO.save(request);

        auditManager.audit(Category.userRequest, UserRequestSubCategory.update, Result.success,
                "Successfully updated user request for " + request.getUserMod().getUsername());

        return dataBinder.getUserRequestTO(request);
    }

    @PreAuthorize("hasRole('USER_REQUEST_LIST')")
    @RequestMapping(method = RequestMethod.GET, value = "/list")
    @Transactional(readOnly = true)
    public List<UserRequestTO> list() {
        List<UserRequestTO> result = new ArrayList<UserRequestTO>();

        for (UserRequest request : userRequestDAO.findAll()) {
            result.add(dataBinder.getUserRequestTO(request));
        }

        auditManager.audit(Category.userRequest, UserRequestSubCategory.list, Result.success,
                "Successfully listed all user requests: " + result.size());

        return result;
    }

    @PreAuthorize("hasRole('USER_REQUEST_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/read/{requestId}")
    @Transactional(readOnly = true)
    public UserRequestTO read(@PathVariable("requestId") final Long requestId) throws NotFoundException {

        UserRequest request = userRequestDAO.find(requestId);
        if (request == null) {
            throw new NotFoundException("User request " + requestId);
        }

        auditManager.audit(Category.userRequest, UserRequestSubCategory.read, Result.success,
                "Successfully read user request for " + request.getUserTO().getUsername());

        return dataBinder.getUserRequestTO(request);
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(method = RequestMethod.GET, value = "/delete/{requestId}")
    public UserRequestTO delete(@PathVariable("requestId") final Long userId) throws NotFoundException, UnauthorizedRoleException {

        LOG.debug("Request user delete called with {}", userId);

        try {
            dataBinder.testDelete(userId);
        } catch (RollbackException e) {
            LOG.debug("Testing delete - ignore exception");
        }

        UserRequest request = new UserRequest();
        request.setUserId(userId);
        request = userRequestDAO.save(request);

        auditManager.audit(Category.userRequest, UserRequestSubCategory.delete, Result.success,
                "Successfully deleted user request for user" + userId);

        return dataBinder.getUserRequestTO(request);
    }

    @PreAuthorize("hasRole('USER_REQUEST_DELETE')")
    @RequestMapping(method = RequestMethod.GET, value = "/deleteRequest/{requestId}")
    public UserRequestTO deleteRequest(@PathVariable("requestId") final Long requestId) throws NotFoundException {

        UserRequest request = userRequestDAO.find(requestId);
        if (request == null) {
            throw new NotFoundException("User request " + requestId);
        }
        
        UserRequestTO requestToDelete = dataBinder.getUserRequestTO(request);

        auditManager.audit(Category.userRequest, UserRequestSubCategory.delete, Result.success,
                "Successfully deleted user request for user" + request.getUserId());

        userRequestDAO.delete(requestId);
        
        return requestToDelete;
    }
}
