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

import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.to.GoogleMfaAuthTokenTO;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.wa.GoogleMfaAuthTokenService;
import org.apache.syncope.core.logic.GoogleMfaAuthTokenLogic;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.rest.cxf.service.AbstractServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.ws.rs.core.Response;

import java.net.URI;
import java.util.Date;
import java.util.List;

@Service
public class GoogleMfaAuthTokenServiceImpl extends AbstractServiceImpl implements GoogleMfaAuthTokenService {
    @Autowired
    private GoogleMfaAuthTokenLogic logic;

    @Override
    public Response deleteTokensByDate(final Date expirationDate) {
        boolean result = logic.delete(expirationDate);
        return result ? Response.ok().build() : Response.status(Response.Status.BAD_REQUEST).build();
    }

    @Override
    public Response deleteToken(final String user, final Integer otp) {
        boolean result = logic.delete(user, otp);
        return result ? Response.ok().build() : Response.status(Response.Status.BAD_REQUEST).build();
    }

    @Override
    public Response deleteTokensFor(final String user) {
        boolean result = logic.delete(user);
        return result ? Response.ok().build() : Response.status(Response.Status.BAD_REQUEST).build();
    }

    @Override
    public Response deleteToken(final Integer otp) {
        boolean result = logic.delete(otp);
        return result ? Response.ok().build() : Response.status(Response.Status.BAD_REQUEST).build();
    }

    @Override
    public Response deleteTokens() {
        logic.deleteAll();
        return Response.ok().build();
    }

    @Override
    public Response save(final GoogleMfaAuthTokenTO tokenTO) {
        final GoogleMfaAuthTokenTO token = logic.save(tokenTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(token.getKey()).build();
        return Response.created(location).
            header(RESTHeaders.RESOURCE_KEY, token.getKey()).
            build();
    }

    @Override
    public GoogleMfaAuthTokenTO findTokenFor(final String key) {
        return logic.read(key);
    }

    @Override
    public List<GoogleMfaAuthTokenTO> findTokensFor(final String user) {
        return logic.getTokensForUser(user);
    }

    @Override
    public GoogleMfaAuthTokenTO findTokenFor(final String user, final Integer otp) {
        try {
            return logic.read(user, otp);
        } catch (final NotFoundException ex) {
            throw SyncopeClientException.build(ClientExceptionType.NotFound);
        }
    }

    @Override
    public long countTokensForUser(final String user) {
        return logic.countTokensForUser(user);
    }

    @Override
    public long countTokens() {
        return logic.countAll();
    }
}
