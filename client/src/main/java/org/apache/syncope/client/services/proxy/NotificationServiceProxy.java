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
package org.apache.syncope.client.services.proxy;

import java.net.URI;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.Response;

import org.apache.syncope.common.SyncopeConstants;
import org.apache.syncope.common.services.NotificationService;
import org.apache.syncope.common.to.NotificationTO;
import org.springframework.web.client.RestTemplate;

public class NotificationServiceProxy extends SpringServiceProxy implements NotificationService {

    public NotificationServiceProxy(final String baseUrl, final RestTemplate restTemplate) {
        super(baseUrl, restTemplate);
    }

    @Override
    public Response create(final NotificationTO notificationTO) {
        NotificationTO notification = getRestTemplate().postForObject(baseUrl + "notification/create.json",
                notificationTO, NotificationTO.class);
        URI location = URI.create(baseUrl + "notification/read/" + notification.getId() + ".json");
        return Response.created(location).header(SyncopeConstants.REST_HEADER_ID, notification.getId()).build();
    }

    @Override
    public NotificationTO delete(final Long notificationId) {
        return getRestTemplate().getForObject(baseUrl + "notification/delete/{notificationId}.json",
                NotificationTO.class, notificationId);
    }

    @Override
    public List<NotificationTO> list() {
        return Arrays
                .asList(getRestTemplate().getForObject(baseUrl + "notification/list.json", NotificationTO[].class));
    }

    @Override
    public NotificationTO read(final Long notificationId) {
        return getRestTemplate().getForObject(baseUrl + "notification/read/{notificationId}.json",
                NotificationTO.class, notificationId);
    }

    @Override
    public NotificationTO update(final Long notificationId, final NotificationTO notificationTO) {
        return getRestTemplate().postForObject(baseUrl + "notification/update.json", notificationTO,
                NotificationTO.class);
    }
}
