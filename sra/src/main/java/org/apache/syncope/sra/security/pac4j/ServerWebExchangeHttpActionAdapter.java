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
package org.apache.syncope.sra.security.pac4j;

import java.util.Optional;
import org.pac4j.core.context.HttpConstants;
import org.pac4j.core.context.WebContext;
import org.pac4j.core.exception.TechnicalException;
import org.pac4j.core.exception.http.HttpAction;
import org.pac4j.core.exception.http.WithContentAction;
import org.pac4j.core.exception.http.WithLocationAction;
import org.pac4j.core.http.adapter.HttpActionAdapter;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.server.reactive.ServerHttpResponse;

public class ServerWebExchangeHttpActionAdapter implements HttpActionAdapter {

    public static final HttpActionAdapter INSTANCE = new ServerWebExchangeHttpActionAdapter();

    @Override
    public Object adapt(final HttpAction action, final WebContext context) {
        if (action == null) {
            throw new TechnicalException("No action provided");
        }

        ServerHttpResponse response = ((ServerWebExchangeContext) context).getNative().getResponse();

        response.setStatusCode(HttpStatusCode.valueOf(action.getCode()));

        switch (action) {
            case WithLocationAction withLocationAction ->
                context.setResponseHeader(HttpConstants.LOCATION_HEADER, withLocationAction.getLocation());

            case WithContentAction withContentAction ->
                Optional.ofNullable(withContentAction.getContent()).
                        ifPresent(content -> response.bufferFactory().wrap(content.getBytes()));

            default -> {
            }
        }

        return null;
    }
}
