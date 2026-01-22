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
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
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
import org.opensearch.client.opensearch._helpers.bulk.BulkIngester;
import org.opensearch.client.opensearch._helpers.bulk.BulkListener;
import org.opensearch.client.opensearch._types.ErrorCause;
import org.opensearch.client.opensearch._types.mapping.TypeMapping;
import org.opensearch.client.opensearch.core.BulkRequest;
import org.opensearch.client.opensearch.core.BulkResponse;
import org.opensearch.client.opensearch.core.bulk.BulkResponseItem;
import org.opensearch.client.opensearch.indices.IndexSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

/**
 * Remove and rebuild all OpenSearch indexes with information from existing users, groups and any objects.
 */
public class OpenSearchReindex extends AbstractSchedTaskJobDelegate<SchedTask> {

    protected static class ErrorLoggingBulkListener implements BulkListener<Void> {

        protected static final ErrorLoggingBulkListener INSTANCE = new ErrorLoggingBulkListener();

        @Override
        public void beforeBulk(
                final long executionId,
                final BulkRequest request,
                final List<Void> contexts) {

            // do nothing
        }

        @Override
        public void afterBulk(
                final long executionId,
                final BulkRequest request,
                final List<Void> contexts,
                final BulkResponse response) {

            if (response.errors()) {
                String details = response.items().stream().map(BulkResponseItem::error).
                        filter(Objects::nonNull).map(ErrorCause::toString).collect(Collectors.joining(", "));
                LOG.error("Errors found for request {}; details: {}", executionId, details);
            }
        }

        @Override
        public void afterBulk(
                final long executionId,
                final BulkRequest request,
                final List<Void> contexts,
                final Throwable failure) {

            LOG.error("Bulk request {} failed", executionId, failure);
        }
    }

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

    protected Pair<String, Long> reindexRealms() throws IOException {
        indexManager.createRealmIndex(AuthContextUtils.getDomain(), realmSettings(), realmMapping());

        long count = realmDAO.count();
        String index = OpenSearchUtils.getRealmIndex(AuthContextUtils.getDomain());
        setStatus("Indexing " + count + " realms under " + index + "...");

        try (BulkIngester<Void> ingester = BulkIngester.of(b -> b.client(client).
                maxOperations(AnyDAO.DEFAULT_PAGE_SIZE).listener(ErrorLoggingBulkListener.INSTANCE))) {

            for (int page = 0; page <= (count / AnyDAO.DEFAULT_PAGE_SIZE); page++) {
                Pageable pageable = PageRequest.of(page, AnyDAO.DEFAULT_PAGE_SIZE, DAO.DEFAULT_SORT);
                for (Realm realm : realmDAO.findAll(pageable)) {
                    ingester.add(op -> op.index(idx -> idx.
                            index(index).
                            id(realm.getKey()).
                            document(utils.document(realm))));
                }
            }
        } catch (Exception e) {
            LOG.error("Errors while ingesting index {}", index, e);
        }

        return Pair.of(index, count);
    }

    protected Pair<String, Long> reindexUsers() throws IOException {
        indexManager.createAnyIndex(AuthContextUtils.getDomain(), AnyTypeKind.USER, userSettings(), userMapping());

        long count = userDAO.count();
        String index = OpenSearchUtils.getAnyIndex(AuthContextUtils.getDomain(), AnyTypeKind.USER);
        setStatus("Indexing " + count + " users under " + index + "...");

        try (BulkIngester<Void> ingester = BulkIngester.of(b -> b.client(client).
                maxOperations(AnyDAO.DEFAULT_PAGE_SIZE).listener(ErrorLoggingBulkListener.INSTANCE))) {

            for (int page = 0; page <= (count / AnyDAO.DEFAULT_PAGE_SIZE); page++) {
                Pageable pageable = PageRequest.of(page, AnyDAO.DEFAULT_PAGE_SIZE, DAO.DEFAULT_SORT);
                for (User user : userDAO.findAll(pageable)) {
                    ingester.add(op -> op.index(idx -> idx.
                            index(index).
                            id(user.getKey()).
                            document(utils.document(user))));
                }
            }
        } catch (Exception e) {
            LOG.error("Errors while ingesting index {}", index, e);
        }

        return Pair.of(index, count);
    }

