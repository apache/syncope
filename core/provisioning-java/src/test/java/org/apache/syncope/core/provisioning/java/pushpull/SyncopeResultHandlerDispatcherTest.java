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
package org.apache.syncope.core.provisioning.java.pushpull;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.apache.syncope.common.lib.types.ConflictResolutionAction;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.lib.types.ThreadPoolSettings;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.provisioning.api.job.JobExecutionException;
import org.apache.syncope.core.provisioning.api.pushpull.AnyPushResultHandler;
import org.apache.syncope.core.provisioning.api.pushpull.ProvisioningProfile;
import org.apache.syncope.core.provisioning.api.pushpull.PushActions;
import org.apache.syncope.core.provisioning.api.pushpull.SyncopePushExecutor;
import org.junit.jupiter.api.Test;

class SyncopeResultHandlerDispatcherTest {

    private static final String USER = "USER";

    private static Any mockAny(final String key) {
        AnyType type = mock(AnyType.class);
        when(type.getKey()).thenReturn(USER);

        Any any = mock(Any.class);
        when(any.getKey()).thenReturn(key);
        when(any.getType()).thenReturn(type);

        return any;
    }

    private static AnyPushResultHandler blockingHandler(final CountDownLatch started, final CountDownLatch release) {
        AnyPushResultHandler handler = mock(AnyPushResultHandler.class);
        when(handler.handle(any(Any.class))).thenAnswer(invocation -> {
            started.countDown();
            release.await();
            return true;
        });
        return handler;
    }

    private static PushResultHandlerDispatcher buildDispatcher(
            final Integer poolSize, final int submitTimeoutSeconds, final int completionTimeoutSeconds) {

        PushTask task = mock(PushTask.class);
        if (poolSize != null) {
            ThreadPoolSettings concurrentSettings = new ThreadPoolSettings();
            concurrentSettings.setPoolSize(poolSize);
            when(task.getConcurrentSettings()).thenReturn(concurrentSettings);
        }

        ProvisioningProfile<PushTask, PushActions> profile = new ProvisioningProfile<>(
                null, TaskType.PUSH, task, ConflictResolutionAction.IGNORE, List.of(), "admin", false);

        PushResultHandlerDispatcher dispatcher = new PushResultHandlerDispatcher();
        dispatcher.setSubmitTimeoutSeconds(submitTimeoutSeconds);
        dispatcher.setCompletionTimeoutSeconds(completionTimeoutSeconds);

        return dispatcher.init(profile, mock(SyncopePushExecutor.class));
    }

    @Test
    void submitIsBoundedBySubmitTimeout() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        PushResultHandlerDispatcher dispatcher = buildDispatcher(1, 1, 60);
        dispatcher.addHandlerSupplier(USER, () -> blockingHandler(started, release));

        assertTrue(dispatcher.handle(mockAny("first")));
        assertTrue(started.await(5, TimeUnit.SECONDS));

        long start = System.nanoTime();
        assertFalse(dispatcher.handle(mockAny("second")));
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        assertTrue(elapsedMillis >= 1000, "expected to wait for the submit timeout, waited " + elapsedMillis + "ms");

        release.countDown();

        JobExecutionException e = assertThrows(JobExecutionException.class, dispatcher::shutdown);
        assertTrue(e.getMessage().contains("not submitted"), e.getMessage());
    }

    @Test
    void shutdownIsBoundedByCompletionTimeout() throws Exception {
        CountDownLatch started = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        PushResultHandlerDispatcher dispatcher = buildDispatcher(1, 60, 1);
        dispatcher.addHandlerSupplier(USER, () -> blockingHandler(started, release));

        assertTrue(dispatcher.handle(mockAny("first")));
        assertTrue(started.await(5, TimeUnit.SECONDS));

        long start = System.nanoTime();
        JobExecutionException e = assertThrows(JobExecutionException.class, dispatcher::shutdown);
        long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        assertTrue(elapsedMillis >= 1000, "expected to wait for the completion timeout, waited "
                + elapsedMillis + "ms");
        assertTrue(e.getMessage().contains("still running"), e.getMessage());

        release.countDown();
    }

    @Test
    void shutdownWaitsForCompletion() throws Exception {
        PushResultHandlerDispatcher dispatcher = buildDispatcher(2, 5, 5);
        dispatcher.addHandlerSupplier(USER, () -> {
            AnyPushResultHandler handler = mock(AnyPushResultHandler.class);
            when(handler.handle(any(Any.class))).thenReturn(true);
            return handler;
        });

        assertTrue(dispatcher.handle(mockAny("first")));
        assertTrue(dispatcher.handle(mockAny("second")));

        dispatcher.shutdown();
    }

    @Test
    void nonConcurrentHandlingIsUnchanged() throws Exception {
        AnyPushResultHandler handler = mock(AnyPushResultHandler.class);
        when(handler.handle(any(Any.class))).thenReturn(true);

        PushResultHandlerDispatcher dispatcher = buildDispatcher(null, 1, 1);
        dispatcher.addHandlerSupplier(USER, () -> handler);

        assertTrue(dispatcher.handle(mockAny("first")));

        // no executor involved: shutdown() has nothing to wait for
        dispatcher.shutdown();
    }
}
