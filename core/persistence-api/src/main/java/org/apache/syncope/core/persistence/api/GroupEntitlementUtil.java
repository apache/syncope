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
package org.apache.syncope.core.persistence.api;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import org.apache.syncope.core.persistence.api.entity.Entitlement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for manipulating entitlements.
 */
public final class GroupEntitlementUtil {

    private static final Pattern GROUP_ENTITLEMENT_NAME_PATTERN = Pattern.compile("^GROUP_([\\d])+");

    private static final Logger LOG = LoggerFactory.getLogger(GroupEntitlementUtil.class);

    public static String getEntitlementNameFromGroupKey(final Long groupKey) {
        return "GROUP_" + groupKey;
    }

    public static boolean isGroupEntitlement(final String entitlementName) {
        return GROUP_ENTITLEMENT_NAME_PATTERN.matcher(entitlementName).matches();
    }

    public static Long getGroupKey(final String entitlementName) {
        Long result = null;

        if (isGroupEntitlement(entitlementName)) {
            try {
                result = Long.valueOf(entitlementName.substring(entitlementName.indexOf('_') + 1));
            } catch (Exception e) {
                LOG.error("unable to parse {} to Long", entitlementName, e);
            }
        }

        return result;
    }

    public static Set<Long> getGroupKeys(final Set<String> entitlements) {
        Set<Long> result = new HashSet<>();

        for (String entitlement : entitlements) {
            if (isGroupEntitlement(entitlement)) {
                Long groupId = getGroupKey(entitlement);
                if (groupId != null) {
                    result.add(groupId);
                }
            }
        }

        return result;
    }

    public static Set<Long> getGroupKeys(final List<Entitlement> entitlements) {
        Set<String> names = new HashSet<>(entitlements.size());
        for (Entitlement entitlement : entitlements) {
            names.add(entitlement.getKey());
        }
        return GroupEntitlementUtil.getGroupKeys(names);
    }

    /**
     * Private default constructor, for static-only classes.
     */
    private GroupEntitlementUtil() {
    }
}
