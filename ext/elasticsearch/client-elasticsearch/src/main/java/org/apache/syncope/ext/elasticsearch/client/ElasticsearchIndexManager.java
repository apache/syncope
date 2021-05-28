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
import java.util.concurrent.ExecutionException;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.provisioning.api.event.AnyCreatedUpdatedEvent;
import org.apache.syncope.core.provisioning.api.event.AnyDeletedEvent;
import org.apache.syncope.core.spring.security.AuthContextUtils;
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
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.client.indices.CreateIndexRequest;
import org.elasticsearch.client.indices.CreateIndexResponse;
import org.elasticsearch.client.indices.GetIndexRequest;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listen to any create / update and delete in order to keep the Elasticsearch indexes consistent.
 */
public class ElasticsearchIndexManager {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchIndexManager.class);

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    private ElasticsearchUtils elasticsearchUtils;

    public boolean existsIndex(final String domain, final AnyTypeKind kind) throws IOException {
        return client.indices().exists(
                new GetIndexRequest(ElasticsearchUtils.getContextDomainName(domain, kind)), RequestOptions.DEFAULT);
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

    public XContentBuilder defaultMapping() throws IOException {
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

    public void createIndex(
            final String domain,
            final AnyTypeKind kind,
            final XContentBuilder settings,
            final XContentBuilder mapping)
            throws InterruptedException, ExecutionException, IOException {

        CreateIndexResponse response = client.indices().create(
                new CreateIndexRequest(ElasticsearchUtils.getContextDomainName(domain, kind)).
                        settings(settings).
                        mapping(mapping), RequestOptions.DEFAULT);
        LOG.debug("Successfully created {} for {}: {}",
                ElasticsearchUtils.getContextDomainName(domain, kind), kind.name(), response);
    }

    public void removeIndex(final String domain, final AnyTypeKind kind) throws IOException {
        AcknowledgedResponse acknowledgedResponse = client.indices().delete(
                new DeleteIndexRequest(ElasticsearchUtils.getContextDomainName(domain, kind)), RequestOptions.DEFAULT);
        LOG.debug("Successfully removed {}: {}",
                ElasticsearchUtils.getContextDomainName(domain, kind), acknowledgedResponse);
    }

    @TransactionalEventListener
    public void after(final AnyCreatedUpdatedEvent<Any<?>> event) throws IOException {
        GetRequest getRequest = new GetRequest(
                ElasticsearchUtils.getContextDomainName(
                        AuthContextUtils.getDomain(), event.getAny().getType().getKind()),
                event.getAny().getKey());
        GetResponse getResponse = client.get(getRequest, RequestOptions.DEFAULT);
        if (getResponse.isExists()) {
            LOG.debug("About to update index for {}", event.getAny());

            UpdateRequest request = new UpdateRequest(
                    ElasticsearchUtils.getContextDomainName(
                            AuthContextUtils.getDomain(), event.getAny().getType().getKind()),
                    event.getAny().getKey()).
                    retryOnConflict(elasticsearchUtils.getRetryOnConflict()).
                    doc(elasticsearchUtils.builder(event.getAny()));
            UpdateResponse response = client.update(request, RequestOptions.DEFAULT);
            LOG.debug("Index successfully updated for {}: {}", event.getAny(), response);
        } else {
            LOG.debug("About to create index for {}", event.getAny());

            IndexRequest request = new IndexRequest(
                    ElasticsearchUtils.getContextDomainName(
                            AuthContextUtils.getDomain(), event.getAny().getType().getKind())).
                    id(event.getAny().getKey()).
                    source(elasticsearchUtils.builder(event.getAny()));
            IndexResponse response = client.index(request, RequestOptions.DEFAULT);
            LOG.debug("Index successfully created for {}: {}", event.getAny(), response);
        }
    }

    @TransactionalEventListener
    public void after(final AnyDeletedEvent event) throws IOException {
        LOG.debug("About to delete index for {}[{}]", event.getAnyTypeKind(), event.getAnyKey());

        DeleteRequest request = new DeleteRequest(
                ElasticsearchUtils.getContextDomainName(AuthContextUtils.getDomain(), event.getAnyTypeKind()),
                event.getAnyKey());
        DeleteResponse response = client.delete(request, RequestOptions.DEFAULT);
        LOG.debug("Index successfully deleted for {}[{}]: {}",
                event.getAnyTypeKind(), event.getAnyKey(), response);
    }
}
