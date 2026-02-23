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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.AbstractSearchCond;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.DynRealmCond;
import org.apache.syncope.core.persistence.api.dao.search.MemberCond;
import org.apache.syncope.core.persistence.api.dao.search.MembershipCond;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

public abstract class AbstractAnySearchDAO extends AbstractSearchDAO implements AnySearchDAO {

    protected static final Logger LOG = LoggerFactory.getLogger(AnySearchDAO.class);

    private static final Set<String> ORDER_BY_NOT_ALLOWED = Set.of(
            "serialVersionUID", "password", "securityQuestion", "securityAnswer", "token", "tokenExpireTime");

    protected static final Set<String> RELATIONSHIP_FIELDS = Set.of("realm", "userOwner", "groupOwner");

    protected static SearchCond buildEffectiveCond(
            final SearchCond cond,
            final Set<String> dynRealmKeys,
            final Set<String> groupOwners,
            final AnyTypeKind kind) {

        List<SearchCond> result = new ArrayList<>();
        result.add(cond);

        List<SearchCond> dynRealmConds = dynRealmKeys.stream().map(key -> {
            DynRealmCond dynRealmCond = new DynRealmCond();
            dynRealmCond.setDynRealm(key);
            return SearchCond.of(dynRealmCond);
        }).toList();
        if (!dynRealmConds.isEmpty()) {
            result.add(SearchCond.or(dynRealmConds));
        }

        List<SearchCond> groupOwnerConds = groupOwners.stream().map(key -> {
            AbstractSearchCond asc;
            if (kind == AnyTypeKind.GROUP) {
                AnyCond anyCond = new AnyCond(AttrCond.Type.EQ);
                anyCond.setSchema("id");
                anyCond.setExpression(key);
                asc = anyCond;
            } else {
                MembershipCond membershipCond = new MembershipCond();
                membershipCond.setGroup(key);
                asc = membershipCond;
            }
            return SearchCond.of(asc);
        }).toList();
        if (!groupOwnerConds.isEmpty()) {
            result.add(SearchCond.or(groupOwnerConds));
        }

        return SearchCond.and(result);
    }

    protected final RealmSearchDAO realmSearchDAO;

    protected final DynRealmDAO dynRealmDAO;

    protected final UserDAO userDAO;

    protected final GroupDAO groupDAO;

    protected final AnyObjectDAO anyObjectDAO;

    protected final AnyUtilsFactory anyUtilsFactory;

    public AbstractAnySearchDAO(
            final RealmSearchDAO realmSearchDAO,
            final DynRealmDAO dynRealmDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnyObjectDAO anyObjectDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final EntityFactory entityFactory,
            final AnyUtilsFactory anyUtilsFactory,
            final PlainAttrValidationManager validator) {

        super(plainSchemaDAO, entityFactory, validator);
        this.realmSearchDAO = realmSearchDAO;
        this.dynRealmDAO = dynRealmDAO;
        this.userDAO = userDAO;
        this.groupDAO = groupDAO;
        this.anyObjectDAO = anyObjectDAO;
        this.anyUtilsFactory = anyUtilsFactory;
    }

    @Transactional(readOnly = true)
    @Override
    public <A extends Any> List<A> findByDerAttrValue(
            final String expression,
            final String value,
            final boolean ignoreCaseMatch,
            final AnyTypeKind anyTypeKind) {

        List<SearchCond> conditions = buildDerAttrValueConditions(expression, value, ignoreCaseMatch);

        LOG.debug("Generated search {} conditions: {}", anyTypeKind, conditions);

        return conditions.isEmpty() ? List.of() : search(SearchCond.and(conditions), anyTypeKind);
    }

    protected abstract long doCount(
            Realm base, boolean recursive, Set<String> adminRealms, SearchCond cond, AnyTypeKind kind);

    @Override
    public long count(
            final Realm base,
            final boolean recursive,
            final Set<String> adminRealms,
            final SearchCond cond,
            final AnyTypeKind kind) {

        if (CollectionUtils.isEmpty(adminRealms)) {
            LOG.error("No realms provided");
            return 0;
        }

        LOG.debug("Search condition:\n{}", cond);
        if (cond == null || !cond.isValid()) {
            LOG.error("Invalid search condition:\n{}", cond);
            return 0;
        }

        return doCount(base, recursive, adminRealms, cond, kind);
    }

    @Override
    public <T extends Any> List<T> search(final SearchCond cond, final AnyTypeKind kind) {
        return search(cond, List.of(), kind);
    }