    protected Pair<String, Long> reindexGroups() throws IOException {
        indexManager.createAnyIndex(AuthContextUtils.getDomain(), AnyTypeKind.GROUP, groupSettings(), groupMapping());

        long count = groupDAO.count();
        String index = OpenSearchUtils.getAnyIndex(AuthContextUtils.getDomain(), AnyTypeKind.GROUP);
        setStatus("Indexing " + count + " groups under " + index + "...");

        try (BulkIngester<Void> ingester = BulkIngester.of(b -> b.client(client).
                maxOperations(AnyDAO.DEFAULT_PAGE_SIZE).listener(ErrorLoggingBulkListener.INSTANCE))) {

            for (int page = 0; page <= (count / AnyDAO.DEFAULT_PAGE_SIZE); page++) {
                Pageable pageable = PageRequest.of(page, AnyDAO.DEFAULT_PAGE_SIZE, DAO.DEFAULT_SORT);
                for (Group group : groupDAO.findAll(pageable)) {
                    ingester.add(op -> op.index(idx -> idx.
                            index(index).
                            id(group.getKey()).
                            document(utils.document(group))));
                }
            }
        } catch (Exception e) {
            LOG.error("Errors while ingesting index {}", index, e);
        }

        return Pair.of(index, count);
    }

    protected Pair<String, Long> reindexAnyObjects() throws IOException {
        indexManager.createAnyIndex(
                AuthContextUtils.getDomain(), AnyTypeKind.ANY_OBJECT, anyObjectSettings(), anyObjectMapping());

        long count = anyObjectDAO.count();
        String index = OpenSearchUtils.getAnyIndex(AuthContextUtils.getDomain(), AnyTypeKind.ANY_OBJECT);
        setStatus("Indexing " + count + " any objects under " + index + "...");

        try (BulkIngester<Void> ingester = BulkIngester.of(b -> b.client(client).
                maxOperations(AnyDAO.DEFAULT_PAGE_SIZE).listener(ErrorLoggingBulkListener.INSTANCE))) {

            for (int page = 0; page <= (count / AnyDAO.DEFAULT_PAGE_SIZE); page++) {
                Pageable pageable = PageRequest.of(page, AnyDAO.DEFAULT_PAGE_SIZE, DAO.DEFAULT_SORT);
                for (AnyObject anyObject : anyObjectDAO.findAll(pageable)) {
                    ingester.add(op -> op.index(idx -> idx.
                            index(index).
                            id(anyObject.getKey()).
                            document(utils.document(anyObject))));
                }
            }
        } catch (Exception e) {
            LOG.error("Errors while ingesting index {}", index, e);
        }

        return Pair.of(index, count);
    }

    protected String reindexAudit() throws IOException {
        indexManager.createAuditIndex(AuthContextUtils.getDomain(), auditSettings(), auditMapping());
        return OpenSearchUtils.getAuditIndex(AuthContextUtils.getDomain());
    }

    @Override
    protected String doExecute(final JobExecutionContext context) throws JobExecutionException {
        if (!context.isDryRun()) {
            setStatus("Start rebuilding indexes");

            try {
                Pair<String, Long> rindex = reindexRealms();

                Pair<String, Long> uindex = reindexUsers();

                Pair<String, Long> gindex = reindexGroups();

                Pair<String, Long> aindex = reindexAnyObjects();

                String audit = reindexAudit();

                setStatus("Rebuild indexes for domain " + AuthContextUtils.getDomain() + " successfully completed");

                return "Indexes created:\n"
                        + " " + rindex.getLeft() + " [" + rindex.getRight() + "]\n"
                        + " " + uindex.getLeft() + " [" + uindex.getRight() + "]\n"
                        + " " + gindex.getLeft() + " [" + gindex.getRight() + "]\n"
                        + " " + aindex.getLeft() + " [" + aindex.getRight() + "]\n"
                        + " " + audit;
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
