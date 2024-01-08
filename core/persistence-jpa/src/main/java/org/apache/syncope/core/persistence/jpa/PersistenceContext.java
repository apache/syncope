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

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.ValidationMode;
import jakarta.validation.Validator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.DomainRegistry;
import org.apache.syncope.core.persistence.api.attrvalue.validation.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.AnyMatchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ApplicationDAO;
import org.apache.syncope.core.persistence.api.dao.AttrRepoDAO;
import org.apache.syncope.core.persistence.api.dao.AuditConfDAO;
import org.apache.syncope.core.persistence.api.dao.AuthModuleDAO;
import org.apache.syncope.core.persistence.api.dao.AuthProfileDAO;
import org.apache.syncope.core.persistence.api.dao.BatchDAO;
import org.apache.syncope.core.persistence.api.dao.CASSPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.ConnInstanceDAO;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.EntityCacheDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.FIQLQueryDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.JobStatusDAO;
import org.apache.syncope.core.persistence.api.dao.MailTemplateDAO;
import org.apache.syncope.core.persistence.api.dao.NotificationDAO;
import org.apache.syncope.core.persistence.api.dao.OIDCJWKSDAO;
import org.apache.syncope.core.persistence.api.dao.OIDCRPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.PersistenceInfoDAO;
import org.apache.syncope.core.persistence.api.dao.PlainAttrValueDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.dao.RemediationDAO;
import org.apache.syncope.core.persistence.api.dao.ReportDAO;
import org.apache.syncope.core.persistence.api.dao.ReportExecDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.SAML2IdPEntityDAO;
import org.apache.syncope.core.persistence.api.dao.SAML2SPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.SAML2SPEntityDAO;
import org.apache.syncope.core.persistence.api.dao.SRARouteDAO;
import org.apache.syncope.core.persistence.api.dao.SecurityQuestionDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.TaskExecDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.WAConfigDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.am.ClientAppUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.policy.PolicyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.persistence.jpa.attrvalue.validation.DefaultPlainAttrValidationManager;
import org.apache.syncope.core.persistence.jpa.content.KeymasterConfParamLoader;
import org.apache.syncope.core.persistence.jpa.content.XMLContentExporter;
import org.apache.syncope.core.persistence.jpa.content.XMLContentLoader;
import org.apache.syncope.core.persistence.jpa.dao.JPAAnyMatchDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAAnySearchDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAEntityCacheDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAJobStatusDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAOIDCJWKSDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAPersistenceInfoDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAPlainAttrValueDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAPolicyDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPATaskDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPATaskExecDAO;
import org.apache.syncope.core.persistence.jpa.dao.repo.AccessTokenRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.AccessTokenRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.AnyObjectRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.AnyObjectRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.AnyTypeClassRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.AnyTypeClassRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.AnyTypeRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.AnyTypeRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.ApplicationRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.ApplicationRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.AttrRepoRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.AttrRepoRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.AuditConfRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.AuditConfRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.AuthModuleRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.AuthModuleRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.AuthProfileRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.AuthProfileRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.BatchRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.BatchRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.CASSPClientAppRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.CASSPClientAppRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.ConnInstanceRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.ConnInstanceRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.DelegationRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.DelegationRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.DerSchemaRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.DerSchemaRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.DynRealmRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.DynRealmRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.ExternalResourceRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.ExternalResourceRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.FIQLQueryRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.FIQLQueryRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.GroupRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.GroupRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.ImplementationRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.ImplementationRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.MailTemplateRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.NotificationRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.NotificationRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.OIDCRPClientAppRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.OIDCRPClientAppRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.PlainSchemaRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.PlainSchemaRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.RealmRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.RealmRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.RelationshipTypeRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.RelationshipTypeRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.RemediationRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.RemediationRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.ReportExecRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.ReportExecRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.ReportRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.RoleRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.RoleRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.SAML2IdPEntityRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.SAML2SPClientAppRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.SAML2SPClientAppRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.SAML2SPEntityRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.SRARouteRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.SecurityQuestionRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.SecurityQuestionRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.UserRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.UserRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.VirSchemaRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.VirSchemaRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.WAConfigRepo;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyUtils;
import org.apache.syncope.core.persistence.jpa.entity.JPAAnyUtilsFactory;
import org.apache.syncope.core.persistence.jpa.entity.JPAEntityFactory;
import org.apache.syncope.core.persistence.jpa.entity.am.JPAClientAppUtilsFactory;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAPolicyUtilsFactory;
import org.apache.syncope.core.persistence.jpa.entity.task.JPATaskUtilsFactory;
import org.apache.syncope.core.persistence.jpa.openjpa.ConnectorManagerRemoteCommitListener;
import org.apache.syncope.core.persistence.jpa.spring.CommonEntityManagerFactoryConf;
import org.apache.syncope.core.persistence.jpa.spring.DomainEntityManagerFactoryBean;
import org.apache.syncope.core.persistence.jpa.spring.DomainRoutingDataSource;
import org.apache.syncope.core.persistence.jpa.spring.MultiJarAwarePersistenceUnitPostProcessor;
import org.apache.syncope.core.persistence.jpa.spring.OpenJpaVendorAdapter;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

