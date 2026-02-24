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
package org.apache.syncope.core.persistence.neo4j.dao;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.commons.text.TextStringBuilder;
import org.apache.syncope.common.lib.SyncopeClientException;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.common.lib.types.ClientExceptionType;
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
import org.apache.syncope.core.persistence.neo4j.dao.repo.AnyRepoExt;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jRealm;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Order;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.data.util.Streamable;
import org.springframework.transaction.annotation.Transactional;

public class Neo4jRealmSearchDAO extends AbstractRealmSearchDAO {

    protected record AnyCondQuery(String query, String field) {

    }

    protected record AttrCondQuery(String query, PlainSchema schema) {

    }

    protected record QueryInfo(
            TextStringBuilder query,
            Set<String> fields,
            Set<PlainSchema> plainSchemas) {

    }

    protected static String setParameter(final Map<String, Object> parameters, final Object parameter) {
        String name = "param" + parameters.size();
        parameters.put(name, parameter);
        return name;
    }

    protected static void appendPlainAttrCond(
            final TextStringBuilder query, final PlainSchema schema, final String cond) {

        if (schema.isUniqueConstraint()) {
            query.append(schema.getKey()).append('.').append(key(schema.getType())).append(cond);
        } else {
            query.append("any(k IN ").append(schema.getKey()).
                    append(" WHERE k").append('.').append(key(schema.getType())).append(cond).
                    append(")");
        }
    }

    protected static String escapeIfString(final String value, final boolean isStr) {
        return isStr
                ? new StringBuilder().append('"').append(value).append('"').toString()
                : value;
    }

    protected static void queryOp(
            final TextStringBuilder query,
            final String op,
            final QueryInfo leftInfo,
            final QueryInfo rightInfo) {

        query.append("WHERE EXISTS { ").
                append(Strings.CS.prependIfMissing(leftInfo.query().toString(), "MATCH (n) ")).
                append(" } ").
                append(op).append(" EXISTS { ").
                append(Strings.CS.prependIfMissing(rightInfo.query().toString(), "MATCH (n) ")).
                append(" }");
    }

    protected final RealmDAO realmDAO;

    protected final RealmUtils realmUtils;

    protected final Neo4jTemplate neo4jTemplate;

    protected final Neo4jClient neo4jClient;

