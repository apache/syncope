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
package org.apache.syncope.client.services.proxy;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Response;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.client.AuthCache;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.syncope.client.http.PreemptiveAuthHttpRequestFactory;
import org.apache.syncope.common.SyncopeConstants;
import org.springframework.web.client.RestTemplate;

public abstract class SpringServiceProxy {

    protected String baseUrl;

    private RestTemplate restTemplate;

    public SpringServiceProxy(final String baseUrl, final RestTemplate restTemplate) {
        this.baseUrl = baseUrl;
        this.restTemplate = restTemplate;
    }

    public void setRestTemplate(final RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public RestTemplate getRestTemplate() {
        return restTemplate;
    }

    protected Response handleStream(final String url) {
        final AuthScope scope = ((PreemptiveAuthHttpRequestFactory) getRestTemplate().getRequestFactory()).
                getAuthScope();
        final HttpHost targetHost = new HttpHost(scope.getHost(), scope.getPort(), scope.getScheme());
        final BasicHttpContext localcontext = new BasicHttpContext();
        final AuthCache authCache = new BasicAuthCache();
        authCache.put(targetHost, new BasicScheme());
        localcontext.setAttribute(ClientContext.AUTH_CACHE, authCache);

        final HttpGet getMethod = new HttpGet(url);
        try {
            final HttpResponse httpResponse =
                    ((PreemptiveAuthHttpRequestFactory) getRestTemplate().getRequestFactory()).
                    getHttpClient().execute(targetHost, getMethod, localcontext);

            Response response;
            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = httpResponse.getEntity();
                response = Response.ok(entity.getContent(), entity.getContentType().getValue()).
                        header(SyncopeConstants.CONTENT_DISPOSITION_HEADER,
                        httpResponse.getLastHeader(SyncopeConstants.CONTENT_DISPOSITION_HEADER).getValue()).
                        build();
            } else {
                response = Response.noContent().status(httpResponse.getStatusLine().getStatusCode()).
                        build();
            }

            return response;
        } catch (IOException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @SuppressWarnings("unchecked")
    protected Collection<String> handlePossiblyEmptyStringCollection(final String url) {
        Collection<String> result = Collections.<String>emptySet();

        final Object object = getRestTemplate().getForObject(url, Object.class);
        if (object instanceof String) {
            String string = (String) object;
            result = Collections.singleton(string);
        } else if (object instanceof Collection) {
            result = (Collection<String>) object;
        }

        return result;
    }
}
