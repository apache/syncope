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
package org.apache.syncope.ext.oidcclient.agent;

public final class Constants {

    public static final String PARAM_OP = "op";

    public static final String CONTEXT_PARAM_LOGIN_SUCCESS_URL = "oidcclient.login.success.url";

    public static final String CONTEXT_PARAM_LOGIN_ERROR_URL = "oidcclient.login.error.url";

    public static final String CONTEXT_PARAM_LOGOUT_SUCCESS_URL = "oidcclient.logout.success.url";

    public static final String CONTEXT_PARAM_LOGOUT_ERROR_URL = "oidcclient.logout.error.url";

    public static final String CONTEXT_PARAM_REDIRECT_SELFREG_URL = "oidcclient.redirect.selfreg";

    public static final String OIDCCLIENTJWT = "oidcclient.jwt";

    public static final String OIDCCLIENTJWT_EXPIRE = "oidcclient.jwt.expire";

    public static final String OIDCCLIENT_USER_ATTRS = "oidcclient.userattrs";

    private Constants() {
        // private constructor for static utility class
    }
}
