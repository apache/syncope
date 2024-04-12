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

import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.lib.batch.BatchRequest;
import org.apache.syncope.client.ui.commons.status.StatusBean;
import org.apache.syncope.client.ui.commons.status.StatusUtils;
import org.apache.syncope.common.lib.request.ResourceAR;
import org.apache.syncope.common.lib.request.ResourceDR;
import org.apache.syncope.common.lib.request.StatusR;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.types.ResourceAssociationAction;
import org.apache.syncope.common.lib.types.ResourceDeassociationAction;
import org.apache.syncope.common.lib.types.StatusRType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.batch.BatchPayloadParser;
import org.apache.syncope.common.rest.api.batch.BatchRequestItem;
import org.apache.syncope.common.rest.api.batch.BatchResponseItem;
import org.apache.syncope.common.rest.api.service.AnyService;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;

public abstract class AbstractAnyRestClient<TO extends AnyTO> extends BaseRestClient {

    private static final long serialVersionUID = 1962529678091410544L;

    protected abstract Class<? extends AnyService<TO>> getAnyServiceClass();

    public abstract long count(String realm, String fiql, String type);

    public abstract List<TO> search(String realm, String fiql, int page, int size, SortParam<String> sort, String type);

    public TO read(final String key) {
        return getService(getAnyServiceClass()).read(key);
    }

    public ProvisioningResult<TO> delete(final String etag, final String key) {
        ProvisioningResult<TO> result;
        synchronized (this) {
            result = getService(etag, getAnyServiceClass()).delete(key).
                    readEntity(new GenericType<>() {
                    });
            resetClient(getAnyServiceClass());
        }
        return result;
    }

    protected List<BatchResponseItem> parseBatchResponse(final Response response) throws IOException {
        return BatchPayloadParser.parse(
                (InputStream) response.getEntity(), response.getMediaType(), new BatchResponseItem());
    }

    public Map<String, String> associate(
            final ResourceAssociationAction action,
            final String etag,
            final String key,
            final List<StatusBean> statuses) {

        Map<String, String> result = new LinkedHashMap<>();
        synchronized (this) {
            AnyService<?> service = getService(etag, getAnyServiceClass());
            Client client = WebClient.client(service);
            List<String> accept = client.getHeaders().get(HttpHeaders.ACCEPT);
            if (!accept.contains(RESTHeaders.MULTIPART_MIXED)) {
                client.accept(RESTHeaders.MULTIPART_MIXED);
            }

            StatusR statusR = StatusUtils.statusR(key, StatusRType.ACTIVATE, statuses);
            ResourceAR resourceAR = new ResourceAR.Builder().key(key).
                    action(action).
                    onSyncope(statusR.isOnSyncope()).
                    resources(statusR.getResources()).build();
            try {
                List<BatchResponseItem> items = parseBatchResponse(service.associate(resourceAR));
                for (int i = 0; i < items.size(); i++) {
                    result.put(
                            resourceAR.getResources().get(i),
                            getStatus(items.get(i).getStatus()));
                }
            } catch (IOException e) {
                LOG.error("While processing Batch response", e);
            }

            resetClient(getAnyServiceClass());
        }
        return result;
    }

    public Map<String, String> deassociate(
            final ResourceDeassociationAction action,
            final String etag,
            final String key,
            final List<StatusBean> statuses) {

        Map<String, String> result = new LinkedHashMap<>();
        synchronized (this) {
            AnyService<?> service = getService(etag, getAnyServiceClass());
            Client client = WebClient.client(service);
            List<String> accept = client.getHeaders().get(HttpHeaders.ACCEPT);
            if (!accept.contains(RESTHeaders.MULTIPART_MIXED)) {
                client.accept(RESTHeaders.MULTIPART_MIXED);
            }

            StatusR statusR = StatusUtils.statusR(key, StatusRType.SUSPEND, statuses);
            ResourceDR resourceDR = new ResourceDR.Builder().key(key).
                    action(action).
                    resources(statusR.getResources()).build();
            try {
                List<BatchResponseItem> items = parseBatchResponse(service.deassociate(resourceDR));
                for (int i = 0; i < items.size(); i++) {
                    result.put(
                            resourceDR.getResources().get(i),
                            getStatus(items.get(i).getStatus()));
                }
            } catch (IOException e) {
                LOG.error("While processing Batch response", e);
            }

            resetClient(getAnyServiceClass());
        }
        return result;
    }

    public Map<String, String> batch(final BatchRequest batchRequest) {
        List<BatchRequestItem> batchRequestItems = new ArrayList<>(batchRequest.getItems());

        Map<String, String> result = new LinkedHashMap<>();
        try {
            List<BatchResponseItem> batchResponseItems = batchRequest.commit().getItems();
            for (int i = 0; i < batchResponseItems.size(); i++) {
                String status = getStatus(batchResponseItems.get(i).getStatus());
                if (batchRequestItems.get(i).getRequestURI().endsWith("/status")) {
                    result.put(StringUtils.substringAfterLast(
                            StringUtils.substringBefore(batchRequestItems.get(i).getRequestURI(), "/status"), "/"),
                            status);
                } else {
                    result.put(StringUtils.substringAfterLast(
                            batchRequestItems.get(i).getRequestURI(), "/"), status);
                }
            }
        } catch (IOException e) {
            LOG.error("While processing Batch response", e);
        }

        return result;
    }
}
