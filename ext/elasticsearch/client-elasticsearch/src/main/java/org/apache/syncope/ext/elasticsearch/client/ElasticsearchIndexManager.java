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

import java.io.IOException;
import org.apache.syncope.common.lib.log.AuditEntry;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.provisioning.api.event.AnyLifecycleEvent;
import org.apache.syncope.core.spring.security.SecureRandomUtils;
import org.elasticsearch.ElasticsearchStatusException;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentFactory;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listen to any create / update and delete in order to keep the Elasticsearch indexes consistent.
 */
@SuppressWarnings("deprecation")
public class ElasticsearchIndexManager {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchIndexManager.class);

    protected final org.elasticsearch.client.RestHighLevelClient client;

    protected final ElasticsearchUtils elasticsearchUtils;

    public ElasticsearchIndexManager(
            final org.elasticsearch.client.RestHighLevelClient client,
            final ElasticsearchUtils elasticsearchUtils) {

        this.client = client;
        this.elasticsearchUtils = elasticsearchUtils;
    }

    public boolean existsAnyIndex(final String domain, final AnyTypeKind kind) throws IOException {
        return client.indices().exists(
                new GetIndexRequest(ElasticsearchUtils.getAnyIndex(domain, kind)), RequestOptions.DEFAULT);
    }

    public boolean existsAuditIndex(final String domain) throws IOException {
        return client.indices().exists(
                new GetIndexRequest(ElasticsearchUtils.getAuditIndex(domain)), RequestOptions.DEFAULT);
    }

    public XContentBuilder defaultSettings() throws IOException {
        return XContentFactory.jsonBuilder().
                startObject().
                startObject("analysis").
                startObject("normalizer").
                startObject("string_lowercase").
                field("type", "custom").
                field("char_filter", new Object[0]).
                field("filter").
                startArray().
                value("lowercase").
                endArray().
                endObject().
                endObject().
                endObject().
                startObject("index").
                field("number_of_shards", elasticsearchUtils.getNumberOfShards()).
                field("number_of_replicas", elasticsearchUtils.getNumberOfReplicas()).
                endObject().
                endObject();
    }

    public XContentBuilder defaultAnyMapping() throws IOException {
        return XContentFactory.jsonBuilder().
                startObject().
                startArray("dynamic_templates").
                startObject().
                startObject("strings").
                field("match_mapping_type", "string").
                startObject("mapping").
                field("type", "keyword").
                field("normalizer", "string_lowercase").
                endObject().
                endObject().
                endObject().
                endArray().
                endObject();
    }

    public XContentBuilder defaultAuditMapping() throws IOException {
        return XContentFactory.jsonBuilder().
                startObject().
                startArray("dynamic_templates").
                startObject().
                startObject("strings").
                field("match_mapping_type", "string").
                startObject("mapping").
                field("type", "keyword").
                field("normalizer", "string_lowercase").
                endObject().
                endObject().
                endObject().
                endArray().
                startObject("properties").
                startObject("message").
                startObject("properties").
                startObject("before").
                field("type", "text").
                field("analyzer", "standard").
                endObject().
                startObject("inputs").
                field("type", "text").
                field("analyzer", "standard").
                endObject().
                startObject("output").
                field("type", "text").
                field("analyzer", "standard").
                endObject().
                startObject("throwable").
                field("type", "text").
                field("analyzer", "standard").
                endObject().
                endObject().
                endObject().
                endObject().
                endObject();
    }

    protected CreateIndexResponse doCreateAnyIndex(
            final String domain,
            final AnyTypeKind kind,
            final XContentBuilder settings,
            final XContentBuilder mapping) throws IOException {

        return client.indices().create(
                new CreateIndexRequest(ElasticsearchUtils.getAnyIndex(domain, kind)).
                        settings(settings).
                        mapping(mapping), RequestOptions.DEFAULT);
    }

    public void createAnyIndex(
            final String domain,
            final AnyTypeKind kind,
            final XContentBuilder settings,
            final XContentBuilder mapping)
            throws IOException {

        try {
            CreateIndexResponse response = doCreateAnyIndex(domain, kind, settings, mapping);

            LOG.debug("Successfully created {} for {}: {}",
                    ElasticsearchUtils.getAnyIndex(domain, kind), kind.name(), response);
        } catch (ElasticsearchStatusException e) {
            LOG.debug("Could not create index {} because it already exists",
                    ElasticsearchUtils.getAnyIndex(domain, kind), e);

            removeAnyIndex(domain, kind);
            doCreateAnyIndex(domain, kind, settings, mapping);
        }
    }

    public void removeAnyIndex(final String domain, final AnyTypeKind kind) throws IOException {
        AcknowledgedResponse acknowledgedResponse = client.indices().delete(
                new DeleteIndexRequest(ElasticsearchUtils.getAnyIndex(domain, kind)), RequestOptions.DEFAULT);
        LOG.debug("Successfully removed {}: {}",
                ElasticsearchUtils.getAnyIndex(domain, kind), acknowledgedResponse);
    }

    protected CreateIndexResponse doCreateAuditIndex(
            final String domain,
            final XContentBuilder settings,
            final XContentBuilder mapping) throws IOException {

        return client.indices().create(
                new CreateIndexRequest(ElasticsearchUtils.getAuditIndex(domain)).
                        settings(settings).
                        mapping(mapping), RequestOptions.DEFAULT);
    }

    public void createAuditIndex(
            final String domain,
            final XContentBuilder settings,
            final XContentBuilder mapping)
            throws IOException {

        try {
            CreateIndexResponse response = doCreateAuditIndex(domain, settings, mapping);

            LOG.debug("Successfully created {} for {}: {}",
                    ElasticsearchUtils.getAuditIndex(domain), response);
        } catch (ElasticsearchStatusException e) {
            LOG.debug("Could not create index {} because it already exists",
                    ElasticsearchUtils.getAuditIndex(domain), e);

            removeAuditIndex(domain);
            doCreateAuditIndex(domain, settings, mapping);
        }
    }

    public void removeAuditIndex(final String domain) throws IOException {
        AcknowledgedResponse acknowledgedResponse = client.indices().delete(
                new DeleteIndexRequest(ElasticsearchUtils.getAuditIndex(domain)), RequestOptions.DEFAULT);
        LOG.debug("Successfully removed {}: {}",
                ElasticsearchUtils.getAuditIndex(domain), acknowledgedResponse);
    }

    @TransactionalEventListener
    public void any(final AnyLifecycleEvent<Any<?>> event) throws IOException {
        LOG.debug("About to {} index for {}", event.getType().name(), event.getAny());

        if (event.getType() == SyncDeltaType.DELETE) {
            DeleteRequest request = new DeleteRequest(
                    ElasticsearchUtils.getAnyIndex(event.getDomain(), event.getAny().getType().getKind()),
                    event.getAny().getKey());
            DeleteResponse response = client.delete(request, RequestOptions.DEFAULT);
            LOG.debug("Index successfully deleted for {}[{}]: {}",
                    event.getAny().getType().getKind(), event.getAny().getKey(), response);
        } else {
            GetRequest getRequest = new GetRequest(
                    ElasticsearchUtils.getAnyIndex(event.getDomain(), event.getAny().getType().getKind()),
                    event.getAny().getKey());
            GetResponse getResponse = client.get(getRequest, RequestOptions.DEFAULT);
            if (getResponse.isExists()) {
                LOG.debug("About to update index for {}", event.getAny());

                UpdateRequest request = new UpdateRequest(
                        ElasticsearchUtils.getAnyIndex(event.getDomain(), event.getAny().getType().getKind()),
                        event.getAny().getKey()).
                        retryOnConflict(elasticsearchUtils.getRetryOnConflict()).
                        doc(elasticsearchUtils.builder(event.getAny(), event.getDomain()));
                UpdateResponse response = client.update(request, RequestOptions.DEFAULT);
                LOG.debug("Index successfully updated for {}: {}", event.getAny(), response);
            } else {
                LOG.debug("About to create index for {}", event.getAny());

                IndexRequest request = new IndexRequest(
                        ElasticsearchUtils.getAnyIndex(event.getDomain(), event.getAny().getType().getKind())).
                        id(event.getAny().getKey()).
                        source(elasticsearchUtils.builder(event.getAny(), event.getDomain()));
                IndexResponse response = client.index(request, RequestOptions.DEFAULT);
                LOG.debug("Index successfully created for {}: {}", event.getAny(), response);
            }
        }
    }

    public void audit(final String domain, final long instant, final AuditEntry message)
            throws IOException {

        LOG.debug("About to audit");

        IndexRequest request = new IndexRequest(
                ElasticsearchUtils.getAuditIndex(domain)).
                id(SecureRandomUtils.generateRandomUUID().toString()).
                source(elasticsearchUtils.builder(instant, message, domain));
        IndexResponse response = client.index(request, RequestOptions.DEFAULT);

        LOG.debug("Audit successfully created: {}", response);
    }
}
