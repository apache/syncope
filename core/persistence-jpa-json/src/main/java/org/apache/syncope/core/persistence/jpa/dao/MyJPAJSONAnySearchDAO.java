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

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.persistence.api.entity.JSONPlainAttr;

public class MyJPAJSONAnySearchDAO extends JPAAnySearchDAO {

    public MyJPAJSONAnySearchDAO(
            final RealmDAO realmDAO,
            final DynRealmDAO dynRealmDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnyObjectDAO anyObjectDAO,
            final PlainSchemaDAO schemaDAO,
            final EntityFactory entityFactory,
            final AnyUtilsFactory anyUtilsFactory) {

        super(realmDAO, dynRealmDAO, userDAO, groupDAO, anyObjectDAO, schemaDAO, entityFactory, anyUtilsFactory);
    }

    @Override
    protected void processOBS(
            final SearchSupport svs,
            final OrderBySupport obs,
            final StringBuilder where) {

        Set<String> attrs = obs.items.stream().
                map(item -> item.orderBy.substring(0, item.orderBy.indexOf(" "))).collect(Collectors.toSet());

        obs.views.forEach(searchView -> {
            boolean searchViewAddedToWhere = false;
            if (searchView.name.equals(svs.field().name)) {
                StringBuilder attrWhere = new StringBuilder();
                StringBuilder nullAttrWhere = new StringBuilder();

                if (svs.nonMandatorySchemas || obs.nonMandatorySchemas) {
                    where.append(", (SELECT * FROM ").append(searchView.name);
                    searchViewAddedToWhere = true;

                    attrs.forEach(field -> {
                        if (attrWhere.length() == 0) {
                            attrWhere.append(" WHERE ");
                        } else {
                            attrWhere.append(" OR ");
                        }
                        attrWhere.append("JSON_CONTAINS(plainAttrs, '[{\"schema\":\"").append(field).append("\"}]')");

                        nullAttrWhere.append(" UNION SELECT DISTINCT any_id,").append(svs.table().alias).append(".*, ").
                                append('"').append(field).append('"').append(" AS plainSchema, ").
                                append("null AS binaryValue, ").
                                append("null AS booleanValue, ").
                                append("null AS dateValue, ").
                                append("null AS doubleValue, ").
                                append("null AS longValue, ").
                                append("null AS stringValue, ").
                                append("null AS attrUniqueValue").
                                append(" FROM ").append(svs.table().name).append(' ').append(svs.table().alias).
                                append(", ").append(svs.field().name).
                                append(" WHERE any_id=").append(svs.table().alias).append(".id").
                                append(" AND any_id NOT IN ").
                                append("(SELECT distinct any_id FROM ").
                                append(svs.field().name).
                                append(" WHERE ").append(svs.table().alias).append(".id=any_id AND ").
                                append("JSON_CONTAINS(plainAttrs, '[{\"schema\":\"").append(field).append("\"}]'))");
                    });
                    where.append(attrWhere).append(nullAttrWhere).append(')');
                }
            }
            if (!searchViewAddedToWhere) {
                where.append(',').append(searchView.name);
            }

            where.append(' ').append(searchView.alias);
        });
    }

    @Override
    protected void parseOrderByForPlainSchema(
            final SearchSupport svs,
            final OrderBySupport obs,
            final OrderBySupport.Item item,
            final OrderByClause clause,
            final PlainSchema schema,
            final String fieldName) {

        // keep track of involvement of non-mandatory schemas in the order by clauses
        obs.nonMandatorySchemas = !"true".equals(schema.getMandatoryCondition());

        obs.views.add(svs.field());

        item.select = svs.field().alias + '.'
                + (schema.isUniqueConstraint() ? "attrUniqueValue" : key(schema.getType()))
                + " AS " + fieldName;
        item.where = "plainSchema = '" + fieldName + '\'';
        item.orderBy = fieldName + ' ' + clause.getDirection().name();
    }

