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

import java.net.URI;
import java.util.Date;
import java.util.List;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.lib.types.GoogleMfaAuthToken;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.wa.GoogleMfaAuthTokenService;
import org.apache.syncope.core.logic.GoogleMfaAuthTokenLogic;
import org.apache.syncope.core.rest.cxf.service.AbstractServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class GoogleMfaAuthTokenServiceImpl extends AbstractServiceImpl implements GoogleMfaAuthTokenService {

    @Autowired
    private GoogleMfaAuthTokenLogic logic;

    @Override
    public Response deleteTokensByDate(final Date expirationDate) {
        logic.delete(expirationDate);
        return Response.noContent().build();
    }

    @Override
    public Response deleteToken(final String owner, final Integer otp) {
        logic.delete(owner, otp);
        return Response.noContent().build();
    }

    @Override
    public Response deleteTokensFor(final String owner) {
        logic.delete(owner);
        return Response.noContent().build();
    }

    @Override
    public Response deleteToken(final Integer otp) {
        logic.delete(otp);
        return Response.noContent().build();
    }

    @Override
    public Response deleteTokens() {
        logic.deleteAll();
        return Response.noContent().build();
    }

    @Override
    public Response save(final GoogleMfaAuthToken tokenTO) {
        final GoogleMfaAuthToken token = logic.save(tokenTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(token.getKey()).build();
        return Response.created(location).
                header(RESTHeaders.RESOURCE_KEY, token.getKey()).
                build();
    }

    @Override
    public GoogleMfaAuthToken findTokenFor(final String owner, final Integer otp) {
        return logic.read(owner, otp);
    }

    @Override
    public PagedResult<GoogleMfaAuthToken> findTokensFor(final String owner) {
        List<GoogleMfaAuthToken> tokens = logic.findTokensFor(owner);

        PagedResult<GoogleMfaAuthToken> result = new PagedResult<>();
        result.getResult().addAll(tokens);

        result.setPage(1);
        result.setSize(result.getResult().size());
        result.setTotalCount(result.getSize());

        return result;
    }

    @Override
    public GoogleMfaAuthToken findTokenFor(final String key) {
        return logic.read(key);
    }

    @Override
    public PagedResult<GoogleMfaAuthToken> countTokens() {
        PagedResult<GoogleMfaAuthToken> result = new PagedResult<>();

        result.setSize(logic.countAll());
        result.setTotalCount(result.getSize());

        return result;
    }
}
