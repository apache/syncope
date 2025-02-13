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
package org.apache.syncope.core.persistence.jpa.entity;

import org.apache.syncope.core.persistence.api.entity.AccessToken;
import org.apache.syncope.core.persistence.api.entity.AnyAbout;
import org.apache.syncope.core.persistence.api.entity.AnyTemplateRealm;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.AuditConf;
import org.apache.syncope.core.persistence.api.entity.AuditEvent;
import org.apache.syncope.core.persistence.api.entity.Batch;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.Delegation;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.persistence.api.entity.DynRealmMembership;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.FIQLQuery;
import org.apache.syncope.core.persistence.api.entity.Implementation;
import org.apache.syncope.core.persistence.api.entity.JobStatus;
import org.apache.syncope.core.persistence.api.entity.MailTemplate;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.Remediation;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.api.entity.ReportExec;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.SRARoute;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.am.AttrRepo;
import org.apache.syncope.core.persistence.api.entity.am.AuthModule;
import org.apache.syncope.core.persistence.api.entity.am.AuthProfile;
import org.apache.syncope.core.persistence.api.entity.am.CASSPClientApp;
import org.apache.syncope.core.persistence.api.entity.am.OIDCJWKS;
import org.apache.syncope.core.persistence.api.entity.am.OIDCRPClientApp;
import org.apache.syncope.core.persistence.api.entity.am.SAML2IdPEntity;
import org.apache.syncope.core.persistence.api.entity.am.SAML2SPClientApp;
import org.apache.syncope.core.persistence.api.entity.am.SAML2SPEntity;
import org.apache.syncope.core.persistence.api.entity.am.WAConfigEntry;
import org.apache.syncope.core.persistence.api.entity.anyobject.ADynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.ARelationship;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.GRelationship;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.group.TypeExtension;
import org.apache.syncope.core.persistence.api.entity.keymaster.ConfParam;
import org.apache.syncope.core.persistence.api.entity.keymaster.DomainEntity;
import org.apache.syncope.core.persistence.api.entity.keymaster.NetworkServiceEntity;
import org.apache.syncope.core.persistence.api.entity.policy.AccessPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AttrReleasePolicy;
import org.apache.syncope.core.persistence.api.entity.policy.AuthPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.InboundCorrelationRuleEntity;
import org.apache.syncope.core.persistence.api.entity.policy.InboundPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PropagationPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.PushCorrelationRuleEntity;
import org.apache.syncope.core.persistence.api.entity.policy.PushPolicy;
import org.apache.syncope.core.persistence.api.entity.policy.TicketExpirationPolicy;
import org.apache.syncope.core.persistence.api.entity.task.AnyTemplatePullTask;
import org.apache.syncope.core.persistence.api.entity.task.FormPropertyDef;
import org.apache.syncope.core.persistence.api.entity.task.LiveSyncTask;
import org.apache.syncope.core.persistence.api.entity.task.MacroTask;
import org.apache.syncope.core.persistence.api.entity.task.MacroTaskCommand;
import org.apache.syncope.core.persistence.api.entity.task.NotificationTask;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.user.LinkedAccount;
import org.apache.syncope.core.persistence.api.entity.user.SecurityQuestion;
import org.apache.syncope.core.persistence.api.entity.user.UDynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.URelationship;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.am.JPAAttrRepo;
import org.apache.syncope.core.persistence.jpa.entity.am.JPAAuthModule;
import org.apache.syncope.core.persistence.jpa.entity.am.JPAAuthProfile;
import org.apache.syncope.core.persistence.jpa.entity.am.JPACASSPClientApp;
import org.apache.syncope.core.persistence.jpa.entity.am.JPAOIDCJWKS;
import org.apache.syncope.core.persistence.jpa.entity.am.JPAOIDCRPClientApp;
import org.apache.syncope.core.persistence.jpa.entity.am.JPASAML2IdPEntity;
import org.apache.syncope.core.persistence.jpa.entity.am.JPASAML2SPClientApp;
import org.apache.syncope.core.persistence.jpa.entity.am.JPASAML2SPEntity;
import org.apache.syncope.core.persistence.jpa.entity.am.JPAWAConfigEntry;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAADynGroupMembership;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAMembership;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAARelationship;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAnyObject;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGRelationship;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGroup;
import org.apache.syncope.core.persistence.jpa.entity.group.JPATypeExtension;
import org.apache.syncope.core.persistence.jpa.entity.keymaster.JPAConfParam;
import org.apache.syncope.core.persistence.jpa.entity.keymaster.JPADomain;
import org.apache.syncope.core.persistence.jpa.entity.keymaster.JPANetworkService;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAAccessPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAAccountPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAAttrReleasePolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAAuthPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAInboundCorrelationRuleEntity;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAInboundPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAPasswordPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAPropagationPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAPushCorrelationRuleEntity;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAPushPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPATicketExpirationPolicy;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAAnyTemplatePullTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAFormPropertyDef;
import org.apache.syncope.core.persistence.jpa.entity.task.JPALiveSyncTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAMacroTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAMacroTaskCommand;
import org.apache.syncope.core.persistence.jpa.entity.task.JPANotificationTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAPropagationTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAPullTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAPushTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPASchedTask;
import org.apache.syncope.core.persistence.jpa.entity.user.JPALinkedAccount;
import org.apache.syncope.core.persistence.jpa.entity.user.JPASecurityQuestion;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUDynGroupMembership;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUMembership;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAURelationship;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;
import org.apache.syncope.core.spring.security.SecureRandomUtils;

