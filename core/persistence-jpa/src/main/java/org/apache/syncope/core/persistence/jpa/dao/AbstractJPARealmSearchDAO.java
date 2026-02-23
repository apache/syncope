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

import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.MalformedPathException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.utils.RealmUtils;
import org.apache.syncope.core.persistence.common.dao.AbstractRealmSearchDAO;
import org.apache.syncope.core.persistence.jpa.entity.JPARealm;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

public abstract class AbstractJPARealmSearchDAO extends AbstractRealmSearchDAO {

    protected record QueryInfo(RealmSearchNode node, Set<String> plainSchemas) {

    }

    protected record AttrCondQuery(Boolean addPlainSchemas, RealmSearchNode node) {

    }

    protected static int setParameter(final List<Object> parameters, final Object parameter) {
        parameters.add(parameter);
        return parameters.size();
    }

    protected static void fillWithParameters(final Query query, final List<Object> parameters) {
        for (int i = 0; i < parameters.size(); i++) {
            if (parameters.get(i) instanceof Boolean aBoolean) {
                query.setParameter(i + 1, aBoolean ? 1 : 0);
            } else {
                query.setParameter(i + 1, parameters.get(i));
            }
        }
    }

    protected final EntityManager entityManager;

    protected final RealmUtils realmUtils;

