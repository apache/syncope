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
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmSearchDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.springframework.data.domain.Sort;

public class OracleJPAAnySearchDAO extends AbstractJPAAnySearchDAO {

    /**
     *
     * @param schema
     * @return JSON_TABLE(plainAttrs, '$[*]?(@.schema == "fullname").uniqueValue' \
     * COLUMNS uniqueValue PATH '$.stringValue') AS fullname
     * or JSON_TABLE(plainAttrs, '$[*]?(@.schema == "loginDate").values[*]' \
     * COLUMNS valuez PATH '$.dateValue') AS loginDate
     */
    public static String from(final PlainSchema schema) {
        return new StringBuilder("JSON_TABLE(plainAttrs, '$[*]?(@.schema == \"").append(schema.getKey()).append("\").").
                append(schema.isUniqueConstraint() ? "uniqueValue" : "values[*]").
                append("' COLUMNS ").append(schema.isUniqueConstraint() ? "uniqueValue" : "valuez").
                append(" PATH '$.").append(key(schema.getType())).append("') AS ").append(schema.getKey()).
                toString();
    }

    public OracleJPAAnySearchDAO(
            final RealmSearchDAO realmSearchDAO,
            final DynRealmDAO dynRealmDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnyObjectDAO anyObjectDAO,
            final PlainSchemaDAO schemaDAO,
            final EntityFactory entityFactory,
            final AnyUtilsFactory anyUtilsFactory,
            final PlainAttrValidationManager validator,
            final EntityManagerFactory entityManagerFactory,
            final EntityManager entityManager) {

        super(
                realmSearchDAO,
                dynRealmDAO,
                userDAO,
                groupDAO,
                anyObjectDAO,
                schemaDAO,
                entityFactory,
                anyUtilsFactory,
                validator,
                entityManagerFactory,
                entityManager);
    }

    @Override
    protected SearchSupport.SearchView defaultSV(final SearchSupport svs) {
        return svs.table();
    }

    @Override
    protected String anyId(final SearchSupport svs) {
        return defaultSV(svs).alias() + ".id";
    }

    @Override
    protected void parseOrderByForPlainSchema(
            final SearchSupport svs,
            final OrderBySupport obs,
            final OrderBySupport.Item item,
            final Sort.Order clause,
            final PlainSchema schema,
            final String fieldName) {

        // keep track of involvement of non-mandatory schemas in the order by clauses
        obs.nonMandatorySchemas = !"true".equals(schema.getMandatoryCondition());

        obs.views.add(svs.table());

        item.select = schema.getKey() + "."
                + (schema.isUniqueConstraint() ? "uniqueValue" : "valuez")
                + " AS " + schema.getKey();
        item.where = StringUtils.EMPTY;
        item.orderBy = fieldName + ' ' + clause.getDirection().name();
    }

    @Override
    protected void parseOrderByForField(
            final SearchSupport svs,
            final OrderBySupport.Item item,
            final String fieldName,
            final Sort.Order clause) {

        item.select = svs.table().alias() + '.' + fieldName;
        item.where = StringUtils.EMPTY;
        item.orderBy = svs.table().alias() + '.' + fieldName + ' ' + clause.getDirection().name();
    }

    protected AnySearchNode.Leaf filJSONAttrQuery(
            final SearchSupport.SearchView from,
            final PlainAttrValue attrValue,
            final PlainSchema schema,
            final AttrCond cond,
            final boolean not,
            final List<Object> parameters) {

        String value = Optional.ofNullable(attrValue.getDateValue()).
                map(DateTimeFormatter.ISO_OFFSET_DATE_TIME::format).
            orElseGet(cond::getExpression);

        boolean lower = (schema.getType() == AttrSchemaType.String || schema.getType() == AttrSchemaType.Enum)
                && (cond.getType() == AttrCond.Type.IEQ || cond.getType() == AttrCond.Type.ILIKE);

        StringBuilder clause = new StringBuilder(lower ? "LOWER(" : "").
                append(schema.getKey()).append('.').append(schema.isUniqueConstraint() ? "uniqueValue" : "valuez").
                append(lower ? ')' : "");

        switch (cond.getType()) {
            case LIKE:
            case ILIKE:
                if (not) {
                    clause.append("NOT ");
                }
                clause.append(" LIKE ");
                break;

            case GE:
                if (not) {
                    clause.append('<');
                } else {
                    clause.append(">=");
                }
                break;

            case GT:
                if (not) {
                    clause.append("<=");
                } else {
                    clause.append('>');
                }
                break;

            case LE:
                if (not) {
                    clause.append('>');
                } else {
                    clause.append("<=");
                }
                break;

            case LT:
                if (not) {
                    clause.append(">=");
                } else {
                    clause.append('<');
                }
                break;

            case EQ:
            case IEQ:
            default:
                if (not) {
                    clause.append('!');
                }
                clause.append('=');
        }

        clause.append(lower ? "LOWER(" : "").
                append('?').append(setParameter(parameters, value)).
                append(lower ? ")" : "");

        // workaround for Oracle DB adding explicit escaping string, to search for literal _ (underscore)
        if (cond.getType() == AttrCond.Type.ILIKE || cond.getType() == AttrCond.Type.LIKE) {
            clause.append(" ESCAPE '\\'");
        }
        return new AnySearchNode.Leaf(from, clause.toString());
    }

