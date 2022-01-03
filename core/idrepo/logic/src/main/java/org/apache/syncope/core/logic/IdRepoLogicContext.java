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

import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.core.logic.init.AuditAccessor;
import org.apache.syncope.core.logic.init.AuditLoader;
import org.apache.syncope.core.logic.init.ClassPathScanImplementationLookup;
import org.apache.syncope.core.logic.init.EntitlementAccessor;
import org.apache.syncope.core.logic.init.IdRepoEntitlementLoader;
import org.apache.syncope.core.logic.init.IdRepoImplementationTypeLoader;
import org.apache.syncope.core.persistence.api.ImplementationLookup;
import org.apache.syncope.core.persistence.api.content.ContentExporter;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ApplicationDAO;
import org.apache.syncope.core.persistence.api.dao.AuditConfDAO;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.ExternalResourceDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.ImplementationDAO;
import org.apache.syncope.core.persistence.api.dao.MailTemplateDAO;
import org.apache.syncope.core.persistence.api.dao.NotificationDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.PolicyDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RelationshipTypeDAO;
import org.apache.syncope.core.persistence.api.dao.ReportDAO;
import org.apache.syncope.core.persistence.api.dao.ReportExecDAO;
import org.apache.syncope.core.persistence.api.dao.ReportTemplateDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
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
import org.apache.syncope.core.provisioning.api.UserProvisioningManager;
import org.apache.syncope.core.provisioning.api.data.AccessTokenDataBinder;
import org.apache.syncope.core.provisioning.api.data.AnyObjectDataBinder;
import org.apache.syncope.core.provisioning.api.data.AnyTypeClassDataBinder;
import org.apache.syncope.core.provisioning.api.data.AnyTypeDataBinder;
import org.apache.syncope.core.provisioning.api.data.ApplicationDataBinder;
import org.apache.syncope.core.provisioning.api.data.AuditDataBinder;
import org.apache.syncope.core.provisioning.api.data.DelegationDataBinder;
import org.apache.syncope.core.provisioning.api.data.DynRealmDataBinder;
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
import org.apache.syncope.core.provisioning.java.utils.TemplateUtils;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.apache.syncope.core.workflow.api.AnyObjectWorkflowAdapter;
import org.apache.syncope.core.workflow.api.GroupWorkflowAdapter;
import org.apache.syncope.core.workflow.api.UserWorkflowAdapter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

@EnableAspectJAutoProxy(proxyTargetClass = false)
@EnableConfigurationProperties(LogicProperties.class)
@Configuration(proxyBeanMethods = false)
public class IdRepoLogicContext {