@EnableConfigurationProperties(PersistenceProperties.class)
@Configuration(proxyBeanMethods = false)
public class PersistenceContext {

    private static final Logger OPENJPA_LOG = LoggerFactory.getLogger("org.apache.openjpa");

    @ConditionalOnMissingBean
    @Bean
    public CommonEntityManagerFactoryConf commonEMFConf(
            final PersistenceProperties persistenceProperties,
            final List<JndiObjectFactoryBean> dataSources) {

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

        dataSources.forEach(ds -> commonEMFConf.getDomains().put(
                StringUtils.substringBetween(ds.getJndiName(), "java:comp/env/jdbc/syncope", "DataSource"),
                (DataSource) ds.getObject()));

        return commonEMFConf;
    }

    @ConditionalOnMissingBean
    @Bean
    public DomainRoutingDataSource domainRoutingDataSource(final DomainHolder domainHolder) {
        Map<Object, Object> targetDataSources = new HashMap<>();
        domainHolder.getDomains().forEach((domain, dataSource) -> targetDataSources.put(domain, dataSource));
        DomainRoutingDataSource routingSA = new DomainRoutingDataSource();
        routingSA.setDefaultTargetDataSource(domainHolder.getDomains().get(SyncopeConstants.MASTER_DOMAIN));
        routingSA.setTargetDataSources(targetDataSources);
        return routingSA;
    }

    @ConditionalOnMissingBean
    @Bean(name = { "entityManagerFactory", "domainEntityManagerFactory" })
    public DomainEntityManagerFactoryBean domainEntityManagerFactory(
            final PersistenceProperties props,
            final DomainRoutingDataSource domainRoutingDataSource,
            final CommonEntityManagerFactoryConf commonEMFConf) {

        OpenJpaVendorAdapter vendorAdapter = new OpenJpaVendorAdapter();
        vendorAdapter.setShowSql(false);
        vendorAdapter.setGenerateDdl(true);
        vendorAdapter.setDatabasePlatform(props.getDomain().get(0).getDatabasePlatform());

        DomainEntityManagerFactoryBean masterEntityManagerFactory = new DomainEntityManagerFactoryBean();
        masterEntityManagerFactory.setMappingResources(props.getDomain().get(0).getOrm());
        masterEntityManagerFactory.setPersistenceUnitName("Syncope");
        masterEntityManagerFactory.setDataSource(domainRoutingDataSource);
        masterEntityManagerFactory.setJpaVendorAdapter(vendorAdapter);
        masterEntityManagerFactory.setCommonEntityManagerFactoryConf(commonEMFConf);
        masterEntityManagerFactory.setConnectorManagerRemoteCommitListener(new ConnectorManagerRemoteCommitListener());

        if (props.getMetaDataFactory() != null) {
            masterEntityManagerFactory.setJpaPropertyMap(Map.of(
                    "openjpa.MetaDataFactory",
                    props.getMetaDataFactory().replace("##orm##", props.getDomain().get(0).getOrm())));
        }

        return masterEntityManagerFactory;
    }

