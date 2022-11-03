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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.nimbusds.jose.JWSAlgorithm;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.BasicAttribute;
import javax.naming.directory.BasicAttributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.ModificationItem;
import javax.sql.DataSource;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.lib.AnonymousAuthenticationHandler;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.self.SelfKeymasterClientContext;
import org.apache.syncope.common.keymaster.client.zookeeper.ZookeeperKeymasterClientContext;
import org.apache.syncope.common.lib.AnyOperations;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.audit.AuditEntry;
import org.apache.syncope.common.lib.policy.AccessPolicyTO;
import org.apache.syncope.common.lib.policy.AttrReleasePolicyTO;
import org.apache.syncope.common.lib.policy.AuthPolicyTO;
import org.apache.syncope.common.lib.policy.DefaultAccessPolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAttrReleasePolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAuthPolicyConf;
import org.apache.syncope.common.lib.policy.PolicyTO;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.ClientAppTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.NotificationTO;
import org.apache.syncope.common.lib.to.OIDCRPClientAppTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.to.SAML2SPClientAppTO;
import org.apache.syncope.common.lib.to.SchemaTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.lib.types.OIDCGrantType;
import org.apache.syncope.common.lib.types.OIDCResponseType;
import org.apache.syncope.common.lib.types.OIDCSubjectType;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.lib.types.SAML2SPNameId;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.batch.BatchPayloadParser;
import org.apache.syncope.common.rest.api.batch.BatchResponseItem;
import org.apache.syncope.common.rest.api.beans.AuditQuery;
import org.apache.syncope.common.rest.api.service.AnyObjectService;
import org.apache.syncope.common.rest.api.service.AnyTypeClassService;
import org.apache.syncope.common.rest.api.service.AnyTypeService;
import org.apache.syncope.common.rest.api.service.ApplicationService;
import org.apache.syncope.common.rest.api.service.AttrRepoService;
import org.apache.syncope.common.rest.api.service.AuditService;
import org.apache.syncope.common.rest.api.service.AuthModuleService;
import org.apache.syncope.common.rest.api.service.AuthProfileService;
import org.apache.syncope.common.rest.api.service.BpmnProcessService;
import org.apache.syncope.common.rest.api.service.ClientAppService;
import org.apache.syncope.common.rest.api.service.CommandService;
import org.apache.syncope.common.rest.api.service.ConnectorService;
import org.apache.syncope.common.rest.api.service.DelegationService;
import org.apache.syncope.common.rest.api.service.DynRealmService;
import org.apache.syncope.common.rest.api.service.GroupService;
import org.apache.syncope.common.rest.api.service.ImplementationService;
import org.apache.syncope.common.rest.api.service.MailTemplateService;
import org.apache.syncope.common.rest.api.service.NotificationService;
import org.apache.syncope.common.rest.api.service.OIDCC4UIProviderService;
import org.apache.syncope.common.rest.api.service.OIDCC4UIService;
import org.apache.syncope.common.rest.api.service.OIDCJWKSService;
import org.apache.syncope.common.rest.api.service.PolicyService;
import org.apache.syncope.common.rest.api.service.RealmService;
import org.apache.syncope.common.rest.api.service.ReconciliationService;
import org.apache.syncope.common.rest.api.service.RelationshipTypeService;
import org.apache.syncope.common.rest.api.service.RemediationService;
import org.apache.syncope.common.rest.api.service.ReportService;
import org.apache.syncope.common.rest.api.service.ReportTemplateService;
import org.apache.syncope.common.rest.api.service.ResourceService;
import org.apache.syncope.common.rest.api.service.RoleService;
import org.apache.syncope.common.rest.api.service.SAML2IdPEntityService;
import org.apache.syncope.common.rest.api.service.SAML2SP4UIIdPService;
import org.apache.syncope.common.rest.api.service.SAML2SP4UIService;
import org.apache.syncope.common.rest.api.service.SAML2SPEntityService;
import org.apache.syncope.common.rest.api.service.SCIMConfService;
import org.apache.syncope.common.rest.api.service.SRARouteService;
import org.apache.syncope.common.rest.api.service.SchemaService;
import org.apache.syncope.common.rest.api.service.SecurityQuestionService;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.common.rest.api.service.UserRequestService;
import org.apache.syncope.common.rest.api.service.UserSelfService;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.syncope.common.rest.api.service.UserWorkflowTaskService;
import org.apache.syncope.common.rest.api.service.wa.GoogleMfaAuthAccountService;
import org.apache.syncope.common.rest.api.service.wa.GoogleMfaAuthTokenService;
import org.apache.syncope.common.rest.api.service.wa.ImpersonationService;
import org.apache.syncope.common.rest.api.service.wa.U2FRegistrationService;
import org.apache.syncope.common.rest.api.service.wa.WAConfigService;
import org.apache.syncope.common.rest.api.service.wa.WebAuthnRegistrationService;
import org.apache.syncope.fit.AbstractITCase.KeymasterInitializer;
import org.apache.syncope.fit.core.CoreITContext;
import org.apache.syncope.fit.core.UserITCase;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.support.TestPropertySourceUtils;

