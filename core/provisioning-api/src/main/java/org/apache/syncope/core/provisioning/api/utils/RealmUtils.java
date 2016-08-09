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
package org.apache.syncope.core.provisioning.api.utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class RealmUtils {

    public static String getGroupOwnerRealm(final String realmPath, final String groupKey) {
        return realmPath + "@" + groupKey;
    }

    public static boolean normalizingAddTo(final Set<String> realms, final String newRealm) {
        boolean dontAdd = false;
        Set<String> toRemove = new HashSet<>();
        for (String realm : realms) {
            if (newRealm.startsWith(realm)) {
                dontAdd = true;
            } else if (realm.startsWith(newRealm)) {
                toRemove.add(realm);
            }
        }

        realms.removeAll(toRemove);
        if (!dontAdd) {
            realms.add(newRealm);
        }
        return !dontAdd;
    }

    public static Set<String> normalize(final Collection<String> realms) {
        Set<String> normalized = new HashSet<>();
        if (realms != null) {
            for (String realm : realms) {
                normalizingAddTo(normalized, realm);
            }
        }

        return normalized;
    }

    private RealmUtils() {
        // empty constructor for static utility class 
    }
}
