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
package org.apache.syncope.core.spring.implementation;

import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.policy.DefaultPasswordRuleConf;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.SpringTestConfiguration;
import org.apache.syncope.core.spring.security.TestImplementation;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = { SpringTestConfiguration.class })
public class ImplementationManagerTest {

    public static DefaultPasswordRuleConf createBaseDefaultPasswordRuleConf() {
        DefaultPasswordRuleConf baseDefaultPasswordRuleConf = new DefaultPasswordRuleConf();
        baseDefaultPasswordRuleConf.setUppercase(1);
        baseDefaultPasswordRuleConf.setDigit(1);
        baseDefaultPasswordRuleConf.setMaxLength(1000);
        baseDefaultPasswordRuleConf.setMinLength(8);
        return baseDefaultPasswordRuleConf;
    }

    @Test
    public void concurrentPasswordRuleBuilding() {
        String body = POJOHelper.serialize(createBaseDefaultPasswordRuleConf());

        assertTimeoutPreemptively(Duration.ofSeconds(30), () -> {
            TestImplementation impl = new TestImplementation();
            impl.setBody(body);
            ReentrantLock lock = new ReentrantLock();
            lock.lock();
            AtomicInteger runningThreads = new AtomicInteger(0);
            AtomicInteger errorCount = new AtomicInteger(0);
            List<String> errorMessages = Collections.synchronizedList(new LinkedList<>());
            for (int i = 0; i < 10; i++) {
                runningThreads.incrementAndGet();
                new Thread(() -> {
                    try {
                        while (lock.isLocked()) {
                            Thread.yield();
                        }
                        try {
                            ImplementationManager.buildPasswordRule(
                                    impl,
                                    () -> null,
                                    instance -> {
                                    }).orElseThrow();
                        } catch (Exception e) {
                            errorMessages.add(e.getLocalizedMessage());
                            errorCount.incrementAndGet();
                        }
                    } finally {
                        runningThreads.decrementAndGet();
                    }
                }).start();
            }
            lock.unlock();
            while (runningThreads.get() > 0) {
                Thread.yield();
            }

            assertTrue(
                    errorMessages.isEmpty(),
                    () -> errorMessages.stream().collect(Collectors.joining(System.lineSeparator())));
        });
    }
}
