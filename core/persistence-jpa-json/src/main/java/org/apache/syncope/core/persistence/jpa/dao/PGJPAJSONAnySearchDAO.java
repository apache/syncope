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

import java.text.ParseException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.persistence.api.entity.JSONPlainAttr;
import org.apache.syncope.core.provisioning.api.utils.FormatUtils;

public class PGJPAJSONAnySearchDAO extends AbstractJPAJSONAnySearchDAO {

    @Override
    protected void processOBS(
            final SearchSupport svs,
            final Set<String> involvedPlainAttrs,
            final OrderBySupport obs,
            final StringBuilder where) {

        Set<String> attrs = obs.items.stream().
                map(item -> item.orderBy.substring(0, item.orderBy.indexOf(" "))).collect(Collectors.toSet());

        obs.views.forEach(searchView -> {
            if (searchView.name.equals(svs.field().name)) {
                StringBuilder attrWhere = new StringBuilder();
                StringBuilder nullAttrWhere = new StringBuilder();

                where.append(", (SELECT * FROM ").append(searchView.name);

                if (svs.nonMandatorySchemas || obs.nonMandatorySchemas) {
                    attrs.forEach(field -> {
                        if (attrWhere.length() == 0) {
                            attrWhere.append(" WHERE ");
                        } else {
                            attrWhere.append(" OR ");
                        }
                        attrWhere.append("plainAttrs @> '[{\"schema\":\"").append(field).append("\"}]'::jsonb");

                        nullAttrWhere.append(" UNION SELECT DISTINCT any_id,").append(svs.table().alias).append(".*, ").
                                append("'{\"schema\": \"").
                                append(field).
                                append("\"}'::jsonb as attrs, '{}'::jsonb as attrValues").
                                append(" FROM ").append(svs.table().name).append(' ').append(svs.table().alias).
                                append(", ").append(svs.field().name).
                                append(" WHERE ").
                                append("any_id NOT IN ").
                                append("(SELECT distinct any_id FROM ").
                                append(svs.field().name).
                                append(" WHERE ").append(svs.table().alias).append(".id=any_id AND ").
                                append("plainAttrs @> '[{\"schema\":\"").append(field).append("\"}]'::jsonb)");
                    });
                    where.append(attrWhere).append(nullAttrWhere);
                }

                where.append(')');
            } else {
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

        item.select = svs.field().alias + ".attrValues ->> '" + key(schema.getType()) + "' AS " + fieldName;
        item.where = "attrs ->> 'schema' = '" + fieldName + '\'';
        item.orderBy = fieldName + ' ' + clause.getDirection().name();
    }

    private void fillAttrQuery(
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

                query.append("plainAttrs @> '").
                        append(POJOHelper.serialize(List.of(container)).replace("'", "''")).
                        append("'::jsonb");
            } else {
                String key = key(schema.getType());
                boolean lower = (schema.getType() == AttrSchemaType.String || schema.getType() == AttrSchemaType.Enum)
                        && (cond.getType() == AttrCond.Type.IEQ || cond.getType() == AttrCond.Type.ILIKE);

                query.append("attrs ->> 'schema' = ?").append(setParameter(parameters, cond.getSchema())).
                        append(" AND ").
                        append(lower ? "LOWER(" : "").
                        append(schema.isUniqueConstraint()
                                ? "attrs -> 'uniqueValue'" : "attrValues").
                        append(" ->> '").append(key).append('\'').
                        append(lower ? ")" : "");

                appendOp(query, cond.getType(), not);

                String value = cond.getExpression();
                if (schema.getType() == AttrSchemaType.Date) {
                    try {
                        value = String.valueOf(FormatUtils.parseDate(value).getTime());
                    } catch (ParseException e) {
                        LOG.error("Could not parse {} as date", value, e);
                    }
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

        Pair<PlainSchema, PlainAttrValue> checked;
        try {
            checked = check(cond, svs.anyTypeKind);
        } catch (IllegalArgumentException e) {
            return EMPTY_QUERY;
        }

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
