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

    InvalidDerivedSchemas(
    "Syncope.InvalidDerivedSchemas",
    "Syncope.InvalidDerivedSchemas.derivedSchemaName"),
    InvalidPassword(
    "Syncope.InvalidPassword",
    "Syncope.InvalidPassword.reason"),
    InvalidResources(
    "Syncope.InvalidResources",
    "Syncope.InvalidResources.resourceName"),
    InvalidRoles(
    "Syncope.InvalidRoles",
    "Syncope.InvalidRoles.roleName"),
    InvalidSchemaUpdate(
    "Syncope.InvalidSchemaUpdate",
    ""),
    InvalidUniques(
    "Syncope.InvalidUniques",
    "Syncope.InvalidUniques.attributeName"),
    InvalidValues(
    "Syncope.InvalidValues",
    "Syncope.InvalidValues.attributeName"),
    NotFound(
    "Syncope.NotFound",
    "Syncope.NotFound.entity"),
    RejectedUserCreate(
    "Syncope.RejectUserCreate",
    "Syncope.RejectUserCreate.syncopeUserId"),
    RequiredValuesMissing(
    "Syncope.RequiredValuesMissing",
    "Syncope.RequiredValuesMissing.attributeName"),
    UserInvalidValues(
    "Syncope.InvalidValues",
    "Syncope.InvalidValues.attributeName"),
    RequiredValueMissing(
    "Syncope.RequiredValuesMissing",
    "Syncope.RequiredValuesMissing.attributeName"),
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
