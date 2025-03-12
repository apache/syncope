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
package org.apache.syncope.core.persistence.neo4j;

import jakarta.validation.Validator;
import java.util.Map;
import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.configuration.MutableConfiguration;
import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.TouchedExpiryPolicy;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
import org.apache.syncope.common.keymaster.client.api.model.Neo4jDomain;
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
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;
import org.apache.syncope.core.persistence.api.search.SearchCondVisitor;
import org.apache.syncope.core.persistence.common.CommonPersistenceContext;
import org.apache.syncope.core.persistence.common.RuntimeDomainLoader;
import org.apache.syncope.core.persistence.common.dao.AnyFinder;
import org.apache.syncope.core.persistence.neo4j.content.XMLContentExporter;
import org.apache.syncope.core.persistence.neo4j.content.XMLContentLoader;
import org.apache.syncope.core.persistence.neo4j.dao.Neo4jAnyMatchDAO;
import org.apache.syncope.core.persistence.neo4j.dao.Neo4jAnySearchDAO;
import org.apache.syncope.core.persistence.neo4j.dao.Neo4jAuditEventDAO;
import org.apache.syncope.core.persistence.neo4j.dao.Neo4jBatchDAO;
import org.apache.syncope.core.persistence.neo4j.dao.Neo4jEntityCacheDAO;
import org.apache.syncope.core.persistence.neo4j.dao.Neo4jJobStatusDAO;
import org.apache.syncope.core.persistence.neo4j.dao.Neo4jOIDCJWKSDAO;
import org.apache.syncope.core.persistence.neo4j.dao.Neo4jPersistenceInfoDAO;
import org.apache.syncope.core.persistence.neo4j.dao.Neo4jPolicyDAO;
import org.apache.syncope.core.persistence.neo4j.dao.Neo4jRealmDAO;
import org.apache.syncope.core.persistence.neo4j.dao.Neo4jRealmSearchDAO;
import org.apache.syncope.core.persistence.neo4j.dao.Neo4jTaskDAO;
import org.apache.syncope.core.persistence.neo4j.dao.Neo4jTaskExecDAO;
import org.apache.syncope.core.persistence.neo4j.dao.repo.AccessTokenRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.AnyObjectRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.AnyObjectRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.AnyObjectRepoExtImpl;
import org.apache.syncope.core.persistence.neo4j.dao.repo.AnyTypeClassRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.AnyTypeClassRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.AnyTypeClassRepoExtImpl;
import org.apache.syncope.core.persistence.neo4j.dao.repo.AnyTypeRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.AnyTypeRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.AnyTypeRepoExtImpl;
import org.apache.syncope.core.persistence.neo4j.dao.repo.AttrRepoRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.AttrRepoRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.AttrRepoRepoExtImpl;
import org.apache.syncope.core.persistence.neo4j.dao.repo.AuditConfRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.AuthModuleRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.AuthModuleRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.AuthModuleRepoExtImpl;
import org.apache.syncope.core.persistence.neo4j.dao.repo.AuthProfileRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.CASSPClientAppRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.CASSPClientAppRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.CASSPClientAppRepoExtImpl;
import org.apache.syncope.core.persistence.neo4j.dao.repo.ConfParamRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.ConnInstanceRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.ConnInstanceRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.ConnInstanceRepoExtImpl;
import org.apache.syncope.core.persistence.neo4j.dao.repo.DelegationRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.DelegationRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.DelegationRepoExtImpl;
import org.apache.syncope.core.persistence.neo4j.dao.repo.DerSchemaRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.DerSchemaRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.DerSchemaRepoExtImpl;
import org.apache.syncope.core.persistence.neo4j.dao.repo.DomainRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.DynRealmRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.DynRealmRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.DynRealmRepoExtImpl;
import org.apache.syncope.core.persistence.neo4j.dao.repo.ExternalResourceRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.ExternalResourceRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.ExternalResourceRepoExtImpl;
import org.apache.syncope.core.persistence.neo4j.dao.repo.FIQLQueryRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.FIQLQueryRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.FIQLQueryRepoExtImpl;
import org.apache.syncope.core.persistence.neo4j.dao.repo.GroupRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.GroupRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.GroupRepoExtImpl;
import org.apache.syncope.core.persistence.neo4j.dao.repo.ImplementationRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.ImplementationRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.ImplementationRepoExtImpl;
import org.apache.syncope.core.persistence.neo4j.dao.repo.MailTemplateRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.NetworkServiceRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.NetworkServiceRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.NetworkServiceRepoExtImpl;
import org.apache.syncope.core.persistence.neo4j.dao.repo.NotificationRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.NotificationRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.NotificationRepoExtImpl;
import org.apache.syncope.core.persistence.neo4j.dao.repo.OIDCRPClientAppRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.OIDCRPClientAppRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.OIDCRPClientAppRepoExtImpl;
import org.apache.syncope.core.persistence.neo4j.dao.repo.PlainSchemaRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.PlainSchemaRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.PlainSchemaRepoExtImpl;
import org.apache.syncope.core.persistence.neo4j.dao.repo.RelationshipTypeRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.RelationshipTypeRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.RelationshipTypeRepoExtImpl;
import org.apache.syncope.core.persistence.neo4j.dao.repo.RemediationRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.RemediationRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.RemediationRepoExtImpl;
import org.apache.syncope.core.persistence.neo4j.dao.repo.ReportExecRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.ReportExecRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.ReportExecRepoExtImpl;
import org.apache.syncope.core.persistence.neo4j.dao.repo.ReportRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.ReportRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.ReportRepoExtImpl;
import org.apache.syncope.core.persistence.neo4j.dao.repo.RoleRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.RoleRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.RoleRepoExtImpl;
import org.apache.syncope.core.persistence.neo4j.dao.repo.SAML2IdPEntityRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.SAML2SPClientAppRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.SAML2SPClientAppRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.SAML2SPClientAppRepoExtImpl;
import org.apache.syncope.core.persistence.neo4j.dao.repo.SAML2SPEntityRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.SRARouteRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.SecurityQuestionRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.SecurityQuestionRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.SecurityQuestionRepoExtImpl;
import org.apache.syncope.core.persistence.neo4j.dao.repo.UserRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.UserRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.UserRepoExtImpl;
import org.apache.syncope.core.persistence.neo4j.dao.repo.VirSchemaRepo;
import org.apache.syncope.core.persistence.neo4j.dao.repo.VirSchemaRepoExt;
import org.apache.syncope.core.persistence.neo4j.dao.repo.VirSchemaRepoExtImpl;
import org.apache.syncope.core.persistence.neo4j.dao.repo.WAConfigRepo;
import org.apache.syncope.core.persistence.neo4j.entity.EntityCacheKey;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jAnyType;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jDelegation;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jDerSchema;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jEntityFactory;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jExternalResource;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jImplementation;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jPlainSchema;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRealm;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRole;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jVirSchema;
import org.apache.syncope.core.persistence.neo4j.entity.anyobject.Neo4jAnyObject;
import org.apache.syncope.core.persistence.neo4j.entity.group.Neo4jGroup;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jTaskUtilsFactory;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jUser;
import org.apache.syncope.core.persistence.neo4j.spring.CacheCleaningTransactionExecutionListener;
import org.apache.syncope.core.persistence.neo4j.spring.DomainRoutingDriver;
import org.apache.syncope.core.persistence.neo4j.spring.NodeValidator;
import org.apache.syncope.core.persistence.neo4j.spring.PlainAttrsConverter;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.neo4j.cypherdsl.core.renderer.Dialect;
import org.neo4j.driver.Driver;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ResourceLoader;
import org.springframework.data.neo4j.config.Neo4jEntityScanner;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jOperations;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.neo4j.core.convert.Neo4jConversions;
import org.springframework.data.neo4j.core.convert.Neo4jPersistentPropertyToMapConverter;
import org.springframework.data.neo4j.core.mapping.Neo4jMappingContext;
import org.springframework.data.neo4j.core.transaction.Neo4jBookmarkManager;
import org.springframework.data.neo4j.core.transaction.Neo4jTransactionManager;
import org.springframework.data.neo4j.repository.config.Neo4jRepositoryConfigurationExtension;
import org.springframework.data.neo4j.repository.support.SyncopeNeo4jRepositoryFactory;
import org.springframework.transaction.PlatformTransactionManager;