abstract class AbstractEntityFactory implements EntityFactory {

    @SuppressWarnings("unchecked")
    @Override
    public <E extends Entity> E newEntity(final Class<E> reference) {
        E result;

        if (reference.equals(Realm.class)) {
            result = (E) new JPARealm();
        } else if (reference.equals(DynRealm.class)) {
            result = (E) new JPADynRealm();
        } else if (reference.equals(DynRealmMembership.class)) {
            result = (E) new JPADynRealmMembership();
        } else if (reference.equals(AnyTemplateRealm.class)) {
            result = (E) new JPAAnyTemplateRealm();
        } else if (reference.equals(AccountPolicy.class)) {
            result = (E) new JPAAccountPolicy();
        } else if (reference.equals(PasswordPolicy.class)) {
            result = (E) new JPAPasswordPolicy();
        } else if (reference.equals(PropagationPolicy.class)) {
            result = (E) new JPAPropagationPolicy();
        } else if (reference.equals(PushPolicy.class)) {
            result = (E) new JPAPushPolicy();
        } else if (reference.equals(InboundPolicy.class)) {
            result = (E) new JPAInboundPolicy();
        } else if (reference.equals(InboundCorrelationRuleEntity.class)) {
            result = (E) new JPAInboundCorrelationRuleEntity();
        } else if (reference.equals(PushCorrelationRuleEntity.class)) {
            result = (E) new JPAPushCorrelationRuleEntity();
        } else if (reference.equals(AnyTypeClass.class)) {
            result = (E) new JPAAnyTypeClass();
        } else if (reference.equals(AnyType.class)) {
            result = (E) new JPAAnyType();
        } else if (reference.equals(AnyObject.class)) {
            result = (E) new JPAAnyObject();
        } else if (reference.equals(Role.class)) {
            result = (E) new JPARole();
        } else if (reference.equals(User.class)) {
            result = (E) new JPAUser();
        } else if (reference.equals(Group.class)) {
            result = (E) new JPAGroup();
        } else if (reference.equals(TypeExtension.class)) {
            result = (E) new JPATypeExtension();
        } else if (reference.equals(RelationshipType.class)) {
            result = (E) new JPARelationshipType();
        } else if (reference.equals(ARelationship.class)) {
            result = (E) new JPAARelationship();
        } else if (reference.equals(URelationship.class)) {
            result = (E) new JPAURelationship();
        } else if (reference.equals(GRelationship.class)) {
            result = (E) new JPAGRelationship();
        } else if (reference.equals(AMembership.class)) {
            result = (E) new JPAAMembership();
        } else if (reference.equals(UMembership.class)) {
            result = (E) new JPAUMembership();
        } else if (reference.equals(LinkedAccount.class)) {
            result = (E) new JPALinkedAccount();
        } else if (reference.equals(AnyAbout.class)) {
            result = (E) new JPAAnyAbout();
        } else if (reference.equals(MailTemplate.class)) {
            result = (E) new JPAMailTemplate();
        } else if (reference.equals(Notification.class)) {
            result = (E) new JPANotification();
        } else if (reference.equals(ConnInstance.class)) {
            result = (E) new JPAConnInstance();
        } else if (reference.equals(ExternalResource.class)) {
            result = (E) new JPAExternalResource();
        } else if (reference.equals(PlainSchema.class)) {
            result = (E) new JPAPlainSchema();
        } else if (reference.equals(DerSchema.class)) {
            result = (E) new JPADerSchema();
        } else if (reference.equals(VirSchema.class)) {
            result = (E) new JPAVirSchema();
        } else if (reference.equals(Report.class)) {
            result = (E) new JPAReport();
        } else if (reference.equals(ReportExec.class)) {
            result = (E) new JPAReportExec();
        } else if (reference.equals(NotificationTask.class)) {
            result = (E) new JPANotificationTask();
        } else if (reference.equals(PropagationTask.class)) {
            result = (E) new JPAPropagationTask();
        } else if (reference.equals(PushTask.class)) {
            result = (E) new JPAPushTask();
        } else if (reference.equals(LiveSyncTask.class)) {
            result = (E) new JPALiveSyncTask();
        } else if (reference.equals(PullTask.class)) {
            result = (E) new JPAPullTask();
        } else if (reference.equals(MacroTask.class)) {
            result = (E) new JPAMacroTask();
        } else if (reference.equals(SchedTask.class)) {
            result = (E) new JPASchedTask();
        } else if (reference.equals(AnyTemplatePullTask.class)) {
            result = (E) new JPAAnyTemplatePullTask();
        } else if (reference.equals(MacroTaskCommand.class)) {
            result = (E) new JPAMacroTaskCommand();
        } else if (reference.equals(FormPropertyDef.class)) {
            result = (E) new JPAFormPropertyDef();
        } else if (reference.equals(SecurityQuestion.class)) {
            result = (E) new JPASecurityQuestion();
        } else if (reference.equals(AuditConf.class)) {
            result = (E) new JPAAuditConf();
        } else if (reference.equals(ADynGroupMembership.class)) {
            result = (E) new JPAADynGroupMembership();
        } else if (reference.equals(UDynGroupMembership.class)) {
            result = (E) new JPAUDynGroupMembership();
        } else if (reference.equals(AccessToken.class)) {
            result = (E) new JPAAccessToken();
        } else if (reference.equals(Implementation.class)) {
            result = (E) new JPAImplementation();
        } else if (reference.equals(Remediation.class)) {
            result = (E) new JPARemediation();
        } else if (reference.equals(Batch.class)) {
            result = (E) new JPABatch();
        } else if (reference.equals(Delegation.class)) {
            result = (E) new JPADelegation();
        } else if (reference.equals(FIQLQuery.class)) {
            result = (E) new JPAFIQLQuery();
        } else if (reference.equals(JobStatus.class)) {
            result = (E) new JPAJobStatus();
        } else if (reference.equals(SRARoute.class)) {
            result = (E) new JPASRARoute();
        } else if (reference.equals(AuthModule.class)) {
            result = (E) new JPAAuthModule();
        } else if (reference.equals(AttrRepo.class)) {
            result = (E) new JPAAttrRepo();
        } else if (reference.equals(AuthPolicy.class)) {
            result = (E) new JPAAuthPolicy();
        } else if (reference.equals(AccessPolicy.class)) {
            result = (E) new JPAAccessPolicy();
        } else if (reference.equals(AttrReleasePolicy.class)) {
            result = (E) new JPAAttrReleasePolicy();
        } else if (reference.equals(TicketExpirationPolicy.class)) {
            result = (E) new JPATicketExpirationPolicy();
        } else if (reference.equals(OIDCRPClientApp.class)) {
            result = (E) new JPAOIDCRPClientApp();
        } else if (reference.equals(CASSPClientApp.class)) {
            result = (E) new JPACASSPClientApp();
        } else if (reference.equals(SAML2SPClientApp.class)) {
            result = (E) new JPASAML2SPClientApp();
        } else if (reference.equals(SAML2IdPEntity.class)) {
            result = (E) new JPASAML2IdPEntity();
        } else if (reference.equals(SAML2SPEntity.class)) {
            result = (E) new JPASAML2SPEntity();
        } else if (reference.equals(AuthProfile.class)) {
            result = (E) new JPAAuthProfile();
        } else if (reference.equals(OIDCJWKS.class)) {
            result = (E) new JPAOIDCJWKS();
        } else if (reference.equals(WAConfigEntry.class)) {
            result = (E) new JPAWAConfigEntry();
        } else if (reference.equals(ConfParam.class)) {
            result = (E) new JPAConfParam();
        } else if (reference.equals(DomainEntity.class)) {
            result = (E) new JPADomain();
        } else if (reference.equals(NetworkServiceEntity.class)) {
            result = (E) new JPANetworkService();
        } else if (reference.equals(AuditEvent.class)) {
            result = (E) new JPAAuditEvent();
        } else {
            throw new IllegalArgumentException("Could not find a JPA implementation of " + reference.getName());
        }

        if (result instanceof AbstractGeneratedKeyEntity generatedKeyEntity) {
            generatedKeyEntity.setKey(SecureRandomUtils.generateRandomUUID().toString());
        }

        return result;
    }

    @Override
    public Class<? extends User> userClass() {
        return JPAUser.class;
    }

    @Override
    public Class<? extends Group> groupClass() {
        return JPAGroup.class;
    }

    @Override
    public Class<? extends AnyObject> anyObjectClass() {
        return JPAAnyObject.class;
    }
}
