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

import java.util.concurrent.atomic.AtomicReference;
import javax.ws.rs.core.Response;
import org.apache.syncope.common.lib.to.OIDCJWKSTO;
import org.apache.syncope.common.lib.types.JWSAlgorithm;
import org.apache.syncope.common.rest.api.service.OIDCJWKSService;

public class OIDCJWKSRestClient extends BaseRestClient {

    private static final long serialVersionUID = -1392090291817187902L;

    public static AtomicReference<OIDCJWKSTO> get() {
        AtomicReference<OIDCJWKSTO> result = new AtomicReference<>();
        try {
            result.set(getService(OIDCJWKSService.class).get());
        } catch (Exception e) {
            LOG.debug("While getting OIDC JKS", e);
        }
        return result;
    }

    public static OIDCJWKSTO generate() {
        Response response = getService(OIDCJWKSService.class).generate(2048, JWSAlgorithm.RS256);
        return response.readEntity(OIDCJWKSTO.class);
    }

    public static void delete() {
        getService(OIDCJWKSService.class).delete();
    }
}
