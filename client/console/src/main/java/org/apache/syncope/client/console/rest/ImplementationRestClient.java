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

import static org.apache.syncope.client.console.rest.BaseRestClient.getObject;
import static org.apache.syncope.client.console.rest.BaseRestClient.getService;

import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.rest.api.service.ImplementationService;

public class ImplementationRestClient extends BaseRestClient {

    private static final long serialVersionUID = -4111950555473526287L;

    public ImplementationTO read(final String key) {
        return getService(ImplementationService.class).read(key);
    }

    public ImplementationTO create(final ImplementationTO implementation) {
        ImplementationService service = getService(ImplementationService.class);
        Response response = service.create(implementation);
        return getObject(service, response.getLocation(), ImplementationTO.class);
    }
}
