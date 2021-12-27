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
package org.apache.syncope.fit;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.http.Consts;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.rest.api.service.ClientAppService;
import org.apache.syncope.common.rest.api.service.OIDCC4UIProviderService;
import org.apache.syncope.common.rest.api.service.PolicyService;
import org.apache.syncope.common.rest.api.service.SAML2IdPEntityService;
import org.apache.syncope.common.rest.api.service.SAML2SP4UIIdPService;
import org.apache.syncope.common.rest.api.service.SRARouteService;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.syncope.fit.sra.AbstractSRAITCase;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AbstractITCase {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractSRAITCase.class);

    protected static final String ADMIN_UNAME = "admin";

    protected static final String ADMIN_PWD = "password";

    protected static final String CORE_ADDRESS = "http://localhost:9080/syncope/rest";

    protected static final String CONSOLE_ADDRESS = "http://localhost:9080/syncope-console/";

    protected static final String ENDUSER_ADDRESS = "http://localhost:9080/syncope-enduser/";

    protected static final String WA_ADDRESS = "http://localhost:9080/syncope-wa";

    protected static final String EN_LANGUAGE = "en-US,en;q=0.5";

    protected static SyncopeClientFactoryBean clientFactory;

    protected static SyncopeClient adminClient;

    protected static UserService userService;

    protected static PolicyService policyService;

    protected static ClientAppService clientAppService;

    protected static SRARouteService sraRouteService;

    protected static SAML2SP4UIIdPService saml2sp4UIIdPService;

    protected static OIDCC4UIProviderService oidcProviderService;

    @BeforeAll
    public static void restSetup() {
        clientFactory = new SyncopeClientFactoryBean().setAddress(CORE_ADDRESS);
        adminClient = clientFactory.create(ADMIN_UNAME, ADMIN_PWD);

        userService = adminClient.getService(UserService.class);
        policyService = adminClient.getService(PolicyService.class);
        clientAppService = adminClient.getService(ClientAppService.class);
        sraRouteService = adminClient.getService(SRARouteService.class);
        saml2sp4UIIdPService = adminClient.getService(SAML2SP4UIIdPService.class);
        oidcProviderService = adminClient.getService(OIDCC4UIProviderService.class);
    }

    @BeforeAll
    public static void waitForWARefresh() {
        SAML2IdPEntityService samlIdPEntityService = adminClient.getService(SAML2IdPEntityService.class);

        await().atMost(50, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            boolean refreshed = false;
            try {
                samlIdPEntityService.get(SAML2IdPEntityService.DEFAULT_OWNER);
                refreshed = true;
            } catch (Exception e) {
                // ignore
            }
            return refreshed;
        });
    }

    protected static Triple<String, String, String> parseSAMLRequestForm(final String responseBody) {
        int begin = responseBody.indexOf("name=\"RelayState\" value=\"");
        assertNotEquals(-1, begin);
        int end = responseBody.indexOf("\"/>", begin);
        assertNotEquals(-1, end);
        String relayState = StringEscapeUtils.unescapeXml(responseBody.substring(begin + 25, end));
        assertNotNull(relayState);

        begin = responseBody.indexOf("name=\"SAMLRequest\" value=\"");
        assertNotEquals(-1, begin);
        end = responseBody.indexOf("\"/>", begin);
        assertNotEquals(-1, end);
        String samlRequest = responseBody.substring(begin + 26, end);
        assertNotNull(samlRequest);

        begin = responseBody.indexOf("<form action=\"");
        assertNotEquals(-1, begin);
        end = responseBody.indexOf("\" method=\"post\">");
        assertNotEquals(-1, end);
        String action = StringEscapeUtils.unescapeXml(responseBody.substring(begin + 14, end));
        assertNotNull(action);

        return Triple.of(action, relayState, samlRequest);
    }

    protected static Triple<String, String, String> parseSAMLResponseForm(final String responseBody) {
        int begin = responseBody.indexOf("name=\"RelayState\" value=\"");
        assertNotEquals(-1, begin);
        int end = responseBody.indexOf("\"/>");
        assertNotEquals(-1, end);
        String relayState = responseBody.substring(begin + 26, end);
        assertNotNull(relayState);

        begin = responseBody.indexOf("name=\"SAMLResponse\" value=\"");
        assertNotEquals(-1, begin);
        end = responseBody.indexOf("\"/>", begin);
        assertNotEquals(-1, end);
        String samlResponse = responseBody.substring(begin + 27, end);
        assertNotNull(samlResponse);

        begin = responseBody.indexOf("<form action=\"");
        assertNotEquals(-1, begin);
        end = responseBody.indexOf("\" method=\"post\">");
        assertNotEquals(-1, end);
        String action = StringEscapeUtils.unescapeXml(responseBody.substring(begin + 14, end));
        assertNotNull(action);

        return Triple.of(action, relayState, samlResponse);
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
            final String username,
            final String password,
            final String responseBody,
            final CloseableHttpClient httpclient,
            final HttpClientContext context)
            throws IOException {

        List<NameValuePair> form = new ArrayList<>();
        form.add(new BasicNameValuePair("_eventId", "submit"));
        form.add(new BasicNameValuePair("execution", extractCASExecution(responseBody)));
        form.add(new BasicNameValuePair("username", username));
        form.add(new BasicNameValuePair("password", password));
        form.add(new BasicNameValuePair("geolocation", ""));

        HttpPost post = new HttpPost(WA_ADDRESS + "/login");
        post.addHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML);
        post.addHeader(HttpHeaders.ACCEPT_LANGUAGE, EN_LANGUAGE);
        post.setEntity(new UrlEncodedFormEntity(form, Consts.UTF_8));
        return httpclient.execute(post, context);
    }
}
