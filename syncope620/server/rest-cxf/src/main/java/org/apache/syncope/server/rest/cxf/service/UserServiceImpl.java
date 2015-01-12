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
package org.apache.syncope.server.rest.cxf.service;

import java.util.List;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.mod.ResourceAssociationMod;
import org.apache.syncope.common.lib.mod.StatusMod;
import org.apache.syncope.common.lib.mod.UserMod;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ResourceAssociationActionType;
import org.apache.syncope.common.lib.types.ResourceDeassociationActionType;
import org.apache.syncope.common.lib.wrap.ResourceName;
import org.apache.syncope.common.rest.api.CollectionWrapper;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.syncope.server.logic.UserLogic;
import org.apache.syncope.server.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.server.persistence.api.dao.search.SearchCond;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends AbstractServiceImpl implements UserService {

    @Autowired
    private UserLogic logic;

    @Override
    public Response getUsername(final Long userKey) {
        return Response.ok().header(HttpHeaders.ALLOW, OPTIONS_ALLOW).
                header(RESTHeaders.USERNAME, logic.getUsername(userKey)).
                build();
    }

    @Override
    public Response getUserId(final String username) {
        return Response.ok().header(HttpHeaders.ALLOW, OPTIONS_ALLOW).
                header(RESTHeaders.USER_ID, logic.getKey(username)).
                build();
    }

    @Override
    public Response create(final UserTO userTO, final boolean storePassword) {
        UserTO created = logic.create(userTO, storePassword);
        return createResponse(created.getKey(), created);
    }

    @Override
    public Response delete(final Long userKey) {
        UserTO user = logic.read(userKey);

        checkETag(user.getETagValue());

        UserTO deleted = logic.delete(userKey);
        return modificationResponse(deleted);
    }

    @Override
    public PagedResult<UserTO> list() {
        return list(DEFAULT_PARAM_PAGE_VALUE, DEFAULT_PARAM_SIZE_VALUE, null);
    }

    @Override
    public PagedResult<UserTO> list(final String orderBy) {
        return list(DEFAULT_PARAM_PAGE_VALUE, DEFAULT_PARAM_SIZE_VALUE, orderBy);
    }

    @Override
    public PagedResult<UserTO> list(final Integer page, final Integer size) {
        return list(page, size, null);
    }

    @Override
    public PagedResult<UserTO> list(final Integer page, final Integer size, final String orderBy) {
        List<OrderByClause> orderByClauses = getOrderByClauses(orderBy);
        return buildPagedResult(logic.list(page, size, orderByClauses), page, size, logic.count());
    }

    @Override
    public UserTO read(final Long userKey) {
        return logic.read(userKey);
    }

    @Override
    public PagedResult<UserTO> search(final String fiql) {
        return search(fiql, DEFAULT_PARAM_PAGE_VALUE, DEFAULT_PARAM_SIZE_VALUE, null);
    }

    @Override
    public PagedResult<UserTO> search(final String fiql, final String orderBy) {
        return search(fiql, DEFAULT_PARAM_PAGE_VALUE, DEFAULT_PARAM_SIZE_VALUE, orderBy);
    }

    @Override
    public PagedResult<UserTO> search(final String fiql, final Integer page, final Integer size) {
        return search(fiql, page, size, null);
    }

    @Override
    public PagedResult<UserTO> search(final String fiql, final Integer page, final Integer size, final String orderBy) {
        SearchCond cond = getSearchCond(fiql);
        List<OrderByClause> orderByClauses = getOrderByClauses(orderBy);
        return buildPagedResult(
                logic.search(cond, page, size, orderByClauses), page, size, logic.searchCount(cond));
    }

    @Override
    public Response update(final Long userKey, final UserMod userMod) {
        UserTO user = logic.read(userKey);

        checkETag(user.getETagValue());

        userMod.setKey(userKey);
        UserTO updated = logic.update(userMod);
        return modificationResponse(updated);
    }

    @Override
    public Response status(final Long userKey, final StatusMod statusMod) {
        UserTO user = logic.read(userKey);

        checkETag(user.getETagValue());

        statusMod.setKey(userKey);
        UserTO updated = logic.status(statusMod);
        return modificationResponse(updated);
    }

    @Override
    public Response bulkDeassociation(
            final Long userKey, final ResourceDeassociationActionType type, final List<ResourceName> resourceNames) {

        final UserTO user = logic.read(userKey);

        checkETag(user.getETagValue());

        UserTO updated;
        switch (type) {
            case UNLINK:
                updated = logic.unlink(userKey, CollectionWrapper.unwrap(resourceNames));
                break;

            case UNASSIGN:
                updated = logic.unassign(userKey, CollectionWrapper.unwrap(resourceNames));
                break;

            case DEPROVISION:
                updated = logic.deprovision(userKey, CollectionWrapper.unwrap(resourceNames));
                break;

            default:
                updated = logic.read(userKey);
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
            final Long userKey, final ResourceAssociationActionType type, final ResourceAssociationMod associationMod) {

        final UserTO user = logic.read(userKey);

        checkETag(user.getETagValue());

        UserTO updated;
        switch (type) {
            case LINK:
                updated = logic.link(
                        userKey,
                        CollectionWrapper.unwrap(associationMod.getTargetResources()));
                break;

            case ASSIGN:
                updated = logic.assign(
                        userKey,
                        CollectionWrapper.unwrap(associationMod.getTargetResources()),
                        associationMod.isChangePwd(),
                        associationMod.getPassword());
                break;

            case PROVISION:
                updated = logic.provision(
                        userKey,
                        CollectionWrapper.unwrap(associationMod.getTargetResources()),
                        associationMod.isChangePwd(),
                        associationMod.getPassword());
                break;

            default:
                updated = logic.read(userKey);
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
        return logic.bulk(bulkAction);
    }
}
