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
package org.apache.syncope.core.rest.cxf.service;

import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.mod.GroupMod;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.types.ResourceAssociationActionType;
import org.apache.syncope.common.lib.types.ResourceDeassociationActionType;
import org.apache.syncope.common.lib.wrap.ResourceName;
import org.apache.syncope.common.rest.api.CollectionWrapper;
import org.apache.syncope.common.rest.api.service.GroupService;
import org.apache.syncope.core.logic.GroupLogic;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GroupServiceImpl extends AbstractServiceImpl implements GroupService {

    @Autowired
    private GroupLogic logic;

    @Override
    public List<GroupTO> children(final Long groupKey) {
        return logic.children(groupKey);
    }

    @Override
    public Response create(final GroupTO groupTO) {
        GroupTO created = logic.create(groupTO);
        return createResponse(created.getKey(), created);
    }

    @Override
    public Response delete(final Long groupKey) {
        GroupTO group = logic.read(groupKey);

        checkETag(group.getETagValue());

        GroupTO deleted = logic.delete(groupKey);
        return modificationResponse(deleted);
    }

    @Override
    public PagedResult<GroupTO> list() {
        return list(DEFAULT_PARAM_PAGE_VALUE, DEFAULT_PARAM_SIZE_VALUE, null);
    }

    @Override
    public PagedResult<GroupTO> list(final String orderBy) {
        return list(DEFAULT_PARAM_PAGE_VALUE, DEFAULT_PARAM_SIZE_VALUE, orderBy);
    }

    @Override
    public PagedResult<GroupTO> list(final Integer page, final Integer size) {
        return list(page, size, null);
    }

    @Override
    public PagedResult<GroupTO> list(final Integer page, final Integer size, final String orderBy) {
        List<OrderByClause> orderByClauses = getOrderByClauses(orderBy);
        return buildPagedResult(logic.list(page, size, orderByClauses), page, size, logic.count());
    }

    @Override
    public GroupTO parent(final Long groupKey) {
        return logic.parent(groupKey);
    }

    @Override
    public GroupTO read(final Long groupKey) {
        return logic.read(groupKey);
    }

    @Override
    public PagedResult<GroupTO> search(final String fiql) {
        return search(fiql, DEFAULT_PARAM_PAGE_VALUE, DEFAULT_PARAM_SIZE_VALUE, null);
    }

    @Override
    public PagedResult<GroupTO> search(final String fiql, final String orderBy) {
        return search(fiql, DEFAULT_PARAM_PAGE_VALUE, DEFAULT_PARAM_SIZE_VALUE, orderBy);
    }

    @Override
    public PagedResult<GroupTO> search(final String fiql, final Integer page, final Integer size) {
        return search(fiql, page, size, null);
    }

    @Override
    public PagedResult<GroupTO> search(final String fiql, final Integer page, final Integer size, final String orderBy) {
        SearchCond cond = getSearchCond(fiql);
        List<OrderByClause> orderByClauses = getOrderByClauses(orderBy);
        return buildPagedResult(
                logic.search(cond, page, size, orderByClauses), page, size, logic.searchCount(cond));
    }

    @Override
    public GroupTO readSelf(final Long groupKey) {
        return logic.readSelf(groupKey);
    }

    @Override
    public Response update(final Long groupKey, final GroupMod groupMod) {
        GroupTO group = logic.read(groupKey);

        checkETag(group.getETagValue());

        groupMod.setKey(groupKey);
        GroupTO updated = logic.update(groupMod);
        return modificationResponse(updated);
    }

    @Override
    public Response bulkDeassociation(
            final Long groupKey, final ResourceDeassociationActionType type, final List<ResourceName> resourceNames) {

        GroupTO group = logic.read(groupKey);

        checkETag(group.getETagValue());

        GroupTO updated;
        switch (type) {
            case UNLINK:
                updated = logic.unlink(groupKey, CollectionWrapper.unwrap(resourceNames));
                break;

            case UNASSIGN:
                updated = logic.unassign(groupKey, CollectionWrapper.unwrap(resourceNames));
                break;

            case DEPROVISION:
                updated = logic.deprovision(groupKey, CollectionWrapper.unwrap(resourceNames));
                break;

            default:
                updated = logic.read(groupKey);
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
            final Long groupKey, final ResourceAssociationActionType type, final List<ResourceName> resourceNames) {

        GroupTO group = logic.read(groupKey);

        checkETag(group.getETagValue());

        GroupTO updated;
        switch (type) {
            case LINK:
                updated = logic.link(groupKey, CollectionWrapper.unwrap(resourceNames));
                break;

            case ASSIGN:
                updated = logic.assign(groupKey, CollectionWrapper.unwrap(resourceNames), false, null);
                break;

            case PROVISION:
                updated = logic.provision(groupKey, CollectionWrapper.unwrap(resourceNames), false, null);
                break;

            default:
                updated = logic.read(groupKey);
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
    public BulkActionResult bulk(final BulkAction bulkAction) {
        return logic.bulk(bulkAction);
    }
}
