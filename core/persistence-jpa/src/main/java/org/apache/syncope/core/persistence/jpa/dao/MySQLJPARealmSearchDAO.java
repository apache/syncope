/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * \"License\"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * \"AS IS\" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.syncope.core.persistence.jpa.dao;

import jakarta.persistence.EntityManager;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.attrvalue.PlainAttrValidationManager;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.entity.EntityFactory;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.utils.RealmUtils;
import org.apache.syncope.core.persistence.jpa.entity.JPARealm;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.springframework.data.domain.Sort;

public class MySQLJPARealmSearchDAO extends AbstractJPARealmSearchDAO {

    protected static String from(final PlainSchema schema) {
        String sqlType = switch (schema.getType()) {
            case Long ->
                "BIGINT";

            case Double ->
                "DOUBLE";

            case Boolean ->
                "VARCHAR(8)";

            default ->
                "VARCHAR(255)";
        };

        if (schema.isUniqueConstraint()) {
            return "JSON_TABLE(r.plainAttrs, '$[*]' COLUMNS ("
                    + "schemaz VARCHAR(255) PATH '$.schema', "
                    + "uniqueValue " + sqlType + " PATH '$.uniqueValue." + key(schema.getType()) + "'"
                    + ")) AS " + schema.getKey();
        }

        return "JSON_TABLE(r.plainAttrs, '$[*]' COLUMNS ("
                + "schemaz VARCHAR(255) PATH '$.schema', "
                + "NESTED PATH '$.values[*]' COLUMNS ("
                + "valuez " + sqlType + " PATH '$." + key(schema.getType()) + "'"
                + "))) AS " + schema.getKey();
    }

    public MySQLJPARealmSearchDAO(
            final EntityManager entityManager,
            final PlainSchemaDAO plainSchemaDAO,
            final EntityFactory entityFactory,
            final PlainAttrValidationManager validator,
            final RealmUtils realmUtils) {

        super(entityManager, plainSchemaDAO, entityFactory, validator, realmUtils);
    }

    @Override
    protected void parseOrderByForPlainSchema(
            final OrderBySupport obs,
            final OrderBySupport.Item item,
            final Sort.Order clause,
            final PlainSchema schema,
            final String fieldName) {

        // keep track of involvement of non-mandatory schemas in the order by clauses
        obs.nonMandatorySchemas = !"true".equals(schema.getMandatoryCondition());

        item.select = schema.getKey() + "."
                + (schema.isUniqueConstraint() ? "uniqueValue" : "valuez")
                + " AS " + schema.getKey();
        item.where = StringUtils.EMPTY;
        item.orderBy = fieldName + ' ' + clause.getDirection().name();
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

        boolean isString = schema.getType() == AttrSchemaType.String || schema.getType() == AttrSchemaType.Enum;
        boolean lower = isString && (cond.getType() == AttrCond.Type.IEQ || cond.getType() == AttrCond.Type.ILIKE);
        boolean binary = isString && !lower;

        StringBuilder clause = new StringBuilder(schema.getKey()).
                append(".schemaz=?").append(setParameter(parameters, cond.getSchema())).
                append(" AND ").
                append(lower ? "LOWER(" : "").
                append(binary ? "BINARY " : "").
                append(schema.getKey()).append('.').
                append(schema.isUniqueConstraint()
                        ? "uniqueValue"
                        : "valuez").
                append(lower ? ')' : "");

        switch (cond.getType()) {
            case LIKE:
            case ILIKE:
                if (not) {
                    clause.append(" NOT");
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
                append('?').append(setParameter(parameters,
                cond.getType() == AttrCond.Type.LIKE || cond.getType() == AttrCond.Type.ILIKE
                ? value
                : typedValue)).
                append(lower ? ")" : "");

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
                return new AttrCondQuery(true, new RealmSearchNode.Leaf(
                        "JSON_SEARCH("
                        + "r.plainAttrs, 'one', '" + checked.schema().getKey() + "', NULL, '$[*].schema'"
                        + ") IS NOT NULL"));
            }

            case ISNULL -> {
                return new AttrCondQuery(true, new RealmSearchNode.Leaf(
                        "JSON_SEARCH("
                        + "r.plainAttrs, 'one', '" + checked.schema().getKey() + "', NULL, '$[*].schema'"
                        + ") IS NULL"));
            }

            default -> {
                if (!not && cond.getType() == AttrCond.Type.EQ) {
                    PlainAttr container = new PlainAttr();
                    container.setPlainSchema(checked.schema());
                    if (checked.schema().isUniqueConstraint()) {
                        container.setUniqueValue(checked.value());
                    } else {
                        container.add(checked.value());
                    }

                    return new AttrCondQuery(true, new RealmSearchNode.Leaf(
                            "JSON_CONTAINS("
                            + "r.plainAttrs, '" + POJOHelper.serialize(List.of(container)).replace("'", "''")
                            + "')"));
                } else {
                    RealmSearchNode.Leaf node;
                    if (not && checked.schema().isMultivalue()) {
                        RealmSearchNode.Leaf notNode = filJSONAttrQuery(
                                checked.value(),
                                checked.schema(),
                                cond,
                                false,
                                parameters);
                        node = new RealmSearchNode.Leaf(
                                "r.id NOT IN ("
                                + "SELECT r.id FROM " + JPARealm.TABLE + " r, " + from(checked.schema())
                                + " WHERE " + notNode.getClause()
                                + ")");
                    } else {
                        node = filJSONAttrQuery(
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
    }

    @Override
    protected String buildFrom(final Set<String> plainSchemas, final OrderBySupport obs) {
        StringBuilder clause = new StringBuilder(super.buildFrom(plainSchemas, obs));

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
