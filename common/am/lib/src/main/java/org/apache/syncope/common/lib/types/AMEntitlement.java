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

    public static final String SRA_SESSION_LIST = "SRA_SESSION_LIST";

    public static final String SRA_SESSION_DELETE = "SRA_SESSION_DELETE";

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

    public static final String ATTR_REPO_LIST = "ATTR_REPO_LIST";

    public static final String ATTR_REPO_CREATE = "ATTR_REPO_CREATE";

    public static final String ATTR_REPO_READ = "ATTR_REPO_READ";

    public static final String ATTR_REPO_UPDATE = "ATTR_REPO_UPDATE";

    public static final String ATTR_REPO_DELETE = "ATTR_REPO_DELETE";

    public static final String SAML2_IDP_ENTITY_SET = "SAML2_IDP_ENTITY_SET";

    public static final String SAML2_IDP_ENTITY_LIST = "SAML2_IDP_ENTITY_LIST";

    public static final String SAML2_IDP_ENTITY_GET = "SAML2_IDP_ENTITY_GET";

    public static final String SAML2_SP_ENTITY_SET = "SAML2_SP_ENTITY_SET";

    public static final String SAML2_SP_ENTITY_DELETE = "SAML2_SP_ENTITY_DELETE";

    public static final String SAML2_SP_ENTITY_LIST = "SAML2_SP_ENTITY_LIST";

    public static final String SAML2_SP_ENTITY_GET = "SAML2_SP_ENTITY_GET";

    public static final String AUTH_PROFILE_DELETE = "AUTH_PROFILE_DELETE";

    public static final String AUTH_PROFILE_CREATE = "AUTH_PROFILE_CREATE";

    public static final String AUTH_PROFILE_UPDATE = "AUTH_PROFILE_UPDATE";

    public static final String AUTH_PROFILE_READ = "AUTH_PROFILE_READ";

    public static final String AUTH_PROFILE_LIST = "AUTH_PROFILE_LIST";

    public static final String OIDC_JWKS_GENERATE = "OIDC_JWKS_GENERATE";

    public static final String OIDC_JWKS_READ = "OIDC_JWKS_READ";

    public static final String OIDC_JWKS_SET = "OIDC_JWKS_SET";

    public static final String OIDC_JWKS_DELETE = "OIDC_JWKS_DELETE";

    public static final String WA_CONFIG_LIST = "WA_CONFIG_LIST";

    public static final String WA_CONFIG_SET = "WA_CONFIG_SET";

    public static final String WA_CONFIG_DELETE = "WA_CONFIG_DELETE";

    public static final String WA_CONFIG_GET = "WA_CONFIG_GET";

    public static final String WA_CONFIG_PUSH = "WA_CONFIG_PUSH";

    public static final String WA_SESSION_LIST = "WA_SESSION_LIST";

    public static final String WA_SESSION_DELETE = "WA_SESSION_DELETE";

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
