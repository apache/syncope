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
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.rest.api.service.ImplementationService;

public class ImplementationRestClient extends BaseRestClient {

    private static final long serialVersionUID = -4111950555473526287L;

    public List<ImplementationTO> list(final String type) {
        return getService(ImplementationService.class).list(type);
    }

    public ImplementationTO read(final String type, final String key) {
        return getService(ImplementationService.class).read(type, key);
    }

    public void create(final ImplementationTO implementation) {
        getService(ImplementationService.class).create(implementation);
    }

    public void update(final ImplementationTO implementation) {
        getService(ImplementationService.class).update(implementation);
    }

    public void delete(final String type, final String key) {
        getService(ImplementationService.class).delete(type, key);
    }
}
