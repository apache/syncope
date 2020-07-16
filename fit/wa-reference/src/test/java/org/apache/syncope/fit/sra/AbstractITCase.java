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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.ObjectMapper;
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
import javax.ws.rs.core.Response;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.auth.SyncopeAuthModuleConf;
import org.apache.syncope.common.lib.policy.AuthPolicyTO;
import org.apache.syncope.common.lib.policy.DefaultAuthPolicyConf;
import org.apache.syncope.common.lib.to.AuthModuleTO;
import org.apache.syncope.common.lib.to.SRARouteTO;
import org.apache.syncope.common.lib.to.client.OIDCRPTO;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.lib.types.OIDCSubjectType;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.lib.types.SRARouteFilter;
import org.apache.syncope.common.lib.types.SRARouteFilterFactory;
import org.apache.syncope.common.lib.types.SRARoutePredicate;
import org.apache.syncope.common.lib.types.SRARoutePredicateFactory;
import org.apache.syncope.common.lib.types.SRARouteType;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.service.AuthModuleService;
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

    protected static AuthModuleService authModuleService;

    protected static PolicyService policyService;

    protected static ClientAppService clientAppService;

    protected static SRARouteService sraRouteService;

    private static Process SRA;

    @BeforeAll
    public static void restSetup() {
        clientFactory = new SyncopeClientFactoryBean().setAddress(CORE_ADDRESS);
        adminClient = clientFactory.create(ADMIN_UNAME, ADMIN_PWD);

        authModuleService = adminClient.getService(AuthModuleService.class);
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

    protected static void oidcClientAppSetup(
            final String appName,
            final String sraRegistrationId,
            final Long clientAppId,
            final String clientId,
            final String clientSecret) {

        AuthModuleTO syncopeAuthModule = authModuleService.list().stream().
                filter(module -> module.getConf() instanceof SyncopeAuthModuleConf).
                findFirst().orElseThrow(() -> new IllegalArgumentException("Could not find Syncope Auth Module"));

        AuthPolicyTO syncopeAuthPolicy = policyService.list(PolicyType.AUTH).stream().
                map(AuthPolicyTO.class::cast).
                filter(policy -> policy.getConf() instanceof DefaultAuthPolicyConf
                && ((DefaultAuthPolicyConf) policy.getConf()).getAuthModules().contains(syncopeAuthModule.getKey())).
                findFirst().
                orElseGet(() -> {
                    DefaultAuthPolicyConf policyConf = new DefaultAuthPolicyConf();
                    policyConf.getAuthModules().add(syncopeAuthModule.getKey());

                    AuthPolicyTO policy = new AuthPolicyTO();
                    policy.setDescription("Syncope authentication");
                    policy.setConf(policyConf);

                    Response response = policyService.create(PolicyType.AUTH, policy);
                    if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
                        fail("Could not create Syncope Auth Policy");
                    }

                    return policyService.read(PolicyType.AUTH, response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                });

        OIDCRPTO clientApp = clientAppService.list(ClientAppType.OIDCRP).stream().
                filter(app -> appName.equals(app.getName())).
                map(OIDCRPTO.class::cast).
                findFirst().
                orElseGet(() -> {
                    OIDCRPTO app = new OIDCRPTO();
                    app.setName(appName);
                    app.setClientAppId(clientAppId);
                    app.setClientId(clientId);
                    app.setClientSecret(clientSecret);

                    Response response = clientAppService.create(ClientAppType.OIDCRP, app);
                    if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
                        fail("Could not create OIDC Client App");
                    }

                    return clientAppService.read(
                            ClientAppType.OIDCRP, response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                });

        clientApp.setClientId(clientId);
        clientApp.setClientSecret(clientSecret);
        clientApp.setSubjectType(OIDCSubjectType.PUBLIC);
        clientApp.getRedirectUris().add(SRA_ADDRESS + "/login/oauth2/code/" + sraRegistrationId);
        clientApp.setAuthPolicy(syncopeAuthPolicy.getKey());
        clientApp.setSignIdToken(true);
        clientApp.setLogoutUri(SRA_ADDRESS + "/logout");

        clientAppService.update(ClientAppType.OIDCRP, clientApp);
        clientAppService.pushToWA();
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
        assertTrue(WebClient.create(SRA_ADDRESS).get().getStatus() < 400);

        sraRouteService.pushToSRA();
    }

    @AfterAll
    public static void stopSRA() throws InterruptedException {
        if (SRA != null) {
            SRA.destroy();
            SRA.waitFor();
        }
    }
}
