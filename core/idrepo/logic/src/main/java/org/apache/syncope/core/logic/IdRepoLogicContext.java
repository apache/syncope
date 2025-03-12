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
package org.apache.syncope.core.logic;

import jakarta.validation.Validator;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.core.logic.init.ClassPathScanImplementationLookup;
import org.apache.syncope.core.logic.init.EntitlementAccessor;
import org.apache.syncope.core.logic.init.IdRepoEntitlementLoader;
import org.apache.syncope.core.logic.init.IdRepoImplementationTypeLoader;
import org.apache.syncope.core.persistence.api.EncryptorManager;
import org.apache.syncope.core.persistence.api.content.ContentExporter;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.AuditConfDAO;
import org.apache.syncope.core.persistence.api.dao.AuditEventDAO;
import org.apache.syncope.core.persistence.api.dao.CASSPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.FIQLQueryDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.JobStatusDAO;
import org.apache.syncope.core.persistence.api.dao.MailTemplateDAO;
import org.apache.syncope.core.persistence.api.dao.NotificationDAO;
import org.apache.syncope.core.persistence.api.dao.OIDCRPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ReportDAO;
import org.apache.syncope.core.persistence.api.dao.ReportExecDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.dao.SAML2SPClientAppDAO;
import org.apache.syncope.core.persistence.api.dao.SecurityQuestionDAO;
import org.apache.syncope.core.persistence.api.dao.TaskDAO;
import org.apache.syncope.core.persistence.api.dao.TaskExecDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.policy.PolicyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.task.TaskUtilsFactory;
import org.apache.syncope.core.provisioning.api.AnyObjectProvisioningManager;
import org.apache.syncope.core.provisioning.api.AuditManager;
import org.apache.syncope.core.provisioning.api.GroupProvisioningManager;
import org.apache.syncope.core.provisioning.api.ImplementationLookup;
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.AccessTokenDataBinder;
import org.apache.syncope.core.provisioning.api.data.AnyObjectDataBinder;
import org.apache.syncope.core.provisioning.api.data.AnyTypeClassDataBinder;
import org.apache.syncope.core.provisioning.api.data.AnyTypeDataBinder;
import org.apache.syncope.core.provisioning.api.data.AuditDataBinder;
import org.apache.syncope.core.provisioning.api.data.DelegationDataBinder;
import org.apache.syncope.core.provisioning.api.data.DynRealmDataBinder;
import org.apache.syncope.core.provisioning.api.data.FIQLQueryDataBinder;
import org.apache.syncope.core.provisioning.api.data.GroupDataBinder;
import org.apache.syncope.core.provisioning.api.data.ImplementationDataBinder;
import org.apache.syncope.core.provisioning.api.data.NotificationDataBinder;
import org.apache.syncope.core.provisioning.api.data.PolicyDataBinder;
import org.apache.syncope.core.provisioning.api.data.RealmDataBinder;
import org.apache.syncope.core.provisioning.api.data.RelationshipTypeDataBinder;
import org.apache.syncope.core.provisioning.api.data.ReportDataBinder;
import org.apache.syncope.core.provisioning.api.data.RoleDataBinder;
import org.apache.syncope.core.provisioning.api.data.SchemaDataBinder;
import org.apache.syncope.core.provisioning.api.data.SecurityQuestionDataBinder;
import org.apache.syncope.core.provisioning.api.data.TaskDataBinder;
import org.apache.syncope.core.provisioning.api.data.UserDataBinder;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.apache.syncope.core.provisioning.api.notification.NotificationJobDelegate;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.api.rules.RuleProvider;
import org.apache.syncope.core.provisioning.java.job.SyncopeTaskScheduler;
import org.apache.syncope.core.provisioning.java.utils.TemplateUtils;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@EnableAspectJAutoProxy(proxyTargetClass = false)
@Configuration(proxyBeanMethods = false)
public class IdRepoLogicContext {

    protected static final Logger LOG = LoggerFactory.getLogger(IdRepoLogicContext.class);

