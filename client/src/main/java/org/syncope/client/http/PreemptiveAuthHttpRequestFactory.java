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
package org.syncope.client.http;

import java.net.URI;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.AuthCache;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

/**
 * Factory for DefaultContextHttpClient, with preemptive BASIC authentication.
 *
 * @see DefaultContextHttpClient
 */
public class PreemptiveAuthHttpRequestFactory
        extends HttpComponentsClientHttpRequestFactory {

    private final HttpHost targetHost;

    public PreemptiveAuthHttpRequestFactory(
            final String host, final int port, final String scheme) {

        super();
        targetHost = new HttpHost(host, port, scheme);
    }

    public PreemptiveAuthHttpRequestFactory(
            final String host, final int port, final String scheme,
            final ClientConnectionManager conman, final HttpParams params) {

        super();
        targetHost = new HttpHost(host, port, scheme);
    }

    public AuthScope getAuthScope() {
        return new AuthScope(targetHost.getHostName(), targetHost.getPort());
    }

    @Override
    protected HttpContext createHttpContext(final HttpMethod httpMethod,
            final URI uri) {

        AuthCache authCache = new BasicAuthCache();
        // Generate BASIC scheme object and add it to the local auth cache
        BasicScheme basicAuth = new BasicScheme();
        authCache.put(targetHost, basicAuth);

        // Add AuthCache to the execution context
        BasicHttpContext localcontext = new BasicHttpContext();
        localcontext.setAttribute(ClientContext.AUTH_CACHE, authCache);

        return localcontext;
    }
}
