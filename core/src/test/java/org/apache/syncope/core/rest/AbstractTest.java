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

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
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
import org.apache.syncope.common.services.PolicyService;
import org.apache.syncope.common.services.ReportService;
import org.apache.syncope.common.services.ResourceService;
import org.apache.syncope.common.services.RoleService;
import org.apache.syncope.common.services.SchemaService;
import org.apache.syncope.common.services.TaskService;
import org.apache.syncope.common.services.UserRequestService;
import org.apache.syncope.common.services.UserService;
import org.apache.syncope.common.services.WorkflowService;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.validation.SyncopeClientErrorHandler;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJacksonHttpMessageConverter;
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

    protected static final String BASE_URL = "http://localhost:9080/syncope/rest/";

    protected static final String ADMIN_UID = "admin";

    protected static final String ADMIN_PWD = "password";

    public static final String CONTENT_TYPE_JSON = "application/json";
    public static final String CONTENT_TYPE_XML = "text/xml";
    public static final String DEFAULT_CONTENT_TYPE = CONTENT_TYPE_JSON;

    private static final String ENV_KEY_CONTENT_TYPE = "jaxrsContentType";

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private MappingJacksonHttpMessageConverter mappingJacksonHttpMessageConverter;

    @Autowired
    private PreemptiveAuthHttpRequestFactory httpClientFactory;

    @Autowired
    protected JAXRSClientFactoryBean restClientFactory;

    @Autowired
    protected DataSource testDataSource;

    private boolean enabledCXF;

    private String contentType;

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

    protected PolicyService policyService;

    @Before
    public void setup() throws Exception {
        if (!enabledCXF) {
            resetRestTemplate();
        } else {
            setupCXFServices();
        }
    }

    // BEGIN Spring MVC Initialization
    protected void setupRestTemplate(final String uid, final String pwd) {
        PreemptiveAuthHttpRequestFactory requestFactory = ((PreemptiveAuthHttpRequestFactory) restTemplate
                .getRequestFactory());

        ((DefaultHttpClient) requestFactory.getHttpClient()).getCredentialsProvider().setCredentials(
                requestFactory.getAuthScope(), new UsernamePasswordCredentials(uid, pwd));
    }

    protected RestTemplate anonymousRestTemplate() {
        RestTemplate template = new RestTemplate(httpClientFactory);
        List<HttpMessageConverter<?>> converters = new ArrayList<HttpMessageConverter<?>>();
        converters.add(mappingJacksonHttpMessageConverter);
        template.setMessageConverters(converters);
        template.setErrorHandler(new SyncopeClientErrorHandler());
        return template;
    }

    protected void resetRestTemplate() {
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

    // END Spring MVC Initialization

    // BEGIN CXF Initialization
    public void setupCXFServices() throws Exception {
        userService = createServiceInstance(UserService.class);
        roleService = createServiceInstance(RoleService.class);
        resourceService = createServiceInstance(ResourceService.class);
        entitlementService = createServiceInstance(EntitlementService.class);
        configurationService = createServiceInstance(ConfigurationService.class);
        connectorService = createServiceInstance(ConnectorService.class);
        loggerService = createServiceInstance(LoggerService.class);
        reportService = createServiceInstance(ReportService.class);
        taskService = createServiceInstance(TaskService.class);
        policyService = createServiceInstance(PolicyService.class);
        workflowService = createServiceInstance(WorkflowService.class);
        notificationService = createServiceInstance(NotificationService.class);
        schemaService = createServiceInstance(SchemaService.class);
        userRequestService = createServiceInstance(UserRequestService.class);
    }

    protected <T> T createServiceInstance(final Class<T> serviceClass) {
        return createServiceInstance(serviceClass, ADMIN_UID);
    }

    protected <T> T createServiceInstance(final Class<T> serviceClass, final String username) {
        return createServiceInstance(serviceClass, username, ADMIN_PWD);
    }

    protected <T> T createServiceInstance(final Class<T> serviceClass, final String username, final String password) {
        restClientFactory.setUsername(username);
        restClientFactory.setPassword(password);
        restClientFactory.setServiceClass(serviceClass);
        T serviceProxy = restClientFactory.create(serviceClass);
        setupContentType(WebClient.client(serviceProxy));
        return serviceProxy;
    }

    public WebClient createWebClient(final String path) {
        WebClient wc = restClientFactory.createWebClient().to(BASE_URL, false);
        wc.accept(MediaType.APPLICATION_JSON_TYPE).type(MediaType.APPLICATION_JSON_TYPE);
        wc.path(path);
        return wc;
    }

    public void setupContentType(final Client restClient) {
        if (contentType == null) {
            String envContentType = System.getProperty(ENV_KEY_CONTENT_TYPE);
            if ((envContentType != null) && (!envContentType.isEmpty())) {
                contentType = envContentType;
            } else {
                contentType = DEFAULT_CONTENT_TYPE;
            }
        }
        restClient.type(contentType).accept(contentType);
    }

    // END CXF Initialization

    public <T> T getObject(final Response response, final Class<T> type, final Object serviceProxy) {
        assertNotNull(response);
        assertNotNull(response.getLocation());
        if (!enabledCXF) {
            return getObjectSpring(response, type);
        } else {
            return getObjectCXF(response, type, serviceProxy);
        }
    }

    private <T> T getObjectSpring(final Response response, final Class<T> type) {
        return restTemplate.getForEntity(response.getLocation(), type).getBody();
    }

    public void setEnabledCXF(final boolean enabledCXF) {
        this.enabledCXF = enabledCXF;
    }

    public void setContentType(final String contentType) {
        this.contentType = contentType;
    }

    private static <T> T getObjectCXF(final Response response, final Class<T> type, final Object serviceProxy) {
        String location = response.getLocation().toString();
        WebClient webClient = WebClient.fromClient(WebClient.client(serviceProxy));
        webClient.to(location, false);

        return webClient.get(type);
    }

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
    
    protected UserTO createUser(UserTO userTO) {
        Response response = userService.create(userTO);
        return response.readEntity(UserTO.class);
    }
}