    @ConditionalOnMissingBean
    @Bean
    public LogicInvocationHandler logicInvocationHandler(
            final NotificationManager notificationManager,
            final AuditManager auditManager) {

        return new LogicInvocationHandler(notificationManager, auditManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public ImplementationLookup implementationLookup() {
        return new ClassPathScanImplementationLookup();
    }

    @ConditionalOnMissingBean
    @Bean
    public EntitlementAccessor entitlementAccessor(final AnyTypeDAO anyTypeDAO) {
        return new EntitlementAccessor(anyTypeDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public IdRepoEntitlementLoader idRepoEntitlementLoader(final EntitlementAccessor entitlementAccessor) {
        return new IdRepoEntitlementLoader(entitlementAccessor);
    }

    @ConditionalOnMissingBean
    @Bean
    public IdRepoImplementationTypeLoader idRepoImplementationTypeLoader() {
        return new IdRepoImplementationTypeLoader();
    }

    @ConditionalOnMissingBean
    @Bean
    public AccessTokenLogic accessTokenLogic(
            final AccessTokenDataBinder binder,
            final AccessTokenDAO accessTokenDAO,
            final SecurityProperties securityProperties,
            final EncryptorManager encryptorManager) {

        return new AccessTokenLogic(securityProperties, encryptorManager, binder, accessTokenDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyObjectLogic anyObjectLogic(
            final RealmSearchDAO realmSearchDAO,
            final AnyTypeDAO anyTypeDAO,
            final TemplateUtils templateUtils,
            final AnyObjectDAO anyObjectDAO,
            final AnySearchDAO anySearchDAO,
            final AnyObjectDataBinder binder,
            final AnyObjectProvisioningManager provisioningManager) {

        return new AnyObjectLogic(
                realmSearchDAO,
                anyTypeDAO,
                templateUtils,
                anyObjectDAO,
                anySearchDAO,
                binder,
                provisioningManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyTypeClassLogic anyTypeClassLogic(
            final AnyTypeClassDataBinder binder,
            final AnyTypeClassDAO anyTypeClassDAO) {

        return new AnyTypeClassLogic(binder, anyTypeClassDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyTypeLogic anyTypeLogic(
            final AnyTypeDataBinder binder,
            final AnyTypeDAO anyTypeDAO,
            final AnyObjectDAO anyObjectDAO,
            final ApplicationEventPublisher publisher) {

        return new AnyTypeLogic(binder, anyTypeDAO, anyObjectDAO, publisher);
    }

    @ConditionalOnMissingBean
    @Bean
    public AuditLogic auditLogic(
            final AuditConfDAO auditConfDAO,
            final AuditEventDAO auditEventDAO,
            final ExternalResourceDAO resourceDAO,
            final EntityFactory entityFactory,
            final ImplementationLookup implementationLookup,
            final AuditDataBinder binder,
            final AuditManager auditManager) {

        return new AuditLogic(
                auditConfDAO,
                auditEventDAO,
                resourceDAO,
                entityFactory,
                implementationLookup,
                binder,
                auditManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public CommandLogic commandLogic(final ImplementationDAO implementationDAO, final Validator validator) {
        return new CommandLogic(implementationDAO, validator);
    }

    @ConditionalOnMissingBean
    @Bean
    public FIQLQueryLogic fiqlQueryLogic(
            final FIQLQueryDataBinder binder,
            final UserDAO userDAO,
            final FIQLQueryDAO fiqlQueryDAO,
            final SecurityProperties securityProperties) {

        return new FIQLQueryLogic(binder, fiqlQueryDAO, userDAO, securityProperties);
    }

    @ConditionalOnMissingBean
    @Bean
    public DelegationLogic delegationLogic(
            final DelegationDataBinder binder,
            final UserDAO userDAO,
            final DelegationDAO delegationDAO) {

        return new DelegationLogic(binder, delegationDAO, userDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public DynRealmLogic dynRealmLogic(
            final DynRealmDataBinder binder,
            final DynRealmDAO dynRealmDAO) {

        return new DynRealmLogic(binder, dynRealmDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public GroupLogic groupLogic(
            final RealmSearchDAO realmSearchDAO,
            final AnyTypeDAO anyTypeDAO,
            final TemplateUtils templateUtils,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final SecurityProperties securityProperties,
            final AnySearchDAO anySearchDAO,
            final ImplementationDAO implementationDAO,
            final TaskDAO taskDAO,
            final GroupDataBinder groupDataBinder,
            final GroupProvisioningManager provisioningManager,
            final TaskDataBinder taskDataBinder,
            final JobManager jobManager,
            final SyncopeTaskScheduler scheduler,
            final EntityFactory entityFactory) {

        return new GroupLogic(
                realmSearchDAO,
                anyTypeDAO,
                templateUtils,
                userDAO,
                groupDAO,
                securityProperties,
                anySearchDAO,
                implementationDAO,
                taskDAO,
                groupDataBinder,
                provisioningManager,
                taskDataBinder,
                jobManager,
                scheduler,
                entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public ImplementationLogic implementationLogic(
            final ImplementationDataBinder binder,
            final PlainSchemaDAO plainSchemaDAO,
            final RealmDAO realmDAO,
            final PolicyDAO policyDAO,
            final ReportDAO reportDAO,
            final TaskDAO taskDAO,
            final ExternalResourceDAO resourceDAO,
            final ImplementationDAO implementationDAO,
            final NotificationDAO notificationDAO) {

        return new ImplementationLogic(
                binder,
                implementationDAO,
                reportDAO,
                policyDAO,
                resourceDAO,
                taskDAO,
                realmDAO,
                plainSchemaDAO,
                notificationDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public MailTemplateLogic mailTemplateLogic(
            final MailTemplateDAO mailTemplateDAO,
            final EntityFactory entityFactory,
            final NotificationDAO notificationDAO) {

        return new MailTemplateLogic(mailTemplateDAO, notificationDAO, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public NotificationLogic notificationLogic(
            final NotificationDataBinder binder,
            final JobManager jobManager,
            final JobStatusDAO jobStatusDAO,
            final SyncopeTaskScheduler scheduler,
            final NotificationDAO notificationDAO) {

        return new NotificationLogic(jobManager, scheduler, jobStatusDAO, notificationDAO, binder);
    }

    @ConditionalOnMissingBean
    @Bean
    public PolicyLogic policyLogic(
            final PolicyDataBinder binder,
            final PolicyDAO policyDAO,
            final PolicyUtilsFactory policyUtilsFactory) {

        return new PolicyLogic(policyDAO, binder, policyUtilsFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public RealmLogic realmLogic(
            final RealmDAO realmDAO,
            final RealmSearchDAO realmSearchDAO,
            final AnySearchDAO anySearchDAO,
            final TaskDAO taskDAO,
            final CASSPClientAppDAO casSPClientAppDAO,
            final OIDCRPClientAppDAO oidcRPClientAppDAO,
            final SAML2SPClientAppDAO saml2SPClientAppDAO,
            final RealmDataBinder binder,
            final PropagationManager propagationManager,
            final PropagationTaskExecutor taskExecutor) {

        return new RealmLogic(
                realmDAO,
                realmSearchDAO,
                anySearchDAO,
                taskDAO,
                casSPClientAppDAO,
                oidcRPClientAppDAO,
                saml2SPClientAppDAO,
                binder,
                propagationManager,
                taskExecutor);
    }

    @ConditionalOnMissingBean
    @Bean
    public RelationshipTypeLogic relationshipTypeLogic(
            final RelationshipTypeDataBinder binder,
            final RelationshipTypeDAO relationshipTypeDAO,
            final ApplicationEventPublisher publisher) {

        return new RelationshipTypeLogic(binder, relationshipTypeDAO, publisher);
    }

    @ConditionalOnMissingBean
    @Bean
    public ReportLogic reportLogic(
            final JobManager jobManager,
            final ReportDataBinder binder,
            final SyncopeTaskScheduler scheduler,
            final JobStatusDAO jobStatusDAO,
            final ReportDAO reportDAO,
            final EntityFactory entityFactory,
            final ReportExecDAO reportExecDAO) {

        return new ReportLogic(
                jobManager,
                scheduler,
                jobStatusDAO,
                reportDAO,
                reportExecDAO,
                binder,
                entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public RoleLogic roleLogic(
            final RoleDataBinder binder,
            final RoleDAO roleDAO) {

        return new RoleLogic(binder, roleDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public SchemaLogic schemaLogic(
            final PlainSchemaDAO plainSchemaDAO,
            final DerSchemaDAO derSchemaDAO,
            final VirSchemaDAO virSchemaDAO,
            final AnyTypeClassDAO anyTypeClassDAO,
            final ImplementationDAO implementationDAO,
            final SchemaDataBinder binder) {

        return new SchemaLogic(plainSchemaDAO, derSchemaDAO, virSchemaDAO, anyTypeClassDAO, implementationDAO, binder);
    }

    @ConditionalOnMissingBean
    @Bean
    public SecurityQuestionLogic securityQuestionLogic(
            final SecurityQuestionDataBinder binder,
            final UserDAO userDAO,
            final SecurityQuestionDAO securityQuestionDAO) {

        return new SecurityQuestionLogic(securityQuestionDAO, userDAO, binder);
    }

    @ConditionalOnMissingBean
    @Bean
    public SyncopeLogic syncopeLogic(
            final RealmSearchDAO realmSearchDAO,
            final AnyTypeDAO anyTypeDAO,
            final GroupDAO groupDAO,
            final AnySearchDAO anySearchDAO,
            final GroupDataBinder groupDataBinder,
            final ConfParamOps confParamOps,
            final ContentExporter exporter) {

        return new SyncopeLogic(
                realmSearchDAO,
                anyTypeDAO,
                groupDAO,
                anySearchDAO,
                groupDataBinder,
                confParamOps,
                exporter);
    }

    @ConditionalOnMissingBean
    @Bean
    public TaskLogic taskLogic(
            final JobManager jobManager,
            final PropagationTaskExecutor taskExecutor,
            final TaskExecDAO taskExecDAO,
            final TaskDAO taskDAO,
            final SyncopeTaskScheduler scheduler,
            final JobStatusDAO jobStatusDAO,
            final ExternalResourceDAO resourceDAO,
            final NotificationJobDelegate notificationJobDelegate,
            final TaskDataBinder taskDataBinder,
            final TaskUtilsFactory taskUtilsFactory,
            final NotificationDAO notificationDAO) {

        return new TaskLogic(
                jobManager,
                scheduler,
                jobStatusDAO,
                taskDAO,
                taskExecDAO,
                resourceDAO,
                notificationDAO,
                taskDataBinder,
                taskExecutor,
                notificationJobDelegate,
                taskUtilsFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public UserLogic userLogic(
            final RealmSearchDAO realmSearchDAO,
            final AnyTypeDAO anyTypeDAO,
            final TemplateUtils templateUtils,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnySearchDAO anySearchDAO,
            final ExternalResourceDAO resourceDAO,
            final AccessTokenDAO accessTokenDAO,
            final DelegationDAO delegationDAO,
            final ConfParamOps confParamOps,
            final UserDataBinder binder,
            final UserProvisioningManager provisioningManager,
            final SyncopeLogic syncopeLogic,
            final RuleProvider ruleProvider,
            final EncryptorManager encryptorManager) {

        return new UserLogic(
                realmSearchDAO,
                anyTypeDAO,
                templateUtils,
                userDAO,
                groupDAO,
                anySearchDAO,
                resourceDAO,
                accessTokenDAO,
                delegationDAO,
                confParamOps,
                binder,
                provisioningManager,
                syncopeLogic,
                ruleProvider,
                encryptorManager);
    }
}
