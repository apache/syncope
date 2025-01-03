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
import org.apache.syncope.core.persistence.api.dao.DAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.provisioning.api.job.JobExecutionContext;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.ext.opensearch.client.OpenSearchIndexManager;
import org.apache.syncope.ext.opensearch.client.OpenSearchUtils;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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

    protected IndexSettings userSettings() {
        return indexManager.defaultSettings();
    }

    protected IndexSettings groupSettings() {
        return indexManager.defaultSettings();
    }

    protected IndexSettings anyObjectSettings() {
        return indexManager.defaultSettings();
    }

    protected IndexSettings realmSettings() {
        return indexManager.defaultSettings();
    }

    protected IndexSettings auditSettings() {
        return indexManager.defaultSettings();
    }

    protected TypeMapping userMapping() {
        return indexManager.defaultAnyMapping();
    }

    protected TypeMapping groupMapping() {
        return indexManager.defaultAnyMapping();
    }

    protected TypeMapping anyObjectMapping() {
        return indexManager.defaultAnyMapping();
    }

    protected TypeMapping realmMapping() {
        return indexManager.defaultRealmMapping();
    }

    protected TypeMapping auditMapping() {
        return indexManager.defaultAuditMapping();
    }

    @Override
    protected String doExecute(final JobExecutionContext context) throws JobExecutionException {
        if (!context.isDryRun()) {
            setStatus("Start rebuilding indexes");

            try {
                indexManager.createRealmIndex(AuthContextUtils.getDomain(), realmSettings(), realmMapping());

                long realms = realmDAO.count();
                String rindex = OpenSearchUtils.getRealmIndex(AuthContextUtils.getDomain());
                setStatus("Indexing " + realms + " realms under " + rindex + "...");
                for (int page = 0; page <= (realms / AnyDAO.DEFAULT_PAGE_SIZE); page++) {
                    BulkRequest.Builder bulkRequest = new BulkRequest.Builder();

                    Pageable pageable = PageRequest.of(page, AnyDAO.DEFAULT_PAGE_SIZE, DAO.DEFAULT_SORT);
                    for (Realm realm : realmDAO.findAll(pageable)) {
                        bulkRequest.operations(op -> op.index(idx -> idx.
                                index(rindex).
                                id(realm.getKey()).
                                document(utils.document(realm))));
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

                long users = userDAO.count();
                String uindex = OpenSearchUtils.getAnyIndex(AuthContextUtils.getDomain(), AnyTypeKind.USER);
                setStatus("Indexing " + users + " users under " + uindex + "...");
                for (int page = 0; page <= (users / AnyDAO.DEFAULT_PAGE_SIZE); page++) {
                    BulkRequest.Builder bulkRequest = new BulkRequest.Builder();

                    Pageable pageable = PageRequest.of(page, AnyDAO.DEFAULT_PAGE_SIZE, DAO.DEFAULT_SORT);
                    for (User user : userDAO.findAll(pageable)) {
                        bulkRequest.operations(op -> op.index(idx -> idx.
                                index(uindex).
                                id(user.getKey()).
                                document(utils.document(user))));
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

                long groups = groupDAO.count();
                String gindex = OpenSearchUtils.getAnyIndex(AuthContextUtils.getDomain(), AnyTypeKind.GROUP);
                setStatus("Indexing " + groups + " groups under " + gindex + "...");
                for (int page = 0; page <= (groups / AnyDAO.DEFAULT_PAGE_SIZE); page++) {
                    BulkRequest.Builder bulkRequest = new BulkRequest.Builder();

                    Pageable pageable = PageRequest.of(page, AnyDAO.DEFAULT_PAGE_SIZE, DAO.DEFAULT_SORT);
                    for (Group group : groupDAO.findAll(pageable)) {
                        bulkRequest.operations(op -> op.index(idx -> idx.
                                index(gindex).
                                id(group.getKey()).
                                document(utils.document(group))));
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

                long anyObjects = anyObjectDAO.count();
                String aindex = OpenSearchUtils.getAnyIndex(AuthContextUtils.getDomain(), AnyTypeKind.ANY_OBJECT);
                setStatus("Indexing " + anyObjects + " any objects under " + aindex + "...");
                for (int page = 0; page <= (anyObjects / AnyDAO.DEFAULT_PAGE_SIZE); page++) {
                    BulkRequest.Builder bulkRequest = new BulkRequest.Builder();

                    Pageable pageable = PageRequest.of(page, AnyDAO.DEFAULT_PAGE_SIZE, DAO.DEFAULT_SORT);
                    for (AnyObject anyObject : anyObjectDAO.findAll(pageable)) {
                        bulkRequest.operations(op -> op.index(idx -> idx.
                                index(aindex).
                                id(anyObject.getKey()).
                                document(utils.document(anyObject))));
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
