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

public class OracleJPARealmSearchDAO extends AbstractJPARealmSearchDAO {

    public OracleJPARealmSearchDAO(
            final EntityManager entityManager,
            final EntityManagerFactory entityManagerFactory,
            final PlainSchemaDAO plainSchemaDAO,
            final EntityFactory entityFactory,
            final PlainAttrValidationManager validator) {

        super(entityManager, entityManagerFactory, plainSchemaDAO, entityFactory, validator);
    }


    protected static String jsonTable(final PlainSchema schema) {
        return new StringBuilder("JSON_TABLE(e.plainAttrs, '$[*]?(@.schema == \"").
                append(schema.getKey()).append("\").").
                append(schema.isUniqueConstraint() ? "uniqueValue" : "values[*]").
                append("' COLUMNS ").
                append(schema.isUniqueConstraint() ? "uniqueValue" : "valuez").
                append(" PATH '$.").append(key(schema.getType())).append("') AS ").
                append(schema.getKey()).
                toString();
    }


    protected RealmSearchNode.Leaf filJSONAttrQuery(
            final PlainAttrValue attrValue,
            final PlainSchema schema,
            final AttrCond cond,
            final boolean not,
            final List<Object> parameters) {

        String value = Optional.ofNullable(attrValue.getDateValue()).
                map(DateTimeFormatter.ISO_OFFSET_DATE_TIME::format).
                orElseGet(cond::getExpression);
        Object typedValue = schema.getType() == AttrSchemaType.Date ? value : attrValue.getValue();

        boolean lower = (schema.getType() == AttrSchemaType.String || schema.getType() == AttrSchemaType.Enum)
                && (cond.getType() == AttrCond.Type.IEQ || cond.getType() == AttrCond.Type.ILIKE);

        StringBuilder clause = new StringBuilder(lower ? "LOWER(" : "").
                append(schema.getKey()).append('.').append(schema.isUniqueConstraint() ? "uniqueValue" : "valuez").
                append(lower ? ')' : "");

        switch (cond.getType()) {
            case LIKE:
            case ILIKE:
                if (not) {
                    clause.append(" NOT");
                }
                clause.append(" LIKE ").
                        append(lower ? "LOWER(" : "").
                        append('?').append(setParameter(parameters, value)).
                        append(lower ? ')' : "");
                if (isOracle()) {
                    clause.append(" ESCAPE '\\'");
                }
                break;

            case GE:
                clause.append(not ? "<" : ">=").
                        append('?').append(setParameter(parameters, typedValue));
                break;

            case GT:
                clause.append(not ? "<=" : ">").
                        append('?').append(setParameter(parameters, typedValue));
                break;

            case LE:
                clause.append(not ? ">" : "<=").
                        append('?').append(setParameter(parameters, typedValue));
                break;

            case LT:
                clause.append(not ? ">=" : "<").
                        append('?').append(setParameter(parameters, typedValue));
                break;

            case IEQ:
            case EQ:
            default:
                clause.append(not ? " != " : " = ").
                        append(lower ? "LOWER(" : "").
                        append('?').append(setParameter(parameters, typedValue)).
                        append(lower ? ")" : "");
                break;
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

        switch (cond.getType()) {
            case ISNOTNULL -> {
                return new AttrCondQuery(false, new RealmSearchNode.Leaf(
                    "JSON_EXISTS(e.plainAttrs, '$[*]?(@.schema == \""
                            + checked.schema().getKey() + "\")')"));
            }

            case ISNULL -> {
                return new AttrCondQuery(false, new RealmSearchNode.Leaf(
                    "NOT JSON_EXISTS(e.plainAttrs, '$[*]?(@.schema == \""
                            + checked.schema().getKey() + "\")')"));
            }

            default -> {
                RealmSearchNode.Leaf node;
                if (not && checked.schema().isMultivalue()) {
                    RealmSearchNode.Leaf notNode = filJSONAttrQuery(
                            checked.value(), checked.schema(), cond, false, parameters);
                    node = new RealmSearchNode.Leaf(
                            "id NOT IN ("
                            + "SELECT id FROM " + JPARealm.TABLE + " e, " + jsonTable(checked.schema())
                            + " WHERE " + notNode.getClause()
                            + ")");
                    return new AttrCondQuery(false, node);
                } else {
                    node = filJSONAttrQuery(checked.value(), checked.schema(), cond, not, parameters);
                }
                return new AttrCondQuery(true, node);
            }
        }
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

        getQuery(searchCond, new ArrayList<>()).ifPresent(queryInfo ->
                queryInfo.plainSchemas().forEach(schemaKey ->
                        plainSchemaDAO.findById(schemaKey).ifPresent(schema -> {
                            int whereIdx = queryString.indexOf(" WHERE ");
                            queryString.insert(whereIdx, ", " + jsonTable(schema));
                        })));

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
}
