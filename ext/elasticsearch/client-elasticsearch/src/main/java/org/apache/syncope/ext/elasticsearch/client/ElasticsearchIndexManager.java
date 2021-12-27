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
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.analysis.CustomNormalizer;
import co.elastic.clients.elasticsearch._types.analysis.Normalizer;
import co.elastic.clients.elasticsearch._types.mapping.DynamicTemplate;
import co.elastic.clients.elasticsearch._types.mapping.KeywordProperty;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.elasticsearch.indices.IndexSettingsAnalysis;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.provisioning.api.event.AnyCreatedUpdatedEvent;
import org.apache.syncope.core.provisioning.api.event.AnyDeletedEvent;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listen to any create / update and delete in order to keep the Elasticsearch indexes consistent.
 */
public class ElasticsearchIndexManager {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchIndexManager.class);

    protected final ElasticsearchClient client;

    protected final ElasticsearchUtils elasticsearchUtils;

    public ElasticsearchIndexManager(
            final ElasticsearchClient client,
            final ElasticsearchUtils elasticsearchUtils) {

        this.client = client;
        this.elasticsearchUtils = elasticsearchUtils;
    }

    public boolean existsIndex(final String domain, final AnyTypeKind kind) throws IOException {
        return client.indices().exists(
                new co.elastic.clients.elasticsearch.indices.ExistsRequest.Builder().
                        index(ElasticsearchUtils.getContextDomainName(domain, kind)).build()).
                value();
    }

    public IndexSettings defaultSettings() throws IOException {
        return new IndexSettings.Builder().
                analysis(new IndexSettingsAnalysis.Builder().
                        normalizer("string_lowercase", new Normalizer.Builder().
                                custom(new CustomNormalizer.Builder().
                                        charFilter(List.of()).
                                        filter("lowercase").
                                        build()).
                                build()).
                        build()).
                numberOfShards(elasticsearchUtils.getNumberOfShards()).
                numberOfReplicas(elasticsearchUtils.getNumberOfReplicas()).
                build();
    }

    public TypeMapping defaultMapping() throws IOException {
        return new TypeMapping.Builder().
                dynamicTemplates(List.of(Map.of(
                        "strings",
                        new DynamicTemplate.Builder().
                                matchMappingType("string").
                                mapping(new Property.Builder().
                                        keyword(new KeywordProperty.Builder().normalizer("string_lowercase").build()).
                                        build()).
                                build()))).
                build();
    }

    protected CreateIndexResponse doCreateIndex(
            final String domain,
            final AnyTypeKind kind,
            final IndexSettings settings,
            final TypeMapping mappings) throws IOException {

        return client.indices().create(
                new CreateIndexRequest.Builder().
                        index(ElasticsearchUtils.getContextDomainName(domain, kind)).
                        settings(settings).
                        mappings(mappings).
                        build());
    }

    public void createIndex(
            final String domain,
            final AnyTypeKind kind,
            final IndexSettings settings,
            final TypeMapping mappings)
            throws IOException {

        try {
            CreateIndexResponse response = doCreateIndex(domain, kind, settings, mappings);

            LOG.debug("Successfully created {} for {}: {}",
                    ElasticsearchUtils.getContextDomainName(domain, kind), kind.name(), response);
        } catch (ElasticsearchException e) {
            LOG.debug("Could not create index {} because it already exists",
                    ElasticsearchUtils.getContextDomainName(domain, kind), e);

            removeIndex(domain, kind);
            doCreateIndex(domain, kind, settings, mappings);
        }
    }

    public void removeIndex(final String domain, final AnyTypeKind kind) throws IOException {
        DeleteIndexResponse response = client.indices().delete(
                new DeleteIndexRequest.Builder().index(ElasticsearchUtils.getContextDomainName(domain, kind)).build());
        LOG.debug("Successfully removed {}: {}",
                ElasticsearchUtils.getContextDomainName(domain, kind), response);
    }

    @TransactionalEventListener
    public void after(final AnyCreatedUpdatedEvent<Any<?>> event) throws IOException {
        String index = ElasticsearchUtils.getContextDomainName(
                AuthContextUtils.getDomain(), event.getAny().getType().getKind());

        IndexRequest<Map<String, Object>> request = new IndexRequest.Builder<Map<String, Object>>().
                index(index).
                id(event.getAny().getKey()).
                document(elasticsearchUtils.document(event.getAny(), event.getDomain())).
                build();
        IndexResponse response = client.index(request);
        LOG.debug("Index successfully created or updated for {}: {}", event.getAny(), response);
    }

    @TransactionalEventListener
    public void after(final AnyDeletedEvent event) throws IOException {
        LOG.debug("About to delete index for {}[{}]", event.getAnyTypeKind(), event.getAnyKey());

        DeleteRequest request = new DeleteRequest.Builder().index(
                ElasticsearchUtils.getContextDomainName(AuthContextUtils.getDomain(), event.getAnyTypeKind())).
                id(event.getAnyKey()).
                build();
        DeleteResponse response = client.delete(request);
        LOG.debug("Index successfully deleted for {}[{}]: {}",
                event.getAnyTypeKind(), event.getAnyKey(), response);
    }
}
