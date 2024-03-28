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
package org.apache.syncope.client.lib;

import java.net.URI;
import java.util.List;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.asyncclient.AsyncHTTPConduit;

public final class WebClientBuilder {

    protected static WebClient setAsync(final WebClient webClient) {
        ClientConfiguration config = WebClient.getConfig(webClient);
        config.getRequestContext().put(AsyncHTTPConduit.USE_ASYNC, Boolean.TRUE);

        return webClient;
    }

    public static WebClient build(final String address,
            final String username,
            final String password,
            final List<?> providers) {

        return setAsync(WebClient.create(address, providers, username, password, null));
    }

    public static WebClient build(final String address) {
        return setAsync(WebClient.create(address));
    }

    public static WebClient build(final URI uri) {
        return setAsync(WebClient.create(uri));
    }

    private WebClientBuilder() {
        // private constructor for static utility class
    }
}
