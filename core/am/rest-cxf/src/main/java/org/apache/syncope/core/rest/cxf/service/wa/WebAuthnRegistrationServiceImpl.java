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

package org.apache.syncope.core.rest.cxf.service.wa;

import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.types.WebAuthnRegisteredAccount;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.wa.WebAuthnRegistrationService;
import org.apache.syncope.core.logic.WebAuthnRegistrationServiceLogic;
import org.apache.syncope.core.rest.cxf.service.AbstractServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;

import java.net.URI;
import java.util.List;

@Service
public class WebAuthnRegistrationServiceImpl extends AbstractServiceImpl implements WebAuthnRegistrationService {
    @Autowired
    private WebAuthnRegistrationServiceLogic logic;

    @Override
    public PagedResult<WebAuthnRegisteredAccount> list() {
        List<WebAuthnRegisteredAccount> accounts = logic.list();
        PagedResult<WebAuthnRegisteredAccount> result = new PagedResult<>();
        result.getResult().addAll(accounts);
        result.setPage(1);
        result.setSize(result.getResult().size());
        result.setTotalCount(result.getSize());
        return result;
    }

    @Override
    public WebAuthnRegisteredAccount read(final String key) {
        return logic.read(key);
    }

    @Override
    public WebAuthnRegisteredAccount findAccountFor(final String owner) {
        return logic.findAccountBy(owner);
    }

    @Override
    public Response delete(final String owner) {
        logic.delete(owner);
        return Response.noContent().build();
    }

    @Override
    public Response delete(final String owner, final String credentialId) {
        logic.delete(owner, credentialId);
        return Response.noContent().build();
    }

    @Override
    public Response deleteAll() {
        logic.deleteAll();
        return Response.noContent().build();
    }

    @Override
    public Response create(final WebAuthnRegisteredAccount account) {
        final WebAuthnRegisteredAccount token = logic.create(account);
        URI location = uriInfo.getAbsolutePathBuilder().path(token.getKey()).build();
        return Response.created(location).
            header(RESTHeaders.RESOURCE_KEY, token.getKey()).
            entity(token).
            build();
    }

    @Override
    public void update(final WebAuthnRegisteredAccount account) {
        logic.update(account);
    }
}
