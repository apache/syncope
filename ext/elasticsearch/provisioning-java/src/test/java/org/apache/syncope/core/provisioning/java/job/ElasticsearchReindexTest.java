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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.mapping.TypeMapping;
import co.elastic.clients.elasticsearch.indices.IndexSettings;
import co.elastic.clients.transport.ElasticsearchTransport;
import java.io.IOException;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.provisioning.api.event.JobStatusEvent;
import org.apache.syncope.core.provisioning.api.job.JobExecutionContext;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.ext.elasticsearch.client.ElasticsearchIndexManager;
import org.apache.syncope.ext.elasticsearch.client.ElasticsearchUtils;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class ElasticsearchReindexTest {

    @Mock
    private ElasticsearchClient client;

    @Mock
    private ElasticsearchTransport transport;

    @Mock
    private ElasticsearchIndexManager indexManager;

    @Mock
    private ElasticsearchUtils utils;

    @Mock
    private UserDAO userDAO;

    @Mock
    private GroupDAO groupDAO;

    @Mock
    private AnyObjectDAO anyObjectDAO;

    @Mock
    private RealmDAO realmDAO;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private SchedTask task;

    private ElasticsearchReindex job;

    @BeforeEach
    public void setupJob() {
        SecurityContextHolder.clearContext();

        job = new ElasticsearchReindex();
        job.client = client;
        job.indexManager = indexManager;
        job.utils = utils;
        job.userDAO = userDAO;
        job.groupDAO = groupDAO;
        job.anyObjectDAO = anyObjectDAO;
        job.realmDAO = realmDAO;
        job.publisher = publisher;
        job.task = task;
    }

    @AfterEach
    public void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static JobExecutionContext context(final boolean dryRun) {
        return new JobExecutionContext("Master", "reindexJob", "admin", dryRun);
    }

    @Test
    public void dryRun() throws JobExecutionException {
        assertThat(job.doExecute(context(true))).isEqualTo("SUCCESS");

        verifyNoInteractions(indexManager, utils, userDAO, groupDAO, anyObjectDAO, realmDAO, publisher);
    }

    @Test
    public void execute() throws Exception {
        IndexSettings settings = new IndexSettings.Builder().numberOfShards("1").numberOfReplicas("1").build();
        TypeMapping anyMapping = new TypeMapping.Builder().build();
        TypeMapping realmMapping = new TypeMapping.Builder().build();
        TypeMapping auditMapping = new TypeMapping.Builder().build();
        when(indexManager.defaultSettings()).thenReturn(settings);
        when(indexManager.defaultAnyMapping()).thenReturn(anyMapping);
        when(indexManager.defaultRealmMapping()).thenReturn(realmMapping);
        when(indexManager.defaultAuditMapping()).thenReturn(auditMapping);

        when(realmDAO.count()).thenReturn(2L);
        when(userDAO.count()).thenReturn(3L);
        when(groupDAO.count()).thenReturn(4L);
        when(anyObjectDAO.count()).thenReturn(5L);

        // needed to let BulkIngester be created out of the mocked client
        when(client._transport()).thenReturn(transport);

        String result = job.doExecute(context(false));

        assertThat(result).isEqualTo("Indexes created:\n"
                + " master_realm [2]\n"
                + " master_user [3]\n"
                + " master_group [4]\n"
                + " master_any_object [5]\n"
                + " master_audit");

        verify(indexManager).createRealmIndex("Master", settings, realmMapping);
        verify(indexManager).createAnyIndex("Master", AnyTypeKind.USER, settings, anyMapping);
        verify(indexManager).createAnyIndex("Master", AnyTypeKind.GROUP, settings, anyMapping);
        verify(indexManager).createAnyIndex("Master", AnyTypeKind.ANY_OBJECT, settings, anyMapping);
        verify(indexManager).createAuditIndex("Master", settings, auditMapping);
        verify(publisher, atLeastOnce()).publishEvent(any(JobStatusEvent.class));
    }

    @Test
    public void executeFailure() throws Exception {
        when(indexManager.defaultSettings()).thenReturn(new IndexSettings.Builder().build());
        when(indexManager.defaultRealmMapping()).thenReturn(new TypeMapping.Builder().build());
        doThrow(new IOException("boom")).when(indexManager).
                createRealmIndex(anyString(), any(IndexSettings.class), any(TypeMapping.class));

        assertThatThrownBy(() -> job.doExecute(context(false))).
                isInstanceOf(JobExecutionException.class).
                hasMessage("While rebuilding index for domain " + AuthContextUtils.getDomain()).
                hasCauseInstanceOf(IOException.class);
    }
}
