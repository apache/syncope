/*
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *  under the License.
 */
package org.syncope.types;

public enum SyncopeClientExceptionType {

    Deadlock(
    "Syncope.Deadlock",
    ""),
    DuplicateUniqueValue(
    "Syncope.DuplicateUniqueValue",
    "Syncope.DuplicateUniqueValue.name"),
    GenericPersistence(
    "Syncope.GenericPersistence",
    "Syncope.GenericPersistence.cause"),
    InvalidConnInstance(
    "Syncope.InvalidConnInstance",
    "Syncope.InvalidConnInstance.message"),
    InvalidPasswordPolicy(
    "Syncope.InvalidPolicy",
    "Syncope.InvalidPolicy.reason"),
    InvalidAccountPolicy(
    "Syncope.InvalidPolicy",
    "Syncope.InvalidPolicy.reason"),
    InvalidSyncPolicy(
    "Syncope.InvalidPolicy",
    "Syncope.InvalidPolicy.reason"),
    InvalidRoles(
    "Syncope.InvalidRoles",
    "Syncope.InvalidRoles.name"),
    InvalidSchemaDefinition(
    "Syncope.InvalidSchemaDefinition",
    ""),
    InvalidSearchCondition(
    "Syncope.InvalidSearchCondition",
    ""),
    InvalidPropagationTaskExecReport(
    "Syncope.InvalidPropagationTaskExecReport",
    "Syncope.InvalidPropagationTaskExecReport.element"),
    InvalidUSchema(
    "Syncope.InvalidUSchemaUpdate",
    "Syncope.InvalidUSchemaUpdate.name"),
    InvalidUDerSchema(
    "Syncope.InvalidUDerSchemaUpdate",
    "Syncope.InvalidUDerSchemaUpdate.name"),
    InvalidUVirSchema(
    "Syncope.InvalidUVirSchemaUpdate",
    "Syncope.InvalidUVirSchemaUpdate.name"),
    InvalidRSchema(
    "Syncope.InvalidRSchemaUpdate",
    "Syncope.InvalidRSchemaUpdate.name"),
    InvalidRDerSchema(
    "Syncope.InvalidRDerSchemaUpdate",
    "Syncope.InvalidRDerSchemaUpdate.name"),
    InvalidRVirSchema(
    "Syncope.InvalidRVirSchemaUpdate",
    "Syncope.InvalidRVirSchemaUpdate.name"),
    InvalidMSchema(
    "Syncope.InvalidMSchemaUpdate",
    "Syncope.InvalidMSchemaUpdate.name"),
    InvalidMDerSchema(
    "Syncope.InvalidMDerSchemaUpdate",
    "Syncope.InvalidMDerSchemaUpdate.name"),
    InvalidMVirSchema(
    "Syncope.InvalidMVirSchemaUpdate",
    "Syncope.InvalidMVirSchemaUpdate.name"),
    InvalidSchemaMapping(
    "Syncope.InvalidSchemaMapping",
    ""),
    InvalidSyncopeUser(
    "Syncope.InvalidSyncopeUser",
    "Syncope.InvalidSyncopeUser.element"),
    InvalidExternalResource(
    "Syncope.InvalidExternalResource",
    "Syncope.InvalidExternalResource.element"),
    InvalidNotification(
    "Syncope.InvalidNotification",
    "Syncope.InvalidNotification.element"),
    InvalidTask(
    "Syncope.InvalidTask",
    "Syncope.InvalidTask.element"),
    InvalidValues(
    "Syncope.InvalidValues",
    "Syncope.InvalidValues.attributeName"),
    NotFound(
    "Syncope.NotFound",
    "Syncope.NotFound.entity"),
    Propagation(
    "Syncope.Propagation",
    "Syncope.Propagation.resourceName"),
    RejectedUserCreate(
    "Syncope.RejectUserCreate",
    "Syncope.RejectUserCreate.syncopeUserId"),
    RequiredValuesMissing(
    "Syncope.RequiredValuesMissing",
    "Syncope.RequiredValuesMissing.attributeName"),
    Scheduling(
    "Syncope.Scheduling",
    "Syncope.Scheduling.message"),
    UnauthorizedRole(
    "Syncope.UnauthorizedRole",
    "Syncope.UnauthorizedRole.id"),
    Unknown(
    "Syncope.Unknown",
    ""),
    Workflow(
    "Syncope.Workflow",
    "Syncope.Workflow.message");

    private String headerValue;

    private String elementHeaderName;

    private SyncopeClientExceptionType(String headerValue,
            String elementHeaderName) {

        this.headerValue = headerValue;
        this.elementHeaderName = elementHeaderName;
    }

    public static SyncopeClientExceptionType getFromHeaderValue(
            String exceptionTypeHeaderValue) {

        SyncopeClientExceptionType result = null;
        for (SyncopeClientExceptionType syncopeClientExceptionType : values()) {
            if (exceptionTypeHeaderValue.equals(
                    syncopeClientExceptionType.getHeaderValue())) {
                result = syncopeClientExceptionType;
            }
        }

        if (result == null) {
            throw new IllegalArgumentException("Unexpected header value: "
                    + exceptionTypeHeaderValue);
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
