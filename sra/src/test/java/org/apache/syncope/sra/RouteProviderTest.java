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
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;
import java.net.URI;
import java.time.ZonedDateTime;
import org.apache.syncope.common.lib.to.SRARouteTO;
import org.apache.syncope.common.lib.types.SRARouteFilter;
import org.apache.syncope.common.lib.types.SRARouteFilterFactory;
import org.apache.syncope.common.lib.types.SRARoutePredicate;
import org.apache.syncope.common.lib.types.SRARoutePredicateCond;
import org.apache.syncope.common.lib.types.SRARoutePredicateFactory;
import org.apache.syncope.common.lib.types.SRARouteType;
import org.apache.syncope.sra.filters.BodyPropertyAddingGatewayFilterFactory;
import org.apache.syncope.sra.filters.PrincipalToRequestHeaderFilterFactory;
import org.apache.syncope.sra.predicates.BodyPropertyMatchingRoutePredicateFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.server.context.WebSessionServerSecurityContextRepository;
import org.springframework.session.MapSession;
import org.springframework.session.Session;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;

public class RouteProviderTest extends AbstractTest {

    @Autowired
    private WebTestClient webClient;

    @BeforeEach
    public void clearRoutes() {
        SyncopeCoreTestingServer.ROUTES.clear();
    }

    @Test
    public void root() {
        webClient.get().exchange().expectStatus().isNotFound();
    }

