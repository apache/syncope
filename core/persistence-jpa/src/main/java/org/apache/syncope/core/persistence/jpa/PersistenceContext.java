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
package org.apache.syncope.core.persistence.jpa;

import java.util.HashMap;
import java.util.Map;
import javax.persistence.ValidationMode;
import javax.validation.Validator;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.DomainRegistry;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.AnyMatchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ApplicationDAO;
import org.apache.syncope.core.persistence.api.dao.AuditConfDAO;
import org.apache.syncope.core.persistence.api.dao.BatchDAO;
import org.apache.syncope.core.persistence.api.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.EntityCacheDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.MailTemplateDAO;
import org.apache.syncope.core.persistence.api.dao.NotificationDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrValueDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.dao.RemediationDAO;
import org.apache.syncope.core.persistence.api.dao.ReportDAO;
import org.apache.syncope.core.persistence.api.dao.ReportExecDAO;
import org.apache.syncope.core.persistence.api.dao.ReportTemplateDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.SRARouteDAO;
import org.apache.syncope.core.persistence.api.dao.SecurityQuestionDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.TaskExecDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.auth.AuthModuleDAO;
import org.apache.syncope.core.persistence.api.dao.auth.AuthProfileDAO;
import org.apache.syncope.core.persistence.api.dao.auth.CASSPDAO;
import org.apache.syncope.core.persistence.api.dao.auth.OIDCJWKSDAO;
import org.apache.syncope.core.persistence.api.dao.auth.OIDCRPDAO;
import org.apache.syncope.core.persistence.api.dao.auth.SAML2IdPEntityDAO;
import org.apache.syncope.core.persistence.api.dao.auth.SAML2SPDAO;
import org.apache.syncope.core.persistence.api.dao.auth.SAML2SPEntityDAO;
import org.apache.syncope.core.persistence.api.dao.auth.WAConfigDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.auth.ClientAppUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.policy.PolicyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.persistence.jpa.content.KeymasterConfParamLoader;
import org.apache.syncope.core.persistence.jpa.content.XMLContentExporter;
import org.apache.syncope.core.persistence.jpa.content.XMLContentLoader;
import org.apache.syncope.core.persistence.jpa.dao.JPAAccessTokenDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAAnyMatchDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAAnyObjectDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAAnySearchDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAAnyTypeClassDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAAnyTypeDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAApplicationDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAAuditConfDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPABatchDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAConnInstanceDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPADelegationDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPADerSchemaDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPADynRealmDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAEntityCacheDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAExternalResourceDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAGroupDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAImplementationDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAMailTemplateDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPANotificationDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAPlainAttrDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAPlainAttrValueDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAPlainSchemaDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAPolicyDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPARealmDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPARelationshipTypeDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPARemediationDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAReportDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAReportExecDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAReportTemplateDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPARoleDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPASRARouteDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPASecurityQuestionDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPATaskDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPATaskExecDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAUserDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAVirSchemaDAO;
import org.apache.syncope.core.persistence.jpa.dao.auth.JPAAuthModuleDAO;
import org.apache.syncope.core.persistence.jpa.dao.auth.JPAAuthProfileDAO;
import org.apache.syncope.core.persistence.jpa.dao.auth.JPACASSPDAO;
import org.apache.syncope.core.persistence.jpa.dao.auth.JPAOIDCJWKSDAO;
import org.apache.syncope.core.persistence.jpa.dao.auth.JPAOIDCRPDAO;
import org.apache.syncope.core.persistence.jpa.dao.auth.JPASAML2IdPEntityDAO;
import org.apache.syncope.core.persistence.jpa.dao.auth.JPASAML2SPDAO;
import org.apache.syncope.core.persistence.jpa.dao.auth.JPASAML2SPEntityDAO;
import org.apache.syncope.core.persistence.jpa.dao.auth.JPAWAConfigDAO;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyUtilsFactory;
import org.apache.syncope.core.persistence.jpa.entity.JPAEntityFactory;
import org.apache.syncope.core.persistence.jpa.entity.auth.JPAClientAppUtilsFactory;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAPolicyUtilsFactory;
import org.apache.syncope.core.persistence.jpa.entity.task.JPATaskUtilsFactory;
import org.apache.syncope.core.persistence.jpa.spring.CommonEntityManagerFactoryConf;
import org.apache.syncope.core.persistence.jpa.spring.DomainTransactionInterceptorInjector;
import org.apache.syncope.core.persistence.jpa.spring.MultiJarAwarePersistenceUnitPostProcessor;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@EnableConfigurationProperties(PersistenceProperties.class)
@Configuration(proxyBeanMethods = false)
public class PersistenceContext {

