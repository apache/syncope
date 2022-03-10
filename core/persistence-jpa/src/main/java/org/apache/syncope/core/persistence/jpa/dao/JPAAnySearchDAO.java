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
package org.apache.syncope.core.persistence.jpa.dao;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.persistence.Query;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.ClientExceptionType;
import org.apache.syncope.common.rest.api.service.JAXRSService;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.provisioning.api.utils.RealmUtils;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.MembershipCond;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.ResourceCond;
import org.apache.syncope.core.persistence.api.dao.search.RoleCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AnyTypeCond;
import org.apache.syncope.core.persistence.api.dao.search.AssignableCond;
import org.apache.syncope.core.persistence.api.dao.search.DynRealmCond;
import org.apache.syncope.core.persistence.api.dao.search.MemberCond;
import org.apache.syncope.core.persistence.api.dao.search.PrivilegeCond;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipCond;
import org.apache.syncope.core.persistence.api.dao.search.RelationshipTypeCond;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Realm;

/**
 * Search engine implementation for users, groups and any objects, based on self-updating SQL views.
 */
public class JPAAnySearchDAO extends AbstractAnySearchDAO {

    public JPAAnySearchDAO(
            final RealmDAO realmDAO,
            final DynRealmDAO dynRealmDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnyObjectDAO anyObjectDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final EntityFactory entityFactory,
            final AnyUtilsFactory anyUtilsFactory) {

        super(realmDAO, dynRealmDAO, userDAO, groupDAO, anyObjectDAO, plainSchemaDAO, entityFactory, anyUtilsFactory);
    }

    protected String buildAdminRealmsFilter(
            final Set<String> realmKeys,
            final SearchSupport svs,
            final List<Object> parameters) {

        if (realmKeys.isEmpty()) {
            return "u.any_id IS NOT NULL";
        }

        String realmKeysArg = realmKeys.stream().
                map(realmKey -> "?" + setParameter(parameters, realmKey)).
                collect(Collectors.joining(","));
        return "u.any_id IN (SELECT any_id FROM " + svs.field().name
                + " WHERE realm_id IN (" + realmKeysArg + "))";
    }

    protected Triple<String, Set<String>, Set<String>> getAdminRealmsFilter(
            final Set<String> adminRealms,
            final SearchSupport svs,
            final List<Object> parameters) {

        Set<String> realmKeys = new HashSet<>();
        Set<String> dynRealmKeys = new HashSet<>();
        Set<String> groupOwners = new HashSet<>();

        adminRealms.forEach(realmPath -> {
            Optional<Pair<String, String>> goRealm = RealmUtils.parseGroupOwnerRealm(realmPath);
            if (goRealm.isPresent()) {
                groupOwners.add(goRealm.get().getRight());
            } else if (realmPath.startsWith("/")) {
                Realm realm = realmDAO.findByFullPath(realmPath);
                if (realm == null) {
                    SyncopeClientException noRealm = SyncopeClientException.build(ClientExceptionType.InvalidRealm);
                    noRealm.getElements().add("Invalid realm specified: " + realmPath);
                    throw noRealm;
                } else {
                    realmKeys.addAll(realmDAO.findDescendants(realm).stream().
                            map(Realm::getKey).collect(Collectors.toSet()));
                }
            } else {
                DynRealm dynRealm = dynRealmDAO.find(realmPath);
                if (dynRealm == null) {
                    LOG.warn("Ignoring invalid dynamic realm {}", realmPath);
                } else {
                    dynRealmKeys.add(dynRealm.getKey());
                }
            }
        });
        if (!dynRealmKeys.isEmpty()) {
            realmKeys.clear();
        }

        return Triple.of(buildAdminRealmsFilter(realmKeys, svs, parameters), dynRealmKeys, groupOwners);
    }

    SearchSupport buildSearchSupport(final AnyTypeKind kind) {
        return new SearchViewSupport(kind);
    }

