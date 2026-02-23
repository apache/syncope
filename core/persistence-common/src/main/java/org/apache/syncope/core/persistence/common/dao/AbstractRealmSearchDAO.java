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
package org.apache.syncope.core.persistence.common.dao;

import java.util.List;
import java.util.Set;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

public abstract class AbstractRealmSearchDAO extends AbstractSearchDAO implements RealmSearchDAO {

    private static final Set<String> ORDER_BY_NOT_ALLOWED = Set.of(
            "serialVersionUID", "parent");

    protected static final Set<String> RELATIONSHIP_FIELDS = Set.of(
            "parent", "passwordPolicy", "accountPolicy", "authPolicy", "accessPolicy", "attrReleasePolicy",
            "ticketExpirationPolicy");

    public AbstractRealmSearchDAO(
            final PlainSchemaDAO plainSchemaDAO,
            final EntityFactory entityFactory,
            final PlainAttrValidationManager validator) {

        super(plainSchemaDAO, entityFactory, validator);
    }

    @Transactional(readOnly = true)
    @Override
    public List<Realm> findByDerAttrValue(
            final String expression,
            final String value,
            final boolean ignoreCaseMatch) {

        List<SearchCond> conditions = buildDerAttrValueConditions(expression, value, ignoreCaseMatch);

        LOG.debug("Generated search Realm conditions: {}", conditions);

        return conditions.isEmpty() ? List.of() : search(
                Set.of(SyncopeConstants.ROOT_REALM), SearchCond.and(conditions), Pageable.unpaged());
    }

    protected abstract long doCount(Set<String> bases, SearchCond cond);

    @Override
    public long count(final Set<String> bases, final SearchCond cond) {
        LOG.debug("Search condition:\n{}", cond);
        if (cond == null || !cond.isValid()) {
            LOG.error("Invalid search condition:\n{}", cond);
            return 0;
        }

        return doCount(bases, cond);
    }

    protected abstract List<Realm> doSearch(Set<String> bases, SearchCond cond, Pageable pageable);

    @Override
    public List<Realm> search(final Set<String> bases, final SearchCond cond, final Pageable pageable) {
        LOG.debug("Search condition:\n{}", cond);
        if (cond == null || !cond.isValid()) {
            LOG.error("Invalid search condition:\n{}", cond);
            return List.of();
        }

        List<Sort.Order> effectiveOrderBy;
        if (pageable.getSort().isEmpty()) {
            effectiveOrderBy = List.of(new Sort.Order(Sort.Direction.ASC, "name"));
        } else {
            effectiveOrderBy = pageable.getSort().stream().
                    filter(clause -> !ORDER_BY_NOT_ALLOWED.contains(clause.getProperty())).
                    toList();
        }

        return doSearch(
                bases,
                cond,
                pageable.isUnpaged()
                ? Pageable.unpaged(Sort.by(effectiveOrderBy))
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(effectiveOrderBy)));
    }
}
