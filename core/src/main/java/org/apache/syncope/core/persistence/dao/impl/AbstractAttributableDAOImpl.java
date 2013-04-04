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
package org.apache.syncope.core.persistence.dao.impl;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import javax.persistence.TypedQuery;
import org.apache.commons.jexl2.parser.Parser;
import org.apache.commons.jexl2.parser.ParserConstants;
import org.apache.commons.jexl2.parser.Token;
import org.apache.syncope.common.services.InvalidSearchConditionException;
import org.apache.syncope.core.persistence.beans.AbstractAttrValue;
import org.apache.syncope.core.persistence.beans.AbstractAttributable;
import org.apache.syncope.core.persistence.beans.AbstractDerSchema;
import org.apache.syncope.core.persistence.beans.AbstractSchema;
import org.apache.syncope.core.persistence.beans.ExternalResource;
import org.apache.syncope.core.persistence.dao.AttributableDAO;
import org.apache.syncope.core.persistence.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.dao.SchemaDAO;
import org.apache.syncope.core.util.AttributableUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;

public abstract class AbstractAttributableDAOImpl extends AbstractDAOImpl implements AttributableDAO {

    @Autowired
    protected SchemaDAO schemaDAO;

    @Autowired
    protected DerSchemaDAO derSchemaDAO;

    /**
     * Split an attribute value recurring on provided literals/tokens.
     *
     * @param attrValue value to be split
     * @param literals literals/tokens
     * @return splitted value
     */
    private List<String> split(final String attrValue, final List<String> literals) {
        final List<String> attrValues = new ArrayList<String>();

        if (literals.isEmpty()) {
            attrValues.add(attrValue);
        } else {
            for (String token : attrValue.split(Pattern.quote(literals.get(0)))) {
                attrValues.addAll(split(token, literals.subList(1, literals.size())));
            }
        }

        return attrValues;
    }

    /**
     * Generate one where clause for each different attribute schema into the derived schema expression provided.
     *
     * @param expression derived schema expression
     * @param value derived attribute value
     * @param attrUtil USER / ROLE
     * @return where clauses to use to build the query
     * @throws InvalidSearchConditionException in case of errors retrieving identifiers
     */
    private Set<String> getWhereClause(final String expression, final String value, final AttributableUtil attrUtil)
            throws InvalidSearchConditionException {

        final Parser parser = new Parser(new StringReader(expression));

        // Schema names
        final List<String> identifiers = new ArrayList<String>();

        // Literals
        final List<String> literals = new ArrayList<String>();

        // Get schema names and literals
        Token token;
        while ((token = parser.getNextToken()) != null && StringUtils.hasText(token.toString())) {
            if (token.kind == ParserConstants.STRING_LITERAL) {
                literals.add(token.toString().substring(1, token.toString().length() - 1));
            }

            if (token.kind == ParserConstants.IDENTIFIER) {
                identifiers.add(token.toString());
            }
        }

        // Sort literals in order to process later literals included into others
        Collections.sort(literals, new Comparator<String>() {

            @Override
            public int compare(final String t, final String t1) {
                if (t == null && t1 == null) {
                    return 0;
                } else if (t != null && t1 == null) {
                    return -1;
                } else if (t == null && t1 != null) {
                    return 1;
                } else if (t.length() == t1.length()) {
                    return 0;
                } else if (t.length() > t1.length()) {
                    return -1;
                } else {
                    return 1;
                }
            }
        });

        // Split value on provided literals
        final List<String> attrValues = split(value, literals);

        if (attrValues.size() != identifiers.size()) {
            LOG.error("Ambiguous jexl expression resolution.");
            throw new InvalidSearchConditionException("literals and values have different size");
        }

        // clauses to be used with INTERSECTed queries
        final Set<String> clauses = new HashSet<String>();

        // builder to build the clauses
        final StringBuilder bld = new StringBuilder();

        // Contains used identifiers in order to avoid replications
        final Set<String> used = new HashSet<String>();

        // Create several clauses: one for eanch identifiers
        for (int i = 0; i < identifiers.size(); i++) {
            if (!used.contains(identifiers.get(i))) {

                // verify schema existence and get schema type
                AbstractSchema schema = schemaDAO.find(identifiers.get(i), attrUtil.schemaClass());
                if (schema == null) {
                    LOG.error("Invalid schema name '{}'", identifiers.get(i));
                    throw new InvalidSearchConditionException("Invalid schema name " + identifiers.get(i));
                }

                // clear builder
                bld.delete(0, bld.length());

                bld.append("(");

                // set schema name
                bld.append("s.name = '").append(identifiers.get(i)).append("'");

                bld.append(" AND ");

                bld.append("s.name = a.schema_name").append(" AND ");

                bld.append("a.id = v.attribute_id");

                bld.append(" AND ");

                // use a value clause different for eanch different schema type
                switch (schema.getType()) {
                    case Boolean:
                        bld.append("v.booleanValue = '").append(attrValues.get(i)).append("'");
                        break;
                    case Long:
                        bld.append("v.longValue = ").append(attrValues.get(i));
                        break;
                    case Double:
                        bld.append("v.doubleValue = ").append(attrValues.get(i));
                        break;
                    case Date:
                        bld.append("v.dateValue = '").append(attrValues.get(i)).append("'");
                        break;
                    default:
                        bld.append("v.stringValue = '").append(attrValues.get(i)).append("'");
                }

                bld.append(")");

                used.add(identifiers.get(i));

                clauses.add(bld.toString());
            }
        }

        LOG.debug("Generated where clauses {}", clauses);

        return clauses;
    }

