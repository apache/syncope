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
import java.util.Date;
import java.util.List;
import javax.persistence.RollbackException;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.to.UserRequestTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.AuditElements.Category;
import org.apache.syncope.common.types.AuditElements.Result;
import org.apache.syncope.common.types.AuditElements.UserRequestSubCategory;
import org.apache.syncope.common.types.SyncopeClientExceptionType;
import org.apache.syncope.common.validation.SyncopeClientCompositeException;
import org.apache.syncope.common.validation.SyncopeClientException;
import org.apache.syncope.core.audit.AuditManager;
import org.apache.syncope.core.persistence.beans.SyncopeConf;
import org.apache.syncope.core.persistence.beans.UserRequest;
import org.apache.syncope.core.persistence.dao.ConfDAO;
import org.apache.syncope.core.persistence.dao.NotFoundException;
import org.apache.syncope.core.persistence.dao.UserRequestDAO;
import org.apache.syncope.core.rest.data.UserRequestDataBinder;
import org.apache.syncope.core.util.EntitlementUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
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
    private UserController userController;

    @Autowired
    private UserRequestDataBinder binder;

    public boolean isCreateAllowed() {
        final SyncopeConf createRequestAllowed = confDAO.find("createRequest.allowed", "false");

        auditManager.audit(Category.userRequest, UserRequestSubCategory.isCreateAllowed, Result.success,
                "Successfully checked whether self create is allowed");

        return Boolean.valueOf(createRequestAllowed.getValue());
    }

    public UserRequestTO create(final UserTO userTO) {
        if (!isCreateAllowed()) {
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
        request.setUsername(userTO.getUsername());
        request.createUser(userTO);
        request.setCreationDate(new Date());
        request = userRequestDAO.save(request);

        auditManager.audit(Category.userRequest, UserRequestSubCategory.create, Result.success,
                "Successfully created user request for " + request.getUsername());

        return binder.getUserRequestTO(request);
    }

    @PreAuthorize("isAuthenticated()")
    public UserRequestTO update(final UserMod userMod) {
        LOG.debug("Request user update called with {}", userMod);

        try {
            binder.testUpdate(userMod);
        } catch (RollbackException e) {
            LOG.debug("Testing update - ignore exception");
        }

        UserRequest request = new UserRequest();
        request.setUsername(binder.getUserFromId(userMod.getId()).getUsername());
        request.updateUser(userMod);
        request.setCreationDate(new Date());
        request = userRequestDAO.save(request);

        auditManager.audit(Category.userRequest, UserRequestSubCategory.update, Result.success,
                "Successfully updated user request for " + request.getUsername());

        return binder.getUserRequestTO(request);
    }

    @PreAuthorize("isAuthenticated()")
    public UserRequestTO delete(final Long userId) {
        LOG.debug("Request user delete called with {}", userId);

        try {
            binder.testDelete(userId);
        } catch (RollbackException e) {
            LOG.debug("Testing delete - ignore exception");
        }

        UserRequest request = new UserRequest();
        request.setUsername(binder.getUserFromId(userId).getUsername());
        request.deleteUser(userId);
        request.setCreationDate(new Date());
        request = userRequestDAO.save(request);

        auditManager.audit(Category.userRequest, UserRequestSubCategory.delete, Result.success,
                "Successfully deleted user request for user" + request.getUsername());

        return binder.getUserRequestTO(request);
    }

    @PreAuthorize("hasRole('USER_REQUEST_LIST')")
    @Transactional(readOnly = true)
    public List<UserRequestTO> list() {
        List<UserRequestTO> result = new ArrayList<UserRequestTO>();

        for (UserRequest request : userRequestDAO.findAll()) {
            result.add(binder.getUserRequestTO(request));
        }

        auditManager.audit(Category.userRequest, UserRequestSubCategory.list, Result.success,
                "Successfully listed all user requests: " + result.size());

        return result;
    }

    @PreAuthorize("hasRole('USER_REQUEST_LIST')")
    @Transactional(readOnly = true)
    public List<UserRequestTO> list(final boolean executed) {
        List<UserRequestTO> result = new ArrayList<UserRequestTO>();

        for (UserRequest request : userRequestDAO.findAll(executed)) {
            result.add(binder.getUserRequestTO(request));
        }

        auditManager.audit(Category.userRequest, UserRequestSubCategory.list, Result.success,
                String.format("Successfully listed all %s executed user requests: %d",
                (executed ? "" : "not"), result.size()));

        return result;
    }

    public List<UserRequestTO> listByUsername(final String username) {
        List<UserRequestTO> result = new ArrayList<UserRequestTO>();

        for (UserRequest request : userRequestDAO.findAll(username)) {
            result.add(binder.getUserRequestTO(request));
        }

        auditManager.audit(Category.userRequest, UserRequestSubCategory.list, Result.success,
                String.format("Successfully listed all %s's requests: %d", username, result.size()));

        return result;
    }

    @PreAuthorize("hasRole('USER_REQUEST_READ')")
    @Transactional(readOnly = true)
    public UserRequestTO read(final Long requestId) {
        UserRequest request = userRequestDAO.find(requestId);
        if (request == null) {
            throw new NotFoundException("User request " + requestId);
        }

        final UserRequestTO userRequestTO = binder.getUserRequestTO(request);
        auditManager.audit(Category.userRequest, UserRequestSubCategory.read, Result.success,
                "Successfully read user request for " + request.getUsername());

        return userRequestTO;
    }

    @PreAuthorize("hasRole('USER_REQUEST_DELETE')")
    public UserRequestTO deleteRequest(final Long requestId) {
        UserRequest request = userRequestDAO.find(requestId);
        if (request == null) {
            throw new NotFoundException("User request " + requestId);
        }

        UserRequestTO requestToDelete = binder.getUserRequestTO(request);
        userRequestDAO.delete(requestId);

        auditManager.audit(Category.userRequest, UserRequestSubCategory.delete, Result.success,
                "Successfully deleted user request for user" + request.getUsername());

        return requestToDelete;
    }

    @PreAuthorize("hasRole('USER_REQUEST_CLAIM')")
    public UserRequestTO claim(final Long requestId) {
        UserRequest request = userRequestDAO.find(requestId);
        if (request == null) {
            throw new NotFoundException("User request " + requestId);
        }

        request.setOwner(EntitlementUtil.getAuthenticatedUsername());
        request.setClaimDate(new Date());

        final UserRequestTO userRequestTO = binder.getUserRequestTO(userRequestDAO.save(request));

        auditManager.audit(Category.userRequest, UserRequestSubCategory.claim, Result.success,
                String.format("Successfully claimed user request %s by %s",
                userRequestTO.getId(), userRequestTO.getOwner()));

        return userRequestTO;
    }

    @PreAuthorize("hasRole('USER_REQUEST_EXECUTE') and ("
            + "(hasRole('USER_CREATE') and #request.type == #request.type.CREATE) or "
            + "(hasRole('USER_UPDATE') and #request.type == #request.type.UPDATE) or "
            + "(hasRole('USER_DELETE') and #request.type == #request.type.DELETE))")
    public UserTO execute(final UserRequestTO request, final UserTO reviewed, final UserMod changes) {
        UserRequest userRequest = userRequestDAO.find(request.getId());
        if (request.isExecuted()) {
            throw new NotFoundException("Executable user request " + request.getId());
        }

        if (StringUtils.isBlank(request.getOwner())
                || !request.getOwner().equalsIgnoreCase(EntitlementUtil.getAuthenticatedUsername())) {

            final SyncopeClientCompositeException scce =
                    new SyncopeClientCompositeException(Response.Status.UNAUTHORIZED.getStatusCode());
            scce.addException(new SyncopeClientException(SyncopeClientExceptionType.Unauthorized));
            throw scce;
        }

        final UserTO res;

        switch (request.getType()) {
            case CREATE:
                res = userController.create(reviewed == null ? request.getUserTO() : reviewed);
                break;
            case UPDATE:
                res = userController.update(changes == null ? request.getUserMod() : changes);
                break;
            case DELETE:
                res = userController.delete(request.getUserId());
                break;
            default:
                throw new NotFoundException("User request " + request.getType());
        }

        userRequest.setExecuted(true);
        userRequest.setExecutionDate(new Date());
        userRequestDAO.save(userRequest);

        auditManager.audit(Category.userRequest, UserRequestSubCategory.execute, Result.success,
                String.format("Successfully executed %s request for user %d", request.getType(), res.getId()));

        return res;
    }
}
