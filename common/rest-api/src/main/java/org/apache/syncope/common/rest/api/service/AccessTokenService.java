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
package org.apache.syncope.common.rest.api.service;

import javax.ws.rs.BeanParam;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.AccessTokenTO;
import org.apache.syncope.common.lib.to.PagedResult;
import org.apache.syncope.common.rest.api.beans.AccessTokenQuery;

/**
 * REST operations for access tokens.
 */
@Path("accessTokens")
public interface AccessTokenService extends JAXRSService {

    /**
     * Returns an empty response bearing the X-Syncope-Token header value, in case of successful authentication.
     * The provided value is a signed JSON Web Token.
     *
     * @return empty response bearing the X-Syncope-Token header value, in case of successful authentication
     */
    @POST
    @Path("login")
    Response login();

    /**
     * Returns an empty response bearing the X-Syncope-Token header value, with extended lifetime.
     * The provided value is a signed JSON Web Token.
     *
     * @return an empty response bearing the X-Syncope-Token header value, with extended lifetime
     */
    @POST
    @Path("refresh")
    Response refresh();

    /**
     * Invalidates the access token of the requesting user.
     */
    @POST
    @Path("logout")
    void logout();

    /**
     * Returns a paged list of existing access tokens matching the given query.
     *
     * @param query query conditions
     * @return paged list of existing access tokens matching the given query
     */
    @GET
    PagedResult<AccessTokenTO> list(@BeanParam AccessTokenQuery query);

    /**
     * Invalidates the access token matching the provided key.
     *
     * @param key access token key
     */
    @DELETE
    @Path("{key}")
    void delete(@PathParam("key") String key);
}
