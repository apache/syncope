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

import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.ext.search.SearchBean;
import org.apache.cxf.jaxrs.ext.search.SearchCondition;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.to.ConnObject;
import org.apache.syncope.common.lib.to.PagedConnObjectResult;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.ConnObjectTOQuery;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.apache.syncope.core.logic.ResourceLogic;
import org.apache.syncope.core.persistence.api.search.FilterVisitor;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.identityconnectors.framework.common.objects.filter.Filter;
import org.springframework.data.domain.Sort;

public class ResourceServiceImpl extends AbstractService implements ResourceService {

    protected final ResourceLogic logic;

    public ResourceServiceImpl(final ResourceLogic logic) {
        this.logic = logic;
    }

    @Override
    public Response create(final ResourceTO resourceTO) {
        ResourceTO created = logic.create(resourceTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(created.getKey()).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_KEY, created.getKey()).
                build();
    }

    @Override
    public void update(final ResourceTO resourceTO) {
        logic.update(resourceTO);
    }

    @Override
    public void setLatestSyncToken(final String key, final String anyTypeKey) {
        logic.setLatestSyncToken(key, anyTypeKey);
    }

    @Override
    public void removeSyncToken(final String key, final String anyTypeKey) {
        logic.removeSyncToken(key, anyTypeKey);
    }

    @Override
    public void delete(final String key) {
        logic.delete(key);
    }

    @Override
    public ResourceTO read(final String key) {
        return logic.read(key);
    }

    @Override
    public List<ResourceTO> list() {
        return logic.list();
    }

    @Override
    public Response getConnObjectKeyValue(final String key, final String anyTypeKey, final String anyKey) {
        String connObjectKeyValue = logic.getConnObjectKeyValue(key, anyTypeKey, anyKey);
        return Response.noContent().header(RESTHeaders.CONNOBJECT_KEY, connObjectKeyValue).build();
    }

    @Override
    public ConnObject readConnObject(final String key, final String anyTypeKey, final String value) {
        return SyncopeConstants.UUID_PATTERN.matcher(value).matches()
                ? logic.readConnObjectByAnyKey(key, anyTypeKey, value)
                : logic.readConnObjectByConnObjectKeyValue(key, anyTypeKey, value);
    }

    @Override
    public PagedConnObjectResult searchConnObjects(
            final String key, final String anyTypeKey, final ConnObjectTOQuery query) {

        Filter filter = null;
        Set<String> moreAttrsToGet = new HashSet<>();
        if (query.getMoreAttrsToGet() != null) {
            moreAttrsToGet.addAll(query.getMoreAttrsToGet());
        }
        if (StringUtils.isNotBlank(query.getFiql())) {
            try {
                FilterVisitor visitor = new FilterVisitor();
                SearchCondition<SearchBean> sc = searchContext.getCondition(query.getFiql(), SearchBean.class);
                sc.accept(visitor);

                filter = visitor.getQuery();
                moreAttrsToGet.addAll(visitor.getAttrs());
            } catch (Exception e) {
                LOG.error("Invalid FIQL expression: {}", query.getFiql(), e);

                SyncopeClientException sce = SyncopeClientException.build(ClientExceptionType.InvalidSearchParameters);
                sce.getElements().add(query.getFiql());
                sce.getElements().add(ExceptionUtils.getRootCauseMessage(e));
                throw sce;
            }
        }

        Pair<SearchResult, List<ConnObject>> list = logic.searchConnObjects(
                filter,
                moreAttrsToGet,
                key,
                anyTypeKey,
                query.getSize(),
                query.getPagedResultsCookie(),
                sort(query.getOrderBy(), Sort.unsorted()).get().toList());

        PagedConnObjectResult result = new PagedConnObjectResult();
        if (list.getLeft() != null) {
            result.setAllResultsReturned(list.getLeft().isAllResultsReturned());
            result.setPagedResultsCookie(list.getLeft().getPagedResultsCookie());
            result.setRemainingPagedResults(list.getLeft().getRemainingPagedResults());
        }
        result.getResult().addAll(list.getRight());

        UriBuilder builder = uriInfo.getAbsolutePathBuilder();
        MultivaluedMap<String, String> queryParams = uriInfo.getQueryParameters();
        for (Map.Entry<String, List<String>> queryParam : queryParams.entrySet()) {
            builder = builder.queryParam(queryParam.getKey(), queryParam.getValue().toArray());
        }

        if (StringUtils.isNotBlank(result.getPagedResultsCookie())) {
            result.setNext(builder.
                    replaceQueryParam(PARAM_CONNID_PAGED_RESULTS_COOKIE, result.getPagedResultsCookie()).
                    replaceQueryParam(PARAM_SIZE, query.getSize()).
                    build());
        }

        return result;
    }

    @Override
    public void check(final ResourceTO resourceTO) {
        logic.check(resourceTO);
    }
}
