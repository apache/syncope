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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.mod.AnyMod;
import org.apache.syncope.common.lib.mod.ResourceAssociationMod;
import org.apache.syncope.common.lib.mod.StatusMod;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.BulkAction;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.PropagationStatus;
import org.apache.syncope.common.lib.types.ResourceAssociationAction;
import org.apache.syncope.common.lib.types.ResourceDeassociationActionType;
import org.apache.syncope.common.lib.wrap.ResourceKey;
import org.apache.syncope.common.rest.api.CollectionWrapper;
import org.apache.syncope.common.rest.api.beans.AnyListQuery;
import org.apache.syncope.common.rest.api.beans.AnySearchQuery;
import org.apache.syncope.common.rest.api.service.AnyService;
import org.apache.syncope.core.logic.AbstractAnyLogic;
import org.apache.syncope.core.logic.UserLogic;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;

public abstract class AbstractAnyService<TO extends AnyTO, MOD extends AnyMod>
        extends AbstractServiceImpl
        implements AnyService<TO, MOD> {

    protected abstract AbstractAnyLogic<TO, MOD> getAnyLogic();

    @Override
    public TO read(final Long key) {
        return getAnyLogic().read(key);
    }

    @Override
    public PagedResult<TO> list(final AnyListQuery listQuery) {
        CollectionUtils.transform(listQuery.getRealms(), new Transformer<String, String>() {

            @Override
            public String transform(final String input) {
                return StringUtils.prependIfMissing(input, SyncopeConstants.ROOT_REALM);
            }
        });

        return buildPagedResult(
                getAnyLogic().list(
                        listQuery.getPage(),
                        listQuery.getSize(),
                        getOrderByClauses(listQuery.getOrderBy()),
                        listQuery.getRealms(),
                        listQuery.isDetails()),
                listQuery.getPage(),
                listQuery.getSize(),
                getAnyLogic().count(listQuery.getRealms()));
    }

    @Override
    public PagedResult<TO> search(final AnySearchQuery searchQuery) {
        CollectionUtils.transform(searchQuery.getRealms(), new Transformer<String, String>() {

            @Override
            public String transform(final String input) {
                return StringUtils.prependIfMissing(input, SyncopeConstants.ROOT_REALM);
            }
        });

        SearchCond cond = getSearchCond(searchQuery.getFiql());
        return buildPagedResult(
                getAnyLogic().search(
                        cond,
                        searchQuery.getPage(),
                        searchQuery.getSize(),
                        getOrderByClauses(searchQuery.getOrderBy()),
                        searchQuery.getRealms(),
                        searchQuery.isDetails()),
                searchQuery.getPage(),
                searchQuery.getSize(),
                getAnyLogic().searchCount(cond, searchQuery.getRealms()));
    }

    @Override
    public Response create(final TO anyTO) {
        TO created = getAnyLogic().create(anyTO);
        return createResponse(created.getKey(), created);
    }

    @Override
    public Response update(final MOD anyMod) {
        TO any = getAnyLogic().read(anyMod.getKey());

        checkETag(any.getETagValue());

        TO updated = getAnyLogic().update(anyMod);
        return modificationResponse(updated);
    }

    @Override
    public Response delete(final Long key) {
        TO group = getAnyLogic().read(key);

        checkETag(group.getETagValue());

        TO deleted = getAnyLogic().delete(key);
        return modificationResponse(deleted);
    }

    @Override
    public Response deassociate(
            final Long key, final ResourceDeassociationActionType type, final List<ResourceKey> resourceNames) {

        TO any = getAnyLogic().read(key);

        checkETag(any.getETagValue());

        TO updated;
        switch (type) {
            case UNLINK:
                updated = getAnyLogic().unlink(key, CollectionWrapper.unwrap(resourceNames));
                break;

            case UNASSIGN:
                updated = getAnyLogic().unassign(key, CollectionWrapper.unwrap(resourceNames));
                break;

            case DEPROVISION:
                updated = getAnyLogic().deprovision(key, CollectionWrapper.unwrap(resourceNames));
                break;

            default:
                updated = getAnyLogic().read(key);
        }

        BulkActionResult result = new BulkActionResult();

        if (type == ResourceDeassociationActionType.UNLINK) {
            for (ResourceKey resourceName : resourceNames) {
                result.getResults().put(resourceName.getElement(),
                        updated.getResources().contains(resourceName.getElement())
                                ? BulkActionResult.Status.FAILURE
                                : BulkActionResult.Status.SUCCESS);
            }
        } else {
            for (PropagationStatus propagationStatusTO : updated.getPropagationStatusTOs()) {
                result.getResults().put(propagationStatusTO.getResource(),
                        BulkActionResult.Status.valueOf(propagationStatusTO.getStatus().toString()));
            }
        }

        return modificationResponse(result);
    }

    @Override
    public Response associate(
            final Long key, final ResourceAssociationAction type, final ResourceAssociationMod associationMod) {

        TO any = getAnyLogic().read(key);

        checkETag(any.getETagValue());

        TO updated;
        switch (type) {
            case LINK:
                updated = getAnyLogic().link(
                        key,
                        CollectionWrapper.unwrap(associationMod.getTargetResources()));
                break;

            case ASSIGN:
                updated = getAnyLogic().assign(
                        key,
                        CollectionWrapper.unwrap(associationMod.getTargetResources()),
                        associationMod.isChangePwd(),
                        associationMod.getPassword());
                break;

            case PROVISION:
                updated = getAnyLogic().provision(
                        key,
                        CollectionWrapper.unwrap(associationMod.getTargetResources()),
                        associationMod.isChangePwd(),
                        associationMod.getPassword());
                break;

            default:
                updated = getAnyLogic().read(key);
        }

        BulkActionResult result = new BulkActionResult();

        if (type == ResourceAssociationAction.LINK) {
            for (ResourceKey resourceName : associationMod.getTargetResources()) {
                result.getResults().put(resourceName.getElement(),
                        updated.getResources().contains(resourceName.getElement())
                                ? BulkActionResult.Status.FAILURE
                                : BulkActionResult.Status.SUCCESS);
            }
        } else {
            for (PropagationStatus propagationStatusTO : updated.getPropagationStatusTOs()) {
                result.getResults().put(propagationStatusTO.getResource(),
                        BulkActionResult.Status.valueOf(propagationStatusTO.getStatus().toString()));
            }
        }

        return modificationResponse(result);
    }

    @Override
    public BulkActionResult bulk(final BulkAction bulkAction) {
        AbstractAnyLogic<TO, MOD> logic = getAnyLogic();

        BulkActionResult result = new BulkActionResult();

        switch (bulkAction.getType()) {
            case DELETE:
                for (String key : bulkAction.getTargets()) {
                    try {
                        result.getResults().put(
                                String.valueOf(logic.delete(Long.valueOf(key)).getKey()),
                                BulkActionResult.Status.SUCCESS);
                    } catch (Exception e) {
                        LOG.error("Error performing delete for user {}", key, e);
                        result.getResults().put(key, BulkActionResult.Status.FAILURE);
                    }
                }
                break;

            case SUSPEND:
                if (logic instanceof UserLogic) {
                    for (String key : bulkAction.getTargets()) {
                        StatusMod statusMod = new StatusMod();
                        statusMod.setKey(Long.valueOf(key));
                        statusMod.setType(StatusMod.ModType.SUSPEND);
                        try {
                            result.getResults().put(
                                    String.valueOf(((UserLogic) logic).status(statusMod).getKey()),
                                    BulkActionResult.Status.SUCCESS);
                        } catch (Exception e) {
                            LOG.error("Error performing suspend for user {}", key, e);
                            result.getResults().put(key, BulkActionResult.Status.FAILURE);
                        }
                    }
                }
                break;

            case REACTIVATE:
                for (String key : bulkAction.getTargets()) {
                    StatusMod statusMod = new StatusMod();
                    statusMod.setKey(Long.valueOf(key));
                    statusMod.setType(StatusMod.ModType.REACTIVATE);
                    try {
                        result.getResults().put(
                                String.valueOf(((UserLogic) logic).status(statusMod).getKey()),
                                BulkActionResult.Status.SUCCESS);
                    } catch (Exception e) {
                        LOG.error("Error performing reactivate for user {}", key, e);
                        result.getResults().put(key, BulkActionResult.Status.FAILURE);
                    }
                }
                break;

            default:
        }

        return result;
    }

}
