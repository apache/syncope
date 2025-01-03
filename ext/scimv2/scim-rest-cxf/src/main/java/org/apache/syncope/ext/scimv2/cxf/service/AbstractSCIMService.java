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

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.ResponseBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.time.OffsetDateTime;
import java.util.List;
import org.apache.commons.lang3.StringUtils;
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
import org.apache.syncope.ext.scimv2.api.BadRequestException;
import org.apache.syncope.ext.scimv2.api.data.ListResponse;
import org.apache.syncope.ext.scimv2.api.data.SCIMResource;
import org.apache.syncope.ext.scimv2.api.data.SCIMSearchRequest;
import org.apache.syncope.ext.scimv2.api.type.ErrorType;
import org.apache.syncope.ext.scimv2.api.type.Resource;
import org.apache.syncope.ext.scimv2.api.type.SortOrder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

abstract class AbstractSCIMService<R extends SCIMResource> {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractSCIMService.class);

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

    protected AbstractSCIMService(
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
            case User -> {
                return userDAO;
            }

            case Group -> {
                return groupDAO;
            }

            default ->
                throw new UnsupportedOperationException();
        }
    }

    protected AbstractAnyLogic<?, ?, ?> anyLogic(final Resource type) {
        switch (type) {
            case User -> {
                return userLogic;
            }

            case Group -> {
                return groupLogic;
            }

            default ->
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
        OffsetDateTime lastChange = anyDAO(resource).findLastChange(key).
                orElseThrow(() -> new NotFoundException("Resource" + key + " not found"));

        return messageContext.getRequest().
                evaluatePreconditions(new EntityTag(String.valueOf(lastChange.toInstant().toEpochMilli()), true));
    }

    @SuppressWarnings("unchecked")
    protected ListResponse<R> doSearch(final Resource type, final SCIMSearchRequest request) {
        if (type == null) {
            throw new UnsupportedOperationException();
        }

        if (request.getCount() > confManager.get().getGeneralConf().getFilterMaxResults()) {
            throw new BadRequestException(ErrorType.tooMany, "Too many results requested");
        }

        SearchCondVisitor visitor = new SearchCondVisitor(type, confManager.get());

        int startIndex = request.getStartIndex() <= 1 ? 1 : request.getStartIndex();

        int itemsPerPage = request.getCount() <= 1 ? AnyDAO.DEFAULT_PAGE_SIZE : request.getCount();

        int page = request.getStartIndex() <= 0 ? 0 : request.getStartIndex() / itemsPerPage;

        /*
         * startIndex=1 and count=10 is supported
         * startIndex=11 and count=10 is supported
         * startIndex=21 and count=10 is supported
         * startIndex=2 and count=10 is not supported
         */
        if ((startIndex - 1) % itemsPerPage != 0) {
            throw new BadRequestException(ErrorType.invalidValue, "Unsupported startIndex value provided");
        }

        List<Sort.Order> sort;
        if (request.getSortBy() == null) {
            sort = List.of();
        } else {
            sort = List.of(new Sort.Order(
                    request.getSortOrder() == null || request.getSortOrder() == SortOrder.ascending
                    ? Sort.Direction.ASC
                    : Sort.Direction.DESC,
                    visitor.createAttrCond(request.getSortBy()).getSchema()));
        }

        Page<? extends AnyTO> result = anyLogic(type).search(
                StringUtils.isBlank(request.getFilter())
                ? null
                : SearchCondConverter.convert(visitor, request.getFilter()),
                PageRequest.of(page, itemsPerPage, Sort.by(sort)),
                SyncopeConstants.ROOT_REALM,
                true,
                true);

        if (result.getTotalElements() > confManager.get().getGeneralConf().getFilterMaxResults()) {
            throw new BadRequestException(ErrorType.tooMany, "Too many results found");
        }

        ListResponse<R> response = new ListResponse<>(result.getTotalElements(), startIndex, itemsPerPage);

        result.forEach(anyTO -> {
            SCIMResource resource = null;
            if (anyTO instanceof UserTO userTO) {
                resource = binder.toSCIMUser(
                        userTO,
                        uriInfo.getAbsolutePathBuilder().path(anyTO.getKey()).build().toASCIIString(),
                        request.getAttributes(),
                        request.getExcludedAttributes());
            } else if (anyTO instanceof GroupTO groupTO) {
                resource = binder.toSCIMGroup(
                        groupTO,
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
