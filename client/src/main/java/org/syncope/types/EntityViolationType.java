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
    "org.syncope.core.validation.externalresource.invalidAccountIdCount"),
    MoreThanOneNonNull(
    "org.syncope.core.validation.attrvalue.moreThanOneNonNull"),
    InvalidUSchema(
    "org.syncope.core.validation.attrvalue.invalidUSchema"),
    InvalidUDerSchema(
    "org.syncope.core.validation.attrvalue.invalidUDerSchema"),
    InvalidUVirSchema(
    "org.syncope.core.validation.attrvalue.invalidUVirSchema"),
    InvalidRSchema(
    "org.syncope.core.validation.attrvalue.invalidRSchema"),
    InvalidRDerSchema(
    "org.syncope.core.validation.attrvalue.invalidRDerSchema"),
    InvalidRVirSchema(
    "org.syncope.core.validation.attrvalue.invalidRVirSchema"),
    InvalidMSchema(
    "org.syncope.core.validation.attrvalue.invalidMSchema"),
    InvalidMDerSchema(
    "org.syncope.core.validation.attrvalue.invalidMDerSchema"),
    InvalidMVirSchema(
    "org.syncope.core.validation.attrvalue.invalidMVirSchema"),
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
    InvalidUsername(
    "org.syncope.core.validation.username.invalid"),
    InvalidPolicy(// not throwable using rest interface because the TO is typed
    "org.syncope.core.validation.policy.invalid"),
    InvalidPasswordPolicy(
    "org.syncope.core.validation.policy.invalid"),
    InvalidAccountPolicy(
    "org.syncope.core.validation.policy.invalid"),
    InvalidSyncPolicy(
    "org.syncope.core.validation.policy.invalid"),
    InvalidNotification(
    "org.syncope.core.validation.notification.invalid"),
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