    protected abstract <T extends AbstractAttributable> T findInternal(final Long id);

    @Override
    public <T extends AbstractAttributable> List<T> findByAttrValue(final String schemaName,
            final AbstractAttrValue attrValue, final AttributableUtil attrUtil) {

        AbstractSchema schema = schemaDAO.find(schemaName, attrUtil.schemaClass());
        if (schema == null) {
            LOG.error("Invalid schema name '{}'", schemaName);
            return Collections.<T>emptyList();
        }

        final String entityName = schema.isUniqueConstraint()
                ? attrUtil.attrUniqueValueClass().getName()
                : attrUtil.attrValueClass().getName();

        TypedQuery<AbstractAttrValue> query = entityManager.createQuery("SELECT e FROM " + entityName + " e"
                + " WHERE e.attribute.schema.name = :schemaName AND (e.stringValue IS NOT NULL"
                + " AND e.stringValue = :stringValue)"
                + " OR (e.booleanValue IS NOT NULL AND e.booleanValue = :booleanValue)"
                + " OR (e.dateValue IS NOT NULL AND e.dateValue = :dateValue)"
                + " OR (e.longValue IS NOT NULL AND e.longValue = :longValue)"
                + " OR (e.doubleValue IS NOT NULL AND e.doubleValue = :doubleValue)",
                AbstractAttrValue.class);

        query.setParameter("schemaName", schemaName);
        query.setParameter("stringValue", attrValue.getStringValue());
        query.setParameter("booleanValue", attrValue.getBooleanValue() == null
                ? null
                : attrValue.getBooleanAsInteger(attrValue.getBooleanValue()));
        if (attrValue.getDateValue() == null) {
            query.setParameter("dateValue", null);
        } else {
            query.setParameter("dateValue", attrValue.getDateValue(), TemporalType.TIMESTAMP);
        }
        query.setParameter("longValue", attrValue.getLongValue());
        query.setParameter("doubleValue", attrValue.getDoubleValue());

        List<T> result = new ArrayList<T>();
        for (AbstractAttrValue value : query.getResultList()) {
            T subject = value.getAttribute().getOwner();
            if (!result.contains(subject)) {
                result.add(subject);
            }
        }

        return result;
    }

    @Override
    public <T extends AbstractAttributable> AbstractAttributable findByAttrUniqueValue(final String schemaName,
            final AbstractAttrValue attrUniqueValue, final AttributableUtil attrUtil) {

        AbstractSchema schema = schemaDAO.find(schemaName, attrUtil.schemaClass());
        if (schema == null) {
            LOG.error("Invalid schema name '{}'", schemaName);
            return null;
        }
        if (!schema.isUniqueConstraint()) {
            LOG.error("This schema has not unique constraint: '{}'", schemaName);
            return null;
        }

        List<T> result = findByAttrValue(schemaName, attrUniqueValue, attrUtil);
        return result.isEmpty()
                ? null
                : result.iterator().next();
    }

    /**
     * Find users / roles by derived attribute value. This method could fail if one or more string literals contained
     * into the derived attribute value provided derive from identifier (schema name) replacement. When you are going to
     * specify a derived attribute expression you must be quite sure that string literals used to build the expression
     * cannot be found into the attribute values used to replace attribute schema names used as identifiers.
     *
     * @param <T> user / role
     * @param schemaName derived schema name
     * @param value derived attribute value
     * @param attrUtil AttributableUtil
     * @return list of users / roles
     * @throws InvalidSearchConditionException in case of errors retrieving schema names used to buid the derived schema
     * expression.
     */
    @Override
    public <T extends AbstractAttributable> List<T> findByDerAttrValue(final String schemaName, final String value,
            final AttributableUtil attrUtil)
            throws InvalidSearchConditionException {

        AbstractDerSchema schema = derSchemaDAO.find(schemaName, attrUtil.derSchemaClass());
        if (schema == null) {
            LOG.error("Invalid schema name '{}'", schemaName);
            return Collections.<T>emptyList();
        }

        // query string
        final StringBuilder querystring = new StringBuilder();

        boolean subquery = false;
        for (String clause : getWhereClause(schema.getExpression(), value, attrUtil)) {
            if (querystring.length() > 0) {
                subquery = true;
                querystring.append(" AND a.owner_id IN ( ");
            }

            querystring.append("SELECT a.owner_id ").
                    append("FROM ").append(attrUtil.attrClass().getSimpleName()).append(" a, ").
                    append(attrUtil.attrValueClass().getSimpleName()).append(" v, ").
                    append(attrUtil.schemaClass().getSimpleName()).append(" s ").
                    append("WHERE ").append(clause);

            if (subquery) {
                querystring.append(')');
            }
        }

        LOG.debug("Execute query {}", querystring);

        final Query query = entityManager.createNativeQuery(querystring.toString());

        final List<T> result = new ArrayList<T>();

        for (Object userId : query.getResultList()) {
            T subject = findInternal(Long.parseLong(userId.toString()));
            if (!result.contains(subject)) {
                result.add(subject);
            }
        }

        return result;
    }

    @Override
    public <T extends AbstractAttributable> List<T> findByResource(final ExternalResource resource,
            final Class<T> reference) {

        TypedQuery<T> query = entityManager.createQuery("SELECT e FROM " + reference.getSimpleName() + " e "
                + "WHERE :resource MEMBER OF e.resources", reference);
        query.setParameter("resource", resource);

        return query.getResultList();
    }
}
