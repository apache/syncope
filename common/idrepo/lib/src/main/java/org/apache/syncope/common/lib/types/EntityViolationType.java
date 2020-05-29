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
package org.apache.syncope.common.lib.types;

public enum EntityViolationType {

    Standard(""),
    InvalidAnyType("org.apache.syncope.core.persistence.validation.anytype"),
    InvalidADynMemberships("org.apache.syncope.core.persistence.validation.group.adynmemberships"),
    InvalidConnInstanceLocation("org.apache.syncope.core.persistence.validation.conninstance.location"),
    InvalidConnPoolConf("org.apache.syncope.core.persistence.validation.conninstance.poolConf"),
    InvalidMapping("org.apache.syncope.core.persistence.validation.mapping"),
    InvalidKey("org.apache.syncope.core.persistence.validation.key"),
    InvalidName("org.apache.syncope.core.persistence.validation.name"),
    InvalidPassword("org.apache.syncope.core.persistence.validation.user.password"),
    InvalidPolicy("org.apache.syncope.core.persistence.validation.policy"),
    InvalidPropagationTask("org.apache.syncope.core.persistence.validation.propagationtask"),
    InvalidRealm("org.apache.syncope.core.persistence.validation.realm"),
    InvalidDynRealm("org.apache.syncope.core.persistence.validation.dynrealm"),
    InvalidReport("org.apache.syncope.core.persistence.validation.report"),
    InvalidResource("org.apache.syncope.core.persistence.validation.externalresource"),
    InvalidGroupOwner("org.apache.syncope.core.persistence.validation.group.owner"),
    InvalidSchemaEncrypted("org.apache.syncope.core.persistence.validation.schema.encrypted"),
    InvalidSchemaEnum("org.apache.syncope.core.persistence.validation.schema.enum"),
    InvalidSchemaMultivalueUnique("org.apache.syncope.core.persistence.validation.schema.multivalueUnique"),
    InvalidSchedTask("org.apache.syncope.core.persistence.validation.schedtask"),
    InvalidProvisioningTask("org.apache.syncope.core.persistence.validation.provisioningtask"),
    InvalidPlainAttr("org.apache.syncope.core.persistence.validation.plainattr"),
    InvalidUsername("org.apache.syncope.core.persistence.validation.user.username"),
    InvalidValueList("org.apache.syncope.core.persistence.validation.attr.valueList"),
    InvalidRemediation("org.apache.syncope.core.persistence.validation.remediation"),
    MoreThanOneNonNull("org.apache.syncope.core.persistence.validation.attrvalue.moreThanOneNonNull");

    private String message;

    private String propertyPath;

    EntityViolationType(final String message) {
        this.message = message;
    }

    public void setMessage(final String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public String getPropertyPath() {
        return propertyPath;
    }

    public void setPropertyPath(final String propertyPath) {
        this.propertyPath = propertyPath;
    }

}
