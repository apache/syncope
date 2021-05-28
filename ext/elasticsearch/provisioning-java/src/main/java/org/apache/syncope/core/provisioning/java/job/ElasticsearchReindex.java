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
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.ext.elasticsearch.client.ElasticsearchIndexManager;
import org.apache.syncope.ext.elasticsearch.client.ElasticsearchUtils;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RequestOptions;
import org.elasticsearch.client.RestHighLevelClient;
import org.elasticsearch.common.xcontent.XContentBuilder;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Remove and rebuild all Elasticsearch indexes with information from existing users, groups and any objects.
 */
public class ElasticsearchReindex extends AbstractSchedTaskJobDelegate {

    @Autowired
    protected RestHighLevelClient client;

    @Autowired
    protected ElasticsearchIndexManager indexManager;

    @Autowired
    protected ElasticsearchUtils elasticsearchUtils;

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected GroupDAO groupDAO;

    @Autowired
    protected AnyObjectDAO anyObjectDAO;

    protected XContentBuilder userSettings() throws IOException {
        return indexManager.defaultSettings();
    }

    protected XContentBuilder groupSettings() throws IOException {
        return indexManager.defaultSettings();
    }

    protected XContentBuilder anyObjectSettings() throws IOException {
        return indexManager.defaultSettings();
    }

    protected XContentBuilder userMapping() throws IOException {
        return indexManager.defaultMapping();
    }

    protected XContentBuilder groupMapping() throws IOException {
        return indexManager.defaultMapping();
    }

    protected XContentBuilder anyObjectMapping() throws IOException {
        return indexManager.defaultMapping();
    }

    @Override
    protected String doExecute(final boolean dryRun, final String executor, final JobExecutionContext context)
            throws JobExecutionException {

        if (!dryRun) {
            LOG.debug("Start rebuilding indexes");

            try {
                if (indexManager.existsIndex(AuthContextUtils.getDomain(), AnyTypeKind.USER)) {
                    indexManager.removeIndex(AuthContextUtils.getDomain(), AnyTypeKind.USER);
                }
                indexManager.createIndex(
                        AuthContextUtils.getDomain(), AnyTypeKind.USER, userSettings(), userMapping());

                if (indexManager.existsIndex(AuthContextUtils.getDomain(), AnyTypeKind.GROUP)) {
                    indexManager.removeIndex(AuthContextUtils.getDomain(), AnyTypeKind.GROUP);
                }
                indexManager.createIndex(
                        AuthContextUtils.getDomain(), AnyTypeKind.GROUP, groupSettings(), groupMapping());

                if (indexManager.existsIndex(AuthContextUtils.getDomain(), AnyTypeKind.ANY_OBJECT)) {
                    indexManager.removeIndex(AuthContextUtils.getDomain(), AnyTypeKind.ANY_OBJECT);
                }
                indexManager.createIndex(
                        AuthContextUtils.getDomain(), AnyTypeKind.ANY_OBJECT, anyObjectSettings(), anyObjectMapping());

                LOG.debug("Indexing users...");
                for (int page = 1; page <= (userDAO.count() / AnyDAO.DEFAULT_PAGE_SIZE) + 1; page++) {
                    for (String user : userDAO.findAllKeys(page, AnyDAO.DEFAULT_PAGE_SIZE)) {
                        IndexRequest request = new IndexRequest(
                                ElasticsearchUtils.getContextDomainName(
                                        AuthContextUtils.getDomain(), AnyTypeKind.USER)).
                                id(user).
                                source(elasticsearchUtils.builder(userDAO.find(user)));
                        IndexResponse response = client.index(request, RequestOptions.DEFAULT);
                        LOG.debug("Index successfully created for {}: {}", user, response);
                    }
                }

                LOG.debug("Indexing groups...");
                for (int page = 1; page <= (groupDAO.count() / AnyDAO.DEFAULT_PAGE_SIZE) + 1; page++) {
                    for (String group : groupDAO.findAllKeys(page, AnyDAO.DEFAULT_PAGE_SIZE)) {
                        IndexRequest request = new IndexRequest(
                                ElasticsearchUtils.getContextDomainName(
                                        AuthContextUtils.getDomain(), AnyTypeKind.GROUP)).
                                id(group).
                                source(elasticsearchUtils.builder(groupDAO.find(group)));
                        IndexResponse response = client.index(request, RequestOptions.DEFAULT);
                        LOG.debug("Index successfully created for {}: {}", group, response);
                    }
                }

                LOG.debug("Indexing any objects...");
                for (int page = 1; page <= (anyObjectDAO.count() / AnyDAO.DEFAULT_PAGE_SIZE) + 1; page++) {
                    for (String anyObject : anyObjectDAO.findAllKeys(page, AnyDAO.DEFAULT_PAGE_SIZE)) {
                        IndexRequest request = new IndexRequest(
                                ElasticsearchUtils.getContextDomainName(
                                        AuthContextUtils.getDomain(), AnyTypeKind.ANY_OBJECT)).
                                id(anyObject).
                                source(elasticsearchUtils.builder(anyObjectDAO.find(anyObject)));
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

    @Override
    protected boolean hasToBeRegistered(final TaskExec execution) {
        return true;
    }
}