@EnableConfigurationProperties(PersistenceProperties.class)
@Import(CommonPersistenceContext.class)
@Configuration(proxyBeanMethods = false)
public class PersistenceContext {

    @ConditionalOnMissingBean
    @Bean
    public org.neo4j.cypherdsl.core.renderer.Configuration cypherDslConfiguration() {
        return org.neo4j.cypherdsl.core.renderer.Configuration.newConfig().withDialect(Dialect.NEO4J_5).build();
    }

    @ConditionalOnMissingBean
    @Bean
    public Neo4jConversions neo4jConversions() {
        return new Neo4jConversions();
    }

    @ConditionalOnMissingBean
    @Bean
    public Neo4jMappingContext neo4jMappingContext(final Neo4jConversions neo4jConversions)
            throws ClassNotFoundException {

        Neo4jMappingContext mappingContext = new Neo4jMappingContext(neo4jConversions);
        mappingContext.setInitialEntitySet(
                Neo4jEntityScanner.get().scan("org.apache.syncope.core.persistence.neo4j.entity"));
        return mappingContext;
    }

    @ConditionalOnMissingBean
    @Bean
    public Neo4jBookmarkManager bookmarkManager() {
        return Neo4jBookmarkManager.create();
    }

    @Primary
    @Bean
    public DomainRoutingDriver driver(final DomainHolder<Driver> domainHolder) {
        return new DomainRoutingDriver(domainHolder);
    }

    @Bean(Neo4jRepositoryConfigurationExtension.DEFAULT_NEO4J_CLIENT_BEAN_NAME)
    public Neo4jClient neo4jClient(
            final DomainRoutingDriver driver,
            final Neo4jBookmarkManager bookmarkManager) {

        return Neo4jClient.
                with(driver).
                withNeo4jBookmarkManager(bookmarkManager).
                build();
    }

    @Bean(Neo4jRepositoryConfigurationExtension.DEFAULT_NEO4J_TEMPLATE_BEAN_NAME)
    public Neo4jOperations neo4jTemplate(
            final Neo4jClient neo4jClient,
            final Neo4jMappingContext mappingContext) {

        return new Neo4jTemplate(neo4jClient, mappingContext);
    }

    @ConditionalOnMissingBean
    @Bean
    public CacheCleaningTransactionExecutionListener cacheCleaningTransactionExecutionListener(
            final Cache<EntityCacheKey, Neo4jAnyType> anyTypeCache,
            final Cache<EntityCacheKey, Neo4jAnyObject> anyObjectCache,
            final Cache<EntityCacheKey, Neo4jDelegation> delegationCache,
            final Cache<EntityCacheKey, Neo4jDerSchema> derSchemaCache,
            final Cache<EntityCacheKey, Neo4jExternalResource> externalResourceCache,
            final Cache<EntityCacheKey, Neo4jGroup> groupCache,
            final Cache<EntityCacheKey, Neo4jImplementation> implementationCache,
            final Cache<EntityCacheKey, Neo4jPlainSchema> plainSchemaCache,
            final Cache<EntityCacheKey, Neo4jRealm> realmCache,
            final Cache<EntityCacheKey, Neo4jRole> roleCache,
            final Cache<EntityCacheKey, Neo4jUser> userCache,
            final Cache<EntityCacheKey, Neo4jVirSchema> virSchemaCache) {

        return new CacheCleaningTransactionExecutionListener(
                anyTypeCache,
                anyObjectCache,
                delegationCache,
                derSchemaCache,
                externalResourceCache,
                groupCache,
                implementationCache,
                plainSchemaCache,
                realmCache,
                roleCache,
                userCache,
                virSchemaCache);
    }

    @Bean(Neo4jRepositoryConfigurationExtension.DEFAULT_TRANSACTION_MANAGER_BEAN_NAME)
    public PlatformTransactionManager transactionManager(
            final DomainRoutingDriver driver,
            final Neo4jBookmarkManager bookmarkManager,
            final CacheCleaningTransactionExecutionListener cacheCleaningTransactionExecutionListener) {

        Neo4jTransactionManager transactionManager = Neo4jTransactionManager.
                with(driver).
                withBookmarkManager(bookmarkManager).
                build();
        transactionManager.addListener(cacheCleaningTransactionExecutionListener);
        return transactionManager;
    }

    @Bean
    public Neo4jPersistentPropertyToMapConverter<String, Map<String, PlainAttr>> plainAttrsConverter() {
        return new PlainAttrsConverter();
    }

