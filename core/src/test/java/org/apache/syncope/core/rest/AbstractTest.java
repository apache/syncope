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

import java.io.InputStream;
import java.util.Properties;
import java.util.UUID;

import javax.sql.DataSource;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import org.apache.http.HttpStatus;
import org.apache.syncope.client.SyncopeClient;
import org.apache.syncope.client.SyncopeClientFactoryBean;
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
import org.apache.syncope.common.types.SchemaType;
import org.apache.syncope.core.util.PasswordEncoder;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath:testJDBCContext.xml"})
public abstract class AbstractTest {

    /**
     * Logger.
     */
    protected static final Logger LOG = LoggerFactory.getLogger(AbstractTest.class);

    protected static final String ADMIN_UNAME = "admin";

    protected static final String ADMIN_PWD = "password";

    private static final String ADDRESS = "http://localhost:9080/syncope/rest";

    private static final String ENV_KEY_CONTENT_TYPE = "jaxrsContentType";

    protected static final SyncopeClientFactoryBean clientFactory = new SyncopeClientFactoryBean().setAddress(ADDRESS);

    protected static String ANONYMOUS_UNAME;

    protected static String ANONYMOUS_KEY;

    protected static SyncopeClient adminClient;

    protected static UserService userService;

    protected static UserWorkflowService userWorkflowService;

    protected static RoleService roleService;

    protected static ResourceService resourceService;

    protected static EntitlementService entitlementService;

    protected static ConfigurationService configurationService;

    protected static ConnectorService connectorService;

    protected static LoggerService loggerService;

    protected static ReportService reportService;

    protected static TaskService taskService;

    protected static WorkflowService workflowService;

    protected static NotificationService notificationService;

    protected static SchemaService schemaService;

    protected static UserRequestService userRequestService;

    protected static PolicyService policyService;

    @Autowired
    protected DataSource testDataSource;

    @BeforeClass
    public static void securitySetup() {
        InputStream propStream = null;
        try {
            propStream = PasswordEncoder.class.getResourceAsStream("/security.properties");
            Properties props = new Properties();
            props.load(propStream);

            ANONYMOUS_UNAME = props.getProperty("anonymousUser");
            ANONYMOUS_KEY = props.getProperty("anonymousKey");
        } catch (Exception e) {
            LOG.error("Could not read secretKey", e);
        } finally {
            IOUtils.closeQuietly(propStream);
        }

        assertNotNull(ANONYMOUS_UNAME);
        assertNotNull(ANONYMOUS_KEY);
    }

    @BeforeClass
    public static void restSetup() {
        final String envContentType = System.getProperty(ENV_KEY_CONTENT_TYPE);
        if (StringUtils.isNotBlank(envContentType)) {
            clientFactory.getRestClientFactoryBean().setContentType(envContentType);
        }
        LOG.info("Performing IT with content type {}", clientFactory.getContentType().getMediaType());

        adminClient = clientFactory.create(ADMIN_UNAME, ADMIN_PWD);

        userService = adminClient.getService(UserService.class);
        userWorkflowService = adminClient.getService(UserWorkflowService.class);
        roleService = adminClient.getService(RoleService.class);
        resourceService = adminClient.getService(ResourceService.class);
        entitlementService = adminClient.getService(EntitlementService.class);
        configurationService = adminClient.getService(ConfigurationService.class);
        connectorService = adminClient.getService(ConnectorService.class);
        loggerService = adminClient.getService(LoggerService.class);
        reportService = adminClient.getService(ReportService.class);
        taskService = adminClient.getService(TaskService.class);
        policyService = adminClient.getService(PolicyService.class);
        workflowService = adminClient.getService(WorkflowService.class);
        notificationService = adminClient.getService(NotificationService.class);
        schemaService = adminClient.getService(SchemaService.class);
        userRequestService = adminClient.getService(UserRequestService.class);
    }

    protected static String getUUIDString() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    protected static AttributeTO attributeTO(final String schema, final String value) {
        AttributeTO attr = new AttributeTO();
        attr.setSchema(schema);
        attr.getValues().add(value);
        return attr;
    }

    protected static AttributeMod attributeMod(final String schema, final String valueToBeAdded) {
        AttributeMod attr = new AttributeMod();
        attr.setSchema(schema);
        attr.getValuesToBeAdded().add(valueToBeAdded);
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
            Exception ex = clientFactory.getExceptionMapper().fromResponse(response);
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
            Exception ex = clientFactory.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }

        return (T) adminClient.getObject(response.getLocation(), SchemaService.class, schemaTO.getClass());
    }

    protected RoleTO createRole(final RoleService roleService, final RoleTO newRoleTO) {
        Response response = roleService.create(newRoleTO);
        if (response.getStatus() != org.apache.http.HttpStatus.SC_CREATED) {
            Exception ex = clientFactory.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return adminClient.getObject(response.getLocation(), RoleService.class, RoleTO.class);
    }

    @SuppressWarnings("unchecked")
    protected <T extends AbstractPolicyTO> T createPolicy(final T policy) {
        Response response = policyService.create(policy);
        if (response.getStatus() != org.apache.http.HttpStatus.SC_CREATED) {
            Exception ex = clientFactory.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return (T) adminClient.getObject(response.getLocation(), PolicyService.class, policy.getClass());
    }

    protected ResourceTO createResource(final ResourceTO resourceTO) {
        Response response = resourceService.create(resourceTO);
        if (response.getStatus() != org.apache.http.HttpStatus.SC_CREATED) {
            Exception ex = clientFactory.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return adminClient.getObject(response.getLocation(), ResourceService.class, ResourceTO.class);
    }
}
