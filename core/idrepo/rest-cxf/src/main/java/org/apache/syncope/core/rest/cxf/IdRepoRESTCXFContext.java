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
package org.apache.syncope.core.rest.cxf;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.fasterxml.jackson.jaxrs.xml.JacksonXMLProvider;
import com.fasterxml.jackson.jaxrs.yaml.JacksonYAMLProvider;
import io.swagger.v3.oas.models.security.SecurityScheme;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletRequestListener;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.ext.ContextProvider;
import org.apache.cxf.jaxrs.ext.search.SearchContext;
import org.apache.cxf.jaxrs.ext.search.SearchContextImpl;
import org.apache.cxf.jaxrs.ext.search.SearchContextProvider;
import org.apache.cxf.jaxrs.ext.search.SearchUtils;
import org.apache.cxf.jaxrs.openapi.OpenApiFeature;
import org.apache.cxf.jaxrs.spring.JAXRSServerFactoryBeanDefinitionParser.SpringJAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.validation.JAXRSBeanValidationInInterceptor;
import org.apache.cxf.transport.common.gzip.GZIPInInterceptor;
import org.apache.cxf.transport.common.gzip.GZIPOutInterceptor;
import org.apache.cxf.validation.BeanValidationProvider;
import org.apache.syncope.common.lib.jackson.SyncopeJsonMapper;
import org.apache.syncope.common.lib.jackson.SyncopeXmlMapper;
import org.apache.syncope.common.lib.jackson.SyncopeYAMLMapper;
import org.apache.syncope.common.lib.search.SyncopeFiqlParser;
import org.apache.syncope.common.rest.api.DateParamConverterProvider;
import org.apache.syncope.common.rest.api.service.AccessTokenService;
import org.apache.syncope.common.rest.api.service.AnyObjectService;
import org.apache.syncope.common.rest.api.service.AnyTypeClassService;
import org.apache.syncope.common.rest.api.service.AnyTypeService;
import org.apache.syncope.common.rest.api.service.ApplicationService;
import org.apache.syncope.common.rest.api.service.AuditService;
import org.apache.syncope.common.rest.api.service.DelegationService;
import org.apache.syncope.common.rest.api.service.DynRealmService;
import org.apache.syncope.common.rest.api.service.GroupService;
import org.apache.syncope.common.rest.api.service.ImplementationService;
import org.apache.syncope.common.rest.api.service.MailTemplateService;
import org.apache.syncope.common.rest.api.service.NotificationService;
import org.apache.syncope.common.rest.api.service.PolicyService;
import org.apache.syncope.common.rest.api.service.RealmService;
import org.apache.syncope.common.rest.api.service.RelationshipTypeService;
import org.apache.syncope.common.rest.api.service.ReportService;
import org.apache.syncope.common.rest.api.service.ReportTemplateService;
import org.apache.syncope.common.rest.api.service.RoleService;
import org.apache.syncope.common.rest.api.service.SchemaService;
import org.apache.syncope.common.rest.api.service.SecurityQuestionService;
import org.apache.syncope.common.rest.api.service.SyncopeService;
import org.apache.syncope.common.rest.api.service.TaskService;
import org.apache.syncope.common.rest.api.service.UserSelfService;
import org.apache.syncope.common.rest.api.service.UserService;
import org.apache.syncope.core.logic.AccessTokenLogic;
import org.apache.syncope.core.logic.AnyObjectLogic;
import org.apache.syncope.core.logic.AnyTypeClassLogic;
import org.apache.syncope.core.logic.AnyTypeLogic;
import org.apache.syncope.core.logic.ApplicationLogic;
import org.apache.syncope.core.logic.AuditLogic;
import org.apache.syncope.core.logic.DelegationLogic;
import org.apache.syncope.core.logic.DynRealmLogic;
import org.apache.syncope.core.logic.GroupLogic;
import org.apache.syncope.core.logic.ImplementationLogic;
import org.apache.syncope.core.logic.MailTemplateLogic;
import org.apache.syncope.core.logic.NotificationLogic;
import org.apache.syncope.core.logic.PolicyLogic;
import org.apache.syncope.core.logic.RealmLogic;
import org.apache.syncope.core.logic.RelationshipTypeLogic;
import org.apache.syncope.core.logic.ReportLogic;
import org.apache.syncope.core.logic.ReportTemplateLogic;
import org.apache.syncope.core.logic.RoleLogic;
import org.apache.syncope.core.logic.SchemaLogic;
import org.apache.syncope.core.logic.SecurityQuestionLogic;
import org.apache.syncope.core.logic.SyncopeLogic;
import org.apache.syncope.core.logic.TaskLogic;
import org.apache.syncope.core.logic.UserLogic;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.BatchDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.rest.cxf.service.AccessTokenServiceImpl;
import org.apache.syncope.core.rest.cxf.service.AnyObjectServiceImpl;
import org.apache.syncope.core.rest.cxf.service.AnyTypeClassServiceImpl;
import org.apache.syncope.core.rest.cxf.service.AnyTypeServiceImpl;
import org.apache.syncope.core.rest.cxf.service.ApplicationServiceImpl;
import org.apache.syncope.core.rest.cxf.service.AuditServiceImpl;
import org.apache.syncope.core.rest.cxf.service.DelegationServiceImpl;
import org.apache.syncope.core.rest.cxf.service.DynRealmServiceImpl;
import org.apache.syncope.core.rest.cxf.service.GroupServiceImpl;
import org.apache.syncope.core.rest.cxf.service.ImplementationServiceImpl;
import org.apache.syncope.core.rest.cxf.service.MailTemplateServiceImpl;
import org.apache.syncope.core.rest.cxf.service.NotificationServiceImpl;
import org.apache.syncope.core.rest.cxf.service.PolicyServiceImpl;
import org.apache.syncope.core.rest.cxf.service.RealmServiceImpl;
import org.apache.syncope.core.rest.cxf.service.RelationshipTypeServiceImpl;
import org.apache.syncope.core.rest.cxf.service.ReportServiceImpl;
import org.apache.syncope.core.rest.cxf.service.ReportTemplateServiceImpl;
import org.apache.syncope.core.rest.cxf.service.RoleServiceImpl;
import org.apache.syncope.core.rest.cxf.service.SchemaServiceImpl;
import org.apache.syncope.core.rest.cxf.service.SecurityQuestionServiceImpl;
import org.apache.syncope.core.rest.cxf.service.SyncopeServiceImpl;
import org.apache.syncope.core.rest.cxf.service.TaskServiceImpl;
import org.apache.syncope.core.rest.cxf.service.UserSelfServiceImpl;
import org.apache.syncope.core.rest.cxf.service.UserServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.web.servlet.ServletListenerRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@PropertySource("classpath:errorMessages.properties")
@Configuration(proxyBeanMethods = false)
public class IdRepoRESTCXFContext {

