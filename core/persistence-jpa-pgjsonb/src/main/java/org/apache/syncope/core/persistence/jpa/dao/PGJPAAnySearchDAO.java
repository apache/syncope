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

import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.types.AnyTypeKind;
import org.apache.syncope.core.persistence.api.dao.search.AttributeCond;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.jpa.entity.PGPlainAttr;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;

public class PGJPAAnySearchDAO extends JPAAnySearchDAO {

    @Override
    SearchSupport buildSearchSupport(final AnyTypeKind kind) {
        return new SearchSupport(kind);
    }

    @Override
    protected void processOBS(final SearchSupport svs, final OrderBySupport obs, final StringBuilder where) {
        obs.views.forEach(searchView -> {
            where.append(',').
                    append(searchView.name).
                    append(' ').append(searchView.alias);
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

        item.select = svs.field().alias + ".attrValues ->> '" + fieldName + "' AS " + fieldName;
        item.where = "attrs ->> 'schema' = '" + fieldName + "'";
        item.orderBy = fieldName + " " + clause.getDirection().name();
    }

    private void fillAttrQuery(
            final AnyUtils anyUtils,
            final StringBuilder query,
            final PlainAttrValue attrValue,
            final PlainSchema schema,
            final AttributeCond cond,
            final boolean not,
            final List<Object> parameters) {

        String key;
        boolean lower = false;
        switch (schema.getType()) {
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
                lower = cond.getType() == AttributeCond.Type.IEQ || cond.getType() == AttributeCond.Type.ILIKE;
                key = "stringValue";
        }

        if (!not && cond.getType() == AttributeCond.Type.EQ) {
            PlainAttr<?> container = anyUtils.newPlainAttr();
            container.setSchema(schema);
            if (attrValue instanceof PlainAttrUniqueValue) {
                container.setUniqueValue((PlainAttrUniqueValue) attrValue);
            } else {
                ((PGPlainAttr) container).add(attrValue);
            }

            query.append("plainAttrs @> '").
                    append(POJOHelper.serialize(Arrays.asList(container))).
                    append("'::jsonb");
        } else {
            query.append("attrs ->> 'schema' = ?").append(setParameter(parameters, cond.getSchema())).
                    append(" AND ").
                    append(lower ? "LOWER(" : "").
                    append(schema.isUniqueConstraint()
                            ? "attrs -> 'uniqueValue'" : "attrValues").
                    append(" ->> '").append(key).append("'").
                    append(lower ? ")" : "");

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
                    append("?").append(setParameter(parameters, cond.getExpression())).
                    append(lower ? ")" : "");
        }
    }

    @Override
    protected String getQuery(
            final AttributeCond cond,
            final boolean not,
            final List<Object> parameters,
            final SearchSupport svs) {

        Pair<PlainSchema, PlainAttrValue> checked;
        try {
            checked = check(cond, svs.anyTypeKind);
        } catch (IllegalArgumentException e) {
            return EMPTY_QUERY;
        }

        // normalize NULL / NOT NULL checks
        if (not) {
            if (cond.getType() == AttributeCond.Type.ISNULL) {
                cond.setType(AttributeCond.Type.ISNOTNULL);
            } else if (cond.getType() == AttributeCond.Type.ISNOTNULL) {
                cond.setType(AttributeCond.Type.ISNULL);
            }
        }

        StringBuilder query = new StringBuilder("SELECT DISTINCT any_id FROM ").
                append(svs.field().name).append(" WHERE ");
        switch (cond.getType()) {
            case ISNOTNULL:
                query.append("plainAttrs @> '[{\"schema\":\"").
                        append(checked.getLeft().getKey()).
                        append("\"}]'::jsonb");
                break;

            case ISNULL:
                query.append("any_id NOT IN (").
                        append("SELECT any_id FROM ").append(svs.field().name).
                        append(" WHERE plainAttrs @> '[{\"schema\":\"").
                        append(checked.getLeft().getKey()).
                        append("\"}]'::jsonb)");
                break;

            default:
                fillAttrQuery(anyUtilsFactory.getInstance(svs.anyTypeKind),
                        query, checked.getRight(), checked.getLeft(), cond, not, parameters);
        }

        return query.toString();
    }
}
