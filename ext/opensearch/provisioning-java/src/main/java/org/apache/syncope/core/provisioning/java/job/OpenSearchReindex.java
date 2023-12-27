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
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.ext.opensearch.client.OpenSearchIndexManager;
import org.apache.syncope.ext.opensearch.client.OpenSearchUtils;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Remove and rebuild all OpenSearch indexes with information from existing users, groups and any objects.
 */
public class OpenSearchReindex extends AbstractSchedTaskJobDelegate<SchedTask> {

    @Autowired
    protected OpenSearchClient client;

    @Autowired
    protected OpenSearchIndexManager indexManager;

    @Autowired
    protected OpenSearchUtils utils;

    @Autowired
    protected UserDAO userDAO;

    @Autowired
    protected GroupDAO groupDAO;

    @Autowired
    protected AnyObjectDAO anyObjectDAO;

    @Autowired
    protected RealmDAO realmDAO;

    protected IndexSettings userSettings() throws IOException {
        return indexManager.defaultSettings();
    }

    protected IndexSettings groupSettings() throws IOException {
        return indexManager.defaultSettings();
    }

    protected IndexSettings anyObjectSettings() throws IOException {
        return indexManager.defaultSettings();
    }

    protected IndexSettings realmSettings() throws IOException {
        return indexManager.defaultSettings();
    }

    protected IndexSettings auditSettings() throws IOException {
        return indexManager.defaultSettings();
    }

    protected TypeMapping userMapping() throws IOException {
        return indexManager.defaultAnyMapping();
    }

    protected TypeMapping groupMapping() throws IOException {
        return indexManager.defaultAnyMapping();
    }

    protected TypeMapping anyObjectMapping() throws IOException {
        return indexManager.defaultAnyMapping();
    }

    protected TypeMapping realmMapping() throws IOException {
        return indexManager.defaultRealmMapping();
    }

    protected TypeMapping auditMapping() throws IOException {
        return indexManager.defaultAuditMapping();
    }

