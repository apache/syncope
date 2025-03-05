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

import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityTag;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.search.SearchContext;
import org.apache.syncope.common.lib.BaseBean;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.Preference;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.AbstractQuery;
import org.apache.syncope.common.rest.api.service.JAXRSService;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public abstract class AbstractService implements JAXRSService {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractService.class);

    protected static final String OPTIONS_ALLOW = "GET,POST,OPTIONS,HEAD";

    protected static Sort sort(final String orderBy, final Sort defaultIfBlank) {
        if (StringUtils.isBlank(orderBy)) {
            return defaultIfBlank;
        }

        List<Sort.Order> clauses = new ArrayList<>();

        for (String clause : orderBy.split(",")) {
            String[] elems = clause.trim().split(" ");

            if (elems.length > 0 && StringUtils.isNotBlank(elems[0])) {
                // Manage difference among external key attribute and internal id field
                String property = "key".equals(elems[0]) ? "id" : elems[0];

                Sort.Direction direction = Sort.DEFAULT_DIRECTION;
                if (elems.length > 1 && StringUtils.isNotBlank(elems[1])) {
                    direction = elems[1].trim().equalsIgnoreCase(Sort.Direction.ASC.name())
                            ? Sort.Direction.ASC : Sort.Direction.DESC;
                }

                clauses.add(new Sort.Order(direction, property.trim()));
            }
        }

        return Sort.by(clauses);
    }

    protected static Pageable pageable(final AbstractQuery query, final Sort defaultIfOrderByIsBlank) {
        // REST query values have page starting from 1 while Pageable starts from 0
        return PageRequest.of(query.getPage() - 1, query.getSize(), sort(query.getOrderBy(), defaultIfOrderByIsBlank));
    }

    protected static Pageable pageable(final AbstractQuery query) {
        return pageable(query, sort(query.getOrderBy(), Sort.unsorted()));
    }

    @Context
    protected UriInfo uriInfo;

    @Context
    protected MessageContext messageContext;

    @Context
    protected SearchContext searchContext;

    protected String findActualKey(final AnyDAO<?> dao, final String pretendingKey) {
        String actualKey = pretendingKey;
        if (uriInfo.getPathParameters(true).containsKey("key")) {
            String keyInPath = uriInfo.getPathParameters(true).get("key").getFirst();
            if (actualKey == null) {
                actualKey = keyInPath;
            } else if (!actualKey.equals(keyInPath)) {
                SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRequest);
                sce.getElements().add("Key specified in request does not match key in the path");
                throw sce;
            }
        }
        if (actualKey == null) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidRequest);
            sce.getElements().add("Key is null");
            throw sce;
        }
        if (!SyncopeConstants.UUID_PATTERN.matcher(actualKey).matches()) {
            actualKey = dao instanceof final UserDAO userDAO
                    ? userDAO.findKey(actualKey).orElse(null)
                    : dao instanceof final GroupDAO groupDAO
                            ? groupDAO.findKey(actualKey).orElse(null)
                            : null;
        }
        return actualKey;
    }

    protected boolean isNullPriorityAsync() {
        return BooleanUtils.toBoolean(
                messageContext.getHttpServletRequest().getHeader(RESTHeaders.NULL_PRIORITY_ASYNC));
    }

    /**
     * Reads {@code Prefer} header from request and parses into a {@code Preference} instance.
     *
     * @return a {@code Preference} instance matching the passed {@code Prefer} header,
     * or {@code Preference.NONE} if missing.
     */
    protected Preference getPreference() {
        return Preference.fromString(messageContext.getHttpServletRequest().getHeader(RESTHeaders.PREFER));
    }

    protected Response.ResponseBuilder applyPreference(
            final ProvisioningResult<?> provisioningResult, final Response.ResponseBuilder builder) {

        switch (getPreference()) {
            case RETURN_NO_CONTENT:
                break;

            case RETURN_CONTENT:
            case NONE:
            default:
                builder.entity(provisioningResult);
                break;

        }
        if (getPreference() == Preference.RETURN_CONTENT || getPreference() == Preference.RETURN_NO_CONTENT) {
            builder.header(RESTHeaders.PREFERENCE_APPLIED, getPreference().toString());
        }

        return builder;
    }

    /**
     * Builds response to successful {@code create} request, taking into account any {@code Prefer} header.
     *
     * @param provisioningResult the entity just created
     * @return response to successful {@code create} request
     */
    protected Response createResponse(final ProvisioningResult<?> provisioningResult) {
        String entityId = provisioningResult.getEntity().getKey();
        Response.ResponseBuilder builder = Response.
                created(uriInfo.getAbsolutePathBuilder().path(entityId).build()).
                header(RESTHeaders.RESOURCE_KEY, entityId);

        return applyPreference(provisioningResult, builder).build();
    }

    /**
     * Builds response to successful modification request, taking into account any {@code Prefer} header.
     *
     * @param entity the entity just modified
     * @return response to successful modification request
     */
    protected Response modificationResponse(final Object entity) {
        Response.ResponseBuilder builder;
        switch (getPreference()) {
            case RETURN_NO_CONTENT:
                builder = Response.noContent();
                break;

            case RETURN_CONTENT:
            case NONE:
            default:
                builder = Response.ok(entity);
                break;
        }
        if (getPreference() == Preference.RETURN_CONTENT || getPreference() == Preference.RETURN_NO_CONTENT) {
            builder.header(RESTHeaders.PREFERENCE_APPLIED, getPreference().toString());
        }

        return builder.build();
    }

    protected void checkETag(final String etag) {
        Response.ResponseBuilder builder = messageContext.getRequest().evaluatePreconditions(new EntityTag(etag));
        if (builder != null) {
            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.ConcurrentModification);
            sce.getElements().add("Mismatching ETag value");
            throw sce;
        }
    }

    /**
     * Builds a paged result out of page.
     *
     * @param <T> result type
     * @param page page
     * @return paged result
     */
    protected <T extends BaseBean> PagedResult<T> buildPagedResult(final Page<T> page) {
        PagedResult<T> result = new PagedResult<>();
        result.getResult().addAll(page.get().toList());

        // PagedResult values expect page starting from 1 while Page starts from 0
        result.setPage(page.getNumber() + 1);
        result.setSize(result.getResult().size());
        result.setTotalCount(page.getTotalElements());

        UriBuilder builder = uriInfo.getAbsolutePathBuilder();
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        queryParams.forEach((key, value) -> builder.queryParam(key, value.toArray()));

        if (result.getPage() > 1) {
            result.setPrev(builder.
                    replaceQueryParam(PARAM_PAGE, result.getPage() - 1).
                    replaceQueryParam(PARAM_SIZE, page.getSize()).
                    build());
        }
        if ((result.getPage() - 1) * page.getSize() + result.getSize() < page.getTotalElements()) {
            result.setNext(builder.
                    replaceQueryParam(PARAM_PAGE, result.getPage() + 1).
                    replaceQueryParam(PARAM_SIZE, page.getSize()).
                    build());
        }

        return result;
    }
}