    @ConditionalOnMissingBean
    @Bean
    public ThreadPoolTaskExecutor batchExecutor() {
        ThreadPoolTaskExecutor batchExecutor = new ThreadPoolTaskExecutor();
        batchExecutor.setCorePoolSize(10);
        batchExecutor.setThreadNamePrefix("Batch-");
        batchExecutor.initialize();
        return batchExecutor;
    }

    @ConditionalOnMissingBean
    @Bean
    public DateParamConverterProvider dateParamConverterProvider() {
        return new DateParamConverterProvider();
    }

    @ConditionalOnMissingBean
    @Bean
    public JacksonJsonProvider jsonProvider() {
        return new JacksonJsonProvider(new SyncopeJsonMapper());
    }

    @ConditionalOnMissingBean
    @Bean
    public JacksonXMLProvider xmlProvider() {
        return new JacksonXMLProvider(new SyncopeXmlMapper());
    }

    @ConditionalOnMissingBean
    @Bean
    public JacksonYAMLProvider yamlProvider() {
        return new JacksonYAMLProvider(new SyncopeYAMLMapper());
    }

    @ConditionalOnMissingBean
    @Bean
    public BeanValidationProvider validationProvider() {
        return new BeanValidationProvider();
    }

    @ConditionalOnMissingBean
    @Bean
    public JAXRSBeanValidationInInterceptor validationInInterceptor(final BeanValidationProvider validationProvider) {
        JAXRSBeanValidationInInterceptor validationInInterceptor = new JAXRSBeanValidationInInterceptor();
        validationInInterceptor.setProvider(validationProvider);
        return validationInInterceptor;
    }

    @ConditionalOnMissingBean
    @Bean
    public GZIPInInterceptor gzipInInterceptor() {
        return new GZIPInInterceptor();
    }

    @ConditionalOnMissingBean
    @Bean
    public GZIPOutInterceptor gzipOutInterceptor() {
        GZIPOutInterceptor gzipOutInterceptor = new GZIPOutInterceptor();
        gzipOutInterceptor.setThreshold(0);
        gzipOutInterceptor.setForce(true);
        return gzipOutInterceptor;
    }

