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

import javax.net.ssl.SSLException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.endpoint.ApiVersion;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.reactive.server.WebTestClient;

public class ActuatorTest extends AbstractTest {

    @Autowired
    private WebTestClient webClient;

    @Test
    public void health() throws SSLException {
        webClient.get().uri("/actuator/health").
                exchange().expectStatus().isUnauthorized();

        webClient.get().uri("/actuator/health").
                header(HttpHeaders.AUTHORIZATION, basicAuthHeader()).
                exchange().
                expectStatus().isOk().
                expectHeader().valueEquals(HttpHeaders.CONTENT_TYPE, ApiVersion.V3.getProducedMimeType().toString());
    }

    @Test
    public void routes() throws SSLException {
        webClient.get().uri("/actuator/gateway/routes").
                exchange().expectStatus().isUnauthorized();

        webClient.get().uri("/actuator/gateway/routes").
                header(HttpHeaders.AUTHORIZATION, basicAuthHeader()).
                exchange().expectStatus().isOk();
    }

    @Test
    public void requests() throws SSLException {
        webClient.get().uri("/actuator/metrics/gateway.requests").
                exchange().expectStatus().isUnauthorized();

        webClient.get().uri("/actuator/metrics/gateway.requests").
                header(HttpHeaders.AUTHORIZATION, basicAuthHeader()).
                exchange().expectStatus().isNotFound();
    }
}
