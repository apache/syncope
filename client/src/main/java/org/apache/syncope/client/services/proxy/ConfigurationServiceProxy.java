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
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
import org.apache.syncope.common.services.ConfigurationService;
import org.apache.syncope.common.to.ConfigurationTO;
import org.apache.syncope.common.to.MailTemplateTO;
import org.apache.syncope.common.to.ValidatorTO;
import org.apache.syncope.common.util.CollectionWrapper;
import org.springframework.web.client.RestTemplate;

public class ConfigurationServiceProxy extends SpringServiceProxy implements ConfigurationService {

    public ConfigurationServiceProxy(final String baseUrl, final RestTemplate restTemplate) {
        super(baseUrl, restTemplate);
    }

    @Override
    public Response create(final ConfigurationTO configurationTO) {
        ConfigurationTO created = getRestTemplate().postForObject(baseUrl + "configuration/create", configurationTO,
                ConfigurationTO.class);
        try {
            URI location = URI.create(baseUrl
                    + "configuration/read/"
                    + URLEncoder.encode(created.getKey(), SyncopeConstants.DEFAULT_ENCODING)
                    + ".json");
            return Response.created(location)
                    .header(SyncopeConstants.REST_HEADER_ID, created.getKey())
                    .build();
        } catch (UnsupportedEncodingException e) {
            throw new InternalServerErrorException(e);
        }
    }

    @Override
    public void delete(final String key) {
        getRestTemplate().getForObject(baseUrl + "configuration/delete/{key}.json", ConfigurationTO.class, key);
    }

    @Override
    public List<ConfigurationTO> list() {
        return Arrays.asList(getRestTemplate().getForObject(baseUrl + "configuration/list.json",
                ConfigurationTO[].class));
    }

    @Override
    public ConfigurationTO read(final String key) {
        return getRestTemplate().getForObject(baseUrl + "configuration/read/{key}.json", ConfigurationTO.class, key);
    }

    @Override
    public void update(final String key, final ConfigurationTO configurationTO) {
        getRestTemplate().postForObject(baseUrl + "configuration/update", configurationTO, ConfigurationTO.class);
    }

    @Override
    public Set<ValidatorTO> getValidators() {
        Set<String> response = new HashSet<String>(Arrays.asList(getRestTemplate().getForObject(
                baseUrl + "configuration/validators.json", String[].class)));
        return CollectionWrapper.wrapValidator(response);
    }

    @Override
    public Set<MailTemplateTO> getMailTemplates() {
        Set<String> response = new HashSet<String>(Arrays.asList(getRestTemplate().getForObject(
                baseUrl + "configuration/mailTemplates.json", String[].class)));
        return CollectionWrapper.wrapMailTemplates(response);
    }

    @Override
    public Response dbExport() {
        final AuthScope scope = ((PreemptiveAuthHttpRequestFactory) getRestTemplate().getRequestFactory()).
                getAuthScope();
        final HttpHost targetHost = new HttpHost(scope.getHost(), scope.getPort(), scope.getScheme());
        final BasicHttpContext localcontext = new BasicHttpContext();
        final AuthCache authCache = new BasicAuthCache();
        authCache.put(targetHost, new BasicScheme());
        localcontext.setAttribute(ClientContext.AUTH_CACHE, authCache);

        final HttpGet getMethod = new HttpGet(baseUrl + "configuration/dbexport");
        try {
            final HttpResponse httpResponse =
                    ((PreemptiveAuthHttpRequestFactory) getRestTemplate().getRequestFactory()).
                    getHttpClient().execute(targetHost, getMethod, localcontext);

            Response response;
            if (httpResponse.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
                HttpEntity entity = httpResponse.getEntity();
                response = Response.ok(entity.getContent(), entity.getContentType().getValue()).
                        location(getMethod.getURI()).
                        header("Content-Disposition", httpResponse.getLastHeader("Content-Disposition").getValue()).
                        build();
            } else {
                response = Response.noContent().status(httpResponse.getStatusLine().getStatusCode()).
                        location(getMethod.getURI()).
                        build();
            }

            return response;
        } catch (IOException e) {
            throw new InternalServerErrorException(e);
        }
    }
}
