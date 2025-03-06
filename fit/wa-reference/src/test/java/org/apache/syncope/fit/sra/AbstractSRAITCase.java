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
package org.apache.syncope.fit.sra;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.oneOf;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.http.HttpStatus;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.apache.syncope.common.lib.policy.AuthPolicyTO;
import org.apache.syncope.common.lib.policy.DefaultAuthPolicyConf;
import org.apache.syncope.common.lib.to.SRARouteTO;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.lib.types.SRARouteFilter;
import org.apache.syncope.common.lib.types.SRARouteFilterFactory;
import org.apache.syncope.common.lib.types.SRARoutePredicate;
import org.apache.syncope.common.lib.types.SRARoutePredicateFactory;
import org.apache.syncope.common.lib.types.SRARouteType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.fit.AbstractITCase;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;

abstract class AbstractSRAITCase extends AbstractITCase {

    protected static final String SRA_ADDRESS = "http://127.0.0.1:8080";

    protected static final String QUERY_STRING =
            "key1=value1&key2=value2&key2=value3&key3=an%20url%20encoded%20value%3A%20this%21";

    protected static final String LOGGED_OUT_HEADER = "X-LOGGED-OUT";

    private static Process SRA;

    @BeforeAll
    public static void sraRouteSetup() {
        SRA_ROUTE_SERVICE.list().forEach(route -> SRA_ROUTE_SERVICE.delete(route.getKey()));

        SRARouteTO publicRoute = new SRARouteTO();
        publicRoute.setName("public");
        publicRoute.setTarget(URI.create("http://localhost:80"));
        publicRoute.setType(SRARouteType.PUBLIC);
        publicRoute.setCsrf(false);
        publicRoute.getPredicates().add(new SRARoutePredicate.Builder().
                factory(SRARoutePredicateFactory.PATH).args("/public/{segment}").build());
        publicRoute.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.SET_PATH).args("/{segment}").build());

        Response response = SRA_ROUTE_SERVICE.create(publicRoute);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            fail("Could not create public SRA Route");
        }

        SRARouteTO protectedRoute = new SRARouteTO();
        protectedRoute.setName("protected");
        protectedRoute.setTarget(URI.create("http://localhost:80"));
        protectedRoute.setType(SRARouteType.PROTECTED);
        protectedRoute.setCsrf(false);
        protectedRoute.getPredicates().add(new SRARoutePredicate.Builder().
                factory(SRARoutePredicateFactory.PATH).args("/protected/{segment}").build());
        protectedRoute.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.SET_PATH).args("/{segment}").build());

        response = SRA_ROUTE_SERVICE.create(protectedRoute);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            fail("Could not create protected SRA Route");
        }

        SRARouteTO logoutRoute = new SRARouteTO();
        logoutRoute.setName("logout");
        logoutRoute.setTarget(URI.create("http://localhost:80"));
        logoutRoute.setType(SRARouteType.PROTECTED);
        logoutRoute.setLogout(true);
        logoutRoute.getPredicates().add(new SRARoutePredicate.Builder().
                factory(SRARoutePredicateFactory.PATH).args("/protected/logout").build());
        logoutRoute.setOrder(-1);

        response = SRA_ROUTE_SERVICE.create(logoutRoute);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            fail("Could not create logout SRA Route");
        }

        SRARouteTO postLogout = new SRARouteTO();
        postLogout.setName("postLogout");
        postLogout.setTarget(URI.create("http://localhost:80"));
        postLogout.setType(SRARouteType.PUBLIC);
        postLogout.getPredicates().add(new SRARoutePredicate.Builder().
                factory(SRARoutePredicateFactory.PATH).args("/logout").build());
        postLogout.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.SET_STATUS).args("204").build());
        postLogout.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.SET_RESPONSE_HEADER).args(LOGGED_OUT_HEADER + ", true").build());
        postLogout.setOrder(-10);

        response = SRA_ROUTE_SERVICE.create(postLogout);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            fail("Could not create logout SRA Route");
        }
    }

    protected static void doStartSRA(final String activeProfile)
            throws IOException, InterruptedException, TimeoutException {

        Properties props = new Properties();
        try (InputStream propStream = AbstractSRAITCase.class.getResourceAsStream("/test.properties")) {
            props.load(propStream);
        } catch (Exception e) {
            fail("Could not load /test.properties", e);
        }

        String javaHome = props.getProperty("java.home");
        assertNotNull(javaHome);

        String sraJar = props.getProperty("sra.jar");
        assertNotNull(sraJar);

        String keymasterApiJar = props.getProperty("keymaster-api.jar");
        assertNotNull(keymasterApiJar);

        String keymasterClientJar = props.getProperty("keymaster-client.jar");
        assertNotNull(keymasterClientJar);

        String trustStore = props.getProperty("trustStore");
        assertNotNull(trustStore);
        String trustStorePassword = props.getProperty("trustStorePassword");
        assertNotNull(trustStorePassword);

        String targetTestClasses = props.getProperty("targetTestClasses");
        assertNotNull(targetTestClasses);

        ProcessBuilder processBuilder = new ProcessBuilder(
                javaHome + "/bin/java",
                "-Dreactor.netty.http.server.accessLogEnabled=true",
                "-Djavax.net.ssl.trustStore=" + trustStore,
                "-Djavax.net.ssl.trustStorePassword=" + trustStorePassword,
                "-jar",
                "-Xdebug",
                "-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=8002",
                sraJar);
        processBuilder.inheritIO();

        Map<String, String> environment = processBuilder.environment();
        environment.put("LOADER_PATH", targetTestClasses + "," + keymasterApiJar + "," + keymasterClientJar);
        environment.put("SPRING_PROFILES_ACTIVE", activeProfile);

        SRA = processBuilder.start();

        await().atMost(120, TimeUnit.SECONDS).pollInterval(3, TimeUnit.SECONDS).until(() -> {
            boolean connected = false;
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("0.0.0.0", 8080));
                connected = socket.isConnected();
            } catch (ConnectException e) {
                // ignore
            }
            return connected;
        });
        assertDoesNotThrow(() -> WebClient.create(SRA_ADDRESS).get().getStatus());

        SRA_ROUTE_SERVICE.pushToSRA();
    }

    @AfterAll
    public static void stopSRA() throws InterruptedException {
        if (SRA != null) {
            SRA.destroy();
            SRA.waitFor();
        }
    }

    protected static AuthPolicyTO getAuthPolicy() {
        String authModule = "DefaultSyncopeAuthModule";
        String description = "SRA auth policy";

        return POLICY_SERVICE.list(PolicyType.AUTH).stream().
                map(AuthPolicyTO.class::cast).
                filter(policy -> description.equals(policy.getName())
                && policy.getConf() instanceof DefaultAuthPolicyConf
                && ((DefaultAuthPolicyConf) policy.getConf()).getAuthModules().contains(authModule)).
                findFirst().
                orElseGet(() -> {
                    DefaultAuthPolicyConf policyConf = new DefaultAuthPolicyConf();
                    policyConf.getAuthModules().add(authModule);

                    AuthPolicyTO policy = new AuthPolicyTO();
                    policy.setName(description);
                    policy.setConf(policyConf);

                    Response response = POLICY_SERVICE.create(PolicyType.AUTH, policy);
                    if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
                        fail(() -> "Could not create " + description);
                    }

                    return POLICY_SERVICE.read(PolicyType.AUTH, response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                });
    }

    protected static ObjectNode checkGetResponse(
            final CloseableHttpResponse response, final String originalRequestURI) throws IOException {

        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        assertEquals(MediaType.APPLICATION_JSON, response.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue());

        JsonNode json = MAPPER.readTree(EntityUtils.toString(response.getEntity()));

        ObjectNode args = (ObjectNode) json.get("args");
        assertEquals("value1", args.get("key1").asText());

        ArrayNode key2 = (ArrayNode) args.get("key2");
        assertEquals("value2", key2.get(0).asText());
        assertEquals("value3", key2.get(1).asText());

        assertEquals("an url encoded value: this!", args.get("key3").asText());

        ObjectNode headers = (ObjectNode) json.get("headers");
        assertEquals(MediaType.TEXT_HTML, headers.get(HttpHeaders.ACCEPT).asText());
        assertThat(headers.get("X-Forwarded-Host").asText(), is(oneOf("localhost:8080", "127.0.0.1:8080")));

        String withHost = StringUtils.substringBefore(originalRequestURI, "?");
        String withIP = withHost.replace("localhost", "127.0.0.1");
        assertThat(StringUtils.substringBefore(json.get("url").asText(), "?"), is(oneOf(withHost, withIP)));

        return headers;
    }

    protected void checkLogout(final CloseableHttpResponse response) throws IOException {
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusLine().getStatusCode());
        assertEquals("true", response.getFirstHeader(LOGGED_OUT_HEADER).getValue());
    }
}
