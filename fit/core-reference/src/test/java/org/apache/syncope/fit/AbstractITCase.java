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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.sql.DataSource;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.rs.security.jose.jwa.SignatureAlgorithm;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.self.SelfKeymasterClientContext;
import org.apache.syncope.common.keymaster.client.zookeper.ZookeeperKeymasterClientContext;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.policy.PolicyTO;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.to.SchemaTO;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.NotificationTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.batch.BatchPayloadParser;
import org.apache.syncope.common.rest.api.batch.BatchResponseItem;
import org.apache.syncope.common.rest.api.service.AnyObjectService;
import org.apache.syncope.common.rest.api.service.AnyTypeClassService;
import org.apache.syncope.common.rest.api.service.AnyTypeService;
import org.apache.syncope.common.rest.api.service.ApplicationService;
import org.apache.syncope.common.rest.api.service.CamelRouteService;
import org.apache.syncope.common.rest.api.service.ConnectorService;
import org.apache.syncope.common.rest.api.service.DynRealmService;
import org.apache.syncope.common.rest.api.service.LoggerService;
import org.apache.syncope.common.rest.api.service.NotificationService;
import org.apache.syncope.common.rest.api.service.PolicyService;
import org.apache.syncope.common.rest.api.service.ReportService;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.apache.syncope.common.rest.api.service.GroupService;
import org.apache.syncope.common.rest.api.service.ImplementationService;
import org.apache.syncope.common.rest.api.service.MailTemplateService;
import org.apache.syncope.common.rest.api.service.OIDCClientService;
import org.apache.syncope.common.rest.api.service.OIDCProviderService;
import org.apache.syncope.common.rest.api.service.RealmService;
import org.apache.syncope.common.rest.api.service.ReconciliationService;
import org.apache.syncope.common.rest.api.service.RelationshipTypeService;
import org.apache.syncope.common.rest.api.service.RemediationService;
import org.apache.syncope.common.rest.api.service.ReportTemplateService;
import org.apache.syncope.common.rest.api.service.RoleService;
import org.apache.syncope.common.rest.api.service.SAML2IdPService;
import org.apache.syncope.common.rest.api.service.SAML2SPService;
import org.apache.syncope.common.rest.api.service.SCIMConfService;
import org.apache.syncope.common.rest.api.service.SchemaService;
import org.apache.syncope.common.rest.api.service.SecurityQuestionService;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.common.rest.api.service.UserSelfService;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.syncope.common.rest.api.service.UserRequestService;
import org.apache.syncope.common.rest.api.service.BpmnProcessService;
import org.apache.syncope.common.rest.api.service.GatewayRouteService;
import org.apache.syncope.common.rest.api.service.UserWorkflowTaskService;
import org.apache.syncope.fit.core.CoreITContext;
import org.apache.syncope.fit.core.UserITCase;
import org.identityconnectors.common.security.Encryptor;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig({ CoreITContext.class, SelfKeymasterClientContext.class, ZookeeperKeymasterClientContext.class })
public abstract class AbstractITCase {

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractITCase.class);

    protected static final ObjectMapper MAPPER = new ObjectMapper();

    protected static final String ADMIN_UNAME = "admin";

    protected static final String ADMIN_PWD = "password";

    protected static final String ADDRESS = "http://localhost:9080/syncope/rest";

    protected static final String BUILD_TOOLS_ADDRESS = "http://localhost:9080/syncope-fit-build-tools/cxf";

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

    protected static final String RESOURCE_NAME_REST = "rest-target-resource";

    protected static final String RESOURCE_LDAP_ADMIN_DN = "uid=admin,ou=system";

    protected static final String RESOURCE_LDAP_ADMIN_PWD = "secret";

    protected static final String PRINTER = "PRINTER";

    protected static final int MAX_WAIT_SECONDS = 50;

    protected static String ANONYMOUS_UNAME;

    protected static String ANONYMOUS_KEY;

    protected static String JWS_KEY;

    protected static String JWT_ISSUER;

    protected static SignatureAlgorithm JWS_ALGORITHM;

    protected static SyncopeClientFactoryBean clientFactory;

    protected static SyncopeClient adminClient;

    protected static SyncopeService syncopeService;

    protected static ApplicationService applicationService;

    protected static AnyTypeClassService anyTypeClassService;

    protected static AnyTypeService anyTypeService;

    protected static RelationshipTypeService relationshipTypeService;

    protected static RealmService realmService;

    protected static AnyObjectService anyObjectService;

    protected static RoleService roleService;

    protected static DynRealmService dynRealmService;

    protected static UserService userService;

    protected static UserSelfService userSelfService;

    protected static UserRequestService userRequestService;

    protected static UserWorkflowTaskService userWorkflowTaskService;

    protected static GroupService groupService;

    protected static ResourceService resourceService;

    protected static ConnectorService connectorService;

    protected static LoggerService loggerService;

    protected static ReportTemplateService reportTemplateService;

    protected static ReportService reportService;

    protected static TaskService taskService;

    protected static ReconciliationService reconciliationService;

    protected static BpmnProcessService bpmnProcessService;

    protected static MailTemplateService mailTemplateService;

    protected static NotificationService notificationService;

    protected static SchemaService schemaService;

    protected static PolicyService policyService;

    protected static SecurityQuestionService securityQuestionService;

    protected static ImplementationService implementationService;

    protected static RemediationService remediationService;

    protected static GatewayRouteService gatewayRouteService;

    protected static CamelRouteService camelRouteService;

    protected static SAML2SPService saml2SpService;

    protected static SAML2IdPService saml2IdPService;

    protected static OIDCClientService oidcClientService;

    protected static OIDCProviderService oidcProviderService;

    protected static SCIMConfService scimConfService;

    @BeforeAll
    public static void securitySetup() {
        try (InputStream propStream = Encryptor.class.getResourceAsStream("/security.properties")) {
            Properties props = new Properties();
            props.load(propStream);

            ANONYMOUS_UNAME = props.getProperty("anonymousUser");
            ANONYMOUS_KEY = props.getProperty("anonymousKey");
            JWT_ISSUER = props.getProperty("jwtIssuer");
            JWS_ALGORITHM = SignatureAlgorithm.valueOf(props.getProperty("jwsAlgorithm"));
            JWS_KEY = props.getProperty("jwsKey");
        } catch (Exception e) {
            LOG.error("Could not read secretKey", e);
        }

        assertNotNull(ANONYMOUS_UNAME);
        assertNotNull(ANONYMOUS_KEY);
        assertNotNull(JWS_KEY);
        assertNotNull(JWT_ISSUER);
    }

    @BeforeAll
    public static void restSetup() {
        clientFactory = new SyncopeClientFactoryBean().setAddress(ADDRESS);

        String envContentType = System.getProperty(ENV_KEY_CONTENT_TYPE);
        if (StringUtils.isNotBlank(envContentType)) {
            clientFactory.setContentType(envContentType);
        }
        LOG.info("Performing IT with content type {}", clientFactory.getContentType().getMediaType());

        adminClient = clientFactory.create(ADMIN_UNAME, ADMIN_PWD);

        syncopeService = adminClient.getService(SyncopeService.class);
        applicationService = adminClient.getService(ApplicationService.class);
        anyTypeClassService = adminClient.getService(AnyTypeClassService.class);
        anyTypeService = adminClient.getService(AnyTypeService.class);
        relationshipTypeService = adminClient.getService(RelationshipTypeService.class);
        realmService = adminClient.getService(RealmService.class);
        anyObjectService = adminClient.getService(AnyObjectService.class);
        roleService = adminClient.getService(RoleService.class);
        dynRealmService = adminClient.getService(DynRealmService.class);
        userService = adminClient.getService(UserService.class);
        userSelfService = adminClient.getService(UserSelfService.class);
        userRequestService = adminClient.getService(UserRequestService.class);
        userWorkflowTaskService = adminClient.getService(UserWorkflowTaskService.class);
        groupService = adminClient.getService(GroupService.class);
        resourceService = adminClient.getService(ResourceService.class);
        connectorService = adminClient.getService(ConnectorService.class);
        loggerService = adminClient.getService(LoggerService.class);
        reportTemplateService = adminClient.getService(ReportTemplateService.class);
        reportService = adminClient.getService(ReportService.class);
        taskService = adminClient.getService(TaskService.class);
        reconciliationService = adminClient.getService(ReconciliationService.class);
        policyService = adminClient.getService(PolicyService.class);
        bpmnProcessService = adminClient.getService(BpmnProcessService.class);
        mailTemplateService = adminClient.getService(MailTemplateService.class);
        notificationService = adminClient.getService(NotificationService.class);
        schemaService = adminClient.getService(SchemaService.class);
        securityQuestionService = adminClient.getService(SecurityQuestionService.class);
        implementationService = adminClient.getService(ImplementationService.class);
        remediationService = adminClient.getService(RemediationService.class);
        gatewayRouteService = adminClient.getService(GatewayRouteService.class);
        camelRouteService = adminClient.getService(CamelRouteService.class);
        saml2SpService = adminClient.getService(SAML2SPService.class);
        saml2IdPService = adminClient.getService(SAML2IdPService.class);
        oidcClientService = adminClient.getService(OIDCClientService.class);
        oidcProviderService = adminClient.getService(OIDCProviderService.class);
        scimConfService = adminClient.getService(SCIMConfService.class);
    }

    @Autowired
    protected ConfParamOps confParamOps;

    @Autowired
    protected ServiceOps serviceOps;

    @Autowired
    protected DomainOps domainOps;

    @Autowired
    protected DataSource testDataSource;

    protected static String getUUIDString() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    protected static Attr attr(final String schema, final String value) {
        return new Attr.Builder(schema).value(value).build();
    }

    protected static AttrPatch attrAddReplacePatch(final String schema, final String value) {
        return new AttrPatch.Builder(attr(schema, value)).operation(PatchOperation.ADD_REPLACE).build();
    }

    public static <T> T getObject(final URI location, final Class<?> serviceClass, final Class<T> resultClass) {
        WebClient webClient = WebClient.fromClient(WebClient.client(adminClient.getService(serviceClass)));
        webClient.accept(clientFactory.getContentType().getMediaType()).to(location.toASCIIString(), false);

        return webClient.
                header(RESTHeaders.DOMAIN, adminClient.getDomain()).
                header(HttpHeaders.AUTHORIZATION, "Bearer " + adminClient.getJWT()).
                get(resultClass);
    }

    @SuppressWarnings("unchecked")
    protected <T extends SchemaTO> T createSchema(final SchemaType type, final T schemaTO) {
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

    protected ReportTO createReport(final ReportTO report) {
        Response response = reportService.create(report);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());
        return getObject(response.getLocation(), ReportService.class, ReportTO.class);
    }

    protected Pair<String, String> createNotificationTask(
            final boolean active,
            final boolean includeAbout,
            final TraceLevel traceLevel,
            final String sender,
            final String subject,
            final String... staticRecipients) {

        // 1. Create notification
        NotificationTO notification = new NotificationTO();
        notification.setTraceLevel(traceLevel);
        notification.getEvents().add("[LOGIC]:[UserLogic]:[]:[create]:[SUCCESS]");

        if (includeAbout) {
            notification.getAbouts().put(AnyTypeKind.USER.name(),
                    SyncopeClient.getUserSearchConditionBuilder().
                            inGroups("bf825fe1-7320-4a54-bd64-143b5c18ab97").query());
        }

        notification.setRecipientsFIQL(SyncopeClient.getUserSearchConditionBuilder().
                inGroups("f779c0d4-633b-4be5-8f57-32eb478a3ca5").query());
        notification.setSelfAsRecipient(true);
        notification.setRecipientAttrName("email");
        if (staticRecipients != null) {
            notification.getStaticRecipients().addAll(List.of(staticRecipients));
        }

        notification.setSender(sender);
        notification.setSubject(subject);
        notification.setTemplate("optin");
        notification.setActive(active);

        Response response = notificationService.create(notification);
        notification = getObject(response.getLocation(), NotificationService.class, NotificationTO.class);
        assertNotNull(notification);

        // 2. create user
        UserCR req = UserITCase.getUniqueSample("notificationtest@syncope.apache.org");
        req.getMemberships().add(new MembershipTO.Builder("bf825fe1-7320-4a54-bd64-143b5c18ab97").build());

        UserTO userTO = createUser(req).getEntity();
        assertNotNull(userTO);
        return Pair.of(notification.getKey(), req.getUsername());
    }

    protected ProvisioningResult<UserTO> createUser(final UserCR req) {
        Response response = userService.create(req);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            Exception ex = clientFactory.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return response.readEntity(new GenericType<ProvisioningResult<UserTO>>() {
        });
    }

    protected ProvisioningResult<UserTO> updateUser(final UserUR req) {
        return userService.update(req).
                readEntity(new GenericType<ProvisioningResult<UserTO>>() {
                });
    }

    protected ProvisioningResult<UserTO> deleteUser(final String key) {
        return userService.delete(key).
                readEntity(new GenericType<ProvisioningResult<UserTO>>() {
                });
    }

    protected ProvisioningResult<AnyObjectTO> createAnyObject(final AnyObjectCR req) {
        Response response = anyObjectService.create(req);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            Exception ex = clientFactory.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return response.readEntity(new GenericType<ProvisioningResult<AnyObjectTO>>() {
        });
    }

    protected ProvisioningResult<AnyObjectTO> updateAnyObject(final AnyObjectUR req) {
        return anyObjectService.update(req).
                readEntity(new GenericType<ProvisioningResult<AnyObjectTO>>() {
                });
    }

    protected ProvisioningResult<AnyObjectTO> deleteAnyObject(final String key) {
        return anyObjectService.delete(key).
                readEntity(new GenericType<ProvisioningResult<AnyObjectTO>>() {
                });
    }

    protected ProvisioningResult<GroupTO> createGroup(final GroupCR req) {
        Response response = groupService.create(req);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            Exception ex = clientFactory.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return response.readEntity(new GenericType<ProvisioningResult<GroupTO>>() {
        });
    }

    protected ProvisioningResult<GroupTO> updateGroup(final GroupUR req) {
        return groupService.update(req).
                readEntity(new GenericType<ProvisioningResult<GroupTO>>() {
                });
    }

    protected ProvisioningResult<GroupTO> deleteGroup(final String key) {
        return groupService.delete(key).
                readEntity(new GenericType<ProvisioningResult<GroupTO>>() {
                });
    }

    @SuppressWarnings("unchecked")
    protected <T extends PolicyTO> T createPolicy(final PolicyType type, final T policy) {
        Response response = policyService.create(type, policy);
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

    protected List<BatchResponseItem> parseBatchResponse(final Response response) throws IOException {
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        return BatchPayloadParser.parse(
                (InputStream) response.getEntity(), response.getMediaType(), new BatchResponseItem());
    }

    @SuppressWarnings({ "unchecked", "rawtypes", "UseOfObsoleteCollectionType" })
    protected InitialDirContext getLdapResourceDirContext(final String bindDn, final String bindPwd)
            throws NamingException {
        ResourceTO ldapRes = resourceService.read(RESOURCE_NAME_LDAP);
        ConnInstanceTO ldapConn = connectorService.read(ldapRes.getConnector(), Locale.ENGLISH.getLanguage());

        Properties env = new Properties();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, "ldap://" + ldapConn.getConf("host").get().getValues().get(0)
                + ':' + ldapConn.getConf("port").get().getValues().get(0) + '/');
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL,
                bindDn == null ? ldapConn.getConf("principal").get().getValues().get(0) : bindDn);
        env.put(Context.SECURITY_CREDENTIALS,
                bindPwd == null ? ldapConn.getConf("credentials").get().getValues().get(0) : bindPwd);

        return new InitialDirContext(env);
    }

    protected Object getLdapRemoteObject(final String bindDn, final String bindPwd, final String objectDn) {
        InitialDirContext ctx = null;
        try {
            ctx = getLdapResourceDirContext(bindDn, bindPwd);
            return ctx.lookup(objectDn);
        } catch (Exception e) {
            LOG.error("Could not fetch {}", objectDn, e);
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

    protected void updateLdapRemoteObject(
            final String bindDn,
            final String bindPwd,
            final String objectDn,
            final Map<String, String> attributes) {

        InitialDirContext ctx = null;
        try {
            ctx = getLdapResourceDirContext(bindDn, bindPwd);

            List<ModificationItem> items = new ArrayList<>();
            attributes.forEach((key, value) -> items.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE, new BasicAttribute(key, value))));

            ctx.modifyAttributes(objectDn, items.toArray(new ModificationItem[] {}));
        } catch (Exception e) {
            LOG.error("While updating {} with {}", objectDn, attributes, e);
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

    protected void removeLdapRemoteObject(
            final String bindDn,
            final String bindPwd,
            final String objectDn) {

        InitialDirContext ctx = null;
        try {
            ctx = getLdapResourceDirContext(bindDn, bindPwd);

            ctx.destroySubcontext(objectDn);
        } catch (Exception e) {
            LOG.error("While removing {}", objectDn, e);
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

    protected <T> T queryForObject(
            final JdbcTemplate jdbcTemplate,
            final int maxWaitSeconds,
            final String sql, final Class<T> requiredType, final Object... args) {

        int i = 0;
        int maxit = maxWaitSeconds;

        T object = null;

        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }

            try {
                object = jdbcTemplate.queryForObject(sql, requiredType, args);
            } catch (Exception e) {
                LOG.warn("While executing query {}", sql, e);
            }

            i++;
        } while (object == null && i < maxit);
        if (object == null) {
            fail("Timeout when executing query " + sql);
        }

        return object;
    }
}
