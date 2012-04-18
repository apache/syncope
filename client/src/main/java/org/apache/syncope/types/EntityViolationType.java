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
package org.apache.syncope.types;

public enum EntityViolationType {

    Standard(""),
    MultivalueAndUniqueConstraint("org.apache.syncope.core.validation.schema.multivalueAndUniqueConstraint"),
    InvalidAccountIdCount("org.apache.syncope.core.validation.externalresource.invalidAccountIdCount"),
    MoreThanOneNonNull("org.apache.syncope.core.validation.attrvalue.moreThanOneNonNull"),
    InvalidUSchema("org.apache.syncope.core.validation.attrvalue.invalidUSchema"),
    InvalidUDerSchema("org.apache.syncope.core.validation.attrvalue.invalidUDerSchema"),
    InvalidUVirSchema("org.apache.syncope.core.validation.attrvalue.invalidUVirSchema"),
    InvalidRSchema("org.apache.syncope.core.validation.attrvalue.invalidRSchema"),
    InvalidRDerSchema("org.apache.syncope.core.validation.attrvalue.invalidRDerSchema"),
    InvalidRVirSchema("org.apache.syncope.core.validation.attrvalue.invalidRVirSchema"),
    InvalidMSchema("org.apache.syncope.core.validation.attrvalue.invalidMSchema"),
    InvalidMDerSchema("org.apache.syncope.core.validation.attrvalue.invalidMDerSchema"),
    InvalidMVirSchema("org.apache.syncope.core.validation.attrvalue.invalidMVirSchema"),
    InvalidSchemaTypeSpecification("org.apache.syncope.core.validation.attrvalue.invalidSchemaTypeSpecification"),
    InvalidValueList("org.apache.syncope.core.validation.attr.invalidValueList"),
    InvalidEntitlementName("org.apache.syncope.core.validation.entitlement.invalidName"),
    InvalidPropagationTask("org.apache.syncope.core.validation.propagationtask.invalid"),
    InvalidSchedTask("org.apache.syncope.core.validation.schedtask.invalid"),
    InvalidSyncTask("org.apache.syncope.core.validation.synctask.invalid"),
    InvalidPassword("org.apache.syncope.core.validation.password.invalid"),
    InvalidUsername("org.apache.syncope.core.validation.username.invalid"),
    InvalidPolicy(// not throwable using rest interface because the TO is typed
            "org.apache.syncope.core.validation.policy.invalid"),
    InvalidPasswordPolicy("org.apache.syncope.core.validation.policy.invalid"),
    InvalidAccountPolicy("org.apache.syncope.core.validation.policy.invalid"),
    InvalidSyncPolicy("org.apache.syncope.core.validation.policy.invalid"),
    InvalidNotification("org.apache.syncope.core.validation.notification.invalid"),
    InvalidReport("org.apache.syncope.core.validation.report.invalid"),
    InvalidSchemaMapping("org.apache.syncope.core.validation.mapping.invalid");

    private String message;

    private EntityViolationType(final String message) {
        this.message = message;
    }

    public void setMessageTemplate(final String message) {
        this.message = message;
    }

    @Override
    public String toString() {
        return this == Standard
                ? message
                : super.toString();
    }
}
