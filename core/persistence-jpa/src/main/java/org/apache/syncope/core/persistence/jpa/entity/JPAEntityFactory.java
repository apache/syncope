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
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAPasswordPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAPullPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAAccountPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPAPushPolicy;
import org.apache.syncope.core.persistence.jpa.entity.user.JPADynRoleMembership;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAExternalResource;
import org.apache.syncope.core.persistence.api.entity.policy.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.AnyAbout;
import org.apache.syncope.core.persistence.api.entity.AnyTemplateRealm;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.ConnInstanceHistoryConf;
import org.apache.syncope.core.persistence.api.entity.ConnPoolConf;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.Domain;
import org.apache.syncope.core.persistence.api.entity.user.DynRoleMembership;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Logger;
import org.apache.syncope.core.persistence.api.entity.MailTemplate;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.policy.PushPolicy;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.api.entity.ReportExec;
import org.apache.syncope.core.persistence.api.entity.ReportTemplate;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.anyobject.ADynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.APlainAttr;
import org.apache.syncope.core.persistence.api.entity.anyobject.APlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.anyobject.APlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.anyobject.ARelationship;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttr;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.conf.Conf;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttr;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.group.TypeExtension;
import org.apache.syncope.core.persistence.api.entity.resource.Mapping;
import org.apache.syncope.core.persistence.api.entity.resource.MappingItem;
import org.apache.syncope.core.persistence.api.entity.resource.Provision;
import org.apache.syncope.core.persistence.api.entity.task.NotificationTask;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.task.PushTaskAnyFilter;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.user.SecurityQuestion;
import org.apache.syncope.core.persistence.api.entity.user.UDynGroupMembership;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.URelationship;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAADynGroupMembership;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAMembership;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAARelationship;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAnyObject;
import org.apache.syncope.core.persistence.jpa.entity.conf.JPACPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.conf.JPACPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.conf.JPACPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.conf.JPAConf;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGroup;
import org.apache.syncope.core.persistence.jpa.entity.group.JPATypeExtension;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAMapping;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAMappingItem;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAProvision;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAPushTaskAnyFilter;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAAnyTemplatePullTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPANotificationTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAPropagationTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAPushTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPASchedTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAPullTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPATaskExec;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUDynGroupMembership;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUMembership;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAURelationship;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;
import org.springframework.stereotype.Component;
import org.apache.syncope.core.persistence.api.entity.task.PullTask;
import org.apache.syncope.core.persistence.api.entity.task.AnyTemplatePullTask;
import org.apache.syncope.core.persistence.api.entity.policy.PullPolicy;
import org.apache.syncope.core.persistence.api.entity.resource.OrgUnit;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAOrgUnit;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.persistence.api.entity.DynRealmMembership;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResourceHistoryConf;
import org.apache.syncope.core.persistence.api.entity.resource.OrgUnitItem;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAExternalResourceHistoryConf;
import org.apache.syncope.core.persistence.jpa.entity.resource.JPAOrgUnitItem;

@Component
public class JPAEntityFactory implements EntityFactory {

