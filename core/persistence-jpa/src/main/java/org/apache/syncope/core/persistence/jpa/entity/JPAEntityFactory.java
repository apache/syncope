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

import org.apache.syncope.core.persistence.api.entity.AccountPolicy;
import org.apache.syncope.core.persistence.api.entity.ConnInstance;
import org.apache.syncope.core.persistence.api.entity.ConnPoolConf;
import org.apache.syncope.core.persistence.api.entity.Entity;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.Logger;
import org.apache.syncope.core.persistence.api.entity.Notification;
import org.apache.syncope.core.persistence.api.entity.PasswordPolicy;
import org.apache.syncope.core.persistence.api.entity.PushPolicy;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.Report;
import org.apache.syncope.core.persistence.api.entity.ReportExec;
import org.apache.syncope.core.persistence.api.entity.ReportletConfInstance;
import org.apache.syncope.core.persistence.api.entity.Role;
import org.apache.syncope.core.persistence.api.entity.SyncPolicy;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttr;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.conf.CPlainSchema;
import org.apache.syncope.core.persistence.api.entity.conf.Conf;
import org.apache.syncope.core.persistence.api.entity.membership.MDerAttr;
import org.apache.syncope.core.persistence.api.entity.membership.MDerAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.membership.MDerSchema;
import org.apache.syncope.core.persistence.api.entity.membership.MPlainAttr;
import org.apache.syncope.core.persistence.api.entity.membership.MPlainAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.membership.MPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.membership.MPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.membership.MPlainSchema;
import org.apache.syncope.core.persistence.api.entity.membership.MVirAttr;
import org.apache.syncope.core.persistence.api.entity.membership.MVirAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.membership.MVirSchema;
import org.apache.syncope.core.persistence.api.entity.membership.Membership;
import org.apache.syncope.core.persistence.api.entity.group.GDerAttr;
import org.apache.syncope.core.persistence.api.entity.group.GDerAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.group.GDerSchema;
import org.apache.syncope.core.persistence.api.entity.group.GMapping;
import org.apache.syncope.core.persistence.api.entity.group.GMappingItem;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttr;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.group.GPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.group.GPlainSchema;
import org.apache.syncope.core.persistence.api.entity.group.GVirAttr;
import org.apache.syncope.core.persistence.api.entity.group.GVirAttrTemplate;
import org.apache.syncope.core.persistence.api.entity.group.GVirSchema;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.task.NotificationTask;
import org.apache.syncope.core.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.core.persistence.api.entity.task.PushTask;
import org.apache.syncope.core.persistence.api.entity.task.SchedTask;
import org.apache.syncope.core.persistence.api.entity.task.SyncTask;
import org.apache.syncope.core.persistence.api.entity.task.TaskExec;
import org.apache.syncope.core.persistence.api.entity.user.SecurityQuestion;
import org.apache.syncope.core.persistence.api.entity.user.UDerAttr;
import org.apache.syncope.core.persistence.api.entity.user.UDerSchema;
import org.apache.syncope.core.persistence.api.entity.user.UMapping;
import org.apache.syncope.core.persistence.api.entity.user.UMappingItem;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.user.UPlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.UPlainSchema;
import org.apache.syncope.core.persistence.api.entity.user.UVirAttr;
import org.apache.syncope.core.persistence.api.entity.user.UVirSchema;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.conf.JPACPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.conf.JPACPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.conf.JPACPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.conf.JPACPlainSchema;
import org.apache.syncope.core.persistence.jpa.entity.conf.JPAConf;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMDerAttr;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMDerAttrTemplate;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMDerSchema;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMPlainAttrTemplate;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMPlainSchema;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMVirAttr;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMVirAttrTemplate;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMVirSchema;
import org.apache.syncope.core.persistence.jpa.entity.membership.JPAMembership;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGDerAttr;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGDerAttrTemplate;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGDerSchema;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGMapping;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGMappingItem;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGPlainAttrTemplate;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGPlainSchema;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGVirAttr;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGVirAttrTemplate;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGVirSchema;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGroup;
import org.apache.syncope.core.persistence.jpa.entity.task.JPANotificationTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAPropagationTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPAPushTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPASchedTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPASyncTask;
import org.apache.syncope.core.persistence.jpa.entity.task.JPATaskExec;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUDerAttr;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUDerSchema;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUMapping;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUMappingItem;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUPlainAttr;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUPlainAttrUniqueValue;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUPlainAttrValue;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUPlainSchema;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUVirAttr;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUVirSchema;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;
import org.springframework.stereotype.Component;

@Component
public class JPAEntityFactory implements EntityFactory {

