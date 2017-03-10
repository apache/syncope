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
package org.apache.syncope.client.console.rest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.commons.collections4.ComparatorUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.patch.ResourceDeassociationPatch;
import org.apache.syncope.common.lib.to.BulkActionResult;
import org.apache.syncope.common.lib.to.ConnObjectTO;
import org.apache.syncope.common.lib.to.PagedConnObjectTOResult;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.types.ResourceDeassociationAction;
import org.apache.syncope.common.rest.api.beans.ConnObjectTOListQuery;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;

/**
 * Console client for invoking Rest Resources services.
 */
public class ResourceRestClient extends BaseRestClient {

    private static final long serialVersionUID = -6898907679835668987L;

    public Pair<Boolean, String> check(final ResourceTO resourceTO) {
        boolean check = false;
        String errorMessage = null;
        try {
            getService(ResourceService.class).check(resourceTO);
            check = true;
        } catch (Exception e) {
            LOG.error("Connector not found {}", resourceTO.getConnector(), e);
            errorMessage = e.getMessage();
        }

        return Pair.of(check, errorMessage);
    }

    public ConnObjectTO readConnObject(final String resource, final String anyTypeKey, final String anyKey) {
        return getService(ResourceService.class).readConnObject(resource, anyTypeKey, anyKey);
    }

    public Pair<String, List<ConnObjectTO>> listConnObjects(
            final String resource,
            final String anyTypeKey,
            final int size,
            final String pagedResultCookie,
            final SortParam<String> sort) {

        ConnObjectTOListQuery.Builder builder = new ConnObjectTOListQuery.Builder().
                pagedResultsCookie(pagedResultCookie).
                size(size).
                orderBy(toOrderBy(sort));

        final List<ConnObjectTO> result = new ArrayList<>();
        String nextPageResultCookie = null;

        PagedConnObjectTOResult list;
        try {
            list = getService(ResourceService.class).listConnObjects(resource, anyTypeKey, builder.build());
            result.addAll(list.getResult());
            nextPageResultCookie = list.getPagedResultsCookie();
        } catch (Exception e) {
            LOG.error("While listing objects on {} for any type {}", resource, anyTypeKey, e);
        }

        return Pair.of(nextPageResultCookie, result);
    }

    public ResourceTO read(final String name) {
        return getService(ResourceService.class).read(name);
    }

    public List<ResourceTO> list() {
        List<ResourceTO> resources = getService(ResourceService.class).list();
        Collections.sort(resources, new Comparator<ResourceTO>() {

            @Override
            public int compare(final ResourceTO o1, final ResourceTO o2) {
                return ComparatorUtils.<String>naturalComparator().compare(o1.getKey(), o2.getKey());
            }
        });
        return resources;
    }

    public ResourceTO create(final ResourceTO resourceTO) {
        ResourceService service = getService(ResourceService.class);
        Response response = service.create(resourceTO);
        return getObject(service, response.getLocation(), ResourceTO.class);
    }

    public void update(final ResourceTO resourceTO) {
        getService(ResourceService.class).update(resourceTO);
    }

    public void delete(final String name) {
        getService(ResourceService.class).delete(name);
    }

    public BulkActionResult bulkAssociationAction(
            final String resourceName, final String anyTypeName,
            final ResourceDeassociationAction action, final List<String> anyKeys) {

        ResourceDeassociationPatch patch = new ResourceDeassociationPatch();
        patch.setKey(resourceName);
        patch.setAnyTypeKey(anyTypeName);
        patch.setAction(action);
        patch.getAnyKyes().addAll(anyKeys);

        return getService(ResourceService.class).bulkDeassociation(patch);
    }

    public void setLatestSyncToken(final String key, final String anyType) {
        getService(ResourceService.class).setLatestSyncToken(key, anyType);
    }

    public void removeSyncToken(final String key, final String anyType) {
        getService(ResourceService.class).removeSyncToken(key, anyType);
    }
}
