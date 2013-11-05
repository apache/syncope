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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import javax.persistence.RollbackException;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.to.UserRequestTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.core.persistence.beans.SyncopeConf;
import org.apache.syncope.core.persistence.beans.UserRequest;
import org.apache.syncope.core.persistence.dao.ConfDAO;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.persistence.dao.UserRequestDAO;
import org.apache.syncope.core.rest.data.UserRequestDataBinder;
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
public class UserRequestController extends AbstractController<UserRequestTO> {

    @Autowired
    private ConfDAO confDAO;

    @Autowired
    private UserRequestDAO userRequestDAO;

    @Autowired
    private UserRequestDataBinder binder;

    public Boolean isCreateAllowedByConf() {
        final SyncopeConf createRequestAllowed = confDAO.find("createRequest.allowed", "false");

        return Boolean.valueOf(createRequestAllowed.getValue());
    }

    @RequestMapping(method = RequestMethod.GET, value = "/create/allowed")
    @Transactional(readOnly = true)
    public ModelAndView isCreateAllowed() {
        return new ModelAndView().addObject(isCreateAllowedByConf());
    }

    @RequestMapping(method = RequestMethod.POST, value = "/create")
    public UserRequestTO create(@RequestBody final UserTO userTO) {
        if (!isCreateAllowedByConf()) {
            LOG.error("Create requests are not allowed");

            throw new UnauthorizedRoleException(-1L);
        }

        LOG.debug("Request user create called with {}", userTO);

        try {
            binder.testCreate(userTO);
        } catch (RollbackException e) {
            LOG.debug("Testing create - ignore exception");
        }

        UserRequest request = new UserRequest();
        request.setUserTO(userTO);
        return binder.getUserRequestTO(userRequestDAO.save(request));
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(method = RequestMethod.POST, value = "/update")
    public UserRequestTO update(@RequestBody final UserMod userMod) {
        LOG.debug("Request user update called with {}", userMod);

        try {
            binder.testUpdate(userMod);
        } catch (RollbackException e) {
            LOG.debug("Testing update - ignore exception");
        }

        UserRequest request = new UserRequest();
        request.setUserMod(userMod);
        return binder.getUserRequestTO(userRequestDAO.save(request));
    }

    @PreAuthorize("hasRole('USER_REQUEST_LIST')")
    @RequestMapping(method = RequestMethod.GET, value = "/list")
    @Transactional(readOnly = true)
    public List<UserRequestTO> list() {
        List<UserRequestTO> result = new ArrayList<UserRequestTO>();
        for (UserRequest request : userRequestDAO.findAll()) {
            result.add(binder.getUserRequestTO(request));
        }
        return result;
    }

    @PreAuthorize("hasRole('USER_REQUEST_READ')")
    @RequestMapping(method = RequestMethod.GET, value = "/read/{requestId}")
    @Transactional(readOnly = true)
    public UserRequestTO read(@PathVariable("requestId") final Long requestId) {
        UserRequest request = userRequestDAO.find(requestId);
        if (request == null) {
            throw new NotFoundException("User request " + requestId);
        }
        return binder.getUserRequestTO(request);
    }

    @PreAuthorize("isAuthenticated()")
    @RequestMapping(method = RequestMethod.GET, value = "/delete/{userId}")
    public UserRequestTO delete(@PathVariable("userId") final Long userId) {
        LOG.debug("Request user delete called with {}", userId);

        try {
            binder.testDelete(userId);
        } catch (RollbackException e) {
            LOG.debug("Testing delete - ignore exception");
        }

        UserRequest request = new UserRequest();
        request.setUserId(userId);
        return binder.getUserRequestTO(userRequestDAO.save(request));
    }

    @PreAuthorize("hasRole('USER_REQUEST_DELETE')")
    @RequestMapping(method = RequestMethod.GET, value = "/deleteRequest/{requestId}")
    public UserRequestTO deleteRequest(@PathVariable("requestId") final Long requestId) {
        UserRequest request = userRequestDAO.find(requestId);
        if (request == null) {
            throw new NotFoundException("User request " + requestId);
        }

        UserRequestTO requestToDelete = binder.getUserRequestTO(request);
        userRequestDAO.delete(requestId);
        return requestToDelete;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected UserRequestTO resolveReference(final Method method, final Object... obj) {
        final UserRequestTO result;

        if (ArrayUtils.isNotEmpty(obj) && obj[0] instanceof Long
                && ("deleteRequest".equals(method.getName()) || "read".equals(method.getName()))) {
            final UserRequest request = userRequestDAO.find((Long) obj[0]);
            result = request == null ? null : binder.getUserRequestTO(request);
        } else {
            result = null;
        }

        return result;
    }
}
