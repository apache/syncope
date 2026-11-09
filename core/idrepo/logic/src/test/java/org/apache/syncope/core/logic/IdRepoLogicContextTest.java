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

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.mock;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.recovery.RecoveryCodeGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import jakarta.validation.Validator;
import javax.cache.Cache;
import org.apache.syncope.common.keymaster.client.api.ConfParamOps;
import org.apache.syncope.common.keymaster.client.api.DomainOps;
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
import org.apache.syncope.core.provisioning.api.jexl.TemplateUtils;
import org.apache.syncope.core.provisioning.api.job.JobManager;
import org.apache.syncope.core.provisioning.api.notification.NotificationJobDelegate;
import org.apache.syncope.core.provisioning.api.notification.NotificationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationManager;
import org.apache.syncope.core.provisioning.api.propagation.PropagationTaskExecutor;
import org.apache.syncope.core.provisioning.api.rules.RuleProvider;
import org.apache.syncope.core.provisioning.java.job.SyncopeTaskScheduler;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.apache.syncope.core.spring.security.throttle.ThrottlerAttempts;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

public class IdRepoLogicContextTest {

    private final IdRepoLogicContext context = new IdRepoLogicContext();

    @Test
    void logicInvocationHandler() {
        assertInstanceOf(LogicInvocationHandler.class, context.logicInvocationHandler(
                mock(NotificationManager.class), mock(AuditManager.class)));
    }

    @Test
    void implementationLookup() {
        assertInstanceOf(ClassPathScanImplementationLookup.class, context.implementationLookup());
        assertInstanceOf(ImplementationLookup.class, context.implementationLookup());
    }

    @Test
    void entitlementAccessor() {
        assertInstanceOf(EntitlementAccessor.class, context.entitlementAccessor(mock(AnyTypeDAO.class)));
    }

    @Test
    void loaders() {
        EntitlementAccessor accessor = mock(EntitlementAccessor.class);

        assertInstanceOf(IdRepoEntitlementLoader.class, context.idRepoEntitlementLoader(accessor));
        assertInstanceOf(IdRepoImplementationTypeLoader.class, context.idRepoImplementationTypeLoader());
    }

    @Test
    void accessTokenLogic() {
        assertInstanceOf(AccessTokenLogic.class, context.accessTokenLogic(
                mock(AccessTokenDataBinder.class),
                mock(AccessTokenDAO.class),
                new SecurityProperties(),
                mock(EncryptorManager.class)));
    }

    @Test
    void anyObjectLogic() {
        assertInstanceOf(AnyObjectLogic.class, context.anyObjectLogic(
                mock(RealmSearchDAO.class),
                mock(AnyTypeDAO.class),
                mock(TemplateUtils.class),
                mock(AnyObjectDAO.class),
                mock(AnySearchDAO.class),
                mock(AnyObjectDataBinder.class),
                mock(AnyObjectProvisioningManager.class)));
    }

    @Test
    void anyTypeClassLogic() {
        assertInstanceOf(AnyTypeClassLogic.class, context.anyTypeClassLogic(
                mock(AnyTypeClassDataBinder.class), mock(AnyTypeClassDAO.class)));
    }

    @Test
    void anyTypeLogic() {
        assertInstanceOf(AnyTypeLogic.class, context.anyTypeLogic(
                mock(AnyTypeDataBinder.class),
                mock(AnyTypeDAO.class),
                mock(AnyObjectDAO.class),
                mock(ApplicationEventPublisher.class)));
    }

    @Test
    void auditLogic() {
        assertInstanceOf(AuditLogic.class, context.auditLogic(
                mock(AuditConfDAO.class),
                mock(AuditEventDAO.class),
                mock(ExternalResourceDAO.class),
                mock(EntityFactory.class),
                mock(ImplementationLookup.class),
                mock(AuditDataBinder.class),
                mock(AuditManager.class)));
    }

