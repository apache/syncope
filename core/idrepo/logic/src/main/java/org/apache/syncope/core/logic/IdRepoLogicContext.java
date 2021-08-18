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

import java.lang.reflect.InvocationTargetException;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.core.logic.init.AuditAccessor;
import org.apache.syncope.core.logic.init.AuditLoader;
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
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.java.utils.TemplateUtils;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.apache.syncope.core.workflow.api.AnyObjectWorkflowAdapter;
import org.apache.syncope.core.workflow.api.GroupWorkflowAdapter;
import org.apache.syncope.core.workflow.api.UserWorkflowAdapter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

@EnableAspectJAutoProxy
@EnableConfigurationProperties(LogicProperties.class)
@Configuration
public class IdRepoLogicContext {

    @Autowired
    private LogicProperties logicProperties;

    @Autowired
    private SecurityProperties securityProperties;

    @Autowired
    private AccessTokenDAO accessTokenDAO;

    @Autowired
    private AnySearchDAO anySearchDAO;

    @Autowired
    private AnyObjectDAO anyObjectDAO;

    @Autowired
    private AnyTypeClassDAO anyTypeClassDAO;

    @Autowired
    private AnyTypeDAO anyTypeDAO;

    @Autowired
    private AuditConfDAO auditConfDAO;

    @Autowired
    private DelegationDAO delegationDAO;

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    @Autowired
    private ExternalResourceDAO externalResourceDAO;

    @Autowired
    private GroupDAO groupDAO;

    @Autowired
    private ImplementationDAO implementationDAO;

    @Autowired
    private NotificationDAO notificationDAO;

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private PolicyDAO policyDAO;

    @Autowired
    private RealmDAO realmDAO;

    @Autowired
    private ReportDAO reportDAO;

    @Autowired
    private TaskDAO taskDAO;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private VirSchemaDAO virSchemaDAO;

    @Autowired
    private TemplateUtils templateUtils;

    @Autowired
    private EntityFactory entityFactory;

    @Autowired
    private GroupDataBinder groupDataBinder;

    @Autowired
    private TaskDataBinder taskDataBinder;

    @Autowired
    private ConfParamOps confParamOps;

    @Autowired
    private JobManager jobManager;

    @Autowired
    private SchedulerFactoryBean scheduler;

    @Autowired
    private PropagationManager propagationManager;

    @Autowired
    private PropagationTaskExecutor taskExecutor;