    @ConditionalOnMissingBean
    @Bean
    public PlatformTransactionManager domainTransactionManager(
            final DomainEntityManagerFactoryBean domainEntityManagerFactory) {

        return new JpaTransactionManager(domainEntityManagerFactory.getObject());
    }

    @ConditionalOnMissingBean
    @Bean
    public TransactionTemplate domainTransactionTemplate(
            final PlatformTransactionManager domainTransactionManager) {

        return new TransactionTemplate(domainTransactionManager);
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
    public PlainAttrValidationManager plainAttrValidationManager() {
        return new DefaultPlainAttrValidationManager();
    }

    @ConditionalOnMissingBean
    @Bean
    public XMLContentLoader xmlContentLoader(
            final PersistenceProperties persistenceProperties,
            final ResourceLoader resourceLoader,
            final Environment env) {

        return new XMLContentLoader(
                persistenceProperties,
                resourceLoader.getResource(persistenceProperties.getViewsXML()),
                resourceLoader.getResource(persistenceProperties.getIndexesXML()),
                env);
    }

    @ConditionalOnMissingBean
    @Bean
    public XMLContentExporter xmlContentExporter(
            final DomainHolder domainHolder,
            final RealmDAO realmDAO,
            final EntityManagerFactory domainEntityManagerFactory) {

        return new XMLContentExporter(domainHolder, realmDAO, domainEntityManagerFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public KeymasterConfParamLoader keymasterConfParamLoader(final ConfParamOps confParamOps) {
        return new KeymasterConfParamLoader(confParamOps);
    }

    @ConditionalOnMissingBean
    @Bean
    public DomainRegistry domainRegistry(final ConfigurableApplicationContext ctx) {
        return new DomainConfFactory(ctx);
    }

    @ConditionalOnMissingBean
    @Bean
    public RuntimeDomainLoader runtimeDomainLoader(
            final DomainHolder domainHolder,
            final DomainRegistry domainRegistry,
            final EntityManagerFactory domainEntityManagerFactory,
            final ConfigurableApplicationContext ctx) {

        return new RuntimeDomainLoader(domainHolder, domainRegistry, domainEntityManagerFactory, ctx);
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

    @Bean(name = "userAnyUtils")
    public AnyUtils userAnyUtils(
            final @Lazy UserDAO userDAO,
            final @Lazy GroupDAO groupDAO,
            final @Lazy AnyObjectDAO anyObjectDAO,
            final @Lazy EntityFactory entityFactory) {

        return new JPAAnyUtils(userDAO, groupDAO, anyObjectDAO, entityFactory, AnyTypeKind.USER, false);
    }

    @Bean(name = "linkedAccountAnyUtils")
    public AnyUtils linkedAccountAnyUtils(
            final @Lazy UserDAO userDAO,
            final @Lazy GroupDAO groupDAO,
            final @Lazy AnyObjectDAO anyObjectDAO,
            final @Lazy EntityFactory entityFactory) {

        return new JPAAnyUtils(userDAO, groupDAO, anyObjectDAO, entityFactory, AnyTypeKind.USER, true);
    }

    @Bean(name = "groupAnyUtils")
    public AnyUtils groupAnyUtils(
            final @Lazy UserDAO userDAO,
            final @Lazy GroupDAO groupDAO,
            final @Lazy AnyObjectDAO anyObjectDAO,
            final @Lazy EntityFactory entityFactory) {

        return new JPAAnyUtils(userDAO, groupDAO, anyObjectDAO, entityFactory, AnyTypeKind.GROUP, false);
    }

    @Bean(name = "anyObjectAnyUtils")
    public AnyUtils anyObjectAnyUtils(
            final @Lazy UserDAO userDAO,
            final @Lazy GroupDAO groupDAO,
            final @Lazy AnyObjectDAO anyObjectDAO,
            final @Lazy EntityFactory entityFactory) {

        return new JPAAnyUtils(userDAO, groupDAO, anyObjectDAO, entityFactory, AnyTypeKind.ANY_OBJECT, false);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyUtilsFactory anyUtilsFactory(
            @Qualifier("userAnyUtils")
            final AnyUtils userAnyUtils,
            @Qualifier("linkedAccountAnyUtils")
            final AnyUtils linkedAccountAnyUtils,
            @Qualifier("groupAnyUtils")
            final AnyUtils groupAnyUtils,
            @Qualifier("anyObjectAnyUtils")
            final AnyUtils anyObjectAnyUtils) {

        return new JPAAnyUtilsFactory(userAnyUtils, linkedAccountAnyUtils, groupAnyUtils, anyObjectAnyUtils);
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
    public TaskUtilsFactory taskUtilsFactory() {
        return new JPATaskUtilsFactory();
    }

    @ConditionalOnMissingBean
    @Bean
    public EntityManager domainEntityManager(final DomainEntityManagerFactoryBean entityManagerFactoryBean) {
        return SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactoryBean.getObject());
    }

    @ConditionalOnMissingBean
    @Bean
    public JpaRepositoryFactory jpaRepositoryFactory(final EntityManager domainEntityManager) {
        return new JpaRepositoryFactory(domainEntityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public AccessTokenDAO accessTokenDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final EntityManager domainEntityManager) {

        return jpaRepositoryFactory.getRepository(
                AccessTokenRepo.class,
                new AccessTokenRepoExtImpl(domainEntityManager));
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyMatchDAO anyMatchDAO(
            final @Lazy UserDAO userDAO,
            final @Lazy GroupDAO groupDAO,
            final @Lazy AnyObjectDAO anyObjectDAO,
            final RealmDAO realmDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final AnyUtilsFactory anyUtilsFactory,
            final PlainAttrValidationManager validator) {

        return new JPAAnyMatchDAO(
                userDAO,
                groupDAO,
                anyObjectDAO,
                realmDAO,
                plainSchemaDAO,
                anyUtilsFactory,
                validator);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyObjectDAO anyObjectDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final AnyUtilsFactory anyUtilsFactory,
            final @Lazy PlainSchemaDAO plainSchemaDAO,
            final @Lazy DerSchemaDAO derSchemaDAO,
            final @Lazy DynRealmDAO dynRealmDAO,
            final @Lazy UserDAO userDAO,
            final @Lazy GroupDAO groupDAO,
            final EntityManager domainEntityManager,
            final DomainRoutingDataSource domainDataSource) {

        return jpaRepositoryFactory.getRepository(
                AnyObjectRepo.class,
                new AnyObjectRepoExtImpl(
                        anyUtilsFactory,
                        plainSchemaDAO,
                        derSchemaDAO,
                        dynRealmDAO,
                        userDAO,
                        groupDAO,
                        domainEntityManager,
                        domainDataSource));
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
            final AnyUtilsFactory anyUtilsFactory,
            final PlainAttrValidationManager validator,
            final EntityManagerFactory domainEntityManagerFactory,
            final EntityManager domainEntityManager) {

        return new JPAAnySearchDAO(
                realmDAO,
                dynRealmDAO,
                userDAO,
                groupDAO,
                anyObjectDAO,
                schemaDAO,
                entityFactory,
                anyUtilsFactory,
                validator,
                domainEntityManagerFactory,
                domainEntityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyTypeClassDAO anyTypeClassDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final AnyTypeDAO anyTypeDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final DerSchemaDAO derSchemaDAO,
            final VirSchemaDAO virSchemaDAO,
            final @Lazy GroupDAO groupDAO,
            final ExternalResourceDAO resourceDAO,
            final EntityManager domainEntityManager) {

        return jpaRepositoryFactory.getRepository(
                AnyTypeClassRepo.class,
                new AnyTypeClassRepoExtImpl(
                        anyTypeDAO,
                        plainSchemaDAO,
                        derSchemaDAO,
                        virSchemaDAO,
                        groupDAO,
                        resourceDAO,
                        domainEntityManager));
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyTypeDAO anyTypeDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final RemediationDAO remediationDAO,
            final EntityManager domainEntityManager) {

        return jpaRepositoryFactory.getRepository(
                AnyTypeRepo.class,
                new AnyTypeRepoExtImpl(remediationDAO, domainEntityManager));
    }

    @ConditionalOnMissingBean
    @Bean
    public ApplicationDAO applicationDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final RoleDAO roleDAO,
            final @Lazy UserDAO userDAO,
            final EntityManager domainEntityManager) {

        return jpaRepositoryFactory.getRepository(
                ApplicationRepo.class,
                new ApplicationRepoExtImpl(roleDAO, userDAO, domainEntityManager));
    }

    @ConditionalOnMissingBean
    @Bean
    public AuditConfDAO auditConfDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final EntityManager domainEntityManager) {

        return jpaRepositoryFactory.getRepository(
                AuditConfRepo.class,
                new AuditConfRepoExtImpl(domainEntityManager));
    }

    @ConditionalOnMissingBean
    @Bean
    public AttrRepoDAO attrRepoDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final EntityManager domainEntityManager) {

        return jpaRepositoryFactory.getRepository(
                AttrRepoRepo.class,
                new AttrRepoRepoExtImpl(domainEntityManager));
    }

    @ConditionalOnMissingBean
    @Bean
    public AuthModuleDAO authModuleDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final PolicyDAO policyDAO,
            final EntityManager domainEntityManager) {

        return jpaRepositoryFactory.getRepository(
                AuthModuleRepo.class,
                new AuthModuleRepoExtImpl(policyDAO, domainEntityManager));
    }

    @ConditionalOnMissingBean
    @Bean
    public AuthProfileDAO authProfileDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final EntityManager domainEntityManager) {

        return jpaRepositoryFactory.getRepository(
                AuthProfileRepo.class,
                new AuthProfileRepoExtImpl(domainEntityManager));
    }

    @ConditionalOnMissingBean
    @Bean
    public BatchDAO batchDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final EntityManager domainEntityManager) {

        return jpaRepositoryFactory.getRepository(
                BatchRepo.class,
                new BatchRepoExtImpl(domainEntityManager));
    }

    @ConditionalOnMissingBean
    @Bean
    public CASSPClientAppDAO casSPClientAppDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final EntityManager domainEntityManager) {

        return jpaRepositoryFactory.getRepository(
                CASSPClientAppRepo.class,
                new CASSPClientAppRepoExtImpl(domainEntityManager));
    }

    @ConditionalOnMissingBean
    @Bean
    public ConnInstanceDAO connInstanceDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final @Lazy ExternalResourceDAO resourceDAO,
            final EntityManager domainEntityManager) {

        return jpaRepositoryFactory.getRepository(
                ConnInstanceRepo.class,
                new ConnInstanceRepoExtImpl(resourceDAO, domainEntityManager));
    }

