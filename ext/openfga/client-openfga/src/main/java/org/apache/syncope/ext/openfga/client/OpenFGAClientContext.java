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
package org.apache.syncope.ext.openfga.client;

import jakarta.ws.rs.core.HttpHeaders;
import java.net.http.HttpClient;
import java.util.Optional;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@EnableConfigurationProperties(OpenFGAProperties.class)
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "openfga", name = "api-url")
public class OpenFGAClientContext {

    @ConditionalOnMissingBean
    @Bean
    public ApiClient openFgaApiClient(final OpenFGAProperties props) {
        HttpClient.Builder httpClientBuilder = ApiClient.createDefaultHttpClientBuilder();

        ApiClient apiClient = new ApiClient(
                httpClientBuilder,
                ApiClient.createDefaultObjectMapper(),
                props.getApiUrl());
        apiClient.setConnectTimeout(props.getConnectTimeout());
        apiClient.setReadTimeout(props.getReadTimeout());

        Optional.ofNullable(props.getApiToken()).
                ifPresent(apiToken -> apiClient.setRequestInterceptor(builder -> builder.
                header(HttpHeaders.AUTHORIZATION, "Bearer " + apiToken)));

        return apiClient;
    }

    @ConditionalOnMissingBean
    @Bean
    public OpenFGAClientFactory openFgaClientFactory(final ApiClient openFgaApiClient, final OpenFGAProperties props) {
        return new OpenFGAClientFactory(openFgaApiClient, props);
    }

    @ConditionalOnMissingBean
    @Bean
    public OpenFGAStoreManager openFgaStoreManager(
            final OpenFGAClientFactory clientFactory,
            final RelationshipTypeDAO relationshipTypeDAO) {

        return new OpenFGAStoreManager(clientFactory, relationshipTypeDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public OpenFGAHealthIndicator openFgaHealthIndicator(
            final DomainHolder<?> domainHolder,
            final OpenFGAClientFactory clientFactory) {

        return new OpenFGAHealthIndicator(domainHolder, clientFactory);
    }
}
