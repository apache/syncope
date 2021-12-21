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
package org.apache.syncope.ext.elasticsearch.client;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.message.BasicHeader;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestClientBuilder;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;

/**
 * Spring {@link FactoryBean} for getting the {@link ElasticsearchClient} singleton instance.
 */
public class ElasticsearchClientFactoryBean implements FactoryBean<ElasticsearchClient>, DisposableBean {

    private final List<HttpHost> hosts;

    private String username;

    private String password;

    private String serviceToken;

    private String apiKeyId;

    private String apiKeySecret;

    private RestClient restClient;

    private ElasticsearchClient client;

    public ElasticsearchClientFactoryBean(final List<HttpHost> hosts) {
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
    public ElasticsearchClient getObject() throws Exception {
        synchronized (this) {
            if (client == null) {
                RestClientBuilder builder = RestClient.builder(hosts.toArray(HttpHost[]::new));
                if (username != null && password != null) {
                    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
                    credentialsProvider.setCredentials(
                            AuthScope.ANY, new UsernamePasswordCredentials(username, password));
                    builder.setHttpClientConfigCallback(b -> b.setDefaultCredentialsProvider(credentialsProvider));
                } else if (serviceToken != null) {
                    builder.setDefaultHeaders(
                            new Header[] { new BasicHeader(HttpHeaders.AUTHORIZATION, "Bearer " + serviceToken) });
                } else if (apiKeyId != null && apiKeySecret != null) {
                    String apiKeyAuth = Base64.getEncoder().encodeToString(
                            (apiKeyId + ":" + apiKeySecret).getBytes(StandardCharsets.UTF_8));
                    builder.setDefaultHeaders(
                            new Header[] { new BasicHeader(HttpHeaders.AUTHORIZATION, "ApiKey " + apiKeyAuth) });
                }

                restClient = builder.build();
                client = new ElasticsearchClient(new RestClientTransport(restClient, new JacksonJsonpMapper()));
            }
        }
        return client;
    }

    @Override
    public Class<?> getObjectType() {
        return ElasticsearchClient.class;
    }

    @Override
    public void destroy() throws Exception {
        if (restClient != null) {
            restClient.close();
        }
    }
}
