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

import java.util.Date;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.time.DateFormatUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.to.AccessTokenTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.beans.AccessTokenQuery;
import org.springframework.stereotype.Service;
import org.apache.syncope.common.rest.api.service.AccessTokenService;
import org.apache.syncope.core.logic.AccessTokenLogic;
import org.springframework.beans.factory.annotation.Autowired;

@Service
public class AccessTokenServiceImpl extends AbstractServiceImpl implements AccessTokenService {

    @Autowired
    private AccessTokenLogic logic;

    @Override
    public Response login() {
        Pair<String, Date> login = logic.login();
        return Response.noContent().
                header(RESTHeaders.TOKEN, login.getLeft()).
                header(RESTHeaders.TOKEN_EXPIRE,
                        DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.format(login.getRight())).
                build();
    }

    @Override
    public Response refresh() {
        Pair<String, Date> refresh = logic.refresh();
        return Response.noContent().
                header(RESTHeaders.TOKEN, refresh.getLeft()).
                header(RESTHeaders.TOKEN_EXPIRE,
                        DateFormatUtils.ISO_8601_EXTENDED_DATETIME_TIME_ZONE_FORMAT.format(refresh.getRight())).
                build();
    }

    @Override
    public void logout() {
        logic.logout();
    }

    @Override
    public PagedResult<AccessTokenTO> list(final AccessTokenQuery query) {
        return buildPagedResult(
                logic.list(
                        query.getPage(),
                        query.getSize(),
                        getOrderByClauses(query.getOrderBy())),
                query.getPage(),
                query.getSize(),
                logic.count());
    }

    @Override
    public void delete(final String key) {
        logic.delete(key);
    }

}
