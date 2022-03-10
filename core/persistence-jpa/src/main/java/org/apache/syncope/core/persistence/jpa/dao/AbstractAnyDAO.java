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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import org.apache.commons.jexl3.parser.Parser;
import org.apache.commons.jexl3.parser.ParserConstants;
import org.apache.commons.jexl3.parser.Token;
import org.apache.commons.lang3.StringUtils;
import org.apache.openjpa.persistence.OpenJPAPersistence;
import org.apache.syncope.core.persistence.api.dao.AllowedSchemas;
import org.apache.syncope.core.persistence.api.dao.AnyDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.search.AnyCond;
import org.apache.syncope.core.persistence.api.dao.search.AttrCond;
import org.apache.syncope.core.persistence.api.dao.search.SearchCond;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.persistence.api.entity.PlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.resource.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public abstract class AbstractAnyDAO<A extends Any<?>> extends AbstractDAO<A> implements AnyDAO<A> {

    protected final AnyUtilsFactory anyUtilsFactory;

    protected final ApplicationEventPublisher publisher;

    protected final PlainSchemaDAO plainSchemaDAO;

    protected final DerSchemaDAO derSchemaDAO;

    protected final DynRealmDAO dynRealmDAO;

    private AnyUtils anyUtils;

    public AbstractAnyDAO(
            final AnyUtilsFactory anyUtilsFactory,
            final ApplicationEventPublisher publisher,
            final PlainSchemaDAO plainSchemaDAO,
            final DerSchemaDAO derSchemaDAO,
            final DynRealmDAO dynRealmDAO) {

        this.anyUtilsFactory = anyUtilsFactory;
        this.publisher = publisher;
        this.plainSchemaDAO = plainSchemaDAO;
        this.derSchemaDAO = derSchemaDAO;
        this.dynRealmDAO = dynRealmDAO;
    }

    protected abstract AnyUtils init();

    protected AnyUtils anyUtils() {
        synchronized (this) {
            if (anyUtils == null) {
                anyUtils = init();
            }
        }
        return anyUtils;
    }

    protected String findKey(final String name, final String table) {
        Query query = entityManager().createNativeQuery(
                "SELECT id FROM " + table + " WHERE " + (JPAUser.TABLE.equals(table) ? "username" : "name") + "=?");
        query.setParameter(1, name);

        String key = null;

        for (Object resultKey : query.getResultList()) {
            key = resultKey instanceof Object[]
                    ? (String) ((Object[]) resultKey)[0]
                    : ((String) resultKey);
        }

        return key;
    }

    @SuppressWarnings("unchecked")
    protected List<String> findAllKeys(final String table, final int page, final int itemsPerPage) {
        Query query = entityManager().createNativeQuery(
                "SELECT id FROM " + table + " ORDER BY id", String.class);
        query.setFirstResult(itemsPerPage * (page <= 0 ? 0 : page - 1));
        query.setMaxResults(itemsPerPage);

        List<String> result = new ArrayList<>();
        query.getResultList().stream().map(resultKey -> resultKey instanceof Object[]
                ? (String) ((Object[]) resultKey)[0]
                : ((String) resultKey)).
                forEach(actualKey -> result.add(actualKey.toString()));
        return result;
    }

    protected OffsetDateTime findLastChange(final String key, final String table) {
        OffsetDateTime creationDate = null;
        OffsetDateTime lastChangeDate = null;

        try (Connection conn = (Connection) OpenJPAPersistence.cast(entityManager()).getConnection()) {
            try (PreparedStatement stmt =
                    conn.prepareStatement("SELECT creationDate, lastChangeDate FROM " + table + " WHERE id=?")) {
                stmt.setString(1, key);

                ResultSet rs = stmt.executeQuery();
                if (rs.next()) {
                    creationDate = rs.getObject(1, OffsetDateTime.class);
                    lastChangeDate = rs.getObject(2, OffsetDateTime.class);
                }
            }
        } catch (SQLException e) {
            LOG.error("While reading {} from {}", key, table, e);
        }

        return Optional.ofNullable(lastChangeDate).orElse(creationDate);
    }

    protected abstract void securityChecks(A any);

    @Transactional(readOnly = true)
    @Override
    public List<A> findByKeys(final List<String> keys) {
        Class<A> entityClass = anyUtils().anyClass();
        TypedQuery<A> query = entityManager().createQuery(
                "SELECT e FROM " + entityClass.getSimpleName() + " e WHERE e.id IN (:keys)", entityClass);
        query.setParameter("keys", keys);
        return query.getResultList();
    }

    @Transactional(readOnly = true)
    @Override
    public A authFind(final String key) {
        if (key == null) {
            throw new NotFoundException("Null key");
        }

        A any = find(key);
        if (any == null) {
            throw new NotFoundException(StringUtils.substringBefore(
                    StringUtils.substringAfter(getClass().getSimpleName(), "JPA"), "DAO") + ' ' + key);
        }

        securityChecks(any);

        return any;
    }

    @Transactional(readOnly = true)
    @Override
    @SuppressWarnings("unchecked")
    public A find(final String key) {
        return (A) entityManager().find(anyUtils().anyClass(), key);
    }

    private Query findByPlainAttrValueQuery(final String entityName, final boolean ignoreCaseMatch) {
        String query = "SELECT e FROM " + entityName + " e"
                + " WHERE e.attribute.schema.id = :schemaKey AND ((e.stringValue IS NOT NULL"
                + " AND "
                + (ignoreCaseMatch ? "LOWER(" : "") + "e.stringValue" + (ignoreCaseMatch ? ")" : "")
                + " = "
                + (ignoreCaseMatch ? "LOWER(" : "") + ":stringValue" + (ignoreCaseMatch ? ")" : "") + ')'
                + " OR (e.booleanValue IS NOT NULL AND e.booleanValue = :booleanValue)"
                + " OR (e.dateValue IS NOT NULL AND e.dateValue = :dateValue)"
                + " OR (e.longValue IS NOT NULL AND e.longValue = :longValue)"
                + " OR (e.doubleValue IS NOT NULL AND e.doubleValue = :doubleValue))";
        return entityManager().createQuery(query);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<A> findByPlainAttrValue(
            final PlainSchema schema,
            final PlainAttrValue attrValue,
            final boolean ignoreCaseMatch) {

        if (schema == null) {
            LOG.error("No PlainSchema");
            return List.of();
        }

        String entityName = schema.isUniqueConstraint()
                ? anyUtils().plainAttrUniqueValueClass().getName()
                : anyUtils().plainAttrValueClass().getName();
        Query query = findByPlainAttrValueQuery(entityName, ignoreCaseMatch);
        query.setParameter("schemaKey", schema.getKey());
        query.setParameter("stringValue", attrValue.getStringValue());
        query.setParameter("booleanValue", attrValue.getBooleanValue());
        if (attrValue.getDateValue() == null) {
            query.setParameter("dateValue", null);
        } else {
            query.setParameter("dateValue", attrValue.getDateValue().toInstant());
        }
        query.setParameter("longValue", attrValue.getLongValue());
        query.setParameter("doubleValue", attrValue.getDoubleValue());

        List<A> result = new ArrayList<>();
        ((List<PlainAttrValue>) query.getResultList()).stream().forEach(value -> {
            A any = (A) value.getAttr().getOwner();
            if (!result.contains(any)) {
                result.add(any);
            }
        });

        return result;
    }

    @Override
    public Optional<A> findByPlainAttrUniqueValue(
            final PlainSchema schema,
            final PlainAttrUniqueValue attrUniqueValue,
            final boolean ignoreCaseMatch) {

        if (schema == null) {
            LOG.error("No PlainSchema");
            return Optional.empty();
        }
        if (!schema.isUniqueConstraint()) {
            LOG.error("This schema has not unique constraint: '{}'", schema.getKey());
            return Optional.empty();
        }

        List<A> result = findByPlainAttrValue(schema, attrUniqueValue, ignoreCaseMatch);
        return result.isEmpty()
                ? Optional.empty()
                : Optional.of(result.get(0));
    }

    /**
     * Split an attribute value recurring on provided literals/tokens.
     *
     * @param attrValue value to be split
     * @param literals literals/tokens
     * @return split value
     */
    private static List<String> split(final String attrValue, final List<String> literals) {
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

    private Set<String> getWhereClause(final String expression, final String value, final boolean ignoreCaseMatch) {
        Parser parser = new Parser(expression);

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
        literals.sort((l1, l2) -> {
            if (l1 == null && l2 == null) {
                return 0;
            } else if (l1 != null && l2 == null) {
                return -1;
            } else if (l1 == null) {
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
            return Set.of();
        }

        // clauses to be used with INTERSECTed queries
        Set<String> clauses = new HashSet<>();

        // builder to build the clauses
        StringBuilder bld = new StringBuilder();

        // Contains used identifiers in order to avoid replications
        Set<String> used = new HashSet<>();

        // Create several clauses: one for each identifiers
        for (int i = 0; i < identifiers.size(); i++) {
            if (!used.contains(identifiers.get(i))) {
                // verify schema existence and get schema type
                PlainSchema schema = plainSchemaDAO.find(identifiers.get(i));
                if (schema == null) {
                    LOG.error("Invalid schema '{}', ignoring", identifiers.get(i));
                } else {
                    // clear builder
                    bld.delete(0, bld.length());

                    bld.append('(');

                    // set schema key
                    bld.append("s.id = '").append(identifiers.get(i)).append('\'');

                    bld.append(" AND ");

                    bld.append("s.id = a.schema_id").append(" AND ");

                    bld.append("a.id = v.attribute_id");

                    bld.append(" AND ");

                    // use a value clause different for each different schema type
                    switch (schema.getType()) {
                        case Boolean:
                            bld.append("v.booleanValue = '").append(attrValues.get(i)).append('\'');
                            break;
                        case Long:
                            bld.append("v.longValue = ").append(attrValues.get(i));
                            break;
                        case Double:
                            bld.append("v.doubleValue = ").append(attrValues.get(i));
                            break;
                        case Date:
                            bld.append("v.dateValue = '").append(attrValues.get(i)).append('\'');
                            break;
                        default:
                            if (ignoreCaseMatch) {
                                bld.append("LOWER(v.stringValue) = '").
                                        append(attrValues.get(i).toLowerCase()).append('\'');
                            } else {
                                bld.append("v.stringValue = '").
                                        append(attrValues.get(i)).append('\'');
                            }
                    }

                    bld.append(')');

                    used.add(identifiers.get(i));

                    clauses.add(bld.toString());
                }
            }
        }

        LOG.debug("Generated where clauses {}", clauses);

        return clauses;
    }

    @Override
    public List<A> findByDerAttrValue(final DerSchema schema, final String value, final boolean ignoreCaseMatch) {
        if (schema == null) {
            LOG.error("No DerSchema");
            return List.of();
        }

        // query string
        StringBuilder querystring = new StringBuilder();

        boolean subquery = false;
        for (String clause : getWhereClause(schema.getExpression(), value, ignoreCaseMatch)) {
            if (querystring.length() > 0) {
                subquery = true;
                querystring.append(" AND a.owner_id IN ( ");
            }

            querystring.append("SELECT a.owner_id ").
                    append("FROM ").append(anyUtils().plainAttrClass().getSimpleName().substring(3)).append(" a, ").
                    append(anyUtils().plainAttrValueClass().getSimpleName().substring(3)).append(" v, ").
                    append(PlainSchema.class.getSimpleName()).append(" s ").
                    append("WHERE ").append(clause);

            if (subquery) {
                querystring.append(')');
            }
        }

        List<A> result = new ArrayList<>();
        if (querystring.length() > 0) {
            Query query = entityManager().createNativeQuery(querystring.toString());

            for (Object anyKey : query.getResultList()) {
                A any = find(anyKey.toString());
                if (!result.contains(any)) {
                    result.add(any);
                }
            }
        }

        return result;
    }

    @SuppressWarnings("unchecked")
    @Override
    public List<A> findByResource(final ExternalResource resource) {
        Query query = entityManager().createQuery("SELECT e FROM " + anyUtils().anyClass().getSimpleName() + " e "
                + "WHERE :resource MEMBER OF e.resources");
        query.setParameter("resource", resource);

        return query.getResultList();
    }

    @Override
    public SearchCond getAllMatchingCond() {
        AnyCond idCond = new AnyCond(AttrCond.Type.ISNOTNULL);
        idCond.setSchema("id");
        return SearchCond.getLeaf(idCond);
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

        typeOwnClasses.forEach(typeClass -> {
            if (reference.equals(PlainSchema.class)) {
                result.getForSelf().addAll((Collection<? extends S>) typeClass.getPlainSchemas());
            } else if (reference.equals(DerSchema.class)) {
                result.getForSelf().addAll((Collection<? extends S>) typeClass.getDerSchemas());
            } else if (reference.equals(VirSchema.class)) {
                result.getForSelf().addAll((Collection<? extends S>) typeClass.getVirSchemas());
            }
        });

        // schemas given by type extensions
        Map<Group, List<? extends AnyTypeClass>> typeExtensionClasses = new HashMap<>();
        if (any instanceof User) {
            ((User) any).getMemberships().forEach(memb -> memb.getRightEnd().getTypeExtensions().
                    forEach(typeExt -> typeExtensionClasses.put(memb.getRightEnd(), typeExt.getAuxClasses())));
        } else if (any instanceof AnyObject) {
            ((AnyObject) any).getMemberships().forEach(memb -> memb.getRightEnd().getTypeExtensions().stream().
                    filter(typeExt -> any.getType().equals(typeExt.getAnyType())).
                    forEach(typeExt -> typeExtensionClasses.put(memb.getRightEnd(), typeExt.getAuxClasses())));
        }

        typeExtensionClasses.entrySet().stream().map(entry -> {
            result.getForMemberships().put(entry.getKey(), new HashSet<>());
            return entry;
        }).forEach(entry -> entry.getValue().forEach(typeClass -> {
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
        }));

        return result;
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

    @Transactional(readOnly = true)
    @Override
    @SuppressWarnings("unchecked")
    public List<String> findDynRealms(final String key) {
        Query query = entityManager().createNativeQuery(
                "SELECT dynRealm_id FROM " + JPADynRealmDAO.DYNMEMB_TABLE + " WHERE any_id=?");
        query.setParameter(1, key);

        List<String> result = new ArrayList<>();
        query.getResultList().stream().map(resultKey -> resultKey instanceof Object[]
                ? (String) ((Object[]) resultKey)[0]
                : ((String) resultKey)).
                forEach((actualKey) -> {
                    DynRealm dynRealm = dynRealmDAO.find(actualKey.toString());
                    if (dynRealm == null) {
                        LOG.error("Could not find dynRealm with id {}, even though returned by the native query",
                                actualKey);
                    } else if (!result.contains(actualKey.toString())) {
                        result.add(actualKey.toString());
                    }
                });
        return result;
    }
}