    public Neo4jRealmSearchDAO(
            final RealmDAO realmDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final EntityFactory entityFactory,
            final PlainAttrValidationManager validator,
            final RealmUtils realmUtils,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient) {

        super(plainSchemaDAO, entityFactory, validator);
        this.realmDAO = realmDAO;
        this.realmUtils = realmUtils;
        this.neo4jTemplate = neo4jTemplate;
        this.neo4jClient = neo4jClient;
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<Realm> findByFullPath(final String fullPath) {
        if (StringUtils.isBlank(fullPath)
                || (!SyncopeConstants.ROOT_REALM.equals(fullPath)
                && !RealmDAO.PATH_PATTERN.matcher(fullPath).matches())) {

            throw new MalformedPathException(fullPath);
        }

        return neo4jClient.query(
                "MATCH (n:" + Neo4jRealm.NODE + ") WHERE n.fullPath = $fullPath RETURN n.id").
                bindAll(Map.of("fullPath", fullPath)).fetch().one().
                flatMap(found -> realmDAO.findById(found.get("n.id").toString()).map(n -> (Realm) n));
    }

    protected List<Realm> toList(
            final Collection<Map<String, Object>> result,
            final String property) {

        return result.stream().
                map(found -> realmDAO.findById(found.get(property).toString())).
                flatMap(Optional::stream).map(n -> (Realm) n).toList();
    }

    @Override
    public List<Realm> findByName(final String name) {
        return toList(neo4jClient.query(
                "MATCH (n:" + Neo4jRealm.NODE + ") WHERE n.name = $name RETURN n.id").
                bindAll(Map.of("name", name)).fetch().all(), "n.id");
    }

    @Override
    public List<Realm> findChildren(final Realm realm) {
        return toList(neo4jClient.query(
                "MATCH (n:" + Neo4jRealm.NODE + " {id: $id})<-[r:" + Neo4jRealm.PARENT_REL + "]-(c) RETURN c.id").
                bindAll(Map.of("id", realm.getKey())).fetch().all(), "c.id");
    }

    @Override
    public List<Realm> findDescendants(final String base, final String prefix) {
        Map<String, Object> parameters = new HashMap<>();

        StringBuilder query = new StringBuilder("MATCH (n:").append(Neo4jRealm.NODE).append(") ").
                append("WHERE (").append("n.fullPath = $base OR n.fullPath =~ $like").append(')');
        parameters.put("base", base);
        parameters.put("like", SyncopeConstants.ROOT_REALM.equals(base) ? "/.*" : base + "/.*");

        if (prefix != null) {
            query.append(" AND (n.fullPath = $prefix OR n.fullPath =~ $likePrefix)");
            parameters.put("prefix", prefix);
            parameters.put("likePrefix", SyncopeConstants.ROOT_REALM.equals(prefix) ? "/.*" : prefix + "/.*");
        }

        query.append(" RETURN n.id ORDER BY n.fullPath");

        return toList(neo4jClient.query(
                query.toString()).bindAll(parameters).fetch().all(), "n.id");
    }

    protected QueryInfo getQuery(final SearchCond cond, final Map<String, Object> parameters) {
        boolean not = cond.getType() == SearchCond.Type.NOT_LEAF;

        TextStringBuilder query = new TextStringBuilder();
        Set<String> involvedFields = new HashSet<>();
        Set<PlainSchema> involvedPlainSchemas = new HashSet<>();

        switch (cond.getType()) {
            case LEAF, NOT_LEAF -> {
                cond.asLeaf(AnyCond.class).ifPresentOrElse(
                        anyCond -> {
                            AnyCondQuery anyCondQuery = getQuery(anyCond, not, parameters);
                            query.append(anyCondQuery.query());
                            Optional.ofNullable(anyCondQuery.field()).ifPresent(involvedFields::add);
                        },
                        () -> cond.asLeaf(AttrCond.class).ifPresent(leaf -> {
                            AttrCondQuery attrCondQuery = getQuery(leaf, not, parameters);
                            query.append(attrCondQuery.query());
                            involvedPlainSchemas.add(attrCondQuery.schema());
                        }));

                // allow for additional search conditions
                getQueryForCustomConds(cond, parameters, not, query);
            }
            case AND -> {
                QueryInfo leftAndInfo = getQuery(cond.getLeft(), parameters);
                involvedFields.addAll(leftAndInfo.fields());
                involvedPlainSchemas.addAll(leftAndInfo.plainSchemas());

                QueryInfo rigthAndInfo = getQuery(cond.getRight(), parameters);
                involvedFields.addAll(rigthAndInfo.fields());
                involvedPlainSchemas.addAll(rigthAndInfo.plainSchemas());

                queryOp(query, "AND", leftAndInfo, rigthAndInfo);
            }

            case OR -> {
                QueryInfo leftOrInfo = getQuery(cond.getLeft(), parameters);
                involvedFields.addAll(leftOrInfo.fields());
                involvedPlainSchemas.addAll(leftOrInfo.plainSchemas());

                QueryInfo rigthOrInfo = getQuery(cond.getRight(), parameters);
                involvedFields.addAll(rigthOrInfo.fields());
                involvedPlainSchemas.addAll(rigthOrInfo.plainSchemas());

                queryOp(query, "OR", leftOrInfo, rigthOrInfo);
            }

            default -> {
            }
        }

        return new QueryInfo(query, involvedFields, involvedPlainSchemas);
    }

    protected void wrapQuery(
            final Set<String> bases,
            final QueryInfo queryInfo,
            final Streamable<Order> orderBy,
            final Map<String, Object> parameters) {

        TextStringBuilder match = new TextStringBuilder("MATCH (n:").append(Neo4jRealm.NODE).append(") ").
                append("WITH n.id AS id");

        // take fields into account
        queryInfo.fields().remove("id");
        Stream.concat(
                queryInfo.fields().stream(),
                orderBy.stream().filter(clause -> !"id".equals(clause.getProperty())
                && realmUtils.getField(clause.getProperty()).isPresent()).map(Order::getProperty)).
                distinct().forEach(field -> match.append(", n.").append(field).append(" AS ").append(field));

        // take plain schemas into account
        Stream.concat(
                queryInfo.plainSchemas().stream(),
                orderBy.stream().map(clause -> plainSchemaDAO.findById(clause.getProperty())).
                        flatMap(Optional::stream)).distinct().forEach(schema -> {

            match.append(", apoc.convert.getJsonProperty(n, 'plainAttrs.").append(schema.getKey());
            if (schema.isUniqueConstraint()) {
                match.append("', '$.uniqueValue')");
            } else {
                match.append("', '$.values')");
            }
            match.append(" AS ").append(schema.getKey());
        });

        TextStringBuilder query = queryInfo.query();

        // take bases into account
        AtomicInteger index = new AtomicInteger(0);
        String basesClause = bases.stream().map(base -> {
            int idx = index.incrementAndGet();
            parameters.put("base" + idx, base);
            parameters.put("like" + idx, SyncopeConstants.ROOT_REALM.equals(base) ? "/.*" : base + "/.*");
            return "n.fullPath = $base" + idx + " OR n.fullPath =~ $like" + idx;
        }).collect(Collectors.joining(" OR "));
        if (query.startsWith("MATCH (n)")) {
            query.replaceFirst("MATCH (n)", match + " WHERE (EXISTS { MATCH (n)");
            query.append("} ");
        } else {
            query.replaceFirst("WHERE EXISTS", "WHERE (EXISTS");
            query.insert(0, match.append(' '));
        }
        query.append(") AND EXISTS { ").append("(n) WHERE (").append(basesClause).append(")").append(" } ");
    }

    protected AttrCondQuery getQuery(
            final AttrCond cond,
            final boolean not,
            final Map<String, Object> parameters) {

        CheckResult<AttrCond> checked = check(cond);

        TextStringBuilder query = new TextStringBuilder("MATCH (n) ");
        switch (cond.getType()) {
            case ISNOTNULL ->
                query.append("WHERE n.`plainAttrs.").append(checked.schema().getKey()).append("` IS NOT NULL");

            case ISNULL ->
                query.append("WHERE n.`plainAttrs.").append(checked.schema().getKey()).append("` IS NULL");

            default ->
                fillAttrQuery(query, checked.value(), checked.schema(), cond, not, parameters);
        }

        return new AttrCondQuery(query.toString(), checked.schema());
    }

    protected void getQueryForCustomConds(
            final SearchCond cond,
            final Map<String, Object> parameters,
            final boolean not,
            final TextStringBuilder query) {

        // do nothing by default, leave it open for subclasses
    }

    protected void fillAttrQuery(
            final TextStringBuilder query,
            final PlainAttrValue attrValue,
            final PlainSchema schema,
            final AttrCond cond,
            final boolean not,
            final Map<String, Object> parameters) {

        if (not && cond.getType() == AttrCond.Type.ISNULL) {
            cond.setType(AttrCond.Type.ISNOTNULL);
            fillAttrQuery(query, attrValue, schema, cond, true, parameters);
            return;
        }
        if (not) {
            if (schema.isUniqueConstraint()) {
                fillAttrQuery(query, attrValue, schema, cond, false, parameters);
                query.replaceFirst("WHERE", "WHERE NOT(");
                query.append(')');
            } else {
                fillAttrQuery(query, attrValue, schema, cond, false, parameters);
                query.replaceAll("any(", schema.getKey() + " IS NULL OR none(");
            }
            return;
        }

        String value = Optional.ofNullable(attrValue.getDateValue()).
                map(DateTimeFormatter.ISO_OFFSET_DATE_TIME::format).
                orElseGet(cond::getExpression);

        boolean isStr = true;
        boolean lower = false;
        if (schema.getType().isStringClass()) {
            lower = (cond.getType() == AttrCond.Type.IEQ || cond.getType() == AttrCond.Type.ILIKE);
        } else if (schema.getType() != AttrSchemaType.Date) {
            lower = false;
            try {
                switch (schema.getType()) {
                    case Long ->
                        Long.valueOf(value);

                    case Double ->
                        Double.valueOf(value);

                    case Boolean -> {
                        if (!("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value))) {
                            throw new IllegalArgumentException();
                        }
                    }

                    default -> {
                    }
                }

                isStr = false;
            } catch (Exception nfe) {
                // ignore
            }
        }

        query.append("WHERE ");

        switch (cond.getType()) {
            case ISNULL -> {
            }

            case ISNOTNULL ->
                query.append(schema.getKey()).append(" IS NOT NULL");

            case ILIKE, LIKE -> {
                if (schema.getType().isStringClass()) {
                    appendPlainAttrCond(
                            query,
                            schema,
                            " =~ \"" + (lower ? "(?i)" : "")
                            + AnyRepoExt.escapeForLikeRegex(value).replace("%", ".*") + '"');
                } else {
                    query.append(ALWAYS_FALSE_CLAUSE);
                    LOG.error("LIKE is only compatible with string or enum schemas");
                }
            }

            case IEQ, EQ -> {
                if (StringUtils.containsAny(value, AnyRepoExt.REGEX_CHARS) || lower) {
                    appendPlainAttrCond(
                            query,
                            schema,
                            " =~ \"^" + (lower ? "(?i)" : "")
                            + AnyRepoExt.escapeForLikeRegex(value).replace("%", ".*") + "$\"");
                } else {
                    appendPlainAttrCond(
                            query,
                            schema,
                            " = " + escapeIfString(value, isStr));
                }
            }

            case GE ->
                appendPlainAttrCond(
                        query,
                        schema,
                        " >= " + escapeIfString(value, isStr));

            case GT ->
                appendPlainAttrCond(
                        query,
                        schema,
                        " > " + escapeIfString(value, isStr));

            case LE ->
                appendPlainAttrCond(
                        query,
                        schema,
                        " <= " + escapeIfString(value, isStr));

            case LT ->
                appendPlainAttrCond(
                        query,
                        schema,
                        " < " + escapeIfString(value, isStr));

            default -> {
            }
        }
        // shouldn't occour: processed before
    }

    protected void fillAttrQuery(
            final TextStringBuilder query,
            final PlainAttrValue attrValue,
            final PlainSchema schema,
            final AnyCond cond,
            final boolean not,
            final Map<String, Object> parameters) {

        if (not && cond.getType() == AttrCond.Type.ISNULL) {
            cond.setType(AttrCond.Type.ISNOTNULL);
            fillAttrQuery(query, attrValue, schema, cond, true, parameters);
            return;
        }
        if (not) {
            query.append("NOT (");
            fillAttrQuery(query, attrValue, schema, cond, false, parameters);
            query.append(')');
            return;
        }
        if (not && cond.getType() == AttrCond.Type.ISNULL) {
            cond.setType(AttrCond.Type.ISNOTNULL);
            fillAttrQuery(query, attrValue, schema, cond, true, parameters);
            return;
        }

        boolean lower = schema.getType().isStringClass()
                && (cond.getType() == AttrCond.Type.IEQ || cond.getType() == AttrCond.Type.ILIKE);

        String property = "n." + cond.getSchema();
        if (lower) {
            property = "toLower (" + property + ')';
        }

        switch (cond.getType()) {

            case ISNULL ->
                query.append(property).append(" IS NULL");

            case ISNOTNULL ->
                query.append(property).append(" IS NOT NULL");

            case ILIKE, LIKE -> {
                if (schema.getType().isStringClass()) {
                    query.append(property).append(" =~ ");
                    if (lower) {
                        query.append("toLower($").
                                append(setParameter(parameters, cond.getExpression().replace("%", ".*"))).
                                append(')');
                    } else {
                        query.append('$').append(setParameter(parameters, cond.getExpression().replace("%", ".*")));
                    }
                } else {
                    query.append(' ').append(ALWAYS_FALSE_CLAUSE);
                    LOG.error("LIKE is only compatible with string or enum schemas");
                }
            }

            case IEQ, EQ -> {
                query.append(property).append('=');

                if (lower) {
                    query.append("toLower($").append(setParameter(parameters, attrValue.getValue())).append(')');
                } else {
                    query.append('$').append(setParameter(parameters, attrValue.getValue()));
                }
            }

            case GE -> {
                query.append(property);
                if (not) {
                    query.append('<');
                } else {
                    query.append(">=");
                }
                query.append('$').append(setParameter(parameters, attrValue.getValue()));
            }

            case GT -> {
                query.append(property);
                if (not) {
                    query.append("<=");
                } else {
                    query.append('>');
                }
                query.append('$').append(setParameter(parameters, attrValue.getValue()));
            }

            case LE -> {
                query.append(property);
                if (not) {
                    query.append('>');
                } else {
                    query.append("<=");
                }
                query.append('$').append(setParameter(parameters, attrValue.getValue()));
            }

            case LT -> {
                query.append(property);
                if (not) {
                    query.append(">=");
                } else {
                    query.append('<');
                }
                query.append('$').append(setParameter(parameters, attrValue.getValue()));
            }

            default -> {
            }
        }
    }

    protected AnyCondQuery getQuery(
            final AnyCond cond,
            final boolean not,
            final Map<String, Object> parameters) {

        CheckResult<AnyCond> checked = check(
                cond,
                realmUtils.getField(cond.getSchema()).
                        orElseThrow(() -> new IllegalArgumentException("Invalid schema " + cond.getSchema())),
                RELATIONSHIP_FIELDS);

        TextStringBuilder query = new TextStringBuilder("MATCH (n) WHERE ");

        fillAttrQuery(query, checked.value(), checked.schema(), checked.cond(), not, parameters);

        return new AnyCondQuery(query.toString(), checked.cond().getSchema());
    }

    @Override
    protected long doCount(final Set<String> bases, final SearchCond cond) {
        Map<String, Object> parameters = new HashMap<>();

        QueryInfo queryInfo = getQuery(cond, parameters);

        wrapQuery(bases, queryInfo, Streamable.empty(), parameters);
        TextStringBuilder query = queryInfo.query();

        query.append("RETURN COUNT(id)");

        LOG.debug("Query: {}, parameters: {}", query, parameters);

        return neo4jTemplate.count(query.toString(), parameters);
    }

    protected List<String> parseOrderBy(final Streamable<Sort.Order> orderBy) {
        List<String> clauses = new ArrayList<>();

        Set<String> orderByUniquePlainSchemas = new HashSet<>();
        Set<String> orderByNonUniquePlainSchemas = new HashSet<>();
        orderBy.forEach(clause -> {
            if (realmUtils.getField(clause.getProperty()).isPresent()) {
                clauses.add(clause.getProperty() + " " + clause.getDirection().name());
            } else {
                plainSchemaDAO.findById(clause.getProperty()).ifPresent(schema -> {
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

                    clauses.add(schema.getKey() + " " + clause.getDirection().name());
                });
            }
        });

        return clauses;
    }

    @Override
    protected List<Realm> doSearch(final Set<String> bases, final SearchCond cond, final Pageable pageable) {
        Map<String, Object> parameters = new HashMap<>();

        QueryInfo queryInfo = getQuery(cond, parameters);

        wrapQuery(bases, queryInfo, pageable.getSort(), parameters);
        TextStringBuilder query = queryInfo.query();

        List<String> orderBy = parseOrderBy(pageable.getSort());
        String orderByStmt = String.join(", ", orderBy);

        query.append("RETURN id ").
                append("ORDER BY ").append(orderByStmt);

        if (pageable.isPaged()) {
            query.append(" SKIP ").append(pageable.getPageSize() * pageable.getPageNumber()).
                    append(" LIMIT ").append(pageable.getPageSize());
        }

        LOG.debug("Query: {}, parameters: {}", query, parameters);

        return toList(neo4jClient.query(
                query.toString()).bindAll(parameters).fetch().all(), "id");
    }
}