    @Test
    void commandLogic() {
        assertInstanceOf(CommandLogic.class, context.commandLogic(
                mock(ImplementationDAO.class), mock(Validator.class)));
    }

    @Test
    void fiqlQueryLogic() {
        assertInstanceOf(FIQLQueryLogic.class, context.fiqlQueryLogic(
                mock(FIQLQueryDataBinder.class),
                mock(UserDAO.class),
                mock(FIQLQueryDAO.class),
                new SecurityProperties()));
    }

    @Test
    void delegationLogic() {
        assertInstanceOf(DelegationLogic.class, context.delegationLogic(
                mock(DelegationDataBinder.class),
                mock(UserDAO.class),
                mock(DelegationDAO.class)));
    }

    @Test
    void groupLogic() {
        assertInstanceOf(GroupLogic.class, context.groupLogic(
                mock(RealmSearchDAO.class),
                mock(AnyTypeDAO.class),
                mock(TemplateUtils.class),
                mock(UserDAO.class),
                mock(GroupDAO.class),
                new SecurityProperties(),
                mock(AnySearchDAO.class),
                mock(ImplementationDAO.class),
                mock(TaskDAO.class),
                mock(GroupDataBinder.class),
                mock(GroupProvisioningManager.class),
                mock(TaskDataBinder.class),
                mock(JobManager.class),
                mock(SyncopeTaskScheduler.class),
                mock(EntityFactory.class)));
    }

    @Test
    void implementationLogic() {
        assertInstanceOf(ImplementationLogic.class, context.implementationLogic(
                mock(ImplementationDataBinder.class),
                mock(PlainSchemaDAO.class),
                mock(RealmDAO.class),
                mock(PolicyDAO.class),
                mock(ReportDAO.class),
                mock(TaskDAO.class),
                mock(ExternalResourceDAO.class),
                mock(ImplementationDAO.class),
                mock(NotificationDAO.class)));
    }

    @Test
    void mailTemplateLogic() {
        assertInstanceOf(MailTemplateLogic.class, context.mailTemplateLogic(
                mock(MailTemplateDAO.class),
                mock(EntityFactory.class),
                mock(NotificationDAO.class)));
    }

    @Test
    void notificationLogic() {
        assertInstanceOf(NotificationLogic.class, context.notificationLogic(
                mock(NotificationDataBinder.class),
                mock(JobManager.class),
                mock(JobStatusDAO.class),
                mock(SyncopeTaskScheduler.class),
                mock(NotificationDAO.class)));
    }

    @Test
    void policyLogic() {
        assertInstanceOf(PolicyLogic.class, context.policyLogic(
                mock(PolicyDataBinder.class),
                mock(PolicyDAO.class),
                mock(PolicyUtilsFactory.class)));
    }

    @Test
    void realmLogic() {
        assertInstanceOf(RealmLogic.class, context.realmLogic(
                mock(RealmDAO.class),
                mock(RealmSearchDAO.class),
                mock(AnySearchDAO.class),
                mock(TaskDAO.class),
                mock(CASSPClientAppDAO.class),
                mock(OIDCRPClientAppDAO.class),
                mock(SAML2SPClientAppDAO.class),
                mock(RealmDataBinder.class),
                mock(PropagationManager.class),
                mock(PropagationTaskExecutor.class)));
    }

    @Test
    void relationshipTypeLogic() {
        assertInstanceOf(RelationshipTypeLogic.class, context.relationshipTypeLogic(
                mock(RelationshipTypeDataBinder.class),
                mock(RelationshipTypeDAO.class),
                mock(ApplicationEventPublisher.class)));
    }

    @Test
    void reportLogic() {
        assertInstanceOf(ReportLogic.class, context.reportLogic(
                mock(JobManager.class),
                mock(ReportDataBinder.class),
                mock(SyncopeTaskScheduler.class),
                mock(JobStatusDAO.class),
                mock(ReportDAO.class),
                mock(EntityFactory.class),
                mock(ReportExecDAO.class)));
    }

