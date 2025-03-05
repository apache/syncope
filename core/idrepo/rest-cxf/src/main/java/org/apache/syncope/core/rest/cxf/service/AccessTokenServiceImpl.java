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
package org.apache.syncope.core.rest.cxf.service;

import jakarta.ws.rs.core.Response;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.to.AccessTokenTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.AccessTokenQuery;
import org.apache.syncope.common.rest.api.service.AccessTokenService;
import org.apache.syncope.core.logic.AccessTokenLogic;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;

public class AccessTokenServiceImpl extends AbstractService implements AccessTokenService {

    protected final AccessTokenLogic logic;

    public AccessTokenServiceImpl(final AccessTokenLogic logic) {
        this.logic = logic;
    }

    @Override
    public Response login() {
        Pair<String, OffsetDateTime> login = logic.login();
        return Response.noContent().
                header(RESTHeaders.TOKEN, login.getLeft()).
                header(RESTHeaders.TOKEN_EXPIRE, DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(login.getRight())).
                build();
    }

    @Override
    public Response refresh() {
        Pair<String, OffsetDateTime> refresh = logic.refresh();
        return Response.noContent().
                header(RESTHeaders.TOKEN, refresh.getLeft()).
                header(RESTHeaders.TOKEN_EXPIRE, DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(refresh.getRight())).
                build();
    }

    @Override
    public void logout() {
        logic.logout();
    }

    @Override
    public PagedResult<AccessTokenTO> list(final AccessTokenQuery query) {
        Page<AccessTokenTO> result = logic.list(pageable(query, Sort.by("expirationTime").descending()));
        return buildPagedResult(result);
    }

    @Override
    public void delete(final String key) {
        logic.delete(key);
    }
}