    @ConditionalOnMissingBean
    @Bean
    public RestServiceExceptionMapper restServiceExceptionMapper(final Environment env) {
        return new RestServiceExceptionMapper(env);
    }

    @ConditionalOnMissingBean
    @Bean
    public ContextProvider<SearchContext> searchContextProvider() {
        return new SearchContextProvider();
    }

    @ConditionalOnMissingBean
    @Bean
    public CheckDomainFilter checkDomainFilter(final DomainHolder domainHolder) {
        return new CheckDomainFilter(domainHolder);
    }

    @ConditionalOnMissingBean
    @Bean
    public AddDomainFilter addDomainFilter() {
        return new AddDomainFilter();
    }

    @ConditionalOnMissingBean
    @Bean
    public AddETagFilter addETagFilter() {
        return new AddETagFilter();
    }

    @ConditionalOnMissingBean
    @Bean
    public OpenApiFeature openapiFeature(final ApplicationContext ctx) {
        String version = ctx.getEnvironment().getProperty("version");
        OpenApiFeature openapiFeature = new OpenApiFeature();
        openapiFeature.setTitle("Apache Syncope");
        openapiFeature.setVersion(version);
        openapiFeature.setDescription("Apache Syncope " + version);
        openapiFeature.setContactName("The Apache Syncope community");
        openapiFeature.setContactEmail("dev@syncope.apache.org");
        openapiFeature.setContactUrl("https://syncope.apache.org");
        openapiFeature.setScan(false);
        openapiFeature.setResourcePackages(Set.of("org.apache.syncope.common.rest.api.service"));

        SyncopeOpenApiCustomizer openApiCustomizer = new SyncopeOpenApiCustomizer(ctx.getEnvironment());
        openApiCustomizer.setDynamicBasePath(false);
        openApiCustomizer.setReplaceTags(false);
        openapiFeature.setCustomizer(openApiCustomizer);

        Map<String, SecurityScheme> securityDefinitions = new HashMap<>();
        SecurityScheme basicAuth = new SecurityScheme();
        basicAuth.setType(SecurityScheme.Type.HTTP);
        basicAuth.setScheme("basic");
        securityDefinitions.put("BasicAuthentication", basicAuth);
        SecurityScheme bearer = new SecurityScheme();
        bearer.setType(SecurityScheme.Type.HTTP);
        bearer.setScheme("bearer");
        bearer.setBearerFormat("JWT");
        securityDefinitions.put("Bearer", bearer);
        openapiFeature.setSecurityDefinitions(securityDefinitions);

        return openapiFeature;
    }

    @ConditionalOnMissingBean
    @Bean
    public Server restContainer(
            final AddETagFilter addETagFilter,
            final AddDomainFilter addDomainFilter,
            final ContextProvider<SearchContext> searchContextProvider,
            final JacksonYAMLProvider yamlProvider,
            final JacksonXMLProvider xmlProvider,
            final JacksonJsonProvider jsonProvider,
            final DateParamConverterProvider dateParamConverterProvider,
            final JAXRSBeanValidationInInterceptor validationInInterceptor,
            final GZIPInInterceptor gzipInInterceptor,
            final GZIPOutInterceptor gzipOutInterceptor,
            final OpenApiFeature openapiFeature,
            final Bus bus,
            final ApplicationContext ctx,
            final CheckDomainFilter checkDomainFilter,
            final RestServiceExceptionMapper restServiceExceptionMapper) {

        SpringJAXRSServerFactoryBean restContainer = new SpringJAXRSServerFactoryBean();
        restContainer.setBus(bus);
        restContainer.setAddress("/");
        restContainer.setStaticSubresourceResolution(true);
        restContainer.setBasePackages(List.of(
                "org.apache.syncope.common.rest.api.service",
                "org.apache.syncope.core.rest.cxf.service"));

        Map<String, Object> properties = new HashMap<>();
        properties.put(SearchContextImpl.CUSTOM_SEARCH_PARSER_CLASS_PROPERTY, SyncopeFiqlParser.class.getName());
        properties.put(SearchUtils.LAX_PROPERTY_MATCH, "true");
        properties.put("convert.wadl.resources.to.dom", "false");
        restContainer.setProperties(properties);

        restContainer.setProviders(List.of(
                dateParamConverterProvider,
                jsonProvider,
                xmlProvider,
                yamlProvider,
                restServiceExceptionMapper,
                searchContextProvider,
                checkDomainFilter,
                addDomainFilter,
                addETagFilter));

        restContainer.setInInterceptors(List.of(
                gzipInInterceptor,
                validationInInterceptor));

        restContainer.setOutInterceptors(List.of(gzipOutInterceptor));

        restContainer.setFeatures(List.of(openapiFeature));

        restContainer.setApplicationContext(ctx);
        return restContainer.create();
    }

