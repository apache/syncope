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
package org.apache.syncope.ext.scimv2.cxf.service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.MembershipUR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.EntityTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.core.logic.GroupLogic;
import org.apache.syncope.core.logic.SCIMDataBinder;
import org.apache.syncope.core.logic.UserLogic;
import org.apache.syncope.core.logic.scim.SCIMConfManager;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.MembershipCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.ext.scimv2.api.BadRequestException;
import org.apache.syncope.ext.scimv2.api.data.ListResponse;
import org.apache.syncope.ext.scimv2.api.data.SCIMGroup;
import org.apache.syncope.ext.scimv2.api.data.SCIMSearchRequest;
import org.apache.syncope.ext.scimv2.api.service.GroupService;
import org.apache.syncope.ext.scimv2.api.type.ErrorType;
import org.apache.syncope.ext.scimv2.api.type.Resource;
import org.apache.syncope.ext.scimv2.api.type.SortOrder;

public class GroupServiceImpl extends AbstractService<SCIMGroup> implements GroupService {

    public GroupServiceImpl(
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final UserLogic userLogic,
            final GroupLogic groupLogic,
            final SCIMDataBinder binder,
            final SCIMConfManager confManager) {

        super(userDAO, groupDAO, userLogic, groupLogic, binder, confManager);
    }

    @Override
    public Response create(final SCIMGroup group) {
        // first create group, no members assigned
        ProvisioningResult<GroupTO> result = groupLogic.create(SCIMDataBinder.toGroupCR(group), false);

        // then assign members
        group.getMembers().forEach(member -> {
            UserUR req = new UserUR.Builder(member.getValue()).
                    membership(new MembershipUR.Builder(result.getEntity().getKey()).
                            operation(PatchOperation.ADD_REPLACE).build()).
                    build();
            try {
                userLogic.update(req, false);
            } catch (Exception e) {
                LOG.error("While setting membership of {} to {}", result.getEntity().getKey(), member.getValue(), e);
            }
        });

        return createResponse(
                result.getEntity().getKey(),
                binder.toSCIMGroup(
                        result.getEntity(),
                        uriInfo.getAbsolutePathBuilder().path(result.getEntity().getKey()).build().toASCIIString(),
                        List.of(),
                        List.of()));
    }

    @Override
    public SCIMGroup get(final String id,
            final String attributes,
            final String excludedAttributes) {

        return binder.toSCIMGroup(
                groupLogic.read(id),
                uriInfo.getAbsolutePathBuilder().build().toASCIIString(),
                List.of(ArrayUtils.nullToEmpty(StringUtils.split(attributes, ','))),
                List.of(ArrayUtils.nullToEmpty(StringUtils.split(excludedAttributes, ','))));
    }

    @Override
    public Response update(final String id) {
        return Response.status(Response.Status.NOT_IMPLEMENTED).build();
    }

    @Override
    public Response replace(final String id, final SCIMGroup group) {
        if (!id.equals(group.getId())) {
            throw new BadRequestException(ErrorType.invalidPath, "Expected " + id + ", found " + group.getId());
        }

        ResponseBuilder builder = checkETag(Resource.Group, id);
        if (builder != null) {
            return builder.build();
        }

        // save current group members
        Set<String> beforeMembers = new HashSet<>();

        MembershipCond membCond = new MembershipCond();
        membCond.setGroup(id);
        SearchCond searchCond = SearchCond.getLeaf(membCond);
        int count = userLogic.search(searchCond,
                1, 1, List.of(),
                SyncopeConstants.ROOT_REALM, false).getLeft();
        for (int page = 1; page <= (count / AnyDAO.DEFAULT_PAGE_SIZE) + 1; page++) {
            beforeMembers.addAll(userLogic.search(
                    searchCond,
                    page,
                    AnyDAO.DEFAULT_PAGE_SIZE,
                    List.of(),
                    SyncopeConstants.ROOT_REALM,
                    false).
                    getRight().stream().map(EntityTO::getKey).collect(Collectors.toSet()));
        }

        // update group, don't change members
        ProvisioningResult<GroupTO> result = groupLogic.update(
                AnyOperations.diff(SCIMDataBinder.toGroupTO(group), groupLogic.read(id), false), false);

        // assign new members
        Set<String> afterMembers = new HashSet<>();
        group.getMembers().forEach(member -> {
            afterMembers.add(member.getValue());

            if (!beforeMembers.contains(member.getValue())) {
                UserUR req = new UserUR.Builder(member.getValue()).
                        membership(new MembershipUR.Builder(result.getEntity().getKey()).
                                operation(PatchOperation.ADD_REPLACE).build()).
                        build();
                try {
                    userLogic.update(req, false);
                } catch (Exception e) {
                    LOG.error("While setting membership of {} to {}",
                            result.getEntity().getKey(), member.getValue(), e);
                }
            }
        });
        // remove unconfirmed members
        beforeMembers.stream().filter(member -> !afterMembers.contains(member)).forEach(user -> {
            UserUR req = new UserUR.Builder(user).
                    membership(new MembershipUR.Builder(result.getEntity().getKey()).
                            operation(PatchOperation.DELETE).build()).
                    build();
            try {
                userLogic.update(req, false);
            } catch (Exception e) {
                LOG.error("While removing membership of {} from {}", result.getEntity().getKey(), user, e);
            }
        });

        return updateResponse(
                result.getEntity().getKey(),
                binder.toSCIMGroup(
                        result.getEntity(),
                        uriInfo.getAbsolutePathBuilder().path(result.getEntity().getKey()).build().toASCIIString(),
                        List.of(),
                        List.of()));
    }

    @Override
    public Response delete(final String id) {
        ResponseBuilder builder = checkETag(Resource.Group, id);
        if (builder != null) {
            return builder.build();
        }

        anyLogic(Resource.Group).delete(id, false);
        return Response.noContent().build();
    }

    @Override
    public ListResponse<SCIMGroup> search(
            final String attributes,
            final String excludedAttributes,
            final String filter,
            final String sortBy,
            final SortOrder sortOrder,
            final Integer startIndex,
            final Integer count) {

        SCIMSearchRequest request = new SCIMSearchRequest(filter, sortBy, sortOrder, startIndex, count);
        if (attributes != null) {
            request.getAttributes().addAll(
                    List.of(ArrayUtils.nullToEmpty(StringUtils.split(attributes, ','))));
        }
        if (excludedAttributes != null) {
            request.getExcludedAttributes().addAll(
                    List.of(ArrayUtils.nullToEmpty(StringUtils.split(excludedAttributes, ','))));
        }

        return doSearch(Resource.Group, request);
    }

    @Override
    public ListResponse<SCIMGroup> search(final SCIMSearchRequest request) {
        return doSearch(Resource.Group, request);
    }
}
