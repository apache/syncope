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

import com.fasterxml.jackson.jakarta.rs.json.JacksonJsonProvider;
import com.fasterxml.jackson.jakarta.rs.xml.JacksonXMLProvider;
import com.fasterxml.jackson.jakarta.rs.yaml.JacksonYAMLProvider;
import io.swagger.v3.oas.models.security.SecurityScheme;
import jakarta.validation.Validator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.cxf.Bus;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.jaxrs.ext.ContextProvider;
import org.apache.cxf.jaxrs.ext.search.SearchContext;
import org.apache.cxf.jaxrs.ext.search.SearchContextImpl;
import org.apache.cxf.jaxrs.ext.search.SearchContextProvider;
import org.apache.cxf.jaxrs.ext.search.SearchUtils;
import org.apache.cxf.jaxrs.model.doc.JavaDocProvider;
import org.apache.cxf.jaxrs.openapi.OpenApiCustomizer;
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
import org.apache.syncope.common.rest.api.service.AuditService;
import org.apache.syncope.common.rest.api.service.CommandService;
import org.apache.syncope.common.rest.api.service.DelegationService;
import org.apache.syncope.common.rest.api.service.DynRealmService;
import org.apache.syncope.common.rest.api.service.FIQLQueryService;
import org.apache.syncope.common.rest.api.service.GroupService;
import org.apache.syncope.common.rest.api.service.ImplementationService;
import org.apache.syncope.common.rest.api.service.JAXRSService;
import org.apache.syncope.common.rest.api.service.MailTemplateService;
import org.apache.syncope.common.rest.api.service.NotificationService;
import org.apache.syncope.common.rest.api.service.PolicyService;
import org.apache.syncope.common.rest.api.service.RealmService;
import org.apache.syncope.common.rest.api.service.RelationshipTypeService;
import org.apache.syncope.common.rest.api.service.ReportService;
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
import org.apache.syncope.core.logic.AuditLogic;
import org.apache.syncope.core.logic.CommandLogic;
import org.apache.syncope.core.logic.DelegationLogic;
import org.apache.syncope.core.logic.DynRealmLogic;
import org.apache.syncope.core.logic.FIQLQueryLogic;
import org.apache.syncope.core.logic.GroupLogic;
import org.apache.syncope.core.logic.ImplementationLogic;
import org.apache.syncope.core.logic.MailTemplateLogic;
import org.apache.syncope.core.logic.NotificationLogic;
import org.apache.syncope.core.logic.PolicyLogic;
import org.apache.syncope.core.logic.RealmLogic;
import org.apache.syncope.core.logic.RelationshipTypeLogic;
import org.apache.syncope.core.logic.ReportLogic;
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
import org.apache.syncope.core.rest.cxf.service.AuditServiceImpl;
import org.apache.syncope.core.rest.cxf.service.CommandServiceImpl;
import org.apache.syncope.core.rest.cxf.service.DelegationServiceImpl;
import org.apache.syncope.core.rest.cxf.service.DynRealmServiceImpl;
import org.apache.syncope.core.rest.cxf.service.FIQLQueryServiceImpl;
import org.apache.syncope.core.rest.cxf.service.GroupServiceImpl;
import org.apache.syncope.core.rest.cxf.service.ImplementationServiceImpl;
import org.apache.syncope.core.rest.cxf.service.MailTemplateServiceImpl;
import org.apache.syncope.core.rest.cxf.service.NotificationServiceImpl;
import org.apache.syncope.core.rest.cxf.service.PolicyServiceImpl;
import org.apache.syncope.core.rest.cxf.service.RealmServiceImpl;
import org.apache.syncope.core.rest.cxf.service.RelationshipTypeServiceImpl;
import org.apache.syncope.core.rest.cxf.service.ReportServiceImpl;
import org.apache.syncope.core.rest.cxf.service.RoleServiceImpl;
import org.apache.syncope.core.rest.cxf.service.SchemaServiceImpl;
import org.apache.syncope.core.rest.cxf.service.SecurityQuestionServiceImpl;
import org.apache.syncope.core.rest.cxf.service.SyncopeServiceImpl;
import org.apache.syncope.core.rest.cxf.service.TaskServiceImpl;
import org.apache.syncope.core.rest.cxf.service.UserSelfServiceImpl;
import org.apache.syncope.core.rest.cxf.service.UserServiceImpl;
import org.apache.syncope.core.spring.task.VirtualThreadPoolTaskExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

@PropertySource("classpath:errorMessages.properties")
@EnableConfigurationProperties(RESTProperties.class)
@Configuration(proxyBeanMethods = false)
public class IdRepoRESTCXFContext {

    private static final Logger LOG = LoggerFactory.getLogger(IdRepoRESTCXFContext.class);

