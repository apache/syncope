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

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.provisioning.api.event.AnyLifecycleEvent;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.OpenSearchException;
import org.opensearch.client.opensearch._types.analysis.CustomNormalizer;
import org.opensearch.client.opensearch._types.analysis.Normalizer;
import org.opensearch.client.opensearch._types.mapping.DynamicTemplate;
import org.opensearch.client.opensearch._types.mapping.KeywordProperty;
import org.opensearch.client.opensearch._types.mapping.ObjectProperty;
import org.opensearch.client.opensearch._types.mapping.Property;
import org.opensearch.client.opensearch._types.mapping.TextProperty;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.core.DeleteRequest;
import org.opensearch.client.opensearch.core.DeleteResponse;
import org.opensearch.client.opensearch.core.IndexRequest;
import org.opensearch.client.opensearch.core.IndexResponse;
import org.opensearch.client.opensearch.indices.CreateIndexRequest;
import org.opensearch.client.opensearch.indices.CreateIndexResponse;
import org.opensearch.client.opensearch.indices.DeleteIndexRequest;
import org.opensearch.client.opensearch.indices.DeleteIndexResponse;
import org.opensearch.client.opensearch.indices.ExistsRequest;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.opensearch.client.opensearch.indices.IndexSettingsAnalysis;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listen to any create / update and delete in order to keep the OpenSearch indexes consistent.
 */
public class OpenSearchIndexManager {

    private static final Logger LOG = LoggerFactory.getLogger(OpenSearchIndexManager.class);

    protected final OpenSearchClient client;

    protected final OpenSearchUtils ppenSearchUtils;

    protected final String numberOfShards;

    protected final String numberOfReplicas;

    public OpenSearchIndexManager(
            final OpenSearchClient client,
            final OpenSearchUtils ppenSearchUtils,
            final String numberOfShards,
            final String numberOfReplicas) {

        this.client = client;
        this.ppenSearchUtils = ppenSearchUtils;
        this.numberOfShards = numberOfShards;
        this.numberOfReplicas = numberOfReplicas;
    }

    public boolean existsAnyIndex(final String domain, final AnyTypeKind kind) throws IOException {
        return client.indices().exists(new ExistsRequest.Builder().
                index(OpenSearchUtils.getAnyIndex(domain, kind)).build()).
                value();
    }

    public boolean existsAuditIndex(final String domain) throws IOException {
        return client.indices().exists(new ExistsRequest.Builder().
                index(OpenSearchUtils.getAuditIndex(domain)).build()).
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
                numberOfShards(numberOfShards).
                numberOfReplicas(numberOfReplicas).
                build();
    }

