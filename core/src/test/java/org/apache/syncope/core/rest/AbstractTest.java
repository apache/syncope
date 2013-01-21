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

import java.util.UUID;
import javax.sql.DataSource;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.syncope.client.http.PreemptiveAuthHttpRequestFactory;
import org.apache.syncope.client.services.proxy.ConfigurationServiceProxy;
import org.apache.syncope.client.services.proxy.ConnectorServiceProxy;
import org.apache.syncope.client.services.proxy.EntitlementServiceProxy;
import org.apache.syncope.client.services.proxy.LoggerServiceProxy;
import org.apache.syncope.client.services.proxy.NotificationServiceProxy;
import org.apache.syncope.client.services.proxy.PolicyServiceProxy;
import org.apache.syncope.client.services.proxy.ReportServiceProxy;
import org.apache.syncope.client.services.proxy.ResourceServiceProxy;
import org.apache.syncope.client.services.proxy.RoleServiceProxy;
import org.apache.syncope.client.services.proxy.SchemaServiceProxy;
import org.apache.syncope.client.services.proxy.TaskServiceProxy;
import org.apache.syncope.client.services.proxy.UserRequestServiceProxy;
import org.apache.syncope.client.services.proxy.UserServiceProxy;
import org.apache.syncope.client.services.proxy.WorkflowServiceProxy;
import org.apache.syncope.common.mod.AttributeMod;
import org.apache.syncope.common.services.ConfigurationService;
import org.apache.syncope.common.services.ConnectorService;
import org.apache.syncope.common.services.EntitlementService;
import org.apache.syncope.common.services.LoggerService;
import org.apache.syncope.common.services.NotificationService;
import org.apache.syncope.common.services.ReportService;
import org.apache.syncope.common.services.ResourceService;
import org.apache.syncope.common.services.RoleService;
import org.apache.syncope.common.services.SchemaService;
import org.apache.syncope.common.services.TaskService;
import org.apache.syncope.common.services.UserRequestService;
import org.apache.syncope.common.services.UserService;
import org.apache.syncope.common.services.WorkflowService;
import org.apache.syncope.common.to.AttributeTO;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.web.client.RestTemplate;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:restClientContext.xml", "classpath:testJDBCContext.xml"})
public abstract class AbstractTest {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractTest.class);

    protected static final String BASE_URL = "http://localhost:9080/syncope/rest/";

    protected static final String ADMIN_UID = "admin";

    protected static final String ADMIN_PWD = "password";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    protected DataSource testDataSource;

    protected UserService userService;

    protected RoleService roleService;

    protected ResourceService resourceService;

    protected EntitlementService entitlementService;

    protected ConfigurationService configurationService;

    protected ConnectorService connectorService;

    protected LoggerService loggerService;

    protected ReportService reportService;

    protected TaskService taskService;

    protected WorkflowService workflowService;

    protected NotificationService notificationService;

    protected SchemaService schemaService;

    protected UserRequestService userRequestService;

    protected PolicyServiceProxy policyService;

    protected static String getUUIDString() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

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

    protected void setupRestTemplate(final String uid, final String pwd) {
        PreemptiveAuthHttpRequestFactory requestFactory = ((PreemptiveAuthHttpRequestFactory) restTemplate
                .getRequestFactory());

        ((DefaultHttpClient) requestFactory.getHttpClient()).getCredentialsProvider().setCredentials(
                requestFactory.getAuthScope(), new UsernamePasswordCredentials(uid, pwd));
    }

    protected RestTemplate anonymousRestTemplate() {
        return new RestTemplate();
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
        userRequestService = new UserRequestServiceProxy(BASE_URL, restTemplate);
    }
}