    @ConditionalOnMissingBean
    @Bean
    public DomainHolder<Driver> domainHolder(
            @Qualifier("MasterDriver")
            final Driver driver) {

        Neo4jDomainHolder domainHolder = new Neo4jDomainHolder();
        domainHolder.getDomains().put(SyncopeConstants.MASTER_DOMAIN, driver);
        return domainHolder;
    }

    @ConditionalOnMissingBean
    @Bean
    public NodeValidator nodeValidator(final Validator validator) {
        return new NodeValidator(validator);
    }

    @ConditionalOnMissingBean
    @Bean
    public XMLContentLoader xmlContentLoader(
            final DomainHolder<Driver> domainHolder,
            final Neo4jMappingContext mappingContext,
            final PersistenceProperties persistenceProperties,
            final ResourceLoader resourceLoader,
            final Environment env) {

        return new XMLContentLoader(
                domainHolder,
                mappingContext,
                resourceLoader.getResource(persistenceProperties.getIndexesXML()),
                env);
    }

    @ConditionalOnMissingBean
    @Bean
    public XMLContentExporter xmlContentExporter(
            final DomainHolder<Driver> domainHolder,
            final Neo4jMappingContext mappingContext) {

        return new XMLContentExporter(domainHolder, mappingContext);
    }

    @ConditionalOnMissingBean
    @Bean
    public DomainRegistry<Neo4jDomain> domainRegistry(final ConfigurableApplicationContext ctx) {
        return new Neo4jDomainRegistry(ctx);
    }

    @ConditionalOnMissingBean
    @Bean
    public RuntimeDomainLoader<Neo4jDomain> runtimeDomainLoader(
            final DomainHolder<?> domainHolder,
            final DomainRegistry<Neo4jDomain> domainRegistry,
            final @Lazy DomainOps domainOps,
            final ConfigurableApplicationContext ctx) {

        return new RuntimeDomainLoader<>(domainHolder, domainRegistry, domainOps, ctx);
    }

    @ConditionalOnMissingBean
    @Bean
    public StartupDomainLoader startupDomainLoader(
            final PersistenceProperties persistenceProperties,
            final ResourceLoader resourceLoader,
            final DomainOps domainOps,
            final DomainHolder<?> domainHolder,
            final DomainRegistry<Neo4jDomain> domainRegistry) {

        return new StartupDomainLoader(domainOps, domainHolder, persistenceProperties, resourceLoader, domainRegistry);
    }

    @ConditionalOnMissingBean
    @Bean
    public EntityFactory entityFactory() {
        return new Neo4jEntityFactory();
    }

    @ConditionalOnMissingBean
    @Bean
    public TaskUtilsFactory taskUtilsFactory() {
        return new Neo4jTaskUtilsFactory();
    }

