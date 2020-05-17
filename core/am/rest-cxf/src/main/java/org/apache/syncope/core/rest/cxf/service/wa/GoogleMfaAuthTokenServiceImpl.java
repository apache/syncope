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

import org.apache.syncope.common.lib.to.GoogleMfaAuthTokenTO;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.wa.GoogleMfaAuthTokenService;
import org.apache.syncope.core.logic.GoogleMfaAuthTokenLogic;
import org.apache.syncope.core.rest.cxf.service.AbstractServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.validation.constraints.NotNull;
import javax.ws.rs.core.Response;

import java.net.URI;
import java.time.LocalDateTime;

@Service
public class GoogleMfaAuthTokenServiceImpl extends AbstractServiceImpl implements GoogleMfaAuthTokenService {
    @Autowired
    private GoogleMfaAuthTokenLogic logic;

    @Override
    public Response delete(@NotNull final LocalDateTime expirationDate) {
        boolean result = logic.delete(expirationDate);
        return result ? Response.ok().build() : Response.status(Response.Status.BAD_REQUEST).build();
    }

    @Override
    public Response delete(@NotNull final String user, @NotNull final Integer otp) {
        boolean result = logic.delete(user, otp);
        return result ? Response.ok().build() : Response.status(Response.Status.BAD_REQUEST).build();
    }

    @Override
    public Response delete(@NotNull final String user) {
        boolean result = logic.delete(user);
        return result ? Response.ok().build() : Response.status(Response.Status.BAD_REQUEST).build();
    }

    @Override
    public Response delete(@NotNull final Integer otp) {
        boolean result = logic.delete(otp);
        return result ? Response.ok().build() : Response.status(Response.Status.BAD_REQUEST).build();
    }

    @Override
    public Response deleteAll() {
        boolean result = logic.deleteAll();
        return result ? Response.ok().build() : Response.status(Response.Status.BAD_REQUEST).build();
    }

    @Override
    public Response save(@NotNull final GoogleMfaAuthTokenTO tokenTO) {
        GoogleMfaAuthTokenTO token = logic.save(tokenTO);
        URI location = uriInfo.getAbsolutePathBuilder().path(token.getKey()).build();
        return Response.created(location).
            header(RESTHeaders.RESOURCE_KEY, token.getKey()).
            build();
    }

    @Override
    public GoogleMfaAuthTokenTO read(@NotNull final String user, @NotNull final Integer otp) {
        return logic.read(user, otp);
    }

    @Override
    public long count(@NotNull final String user) {
        return logic.count(user);
    }

    @Override
    public long count() {
        return logic.count();
    }
}
