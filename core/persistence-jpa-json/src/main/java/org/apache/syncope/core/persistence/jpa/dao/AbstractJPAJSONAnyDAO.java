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

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.persistence.Query;
import org.apache.commons.jexl3.parser.Parser;
import org.apache.commons.jexl3.parser.ParserConstants;
import org.apache.commons.jexl3.parser.Token;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.common.lib.types.AttrSchemaType;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.JPAJSONAnyDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.JSONPlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.jpa.entity.AbstractEntity;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

abstract class AbstractJPAJSONAnyDAO extends AbstractDAO<AbstractEntity> implements JPAJSONAnyDAO {

    @Autowired
    private PlainSchemaDAO plainSchemaDAO;

    @Autowired
    private DerSchemaDAO derSchemaDAO;

    protected abstract String queryBegin(String table);

    protected abstract String attrValueMatch(
            AnyUtils anyUtils,
            PlainSchema schema,
            PlainAttrValue attrValue,
            boolean ignoreCaseMatch);

    protected Pair<String, Boolean> schemaInfo(final AttrSchemaType schemaType, final boolean ignoreCaseMatch) {
        String key;
        boolean lower = false;

        switch (schemaType) {
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
                lower = ignoreCaseMatch;
                key = "stringValue";
        }

        return Pair.of(key, lower);
    }

    protected <A extends Any<?>> List<A> buildResult(final AnyUtils anyUtils, final List<Object> queryResult) {
        List<A> result = new ArrayList<>();
        queryResult.forEach(anyKey -> {
            A any = anyUtils.<A>dao().find(anyKey.toString());
            if (any == null) {
                LOG.error("Could not find any for key {}", anyKey);
            } else {
                result.add(any);
            }
        });
        return result;
    }

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    @Override
    public <A extends Any<?>> List<A> findByPlainAttrValue(
            final String table,
            final AnyUtils anyUtils,
            final PlainSchema schema,
            final PlainAttrValue attrValue,
            final boolean ignoreCaseMatch) {

        if (schema == null) {
            LOG.error("No PlainSchema");
            return Collections.<A>emptyList();
        }

        Query query = entityManager().createNativeQuery(
                queryBegin(table)
                + "WHERE " + attrValueMatch(anyUtils, schema, attrValue, ignoreCaseMatch));
        query.setParameter(1, schema.getKey());
        query.setParameter(2, attrValue.getValue());

        return buildResult(anyUtils, query.getResultList());
    }

    @Transactional(readOnly = true)
    @Override
    public <A extends Any<?>> A findByPlainAttrUniqueValue(
            final String table,
            final AnyUtils anyUtils,
            final PlainSchema schema,
            final PlainAttrValue attrUniqueValue,
            final boolean ignoreCaseMatch) {

        if (schema == null) {
            LOG.error("No PlainSchema");
            return null;
        }
        if (!schema.isUniqueConstraint()) {
            LOG.error("This schema has not unique constraint: '{}'", schema.getKey());
            return null;
        }

        List<A> result = findByPlainAttrValue(table, anyUtils, schema, attrUniqueValue, ignoreCaseMatch);
        return result.isEmpty()
                ? null
                : result.get(0);
    }

    /**
     * Split an attribute value recurring on provided literals/tokens.
     *
     * @param attrValue value to be split
     * @param literals literals/tokens
     * @return split value
     */
    private List<String> split(final String attrValue, final List<String> literals) {
        final List<String> attrValues = new ArrayList<>();

        if (literals.isEmpty()) {
            attrValues.add(attrValue);
        } else {
            for (String token : attrValue.split(Pattern.quote(literals.get(0)))) {
                if (!token.isEmpty()) {
                    attrValues.addAll(split(token, literals.subList(1, literals.size())));
                }
            }
        }

        return attrValues;
    }

    protected abstract List<Object> findByDerAttrValue(String table, Map<String, List<Object>> clauses);