    @Test
    public void addResponseHeader() {
        // 1. no mapping for URL
        webClient.get().uri("/addResponseHeader").exchange().expectStatus().isNotFound();

        // 2. stub for proxied URL
        stubFor(get(urlEqualTo("/addResponseHeader")).willReturn(aResponse()));

        // 3. create route configuration
        SRARouteTO route = new SRARouteTO();
        route.setKey("addResponseHeader");
        route.setTarget(URI.create("http://localhost:" + wiremockPort));
        route.getPredicates().add(new SRARoutePredicate.Builder().
                factory(SRARoutePredicateFactory.METHOD).args("GET").build());
        route.getPredicates().add(new SRARoutePredicate.Builder().
                factory(SRARoutePredicateFactory.PATH).args("/addResponseHeader").cond(SRARoutePredicateCond.AND).
                build());
        route.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.ADD_RESPONSE_HEADER).args("Hello,World").build());

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        // 4. now mapping works for URL
        webClient.get().uri("/addResponseHeader").exchange().
                expectStatus().isOk().
                expectHeader().valueEquals("Hello", "World");

        // 5. update route configuration
        route.getFilters().clear();
        route.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.ADD_RESPONSE_HEADER).args("Hello,WorldZ").build());

        routeRefresher.refresh();

        // 6. mapping for URL is updated too
        webClient.get().uri("/addResponseHeader").exchange().
                expectStatus().isOk().
                expectHeader().valueEquals("Hello", "WorldZ");

        // 7. update route configuration again
        route.getFilters().clear();

        routeRefresher.refresh();

        // 8. mapping for URL is updated again
        webClient.get().uri("/addResponseHeader").exchange().
                expectStatus().isOk().
                expectHeader().doesNotExist("Hello");
    }

    @Test
    public void addRequestHeader() {
        webClient.get().uri("/requestHeader").exchange().expectStatus().isNotFound();

        stubFor(get(urlEqualTo("/requestHeader")).withHeader("Hello", equalTo("World")).willReturn(aResponse()));

        SRARouteTO route = new SRARouteTO();
        route.setKey("requestHeader");
        route.setTarget(URI.create("http://localhost:" + wiremockPort));
        route.getPredicates().add(new SRARoutePredicate.Builder().
                factory(SRARoutePredicateFactory.REMOTE_ADDR).args("localhost").build());
        route.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.ADD_REQUEST_HEADER).args("Hello,World").build());

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/requestHeader").exchange().expectStatus().isOk();

        route.getFilters().clear();
        route.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.ADD_REQUEST_HEADER).args("Hello,Mondo").build());

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/requestHeader").exchange().expectStatus().isNotFound();

        route.getFilters().clear();
        route.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.REMOVE_REQUEST_HEADER).args("Hello").build());

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/requestHeader").header("Hello", "World").exchange().expectStatus().isNotFound();

        route.getFilters().clear();
        route.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.SET_REQUEST_HEADER).args("Hello, World").build());

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/requestHeader").header("Hello", "Mondo").exchange().expectStatus().isOk();
    }

    @Test
    public void requestHeaderToRequestUri() {
        webClient.get().uri("/requestHeaderToRequestUri").exchange().expectStatus().isNotFound();

        stubFor(get(urlEqualTo("/requestHeaderToRequestUri")).willReturn(aResponse()));

        SRARouteTO route = new SRARouteTO();
        route.setKey("requestHeaderToRequestUri");
        route.setTarget(URI.create("http://localhost:" + wiremockPort));
        route.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.REQUEST_HEADER_TO_REQUEST_URI).args("NewUri").build());

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/requestHeaderToRequestUri").
                header("NewUri", "http://localhost:" + wiremockPort + "/requestHeaderToRequestUri").
                exchange().expectStatus().isOk();
    }

    @Test
    public void responseHeader() {
        webClient.get().uri("/responseHeader").exchange().expectStatus().isNotFound();

        stubFor(get(urlEqualTo("/responseHeader")).willReturn(aResponse().withHeader("Hello", "World")));

        SRARouteTO route = new SRARouteTO();
        route.setKey("responseHeader");
        route.setTarget(URI.create("http://localhost:" + wiremockPort));
        route.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.REMOVE_RESPONSE_HEADER).args("Hello").build());

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/responseHeader").exchange().
                expectStatus().isOk().
                expectHeader().doesNotExist("Hello");

        route.getFilters().clear();

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/responseHeader").exchange().
                expectStatus().isOk().
                expectHeader().valueEquals("Hello", "World");

        route.getFilters().clear();
        route.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.REWRITE_RESPONSE_HEADER).args("Hello,World,Mondo").build());
        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/responseHeader").exchange().
                expectStatus().isOk().
                expectHeader().valueEquals("Hello", "Mondo");

        route.getFilters().clear();
        route.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.SET_RESPONSE_HEADER).args("Hello,Mondo").build());
        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/responseHeader").exchange().
                expectStatus().isOk().
                expectHeader().valueEquals("Hello", "Mondo");
    }

    @Test
    public void addRequestParameter() {
        webClient.get().uri("/addRequestParameter?Hello=World").exchange().expectStatus().isNotFound();

        stubFor(get(urlEqualTo("/addRequestParameter?Hello=World")).withQueryParam("Hello", equalTo("World")).
                willReturn(aResponse()));

        SRARouteTO route = new SRARouteTO();
        route.setKey("addRequestParameter");
        route.setTarget(URI.create("http://localhost:" + wiremockPort));
        route.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.ADD_REQUEST_PARAMETER).args("Hello,World").build());

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/addRequestParameter").exchange().expectStatus().isOk();

        route.getFilters().clear();
        route.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.ADD_REQUEST_PARAMETER).args("Hello,Mondo").build());

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/addRequestParameter").exchange().expectStatus().isNotFound();
    }

    @Test
    public void rewritePath() {
        webClient.get().uri("/rewrite").exchange().expectStatus().isNotFound();

        stubFor(get(urlEqualTo("/rewrite")).willReturn(aResponse()));

        SRARouteTO route = new SRARouteTO();
        route.setKey("rewrite");
        route.setTarget(URI.create("http://localhost:" + wiremockPort));
        route.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.REWRITE_PATH).args("/remove/(?<segment>.*), /${segment}").build());
        route.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.SECURE_HEADERS).build());

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/remove/rewrite").exchange().
                expectStatus().isOk().
                expectHeader().valueEquals("X-XSS-Protection", "1 ; mode=block");

        route.getFilters().clear();

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/remove/rewrite").exchange().
                expectStatus().isNotFound();

        route.getFilters().clear();

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/rewrite").exchange().
                expectStatus().isOk().
                expectHeader().doesNotExist("X-XSS-Protection");

        route.getFilters().clear();
        route.getPredicates().add(new SRARoutePredicate.Builder().
                factory(SRARoutePredicateFactory.PATH).args("/remove/{segment}").build());
        route.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.SET_PATH).args("/{segment}").build());

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/remove/rewrite").exchange().
                expectStatus().isOk();

        route.getFilters().clear();
        route.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.STRIP_PREFIX).args("1").build());

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/remove/rewrite").exchange().expectStatus().isOk();
    }

    @Test
    public void redirect() {
        webClient.get().uri("/redirect").exchange().expectStatus().isNotFound();

        stubFor(get(urlEqualTo("/redirect")).willReturn(aResponse()));

        SRARouteTO route = new SRARouteTO();
        route.setKey("redirect");
        route.setTarget(URI.create("http://localhost:" + wiremockPort));
        route.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.REDIRECT_TO).args("307,http://127.0.0.1:" + wiremockPort).build());

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/redirect").exchange().expectStatus().isTemporaryRedirect();

        route.getFilters().clear();

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/redirect").exchange().expectStatus().isOk();

        route.getFilters().clear();
        route.getFilters().add(new SRARouteFilter.Builder().factory(SRARouteFilterFactory.SET_STATUS).args("404").
                build());

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/redirect").exchange().expectStatus().isNotFound();
    }

    @Test
    public void datetime() {
        webClient.get().uri("/prefix/datetime").exchange().expectStatus().isNotFound();

        stubFor(get(urlEqualTo("/prefix/datetime")).willReturn(aResponse()));

        SRARouteTO route = new SRARouteTO();
        route.setKey("datetime");
        route.setTarget(URI.create("http://localhost:" + wiremockPort));
        route.getPredicates().add(new SRARoutePredicate.Builder().
                factory(SRARoutePredicateFactory.AFTER).args(ZonedDateTime.now().minusYears(1).toString()).build());
        route.getPredicates().add(new SRARoutePredicate.Builder().
                factory(SRARoutePredicateFactory.BEFORE).args(ZonedDateTime.now().plusYears(1).toString()).
                cond(SRARoutePredicateCond.AND).build());
        route.getPredicates().add(new SRARoutePredicate.Builder().
                factory(SRARoutePredicateFactory.BETWEEN).args(ZonedDateTime.now().minusYears(1) + ","
                + ZonedDateTime.now().plusYears(1)).
                cond(SRARoutePredicateCond.AND).build());
        route.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.PREFIX_PATH).args("/prefix").build());

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/datetime").exchange().
                expectStatus().isOk();

        route.getPredicates().clear();
        route.getPredicates().add(new SRARoutePredicate.Builder().
                factory(SRARoutePredicateFactory.AFTER).args(ZonedDateTime.now().plusYears(1).toString()).build());
        route.getPredicates().add(new SRARoutePredicate.Builder().
                factory(SRARoutePredicateFactory.BEFORE).args(ZonedDateTime.now().minusYears(1).toString()).
                cond(SRARoutePredicateCond.OR).build());
        route.getPredicates().add(new SRARoutePredicate.Builder().
                factory(SRARoutePredicateFactory.BETWEEN).args(ZonedDateTime.now().plusYears(1) + ","
                + ZonedDateTime.now().minusYears(1)).cond(SRARoutePredicateCond.OR).build());

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/datetime").exchange().expectStatus().isNotFound();

        route.getPredicates().clear();
        route.getPredicates().add(new SRARoutePredicate.Builder().
                factory(SRARoutePredicateFactory.BEFORE).negate().args(ZonedDateTime.now().minusYears(1).toString()).
                build());

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/datetime").exchange().expectStatus().isOk();
    }

    @Test
    public void header() {
        webClient.get().uri("/header").exchange().expectStatus().isNotFound();

        stubFor(get(urlEqualTo("/header")).willReturn(aResponse()));

        SRARouteTO route = new SRARouteTO();
        route.setKey("header");
        route.setTarget(URI.create("http://localhost:" + wiremockPort));
        route.getPredicates().add(new SRARoutePredicate.Builder().
                factory(SRARoutePredicateFactory.COOKIE).args("Hello,World").build());
        route.getPredicates().add(new SRARoutePredicate.Builder().
                factory(SRARoutePredicateFactory.HOST).args("host").cond(SRARoutePredicateCond.AND).build());
        route.getPredicates().add(new SRARoutePredicate.Builder().
                factory(SRARoutePredicateFactory.HEADER).args("Hello,World").cond(SRARoutePredicateCond.AND).build());
        route.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.PRESERVE_HOST_HEADER).build());

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/header").cookie("Hello", "World").header("Host", "host").header("Hello", "World").
                exchange().expectStatus().isOk();

        webClient.get().uri("/header").cookie("Hello", "Mondo").header("Host", "host").header("Hello", "World").
                exchange().expectStatus().isNotFound();

        webClient.get().uri("/header").cookie("Hello", "World").header("Host", "anotherHost").header("Hello", "World").
                exchange().expectStatus().isNotFound();

        webClient.get().uri("/header").cookie("Hello", "World").header("Host", "host").header("Hello", "Mondo").
                exchange().expectStatus().isNotFound();
    }

    @Test
    public void query() {
        stubFor(get(urlEqualTo("/query?name=value")).willReturn(aResponse()));

        SRARouteTO route = new SRARouteTO();
        route.setKey("query");
        route.setTarget(URI.create("http://localhost:" + wiremockPort));
        route.getPredicates().add(new SRARoutePredicate.Builder().
                factory(SRARoutePredicateFactory.QUERY).args("name,value").build());
        route.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.SAVE_SESSION).build());
        route.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.SET_REQUEST_SIZE).args("5000").build());
        route.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.RETRY).args("3").build());

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/query?name=value").exchange().expectStatus().isOk();

        route.getPredicates().clear();
        route.getPredicates().add(new SRARoutePredicate.Builder().
                factory(SRARoutePredicateFactory.QUERY).args("name,anotherValue").build());

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/query?name=value").exchange().expectStatus().isNotFound();
    }

    @Test
    public void path() {
        stubFor(get(urlEqualTo("/pathMatcher/1")).willReturn(aResponse()));
        stubFor(get(urlEqualTo("/pathMatcher/2")).willReturn(aResponse()));
        stubFor(get(urlEqualTo("/pathMatcher/2/3")).willReturn(aResponse()));

        SRARouteTO route = new SRARouteTO();
        route.setKey("pathMatcher");
        route.setTarget(URI.create("http://localhost:" + wiremockPort));
        route.getPredicates().add(new SRARoutePredicate.Builder().
                factory(SRARoutePredicateFactory.PATH).args("/pathMatcher/**").build());

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/pathMatcher/1").exchange().expectStatus().isOk();
        webClient.get().uri("/pathMatcher/2").exchange().expectStatus().isOk();
        webClient.get().uri("/pathMatcher/2/3").exchange().expectStatus().isOk();
        webClient.get().uri("/pathMatcher/4").exchange().expectStatus().isNotFound();
    }

    @Test
    public void linkRewrite() {
        stubFor(get(urlEqualTo("/linkRewrite")).willReturn(aResponse().
                withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE).
                withBody("<html><head></head><body><a href=\"/absolute\">absolute link</a></body></html>")));

        SRARouteTO route = new SRARouteTO();
        route.setKey("linkRewrite");
        route.setTarget(URI.create("http://localhost:" + wiremockPort));
        route.getFilters().add(new SRARouteFilter.Builder().factory(SRARouteFilterFactory.LINK_REWRITE).
                args("http://localhost:" + sraPort).build());

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/linkRewrite").exchange().
                expectStatus().isOk().
                expectBody().consumeWith(exchange -> {
                    assertTrue(new String(exchange.getResponseBody()).
                            contains("<a href=\"http://localhost:" + sraPort + "/absolute\">"));
                });

        route.getFilters().clear();
        route.getFilters().add(new SRARouteFilter.Builder().factory(SRARouteFilterFactory.LINK_REWRITE).
                args("http://localhost:" + sraPort + ",true").build());

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/linkRewrite").exchange().
                expectStatus().isOk().
                expectBody().consumeWith(exchange -> {
                    assertTrue(new String(exchange.getResponseBody()).
                            contains("<a href=\"http://localhost:" + sraPort + "/absolute\">"));
                });
    }

    @Test
    public void clientCertToRequestHeader() {
        stubFor(get(urlEqualTo("/clientCert")).willReturn(aResponse().
                withHeader(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)));

        SRARouteTO route = new SRARouteTO();
        route.setKey("clientCert");
        route.setTarget(URI.create("http://localhost:" + wiremockPort));
        route.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.CLIENT_CERTS_TO_REQUEST_HEADER).build());

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/clientCert").exchange().
                expectStatus().isOk().
                expectHeader().doesNotExist("X-Client-Certificate");
    }

    @Test
    public void queryParamToRequestHeader() {
        stubFor(get(urlEqualTo("/queryParamToRequestHeader")).
                withHeader("Hello", equalTo("World")).willReturn(aResponse()));

        stubFor(get(urlEqualTo("/queryParamToRequestHeader?Header=Test&Header=Test1")).
                withHeader("Hello", equalTo("World")).willReturn(aResponse()));

        SRARouteTO route = new SRARouteTO();
        route.setKey("queryParamToRequestHeader");
        route.setTarget(URI.create("http://localhost:" + wiremockPort));
        route.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.QUERY_PARAM_TO_REQUEST_HEADER).args("Hello").build());

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/queryParamToRequestHeader").exchange().
                expectStatus().isNotFound();

        webClient.get().uri("/queryParamToRequestHeader?Hello=World").exchange().
                expectStatus().isOk();

        webClient.get().uri("/queryParamToRequestHeader?Header=Test&Hello=World&Header=Test1").exchange().
                expectStatus().isOk();
    }

    @Test
    public void principalToRequestHeader() throws IllegalArgumentException, IllegalAccessException {
        // first mock...
        OidcIdToken oidcIdToken = mock(OidcIdToken.class);
        when(oidcIdToken.getTokenValue()).thenReturn("john.doe");

        OidcUser user = mock(OidcUser.class);
        when(user.getIdToken()).thenReturn(oidcIdToken);

        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(user);

        MapSession session = new MapSession();
        session.setAttribute(
                WebSessionServerSecurityContextRepository.DEFAULT_SPRING_SECURITY_CONTEXT_ATTR_NAME,
                new SecurityContextImpl(authentication));

        Cache cache = mock(Cache.class);
        when(cache.get(anyString(), eq(Session.class))).thenReturn(session);

        CacheManager cacheManager = mock(CacheManager.class);
        when(cacheManager.getCache(eq(SessionConfig.DEFAULT_CACHE))).thenReturn(cache);

        PrincipalToRequestHeaderFilterFactory factory = new PrincipalToRequestHeaderFilterFactory();
        ReflectionTestUtils.setField(factory, "cacheManager", cacheManager);
        ctx.getBeanFactory().registerSingleton(PrincipalToRequestHeaderFilterFactory.class.getName(), factory);

        // ...then test
        stubFor(get(urlEqualTo("/principalToRequestHeader")).willReturn(aResponse()));

        SRARouteTO route = new SRARouteTO();
        route.setKey("principalToRequestHeader");
        route.setTarget(URI.create("http://localhost:" + wiremockPort));
        route.setType(SRARouteType.PROTECTED);
        route.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.PRINCIPAL_TO_REQUEST_HEADER).args("HTTP_REMOTE_USER").build());

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/principalToRequestHeader").exchange().
                expectStatus().isOk();

        verify(getRequestedFor(urlEqualTo("/principalToRequestHeader")).
                withHeader("HTTP_REMOTE_USER", equalTo("john.doe")));
    }

    @Test
    public void custom() {
        stubFor(post(urlEqualTo("/custom")).
                willReturn(aResponse().
                        withHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE).
                        withBody("{\"data\": \"data\"}")));

        SRARouteTO route = new SRARouteTO();
        route.setKey("custom");
        route.setTarget(URI.create("http://localhost:" + wiremockPort));
        route.getPredicates().add(new SRARoutePredicate.Builder().
                factory(SRARoutePredicateFactory.CUSTOM).
                args(BodyPropertyMatchingRoutePredicateFactory.class.getName() + ";cool").build());
        route.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.ADD_RESPONSE_HEADER).args("Custom,matched").build());
        route.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.CUSTOM).
                args(BodyPropertyAddingGatewayFilterFactory.class.getName() + ";customized=true").build());

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.post().uri("/custom").
                body(BodyInserters.fromValue(MAPPER.createObjectNode().put("other", true))).
                exchange().
                expectStatus().isNotFound();

        webClient.post().uri("/custom").
                body(BodyInserters.fromValue(MAPPER.createObjectNode().put("cool", true))).
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
