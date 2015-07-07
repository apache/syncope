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
package org.apache.syncope.core.logic;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.syncope.common.lib.mod.AnyMod;
import org.apache.syncope.common.lib.to.AnyTO;
import org.apache.syncope.core.misc.RealmUtils;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;

public abstract class AbstractAnyLogic<TO extends AnyTO, MOD extends AnyMod>
        extends AbstractResourceAssociator<TO> {

    private static class StartsWithPredicate implements Predicate<String> {

        private final Collection<String> targets;

        public StartsWithPredicate(final Collection<String> targets) {
            this.targets = targets;
        }

        @Override
        public boolean evaluate(final String realm) {
            return CollectionUtils.exists(targets, new Predicate<String>() {

                @Override
                public boolean evaluate(final String target) {
                    return realm.startsWith(target);
                }
            });
        }

    }

    protected Set<String> getEffectiveRealms(
            final Set<String> allowedRealms, final Collection<String> requestedRealms) {

        final Set<String> allowed = RealmUtils.normalize(allowedRealms);
        final Set<String> requested = RealmUtils.normalize(requestedRealms);

        Set<String> effective = new HashSet<>();
        CollectionUtils.select(requested, new StartsWithPredicate(allowed), effective);
        CollectionUtils.select(allowed, new StartsWithPredicate(requested), effective);

        return effective;
    }

    public abstract TO read(Long key);

    public abstract int count(List<String> realms);

    public abstract TO create(TO anyTO);

    public abstract TO update(MOD anyMod);

    public abstract TO delete(Long key);

    public abstract List<TO> list(
            int page, int size, List<OrderByClause> orderBy,
            List<String> realms,
            boolean details);

    public abstract List<TO> search(
            SearchCond searchCondition,
            int page, int size, List<OrderByClause> orderBy,
            List<String> realms,
            boolean details);

    public abstract int searchCount(SearchCond searchCondition, List<String> realms);
}
