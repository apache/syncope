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

import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.syncope.client.http.PreemptiveAuthHttpRequestFactory;
import org.apache.syncope.client.mod.AttributeMod;
import org.apache.syncope.client.to.AttributeTO;
import org.apache.syncope.services.proxy.ConfigurationServiceProxy;
import org.apache.syncope.services.proxy.ConnectorServiceProxy;
import org.apache.syncope.services.proxy.EntitlementServiceProxy;
import org.apache.syncope.services.proxy.LoggerServiceProxy;
import org.apache.syncope.services.proxy.NotificationServiceProxy;
import org.apache.syncope.services.proxy.PolicyServiceProxy;
import org.apache.syncope.services.proxy.ReportServiceProxy;
import org.apache.syncope.services.proxy.ResourceServiceProxy;
import org.apache.syncope.services.proxy.RoleServiceProxy;
import org.apache.syncope.services.proxy.SchemaServiceProxy;
import org.apache.syncope.services.proxy.TaskServiceProxy;
import org.apache.syncope.services.proxy.UserServiceProxy;
import org.apache.syncope.services.proxy.WorkflowServiceProxy;
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

    protected static AttributeTO attributeTO(final String schema, final String value) {
        AttributeTO attr = new AttributeTO();
        attr.setSchema(schema);
        attr.addValue(value);
        return attr;
    }

    protected static AttributeMod attributeMod(final String schema, final String valueToBeAdded) {
        AttributeMod attr = new AttributeMod();
        attr.setSchema(schema);
        attr.addValueToBeAdded(valueToBeAdded);
        return attr;
    }

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractTest.class);

    protected static final String BASE_URL = "http://localhost:9080/syncope/rest/";

    public static final String ADMIN_UID = "admin";

    public static final String ADMIN_PWD = "password";

    protected PolicyServiceProxy policyService;

    @Autowired
    protected RestTemplate restTemplate;

    protected UserServiceProxy userService;

    protected RoleServiceProxy roleService;

    protected ResourceServiceProxy resourceService;

    protected EntitlementServiceProxy entitlementService;

    protected ConfigurationServiceProxy configurationService;

    protected ConnectorServiceProxy connectorService;

    protected LoggerServiceProxy loggerService;

    protected ReportServiceProxy reportService;

    protected TaskServiceProxy taskService;

    protected WorkflowServiceProxy workflowService;

    protected NotificationServiceProxy notificationService;

    protected SchemaServiceProxy schemaService;

    @Autowired
    protected DataSource testDataSource;

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
        userService = new UserServiceProxy(BASE_URL, restTemplate);
        roleService = new RoleServiceProxy(BASE_URL, restTemplate);
        resourceService = new ResourceServiceProxy(BASE_URL, restTemplate);
        entitlementService = new EntitlementServiceProxy(BASE_URL, restTemplate);
        configurationService = new ConfigurationServiceProxy(BASE_URL, restTemplate);
        connectorService = new ConnectorServiceProxy(BASE_URL, restTemplate);
        loggerService = new LoggerServiceProxy(BASE_URL, restTemplate);
        reportService = new ReportServiceProxy(BASE_URL, restTemplate);
        taskService = new TaskServiceProxy(BASE_URL, restTemplate);
        policyService = new PolicyServiceProxy(BASE_URL, restTemplate);
        workflowService = new WorkflowServiceProxy(BASE_URL, restTemplate);
        notificationService = new NotificationServiceProxy(BASE_URL, restTemplate);
        schemaService = new SchemaServiceProxy(BASE_URL, restTemplate);
    }
}
