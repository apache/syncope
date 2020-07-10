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
import org.apache.syncope.core.persistence.api.entity.task.Task;
import org.apache.syncope.core.provisioning.api.event.TaskCreatedUpdatedEvent;
import org.apache.syncope.core.provisioning.api.event.TaskDeletedEvent;
import org.elasticsearch.action.delete.DeleteRequest;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetRequest;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateRequest;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.RequestOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listen to any create / update and delete in order to keep the Elasticsearch indexes consistent.
 */
public class ElasticsearchTaskIndexManager
        extends AbstractIndexManager<TaskCreatedUpdatedEvent<? extends Task>, TaskDeletedEvent> {

    private static final Logger LOG = LoggerFactory.getLogger(ElasticsearchTaskIndexManager.class);

    @Override
    @TransactionalEventListener
    public void afterCreate(final TaskCreatedUpdatedEvent<? extends Task> event) throws IOException {
        GetRequest getRequest = new GetRequest(
                Task.class.getSimpleName(),
                event.getTask().getClass().getSimpleName(),
                event.getTask().getKey());
        GetResponse getResponse = client.get(getRequest, RequestOptions.DEFAULT);
        if (getResponse.isExists()) {
            LOG.debug("About to update index for {}", event.getTask());

            UpdateRequest request = new UpdateRequest(
                    Task.class.getSimpleName(),
                    event.getTask().getClass().getSimpleName(),
                    event.getTask().getKey()).
                    retryOnConflict(elasticsearchUtils.getRetryOnConflict()).
                    doc(elasticsearchUtils.builder(event.getTask()));
            UpdateResponse response = client.update(request, RequestOptions.DEFAULT);
            LOG.debug("Index successfully updated for {}: {}", event.getTask(), response);
        } else {
            LOG.debug("About to create index for {}", event.getTask());

            IndexRequest request = new IndexRequest(
                    Task.class.getSimpleName(),
                    event.getTask().getClass().getSimpleName(),
                    event.getTask().getKey()).
                    source(elasticsearchUtils.builder(event.getTask()));
            IndexResponse response = client.index(request, RequestOptions.DEFAULT);
            LOG.debug("Index successfully created for {}: {}", event.getTask(), response);
        }
    }

    @Override
    @TransactionalEventListener
    public void afterDelete(final TaskDeletedEvent event) throws IOException {
        LOG.debug("About to delete index for {}[{}]", event.getTaskType(), event.getTaskKey());

        DeleteRequest request = new DeleteRequest(
                Task.class.getSimpleName(),
                event.getTaskType(),
                event.getTaskKey());
        DeleteResponse response = client.delete(request, RequestOptions.DEFAULT);
        LOG.debug("Index successfully deleted for {}[{}]: {}", event.getTaskType(), event.getTaskKey(), response);
    }
}
