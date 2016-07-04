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

import java.io.InputStream;
import java.net.URI;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.InitialDirContext;
import javax.sql.DataSource;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Response;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.lib.patch.AnyObjectPatch;
import org.apache.syncope.common.lib.patch.AttrPatch;
import org.apache.syncope.common.lib.patch.GroupPatch;
import org.apache.syncope.common.lib.patch.UserPatch;
import org.apache.syncope.common.lib.policy.AbstractPolicyTO;
import org.apache.syncope.common.lib.to.AbstractSchemaTO;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AttrTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.ConnConfProperty;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.rest.api.service.AnyObjectService;
import org.apache.syncope.common.rest.api.service.AnyTypeClassService;
import org.apache.syncope.common.rest.api.service.AnyTypeService;
import org.apache.syncope.common.rest.api.service.CamelRouteService;
import org.apache.syncope.common.rest.api.service.ConfigurationService;
import org.apache.syncope.common.rest.api.service.ConnectorService;
import org.apache.syncope.common.rest.api.service.DomainService;
import org.apache.syncope.common.rest.api.service.LoggerService;
import org.apache.syncope.common.rest.api.service.NotificationService;
import org.apache.syncope.common.rest.api.service.PolicyService;
import org.apache.syncope.common.rest.api.service.ReportService;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.apache.syncope.common.rest.api.service.GroupService;
import org.apache.syncope.common.rest.api.service.MailTemplateService;
import org.apache.syncope.common.rest.api.service.RealmService;
import org.apache.syncope.common.rest.api.service.RelationshipTypeService;
import org.apache.syncope.common.rest.api.service.ReportTemplateService;
import org.apache.syncope.common.rest.api.service.RoleService;
import org.apache.syncope.common.rest.api.service.SchemaService;
import org.apache.syncope.common.rest.api.service.SecurityQuestionService;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.common.rest.api.service.UserSelfService;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.syncope.common.rest.api.service.UserWorkflowService;
import org.apache.syncope.common.rest.api.service.WorkflowService;
import org.identityconnectors.common.security.Encryptor;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertNotNull;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath:testJDBCContext.xml" })
public abstract class AbstractITCase {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractITCase.class);

    protected static final String ADMIN_UNAME = "admin";

    protected static final String ADMIN_PWD = "password";

    protected static final String ADDRESS = "http://localhost:9080/syncope/rest";

    protected static final String ENV_KEY_CONTENT_TYPE = "jaxrsContentType";

    protected static final String RESOURCE_NAME_WS1 = "ws-target-resource-1";

    protected static final String RESOURCE_NAME_WS2 = "ws-target-resource-2";

    protected static final String RESOURCE_NAME_LDAP = "resource-ldap";

    protected static final String RESOURCE_NAME_LDAP_ORGUNIT = "resource-ldap-orgunit";

    protected static final String RESOURCE_NAME_TESTDB = "resource-testdb";

    protected static final String RESOURCE_NAME_TESTDB2 = "resource-testdb2";

    protected static final String RESOURCE_NAME_CSV = "resource-csv";

    protected static final String RESOURCE_NAME_DBPULL = "resource-db-pull";

    protected static final String RESOURCE_NAME_DBVIRATTR = "resource-db-virattr";

    protected static final String RESOURCE_NAME_NOPROPAGATION = "ws-target-resource-nopropagation";

    protected static final String RESOURCE_NAME_NOPROPAGATION2 = "ws-target-resource-nopropagation2";

    protected static final String RESOURCE_NAME_NOPROPAGATION3 = "ws-target-resource-nopropagation3";

    protected static final String RESOURCE_NAME_NOPROPAGATION4 = "ws-target-resource-nopropagation4";

    protected static final String RESOURCE_NAME_RESETSYNCTOKEN = "ws-target-resource-update-resetsynctoken";

    protected static final String RESOURCE_NAME_TIMEOUT = "ws-target-resource-timeout";

    protected static final String RESOURCE_NAME_MAPPINGS1 = "ws-target-resource-list-mappings-1";

    protected static final String RESOURCE_NAME_MAPPINGS2 = "ws-target-resource-list-mappings-2";

    protected static final String RESOURCE_NAME_CREATE_SINGLE = "ws-target-resource-create-single";

    protected static final String RESOURCE_NAME_CREATE_WRONG = "ws-target-resource-create-wrong";

    protected static final String RESOURCE_NAME_DELETE = "ws-target-resource-delete";

    protected static final String RESOURCE_NAME_UPDATE = "ws-target-resource-update";

    protected static final String RESOURCE_NAME_CREATE_NONE = "ws-target-resource-create-none";

    protected static final String RESOURCE_NAME_DBSCRIPTED = "resource-db-scripted";

    protected static final String RESOURCE_LDAP_ADMIN_DN = "uid=admin,ou=system";

    protected static final String RESOURCE_LDAP_ADMIN_PWD = "secret";

    protected static String ANONYMOUS_UNAME;

    protected static String ANONYMOUS_KEY;

    protected static SyncopeClientFactoryBean clientFactory;

    protected static SyncopeClient adminClient;

    protected static SyncopeService syncopeService;

    protected static DomainService domainService;

    protected static AnyTypeClassService anyTypeClassService;

    protected static AnyTypeService anyTypeService;

    protected static RelationshipTypeService relationshipTypeService;

    protected static RealmService realmService;

    protected static AnyObjectService anyObjectService;

    protected static RoleService roleService;

    protected static UserService userService;

    protected static UserSelfService userSelfService;

    protected static UserWorkflowService userWorkflowService;

    protected static GroupService groupService;

    protected static ResourceService resourceService;

    protected static ConfigurationService configurationService;

    protected static ConnectorService connectorService;

    protected static LoggerService loggerService;

    protected static ReportTemplateService reportTemplateService;

    protected static ReportService reportService;

    protected static TaskService taskService;

    protected static WorkflowService workflowService;

    protected static MailTemplateService mailTemplateService;

    protected static NotificationService notificationService;

    protected static SchemaService schemaService;

    protected static PolicyService policyService;

    protected static SecurityQuestionService securityQuestionService;

    protected static CamelRouteService camelRouteService;

    @Autowired
    protected DataSource testDataSource;

    @BeforeClass
    public static void securitySetup() {
        InputStream propStream = null;
        try {
            propStream = Encryptor.class.getResourceAsStream("/security.properties");
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
        clientFactory = new SyncopeClientFactoryBean().setAddress(ADDRESS);

        String envContentType = System.getProperty(ENV_KEY_CONTENT_TYPE);
        if (StringUtils.isNotBlank(envContentType)) {
            clientFactory.setContentType(envContentType);
        }
        LOG.info("Performing IT with content type {}", clientFactory.getContentType().getMediaType());

        adminClient = clientFactory.create(ADMIN_UNAME, ADMIN_PWD);

        syncopeService = adminClient.getService(SyncopeService.class);
        domainService = adminClient.getService(DomainService.class);
        anyTypeClassService = adminClient.getService(AnyTypeClassService.class);
        anyTypeService = adminClient.getService(AnyTypeService.class);
        relationshipTypeService = adminClient.getService(RelationshipTypeService.class);
        realmService = adminClient.getService(RealmService.class);
        anyObjectService = adminClient.getService(AnyObjectService.class);
        roleService = adminClient.getService(RoleService.class);
        userService = adminClient.getService(UserService.class);
        userSelfService = adminClient.getService(UserSelfService.class);
        userWorkflowService = adminClient.getService(UserWorkflowService.class);
        groupService = adminClient.getService(GroupService.class);
        resourceService = adminClient.getService(ResourceService.class);
        configurationService = adminClient.getService(ConfigurationService.class);
        connectorService = adminClient.getService(ConnectorService.class);
        loggerService = adminClient.getService(LoggerService.class);
        reportTemplateService = adminClient.getService(ReportTemplateService.class);
        reportService = adminClient.getService(ReportService.class);
        taskService = adminClient.getService(TaskService.class);
        policyService = adminClient.getService(PolicyService.class);
        workflowService = adminClient.getService(WorkflowService.class);
        mailTemplateService = adminClient.getService(MailTemplateService.class);
        notificationService = adminClient.getService(NotificationService.class);
        schemaService = adminClient.getService(SchemaService.class);
        securityQuestionService = adminClient.getService(SecurityQuestionService.class);
        camelRouteService = adminClient.getService(CamelRouteService.class);
    }

    protected static String getUUIDString() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    protected static AttrTO attrTO(final String schema, final String value) {
        return new AttrTO.Builder().schema(schema).value(value).build();
    }

    protected static AttrPatch attrAddReplacePatch(final String schema, final String value) {
        return new AttrPatch.Builder().operation(PatchOperation.ADD_REPLACE).attrTO(attrTO(schema, value)).build();
    }

    public <T> T getObject(final URI location, final Class<?> serviceClass, final Class<T> resultClass) {
        WebClient webClient = WebClient.fromClient(WebClient.client(adminClient.getService(serviceClass)));
        webClient.accept(clientFactory.getContentType().getMediaType()).to(location.toASCIIString(), false);

        return webClient.get(resultClass);
    }

    @SuppressWarnings("unchecked")
    protected <T extends AbstractSchemaTO> T createSchema(final SchemaType type, final T schemaTO) {
        Response response = schemaService.create(type, schemaTO);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            Exception ex = clientFactory.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }

        return (T) getObject(response.getLocation(), SchemaService.class, schemaTO.getClass());
    }

    protected RoleTO createRole(final RoleTO roleTO) {
        Response response = roleService.create(roleTO);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            Exception ex = clientFactory.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return getObject(response.getLocation(), RoleService.class, RoleTO.class);
    }

    protected ProvisioningResult<UserTO> createUser(final UserTO userTO) {
        return createUser(userTO, true);
    }

    protected ProvisioningResult<UserTO> createUser(final UserTO userTO, final boolean storePassword) {
        Response response = userService.create(userTO, storePassword);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            Exception ex = clientFactory.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return response.readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        });
    }

    protected ProvisioningResult<UserTO> updateUser(final UserPatch userPatch) {
        return userService.update(userPatch).
                readEntity(new GenericType<ProvisioningResult<UserTO>>() {
                });
    }

    protected ProvisioningResult<UserTO> deleteUser(final String key) {
        return userService.delete(key).
                readEntity(new GenericType<ProvisioningResult<UserTO>>() {
                });
    }

    protected ProvisioningResult<AnyObjectTO> createAnyObject(final AnyObjectTO anyObjectTO) {
        Response response = anyObjectService.create(anyObjectTO);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            Exception ex = clientFactory.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return response.readEntity(new GenericType<ProvisioningResult<AnyObjectTO>>() {
        });
    }

    protected ProvisioningResult<AnyObjectTO> updateAnyObject(final AnyObjectPatch anyObjectPatch) {
        return anyObjectService.update(anyObjectPatch).
                readEntity(new GenericType<ProvisioningResult<AnyObjectTO>>() {
                });
    }

    protected ProvisioningResult<AnyObjectTO> deleteAnyObject(final String key) {
        return anyObjectService.delete(key).
                readEntity(new GenericType<ProvisioningResult<AnyObjectTO>>() {
                });
    }

    protected ProvisioningResult<GroupTO> createGroup(final GroupTO groupTO) {
        Response response = groupService.create(groupTO);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            Exception ex = clientFactory.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return response.readEntity(new GenericType<ProvisioningResult<GroupTO>>() {
        });
    }

    protected ProvisioningResult<GroupTO> updateGroup(final GroupPatch groupPatch) {
        return groupService.update(groupPatch).
                readEntity(new GenericType<ProvisioningResult<GroupTO>>() {
                });
    }

    protected ProvisioningResult<GroupTO> deleteGroup(final String key) {
        return groupService.delete(key).
                readEntity(new GenericType<ProvisioningResult<GroupTO>>() {
                });
    }

    @SuppressWarnings("unchecked")
    protected <T extends AbstractPolicyTO> T createPolicy(final T policy) {
        Response response = policyService.create(policy);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            Exception ex = clientFactory.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return (T) getObject(response.getLocation(), PolicyService.class, policy.getClass());
    }

    protected ResourceTO createResource(final ResourceTO resourceTO) {
        Response response = resourceService.create(resourceTO);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            Exception ex = clientFactory.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return getObject(response.getLocation(), ResourceService.class, ResourceTO.class);
    }

    @SuppressWarnings({ "unchecked", "rawtypes", "UseOfObsoleteCollectionType" })
    protected InitialDirContext getLdapResourceDirContext(final String bindDn, final String bindPwd)
            throws NamingException {
        ResourceTO ldapRes = resourceService.read(RESOURCE_NAME_LDAP);
        final Map<String, ConnConfProperty> ldapConnConf =
                connectorService.read(ldapRes.getConnector(), Locale.ENGLISH.getLanguage()).getConfMap();

        Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://" + ldapConnConf.get("host").getValues().get(0)
                + ":" + ldapConnConf.get("port").getValues().get(0) + "/");
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL,
                bindDn == null ? ldapConnConf.get("principal").getValues().get(0) : bindDn);
        env.put(Context.SECURITY_CREDENTIALS,
                bindPwd == null ? ldapConnConf.get("credentials").getValues().get(0) : bindPwd);

        return new InitialDirContext(env);
    }

    protected Object getLdapRemoteObject(final String bindDn, final String bindPwd, final String objectDn) {
        InitialDirContext ctx = null;
        try {
            ctx = getLdapResourceDirContext(bindDn, bindPwd);
            return ctx.lookup(objectDn);
        } catch (Exception e) {
            return null;
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException e) {
                    // ignore
                }
            }
        }
    }
}
