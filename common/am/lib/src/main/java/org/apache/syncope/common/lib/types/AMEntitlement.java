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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

public final class AMEntitlement {

    public static final String SRA_ROUTE_CREATE = "SRA_ROUTE_CREATE";

    public static final String SRA_ROUTE_UPDATE = "SRA_ROUTE_UPDATE";

    public static final String SRA_ROUTE_DELETE = "SRA_ROUTE_DELETE";

    public static final String SRA_ROUTE_PUSH = "SRA_ROUTE_PUSH";

    public static final String CLIENTAPP_READ = "CLIENTAPP_READ";

    public static final String CLIENTAPP_LIST = "CLIENTAPP_LIST";

    public static final String CLIENTAPP_CREATE = "CLIENTAPP_CREATE";

    public static final String CLIENTAPP_UPDATE = "CLIENTAPP_CREATE";

    public static final String CLIENTAPP_DELETE = "CLIENTAPP_DELETE";

    public static final String CLIENTAPP_PUSH = "CLIENTAPP_PUSH";

    public static final String AUTH_MODULE_LIST = "AUTH_MODULE_LIST";

    public static final String AUTH_MODULE_CREATE = "AUTH_MODULE_CREATE";

    public static final String AUTH_MODULE_READ = "AUTH_MODULE_READ";

    public static final String AUTH_MODULE_UPDATE = "AUTH_MODULE_UPDATE";

    public static final String AUTH_MODULE_DELETE = "AUTH_MODULE_DELETE";

    public static final String SAML2_IDP_METADATA_SET = "SAML2_IDP_METADATA_SET";

    public static final String SAML2_IDP_METADATA_READ = "SAML2_IDP_METADATA_READ";

    public static final String SAML2_SP_METADATA_SET = "SAML2_SP_METADATA_SET";

    public static final String SAML2_SP_METADATA_READ = "SAML2_SP_METADATA_READ";

    public static final String SAML2_SP_KEYSTORE_SET = "SAML2_SP_KEYSTORE_SET";

    public static final String SAML2_SP_KEYSTORE_READ = "SAML2_SP_KEYSTORE_READ";

    public static final String GOOGLE_MFA_DELETE_TOKEN = "GOOGLE_MFA_DELETE_TOKEN";

    public static final String GOOGLE_MFA_STORE_TOKEN = "GOOGLE_MFA_STORE_TOKEN";

    public static final String GOOGLE_MFA_READ_TOKEN = "GOOGLE_MFA_READ_TOKEN";

    public static final String GOOGLE_MFA_LIST_TOKENS = "GOOGLE_MFA_LIST_TOKENS";

    public static final String AUTH_PROFILE_DELETE = "AUTH_PROFILE_DELETE";

    public static final String AUTH_PROFILE_READ = "AUTH_PROFILE_READ";

    public static final String AUTH_PROFILE_LIST = "AUTH_PROFILE_LIST";

    public static final String GOOGLE_MFA_DELETE_ACCOUNT = "GOOGLE_MFA_DELETE_ACCOUNT";

    public static final String GOOGLE_MFA_CREATE_ACCOUNT = "GOOGLE_MFA_CREATE_ACCOUNT";

    public static final String GOOGLE_MFA_UPDATE_ACCOUNT = "GOOGLE_MFA_UPDATE_ACCOUNT";

    public static final String GOOGLE_MFA_READ_ACCOUNT = "GOOGLE_MFA_READ_ACCOUNT";

    public static final String GOOGLE_MFA_LIST_ACCOUNTS = "GOOGLE_MFA_LIST_ACCOUNTS";

    public static final String OIDC_JWKS_GENERATE = "OIDC_JWKS_GENERATE";

    public static final String OIDC_JWKS_READ = "OIDC_JWKS_READ";

    public static final String OIDC_JWKS_DELETE = "OIDC_JWKS_DELETE";

    public static final String U2F_DELETE_DEVICE = "U2F_DELETE_DEVICE";

    public static final String U2F_CREATE_DEVICE = "U2F_CREATE_DEVICE";

    public static final String U2F_READ_DEVICE = "U2F_READ_DEVICE";

    public static final String U2F_SEARCH_DEVICES = "U2F_SEARCH_DEVICES";

    public static final String U2F_UPDATE_DEVICE = "U2F_UPDATE_DEVICE";

    public static final String WA_CONFIG_LIST = "WA_CONFIG_LIST";

    public static final String WA_CONFIG_SET = "WA_CONFIG_SET";

    public static final String WA_CONFIG_DELETE = "WA_CONFIG_DELETE";

    public static final String WA_CONFIG_GET = "WA_CONFIG_GET";

    public static final String WA_CONFIG_PUSH = "WA_CONFIG_PUSH";

    public static final String WEBAUTHN_DELETE_DEVICE = "WEBAUTHN_DELETE_DEVICE";

    public static final String WEBAUTHN_READ_DEVICE = "WEBAUTHN_READ_DEVICE";

    public static final String WEBAUTHN_UPDATE_DEVICE = "WEBAUTHN_UPDATE_DEVICE";

    public static final String WEBAUTHN_CREATE_DEVICE = "WEBAUTHN_CREATE_DEVICE";

    public static final String WEBAUTHN_LIST_DEVICE = "WEBAUTHN_LIST_DEVICE";

    public static final String IMPERSONATION_CREATE_ACCOUNT = "IMPERSONATION_CREATE_ACCOUNT";

    public static final String IMPERSONATION_UPDATE_ACCOUNT = "IMPERSONATION_UPDATE_ACCOUNT";

    public static final String IMPERSONATION_DELETE_ACCOUNT = "IMPERSONATION_DELETE_ACCOUNT";

    public static final String IMPERSONATION_READ_ACCOUNT = "IMPERSONATION_READ_ACCOUNT";

    private static final Set<String> VALUES;

    static {
        Set<String> values = new TreeSet<>();
        for (Field field : AMEntitlement.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) && String.class.equals(field.getType())) {
                values.add(field.getName());
            }
        }
        VALUES = Collections.unmodifiableSet(values);
    }

    public static Set<String> values() {
        return VALUES;
    }

    private AMEntitlement() {
        // private constructor for static utility class
    }
}
