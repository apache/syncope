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
package org.apache.syncope.core.logic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import org.apache.syncope.common.lib.to.AnyTypeClassTO;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.event.AfterHandlingEvent;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class LogicInvocationHandlerTest {

    private NotificationManager notificationManager;

    private AuditManager auditManager;

    private LogicInvocationHandler handler;

    private ProceedingJoinPoint joinPoint;

    private AbstractLogic<AnyTypeClassTO> target;

    private Method method;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() throws Exception {
        notificationManager = mock(NotificationManager.class);
        auditManager = mock(AuditManager.class);
        handler = new LogicInvocationHandler(notificationManager, auditManager);

        method = Object.class.getMethod("toString");

        MethodSignature signature = mock(MethodSignature.class);
        when(signature.getMethod()).thenReturn(method);
        when(signature.getName()).thenReturn("operation");

        target = mock(AbstractLogic.class);

        joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.getTarget()).thenReturn(target);
        when(joinPoint.getSignature()).thenReturn(signature);
        when(joinPoint.getArgs()).thenReturn(new Object[] { "arg" });
    }

    @AfterEach
    void tearDown() {
        LogicTestAuth.clear();
    }

    @Test
    void noNotificationNorAudit() throws Throwable {
        when(joinPoint.proceed()).thenReturn("output");

        assertEquals("output", handler.around(joinPoint));

        verify(notificationManager, never()).createTasks(any());
        verify(auditManager, never()).audit(any());
    }

    @Test
    void notificationAndAudit() throws Throwable {
        when(notificationManager.notificationsAvailable(any(), any(), any(), any(), any())).thenReturn(true);
        when(auditManager.auditRequested(any(), any(), any(), any(), any(), any())).thenReturn(true);
        doThrow(new UnresolvedReferenceException()).when(target).resolveBeanReference(any(), any());
        when(joinPoint.proceed()).thenReturn("output");

        assertEquals("output", handler.around(joinPoint));

        verify(notificationManager).createTasks(any(AfterHandlingEvent.class));
        verify(auditManager).audit(any(AfterHandlingEvent.class));
    }

    @Test
    void beforeReferenceResolved() throws Throwable {
        when(auditManager.auditRequested(any(), any(), any(), any(), any(), any())).thenReturn(true);
        when(target.resolveBeanReference(any(), any())).thenReturn(new AnyTypeClassTO());
        when(joinPoint.proceed()).thenReturn("output");

        assertEquals("output", handler.around(joinPoint));

        verify(target).resolveBeanReference(any(), any());
        verify(auditManager).audit(any(AfterHandlingEvent.class));
    }

    @Test
    void failureIsPropagated() throws Throwable {
        when(notificationManager.notificationsAvailable(
                any(), any(), any(), any(), any())).thenReturn(true);
        when(joinPoint.proceed()).thenThrow(new IllegalStateException("failure"));

        assertThrows(IllegalStateException.class, () -> handler.around(joinPoint));

        verify(notificationManager).createTasks(any(AfterHandlingEvent.class));
        verify(auditManager).audit(any(AfterHandlingEvent.class));
    }

    @Test
    void notificationErrorsAreIgnored() throws Throwable {
        when(notificationManager.notificationsAvailable(
                any(), any(), any(), any(), any())).thenReturn(true);
        doThrow(new IllegalStateException("failure")).when(notificationManager).createTasks(any());
        when(joinPoint.proceed()).thenReturn("output");

        assertEquals("output", handler.around(joinPoint));
    }
}