    @Override
    public <T extends Any> List<T> search(
            final SearchCond cond, final List<Sort.Order> orderBy, final AnyTypeKind kind) {

        return search(
                realmSearchDAO.findByFullPath(SyncopeConstants.ROOT_REALM).orElse(null),
                true,
                SyncopeConstants.FULL_ADMIN_REALMS,
                cond,
                Pageable.unpaged(Sort.by(orderBy)),
                kind);
    }

    protected abstract <T extends Any> List<T> doSearch(
            Realm base,
            boolean recursive,
            Set<String> adminRealms,
            SearchCond searchCondition,
            Pageable pageable,
            AnyTypeKind kind);

    protected boolean isPatternMatch(final String clause) {
        return clause.indexOf('%') != -1;
    }

    protected List<String> check(final MembershipCond cond) {
        List<String> groups = SyncopeConstants.UUID_PATTERN.matcher(cond.getGroup()).matches()
                ? List.of(cond.getGroup())
                : isPatternMatch(cond.getGroup())
                ? groupDAO.findKeysByNamePattern(cond.getGroup().toLowerCase())
                : groupDAO.findKey(cond.getGroup()).map(List::of).orElseGet(List::of);

        if (groups.isEmpty()) {
            throw new IllegalArgumentException("Could not find group(s) for " + cond.getGroup());
        }

        return groups;
    }

    protected Set<String> check(final RelationshipCond cond) {
        Set<String> rightAnyObjects = cond.getAnyObject() == null
                ? Set.of()
                : SyncopeConstants.UUID_PATTERN.matcher(cond.getAnyObject()).matches()
                ? Set.of(cond.getAnyObject())
                : anyObjectDAO.findByName(cond.getAnyObject()).stream().
                        map(AnyObject::getKey).collect(Collectors.toSet());

        if (rightAnyObjects.isEmpty()) {
            throw new IllegalArgumentException("Could not find any object for " + cond.getAnyObject());
        }

        return rightAnyObjects;
    }

    protected Set<String> check(final MemberCond cond) {
        Set<String> members = cond.getMember() == null
                ? Set.of()
                : SyncopeConstants.UUID_PATTERN.matcher(cond.getMember()).matches()
                ? Set.of(cond.getMember())
                : userDAO.findKey(cond.getMember()).map(Set::of).
                        orElseGet(() -> anyObjectDAO.findByName(cond.getMember()).stream().
                        map(AnyObject::getKey).collect(Collectors.toSet()));

        if (members.isEmpty()) {
            throw new IllegalArgumentException("Could not find user or any object for " + cond.getMember());
        }

        return members;
    }

    @SuppressWarnings("unchecked")
    protected <T extends Any> List<T> buildResult(final List<Object> raw, final AnyTypeKind kind) {
        List<String> keys = raw.stream().
                map(key -> key instanceof Object[] array ? (String) (array)[0] : ((String) key)).
                toList();

        // sort anys according to keys' sorting, as their ordering is same as raw, e.g. the actual query results
        List<Any> anys = anyUtilsFactory.getInstance(kind).dao().findByKeys(keys).stream().
                sorted(Comparator.comparing(any -> keys.indexOf(any.getKey()))).toList();

        keys.stream().filter(key -> anys.stream().noneMatch(any -> key.equals(any.getKey()))).
                forEach(key -> LOG.error("Could not find {} with id {}, even if returned by native query", kind, key));

        return (List<T>) anys;
    }

    @Override
    public <T extends Any> List<T> search(
            final Realm base,
            final boolean recursive,
            final Set<String> adminRealms,
            final SearchCond cond,
            final Pageable pageable,
            final AnyTypeKind kind) {

        if (CollectionUtils.isEmpty(adminRealms)) {
            LOG.error("No realms provided");
            return List.of();
        }

        LOG.debug("Search condition:\n{}", cond);
        if (cond == null || !cond.isValid()) {
            LOG.error("Invalid search condition:\n{}", cond);
            return List.of();
        }

        List<Sort.Order> effectiveOrderBy;
        if (pageable.getSort().isEmpty()) {
            effectiveOrderBy = List.of(
                    new Sort.Order(Sort.Direction.ASC, kind == AnyTypeKind.USER ? "username" : "name"));
        } else {
            effectiveOrderBy = pageable.getSort().stream().
                    filter(clause -> !ORDER_BY_NOT_ALLOWED.contains(clause.getProperty())).
                    toList();
        }

        return doSearch(
                base,
                recursive,
                adminRealms,
                cond,
                pageable.isUnpaged()
                ? Pageable.unpaged(Sort.by(effectiveOrderBy))
                : PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(effectiveOrderBy)),
                kind);
    }
}
