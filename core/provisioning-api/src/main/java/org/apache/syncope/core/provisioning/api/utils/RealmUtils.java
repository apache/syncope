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
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.Predicate;

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

    private static class StartsWithPredicate implements Predicate<String> {

        private final Collection<String> targets;

        StartsWithPredicate(final Collection<String> targets) {
            this.targets = targets;
        }

        @Override
        public boolean evaluate(final String realm) {
            return IterableUtils.matchesAny(targets, new Predicate<String>() {

                @Override
                public boolean evaluate(final String target) {
                    return realm.startsWith(target);
                }
            });
        }

    }

    public static class DynRealmsPredicate implements Predicate<String> {

        @Override
        public boolean evaluate(final String realm) {
            return !realm.startsWith("/");
        }
    }

    public static Set<String> getEffective(final Set<String> allowedRealms, final String requestedRealm) {
        Set<String> allowed = RealmUtils.normalize(allowedRealms);
        Set<String> requested = new HashSet<>();
        requested.add(requestedRealm);

        Set<String> effective = new HashSet<>();
        CollectionUtils.select(requested, new StartsWithPredicate(allowed), effective);
        CollectionUtils.select(allowed, new StartsWithPredicate(requested), effective);

        // includes dynamic realms
        CollectionUtils.select(allowedRealms, new DynRealmsPredicate(), effective);

        return effective;
    }

    private RealmUtils() {
        // empty constructor for static utility class 
    }
}
