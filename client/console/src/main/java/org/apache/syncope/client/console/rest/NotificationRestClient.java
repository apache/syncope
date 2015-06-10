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

import java.util.List;
import org.apache.syncope.common.lib.to.NotificationTO;
import org.apache.syncope.common.rest.api.service.NotificationService;
import org.springframework.stereotype.Component;

@Component
public class NotificationRestClient extends BaseRestClient {

    private static final long serialVersionUID = 6328933265096511690L;

    public List<NotificationTO> getAllNotifications() {
        return getService(NotificationService.class).list();
    }

    public NotificationTO read(final Long key) {
        return getService(NotificationService.class).read(key);
    }

    public void create(final NotificationTO notificationTO) {
        getService(NotificationService.class).create(notificationTO);
    }

    public void update(final NotificationTO notificationTO) {
        getService(NotificationService.class).update(notificationTO);
    }

    public void delete(final Long key) {
        getService(NotificationService.class).delete(key);
    }
}