    private static final Logger OPENJPA_LOG = LoggerFactory.getLogger("org.apache.openjpa");

    @Bean
    public static BeanFactoryPostProcessor domainTransactionInterceptorInjector() {
        return new DomainTransactionInterceptorInjector();
    }

    @ConditionalOnMissingBean
    @Bean
    public SearchCondVisitor searchCondVisitor() {
        return new SearchCondVisitor();
    }

    @Bean
    public Validator localValidatorFactoryBean() {
        return new LocalValidatorFactoryBean();
    }

    @ConditionalOnMissingBean
    @Bean
    public CommonEntityManagerFactoryConf commonEMFConf(final PersistenceProperties persistenceProperties) {
        CommonEntityManagerFactoryConf commonEMFConf = new CommonEntityManagerFactoryConf();
        commonEMFConf.setPackagesToScan("org.apache.syncope.core.persistence.jpa.entity");
        commonEMFConf.setValidationMode(ValidationMode.NONE);
        commonEMFConf.setPersistenceUnitPostProcessors(new MultiJarAwarePersistenceUnitPostProcessor());
        Map<String, Object> jpaPropertyMap = new HashMap<>();

        jpaPropertyMap.put("openjpa.Log", "slf4j");
        if (OPENJPA_LOG.isDebugEnabled()) {
            jpaPropertyMap.put("openjpa.Log", "SQL=TRACE");
            jpaPropertyMap.put("openjpa.ConnectionFactoryProperties",
                    "PrintParameters=true, PrettyPrint=true, PrettyPrintLineLength=120");
        }

        jpaPropertyMap.put("openjpa.NontransactionalWrite", false);

        jpaPropertyMap.put("openjpa.jdbc.MappingDefaults",
                "ForeignKeyDeleteAction=restrict, JoinForeignKeyDeleteAction=restrict,"
                + "FieldStrategies='"
                + "java.util.Locale=org.apache.syncope.core.persistence.jpa.openjpa.LocaleValueHandler,"
                + "java.lang.Boolean=org.apache.syncope.core.persistence.jpa.openjpa.BooleanValueHandler'");

        jpaPropertyMap.put("openjpa.DataCache", "true");
        jpaPropertyMap.put("openjpa.QueryCache", "true");

        jpaPropertyMap.put("openjpa.RemoteCommitProvider", persistenceProperties.getRemoteCommitProvider());

        commonEMFConf.setJpaPropertyMap(jpaPropertyMap);
        return commonEMFConf;
    }

    @ConditionalOnMissingBean
    @Bean
    public XMLContentLoader xmlContentLoader(
            final PersistenceProperties persistenceProperties,
            final ResourceLoader resourceLoader,
            final Environment env) {

        return new XMLContentLoader(
                resourceLoader.getResource(persistenceProperties.getViewsXML()),
                resourceLoader.getResource(persistenceProperties.getIndexesXML()),
                env);
    }

