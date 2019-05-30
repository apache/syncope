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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import org.apache.syncope.common.lib.to.GatewayRouteTO;
import org.apache.syncope.common.lib.types.FilterFactory;
import org.apache.syncope.common.lib.types.GatewayRouteFilter;
import org.apache.syncope.common.lib.types.GatewayRoutePredicate;
import org.apache.syncope.common.lib.types.GatewayRouteStatus;
import org.apache.syncope.common.lib.types.PredicateCond;
import org.apache.syncope.common.lib.types.PredicateFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.contract.wiremock.AutoConfigureWireMock;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWireMock(port = 0)
public class SyncopeSRATest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Autowired
    private WebTestClient webClient;

    @Autowired
    private RouteRefresher routeRefresher;

    @Value("${wiremock.server.port}")
    private int wiremockPort;

    @BeforeEach
    public void clearRoutes() {
        SyncopeSRATestCoreStartup.ROUTES.clear();
    }

    @Test
    public void root() {
        webClient.get().exchange().expectStatus().isNotFound();
    }

    @Test
    public void getAddResponseHeader() {
        // 1. no mapping for URL
        webClient.get().uri("/getAddResponseHeader").exchange().expectStatus().isNotFound();

        // 2. stub for proxied URL
        stubFor(get(urlEqualTo("/getAddResponseHeader")).willReturn(aResponse()));

        // 3. create route configuration
        GatewayRouteTO routeTO = new GatewayRouteTO();
        routeTO.setKey("getAddResponseHeader");
        routeTO.setStatus(GatewayRouteStatus.PUBLISHED);
        routeTO.setTarget(URI.create("http://localhost:" + wiremockPort));
        routeTO.getPredicates().add(new GatewayRoutePredicate.Builder().
                factory(PredicateFactory.METHOD).args("GET").build());
        routeTO.getPredicates().add(new GatewayRoutePredicate.Builder().
                factory(PredicateFactory.PATH).args("/getAddResponseHeader").cond(PredicateCond.AND).build());
        routeTO.getFilters().add(new GatewayRouteFilter.Builder().
                factory(FilterFactory.ADD_RESPONSE_HEADER).args("Hello,World").build());

        SyncopeSRATestCoreStartup.ROUTES.put(routeTO.getKey(), routeTO);

        routeRefresher.refresh();

        // 4. now mapping works for URL
        webClient.get().uri("/getAddResponseHeader").exchange().
                expectStatus().isOk().
                expectHeader().valueEquals("Hello", "World");

        // 5. update route configuration
        routeTO.getFilters().clear();
        routeTO.getFilters().add(new GatewayRouteFilter.Builder().
                factory(FilterFactory.ADD_RESPONSE_HEADER).args("Hello,WorldZ").build());

        routeRefresher.refresh();

        // 6. mapping for URL is updated too
        webClient.get().uri("/getAddResponseHeader").exchange().
                expectStatus().isOk().
                expectHeader().valueEquals("Hello", "WorldZ");

        // 7. update route configuration again
        routeTO.getFilters().clear();

        routeRefresher.refresh();

        // 8. mapping for URL is updated again
        webClient.get().uri("/getAddResponseHeader").exchange().
                expectStatus().isOk().
                expectHeader().doesNotExist("Hello");
    }

    @Test
    public void hystrix() {
        webClient.get().uri("/fallback").exchange().
                expectStatus().isOk().
                expectBody().
                consumeWith(response -> assertThat(response.getResponseBody()).isEqualTo("fallback".getBytes()));

        stubFor(get(urlEqualTo("/delay/3")).
                willReturn(aResponse().
                        withBody("no fallback").
                        withFixedDelay(3000)));

        GatewayRouteTO routeTO = new GatewayRouteTO();
        routeTO.setKey("hystrix");
        routeTO.setStatus(GatewayRouteStatus.PUBLISHED);
        routeTO.setTarget(URI.create("http://localhost:" + wiremockPort));
        routeTO.getPredicates().add(new GatewayRoutePredicate.Builder().
                factory(PredicateFactory.HOST).args("*.hystrix.com").build());
        routeTO.getFilters().add(new GatewayRouteFilter.Builder().
                factory(FilterFactory.HYSTRIX).args("fallbackcmd,forward:/fallback").build());

        SyncopeSRATestCoreStartup.ROUTES.put(routeTO.getKey(), routeTO);

        routeRefresher.refresh();

        webClient.get().uri("/delay/3").
                header(HttpHeaders.HOST, "www.hystrix.com").
                exchange().
                expectStatus().isOk().
                expectBody().
                consumeWith(response -> assertThat(response.getResponseBody()).isEqualTo("fallback".getBytes()));
    }

    @Test
    public void custom() {
        stubFor(post(urlEqualTo("/custom")).
                willReturn(aResponse().
                        withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).
                        withBody("{\"data\": \"data\"}")));

        GatewayRouteTO routeTO = new GatewayRouteTO();
        routeTO.setKey("custom");
        routeTO.setStatus(GatewayRouteStatus.PUBLISHED);
        routeTO.setTarget(URI.create("http://localhost:" + wiremockPort));
        routeTO.getPredicates().add(new GatewayRoutePredicate.Builder().
                factory(PredicateFactory.CUSTOM).
                args(BodyPropertyMatchingRoutePredicateFactory.class.getName() + ";cool").build());
        routeTO.getFilters().add(new GatewayRouteFilter.Builder().
                factory(FilterFactory.ADD_RESPONSE_HEADER).args("Custom,matched").build());
        routeTO.getFilters().add(new GatewayRouteFilter.Builder().
                factory(FilterFactory.CUSTOM).
                args(BodyPropertyAddingGatewayFilterFactory.class.getName() + ";customized=true").build());

        SyncopeSRATestCoreStartup.ROUTES.put(routeTO.getKey(), routeTO);

        routeRefresher.refresh();

        webClient.post().uri("/custom").
                body(BodyInserters.fromObject(MAPPER.createObjectNode().put("other", true))).
                exchange().
                expectStatus().isNotFound();

        webClient.post().uri("/custom").
                body(BodyInserters.fromObject(MAPPER.createObjectNode().put("cool", true))).
                exchange().
                expectStatus().isOk().
                expectHeader().valueEquals("Custom", "matched").
                expectBody().
                consumeWith(response -> {
                    try {
                        JsonNode body = MAPPER.readTree(response.getResponseBody());
                        assertTrue(body.has("customized"));
                        assertTrue(body.get("customized").asBoolean());
                    } catch (IOException e) {
                        fail(e.getMessage(), e);
                    }
                });
    }
}
