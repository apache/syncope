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
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;

import com.github.tomakehurst.wiremock.matching.AnythingPattern;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import java.io.IOException;
import java.net.URI;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLException;
import org.apache.syncope.common.lib.to.SRARouteTO;
import org.apache.syncope.common.lib.types.SRARouteFilter;
import org.apache.syncope.common.lib.types.SRARouteFilterFactory;
import org.apache.syncope.common.lib.types.SRARoutePredicate;
import org.apache.syncope.common.lib.types.SRARoutePredicateCond;
import org.apache.syncope.common.lib.types.SRARoutePredicateFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.netty.http.client.HttpClient;

@ActiveProfiles({ "tls" })
@TestPropertySource("classpath:sra-tls.properties")
public class TLSRouteProviderTest extends AbstractTest {

    private WebTestClient webClient() throws SSLException {
        SslContext sslContext = SslContextBuilder.forClient().
                trustManager(InsecureTrustManagerFactory.INSTANCE).
                build();
        return webClient(sslContext);
    }

    private WebTestClient webClient(final SslContext sslContext) throws SSLException {
        HttpClient httpClient = HttpClient.create().
                secure(sslContextSpec -> sslContextSpec.sslContext(sslContext));
        ClientHttpConnector connector = new ReactorClientHttpConnector(httpClient);
        return WebTestClient.bindToServer(connector).baseUrl("https://localhost:" + sraPort).build();
    }

    @BeforeEach
    public void clearRoutes() {
        SyncopeCoreTestingServer.ROUTES.clear();
    }

    @Test
    public void root() throws SSLException {
        webClient().get().exchange().expectStatus().isNotFound();
    }

    @Test
    public void clientAuth() throws SSLException, KeyStoreException, IOException, NoSuchAlgorithmException,
            CertificateException, UnrecoverableKeyException, KeyManagementException {

        KeyStore store = KeyStore.getInstance("PKCS12");
        store.load(getClass().getResourceAsStream("/user.p12"), "password".toCharArray());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        kmf.init(store, "password".toCharArray());

        SslContext sslContext = SslContextBuilder.forClient().
                trustManager(InsecureTrustManagerFactory.INSTANCE).
                keyManager(kmf).
                build();
        WebTestClient webClient = webClient(sslContext);

        stubFor(get(urlEqualTo("/getWithClientAuth")).willReturn(aResponse()));

        SRARouteTO route = new SRARouteTO();
        route.setKey("getWithClientAuth");
        route.setTarget(URI.create("http://localhost:" + wiremockPort));
        route.getPredicates().add(new SRARoutePredicate.Builder().
                factory(SRARoutePredicateFactory.METHOD).args("GET").build());
        route.getPredicates().add(new SRARoutePredicate.Builder().
                factory(SRARoutePredicateFactory.PATH).args("/getWithClientAuth").
                cond(SRARoutePredicateCond.AND).build());
        route.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.CLIENT_CERTS_TO_REQUEST_HEADER).build());

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/getWithClientAuth").exchange().expectStatus().isOk();

        verify(getRequestedFor(urlEqualTo("/getWithClientAuth")).
                withHeader("X-Client-Certificate", new AnythingPattern()));
    }

    @Test
    public void withoutClientCert() throws SSLException, KeyStoreException, IOException, NoSuchAlgorithmException,
            CertificateException, UnrecoverableKeyException, KeyManagementException {

        SslContext sslContext = SslContextBuilder.forClient().
                trustManager(InsecureTrustManagerFactory.INSTANCE).
                build();
        WebTestClient webClient = webClient(sslContext);

        stubFor(get(urlEqualTo("/withoutClientCert")).willReturn(aResponse()));

        SRARouteTO route = new SRARouteTO();
        route.setKey("withoutClientCert");
        route.setTarget(URI.create("http://localhost:" + wiremockPort));
        route.getPredicates().add(new SRARoutePredicate.Builder().
                factory(SRARoutePredicateFactory.METHOD).args("GET").build());
        route.getPredicates().add(new SRARoutePredicate.Builder().
                factory(SRARoutePredicateFactory.PATH).args("/withoutClientCert").cond(SRARoutePredicateCond.AND).
                build());
        route.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.CLIENT_CERTS_TO_REQUEST_HEADER).build());

        SyncopeCoreTestingServer.ROUTES.put(route.getKey(), route);
        routeRefresher.refresh();

        webClient.get().uri("/withoutClientCert").exchange().
                expectStatus().isOk().
                expectHeader().doesNotExist("X-Client-Certificate");
    }
}
