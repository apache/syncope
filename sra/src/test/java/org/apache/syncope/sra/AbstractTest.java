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
package org.apache.syncope.sra;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.net.Socket;
import java.util.Base64;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(locations = { "classpath:sra.properties", "classpath:test.properties" })
@ContextConfiguration(initializers = ZookeeperTestingServer.class)
@AutoConfigureWireMock(port = 0)
public abstract class AbstractTest {

    protected static final JsonMapper MAPPER = JsonMapper.builder().addModule(new JavaTimeModule()).build();

    public static boolean available(final int port) {
        try (Socket ignored = new Socket("localhost", port)) {
            return false;
        } catch (IOException ignored) {
            return true;
        }
    }

    @Autowired
    protected RouteRefresher routeRefresher;

    @Autowired
    protected ConfigurableApplicationContext ctx;

    @Value("${local.server.port}")
    protected int sraPort;

    @Value("${wiremock.server.port}")
    protected int wiremockPort;

    @Autowired
    private SRAProperties props;

    protected String basicAuthHeader() {
        return "Basic " + Base64.getEncoder().encodeToString(
                (props.getAnonymousUser() + ":" + props.getAnonymousKey()).getBytes());
    }
}
