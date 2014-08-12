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
package org.apache.syncope.upgrader.util;

public enum SyncopeDefParams {

    PASSWORD_CIPHER_ALGORITHM("password.cipher.algorithm"),
    NOTIFICATION_JOB_CRON_EXPRESSION("notificationjob.cronExpression"),
    NOTIFICATION_MAX_RETRIES("notification.maxRetries"),
    TOKEN_LENGTH("token.length"),
    TOKEN_EXPIRE_TIME("token.expireTime"),
    SELF_REGISTRATION_ALLOWED("selfRegistration.allowed"),
    AUTHENTICATION_STATUSES("authentication.statuses"),
    LOG_LASTLOGIN_DATE("log.lastlogindate");

    private final String name;

    SyncopeDefParams(final String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public static boolean contains(final String param) {
        boolean contains = false;
        for (SyncopeDefParams defParam : values()) {
            if (defParam.getName().equals(param)) {
                contains = true;
            }
        }
        return contains;
    }
}