    @ConditionalOnMissingBean
    @Bean
    public SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory(
            final Neo4jOperations neo4jOperations,
            final Neo4jMappingContext mappingContext) {

        return new SyncopeNeo4jRepositoryFactory(neo4jOperations, mappingContext);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyFinder anyFinder(final @Lazy PlainSchemaDAO plainSchemaDAO, final @Lazy AnySearchDAO anySearchDAO) {
        return new AnyFinder(plainSchemaDAO, anySearchDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public AccessTokenDAO accessTokenDAO(final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory) {
        return neo4jRepositoryFactory.getRepository(AccessTokenRepo.class);
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

        return new Neo4jAnyMatchDAO(
                userDAO,
                groupDAO,
                anyObjectDAO,
                realmDAO,
                plainSchemaDAO,
                anyUtilsFactory,
                validator,
                entityFactory);
    }

    @ConditionalOnMissingBean(name = AnyObjectRepoExt.CACHE)
    @Bean(name = AnyObjectRepoExt.CACHE)
    public Cache<EntityCacheKey, Neo4jAnyObject> anyObjectCache(final CacheManager cacheManager) {
        return cacheManager.createCache(AnyObjectRepoExt.CACHE,
                new MutableConfiguration<EntityCacheKey, Neo4jAnyObject>().
                        setTypes(EntityCacheKey.class, Neo4jAnyObject.class).
                        setStoreByValue(false).
                        setReadThrough(true).
                        setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(Duration.ZERO)));
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyObjectRepoExt anyObjectRepoExt(
            final AnyUtilsFactory anyUtilsFactory,
            final @Lazy AnyTypeDAO anyTypeDAO,
            final @Lazy AnyTypeClassDAO anyTypeClassDAO,
            final @Lazy PlainSchemaDAO plainSchemaDAO,
            final @Lazy DerSchemaDAO derSchemaDAO,
            final @Lazy VirSchemaDAO virSchemaDAO,
            final @Lazy DynRealmDAO dynRealmDAO,
            final @Lazy UserDAO userDAO,
            final @Lazy GroupDAO groupDAO,
            final @Lazy AnyFinder anyFinder,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator,
            final Cache<EntityCacheKey, Neo4jAnyObject> anyObjectCache) {

        return new AnyObjectRepoExtImpl(
                anyUtilsFactory,
                anyTypeDAO,
                anyTypeClassDAO,
                plainSchemaDAO,
                derSchemaDAO,
                virSchemaDAO,
                dynRealmDAO,
                userDAO,
                groupDAO,
                anyFinder,
                neo4jTemplate,
                neo4jClient,
                nodeValidator,
                anyObjectCache);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyObjectDAO anyObjectDAO(
            final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory,
            final AnyObjectRepoExt anyObjectRepoExt) {

        return neo4jRepositoryFactory.getRepository(AnyObjectRepo.class, anyObjectRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnySearchDAO anySearchDAO(
            final RealmSearchDAO realmSearchDAO,
            final @Lazy DynRealmDAO dynRealmDAO,
            final @Lazy UserDAO userDAO,
            final @Lazy GroupDAO groupDAO,
            final @Lazy AnyObjectDAO anyObjectDAO,
            final PlainSchemaDAO schemaDAO,
            final EntityFactory entityFactory,
            final AnyUtilsFactory anyUtilsFactory,
            final PlainAttrValidationManager validator,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient) {

        return new Neo4jAnySearchDAO(
                realmSearchDAO,
                dynRealmDAO,
                userDAO,
                groupDAO,
                anyObjectDAO,
                schemaDAO,
                entityFactory,
                anyUtilsFactory,
                validator,
                neo4jTemplate,
                neo4jClient);
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
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator,
            final Cache<EntityCacheKey, Neo4jAnyType> anyTypeCache,
            final Cache<EntityCacheKey, Neo4jExternalResource> externalResourceCache,
            final Cache<EntityCacheKey, Neo4jGroup> groupCache) {

        return new AnyTypeClassRepoExtImpl(
                anyTypeDAO,
                plainSchemaDAO,
                derSchemaDAO,
                virSchemaDAO,
                groupDAO,
                resourceDAO,
                neo4jTemplate,
                neo4jClient,
                nodeValidator,
                anyTypeCache,
                externalResourceCache,
                groupCache);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyTypeClassDAO anyTypeClassDAO(
            final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory,
            final AnyTypeClassRepoExt anyTypeClassRepoExt) {

        return neo4jRepositoryFactory.getRepository(AnyTypeClassRepo.class, anyTypeClassRepoExt);
    }

    @ConditionalOnMissingBean(name = AnyTypeRepoExt.CACHE)
    @Bean(name = AnyTypeRepoExt.CACHE)
    public Cache<EntityCacheKey, Neo4jAnyType> anyTypeCache(final CacheManager cacheManager) {
        return cacheManager.createCache(AnyTypeRepoExt.CACHE,
                new MutableConfiguration<EntityCacheKey, Neo4jAnyType>().
                        setTypes(EntityCacheKey.class, Neo4jAnyType.class).
                        setStoreByValue(false).
                        setReadThrough(true).
                        setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ETERNAL)));
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyTypeRepoExt anyTypeRepoExt(
            final RemediationDAO remediationDAO,
            final RelationshipTypeDAO relationshipTypeDAO,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator,
            final Cache<EntityCacheKey, Neo4jAnyType> anyTypeCache) {

        return new AnyTypeRepoExtImpl(
                remediationDAO,
                relationshipTypeDAO,
                neo4jTemplate,
                neo4jClient,
                nodeValidator,
                anyTypeCache);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyTypeDAO anyTypeDAO(
            final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory,
            final AnyTypeRepoExt anyTypeRepoExt) {

        return neo4jRepositoryFactory.getRepository(AnyTypeRepo.class, anyTypeRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public AuditConfDAO auditConfDAO(final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory) {
        return neo4jRepositoryFactory.getRepository(AuditConfRepo.class);
    }

    @ConditionalOnMissingBean
    @Bean
    public AuditEventDAO auditEventDAO(
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator) {

        return new Neo4jAuditEventDAO(neo4jTemplate, neo4jClient, nodeValidator);
    }

    @ConditionalOnMissingBean
    @Bean
    public AttrRepoRepoExt attrRepoRepoExt(
            final Neo4jTemplate neo4jTemplate,
            final NodeValidator nodeValidator) {

        return new AttrRepoRepoExtImpl(neo4jTemplate, nodeValidator);
    }

    @ConditionalOnMissingBean
    @Bean
    public AttrRepoDAO attrRepoDAO(
            final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory,
            final AttrRepoRepoExt attrRepoRepoExt) {

        return neo4jRepositoryFactory.getRepository(AttrRepoRepo.class, attrRepoRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public AuthModuleRepoExt authModuleRepoExt(
            final PolicyDAO policyDAO,
            final Neo4jTemplate neo4jTemplate,
            final NodeValidator nodeValidator) {

        return new AuthModuleRepoExtImpl(policyDAO, neo4jTemplate, nodeValidator);
    }

    @ConditionalOnMissingBean
    @Bean
    public AuthModuleDAO authModuleDAO(
            final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory,
            final AuthModuleRepoExt authModuleRepoExt) {

        return neo4jRepositoryFactory.getRepository(AuthModuleRepo.class, authModuleRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public AuthProfileDAO authProfileDAO(final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory) {
        return neo4jRepositoryFactory.getRepository(AuthProfileRepo.class);
    }

    @ConditionalOnMissingBean
    @Bean
    public BatchDAO batchDAO(
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator) {

        return new Neo4jBatchDAO(neo4jTemplate, neo4jClient, nodeValidator);
    }

    @ConditionalOnMissingBean
    @Bean
    public CASSPClientAppRepoExt casSPClientAppRepoExt(
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient) {

        return new CASSPClientAppRepoExtImpl(neo4jTemplate, neo4jClient);
    }

    @ConditionalOnMissingBean
    @Bean
    public CASSPClientAppDAO casSPClientAppDAO(
            final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory,
            final CASSPClientAppRepoExt casSPClientAppRepoExt) {

        return neo4jRepositoryFactory.getRepository(CASSPClientAppRepo.class, casSPClientAppRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public ConnInstanceRepoExt connInstanceRepoExt(
            final @Lazy ExternalResourceDAO resourceDAO,
            final Neo4jTemplate neo4jTemplate,
            final NodeValidator nodeValidator) {

        return new ConnInstanceRepoExtImpl(resourceDAO, neo4jTemplate, nodeValidator);
    }

    @ConditionalOnMissingBean
    @Bean
    public ConnInstanceDAO connInstanceDAO(
            final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory,
            final ConnInstanceRepoExt connInstanceRepoExt) {

        return neo4jRepositoryFactory.getRepository(ConnInstanceRepo.class, connInstanceRepoExt);
    }

    @ConditionalOnMissingBean(name = DelegationRepoExt.CACHE)
    @Bean(name = DelegationRepoExt.CACHE)
    public Cache<EntityCacheKey, Neo4jDelegation> delegationCache(final CacheManager cacheManager) {
        return cacheManager.createCache(DelegationRepoExt.CACHE,
                new MutableConfiguration<EntityCacheKey, Neo4jDelegation>().
                        setTypes(EntityCacheKey.class, Neo4jDelegation.class).
                        setStoreByValue(false).
                        setReadThrough(true).
                        setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ETERNAL)));
    }

    @ConditionalOnMissingBean
    @Bean
    public DelegationRepoExt delegationRepoExt(
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator,
            final Cache<EntityCacheKey, Neo4jDelegation> delegationCache) {

        return new DelegationRepoExtImpl(neo4jTemplate, neo4jClient, nodeValidator, delegationCache);
    }

    @ConditionalOnMissingBean
    @Bean
    public DelegationDAO delegationDAO(
            final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory,
            final DelegationRepoExt delegationRepoExt) {

        return neo4jRepositoryFactory.getRepository(DelegationRepo.class, delegationRepoExt);
    }

    @ConditionalOnMissingBean(name = DerSchemaRepoExt.CACHE)
    @Bean(name = DerSchemaRepoExt.CACHE)
    public Cache<EntityCacheKey, Neo4jDerSchema> derSchemaCache(final CacheManager cacheManager) {
        return cacheManager.createCache(DerSchemaRepoExt.CACHE,
                new MutableConfiguration<EntityCacheKey, Neo4jDerSchema>().
                        setTypes(EntityCacheKey.class, Neo4jDerSchema.class).
                        setStoreByValue(false).
                        setReadThrough(true).
                        setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ETERNAL)));
    }

    @ConditionalOnMissingBean
    @Bean
    public DerSchemaRepoExt derSchemaRepoExt(
            final @Lazy ExternalResourceDAO resourceDAO,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator,
            final Cache<EntityCacheKey, Neo4jDerSchema> derSchemaCache) {

        return new DerSchemaRepoExtImpl(
                resourceDAO,
                neo4jTemplate,
                neo4jClient,
                nodeValidator,
                derSchemaCache);
    }

    @ConditionalOnMissingBean
    @Bean
    public DerSchemaDAO derSchemaDAO(
            final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory,
            final DerSchemaRepoExt derSchemaRepoExt) {

        return neo4jRepositoryFactory.getRepository(DerSchemaRepo.class, derSchemaRepoExt);
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
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator) {

        return new DynRealmRepoExtImpl(
                publisher,
                userDAO,
                groupDAO,
                anyObjectDAO,
                anySearchDAO,
                anyMatchDAO,
                searchCondVisitor,
                neo4jTemplate,
                neo4jClient,
                nodeValidator);
    }

    @ConditionalOnMissingBean
    @Bean
    public DynRealmDAO dynRealmDAO(
            final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory,
            final DynRealmRepoExt dynRealmRepoExt) {

        return neo4jRepositoryFactory.getRepository(DynRealmRepo.class, dynRealmRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public EntityCacheDAO entityCacheDAO(
            final Cache<EntityCacheKey, Neo4jAnyType> anyTypeCache,
            final Cache<EntityCacheKey, Neo4jAnyObject> anyObjectCache,
            final Cache<EntityCacheKey, Neo4jDelegation> delegationCache,
            final Cache<EntityCacheKey, Neo4jDerSchema> derSchemaCache,
            final Cache<EntityCacheKey, Neo4jExternalResource> externalResourceCache,
            final Cache<EntityCacheKey, Neo4jGroup> groupCache,
            final Cache<EntityCacheKey, Neo4jImplementation> implementationCache,
            final Cache<EntityCacheKey, Neo4jPlainSchema> plainSchemaCache,
            final Cache<EntityCacheKey, Neo4jRealm> realmCache,
            final Cache<EntityCacheKey, Neo4jRole> roleCache,
            final Cache<EntityCacheKey, Neo4jUser> userCache,
            final Cache<EntityCacheKey, Neo4jVirSchema> virSchemaCache) {

        return new Neo4jEntityCacheDAO(
                anyTypeCache,
                anyObjectCache,
                delegationCache,
                derSchemaCache,
                externalResourceCache,
                groupCache,
                implementationCache,
                plainSchemaCache,
                realmCache,
                roleCache,
                userCache,
                virSchemaCache);
    }

    @ConditionalOnMissingBean
    @Bean
    public FIQLQueryRepoExt fiqlQueryRepoExt(
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient) {

        return new FIQLQueryRepoExtImpl(neo4jTemplate, neo4jClient);
    }

    @ConditionalOnMissingBean
    @Bean
    public FIQLQueryDAO fiqlQueryDAO(
            final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory,
            final FIQLQueryRepoExt fiqlQueryRepoExt) {

        return neo4jRepositoryFactory.getRepository(FIQLQueryRepo.class, fiqlQueryRepoExt);
    }

    @ConditionalOnMissingBean(name = GroupRepoExt.CACHE)
    @Bean(name = GroupRepoExt.CACHE)
    public Cache<EntityCacheKey, Neo4jGroup> groupCache(final CacheManager cacheManager) {
        return cacheManager.createCache(GroupRepoExt.CACHE,
                new MutableConfiguration<EntityCacheKey, Neo4jGroup>().
                        setTypes(EntityCacheKey.class, Neo4jGroup.class).
                        setStoreByValue(false).
                        setReadThrough(true).
                        setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(Duration.ZERO)));
    }

    @ConditionalOnMissingBean
    @Bean
    public GroupRepoExt groupRepoExt(
            final ApplicationEventPublisher publisher,
            final AnyUtilsFactory anyUtilsFactory,
            final @Lazy AnyTypeDAO anyTypeDAO,
            final @Lazy AnyTypeClassDAO anyTypeClassDAO,
            final @Lazy PlainSchemaDAO plainSchemaDAO,
            final @Lazy DerSchemaDAO derSchemaDAO,
            final @Lazy VirSchemaDAO virSchemaDAO,
            final @Lazy DynRealmDAO dynRealmDAO,
            final AnyMatchDAO anyMatchDAO,
            final @Lazy UserDAO userDAO,
            final @Lazy AnyObjectDAO anyObjectDAO,
            final AnySearchDAO anySearchDAO,
            final @Lazy AnyFinder anyFinder,
            final SearchCondVisitor searchCondVisitor,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator,
            final Cache<EntityCacheKey, Neo4jGroup> groupCache) {

        return new GroupRepoExtImpl(
                anyUtilsFactory,
                publisher,
                anyTypeDAO,
                anyTypeClassDAO,
                plainSchemaDAO,
                derSchemaDAO,
                virSchemaDAO,
                dynRealmDAO,
                anyMatchDAO,
                userDAO,
                anyObjectDAO,
                anySearchDAO,
                anyFinder,
                searchCondVisitor,
                neo4jTemplate,
                neo4jClient,
                nodeValidator,
                groupCache);
    }

    @ConditionalOnMissingBean
    @Bean
    public GroupDAO groupDAO(
            final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory,
            final GroupRepoExt groupRepoExt) {

        return neo4jRepositoryFactory.getRepository(GroupRepo.class, groupRepoExt);
    }

    @ConditionalOnMissingBean(name = ImplementationRepoExt.CACHE)
    @Bean(name = ImplementationRepoExt.CACHE)
    public Cache<EntityCacheKey, Neo4jImplementation> implementationCache(final CacheManager cacheManager) {
        return cacheManager.createCache(ImplementationRepoExt.CACHE,
                new MutableConfiguration<EntityCacheKey, Neo4jImplementation>().
                        setTypes(EntityCacheKey.class, Neo4jImplementation.class).
                        setStoreByValue(false).
                        setReadThrough(true).
                        setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ETERNAL)));
    }

    @ConditionalOnMissingBean
    @Bean
    public ImplementationRepoExt implementationRepoExt(
            final ExternalResourceDAO resourceDAO,
            final EntityCacheDAO entityCacheDAO,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator,
            final Cache<EntityCacheKey, Neo4jImplementation> implementationCache) {

        return new ImplementationRepoExtImpl(
                resourceDAO, entityCacheDAO, neo4jTemplate, neo4jClient, nodeValidator, implementationCache);
    }

    @ConditionalOnMissingBean
    @Bean
    public ImplementationDAO implementationDAO(
            final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory,
            final ImplementationRepoExt implementationRepoExt) {

        return neo4jRepositoryFactory.getRepository(ImplementationRepo.class, implementationRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public JobStatusDAO jobStatusDAO(final Neo4jTemplate neo4jTemplate, final NodeValidator nodeValidator) {
        return new Neo4jJobStatusDAO(neo4jTemplate, nodeValidator);
    }

    @ConditionalOnMissingBean
    @Bean
    public MailTemplateDAO mailTemplateDAO(final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory) {
        return neo4jRepositoryFactory.getRepository(MailTemplateRepo.class);
    }

    @ConditionalOnMissingBean
    @Bean
    public NotificationRepoExt notificationRepoExt(
            final TaskDAO taskDAO,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator) {

        return new NotificationRepoExtImpl(taskDAO, neo4jTemplate, neo4jClient, nodeValidator);
    }

    @ConditionalOnMissingBean
    @Bean
    public NotificationDAO notificationDAO(
            final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory,
            final NotificationRepoExt notificationRepoExt) {

        return neo4jRepositoryFactory.getRepository(NotificationRepo.class, notificationRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public OIDCJWKSDAO oidcJWKSDAO(final Neo4jTemplate neo4jTemplate, final NodeValidator nodeValidator) {
        return new Neo4jOIDCJWKSDAO(neo4jTemplate, nodeValidator);
    }

    @ConditionalOnMissingBean
    @Bean
    public OIDCRPClientAppRepoExt oidcRPClientAppRepoExt(
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator) {

        return new OIDCRPClientAppRepoExtImpl(neo4jTemplate, neo4jClient, nodeValidator);
    }

    @ConditionalOnMissingBean
    @Bean
    public OIDCRPClientAppDAO oidcRPClientAppDAO(
            final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory,
            final OIDCRPClientAppRepoExt oidcRPClientAppRepoExt) {

        return neo4jRepositoryFactory.getRepository(OIDCRPClientAppRepo.class, oidcRPClientAppRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public PersistenceInfoDAO persistenceInfoDAO(final Driver driver) {
        return new Neo4jPersistenceInfoDAO(driver);
    }

    @ConditionalOnMissingBean(name = PlainSchemaRepoExt.CACHE)
    @Bean(name = PlainSchemaRepoExt.CACHE)
    public Cache<EntityCacheKey, Neo4jPlainSchema> plainSchemaCache(final CacheManager cacheManager) {
        return cacheManager.createCache(PlainSchemaRepoExt.CACHE,
                new MutableConfiguration<EntityCacheKey, Neo4jPlainSchema>().
                        setTypes(EntityCacheKey.class, Neo4jPlainSchema.class).
                        setStoreByValue(false).
                        setReadThrough(true).
                        setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ETERNAL)));
    }

    @ConditionalOnMissingBean
    @Bean
    public PlainSchemaRepoExt plainSchemaRepoExt(
            final @Lazy ExternalResourceDAO resourceDAO,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator,
            final Cache<EntityCacheKey, Neo4jPlainSchema> plainSchemaCache) {

        return new PlainSchemaRepoExtImpl(
                resourceDAO,
                neo4jTemplate,
                neo4jClient,
                nodeValidator,
                plainSchemaCache);
    }

    @ConditionalOnMissingBean
    @Bean
    public PlainSchemaDAO plainSchemaDAO(
            final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory,
            final PlainSchemaRepoExt plainSchemaRepoExt) {

        return neo4jRepositoryFactory.getRepository(PlainSchemaRepo.class, plainSchemaRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public PolicyDAO policyDAO(
            final @Lazy RealmDAO realmDAO,
            final @Lazy ExternalResourceDAO resourceDAO,
            final @Lazy CASSPClientAppDAO casSPClientAppDAO,
            final @Lazy OIDCRPClientAppDAO oidcRPClientAppDAO,
            final @Lazy SAML2SPClientAppDAO saml2SPClientAppDAO,
            final EntityCacheDAO entityCacheDAO,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator) {

        return new Neo4jPolicyDAO(
                realmDAO,
                resourceDAO,
                casSPClientAppDAO,
                oidcRPClientAppDAO,
                saml2SPClientAppDAO,
                entityCacheDAO,
                neo4jTemplate,
                neo4jClient,
                nodeValidator);
    }

    @ConditionalOnMissingBean
    @Bean
    public RelationshipTypeRepoExt relationshipTypeRepoExt(
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient) {

        return new RelationshipTypeRepoExtImpl(neo4jTemplate, neo4jClient);
    }

    @ConditionalOnMissingBean
    @Bean
    public RelationshipTypeDAO relationshipTypeDAO(
            final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory,
            final RelationshipTypeRepoExt relationshipTypeRepoExt) {

        return neo4jRepositoryFactory.getRepository(RelationshipTypeRepo.class, relationshipTypeRepoExt);
    }

    @ConditionalOnMissingBean(name = Neo4jRealmDAO.CACHE)
    @Bean(name = Neo4jRealmDAO.CACHE)
    public Cache<EntityCacheKey, Neo4jRealm> realmCache(final CacheManager cacheManager) {
        return cacheManager.createCache(Neo4jRealmDAO.CACHE,
                new MutableConfiguration<EntityCacheKey, Neo4jRealm>().
                        setTypes(EntityCacheKey.class, Neo4jRealm.class).
                        setStoreByValue(false).
                        setReadThrough(true).
                        setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ETERNAL)));
    }

    @ConditionalOnMissingBean
    @Bean
    public RealmDAO realmDAO(
            final @Lazy RoleDAO roleDAO,
            final RealmSearchDAO realmSearchDAO,
            final ApplicationEventPublisher publisher,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator,
            final Cache<EntityCacheKey, Neo4jRealm> realmCache) {

        return new Neo4jRealmDAO(
                roleDAO,
                realmSearchDAO,
                publisher,
                neo4jTemplate,
                neo4jClient,
                nodeValidator,
                realmCache);
    }

    @ConditionalOnMissingBean
    @Bean
    public RealmSearchDAO realmSearchDAO(
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final Cache<EntityCacheKey, Neo4jRealm> realmCache) {

        return new Neo4jRealmSearchDAO(neo4jTemplate, neo4jClient, realmCache);
    }

    @ConditionalOnMissingBean
    @Bean
    public RemediationRepoExt remediationRepoExt(
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient) {

        return new RemediationRepoExtImpl(neo4jTemplate, neo4jClient);
    }

    @ConditionalOnMissingBean
    @Bean
    public RemediationDAO remediationDAO(
            final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory,
            final RemediationRepoExt remediationRepoExt) {

        return neo4jRepositoryFactory.getRepository(RemediationRepo.class, remediationRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public ReportRepoExt reportRepoExt(final Neo4jTemplate neo4jTemplate) {
        return new ReportRepoExtImpl(neo4jTemplate);
    }

    @ConditionalOnMissingBean
    @Bean
    public ReportDAO reportDAO(
            final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory,
            final ReportRepoExt reportRepoExt) {

        return neo4jRepositoryFactory.getRepository(ReportRepo.class, reportRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public ReportExecRepoExt reportExecRepoExt(
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator) {

        return new ReportExecRepoExtImpl(neo4jTemplate, neo4jClient, nodeValidator);
    }

    @ConditionalOnMissingBean
    @Bean
    public ReportExecDAO reportExecDAO(
            final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory,
            final ReportExecRepoExt reportExecRepoExt) {

        return neo4jRepositoryFactory.getRepository(ReportExecRepo.class, reportExecRepoExt);
    }

    @ConditionalOnMissingBean(name = ExternalResourceRepoExt.CACHE)
    @Bean(name = ExternalResourceRepoExt.CACHE)
    public Cache<EntityCacheKey, Neo4jExternalResource> externalResourceCache(final CacheManager cacheManager) {
        return cacheManager.createCache(ExternalResourceRepoExt.CACHE,
                new MutableConfiguration<EntityCacheKey, Neo4jExternalResource>().
                        setTypes(EntityCacheKey.class, Neo4jExternalResource.class).
                        setStoreByValue(false).
                        setReadThrough(true).
                        setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ETERNAL)));
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
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator,
            final Cache<EntityCacheKey, Neo4jExternalResource> externalResourceCache) {

        return new ExternalResourceRepoExtImpl(
                taskDAO,
                anyObjectDAO,
                userDAO,
                groupDAO,
                virSchemaDAO,
                realmDAO,
                neo4jTemplate,
                neo4jClient,
                nodeValidator,
                externalResourceCache);
    }

    @ConditionalOnMissingBean
    @Bean
    public ExternalResourceDAO resourceDAO(
            final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory,
            final ExternalResourceRepoExt resourceRepoExt) {

        return neo4jRepositoryFactory.getRepository(ExternalResourceRepo.class, resourceRepoExt);
    }

    @ConditionalOnMissingBean(name = RoleRepoExt.CACHE)
    @Bean(name = RoleRepoExt.CACHE)
    public Cache<EntityCacheKey, Neo4jRole> roleCache(final CacheManager cacheManager) {
        return cacheManager.createCache(RoleRepoExt.CACHE,
                new MutableConfiguration<EntityCacheKey, Neo4jRole>().
                        setTypes(EntityCacheKey.class, Neo4jRole.class).
                        setStoreByValue(false).
                        setReadThrough(true).
                        setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ETERNAL)));
    }

    @ConditionalOnMissingBean
    @Bean
    public RoleRepoExt roleRepoExt(
            final ApplicationEventPublisher publisher,
            final @Lazy AnyMatchDAO anyMatchDAO,
            final @Lazy AnySearchDAO anySearchDAO,
            final DelegationDAO delegationDAO,
            final SearchCondVisitor searchCondVisitor,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator,
            final Cache<EntityCacheKey, Neo4jRole> roleCache) {

        return new RoleRepoExtImpl(
                publisher,
                anyMatchDAO,
                anySearchDAO,
                delegationDAO,
                searchCondVisitor,
                neo4jTemplate,
                neo4jClient,
                nodeValidator,
                roleCache);
    }

    @ConditionalOnMissingBean
    @Bean
    public RoleDAO roleDAO(
            final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory,
            final RoleRepoExt roleRepoExt) {

        return neo4jRepositoryFactory.getRepository(RoleRepo.class, roleRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public SAML2IdPEntityDAO saml2IdPEntityDAO(final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory) {
        return neo4jRepositoryFactory.getRepository(SAML2IdPEntityRepo.class);
    }

    @ConditionalOnMissingBean
    @Bean
    public SAML2SPClientAppRepoExt saml2SPClientAppRepoExt(
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator) {

        return new SAML2SPClientAppRepoExtImpl(neo4jTemplate, neo4jClient, nodeValidator);
    }

    @ConditionalOnMissingBean
    @Bean
    public SAML2SPClientAppDAO saml2SPClientAppDAO(
            final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory,
            final SAML2SPClientAppRepoExt saml2SPClientAppRepoExt) {

        return neo4jRepositoryFactory.getRepository(SAML2SPClientAppRepo.class, saml2SPClientAppRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public SAML2SPEntityDAO saml2SPEntityDAO(final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory) {
        return neo4jRepositoryFactory.getRepository(SAML2SPEntityRepo.class);
    }

    @ConditionalOnMissingBean
    @Bean
    public SecurityQuestionRepoExt securityQuestionRepoExt(
            final UserDAO userDAO,
            final Neo4jTemplate neo4jTemplate) {

        return new SecurityQuestionRepoExtImpl(userDAO, neo4jTemplate);
    }

    @ConditionalOnMissingBean
    @Bean
    public SecurityQuestionDAO securityQuestionDAO(
            final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory,
            final SecurityQuestionRepoExt securityQuestionRepoExt) {

        return neo4jRepositoryFactory.getRepository(SecurityQuestionRepo.class, securityQuestionRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public SRARouteDAO sraRouteDAO(final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory) {
        return neo4jRepositoryFactory.getRepository(SRARouteRepo.class);
    }

    @ConditionalOnMissingBean
    @Bean
    public TaskDAO taskDAO(
            final RealmSearchDAO realmSearchDAO,
            final RemediationDAO remediationDAO,
            final TaskUtilsFactory taskUtilsFactory,
            final SecurityProperties securityProperties,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator) {

        return new Neo4jTaskDAO(
                realmSearchDAO,
                remediationDAO,
                taskUtilsFactory,
                securityProperties,
                neo4jTemplate,
                neo4jClient,
                nodeValidator);
    }

    @ConditionalOnMissingBean
    @Bean
    public TaskExecDAO taskExecDAO(
            final TaskDAO taskDAO,
            final TaskUtilsFactory taskUtilsFactory,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator) {

        return new Neo4jTaskExecDAO(taskDAO, taskUtilsFactory, neo4jTemplate, neo4jClient, nodeValidator);
    }

    @ConditionalOnMissingBean(name = UserRepoExt.CACHE)
    @Bean(name = UserRepoExt.CACHE)
    public Cache<EntityCacheKey, Neo4jUser> userCache(final CacheManager cacheManager) {
        return cacheManager.createCache(UserRepoExt.CACHE,
                new MutableConfiguration<EntityCacheKey, Neo4jUser>().
                        setTypes(EntityCacheKey.class, Neo4jUser.class).
                        setStoreByValue(false).
                        setReadThrough(true).
                        setExpiryPolicyFactory(TouchedExpiryPolicy.factoryOf(Duration.ZERO)));
    }

    @ConditionalOnMissingBean
    @Bean
    public UserRepoExt userRepoExt(
            final SecurityProperties securityProperties,
            final AnyUtilsFactory anyUtilsFactory,
            final @Lazy AnyTypeDAO anyTypeDAO,
            final @Lazy AnyTypeClassDAO anyTypeClassDAO,
            final @Lazy PlainSchemaDAO plainSchemaDAO,
            final @Lazy DerSchemaDAO derSchemaDAO,
            final @Lazy VirSchemaDAO virSchemaDAO,
            final @Lazy DynRealmDAO dynRealmDAO,
            final RoleDAO roleDAO,
            final AccessTokenDAO accessTokenDAO,
            final @Lazy GroupDAO groupDAO,
            final DelegationDAO delegationDAO,
            final FIQLQueryDAO fiqlQueryDAO,
            final @Lazy AnyFinder anyFinder,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator,
            final Cache<EntityCacheKey, Neo4jUser> userCache) {

        return new UserRepoExtImpl(
                anyUtilsFactory,
                anyTypeDAO,
                anyTypeClassDAO,
                plainSchemaDAO,
                derSchemaDAO,
                virSchemaDAO,
                dynRealmDAO,
                roleDAO,
                accessTokenDAO,
                groupDAO,
                delegationDAO,
                fiqlQueryDAO,
                anyFinder,
                securityProperties,
                neo4jTemplate,
                neo4jClient,
                nodeValidator,
                userCache);
    }

    @ConditionalOnMissingBean
    @Bean
    public UserDAO userDAO(
            final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory,
            final UserRepoExt userRepoExt) {

        return neo4jRepositoryFactory.getRepository(UserRepo.class, userRepoExt);
    }

    @ConditionalOnMissingBean(name = VirSchemaRepoExt.CACHE)
    @Bean(name = VirSchemaRepoExt.CACHE)
    public Cache<EntityCacheKey, Neo4jVirSchema> virSchemaCache(final CacheManager cacheManager) {
        return cacheManager.createCache(VirSchemaRepoExt.CACHE,
                new MutableConfiguration<EntityCacheKey, Neo4jVirSchema>().
                        setTypes(EntityCacheKey.class, Neo4jVirSchema.class).
                        setStoreByValue(false).
                        setReadThrough(true).
                        setExpiryPolicyFactory(CreatedExpiryPolicy.factoryOf(Duration.ETERNAL)));
    }

    @ConditionalOnMissingBean
    @Bean
    public VirSchemaRepoExt virSchemaRepoExt(
            final @Lazy ExternalResourceDAO resourceDAO,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient,
            final NodeValidator nodeValidator,
            final Cache<EntityCacheKey, Neo4jVirSchema> virSchemaCache) {

        return new VirSchemaRepoExtImpl(
                resourceDAO,
                neo4jTemplate,
                neo4jClient,
                nodeValidator,
                virSchemaCache);
    }

    @ConditionalOnMissingBean
    @Bean
    public VirSchemaDAO virSchemaDAO(
            final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory,
            final VirSchemaRepoExt virSchemaRepoExt) {

        return neo4jRepositoryFactory.getRepository(VirSchemaRepo.class, virSchemaRepoExt);
    }

    @ConditionalOnMissingBean
    @Bean
    public WAConfigDAO waConfigDAO(final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory) {
        return neo4jRepositoryFactory.getRepository(WAConfigRepo.class);
    }

    @Bean
    public ConfParamDAO confParamDAO(final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory) {
        return neo4jRepositoryFactory.getRepository(ConfParamRepo.class);
    }

    @Bean
    public DomainDAO domainDAO(final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory) {
        return neo4jRepositoryFactory.getRepository(DomainRepo.class);
    }

    @ConditionalOnMissingBean
    @Bean
    public NetworkServiceRepoExt networkServiceRepoExt(
            final Neo4jTemplate neo4jTemplate,
            final NodeValidator nodeValidator) {

        return new NetworkServiceRepoExtImpl(neo4jTemplate, nodeValidator);
    }

    @Bean
    public NetworkServiceDAO networkServiceDAO(
            final SyncopeNeo4jRepositoryFactory neo4jRepositoryFactory,
            final NetworkServiceRepoExt networkServiceRepoExt) {

        return neo4jRepositoryFactory.getRepository(NetworkServiceRepo.class, networkServiceRepoExt);
    }
}
