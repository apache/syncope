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
package org.apache.syncope.core.services;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.ext.MessageContext;
import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.cxf.jaxrs.ext.search.SearchContext;
import org.apache.syncope.common.AbstractBaseBean;
import org.apache.syncope.common.SyncopeClientException;
import org.apache.syncope.common.services.JAXRSService;
import org.apache.syncope.common.reqres.PagedResult;
import org.apache.syncope.common.types.ClientExceptionType;
import org.apache.syncope.common.types.Preference;
import org.apache.syncope.common.types.RESTHeaders;
import org.apache.syncope.core.persistence.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.dao.search.SearchCond;
import org.apache.syncope.core.rest.data.SearchCondVisitor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractServiceImpl implements JAXRSService {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractServiceImpl.class);

    protected static final String OPTIONS_ALLOW = "GET,POST,OPTIONS,HEAD";

    @Context
    protected UriInfo uriInfo;

    @Context
    protected MessageContext messageContext;

    @Context
    protected SearchContext searchContext;

    /**
     * Reads <tt>Prefer</tt> header from request and parses into a <tt>Preference</tt> instance.
     *
     * @return a <tt>Preference</tt> instance matching the passed <tt>Prefer</tt> header,
     * or <tt>Preference.NONE</tt> if missing.
     */
    protected Preference getPreference() {
        return Preference.fromString(messageContext.getHttpHeaders().getHeaderString(RESTHeaders.PREFER));
    }

    /**
     * Builds response to successful <tt>create</tt> request, taking into account any <tt>Prefer</tt> header.
     *
     * @param id identifier of the created entity
     * @param entity the entity just created
     * @return response to successful <tt>create</tt> request
     */
    protected Response.ResponseBuilder createResponse(final Object id, final Object entity) {
        URI location = uriInfo.getAbsolutePathBuilder().path(String.valueOf(id)).build();

        Response.ResponseBuilder builder = Response.
                created(location).
                header(RESTHeaders.RESOURCE_ID, id);

        switch (getPreference()) {
            case RETURN_NO_CONTENT:
                break;

            case RETURN_CONTENT:
            case NONE:
            default:
                builder = builder.entity(entity);
                break;

        }
        if (getPreference() == Preference.RETURN_CONTENT || getPreference() == Preference.RETURN_NO_CONTENT) {
            builder = builder.header(RESTHeaders.PREFERENCE_APPLIED, getPreference().toString());
        }

        return builder;
    }

    /**
     * Builds response to successful modification request, taking into account any <tt>Prefer</tt> header.
     *
     * @param entity the entity just modified
     * @return response to successful modification request
     */
    protected Response.ResponseBuilder modificationResponse(final Object entity) {
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
            builder = builder.header(RESTHeaders.PREFERENCE_APPLIED, getPreference().toString());
        }

        return builder;
    }

    /**
     * Checks whether given page and size values are valid.
     *
     * [SYNCOPE-461] Keep this method until BVal 1.0 (implementing JSR 303 1.1 which will work with CXF JAX-RS
     * validation) is available.
     *
     * @param page result page number
     * @param size number of entries per page
     * @see https://issues.apache.org/jira/browse/SYNCOPE-461
     */
    protected void checkPageSize(final int page, final int size) {
        if (page <= 0 || size <= 0) {
            LOG.error("Invalid page / size specified: {},{}", page, size);

            SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidPageOrSize);
            sce.getElements().add(page);
            sce.getElements().add(size);
            throw sce;
        }
    }

    protected SearchCond getSearchCond(final String fiql) {
        try {
            SearchCondVisitor visitor = new SearchCondVisitor();
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

        List<OrderByClause> result = new ArrayList<OrderByClause>();

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
     * @return
     */
    protected <T extends AbstractBaseBean> PagedResult<T> buildPagedResult(
            final List<T> list, final int page, final int size, final int totalCount) {

        PagedResult<T> result = new PagedResult<T>();
        result.getResult().addAll(list);

        result.setPage(page);
        result.setSize(result.getResult().size());
        result.setTotalCount(totalCount);

        UriBuilder builder = uriInfo.getAbsolutePathBuilder();
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        for (Map.Entry<String, List<String>> queryParam : queryParams.entrySet()) {
            builder = builder.queryParam(queryParam.getKey(), queryParam.getValue().toArray());
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