    protected void fillAttrQuery(
            final AnyUtils anyUtils,
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

            query.append("id NOT IN (SELECT DISTINCT any_id FROM ");
            query.append(svs.field().name).append(" WHERE ");
            fillAttrQuery(anyUtils, query, attrValue, schema, cond, false, parameters, svs);
            query.append(')');
        } else {
            if (!not && cond.getType() == AttrCond.Type.EQ) {
                PlainAttr<?> container = anyUtils.newPlainAttr();
                container.setSchema(schema);
                if (attrValue instanceof PlainAttrUniqueValue) {
                    container.setUniqueValue((PlainAttrUniqueValue) attrValue);
                } else {
                    ((JSONPlainAttr) container).add(attrValue);
                }

                query.append("JSON_CONTAINS(plainAttrs, '").
                        append(POJOHelper.serialize(List.of(container)).replace("'", "''")).
                        append("')");
            } else {
                String key = key(schema.getType());

                String value = Optional.ofNullable(attrValue.getDateValue()).
                        map(DateTimeFormatter.ISO_OFFSET_DATE_TIME::format).
                        orElse(cond.getExpression());

                boolean lower = (schema.getType() == AttrSchemaType.String || schema.getType() == AttrSchemaType.Enum)
                        && (cond.getType() == AttrCond.Type.IEQ || cond.getType() == AttrCond.Type.ILIKE);

                query.append("plainSchema=?").append(setParameter(parameters, cond.getSchema())).
                        append(" AND ").
                        append(lower ? "LOWER(" : "").
                        append(schema.isUniqueConstraint()
                                ? "attrUniqueValue ->> '$." + key + '\''
                                : key).
                        append(lower ? ')' : "");

                switch (cond.getType()) {
                    case LIKE:
                    case ILIKE:
                        if (not) {
                            query.append("NOT ");
                        }
                        query.append(" LIKE ");
                        break;

                    case GE:
                        if (not) {
                            query.append('<');
                        } else {
                            query.append(">=");
                        }
                        break;

                    case GT:
                        if (not) {
                            query.append("<=");
                        } else {
                            query.append('>');
                        }
                        break;

                    case LE:
                        if (not) {
                            query.append('>');
                        } else {
                            query.append("<=");
                        }
                        break;

                    case LT:
                        if (not) {
                            query.append(">=");
                        } else {
                            query.append('<');
                        }
                        break;

                    case EQ:
                    case IEQ:
                    default:
                        if (not) {
                            query.append('!');
                        }
                        query.append('=');
                }

                query.append(lower ? "LOWER(" : "").
                        append('?').append(setParameter(parameters, value)).
                        append(lower ? ")" : "");
            }
        }
    }

    @Override
    protected String getQuery(
            final AttrCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        Pair<PlainSchema, PlainAttrValue> checked = check(cond, svs.anyTypeKind);

        // normalize NULL / NOT NULL checks
        if (not) {
            if (cond.getType() == AttrCond.Type.ISNULL) {
                cond.setType(AttrCond.Type.ISNOTNULL);
            } else if (cond.getType() == AttrCond.Type.ISNOTNULL) {
                cond.setType(AttrCond.Type.ISNULL);
            }
        }

        StringBuilder query =
                new StringBuilder("SELECT DISTINCT any_id FROM ").append(svs.field().name).append(" WHERE ");
        switch (cond.getType()) {
            case ISNOTNULL:
                query.append("JSON_SEARCH(plainAttrs, 'one', '").
                        append(checked.getLeft().getKey()).
                        append("', NULL, '$[*].schema') IS NOT NULL");
                break;

            case ISNULL:
                query.append("JSON_SEARCH(plainAttrs, 'one', '").
                        append(checked.getLeft().getKey()).
                        append("', NULL, '$[*].schema') IS NULL");
                break;

            default:
                if (not && !(cond instanceof AnyCond) && checked.getLeft().isMultivalue()) {
                    query = new StringBuilder("SELECT DISTINCT id AS any_id FROM ").append(svs.table().name).
                            append(" WHERE ");
                }
                fillAttrQuery(anyUtilsFactory.getInstance(svs.anyTypeKind),
                        query, checked.getRight(), checked.getLeft(), cond, not, parameters, svs);
        }

        return query.toString();
    }
}
