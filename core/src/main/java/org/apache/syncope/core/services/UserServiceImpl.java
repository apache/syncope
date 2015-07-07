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
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.mod.ResourceAssociationMod;
import org.apache.syncope.common.mod.StatusMod;
import org.apache.syncope.common.mod.UserMod;
import org.apache.syncope.common.reqres.BulkAction;
import org.apache.syncope.common.services.UserService;
import org.apache.syncope.common.reqres.BulkActionResult;
import org.apache.syncope.common.reqres.PagedResult;
import org.apache.syncope.common.to.PropagationStatus;
import org.apache.syncope.common.wrap.ResourceName;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.RESTHeaders;
import org.apache.syncope.common.types.ResourceAssociationActionType;
import org.apache.syncope.common.types.ResourceDeassociationActionType;
import org.apache.syncope.common.util.CollectionWrapper;
import org.apache.syncope.core.persistence.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.dao.search.SearchCond;
import org.apache.syncope.core.rest.controller.UserController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends AbstractServiceImpl implements UserService {

    @Autowired
    private UserController controller;

    @Override
    public Response getUsername(final Long userId) {
        return Response.ok().header(HttpHeaders.ALLOW, OPTIONS_ALLOW).
                header(RESTHeaders.USERNAME, controller.getUsername(userId)).
                build();
    }

    @Override
    public Response getUserId(final String username) {
        return Response.ok().header(HttpHeaders.ALLOW, OPTIONS_ALLOW).
                header(RESTHeaders.USER_ID, controller.getUserId(username)).
                build();
    }

    @Override
    public Response create(final UserTO userTO, final boolean storePassword) {
        UserTO created = controller.create(userTO, storePassword);
        return createResponse(created.getId(), created);
    }

    @Override
    public Response delete(final Long userId) {
        UserTO user = controller.read(userId);

        checkETag(user.getETagValue());

        UserTO deleted = controller.delete(userId);
        return modificationResponse(deleted);
    }

    @Override
    public PagedResult<UserTO> list() {
        return list(DEFAULT_PARAM_PAGE_VALUE, DEFAULT_PARAM_SIZE_VALUE, null, true);
    }

    @Override
    public PagedResult<UserTO> list(final String orderBy) {
        return list(DEFAULT_PARAM_PAGE_VALUE, DEFAULT_PARAM_SIZE_VALUE, orderBy, true);
    }

    @Override
    public PagedResult<UserTO> list(final Integer page, final Integer size) {
        return list(page, size, null, true);
    }

    @Override
    public PagedResult<UserTO> list(
            final Integer page, final Integer size, final String orderBy, final boolean details) {

        List<OrderByClause> orderByClauses = getOrderByClauses(orderBy);
        return buildPagedResult(controller.list(page, size, orderByClauses, details), page, size, controller.count());
    }

    @Override
    public UserTO read(final Long userId) {
        return controller.read(userId);
    }

    @Override
    public PagedResult<UserTO> search(final String fiql) {
        return search(fiql, DEFAULT_PARAM_PAGE_VALUE, DEFAULT_PARAM_SIZE_VALUE, null, true);
    }

    @Override
    public PagedResult<UserTO> search(final String fiql, final String orderBy) {
        return search(fiql, DEFAULT_PARAM_PAGE_VALUE, DEFAULT_PARAM_SIZE_VALUE, orderBy, true);
    }

    @Override
    public PagedResult<UserTO> search(final String fiql, final Integer page, final Integer size) {
        return search(fiql, page, size, null, true);
    }

    @Override
    public PagedResult<UserTO> search(
            final String fiql, final Integer page, final Integer size, final String orderBy, final boolean details) {

        SearchCond cond = getSearchCond(fiql);
        List<OrderByClause> orderByClauses = getOrderByClauses(orderBy);
        return buildPagedResult(
                controller.search(cond, page, size, orderByClauses, details), page, size, controller.searchCount(cond));
    }

    @Override
    public Response update(final Long userId, final UserMod userMod) {
        UserTO user = controller.read(userId);

        checkETag(user.getETagValue());

        userMod.setId(userId);
        UserTO updated = controller.update(userMod);
        return modificationResponse(updated);
    }

    @Override
    public Response status(final Long userId, final StatusMod statusMod) {
        UserTO user = controller.read(userId);

        checkETag(user.getETagValue());

        statusMod.setId(userId);
        UserTO updated = controller.status(statusMod);
        return modificationResponse(updated);
    }

    @Override
    public Response bulkDeassociation(
            final Long userId, final ResourceDeassociationActionType type, final List<ResourceName> resourceNames) {

        final UserTO user = controller.read(userId);

        checkETag(user.getETagValue());

        UserTO updated;
        switch (type) {
            case UNLINK:
                updated = controller.unlink(userId, CollectionWrapper.unwrap(resourceNames));
                break;

            case UNASSIGN:
                updated = controller.unassign(userId, CollectionWrapper.unwrap(resourceNames));
                break;

            case DEPROVISION:
                updated = controller.deprovision(userId, CollectionWrapper.unwrap(resourceNames));
                break;

            default:
                updated = controller.read(userId);
        }

        final BulkActionResult res = new BulkActionResult();

        if (type == ResourceDeassociationActionType.UNLINK) {
            for (ResourceName resourceName : resourceNames) {
                res.add(resourceName.getElement(), updated.getResources().contains(resourceName.getElement())
                        ? BulkActionResult.Status.FAILURE
                        : BulkActionResult.Status.SUCCESS);
            }
        } else {
            for (PropagationStatus propagationStatusTO : updated.getPropagationStatusTOs()) {
                res.add(propagationStatusTO.getResource(), propagationStatusTO.getStatus().toString());
            }
        }

        return modificationResponse(res);
    }

    @Override
    public Response bulkAssociation(
            final Long userId, final ResourceAssociationActionType type, final ResourceAssociationMod associationMod) {

        final UserTO user = controller.read(userId);

        checkETag(user.getETagValue());

        UserTO updated;
        switch (type) {
            case LINK:
                updated = controller.link(
                        userId,
                        CollectionWrapper.unwrap(associationMod.getTargetResources()));
                break;

            case ASSIGN:
                updated = controller.assign(
                        userId,
                        CollectionWrapper.unwrap(associationMod.getTargetResources()),
                        associationMod.isChangePwd(),
                        associationMod.getPassword());
                break;

            case PROVISION:
                updated = controller.provision(
                        userId,
                        CollectionWrapper.unwrap(associationMod.getTargetResources()),
                        associationMod.isChangePwd(),
                        associationMod.getPassword());
                break;

            default:
                updated = controller.read(userId);
        }

        final BulkActionResult res = new BulkActionResult();

        if (type == ResourceAssociationActionType.LINK) {
            for (ResourceName resourceName : associationMod.getTargetResources()) {
                res.add(resourceName.getElement(), updated.getResources().contains(resourceName.getElement())
                        ? BulkActionResult.Status.FAILURE
                        : BulkActionResult.Status.SUCCESS);
            }
        } else {
            for (PropagationStatus propagationStatusTO : updated.getPropagationStatusTOs()) {
                res.add(propagationStatusTO.getResource(), propagationStatusTO.getStatus().toString());
            }
        }

        return modificationResponse(res);
    }

    @Override
    public BulkActionResult bulk(final BulkAction bulkAction) {
        return controller.bulk(bulkAction);
    }
}
