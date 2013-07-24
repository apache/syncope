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
import static org.junit.Assert.fail;

import java.util.UUID;

import javax.sql.DataSource;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;

import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.http.HttpStatus;
import org.apache.syncope.client.rest.RestClientExceptionMapper;
import org.apache.syncope.client.rest.RestClientFactoryBean;
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
import org.apache.syncope.common.services.UserWorkflowService;
import org.apache.syncope.common.services.WorkflowService;
import org.apache.syncope.common.to.AbstractSchemaTO;
import org.apache.syncope.common.to.AttributeTO;
import org.apache.syncope.common.to.AbstractPolicyTO;
import org.apache.syncope.common.to.ResourceTO;
import org.apache.syncope.common.to.RoleTO;
import org.apache.syncope.common.to.UserTO;
import org.apache.syncope.common.types.AttributableType;
import org.apache.syncope.common.types.PolicyType;
import org.apache.syncope.common.types.SchemaType;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:restClientContext.xml", "classpath:testJDBCContext.xml"})
public abstract class AbstractTest {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractTest.class);

    protected static final String ADMIN_UNAME = "admin";

    protected static final String ADMIN_PWD = "password";

    private static final String ENV_KEY_CONTENT_TYPE = "jaxrsContentType";

    @Autowired
    protected RestClientFactoryBean restClientFactory;

    @Autowired
    protected DataSource testDataSource;

    protected UserService userService;

    protected UserWorkflowService userWorkflowService;

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

    @Autowired
    protected RestClientExceptionMapper clientExceptionMapper;

    @Before
    public void setup() throws Exception {
        final String envContentType = System.getProperty(ENV_KEY_CONTENT_TYPE);
        if (StringUtils.isNotBlank(envContentType)) {
            restClientFactory.setContentType(envContentType);
        }
        LOG.info("Performing IT with content type {}", restClientFactory.getContentType());

        userService = createServiceInstance(UserService.class);
        userWorkflowService = createServiceInstance(UserWorkflowService.class);
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
        return restClientFactory.createServiceInstance(serviceClass, ADMIN_UNAME, ADMIN_PWD);
    }

    protected <T> T createServiceInstance(final Class<T> serviceClass, final String username, final String password) {
        return restClientFactory.createServiceInstance(serviceClass, username, password);
    }

    protected <T> T getObject(final Response response, final Class<T> type, final Object serviceProxy) {
        assertNotNull(response);
        assertNotNull(response.getLocation());

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

    protected void assertCreated(final Response response) {
        if (response.getStatus() != HttpStatus.SC_CREATED) {
            StringBuilder builder = new StringBuilder();
            MultivaluedMap<String, Object> headers = response.getHeaders();
            builder.append("Headers (");
            for (String key : headers.keySet()) {
                builder.append(key).append(':').append(headers.getFirst(key)).append(',');
            }
            builder.append(")");
            fail("Error on create. Status is : " + response.getStatus() + " with headers "
                    + builder.toString());
        }
    }

    protected UserTO createUser(final UserTO userTO) {
        Response response = userService.create(userTO);
        if (response.getStatus() != HttpStatus.SC_CREATED) {
            Exception ex = clientExceptionMapper.fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return response.readEntity(UserTO.class);
    }

    @SuppressWarnings("unchecked")
    protected <T extends AbstractSchemaTO> T createSchema(final AttributableType kind,
            final SchemaType type, final T schemaTO) {

        Response response = schemaService.create(kind, type, schemaTO);
        if (response.getStatus() != HttpStatus.SC_CREATED) {
            Exception ex = clientExceptionMapper.fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }

        return (T) getObject(response, schemaTO.getClass(), schemaService);
    }

    protected RoleTO createRole(final RoleService roleService, final RoleTO newRoleTO) {
        Response response = roleService.create(newRoleTO);
        if (response.getStatus() != org.apache.http.HttpStatus.SC_CREATED) {
            Exception ex = clientExceptionMapper.fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return getObject(response, RoleTO.class, roleService);
    }

    @SuppressWarnings("unchecked")
    protected <T extends AbstractPolicyTO> T createPolicy(final PolicyType policyType, final T policy) {
        Response response = policyService.create(policyType, policy);
        if (response.getStatus() != org.apache.http.HttpStatus.SC_CREATED) {
            Exception ex = clientExceptionMapper.fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return (T) getObject(response, policy.getClass(), policyService);
    }

    protected ResourceTO createResource(final ResourceTO resourceTO) {
        Response response = resourceService.create(resourceTO);
        if (response.getStatus() != org.apache.http.HttpStatus.SC_CREATED) {
            Exception ex = clientExceptionMapper.fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return getObject(response, ResourceTO.class, resourceService);
    }
}