    @Override
    protected Pair<Boolean, AnySearchNode> getQuery(
            final AttrCond cond,
            final boolean not,
            final Pair<PlainSchema, PlainAttrValue> checked,
            final List<Object> parameters,
            final SearchSupport svs) {

        // normalize NULL / NOT NULL checks
        if (not) {
            if (cond.getType() == AttrCond.Type.ISNULL) {
                cond.setType(AttrCond.Type.ISNOTNULL);
            } else if (cond.getType() == AttrCond.Type.ISNOTNULL) {
                cond.setType(AttrCond.Type.ISNULL);
            }
        }

        switch (cond.getType()) {
            case ISNOTNULL -> {
                return Pair.of(false, new AnySearchNode.Leaf(
                        svs.table(),
                        "JSON_EXISTS(plainAttrs, '$[*]?(@.schema == \"" + checked.getLeft().getKey() + "\")')"));
            }

            case ISNULL -> {
                return Pair.of(false, new AnySearchNode.Leaf(
                        svs.table(),
                        "NOT JSON_EXISTS(plainAttrs, '$[*]?(@.schema == \"" + checked.getLeft().getKey() + "\")')"));
            }

            default -> {
                AnySearchNode.Leaf node;
                if (not && checked.getLeft().isMultivalue()) {
                    AnySearchNode.Leaf notNode = filJSONAttrQuery(
                            svs.table(),
                            checked.getRight(),
                            checked.getLeft(),
                            cond,
                            false,
                            parameters);
                    node = new AnySearchNode.Leaf(
                            notNode.getFrom(),
                            "id NOT IN ("
                            + "SELECT id FROM " + notNode.getFrom().name() + "," + from(checked.getLeft())
                            + " WHERE " + notNode.getClause().replace(notNode.getFrom().alias() + ".", "")
                            + ")");
                    return Pair.of(false, node);
                } else {
                    node = filJSONAttrQuery(
                            svs.table(),
                            checked.getRight(),
                            checked.getLeft(),
                            cond,
                            not,
                            parameters);
                }
                return Pair.of(true, node);
            }
        }
    }

    @Override
    protected void visitNode(
            final AnySearchNode node,
            final Map<SearchSupport.SearchView, Boolean> counters,
            final Set<SearchSupport.SearchView> from,
            final List<String> where,
            final SearchSupport svs) {

        counters.clear();
        super.visitNode(node, counters, from, where, svs);
    }

    @Override
    protected String buildFrom(
            final Set<SearchSupport.SearchView> from,
            final Set<String> plainSchemas,
            final OrderBySupport obs) {

        StringBuilder clause = new StringBuilder(super.buildFrom(from, plainSchemas, obs));

        plainSchemas.forEach(schema -> plainSchemaDAO.findById(schema).
                ifPresent(pschema -> clause.append(",").append(from(pschema))));

        if (obs != null) {
            obs.items.forEach(item -> {
                String schema = StringUtils.substringBefore(item.orderBy, ' ');
                if (StringUtils.isNotBlank(schema) && !plainSchemas.contains(schema)) {
                    plainSchemaDAO.findById(schema).ifPresent(
                            pschema -> clause.append(" LEFT OUTER JOIN ").append(from(pschema)).append(" ON 1=1"));
                }
            });
        }

        return clause.toString();
    }
}
