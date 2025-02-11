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
package org.apache.syncope.core.persistence.neo4j.entity;

import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
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
import org.apache.syncope.core.persistence.neo4j.entity.am.Neo4jAttrRepo;
import org.apache.syncope.core.persistence.neo4j.entity.am.Neo4jAuthModule;
import org.apache.syncope.core.persistence.neo4j.entity.am.Neo4jAuthProfile;
import org.apache.syncope.core.persistence.neo4j.entity.am.Neo4jCASSPClientApp;
import org.apache.syncope.core.persistence.neo4j.entity.am.Neo4jOIDCJWKS;
import org.apache.syncope.core.persistence.neo4j.entity.am.Neo4jOIDCRPClientApp;
import org.apache.syncope.core.persistence.neo4j.entity.am.Neo4jSAML2IdPEntity;
import org.apache.syncope.core.persistence.neo4j.entity.am.Neo4jSAML2SPClientApp;
import org.apache.syncope.core.persistence.neo4j.entity.am.Neo4jSAML2SPEntity;
import org.apache.syncope.core.persistence.neo4j.entity.am.Neo4jWAConfigEntry;
import org.apache.syncope.core.persistence.neo4j.entity.anyobject.Neo4jADynGroupMembership;
import org.apache.syncope.core.persistence.neo4j.entity.anyobject.Neo4jAMembership;
import org.apache.syncope.core.persistence.neo4j.entity.anyobject.Neo4jARelationship;
import org.apache.syncope.core.persistence.neo4j.entity.anyobject.Neo4jAnyObject;
import org.apache.syncope.core.persistence.neo4j.entity.group.Neo4jGRelationship;
import org.apache.syncope.core.persistence.neo4j.entity.group.Neo4jGroup;
import org.apache.syncope.core.persistence.neo4j.entity.group.Neo4jTypeExtension;
import org.apache.syncope.core.persistence.neo4j.entity.keymaster.Neo4jConfParam;
import org.apache.syncope.core.persistence.neo4j.entity.keymaster.Neo4jDomain;
import org.apache.syncope.core.persistence.neo4j.entity.keymaster.Neo4jNetworkService;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jAccessPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jAccountPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jAttrReleasePolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jAuthPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jInboundCorrelationRuleEntity;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jInboundPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jPasswordPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jPropagationPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jPushCorrelationRuleEntity;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jPushPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.policy.Neo4jTicketExpirationPolicy;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jAnyTemplatePullTask;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jFormPropertyDef;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jLiveSyncTask;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jMacroTask;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jMacroTaskCommand;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jNotificationTask;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jPropagationTask;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jPullTask;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jPushTask;
import org.apache.syncope.core.persistence.neo4j.entity.task.Neo4jSchedTask;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jLinkedAccount;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jSecurityQuestion;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jUDynGroupMembership;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jUMembership;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jURelationship;
import org.apache.syncope.core.persistence.neo4j.entity.user.Neo4jUser;
import org.apache.syncope.core.spring.security.SecureRandomUtils;

public class Neo4jEntityFactory implements EntityFactory {

