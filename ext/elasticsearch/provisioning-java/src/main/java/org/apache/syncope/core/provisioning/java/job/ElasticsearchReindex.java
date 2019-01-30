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
package org.apache.syncope.core.provisioning.java.job;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.ext.elasticsearch.client.ElasticsearchUtils;
import org.elasticsearch.action.admin.indices.create.CreateIndexRequest;
import org.elasticsearch.action.admin.indices.create.CreateIndexResponse;
import org.elasticsearch.action.admin.indices.delete.DeleteIndexRequest;
import org.elasticsearch.action.admin.indices.get.GetIndexRequest;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.action.support.master.AcknowledgedResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Remove and rebuild all Elasticsearch indexes with information from existing users, groups and any objects.
 */
public class ElasticsearchReindex extends AbstractSchedTaskJobDelegate {

    @Autowired
    private RestHighLevelClient client;

    @Autowired
    private ElasticsearchUtils elasticsearchUtils;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    @Override
    protected String doExecute(final boolean dryRun) throws JobExecutionException {
        if (!dryRun) {
            LOG.debug("Start rebuilding indexes");

            try {
                removeIndexIfExists(AnyTypeKind.USER);
                removeIndexIfExists(AnyTypeKind.GROUP);
                removeIndexIfExists(AnyTypeKind.ANY_OBJECT);

                createIndex(AnyTypeKind.USER);
                createIndex(AnyTypeKind.GROUP);
                createIndex(AnyTypeKind.ANY_OBJECT);

                LOG.debug("Indexing users...");
                for (int page = 1; page <= (userDAO.count() / AnyDAO.DEFAULT_PAGE_SIZE) + 1; page++) {

                    for (User user : userDAO.findAll(page, AnyDAO.DEFAULT_PAGE_SIZE)) {
                        IndexRequest request = new IndexRequest(
                                elasticsearchUtils.getContextDomainName(AnyTypeKind.USER),
                                AnyTypeKind.USER.name(),
                                user.getKey()).
                                source(elasticsearchUtils.builder(user));
                        IndexResponse response = client.index(request, RequestOptions.DEFAULT);
                        LOG.debug("Index successfully created for {}: {}", user, response);
                    }
                }

                LOG.debug("Indexing groups...");
                for (int page = 1; page <= (groupDAO.count() / AnyDAO.DEFAULT_PAGE_SIZE) + 1; page++) {
                    for (Group group : groupDAO.findAll(page, AnyDAO.DEFAULT_PAGE_SIZE)) {
                        IndexRequest request = new IndexRequest(
                                elasticsearchUtils.getContextDomainName(AnyTypeKind.GROUP),
                                AnyTypeKind.GROUP.name(),
                                group.getKey()).
                                source(elasticsearchUtils.builder(group));
                        IndexResponse response = client.index(request, RequestOptions.DEFAULT);
                        LOG.debug("Index successfully created for {}: {}", group, response);
                    }
                }

                LOG.debug("Indexing any objects...");
                for (int page = 1; page <= (anyObjectDAO.count() / AnyDAO.DEFAULT_PAGE_SIZE) + 1; page++) {
                    for (AnyObject anyObject : anyObjectDAO.findAll(page, AnyDAO.DEFAULT_PAGE_SIZE)) {
                        IndexRequest request = new IndexRequest(
                                elasticsearchUtils.getContextDomainName(AnyTypeKind.ANY_OBJECT),
                                AnyTypeKind.ANY_OBJECT.name(),
                                anyObject.getKey()).
                                source(elasticsearchUtils.builder(anyObject));
                        IndexResponse response = client.index(request, RequestOptions.DEFAULT);
                        LOG.debug("Index successfully created for {}: {}", anyObject, response);
                    }
                }

                LOG.debug("Rebuild indexes for domain {} successfully completed", AuthContextUtils.getDomain());
            } catch (Exception e) {
                throw new JobExecutionException("While rebuilding index for domain " + AuthContextUtils.getDomain(), e);
            }
        }

        return "SUCCESS";
    }

    private void removeIndexIfExists(final AnyTypeKind kind) throws IOException {
        if (client.indices().exists(
                new GetIndexRequest().indices(elasticsearchUtils.getContextDomainName(kind)), RequestOptions.DEFAULT)) {

            AcknowledgedResponse acknowledgedResponse = client.indices().delete(
                    new DeleteIndexRequest(elasticsearchUtils.getContextDomainName(kind)), RequestOptions.DEFAULT);
            LOG.debug("Successfully removed {}: {}",
                    elasticsearchUtils.getContextDomainName(kind), acknowledgedResponse);
        }
    }

    private void createIndex(final AnyTypeKind kind)
            throws InterruptedException, ExecutionException, IOException {

        XContentBuilder settings = XContentFactory.jsonBuilder().
                startObject().
                startObject("analysis").
                startObject("analyzer").
                startObject("string_lowercase").
                field("type", "custom").
                field("tokenizer", "standard").
                field("filter").
                startArray().
                value("lowercase").
                endArray().
                endObject().
                endObject().
                endObject().
                endObject();

        XContentBuilder mapping = XContentFactory.jsonBuilder().
                startObject().
                startArray("dynamic_templates").
                startObject().
                startObject("strings").
                field("match_mapping_type", "string").
                startObject("mapping").
                field("type", "keyword").
                field("analyzer", "string_lowercase").
                endObject().
                endObject().
                endObject().
                endArray().
                endObject();

        CreateIndexResponse response = client.indices().create(
                new CreateIndexRequest(elasticsearchUtils.getContextDomainName(kind)).settings(settings).
                        mapping(kind.name(), mapping), RequestOptions.DEFAULT);
        LOG.debug("Successfully created {} for {}: {}",
                elasticsearchUtils.getContextDomainName(kind), kind.name(), response);
    }

    @Override
    protected boolean hasToBeRegistered(final TaskExec execution) {
        return true;
    }
}
