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

import org.apache.http.HttpHost;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.identityconnectors.common.CollectionUtil;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@EnableConfigurationProperties(OpenSearchProperties.class)
@Configuration(proxyBeanMethods = false)
public class OpenSearchClientContext {

    @ConditionalOnMissingBean
    @Bean
    public OpenSearchClientFactoryBean openSearchClientFactoryBean(final OpenSearchProperties props) {
        return new OpenSearchClientFactoryBean(
                CollectionUtil.nullAsEmpty(props.getHosts()).stream().
                        map(HttpHost::create).toList());
    }

    @ConditionalOnMissingBean
    @Bean
    public OpenSearchUtils openSearchUtils(
            final @Lazy UserDAO userDAO,
            final @Lazy GroupDAO groupDAO,
            final @Lazy AnyObjectDAO anyObjectDAO) {

        return new OpenSearchUtils(userDAO, groupDAO, anyObjectDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public OpenSearchIndexManager openSearchIndexManager(
            final OpenSearchProperties props,
            final OpenSearchClient client,
            final OpenSearchUtils openSearchUtils) {

        return new OpenSearchIndexManager(
                client,
                openSearchUtils,
                props.getNumberOfShards(),
                props.getNumberOfReplicas());
    }

    @ConditionalOnMissingBean
    @Bean
    public OpenSearchIndexLoader openSearchIndexLoader(final OpenSearchIndexManager indexManager) {
        return new OpenSearchIndexLoader(indexManager);
    }

    @ConditionalOnMissingBean(name = "syncopeOpenSearchHealthContributor")
    @Bean(name = {
        "syncopeOpenSearchHealthContributor", "openSearchHealthIndicator", "openSearchHealthContributor" })
    public HealthContributor syncopeOpenSearchHealthContributor(final OpenSearchClient client) {
        return new SyncopeOpenSearchHealthContributor(client);
    }
}