    @SuppressWarnings("unchecked")
    @Transactional(readOnly = true)
    @Override
    public <A extends Any<?>> List<A> findByDerAttrValue(
            final String table,
            final AnyUtils anyUtils,
            final DerSchema derSchema,
            final String value,
            final boolean ignoreCaseMatch) {

        if (derSchema == null) {
            LOG.error("No DerSchema");
            return Collections.<A>emptyList();
        }

        Parser parser = new Parser(new StringReader(derSchema.getExpression()));

        // Schema keys
        List<String> identifiers = new ArrayList<>();

        // Literals
        List<String> literals = new ArrayList<>();

        // Get schema keys and literals
        for (Token token = parser.getNextToken(); token != null && StringUtils.isNotBlank(token.toString());
                token = parser.getNextToken()) {

            if (token.kind == ParserConstants.STRING_LITERAL) {
                literals.add(token.toString().substring(1, token.toString().length() - 1));
            }

            if (token.kind == ParserConstants.IDENTIFIER) {
                identifiers.add(token.toString());
            }
        }

        // Sort literals in order to process later literals included into others
        Collections.sort(literals, (l1, l2) -> {
            if (l1 == null && l2 == null) {
                return 0;
            } else if (l1 != null && l2 == null) {
                return -1;
            } else if (l1 == null && l2 != null) {
                return 1;
            } else if (l1.length() == l2.length()) {
                return 0;
            } else if (l1.length() > l2.length()) {
                return -1;
            } else {
                return 1;
            }
        });

        // Split value on provided literals
        List<String> attrValues = split(value, literals);

        if (attrValues.size() != identifiers.size()) {
            LOG.error("Ambiguous JEXL expression resolution: literals and values have different size");
            return Collections.emptyList();
        }

        Map<String, List<Object>> clauses = new LinkedHashMap<>();

        // builder to build the clauses
        StringBuilder bld = new StringBuilder();

        // Contains used identifiers in order to avoid replications
        Set<String> used = new HashSet<>();

        // Create several clauses: one for eanch identifiers
        for (int i = 0; i < identifiers.size(); i++) {
            if (!used.contains(identifiers.get(i))) {
                // verify schema existence and get schema type
                PlainSchema schema = plainSchemaDAO.find(identifiers.get(i));
                if (schema == null) {
                    LOG.error("Invalid schema '{}', ignoring", identifiers.get(i));
                } else {
                    // clear builder
                    bld.delete(0, bld.length());

                    PlainAttrValue attrValue;
                    if (schema.isUniqueConstraint()) {
                        attrValue = anyUtils.newPlainAttrUniqueValue();
                    } else {
                        attrValue = anyUtils.newPlainAttrValue();
                    }
                    attrValue.setStringValue(attrValues.get(i));

                    bld.append('(').
                            append(queryBegin(table)).
                            append("WHERE ").
                            append(attrValueMatch(anyUtils, schema, attrValue, ignoreCaseMatch)).
                            append(')');

                    used.add(identifiers.get(i));

                    List<Object> queryParams = new ArrayList<>();
                    queryParams.add(schema.getKey());
                    queryParams.add(attrValues.get(i));

                    clauses.put(bld.toString(), queryParams);
                }
            }
        }

        LOG.debug("Generated where clauses {}", clauses);

        return buildResult(anyUtils, findByDerAttrValue(table, clauses));
    }

    @Transactional
    @Override
    public <A extends Any<?>> void checkBeforeSave(final String table, final AnyUtils anyUtils, final A any) {
        // check UNIQUE constraints
        any.getPlainAttrs().stream().
                filter(attr -> attr.getUniqueValue() != null).
                map(JSONPlainAttr.class::cast).
                forEach(attr -> {
                    PlainSchema schema = attr.getSchema();
                    List<A> others = findByPlainAttrValue(table, anyUtils, schema, attr.getUniqueValue(), false);
                    if (others.isEmpty() || (others.size() == 1 && others.get(0).getKey().equals(any.getKey()))) {
                        LOG.debug("No duplicate value found for {}", attr.getUniqueValue().getValueAsString());
                    } else {
                        throw new DuplicateException(
                                "Value " + attr.getUniqueValue().getValueAsString()
                                + " existing for " + schema.getKey());
                    }
                });

        // update sysInfo - as org.apache.syncope.core.persistence.jpa.entity.PlainAttrListener is not invoked
        Date now = new Date();
        String username = AuthContextUtils.getUsername();
        LOG.debug("Set last change date '{}' and modifier '{}' for '{}'", now, username, any);
        any.setLastModifier(username);
        any.setLastChangeDate(now);
    }
}
