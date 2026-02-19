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
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.NoResultException;
import jakarta.persistence.Query;
import jakarta.persistence.TypedQuery;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.openjpa.jdbc.meta.MappingRepository;
import org.apache.openjpa.jdbc.sql.OracleDictionary;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.MalformedPathException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.api.utils.RealmUtils;
import org.apache.syncope.core.persistence.jpa.entity.JPARealm;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

public abstract class AbstractJPARealmSearchDAO implements RealmSearchDAO {

    protected static final Logger LOG = LoggerFactory.getLogger(RealmSearchDAO.class);

    protected static final String ALWAYS_FALSE_CLAUSE = "1=2";

    private static final Map<String, Boolean> IS_ORACLE = new ConcurrentHashMap<>();

    protected final EntityManager entityManager;

    protected final EntityManagerFactory entityManagerFactory;

    protected final PlainSchemaDAO plainSchemaDAO;

    protected final EntityFactory entityFactory;

    protected final PlainAttrValidationManager validator;

    protected final RealmUtils realmUtils;

    protected record CheckResult<C extends AttrCond>(PlainSchema schema, PlainAttrValue value, C cond) {

    }

    protected record QueryInfo(RealmSearchNode node, Set<String> plainSchemas) {

    }

    protected record AttrCondQuery(Boolean addPlainSchemas, RealmSearchNode node) {

    }

    protected static int setParameter(final List<Object> parameters, final Object parameter) {
        parameters.add(parameter);
        return parameters.size();
    }

    protected static String key(final AttrSchemaType schemaType) {
        return switch (schemaType) {
            case Boolean -> "booleanValue";
            case Date -> "dateValue";
            case Double -> "doubleValue";
            case Long -> "longValue";
            case Binary -> "binaryValue";
            default -> "stringValue";
        };
    }

    public AbstractJPARealmSearchDAO(
            final EntityManager entityManager,
            final EntityManagerFactory entityManagerFactory,
            final PlainSchemaDAO plainSchemaDAO,
            final EntityFactory entityFactory,
            final PlainAttrValidationManager validator) {

        this.entityManager = entityManager;
        this.entityManagerFactory = entityManagerFactory;
        this.plainSchemaDAO = plainSchemaDAO;
        this.entityFactory = entityFactory;
        this.validator = validator;
        this.realmUtils = new RealmUtils(entityFactory);
    }

    protected String realmId() {
        return "e.id";
    }

    protected boolean isOracle() {
        return IS_ORACLE.computeIfAbsent(
                AuthContextUtils.getDomain(),
                k -> {
                    OpenJPAEntityManagerFactorySPI emfspi = entityManagerFactory.unwrap(
                            OpenJPAEntityManagerFactorySPI.class);
                    return ((MappingRepository) emfspi.getConfiguration().
                            getMetaDataRepositoryInstance()).getDBDictionary() instanceof OracleDictionary;
                });
    }

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

    protected AttrCondQuery getQuery(
            final AttrCond cond,
            final boolean not,
            final CheckResult<AttrCond> checked,
            final List<Object> parameters) {

        // normalize NULL / NOT NULL checks when negated
        if (not) {
            if (cond.getType() == AttrCond.Type.ISNULL) {
                cond.setType(AttrCond.Type.ISNOTNULL);
            } else if (cond.getType() == AttrCond.Type.ISNOTNULL) {
                cond.setType(AttrCond.Type.ISNULL);
            }
        }

        switch (cond.getType()) {
            case ISNOTNULL -> {
                return new AttrCondQuery(true,
                        new RealmSearchNode.Leaf("r.schema_id='" + checked.schema().getKey() + "'"));
            }

            case ISNULL -> {
                String clause = new StringBuilder(realmId()).
                        append(" NOT IN (").
                        append("SELECT DISTINCT realm_id FROM ").
                        append(JPARealm.TABLE).
                        append(" WHERE schema_id='").append(checked.schema().getKey()).append("'").
                        append(')').toString();
                return new AttrCondQuery(true, new RealmSearchNode.Leaf(clause));
            }

            default -> {
                RealmSearchNode.Leaf node;
                if (not && checked.schema().isMultivalue()) {
                    // for negated multivalue schemas, exclude realms that match positively
                    RealmSearchNode.Leaf notNode = fillAttrQuery(
                            "r." + key(checked.schema().getType()),
                            checked.value(),
                            checked.schema(),
                            cond,
                            false,
                            parameters);
                    node = new RealmSearchNode.Leaf(
                            realmId() + " NOT IN ("
                            + "SELECT realm_id FROM " + JPARealm.TABLE
                            + " WHERE " + notNode.getClause().replace("r.", "")
                            + ")");
                } else {
                    node = fillAttrQuery(
                            "r." + key(checked.schema().getType()),
                            checked.value(),
                            checked.schema(),
                            cond,
                            not,
                            parameters);
                }
                return new AttrCondQuery(true, node);
            }
        }
    }

