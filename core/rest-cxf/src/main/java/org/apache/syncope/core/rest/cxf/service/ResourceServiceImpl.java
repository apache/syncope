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

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.patch.ResourceDeassociationPatch;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.PagedConnObjectTOResult;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.ConnObjectTOListQuery;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.apache.syncope.core.logic.AbstractResourceAssociator;
import org.apache.syncope.core.logic.AnyObjectLogic;
import org.apache.syncope.core.logic.ResourceLogic;
import org.apache.syncope.core.logic.GroupLogic;
import org.apache.syncope.core.logic.UserLogic;
import org.identityconnectors.framework.common.objects.SearchResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ResourceServiceImpl extends AbstractServiceImpl implements ResourceService {

    @Autowired
    private ResourceLogic logic;

    @Autowired
    private AnyObjectLogic anyObjectLogic;

    @Autowired
    private UserLogic userLogic;

    @Autowired
    private GroupLogic groupLogic;

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
    public ConnObjectTO readConnObject(final String key, final String anyTypeKey, final String anyKey) {
        return logic.readConnObject(key, anyTypeKey, anyKey);
    }

    @Override
    public PagedConnObjectTOResult listConnObjects(
            final String key, final String anyTypeKey, final ConnObjectTOListQuery listQuery) {

        Pair<SearchResult, List<ConnObjectTO>> list = logic.listConnObjects(key, anyTypeKey,
                listQuery.getSize(), listQuery.getPagedResultsCookie(), getOrderByClauses(listQuery.getOrderBy()));

        PagedConnObjectTOResult result = new PagedConnObjectTOResult();
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
                    build());
        }

        return result;
    }

    @Override
    public void check(final ResourceTO resourceTO) {
        logic.check(resourceTO);
    }

    @Override
    public BulkActionResult bulkDeassociation(final ResourceDeassociationPatch patch) {
        AbstractResourceAssociator<? extends AnyTO> associator =
                patch.getAnyTypeKey().equalsIgnoreCase(AnyTypeKind.USER.name())
                ? userLogic
                : patch.getAnyTypeKey().equalsIgnoreCase(AnyTypeKind.GROUP.name())
                ? groupLogic
                : anyObjectLogic;

        BulkActionResult result = new BulkActionResult();

        for (String anyKey : patch.getAnyKyes()) {
            Set<String> resources = Collections.singleton(patch.getKey());
            try {
                switch (patch.getAction()) {
                    case DEPROVISION:
                        associator.deprovision(anyKey, resources, isNullPriorityAsync());
                        break;

                    case UNASSIGN:
                        associator.unassign(anyKey, resources, isNullPriorityAsync());
                        break;

                    case UNLINK:
                        associator.unlink(anyKey, resources);
                        break;

                    default:
                }

                result.getResults().put(anyKey, BulkActionResult.Status.SUCCESS);
            } catch (Exception e) {
                LOG.warn("While executing {} on {} {}", patch.getAction(), patch.getAnyTypeKey(), anyKey, e);
                result.getResults().put(anyKey, BulkActionResult.Status.FAILURE);
            }
        }

        return result;
    }
}
