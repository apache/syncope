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

import java.time.OffsetDateTime;
import java.util.List;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
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
import org.apache.syncope.core.logic.scim.SCIMConfManager;
import org.apache.syncope.core.logic.scim.SearchCondConverter;
import org.apache.syncope.core.logic.scim.SearchCondVisitor;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.ext.scimv2.api.BadRequestException;
import org.apache.syncope.ext.scimv2.api.data.ListResponse;
import org.apache.syncope.ext.scimv2.api.data.SCIMResource;
import org.apache.syncope.ext.scimv2.api.data.SCIMSearchRequest;
import org.apache.syncope.ext.scimv2.api.type.ErrorType;
import org.apache.syncope.ext.scimv2.api.type.Resource;
import org.apache.syncope.ext.scimv2.api.type.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractService<R extends SCIMResource> {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractService.class);

    @Context
    protected UriInfo uriInfo;

    @Context
    protected MessageContext messageContext;

    protected final UserDAO userDAO;

    protected final GroupDAO groupDAO;

    protected final UserLogic userLogic;

    protected final GroupLogic groupLogic;

    protected final SCIMDataBinder binder;

    protected final SCIMConfManager confManager;

    protected AbstractService(
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final UserLogic userLogic,
            final GroupLogic groupLogic,
            final SCIMDataBinder binder,
            final SCIMConfManager confManager) {

        this.userDAO = userDAO;
        this.groupDAO = groupDAO;
        this.userLogic = userLogic;
        this.groupLogic = groupLogic;
        this.binder = binder;
        this.confManager = confManager;
    }

    protected AnyDAO<?> anyDAO(final Resource type) {
        switch (type) {
            case User:
                return userDAO;

            case Group:
                return groupDAO;

            default:
                throw new UnsupportedOperationException();
        }
    }

    protected AbstractAnyLogic<?, ?, ?> anyLogic(final Resource type) {
        switch (type) {
            case User:
                return userLogic;

            case Group:
                return groupLogic;

            default:
                throw new UnsupportedOperationException();
        }
    }

    protected Response createResponse(final String key, final SCIMResource resource) {
        return Response.created(uriInfo.getAbsolutePathBuilder().path(key).build()).
                entity(resource).
                build();
    }

    protected Response updateResponse(final String key, final SCIMResource resource) {
        return Response.ok(uriInfo.getAbsolutePathBuilder().path(key).build()).
                entity(resource).
                build();
    }

    protected ResponseBuilder checkETag(final Resource resource, final String key) {
        OffsetDateTime lastChange = anyDAO(resource).findLastChange(key);
        if (lastChange == null) {
            throw new NotFoundException("Resource" + key + " not found");
        }

        return messageContext.getRequest().
                evaluatePreconditions(new EntityTag(String.valueOf(lastChange.toInstant().toEpochMilli()), true));
    }

    @SuppressWarnings("unchecked")
    protected ListResponse<R> doSearch(
            final Resource type,
            final SCIMSearchRequest request) {

        if (type == null) {
            throw new UnsupportedOperationException();
        }

        if (request.getCount() > confManager.get().getGeneralConf().getFilterMaxResults()) {
            throw new BadRequestException(ErrorType.tooMany, "Too many results requested");
        }

        SearchCondVisitor visitor = new SearchCondVisitor(type, confManager.get());

        int startIndex = request.getStartIndex() <= 1
                ? 1
                : (request.getStartIndex() / AnyDAO.DEFAULT_PAGE_SIZE) + 1;

        int itemsPerPage = request.getCount() <= 1 ? AnyDAO.DEFAULT_PAGE_SIZE : request.getCount();

        List<OrderByClause> sort;
        if (request.getSortBy() == null) {
            sort = List.of();
        } else {
            OrderByClause clause = new OrderByClause();
            clause.setField(visitor.createAttrCond(request.getSortBy()).getSchema());
            clause.setDirection(request.getSortOrder() == null || request.getSortOrder() == SortOrder.ascending
                    ? OrderByClause.Direction.ASC
                    : OrderByClause.Direction.DESC);
            sort = List.of(clause);
        }

        Pair<Integer, ? extends List<? extends AnyTO>> result = anyLogic(type).search(
                StringUtils.isBlank(request.getFilter())
                ? null
                : SearchCondConverter.convert(visitor, request.getFilter()),
                startIndex,
                itemsPerPage,
                sort,
                SyncopeConstants.ROOT_REALM,
                false);

        if (result.getLeft() > confManager.get().getGeneralConf().getFilterMaxResults()) {
            throw new BadRequestException(ErrorType.tooMany, "Too many results found");
        }

        ListResponse<R> response = new ListResponse<>(
                result.getLeft(), startIndex == 1 ? 1 : startIndex - 1, itemsPerPage);

        result.getRight().forEach(anyTO -> {
            SCIMResource resource = null;
            if (anyTO instanceof UserTO) {
                resource = binder.toSCIMUser(
                        (UserTO) anyTO,
                        uriInfo.getAbsolutePathBuilder().path(anyTO.getKey()).build().toASCIIString(),
                        request.getAttributes(),
                        request.getExcludedAttributes());
            } else if (anyTO instanceof GroupTO) {
                resource = binder.toSCIMGroup((GroupTO) anyTO,
                        uriInfo.getAbsolutePathBuilder().path(anyTO.getKey()).build().toASCIIString(),
                        request.getAttributes(),
                        request.getExcludedAttributes());
            }

            if (resource != null) {
                response.getResources().add((R) resource);
            }
        });

        return response;
    }

}