    @SuppressWarnings("unchecked")
    @Override
    public <KEY, T extends Entity<KEY>> T newEntity(final Class<T> reference) {
        T result;

        if (reference.equals(Realm.class)) {
            result = (T) new JPARealm();
        } else if (reference.equals(AccountPolicy.class)) {
            result = (T) new JPAAccountPolicy();
        } else if (reference.equals(PasswordPolicy.class)) {
            result = (T) new JPAPasswordPolicy();
        } else if (reference.equals(PushPolicy.class)) {
            result = (T) new JPAPushPolicy();
        } else if (reference.equals(SyncPolicy.class)) {
            result = (T) new JPASyncPolicy();
        } else if (reference.equals(Role.class)) {
            result = (T) new JPARole();
        } else if (reference.equals(User.class)) {
            result = (T) new JPAUser();
        } else if (reference.equals(Group.class)) {
            result = (T) new JPAGroup();
        } else if (reference.equals(Membership.class)) {
            result = (T) new JPAMembership();
        } else if (reference.equals(Conf.class)) {
            result = (T) new JPAConf();
        } else if (reference.equals(Notification.class)) {
            result = (T) new JPANotification();
        } else if (reference.equals(ExternalResource.class)) {
            result = (T) new JPAExternalResource();
        } else if (reference.equals(ConnInstance.class)) {
            result = (T) new JPAConnInstance();
        } else if (reference.equals(UPlainSchema.class)) {
            result = (T) new JPAUPlainSchema();
        } else if (reference.equals(UPlainAttr.class)) {
            result = (T) new JPAUPlainAttr();
        } else if (reference.equals(UPlainAttrValue.class)) {
            result = (T) new JPAUPlainAttrValue();
        } else if (reference.equals(UPlainAttrUniqueValue.class)) {
            result = (T) new JPAUPlainAttrUniqueValue();
        } else if (reference.equals(UDerSchema.class)) {
            result = (T) new JPAUDerSchema();
        } else if (reference.equals(UDerAttr.class)) {
            result = (T) new JPAUDerAttr();
        } else if (reference.equals(UVirSchema.class)) {
            result = (T) new JPAUVirSchema();
        } else if (reference.equals(UVirAttr.class)) {
            result = (T) new JPAUVirAttr();
        } else if (reference.equals(UMapping.class)) {
            result = (T) new JPAUMapping();
        } else if (reference.equals(UMappingItem.class)) {
            result = (T) new JPAUMappingItem();
        } else if (reference.equals(GPlainSchema.class)) {
            result = (T) new JPAGPlainSchema();
        } else if (reference.equals(GPlainAttr.class)) {
            result = (T) new JPAGPlainAttr();
        } else if (reference.equals(GPlainAttrValue.class)) {
            result = (T) new JPAGPlainAttrValue();
        } else if (reference.equals(GPlainAttrUniqueValue.class)) {
            result = (T) new JPAGPlainAttrUniqueValue();
        } else if (reference.equals(GPlainAttrTemplate.class)) {
            result = (T) new JPAGPlainAttrTemplate();
        } else if (reference.equals(GDerAttrTemplate.class)) {
            result = (T) new JPAGDerAttrTemplate();
        } else if (reference.equals(GVirAttrTemplate.class)) {
            result = (T) new JPAGVirAttrTemplate();
        } else if (reference.equals(GDerSchema.class)) {
            result = (T) new JPAGDerSchema();
        } else if (reference.equals(GDerAttr.class)) {
            result = (T) new JPAGDerAttr();
        } else if (reference.equals(GVirSchema.class)) {
            result = (T) new JPAGVirSchema();
        } else if (reference.equals(GVirAttr.class)) {
            result = (T) new JPAGVirAttr();
        } else if (reference.equals(GMapping.class)) {
            result = (T) new JPAGMapping();
        } else if (reference.equals(GMappingItem.class)) {
            result = (T) new JPAGMappingItem();
        } else if (reference.equals(MPlainSchema.class)) {
            result = (T) new JPAMPlainSchema();
        } else if (reference.equals(MPlainAttr.class)) {
            result = (T) new JPAMPlainAttr();
        } else if (reference.equals(MPlainAttrValue.class)) {
            result = (T) new JPAMPlainAttrValue();
        } else if (reference.equals(MPlainAttrUniqueValue.class)) {
            result = (T) new JPAMPlainAttrUniqueValue();
        } else if (reference.equals(MDerSchema.class)) {
            result = (T) new JPAMDerSchema();
        } else if (reference.equals(MDerAttr.class)) {
            result = (T) new JPAMDerAttr();
        } else if (reference.equals(MVirSchema.class)) {
            result = (T) new JPAMVirSchema();
        } else if (reference.equals(MVirAttr.class)) {
            result = (T) new JPAMVirAttr();
        } else if (reference.equals(MPlainAttrTemplate.class)) {
            result = (T) new JPAMPlainAttrTemplate();
        } else if (reference.equals(MDerAttrTemplate.class)) {
            result = (T) new JPAMDerAttrTemplate();
        } else if (reference.equals(MVirAttrTemplate.class)) {
            result = (T) new JPAMVirAttrTemplate();
        } else if (reference.equals(CPlainSchema.class)) {
            result = (T) new JPACPlainSchema();
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
        } else if (reference.equals(ReportletConfInstance.class)) {
            result = (T) new JPAReportletConfInstance();
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
        } else if (reference.equals(SecurityQuestion.class)) {
            result = (T) new JPASecurityQuestion();
        } else if (reference.equals(Logger.class)) {
            result = (T) new JPALogger();
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