    @SuppressWarnings("unchecked")
    @Override
    public <E extends Entity> E newEntity(final Class<E> reference) {
        E result;

        if (reference.equals(Realm.class)) {
            result = (E) new Neo4jRealm();
        } else if (reference.equals(DynRealm.class)) {
            result = (E) new Neo4jDynRealm();
        } else if (reference.equals(DynRealmMembership.class)) {
            result = (E) new Neo4jDynRealmMembership();
        } else if (reference.equals(AnyTemplateRealm.class)) {
            result = (E) new Neo4jAnyTemplateRealm();
        } else if (reference.equals(AccountPolicy.class)) {
            result = (E) new Neo4jAccountPolicy();
        } else if (reference.equals(PasswordPolicy.class)) {
            result = (E) new Neo4jPasswordPolicy();
        } else if (reference.equals(PropagationPolicy.class)) {
            result = (E) new Neo4jPropagationPolicy();
        } else if (reference.equals(PushPolicy.class)) {
            result = (E) new Neo4jPushPolicy();
        } else if (reference.equals(InboundPolicy.class)) {
            result = (E) new Neo4jInboundPolicy();
        } else if (reference.equals(InboundCorrelationRuleEntity.class)) {
            result = (E) new Neo4jInboundCorrelationRuleEntity();
        } else if (reference.equals(PushCorrelationRuleEntity.class)) {
            result = (E) new Neo4jPushCorrelationRuleEntity();
        } else if (reference.equals(AnyTypeClass.class)) {
            result = (E) new Neo4jAnyTypeClass();
        } else if (reference.equals(AnyType.class)) {
            result = (E) new Neo4jAnyType();
        } else if (reference.equals(AnyObject.class)) {
            result = (E) new Neo4jAnyObject();
        } else if (reference.equals(Role.class)) {
            result = (E) new Neo4jRole();
        } else if (reference.equals(User.class)) {
            result = (E) new Neo4jUser();
        } else if (reference.equals(Group.class)) {
            result = (E) new Neo4jGroup();
        } else if (reference.equals(TypeExtension.class)) {
            result = (E) new Neo4jTypeExtension();
        } else if (reference.equals(RelationshipType.class)) {
            result = (E) new Neo4jRelationshipType();
        } else if (reference.equals(ARelationship.class)) {
            result = (E) new Neo4jARelationship();
        } else if (reference.equals(GRelationship.class)) {
            result = (E) new Neo4jGRelationship();
        } else if (reference.equals(URelationship.class)) {
            result = (E) new Neo4jURelationship();
        } else if (reference.equals(AMembership.class)) {
            result = (E) new Neo4jAMembership();
        } else if (reference.equals(UMembership.class)) {
            result = (E) new Neo4jUMembership();
        } else if (reference.equals(LinkedAccount.class)) {
            result = (E) new Neo4jLinkedAccount();
        } else if (reference.equals(AnyAbout.class)) {
            result = (E) new Neo4jAnyAbout();
        } else if (reference.equals(MailTemplate.class)) {
            result = (E) new Neo4jMailTemplate();
        } else if (reference.equals(Notification.class)) {
            result = (E) new Neo4jNotification();
        } else if (reference.equals(ConnInstance.class)) {
            result = (E) new Neo4jConnInstance();
        } else if (reference.equals(ExternalResource.class)) {
            result = (E) new Neo4jExternalResource();
        } else if (reference.equals(PlainSchema.class)) {
            result = (E) new Neo4jPlainSchema();
        } else if (reference.equals(DerSchema.class)) {
            result = (E) new Neo4jDerSchema();
        } else if (reference.equals(VirSchema.class)) {
            result = (E) new Neo4jVirSchema();
        } else if (reference.equals(Report.class)) {
            result = (E) new Neo4jReport();
        } else if (reference.equals(ReportExec.class)) {
            result = (E) new Neo4jReportExec();
        } else if (reference.equals(NotificationTask.class)) {
            result = (E) new Neo4jNotificationTask();
        } else if (reference.equals(PropagationTask.class)) {
            result = (E) new Neo4jPropagationTask();
        } else if (reference.equals(PushTask.class)) {
            result = (E) new Neo4jPushTask();
        } else if (reference.equals(LiveSyncTask.class)) {
            result = (E) new Neo4jLiveSyncTask();
        } else if (reference.equals(PullTask.class)) {
            result = (E) new Neo4jPullTask();
        } else if (reference.equals(MacroTask.class)) {
            result = (E) new Neo4jMacroTask();
        } else if (reference.equals(SchedTask.class)) {
            result = (E) new Neo4jSchedTask();
        } else if (reference.equals(AnyTemplatePullTask.class)) {
            result = (E) new Neo4jAnyTemplatePullTask();
        } else if (reference.equals(MacroTaskCommand.class)) {
            result = (E) new Neo4jMacroTaskCommand();
        } else if (reference.equals(FormPropertyDef.class)) {
            result = (E) new Neo4jFormPropertyDef();
        } else if (reference.equals(SecurityQuestion.class)) {
            result = (E) new Neo4jSecurityQuestion();
        } else if (reference.equals(AuditConf.class)) {
            result = (E) new Neo4jAuditConf();
        } else if (reference.equals(ADynGroupMembership.class)) {
            result = (E) new Neo4jADynGroupMembership();
        } else if (reference.equals(UDynGroupMembership.class)) {
            result = (E) new Neo4jUDynGroupMembership();
        } else if (reference.equals(AccessToken.class)) {
            result = (E) new Neo4jAccessToken();
        } else if (reference.equals(Implementation.class)) {
            result = (E) new Neo4jImplementation();
        } else if (reference.equals(Remediation.class)) {
            result = (E) new Neo4jRemediation();
        } else if (reference.equals(Batch.class)) {
            result = (E) new Neo4jBatch();
        } else if (reference.equals(Delegation.class)) {
            result = (E) new Neo4jDelegation();
        } else if (reference.equals(FIQLQuery.class)) {
            result = (E) new Neo4jFIQLQuery();
        } else if (reference.equals(JobStatus.class)) {
            result = (E) new Neo4jJobStatus();
        } else if (reference.equals(SRARoute.class)) {
            result = (E) new Neo4jSRARoute();
        } else if (reference.equals(AuthModule.class)) {
            result = (E) new Neo4jAuthModule();
        } else if (reference.equals(AttrRepo.class)) {
            result = (E) new Neo4jAttrRepo();
        } else if (reference.equals(AuthPolicy.class)) {
            result = (E) new Neo4jAuthPolicy();
        } else if (reference.equals(AccessPolicy.class)) {
            result = (E) new Neo4jAccessPolicy();
        } else if (reference.equals(AttrReleasePolicy.class)) {
            result = (E) new Neo4jAttrReleasePolicy();
        } else if (reference.equals(TicketExpirationPolicy.class)) {
            result = (E) new Neo4jTicketExpirationPolicy();
        } else if (reference.equals(OIDCRPClientApp.class)) {
            result = (E) new Neo4jOIDCRPClientApp();
        } else if (reference.equals(CASSPClientApp.class)) {
            result = (E) new Neo4jCASSPClientApp();
        } else if (reference.equals(SAML2SPClientApp.class)) {
            result = (E) new Neo4jSAML2SPClientApp();
        } else if (reference.equals(SAML2IdPEntity.class)) {
            result = (E) new Neo4jSAML2IdPEntity();
        } else if (reference.equals(SAML2SPEntity.class)) {
            result = (E) new Neo4jSAML2SPEntity();
        } else if (reference.equals(AuthProfile.class)) {
            result = (E) new Neo4jAuthProfile();
        } else if (reference.equals(OIDCJWKS.class)) {
            result = (E) new Neo4jOIDCJWKS();
        } else if (reference.equals(WAConfigEntry.class)) {
            result = (E) new Neo4jWAConfigEntry();
        } else if (reference.equals(ConfParam.class)) {
            result = (E) new Neo4jConfParam();
        } else if (reference.equals(DomainEntity.class)) {
            result = (E) new Neo4jDomain();
        } else if (reference.equals(NetworkServiceEntity.class)) {
            result = (E) new Neo4jNetworkService();
        } else if (reference.equals(AuditEvent.class)) {
            result = (E) new Neo4jAuditEvent();
        } else {
            throw new IllegalArgumentException("Could not find a Neo4j implementation of " + reference.getName());
        }

        if (result instanceof AbstractGeneratedKeyNode generatedKeyEntity) {
            generatedKeyEntity.setKey(SecureRandomUtils.generateRandomUUID().toString());
        }

        return result;
    }

    @Override
    public Class<? extends User> userClass() {
        return Neo4jUser.class;
    }

    @Override
    public Class<? extends Group> groupClass() {
        return Neo4jGroup.class;
    }

    @Override
    public Class<? extends AnyObject> anyObjectClass() {
        return Neo4jAnyObject.class;
    }

    @Override
    public Class<? extends AnySearchDAO> anySearchDAOClass() {
        return null;
    }
}
