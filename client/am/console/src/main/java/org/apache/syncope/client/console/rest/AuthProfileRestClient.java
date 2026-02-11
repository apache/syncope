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
import org.apache.syncope.common.lib.to.AuthProfileTO;
import org.apache.syncope.common.rest.api.beans.AuthProfileQuery;
import org.apache.syncope.common.rest.api.service.AuthProfileService;

public class AuthProfileRestClient extends BaseRestClient {

    private static final long serialVersionUID = -7379778542101161274L;

    public long count(final String keyword) {
        return getService(AuthProfileService.class).
                search(new AuthProfileQuery.Builder().page(1).size(0).keyword(keyword).build()).
                getTotalCount();
    }

    public List<AuthProfileTO> search(final int page, final int size, final String keyword) {
        return getService(AuthProfileService.class).
                search(new AuthProfileQuery.Builder().page(page).size(size).keyword(keyword).build()).
                getResult();
    }

    public AuthProfileTO read(final String key) {
        return getService(AuthProfileService.class).read(key);
    }

    public void update(final AuthProfileTO authProfile) {
        getService(AuthProfileService.class).update(authProfile);
    }

    public void delete(final String key) {
        getService(AuthProfileService.class).delete(key);
    }
}
