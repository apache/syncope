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
package org.apache.syncope.ext.opensearch.client;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.apache.hc.core5.http.Header;
import org.apache.hc.core5.http.HttpHeaders;
import org.apache.hc.core5.http.HttpHost;
import org.apache.hc.core5.http.message.BasicHeader;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5Transport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

/**
 * Spring {@link FactoryBean} for getting the {@link OpenSearchClient} singleton instance.
 */
public class OpenSearchClientFactoryBean implements FactoryBean<OpenSearchClient>, DisposableBean {

    private final List<HttpHost> hosts;

    private String username;

    private String password;

    private String serviceToken;

    private String apiKeyId;

    private String apiKeySecret;

    private ApacheHttpClient5Transport transport;

    private OpenSearchClient client;

    public OpenSearchClientFactoryBean(final List<HttpHost> hosts) {
        this.hosts = hosts;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public String getServiceToken() {
        return serviceToken;
    }

    public void setServiceToken(final String serviceToken) {
        this.serviceToken = serviceToken;
    }

    public String getApiKeyId() {
        return apiKeyId;
    }

    public void setApiKeyId(final String apiKeyId) {
        this.apiKeyId = apiKeyId;
    }

    public String getApiKeySecret() {
        return apiKeySecret;
    }

    public void setApiKeySecret(final String apiKeySecret) {
        this.apiKeySecret = apiKeySecret;
    }

    @Override
    public OpenSearchClient getObject() {
        synchronized (this) {
            if (client == null) {
                ApacheHttpClient5TransportBuilder builder = ApacheHttpClient5TransportBuilder.
                        builder(hosts.toArray(HttpHost[]::new)).
                        setMapper(new JacksonJsonpMapper(JsonMapper.builder().
                                findAndAddModules().disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS).build()));
                if (username != null && password != null) {
                    String encodedAuth = Base64.getEncoder().
                            encodeToString((username + ":" + password).getBytes(StandardCharsets.UTF_8));
                    builder.setDefaultHeaders(
                            new Header[] { new BasicHeader(HttpHeaders.AUTHORIZATION, "Basic " + encodedAuth) });
                } else if (serviceToken != null) {
                    builder.setDefaultHeaders(
                            new Header[] { new BasicHeader(HttpHeaders.AUTHORIZATION, "Bearer " + serviceToken) });
                } else if (apiKeyId != null && apiKeySecret != null) {
                    String apiKeyAuth = Base64.getEncoder().encodeToString(
                            (apiKeyId + ":" + apiKeySecret).getBytes(StandardCharsets.UTF_8));
                    builder.setDefaultHeaders(
                            new Header[] { new BasicHeader(HttpHeaders.AUTHORIZATION, "ApiKey " + apiKeyAuth) });
                }

                transport = builder.build();
                client = new OpenSearchClient(transport);
            }
        }
        return client;
    }

    @Override
    public Class<?> getObjectType() {
        return OpenSearchClient.class;
    }

    @Override
    public void destroy() throws Exception {
        if (transport != null) {
            transport.close();
        }
    }
}