    @Override
    protected String doExecute(final boolean dryRun, final String executor, final JobExecutionContext context)
            throws JobExecutionException {

        if (!dryRun) {
            setStatus("Start rebuilding indexes");

            try {
                indexManager.createRealmIndex(AuthContextUtils.getDomain(), realmSettings(), realmMapping());

                int realms = realmDAO.count();
                String rindex = OpenSearchUtils.getRealmIndex(AuthContextUtils.getDomain());
                setStatus("Indexing " + realms + " realms under " + rindex + "...");
                for (int page = 1; page <= (realms / AnyDAO.DEFAULT_PAGE_SIZE) + 1; page++) {
                    BulkRequest.Builder bulkRequest = new BulkRequest.Builder();

                    for (String realm : realmDAO.findAllKeys(page, AnyDAO.DEFAULT_PAGE_SIZE)) {
                        bulkRequest.operations(op -> op.index(idx -> idx.
                                index(rindex).
                                id(realm).
                                document(utils.document(realmDAO.find(realm)))));
                    }

                    try {
                        BulkResponse response = client.bulk(bulkRequest.build());
                        LOG.debug("Index successfully created for {} [{}/{}]: {}",
                                rindex, page, AnyDAO.DEFAULT_PAGE_SIZE, response);
                    } catch (Exception e) {
                        LOG.error("Could not create index for {} [{}/{}]: {}",
                                rindex, page, AnyDAO.DEFAULT_PAGE_SIZE, e);
                    }
                }

                indexManager.createAnyIndex(
                        AuthContextUtils.getDomain(), AnyTypeKind.USER, userSettings(), userMapping());

                int users = userDAO.count();
                String uindex = OpenSearchUtils.getAnyIndex(AuthContextUtils.getDomain(), AnyTypeKind.USER);
                setStatus("Indexing " + users + " users under " + uindex + "...");
                for (int page = 1; page <= (users / AnyDAO.DEFAULT_PAGE_SIZE) + 1; page++) {
                    BulkRequest.Builder bulkRequest = new BulkRequest.Builder();

                    for (String user : userDAO.findAllKeys(page, AnyDAO.DEFAULT_PAGE_SIZE)) {
                        bulkRequest.operations(op -> op.index(idx -> idx.
                                index(uindex).
                                id(user).
                                document(utils.document(userDAO.find(user)))));
                    }

                    try {
                        BulkResponse response = client.bulk(bulkRequest.build());
                        LOG.debug("Index successfully created for {} [{}/{}]: {}",
                                uindex, page, AnyDAO.DEFAULT_PAGE_SIZE, response);
                    } catch (Exception e) {
                        LOG.error("Could not create index for {} [{}/{}]: {}",
                                uindex, page, AnyDAO.DEFAULT_PAGE_SIZE, e);
                    }
                }

                indexManager.createAnyIndex(
                        AuthContextUtils.getDomain(), AnyTypeKind.GROUP, groupSettings(), groupMapping());

                int groups = groupDAO.count();
                String gindex = OpenSearchUtils.getAnyIndex(AuthContextUtils.getDomain(), AnyTypeKind.GROUP);
                setStatus("Indexing " + groups + " groups under " + gindex + "...");
                for (int page = 1; page <= (groups / AnyDAO.DEFAULT_PAGE_SIZE) + 1; page++) {
                    BulkRequest.Builder bulkRequest = new BulkRequest.Builder();

                    for (String group : groupDAO.findAllKeys(page, AnyDAO.DEFAULT_PAGE_SIZE)) {
                        bulkRequest.operations(op -> op.index(idx -> idx.
                                index(gindex).
                                id(group).
                                document(utils.document(groupDAO.find(group)))));
                    }

                    try {
                        BulkResponse response = client.bulk(bulkRequest.build());
                        LOG.debug("Index successfully created for {} [{}/{}]: {}",
                                gindex, page, AnyDAO.DEFAULT_PAGE_SIZE, response);
                    } catch (Exception e) {
                        LOG.error("Could not create index for {} [{}/{}]: {}",
                                gindex, page, AnyDAO.DEFAULT_PAGE_SIZE, e);
                    }
                }

                indexManager.createAnyIndex(
                        AuthContextUtils.getDomain(), AnyTypeKind.ANY_OBJECT, anyObjectSettings(), anyObjectMapping());

                int anyObjects = anyObjectDAO.count();
                String aindex = OpenSearchUtils.getAnyIndex(AuthContextUtils.getDomain(), AnyTypeKind.ANY_OBJECT);
                setStatus("Indexing " + anyObjects + " any objects under " + aindex + "...");
                for (int page = 1; page <= (anyObjects / AnyDAO.DEFAULT_PAGE_SIZE) + 1; page++) {
                    BulkRequest.Builder bulkRequest = new BulkRequest.Builder();

                    for (String anyObject : anyObjectDAO.findAllKeys(page, AnyDAO.DEFAULT_PAGE_SIZE)) {
                        bulkRequest.operations(op -> op.index(idx -> idx.
                                index(aindex).
                                id(anyObject).
                                document(utils.document(anyObjectDAO.find(anyObject)))));
                    }

                    try {
                        BulkResponse response = client.bulk(bulkRequest.build());
                        LOG.debug("Index successfully created for {} [{}/{}]: {}",
                                aindex, page, AnyDAO.DEFAULT_PAGE_SIZE, response);
                    } catch (Exception e) {
                        LOG.error("Could not create index for {} [{}/{}]: {}",
                                aindex, page, AnyDAO.DEFAULT_PAGE_SIZE, e);
                    }
                }

                indexManager.createAuditIndex(AuthContextUtils.getDomain(), auditSettings(), auditMapping());

                setStatus("Rebuild indexes for domain " + AuthContextUtils.getDomain() + " successfully completed");

                return "Indexes created:\n"
                        + " " + rindex + " [" + realms + "]\n"
                        + " " + uindex + " [" + users + "]\n"
                        + " " + gindex + " [" + groups + "]\n"
                        + " " + aindex + " [" + anyObjects + "]\n"
                        + " " + OpenSearchUtils.getAuditIndex(AuthContextUtils.getDomain());
            } catch (Exception e) {
                throw new JobExecutionException("While rebuilding index for domain " + AuthContextUtils.getDomain(), e);
            }
        }

        return "SUCCESS";
    }

    @Override
    protected boolean hasToBeRegistered(final TaskExec<?> execution) {
        return true;
    }
}