    protected RealmSearchNode getQuery(final AnyCond cond, final boolean not, final List<Object> parameters) {

        CheckResult<AnyCond> checked = check(cond);

        return switch (checked.cond().getType()) {
            case ISNULL ->
                new RealmSearchNode.Leaf("e." + checked.cond().getSchema() + (not ? " IS NOT NULL" : " IS NULL"));

            case ISNOTNULL ->
                new RealmSearchNode.Leaf("e." + checked.cond().getSchema() + (not ? " IS NULL" : " IS NOT NULL"));

            default ->
                fillAttrQuery(
                        "e." + checked.cond().getSchema(),
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
                    if (isOracle()) {
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

    protected CheckResult<AttrCond> check(final AttrCond cond) {
        PlainSchema schema = plainSchemaDAO.findById(cond.getSchema()).
                orElseThrow(() -> new IllegalArgumentException("Invalid schema " + cond.getSchema()));

        PlainAttrValue attrValue = new PlainAttrValue();

        if (AttrSchemaType.Encrypted == schema.getType()) {
            throw new IllegalArgumentException("Cannot search by encrypted schema " + cond.getSchema());
        }

        try {
            if (cond.getType() != AttrCond.Type.LIKE
                    && cond.getType() != AttrCond.Type.ILIKE
                    && cond.getType() != AttrCond.Type.ISNULL
                    && cond.getType() != AttrCond.Type.ISNOTNULL) {

                validator.validate(schema, cond.getExpression(), attrValue);
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Could not validate expression " + cond.getExpression(), e);
        }

        return new CheckResult<>(schema, attrValue, cond);
    }

    protected CheckResult<AnyCond> check(final AnyCond cond) {
        AnyCond computed = new AnyCond(cond.getType());
        computed.setSchema(cond.getSchema());
        computed.setExpression(cond.getExpression());

        Field realmField = realmUtils.getField(computed.getSchema()).
                orElseThrow(() -> new IllegalArgumentException("Invalid schema " + computed.getSchema()));

        if ("key".equals(computed.getSchema())) {
            computed.setSchema("id");
        }

        PlainSchema schema = entityFactory.newEntity(PlainSchema.class);
        schema.setKey(realmField.getName());

        Class<?> fieldType = realmField.getType();
        AttrSchemaType schemaType = null;
        for (AttrSchemaType attrSchemaType : AttrSchemaType.values()) {
            if (fieldType.isAssignableFrom(attrSchemaType.getType())) {
                schemaType = attrSchemaType;
                break;
            }
        }

        schema.setType(schemaType == null || schemaType == AttrSchemaType.Dropdown
                ? AttrSchemaType.String : schemaType);

        PlainAttrValue attrValue = new PlainAttrValue();

        if (computed.getType() != AttrCond.Type.ISNULL && computed.getType() != AttrCond.Type.ISNOTNULL) {
            try {
                validator.validate(schema, computed.getExpression(), attrValue);
            } catch (Exception e) {
                throw new IllegalArgumentException("Could not validate expression " + computed.getExpression(), e);
            }
        }

        return new CheckResult<>(schema, attrValue, computed);
    }

    protected void visitNode(final RealmSearchNode node, final List<String> where) {
        node.asLeaf().ifPresentOrElse(
                leaf -> {
                    String clause = leaf.getClause();
                    if (clause.contains("e.")) {
                        where.add(clause);
                    } else {
                        where.add(realmId() + " IN ("
                                + "SELECT r.realm_id FROM " + JPARealm.TABLE + " r"
                                + " WHERE " + clause
                                + ")");
                    }
                },
                () -> {
                    List<String> nodeWhere = new ArrayList<>();
                    node.getChildren().forEach(child -> visitNode(child, nodeWhere));
                    String op = " " + node.getType().name() + " ";
                    where.add(nodeWhere.stream().
                            map(w -> w.contains(" AND ") || w.contains(" OR ") ? "(" + w + ")" : w).
                            collect(Collectors.joining(op)));
                });
    }

    protected String buildWhere(final List<String> where, final RealmSearchNode root) {
        String op = " " + root.getType().name() + " ";
        if (where.size() == 1) {
            return where.getFirst();
        }
        return where.stream().
                map(w -> w.contains(" AND ") || w.contains(" OR ") ? "(" + w + ")" : w).
                collect(Collectors.joining(op));
    }

    protected String buildPlainAttrQuery(
            final QueryInfo queryInfo,
            final List<Object> parameters,
            final List<Sort.Order> orderBy) {

        RealmSearchNode root;
        if (queryInfo.node().getType() == RealmSearchNode.Type.AND) {
            root = queryInfo.node();
        } else {
            root = new RealmSearchNode(RealmSearchNode.Type.AND);
            root.add(queryInfo.node());
        }

        List<String> where = new ArrayList<>();
        visitNode(root, where);

        String queryFragment = buildWhere(where, root);
        LOG.debug("Plain attr query fragment: {}, parameters: {}", queryFragment, parameters);

        return queryFragment;
    }

    protected StringBuilder buildDescendantsQuery(
            final Set<String> bases,
            final SearchCond searchCond,
            final List<Object> parameters) {

        String basesClause = bases.stream().
                map(base -> "e.fullPath=?" + setParameter(parameters, base)
                + " OR e.fullPath LIKE ?" + setParameter(
                        parameters, SyncopeConstants.ROOT_REALM.equals(base) ? "/%" : base + "/%")).
                collect(Collectors.joining(" OR "));

        StringBuilder queryString = new StringBuilder("SELECT e FROM ").
                append(JPARealm.class.getSimpleName()).append(" e ").
                append("WHERE (").append(basesClause).append(')');

        getQuery(searchCond, parameters).ifPresent(condition ->
                queryString.append(" AND (").
                        append(buildPlainAttrQuery(condition, parameters, List.of())).
                        append(')'));

        return queryString;
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
    public long countDescendants(final String base, final SearchCond searchCond) {
        return countDescendants(Set.of(base), searchCond);
    }

    @Override
    public long countDescendants(final Set<String> bases, final SearchCond searchCond) {
        List<Object> parameters = new ArrayList<>();

        StringBuilder queryString = buildDescendantsQuery(bases, searchCond, parameters);
        Query query = entityManager.createQuery(Strings.CS.replaceOnce(
                queryString.toString(),
                "SELECT e ",
                "SELECT COUNT(e) "));

        for (int i = 1; i <= parameters.size(); i++) {
            query.setParameter(i, parameters.get(i - 1));
        }

        return ((Number) query.getSingleResult()).longValue();
    }

    @Override
    public List<Realm> findDescendants(final String base, final SearchCond searchCond, final Pageable pageable) {
        return findDescendants(Set.of(base), searchCond, pageable);
    }

    @Override
    public List<Realm> findDescendants(final Set<String> bases, final SearchCond searchCond, final Pageable pageable) {
        List<Object> parameters = new ArrayList<>();

        StringBuilder queryString = buildDescendantsQuery(bases, searchCond, parameters);
        TypedQuery<Realm> query = entityManager.createQuery(
                queryString.append(" ORDER BY e.fullPath").toString(), Realm.class);

        for (int i = 1; i <= parameters.size(); i++) {
            query.setParameter(i, parameters.get(i - 1));
        }

        if (pageable.isPaged()) {
            query.setFirstResult(pageable.getPageSize() * pageable.getPageNumber());
            query.setMaxResults(pageable.getPageSize());
        }

        return query.getResultList();
    }

    @Override
    public List<String> findDescendants(final String base, final String prefix) {
        List<Object> parameters = new ArrayList<>();

        StringBuilder queryString = new StringBuilder("SELECT e FROM ").
                append(JPARealm.class.getSimpleName()).
                append(" e WHERE (e.fullPath=?").append(setParameter(parameters, base)).
                append(" OR e.fullPath LIKE ?").append(setParameter(
                        parameters,
                        SyncopeConstants.ROOT_REALM.equals(base) ? "/%" : base + "/%")).
                append(") AND (e.fullPath=?").append(setParameter(parameters, prefix)).
                append(" OR e.fullPath LIKE ?").append(setParameter(
                        parameters,
                        SyncopeConstants.ROOT_REALM.equals(prefix) ? "/%" : prefix + "/%")).
                append(") ORDER BY e.fullPath");
        TypedQuery<Realm> query = entityManager.createQuery(queryString.toString(), Realm.class);

        for (int i = 1; i <= parameters.size(); i++) {
            query.setParameter(i, parameters.get(i - 1));
        }

        return query.getResultList().stream().map(Realm::getKey).toList();
    }
}
