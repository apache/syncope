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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchContext;
import org.apache.syncope.common.lib.AbstractBaseBean;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.service.JAXRSService;
import org.apache.syncope.common.rest.api.Preference;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractServiceImpl implements JAXRSService {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractServiceImpl.class);

    protected static final String OPTIONS_ALLOW = "GET,POST,OPTIONS,HEAD";

    @Context
    protected UriInfo uriInfo;

    @Context
    protected MessageContext messageContext;

    @Context
    protected SearchContext searchContext;

    protected boolean isNullPriorityAsync() {
        return BooleanUtils.toBoolean(messageContext.getHttpHeaders().getHeaderString(RESTHeaders.NULL_PRIORITY_ASYNC));
    }

    /**
     * Reads {@code Prefer} header from request and parses into a {@code Preference} instance.
     *
     * @return a {@code Preference} instance matching the passed {@code Prefer} header,
     * or {@code Preference.NONE} if missing.
     */
    private Preference getPreference() {
        return Preference.fromString(messageContext.getHttpHeaders().getHeaderString(RESTHeaders.PREFER));
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

    protected SearchCond getSearchCond(final String fiql, final String realm) {
        try {
            SearchCondVisitor visitor = new SearchCondVisitor();
            visitor.setRealm(realm);
            SearchCondition<SearchBean> sc = searchContext.getCondition(fiql, SearchBean.class);
            sc.accept(visitor);

            return visitor.getQuery();
        } catch (Exception e) {
            LOG.error("Invalid FIQL expression: {}", fiql, e);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidSearchExpression);
            sce.getElements().add(fiql);
            throw sce;
        }
    }

    protected List<OrderByClause> getOrderByClauses(final String orderBy) {
        if (StringUtils.isBlank(orderBy)) {
            return Collections.<OrderByClause>emptyList();
        }

        List<OrderByClause> result = new ArrayList<>();

        for (String clause : orderBy.split(",")) {
            String[] elems = clause.split(" ");

            if (elems.length > 0 && StringUtils.isNotBlank(elems[0])) {
                OrderByClause obc = new OrderByClause();
                obc.setField(elems[0].trim());
                if (elems.length > 1 && StringUtils.isNotBlank(elems[1])) {
                    obc.setDirection(elems[1].trim().equalsIgnoreCase(OrderByClause.Direction.ASC.name())
                            ? OrderByClause.Direction.ASC : OrderByClause.Direction.DESC);
                }
                result.add(obc);
            }
        }

        return result;
    }

    /**
     * Builds a paged result out of a list of items and additional information.
     *
     * @param <T> result type
     * @param list bare list of items to be returned
     * @param page current page
     * @param size requested size
     * @param totalCount total result size (not considering pagination)
     * @return paged result
     */
    protected <T extends AbstractBaseBean> PagedResult<T> buildPagedResult(
            final List<T> list, final int page, final int size, final int totalCount) {

        PagedResult<T> result = new PagedResult<>();
        result.getResult().addAll(list);

        result.setPage(page);
        result.setSize(result.getResult().size());
        result.setTotalCount(totalCount);

        UriBuilder builder = uriInfo.getAbsolutePathBuilder();
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        for (Map.Entry<String, List<String>> queryParam : queryParams.entrySet()) {
            builder.queryParam(queryParam.getKey(), queryParam.getValue().toArray());
        }

        if (result.getPage() > 1) {
            result.setPrev(builder.
                    replaceQueryParam(PARAM_PAGE, result.getPage() - 1).
                    replaceQueryParam(PARAM_SIZE, size).
                    build());
        }
        if ((result.getPage() - 1) * size + result.getSize() < totalCount) {
            result.setNext(builder.
                    replaceQueryParam(PARAM_PAGE, result.getPage() + 1).
                    replaceQueryParam(PARAM_SIZE, size).
                    build());
        }

        return result;
    }

}
