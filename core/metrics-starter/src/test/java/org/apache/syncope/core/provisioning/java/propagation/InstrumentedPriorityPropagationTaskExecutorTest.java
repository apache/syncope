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
package org.apache.syncope.core.provisioning.java.propagation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Set;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ResourceOperation;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.task.PropagationData;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.ConnectorManager;
import org.apache.syncope.core.provisioning.api.data.TaskDataBinder;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskInfo;
import org.apache.syncope.core.provisioning.java.pushpull.OutboundMatcher;
import org.apache.syncope.core.provisioning.java.utils.ConnObjectUtils;
import org.apache.syncope.core.spring.security.AuthDataAccessor;
import org.identityconnectors.framework.common.objects.ObjectClass;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.task.AsyncTaskExecutor;

class InstrumentedPriorityPropagationTaskExecutorTest {

    private ConnectorManager connectorManager;

    private ConnObjectUtils connObjectUtils;

    private TaskDAO taskDAO;

    private ExternalResourceDAO resourceDAO;

    private PlainSchemaDAO plainSchemaDAO;

    private NotificationManager notificationManager;

    private AuditManager auditManager;

    private TaskDataBinder taskDataBinder;

    private AnyUtilsFactory anyUtilsFactory;

    private TaskUtilsFactory taskUtilsFactory;

    private OutboundMatcher outboundMatcher;

    private PlainAttrValidationManager validator;

    private ApplicationEventPublisher publisher;

    private AuthDataAccessor authDataAccessor;

    private AsyncTaskExecutor taskExecutor;

    private SimpleMeterRegistry meterRegistry;

    private InstrumentedPriorityPropagationTaskExecutor executor;

    @BeforeEach
    void setUp() {
        connectorManager = mock(ConnectorManager.class);
        connObjectUtils = mock(ConnObjectUtils.class);
        taskDAO = mock(TaskDAO.class);
        resourceDAO = mock(ExternalResourceDAO.class);
        plainSchemaDAO = mock(PlainSchemaDAO.class);
        notificationManager = mock(NotificationManager.class);
        auditManager = mock(AuditManager.class);
        taskDataBinder = mock(TaskDataBinder.class);
        anyUtilsFactory = mock(AnyUtilsFactory.class);
        taskUtilsFactory = mock(TaskUtilsFactory.class);
        outboundMatcher = mock(OutboundMatcher.class);
        validator = mock(PlainAttrValidationManager.class);
        publisher = mock(ApplicationEventPublisher.class);
        authDataAccessor = mock(AuthDataAccessor.class);
        taskExecutor = mock(AsyncTaskExecutor.class);
        meterRegistry = new SimpleMeterRegistry();

        when(authDataAccessor.getAuthorities(any(), isNull())).thenReturn(Set.of());

        executor = new InstrumentedPriorityPropagationTaskExecutor(
                connectorManager,
                connObjectUtils,
                taskDAO,
                resourceDAO,
                plainSchemaDAO,
                notificationManager,
                auditManager,
                taskDataBinder,
                anyUtilsFactory,
                taskUtilsFactory,
                outboundMatcher,
                validator,
                publisher,
                authDataAccessor,
                taskExecutor,
                meterRegistry);
    }

    @Test
    void propagationWithCreatedStatusIncrementsCounter() {
        ExternalResource resource = mock(ExternalResource.class);
        when(resource.getKey()).thenReturn("resource");
        when(resource.getPropagationPriority()).thenReturn(null);

        ObjectClass objectClass = mock(ObjectClass.class);
        PropagationData propagationData = mock(PropagationData.class);
        PropagationTaskInfo taskInfo = new PropagationTaskInfo(
                resource,
                ResourceOperation.CREATE,
                objectClass,
                AnyTypeKind.USER,
                "user",
                "entityKey",
                null,
                propagationData);

        executor.execute(List.of(taskInfo), true, "executor");

        assertEquals(
                1.0,
                meterRegistry.counter("syncope.propagation.created.count", "resource", "resource").count());
    }

    @Test
    void propagationWithMultipleStatusesIncrementsCounters() {
        ExternalResource first = mock(ExternalResource.class);
        when(first.getKey()).thenReturn("first");
        when(first.getPropagationPriority()).thenReturn(null);

        ExternalResource second = mock(ExternalResource.class);
        when(second.getKey()).thenReturn("second");
        when(second.getPropagationPriority()).thenReturn(null);

        ObjectClass objectClass = mock(ObjectClass.class);
        PropagationData propagationData = mock(PropagationData.class);

        PropagationTaskInfo firstInfo = new PropagationTaskInfo(
                first, ResourceOperation.CREATE, objectClass, AnyTypeKind.USER, "user",
                "entityKey1", null, propagationData);
        PropagationTaskInfo secondInfo = new PropagationTaskInfo(
                second, ResourceOperation.UPDATE, objectClass, AnyTypeKind.USER, "user",
                "entityKey2", null, propagationData);

        // Override the reporter used by the executor: the DefaultPropagationReporter created internally
        // deduplicates by resource, so each resource yields a single CREATED status.
        executor.execute(List.of(firstInfo, secondInfo), true, "executor");

        assertEquals(
                1.0,
                meterRegistry.counter("syncope.propagation.created.count", "resource", "first").count());
        assertEquals(
                1.0,
                meterRegistry.counter("syncope.propagation.created.count", "resource", "second").count());
    }
}
