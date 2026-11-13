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
package org.apache.syncope.core.spring.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class VirtualThreadPoolTaskExecutorTest {

    @Test
    void executesTasks() throws Exception {
        VirtualThreadPoolTaskExecutor executor = new VirtualThreadPoolTaskExecutor();
        executor.setPoolSize(2);
        executor.setTaskTerminationTimeout(1_000);
        executor.afterPropertiesSet();

        try {
            CountDownLatch latch = new CountDownLatch(1);
            executor.execute(latch::countDown);

            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void submitsCallableAndRunnable() throws Exception {
        VirtualThreadPoolTaskExecutor executor = new VirtualThreadPoolTaskExecutor();
        executor.afterPropertiesSet();

        try {
            Future<String> result = executor.submit(() -> "done");
            assertEquals("done", result.get(5, TimeUnit.SECONDS));

            CountDownLatch latch = new CountDownLatch(1);
            Future<?> runnable = executor.submit(latch::countDown);
            runnable.get(5, TimeUnit.SECONDS);
            assertTrue(latch.await(5, TimeUnit.SECONDS));
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void appliesTaskDecorator() throws Exception {
        AtomicBoolean decorated = new AtomicBoolean();

        VirtualThreadPoolTaskExecutor executor = new VirtualThreadPoolTaskExecutor();
        executor.setTaskDecorator(task -> () -> {
            decorated.set(true);
            task.run();
        });
        executor.afterPropertiesSet();

        try {
            CountDownLatch latch = new CountDownLatch(1);
            executor.execute(latch::countDown);

            assertTrue(latch.await(5, TimeUnit.SECONDS));
            assertTrue(decorated.get());
        } finally {
            executor.shutdown();
        }
    }
}
