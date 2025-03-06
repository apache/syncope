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
import java.util.HashSet;
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

public class PGJPAAnySearchDAO extends AbstractJPAAnySearchDAO {

    protected static final String REGEX_CHARS = "!$()*+.:<=>?[\\]^{|}-";

    protected static String escapeForLikeRegex(final String input) {
        String output = input;
        for (char toEscape : REGEX_CHARS.toCharArray()) {
            output = output.replace(String.valueOf(toEscape), "\\" + toEscape);
        }
        return output.replace("'", "''");
    }

    protected static String escapeIfString(final String value, final boolean isStr) {
        return isStr
                ? new StringBuilder().append('"').append(value.replace("'", "''")).append('"').toString()
                : value;
    }

    public PGJPAAnySearchDAO(
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

        item.select = fieldName + " -> 0 AS " + fieldName;
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
            final boolean not) {

        String key = key(schema.getType());

        String value = Optional.ofNullable(attrValue.getDateValue()).
                map(DateTimeFormatter.ISO_OFFSET_DATE_TIME::format).
            orElseGet(cond::getExpression);

        boolean isStr = true;
        boolean lower = false;
        if (schema.getType() == AttrSchemaType.String || schema.getType() == AttrSchemaType.Enum) {
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

        StringBuilder clause = new StringBuilder();
        switch (cond.getType()) {
            case ILIKE:
            case LIKE:
                // jsonb_path_exists(Nome, '$[*] ? (@.stringValue like_regex "EL.*" flag "i")')
                if (schema.getType() == AttrSchemaType.String || schema.getType() == AttrSchemaType.Enum) {
                    clause.append("jsonb_path_exists(").append(schema.getKey()).append(", '$[*] ? ").
                            append("(@.").append(key).append(" like_regex \"").
                            append(escapeForLikeRegex(value).replace("%", ".*")).
                            append("\"").
                            append(lower ? " flag \"i\"" : "").append(")')");
                } else {
                    LOG.error("LIKE is only compatible with string or enum schemas");
                    clause.append(' ').append(ALWAYS_FALSE_CLAUSE);
                }
                break;

            case IEQ:
            case EQ:
            default:
                clause.append("jsonb_path_exists(").append(schema.getKey()).append(", '$[*] ? ").
                        append("(@.").append(key);

                if (StringUtils.containsAny(value, REGEX_CHARS) || lower) {
                    clause.append(" like_regex \"^").
                            append(escapeForLikeRegex(value).replace("'", "''")).
                            append("$\"");
                } else {
                    clause.append(" == ").append(escapeIfString(value, isStr));
                }

                clause.append(lower ? " flag \"i\"" : "").append(")')");
                break;

            case GE:
                clause.append("jsonb_path_exists(").append(schema.getKey()).append(", '$[*] ? ").
                        append("(@.").append(key).append(" >= ").
                        append(escapeIfString(value, isStr)).append(")')");
                break;

            case GT:
                clause.append("jsonb_path_exists(").append(schema.getKey()).append(", '$[*] ? ").
                        append("(@.").append(key).append(" > ").
                        append(escapeIfString(value, isStr)).append(")')");
                break;

            case LE:
                clause.append("jsonb_path_exists(").append(schema.getKey()).append(", '$[*] ? ").
                        append("(@.").append(key).append(" <= ").
                        append(escapeIfString(value, isStr)).append(")')");
                break;

            case LT:
                clause.append("jsonb_path_exists(").append(schema.getKey()).append(", '$[*] ? ").
                        append("(@.").append(key).append(" < ").
                        append(escapeIfString(value, isStr)).append(")')");
                break;
        }

        if (not) {
            clause.insert(0, "NOT ");
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

        return switch (cond.getType()) {
            case ISNOTNULL ->
                Pair.of(true, new AnySearchNode.Leaf(
                svs.table(),
                "jsonb_path_exists(" + checked.getLeft().getKey() + ",'$[*]')"));

            case ISNULL ->
                Pair.of(true, new AnySearchNode.Leaf(
                svs.table(),
                "NOT jsonb_path_exists(" + checked.getLeft().getKey() + ",'$[*]')"));

            default ->
                Pair.of(true, filJSONAttrQuery(
                svs.table(),
                checked.getRight(),
                checked.getLeft(),
                cond,
                not));
        };
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

        Set<String> schemas = new HashSet<>(plainSchemas);

        if (obs != null) {
            obs.items.forEach(item -> {
                String schema = StringUtils.substringBefore(item.orderBy, ' ');
                if (StringUtils.isNotBlank(schema)) {
                    schemas.add(schema);
                }
            });
        }

        // i.e jsonb_path_query(plainattrs, '$[*] ? (@.schema=="Nome")."values"') AS Nome
        schemas.forEach(schema -> plainSchemaDAO.findById(schema).ifPresent(
                pschema -> clause.append(',').
                        append("jsonb_path_query_array(plainattrs, '$[*] ? (@.schema==\"").
                        append(schema).append("\").").
                        append("\"").append(pschema.isUniqueConstraint() ? "uniqueValue" : "values").append("\"')").
                        append(" AS ").append(schema)));

        return clause.toString();
    }
}