    protected AbstractJPARealmSearchDAO(
            final EntityManager entityManager,
            final PlainSchemaDAO plainSchemaDAO,
            final EntityFactory entityFactory,
            final PlainAttrValidationManager validator,
            final RealmUtils realmUtils) {

        super(plainSchemaDAO, entityFactory, validator);

        this.entityManager = entityManager;
        this.realmUtils = realmUtils;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<Realm> findByFullPath(final String fullPath) {
        if (StringUtils.isBlank(fullPath)
                || (!SyncopeConstants.ROOT_REALM.equals(fullPath)
                && !RealmDAO.PATH_PATTERN.matcher(fullPath).matches())) {

            throw new MalformedPathException(fullPath);
        }

        TypedQuery<Realm> query = entityManager.createQuery(
                "SELECT e FROM " + JPARealm.class.getSimpleName() + " e WHERE e.fullPath=:fullPath", Realm.class);
        query.setParameter("fullPath", fullPath);

        Realm result = null;
        try {
            result = query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("Realm with fullPath {} not found", fullPath, e);
        }

        return Optional.ofNullable(result);
    }

    @Override
    public List<Realm> findByName(final String name) {
        TypedQuery<Realm> query = entityManager.createQuery(
                "SELECT e FROM " + JPARealm.class.getSimpleName() + " e WHERE e.name=:name", Realm.class);
        query.setParameter("name", name);

        return query.getResultList();
    }

    @Override
    public List<Realm> findChildren(final Realm realm) {
        TypedQuery<Realm> query = entityManager.createQuery(
                "SELECT e FROM " + JPARealm.class.getSimpleName() + " e WHERE e.parent=:realm", Realm.class);
        query.setParameter("realm", realm);

        return query.getResultList();
    }

    @Override
    public List<Realm> findDescendants(final String base, final String prefix) {
        List<Object> parameters = new ArrayList<>();

        StringBuilder queryString = new StringBuilder("SELECT e FROM ").
                append(JPARealm.class.getSimpleName()).
                append(" e WHERE (e.fullPath=?").append(setParameter(parameters, base)).
                append(" OR e.fullPath LIKE ?").append(setParameter(
                parameters,
                SyncopeConstants.ROOT_REALM.equals(base) ? "/%" : base + "/%")).
                append(") ");
        if (prefix != null) {
            queryString.append("AND (e.fullPath=?").append(setParameter(parameters, prefix)).
                    append(" OR e.fullPath LIKE ?").append(setParameter(
                    parameters,
                    SyncopeConstants.ROOT_REALM.equals(prefix) ? "/%" : prefix + "/%")).
                    append(") ");
        }
        queryString.append("ORDER BY e.fullPath");

        TypedQuery<Realm> query = entityManager.createQuery(queryString.toString(), Realm.class);

        fillWithParameters(query, parameters);

        return query.getResultList();
    }

    // ------------------------------------------ //
    protected Optional<RealmSearchNode> getQueryForCustomConds(
            final SearchCond cond,
            final boolean not,
            final List<Object> parameters) {

        return Optional.empty();
    }

    protected Optional<QueryInfo> getQuery(final SearchCond cond, final List<Object> parameters) {
        if (cond == null) {
            return Optional.empty();
        }

        boolean not = cond.getType() == SearchCond.Type.NOT_LEAF;

        Optional<RealmSearchNode> node = Optional.empty();
        Set<String> plainSchemas = new HashSet<>();

        switch (cond.getType()) {
            case LEAF:
            case NOT_LEAF:
                node = cond.asLeaf(AnyCond.class).
                        map(anyCond -> getQuery(anyCond, not, parameters)).
                        or(() -> cond.asLeaf(AttrCond.class).
                        map(attrCond -> {
                            CheckResult<AttrCond> checked = check(attrCond);
                            AttrCondQuery query = getQuery(attrCond, not, checked, parameters);
                            if (query.addPlainSchemas()) {
                                plainSchemas.add(checked.schema().getKey());
                            }
                            return query.node();
                        }));

                if (node.isEmpty()) {
                    node = getQueryForCustomConds(cond, not, parameters);
                }
                break;

            case AND:
                RealmSearchNode andNode = new RealmSearchNode(RealmSearchNode.Type.AND);

                getQuery(cond.getLeft(), parameters).ifPresent(left -> {
                    andNode.add(left.node());
                    plainSchemas.addAll(left.plainSchemas());
                });

                getQuery(cond.getRight(), parameters).ifPresent(right -> {
                    andNode.add(right.node());
                    plainSchemas.addAll(right.plainSchemas());
                });

                if (!andNode.getChildren().isEmpty()) {
                    node = Optional.of(andNode);
                }
                break;

            case OR:
                RealmSearchNode orNode = new RealmSearchNode(RealmSearchNode.Type.OR);

                getQuery(cond.getLeft(), parameters).ifPresent(left -> {
                    orNode.add(left.node());
                    plainSchemas.addAll(left.plainSchemas());
                });

                getQuery(cond.getRight(), parameters).ifPresent(right -> {
                    orNode.add(right.node());
                    plainSchemas.addAll(right.plainSchemas());
                });

                if (!orNode.getChildren().isEmpty()) {
                    node = Optional.of(orNode);
                }
                break;

            default:
        }

        return node.map(n -> new QueryInfo(n, plainSchemas));
    }

    protected abstract AttrCondQuery getQuery(
            AttrCond cond,
            boolean not,
            CheckResult<AttrCond> checked,
            List<Object> parameters);

    protected RealmSearchNode getQuery(final AnyCond cond, final boolean not, final List<Object> parameters) {
        CheckResult<AnyCond> checked = check(
                cond,
                realmUtils.getField(cond.getSchema()).
                        orElseThrow(() -> new IllegalArgumentException("Invalid schema " + cond.getSchema())),
                RELATIONSHIP_FIELDS);

        return switch (checked.cond().getType()) {
            case ISNULL ->
                new RealmSearchNode.Leaf("r." + checked.cond().getSchema() + (not ? " IS NOT NULL" : " IS NULL"));

            case ISNOTNULL ->
                new RealmSearchNode.Leaf("r." + checked.cond().getSchema() + (not ? " IS NULL" : " IS NOT NULL"));

            default ->
                fillAttrQuery(
                "r." + checked.cond().getSchema(),
                checked.value(),
                checked.schema(),
                checked.cond(),
                not,
                parameters);
        };
    }

    protected RealmSearchNode.Leaf fillAttrQuery(
            final String column,
            final PlainAttrValue attrValue,
            final PlainSchema schema,
            final AttrCond cond,
            final boolean not,
            final List<Object> parameters) {

        boolean ignoreCase = AttrCond.Type.ILIKE == cond.getType() || AttrCond.Type.IEQ == cond.getType();

        String left = column;
        if (ignoreCase && (schema.getType() == AttrSchemaType.String || schema.getType() == AttrSchemaType.Enum)) {
            left = "LOWER(" + left + ')';
        }

        StringBuilder clause = new StringBuilder(left);
        switch (cond.getType()) {

            case ILIKE:
            case LIKE:
                if (schema.getType() == AttrSchemaType.String || schema.getType() == AttrSchemaType.Enum) {
                    if (not) {
                        clause.append(" NOT");
                    }
                    clause.append(" LIKE ");
                    if (ignoreCase) {
                        clause.append("LOWER(?").append(setParameter(parameters, cond.getExpression())).append(')');
                    } else {
                        clause.append('?').append(setParameter(parameters, cond.getExpression()));
                    }
                    if (this instanceof OracleJPARealmSearchDAO) {
                        clause.append(" ESCAPE '\\'");
                    }
                } else {
                    LOG.error("LIKE is only compatible with string or enum schemas");
                    return new RealmSearchNode.Leaf(ALWAYS_FALSE_CLAUSE);
                }
                break;

            case IEQ:
            case EQ:
            default:
                clause.append(not ? "<>" : "=");
                if (ignoreCase
                        && (schema.getType() == AttrSchemaType.String || schema.getType() == AttrSchemaType.Enum)) {
                    clause.append("LOWER(?").append(setParameter(parameters, attrValue.getValue())).append(')');
                } else {
                    clause.append('?').append(setParameter(parameters, attrValue.getValue()));
                }
                break;

            case GE:
                clause.append(not ? "<" : ">=");
                clause.append('?').append(setParameter(parameters, attrValue.getValue()));
                break;

            case GT:
                clause.append(not ? "<=" : ">");
                clause.append('?').append(setParameter(parameters, attrValue.getValue()));
                break;

            case LE:
                clause.append(not ? ">" : "<=");
                clause.append('?').append(setParameter(parameters, attrValue.getValue()));
                break;

            case LT:
                clause.append(not ? ">=" : "<");
                clause.append('?').append(setParameter(parameters, attrValue.getValue()));
                break;
        }

        return new RealmSearchNode.Leaf(
                cond instanceof AnyCond
                        ? clause.toString()
                        : "r.schema_id='" + schema.getKey() + "' AND " + clause);
    }

    protected void visitNode(final RealmSearchNode node, final List<String> where) {
        node.asLeaf().ifPresentOrElse(
                leaf -> where.add(leaf.getClause()),
                () -> {
                    List<String> nodeWhere = new ArrayList<>();
                    node.getChildren().forEach(child -> visitNode(child, nodeWhere));
                    String op = " " + node.getType().name() + " ";
                    where.add(nodeWhere.stream().
                            map(w -> w.contains(" AND ") || w.contains(" OR ") ? "(" + w + ")" : w).
                            collect(Collectors.joining(op)));
                });
    }

    protected String buildFrom(final Set<String> plainSchemas, final OrderBySupport obs) {
        return JPARealm.TABLE + " r";
    }

    protected String buildWhere(final Set<String> bases, final QueryInfo queryInfo, final List<Object> parameters) {
        String fullPaths = bases.stream().
                map(base -> "r.fullPath=?" + setParameter(parameters, base)
                + " OR r.fullPath LIKE ?" + setParameter(
                        parameters, SyncopeConstants.ROOT_REALM.equals(base) ? "/%" : base + "/%")).
                collect(Collectors.joining(" OR "));

        RealmSearchNode root;
        if (queryInfo.node().getType() == RealmSearchNode.Type.AND) {
            root = queryInfo.node();
        } else {
            root = new RealmSearchNode(RealmSearchNode.Type.AND);
            root.add(queryInfo.node());
        }

        List<String> where = new ArrayList<>();
        visitNode(root, where);

        return "(" + fullPaths + ')'
                + " AND (" + where.stream().
                        map(w -> w.contains(" AND ") || w.contains(" OR ") ? "(" + w + ")" : w).
                        collect(Collectors.joining(' ' + root.getType().name() + ' '))
                + ')';
    }

    @Override
    protected long doCount(final Set<String> bases, final SearchCond cond) {
        List<Object> parameters = new ArrayList<>();

        QueryInfo queryInfo = getQuery(cond, parameters).orElse(null);
        if (queryInfo == null) {
            LOG.error("Invalid search condition: {}", cond);
            return 0;
        }

        String queryString = new StringBuilder("SELECT COUNT(DISTINCT r.id)").
                append(" FROM ").append(buildFrom(queryInfo.plainSchemas(), null)).
                append(" WHERE ").append(buildWhere(bases, queryInfo, parameters)).
                toString();

        LOG.debug("Query: {}, parameters: {}", queryString, parameters);

        Query query = entityManager.createNativeQuery(queryString);
        fillWithParameters(query, parameters);

        return ((Number) query.getSingleResult()).longValue();
    }

    protected abstract void parseOrderByForPlainSchema(
            OrderBySupport obs,
            OrderBySupport.Item item,
            Sort.Order clause,
            PlainSchema schema,
            String fieldName);

    protected void parseOrderByForField(
            final OrderBySupport.Item item,
            final String fieldName,
            final Sort.Order clause) {

        item.select = "r." + fieldName;
        item.where = StringUtils.EMPTY;
        item.orderBy = "r." + fieldName + ' ' + clause.getDirection().name();
    }

    protected void parseOrderByForCustom(
            final Sort.Order clause,
            final OrderBySupport.Item item,
            final OrderBySupport obs) {

        // do nothing by default, meant for subclasses
    }

    protected OrderBySupport parseOrderBy(final List<Sort.Order> orderBy) {
        OrderBySupport obs = new OrderBySupport();

        Set<String> orderByUniquePlainSchemas = new HashSet<>();
        Set<String> orderByNonUniquePlainSchemas = new HashSet<>();
        orderBy.forEach(clause -> {
            OrderBySupport.Item item = new OrderBySupport.Item();

            parseOrderByForCustom(clause, item, obs);

            if (item.isEmpty()) {
                realmUtils.getField(clause.getProperty()).ifPresentOrElse(
                        field -> {
                            // Manage difference among external key attribute and internal JPA @Id
                            String fieldName = "key".equals(clause.getProperty()) ? "id" : clause.getProperty();

                            // Adjust field name to column name
                            if (RELATIONSHIP_FIELDS.contains(fieldName)) {
                                fieldName += "_id";
                            }

                            parseOrderByForField(item, fieldName, clause);
                        },
                        () -> {
                            plainSchemaDAO.findById(clause.getProperty()).ifPresent(schema -> {
                                if (schema.isUniqueConstraint()) {
                                    orderByUniquePlainSchemas.add(schema.getKey());
                                } else {
                                    orderByNonUniquePlainSchemas.add(schema.getKey());
                                }
                                if (orderByUniquePlainSchemas.size() > 1 || orderByNonUniquePlainSchemas.size() > 1) {
                                    throw syncopeClientException("Order by more than one attribute is not allowed; "
                                            + "remove one from " + (orderByUniquePlainSchemas.size() > 1
                                            ? orderByUniquePlainSchemas : orderByNonUniquePlainSchemas)).get();
                                }
                                parseOrderByForPlainSchema(obs, item, clause, schema, clause.getProperty());
                            });
                        });
            }

            if (item.isEmpty()) {
                LOG.warn("Cannot build any valid clause from {}", clause);
            } else {
                obs.items.add(item);
            }
        });

        return obs;
    }

    @Override
    protected List<Realm> doSearch(final Set<String> bases, final SearchCond cond, final Pageable pageable) {
        List<Object> parameters = new ArrayList<>();

        QueryInfo queryInfo = getQuery(cond, parameters).orElse(null);
        if (queryInfo == null) {
            LOG.error("Invalid search condition: {}", cond);
            return List.of();
        }

        OrderBySupport obs = parseOrderBy(pageable.getSort().toList());

        StringBuilder queryString = new StringBuilder("SELECT DISTINCT r.id");
        obs.items.forEach(item -> queryString.append(',').append(item.select));

        queryString.append(" FROM ").append(buildFrom(queryInfo.plainSchemas(), obs)).
                append(" WHERE ").append(buildWhere(bases, queryInfo, parameters)).
                toString();

        if (!obs.items.isEmpty()) {
            queryString.append(" ORDER BY ").
                    append(obs.items.stream().map(item -> item.orderBy).collect(Collectors.joining(",")));
        }

        LOG.debug("Query: {}, parameters: {}", queryString, parameters);

        Query query = entityManager.createNativeQuery(queryString.toString());

        if (pageable.isPaged()) {
            query.setFirstResult(pageable.getPageSize() * pageable.getPageNumber());
            query.setMaxResults(pageable.getPageSize());
        }

        fillWithParameters(query, parameters);

        @SuppressWarnings("unchecked")
        List<String> keys = query.getResultList().stream().
                map(key -> key instanceof Object[] array ? (String) (array)[0] : ((String) key)).
                toList();

        return keys.stream().map(k -> entityManager.find(JPARealm.class, k)).
                filter(Objects::nonNull).map(Realm.class::cast).toList();
    }
}
