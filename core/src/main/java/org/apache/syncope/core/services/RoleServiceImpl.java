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

import javax.ws.rs.core.Response;
import org.apache.syncope.common.mod.RoleMod;
import org.apache.syncope.common.reqres.BulkAction;
import org.apache.syncope.common.reqres.BulkActionResult;
import org.apache.syncope.common.services.RoleService;
import org.apache.syncope.common.reqres.PagedResult;
import org.apache.syncope.common.to.PropagationStatus;
import org.apache.syncope.common.wrap.ResourceName;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.TaskExecTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.ResourceAssociationActionType;
import org.apache.syncope.common.types.ResourceDeassociationActionType;
import org.apache.syncope.common.util.CollectionWrapper;
import org.apache.syncope.core.persistence.dao.search.OrderByClause;
import org.apache.syncope.core.rest.controller.RoleController;
import org.apache.syncope.core.rest.controller.UserController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RoleServiceImpl extends AbstractServiceImpl implements RoleService {

    @Autowired
    private RoleController controller;

    @Autowired
    private UserController userController;

    @Override
    public List<RoleTO> children(final Long roleId, final boolean details) {
        return controller.children(roleId, details);
    }

    @Override
    public Response create(final RoleTO roleTO) {
        RoleTO created = controller.create(roleTO);
        return createResponse(created.getId(), created);
    }

    @Override
    public Response delete(final Long roleId) {
        RoleTO role = controller.read(roleId);

        checkETag(role.getETagValue());

        RoleTO deleted = controller.delete(roleId);
        return modificationResponse(deleted);
    }

    @Override
    public PagedResult<RoleTO> list() {
        return list(DEFAULT_PARAM_PAGE_VALUE, DEFAULT_PARAM_SIZE_VALUE, null, true);
    }

    @Override
    public PagedResult<RoleTO> list(final String orderBy) {
        return list(DEFAULT_PARAM_PAGE_VALUE, DEFAULT_PARAM_SIZE_VALUE, orderBy, true);
    }

    @Override
    public PagedResult<RoleTO> list(final Integer page, final Integer size) {
        return list(page, size, null, true);
    }

    @Override
    public PagedResult<RoleTO> list(
            final Integer page, final Integer size, final String orderBy, final boolean details) {

        List<OrderByClause> orderByClauses = getOrderByClauses(orderBy);
        return buildPagedResult(controller.list(page, size, orderByClauses, details), page, size, controller.count());
    }

    @Override
    public RoleTO parent(final Long roleId) {
        return controller.parent(roleId);
    }

    @Override
    public RoleTO read(final Long roleId) {
        return controller.read(roleId);
    }

    @Override
    public PagedResult<RoleTO> search(final String fiql) {
        return search(fiql, DEFAULT_PARAM_PAGE_VALUE, DEFAULT_PARAM_SIZE_VALUE, null, true);
    }

    @Override
    public PagedResult<RoleTO> search(final String fiql, final String orderBy) {
        return search(fiql, DEFAULT_PARAM_PAGE_VALUE, DEFAULT_PARAM_SIZE_VALUE, orderBy, true);
    }

    @Override
    public PagedResult<RoleTO> search(final String fiql, final Integer page, final Integer size) {
        return search(fiql, page, size, null, true);
    }

    @Override
    public PagedResult<RoleTO> search(
            final String fiql, final Integer page, final Integer size, final String orderBy, final boolean details) {

        List<OrderByClause> orderByClauses = getOrderByClauses(orderBy);
        return buildPagedResult(
                controller.search(getSearchCond(fiql),
                        page,
                        size,
                        orderByClauses,
                        details),
                page,
                size,
                controller.searchCount(getSearchCond(fiql)));
    }

    @Override
    public RoleTO readSelf(final Long roleId) {
        return controller.readSelf(roleId);
    }

    @Override
    public Response update(final Long roleId, final RoleMod roleMod) {
        RoleTO role = controller.read(roleId);

        checkETag(role.getETagValue());

        roleMod.setId(roleId);
        RoleTO updated = controller.update(roleMod);
        return modificationResponse(updated);
    }

    @Override
    public Response bulkDeassociation(
            final Long roleId, final ResourceDeassociationActionType type, final List<ResourceName> resourceNames) {

        RoleTO role = controller.read(roleId);

        checkETag(role.getETagValue());

        RoleTO updated;
        switch (type) {
            case UNLINK:
                updated = controller.unlink(roleId, CollectionWrapper.unwrap(resourceNames));
                break;

            case UNASSIGN:
                updated = controller.unassign(roleId, CollectionWrapper.unwrap(resourceNames));
                break;

            case DEPROVISION:
                updated = controller.deprovision(roleId, CollectionWrapper.unwrap(resourceNames));
                break;

            default:
                updated = controller.read(roleId);
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
            final Long roleId, final ResourceAssociationActionType type, final List<ResourceName> resourceNames) {

        RoleTO role = controller.read(roleId);

        checkETag(role.getETagValue());

        RoleTO updated;
        switch (type) {
            case LINK:
                updated = controller.link(roleId, CollectionWrapper.unwrap(resourceNames));
                break;

            case ASSIGN:
                updated = controller.assign(roleId, CollectionWrapper.unwrap(resourceNames), false, null);
                break;

            case PROVISION:
                updated = controller.provision(roleId, CollectionWrapper.unwrap(resourceNames), false, null);
                break;

            default:
                updated = controller.read(roleId);
        }

        final BulkActionResult res = new BulkActionResult();

        if (type == ResourceAssociationActionType.LINK) {
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
    public TaskExecTO bulkProvisionMembers(final Long roleId) {
        return controller.bulkProvisionMembers(roleId);
    }

    @Override
    public TaskExecTO bulkDeprovisionMembers(final Long roleId) {
        return controller.bulkDeprovisionMembers(roleId);
    }

    @Override
    public BulkActionResult bulk(final BulkAction bulkAction) {
        return controller.bulk(bulkAction);
    }
}
