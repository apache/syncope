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
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class RealmUtils {

    public static String getGroupOwnerRealm(final String realmPath, final String groupKey) {
        return realmPath + '@' + groupKey;
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
            realms.forEach(realm -> normalizingAddTo(normalized, realm));
        }

        return normalized;
    }

    private static class StartsWithPredicate implements Predicate<String> {

        private final Collection<String> targets;

        StartsWithPredicate(final Collection<String> targets) {
            this.targets = targets;
        }

        @Override
        public boolean test(final String realm) {
            return targets.stream().anyMatch(realm::startsWith);
        }

    }

    public static class DynRealmsPredicate implements Predicate<String> {

        @Override
        public boolean test(final String realm) {
            return !realm.startsWith("/");
        }
    }

    public static Set<String> getEffective(final Set<String> allowedRealms, final String requestedRealm) {
        Set<String> allowed = RealmUtils.normalize(allowedRealms);
        Set<String> requested = new HashSet<>();
        requested.add(requestedRealm);

        Set<String> effective = new HashSet<>();
        effective.addAll(requested.stream().filter(new StartsWithPredicate(allowed)).collect(Collectors.toSet()));
        effective.addAll(allowed.stream().filter(new StartsWithPredicate(requested)).collect(Collectors.toSet()));

        // includes dynamic realms
        if (allowedRealms != null) {
            effective.addAll(allowedRealms.stream().filter(new DynRealmsPredicate()).collect(Collectors.toSet()));
        }

        return effective;
    }

    private RealmUtils() {
        // empty constructor for static utility class 
    }
}
