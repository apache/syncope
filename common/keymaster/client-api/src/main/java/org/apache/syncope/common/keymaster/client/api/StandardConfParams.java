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
package org.apache.syncope.common.keymaster.client.api;

public final class StandardConfParams {

    public static final String SELF_REGISTRATION_ALLOWED = "selfRegistration.allowed";

    public static final String PASSWORD_RESET_ALLOWED = "passwordReset.allowed";

    public static final String PASSWORD_RESET_SECURITY_QUESTION = "passwordReset.securityQuestion";

    public static final String MFA_ENABLED = "mfa.enabled";

    public static final String JWT_LIFETIME_MINUTES = "jwt.lifetime.minutes";

    public static final String NOTIFICATION_MAX_RETRIES = "notification.maxRetries";

    public static final String PASSWORD_CIPHER_ALGORITHM = "password.cipher.algorithm";

    public static final String AUTHENTICATION_ATTRIBUTES = "authentication.attributes";

    public static final String AUTHENTICATION_STATUSES = "authentication.statuses";

    public static final String NOTIFICATION_JOB_CRON_EXPRESSION = "notificationjob.cronExpression";

    public static final String TOKEN_LENGTH = "token.length";

    public static final String TOKEN_EXPIRE_TIME = "token.expireTime";

    public static final String LOG_LAST_LOGIN_DATE = "log.lastlogindate";

    private StandardConfParams() {
        // private constructor for static utility class
    }
}
