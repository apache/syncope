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

import org.apache.syncope.core.persistence.jpa.entity.policy.JPAPasswordPolicy;
import org.apache.syncope.core.persistence.jpa.entity.policy.JPASyncPolicy;
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
import org.apache.syncope.core.persistence.api.entity.ConnPoolConf;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.Domain;
import org.apache.syncope.core.persistence.api.entity.user.DynRoleMembership;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Logger;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.apache.syncope.core.persistence.api.entity.policy.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.policy.PushPolicy;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.RelationshipType;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.api.entity.ReportExec;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.policy.SyncPolicy;
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
import org.apache.syncope.core.persistence.api.entity.task.AnyTemplateSyncTask;
import org.apache.syncope.core.persistence.api.entity.task.NotificationTask;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.task.PushTaskAnyFilter;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.SyncTask;
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
import org.apache.syncope.core.persistence.jpa.entity.task.JPAAnyTemplateSyncTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPANotificationTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAPropagationTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAPushTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPASchedTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPASyncTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPATaskExec;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUDynGroupMembership;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUMembership;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAURelationship;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;
import org.springframework.stereotype.Component;

@Component
public class JPAEntityFactory implements EntityFactory {

    @SuppressWarnings("unchecked")
    @Override
    public <KEY, T extends Entity<KEY>> T newEntity(final Class<T> reference) {
        T result;

        if (reference.equals(Domain.class)) {
            result = (T) new JPADomain();
        } else if (reference.equals(Realm.class)) {
            result = (T) new JPARealm();
        } else if (reference.equals(AnyTemplateRealm.class)) {
            result = (T) new JPAAnyTemplateRealm();
        } else if (reference.equals(AccountPolicy.class)) {
            result = (T) new JPAAccountPolicy();
        } else if (reference.equals(PasswordPolicy.class)) {
            result = (T) new JPAPasswordPolicy();
        } else if (reference.equals(PushPolicy.class)) {
            result = (T) new JPAPushPolicy();
        } else if (reference.equals(SyncPolicy.class)) {
            result = (T) new JPASyncPolicy();
        } else if (reference.equals(AnyTypeClass.class)) {
            result = (T) new JPAAnyTypeClass();
        } else if (reference.equals(AnyType.class)) {
            result = (T) new JPAAnyType();
        } else if (reference.equals(AnyObject.class)) {
            result = (T) new JPAAnyObject();
        } else if (reference.equals(Role.class)) {
            result = (T) new JPARole();
        } else if (reference.equals(User.class)) {
            result = (T) new JPAUser();
        } else if (reference.equals(Group.class)) {
            result = (T) new JPAGroup();
        } else if (reference.equals(TypeExtension.class)) {
            result = (T) new JPATypeExtension();
        } else if (reference.equals(RelationshipType.class)) {
            result = (T) new JPARelationshipType();
        } else if (reference.equals(ARelationship.class)) {
            result = (T) new JPAARelationship();
        } else if (reference.equals(URelationship.class)) {
            result = (T) new JPAURelationship();
        } else if (reference.equals(AMembership.class)) {
            result = (T) new JPAAMembership();
        } else if (reference.equals(UMembership.class)) {
            result = (T) new JPAUMembership();
        } else if (reference.equals(Conf.class)) {
            result = (T) new JPAConf();
        } else if (reference.equals(AnyAbout.class)) {
            result = (T) new JPAAnyAbout();
        } else if (reference.equals(Notification.class)) {
            result = (T) new JPANotification();
        } else if (reference.equals(ExternalResource.class)) {
            result = (T) new JPAExternalResource();
        } else if (reference.equals(Provision.class)) {
            result = (T) new JPAProvision();
        } else if (reference.equals(ConnInstance.class)) {
            result = (T) new JPAConnInstance();
        } else if (reference.equals(PlainSchema.class)) {
            result = (T) new JPAPlainSchema();
        } else if (reference.equals(APlainAttr.class)) {
            result = (T) new JPAAPlainAttr();
        } else if (reference.equals(APlainAttrValue.class)) {
            result = (T) new JPAAPlainAttrValue();
        } else if (reference.equals(APlainAttrUniqueValue.class)) {
            result = (T) new JPAAPlainAttrUniqueValue();
        } else if (reference.equals(UPlainAttr.class)) {
            result = (T) new JPAUPlainAttr();
        } else if (reference.equals(UPlainAttrValue.class)) {
            result = (T) new JPAUPlainAttrValue();
        } else if (reference.equals(UPlainAttrUniqueValue.class)) {
            result = (T) new JPAUPlainAttrUniqueValue();
        } else if (reference.equals(DerSchema.class)) {
            result = (T) new JPADerSchema();
        } else if (reference.equals(VirSchema.class)) {
            result = (T) new JPAVirSchema();
        } else if (reference.equals(Mapping.class)) {
            result = (T) new JPAMapping();
        } else if (reference.equals(MappingItem.class)) {
            result = (T) new JPAMappingItem();
        } else if (reference.equals(GPlainAttr.class)) {
            result = (T) new JPAGPlainAttr();
        } else if (reference.equals(GPlainAttrValue.class)) {
            result = (T) new JPAGPlainAttrValue();
        } else if (reference.equals(GPlainAttrUniqueValue.class)) {
            result = (T) new JPAGPlainAttrUniqueValue();
        } else if (reference.equals(CPlainAttr.class)) {
            result = (T) new JPACPlainAttr();
        } else if (reference.equals(CPlainAttrValue.class)) {
            result = (T) new JPACPlainAttrValue();
        } else if (reference.equals(CPlainAttrUniqueValue.class)) {
            result = (T) new JPACPlainAttrUniqueValue();
        } else if (reference.equals(Report.class)) {
            result = (T) new JPAReport();
        } else if (reference.equals(ReportExec.class)) {
            result = (T) new JPAReportExec();
        } else if (reference.equals(NotificationTask.class)) {
            result = (T) new JPANotificationTask();
        } else if (reference.equals(PropagationTask.class)) {
            result = (T) new JPAPropagationTask();
        } else if (reference.equals(PushTask.class)) {
            result = (T) new JPAPushTask();
        } else if (reference.equals(SyncTask.class)) {
            result = (T) new JPASyncTask();
        } else if (reference.equals(SchedTask.class)) {
            result = (T) new JPASchedTask();
        } else if (reference.equals(TaskExec.class)) {
            result = (T) new JPATaskExec();
        } else if (reference.equals(PushTaskAnyFilter.class)) {
            result = (T) new JPAPushTaskAnyFilter();
        } else if (reference.equals(AnyTemplateSyncTask.class)) {
            result = (T) new JPAAnyTemplateSyncTask();
        } else if (reference.equals(SecurityQuestion.class)) {
            result = (T) new JPASecurityQuestion();
        } else if (reference.equals(Logger.class)) {
            result = (T) new JPALogger();
        } else if (reference.equals(DynRoleMembership.class)) {
            result = (T) new JPADynRoleMembership();
        } else if (reference.equals(ADynGroupMembership.class)) {
            result = (T) new JPAADynGroupMembership();
        } else if (reference.equals(UDynGroupMembership.class)) {
            result = (T) new JPAUDynGroupMembership();
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
