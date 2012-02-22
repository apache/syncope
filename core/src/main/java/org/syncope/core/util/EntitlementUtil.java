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
package org.syncope.core.util;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.syncope.core.persistence.beans.Entitlement;

public class EntitlementUtil {

    private static final Pattern ROLE_ENTITLEMENT_NAME_PATTERN =
            Pattern.compile("^ROLE_([\\d])+");

    public static Set<String> getOwnedEntitlementNames() {
        final Set<String> result = new HashSet<String>();

        final SecurityContext ctx = SecurityContextHolder.getContext();

        if (ctx != null
                && ctx.getAuthentication() != null
                && ctx.getAuthentication().getAuthorities() != null) {

            for (GrantedAuthority authority :
                    SecurityContextHolder.getContext().
                    getAuthentication().getAuthorities()) {

                result.add(authority.getAuthority());
            }
        }

        return result;
    }

    public static String getEntitlementNameFromRoleId(final Long roleId) {
        return "ROLE_" + roleId;
    }

    public static boolean isRoleEntitlement(final String entitlementName) {
        return ROLE_ENTITLEMENT_NAME_PATTERN.matcher(entitlementName).matches();
    }

    public static Long getRoleId(final String entitlementName) {
        Long result = null;

        if (isRoleEntitlement(entitlementName)) {
            try {
                result = Long.valueOf(entitlementName.substring(
                        entitlementName.indexOf("_") + 1));
            } catch (Throwable t) {
            }
        }

        return result;
    }

    public static Set<Long> getRoleIds(final Set<String> entitlements) {
        Set<Long> result = new HashSet<Long>();

        Long roleId;
        for (String entitlement : entitlements) {
            if (isRoleEntitlement(entitlement)) {
                roleId = getRoleId(entitlement);
                if (roleId != null) {
                    result.add(roleId);
                }
            }
        }

        return result;
    }

    public static Set<Long> getRoleIds(final List<Entitlement> entitlements) {
        Set<String> names = new HashSet<String>(entitlements.size());
        for (Entitlement entitlement : entitlements) {
            names.add(entitlement.getName());
        }
        return getRoleIds(names);
    }
}
