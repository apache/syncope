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
import java.net.URISyntaxException;
import java.util.Objects;
import org.apache.hc.core5.http.HttpHost;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.identityconnectors.common.CollectionUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.actuate.health.HealthContributor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@EnableConfigurationProperties(ElasticsearchProperties.class)
@Configuration(proxyBeanMethods = false)
public class ElasticsearchClientContext {

    protected static final Logger LOG = LoggerFactory.getLogger(ElasticsearchClientContext.class);

    @ConditionalOnMissingBean
    @Bean
    public ElasticsearchClientFactoryBean elasticsearchClientFactoryBean(final ElasticsearchProperties props) {
        return new ElasticsearchClientFactoryBean(CollectionUtil.nullAsEmpty(props.getHosts()).stream().
                map(host -> {
                    try {
                        return HttpHost.create(host);
                    } catch (URISyntaxException e) {
                        LOG.error("Invalid host: {}", host, e);
                        return null;
                    }
                }).filter(Objects::nonNull).toList());
    }

    @ConditionalOnMissingBean
    @Bean
    public ElasticsearchUtils elasticsearchUtils(
            final @Lazy UserDAO userDAO,
            final @Lazy GroupDAO groupDAO,
            final @Lazy AnyObjectDAO anyObjectDAO) {

        return new ElasticsearchUtils(userDAO, groupDAO, anyObjectDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public ElasticsearchIndexManager elasticsearchIndexManager(
            final ElasticsearchProperties props,
            final ElasticsearchClient client,
            final ElasticsearchUtils elasticsearchUtils) {

        return new ElasticsearchIndexManager(
                client,
                elasticsearchUtils,
                props.getNumberOfShards(),
                props.getNumberOfReplicas());
    }

    @ConditionalOnMissingBean
    @Bean
    public ElasticsearchIndexLoader elasticsearchIndexLoader(final ElasticsearchIndexManager indexManager) {
        return new ElasticsearchIndexLoader(indexManager);
    }

    @ConditionalOnMissingBean(name = "syncopeElasticsearchHealthContributor")
    @Bean(name = {
        "syncopeElasticsearchHealthContributor", "elasticsearchHealthIndicator", "elasticsearchHealthContributor" })
    public HealthContributor syncopeElasticsearchHealthContributor(final ElasticsearchClient client) {
        return new SyncopeElasticsearchHealthContributor(client);
    }
}
