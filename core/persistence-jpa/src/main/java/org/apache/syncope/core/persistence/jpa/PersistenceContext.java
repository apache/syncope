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
import java.util.HashMap;
import java.util.Map;
import javax.sql.DataSource;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.model.JPADomain;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.DomainHolder;
import org.apache.syncope.core.persistence.api.DomainRegistry;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.AnyMatchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.AttrRepoDAO;
import org.apache.syncope.core.persistence.api.dao.AuditConfDAO;
import org.apache.syncope.core.persistence.api.dao.AuditEventDAO;
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
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
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
import org.apache.syncope.core.persistence.api.dao.keymaster.ConfParamDAO;
import org.apache.syncope.core.persistence.api.dao.keymaster.DomainDAO;
import org.apache.syncope.core.persistence.api.dao.keymaster.NetworkServiceDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.persistence.common.CommonPersistenceContext;
import org.apache.syncope.core.persistence.common.RuntimeDomainLoader;
import org.apache.syncope.core.persistence.common.dao.AnyFinder;
import org.apache.syncope.core.persistence.jpa.content.XMLContentExporter;
import org.apache.syncope.core.persistence.jpa.content.XMLContentLoader;
import org.apache.syncope.core.persistence.jpa.dao.JPAAnyMatchDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAAuditEventDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPABatchDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAEntityCacheDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAJobStatusDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAOIDCJWKSDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAPersistenceInfoDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPAPolicyDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPARealmDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPARealmSearchDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPATaskDAO;
import org.apache.syncope.core.persistence.jpa.dao.JPATaskExecDAO;
import org.apache.syncope.core.persistence.jpa.dao.repo.AccessTokenRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.AnyObjectRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.AnyObjectRepoExt;
import org.apache.syncope.core.persistence.jpa.dao.repo.AnyObjectRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.AnyTypeClassRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.AnyTypeClassRepoExt;
import org.apache.syncope.core.persistence.jpa.dao.repo.AnyTypeClassRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.AnyTypeRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.AnyTypeRepoExt;
import org.apache.syncope.core.persistence.jpa.dao.repo.AnyTypeRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.AttrRepoRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.AttrRepoRepoExt;
import org.apache.syncope.core.persistence.jpa.dao.repo.AttrRepoRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.AuditConfRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.AuthModuleRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.AuthModuleRepoExt;
import org.apache.syncope.core.persistence.jpa.dao.repo.AuthModuleRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.AuthProfileRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.CASSPClientAppRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.CASSPClientAppRepoExt;
import org.apache.syncope.core.persistence.jpa.dao.repo.CASSPClientAppRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.ConfParamRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.ConnInstanceRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.ConnInstanceRepoExt;
import org.apache.syncope.core.persistence.jpa.dao.repo.ConnInstanceRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.DelegationRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.DerSchemaRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.DerSchemaRepoExt;
import org.apache.syncope.core.persistence.jpa.dao.repo.DerSchemaRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.DomainRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.DynRealmRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.DynRealmRepoExt;
import org.apache.syncope.core.persistence.jpa.dao.repo.DynRealmRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.ExternalResourceRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.ExternalResourceRepoExt;
import org.apache.syncope.core.persistence.jpa.dao.repo.ExternalResourceRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.FIQLQueryRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.FIQLQueryRepoExt;
import org.apache.syncope.core.persistence.jpa.dao.repo.FIQLQueryRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.GroupRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.GroupRepoExt;
import org.apache.syncope.core.persistence.jpa.dao.repo.GroupRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.ImplementationRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.ImplementationRepoExt;
import org.apache.syncope.core.persistence.jpa.dao.repo.ImplementationRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.MailTemplateRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.NetworkServiceRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.NetworkServiceRepoExt;
import org.apache.syncope.core.persistence.jpa.dao.repo.NetworkServiceRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.NotificationRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.NotificationRepoExt;
import org.apache.syncope.core.persistence.jpa.dao.repo.NotificationRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.OIDCRPClientAppRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.OIDCRPClientAppRepoExt;
import org.apache.syncope.core.persistence.jpa.dao.repo.OIDCRPClientAppRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.PlainSchemaRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.PlainSchemaRepoExt;
import org.apache.syncope.core.persistence.jpa.dao.repo.RelationshipTypeRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.RelationshipTypeRepoExt;
import org.apache.syncope.core.persistence.jpa.dao.repo.RelationshipTypeRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.RemediationRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.RemediationRepoExt;
import org.apache.syncope.core.persistence.jpa.dao.repo.RemediationRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.ReportExecRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.ReportExecRepoExt;
import org.apache.syncope.core.persistence.jpa.dao.repo.ReportExecRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.ReportRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.RoleRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.RoleRepoExt;
import org.apache.syncope.core.persistence.jpa.dao.repo.RoleRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.SAML2IdPEntityRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.SAML2SPClientAppRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.SAML2SPClientAppRepoExt;
import org.apache.syncope.core.persistence.jpa.dao.repo.SAML2SPClientAppRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.SAML2SPEntityRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.SRARouteRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.SecurityQuestionRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.SecurityQuestionRepoExt;
import org.apache.syncope.core.persistence.jpa.dao.repo.SecurityQuestionRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.UserRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.UserRepoExt;
import org.apache.syncope.core.persistence.jpa.dao.repo.UserRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.VirSchemaRepo;
import org.apache.syncope.core.persistence.jpa.dao.repo.VirSchemaRepoExt;
import org.apache.syncope.core.persistence.jpa.dao.repo.VirSchemaRepoExtImpl;
import org.apache.syncope.core.persistence.jpa.dao.repo.WAConfigRepo;
import org.apache.syncope.core.persistence.jpa.entity.task.JPATaskUtilsFactory;
import org.apache.syncope.core.persistence.jpa.spring.CommonEntityManagerFactoryConf;
import org.apache.syncope.core.persistence.jpa.spring.DomainRoutingEntityManagerFactory;
import org.apache.syncope.core.persistence.jpa.spring.MultiJarAwarePersistenceUnitPostProcessor;
import org.apache.syncope.core.persistence.jpa.spring.SyncopeJPARepository;
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
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.jndi.JndiObjectFactoryBean;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.SharedEntityManagerCreator;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@EnableConfigurationProperties(PersistenceProperties.class)
@Import(CommonPersistenceContext.class)
@Configuration(proxyBeanMethods = false)
public class PersistenceContext {

