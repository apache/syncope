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

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkIngester;
import co.elastic.clients.elasticsearch._helpers.bulk.BulkListener;
import co.elastic.clients.elasticsearch._types.ErrorCause;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.ext.elasticsearch.client.ElasticsearchIndexManager;
import org.apache.syncope.ext.elasticsearch.client.ElasticsearchUtils;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Remove and rebuild all Elasticsearch indexes with information from existing users, groups and any objects.
 */
public class ElasticsearchReindex extends AbstractSchedTaskJobDelegate<SchedTask> {

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
    protected ElasticsearchClient client;

    @Autowired
    protected ElasticsearchIndexManager indexManager;

    @Autowired
    protected ElasticsearchUtils utils;

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

                long realms = realmDAO.count();
                String rindex = ElasticsearchUtils.getRealmIndex(AuthContextUtils.getDomain());
                setStatus("Indexing " + realms + " realms under " + rindex + "...");

                try (BulkIngester<Void> ingester = BulkIngester.of(b -> b.client(client).
                        maxOperations(AnyDAO.DEFAULT_PAGE_SIZE).listener(ErrorLoggingBulkListener.INSTANCE))) {

                    for (int page = 1; page <= (realms / AnyDAO.DEFAULT_PAGE_SIZE) + 1; page++) {
                        for (String realm : realmDAO.findAllKeys(page, AnyDAO.DEFAULT_PAGE_SIZE)) {
                            realmDAO.findById(realm).ifPresent(
                                    r -> ingester.add(op -> op.index(idx -> idx.
                                    index(rindex).
                                    id(realm).
                                    document(utils.document(r)))));
                        }
                    }
                } catch (Exception e) {
                    LOG.error("Errors while ingesting index {}", rindex, e);
                }

                indexManager.createAnyIndex(
                        AuthContextUtils.getDomain(), AnyTypeKind.USER, userSettings(), userMapping());

                long users = userDAO.count();
                String uindex = ElasticsearchUtils.getAnyIndex(AuthContextUtils.getDomain(), AnyTypeKind.USER);
                setStatus("Indexing " + users + " users under " + uindex + "...");

                try (BulkIngester<Void> ingester = BulkIngester.of(b -> b.client(client).
                        maxOperations(AnyDAO.DEFAULT_PAGE_SIZE).listener(ErrorLoggingBulkListener.INSTANCE))) {

                    for (int page = 1; page <= (users / AnyDAO.DEFAULT_PAGE_SIZE) + 1; page++) {
                        for (String user : userDAO.findAllKeys(page, AnyDAO.DEFAULT_PAGE_SIZE)) {
                            userDAO.findById(user).ifPresent(
                                    u -> ingester.add(op -> op.index(idx -> idx.
                                    index(uindex).
                                    id(user).
                                    document(utils.document(u)))));
                        }
                    }
                } catch (Exception e) {
                    LOG.error("Errors while ingesting index {}", uindex, e);
                }

                indexManager.createAnyIndex(
                        AuthContextUtils.getDomain(), AnyTypeKind.GROUP, groupSettings(), groupMapping());

                long groups = groupDAO.count();
                String gindex = ElasticsearchUtils.getAnyIndex(AuthContextUtils.getDomain(), AnyTypeKind.GROUP);
                setStatus("Indexing " + groups + " groups under " + gindex + "...");

                try (BulkIngester<Void> ingester = BulkIngester.of(b -> b.client(client).
                        maxOperations(AnyDAO.DEFAULT_PAGE_SIZE).listener(ErrorLoggingBulkListener.INSTANCE))) {

                    for (int page = 1; page <= (groups / AnyDAO.DEFAULT_PAGE_SIZE) + 1; page++) {
                        for (String group : groupDAO.findAllKeys(page, AnyDAO.DEFAULT_PAGE_SIZE)) {
                            groupDAO.findById(group).ifPresent(
                                    g -> ingester.add(op -> op.index(idx -> idx.
                                    index(gindex).
                                    id(group).
                                    document(utils.document(g)))));
                        }
                    }
                } catch (Exception e) {
                    LOG.error("Errors while ingesting index {}", gindex, e);
                }

                indexManager.createAnyIndex(
                        AuthContextUtils.getDomain(), AnyTypeKind.ANY_OBJECT, anyObjectSettings(), anyObjectMapping());

                long anyObjects = anyObjectDAO.count();
                String aindex = ElasticsearchUtils.getAnyIndex(AuthContextUtils.getDomain(), AnyTypeKind.ANY_OBJECT);
                setStatus("Indexing " + anyObjects + " any objects under " + aindex + "...");

                try (BulkIngester<Void> ingester = BulkIngester.of(b -> b.client(client).
                        maxOperations(AnyDAO.DEFAULT_PAGE_SIZE).listener(ErrorLoggingBulkListener.INSTANCE))) {

                    for (int page = 1; page <= (anyObjects / AnyDAO.DEFAULT_PAGE_SIZE) + 1; page++) {
                        for (String anyObject : anyObjectDAO.findAllKeys(page, AnyDAO.DEFAULT_PAGE_SIZE)) {
                            anyObjectDAO.findById(anyObject).ifPresent(
                                    a -> ingester.add(op -> op.index(idx -> idx.
                                    index(aindex).
                                    id(anyObject).
                                    document(utils.document(a)))));
                        }
                    }
                } catch (Exception e) {
                    LOG.error("Errors while ingesting index {}", aindex, e);
                }

                indexManager.createAuditIndex(AuthContextUtils.getDomain(), auditSettings(), auditMapping());

                setStatus("Rebuild indexes for domain " + AuthContextUtils.getDomain() + " successfully completed");

                return "Indexes created:\n"
                        + " " + rindex + " [" + realms + "]\n"
                        + " " + uindex + " [" + users + "]\n"
                        + " " + gindex + " [" + groups + "]\n"
                        + " " + aindex + " [" + anyObjects + "]\n"
                        + " " + ElasticsearchUtils.getAuditIndex(AuthContextUtils.getDomain());
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
