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
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.attrvalue.validation.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.AnyObjectDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.UserDAO;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;

public class OJPAJSONAnySearchDAO extends JPAAnySearchDAO {

    public OJPAJSONAnySearchDAO(
            final RealmDAO realmDAO,
            final DynRealmDAO dynRealmDAO,
            final UserDAO userDAO,
            final GroupDAO groupDAO,
            final AnyObjectDAO anyObjectDAO,
            final PlainSchemaDAO schemaDAO,
            final EntityFactory entityFactory,
            final AnyUtilsFactory anyUtilsFactory,
            final PlainAttrValidationManager validator) {

        super(
                realmDAO,
                dynRealmDAO,
                userDAO,
                groupDAO,
                anyObjectDAO,
                schemaDAO,
                entityFactory,
                anyUtilsFactory,
                validator);
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

        item.select = new StringBuilder().append("( SELECT usa").
                append('.').
                append((schema.isUniqueConstraint() ? "u" : "") + key(schema.getType())).
                append(" FROM ").
                append(svs.field().name).
                append(" usa WHERE usa.any_id = ").
                append(svs.field().alias).
                append(".any_id").
                append(" AND usa.plainSchema ='").append(fieldName).append("'").
                append(" FETCH FIRST 1 ROWS ONLY ").
                append(") AS ").append(fieldName).toString();
        
        item.where = "plainSchema = '" + fieldName + '\'';
        item.orderBy = fieldName + ' ' + clause.getDirection().name();
    }

    protected AnySearchNode.Leaf filJSONAttrQuery(
            final SearchSupport.SearchView from,
            final PlainAttrValue attrValue,
            final PlainSchema schema,
            final AttrCond cond,
            final boolean not,
            final List<Object> parameters) {

        String key = key(schema.getType());

        String value = Optional.ofNullable(attrValue.getDateValue()).
                map(DateTimeFormatter.ISO_OFFSET_DATE_TIME::format).
                orElseGet(() -> schema.getType() == AttrSchemaType.Boolean
                ? BooleanUtils.toStringTrueFalse(attrValue.getBooleanValue())
                : cond.getExpression());

        boolean lower = (schema.getType() == AttrSchemaType.String || schema.getType() == AttrSchemaType.Enum)
                && (cond.getType() == AttrCond.Type.IEQ || cond.getType() == AttrCond.Type.ILIKE);

        StringBuilder clause = new StringBuilder("plainSchema=?").append(setParameter(parameters, cond.getSchema())).
                append(" AND ").
                append(lower ? "LOWER(" : "");
        if (schema.isUniqueConstraint()) {
            clause.append("u").append(key);
        } else {
            clause.append("JSON_VALUE(").append(key).append(", '$[*]')");
        }
        clause.append(lower ? ')' : "");

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

        // workaround for Oracle DB adding explicit escaping string, to search 
        // for literal _ (underscore) (SYNCOPE-1779)
        if (cond.getType() == AttrCond.Type.ILIKE || cond.getType() == AttrCond.Type.LIKE) {
            clause.append(" ESCAPE '\\' ");
        }

        return new AnySearchNode.Leaf(from, clause.toString());
    }

    @Override
    protected AnySearchNode getQuery(
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
            case ISNOTNULL:
                return new AnySearchNode.Leaf(
                        svs.field(),
                        "JSON_EXISTS(plainAttrs, '$[*]?(@.schema == \"" + checked.getLeft().getKey() + "\")')");

            case ISNULL:
                return new AnySearchNode.Leaf(
                        svs.field(),
                        "NOT JSON_EXISTS(plainAttrs, '$[*]?(@.schema == \"" + checked.getLeft().getKey() + "\")')");

            default:
                AnySearchNode.Leaf node;
                if (not && checked.getLeft().isMultivalue()) {
                    AnySearchNode.Leaf notNode = filJSONAttrQuery(
                            svs.field(),
                            checked.getRight(),
                            checked.getLeft(),
                            cond,
                            false,
                            parameters);
                    node = new AnySearchNode.Leaf(
                            notNode.getFrom(),
                            "sv.any_id NOT IN ("
                            + "SELECT any_id FROM " + notNode.getFrom().name
                            + " WHERE " + notNode.getClause().replace(notNode.getFrom().alias + ".", "")
                            + ")");
                } else {
                    node = filJSONAttrQuery(
                            svs.field(),
                            checked.getRight(),
                            checked.getLeft(),
                            cond,
                            not,
                            parameters);
                }
                return node;
        }
    }
}
