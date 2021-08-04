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
package org.apache.syncope.wa.starter;

import java.util.UUID;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(
        classes = { SyncopeWAApplication.class, AbstractTest.SyncopeTestConfiguration.class },
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "cas.authn.accept.users=mrossi::password",
            "cas.authn.syncope.url=http://localhost:8080",
            "cas.sso.allow-missing-service-parameter=true"
        })
@TestPropertySource(locations = { "classpath:wa.properties", "classpath:test.properties" })
@ContextConfiguration(initializers = ZookeeperTestingServer.class)
public abstract class AbstractTest {

    @LocalServerPort
    protected int port;

    protected static String getUUIDString() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    @TestConfiguration
    @ComponentScan("org.apache.syncope.wa.starter")
    public static class SyncopeTestConfiguration {

        @Bean
        public SyncopeCoreTestingServer syncopeCoreTestingServer() {
            return new SyncopeCoreTestingServer();
        }
    }
}