    public TypeMapping defaultAnyMapping() throws IOException {
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

    public TypeMapping defaultAuditMapping() throws IOException {
        return new TypeMapping.Builder().
                dynamicTemplates(List.of(Map.of(
                        "strings",
                        new DynamicTemplate.Builder().
                                matchMappingType("string").
                                mapping(new Property.Builder().
                                        keyword(new KeywordProperty.Builder().normalizer("string_lowercase").build()).
                                        build()).
                                build()))).
                properties(
                        "message",
                        new Property.Builder().object(new ObjectProperty.Builder().
                                properties(
                                        "before",
                                        new Property.Builder().
                                                text(new TextProperty.Builder().analyzer("standard").build()).
                                                build()).
                                properties(
                                        "inputs",
                                        new Property.Builder().
                                                text(new TextProperty.Builder().analyzer("standard").build()).
                                                build()).
                                properties(
                                        "output",
                                        new Property.Builder().
                                                text(new TextProperty.Builder().analyzer("standard").build()).
                                                build()).
                                properties(
                                        "throwable",
                                        new Property.Builder().
                                                text(new TextProperty.Builder().analyzer("standard").build()).
                                                build()).
                                build()).
                                build()).
                build();
    }

    protected CreateIndexResponse doCreateAnyIndex(
            final String domain,
            final AnyTypeKind kind,
            final IndexSettings settings,
            final TypeMapping mappings) throws IOException {

        return client.indices().create(
                new CreateIndexRequest.Builder().
                        index(OpenSearchUtils.getAnyIndex(domain, kind)).
                        settings(settings).
                        mappings(mappings).
                        build());
    }

    public void createAnyIndex(
            final String domain,
            final AnyTypeKind kind,
            final IndexSettings settings,
            final TypeMapping mappings)
            throws IOException {

        try {
            CreateIndexResponse response = doCreateAnyIndex(domain, kind, settings, mappings);

            LOG.debug("Successfully created {} for {}: {}",
                    OpenSearchUtils.getAnyIndex(domain, kind), kind.name(), response);
        } catch (OpenSearchException e) {
            LOG.debug("Could not create index {} because it already exists",
                    OpenSearchUtils.getAnyIndex(domain, kind), e);

            removeAnyIndex(domain, kind);
            doCreateAnyIndex(domain, kind, settings, mappings);
        }
    }

    public void removeAnyIndex(final String domain, final AnyTypeKind kind) throws IOException {
        DeleteIndexResponse response = client.indices().delete(
                new DeleteIndexRequest.Builder().index(OpenSearchUtils.getAnyIndex(domain, kind)).build());
        LOG.debug("Successfully removed {}: {}", OpenSearchUtils.getAnyIndex(domain, kind), response);
    }

    protected CreateIndexResponse doCreateAuditIndex(
            final String domain,
            final IndexSettings settings,
            final TypeMapping mappings) throws IOException {

        return client.indices().create(
                new CreateIndexRequest.Builder().
                        index(OpenSearchUtils.getAuditIndex(domain)).
                        settings(settings).
                        mappings(mappings).
                        build());
    }

    public void createAuditIndex(
            final String domain,
            final IndexSettings settings,
            final TypeMapping mappings)
            throws IOException {

        try {
            CreateIndexResponse response = doCreateAuditIndex(domain, settings, mappings);

            LOG.debug("Successfully created audit index {}: {}",
                    OpenSearchUtils.getAuditIndex(domain), response);
        } catch (OpenSearchException e) {
            LOG.debug("Could not create audit index {} because it already exists",
                    OpenSearchUtils.getAuditIndex(domain), e);

            removeAuditIndex(domain);
            doCreateAuditIndex(domain, settings, mappings);
        }
    }

    public void removeAuditIndex(final String domain) throws IOException {
        DeleteIndexResponse response = client.indices().delete(
                new DeleteIndexRequest.Builder().index(OpenSearchUtils.getAuditIndex(domain)).build());
        LOG.debug("Successfully removed {}: {}", OpenSearchUtils.getAuditIndex(domain), response);
    }

    @TransactionalEventListener
    public void any(final AnyLifecycleEvent<Any<?>> event) throws IOException {
        LOG.debug("About to {} index for {}", event.getType().name(), event.getAny());

        if (event.getType() == SyncDeltaType.DELETE) {
            DeleteRequest request = new DeleteRequest.Builder().index(
                    OpenSearchUtils.getAnyIndex(event.getDomain(), event.getAny().getType().getKind())).
                    id(event.getAny().getKey()).
                    build();
            DeleteResponse response = client.delete(request);
            LOG.debug("Index successfully deleted for {}[{}]: {}",
                    event.getAny().getType().getKind(), event.getAny().getKey(), response);
        } else {
            IndexRequest<Map<String, Object>> request = new IndexRequest.Builder<Map<String, Object>>().
                    index(OpenSearchUtils.getAnyIndex(event.getDomain(), event.getAny().getType().getKind())).
                    id(event.getAny().getKey()).
                    document(ppenSearchUtils.document(event.getAny())).
                    build();
            IndexResponse response = client.index(request);
            LOG.debug("Index successfully created or updated for {}: {}", event.getAny(), response);
        }
    }

    public void audit(final String domain, final long instant, final JsonNode message)
            throws IOException {

        LOG.debug("About to audit");

        IndexRequest<Map<String, Object>> request = new IndexRequest.Builder<Map<String, Object>>().
                index(OpenSearchUtils.getAuditIndex(domain)).
                id(SecureRandomUtils.generateRandomUUID().toString()).
                document(ppenSearchUtils.document(instant, message, domain)).
                build();
        IndexResponse response = client.index(request);

        LOG.debug("Audit successfully created: {}", response);
    }
}
