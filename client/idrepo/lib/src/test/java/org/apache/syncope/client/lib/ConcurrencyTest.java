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
package org.apache.syncope.client.lib;

import static org.junit.jupiter.api.Assertions.fail;

import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConcurrencyTest {

    private static final Logger LOG = LoggerFactory.getLogger(ConcurrencyTest.class);

    private static final int THREAD_NUMBER = 1000;

    private static final SyncopeClient CLIENT = new SyncopeClientFactoryBean().
            setAddress("http://url").create(new AuthenticationHandler() {
    });

    @Test
    public void multiThreadTest() throws InterruptedException {
        for (int i = 0; i < THREAD_NUMBER; i++) {
            Thread execution = new Thread("Th-" + StringUtils.leftPad(String.valueOf(i), 5, '0')) {

                @Override
                public void run() {

                    try {
                        CLIENT.getService(SyncopeService.class);

                        LOG.info(getName() + " completed successfully!");
                    } catch (Exception e) {
                        LOG.error(getName() + " did not complete", e);
                    }
                }
            };
            try {
                execution.start();
            } catch (OutOfMemoryError e) {
                // ignore
            }
        }

        Thread.sleep(THREAD_NUMBER);
    }

    @Test
    public void multiCallTest() {
        try {
            for (int i = 0; i < THREAD_NUMBER; i++) {
                CLIENT.getService(SyncopeService.class);
            }
        } catch (Exception e) {
            fail(e::getMessage);
        }
    }
}
