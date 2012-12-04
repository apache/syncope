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
package org.apache.syncope.core.rest;

import javax.sql.DataSource;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.syncope.client.http.PreemptiveAuthHttpRequestFactory;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:restClientContext.xml", "classpath:testJDBCContext.xml" })
public abstract class AbstractTest {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractTest.class);

    protected static final String BASE_URL = "http://localhost:9081/syncope/rest/";

    public static final String ADMIN_UID = "admin";

    public static final String ADMIN_PWD = "password";

    @Autowired
    protected RestTemplate restTemplate;

    @Autowired
    protected JAXRSClientFactoryBean restClientFactory;

    @Autowired
    protected DataSource testDataSource;

    @Before
    public abstract void setupService();

    protected RestTemplate anonymousRestTemplate() {
        return new RestTemplate();
    }

    public void setupRestTemplate(final String uid, final String pwd) {
        PreemptiveAuthHttpRequestFactory requestFactory = ((PreemptiveAuthHttpRequestFactory) restTemplate
                .getRequestFactory());

        ((DefaultHttpClient) requestFactory.getHttpClient()).getCredentialsProvider().setCredentials(
                requestFactory.getAuthScope(), new UsernamePasswordCredentials(uid, pwd));
    }

    @Before
    public void resetRestTemplate() {
        setupRestTemplate(ADMIN_UID, ADMIN_PWD);
        restClientFactory.setUsername(ADMIN_UID);
    }

    public void setupXML(Client restClient) {
        restClient.type("application/xml").accept("application/xml");
    }

    public void setupJSON(Client restClient) {
        restClient.type("application/json").accept("application/json");
    }

    protected <T> T createServiceInstance(Class<T> serviceClass) {
        return createServiceInstance(serviceClass, ADMIN_UID);
    }

    protected <T> T createServiceInstance(Class<T> serviceClass, String username) {
        return createServiceInstance(serviceClass, username, null);
    }

    protected <T> T createServiceInstance(Class<T> serviceClass, String username, Object proxy) {
        restClientFactory.setUsername(username);
        restClientFactory.setServiceClass(serviceClass);
        T user2RoleService = restClientFactory.create(serviceClass);
        if (proxy != null) {
            String type = WebClient.client(proxy).getHeaders().getFirst("Content-Type");
            String accept = WebClient.client(proxy).getHeaders().getFirst("Accept");
            WebClient.client(user2RoleService).type(type).accept(accept);
        }
        return user2RoleService;
    }

    public WebClient createWebClient(String path) {
        WebClient wc = restClientFactory.createWebClient().to(BASE_URL, false);
        wc.accept(MediaType.APPLICATION_JSON_TYPE).type(MediaType.APPLICATION_JSON_TYPE);
        wc.path(path);
        return wc;
    }
}
