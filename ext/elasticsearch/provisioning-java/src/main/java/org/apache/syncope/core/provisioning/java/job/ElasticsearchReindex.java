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
import org.elasticsearch.action.admin.indices.delete.DeleteIndexResponse;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsRequest;
import org.elasticsearch.action.admin.indices.exists.indices.IndicesExistsResponse;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.elasticsearch.common.xcontent.XContentFactory;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Remove and rebuild all Elasticsearch indexes with information from existing users, groups and any objects.
 */
public class ElasticsearchReindex extends AbstractSchedTaskJobDelegate {

    @Autowired
    private Client client;

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
            try {
                LOG.debug("Start rebuild index {}", AuthContextUtils.getDomain().toLowerCase());

                IndicesExistsResponse existsIndexResponse = client.admin().indices().
                        exists(new IndicesExistsRequest(AuthContextUtils.getDomain().toLowerCase())).
                        get();
                if (existsIndexResponse.isExists()) {
                    DeleteIndexResponse deleteIndexResponse = client.admin().indices().
                            delete(new DeleteIndexRequest(AuthContextUtils.getDomain().toLowerCase())).
                            get();
                    LOG.debug("Successfully removed {}: {}",
                            AuthContextUtils.getDomain().toLowerCase(), deleteIndexResponse);
                }

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
                CreateIndexResponse createIndexResponse = client.admin().indices().
                        create(new CreateIndexRequest(AuthContextUtils.getDomain().toLowerCase()).
                                settings(settings).
                                mapping(AnyTypeKind.USER.name(), mapping).
                                mapping(AnyTypeKind.GROUP.name(), mapping).
                                mapping(AnyTypeKind.ANY_OBJECT.name(), mapping)).
                        get();
                LOG.debug("Successfully created {}: {}",
                        AuthContextUtils.getDomain().toLowerCase(), createIndexResponse);

                LOG.debug("Indexing users...");
                for (int page = 1; page <= (userDAO.count() / AnyDAO.DEFAULT_PAGE_SIZE) + 1; page++) {
                    for (User user : userDAO.findAll(page, AnyDAO.DEFAULT_PAGE_SIZE)) {
                        IndexResponse response = client.prepareIndex(
                                AuthContextUtils.getDomain().toLowerCase(),
                                AnyTypeKind.USER.name(),
                                user.getKey()).
                                setSource(elasticsearchUtils.builder(user)).
                                get();
                        LOG.debug("Index successfully created for {}: {}", user, response);
                    }
                }
                LOG.debug("Indexing groups...");
                for (int page = 1; page <= (groupDAO.count() / AnyDAO.DEFAULT_PAGE_SIZE) + 1; page++) {
                    for (Group group : groupDAO.findAll(page, AnyDAO.DEFAULT_PAGE_SIZE)) {
                        IndexResponse response = client.prepareIndex(
                                AuthContextUtils.getDomain().toLowerCase(),
                                AnyTypeKind.GROUP.name(),
                                group.getKey()).
                                setSource(elasticsearchUtils.builder(group)).
                                get();
                        LOG.debug("Index successfully created for {}: {}", group, response);
                    }
                }
                LOG.debug("Indexing any objects...");
                for (int page = 1; page <= (anyObjectDAO.count() / AnyDAO.DEFAULT_PAGE_SIZE) + 1; page++) {
                    for (AnyObject anyObject : anyObjectDAO.findAll(page, AnyDAO.DEFAULT_PAGE_SIZE)) {
                        IndexResponse response = client.prepareIndex(
                                AuthContextUtils.getDomain().toLowerCase(),
                                AnyTypeKind.ANY_OBJECT.name(),
                                anyObject.getKey()).
                                setSource(elasticsearchUtils.builder(anyObject)).
                                get();
                        LOG.debug("Index successfully created for {}: {}", anyObject, response);
                    }
                }

                LOG.debug("Rebuild index {} successfully completed", AuthContextUtils.getDomain().toLowerCase());
            } catch (Exception e) {
                throw new JobExecutionException(
                        "While rebuilding index " + AuthContextUtils.getDomain().toLowerCase(), e);
            }
        }

        return "SUCCESS";
    }

    @Override
    protected boolean hasToBeRegistered(final TaskExec execution) {
        return true;
    }
}