    @SuppressWarnings("unchecked")
    @Override
    public <E extends Entity> E newEntity(final Class<E> reference) {
        E result;

        if (reference.equals(Domain.class)) {
            result = (E) new JPADomain();
        } else if (reference.equals(Realm.class)) {
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
        } else if (reference.equals(PushPolicy.class)) {
            result = (E) new JPAPushPolicy();
        } else if (reference.equals(PullPolicy.class)) {
            result = (E) new JPAPullPolicy();
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
        } else if (reference.equals(AMembership.class)) {
            result = (E) new JPAAMembership();
        } else if (reference.equals(UMembership.class)) {
            result = (E) new JPAUMembership();
        } else if (reference.equals(Conf.class)) {
            result = (E) new JPAConf();
        } else if (reference.equals(AnyAbout.class)) {
            result = (E) new JPAAnyAbout();
        } else if (reference.equals(MailTemplate.class)) {
            result = (E) new JPAMailTemplate();
        } else if (reference.equals(Notification.class)) {
            result = (E) new JPANotification();
        } else if (reference.equals(ExternalResource.class)) {
            result = (E) new JPAExternalResource();
        } else if (reference.equals(ExternalResourceHistoryConf.class)) {
            result = (E) new JPAExternalResourceHistoryConf();
        } else if (reference.equals(Provision.class)) {
            result = (E) new JPAProvision();
        } else if (reference.equals(OrgUnit.class)) {
            result = (E) new JPAOrgUnit();
        } else if (reference.equals(ConnInstance.class)) {
            result = (E) new JPAConnInstance();
        } else if (reference.equals(ConnInstanceHistoryConf.class)) {
            result = (E) new JPAConnInstanceHistoryConf();
        } else if (reference.equals(PlainSchema.class)) {
            result = (E) new JPAPlainSchema();
        } else if (reference.equals(APlainAttr.class)) {
            result = (E) new JPAAPlainAttr();
        } else if (reference.equals(APlainAttrValue.class)) {
            result = (E) new JPAAPlainAttrValue();
        } else if (reference.equals(APlainAttrUniqueValue.class)) {
            result = (E) new JPAAPlainAttrUniqueValue();
        } else if (reference.equals(UPlainAttr.class)) {
            result = (E) new JPAUPlainAttr();
        } else if (reference.equals(UPlainAttrValue.class)) {
            result = (E) new JPAUPlainAttrValue();
        } else if (reference.equals(UPlainAttrUniqueValue.class)) {
            result = (E) new JPAUPlainAttrUniqueValue();
        } else if (reference.equals(DerSchema.class)) {
            result = (E) new JPADerSchema();
        } else if (reference.equals(VirSchema.class)) {
            result = (E) new JPAVirSchema();
        } else if (reference.equals(Mapping.class)) {
            result = (E) new JPAMapping();
        } else if (reference.equals(MappingItem.class)) {
            result = (E) new JPAMappingItem();
        } else if (reference.equals(OrgUnitItem.class)) {
            result = (E) new JPAOrgUnitItem();
        } else if (reference.equals(GPlainAttr.class)) {
            result = (E) new JPAGPlainAttr();
        } else if (reference.equals(GPlainAttrValue.class)) {
            result = (E) new JPAGPlainAttrValue();
        } else if (reference.equals(GPlainAttrUniqueValue.class)) {
            result = (E) new JPAGPlainAttrUniqueValue();
        } else if (reference.equals(CPlainAttr.class)) {
            result = (E) new JPACPlainAttr();
        } else if (reference.equals(CPlainAttrValue.class)) {
            result = (E) new JPACPlainAttrValue();
        } else if (reference.equals(CPlainAttrUniqueValue.class)) {
            result = (E) new JPACPlainAttrUniqueValue();
        } else if (reference.equals(Report.class)) {
            result = (E) new JPAReport();
        } else if (reference.equals(ReportTemplate.class)) {
            result = (E) new JPAReportTemplate();
        } else if (reference.equals(ReportExec.class)) {
            result = (E) new JPAReportExec();
        } else if (reference.equals(NotificationTask.class)) {
            result = (E) new JPANotificationTask();
        } else if (reference.equals(PropagationTask.class)) {
            result = (E) new JPAPropagationTask();
        } else if (reference.equals(PushTask.class)) {
            result = (E) new JPAPushTask();
        } else if (reference.equals(PullTask.class)) {
            result = (E) new JPAPullTask();
        } else if (reference.equals(SchedTask.class)) {
            result = (E) new JPASchedTask();
        } else if (reference.equals(TaskExec.class)) {
            result = (E) new JPATaskExec();
        } else if (reference.equals(PushTaskAnyFilter.class)) {
            result = (E) new JPAPushTaskAnyFilter();
        } else if (reference.equals(AnyTemplatePullTask.class)) {
            result = (E) new JPAAnyTemplatePullTask();
        } else if (reference.equals(SecurityQuestion.class)) {
            result = (E) new JPASecurityQuestion();
        } else if (reference.equals(Logger.class)) {
            result = (E) new JPALogger();
        } else if (reference.equals(DynRoleMembership.class)) {
            result = (E) new JPADynRoleMembership();
        } else if (reference.equals(ADynGroupMembership.class)) {
            result = (E) new JPAADynGroupMembership();
        } else if (reference.equals(UDynGroupMembership.class)) {
            result = (E) new JPAUDynGroupMembership();
        } else if (reference.equals(AccessToken.class)) {
            result = (E) new JPAAccessToken();
        } else {
            throw new IllegalArgumentException("Could not find a JPA implementation of " + reference.getName());
        }

        return result;
    }

    @Override
    public ConnPoolConf newConnPoolConf() {
        return new JPAConnPoolConf();
    }

}
