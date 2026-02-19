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
import jakarta.persistence.Query;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Realm;
import org.apache.syncope.core.persistence.jpa.entity.JPARealm;
import org.springframework.data.domain.Pageable;

public class PGJPARealmSearchDAO extends AbstractJPARealmSearchDAO {

    protected static final String REGEX_CHARS = "!$()*+.:<=>?[\\]^{|}-";

    protected static String escapeForLikeRegex(final String input) {
        String output = input;
        for (char toEscape : REGEX_CHARS.toCharArray()) {
            output = output.replace(String.valueOf(toEscape), "\\" + toEscape);
        }
        return output.replace("'", "''");
    }

    protected static String escapeIfString(final String value, final boolean isStr) {
        return isStr ? '"' + value.replace("'", "''") + '"' : value;
    }

    public PGJPARealmSearchDAO(
            final EntityManager entityManager,
            final EntityManagerFactory entityManagerFactory,
            final PlainSchemaDAO plainSchemaDAO,
            final EntityFactory entityFactory,
            final PlainAttrValidationManager validator) {

        super(entityManager, entityManagerFactory, plainSchemaDAO, entityFactory, validator);
    }

    protected RealmSearchNode.Leaf filJSONAttrQuery(
            final PlainAttrValue attrValue,
            final PlainSchema schema,
            final AttrCond cond,
            final boolean not) {

        String key = key(schema.getType());
        String valuesPath = schema.isUniqueConstraint() ? "uniqueValue" : "values[*]";

        String value = Optional.ofNullable(attrValue.getDateValue()).
                map(DateTimeFormatter.ISO_OFFSET_DATE_TIME::format).
                orElseGet(cond::getExpression);

        boolean isStr = true;
        boolean lower = false;
        if (schema.getType() == AttrSchemaType.String || schema.getType() == AttrSchemaType.Enum) {
            lower = cond.getType() == AttrCond.Type.IEQ || cond.getType() == AttrCond.Type.ILIKE;
        } else if (schema.getType() != AttrSchemaType.Date) {
            try {
                switch (schema.getType()) {
                    case Long -> Long.valueOf(value);
                    case Double -> Double.valueOf(value);
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
                // ignore — treat as string
            }
        }

        StringBuilder clause = new StringBuilder();
        switch (cond.getType()) {
            case ILIKE:
            case LIKE:
                if (schema.getType() == AttrSchemaType.String || schema.getType() == AttrSchemaType.Enum) {
                    clause.append("jsonb_path_exists(e.plainAttrs, '$[*] ? (@.schema == \"").
                            append(schema.getKey()).append("\").").append(valuesPath).
                            append(" ? (@ like_regex \"").
                            append(escapeForLikeRegex(value).replace("%", ".*")).append("\"").
                            append(lower ? " flag \"i\"" : "").append(")')");
                } else {
                    LOG.error("LIKE is only compatible with string or enum schemas");
                    clause.append(ALWAYS_FALSE_CLAUSE);
                }
                break;

            case IEQ:
            case EQ:
            default:
                clause.append("jsonb_path_exists(e.plainAttrs, '$[*] ? (@.schema == \"").
                        append(schema.getKey()).append("\").").append(valuesPath).
                        append(" ? (");
                if (StringUtils.containsAny(value, REGEX_CHARS) || lower) {
                    clause.append("@.").append(key).append(" like_regex \"^").
                            append(escapeForLikeRegex(value).replace("'", "''")).append("$\"");
                } else if (isStr) {
                    clause.append("@.").append(key).append(" == ").append(escapeIfString(value, true));
                } else {
                    // Some datasets can store scalar values as JSON strings; accept both representations.
                    clause.append("@.").append(key).append(" == ").append(escapeIfString(value, false)).
                            append(" || @.").append(key).append(" == ").append(escapeIfString(value, true)).
                            append(" || @.stringValue == ").append(escapeIfString(value, true));
                }
                clause.append(lower ? " flag \"i\"" : "").append(")')");
                break;

            case GE:
                clause.append("jsonb_path_exists(e.plainAttrs, '$[*] ? (@.schema == \"").
                        append(schema.getKey()).append("\").").append(valuesPath).
                        append(" ? (@.").append(key).append(" >= ").
                        append(escapeIfString(value, isStr)).append(")')");
                break;

            case GT:
                clause.append("jsonb_path_exists(e.plainAttrs, '$[*] ? (@.schema == \"").
                        append(schema.getKey()).append("\").").append(valuesPath).
                        append(" ? (@.").append(key).append(" > ").
                        append(escapeIfString(value, isStr)).append(")')");
                break;

            case LE:
                clause.append("jsonb_path_exists(e.plainAttrs, '$[*] ? (@.schema == \"").
                        append(schema.getKey()).append("\").").append(valuesPath).
                        append(" ? (@.").append(key).append(" <= ").
                        append(escapeIfString(value, isStr)).append(")')");
                break;

            case LT:
                clause.append("jsonb_path_exists(e.plainAttrs, '$[*] ? (@.schema == \"").
                        append(schema.getKey()).append("\").").append(valuesPath).
                        append(" ? (@.").append(key).append(" < ").
                        append(escapeIfString(value, isStr)).append(")')");
                break;
        }

        if (not) {
            clause.insert(0, "NOT ");
        }

        return new RealmSearchNode.Leaf(clause.toString());
    }

    @Override
    protected AttrCondQuery getQuery(
            final AttrCond cond,
            final boolean not,
            final CheckResult<AttrCond> checked,
            final List<Object> parameters) {

        if (not) {
            if (cond.getType() == AttrCond.Type.ISNULL) {
                cond.setType(AttrCond.Type.ISNOTNULL);
            } else if (cond.getType() == AttrCond.Type.ISNOTNULL) {
                cond.setType(AttrCond.Type.ISNULL);
            }
        }

        return switch (cond.getType()) {
            case ISNOTNULL ->
                new AttrCondQuery(true, new RealmSearchNode.Leaf(
                "jsonb_path_exists(e.plainAttrs, '$[*] ? (@.schema == \""
                + checked.schema().getKey() + "\")')"));

            case ISNULL ->
                new AttrCondQuery(true, new RealmSearchNode.Leaf(
                "NOT jsonb_path_exists(e.plainAttrs, '$[*] ? (@.schema == \""
                + checked.schema().getKey() + "\")')"));

            default -> {
                RealmSearchNode.Leaf node;
                if (not && checked.schema().isMultivalue()) {
                    // negate multivalue by wrapping the positive expression in NOT
                    RealmSearchNode.Leaf notNode = filJSONAttrQuery(
                            checked.value(), checked.schema(), cond, false);
                    node = new RealmSearchNode.Leaf("NOT " + notNode.getClause());
                } else {
                    node = filJSONAttrQuery(checked.value(), checked.schema(), cond, not);
                }
                yield new AttrCondQuery(true, node);
            }
        };
    }

    @Override
    protected StringBuilder buildDescendantsQuery(
            final Set<String> bases,
            final SearchCond searchCond,
            final List<Object> parameters) {

        String basesClause = bases.stream().
                map(base -> "e.fullpath=?" + setParameter(parameters, base)
                + " OR e.fullpath LIKE ?" + setParameter(
                        parameters, SyncopeConstants.ROOT_REALM.equals(base) ? "/%" : base + "/%")).
                collect(Collectors.joining(" OR "));

        StringBuilder queryString = new StringBuilder("SELECT e.* FROM ").
                append(JPARealm.TABLE).append(" e ").
                append("WHERE (").append(basesClause).append(')');

        getQuery(searchCond, parameters).ifPresent(condition ->
                queryString.append(" AND (").
                        append(buildPlainAttrQuery(condition, parameters, List.of())).
                        append(')'));

        return queryString;
    }

    @Override
    public long countDescendants(final Set<String> bases, final SearchCond searchCond) {
        List<Object> parameters = new ArrayList<>();

        StringBuilder queryString = buildDescendantsQuery(bases, searchCond, parameters);
        Query query = entityManager.createNativeQuery(Strings.CS.replaceOnce(
                queryString.toString(),
                "SELECT e.* ",
                "SELECT COUNT(e.id) "));

        for (int i = 1; i <= parameters.size(); i++) {
            query.setParameter(i, parameters.get(i - 1));
        }

        return ((Number) query.getSingleResult()).longValue();
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<Realm> findDescendants(final Set<String> bases, final SearchCond searchCond, final Pageable pageable) {
        List<Object> parameters = new ArrayList<>();

        StringBuilder queryString = buildDescendantsQuery(bases, searchCond, parameters);
        Query query = entityManager.createNativeQuery(
                queryString.append(" ORDER BY e.fullpath").toString(), JPARealm.class);

        for (int i = 1; i <= parameters.size(); i++) {
            query.setParameter(i, parameters.get(i - 1));
        }

        if (pageable.isPaged()) {
            query.setFirstResult(pageable.getPageSize() * pageable.getPageNumber());
            query.setMaxResults(pageable.getPageSize());
        }

        return (List<Realm>) query.getResultList();
    }

    @Override
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
}
