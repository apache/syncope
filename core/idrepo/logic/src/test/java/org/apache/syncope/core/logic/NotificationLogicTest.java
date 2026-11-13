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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.apache.syncope.common.lib.to.JobTO;
import org.apache.syncope.common.lib.to.NotificationTO;
import org.apache.syncope.common.lib.types.JobAction;
import org.apache.syncope.common.lib.types.JobType;
import org.apache.syncope.core.persistence.api.dao.JobStatusDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.NotificationDAO;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.apache.syncope.core.provisioning.api.data.NotificationDataBinder;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.apache.syncope.core.provisioning.java.job.SyncopeTaskScheduler;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class NotificationLogicTest {

    private JobManager jobManager;

    private SyncopeTaskScheduler scheduler;

    private JobStatusDAO jobStatusDAO;

    private NotificationDAO notificationDAO;

    private NotificationDataBinder binder;

    private NotificationLogic logic;

    @BeforeEach
    void setUp() {
        jobManager = mock(JobManager.class);
        scheduler = mock(SyncopeTaskScheduler.class);
        jobStatusDAO = mock(JobStatusDAO.class);
        notificationDAO = mock(NotificationDAO.class);
        binder = mock(NotificationDataBinder.class);

        logic = new NotificationLogic(jobManager, scheduler, jobStatusDAO, notificationDAO, binder);
    }

    @AfterEach
    void tearDown() {
        LogicTestAuth.clear();
    }

    @Test
    void readMissing() {
        doReturn(Optional.empty()).when(notificationDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.read("missing"));
    }

    @Test
    void readAndList() {
        Notification notification = mock(Notification.class);
        NotificationTO to = new NotificationTO();
        to.setKey("key");

        doReturn(Optional.of(notification)).when(notificationDAO).findById("key");
        doReturn(List.of(notification)).when(notificationDAO).findAll();
        when(binder.getNotificationTO(notification)).thenReturn(to);

        assertSame(to, logic.read("key"));
        assertEquals(List.of(to), logic.list());
    }

    @Test
    void create() {
        NotificationTO to = new NotificationTO();
        to.setKey("key");
        Notification notification = mock(Notification.class);

        when(binder.create(to)).thenReturn(notification);
        when(notificationDAO.save(notification)).thenReturn(notification);
        when(binder.getNotificationTO(notification)).thenReturn(to);

        assertSame(to, logic.create(to));
    }

    @Test
    void updateMissing() {
        NotificationTO to = new NotificationTO();
        to.setKey("missing");
        doReturn(Optional.empty()).when(notificationDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.update(to));
    }

    @Test
    void update() {
        NotificationTO to = new NotificationTO();
        to.setKey("key");
        Notification notification = mock(Notification.class);

        doReturn(Optional.of(notification)).when(notificationDAO).findById("key");
        when(notificationDAO.save(notification)).thenReturn(notification);
        when(binder.getNotificationTO(notification)).thenReturn(to);

        assertSame(to, logic.update(to));
        verify(binder).update(notification, to);
    }

    @Test
    void deleteMissing() {
        doReturn(Optional.empty()).when(notificationDAO).findById("missing");

        assertThrows(NotFoundException.class, () -> logic.delete("missing"));
    }

    @Test
    void delete() {
        Notification notification = mock(Notification.class);
        NotificationTO to = new NotificationTO();
        to.setKey("key");

        doReturn(Optional.of(notification)).when(notificationDAO).findById("key");
        when(binder.getNotificationTO(notification)).thenReturn(to);

        assertSame(to, logic.delete("key"));
        verify(notificationDAO).deleteById("key");
    }

    @Test
    void getJob() {
        LogicTestAuth.authenticate("admin");

        when(scheduler.getJobNames("Master")).thenReturn(List.of(JobManager.NOTIFICATION_JOB));
        when(scheduler.getNextTrigger("Master", JobManager.NOTIFICATION_JOB)).thenReturn(Optional.empty());
        when(jobManager.isRunning(JobManager.NOTIFICATION_JOB)).thenReturn(false);
        when(jobStatusDAO.get(JobManager.NOTIFICATION_JOB)).thenReturn("status");

        JobTO job = logic.getJob();

        assertNotNull(job);
        assertEquals(JobType.NOTIFICATION, job.getType());
        assertFalse(job.isScheduled());
        assertFalse(job.isRunning());
        assertEquals("status", job.getStatus());
    }

    @Test
    void actionJob() {
        logic.actionJob(JobAction.START);

        verify(scheduler).start("Master", JobManager.NOTIFICATION_JOB);
    }
}
