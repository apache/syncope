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
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.rest.api.service.wa.WAConfigService;

public class WAConfigRestClient extends BaseRestClient {

    private static final long serialVersionUID = -7379778542101161274L;

    public List<Attr> list() {
        return getService(WAConfigService.class).list();
    }

    public void set(final Attr value) {
        getService(WAConfigService.class).set(value);
    }

    public void delete(final String schema) {
        getService(WAConfigService.class).delete(schema);
    }

    public void push(final WAConfigService.PushSubject subject, final List<String> services) {
        getService(WAConfigService.class).pushToWA(subject, services);
    }
}
