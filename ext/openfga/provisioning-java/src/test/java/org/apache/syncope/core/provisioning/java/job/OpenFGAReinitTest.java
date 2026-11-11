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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.provisioning.api.event.JobStatusEvent;
import org.apache.syncope.core.provisioning.api.job.JobExecutionContext;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.ext.openfga.client.OpenFGAClient;
import org.apache.syncope.ext.openfga.client.OpenFGAClientFactory;
import org.apache.syncope.ext.openfga.client.OpenFGAStoreManager;
import org.identityconnectors.framework.common.objects.SyncDeltaType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class OpenFGAReinitTest {

    private static JobExecutionContext context(final boolean dryRun) {
        return new JobExecutionContext(SyncopeConstants.MASTER_DOMAIN, "reinitJob", "admin", dryRun);
    }

    @Mock
    private OpenFGAClientFactory clientFactory;

    @Mock
    private OpenFGAStoreManager storeManager;

    @Mock
    private AnyTypeDAO anyTypeDAO;

    @Mock
    private RelationshipTypeDAO relationshipTypeDAO;

    @Mock
    private UserDAO userDAO;

    @Mock
    private GroupDAO groupDAO;

    @Mock
    private AnyObjectDAO anyObjectDAO;

    @Mock
    private ApplicationEventPublisher publisher;

    @Mock
    private SchedTask task;

    private OpenFGAReinit job;

    @BeforeEach
    void setUp() {
        job = new OpenFGAReinit();
        job.clientFactory = clientFactory;
        job.storeManager = storeManager;
        job.anyTypeDAO = anyTypeDAO;
        job.relationshipTypeDAO = relationshipTypeDAO;
        job.userDAO = userDAO;
        job.groupDAO = groupDAO;
        job.anyObjectDAO = anyObjectDAO;
        job.publisher = publisher;
        job.task = task;
    }

    @Test
    void dryRun() throws JobExecutionException {
        assertThat(job.doExecute(context(true))).isEqualTo("SUCCESS");

        verifyNoInteractions(clientFactory, storeManager,
                anyTypeDAO, relationshipTypeDAO, userDAO, groupDAO, anyObjectDAO, publisher);
    }

    @Test
    void execute() throws Exception {
        when(task.getKey()).thenReturn("reinitTask");

        OpenFGAClient client = mock(OpenFGAClient.class);
        when(clientFactory.get(SyncopeConstants.MASTER_DOMAIN)).thenReturn(client);

        AnyType userType = mock(AnyType.class);
        when(userType.getKind()).thenReturn(AnyTypeKind.USER);
        AnyType anyObjectType = mock(AnyType.class);
        when(anyObjectType.getKind()).thenReturn(AnyTypeKind.ANY_OBJECT);
        doReturn(List.of(userType, anyObjectType)).when(anyTypeDAO).findAll();

        RelationshipType first = mock(RelationshipType.class);
        RelationshipType second = mock(RelationshipType.class);
        doReturn(List.of(first, second)).when(relationshipTypeDAO).findAll();

        when(userDAO.count()).thenReturn(0L);
        doReturn(Page.empty()).when(userDAO).findAll(any(Pageable.class));
        when(groupDAO.count()).thenReturn(0L);
        doReturn(Page.empty()).when(groupDAO).findAll(any(Pageable.class));

        AnyObject anyObject = mock(AnyObject.class);
        when(anyObjectDAO.count()).thenReturn(1L);
        doReturn(new PageImpl<>(List.of(anyObject))).when(anyObjectDAO).findAll(any(Pageable.class));

        String result = job.doExecute(context(false));

        assertThat(result).isEqualTo("Authorization model replaced with USER, GROUP, 1 AnyTypes, Memberships "
                + "and 2 RelationshipTypes\n"
                + "Tuples:\n"
                + " * 0 Users\n"
                + " * 0 Groups\n"
                + " * 1 AnyObjects");

        verify(clientFactory).initAuthorizationModel(client, SyncopeConstants.MASTER_DOMAIN, true);
        verify(storeManager).handle(client, SyncDeltaType.CREATE, anyObjectType);
        verify(storeManager).handle(client, SyncDeltaType.CREATE, anyObject);
        verify(storeManager, never()).handle(client, SyncDeltaType.CREATE, userType);
        verify(storeManager, times(2)).
                handle(eq(client), eq(SyncDeltaType.CREATE), any(RelationshipType.class));
        verify(publisher, atLeastOnce()).publishEvent(any(JobStatusEvent.class));
    }

    @Test
    void failure() {
        when(task.getKey()).thenReturn("reinitTask");
        when(clientFactory.get(SyncopeConstants.MASTER_DOMAIN)).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> job.doExecute(context(false))).
                isInstanceOf(JobExecutionException.class).
                hasMessage("While rebuilding for domain " + SyncopeConstants.MASTER_DOMAIN).
                hasCauseInstanceOf(IllegalStateException.class);

        verifyNoInteractions(storeManager);
    }
}
