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
import co.elastic.clients.elasticsearch._types.Refresh;
import co.elastic.clients.elasticsearch._types.analysis.CustomNormalizer;
import co.elastic.clients.elasticsearch._types.analysis.Normalizer;
import co.elastic.clients.elasticsearch._types.mapping.DynamicTemplate;
import co.elastic.clients.elasticsearch._types.mapping.KeywordProperty;
import co.elastic.clients.elasticsearch._types.mapping.Property;
import co.elastic.clients.elasticsearch._types.mapping.TextProperty;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.DeleteRequest;
import co.elastic.clients.elasticsearch.core.DeleteResponse;
import co.elastic.clients.elasticsearch.core.IndexRequest;
import co.elastic.clients.elasticsearch.core.IndexResponse;
import co.elastic.clients.elasticsearch.indices.CreateIndexRequest;
import co.elastic.clients.elasticsearch.indices.CreateIndexResponse;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.elasticsearch.indices.IndexSettingsAnalysis;
import co.elastic.clients.util.NamedValue;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AuditEvent;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.provisioning.api.event.EntityLifecycleEvent;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
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

    protected final String numberOfShards;

    protected final String numberOfReplicas;

    public ElasticsearchIndexManager(
            final ElasticsearchClient client,
            final ElasticsearchUtils elasticsearchUtils,
            final String numberOfShards,
            final String numberOfReplicas) {

        this.client = client;
        this.elasticsearchUtils = elasticsearchUtils;
        this.numberOfShards = numberOfShards;
        this.numberOfReplicas = numberOfReplicas;
    }

    public boolean existsAnyIndex(final String domain, final AnyTypeKind kind) throws IOException {
        return client.indices().exists(new ExistsRequest.Builder().
                index(ElasticsearchUtils.getAnyIndex(domain, kind)).build()).
                value();
    }

    public boolean existsRealmIndex(final String domain) throws IOException {
        return client.indices().exists(new ExistsRequest.Builder().
                index(ElasticsearchUtils.getRealmIndex(domain)).build()).
                value();
    }

    public boolean existsAuditIndex(final String domain) throws IOException {
        return client.indices().exists(new ExistsRequest.Builder().
                index(ElasticsearchUtils.getAuditIndex(domain)).build()).
                value();
    }

    public IndexSettings defaultSettings() {
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

    public TypeMapping defaultAnyMapping() {
        return new TypeMapping.Builder().
                dynamicTemplates(List.of(new NamedValue<>(
                        "strings",
                        new DynamicTemplate.Builder().
                                matchMappingType("string").
                                mapping(new Property.Builder().
                                        keyword(new KeywordProperty.Builder().normalizer("string_lowercase").build()).
                                        build()).
                                build()))).
                build();
    }

    public TypeMapping defaultRealmMapping() {
        return new TypeMapping.Builder().
                dynamicTemplates(List.of(new NamedValue<>(
                        "strings",
                        new DynamicTemplate.Builder().
                                matchMappingType("string").
                                mapping(new Property.Builder().
                                        keyword(new KeywordProperty.Builder().normalizer("string_lowercase").build()).
                                        build()).
                                build()))).
                build();
    }

    public TypeMapping defaultAuditMapping() {
        return new TypeMapping.Builder().
                dynamicTemplates(List.of(new NamedValue<>(
                        "strings",
                        new DynamicTemplate.Builder().
                                matchMappingType("string").
                                mapping(new Property.Builder().
                                        keyword(new KeywordProperty.Builder().normalizer("string_lowercase").build()).
                                        build()).
                                build()))).
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
                build();
    }

    protected CreateIndexResponse doCreateAnyIndex(
            final String domain,
            final AnyTypeKind kind,
            final IndexSettings settings,
            final TypeMapping mappings) throws IOException {

        return client.indices().create(
                new CreateIndexRequest.Builder().
                        index(ElasticsearchUtils.getAnyIndex(domain, kind)).
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
                    ElasticsearchUtils.getAnyIndex(domain, kind), kind.name(), response);
        } catch (ElasticsearchException e) {
            LOG.debug("Could not create index {} because it already exists",
                    ElasticsearchUtils.getAnyIndex(domain, kind), e);

            removeAnyIndex(domain, kind);
            doCreateAnyIndex(domain, kind, settings, mappings);
        }
    }

    public void removeAnyIndex(final String domain, final AnyTypeKind kind) throws IOException {
        DeleteIndexResponse response = client.indices().delete(
                new DeleteIndexRequest.Builder().index(ElasticsearchUtils.getAnyIndex(domain, kind)).build());
        LOG.debug("Successfully removed {}: {}", ElasticsearchUtils.getAnyIndex(domain, kind), response);
    }

    protected CreateIndexResponse doCreateRealmIndex(
            final String domain,
            final IndexSettings settings,
            final TypeMapping mappings) throws IOException {

        return client.indices().create(
                new CreateIndexRequest.Builder().
                        index(ElasticsearchUtils.getRealmIndex(domain)).
                        settings(settings).
                        mappings(mappings).
                        build());
    }

    public void createRealmIndex(
            final String domain,
            final IndexSettings settings,
            final TypeMapping mappings)
            throws IOException {

        try {
            CreateIndexResponse response = doCreateRealmIndex(domain, settings, mappings);

            LOG.debug("Successfully created realm index {}: {}",
                    ElasticsearchUtils.getRealmIndex(domain), response);
        } catch (ElasticsearchException e) {
            LOG.debug("Could not create realm index {} because it already exists",
                    ElasticsearchUtils.getRealmIndex(domain), e);

            removeRealmIndex(domain);
            doCreateRealmIndex(domain, settings, mappings);
        }
    }

    public void removeRealmIndex(final String domain) throws IOException {
        DeleteIndexResponse response = client.indices().delete(
                new DeleteIndexRequest.Builder().index(ElasticsearchUtils.getRealmIndex(domain)).build());
        LOG.debug("Successfully removed {}: {}", ElasticsearchUtils.getRealmIndex(domain), response);
    }

    protected CreateIndexResponse doCreateAuditIndex(
            final String domain,
            final IndexSettings settings,
            final TypeMapping mappings) throws IOException {

        return client.indices().create(
                new CreateIndexRequest.Builder().
                        index(ElasticsearchUtils.getAuditIndex(domain)).
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
                    ElasticsearchUtils.getAuditIndex(domain), response);
        } catch (ElasticsearchException e) {
            LOG.debug("Could not create audit index {} because it already exists",
                    ElasticsearchUtils.getAuditIndex(domain), e);

            removeAuditIndex(domain);
            doCreateAuditIndex(domain, settings, mappings);
        }
    }

    public void removeAuditIndex(final String domain) throws IOException {
        DeleteIndexResponse response = client.indices().delete(
                new DeleteIndexRequest.Builder().index(ElasticsearchUtils.getAuditIndex(domain)).build());
        LOG.debug("Successfully removed {}: {}", ElasticsearchUtils.getAuditIndex(domain), response);
    }

    @TransactionalEventListener
    public void entity(final EntityLifecycleEvent<Entity> event) throws IOException {
        LOG.debug("About to {} index for {}", event.getType().name(), event.getEntity());

        if (event.getEntity() instanceof final Any any) {
            if (event.getType() == SyncDeltaType.DELETE) {
                DeleteRequest request = new DeleteRequest.Builder().index(
                        ElasticsearchUtils.getAnyIndex(event.getDomain(), any.getType().getKind())).
                        id(any.getKey()).
                        build();
                DeleteResponse response = client.delete(request);
                LOG.debug("Index successfully deleted for {}[{}]: {}",
                        any.getType().getKind(), any.getKey(), response);
            } else {
                IndexRequest<Map<String, Object>> request = new IndexRequest.Builder<Map<String, Object>>().
                        index(ElasticsearchUtils.getAnyIndex(event.getDomain(), any.getType().getKind())).
                        id(any.getKey()).
                        document(elasticsearchUtils.document(any)).
                        build();
                IndexResponse response = client.index(request);
                LOG.debug("Index successfully created or updated for {}: {}", any, response);
            }
        } else if (event.getEntity() instanceof Realm realm) {
            if (event.getType() == SyncDeltaType.DELETE) {
                DeleteRequest request = new DeleteRequest.Builder().
                        index(ElasticsearchUtils.getRealmIndex(event.getDomain())).
                        id(realm.getKey()).
                        refresh(Refresh.True).
                        build();
                DeleteResponse response = client.delete(request);
                LOG.debug("Index successfully deleted for {}: {}", realm, response);
            } else {
                IndexRequest<Map<String, Object>> request = new IndexRequest.Builder<Map<String, Object>>().
                        index(ElasticsearchUtils.getRealmIndex(event.getDomain())).
                        id(realm.getKey()).
                        document(elasticsearchUtils.document(realm)).
                        refresh(Refresh.True).
                        build();
                IndexResponse response = client.index(request);
                LOG.debug("Index successfully created or updated for {}: {}", realm, response);
            }
        }
    }

    public void audit(final String domain, final AuditEvent auditEvent) throws IOException {
        LOG.debug("About to audit");

        IndexRequest<Map<String, Object>> request = new IndexRequest.Builder<Map<String, Object>>().
                index(ElasticsearchUtils.getAuditIndex(domain)).
                id(SecureRandomUtils.generateRandomUUID().toString()).
                document(elasticsearchUtils.document(auditEvent)).
                build();
        IndexResponse response = client.index(request);

        LOG.debug("Audit successfully created: {}", response);
    }
}
