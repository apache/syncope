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
import com.unboundid.ldap.sdk.AddRequest;
import com.unboundid.ldap.sdk.Attribute;
import com.unboundid.ldap.sdk.LDAPConnection;
import com.unboundid.ldap.sdk.LDAPException;
import com.unboundid.ldap.sdk.Modification;
import com.unboundid.ldap.sdk.ModificationType;
import com.unboundid.ldap.sdk.SearchRequest;
import com.unboundid.ldap.sdk.SearchResult;
import com.unboundid.ldap.sdk.SearchResultEntry;
import com.unboundid.ldap.sdk.SearchScope;
import jakarta.ws.rs.core.GenericType;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.syncope.client.lib.SyncopeAnonymousClient;
import org.apache.syncope.client.lib.SyncopeClient;
import org.apache.syncope.client.lib.SyncopeClientFactoryBean;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.ServiceOps;
import org.apache.syncope.common.keymaster.client.self.SelfKeymasterClientContext;
import org.apache.syncope.common.keymaster.client.zookeeper.ZookeeperKeymasterClientContext;
import org.apache.syncope.common.lib.Attr;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.policy.AccessPolicyTO;
import org.apache.syncope.common.lib.policy.AttrReleasePolicyTO;
import org.apache.syncope.common.lib.policy.AuthPolicyTO;
import org.apache.syncope.common.lib.policy.DefaultAccessPolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAttrReleasePolicyConf;
import org.apache.syncope.common.lib.policy.DefaultAuthPolicyConf;
import org.apache.syncope.common.lib.policy.DefaultTicketExpirationPolicyConf;
import org.apache.syncope.common.lib.policy.PolicyTO;
import org.apache.syncope.common.lib.policy.TicketExpirationPolicyTO;
import org.apache.syncope.common.lib.request.AnyObjectCR;
import org.apache.syncope.common.lib.request.AnyObjectUR;
import org.apache.syncope.common.lib.request.AttrPatch;
import org.apache.syncope.common.lib.request.GroupCR;
import org.apache.syncope.common.lib.request.GroupUR;
import org.apache.syncope.common.lib.request.UserCR;
import org.apache.syncope.common.lib.request.UserUR;
import org.apache.syncope.common.lib.to.AnyObjectTO;
import org.apache.syncope.common.lib.to.AuditEventTO;
import org.apache.syncope.common.lib.to.ClientAppTO;
import org.apache.syncope.common.lib.to.ConnInstanceTO;
import org.apache.syncope.common.lib.to.ExecTO;
import org.apache.syncope.common.lib.to.GroupTO;
import org.apache.syncope.common.lib.to.ImplementationTO;
import org.apache.syncope.common.lib.to.MembershipTO;
import org.apache.syncope.common.lib.to.NotificationTO;
import org.apache.syncope.common.lib.to.OIDCRPClientAppTO;
import org.apache.syncope.common.lib.to.ProvisioningResult;
import org.apache.syncope.common.lib.to.RealmTO;
import org.apache.syncope.common.lib.to.ReportTO;
import org.apache.syncope.common.lib.to.ResourceTO;
import org.apache.syncope.common.lib.to.RoleTO;
import org.apache.syncope.common.lib.to.SAML2SPClientAppTO;
import org.apache.syncope.common.lib.to.SchedTaskTO;
import org.apache.syncope.common.lib.to.SchemaTO;
import org.apache.syncope.common.lib.to.UserTO;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.ClientAppType;
import org.apache.syncope.common.lib.types.ExecStatus;
import org.apache.syncope.common.lib.types.IdRepoImplementationType;
import org.apache.syncope.common.lib.types.ImplementationEngine;
import org.apache.syncope.common.lib.types.OIDCGrantType;
import org.apache.syncope.common.lib.types.OIDCResponseType;
import org.apache.syncope.common.lib.types.OIDCSubjectType;
import org.apache.syncope.common.lib.types.PatchOperation;
import org.apache.syncope.common.lib.types.PolicyType;
import org.apache.syncope.common.lib.types.SAML2SPNameId;
import org.apache.syncope.common.lib.types.SchemaType;
import org.apache.syncope.common.lib.types.TaskType;
import org.apache.syncope.common.lib.types.TraceLevel;
import org.apache.syncope.common.rest.api.RESTHeaders;
import org.apache.syncope.common.rest.api.batch.BatchPayloadParser;
import org.apache.syncope.common.rest.api.batch.BatchResponseItem;
import org.apache.syncope.common.rest.api.beans.AuditQuery;
import org.apache.syncope.common.rest.api.beans.RealmQuery;
import org.apache.syncope.common.rest.api.beans.TaskQuery;
import org.apache.syncope.common.rest.api.service.AnyObjectService;
import org.apache.syncope.common.rest.api.service.AnyTypeClassService;
import org.apache.syncope.common.rest.api.service.AnyTypeService;
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
import org.apache.syncope.common.rest.api.service.wa.MfaTrustStorageService;
import org.apache.syncope.common.rest.api.service.wa.WAConfigService;
import org.apache.syncope.common.rest.api.service.wa.WebAuthnRegistrationService;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.spring.security.DefaultEncryptorManager;
import org.apache.syncope.fit.AbstractITCase.KeymasterInitializer;
import org.apache.syncope.fit.core.AbstractTaskITCase;
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
import org.springframework.util.function.ThrowingFunction;

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

    protected static final String RESOURCE_NAME_KAFKA = "resource-kafka";

    protected static final String PRINTER = "PRINTER";

    protected static final int MAX_WAIT_SECONDS = 50;

    protected static String ANONYMOUS_UNAME;

    protected static String ANONYMOUS_KEY;

    protected static String JWS_KEY;

    protected static String JWT_ISSUER;

    protected static JWSAlgorithm JWS_ALGORITHM;

    protected static SyncopeClientFactoryBean CLIENT_FACTORY;

    protected static SyncopeClient ADMIN_CLIENT;

    protected static SyncopeAnonymousClient ANONYMOUS_CLIENT;

    protected static SyncopeService SYNCOPE_SERVICE;

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

    protected static MfaTrustStorageService MFA_TRUST_STORAGE_SERVICE;

    protected static WebAuthnRegistrationService WEBAUTHN_REGISTRATION_SERVICE;

    protected static ImpersonationService IMPERSONATION_SERVICE;

    protected static boolean IS_FLOWABLE_ENABLED = false;

    protected static boolean IS_ELASTICSEARCH_ENABLED = false;

    protected static boolean IS_OPENSEARCH_ENABLED = false;

    protected static boolean IS_EXT_SEARCH_ENABLED = false;

    protected static boolean IS_NEO4J_PERSISTENCE = false;

    private static void initExtSearch(
            final ImplementationService implementationService,
            final TaskService taskService,
            final String delegateClass) {

        String delegateKey = StringUtils.substringAfterLast(delegateClass, ".");

        List<SchedTaskTO> schedTasks = taskService.<SchedTaskTO>search(
                new TaskQuery.Builder(TaskType.SCHEDULED).build()).getResult();
        if (schedTasks.stream().anyMatch(t -> delegateKey.equals(t.getJobDelegate()))) {
            return;
        }

        ImplementationTO delegate = null;
        try {
            delegate = implementationService.read(IdRepoImplementationType.TASKJOB_DELEGATE, delegateKey);
        } catch (SyncopeClientException e) {
            if (e.getType().getResponseStatus() == Response.Status.NOT_FOUND) {
                delegate = new ImplementationTO();
                delegate.setKey(delegateKey);
                delegate.setEngine(ImplementationEngine.JAVA);
                delegate.setType(IdRepoImplementationType.TASKJOB_DELEGATE);
                delegate.setBody(delegateClass);
                Response response = implementationService.create(delegate);
                delegate = implementationService.read(
                        delegate.getType(), response.getHeaderString(RESTHeaders.RESOURCE_KEY));
                assertNotNull(delegate);
            }
        }
        assertNotNull(delegate);

        SchedTaskTO schedTask = new SchedTaskTO();
        schedTask.setJobDelegate(delegate.getKey());
        schedTask.setName(delegateKey);

        Response response = taskService.create(TaskType.SCHEDULED, schedTask);

        ExecTO exec = AbstractTaskITCase.execSchedTask(
                taskService, TaskType.SCHEDULED, response.getHeaderString(RESTHeaders.RESOURCE_KEY),
                MAX_WAIT_SECONDS, false);
        assertEquals(ExecStatus.SUCCESS, ExecStatus.valueOf(exec.getStatus()));
    }

    @BeforeAll
    public static void anonymousSetup() throws IOException {
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

        ANONYMOUS_CLIENT = CLIENT_FACTORY.createAnonymous(ANONYMOUS_UNAME, ANONYMOUS_KEY);

        GOOGLE_MFA_AUTH_TOKEN_SERVICE = ANONYMOUS_CLIENT.getService(GoogleMfaAuthTokenService.class);
        GOOGLE_MFA_AUTH_ACCOUNT_SERVICE = ANONYMOUS_CLIENT.getService(GoogleMfaAuthAccountService.class);
        MFA_TRUST_STORAGE_SERVICE = ANONYMOUS_CLIENT.getService(MfaTrustStorageService.class);
        WEBAUTHN_REGISTRATION_SERVICE = ANONYMOUS_CLIENT.getService(WebAuthnRegistrationService.class);
        IMPERSONATION_SERVICE = ANONYMOUS_CLIENT.getService(ImpersonationService.class);

        String beansJSON = await().atMost(10, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            try {
                return WebClient.create(StringUtils.substringBeforeLast(ADDRESS, "/") + "/actuator/beans",
                        ANONYMOUS_UNAME,
                        ANONYMOUS_KEY,
                        null).
                        accept(MediaType.APPLICATION_JSON).get().readEntity(String.class);
            } catch (Exception e) {
                return null;
            }
        }, Objects::nonNull);
        JsonNode beans = JSON_MAPPER.readTree(beansJSON);

        JsonNode uwfAdapter = beans.findValues("uwfAdapter").getFirst();
        IS_FLOWABLE_ENABLED = uwfAdapter.get("resource").asText().contains("Flowable");

        JsonNode anySearchDAO = beans.findValues("anySearchDAO").getFirst();
        IS_ELASTICSEARCH_ENABLED = anySearchDAO.get("type").asText().contains("Elasticsearch");
        IS_OPENSEARCH_ENABLED = anySearchDAO.get("type").asText().contains("OpenSearch");
        IS_EXT_SEARCH_ENABLED = IS_ELASTICSEARCH_ENABLED || IS_OPENSEARCH_ENABLED;

        IS_NEO4J_PERSISTENCE = anySearchDAO.get("type").asText().contains("Neo4j");

        if (!IS_EXT_SEARCH_ENABLED) {
            return;
        }

        SyncopeClientFactoryBean masterCF = new SyncopeClientFactoryBean().setAddress(ADDRESS);
        SyncopeClientFactoryBean twoCF = new SyncopeClientFactoryBean().setAddress(ADDRESS).setDomain("Two");
        String envContentType = System.getProperty(ENV_KEY_CONTENT_TYPE);
        if (StringUtils.isNotBlank(envContentType)) {
            masterCF.setContentType(envContentType);
            twoCF.setContentType(envContentType);
        }
        SyncopeClient masterSC = masterCF.create(ADMIN_UNAME, ADMIN_PWD);
        ImplementationService masterIS = masterSC.getService(ImplementationService.class);
        TaskService masterTS = masterSC.getService(TaskService.class);
        SyncopeClient twoSC = twoCF.create(ADMIN_UNAME, "password2");
        ImplementationService twoIS = twoSC.getService(ImplementationService.class);
        TaskService twoTS = twoSC.getService(TaskService.class);

        if (IS_ELASTICSEARCH_ENABLED) {
            initExtSearch(masterIS, masterTS, "org.apache.syncope.core.provisioning.java.job.ElasticsearchReindex");
            initExtSearch(twoIS, twoTS, "org.apache.syncope.core.provisioning.java.job.ElasticsearchReindex");
        } else if (IS_OPENSEARCH_ENABLED) {
            initExtSearch(masterIS, masterTS, "org.apache.syncope.core.provisioning.java.job.OpenSearchReindex");
            initExtSearch(twoIS, twoTS, "org.apache.syncope.core.provisioning.java.job.OpenSearchReindex");
        }
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

    private static <T> T execOnLDAP(
            final String bindDn,
            final String bindPassword,
            final ConnInstanceTO connInstance,
            final ThrowingFunction<LDAPConnection, T> function) throws LDAPException {

        try (LDAPConnection ldapConn = new LDAPConnection(
                connInstance.getConf("host").orElseThrow().getValues().getFirst().toString(),
                Integer.parseInt(connInstance.getConf("port").orElseThrow().getValues().getFirst().toString()),
                bindDn,
                bindPassword)) {

            return function.apply(ldapConn);
        }
    }

    private static <T> T execOnLDAP(
            final String bindDn,
            final String bindPassword,
            final ThrowingFunction<LDAPConnection, T> function) throws LDAPException {

        ConnInstanceTO connInstance = CONNECTOR_SERVICE.read("74141a3b-0762-4720-a4aa-fc3e374ef3ef", null);

        return execOnLDAP(bindDn, bindPassword, connInstance, function);
    }

    private static <T> T execOnLDAP(final ThrowingFunction<LDAPConnection, T> function) throws LDAPException {
        ConnInstanceTO connInstance = CONNECTOR_SERVICE.read("74141a3b-0762-4720-a4aa-fc3e374ef3ef", null);

        return execOnLDAP(
                connInstance.getConf("principal").orElseThrow().getValues().getFirst().toString(),
                connInstance.getConf("credentials").orElseThrow().getValues().getFirst().toString(),
                connInstance,
                function);
    }

    protected static SearchResult ldapSearch(final String baseDn, final String filter) {
        try {
            return execOnLDAP(ldapConn -> ldapConn.search(
                    new SearchRequest(baseDn, SearchScope.SUB, filter)));
        } catch (LDAPException e) {
            LOG.error("While searching from {} with filter {}", baseDn, filter, e);
            return new SearchResult(e);
        }
    }

    protected static SearchResultEntry getLdapRemoteObject(final String objectDn) {
        try {
            return execOnLDAP(ldapConn -> ldapConn.searchForEntry(
                    new SearchRequest(objectDn, SearchScope.BASE, "objectClass=*")));
        } catch (LDAPException e) {
            LOG.error("While reading {}", objectDn, e);
            return null;
        }
    }

    protected static SearchResultEntry getLdapRemoteObject(
            final String bindDn,
            final String bindPassword,
            final String objectDn) {

        try {
            return execOnLDAP(bindDn, bindPassword, ldapConn -> ldapConn.searchForEntry(
                    new SearchRequest(objectDn, SearchScope.BASE, "objectClass=*")));
        } catch (LDAPException e) {
            LOG.error("While reading {}", objectDn, e);
            return null;
        }
    }

    protected static void createLdapRemoteObject(final String objectDn, final Map<String, String> attributes) {
        try {
            execOnLDAP(ldapConn -> {
                List<Attribute> attrs = new ArrayList<>();
                attributes.forEach((key, value) -> attrs.add(new Attribute(key, value)));

                ldapConn.add(new AddRequest(objectDn, attrs));

                return null;
            });
        } catch (LDAPException e) {
            LOG.error("While creating {} with {}", objectDn, attributes, e);
        }
    }

    protected static void updateLdapRemoteObject(final String objectDn, final Map<String, String> attributes) {
        try {
            execOnLDAP(ldapConn -> {
                List<Modification> modifications = new ArrayList<>();
                attributes.forEach((key, value) -> modifications.add(
                        value == null
                                ? new Modification(ModificationType.DELETE, key)
                                : new Modification(ModificationType.REPLACE, key, value)));

                ldapConn.modify(objectDn, modifications);

                return null;
            });
        } catch (LDAPException e) {
            LOG.error("While updating {} with {}", objectDn, attributes, e);
        }
    }

    protected static void removeLdapRemoteObject(final String objectDn) {
        try {
            execOnLDAP(ldapConn -> ldapConn.delete(objectDn));
        } catch (LDAPException e) {
            LOG.error("While removing {}", objectDn, e);
        }
    }

    protected static <T> T queryForObject(
            final JdbcTemplate jdbcTemplate,
            final int maxWaitSeconds,
            final String sql,
            final Class<T> requiredType,
            final Object... args) {

        Mutable<T> object = new MutableObject<>();
        await().atMost(maxWaitSeconds, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            try {
                object.setValue(jdbcTemplate.queryForObject(sql, requiredType, args));
                return object.getValue() != null;
            } catch (Exception e) {
                return false;
            }
        });

        return object.getValue();
    }

    protected static <T> List<T> queryForList(
            final JdbcTemplate jdbcTemplate,
            final int maxWaitSeconds,
            final String sql,
            final Class<T> requiredType,
            final Object... args) {

        Mutable<List<T>> object = new MutableObject<>();
        await().atMost(maxWaitSeconds, TimeUnit.SECONDS).pollInterval(1, TimeUnit.SECONDS).until(() -> {
            try {
                object.setValue(jdbcTemplate.queryForList(sql, requiredType, args));
                return object.getValue() != null;
            } catch (Exception e) {
                return false;
            }
        });

        return object.getValue();
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
        conf.getReleaseAttrs().put("uid", "username");
        conf.getReleaseAttrs().put("cn", "fullname");
        conf.getAllowedAttrs().addAll(List.of("cn", "givenName"));
        conf.getIncludeOnlyAttrs().add("cn");

        policy.setConf(conf);

        return policy;
    }

    protected static AccessPolicyTO buildAccessPolicyTO() {
        AccessPolicyTO policy = new AccessPolicyTO();
        policy.setName("Test Access policy");

        DefaultAccessPolicyConf conf = new DefaultAccessPolicyConf();
        conf.setEnabled(true);
        conf.getRequiredAttrs().put("cn", "admin,Admin,TheAdmin");
        policy.setConf(conf);

        return policy;
    }

    protected static TicketExpirationPolicyTO buildTicketExpirationPolicyTO() {
        TicketExpirationPolicyTO policy = new TicketExpirationPolicyTO();
        policy.setName("Test Ticket Expiration policy");

        DefaultTicketExpirationPolicyConf conf = new DefaultTicketExpirationPolicyConf();
        DefaultTicketExpirationPolicyConf.TGTConf tgtConf = new DefaultTicketExpirationPolicyConf.TGTConf();
        tgtConf.setMaxTimeToLiveInSeconds(110);
        conf.setTgtConf(tgtConf);
        DefaultTicketExpirationPolicyConf.STConf stConf = new DefaultTicketExpirationPolicyConf.STConf();
        stConf.setMaxTimeToLiveInSeconds(0);
        stConf.setNumberOfUses(1);
        conf.setStConf(stConf);
        policy.setConf(conf);

        return policy;
    }

    protected static List<AuditEventTO> query(final AuditQuery query, final int maxWaitSeconds) {
        if (IS_EXT_SEARCH_ENABLED) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException ex) {
                // ignore
            }
        }

        int i = 0;
        List<AuditEventTO> results = List.of();
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

    protected static Optional<RealmTO> getRealm(final String fullPath) {
        return REALM_SERVICE.search(new RealmQuery.Builder().base(fullPath).build()).getResult().stream().
                filter(realm -> fullPath.equals(realm.getFullPath())).findFirst();
    }

    @Autowired
    protected ConfParamOps confParamOps;

    @Autowired
    protected ServiceOps serviceOps;

    @Autowired
    protected DomainOps domainOps;

    @Autowired
    protected DataSource testDataSource;

    protected final EncryptorManager encryptorManager = new DefaultEncryptorManager();

}
