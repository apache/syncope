/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.syncope.core.rest.cxf.service.wa;

import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.types.GoogleMfaAuthAccount;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.wa.GoogleMfaAuthAccountService;
import org.apache.syncope.core.logic.GoogleMfaAuthAccountLogic;
import org.apache.syncope.core.rest.cxf.service.AbstractServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;

import java.net.URI;

@Service
public class GoogleMfaAuthAccountServiceImpl extends AbstractServiceImpl implements GoogleMfaAuthAccountService {
    @Autowired
    private GoogleMfaAuthAccountLogic logic;

    @Override
    public Response deleteAccountFor(final String owner) {
        logic.deleteAccountFor(owner);
        return Response.noContent().build();
    }

    @Override
    public Response deleteAccountBy(final String key) {
        logic.deleteAccountBy(key);
        return Response.noContent().build();
    }

    @Override
    public Response deleteAll() {
        logic.deleteAll();
        return Response.noContent().build();
    }

    @Override
    public Response save(final GoogleMfaAuthAccount acct) {
        final GoogleMfaAuthAccount token = logic.save(acct);
        URI location = uriInfo.getAbsolutePathBuilder().path(token.getKey()).build();
        return Response.created(location).
            header(RESTHeaders.RESOURCE_KEY, token.getKey()).
            build();
    }

    @Override
    public void update(final GoogleMfaAuthAccount acct) {
        logic.update(acct);
    }

    @Override
    public GoogleMfaAuthAccount findAccountFor(final String owner) {
        return logic.findAccountFor(owner);
    }

    @Override
    public GoogleMfaAuthAccount findAccountBy(final String key) {
        return logic.findAccountBy(key);
    }

    @Override
    public PagedResult<GoogleMfaAuthAccount> countAll() {
        PagedResult<GoogleMfaAuthAccount> result = new PagedResult<>();
        result.setSize(Long.valueOf(logic.countAll()).intValue());
        result.setPage(1);
        result.setTotalCount(result.getSize());
        return result;
    }

    @Override
    public PagedResult<GoogleMfaAuthAccount> list() {
        PagedResult<GoogleMfaAuthAccount> result = new PagedResult<>();
        result.setSize(Long.valueOf(logic.countAll()).intValue());
        result.setPage(1);
        result.setTotalCount(result.getSize());
        result.getResult().addAll(logic.list());
        return result;
    }
}
