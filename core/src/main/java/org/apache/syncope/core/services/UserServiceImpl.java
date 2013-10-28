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
import javax.ws.rs.core.Response;
import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.search.NodeCond;
import org.apache.syncope.common.services.InvalidSearchConditionException;
import org.apache.syncope.common.services.UserService;
import org.apache.syncope.common.to.BulkAction;
import org.apache.syncope.common.to.BulkActionRes;
import org.apache.syncope.common.to.PropagationRequestTO;
import org.apache.syncope.common.to.PropagationTargetsTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.core.rest.controller.UserController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends AbstractServiceImpl implements UserService, ContextAware {

    @Autowired
    private UserController controller;

    @Override
    public UserTO activate(final Long userId, final String token) {
        return controller.activate(userId, token);
    }

    @Override
    public UserTO activate(final Long userId, final String token, final PropagationRequestTO propagationRequestTO) {
        return controller.activate(userId, token, propagationRequestTO);
    }

    @Override
    public UserTO activateByUsername(final String username, final String token) {
        return controller.activate(username, token);
    }

    @Override
    public UserTO activateByUsername(final String username, final String token,
            final PropagationRequestTO propagationRequestTO) {
        return controller.activate(username, token, propagationRequestTO);
    }

    @Override
    public int count() {
        return controller.count();
    }

    @Override
    public Response create(final UserTO userTO) {
        UserTO created = controller.create(userTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(created.getId())).build();
        return Response.created(location).
                header(SyncopeConstants.REST_RESOURCE_ID_HEADER, created.getId()).
                entity(created)
                .build();
    }

    @Override
    public UserTO delete(final Long userId) {
        return controller.delete(userId);
    }

    @Override
    public List<UserTO> list() {
        return controller.list();
    }

    @Override
    public List<UserTO> list(final int page, final int size) {
        return controller.list(page, size);
    }

    @Override
    public UserTO reactivate(final Long userId) {
        return controller.reactivate(userId);
    }

    @Override
    public UserTO reactivate(final Long userId, final PropagationRequestTO propagationRequestTO) {
        return controller.reactivate(userId, propagationRequestTO);
    }

    @Override
    public UserTO reactivateByUsername(final String username) {
        return controller.reactivate(username);
    }

    @Override
    public UserTO reactivateByUsername(final String username, final PropagationRequestTO propagationRequestTO) {
        return controller.reactivate(username, propagationRequestTO);
    }

    @Override
    public UserTO read(final Long userId) {
        return controller.read(userId);
    }

    @Override
    public UserTO read(final String username) {
        return controller.read(username);
    }

    @Override
    public UserTO readSelf() {
        return controller.read();
    }

    @Override
    public List<UserTO> search(final NodeCond searchCondition) throws InvalidSearchConditionException {
        return controller.search(searchCondition);
    }

    @Override
    public List<UserTO> search(final NodeCond searchCondition, final int page, final int size)
            throws InvalidSearchConditionException {
        return controller.search(searchCondition, page, size);
    }

    @Override
    public int searchCount(final NodeCond searchCondition) throws InvalidSearchConditionException {
        return controller.searchCount(searchCondition);
    }

    @Override
    public UserTO suspend(final Long userId) {
        return controller.suspend(userId);
    }

    @Override
    public UserTO suspend(final Long userId, final PropagationRequestTO propagationRequestTO) {
        return controller.suspend(userId, propagationRequestTO);
    }

    @Override
    public UserTO suspendByUsername(final String username) {
        return controller.suspend(username);
    }

    @Override
    public UserTO suspendByUsername(final String username, final PropagationRequestTO propagationRequestTO) {
        return controller.suspend(username, propagationRequestTO);
    }

    @Override
    public UserTO update(final Long userId, final UserMod userMod) {
        userMod.setId(userId);
        return controller.update(userMod);
    }

    @Override
    public BulkActionRes bulkAction(final BulkAction bulkAction) {
        return controller.bulkAction(bulkAction);
    }

    @Override
    public UserTO unlink(final Long userId, final PropagationTargetsTO propagationTargetsTO) {
        return controller.unlink(userId, propagationTargetsTO.getResources());
    }

    @Override
    public UserTO unassign(final Long userId, final PropagationTargetsTO propagationTargetsTO) {
        return controller.unassign(userId, propagationTargetsTO.getResources());
    }

    @Override
    public UserTO deprovision(final Long userId, final PropagationTargetsTO propagationTargetsTO) {
        return controller.deprovision(userId, propagationTargetsTO.getResources());
    }
}
