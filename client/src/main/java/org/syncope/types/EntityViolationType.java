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

public enum EntityViolationType {

    Standard(""),
    MultivalueAndUniqueConstraint(
    "org.syncope.core.validation.schema.multivalueAndUniqueConstraint"),
    InvalidAccountIdCount(
    "org.syncope.core.validation.targetresource.invalidAccountIdCount"),
    MoreThanOneNonNull(
    "org.syncope.core.validation.attrvalue.moreThanOneNonNull"),
    InvalidSchema(
    "org.syncope.core.validation.attrvalue.invalidSchema"),
    InvalidSchemaTypeSpecification(
    "org.syncope.core.validation.attrvalue.invalidSchemaTypeSpecification"),
    InvalidValueList(
    "org.syncope.core.validation.attr.invalidValueList"),
    InvalidEntitlementName(
    "org.syncope.core.validation.entitlement.invalidName"),
    InvalidPropagationTask(
    "org.syncope.core.validation.propagationtask.invalid"),
    InvalidSchedTask(
    "org.syncope.core.validation.schedtask.invalid"),
    InvalidSyncTask(
    "org.syncope.core.validation.synctask.invalid"),
    InvalidPassword(
    "org.syncope.core.validation.password.invalid"),
    InvalidPolicy(
    "org.syncope.core.validation.policy.invalid"),
    InvalidSchemaMapping(
    "org.syncope.core.validation.mapping.invalid");

    private String message;

    private EntityViolationType(final String message) {
        this.message = message;
    }

    public void setMessageTemplate(final String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return this == Standard ? message : super.toString();
    }
}