    @Override
    protected int doCount(final Set<String> adminRealms, final SearchCond cond, final AnyTypeKind kind) {
        List<Object> parameters = new ArrayList<>();

        SearchSupport svs = buildSearchSupport(kind);

        Triple<String, Set<String>, Set<String>> filter = getAdminRealmsFilter(adminRealms, svs, parameters);

        // 1. get the query string from the search condition
        Pair<StringBuilder, Set<String>> queryInfo =
                getQuery(buildEffectiveCond(cond, filter.getMiddle(), filter.getRight(), kind), parameters, svs);

        StringBuilder queryString = queryInfo.getLeft();

        // 2. take into account administrative realms
        queryString.insert(0, "SELECT u.any_id FROM (");
        queryString.append(") u WHERE ").append(filter.getLeft());

        // 3. prepare the COUNT query
        queryString.insert(0, "SELECT COUNT(any_id) FROM (");
        queryString.append(") count_any_id");

        Query countQuery = entityManager().createNativeQuery(queryString.toString());
        fillWithParameters(countQuery, parameters);

        return ((Number) countQuery.getSingleResult()).intValue();
    }

    @Override
    @SuppressWarnings("unchecked")
    protected <T extends Any<?>> List<T> doSearch(
            final Set<String> adminRealms,
            final SearchCond cond,
            final int page,
            final int itemsPerPage,
            final List<OrderByClause> orderBy,
            final AnyTypeKind kind) {

        try {
            List<Object> parameters = new ArrayList<>();

            SearchSupport svs = buildSearchSupport(kind);

            Triple<String, Set<String>, Set<String>> filter = getAdminRealmsFilter(adminRealms, svs, parameters);

            // 1. get the query string from the search condition
            Pair<StringBuilder, Set<String>> queryInfo =
                    getQuery(buildEffectiveCond(cond, filter.getMiddle(), filter.getRight(), kind), parameters, svs);

            StringBuilder queryString = queryInfo.getLeft();

            LOG.debug("Query: {}, parameters: {}", queryString, parameters);

            // 2. take into account realms and ordering
            OrderBySupport obs = parseOrderBy(svs, orderBy);
            if (queryString.charAt(0) == '(') {
                queryString.insert(0, buildSelect(obs));
            } else {
                queryString.insert(0, buildSelect(obs).append('('));
                queryString.append(')');
            }
            queryString.
                    append(buildWhere(svs, obs)).
                    append(filter.getLeft()).
                    append(buildOrderBy(obs));

            LOG.debug("Query with auth and order by statements: {}, parameters: {}", queryString, parameters);

            // 3. prepare the search query
            Query query = entityManager().createNativeQuery(queryString.toString());

            // 4. page starts from 1, while setFirtResult() starts from 0
            query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));

            if (itemsPerPage >= 0) {
                query.setMaxResults(itemsPerPage);
            }

            // 5. populate the search query with parameter values
            fillWithParameters(query, parameters);