    @ConditionalOnMissingBean
    @Bean
    public LogicInvocationHandler logicInvocationHandler(final NotificationManager notificationManager,
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
    public AuditAccessor auditAccessor(final AuditConfDAO auditConfDAO) {
        return new AuditAccessor(auditConfDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public AuditLoader auditLoader(final AuditAccessor auditAccessor, final ImplementationLookup implementationLookup,
                                   final LogicProperties logicProperties) {
        return new AuditLoader(auditAccessor, implementationLookup, logicProperties);
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
    public AccessTokenLogic accessTokenLogic(final AccessTokenDataBinder binder,
                                             final AccessTokenDAO accessTokenDAO,
                                             final SecurityProperties securityProperties) {
        return new AccessTokenLogic(securityProperties, binder, accessTokenDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyObjectLogic anyObjectLogic(
            final AnyObjectDataBinder binder,
            final TemplateUtils templateUtils,
            final RealmDAO realmDAO,
            final AnyTypeDAO anyTypeDAO,
            final AnyObjectDAO anyObjectDAO,
            final AnySearchDAO anySearchDAO,
            final AnyObjectProvisioningManager provisioningManager) {

        return new AnyObjectLogic(
                realmDAO,
                anyTypeDAO,
                templateUtils,
                anyObjectDAO,
                anySearchDAO,
                binder,
                provisioningManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyTypeClassLogic anyTypeClassLogic(final AnyTypeClassDataBinder binder,
                                               final AnyTypeClassDAO anyTypeClassDAO) {
        return new AnyTypeClassLogic(binder, anyTypeClassDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public AnyTypeLogic anyTypeLogic(final AnyTypeDataBinder binder,
                                     final AnyTypeDAO anyTypeDAO,
                                     final AnyObjectDAO anyObjectDAO) {
        return new AnyTypeLogic(binder, anyTypeDAO, anyObjectDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public ApplicationLogic applicationLogic(
            final ApplicationDataBinder binder,
            final ApplicationDAO applicationDAO) {

        return new ApplicationLogic(binder, applicationDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public AuditLogic auditLogic(
            final AuditManager auditManager,
            final AuditLoader auditLoader,
            final AuditConfDAO auditConfDAO,
            final ExternalResourceDAO externalResourceDAO,
            final EntityFactory entityFactory,
            final AuditDataBinder binder) {

        return new AuditLogic(
                auditLoader,
                auditConfDAO,
                externalResourceDAO,
                entityFactory,
                binder,
                auditManager);
    }

    @ConditionalOnMissingBean
    @Bean
    public DelegationLogic delegationLogic(final DelegationDataBinder binder,
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
    public GroupLogic groupLogic(final GroupProvisioningManager provisioningManager,
                                 final JobManager jobManager,
                                 final TemplateUtils templateUtils,
                                 final EntityFactory entityFactory,
                                 final RealmDAO realmDAO,
                                 final AnyTypeDAO anyTypeDAO,
                                 final UserDAO userDAO,
                                 final GroupDAO groupDAO,
                                 final AnySearchDAO anySearchDAO,
                                 final SchedulerFactoryBean scheduler,
                                 final TaskDAO taskDAO,
                                 final ConfParamOps confParamOps,
                                 final GroupDataBinder groupDataBinder,
                                 final TaskDataBinder taskDataBinder,
                                 final ImplementationDAO implementationDAO,
                                 final SecurityProperties securityProperties) {
        return new GroupLogic(
                realmDAO,
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
                confParamOps,
                jobManager,
                scheduler,
                entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public ImplementationLogic implementationLogic(final ImplementationDataBinder binder,
                                                   final PlainSchemaDAO plainSchemaDAO,
                                                   final RealmDAO realmDAO,
                                                   final PolicyDAO policyDAO,
                                                   final ReportDAO reportDAO,
                                                   final TaskDAO taskDAO,
                                                   final ExternalResourceDAO externalResourceDAO,
                                                   final ImplementationDAO implementationDAO,
                                                   final NotificationDAO notificationDAO) {
        return new ImplementationLogic(
                binder,
                implementationDAO,
                reportDAO,
                policyDAO,
                externalResourceDAO,
                taskDAO,
                realmDAO,
                plainSchemaDAO,
                notificationDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public MailTemplateLogic mailTemplateLogic(final MailTemplateDAO mailTemplateDAO,
                                               final EntityFactory entityFactory,
                                               final NotificationDAO notificationDAO) {
        return new MailTemplateLogic(mailTemplateDAO, notificationDAO, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public NotificationLogic notificationLogic(final NotificationDataBinder binder,
                                               final JobManager jobManager,
                                               final SchedulerFactoryBean scheduler,
                                               final NotificationDAO notificationDAO) {
        return new NotificationLogic(jobManager, scheduler, notificationDAO, binder);
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
    public RealmLogic realmLogic(final RealmDataBinder binder,
                                 final RealmDAO realmDAO,
                                 final AnySearchDAO anySearchDAO,
                                 final PropagationManager propagationManager,
                                 final PropagationTaskExecutor taskExecutor) {
        return new RealmLogic(realmDAO, anySearchDAO, binder, propagationManager, taskExecutor);
    }

    @ConditionalOnMissingBean
    @Bean
    public RelationshipTypeLogic relationshipTypeLogic(
            final RelationshipTypeDataBinder binder,
            final RelationshipTypeDAO relationshipTypeDAO) {

        return new RelationshipTypeLogic(binder, relationshipTypeDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public ReportLogic reportLogic(
            final JobManager jobManager,
            final ConfParamOps confParamOps,
            final ReportDataBinder binder,
            final SchedulerFactoryBean scheduler,
            final ReportDAO reportDAO,
            final EntityFactory entityFactory,
            final ReportExecDAO reportExecDAO) {

        return new ReportLogic(jobManager, scheduler, reportDAO, reportExecDAO, confParamOps, binder, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public ReportTemplateLogic reportTemplateLogic(final ReportTemplateDAO reportTemplateDAO,
                                                   final ReportDAO reportDAO,
                                                   final EntityFactory entityFactory) {
        return new ReportTemplateLogic(reportTemplateDAO, reportDAO, entityFactory);
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
    public SchemaLogic schemaLogic(final SchemaDataBinder binder,
                                   final VirSchemaDAO virSchemaDAO,
                                   final AnyTypeClassDAO anyTypeClassDAO,
                                   final DerSchemaDAO derSchemaDAO,
                                   final PlainSchemaDAO plainSchemaDAO) {
        return new SchemaLogic(plainSchemaDAO, derSchemaDAO, virSchemaDAO, anyTypeClassDAO, binder);
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
            final ContentExporter exporter,
            final UserWorkflowAdapter uwfAdapter,
            final AnyTypeDAO anyTypeDAO,
            final GroupDAO groupDAO,
            final ConfParamOps confParamOps,
            final GroupDataBinder groupDataBinder,
            final AnySearchDAO anySearchDAO,
            final GroupWorkflowAdapter gwfAdapter,
            final AnyObjectWorkflowAdapter awfAdapter) {

        return new SyncopeLogic(
                anyTypeDAO,
                groupDAO,
                anySearchDAO,
                groupDataBinder,
                confParamOps,
                exporter,
                uwfAdapter,
                gwfAdapter,
                awfAdapter);
    }

    @ConditionalOnMissingBean
    @Bean
    public TaskLogic taskLogic(
            final JobManager jobManager,
            final PropagationTaskExecutor taskExecutor,
            final TaskExecDAO taskExecDAO,
            final TaskDAO taskDAO,
            final SchedulerFactoryBean scheduler,
            final ConfParamOps confParamOps,
            final ExternalResourceDAO externalResourceDAO,
            final NotificationJobDelegate notificationJobDelegate,
            final TaskDataBinder taskDataBinder,
            final TaskUtilsFactory taskUtilsFactory,
            final NotificationDAO notificationDAO) {

        return new TaskLogic(
                jobManager,
                scheduler,
                taskDAO,
                taskExecDAO,
                externalResourceDAO,
                notificationDAO,
                confParamOps,
                taskDataBinder,
                taskExecutor,
                notificationJobDelegate,
                taskUtilsFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    public UserLogic userLogic(
            final UserDataBinder binder,
            final TemplateUtils templateUtils,
            final RealmDAO realmDAO,
            final AnyTypeDAO anyTypeDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnySearchDAO anySearchDAO,
            final AccessTokenDAO accessTokenDAO,
            final DelegationDAO delegationDAO,
            final ConfParamOps confParamOps,
            final UserProvisioningManager provisioningManager,
            final SyncopeLogic syncopeLogic) {

        return new UserLogic(
                realmDAO,
                anyTypeDAO,
                templateUtils,
                userDAO,
                groupDAO,
                anySearchDAO,
                accessTokenDAO,
                delegationDAO,
                confParamOps,
                binder,
                provisioningManager,
                syncopeLogic);
    }
}
