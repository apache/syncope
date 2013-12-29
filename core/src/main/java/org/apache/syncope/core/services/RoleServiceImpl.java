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

import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;

import org.apache.syncope.common.mod.RoleMod;
import org.apache.syncope.common.reqres.BulkActionResult;
import org.apache.syncope.common.services.RoleService;
import org.apache.syncope.common.reqres.PagedResult;
import org.apache.syncope.common.to.PropagationStatus;
import org.apache.syncope.common.wrap.ResourceName;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.types.ResourceAssociationActionType;
import org.apache.syncope.common.types.ResourceDeAssociationActionType;
import org.apache.syncope.common.util.CollectionWrapper;
import org.apache.syncope.core.persistence.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.dao.search.SearchCond;
import org.apache.syncope.core.rest.controller.RoleController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RoleServiceImpl extends AbstractServiceImpl implements RoleService {

    @Autowired
    private RoleController controller;

    @Override
    public List<RoleTO> children(final Long roleId) {
        return controller.children(roleId);
    }

    @Override
    public Response create(final RoleTO roleTO) {
        RoleTO created = controller.create(roleTO);
        return createResponse(created.getId(), created).build();
    }

    @Override
    public Response delete(final Long roleId) {
        RoleTO role = controller.read(roleId);

        ResponseBuilder builder = messageContext.getRequest().evaluatePreconditions(new EntityTag(role.getETagValue()));
        if (builder == null) {
            RoleTO deleted = controller.delete(roleId);
            builder = modificationResponse(deleted);
        }

        return builder.build();
    }

    @Override
    public PagedResult<RoleTO> list() {
        return list(DEFAULT_PARAM_PAGE_VALUE, DEFAULT_PARAM_SIZE_VALUE, null);
    }

    @Override
    public PagedResult<RoleTO> list(final String orderBy) {
        return list(DEFAULT_PARAM_PAGE_VALUE, DEFAULT_PARAM_SIZE_VALUE, orderBy);
    }

    @Override
    public PagedResult<RoleTO> list(final int page, final int size) {
        return list(page, size, null);
    }

    @Override
    public PagedResult<RoleTO> list(final int page, final int size, final String orderBy) {
        checkPageSize(page, size);
        List<OrderByClause> orderByClauses = getOrderByClauses(orderBy);
        return buildPagedResult(controller.list(page, size, orderByClauses), page, size, controller.count());
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
        return search(fiql, DEFAULT_PARAM_PAGE_VALUE, DEFAULT_PARAM_SIZE_VALUE, null);
    }

    @Override
    public PagedResult<RoleTO> search(final String fiql, final String orderBy) {
        return search(fiql, DEFAULT_PARAM_PAGE_VALUE, DEFAULT_PARAM_SIZE_VALUE, orderBy);
    }

    @Override
    public PagedResult<RoleTO> search(final String fiql, final int page, final int size) {
        return search(fiql, page, size, null);
    }

    @Override
    public PagedResult<RoleTO> search(final String fiql, final int page, final int size, final String orderBy) {
        checkPageSize(page, size);
        SearchCond cond = getSearchCond(fiql);
        List<OrderByClause> orderByClauses = getOrderByClauses(orderBy);
        return buildPagedResult(
                controller.search(cond, page, size, orderByClauses), page, size, controller.searchCount(cond));
    }

    @Override
    public RoleTO readSelf(final Long roleId) {
        return controller.readSelf(roleId);
    }

    @Override
    public Response update(final Long roleId, final RoleMod roleMod) {
        RoleTO role = controller.read(roleId);

        ResponseBuilder builder = messageContext.getRequest().evaluatePreconditions(new EntityTag(role.getETagValue()));
        if (builder == null) {
            roleMod.setId(roleId);
            RoleTO updated = controller.update(roleMod);
            builder = modificationResponse(updated);
        }

        return builder.build();
    }

    @Override
    public Response bulkDeassociation(
            final Long roleId, final ResourceDeAssociationActionType type, final List<ResourceName> resourceNames) {

        RoleTO role = controller.read(roleId);

        ResponseBuilder builder = messageContext.getRequest().evaluatePreconditions(new EntityTag(role.getETagValue()));
        if (builder == null) {
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

            if (type == ResourceDeAssociationActionType.UNLINK) {
                for (ResourceName resourceName : resourceNames) {
                    res.add(resourceName.getName(), updated.getResources().contains(resourceName.getName())
                            ? BulkActionResult.Status.FAILURE
                            : BulkActionResult.Status.SUCCESS);
                }
            } else {
                for (PropagationStatus propagationStatusTO : updated.getPropagationStatusTOs()) {
                    res.add(propagationStatusTO.getResource(), propagationStatusTO.getStatus().toString());
                }
            }

            builder = modificationResponse(res);
        }

        return builder.build();
    }

    @Override
    public Response bulkAssociation(
            final Long roleId, final ResourceAssociationActionType type, final List<ResourceName> resourceNames) {

        RoleTO role = controller.read(roleId);

        ResponseBuilder builder = messageContext.getRequest().evaluatePreconditions(new EntityTag(role.getETagValue()));
        if (builder == null) {
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
                    res.add(resourceName.getName(), updated.getResources().contains(resourceName.getName())
                            ? BulkActionResult.Status.FAILURE
                            : BulkActionResult.Status.SUCCESS);
                }
            } else {
                for (PropagationStatus propagationStatusTO : updated.getPropagationStatusTOs()) {
                    res.add(propagationStatusTO.getResource(), propagationStatusTO.getStatus().toString());
                }
            }

            builder = modificationResponse(res);
        }

        return builder.build();
    }
}
