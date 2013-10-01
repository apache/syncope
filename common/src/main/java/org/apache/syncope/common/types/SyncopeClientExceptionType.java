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
package org.apache.syncope.common.types;

import javax.xml.bind.annotation.XmlEnum;

@XmlEnum
public enum SyncopeClientExceptionType {

    AssociatedResources("Syncope.AssociatedResources", "Syncope.AssociatedResources.name"),
    Deadlock("Syncope.Deadlock", ""),
    DataIntegrityViolation("Syncope.DataIntegrityViolation", "Syncope.DataIntegrityViolation.name"),
    EntityExists("Syncope.EntityExists", "Syncope.EntityExists.name"),
    GenericPersistence("Syncope.GenericPersistence", "Syncope.GenericPersistence.cause"),
    InvalidLogger("Syncope.InvalidLogger", "Syncope.InvalidLogger.message"),
    InvalidConnInstance("Syncope.InvalidConnInstance", "Syncope.InvalidConnInstance.message"),
    InvalidConnIdConf("Syncope.InvalidConnIdConf", "Syncope.InvalidConnIdConf.message"),
    InvalidPasswordPolicy("Syncope.InvalidPasswordPolicy", "Syncope.InvalidPasswordPolicy.reason"),
    InvalidAccountPolicy("Syncope.InvalidAccountPolicy", "Syncope.InvalidAccountPolicy.reason"),
    InvalidSyncPolicy("Syncope.InvalidSyncPolicy", "Syncope.InvalidSyncPolicy.reason"),
    InvalidSyncopeRole("Syncope.InvalidSyncopeRole", "Syncope.InvalidSyncopeRole.reason"),
    InvalidReportExec("Syncope.InvalidReportExec", "Syncope.InvalidReportExec.reason"),
    InvalidRoles("Syncope.InvalidRoles", "Syncope.InvalidRoles.name"),
    InvalidSchemaDefinition("Syncope.InvalidSchemaDefinition", ""),
    InvalidSearchCondition("Syncope.InvalidSearchCondition", ""),
    InvalidPropagationTaskExecReport(
    "Syncope.InvalidPropagationTaskExecReport",
    "Syncope.InvalidPropagationTaskExecReport.element"),
    InvalidUSchema("Syncope.InvalidUSchema", "Syncope.InvalidUSchema.name"),
    InvalidUDerSchema("Syncope.InvalidUDerSchema", "Syncope.InvalidUDerSchema.name"),
    InvalidUVirSchema("Syncope.InvalidUVirSchema", "Syncope.InvalidUVirSchema.name"),
    InvalidRSchema("Syncope.InvalidRSchema", "Syncope.InvalidRSchema.name"),
    InvalidRDerSchema("Syncope.InvalidRDerSchema", "Syncope.InvalidRDerSchema.name"),
    InvalidRVirSchema("Syncope.InvalidRVirSchema", "Syncope.InvalidRVirSchema.name"),
    InvalidMSchema("Syncope.InvalidMSchema", "Syncope.InvalidMSchema.name"),
    InvalidMDerSchema("Syncope.InvalidMDerSchema", "Syncope.InvalidMDerSchema.name"),
    InvalidMVirSchema("Syncope.InvalidMVirSchema", "Syncope.InvalidMVirSchema.name"),
    InvalidSchemaMapping("Syncope.InvalidSchemaMapping", "Syncope.InvalidSchemaMapping.name"),
    InvalidSyncopeConf("Syncope.InvalidSyncopeConf", "Syncope.InvalidSyncopeConf.name"),
    InvalidSyncopeUser("Syncope.InvalidSyncopeUser", "Syncope.InvalidSyncopeUser.element"),
    InvalidExternalResource("Syncope.InvalidExternalResource", "Syncope.InvalidExternalResource.element"),
    InvalidNotification("Syncope.InvalidNotification", "Syncope.InvalidNotification.element"),
    InvalidPropagationTask("Syncope.InvalidPropagationTask", "Syncope.InvalidPropagationTask.element"),
    InvalidSchedTask("Syncope.InvalidSchedTask", "Syncope.InvalidSchedTask.element"),
    InvalidSyncTask("Syncope.InvalidSyncTask", "Syncope.InvalidSyncTask.element"),
    InvalidValues("Syncope.InvalidValues", "Syncope.InvalidValues.attributeName"),
    NotFound("Syncope.NotFound", "Syncope.NotFound.entity"),
    RejectedUserCreate("Syncope.RejectUserCreate", "Syncope.RejectUserCreate.userId"),
    RequiredValuesMissing("Syncope.RequiredValuesMissing", "Syncope.RequiredValuesMissing.attributeName"),
    RoleOwnership("Syncope.RoleOwnership", "Syncope.RoleOwnership.role"),
    Scheduling("Syncope.Scheduling", "Syncope.Scheduling.message"),
    UnauthorizedRole("Syncope.UnauthorizedRole", "Syncope.UnauthorizedRole.id"),
    Unauthorized("Syncope.Unauthorized", "Syncope.Unauthorized"),
    Unknown("Syncope.Unknown", ""),
    Workflow("Syncope.Workflow", "Syncope.Workflow.message");

    private String headerValue;

    private String elementHeaderName;

    private SyncopeClientExceptionType(final String headerValue, final String elementHeaderName) {
        this.headerValue = headerValue;
        this.elementHeaderName = elementHeaderName;
    }

    public static SyncopeClientExceptionType getFromHeaderValue(final String exceptionTypeHeaderValue) {
        SyncopeClientExceptionType result = null;
        for (SyncopeClientExceptionType syncopeClientExceptionType : values()) {
            if (exceptionTypeHeaderValue.equals(syncopeClientExceptionType.getHeaderValue())) {
                result = syncopeClientExceptionType;
            }
        }

        if (result == null) {
            throw new IllegalArgumentException("Unexpected header value: " + exceptionTypeHeaderValue);
        }

        return result;
    }

    public String getElementHeaderName() {
        return elementHeaderName;
    }

    public String getHeaderValue() {
        return headerValue;
    }
}
