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

    public static final String GATEWAY_ROUTE_CREATE = "GATEWAY_ROUTE_CREATE";

    public static final String GATEWAY_ROUTE_UPDATE = "GATEWAY_ROUTE_UPDATE";

    public static final String GATEWAY_ROUTE_DELETE = "GATEWAY_ROUTE_DELETE";

    public static final String GATEWAY_ROUTE_PUSH = "GATEWAY_ROUTE_PUSH";

    public static final String CLIENTAPP_READ = "CLIENTAPP_READ";

    public static final String CLIENTAPP_LIST = "CLIENTAPP_LIST";

    public static final String CLIENTAPP_CREATE = "CLIENTAPP_CREATE";

    public static final String CLIENTAPP_UPDATE = "CLIENTAPP_CREATE";

    public static final String CLIENTAPP_DELETE = "CLIENTAPP_DELETE";

    public static final String AUTH_MODULE_LIST = "AUTH_MODULE_LIST";

    public static final String AUTH_MODULE_CREATE = "AUTH_MODULE_CREATE";

    public static final String AUTH_MODULE_READ = "AUTH_MODULE_READ";

    public static final String AUTH_MODULE_UPDATE = "AUTH_MODULE_UPDATE";

    public static final String AUTH_MODULE_DELETE = "AUTH_MODULE_DELETE";

    public static final String SAML2_IDP_METADATA_CREATE = "SAML2_IDP_METADATA_CREATE";

    public static final String SAML2_IDP_METADATA_UPDATE = "SAML2_IDP_METADATA_UPDATE";

    public static final String SAML2_IDP_METADATA_READ = "SAML2_IDP_METADATA_READ";

    public static final String SAML2_SP_METADATA_CREATE = "SAML2_SP_METADATA_CREATE";

    public static final String SAML2_SP_METADATA_UPDATE = "SAML2_SP_METADATA_UPDATE";

    public static final String SAML2_SP_METADATA_READ = "SAML2_SP_METADATA_READ";

    public static final String SAML2_SP_KEYSTORE_CREATE = "SAML2_SP_KEYSTORE_CREATE";

    public static final String SAML2_SP_KEYSTORE_UPDATE = "SAML2_SP_KEYSTORE_UPDATE";

    public static final String SAML2_SP_KEYSTORE_READ = "SAML2_SP_KEYSTORE_READ";

    public static final String GOOGLE_MFA_DELETE_TOKEN = "GOOGLE_MFA_DELETE_TOKEN";

    public static final String GOOGLE_MFA_SAVE_TOKEN = "GOOGLE_MFA_SAVE_TOKEN";

    public static final String GOOGLE_MFA_READ_TOKEN = "GOOGLE_MFA_READ_TOKEN";

    public static final String GOOGLE_MFA_COUNT_TOKEN = "GOOGLE_MFA_COUNT_TOKEN";

    public static final String AUTH_PROFILE_DELETE = "AUTH_PROFILE_DELETE";
    
    public static final String AUTH_PROFILE_READ = "AUTH_PROFILE_READ";

    public static final String AUTH_PROFILE_LIST = "AUTH_PROFILE_LIST";

    public static final String GOOGLE_MFA_DELETE_ACCOUNT = "GOOGLE_MFA_DELETE_ACCOUNT";

    public static final String GOOGLE_MFA_SAVE_ACCOUNT = "GOOGLE_MFA_SAVE_ACCOUNT";

    public static final String GOOGLE_MFA_UPDATE_ACCOUNT = "GOOGLE_MFA_UPDATE_ACCOUNT";

    public static final String GOOGLE_MFA_READ_ACCOUNT = "GOOGLE_MFA_READ_ACCOUNT";

    public static final String GOOGLE_MFA_COUNT_ACCOUNTS = "GOOGLE_MFA_COUNT_ACCOUNTS";

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
