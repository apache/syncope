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
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.spring.event.AnyCreatedUpdatedEvent;
import org.apache.syncope.core.spring.event.AnyDeletedEvent;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.elasticsearch.action.delete.DeleteResponse;
import org.elasticsearch.action.get.GetResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.update.UpdateResponse;
import org.elasticsearch.client.Client;
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
    private Client client;

    @Autowired
    private ElasticsearchUtils elasticsearchUtils;

    @TransactionalEventListener
    public void after(final AnyCreatedUpdatedEvent<Any<?>> event) throws IOException {
        GetResponse getResponse = client.prepareGet(AuthContextUtils.getDomain().toLowerCase(),
                event.getAny().getType().getKind().name(),
                event.getAny().getKey()).
                get();
        if (getResponse.isExists()) {
            LOG.debug("About to update index for {}", event.getAny());

            UpdateResponse response = client.prepareUpdate(
                    AuthContextUtils.getDomain().toLowerCase(),
                    event.getAny().getType().getKind().name(),
                    event.getAny().getKey()).
                    setRetryOnConflict(elasticsearchUtils.getRetryOnConflict()).
                    setDoc(elasticsearchUtils.builder(event.getAny())).
                    get();
            LOG.debug("Index successfully updated for {}: {}", event.getAny(), response);
        } else {
            LOG.debug("About to create index for {}", event.getAny());

            IndexResponse response = client.prepareIndex(
                    AuthContextUtils.getDomain().toLowerCase(),
                    event.getAny().getType().getKind().name(),
                    event.getAny().getKey()).
                    setSource(elasticsearchUtils.builder(event.getAny())).
                    get();

            LOG.debug("Index successfully created for {}: {}", event.getAny(), response);
        }
    }

    @TransactionalEventListener
    public void after(final AnyDeletedEvent event) {
        LOG.debug("About to delete index for {}[{}]", event.getAnyTypeKind(), event.getAnyKey());

        DeleteResponse response = client.prepareDelete(
                AuthContextUtils.getDomain().toLowerCase(),
                event.getAnyTypeKind().name(),
                event.getAnyKey()).
                get();

        LOG.debug("Index successfully deleted for {}[{}]: {}",
                event.getAnyTypeKind(), event.getAnyKey(), response);
    }
}
