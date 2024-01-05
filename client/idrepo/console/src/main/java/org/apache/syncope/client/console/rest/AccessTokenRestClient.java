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
import org.apache.syncope.common.lib.to.AccessTokenTO;
import org.apache.syncope.common.rest.api.beans.AccessTokenQuery;
import org.apache.syncope.common.rest.api.service.AccessTokenService;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;

/**
 * Console client for invoking Rest Access Token's services.
 */
public class AccessTokenRestClient extends BaseRestClient {

    private static final long serialVersionUID = -3161863874876938094L;

    public void delete(final String key) {
        getService(AccessTokenService.class).delete(key);
    }

    public long count() {
        return getService(AccessTokenService.class).list(
                new AccessTokenQuery.Builder().page(1).size(0).build()).
                getTotalCount();
    }

    public List<AccessTokenTO> list(final int page, final int size, final SortParam<String> sort) {
        return getService(AccessTokenService.class).list(
                new AccessTokenQuery.Builder().page(page).size(size).orderBy(toOrderBy(sort)).build()).
                getResult();
    }
}