    @ConditionalOnMissingBean
    @Bean
    public LogicInvocationHandler logicInvocationHandler() throws NoSuchMethodException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        return logicProperties.getInvocationHandler().getDeclaredConstructor().newInstance();
    }

    @ConditionalOnMissingBean
    @Bean
    public ImplementationLookup implementationLookup() throws NoSuchMethodException,
            InstantiationException, IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        return logicProperties.getImplementationLookup().getDeclaredConstructor().newInstance();
    }

    @ConditionalOnMissingBean
    @Bean
    public AuditAccessor auditAccessor() {
        return new AuditAccessor(auditConfDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public AuditLoader auditLoader(final AuditAccessor auditAccessor, final ImplementationLookup implementationLookup) {
        return new AuditLoader(auditAccessor, implementationLookup, logicProperties);
    }

    @ConditionalOnMissingBean
    @Bean
    public EntitlementAccessor entitlementAccessor() {
        return new EntitlementAccessor(anyTypeDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    public IdRepoEntitlementLoader idRepoEntitlementLoader() {
        return new IdRepoEntitlementLoader(entitlementAccessor());
    }

    @ConditionalOnMissingBean
    @Bean
    public IdRepoImplementationTypeLoader idRepoImplementationTypeLoader() {
        return new IdRepoImplementationTypeLoader();
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public AccessTokenLogic accessTokenLogic(final AccessTokenDataBinder binder) {
        return new AccessTokenLogic(securityProperties, binder, accessTokenDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public AnyObjectLogic anyObjectLogic(
            final AnyObjectDataBinder binder,
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
    @Autowired
    public AnyTypeClassLogic anyTypeClassLogic(final AnyTypeClassDataBinder binder) {
        return new AnyTypeClassLogic(binder, anyTypeClassDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public AnyTypeLogic anyTypeLogic(final AnyTypeDataBinder binder) {
        return new AnyTypeLogic(binder, anyTypeDAO, anyObjectDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public ApplicationLogic applicationLogic(
            final ApplicationDataBinder binder,
            final ApplicationDAO applicationDAO) {

        return new ApplicationLogic(binder, applicationDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public AuditLogic auditLogic(
            final AuditLoader auditLoader,
            final AuditDataBinder binder,
            final AuditManager auditManager) {

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
    @Autowired
    public DelegationLogic delegationLogic(final DelegationDataBinder binder) {
        return new DelegationLogic(binder, delegationDAO, userDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public DynRealmLogic dynRealmLogic(
            final DynRealmDataBinder binder,
            final DynRealmDAO dynRealmDAO) {

        return new DynRealmLogic(binder, dynRealmDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public GroupLogic groupLogic(final GroupProvisioningManager provisioningManager) {
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
    @Autowired
    public ImplementationLogic implementationLogic(final ImplementationDataBinder binder) {
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
    @Autowired
    public MailTemplateLogic mailTemplateLogic(final MailTemplateDAO mailTemplateDAO) {
        return new MailTemplateLogic(mailTemplateDAO, notificationDAO, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public NotificationLogic notificationLogic(final NotificationDataBinder binder) {
        return new NotificationLogic(jobManager, scheduler, notificationDAO, binder);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public PolicyLogic policyLogic(
            final PolicyDataBinder binder,
            final PolicyUtilsFactory policyUtilsFactory) {

        return new PolicyLogic(policyDAO, binder, policyUtilsFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public RealmLogic realmLogic(final RealmDataBinder binder) {
        return new RealmLogic(realmDAO, anySearchDAO, binder, propagationManager, taskExecutor);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public RelationshipTypeLogic relationshipTypeLogic(
            final RelationshipTypeDataBinder binder,
            final RelationshipTypeDAO relationshipTypeDAO) {

        return new RelationshipTypeLogic(binder, relationshipTypeDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public ReportLogic reportLogic(
            final ReportDataBinder binder,
            final ReportExecDAO reportExecDAO) {

        return new ReportLogic(jobManager, scheduler, reportDAO, reportExecDAO, confParamOps, binder, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public ReportTemplateLogic reportTemplateLogic(final ReportTemplateDAO reportTemplateDAO) {
        return new ReportTemplateLogic(reportTemplateDAO, reportDAO, entityFactory);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public RoleLogic roleLogic(
            final RoleDataBinder binder,
            final RoleDAO roleDAO) {

        return new RoleLogic(binder, roleDAO);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public SchemaLogic schemaLogic(final SchemaDataBinder binder) {
        return new SchemaLogic(plainSchemaDAO, derSchemaDAO, virSchemaDAO, anyTypeClassDAO, binder);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public SecurityQuestionLogic securityQuestionLogic(
            final SecurityQuestionDataBinder binder,
            final SecurityQuestionDAO securityQuestionDAO) {

        return new SecurityQuestionLogic(securityQuestionDAO, userDAO, binder);
    }

    @ConditionalOnMissingBean
    @Bean
    @Autowired
    public SyncopeLogic syncopeLogic(
            final ContentExporter exporter,
            final UserWorkflowAdapter uwfAdapter,
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
    @Autowired
    public TaskLogic taskLogic(
            final TaskExecDAO taskExecDAO,
            final NotificationJobDelegate notificationJobDelegate,
            final TaskUtilsFactory taskUtilsFactory) {

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
    @Autowired
    public UserLogic userLogic(
            final UserDataBinder binder,
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