            // 6. Prepare the result (avoiding duplicates)
            return buildResult(query.getResultList(), kind);
        } catch (SyncopeClientException e) {
            throw e;
        } catch (Exception e) {
            LOG.error("While searching for {}", kind, e);
        }

        return List.of();
    }

    protected int setParameter(final List<Object> parameters, final Object parameter) {
        parameters.add(parameter);
        return parameters.size();
    }

    protected void fillWithParameters(final Query query, final List<Object> parameters) {
        for (int i = 0; i < parameters.size(); i++) {
            if (parameters.get(i) instanceof Boolean) {
                query.setParameter(i + 1, ((Boolean) parameters.get(i)) ? 1 : 0);
            } else {
                query.setParameter(i + 1, parameters.get(i));
            }
        }
    }

    protected StringBuilder buildSelect(final OrderBySupport obs) {
        StringBuilder select = new StringBuilder("SELECT DISTINCT u.any_id");

        obs.items.forEach(item -> select.append(',').append(item.select));
        select.append(" FROM ");

        return select;
    }

    protected void processOBS(
            final SearchSupport svs,
            final OrderBySupport obs,
            final StringBuilder where) {

        Set<String> attrs = obs.items.stream().
                map(item -> item.orderBy.substring(0, item.orderBy.indexOf(' '))).collect(Collectors.toSet());

        obs.views.forEach(searchView -> {
            where.append(',');

            boolean searchViewAddedToWhere = false;
            if (searchView.name.equals(svs.asSearchViewSupport().attr().name)) {
                StringBuilder attrWhere = new StringBuilder();
                StringBuilder nullAttrWhere = new StringBuilder();

                if (svs.nonMandatorySchemas || obs.nonMandatorySchemas) {
                    where.append(" (SELECT * FROM ").append(searchView.name);
                    searchViewAddedToWhere = true;

                    attrs.forEach(field -> {
                        if (attrWhere.length() == 0) {
                            attrWhere.append(" WHERE ");
                        } else {
                            attrWhere.append(" OR ");
                        }
                        attrWhere.append("schema_id='").append(field).append("'");

                        nullAttrWhere.append(" UNION SELECT any_id, ").
                                append("'").
                                append(field).
                                append("' AS schema_id, ").
                                append("null AS booleanvalue, ").
                                append("null AS datevalue, ").
                                append("null AS doublevalue, ").
                                append("null AS longvalue, ").
                                append("null AS stringvalue FROM ").append(svs.field().name).
                                append(" WHERE ").
                                append("any_id NOT IN (").
                                append("SELECT any_id FROM ").
                                append(svs.asSearchViewSupport().attr().name).append(' ').append(searchView.alias).
                                append(" WHERE ").append("schema_id='").append(field).append("')");
                    });
                    where.append(attrWhere).append(nullAttrWhere).append(')');
                }
            }
            if (!searchViewAddedToWhere) {
                where.append(searchView.name);
            }

            where.append(' ').append(searchView.alias);
        });
    }

    protected StringBuilder buildWhere(
            final SearchSupport svs,
            final OrderBySupport obs) {

        StringBuilder where = new StringBuilder(" u");
        processOBS(svs, obs, where);
        where.append(" WHERE ");

        obs.views.forEach(searchView -> where.append("u.any_id=").append(searchView.alias).append(".any_id AND "));

        obs.items.stream().
                filter(item -> StringUtils.isNotBlank(item.where)).
                forEach(item -> where.append(item.where).append(" AND "));

        return where;
    }

    protected StringBuilder buildOrderBy(final OrderBySupport obs) {
        StringBuilder orderBy = new StringBuilder();

        if (!obs.items.isEmpty()) {
            obs.items.forEach(item -> orderBy.append(item.orderBy).append(','));

            orderBy.insert(0, " ORDER BY ");
            orderBy.deleteCharAt(orderBy.length() - 1);
        }

        return orderBy;
    }

    protected String key(final AttrSchemaType schemaType) {
        String key;
        switch (schemaType) {
            case Boolean:
                key = "booleanValue";
                break;

            case Date:
                key = "dateValue";
                break;

            case Double:
                key = "doubleValue";
                break;

            case Long:
                key = "longValue";
                break;

            case Binary:
                key = "binaryValue";
                break;

            default:
                key = "stringValue";
        }

        return key;
    }

    protected void parseOrderByForPlainSchema(
            final SearchSupport svs,
            final OrderBySupport obs,
            final OrderBySupport.Item item,
            final OrderByClause clause,
            final PlainSchema schema,
            final String fieldName) {

        // keep track of involvement of non-mandatory schemas in the order by clauses
        obs.nonMandatorySchemas = !"true".equals(schema.getMandatoryCondition());

        if (schema.isUniqueConstraint()) {
            obs.views.add(svs.asSearchViewSupport().uniqueAttr());

            item.select = new StringBuilder().
                    append(svs.asSearchViewSupport().uniqueAttr().alias).append('.').
                    append(key(schema.getType())).
                    append(" AS ").append(fieldName).toString();
            item.where = new StringBuilder().
                    append(svs.asSearchViewSupport().uniqueAttr().alias).
                    append(".schema_id='").append(fieldName).append("'").toString();
            item.orderBy = fieldName + ' ' + clause.getDirection().name();
        } else {
            obs.views.add(svs.asSearchViewSupport().attr());

            item.select = new StringBuilder().
                    append(svs.asSearchViewSupport().attr().alias).append('.').append(key(schema.getType())).
                    append(" AS ").append(fieldName).toString();
            item.where = new StringBuilder().
                    append(svs.asSearchViewSupport().attr().alias).
                    append(".schema_id='").append(fieldName).append("'").toString();
            item.orderBy = fieldName + ' ' + clause.getDirection().name();
        }
    }

    protected void parseOrderByForField(
            final SearchSupport svs,
            final OrderBySupport.Item item,
            final String fieldName,
            final OrderByClause clause) {

        item.select = svs.field().alias + '.' + fieldName;
        item.where = StringUtils.EMPTY;
        item.orderBy = svs.field().alias + '.' + fieldName + ' ' + clause.getDirection().name();
    }

    protected void parseOrderByForCustom(
            final SearchSupport svs,
            final OrderByClause clause,
            final OrderBySupport.Item item,
            final OrderBySupport obs) {

        // do nothing by default, meant for subclasses
    }

    protected OrderBySupport parseOrderBy(
            final SearchSupport svs,
            final List<OrderByClause> orderBy) {

        AnyUtils anyUtils = anyUtilsFactory.getInstance(svs.anyTypeKind);

        OrderBySupport obs = new OrderBySupport();

        Set<String> orderByUniquePlainSchemas = new HashSet<>();
        Set<String> orderByNonUniquePlainSchemas = new HashSet<>();
        orderBy.forEach(clause -> {
            OrderBySupport.Item item = new OrderBySupport.Item();

            parseOrderByForCustom(svs, clause, item, obs);

            if (item.isEmpty()) {
                if (anyUtils.getField(clause.getField()) == null) {
                    PlainSchema schema = plainSchemaDAO.find(clause.getField());
                    if (schema != null) {
                        if (schema.isUniqueConstraint()) {
                            orderByUniquePlainSchemas.add(schema.getKey());
                        } else {
                            orderByNonUniquePlainSchemas.add(schema.getKey());
                        }
                        if (orderByUniquePlainSchemas.size() > 1 || orderByNonUniquePlainSchemas.size() > 1) {
                            SyncopeClientException invalidSearch =
                                    SyncopeClientException.build(ClientExceptionType.InvalidSearchParameters);
                            invalidSearch.getElements().add("Order by more than one attribute is not allowed; "
                                    + "remove one from " + (orderByUniquePlainSchemas.size() > 1
                                    ? orderByUniquePlainSchemas : orderByNonUniquePlainSchemas));
                            throw invalidSearch;
                        }
                        parseOrderByForPlainSchema(svs, obs, item, clause, schema, clause.getField());
                    }
                } else {
                    // Manage difference among external key attribute and internal JPA @Id
                    String fieldName = "key".equals(clause.getField()) ? "id" : clause.getField();

                    // Adjust field name to column name
                    if (ArrayUtils.contains(RELATIONSHIP_FIELDS, fieldName)) {
                        fieldName += "_id";
                    }

                    obs.views.add(svs.field());

                    parseOrderByForField(svs, item, fieldName, clause);
                }
            }

            if (item.isEmpty()) {
                LOG.warn("Cannot build any valid clause from {}", clause);
            } else {
                obs.items.add(item);
            }
        });

        return obs;
    }

    protected void getQueryForCustomConds(
            final SearchCond cond,
            final List<Object> parameters,
            final SearchSupport svs,
            final boolean not,
            final StringBuilder query) {

        // do nothing by default, leave it open for subclasses
    }

    protected void queryOp(
            final StringBuilder query,
            final String op,
            final Pair<StringBuilder, Set<String>> leftInfo,
            final Pair<StringBuilder, Set<String>> rightInfo) {

        String subQuery = leftInfo.getKey().toString();
        // Add extra parentheses
        subQuery = subQuery.replaceFirst("WHERE ", "WHERE (");
        query.append(subQuery).
                append(' ').append(op).append(" any_id IN ( ").append(rightInfo.getKey()).append("))");
    }

    protected Pair<StringBuilder, Set<String>> getQuery(
            final SearchCond cond, final List<Object> parameters, final SearchSupport svs) {

        boolean not = cond.getType() == SearchCond.Type.NOT_LEAF;

        StringBuilder query = new StringBuilder();
        Set<String> involvedPlainAttrs = new HashSet<>();

        switch (cond.getType()) {
            case LEAF:
            case NOT_LEAF:
                cond.getLeaf(AnyTypeCond.class).
                        filter(leaf -> AnyTypeKind.ANY_OBJECT == svs.anyTypeKind).
                        ifPresent(leaf -> query.append(getQuery(leaf, not, parameters, svs)));

                cond.getLeaf(RelationshipTypeCond.class).
                        filter(leaf -> AnyTypeKind.GROUP != svs.anyTypeKind).
                        ifPresent(leaf -> query.append(getQuery(leaf, not, parameters, svs)));

                cond.getLeaf(RelationshipCond.class).
                        filter(leaf -> AnyTypeKind.GROUP != svs.anyTypeKind).
                        ifPresent(leaf -> query.append(getQuery(leaf, not, parameters, svs)));

                cond.getLeaf(MembershipCond.class).
                        filter(leaf -> AnyTypeKind.GROUP != svs.anyTypeKind).
                        ifPresent(leaf -> query.append(getQuery(leaf, not, parameters, svs)));

                cond.getLeaf(MemberCond.class).
                        filter(leaf -> AnyTypeKind.GROUP == svs.anyTypeKind).
                        ifPresent(leaf -> query.append(getQuery(leaf, not, parameters, svs)));

                cond.getLeaf(AssignableCond.class).
                        ifPresent(leaf -> query.append(getQuery(leaf, parameters, svs)));

                cond.getLeaf(RoleCond.class).
                        filter(leaf -> AnyTypeKind.USER == svs.anyTypeKind).
                        ifPresent(leaf -> query.append(getQuery(leaf, not, parameters, svs)));

                cond.getLeaf(PrivilegeCond.class).
                        filter(leaf -> AnyTypeKind.USER == svs.anyTypeKind).
                        ifPresent(leaf -> query.append(getQuery(leaf, not, parameters, svs)));

                cond.getLeaf(DynRealmCond.class).
                        ifPresent(leaf -> query.append(getQuery(leaf, not, parameters, svs)));

                cond.getLeaf(ResourceCond.class).
                        ifPresent(leaf -> query.append(getQuery(leaf, not, parameters, svs)));

                Optional<AnyCond> anyCond = cond.getLeaf(AnyCond.class);
                if (anyCond.isPresent()) {
                    query.append(getQuery(anyCond.get(), not, parameters, svs));
                } else {
                    cond.getLeaf(AttrCond.class).ifPresent(leaf -> {
                        query.append(getQuery(leaf, not, parameters, svs));
                        try {
                            involvedPlainAttrs.add(check(leaf, svs.anyTypeKind).getLeft().getKey());
                        } catch (IllegalArgumentException e) {
                            // ignore
                        }
                    });
                }

                // allow for additional search conditions
                getQueryForCustomConds(cond, parameters, svs, not, query);
                break;

            case AND:
                Pair<StringBuilder, Set<String>> leftAndInfo = getQuery(cond.getLeft(), parameters, svs);
                involvedPlainAttrs.addAll(leftAndInfo.getRight());

                Pair<StringBuilder, Set<String>> rigthAndInfo = getQuery(cond.getRight(), parameters, svs);
                involvedPlainAttrs.addAll(rigthAndInfo.getRight());

                queryOp(query, "AND", leftAndInfo, rigthAndInfo);
                break;

            case OR:
                Pair<StringBuilder, Set<String>> leftOrInfo = getQuery(cond.getLeft(), parameters, svs);
                involvedPlainAttrs.addAll(leftOrInfo.getRight());

                Pair<StringBuilder, Set<String>> rigthOrInfo = getQuery(cond.getRight(), parameters, svs);
                involvedPlainAttrs.addAll(rigthOrInfo.getRight());

                queryOp(query, "OR", leftOrInfo, rigthOrInfo);
                break;

            default:
        }

        return Pair.of(query, involvedPlainAttrs);
    }

    protected String getQuery(
            final AnyTypeCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        StringBuilder query = new StringBuilder("SELECT DISTINCT any_id FROM ").
                append(svs.field().name).append(" WHERE type_id");

        if (not) {
            query.append("<>");
        } else {
            query.append('=');
        }

        query.append('?').append(setParameter(parameters, cond.getAnyTypeKey()));

        return query.toString();
    }

    protected String getQuery(
            final RelationshipTypeCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        StringBuilder query = new StringBuilder("SELECT DISTINCT any_id FROM ").
                append(svs.field().name).append(" WHERE ");

        if (not) {
            query.append("any_id NOT IN (");
        } else {
            query.append("any_id IN (");
        }

        query.append("SELECT any_id ").append("FROM ").
                append(svs.relationship().name).
                append(" WHERE type=?").append(setParameter(parameters, cond.getRelationshipTypeKey())).
                append(" UNION SELECT right_any_id AS any_id FROM ").
                append(svs.relationship().name).
                append(" WHERE type=?").append(setParameter(parameters, cond.getRelationshipTypeKey())).
                append(')');

        return query.toString();
    }

    protected String getQuery(
            final RelationshipCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        String rightAnyObjectKey = check(cond);

        StringBuilder query = new StringBuilder("SELECT DISTINCT any_id FROM ").
                append(svs.field().name).append(" WHERE ");

        if (not) {
            query.append("any_id NOT IN (");
        } else {
            query.append("any_id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(svs.relationship().name).append(" WHERE ").
                append("right_any_id=?").append(setParameter(parameters, rightAnyObjectKey)).
                append(')');

        return query.toString();
    }

    protected String getQuery(
            final MembershipCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        List<String> groupKeys = check(cond);

        String where = groupKeys.stream().
                map(key -> "group_id=?" + setParameter(parameters, key)).
                collect(Collectors.joining(" OR "));

        StringBuilder query = new StringBuilder("SELECT DISTINCT any_id FROM ").
                append(svs.field().name).append(" WHERE (");

        if (not) {
            query.append("any_id NOT IN (");
        } else {
            query.append("any_id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(svs.membership().name).append(" WHERE ").
                append(where).
                append(") ");

        if (not) {
            query.append("AND any_id NOT IN (");
        } else {
            query.append("OR any_id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(svs.dyngroupmembership().name).append(" WHERE ").
                append(where).
                append("))");

        return query.toString();
    }

    protected String getQuery(
            final RoleCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        StringBuilder query = new StringBuilder("SELECT DISTINCT any_id FROM ").
                append(svs.field().name).append(" WHERE (");

        if (not) {
            query.append("any_id NOT IN (");
        } else {
            query.append("any_id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(svs.role().name).append(" WHERE ").
                append("role_id=?").append(setParameter(parameters, cond.getRole())).
                append(") ");

        if (not) {
            query.append("AND any_id NOT IN (");
        } else {
            query.append("OR any_id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(svs.dynrolemembership().name).append(" WHERE ").
                append("role_id=?").append(setParameter(parameters, cond.getRole())).
                append("))");

        return query.toString();
    }

    protected String getQuery(
            final PrivilegeCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        StringBuilder query = new StringBuilder("SELECT DISTINCT any_id FROM ").
                append(svs.field().name).append(" WHERE (");

        if (not) {
            query.append("any_id NOT IN (");
        } else {
            query.append("any_id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(svs.priv().name).append(" WHERE ").
                append("privilege_id=?").append(setParameter(parameters, cond.getPrivilege())).
                append(") ");

        if (not) {
            query.append("AND any_id NOT IN (");
        } else {
            query.append("OR any_id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(svs.dynpriv().name).append(" WHERE ").
                append("privilege_id=?").append(setParameter(parameters, cond.getPrivilege())).
                append("))");

        return query.toString();
    }

    protected String getQuery(
            final DynRealmCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        StringBuilder query = new StringBuilder("SELECT DISTINCT any_id FROM ").
                append(svs.field().name).append(" WHERE (");

        if (not) {
            query.append("any_id NOT IN (");
        } else {
            query.append("any_id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(svs.dynrealmmembership().name).append(" WHERE ").
                append("dynRealm_id=?").append(setParameter(parameters, cond.getDynRealm())).
                append("))");

        return query.toString();
    }

    protected String getQuery(
            final ResourceCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        StringBuilder query = new StringBuilder("SELECT DISTINCT any_id FROM ").
                append(svs.field().name).append(" WHERE ");

        if (not) {
            query.append("any_id NOT IN (");
        } else {
            query.append("any_id IN (");
        }

        query.append("SELECT DISTINCT any_id FROM ").
                append(svs.resource().name).
                append(" WHERE resource_id=?").
                append(setParameter(parameters, cond.getResourceKey()));

        if (svs.anyTypeKind == AnyTypeKind.USER || svs.anyTypeKind == AnyTypeKind.ANY_OBJECT) {
            query.append(" UNION SELECT DISTINCT any_id FROM ").
                    append(svs.groupResource().name).
                    append(" WHERE resource_id=?").
                    append(setParameter(parameters, cond.getResourceKey()));
        }

        query.append(')');

        return query.toString();
    }

    protected String getQuery(
            final AssignableCond cond,
            final List<Object> parameters,
            final SearchSupport svs) {

        Realm realm = check(cond);

        StringBuilder query = new StringBuilder("SELECT DISTINCT any_id FROM ").
                append(svs.field().name).append(" WHERE (");
        if (cond.isFromGroup()) {
            realmDAO.findDescendants(realm).forEach(current -> {
                query.append("realm_id=?").append(setParameter(parameters, current.getKey())).append(" OR ");
            });
            query.setLength(query.length() - 4);
        } else {
            for (Realm current = realm; current.getParent() != null; current = current.getParent()) {
                query.append("realm_id=?").append(setParameter(parameters, current.getKey())).append(" OR ");
            }
            query.append("realm_id=?").append(setParameter(parameters, realmDAO.getRoot().getKey()));
        }
        query.append(')');

        return query.toString();
    }

    protected String getQuery(
            final MemberCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        String memberKey = check(cond);

        StringBuilder query = new StringBuilder("SELECT DISTINCT any_id FROM ").
                append(svs.field().name).append(" WHERE ");

        if (not) {
            query.append("any_id NOT IN (");
        } else {
            query.append("any_id IN (");
        }

        query.append("SELECT DISTINCT group_id AS any_id FROM ").
                append(new SearchSupport(AnyTypeKind.USER).membership().name).append(" WHERE (").
                append("any_id=?").append(setParameter(parameters, memberKey)).
                append(") ");

        if (not) {
            query.append("AND any_id NOT IN (");
        } else {
            query.append("OR any_id IN (");
        }

        query.append("SELECT DISTINCT group_id AS any_id FROM ").
                append(new SearchSupport(AnyTypeKind.ANY_OBJECT).membership().name).append(" WHERE ").
                append("any_id=?").append(setParameter(parameters, memberKey)).
                append("))");

        return query.toString();
    }

    protected void fillAttrQuery(
            final StringBuilder query,
            final PlainAttrValue attrValue,
            final PlainSchema schema,
            final AttrCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        // This first branch is required for handling with not conditions given on multivalue fields (SYNCOPE-1419)
        if (not && schema.isMultivalue()
                && !(cond instanceof AnyCond)
                && cond.getType() != AttrCond.Type.ISNULL && cond.getType() != AttrCond.Type.ISNOTNULL) {

            query.append("any_id NOT IN (SELECT DISTINCT any_id FROM ");
            if (schema.isUniqueConstraint()) {
                query.append(svs.asSearchViewSupport().uniqueAttr().name);
            } else {
                query.append(svs.asSearchViewSupport().attr().name);
            }
            query.append(" WHERE schema_id='").append(schema.getKey());
            fillAttrQuery(query, attrValue, schema, cond, false, parameters, svs);
            query.append(')');
        } else {
            // activate ignoreCase only for EQ and LIKE operators
            boolean ignoreCase = AttrCond.Type.ILIKE == cond.getType() || AttrCond.Type.IEQ == cond.getType();

            String column = (cond instanceof AnyCond) ? cond.getSchema() : key(schema.getType());
            if ((schema.getType() == AttrSchemaType.String || schema.getType() == AttrSchemaType.Enum) && ignoreCase) {
                column = "LOWER (" + column + ')';
            }
            if (!(cond instanceof AnyCond)) {
                column = "' AND " + column;
            }

            switch (cond.getType()) {

                case ISNULL:
                    query.append(column).append(not
                            ? " IS NOT NULL"
                            : " IS NULL");
                    break;

                case ISNOTNULL:
                    query.append(column).append(not
                            ? " IS NULL"
                            : " IS NOT NULL");
                    break;

                case ILIKE:
                case LIKE:
                    if (schema.getType() == AttrSchemaType.String || schema.getType() == AttrSchemaType.Enum) {
                        query.append(column);
                        if (not) {
                            query.append(" NOT ");
                        }
                        query.append(" LIKE ");
                        if (ignoreCase) {
                            query.append("LOWER(?").append(setParameter(parameters, cond.getExpression())).append(')');
                        } else {
                            query.append('?').append(setParameter(parameters, cond.getExpression()));
                        }
                    } else {
                        if (!(cond instanceof AnyCond)) {
                            query.append("' AND");
                        }
                        query.append(" 1=2");
                        LOG.error("LIKE is only compatible with string or enum schemas");
                    }
                    break;

                case IEQ:
                case EQ:
                    query.append(column);
                    if (not) {
                        query.append("<>");
                    } else {
                        query.append('=');
                    }
                    if ((schema.getType() == AttrSchemaType.String
                            || schema.getType() == AttrSchemaType.Enum) && ignoreCase) {
                        query.append("LOWER(?").append(setParameter(parameters, attrValue.getValue())).append(')');
                    } else {
                        query.append('?').append(setParameter(parameters, attrValue.getValue()));
                    }
                    break;

                case GE:
                    query.append(column);
                    if (not) {
                        query.append('<');
                    } else {
                        query.append(">=");
                    }
                    query.append('?').append(setParameter(parameters, attrValue.getValue()));
                    break;

                case GT:
                    query.append(column);
                    if (not) {
                        query.append("<=");
                    } else {
                        query.append('>');
                    }
                    query.append('?').append(setParameter(parameters, attrValue.getValue()));
                    break;

                case LE:
                    query.append(column);
                    if (not) {
                        query.append('>');
                    } else {
                        query.append("<=");
                    }
                    query.append('?').append(setParameter(parameters, attrValue.getValue()));
                    break;

                case LT:
                    query.append(column);
                    if (not) {
                        query.append(">=");
                    } else {
                        query.append('<');
                    }
                    query.append('?').append(setParameter(parameters, attrValue.getValue()));
                    break;

                default:
            }
        }
    }

    protected String getQuery(
            final AttrCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        Pair<PlainSchema, PlainAttrValue> checked = check(cond, svs.anyTypeKind);

        StringBuilder query = new StringBuilder("SELECT DISTINCT any_id FROM ");
        switch (cond.getType()) {
            case ISNOTNULL:
                query.append(checked.getLeft().isUniqueConstraint()
                        ? svs.asSearchViewSupport().uniqueAttr().name
                        : svs.asSearchViewSupport().attr().name).
                        append(" WHERE schema_id=").append("'").append(checked.getLeft().getKey()).append("'");
                break;

            case ISNULL:
                query.append(svs.field().name).
                        append(" WHERE any_id NOT IN ").
                        append('(').
                        append("SELECT DISTINCT any_id FROM ").
                        append(checked.getLeft().isUniqueConstraint()
                                ? svs.asSearchViewSupport().uniqueAttr().name
                                : svs.asSearchViewSupport().attr().name).
                        append(" WHERE schema_id=").append("'").append(checked.getLeft().getKey()).append("'").
                        append(')');
                break;

            default:
                if (not && !(cond instanceof AnyCond) && checked.getLeft().isMultivalue()) {
                    query.append(svs.field().name).append(" WHERE ");
                } else {
                    if (checked.getLeft().isUniqueConstraint()) {
                        query.append(svs.asSearchViewSupport().uniqueAttr().name);
                    } else {
                        query.append(svs.asSearchViewSupport().attr().name);
                    }
                    query.append(" WHERE schema_id='").append(checked.getLeft().getKey());
                }
                fillAttrQuery(query, checked.getRight(), checked.getLeft(), cond, not, parameters, svs);
        }

        return query.toString();
    }

    protected String getQuery(
            final AnyCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        if (JAXRSService.PARAM_REALM.equals(cond.getSchema())
                && !SyncopeConstants.UUID_PATTERN.matcher(cond.getExpression()).matches()) {

            Realm realm = realmDAO.findByFullPath(cond.getExpression());
            if (realm == null) {
                throw new IllegalArgumentException("Invalid Realm full path: " + cond.getExpression());
            }
            cond.setExpression(realm.getKey());
        }

        Triple<PlainSchema, PlainAttrValue, AnyCond> checked = check(cond, svs.anyTypeKind);

        StringBuilder query = new StringBuilder("SELECT DISTINCT any_id FROM ").
                append(svs.field().name).append(" WHERE ");

        fillAttrQuery(query, checked.getMiddle(), checked.getLeft(), checked.getRight(), not, parameters, svs);

        return query.toString();
    }
}