    @ConditionalOnMissingBean
    @Bean
    public XMLContentExporter xmlContentExporter(final DomainHolder domainHolder, final RealmDAO realmDAO) {
        return new XMLContentExporter(domainHolder, realmDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public KeymasterConfParamLoader keymasterConfParamLoader(final ConfParamOps confParamOps) {
        return new KeymasterConfParamLoader(confParamOps);
    }

    @ConditionalOnMissingBean
    @Bean
    public DomainRegistry domainRegistry(final Environment env) {
        return new DomainConfFactory(env);
    }

    @ConditionalOnMissingBean
    @Bean
    public RuntimeDomainLoader runtimeDomainLoader(
            final DomainHolder domainHolder,
            final DomainRegistry domainRegistry) {

        return new RuntimeDomainLoader(domainHolder, domainRegistry);
    }

    @ConditionalOnMissingBean
    @Bean
    public StartupDomainLoader startupDomainLoader(
            final PersistenceProperties persistenceProperties,
            final ResourceLoader resourceLoader,
            final DomainOps domainOps,
            final DomainHolder domainHolder,
            final DomainRegistry domainRegistry) {

        return new StartupDomainLoader(domainOps, domainHolder, persistenceProperties, resourceLoader, domainRegistry);
    }

    @ConditionalOnMissingBean
    @Bean
    public EntityFactory entityFactory() {
        return new JPAEntityFactory();
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyUtilsFactory anyUtilsFactory(
            final @Lazy UserDAO userDAO,
            final @Lazy GroupDAO groupDAO,
            final @Lazy AnyObjectDAO anyObjectDAO,
            final @Lazy EntityFactory entityFactory) {

        return new JPAAnyUtilsFactory(userDAO, groupDAO, anyObjectDAO, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public ClientAppUtilsFactory clientAppUtilsFactory() {
        return new JPAClientAppUtilsFactory();
    }

    @ConditionalOnMissingBean
    @Bean
    public PolicyUtilsFactory policyUtilsFactory() {
        return new JPAPolicyUtilsFactory();
    }

    @ConditionalOnMissingBean
    @Bean
    public TaskUtilsFactory taskUtilsFactory(final @Lazy EntityFactory entityFactory) {
        return new JPATaskUtilsFactory(entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public EntityCacheDAO entityCacheDAO() {
        return new JPAEntityCacheDAO();
    }

    @ConditionalOnMissingBean
    @Bean
    public AccessTokenDAO accessTokenDAO() {
        return new JPAAccessTokenDAO();
    }

    @ConditionalOnMissingBean
    @Bean
    public ApplicationDAO applicationDAO(final RoleDAO roleDAO, final @Lazy UserDAO userDAO) {
        return new JPAApplicationDAO(roleDAO, userDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyMatchDAO anyMatchDAO(
            final @Lazy UserDAO userDAO,
            final @Lazy GroupDAO groupDAO,
            final @Lazy AnyObjectDAO anyObjectDAO,
            final RealmDAO realmDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final AnyUtilsFactory anyUtilsFactory) {

        return new JPAAnyMatchDAO(userDAO, groupDAO, anyObjectDAO, realmDAO, plainSchemaDAO, anyUtilsFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyObjectDAO anyObjectDAO(
            final ApplicationEventPublisher publisher,
            final AnyUtilsFactory anyUtilsFactory,
            final @Lazy PlainSchemaDAO plainSchemaDAO,
            final @Lazy DerSchemaDAO derSchemaDAO,
            final @Lazy DynRealmDAO dynRealmDAO,
            final @Lazy UserDAO userDAO,
            final @Lazy GroupDAO groupDAO) {

        return new JPAAnyObjectDAO(
                anyUtilsFactory,
                publisher,
                plainSchemaDAO,
                derSchemaDAO,
                dynRealmDAO,
                userDAO,
                groupDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnySearchDAO anySearchDAO(
            final RealmDAO realmDAO,
            final @Lazy DynRealmDAO dynRealmDAO,
            final @Lazy UserDAO userDAO,
            final @Lazy GroupDAO groupDAO,
            final @Lazy AnyObjectDAO anyObjectDAO,
            final PlainSchemaDAO schemaDAO,
            final EntityFactory entityFactory,
            final AnyUtilsFactory anyUtilsFactory) {

        return new JPAAnySearchDAO(
                realmDAO,
                dynRealmDAO,
                userDAO,
                groupDAO,
                anyObjectDAO,
                schemaDAO,
                entityFactory,
                anyUtilsFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyTypeDAO anyTypeDAO(final RemediationDAO remediationDAO) {
        return new JPAAnyTypeDAO(remediationDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyTypeClassDAO anyTypeClassDAO(
            final AnyTypeDAO anyTypeDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final DerSchemaDAO derSchemaDAO,
            final VirSchemaDAO virSchemaDAO,
            final @Lazy GroupDAO groupDAO,
            final ExternalResourceDAO resourceDAO) {

        return new JPAAnyTypeClassDAO(anyTypeDAO, plainSchemaDAO, derSchemaDAO, virSchemaDAO, groupDAO, resourceDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public AuditConfDAO auditConfDAO() {
        return new JPAAuditConfDAO();
    }

    @ConditionalOnMissingBean
    @Bean
    public AuthModuleDAO authModuleDAO() {
        return new JPAAuthModuleDAO();
    }

    @ConditionalOnMissingBean
    @Bean
    public AuthProfileDAO authProfileDAO() {
        return new JPAAuthProfileDAO();
    }

    @ConditionalOnMissingBean
    @Bean
    public BatchDAO batchDAO() {
        return new JPABatchDAO();
    }

    @ConditionalOnMissingBean
    @Bean
    public CASSPDAO casSPDAO() {
        return new JPACASSPDAO();
    }

    @ConditionalOnMissingBean
    @Bean
    public ConnInstanceDAO connInstanceDAO(final @Lazy ExternalResourceDAO resourceDAO) {
        return new JPAConnInstanceDAO(resourceDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public DelegationDAO delegationDAO() {
        return new JPADelegationDAO();
    }

    @ConditionalOnMissingBean
    @Bean
    public DerSchemaDAO derSchemaDAO(final @Lazy ExternalResourceDAO resourceDAO) {
        return new JPADerSchemaDAO(resourceDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public DynRealmDAO dynRealmDAO(
            final ApplicationEventPublisher publisher,
            final @Lazy UserDAO userDAO,
            final @Lazy GroupDAO groupDAO,
            final @Lazy AnyObjectDAO anyObjectDAO,
            final AnySearchDAO searchDAO,
            final AnyMatchDAO anyMatchDAO,
            final SearchCondVisitor searchCondVisitor) {

        return new JPADynRealmDAO(
                publisher,
                userDAO,
                groupDAO,
                anyObjectDAO,
                searchDAO,
                anyMatchDAO,
                searchCondVisitor);
    }

    @ConditionalOnMissingBean
    @Bean
    public GroupDAO groupDAO(
            final ApplicationEventPublisher publisher,
            final AnyUtilsFactory anyUtilsFactory,
            final @Lazy PlainSchemaDAO plainSchemaDAO,
            final @Lazy DerSchemaDAO derSchemaDAO,
            final @Lazy DynRealmDAO dynRealmDAO,
            final AnyMatchDAO anyMatchDAO,
            final PlainAttrDAO plainAttrDAO,
            final @Lazy UserDAO userDAO,
            final @Lazy AnyObjectDAO anyObjectDAO,
            final AnySearchDAO anySearchDAO,
            final SearchCondVisitor searchCondVisitor) {

        return new JPAGroupDAO(
                anyUtilsFactory,
                publisher,
                plainSchemaDAO,
                derSchemaDAO,
                dynRealmDAO,
                anyMatchDAO,
                plainAttrDAO,
                userDAO,
                anyObjectDAO,
                anySearchDAO,
                searchCondVisitor);
    }

    @ConditionalOnMissingBean
    @Bean
    public ImplementationDAO implementationDAO() {
        return new JPAImplementationDAO();
    }

    @ConditionalOnMissingBean
    @Bean
    public MailTemplateDAO mailTemplateDAO() {
        return new JPAMailTemplateDAO();
    }

    @ConditionalOnMissingBean
    @Bean
    public NotificationDAO notificationDAO(final TaskDAO taskDAO) {
        return new JPANotificationDAO(taskDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public OIDCJWKSDAO oidcJWKSDAO() {
        return new JPAOIDCJWKSDAO();
    }

    @ConditionalOnMissingBean
    @Bean
    public OIDCRPDAO oidcRPDAO() {
        return new JPAOIDCRPDAO();
    }

    @ConditionalOnMissingBean
    @Bean
    public PlainAttrDAO plainAttrDAO() {
        return new JPAPlainAttrDAO();
    }

    @ConditionalOnMissingBean
    @Bean
    public PlainAttrValueDAO plainAttrValueDAO() {
        return new JPAPlainAttrValueDAO();
    }

    @ConditionalOnMissingBean
    @Bean
    public PlainSchemaDAO plainSchemaDAO(
            final AnyUtilsFactory anyUtilsFactory,
            final PlainAttrDAO plainAttrDAO,
            final @Lazy ExternalResourceDAO resourceDAO) {

        return new JPAPlainSchemaDAO(anyUtilsFactory, plainAttrDAO, resourceDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public PolicyDAO policyDAO(
            final @Lazy RealmDAO realmDAO,
            final @Lazy ExternalResourceDAO resourceDAO) {

        return new JPAPolicyDAO(realmDAO, resourceDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public RealmDAO realmDAO(final @Lazy RoleDAO roleDAO) {
        return new JPARealmDAO(roleDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public RelationshipTypeDAO relationshipTypeDAO() {
        return new JPARelationshipTypeDAO();
    }

    @ConditionalOnMissingBean
    @Bean
    public RemediationDAO remediationDAO() {
        return new JPARemediationDAO();
    }

    @ConditionalOnMissingBean
    @Bean
    public ReportTemplateDAO reportTemplateDAO() {
        return new JPAReportTemplateDAO();
    }

    @ConditionalOnMissingBean
    @Bean
    public ReportDAO reportDAO() {
        return new JPAReportDAO();
    }

    @ConditionalOnMissingBean
    @Bean
    public ReportExecDAO reportExecDAO() {
        return new JPAReportExecDAO();
    }

    @ConditionalOnMissingBean
    @Bean
    public ExternalResourceDAO resourceDAO(
            final TaskDAO taskDAO,
            final AnyObjectDAO anyObjectDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final PolicyDAO policyDAO,
            final VirSchemaDAO virSchemaDAO,
            final RealmDAO realmDAO) {

        return new JPAExternalResourceDAO(taskDAO, anyObjectDAO, userDAO, groupDAO, policyDAO, virSchemaDAO, realmDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public RoleDAO roleDAO(
            final ApplicationEventPublisher publisher,
            final @Lazy AnyMatchDAO anyMatchDAO,
            final @Lazy AnySearchDAO anySearchDAO,
            final DelegationDAO delegationDAO,
            final SearchCondVisitor searchCondVisitor) {

        return new JPARoleDAO(anyMatchDAO, publisher, anySearchDAO, delegationDAO, searchCondVisitor);
    }

    @ConditionalOnMissingBean
    @Bean
    public SAML2SPDAO saml2SPDAO() {
        return new JPASAML2SPDAO();
    }

    @ConditionalOnMissingBean
    @Bean
    public SAML2IdPEntityDAO saml2IdPEntityDAO() {
        return new JPASAML2IdPEntityDAO();
    }

    @ConditionalOnMissingBean
    @Bean
    public SAML2SPEntityDAO saml2SPEntityDAO() {
        return new JPASAML2SPEntityDAO();
    }

    @ConditionalOnMissingBean
    @Bean
    public SecurityQuestionDAO securityQuestionDAO(final UserDAO userDAO) {
        return new JPASecurityQuestionDAO(userDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public SRARouteDAO sraRouteDAO() {
        return new JPASRARouteDAO();
    }

    @ConditionalOnMissingBean
    @Bean
    public TaskDAO taskDAO(final RemediationDAO remediationDAO) {
        return new JPATaskDAO(remediationDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public TaskExecDAO taskExecDAO(final TaskDAO taskDAO) {
        return new JPATaskExecDAO(taskDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public UserDAO userDAO(
            final ApplicationEventPublisher publisher,
            final SecurityProperties securityProperties,
            final AnyUtilsFactory anyUtilsFactory,
            final @Lazy PlainSchemaDAO plainSchemaDAO,
            final @Lazy DerSchemaDAO derSchemaDAO,
            final @Lazy DynRealmDAO dynRealmDAO,
            final RoleDAO roleDAO,
            final AccessTokenDAO accessTokenDAO,
            final RealmDAO realmDAO,
            final @Lazy GroupDAO groupDAO,
            final DelegationDAO delegationDAO) {

        return new JPAUserDAO(
                anyUtilsFactory,
                publisher,
                plainSchemaDAO,
                derSchemaDAO,
                dynRealmDAO,
                roleDAO,
                accessTokenDAO,
                realmDAO,
                groupDAO,
                delegationDAO,
                securityProperties);
    }

    @ConditionalOnMissingBean
    @Bean
    public VirSchemaDAO virSchemaDAO(final @Lazy ExternalResourceDAO resourceDAO) {
        return new JPAVirSchemaDAO(resourceDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public WAConfigDAO waConfigDAO() {
        return new JPAWAConfigDAO();
    }
}
