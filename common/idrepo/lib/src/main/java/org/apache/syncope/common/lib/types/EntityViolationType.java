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

    Standard,
    InvalidAnyType,
    InvalidConnInstanceLocation,
    InvalidConnPoolConf,
    InvalidFormPropertyDef,
    InvalidMapping,
    InvalidKey,
    InvalidName,
    InvalidPassword,
    InvalidPolicy,
    InvalidPropagationTask,
    InvalidRealm,
    InvalidReport,
    InvalidResource,
    InvalidManager,
    InvalidSchema,
    InvalidSchedTask,
    InvalidProvisioningTask,
    InvalidPlainAttr,
    InvalidUsername,
    InvalidValueList,
    InvalidRemediation,
    MoreThanOneNonNull,
    MoreThanOneEnabled;

    private String message;

    private String propertyPath;

    private Object invalidValue;

    public EntityViolationType message(final String message) {
        this.message = message;
        return this;
    }

    public String getMessage() {
        return message;
    }

    public EntityViolationType propertyPath(final String propertyPath) {
        this.propertyPath = propertyPath;
        return this;
    }

    public String getPropertyPath() {
        return propertyPath;
    }

    public EntityViolationType invalidValue(final Object invalidValue) {
        this.invalidValue = invalidValue;
        return this;
    }

    public Object getInvalidValue() {
        return invalidValue;
    }

    @Override
    public String toString() {
        return name() + "{"
                + "message=" + message
                + ", propertyPath=" + propertyPath
                + ", invalidValue=" + invalidValue
                + '}';
    }
}
