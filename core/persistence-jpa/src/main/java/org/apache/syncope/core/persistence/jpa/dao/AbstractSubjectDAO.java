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
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import org.apache.commons.jexl2.parser.Parser;
import org.apache.commons.jexl2.parser.ParserConstants;
import org.apache.commons.jexl2.parser.Token;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.SubjectDAO;
import org.apache.syncope.core.persistence.api.dao.SubjectSearchDAO;
import org.apache.syncope.core.persistence.api.dao.search.AttributeCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.dao.search.SubjectCond;
import org.apache.syncope.core.persistence.api.entity.AttributableUtils;
import org.apache.syncope.core.persistence.api.entity.DerAttr;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Subject;
import org.apache.syncope.core.persistence.api.entity.VirAttr;
import org.apache.syncope.core.persistence.jpa.entity.AbstractPlainAttrValue;
import org.springframework.beans.factory.annotation.Autowired;

abstract class AbstractSubjectDAO<P extends PlainAttr, D extends DerAttr, V extends VirAttr>
        extends AbstractDAO<Subject<P, D, V>, Long> implements SubjectDAO<P, D, V> {

    @Autowired
    protected PlainSchemaDAO plainSchemaDAO;

    @Autowired
    protected DerSchemaDAO derSchemaDAO;

    @Autowired
    protected SubjectSearchDAO searchDAO;

    protected SearchCond getAllMatchingCond() {
        SubjectCond idCond = new SubjectCond(AttributeCond.Type.ISNOTNULL);
        idCond.setSchema("id");
        return SearchCond.getLeafCond(idCond);
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
     * @param attrUtils USER / GROUP
     * @return where clauses to use to build the query
     */
    private Set<String> getWhereClause(final String expression, final String value, final AttributableUtils attrUtils) {
        final Parser parser = new Parser(new StringReader(expression));

        // Schema names
        final List<String> identifiers = new ArrayList<>();

        // Literals
        final List<String> literals = new ArrayList<>();

        // Get schema names and literals
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
            LOG.error("Ambiguous JEXL expression resolution.");
            throw new IllegalArgumentException("literals and values have different size");
        }

        // clauses to be used with INTERSECTed queries
        final Set<String> clauses = new HashSet<>();

        // builder to build the clauses
        final StringBuilder bld = new StringBuilder();

        // Contains used identifiers in order to avoid replications
        final Set<String> used = new HashSet<>();

        // Create several clauses: one for eanch identifiers
        for (int i = 0; i < identifiers.size(); i++) {
            if (!used.contains(identifiers.get(i))) {

                // verify schema existence and get schema type
                PlainSchema schema = plainSchemaDAO.find(identifiers.get(i), attrUtils.plainSchemaClass());
                if (schema == null) {
                    LOG.error("Invalid schema name '{}'", identifiers.get(i));
                    throw new IllegalArgumentException("Invalid schema name " + identifiers.get(i));
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

    protected abstract Subject<P, D, V> findInternal(Long key);

    private Query findByAttrValueQuery(final String entityName) {
        return entityManager.createQuery("SELECT e FROM " + entityName + " e"
                + " WHERE e.attribute.schema.name = :schemaName AND (e.stringValue IS NOT NULL"
                + " AND e.stringValue = :stringValue)"
                + " OR (e.booleanValue IS NOT NULL AND e.booleanValue = :booleanValue)"
                + " OR (e.dateValue IS NOT NULL AND e.dateValue = :dateValue)"
                + " OR (e.longValue IS NOT NULL AND e.longValue = :longValue)"
                + " OR (e.doubleValue IS NOT NULL AND e.doubleValue = :doubleValue)");
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<? extends Subject<P, D, V>> findByAttrValue(
            final String schemaName, final PlainAttrValue attrValue, final AttributableUtils attrUtils) {

        PlainSchema schema = plainSchemaDAO.find(schemaName, attrUtils.plainSchemaClass());
        if (schema == null) {
            LOG.error("Invalid schema name '{}'", schemaName);
            return Collections.<Subject<P, D, V>>emptyList();
        }

        final String entityName = schema.isUniqueConstraint()
                ? attrUtils.plainAttrUniqueValueClass().getName()
                : attrUtils.plainAttrValueClass().getName();

        Query query = findByAttrValueQuery(entityName);

        query.setParameter("schemaName", schemaName);
        query.setParameter("stringValue", attrValue.getStringValue());
        query.setParameter("booleanValue", attrValue.getBooleanValue() == null
                ? null
                : ((AbstractPlainAttrValue) attrValue).getBooleanAsInteger(attrValue.getBooleanValue()));
        if (attrValue.getDateValue() == null) {
            query.setParameter("dateValue", null);
        } else {
            query.setParameter("dateValue", attrValue.getDateValue(), TemporalType.TIMESTAMP);
        }
        query.setParameter("longValue", attrValue.getLongValue());
        query.setParameter("doubleValue", attrValue.getDoubleValue());

        List<Subject<P, D, V>> result = new ArrayList<>();
        for (PlainAttrValue value : (List<PlainAttrValue>) query.getResultList()) {
            Subject<P, D, V> subject = (Subject<P, D, V>) value.getAttr().getOwner();
            if (!result.contains(subject)) {
                result.add(subject);
            }
        }

        return result;
    }

    @Override
    public Subject<P, D, V> findByAttrUniqueValue(
            final String schemaName, final PlainAttrValue attrUniqueValue, final AttributableUtils attrUtils) {

        PlainSchema schema = plainSchemaDAO.find(schemaName, attrUtils.plainSchemaClass());
        if (schema == null) {
            LOG.error("Invalid schema name '{}'", schemaName);
            return null;
        }
        if (!schema.isUniqueConstraint()) {
            LOG.error("This schema has not unique constraint: '{}'", schemaName);
            return null;
        }

        List<? extends Subject<P, D, V>> result = findByAttrValue(schemaName, attrUniqueValue, attrUtils);
        return result.isEmpty()
                ? null
                : result.iterator().next();
    }

    /**
     * Find users / groups by derived attribute value. This method could fail if one or more string literals contained
     * into the derived attribute value provided derive from identifier (schema name) replacement. When you are going to
     * specify a derived attribute expression you must be quite sure that string literals used to build the expression
     * cannot be found into the attribute values used to replace attribute schema names used as identifiers.
     *
     * @param schemaName derived schema name
     * @param value derived attribute value
     * @param attrUtils AttributableUtil
     * @return list of users / groups
     */
    @Override
    public List<? extends Subject<P, D, V>> findByDerAttrValue(
            final String schemaName, final String value, final AttributableUtils attrUtils) {

        DerSchema schema = derSchemaDAO.find(schemaName, attrUtils.derSchemaClass());
        if (schema == null) {
            LOG.error("Invalid schema name '{}'", schemaName);
            return Collections.<Subject<P, D, V>>emptyList();
        }

        // query string
        final StringBuilder querystring = new StringBuilder();

        boolean subquery = false;
        for (String clause : getWhereClause(schema.getExpression(), value, attrUtils)) {
            if (querystring.length() > 0) {
                subquery = true;
                querystring.append(" AND a.owner_id IN ( ");
            }

            querystring.append("SELECT a.owner_id ").
                    append("FROM ").append(attrUtils.plainAttrClass().getSimpleName().substring(3)).append(" a, ").
                    append(attrUtils.plainAttrValueClass().getSimpleName().substring(3)).append(" v, ").
                    append(attrUtils.plainSchemaClass().getSimpleName().substring(3)).append(" s ").
                    append("WHERE ").append(clause);

            if (subquery) {
                querystring.append(')');
            }
        }

        LOG.debug("Execute query {}", querystring);

        final Query query = entityManager.createNativeQuery(querystring.toString());

        final List<Subject<P, D, V>> result = new ArrayList<>();
        for (Object userId : query.getResultList()) {
            Subject<P, D, V> subject = findInternal(Long.parseLong(userId.toString()));
            if (!result.contains(subject)) {
                result.add(subject);
            }
        }

        return result;
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<? extends Subject<P, D, V>> findByResource(
            final ExternalResource resource, final AttributableUtils attrUtils) {

        Query query = entityManager.createQuery(
                "SELECT e FROM " + attrUtils.attributableClass().getSimpleName() + " e "
                + "WHERE :resource MEMBER OF e.resources");
        query.setParameter("resource", resource);

        return query.getResultList();
    }
}