@SpringJUnitConfig(
        classes = { CoreITContext.class, SelfKeymasterClientContext.class, ZookeeperKeymasterClientContext.class },
        initializers = KeymasterInitializer.class)
@TestPropertySource("classpath:test.properties")
public abstract class AbstractITCase {

    static class KeymasterInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(final ConfigurableApplicationContext ctx) {
            String profiles = ctx.getEnvironment().getProperty("springActiveProfiles");
            if (profiles.contains("zookeeper")) {
                TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                        ctx, "keymaster.address=127.0.0.1:2181");
            } else {
                TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                        ctx, "keymaster.address=http://localhost:9080/syncope/rest/keymaster");
            }
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    ctx, "keymaster.username=" + ANONYMOUS_UNAME);
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(
                    ctx, "keymaster.password=" + ANONYMOUS_KEY);
        }
    }

    protected static final Logger LOG = LoggerFactory.getLogger(AbstractITCase.class);

    protected static final JsonMapper JSON_MAPPER = JsonMapper.builder().findAndAddModules().build();

    protected static final XmlMapper XML_MAPPER = XmlMapper.builder().findAndAddModules().build();

    protected static final YAMLMapper YAML_MAPPER = YAMLMapper.builder().findAndAddModules().build();

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

    protected static JWSAlgorithm JWS_ALGORITHM;

    protected static SyncopeClientFactoryBean CLIENT_FACTORY;

    protected static SyncopeClient ADMIN_CLIENT;

    protected static SyncopeClient ANONYMOUS_CLIENT;

    protected static SyncopeService SYNCOPE_SERVICE;

    protected static ApplicationService APPLICATION_SERVICE;

    protected static AnyTypeClassService ANY_TYPE_CLASS_SERVICE;

    protected static AnyTypeService ANY_TYPE_SERVICE;

    protected static RelationshipTypeService RELATIONSHIP_TYPE_SERVICE;

    protected static RealmService REALM_SERVICE;

    protected static AnyObjectService ANY_OBJECT_SERVICE;

    protected static RoleService ROLE_SERVICE;

    protected static DynRealmService DYN_REALM_SERVICE;

    protected static UserService USER_SERVICE;

    protected static UserSelfService USER_SELF_SERVICE;

    protected static UserRequestService USER_REQUEST_SERVICE;

    protected static UserWorkflowTaskService USER_WORKFLOW_TASK_SERVICE;

    protected static GroupService GROUP_SERVICE;

    protected static ResourceService RESOURCE_SERVICE;

    protected static ConnectorService CONNECTOR_SERVICE;

    protected static AuditService AUDIT_SERVICE;

    protected static ReportTemplateService REPORT_TEMPLATE_SERVICE;

    protected static ReportService REPORT_SERVICE;

    protected static TaskService TASK_SERVICE;

    protected static ReconciliationService RECONCILIATION_SERVICE;

    protected static BpmnProcessService BPMN_PROCESS_SERVICE;

    protected static MailTemplateService MAIL_TEMPLATE_SERVICE;

    protected static NotificationService NOTIFICATION_SERVICE;

    protected static SchemaService SCHEMA_SERVICE;

    protected static PolicyService POLICY_SERVICE;

    protected static AuthModuleService AUTH_MODULE_SERVICE;

    protected static AttrRepoService ATTR_REPO_SERVICE;

    protected static SecurityQuestionService SECURITY_QUESTION_SERVICE;

    protected static ImplementationService IMPLEMENTATION_SERVICE;

    protected static RemediationService REMEDIATION_SERVICE;

    protected static DelegationService DELEGATION_SERVICE;

    protected static CommandService COMMAND_SERVICE;

    protected static SRARouteService SRA_ROUTE_SERVICE;

    protected static SAML2SP4UIService SAML2SP4UI_SERVICE;

    protected static SAML2SP4UIIdPService SAML2SP4UI_IDP_SERVICE;

    protected static OIDCC4UIService OIDCC4UI_SERVICE;

    protected static OIDCC4UIProviderService OIDCC4UI_PROVIDER_SERVICE;

    protected static SCIMConfService SCIM_CONF_SERVICE;

    protected static ClientAppService CLIENT_APP_SERVICE;

    protected static AuthProfileService AUTH_PROFILE_SERVICE;

    protected static SAML2SPEntityService SAML2SP_ENTITY_SERVICE;

    protected static SAML2IdPEntityService SAML2IDP_ENTITY_SERVICE;

    protected static OIDCJWKSService OIDC_JWKS_SERVICE;

    protected static WAConfigService WA_CONFIG_SERVICE;

    protected static GoogleMfaAuthTokenService GOOGLE_MFA_AUTH_TOKEN_SERVICE;

    protected static GoogleMfaAuthAccountService GOOGLE_MFA_AUTH_ACCOUNT_SERVICE;

    protected static U2FRegistrationService U2F_REGISTRATION_SERVICE;

    protected static WebAuthnRegistrationService WEBAUTHN_REGISTRATION_SERVICE;

    protected static ImpersonationService IMPERSONATION_SERVICE;

    protected static boolean IS_FLOWABLE_ENABLED = false;

    protected static boolean IS_ELASTICSEARCH_ENABLED = false;

    @BeforeAll
    public static void securitySetup() {
        try (InputStream propStream = AbstractITCase.class.getResourceAsStream("/core.properties")) {
            Properties props = new Properties();
            props.load(propStream);

            ANONYMOUS_UNAME = props.getProperty("security.anonymousUser");
            ANONYMOUS_KEY = props.getProperty("security.anonymousKey");
            JWT_ISSUER = props.getProperty("security.jwtIssuer");
            JWS_ALGORITHM = JWSAlgorithm.parse(props.getProperty("security.jwsAlgorithm"));
            JWS_KEY = props.getProperty("security.jwsKey");
        } catch (Exception e) {
            LOG.error("Could not read core.properties", e);
        }

        assertNotNull(ANONYMOUS_UNAME);
        assertNotNull(ANONYMOUS_KEY);
        assertNotNull(JWS_KEY);
        assertNotNull(JWT_ISSUER);

        ANONYMOUS_CLIENT = CLIENT_FACTORY.create(new AnonymousAuthenticationHandler(ANONYMOUS_UNAME, ANONYMOUS_KEY));

        GOOGLE_MFA_AUTH_TOKEN_SERVICE = ANONYMOUS_CLIENT.getService(GoogleMfaAuthTokenService.class);
        GOOGLE_MFA_AUTH_ACCOUNT_SERVICE = ANONYMOUS_CLIENT.getService(GoogleMfaAuthAccountService.class);
        U2F_REGISTRATION_SERVICE = ANONYMOUS_CLIENT.getService(U2FRegistrationService.class);
        WEBAUTHN_REGISTRATION_SERVICE = ANONYMOUS_CLIENT.getService(WebAuthnRegistrationService.class);
        IMPERSONATION_SERVICE = ANONYMOUS_CLIENT.getService(ImpersonationService.class);
    }

    @BeforeAll
    public static void restSetup() {
        CLIENT_FACTORY = new SyncopeClientFactoryBean().setAddress(ADDRESS);

        String envContentType = System.getProperty(ENV_KEY_CONTENT_TYPE);
        if (StringUtils.isNotBlank(envContentType)) {
            CLIENT_FACTORY.setContentType(envContentType);
        }
        LOG.info("Performing IT with content type {}", CLIENT_FACTORY.getContentType().getMediaType());

        ADMIN_CLIENT = CLIENT_FACTORY.create(ADMIN_UNAME, ADMIN_PWD);

        SYNCOPE_SERVICE = ADMIN_CLIENT.getService(SyncopeService.class);
        APPLICATION_SERVICE = ADMIN_CLIENT.getService(ApplicationService.class);
        ANY_TYPE_CLASS_SERVICE = ADMIN_CLIENT.getService(AnyTypeClassService.class);
        ANY_TYPE_SERVICE = ADMIN_CLIENT.getService(AnyTypeService.class);
        RELATIONSHIP_TYPE_SERVICE = ADMIN_CLIENT.getService(RelationshipTypeService.class);
        REALM_SERVICE = ADMIN_CLIENT.getService(RealmService.class);
        ANY_OBJECT_SERVICE = ADMIN_CLIENT.getService(AnyObjectService.class);
        ROLE_SERVICE = ADMIN_CLIENT.getService(RoleService.class);
        DYN_REALM_SERVICE = ADMIN_CLIENT.getService(DynRealmService.class);
        USER_SERVICE = ADMIN_CLIENT.getService(UserService.class);
        USER_SELF_SERVICE = ADMIN_CLIENT.getService(UserSelfService.class);
        USER_REQUEST_SERVICE = ADMIN_CLIENT.getService(UserRequestService.class);
        USER_WORKFLOW_TASK_SERVICE = ADMIN_CLIENT.getService(UserWorkflowTaskService.class);
        GROUP_SERVICE = ADMIN_CLIENT.getService(GroupService.class);
        RESOURCE_SERVICE = ADMIN_CLIENT.getService(ResourceService.class);
        CONNECTOR_SERVICE = ADMIN_CLIENT.getService(ConnectorService.class);
        AUDIT_SERVICE = ADMIN_CLIENT.getService(AuditService.class);
        REPORT_TEMPLATE_SERVICE = ADMIN_CLIENT.getService(ReportTemplateService.class);
        REPORT_SERVICE = ADMIN_CLIENT.getService(ReportService.class);
        TASK_SERVICE = ADMIN_CLIENT.getService(TaskService.class);
        RECONCILIATION_SERVICE = ADMIN_CLIENT.getService(ReconciliationService.class);
        POLICY_SERVICE = ADMIN_CLIENT.getService(PolicyService.class);
        BPMN_PROCESS_SERVICE = ADMIN_CLIENT.getService(BpmnProcessService.class);
        MAIL_TEMPLATE_SERVICE = ADMIN_CLIENT.getService(MailTemplateService.class);
        NOTIFICATION_SERVICE = ADMIN_CLIENT.getService(NotificationService.class);
        SCHEMA_SERVICE = ADMIN_CLIENT.getService(SchemaService.class);
        SECURITY_QUESTION_SERVICE = ADMIN_CLIENT.getService(SecurityQuestionService.class);
        IMPLEMENTATION_SERVICE = ADMIN_CLIENT.getService(ImplementationService.class);
        REMEDIATION_SERVICE = ADMIN_CLIENT.getService(RemediationService.class);
        DELEGATION_SERVICE = ADMIN_CLIENT.getService(DelegationService.class);
        COMMAND_SERVICE = ADMIN_CLIENT.getService(CommandService.class);
        SRA_ROUTE_SERVICE = ADMIN_CLIENT.getService(SRARouteService.class);
        SAML2SP4UI_SERVICE = ADMIN_CLIENT.getService(SAML2SP4UIService.class);
        SAML2SP4UI_IDP_SERVICE = ADMIN_CLIENT.getService(SAML2SP4UIIdPService.class);
        OIDCC4UI_SERVICE = ADMIN_CLIENT.getService(OIDCC4UIService.class);
        OIDCC4UI_PROVIDER_SERVICE = ADMIN_CLIENT.getService(OIDCC4UIProviderService.class);
        SCIM_CONF_SERVICE = ADMIN_CLIENT.getService(SCIMConfService.class);
        CLIENT_APP_SERVICE = ADMIN_CLIENT.getService(ClientAppService.class);
        AUTH_MODULE_SERVICE = ADMIN_CLIENT.getService(AuthModuleService.class);
        ATTR_REPO_SERVICE = ADMIN_CLIENT.getService(AttrRepoService.class);
        SAML2SP_ENTITY_SERVICE = ADMIN_CLIENT.getService(SAML2SPEntityService.class);
        SAML2IDP_ENTITY_SERVICE = ADMIN_CLIENT.getService(SAML2IdPEntityService.class);
        AUTH_PROFILE_SERVICE = ADMIN_CLIENT.getService(AuthProfileService.class);
        OIDC_JWKS_SERVICE = ADMIN_CLIENT.getService(OIDCJWKSService.class);
        WA_CONFIG_SERVICE = ADMIN_CLIENT.getService(WAConfigService.class);
    }

    @BeforeAll
    public static void actuatorInfoSetup() throws IOException {
        JsonNode beans = JSON_MAPPER.readTree(
                (InputStream) WebClient.create(StringUtils.substringBeforeLast(ADDRESS, "/") + "/actuator/beans").
                        accept(MediaType.APPLICATION_JSON).get().getEntity());

        JsonNode uwfAdapter = beans.findValues("uwfAdapter").get(0);
        IS_FLOWABLE_ENABLED = uwfAdapter.get("resource").asText().contains("Flowable");

        JsonNode anySearchDAO = beans.findValues("anySearchDAO").get(0);
        IS_ELASTICSEARCH_ENABLED = anySearchDAO.get("type").asText().contains("Elasticsearch");
    }

    protected static String getUUIDString() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    protected static Attr attr(final String schema, final String value) {
        return new Attr.Builder(schema).value(value).build();
    }

    protected static AttrPatch attrAddReplacePatch(final String schema, final String value) {
        return new AttrPatch.Builder(attr(schema, value)).operation(PatchOperation.ADD_REPLACE).build();
    }

    protected static <T> T getObject(final URI location, final Class<?> serviceClass, final Class<T> resultClass) {
        WebClient webClient = WebClient.fromClient(WebClient.client(ADMIN_CLIENT.getService(serviceClass)));
        webClient.accept(CLIENT_FACTORY.getContentType().getMediaType()).to(location.toASCIIString(), false);

        return webClient.
                header(RESTHeaders.DOMAIN, ADMIN_CLIENT.getDomain()).
                header(HttpHeaders.AUTHORIZATION, "Bearer " + ADMIN_CLIENT.getJWT()).
                get(resultClass);
    }

    @SuppressWarnings("unchecked")
    protected static <T extends SchemaTO> T createSchema(final SchemaType type, final T schemaTO) {
        Response response = SCHEMA_SERVICE.create(type, schemaTO);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            Exception ex = CLIENT_FACTORY.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }

        return (T) getObject(response.getLocation(), SchemaService.class, schemaTO.getClass());
    }

    protected static RoleTO createRole(final RoleTO roleTO) {
        Response response = ROLE_SERVICE.create(roleTO);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            Exception ex = CLIENT_FACTORY.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return getObject(response.getLocation(), RoleService.class, RoleTO.class);
    }

    protected static ReportTO createReport(final ReportTO report) {
        Response response = REPORT_SERVICE.create(report);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatusInfo().getStatusCode());
        return getObject(response.getLocation(), ReportService.class, ReportTO.class);
    }

    protected static Pair<String, String> createNotificationTask(
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

        Response response = NOTIFICATION_SERVICE.create(notification);
        notification = getObject(response.getLocation(), NotificationService.class, NotificationTO.class);
        assertNotNull(notification);

        // 2. create user
        UserCR req = UserITCase.getUniqueSample("notificationtest@syncope.apache.org");
        req.getMemberships().add(new MembershipTO.Builder("bf825fe1-7320-4a54-bd64-143b5c18ab97").build());

        UserTO userTO = createUser(req).getEntity();
        assertNotNull(userTO);
        return Pair.of(notification.getKey(), req.getUsername());
    }

    protected static ProvisioningResult<UserTO> createUser(final UserCR req) {
        Response response = USER_SERVICE.create(req);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            Exception ex = CLIENT_FACTORY.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return response.readEntity(new GenericType<>() {
        });
    }

    protected static ProvisioningResult<UserTO> updateUser(final UserUR req) {
        return USER_SERVICE.update(req).
                readEntity(new GenericType<>() {
                });
    }

    protected static ProvisioningResult<UserTO> updateUser(final UserTO userTO) {
        UserTO before = USER_SERVICE.read(userTO.getKey());
        return USER_SERVICE.update(AnyOperations.diff(userTO, before, false)).
                readEntity(new GenericType<>() {
                });
    }

    protected static ProvisioningResult<UserTO> deleteUser(final String key) {
        return USER_SERVICE.delete(key).
                readEntity(new GenericType<>() {
                });
    }

    protected static ProvisioningResult<AnyObjectTO> createAnyObject(final AnyObjectCR req) {
        Response response = ANY_OBJECT_SERVICE.create(req);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            Exception ex = CLIENT_FACTORY.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return response.readEntity(new GenericType<>() {
        });
    }

    protected static ProvisioningResult<AnyObjectTO> updateAnyObject(final AnyObjectUR req) {
        return ANY_OBJECT_SERVICE.update(req).
                readEntity(new GenericType<>() {
                });
    }

    protected static ProvisioningResult<AnyObjectTO> deleteAnyObject(final String key) {
        return ANY_OBJECT_SERVICE.delete(key).
                readEntity(new GenericType<>() {
                });
    }

    protected static ProvisioningResult<GroupTO> createGroup(final GroupCR req) {
        Response response = GROUP_SERVICE.create(req);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            Exception ex = CLIENT_FACTORY.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return response.readEntity(new GenericType<>() {
        });
    }

    protected static ProvisioningResult<GroupTO> updateGroup(final GroupUR req) {
        return GROUP_SERVICE.update(req).
                readEntity(new GenericType<>() {
                });
    }

    protected static ProvisioningResult<GroupTO> deleteGroup(final String key) {
        return GROUP_SERVICE.delete(key).
                readEntity(new GenericType<>() {
                });
    }

    @SuppressWarnings("unchecked")
    protected static <T extends PolicyTO> T createPolicy(final PolicyType type, final T policy) {
        Response response = POLICY_SERVICE.create(type, policy);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            Exception ex = CLIENT_FACTORY.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return (T) getObject(response.getLocation(), PolicyService.class, policy.getClass());
    }

    protected static ResourceTO createResource(final ResourceTO resourceTO) {
        Response response = RESOURCE_SERVICE.create(resourceTO);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            Exception ex = CLIENT_FACTORY.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return getObject(response.getLocation(), ResourceService.class, ResourceTO.class);
    }

    protected static List<BatchResponseItem> parseBatchResponse(final Response response) throws IOException {
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        return BatchPayloadParser.parse(
                (InputStream) response.getEntity(), response.getMediaType(), new BatchResponseItem());
    }

    @SuppressWarnings({ "unchecked", "rawtypes", "UseOfObsoleteCollectionType" })
    protected static InitialDirContext getLdapResourceDirContext(final String bindDn, final String bindPwd)
            throws NamingException {
        ResourceTO ldapRes = RESOURCE_SERVICE.read(RESOURCE_NAME_LDAP);
        ConnInstanceTO ldapConn = CONNECTOR_SERVICE.read(ldapRes.getConnector(), Locale.ENGLISH.getLanguage());

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

    protected static Object getLdapRemoteObject(final String bindDn, final String bindPwd, final String objectDn) {
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

    protected static void createLdapRemoteObject(
            final String bindDn,
            final String bindPwd,
            final Pair<String, Set<Attribute>> entryAttrs) throws NamingException {

        InitialDirContext ctx = null;
        try {
            ctx = getLdapResourceDirContext(bindDn, bindPwd);

            BasicAttributes entry = new BasicAttributes();
            entryAttrs.getRight().forEach(item -> entry.put(item));

            ctx.createSubcontext(entryAttrs.getLeft(), entry);

        } catch (NamingException e) {
            LOG.error("While creating {} with {}", entryAttrs.getLeft(), entryAttrs.getRight(), e);
            throw e;
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

    protected static void updateLdapRemoteObject(
            final String bindDn,
            final String bindPwd,
            final String objectDn,
            final Map<String, String> attributes) {

        InitialDirContext ctx = null;
        try {
            ctx = getLdapResourceDirContext(bindDn, bindPwd);

            List<ModificationItem> items = new ArrayList<>();
            attributes.forEach((key, value) -> items.add(new ModificationItem(DirContext.REPLACE_ATTRIBUTE,
                    new BasicAttribute(key, value))));

            ctx.modifyAttributes(objectDn, items.toArray(ModificationItem[]::new));
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

    protected static void removeLdapRemoteObject(
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

    protected static <T> T queryForObject(
            final JdbcTemplate jdbcTemplate,
            final int maxWaitSeconds,
            final String sql,
            final Class<T> requiredType,
            final Object... args) {

        AtomicReference<T> object = new AtomicReference<>();
        await().atMost(maxWaitSeconds, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            try {
                object.set(jdbcTemplate.queryForObject(sql, requiredType, args));
                return object.get() != null;
            } catch (Exception e) {
                return false;
            }
        });

        return object.get();
    }

    protected static <T> List<T> queryForList(
            final JdbcTemplate jdbcTemplate,
            final int maxWaitSeconds,
            final String sql,
            final Class<T> requiredType,
            final Object... args) {

        AtomicReference<List<T>> object = new AtomicReference<>();
        await().atMost(maxWaitSeconds, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            try {
                object.set(jdbcTemplate.queryForList(sql, requiredType, args));
                return object.get() != null;
            } catch (Exception e) {
                return false;
            }
        });

        return object.get();
    }

    protected static OIDCRPClientAppTO buildOIDCRP() {
        AuthPolicyTO authPolicyTO = new AuthPolicyTO();
        authPolicyTO.setKey("AuthPolicyTest_" + getUUIDString());
        authPolicyTO.setName("Authentication Policy");
        authPolicyTO = createPolicy(PolicyType.AUTH, authPolicyTO);
        assertNotNull(authPolicyTO);

        AccessPolicyTO accessPolicyTO = new AccessPolicyTO();
        accessPolicyTO.setKey("AccessPolicyTest_" + getUUIDString());
        accessPolicyTO.setName("Access policy");
        accessPolicyTO = createPolicy(PolicyType.ACCESS, accessPolicyTO);
        assertNotNull(accessPolicyTO);

        OIDCRPClientAppTO oidcrpTO = new OIDCRPClientAppTO();
        oidcrpTO.setName("ExampleRP_" + getUUIDString());
        oidcrpTO.setClientAppId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        oidcrpTO.setDescription("Example OIDC RP application");
        oidcrpTO.setClientId("clientId_" + getUUIDString());
        oidcrpTO.setClientSecret("secret");
        oidcrpTO.setSubjectType(OIDCSubjectType.PUBLIC);
        oidcrpTO.getSupportedGrantTypes().add(OIDCGrantType.authorization_code);
        oidcrpTO.getSupportedResponseTypes().add(OIDCResponseType.CODE);

        oidcrpTO.setAuthPolicy(authPolicyTO.getKey());
        oidcrpTO.setAccessPolicy(accessPolicyTO.getKey());

        return oidcrpTO;
    }

    protected static SAML2SPClientAppTO buildSAML2SP() {
        AuthPolicyTO authPolicyTO = new AuthPolicyTO();
        authPolicyTO.setKey("AuthPolicyTest_" + getUUIDString());
        authPolicyTO.setName("Authentication Policy");
        authPolicyTO = createPolicy(PolicyType.AUTH, authPolicyTO);
        assertNotNull(authPolicyTO);

        AccessPolicyTO accessPolicyTO = new AccessPolicyTO();
        accessPolicyTO.setKey("AccessPolicyTest_" + getUUIDString());
        accessPolicyTO.setName("Access policy");
        accessPolicyTO = createPolicy(PolicyType.ACCESS, accessPolicyTO);
        assertNotNull(accessPolicyTO);

        SAML2SPClientAppTO saml2spto = new SAML2SPClientAppTO();
        saml2spto.setName("ExampleSAML2SP_" + getUUIDString());
        saml2spto.setClientAppId(UUID.randomUUID().getMostSignificantBits() & Long.MAX_VALUE);
        saml2spto.setDescription("Example SAML 2.0 service provider");
        saml2spto.setEntityId("SAML2SPEntityId_" + getUUIDString());
        saml2spto.setMetadataLocation("file:./test.xml");
        saml2spto.setRequiredNameIdFormat(SAML2SPNameId.EMAIL_ADDRESS);
        saml2spto.setEncryptionOptional(true);
        saml2spto.setEncryptAssertions(true);

        saml2spto.setAuthPolicy(authPolicyTO.getKey());
        saml2spto.setAccessPolicy(accessPolicyTO.getKey());

        return saml2spto;
    }

    @SuppressWarnings("unchecked")
    protected static <T extends ClientAppTO> T createClientApp(final ClientAppType type, final T clientAppTO) {
        Response response = CLIENT_APP_SERVICE.create(type, clientAppTO);
        if (response.getStatusInfo().getStatusCode() != Response.Status.CREATED.getStatusCode()) {
            Exception ex = CLIENT_FACTORY.getExceptionMapper().fromResponse(response);
            if (ex != null) {
                throw (RuntimeException) ex;
            }
        }
        return (T) getObject(response.getLocation(), ClientAppService.class, clientAppTO.getClass());
    }

    protected static AuthPolicyTO buildAuthPolicyTO(final String authModuleKey) {
        AuthPolicyTO policy = new AuthPolicyTO();
        policy.setName("Test Authentication policy");

        DefaultAuthPolicyConf conf = new DefaultAuthPolicyConf();
        conf.getAuthModules().add(authModuleKey);
        policy.setConf(conf);

        return policy;
    }

    protected static AttrReleasePolicyTO buildAttrReleasePolicyTO() {
        AttrReleasePolicyTO policy = new AttrReleasePolicyTO();
        policy.setName("Test Attribute Release policy");
        policy.setStatus(Boolean.TRUE);

        DefaultAttrReleasePolicyConf conf = new DefaultAttrReleasePolicyConf();
        conf.getAllowedAttrs().addAll(List.of("cn", "givenName"));
        conf.getIncludeOnlyAttrs().add("cn");

        policy.setConf(conf);

        return policy;
    }

    protected static AccessPolicyTO buildAccessPolicyTO() {
        AccessPolicyTO policy = new AccessPolicyTO();
        policy.setName("Test Access policy");
        policy.setEnabled(true);

        DefaultAccessPolicyConf conf = new DefaultAccessPolicyConf();
        conf.getRequiredAttrs().add(new Attr.Builder("cn").values("admin", "Admin", "TheAdmin").build());
        policy.setConf(conf);

        return policy;
    }

    protected static List<AuditEntry> query(final AuditQuery query, final int maxWaitSeconds) {
        if (IS_ELASTICSEARCH_ENABLED) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        int i = 0;
        List<AuditEntry> results = List.of();
        do {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
            results = AUDIT_SERVICE.search(query).getResult();
            i++;
        } while (results.isEmpty() && i < maxWaitSeconds);
        return results;
    }

    @Autowired
    protected ConfParamOps confParamOps;

    @Autowired
    protected ServiceOps serviceOps;

    @Autowired
    protected DomainOps domainOps;

    @Autowired
    protected DataSource testDataSource;

}
