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

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.lib.WebClientBuilder;
import org.apache.syncope.common.lib.to.ConnObject;
import org.apache.syncope.common.lib.to.PagedConnObjectResult;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.ConnObjectTOQuery;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;

/**
 * Console client for invoking Rest Resources services.
 */
public class ResourceRestClient extends BaseRestClient {

    private static final long serialVersionUID = -6898907679835668987L;

    public boolean check(final String coreAddress, final String domain, final String jwt, final String key)
            throws IOException {

        WebClient client = WebClientBuilder.build(coreAddress).
                path("resources").
                accept(MediaType.APPLICATION_JSON_TYPE).
                type(MediaType.APPLICATION_JSON_TYPE).
                header(RESTHeaders.DOMAIN, domain).
                authorization("Bearer " + jwt);
        Response response = client.path(key).get();
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            response = client.back(false).path("check").
                    post(IOUtils.toString((InputStream) response.getEntity(), StandardCharsets.UTF_8));
            return response.getStatus() == Response.Status.NO_CONTENT.getStatusCode();
        }
        return false;
    }

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

    public ConnObject readConnObject(final String resource, final String anyTypeKey, final String anyKey) {
        return getService(ResourceService.class).readConnObject(resource, anyTypeKey, anyKey);
    }

    public String getConnObjectKeyValue(final String resource, final String anyTypeKey, final String anyKey) {
        try {
            Response response = getService(ResourceService.class).getConnObjectKeyValue(resource, anyTypeKey, anyKey);
            if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
                return response.getHeaderString(RESTHeaders.CONNOBJECT_KEY);
            }
        } catch (Exception e) {
            LOG.debug("Error fetching connector object key", e);
        }
        LOG.error("Unable to determine connector object key value for resource {}, {} and {}",
                resource, anyTypeKey, anyKey);
        return null;
    }

    public Pair<String, List<ConnObject>> searchConnObjects(
            final String resource,
            final String anyTypeKey,
            final ConnObjectTOQuery.Builder queryBuilder,
            final SortParam<String> sortParam) {

        final List<ConnObject> result = new ArrayList<>();
        String nextPageResultCookie = null;

        PagedConnObjectResult list;
        try {
            if (sortParam != null) {
                queryBuilder.orderBy(toOrderBy(sortParam));
            }
            list = getService(ResourceService.class).searchConnObjects(resource, anyTypeKey, queryBuilder.build());
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
        List<ResourceTO> resources = List.of();
        try {
            resources = getService(ResourceService.class).list();
            resources.sort(Comparator.comparing(ResourceTO::getKey));
        } catch (Exception e) {
            LOG.error("Could not fetch the Resource list", e);
        }

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

    public void setLatestSyncToken(final String key, final String anyType) {
        getService(ResourceService.class).setLatestSyncToken(key, anyType);
    }

    public void removeSyncToken(final String key, final String anyType) {
        getService(ResourceService.class).removeSyncToken(key, anyType);
    }
}