    @ConditionalOnMissingBean
    @Bean
    public DelegationDAO delegationDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final EntityManager domainEntityManager) {

        return jpaRepositoryFactory.getRepository(
                DelegationRepo.class,
                new DelegationRepoExtImpl(domainEntityManager));
    }

    @ConditionalOnMissingBean
    @Bean
    public DerSchemaDAO derSchemaDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final @Lazy ExternalResourceDAO resourceDAO,
            final EntityManager domainEntityManager) {

        return jpaRepositoryFactory.getRepository(
                DerSchemaRepo.class,
                new DerSchemaRepoExtImpl(resourceDAO, domainEntityManager));
    }

    @ConditionalOnMissingBean
    @Bean
    public DynRealmDAO dynRealmDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final ApplicationEventPublisher publisher,
            final @Lazy UserDAO userDAO,
            final @Lazy GroupDAO groupDAO,
            final @Lazy AnyObjectDAO anyObjectDAO,
            final AnySearchDAO searchDAO,
            final AnyMatchDAO anyMatchDAO,
            final SearchCondVisitor searchCondVisitor,
            final EntityManager domainEntityManager) {

        return jpaRepositoryFactory.getRepository(
                DynRealmRepo.class,
                new DynRealmRepoExtImpl(
                        publisher,
                        userDAO,
                        groupDAO,
                        anyObjectDAO,
                        searchDAO,
                        anyMatchDAO,
                        searchCondVisitor,
                        domainEntityManager));
    }

    @ConditionalOnMissingBean
    @Bean
    public EntityCacheDAO entityCacheDAO(final EntityManagerFactory domainEntityManagerFactory) {
        return new JPAEntityCacheDAO(domainEntityManagerFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public FIQLQueryDAO fiqlQueryDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final EntityManager domainEntityManager) {

        return jpaRepositoryFactory.getRepository(
                FIQLQueryRepo.class,
                new FIQLQueryRepoExtImpl(domainEntityManager));
    }

    @ConditionalOnMissingBean
    @Bean
    public GroupDAO groupDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final ApplicationEventPublisher publisher,
            final AnyUtilsFactory anyUtilsFactory,
            final @Lazy PlainSchemaDAO plainSchemaDAO,
            final @Lazy DerSchemaDAO derSchemaDAO,
            final @Lazy DynRealmDAO dynRealmDAO,
            final AnyMatchDAO anyMatchDAO,
            final @Lazy UserDAO userDAO,
            final @Lazy AnyObjectDAO anyObjectDAO,
            final AnySearchDAO anySearchDAO,
            final SearchCondVisitor searchCondVisitor,
            final EntityManager domainEntityManager,
            final DomainRoutingDataSource domainDataSource) {

        return jpaRepositoryFactory.getRepository(
                GroupRepo.class,
                new GroupRepoExtImpl(
                        anyUtilsFactory,
                        publisher,
                        plainSchemaDAO,
                        derSchemaDAO,
                        dynRealmDAO,
                        anyMatchDAO,
                        userDAO,
                        anyObjectDAO,
                        anySearchDAO,
                        searchCondVisitor,
                        domainEntityManager,
                        domainDataSource));
    }

    @ConditionalOnMissingBean
    @Bean
    public ImplementationDAO implementationDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final @Lazy ExternalResourceDAO resourceDAO,
            final @Lazy EntityCacheDAO entityCacheDAO,
            final EntityManager domainEntityManager) {

        return jpaRepositoryFactory.getRepository(
                ImplementationRepo.class,
                new ImplementationRepoExtImpl(resourceDAO, entityCacheDAO, domainEntityManager));
    }

    @ConditionalOnMissingBean
    @Bean
    public JobStatusDAO jobStatusDAO() {
        return new JPAJobStatusDAO();
    }

    @ConditionalOnMissingBean
    @Bean
    public MailTemplateDAO mailTemplateDAO(final JpaRepositoryFactory jpaRepositoryFactory) {
        return jpaRepositoryFactory.getRepository(MailTemplateRepo.class);
    }

    @ConditionalOnMissingBean
    @Bean
    public NotificationDAO notificationDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final TaskDAO taskDAO,
            final EntityManager domainEntityManager) {

        return jpaRepositoryFactory.getRepository(
                NotificationRepo.class,
                new NotificationRepoExtImpl(taskDAO, domainEntityManager));
    }

    @ConditionalOnMissingBean
    @Bean
    public OIDCJWKSDAO oidcJWKSDAO(final EntityManager domainEntityManager) {
        return new JPAOIDCJWKSDAO(domainEntityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public OIDCRPClientAppDAO oidcRPClientAppDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final EntityManager domainEntityManager) {

        return jpaRepositoryFactory.getRepository(
                OIDCRPClientAppRepo.class,
                new OIDCRPClientAppRepoExtImpl(domainEntityManager));
    }

    @ConditionalOnMissingBean
    @Bean
    public PersistenceInfoDAO persistenceInfoDAO(final EntityManagerFactory domainEntityManagerFactory) {
        return new JPAPersistenceInfoDAO(domainEntityManagerFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public PlainAttrValueDAO plainAttrValueDAO(final EntityManager domainEntityManager) {
        return new JPAPlainAttrValueDAO(domainEntityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public PlainSchemaDAO plainSchemaDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final AnyUtilsFactory anyUtilsFactory,
            final @Lazy ExternalResourceDAO resourceDAO,
            final EntityManager domainEntityManager) {

        return jpaRepositoryFactory.getRepository(
                PlainSchemaRepo.class,
                new PlainSchemaRepoExtImpl(anyUtilsFactory, resourceDAO, domainEntityManager));
    }

    @ConditionalOnMissingBean
    @Bean
    public PolicyDAO policyDAO(
            final @Lazy RealmDAO realmDAO,
            final @Lazy ExternalResourceDAO resourceDAO,
            final @Lazy CASSPClientAppDAO casSPClientAppDAO,
            final @Lazy OIDCRPClientAppDAO oidcRPClientAppDAO,
            final @Lazy SAML2SPClientAppDAO saml2SPClientAppDAO,
            final @Lazy EntityCacheDAO entityCacheDAO,
            final EntityManager domainEntityManager) {

        return new JPAPolicyDAO(
                realmDAO,
                resourceDAO,
                casSPClientAppDAO,
                oidcRPClientAppDAO,
                saml2SPClientAppDAO,
                entityCacheDAO,
                domainEntityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public RealmDAO realmDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final @Lazy RoleDAO roleDAO,
            final ApplicationEventPublisher publisher,
            final EntityManager domainEntityManager) {

        return jpaRepositoryFactory.getRepository(
                RealmRepo.class,
                new RealmRepoExtImpl(roleDAO, publisher, domainEntityManager));
    }

    @ConditionalOnMissingBean
    @Bean
    public RelationshipTypeDAO relationshipTypeDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final EntityManager domainEntityManager) {

        return jpaRepositoryFactory.getRepository(
                RelationshipTypeRepo.class,
                new RelationshipTypeRepoExtImpl(domainEntityManager));
    }

    @ConditionalOnMissingBean
    @Bean
    public RemediationDAO remediationDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final EntityManager domainEntityManager) {

        return jpaRepositoryFactory.getRepository(
                RemediationRepo.class,
                new RemediationRepoExtImpl(domainEntityManager));
    }

    @ConditionalOnMissingBean
    @Bean
    public ReportDAO reportDAO(final JpaRepositoryFactory jpaRepositoryFactory) {
        return jpaRepositoryFactory.getRepository(ReportRepo.class);
    }

    @ConditionalOnMissingBean
    @Bean
    public ReportExecDAO reportExecDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final EntityManager domainEntityManager) {

        return jpaRepositoryFactory.getRepository(
                ReportExecRepo.class,
                new ReportExecRepoExtImpl(domainEntityManager));
    }

    @ConditionalOnMissingBean
    @Bean
    public ExternalResourceDAO resourceDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final TaskDAO taskDAO,
            final AnyObjectDAO anyObjectDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final PolicyDAO policyDAO,
            final VirSchemaDAO virSchemaDAO,
            final RealmDAO realmDAO,
            final EntityManager domainEntityManager) {

        return jpaRepositoryFactory.getRepository(
                ExternalResourceRepo.class,
                new ExternalResourceRepoExtImpl(
                        taskDAO,
                        anyObjectDAO,
                        userDAO,
                        groupDAO,
                        policyDAO,
                        virSchemaDAO,
                        realmDAO,
                        domainEntityManager));
    }

    @ConditionalOnMissingBean
    @Bean
    public RoleDAO roleDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final ApplicationEventPublisher publisher,
            final @Lazy AnyMatchDAO anyMatchDAO,
            final @Lazy AnySearchDAO anySearchDAO,
            final DelegationDAO delegationDAO,
            final SearchCondVisitor searchCondVisitor,
            final EntityManager domainEntityManager) {

        return jpaRepositoryFactory.getRepository(
                RoleRepo.class,
                new RoleRepoExtImpl(
                        publisher,
                        anyMatchDAO,
                        anySearchDAO,
                        delegationDAO,
                        searchCondVisitor,
                        domainEntityManager));
    }

    @ConditionalOnMissingBean
    @Bean
    public SAML2IdPEntityDAO saml2IdPEntityDAO(final JpaRepositoryFactory jpaRepositoryFactory) {
        return jpaRepositoryFactory.getRepository(SAML2IdPEntityRepo.class);
    }

    @ConditionalOnMissingBean
    @Bean
    public SAML2SPClientAppDAO saml2SPClientAppDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final EntityManager domainEntityManager) {

        return jpaRepositoryFactory.getRepository(
                SAML2SPClientAppRepo.class,
                new SAML2SPClientAppRepoExtImpl(domainEntityManager));
    }

    @ConditionalOnMissingBean
    @Bean
    public SAML2SPEntityDAO saml2SPEntityDAO(final JpaRepositoryFactory jpaRepositoryFactory) {
        return jpaRepositoryFactory.getRepository(SAML2SPEntityRepo.class);
    }

    @ConditionalOnMissingBean
    @Bean
    public SecurityQuestionDAO securityQuestionDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final UserDAO userDAO,
            final EntityManager domainEntityManager) {

        return jpaRepositoryFactory.getRepository(
                SecurityQuestionRepo.class,
                new SecurityQuestionRepoExtImpl(userDAO, domainEntityManager));
    }

    @ConditionalOnMissingBean
    @Bean
    public SRARouteDAO sraRouteDAO(final JpaRepositoryFactory jpaRepositoryFactory) {
        return jpaRepositoryFactory.getRepository(SRARouteRepo.class);
    }

    @ConditionalOnMissingBean
    @Bean
    public TaskDAO taskDAO(
            final RealmDAO realmDAO,
            final RemediationDAO remediationDAO,
            final TaskUtilsFactory taskUtilsFactory,
            final SecurityProperties securityProperties,
            final EntityManager domainEntityManager) {

        return new JPATaskDAO(realmDAO, remediationDAO, taskUtilsFactory, securityProperties, domainEntityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public TaskExecDAO taskExecDAO(
            final TaskDAO taskDAO,
            final TaskUtilsFactory taskUtilsFactory,
            final EntityManager domainEntityManager) {

        return new JPATaskExecDAO(taskDAO, taskUtilsFactory, domainEntityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public UserDAO userDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final SecurityProperties securityProperties,
            final AnyUtilsFactory anyUtilsFactory,
            final @Lazy PlainSchemaDAO plainSchemaDAO,
            final @Lazy DerSchemaDAO derSchemaDAO,
            final @Lazy DynRealmDAO dynRealmDAO,
            final RoleDAO roleDAO,
            final AccessTokenDAO accessTokenDAO,
            final @Lazy GroupDAO groupDAO,
            final DelegationDAO delegationDAO,
            final FIQLQueryDAO fiqlQueryDAO,
            final EntityManager domainEntityManager,
            final DomainRoutingDataSource domainDataSource) {

        return jpaRepositoryFactory.getRepository(
                UserRepo.class,
                new UserRepoExtImpl(
                        anyUtilsFactory,
                        plainSchemaDAO,
                        derSchemaDAO,
                        dynRealmDAO,
                        roleDAO,
                        accessTokenDAO,
                        groupDAO,
                        delegationDAO,
                        fiqlQueryDAO,
                        securityProperties,
                        domainEntityManager,
                        domainDataSource));
    }

    @ConditionalOnMissingBean
    @Bean
    public VirSchemaDAO virSchemaDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final @Lazy ExternalResourceDAO resourceDAO,
            final EntityManager domainEntityManager) {

        return jpaRepositoryFactory.getRepository(
                VirSchemaRepo.class,
                new VirSchemaRepoExtImpl(resourceDAO, domainEntityManager));
    }

    @ConditionalOnMissingBean
    @Bean
    public WAConfigDAO waConfigDAO(final JpaRepositoryFactory jpaRepositoryFactory) {
        return jpaRepositoryFactory.getRepository(WAConfigRepo.class);
    }
}