    @Test
    void roleLogic() {
        assertInstanceOf(RoleLogic.class, context.roleLogic(
                mock(RoleDataBinder.class), mock(RoleDAO.class)));
    }

    @Test
    void schemaLogic() {
        assertInstanceOf(SchemaLogic.class, context.schemaLogic(
                mock(PlainSchemaDAO.class),
                mock(DerSchemaDAO.class),
                mock(AnyTypeClassDAO.class),
                mock(ImplementationDAO.class),
                mock(SchemaDataBinder.class)));
    }

    @Test
    void securityQuestionLogic() {
        assertInstanceOf(SecurityQuestionLogic.class, context.securityQuestionLogic(
                mock(SecurityQuestionDataBinder.class),
                mock(UserDAO.class),
                mock(SecurityQuestionDAO.class)));
    }

    @Test
    void syncopeLogic() {
        assertInstanceOf(SyncopeLogic.class, context.syncopeLogic(
                mock(RealmSearchDAO.class),
                mock(AnyTypeDAO.class),
                mock(GroupDAO.class),
                mock(RelationshipTypeDAO.class),
                mock(AnySearchDAO.class),
                mock(GroupDataBinder.class),
                mock(RelationshipTypeDataBinder.class),
                mock(ContentExporter.class)));
    }

    @Test
    void taskLogic() {
        assertInstanceOf(TaskLogic.class, context.taskLogic(
                mock(JobManager.class),
                mock(PropagationTaskExecutor.class),
                mock(TaskExecDAO.class),
                mock(TaskDAO.class),
                mock(SyncopeTaskScheduler.class),
                mock(JobStatusDAO.class),
                mock(ExternalResourceDAO.class),
                mock(NotificationJobDelegate.class),
                mock(TaskDataBinder.class),
                mock(TaskUtilsFactory.class),
                mock(NotificationDAO.class)));
    }

    @Test
    void userLogic() {
        assertInstanceOf(UserLogic.class, context.userLogic(
                mock(RealmSearchDAO.class),
                mock(AnyTypeDAO.class),
                mock(TemplateUtils.class),
                mock(UserDAO.class),
                mock(UserDataBinder.class),
                mock(UserProvisioningManager.class),
                mock(EncryptorManager.class),
                mock(ConfParamOps.class),
                mock(GroupDAO.class),
                mock(AnySearchDAO.class)));
    }

    @Test
    @SuppressWarnings("unchecked")
    void userSelfLogic() {
        Cache<String, ThrottlerAttempts> cache = mock(Cache.class);

        assertInstanceOf(UserSelfLogic.class, context.userSelfLogic(
                mock(RealmSearchDAO.class),
                mock(AnyTypeDAO.class),
                mock(TemplateUtils.class),
                mock(UserDAO.class),
                mock(UserDataBinder.class),
                mock(UserProvisioningManager.class),
                mock(EncryptorManager.class),
                mock(ConfParamOps.class),
                mock(DelegationDAO.class),
                mock(AccessTokenDAO.class),
                mock(ExternalResourceDAO.class),
                mock(RuleProvider.class),
                new SecurityProperties(),
                cache));
    }

    @Test
    void totpBeans() {
        assertInstanceOf(SecretGenerator.class, context.totpSecretGenerator());
        assertInstanceOf(QrGenerator.class, context.totpQrGenerator());
        assertInstanceOf(RecoveryCodeGenerator.class, context.totpRecoveryCodeGenerator());
    }

    @Test
    void mfaLogic() {
        assertInstanceOf(MfaLogic.class, context.mfaLogic(
                mock(UserDataBinder.class),
                mock(UserDAO.class),
                mock(EncryptorManager.class),
                mock(DomainOps.class),
                mock(SecretGenerator.class),
                mock(QrGenerator.class),
                HashingAlgorithm.SHA1,
                mock(RecoveryCodeGenerator.class),
                mock(CodeVerifier.class),
                new SecurityProperties()));
    }
}
