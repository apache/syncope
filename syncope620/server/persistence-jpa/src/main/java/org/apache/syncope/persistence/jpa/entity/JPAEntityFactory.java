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
package org.apache.syncope.persistence.jpa.entity;

import org.apache.syncope.persistence.api.entity.AccountPolicy;
import org.apache.syncope.persistence.api.entity.ConnInstance;
import org.apache.syncope.persistence.api.entity.ConnPoolConf;
import org.apache.syncope.persistence.api.entity.Entitlement;
import org.apache.syncope.persistence.api.entity.Entity;
import org.apache.syncope.persistence.api.entity.EntityFactory;
import org.apache.syncope.persistence.api.entity.ExternalResource;
import org.apache.syncope.persistence.api.entity.Notification;
import org.apache.syncope.persistence.api.entity.PasswordPolicy;
import org.apache.syncope.persistence.api.entity.Policy;
import org.apache.syncope.persistence.api.entity.PushPolicy;
import org.apache.syncope.persistence.api.entity.Report;
import org.apache.syncope.persistence.api.entity.ReportExec;
import org.apache.syncope.persistence.api.entity.ReportletConfInstance;
import org.apache.syncope.persistence.api.entity.SyncPolicy;
import org.apache.syncope.persistence.api.entity.conf.CPlainAttr;
import org.apache.syncope.persistence.api.entity.conf.CPlainAttrUniqueValue;
import org.apache.syncope.persistence.api.entity.conf.CPlainAttrValue;
import org.apache.syncope.persistence.api.entity.conf.CPlainSchema;
import org.apache.syncope.persistence.api.entity.conf.Conf;
import org.apache.syncope.persistence.api.entity.membership.MDerAttr;
import org.apache.syncope.persistence.api.entity.membership.MDerAttrTemplate;
import org.apache.syncope.persistence.api.entity.membership.MDerSchema;
import org.apache.syncope.persistence.api.entity.membership.MPlainAttr;
import org.apache.syncope.persistence.api.entity.membership.MPlainAttrTemplate;
import org.apache.syncope.persistence.api.entity.membership.MPlainAttrUniqueValue;
import org.apache.syncope.persistence.api.entity.membership.MPlainAttrValue;
import org.apache.syncope.persistence.api.entity.membership.MPlainSchema;
import org.apache.syncope.persistence.api.entity.membership.MVirAttr;
import org.apache.syncope.persistence.api.entity.membership.MVirAttrTemplate;
import org.apache.syncope.persistence.api.entity.membership.MVirSchema;
import org.apache.syncope.persistence.api.entity.membership.Membership;
import org.apache.syncope.persistence.api.entity.role.RDerAttr;
import org.apache.syncope.persistence.api.entity.role.RDerAttrTemplate;
import org.apache.syncope.persistence.api.entity.role.RDerSchema;
import org.apache.syncope.persistence.api.entity.role.RMapping;
import org.apache.syncope.persistence.api.entity.role.RMappingItem;
import org.apache.syncope.persistence.api.entity.role.RPlainAttr;
import org.apache.syncope.persistence.api.entity.role.RPlainAttrTemplate;
import org.apache.syncope.persistence.api.entity.role.RPlainAttrUniqueValue;
import org.apache.syncope.persistence.api.entity.role.RPlainAttrValue;
import org.apache.syncope.persistence.api.entity.role.RPlainSchema;
import org.apache.syncope.persistence.api.entity.role.RVirAttr;
import org.apache.syncope.persistence.api.entity.role.RVirAttrTemplate;
import org.apache.syncope.persistence.api.entity.role.RVirSchema;
import org.apache.syncope.persistence.api.entity.role.Role;
import org.apache.syncope.persistence.api.entity.task.NotificationTask;
import org.apache.syncope.persistence.api.entity.task.PropagationTask;
import org.apache.syncope.persistence.api.entity.task.PushTask;
import org.apache.syncope.persistence.api.entity.task.SchedTask;
import org.apache.syncope.persistence.api.entity.task.SyncTask;
import org.apache.syncope.persistence.api.entity.task.TaskExec;
import org.apache.syncope.persistence.api.entity.user.SecurityQuestion;
import org.apache.syncope.persistence.api.entity.user.UDerAttr;
import org.apache.syncope.persistence.api.entity.user.UDerSchema;
import org.apache.syncope.persistence.api.entity.user.UMapping;
import org.apache.syncope.persistence.api.entity.user.UMappingItem;
import org.apache.syncope.persistence.api.entity.user.UPlainAttr;
import org.apache.syncope.persistence.api.entity.user.UPlainAttrUniqueValue;
import org.apache.syncope.persistence.api.entity.user.UPlainAttrValue;
import org.apache.syncope.persistence.api.entity.user.UPlainSchema;
import org.apache.syncope.persistence.api.entity.user.UVirAttr;
import org.apache.syncope.persistence.api.entity.user.UVirSchema;
import org.apache.syncope.persistence.api.entity.user.User;
import org.apache.syncope.persistence.jpa.entity.conf.JPACPlainAttr;
import org.apache.syncope.persistence.jpa.entity.conf.JPACPlainAttrUniqueValue;
import org.apache.syncope.persistence.jpa.entity.conf.JPACPlainAttrValue;
import org.apache.syncope.persistence.jpa.entity.conf.JPACPlainSchema;
import org.apache.syncope.persistence.jpa.entity.conf.JPAConf;
import org.apache.syncope.persistence.jpa.entity.membership.JPAMDerAttr;
import org.apache.syncope.persistence.jpa.entity.membership.JPAMDerAttrTemplate;
import org.apache.syncope.persistence.jpa.entity.membership.JPAMDerSchema;
import org.apache.syncope.persistence.jpa.entity.membership.JPAMPlainAttr;
import org.apache.syncope.persistence.jpa.entity.membership.JPAMPlainAttrTemplate;
import org.apache.syncope.persistence.jpa.entity.membership.JPAMPlainAttrUniqueValue;
import org.apache.syncope.persistence.jpa.entity.membership.JPAMPlainAttrValue;
import org.apache.syncope.persistence.jpa.entity.membership.JPAMPlainSchema;
import org.apache.syncope.persistence.jpa.entity.membership.JPAMVirAttr;
import org.apache.syncope.persistence.jpa.entity.membership.JPAMVirAttrTemplate;
import org.apache.syncope.persistence.jpa.entity.membership.JPAMVirSchema;
import org.apache.syncope.persistence.jpa.entity.membership.JPAMembership;
import org.apache.syncope.persistence.jpa.entity.role.JPARDerAttr;
import org.apache.syncope.persistence.jpa.entity.role.JPARDerAttrTemplate;
import org.apache.syncope.persistence.jpa.entity.role.JPARDerSchema;
import org.apache.syncope.persistence.jpa.entity.role.JPARMapping;
import org.apache.syncope.persistence.jpa.entity.role.JPARMappingItem;
import org.apache.syncope.persistence.jpa.entity.role.JPARPlainAttr;
import org.apache.syncope.persistence.jpa.entity.role.JPARPlainAttrTemplate;
import org.apache.syncope.persistence.jpa.entity.role.JPARPlainAttrUniqueValue;
import org.apache.syncope.persistence.jpa.entity.role.JPARPlainAttrValue;
import org.apache.syncope.persistence.jpa.entity.role.JPARPlainSchema;
import org.apache.syncope.persistence.jpa.entity.role.JPARVirAttr;
import org.apache.syncope.persistence.jpa.entity.role.JPARVirAttrTemplate;
import org.apache.syncope.persistence.jpa.entity.role.JPARVirSchema;
import org.apache.syncope.persistence.jpa.entity.role.JPARole;
import org.apache.syncope.persistence.jpa.entity.task.JPANotificationTask;
import org.apache.syncope.persistence.jpa.entity.task.JPAPropagationTask;
import org.apache.syncope.persistence.jpa.entity.task.JPAPushTask;
import org.apache.syncope.persistence.jpa.entity.task.JPASchedTask;
import org.apache.syncope.persistence.jpa.entity.task.JPASyncTask;
import org.apache.syncope.persistence.jpa.entity.task.JPATaskExec;
import org.apache.syncope.persistence.jpa.entity.user.JPAUDerAttr;
import org.apache.syncope.persistence.jpa.entity.user.JPAUDerSchema;
import org.apache.syncope.persistence.jpa.entity.user.JPAUMapping;
import org.apache.syncope.persistence.jpa.entity.user.JPAUMappingItem;
import org.apache.syncope.persistence.jpa.entity.user.JPAUPlainAttr;
import org.apache.syncope.persistence.jpa.entity.user.JPAUPlainAttrUniqueValue;
import org.apache.syncope.persistence.jpa.entity.user.JPAUPlainAttrValue;
import org.apache.syncope.persistence.jpa.entity.user.JPAUPlainSchema;
import org.apache.syncope.persistence.jpa.entity.user.JPAUVirAttr;
import org.apache.syncope.persistence.jpa.entity.user.JPAUVirSchema;
import org.apache.syncope.persistence.jpa.entity.user.JPAUser;
import org.springframework.stereotype.Component;