    private static final Logger OPENJPA_LOG = LoggerFactory.getLogger("org.apache.openjpa");

    @ConditionalOnMissingBean
    @Bean
    public CommonEntityManagerFactoryConf commonEMFConf(
            final PersistenceProperties props,
            @Qualifier("MasterDataSource")
            final JndiObjectFactoryBean masterDataSource) {

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

        jpaPropertyMap.put("openjpa.RemoteCommitProvider", props.getRemoteCommitProvider());

        commonEMFConf.setJpaPropertyMap(jpaPropertyMap);

        commonEMFConf.getDomains().put(SyncopeConstants.MASTER_DOMAIN, (DataSource) masterDataSource.getObject());

        return commonEMFConf;
    }

    @ConditionalOnMissingBean
    @Bean
    public DomainRoutingEntityManagerFactory entityManagerFactory(
            final PersistenceProperties props,
            @Qualifier("MasterDataSource")
            final JndiObjectFactoryBean masterDataSource,
            final CommonEntityManagerFactoryConf commonEMFConf) {

        DomainRoutingEntityManagerFactory emf = new DomainRoutingEntityManagerFactory(commonEMFConf);
        emf.master(props, masterDataSource);
        return emf;
    }

    @ConditionalOnMissingBean
    @Bean
    public PlatformTransactionManager domainTransactionManager(final EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public TransactionTemplate domainTransactionTemplate(final PlatformTransactionManager domainTransactionManager) {
        return new TransactionTemplate(domainTransactionManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public XMLContentLoader xmlContentLoader(
            final DomainHolder<DataSource> domainHolder,
            final PersistenceProperties props,
            final ResourceLoader resourceLoader,
            final Environment env) {

        return new XMLContentLoader(
                domainHolder,
                resourceLoader.getResource(props.getViewsXML()),
                resourceLoader.getResource(props.getIndexesXML()),
                env);
    }

    @ConditionalOnMissingBean
    @Bean
    public XMLContentExporter xmlContentExporter(
            final DomainHolder<DataSource> domainHolder,
            final RealmSearchDAO realmSearchDAO,
            final EntityManagerFactory entityManagerFactory) {

        return new XMLContentExporter(domainHolder, realmSearchDAO, entityManagerFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public DomainRegistry<JPADomain> domainRegistry(final ConfigurableApplicationContext ctx) {
        return new JPADomainRegistry(ctx);
    }

    @ConditionalOnMissingBean
    @Bean
    public RuntimeDomainLoader<JPADomain> runtimeDomainLoader(
            final DomainHolder<?> domainHolder,
            final DomainRegistry<JPADomain> domainRegistry,
            final DomainRoutingEntityManagerFactory entityManagerFactory,
            final @Lazy DomainOps domainOps,
            final ConfigurableApplicationContext ctx) {

        return new JPARuntimeDomainLoader(domainHolder, domainRegistry, entityManagerFactory, domainOps, ctx);
    }

    @ConditionalOnMissingBean
    @Bean
    public StartupDomainLoader startupDomainLoader(
            final PersistenceProperties props,
            final ResourceLoader resourceLoader,
            final DomainOps domainOps,
            final DomainHolder<?> domainHolder,
            final DomainRegistry<JPADomain> domainRegistry) {

        return new StartupDomainLoader(domainOps, domainHolder, props, resourceLoader, domainRegistry);
    }

    @ConditionalOnMissingBean
    @Bean
    public TaskUtilsFactory taskUtilsFactory() {
        return new JPATaskUtilsFactory();
    }

    @ConditionalOnMissingBean
    @Bean
    public EntityManager entityManager(final EntityManagerFactory entityManagerFactory) {
        return SharedEntityManagerCreator.createSharedEntityManager(entityManagerFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public JpaRepositoryFactory jpaRepositoryFactory(final EntityManager entityManager) {
        return new JpaRepositoryFactory(entityManager) {

            @Override
            protected Class<?> getRepositoryBaseClass(final RepositoryMetadata metadata) {
                return SyncopeJPARepository.class;
            }
        };
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyFinder anyFinder(final @Lazy PlainSchemaDAO plainSchemaDAO, final @Lazy AnySearchDAO anySearchDAO) {
        return new AnyFinder(plainSchemaDAO, anySearchDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public AccessTokenDAO accessTokenDAO(final JpaRepositoryFactory jpaRepositoryFactory) {
        return jpaRepositoryFactory.getRepository(AccessTokenRepo.class);
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
            final PlainAttrValidationManager validator,
            final EntityFactory entityFactory) {

        return new JPAAnyMatchDAO(
                userDAO,
                groupDAO,
                anyObjectDAO,
                realmDAO,
                plainSchemaDAO,
                anyUtilsFactory,
                validator,
                entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyObjectRepoExt anyObjectRepoExt(
            final AnyUtilsFactory anyUtilsFactory,
            final @Lazy DynRealmDAO dynRealmDAO,
            final @Lazy PlainSchemaDAO plainSchemaDAO,
            final @Lazy UserDAO userDAO,
            final @Lazy GroupDAO groupDAO,
            final EntityManager entityManager,
            final AnyFinder anyFinder) {

        return new AnyObjectRepoExtImpl(
                anyUtilsFactory,
                dynRealmDAO,
                plainSchemaDAO,
                userDAO,
                groupDAO,
                entityManager,
                anyFinder);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyObjectDAO anyObjectDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final AnyObjectRepoExt anyObjectRepoExt) {

        return jpaRepositoryFactory.getRepository(AnyObjectRepo.class, anyObjectRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyTypeClassRepoExt anyTypeClassRepoExt(
            final AnyTypeDAO anyTypeDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final DerSchemaDAO derSchemaDAO,
            final VirSchemaDAO virSchemaDAO,
            final @Lazy GroupDAO groupDAO,
            final ExternalResourceDAO resourceDAO,
            final EntityManager entityManager) {

        return new AnyTypeClassRepoExtImpl(
                anyTypeDAO,
                plainSchemaDAO,
                derSchemaDAO,
                virSchemaDAO,
                groupDAO,
                resourceDAO,
                entityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyTypeClassDAO anyTypeClassDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final AnyTypeClassRepoExt anyTypeClassRepoExt) {

        return jpaRepositoryFactory.getRepository(AnyTypeClassRepo.class, anyTypeClassRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyTypeRepoExt anyTypeRepoExt(
            final RemediationDAO remediationDAO,
            final RelationshipTypeDAO relationshipTypeDAO,
            final EntityManager entityManager) {

        return new AnyTypeRepoExtImpl(remediationDAO, relationshipTypeDAO, entityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyTypeDAO anyTypeDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final AnyTypeRepoExt anyTypeRepoExt) {

        return jpaRepositoryFactory.getRepository(AnyTypeRepo.class, anyTypeRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public AuditConfDAO auditConfDAO(final JpaRepositoryFactory jpaRepositoryFactory) {
        return jpaRepositoryFactory.getRepository(AuditConfRepo.class);
    }

    @ConditionalOnMissingBean
    @Bean
    public AuditEventDAO auditEventDAO(final EntityManager entityManager) {
        return new JPAAuditEventDAO(entityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public AttrRepoRepoExt attrRepoRepoExt(final EntityManager entityManager) {
        return new AttrRepoRepoExtImpl(entityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public AttrRepoDAO attrRepoDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final AttrRepoRepoExt attrRepoRepoExt) {

        return jpaRepositoryFactory.getRepository(AttrRepoRepo.class, attrRepoRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public AuthModuleRepoExt authModuleRepoExt(final PolicyDAO policyDAO, final EntityManager entityManager) {
        return new AuthModuleRepoExtImpl(policyDAO, entityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public AuthModuleDAO authModuleDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final AuthModuleRepoExt authModuleRepoExt) {

        return jpaRepositoryFactory.getRepository(AuthModuleRepo.class, authModuleRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public AuthProfileDAO authProfileDAO(final JpaRepositoryFactory jpaRepositoryFactory) {
        return jpaRepositoryFactory.getRepository(AuthProfileRepo.class);
    }

    @ConditionalOnMissingBean
    @Bean
    public BatchDAO batchDAO(final EntityManager entityManager) {
        return new JPABatchDAO(entityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public CASSPClientAppRepoExt casSPClientAppRepoExt(final EntityManager entityManager) {
        return new CASSPClientAppRepoExtImpl(entityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public CASSPClientAppDAO casSPClientAppDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final CASSPClientAppRepoExt casSPClientAppRepoExt) {

        return jpaRepositoryFactory.getRepository(CASSPClientAppRepo.class, casSPClientAppRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public ConnInstanceRepoExt connInstanceRepoExt(
            final @Lazy ExternalResourceDAO resourceDAO,
            final EntityManager entityManager) {

        return new ConnInstanceRepoExtImpl(resourceDAO, entityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public ConnInstanceDAO connInstanceDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final ConnInstanceRepoExt connInstanceRepoExt) {

        return jpaRepositoryFactory.getRepository(ConnInstanceRepo.class, connInstanceRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public DelegationDAO delegationDAO(final JpaRepositoryFactory jpaRepositoryFactory) {
        return jpaRepositoryFactory.getRepository(DelegationRepo.class);
    }

    @ConditionalOnMissingBean
    @Bean
    public DerSchemaRepoExt derSchemaRepoExt(
            final @Lazy ExternalResourceDAO resourceDAO,
            final EntityManager entityManager) {

        return new DerSchemaRepoExtImpl(resourceDAO, entityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public DerSchemaDAO derSchemaDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final DerSchemaRepoExt derSchemaRepoExt) {

        return jpaRepositoryFactory.getRepository(DerSchemaRepo.class, derSchemaRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public DynRealmRepoExt dynRealmRepoExt(
            final ApplicationEventPublisher publisher,
            final @Lazy UserDAO userDAO,
            final @Lazy GroupDAO groupDAO,
            final @Lazy AnyObjectDAO anyObjectDAO,
            final AnySearchDAO anySearchDAO,
            final AnyMatchDAO anyMatchDAO,
            final SearchCondVisitor searchCondVisitor,
            final EntityManager entityManager) {

        return new DynRealmRepoExtImpl(
                publisher,
                userDAO,
                groupDAO,
                anyObjectDAO,
                anySearchDAO,
                anyMatchDAO,
                searchCondVisitor,
                entityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public DynRealmDAO dynRealmDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final DynRealmRepoExt dynRealmRepoExt) {

        return jpaRepositoryFactory.getRepository(DynRealmRepo.class, dynRealmRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public EntityCacheDAO entityCacheDAO(final EntityManagerFactory entityManagerFactory) {
        return new JPAEntityCacheDAO(entityManagerFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public FIQLQueryRepoExt fiqlQueryRepoExt(final EntityManager entityManager) {
        return new FIQLQueryRepoExtImpl(entityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public FIQLQueryDAO fiqlQueryDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final FIQLQueryRepoExt fiqlQueryRepoExt) {

        return jpaRepositoryFactory.getRepository(FIQLQueryRepo.class, fiqlQueryRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public GroupRepoExt groupRepoExt(
            final ApplicationEventPublisher publisher,
            final AnyUtilsFactory anyUtilsFactory,
            final @Lazy DynRealmDAO dynRealmDAO,
            final @Lazy PlainSchemaDAO plainSchemaDAO,
            final AnyMatchDAO anyMatchDAO,
            final @Lazy UserDAO userDAO,
            final @Lazy AnyObjectDAO anyObjectDAO,
            final AnySearchDAO anySearchDAO,
            final SearchCondVisitor searchCondVisitor,
            final EntityManager entityManager,
            final AnyFinder anyFinder) {

        return new GroupRepoExtImpl(
                anyUtilsFactory,
                publisher,
                dynRealmDAO,
                plainSchemaDAO,
                anyMatchDAO,
                userDAO,
                anyObjectDAO,
                anySearchDAO,
                searchCondVisitor,
                entityManager,
                anyFinder);
    }

    @ConditionalOnMissingBean
    @Bean
    public GroupDAO groupDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final GroupRepoExt groupRepoExt) {

        return jpaRepositoryFactory.getRepository(GroupRepo.class, groupRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public ImplementationRepoExt implementationRepoExt(
            final @Lazy ExternalResourceDAO resourceDAO,
            final @Lazy EntityCacheDAO entityCacheDAO,
            final EntityManager entityManager) {

        return new ImplementationRepoExtImpl(resourceDAO, entityCacheDAO, entityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public ImplementationDAO implementationDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final ImplementationRepoExt implementationRepoExt) {

        return jpaRepositoryFactory.getRepository(ImplementationRepo.class, implementationRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public JobStatusDAO jobStatusDAO(final EntityManager entityManager) {
        return new JPAJobStatusDAO(entityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public MailTemplateDAO mailTemplateDAO(final JpaRepositoryFactory jpaRepositoryFactory) {
        return jpaRepositoryFactory.getRepository(MailTemplateRepo.class);
    }

    @ConditionalOnMissingBean
    @Bean
    public NotificationRepoExt notificationRepoExt(
            final TaskDAO taskDAO,
            final EntityManager entityManager) {

        return new NotificationRepoExtImpl(taskDAO, entityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public NotificationDAO notificationDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final NotificationRepoExt notificationRepoExt) {

        return jpaRepositoryFactory.getRepository(NotificationRepo.class, notificationRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public OIDCJWKSDAO oidcJWKSDAO(final EntityManager entityManager) {
        return new JPAOIDCJWKSDAO(entityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public OIDCRPClientAppRepoExt oidcRPClientAppRepoExt(final EntityManager entityManager) {
        return new OIDCRPClientAppRepoExtImpl(entityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public OIDCRPClientAppDAO oidcRPClientAppDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final OIDCRPClientAppRepoExt oidcRPClientAppRepoExt) {

        return jpaRepositoryFactory.getRepository(OIDCRPClientAppRepo.class, oidcRPClientAppRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public PersistenceInfoDAO persistenceInfoDAO(final EntityManagerFactory entityManagerFactory) {
        return new JPAPersistenceInfoDAO(entityManagerFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public PlainSchemaDAO plainSchemaDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final PlainSchemaRepoExt plainSchemaRepoExt) {

        return jpaRepositoryFactory.getRepository(PlainSchemaRepo.class, plainSchemaRepoExt);
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
            final EntityManager entityManager) {

        return new JPAPolicyDAO(
                realmDAO,
                resourceDAO,
                casSPClientAppDAO,
                oidcRPClientAppDAO,
                saml2SPClientAppDAO,
                entityCacheDAO,
                entityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public RealmDAO realmDAO(
            final @Lazy RoleDAO roleDAO,
            final RealmSearchDAO realmSearchDAO,
            final ApplicationEventPublisher publisher,
            final EntityManager entityManager) {

        return new JPARealmDAO(roleDAO, realmSearchDAO, publisher, entityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public RealmSearchDAO realmSearchDAO(final EntityManager entityManager) {
        return new JPARealmSearchDAO(entityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public RelationshipTypeRepoExt relationshipTypeRepoExt(final EntityManager entityManager) {
        return new RelationshipTypeRepoExtImpl(entityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public RelationshipTypeDAO relationshipTypeDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final RelationshipTypeRepoExt relationshipTypeRepoExt) {

        return jpaRepositoryFactory.getRepository(RelationshipTypeRepo.class, relationshipTypeRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public RemediationRepoExt remediationRepoExt(final EntityManager entityManager) {
        return new RemediationRepoExtImpl(entityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public RemediationDAO remediationDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final RemediationRepoExt remediationRepoExt) {

        return jpaRepositoryFactory.getRepository(RemediationRepo.class, remediationRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public ReportDAO reportDAO(final JpaRepositoryFactory jpaRepositoryFactory) {
        return jpaRepositoryFactory.getRepository(ReportRepo.class);
    }

    @ConditionalOnMissingBean
    @Bean
    public ReportExecRepoExt reportExecRepoExt(final EntityManager entityManager) {
        return new ReportExecRepoExtImpl(entityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public ReportExecDAO reportExecDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final ReportExecRepoExt reportExecRepoExt) {

        return jpaRepositoryFactory.getRepository(ReportExecRepo.class, reportExecRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public ExternalResourceRepoExt resourceRepoExt(
            final TaskDAO taskDAO,
            final AnyObjectDAO anyObjectDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final VirSchemaDAO virSchemaDAO,
            final RealmDAO realmDAO,
            final EntityManager entityManager) {

        return new ExternalResourceRepoExtImpl(
                taskDAO,
                anyObjectDAO,
                userDAO,
                groupDAO,
                virSchemaDAO,
                realmDAO,
                entityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public ExternalResourceDAO resourceDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final ExternalResourceRepoExt resourceRepoExt) {

        return jpaRepositoryFactory.getRepository(ExternalResourceRepo.class, resourceRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public RoleRepoExt roleRepoExt(
            final ApplicationEventPublisher publisher,
            final @Lazy AnyMatchDAO anyMatchDAO,
            final @Lazy AnySearchDAO anySearchDAO,
            final DelegationDAO delegationDAO,
            final SearchCondVisitor searchCondVisitor,
            final EntityManager entityManager) {

        return new RoleRepoExtImpl(
                publisher,
                anyMatchDAO,
                anySearchDAO,
                delegationDAO,
                searchCondVisitor,
                entityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public RoleDAO roleDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final RoleRepoExt roleRepoExt) {

        return jpaRepositoryFactory.getRepository(RoleRepo.class, roleRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public SAML2IdPEntityDAO saml2IdPEntityDAO(final JpaRepositoryFactory jpaRepositoryFactory) {
        return jpaRepositoryFactory.getRepository(SAML2IdPEntityRepo.class);
    }

    @ConditionalOnMissingBean
    @Bean
    public SAML2SPClientAppRepoExt saml2SPClientAppRepoExt(final EntityManager entityManager) {
        return new SAML2SPClientAppRepoExtImpl(entityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public SAML2SPClientAppDAO saml2SPClientAppDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final SAML2SPClientAppRepoExt saml2SPClientAppRepoExt) {

        return jpaRepositoryFactory.getRepository(SAML2SPClientAppRepo.class, saml2SPClientAppRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public SAML2SPEntityDAO saml2SPEntityDAO(final JpaRepositoryFactory jpaRepositoryFactory) {
        return jpaRepositoryFactory.getRepository(SAML2SPEntityRepo.class);
    }

    @ConditionalOnMissingBean
    @Bean
    public SecurityQuestionRepoExt securityQuestionRepoExt(
            final UserDAO userDAO,
            final EntityManager entityManager) {

        return new SecurityQuestionRepoExtImpl(userDAO, entityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public SecurityQuestionDAO securityQuestionDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final SecurityQuestionRepoExt securityQuestionRepoExt) {

        return jpaRepositoryFactory.getRepository(SecurityQuestionRepo.class, securityQuestionRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public SRARouteDAO sraRouteDAO(final JpaRepositoryFactory jpaRepositoryFactory) {
        return jpaRepositoryFactory.getRepository(SRARouteRepo.class);
    }

    @ConditionalOnMissingBean
    @Bean
    public TaskDAO taskDAO(
            final RealmSearchDAO realmSearchDAO,
            final RemediationDAO remediationDAO,
            final TaskUtilsFactory taskUtilsFactory,
            final SecurityProperties securityProperties,
            final EntityManager entityManager) {

        return new JPATaskDAO(realmSearchDAO, remediationDAO, taskUtilsFactory, securityProperties, entityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public TaskExecDAO taskExecDAO(
            final TaskDAO taskDAO,
            final TaskUtilsFactory taskUtilsFactory,
            final EntityManager entityManager) {

        return new JPATaskExecDAO(taskDAO, taskUtilsFactory, entityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public UserRepoExt userRepoExt(
            final SecurityProperties securityProperties,
            final AnyUtilsFactory anyUtilsFactory,
            final @Lazy DynRealmDAO dynRealmDAO,
            final @Lazy PlainSchemaDAO plainSchemaDAO,
            final RoleDAO roleDAO,
            final AccessTokenDAO accessTokenDAO,
            final @Lazy GroupDAO groupDAO,
            final DelegationDAO delegationDAO,
            final FIQLQueryDAO fiqlQueryDAO,
            final EntityManager entityManager,
            final AnyFinder anyFinder) {

        return new UserRepoExtImpl(
                anyUtilsFactory,
                dynRealmDAO,
                plainSchemaDAO,
                roleDAO,
                accessTokenDAO,
                groupDAO,
                delegationDAO,
                fiqlQueryDAO,
                securityProperties,
                entityManager,
                anyFinder);
    }

    @ConditionalOnMissingBean
    @Bean
    public UserDAO userDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final UserRepoExt userRepoExt) {

        return jpaRepositoryFactory.getRepository(UserRepo.class, userRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public VirSchemaRepoExt virSchemaRepoExt(
            final @Lazy ExternalResourceDAO resourceDAO,
            final EntityManager entityManager) {

        return new VirSchemaRepoExtImpl(resourceDAO, entityManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public VirSchemaDAO virSchemaDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final VirSchemaRepoExt virSchemaRepoExt) {

        return jpaRepositoryFactory.getRepository(VirSchemaRepo.class, virSchemaRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public WAConfigDAO waConfigDAO(final JpaRepositoryFactory jpaRepositoryFactory) {
        return jpaRepositoryFactory.getRepository(WAConfigRepo.class);
    }

    @Bean
    public ConfParamDAO confParamDAO(final JpaRepositoryFactory jpaRepositoryFactory) {
        return jpaRepositoryFactory.getRepository(ConfParamRepo.class);
    }

    @Bean
    public DomainDAO domainDAO(final JpaRepositoryFactory jpaRepositoryFactory) {
        return jpaRepositoryFactory.getRepository(DomainRepo.class);
    }

    @ConditionalOnMissingBean
    @Bean
    public NetworkServiceRepoExt networkServiceRepoExt(final EntityManager entityManager) {
        return new NetworkServiceRepoExtImpl(entityManager);
    }

    @Bean
    public NetworkServiceDAO networkServiceDAO(
            final JpaRepositoryFactory jpaRepositoryFactory,
            final NetworkServiceRepoExt networkServiceRepoExt) {

        return jpaRepositoryFactory.getRepository(NetworkServiceRepo.class, networkServiceRepoExt);
    }
}
