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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.persistence.NoResultException;
import javax.persistence.Query;
import javax.persistence.TemporalType;
import org.apache.commons.jexl3.parser.Parser;
import org.apache.commons.jexl3.parser.ParserConstants;
import org.apache.commons.jexl3.parser.Token;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.common.lib.SyncopeConstants;
import org.apache.syncope.core.persistence.api.dao.AllowedSchemas;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.AnySearchDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttributeCond;
import org.apache.syncope.core.persistence.api.dao.search.OrderByClause;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.anyobject.AMembership;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.group.TypeExtension;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.user.UMembership;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.AbstractPlainAttrValue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public abstract class AbstractAnyDAO<A extends Any<?>> extends AbstractDAO<A> implements AnyDAO<A> {

    @Autowired
    protected PlainSchemaDAO plainSchemaDAO;

    @Autowired
    protected DerSchemaDAO derSchemaDAO;

    @Autowired
    protected AnySearchDAO searchDAO;

    protected AnyUtils anyUtils;

    protected abstract AnyUtils init();

    protected AnyUtils getAnyUtils() {
        synchronized (this) {
            if (anyUtils == null) {
                anyUtils = init();
            }
        }
        return anyUtils;
    }

    protected abstract void securityChecks(A any);

    @Transactional(readOnly = true)
    @Override
    public A authFind(final String key) {
        if (key == null) {
            throw new NotFoundException("Null key");
        }

        A any = find(key);
        if (any == null) {
            throw new NotFoundException(StringUtils.substringBefore(
                    StringUtils.substringAfter(getClass().getSimpleName(), "JPA"), "DAO") + " " + key);
        }

        securityChecks(any);

        return any;
    }

    @Transactional(readOnly = true)
    @Override
    @SuppressWarnings("unchecked")
    public A find(final String key) {
        return (A) entityManager().find(getAnyUtils().anyClass(), key);
    }

    @SuppressWarnings("unchecked")
    @Override
    public A findByWorkflowId(final String workflowId) {
        Query query = entityManager().createQuery("SELECT e FROM " + getAnyUtils().anyClass().getSimpleName()
                + " e WHERE e.workflowId = :workflowId", User.class);
        query.setParameter("workflowId", workflowId);

        A result = null;
        try {
            result = (A) query.getSingleResult();
        } catch (NoResultException e) {
            LOG.debug("No user found with workflow id {}", workflowId, e);
        }

        return result;
    }

    private Query findByAttrValueQuery(final String entityName) {
        return entityManager().createQuery("SELECT e FROM " + entityName + " e"
                + " WHERE e.attribute.schema.id = :schemaKey AND (e.stringValue IS NOT NULL"
                + " AND e.stringValue = :stringValue)"
                + " OR (e.booleanValue IS NOT NULL AND e.booleanValue = :booleanValue)"
                + " OR (e.dateValue IS NOT NULL AND e.dateValue = :dateValue)"
                + " OR (e.longValue IS NOT NULL AND e.longValue = :longValue)"
                + " OR (e.doubleValue IS NOT NULL AND e.doubleValue = :doubleValue)");
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<A> findByAttrValue(final String schemaKey, final PlainAttrValue attrValue) {
        PlainSchema schema = plainSchemaDAO.find(schemaKey);
        if (schema == null) {
            LOG.error("Invalid schema name '{}'", schemaKey);
            return Collections.<A>emptyList();
        }

        String entityName = schema.isUniqueConstraint()
                ? getAnyUtils().plainAttrUniqueValueClass().getName()
                : getAnyUtils().plainAttrValueClass().getName();
        Query query = findByAttrValueQuery(entityName);
        query.setParameter("schemaKey", schemaKey);
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

        List<A> result = new ArrayList<>();
        for (PlainAttrValue value : (List<PlainAttrValue>) query.getResultList()) {
            A any = (A) value.getAttr().getOwner();
            if (!result.contains(any)) {
                result.add(any);
            }
        }

        return result;
    }

    @Override
    public A findByAttrUniqueValue(final String schemaKey, final PlainAttrValue attrUniqueValue) {
        PlainSchema schema = plainSchemaDAO.find(schemaKey);
        if (schema == null) {
            LOG.error("Invalid schema name '{}'", schemaKey);
            return null;
        }
        if (!schema.isUniqueConstraint()) {
            LOG.error("This schema has not unique constraint: '{}'", schemaKey);
            return null;
        }

        List<A> result = findByAttrValue(schemaKey, attrUniqueValue);
        return result.isEmpty()
                ? null
                : result.iterator().next();
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

    /**
     * Generate one where clause for each different attribute schema into the derived schema expression provided.
     *
     * @param expression derived schema expression
     * @param value derived attribute value
     * @param attrUtils USER / GROUP
     * @return where clauses to use to build the query
     */
    private Set<String> getWhereClause(final String expression, final String value) {
        Parser parser = new Parser(new StringReader(expression));

        // Schema names
        List<String> identifiers = new ArrayList<>();

        // Literals
        List<String> literals = new ArrayList<>();

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
        List<String> attrValues = split(value, literals);

        if (attrValues.size() != identifiers.size()) {
            LOG.error("Ambiguous JEXL expression resolution.");
            throw new IllegalArgumentException("literals and values have different size");
        }

        // clauses to be used with INTERSECTed queries
        Set<String> clauses = new HashSet<>();

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
                    LOG.error("Invalid schema id '{}'", identifiers.get(i));
                    throw new IllegalArgumentException("Invalid schema id " + identifiers.get(i));
                }

                // clear builder
                bld.delete(0, bld.length());

                bld.append("(");

                // set schema name
                bld.append("s.id = '").append(identifiers.get(i)).append("'");

                bld.append(" AND ");

                bld.append("s.id = a.schema_id").append(" AND ");

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

    @Override
    public List<A> findByDerAttrValue(final String schemaKey, final String value) {
        DerSchema schema = derSchemaDAO.find(schemaKey);
        if (schema == null) {
            LOG.error("Invalid schema name '{}'", schemaKey);
            return Collections.<A>emptyList();
        }

        // query string
        StringBuilder querystring = new StringBuilder();

        boolean subquery = false;
        for (String clause : getWhereClause(schema.getExpression(), value)) {
            if (querystring.length() > 0) {
                subquery = true;
                querystring.append(" AND a.owner_id IN ( ");
            }

            querystring.append("SELECT a.owner_id ").
                    append("FROM ").append(getAnyUtils().plainAttrClass().getSimpleName().substring(3)).append(" a, ").
                    append(getAnyUtils().plainAttrValueClass().getSimpleName().substring(3)).append(" v, ").
                    append(PlainSchema.class.getSimpleName()).append(" s ").
                    append("WHERE ").append(clause);

            if (subquery) {
                querystring.append(')');
            }
        }

        Query query = entityManager().createNativeQuery(querystring.toString());

        List<A> result = new ArrayList<>();
        for (Object anyKey : query.getResultList()) {
            A any = find(anyKey.toString());
            if (!result.contains(any)) {
                result.add(any);
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<A> findByResource(final ExternalResource resource) {
        Query query = entityManager().createQuery(
                "SELECT e FROM " + getAnyUtils().anyClass().getSimpleName() + " e "
                + "WHERE :resource MEMBER OF e.resources");
        query.setParameter("resource", resource);

        return query.getResultList();
    }

    @Override
    public final List<A> findAll() {
        return findAll(SyncopeConstants.FULL_ADMIN_REALMS, -1, -1, Collections.<OrderByClause>emptyList());
    }

    private SearchCond getAllMatchingCond() {
        AnyCond idCond = new AnyCond(AttributeCond.Type.ISNOTNULL);
        idCond.setSchema("id");
        return SearchCond.getLeafCond(idCond);
    }

    @Override
    public List<A> findAll(final Set<String> adminRealms,
            final int page, final int itemsPerPage, final List<OrderByClause> orderBy) {

        return searchDAO.search(adminRealms, getAllMatchingCond(), page, itemsPerPage, orderBy,
                getAnyUtils().getAnyTypeKind());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    @SuppressWarnings("unchecked")
    public <S extends Schema> AllowedSchemas<S> findAllowedSchemas(final A any, final Class<S> reference) {
        AllowedSchemas<S> result = new AllowedSchemas<>();

        // schemas given by type and aux classes
        Set<AnyTypeClass> typeOwnClasses = new HashSet<>();
        typeOwnClasses.addAll(any.getType().getClasses());
        typeOwnClasses.addAll(any.getAuxClasses());

        for (AnyTypeClass typeClass : typeOwnClasses) {
            if (reference.equals(PlainSchema.class)) {
                result.getForSelf().addAll((Collection<? extends S>) typeClass.getPlainSchemas());
            } else if (reference.equals(DerSchema.class)) {
                result.getForSelf().addAll((Collection<? extends S>) typeClass.getDerSchemas());
            } else if (reference.equals(VirSchema.class)) {
                result.getForSelf().addAll((Collection<? extends S>) typeClass.getVirSchemas());
            }
        }

        // schemas given by type extensions
        Map<Group, List<? extends AnyTypeClass>> typeExtensionClasses = new HashMap<>();
        if (any instanceof User) {
            for (UMembership memb : ((User) any).getMemberships()) {
                for (TypeExtension typeExtension : memb.getRightEnd().getTypeExtensions()) {
                    typeExtensionClasses.put(memb.getRightEnd(), typeExtension.getAuxClasses());
                }
            }
        } else if (any instanceof AnyObject) {
            for (AMembership memb : ((AnyObject) any).getMemberships()) {
                for (TypeExtension typeExtension : memb.getRightEnd().getTypeExtensions()) {
                    if (any.getType().equals(typeExtension.getAnyType())) {
                        typeExtensionClasses.put(memb.getRightEnd(), typeExtension.getAuxClasses());
                    }
                }
            }
        }

        for (Map.Entry<Group, List<? extends AnyTypeClass>> entry : typeExtensionClasses.entrySet()) {
            result.getForMemberships().put(entry.getKey(), new HashSet<S>());
            for (AnyTypeClass typeClass : entry.getValue()) {
                if (reference.equals(PlainSchema.class)) {
                    result.getForMemberships().get(entry.getKey()).
                            addAll((Collection<? extends S>) typeClass.getPlainSchemas());
                } else if (reference.equals(DerSchema.class)) {
                    result.getForMemberships().get(entry.getKey()).
                            addAll((Collection<? extends S>) typeClass.getDerSchemas());
                } else if (reference.equals(VirSchema.class)) {
                    result.getForMemberships().get(entry.getKey()).
                            addAll((Collection<? extends S>) typeClass.getVirSchemas());
                }
            }
        }

        return result;
    }

    @Override
    public final int count(final Set<String> adminRealms) {
        return searchDAO.count(adminRealms, getAllMatchingCond(), getAnyUtils().getAnyTypeKind());
    }

    @Override
    public A save(final A any) {
        return entityManager().merge(any);
    }

    @Override
    public void delete(final String key) {
        A any = find(key);
        if (any == null) {
            return;
        }

        delete(any);
    }
}
