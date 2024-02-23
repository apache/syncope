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
package org.apache.syncope.core.persistence.api.utils;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.SyncopeConstants;

public final class RealmUtils {

    public static String getGroupOwnerRealm(final String realmPath, final String groupKey) {
        return realmPath + '@' + groupKey;
    }

    public static Optional<Pair<String, String>> parseGroupOwnerRealm(final String input) {
        String[] split = input.split("@");
        return split == null || split.length < 2
                ? Optional.empty()
                : Optional.of(Pair.of(split[0], split[1]));
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

    public static Pair<Set<String>, Set<String>> normalize(final Collection<String> realms) {
        Set<String> normalized = new HashSet<>();
        Set<String> groupOwnership = new HashSet<>();
        if (realms != null) {
            realms.forEach(realm -> {
                if (realm.indexOf('@') == -1) {
                    normalizingAddTo(normalized, realm);
                } else {
                    groupOwnership.add(realm);
                }
            });
        }

        return Pair.of(normalized, groupOwnership);
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

    private static final Predicate<String> DYN_REALMS_PREDICATE = r -> !r.startsWith(SyncopeConstants.ROOT_REALM);

    public static Set<String> getEffective(final Set<String> allowedRealms, final String requestedRealm) {
        Pair<Set<String>, Set<String>> normalized = normalize(allowedRealms);

        Set<String> requested = Set.of(requestedRealm);

        StartsWithPredicate normalizedFilter = new StartsWithPredicate(normalized.getLeft());
        StartsWithPredicate requestedFilter = new StartsWithPredicate(requested);

        Set<String> effective = new HashSet<>();
        effective.addAll(requested.stream().filter(normalizedFilter).collect(Collectors.toSet()));
        effective.addAll(normalized.getLeft().stream().filter(requestedFilter).collect(Collectors.toSet()));

        // includes group ownership
        effective.addAll(normalized.getRight());

        // includes dynamic realms
        if (allowedRealms != null) {
            effective.addAll(allowedRealms.stream().filter(DYN_REALMS_PREDICATE).collect(Collectors.toSet()));
        }

        return effective;
    }

    private RealmUtils() {
        // empty constructor for static utility class 
    }
}
