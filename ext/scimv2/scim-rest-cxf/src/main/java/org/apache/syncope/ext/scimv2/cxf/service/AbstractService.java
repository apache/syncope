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
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.core.logic.AbstractAnyLogic;
import org.apache.syncope.core.logic.GroupLogic;
import org.apache.syncope.core.logic.SCIMDataBinder;
import org.apache.syncope.core.logic.UserLogic;
import org.apache.syncope.core.logic.scim.SearchCondConverter;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.spring.ApplicationContextProvider;
import org.apache.syncope.ext.scimv2.api.data.ListResponse;
import org.apache.syncope.ext.scimv2.api.data.SCIMResource;
import org.apache.syncope.ext.scimv2.api.data.SCIMSearchRequest;
import org.apache.syncope.ext.scimv2.api.type.Resource;
import org.apache.syncope.ext.scimv2.api.type.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.syncope.ext.scimv2.api.service.SearchService;

abstract class AbstractService<R extends SCIMResource> implements SearchService<R> {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractService.class);

    @Context
    protected UriInfo uriInfo;

    @Context
    protected MessageContext messageContext;

    private UserLogic userLogic;

    private GroupLogic groupLogic;

    private SCIMDataBinder binder;

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

    protected SCIMDataBinder binder() {
        synchronized (this) {
            if (binder == null) {
                binder = ApplicationContextProvider.getApplicationContext().getBean(SCIMDataBinder.class);
            }
        }
        return binder;
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

    @SuppressWarnings("unchecked")
    protected ListResponse<R> doSearch(
            final Resource type,
            final SCIMSearchRequest request) {

        if (type == null) {
            throw new UnsupportedOperationException();
        }

        int startIndex = request.getStartIndex() == null || request.getStartIndex() <= 1
                ? 1
                : (request.getStartIndex() / AnyDAO.DEFAULT_PAGE_SIZE) + 1;

        Pair<Integer, ? extends List<? extends AnyTO>> result = anyLogic(type).search(
                StringUtils.isBlank(request.getFilter()) ? null : SearchCondConverter.convert(request.getFilter()),
                startIndex,
                AnyDAO.DEFAULT_PAGE_SIZE,
                Collections.<OrderByClause>emptyList(),
                SyncopeConstants.ROOT_REALM,
                false);

        ListResponse<R> response = new ListResponse<>(
                result.getLeft(), startIndex == 1 ? 1 : startIndex - 1, AnyDAO.DEFAULT_PAGE_SIZE);

        result.getRight().forEach(anyTO -> {
            SCIMResource resource = null;
            if (anyTO instanceof UserTO) {
                resource = binder().toSCIMUser(
                        (UserTO) anyTO,
                        uriInfo.getAbsolutePathBuilder().path(anyTO.getKey()).build().toASCIIString());
            } else if (anyTO instanceof GroupTO) {
                resource = binder().toSCIMGroup(
                        (GroupTO) anyTO,
                        uriInfo.getAbsolutePathBuilder().path(anyTO.getKey()).build().toASCIIString());
            }

            if (resource != null) {
                response.getResources().add((R) resource);
            }
        });

        return response;
    }

    @Override
    public ListResponse<R> search(
            final List<String> attributes,
            final List<String> excludedAttributes,
            final String filter,
            final String sortBy,
            final SortOrder sortOrder,
            final Integer startIndex,
            final Integer count) {

        SCIMSearchRequest request = new SCIMSearchRequest(filter, sortBy, sortOrder, startIndex, count);
        if (attributes != null) {
            request.getAttributes().addAll(attributes);
        }
        if (excludedAttributes != null) {
            request.getExcludedAttributes().addAll(excludedAttributes);
        }

        return doSearch(null, request);
    }

    @Override
    public ListResponse<R> search(final SCIMSearchRequest request) {
        return doSearch(null, request);
    }

}
