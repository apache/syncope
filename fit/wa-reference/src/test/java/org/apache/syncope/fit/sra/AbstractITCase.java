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
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.io.IOException;
import java.io.InputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.http.Consts;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
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
import org.apache.syncope.common.rest.api.service.ClientAppService;
import org.apache.syncope.common.rest.api.service.PolicyService;
import org.apache.syncope.common.rest.api.service.SRARouteService;
import org.apache.syncope.common.rest.api.service.wa.WASAML2IdPMetadataService;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractITCase {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractITCase.class);

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    protected static final String EN_LANGUAGE = "en-US,en;q=0.5";

    protected static final int PORT = 8080;

    protected static final String ADMIN_UNAME = "admin";

    protected static final String ADMIN_PWD = "password";

    protected static final String CORE_ADDRESS = "http://localhost:9080/syncope/rest";

    protected static final String WA_ADDRESS = "http://localhost:9080/syncope-wa";

    protected static final String SRA_ADDRESS = "http://localhost:" + PORT;

    protected static final String LOGGED_OUT_HEADER = "X-LOGGED-OUT";

    protected static SyncopeClientFactoryBean clientFactory;

    protected static SyncopeClient adminClient;

    protected static PolicyService policyService;

    protected static ClientAppService clientAppService;

    protected static SRARouteService sraRouteService;

    private static Process SRA;

    @BeforeAll
    public static void restSetup() {
        clientFactory = new SyncopeClientFactoryBean().setAddress(CORE_ADDRESS);
        adminClient = clientFactory.create(ADMIN_UNAME, ADMIN_PWD);

        policyService = adminClient.getService(PolicyService.class);
        clientAppService = adminClient.getService(ClientAppService.class);
        sraRouteService = adminClient.getService(SRARouteService.class);
    }

    @BeforeAll
    public static void waitForWARefresh() {
        WASAML2IdPMetadataService samlIdPMetadataService = adminClient.getService(WASAML2IdPMetadataService.class);

        await().atMost(50, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            boolean refreshed = false;
            try {
                samlIdPMetadataService.getByOwner("Syncope");
                refreshed = true;
            } catch (Exception e) {
                // ignore
            }
            return refreshed;
        });
    }

    @BeforeAll
    public static void sraRouteSetup() {
        sraRouteService.list().forEach(route -> sraRouteService.delete(route.getKey()));

        SRARouteTO publicRoute = new SRARouteTO();
        publicRoute.setName("public");
        publicRoute.setTarget(URI.create("http://httpbin.org:80"));
        publicRoute.setType(SRARouteType.PUBLIC);
        publicRoute.setCsrf(false);
        publicRoute.getPredicates().add(new SRARoutePredicate.Builder().
                factory(SRARoutePredicateFactory.PATH).args("/public/{segment}").build());
        publicRoute.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.SET_PATH).args("/{segment}").build());

        Response response = sraRouteService.create(publicRoute);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            fail("Could not create public SRA Route");
        }

        SRARouteTO protectedRoute = new SRARouteTO();
        protectedRoute.setName("protected");
        protectedRoute.setTarget(URI.create("http://httpbin.org:80"));
        protectedRoute.setType(SRARouteType.PROTECTED);
        protectedRoute.setCsrf(false);
        protectedRoute.getPredicates().add(new SRARoutePredicate.Builder().
                factory(SRARoutePredicateFactory.PATH).args("/protected/{segment}").build());
        protectedRoute.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.SET_PATH).args("/{segment}").build());

        response = sraRouteService.create(protectedRoute);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            fail("Could not create protected SRA Route");
        }

        SRARouteTO logoutRoute = new SRARouteTO();
        logoutRoute.setName("logout");
        logoutRoute.setTarget(URI.create("http://httpbin.org:80"));
        logoutRoute.setType(SRARouteType.PROTECTED);
        logoutRoute.setLogout(true);
        logoutRoute.getPredicates().add(new SRARoutePredicate.Builder().
                factory(SRARoutePredicateFactory.PATH).args("/protected/logout").build());
        logoutRoute.setOrder(-1);

        response = sraRouteService.create(logoutRoute);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            fail("Could not create logout SRA Route");
        }

        SRARouteTO postLogout = new SRARouteTO();
        postLogout.setName("postLogout");
        postLogout.setTarget(URI.create("http://httpbin.org:80"));
        postLogout.setType(SRARouteType.PUBLIC);
        postLogout.getPredicates().add(new SRARoutePredicate.Builder().
                factory(SRARoutePredicateFactory.PATH).args("/logout").build());
        postLogout.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.SET_STATUS).args("204").build());
        postLogout.getFilters().add(new SRARouteFilter.Builder().
                factory(SRARouteFilterFactory.SET_RESPONSE_HEADER).args(LOGGED_OUT_HEADER + ", true").build());
        postLogout.setOrder(-10);

        response = sraRouteService.create(postLogout);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            fail("Could not create logout SRA Route");
        }
    }

    protected static void doStartSRA(final String activeProfile)
            throws IOException, InterruptedException, TimeoutException {

        Properties props = new Properties();
        try (InputStream propStream = AbstractITCase.class.getResourceAsStream("/test.properties")) {
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

        String targetTestClasses = props.getProperty("targetTestClasses");
        assertNotNull(targetTestClasses);

        ProcessBuilder processBuilder = new ProcessBuilder(
                javaHome + "/bin/java",
                "-Dreactor.netty.http.server.accessLogEnabled=true",
                "-jar", sraJar);
        processBuilder.inheritIO();

        Map<String, String> environment = processBuilder.environment();
        environment.put("LOADER_PATH", targetTestClasses + "," + keymasterApiJar + "," + keymasterClientJar);
        environment.put("SPRING_PROFILES_ACTIVE", activeProfile);

        SRA = processBuilder.start();

        await().atMost(30, TimeUnit.SECONDS).pollInterval(3, TimeUnit.SECONDS).until(() -> {
            boolean connected = false;
            try (Socket socket = new Socket()) {
                socket.connect(new InetSocketAddress("0.0.0.0", PORT));
                connected = socket.isConnected();
            } catch (ConnectException e) {
                // ignore
            }
            return connected;
        });
        assertDoesNotThrow(() -> WebClient.create(SRA_ADDRESS).get().getStatus());

        sraRouteService.pushToSRA();
    }

    protected static AuthPolicyTO getAuthPolicy() {
        String authModule = "DefaultSyncopeAuthModule";

        return policyService.list(PolicyType.AUTH).stream().
                map(AuthPolicyTO.class::cast).
                filter(policy -> policy.getConf() instanceof DefaultAuthPolicyConf
                && ((DefaultAuthPolicyConf) policy.getConf()).getAuthModules().contains(authModule)).
                findFirst().
                orElseGet(() -> {
                    DefaultAuthPolicyConf policyConf = new DefaultAuthPolicyConf();
                    policyConf.getAuthModules().add(authModule);

                    AuthPolicyTO policy = new AuthPolicyTO();
                    policy.setDescription("Syncope authentication");
                    policy.setConf(policyConf);

                    Response response = policyService.create(PolicyType.AUTH, policy);
                    if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
                        fail("Could not create Syncope Auth Policy");
                    }

                    return policyService.read(PolicyType.AUTH, response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                });
    }

    @AfterAll
    public static void stopSRA() throws InterruptedException {
        if (SRA != null) {
            SRA.destroy();
            SRA.waitFor();
        }
    }

    protected static String extractCASExecution(final String responseBody) {
        int begin = responseBody.indexOf("name=\"execution\" value=\"");
        assertNotEquals(-1, begin);
        int end = responseBody.indexOf("\"/><input type=\"hidden\" name=\"_eventId\"");
        assertNotEquals(-1, end);

        String execution = responseBody.substring(begin + 24, end);
        assertNotNull(execution);
        return execution;
    }

    protected static CloseableHttpResponse authenticateToCas(
            final String responseBody, final CloseableHttpClient httpclient, final HttpClientContext context)
            throws IOException {

        List<NameValuePair> form = new ArrayList<>();
        form.add(new BasicNameValuePair("_eventId", "submit"));
        form.add(new BasicNameValuePair("execution", extractCASExecution(responseBody)));
        form.add(new BasicNameValuePair("username", "bellini"));
        form.add(new BasicNameValuePair("password", "password"));
        form.add(new BasicNameValuePair("geolocation", ""));

        HttpPost post = new HttpPost(WA_ADDRESS + "/login");
        post.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML);
        post.addHeader(HttpHeaders.ACCEPT_LANGUAGE, EN_LANGUAGE);
        post.setEntity(new UrlEncodedFormEntity(form, Consts.UTF_8));
        return httpclient.execute(post, context);
    }

    protected static ObjectNode checkGetResponse(
            final CloseableHttpResponse response, final String originalRequestURI) throws IOException {

        assertEquals(HttpStatus.SC_OK, response.getStatusLine().getStatusCode());

        assertEquals(MediaType.APPLICATION_JSON, response.getFirstHeader(HttpHeaders.CONTENT_TYPE).getValue());

        JsonNode json = OBJECT_MAPPER.readTree(EntityUtils.toString(response.getEntity()));

        ObjectNode args = (ObjectNode) json.get("args");
        assertEquals("value1", args.get("key1").asText());

        ArrayNode key2 = (ArrayNode) args.get("key2");
        assertEquals("value2", key2.get(0).asText());
        assertEquals("value3", key2.get(1).asText());

        ObjectNode headers = (ObjectNode) json.get("headers");
        assertEquals(MediaType.TEXT_HTML, headers.get(HttpHeaders.ACCEPT).asText());
        assertEquals(EN_LANGUAGE, headers.get(HttpHeaders.ACCEPT_LANGUAGE).asText());
        assertEquals("localhost:" + PORT, headers.get("X-Forwarded-Host").asText());

        assertEquals(originalRequestURI, json.get("url").asText());

        return headers;
    }

    protected void checkLogout(final CloseableHttpResponse response) throws IOException {
        assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatusLine().getStatusCode());
        assertEquals("true", response.getFirstHeader(LOGGED_OUT_HEADER).getValue());
    }
}
