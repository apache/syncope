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

import java.util.Collections;
import java.util.List;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.logic.AbstractAnyLogic;
import org.apache.syncope.core.logic.GroupLogic;
import org.apache.syncope.core.logic.UserLogic;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.search.MembershipCond;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.ext.scimv2.api.data.Display;
import org.apache.syncope.ext.scimv2.api.data.Group;
import org.apache.syncope.ext.scimv2.api.data.ListResponse;
import org.apache.syncope.ext.scimv2.api.data.Member;
import org.apache.syncope.ext.scimv2.api.data.Meta;
import org.apache.syncope.ext.scimv2.api.data.SCIMGroup;
import org.apache.syncope.ext.scimv2.api.data.SCIMResource;
import org.apache.syncope.ext.scimv2.api.data.SCIMUser;
import org.apache.syncope.ext.scimv2.api.service.SCIMService;
import org.apache.syncope.ext.scimv2.api.type.Function;
import org.apache.syncope.ext.scimv2.api.type.Resource;
import org.apache.syncope.ext.scimv2.api.type.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractSCIMService<R extends SCIMResource> implements SCIMService<R> {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractSCIMService.class);

    @Context
    protected UriInfo uriInfo;

    @Context
    protected MessageContext messageContext;

    private UserLogic userLogic;

    private GroupLogic groupLogic;

    protected UserLogic userLogic() {
        synchronized (this) {
            if (userLogic == null) {
                userLogic = ApplicationContextProvider.getApplicationContext().getBean(UserLogic.class);
            }
        }
        return userLogic;
    }

    protected GroupLogic groupLogic() {
        synchronized (this) {
            if (groupLogic == null) {
                groupLogic = ApplicationContextProvider.getApplicationContext().getBean(GroupLogic.class);
            }
        }
        return groupLogic;
    }

    protected AbstractAnyLogic<?, ?> anyLogic(final Resource type) {
        switch (type) {
            case User:
                return userLogic();

            case Group:
                return groupLogic();

            default:
                throw new UnsupportedOperationException();
        }
    }

    protected SCIMUser toSCIMUser(final UserTO userTO, final String location) {
        SCIMUser user = new SCIMUser(
                userTO.getKey(),
                Collections.singletonList(Resource.User.schema()),
                new Meta(
                        Resource.User,
                        userTO.getCreationDate(),
                        userTO.getLastChangeDate() == null
                        ? userTO.getCreationDate() : userTO.getLastChangeDate(),
                        userTO.getETagValue(),
                        location),
                userTO.getUsername(),
                !userTO.isSuspended());

        for (MembershipTO membership : userTO.getMemberships()) {
            user.getGroups().add(new Group(
                    membership.getGroupKey(),
                    StringUtils.substringBefore(location, "/Users") + "/Groups/" + membership.getGroupKey(),
                    membership.getGroupName(),
                    Function.direct));
        }
        for (MembershipTO membership : userTO.getDynMemberships()) {
            user.getGroups().add(new Group(
                    membership.getGroupKey(),
                    StringUtils.substringBefore(location, "/Users") + "/Groups/" + membership.getGroupKey(),
                    membership.getGroupName(),
                    Function.indirect));
        }

        for (String role : userTO.getRoles()) {
            user.getRoles().add(new Display(role, null));
        }

        return user;
    }

    protected SCIMGroup toSCIMGroup(final GroupTO groupTO, final String location) {
        SCIMGroup group = new SCIMGroup(
                groupTO.getKey(),
                Collections.singletonList(Resource.Group.schema()),
                new Meta(
                        Resource.Group,
                        groupTO.getCreationDate(),
                        groupTO.getLastChangeDate() == null
                        ? groupTO.getCreationDate() : groupTO.getLastChangeDate(),
                        groupTO.getETagValue(),
                        location),
                groupTO.getName());

        MembershipCond membCond = new MembershipCond();
        membCond.setGroup(groupTO.getKey());
        SearchCond searchCond = SearchCond.getLeafCond(membCond);

        int count = userLogic().
                search(searchCond, 1, 1, Collections.<OrderByClause>emptyList(), SyncopeConstants.ROOT_REALM, false).
                getLeft();

        for (int page = 1; page <= (count / AnyDAO.DEFAULT_PAGE_SIZE) + 1; page++) {
            List<UserTO> users = userLogic().search(
                    searchCond,
                    page,
                    AnyDAO.DEFAULT_PAGE_SIZE,
                    Collections.<OrderByClause>emptyList(),
                    SyncopeConstants.ROOT_REALM,
                    false).
                    getRight();
            for (UserTO userTO : users) {
                group.getMembers().add(new Member(
                        userTO.getKey(),
                        StringUtils.substringBefore(location, "/Groups") + "/Users/" + userTO.getKey(),
                        userTO.getUsername(),
                        Resource.User));
            }
        }

        return group;
    }

    @SuppressWarnings("unchecked")
    protected ListResponse<R> doSearch(
            final Resource type,
            final Integer startIndex,
            final Integer count,
            final String filter,
            final String sortBy,
            final SortOrder sortOrder,
            final List<String> attributes) {

        if (type == null) {
            throw new UnsupportedOperationException();
        }

        int page = startIndex == null || startIndex <= 1 ? 1 : (startIndex / AnyDAO.DEFAULT_PAGE_SIZE) + 1;

        Pair<Integer, ? extends List<? extends AnyTO>> result = anyLogic(type).search(
                null,
                page,
                AnyDAO.DEFAULT_PAGE_SIZE,
                Collections.<OrderByClause>emptyList(),
                SyncopeConstants.ROOT_REALM,
                false);

        ListResponse<R> response = new ListResponse<>(
                result.getLeft(), startIndex == null || startIndex <= 1 ? 1 : startIndex, AnyDAO.DEFAULT_PAGE_SIZE);

        for (AnyTO anyTO : result.getRight()) {
            SCIMResource resource = null;
            if (anyTO instanceof UserTO) {
                resource = toSCIMUser(
                        (UserTO) anyTO,
                        uriInfo.getAbsolutePathBuilder().path(anyTO.getKey()).build().toASCIIString());
            } else if (anyTO instanceof GroupTO) {
                resource = toSCIMGroup(
                        (GroupTO) anyTO,
                        uriInfo.getAbsolutePathBuilder().path(anyTO.getKey()).build().toASCIIString());
            }

            if (resource != null) {
                response.getResources().add((R) resource);
            }
        }

        return response;
    }

    @Override
    public ListResponse<R> search(
            final Integer startIndex,
            final Integer count,
            final String filter,
            final String sortBy,
            final SortOrder sortOrder,
            final List<String> attributes) {

        return doSearch(null, startIndex, count, filter, sortBy, sortOrder, attributes);
    }
}
