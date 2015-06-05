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
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
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
import org.apache.syncope.common.rest.api.beans.AnyListQuery;
import org.apache.syncope.common.rest.api.beans.AnySearchQuery;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.syncope.core.logic.UserLogic;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
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
    public Response getUserKey(final String username) {
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
    public PagedResult<UserTO> list(final AnyListQuery listQuery) {
        CollectionUtils.transform(listQuery.getRealms(), new Transformer<String, String>() {

            @Override
            public String transform(final String input) {
                return StringUtils.prependIfMissing(input, SyncopeConstants.ROOT_REALM);
            }
        });

        return buildPagedResult(
                logic.list(
                        listQuery.getPage(),
                        listQuery.getSize(),
                        getOrderByClauses(listQuery.getOrderBy()),
                        listQuery.getRealms()),
                listQuery.getPage(),
                listQuery.getSize(),
                logic.count(listQuery.getRealms()));
    }

    @Override
    public UserTO read(final Long userKey) {
        return logic.read(userKey);
    }

    @Override
    public PagedResult<UserTO> search(final AnySearchQuery searchQuery) {
        CollectionUtils.transform(searchQuery.getRealms(), new Transformer<String, String>() {

            @Override
            public String transform(final String input) {
                return StringUtils.prependIfMissing(input, SyncopeConstants.ROOT_REALM);
            }
        });

        SearchCond cond = getSearchCond(searchQuery.getFiql());
        return buildPagedResult(
                logic.search(
                        cond,
                        searchQuery.getPage(),
                        searchQuery.getSize(),
                        getOrderByClauses(searchQuery.getOrderBy()),
                        searchQuery.getRealms()),
                searchQuery.getPage(),
                searchQuery.getSize(),
                logic.searchCount(cond, searchQuery.getRealms()));
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

        BulkActionResult result = new BulkActionResult();

        if (type == ResourceDeassociationActionType.UNLINK) {
            for (ResourceName resourceName : resourceNames) {
                result.getResults().put(
                        resourceName.getElement(), updated.getResources().contains(resourceName.getElement())
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

        BulkActionResult result = new BulkActionResult();

        if (type == ResourceAssociationActionType.LINK) {
            for (ResourceName resourceName : associationMod.getTargetResources()) {
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
        BulkActionResult result = new BulkActionResult();

        switch (bulkAction.getOperation()) {
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
                for (String key : bulkAction.getTargets()) {
                    StatusMod statusMod = new StatusMod();
                    statusMod.setKey(Long.valueOf(key));
                    statusMod.setType(StatusMod.ModType.SUSPEND);
                    try {
                        result.getResults().put(
                                String.valueOf(logic.status(statusMod).getKey()), BulkActionResult.Status.SUCCESS);
                    } catch (Exception e) {
                        LOG.error("Error performing suspend for user {}", key, e);
                        result.getResults().put(key, BulkActionResult.Status.FAILURE);
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
                                String.valueOf(logic.status(statusMod).getKey()), BulkActionResult.Status.SUCCESS);
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
