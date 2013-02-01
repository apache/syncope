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
package org.apache.syncope.core.services;

import java.net.URI;
import java.util.List;
import javax.ws.rs.POST;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.search.NodeCond;
import org.apache.syncope.common.services.InvalidSearchConditionException;
import org.apache.syncope.common.services.UserService;
import org.apache.syncope.common.to.PropagationRequestTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.to.WorkflowFormTO;
import org.apache.syncope.core.rest.controller.UserController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService, ContextAware {

    @Autowired
    private UserController userController;

    private UriInfo uriInfo;

    @Override
    public UserTO activate(final long userId, final String token) {
        return userController.activate(userId, token);
    }

    @Override
    public UserTO activate(final long userId, final String token, final PropagationRequestTO propagationRequestTO) {
        return userController.activate(userId, token, propagationRequestTO);
    }

    @Override
    public UserTO activateByUsername(final String username, final String token) {
        return userController.activate(username, token);
    }

    @Override
    public UserTO activateByUsername(final String username, final String token,
            final PropagationRequestTO propagationRequestTO) {
        return userController.activate(username, token, propagationRequestTO);
    }

    @Override
    public WorkflowFormTO claimForm(final String taskId) {
        return userController.claimForm(taskId);
    }

    @Override
    public int count() {
        return userController.countInternal();
    }

    @Override
    public Response create(final UserTO userTO) {
        UserTO created = userController.createInternal(userTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(created.getId() + "").build();
        return Response.created(location)
                .header(SyncopeConstants.REST_HEADER_ID, created.getId())
                .entity(created)
                .build();
    }

    @Override
    public UserTO delete(final Long userId) {
        return userController.delete(userId);
    }

    @Override
    @POST
    public UserTO executeWorkflow(final String taskId, final UserTO userTO) {
        return userController.executeWorkflow(userTO, taskId);
    }

    @Override
    public WorkflowFormTO getFormForUser(final Long userId) {
        return userController.getFormForUser(userId);
    }

    @Override
    public List<WorkflowFormTO> getForms() {
        return userController.getForms();
    }

    @Override
    public List<UserTO> list() {
        return userController.list();
    }

    @Override
    public List<UserTO> list(final int page, final int size) {
        return userController.list(page, size);
    }

    @Override
    public UserTO reactivate(final long userId) {
        return userController.reactivate(userId);
    }

    @Override
    public UserTO reactivate(final long userId, final PropagationRequestTO propagationRequestTO) {
        return userController.reactivate(userId, propagationRequestTO);
    }

    @Override
    public UserTO reactivateByUsername(final String username) {
        return userController.reactivate(username);
    }

    @Override
    public UserTO reactivateByUsername(final String username, final PropagationRequestTO propagationRequestTO) {
        return userController.reactivate(username, propagationRequestTO);
    }

    @Override
    public UserTO read(final Long userId) {
        return userController.read(userId);
    }

    @Override
    public UserTO read(final String username) {
        return userController.read(username);
    }

    @Override
    public UserTO readSelf() {
        return userController.read();
    }

    @Override
    public List<UserTO> search(final NodeCond searchCondition) throws InvalidSearchConditionException {
        return userController.search(searchCondition);
    }

    @Override
    public List<UserTO> search(final NodeCond searchCondition, final int page, final int size) throws InvalidSearchConditionException {
        return userController.search(searchCondition, page, size);
    }

    @Override
    public int searchCount(final NodeCond searchCondition) throws InvalidSearchConditionException {
        return userController.searchCountInternal(searchCondition);
    }

    @Override
    public UserTO submitForm(final WorkflowFormTO form) {
        return userController.submitForm(form);
    }

    @Override
    public UserTO suspend(final long userId) {
        return userController.suspend(userId);
    }

    @Override
    public UserTO suspend(final long userId, final PropagationRequestTO propagationRequestTO) {
        return userController.suspend(userId, propagationRequestTO);
    }

    @Override
    public UserTO suspendByUsername(final String username) {
        return userController.suspend(username);
    }

    @Override
    public UserTO suspendByUsername(final String username, final PropagationRequestTO propagationRequestTO) {
        return userController.suspend(username, propagationRequestTO);
    }

    @Override
    public UserTO update(final Long userId, final UserMod userMod) {
        return userController.update(userMod);
    }

    @Override
    public Boolean verifyPassword(final String username, final String password) {
        return userController.verifyPasswordInternal(username, password);
    }

    @Override
    public void setUriInfo(final UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }
}
