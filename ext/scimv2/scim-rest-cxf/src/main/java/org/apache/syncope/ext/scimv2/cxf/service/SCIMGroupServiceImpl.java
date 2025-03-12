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

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.jexl3.MapContext;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.request.MembershipUR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.core.logic.GroupLogic;
import org.apache.syncope.core.logic.SCIMDataBinder;
import org.apache.syncope.core.logic.UserLogic;
import org.apache.syncope.core.logic.scim.SCIMConfManager;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.DAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.MembershipCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.provisioning.api.jexl.JexlUtils;
import org.apache.syncope.ext.scimv2.api.BadRequestException;
import org.apache.syncope.ext.scimv2.api.data.ListResponse;
import org.apache.syncope.ext.scimv2.api.data.Member;
import org.apache.syncope.ext.scimv2.api.data.SCIMGroup;
import org.apache.syncope.ext.scimv2.api.data.SCIMPatchOp;
import org.apache.syncope.ext.scimv2.api.data.SCIMSearchRequest;
import org.apache.syncope.ext.scimv2.api.service.SCIMGroupService;
import org.apache.syncope.ext.scimv2.api.type.ErrorType;
import org.apache.syncope.ext.scimv2.api.type.PatchOp;
import org.apache.syncope.ext.scimv2.api.type.Resource;
import org.apache.syncope.ext.scimv2.api.type.SortOrder;
import org.springframework.data.domain.PageRequest;
import org.springframework.util.CollectionUtils;

public class SCIMGroupServiceImpl extends AbstractSCIMService<SCIMGroup> implements SCIMGroupService {

    public SCIMGroupServiceImpl(
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final UserLogic userLogic,
            final GroupLogic groupLogic,
            final SCIMDataBinder binder,
            final SCIMConfManager confManager) {

        super(userDAO, groupDAO, userLogic, groupLogic, binder, confManager);
    }

    private void changeMembership(final String user, final String group, final PatchOp patchOp) {
        UserUR req = new UserUR.Builder(user).
                membership(new MembershipUR.Builder(group).operation(patchOp == PatchOp.remove
                        ? PatchOperation.DELETE : PatchOperation.ADD_REPLACE).build()).
                build();
        try {
            userLogic.update(req, false);
        } catch (Exception e) {
            LOG.error("While applying {} on membership of {} to {}", patchOp, group, user, e);
        }
    }

    @Override
    public Response create(final SCIMGroup group) {
        // first create group, no members assigned
        ProvisioningResult<GroupTO> result = groupLogic.create(binder.toGroupCR(group), false);

        // then assign members
        group.getMembers().forEach(member -> changeMembership(
                member.getValue(), result.getEntity().getKey(), PatchOp.add));

        return createResponse(
                result.getEntity().getKey(),
                binder.toSCIMGroup(
                        result.getEntity(),
                        uriInfo.getAbsolutePathBuilder().path(result.getEntity().getKey()).build().toASCIIString(),
                        List.of(),
                        List.of()));
    }

    @Override
    public SCIMGroup get(
            final String id,
            final String attributes,
            final String excludedAttributes) {

        return binder.toSCIMGroup(
                groupLogic.read(id),
                uriInfo.getAbsolutePathBuilder().build().toASCIIString(),
                List.of(ArrayUtils.nullToEmpty(StringUtils.split(attributes, ','))),
                List.of(ArrayUtils.nullToEmpty(StringUtils.split(excludedAttributes, ','))));
    }

    private Set<String> members(final String group) {
        Set<String> members = new HashSet<>();

        MembershipCond membCond = new MembershipCond();
        membCond.setGroup(group);
        SearchCond searchCond = SearchCond.of(membCond);
        long count = userLogic.search(
                searchCond, PageRequest.of(0, 1), SyncopeConstants.ROOT_REALM, true, false).getTotalElements();
        for (int page = 0; page <= (count / AnyDAO.DEFAULT_PAGE_SIZE); page++) {
            members.addAll(userLogic.search(
                    searchCond,
                    PageRequest.of(page, AnyDAO.DEFAULT_PAGE_SIZE, DAO.DEFAULT_SORT),
                    SyncopeConstants.ROOT_REALM,
                    true,
                    false).
                    get().map(UserTO::getKey).collect(Collectors.toSet()));
        }

        return members;
    }

    @Override
    public Response update(final String id, final SCIMPatchOp patch) {
        ResponseBuilder builder = checkETag(Resource.Group, id);
        if (builder != null) {
            return builder.build();
        }

        patch.getOperations().forEach(op -> {
            if (op.getPath() != null && "members".equals(op.getPath().getAttribute())) {
                if (CollectionUtils.isEmpty(op.getValue())) {
                    members(id).stream().filter(member -> op.getPath().getFilter() == null
                            ? true
                            : BooleanUtils.toBoolean(JexlUtils.evaluateExpr(
                                    SCIMDataBinder.filter2JexlExpression(op.getPath().getFilter()),
                                    new MapContext(Map.of("value", member))).toString())).
                            forEach(member -> changeMembership(member, id, op.getOp()));
                } else {
                    op.getValue().stream().map(Member.class::cast).
                            forEach(member -> changeMembership(member.getValue(), id, op.getOp()));
                }
            } else {
                groupLogic.update(binder.toGroupUR(groupLogic.read(id), op), false);
            }
        });

        return updateResponse(
                id,
                binder.toSCIMGroup(
                        groupLogic.read(id),
                        uriInfo.getAbsolutePathBuilder().path(id).build().toASCIIString(),
                        List.of(),
                        List.of()));
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
        Set<String> beforeMembers = members(id);

        // update group, don't change members
        GroupUR req = AnyOperations.diff(binder.toGroupTO(group, true), groupLogic.read(id), false);
        req.getResources().clear();
        req.getAuxClasses().clear();
        ProvisioningResult<GroupTO> result = groupLogic.update(req, false);

        // assign new members
        Set<String> afterMembers = new HashSet<>();
        group.getMembers().forEach(member -> {
            afterMembers.add(member.getValue());

            if (!beforeMembers.contains(member.getValue())) {
                changeMembership(member.getValue(), result.getEntity().getKey(), PatchOp.add);
            }
        });
        // remove unconfirmed members
        beforeMembers.stream().filter(member -> !afterMembers.contains(member)).forEach(user -> changeMembership(
                user, result.getEntity().getKey(), PatchOp.remove));

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