@Component
public class JPAEntityFactory implements EntityFactory {

    @SuppressWarnings("unchecked")
    @Override
    public <KEY, T extends Entity<KEY>> T newEntity(final Class<T> reference) {
        T result;

        if (reference.equals(User.class)) {
            result = (T) new JPAUser();
        } else if (reference.equals(Role.class)) {
            result = (T) new JPARole();
        } else if (reference.equals(Membership.class)) {
            result = (T) new JPAMembership();
        } else if (reference.equals(Conf.class)) {
            result = (T) new JPAConf();
        } else if (reference.equals(Notification.class)) {
            result = (T) new JPANotification();
        } else if (reference.equals(Entitlement.class)) {
            result = (T) new JPAEntitlement();
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
        } else if (reference.equals(RPlainSchema.class)) {
            result = (T) new JPARPlainSchema();
        } else if (reference.equals(RPlainAttr.class)) {
            result = (T) new JPARPlainAttr();
        } else if (reference.equals(RPlainAttrValue.class)) {
            result = (T) new JPARPlainAttrValue();
        } else if (reference.equals(RPlainAttrUniqueValue.class)) {
            result = (T) new JPARPlainAttrUniqueValue();
        } else if (reference.equals(RPlainAttrTemplate.class)) {
            result = (T) new JPARPlainAttrTemplate();
        } else if (reference.equals(RDerAttrTemplate.class)) {
            result = (T) new JPARDerAttrTemplate();
        } else if (reference.equals(RVirAttrTemplate.class)) {
            result = (T) new JPARVirAttrTemplate();
        } else if (reference.equals(RDerSchema.class)) {
            result = (T) new JPARDerSchema();
        } else if (reference.equals(RDerAttr.class)) {
            result = (T) new JPARDerAttr();
        } else if (reference.equals(RVirSchema.class)) {
            result = (T) new JPARVirSchema();
        } else if (reference.equals(RVirAttr.class)) {
            result = (T) new JPARVirAttr();
        } else if (reference.equals(RMapping.class)) {
            result = (T) new JPARMapping();
        } else if (reference.equals(RMappingItem.class)) {
            result = (T) new JPARMappingItem();
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
        } else {
            throw new IllegalArgumentException("Could not find a JPA implementation of " + reference.getName());
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends Policy> T newPolicy(final Class<T> reference, final boolean global) {
        T result;

        if (reference.equals(AccountPolicy.class)) {
            result = (T) new JPAAccountPolicy(global);
        } else if (reference.equals(PasswordPolicy.class)) {
            result = (T) new JPAPasswordPolicy(global);
        } else if (reference.equals(PushPolicy.class)) {
            result = (T) new JPAPushPolicy(global);
        } else if (reference.equals(SyncPolicy.class)) {
            result = (T) new JPASyncPolicy(global);
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