    @ConditionalOnMissingBean
    @Bean
    public ServletListenerRegistrationBean<ServletRequestListener> listenerRegistrationBean() {
        ServletListenerRegistrationBean<ServletRequestListener> bean = new ServletListenerRegistrationBean<>();
        bean.setListener(new ThreadLocalCleanupListener());
        return bean;
    }

    @ConditionalOnMissingBean
    @Bean
    public AccessTokenService accessTokenService(final AccessTokenLogic accessTokenLogic) {
        return new AccessTokenServiceImpl(accessTokenLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyObjectService anyObjectService(final AnyObjectDAO anyObjectDAO, final AnyObjectLogic anyObjectLogic,
            final SearchCondVisitor searchCondVisitor) {
        return new AnyObjectServiceImpl(searchCondVisitor, anyObjectDAO, anyObjectLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyTypeClassService anyTypeClassService(final AnyTypeClassLogic anyTypeClassLogic) {
        return new AnyTypeClassServiceImpl(anyTypeClassLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyTypeService anyTypeService(final AnyTypeLogic anyTypeLogic) {
        return new AnyTypeServiceImpl(anyTypeLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public ApplicationService applicationService(final ApplicationLogic applicationLogic) {
        return new ApplicationServiceImpl(applicationLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public AuditService auditService(final AuditLogic auditLogic) {
        return new AuditServiceImpl(auditLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public DelegationService delegationService(final DelegationLogic delegationLogic) {
        return new DelegationServiceImpl(delegationLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public DynRealmService dynRealmService(final DynRealmLogic dynRealmLogic) {
        return new DynRealmServiceImpl(dynRealmLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public GroupService groupService(final GroupDAO groupDAO, final GroupLogic groupLogic,
            final SearchCondVisitor searchCondVisitor) {
        return new GroupServiceImpl(searchCondVisitor, groupDAO, groupLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public ImplementationService implementationService(final ImplementationLogic implementationLogic) {
        return new ImplementationServiceImpl(implementationLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public MailTemplateService mailTemplateService(final MailTemplateLogic mailTemplateLogic) {
        return new MailTemplateServiceImpl(mailTemplateLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public NotificationService notificationService(final NotificationLogic notificationLogic) {
        return new NotificationServiceImpl(notificationLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public PolicyService policyService(final PolicyLogic policyLogic) {
        return new PolicyServiceImpl(policyLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public RealmService realmService(final RealmLogic realmLogic) {
        return new RealmServiceImpl(realmLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public RelationshipTypeService relationshipTypeService(final RelationshipTypeLogic relationshipTypeLogic) {
        return new RelationshipTypeServiceImpl(relationshipTypeLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public ReportService reportService(final ReportLogic reportLogic) {
        return new ReportServiceImpl(reportLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public ReportTemplateService reportTemplateService(final ReportTemplateLogic reportTemplateLogic) {
        return new ReportTemplateServiceImpl(reportTemplateLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public RoleService roleService(final RoleLogic roleLogic) {
        return new RoleServiceImpl(roleLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public SchemaService schemaService(final SchemaLogic schemaLogic) {
        return new SchemaServiceImpl(schemaLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public SecurityQuestionService securityQuestionService(final SecurityQuestionLogic securityQuestionLogic) {
        return new SecurityQuestionServiceImpl(securityQuestionLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public SyncopeService syncopeService(
            final Bus bus,
            final SyncopeLogic syncopeLogic,
            final ThreadPoolTaskExecutor batchExecutor,
            final BatchDAO batchDAO,
            final EntityFactory entityFactory) {

        return new SyncopeServiceImpl(syncopeLogic, batchExecutor, bus, batchDAO, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public TaskService taskService(final TaskLogic taskLogic) {
        return new TaskServiceImpl(taskLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public UserSelfService userSelfService(final UserLogic userLogic, final SyncopeLogic syncopeLogic) {
        return new UserSelfServiceImpl(userLogic, syncopeLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public UserService userService(final UserDAO userDAO, final UserLogic userLogic,
            final SearchCondVisitor searchCondVisitor) {
        return new UserServiceImpl(searchCondVisitor, userDAO, userLogic);
    }
}
