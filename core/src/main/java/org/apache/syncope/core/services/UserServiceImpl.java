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

import java.util.List;

import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.PathParam;

import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.search.NodeCond;
import org.apache.syncope.common.services.UserService;
import org.apache.syncope.common.to.PropagationRequestTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.to.WorkflowFormTO;
import org.apache.syncope.core.rest.controller.UserController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    UserController userController;

    @Override
    public UserTO activate(@PathParam("userId") long userId, String token) {
        return userController.activate(userId, token);
    }

    @Override
    public UserTO activate(long userId, String token,
            PropagationRequestTO propagationRequestTO) {
        return userController.activate(userId, token, propagationRequestTO);
    }

    @Override
    public UserTO activateByUsername(String username,
            @MatrixParam("token") String token) {
        return userController.activate(username, token);
    }

    @Override
    public UserTO activateByUsername(String username, String token,
            PropagationRequestTO propagationRequestTO) {
        return userController.activate(username, token, propagationRequestTO);
    }

    @Override
    public WorkflowFormTO claimForm(String taskId) {
        return userController.claimForm(taskId);
    }

    @Override
    public int count() {
        return userController.countInternal();
    }

    @Override
    public UserTO create(UserTO userTO) {
        return userController.createInternal(userTO);
    }

    @Override
    public UserTO delete(Long userId) {
        return userController.delete(userId);
    }

    @Override
    @POST
    public UserTO executeWorkflow(String taskId, UserTO userTO) {
        return userController.executeWorkflow(userTO, taskId);
    }

    @Override
    public WorkflowFormTO getFormForUser(Long userId) {
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
    public List<UserTO> list(int page, int size) {
        return userController.list(page, size);
    }

    @Override
    public UserTO reactivate(long userId) {
        return userController.reactivate(userId);
    }

    @Override
    public UserTO reactivate(long userId, PropagationRequestTO propagationRequestTO) {
        return userController.reactivate(userId, propagationRequestTO);
    }

    @Override
    public UserTO reactivateByUsername(String username) {
        return userController.reactivate(username);
    }

    @Override
    public UserTO reactivateByUsername(String username,
            PropagationRequestTO propagationRequestTO) {
        return userController.reactivate(username, propagationRequestTO);
    }

    @Override
    public UserTO read(Long userId) {
        return userController.read(userId);
    }

    @Override
    public UserTO read(String username) {
        return userController.read(username);
    }

    @Override
    public UserTO readSelf() {
        return userController.read();
    }

    @Override
    public List<UserTO> search(NodeCond searchCondition) {
        return userController.search(searchCondition);
    }

    @Override
    public List<UserTO> search(NodeCond searchCondition, int page, int size) {
        return userController.search(searchCondition, page, size);
    }

    @Override
    public int searchCount(NodeCond searchCondition) {
        return userController.searchCountInternal(searchCondition);
    }

    @Override
    public UserTO submitForm(WorkflowFormTO form) {
        return userController.submitForm(form);
    }

    @Override
    public UserTO suspend(long userId) {
        return userController.suspend(userId);
    }

    @Override
    public UserTO suspend(long userId, PropagationRequestTO propagationRequestTO) {
        return userController.suspend(userId, propagationRequestTO);
    }

    @Override
    public UserTO suspendByUsername(String username) {
        return userController.suspend(username);
    }

    @Override
    public UserTO suspendByUsername(String username, PropagationRequestTO propagationRequestTO) {
        return userController.suspend(username, propagationRequestTO);
    }

    @Override
    public UserTO update(Long userId, UserMod userMod) {
        return userController.update(userMod);
    }

    @Override
    public Boolean verifyPassword(String username, String password) {
        return userController.verifyPasswordInternal(username, password);
    }

}