    @Bean
    public VirtualThreadPoolTaskExecutor batchExecutor(final RESTProperties props) {
        VirtualThreadPoolTaskExecutor executor = new VirtualThreadPoolTaskExecutor();
        executor.setPoolSize(props.getBatchExecutor().getPoolSize());
        executor.setAwaitTerminationSeconds(props.getBatchExecutor().getAwaitTerminationSeconds());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setThreadNamePrefix("Batch-");
        executor.initialize();
        return executor;
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
    public MDCInInterceptor mdcInInterceptor() {
        return new MDCInInterceptor();
    }

    @ConditionalOnMissingBean
    @Bean
    public JAXRSBeanValidationInInterceptor validationInInterceptor(final Validator validator) {
        JAXRSBeanValidationInInterceptor validationInInterceptor = new JAXRSBeanValidationInInterceptor();
        validationInInterceptor.setProvider(new BeanValidationProvider(validator));
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
    public ThreadLocalCleanupOutInterceptor threadLocalCleanupOutInterceptor() {
        return new ThreadLocalCleanupOutInterceptor();
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
    public AddDomainFilter addDomainFilter() {
        return new AddDomainFilter();
    }

    @ConditionalOnMissingBean
    @Bean
    public AddETagFilter addETagFilter() {
        return new AddETagFilter();
    }

    @ConditionalOnMissingBean(name = { "openApiCustomizer", "syncopeOpenApiCustomizer" })
    @Bean
    public OpenApiCustomizer openApiCustomizer(final DomainHolder<?> domainHolder, final Environment env) {
        JavaDocProvider javaDocProvider = JavaDocUtils.getJavaDocURLs().
                map(JavaDocProvider::new).
                orElseGet(() -> JavaDocUtils.getJavaDocPaths(env).
                map(javaDocPaths -> {
                    try {
                        return new JavaDocProvider(javaDocPaths);
                    } catch (Exception e) {
                        LOG.error("Could not set javadoc paths from {}", List.of(javaDocPaths), e);
                        return null;
                    }
                }).
                orElse(null));

        SyncopeOpenApiCustomizer openApiCustomizer = new SyncopeOpenApiCustomizer(domainHolder);
        openApiCustomizer.setDynamicBasePath(false);
        openApiCustomizer.setReplaceTags(false);
        openApiCustomizer.setJavadocProvider(javaDocProvider);
        return openApiCustomizer;
    }

    @ConditionalOnMissingBean
    @Bean
    public OpenApiFeature openapiFeature(final OpenApiCustomizer openApiCustomizer, final Environment env) {
        String version = env.getProperty("version");
        OpenApiFeature openapiFeature = new OpenApiFeature();
        openapiFeature.setUseContextBasedConfig(true);
        openapiFeature.setTitle("Apache Syncope");
        openapiFeature.setVersion(version);
        openapiFeature.setDescription("Apache Syncope " + version);
        openapiFeature.setContactName("The Apache Syncope community");
        openapiFeature.setContactEmail("dev@syncope.apache.org");
        openapiFeature.setContactUrl("https://syncope.apache.org");
        openapiFeature.setScan(false);
        openapiFeature.setResourcePackages(Set.of("org.apache.syncope.common.rest.api.service"));
        openapiFeature.setCustomizer(openApiCustomizer);
        openapiFeature.setSecurityDefinitions(Map.of(
                "BasicAuthentication", new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("basic"),
                "Bearer", new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));

        return openapiFeature;
    }

    @ConditionalOnMissingBean
    @Bean
    public Server restContainer(
            final List<JAXRSService> services,
            final AddETagFilter addETagFilter,
            final AddDomainFilter addDomainFilter,
            final ContextProvider<SearchContext> searchContextProvider,
            final JacksonYAMLProvider yamlProvider,
            final JacksonXMLProvider xmlProvider,
            final JacksonJsonProvider jsonProvider,
            final DateParamConverterProvider dateParamConverterProvider,
            final MDCInInterceptor mdcInInterceptor,
            final JAXRSBeanValidationInInterceptor validationInInterceptor,
            final GZIPInInterceptor gzipInInterceptor,
            final GZIPOutInterceptor gzipOutInterceptor,
            final ThreadLocalCleanupOutInterceptor threadLocalCleanupOutInterceptor,
            final OpenApiFeature openapiFeature,
            final RestServiceExceptionMapper restServiceExceptionMapper,
            final Bus bus) {

        SpringJAXRSServerFactoryBean restContainer = new SpringJAXRSServerFactoryBean();
        restContainer.setBus(bus);
        restContainer.setAddress("/");
        restContainer.setStaticSubresourceResolution(true);

        Map<String, Object> properties = new HashMap<>();
        properties.put(SearchContextImpl.CUSTOM_SEARCH_PARSER_CLASS_PROPERTY, SyncopeFiqlParser.class.getName());
        properties.put(SearchUtils.LAX_PROPERTY_MATCH, "true");
        properties.put("convert.wadl.resources.to.dom", "false");
        restContainer.setProperties(properties);

        restContainer.setServiceBeans(services.stream().map(Object.class::cast).toList());

        restContainer.setProviders(List.of(
                dateParamConverterProvider,
                jsonProvider,
                xmlProvider,
                yamlProvider,
                restServiceExceptionMapper,
                searchContextProvider,
                addDomainFilter,
                addETagFilter));

        restContainer.setInInterceptors(List.of(mdcInInterceptor, validationInInterceptor, gzipInInterceptor));

        restContainer.setOutInterceptors(List.of(gzipOutInterceptor, threadLocalCleanupOutInterceptor));

        restContainer.setFeatures(List.of(openapiFeature));

        return restContainer.create();
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
    public AuditService auditService(final AuditLogic auditLogic) {
        return new AuditServiceImpl(auditLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public CommandService commandService(final CommandLogic commandLogic) {
        return new CommandServiceImpl(commandLogic);
    }

    @ConditionalOnMissingBean
    @Bean
    public FIQLQueryService fiqlQueryService(final FIQLQueryLogic fiqlQueryLogic) {
        return new FIQLQueryServiceImpl(fiqlQueryLogic);
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
            @Qualifier("batchExecutor")
            final VirtualThreadPoolTaskExecutor batchExecutor,
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
    public UserService userService(
            final UserDAO userDAO,
            final UserLogic userLogic,
            final SearchCondVisitor searchCondVisitor) {

        return new UserServiceImpl(searchCondVisitor, userDAO, userLogic);
    }
}
