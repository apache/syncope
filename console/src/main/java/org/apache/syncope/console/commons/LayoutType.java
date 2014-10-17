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
package org.apache.syncope.console.commons;

public enum LayoutType {

    ADMIN_USER("admin.user.layout"),
    SELF_USER("self.user.layout"),
    ADMIN_ROLE("admin.role.layout"),
    SELF_ROLE("self.role.layout"),
    ADMIN_MEMBERSHIP("admin.membership.layout"),
    SELF_MEMBERSHIP("self.membership.layout");

    private final String parameter;

    LayoutType(final String parameter) {
        this.parameter = parameter;
    }

    public String getParameter() {
        return parameter;
    }

    public static LayoutType fromString(final String value) {
        return LayoutType.valueOf(value.toUpperCase());
    }
}
