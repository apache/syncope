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
package org.apache.syncope.core.persistence.neo4j.dao.repo;

import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.cache.Cache;
import org.apache.commons.jexl3.parser.Parser;
import org.apache.commons.jexl3.parser.ParserConstants;
import org.apache.commons.jexl3.parser.Token;
import org.apache.commons.lang3.StringUtils;
import org.apache.syncope.core.persistence.api.dao.AllowedSchemas;
import org.apache.syncope.core.persistence.api.dao.AnyTypeClassDAO;
import org.apache.syncope.core.persistence.api.dao.AnyTypeDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.VirSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyType;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.ExternalResource;
import org.apache.syncope.core.persistence.api.entity.PlainAttr;
import org.apache.syncope.core.persistence.api.entity.PlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.neo4j.dao.AbstractDAO;
import org.apache.syncope.core.persistence.neo4j.entity.AbstractAny;
import org.apache.syncope.core.persistence.neo4j.entity.EntityCacheKey;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jDynRealm;
import org.apache.syncope.core.persistence.neo4j.entity.Neo4jExternalResource;
import org.apache.syncope.core.provisioning.api.serialization.POJOHelper;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.springframework.data.neo4j.core.Neo4jClient;
import org.springframework.data.neo4j.core.Neo4jTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public abstract class AbstractAnyRepoExt<A extends Any<?>, N extends AbstractAny<?>>
        extends AbstractDAO implements AnyRepoExt<A> {

    /**
     * Split an attribute value recurring on provided literals/tokens.
     *
     * @param attrValue value to be split
     * @param literals literals/tokens
     * @return split value
     */
    protected static List<String> split(final String attrValue, final List<String> literals) {
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

    protected final AnyTypeDAO anyTypeDAO;

    protected final AnyTypeClassDAO anyTypeClassDAO;

    protected final PlainSchemaDAO plainSchemaDAO;

    protected final DerSchemaDAO derSchemaDAO;

    protected final VirSchemaDAO virSchemaDAO;

    protected final DynRealmDAO dynRealmDAO;

    protected final AnyUtils anyUtils;

    protected AbstractAnyRepoExt(
            final AnyTypeDAO anyTypeDAO,
            final AnyTypeClassDAO anyTypeClassDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final DerSchemaDAO derSchemaDAO,
            final VirSchemaDAO virSchemaDAO,
            final DynRealmDAO dynRealmDAO,
            final AnyUtils anyUtils,
            final Neo4jTemplate neo4jTemplate,
            final Neo4jClient neo4jClient) {

        super(neo4jTemplate, neo4jClient);
        this.anyTypeDAO = anyTypeDAO;
        this.anyTypeClassDAO = anyTypeClassDAO;
        this.plainSchemaDAO = plainSchemaDAO;
        this.derSchemaDAO = derSchemaDAO;
        this.virSchemaDAO = virSchemaDAO;
        this.dynRealmDAO = dynRealmDAO;
        this.anyUtils = anyUtils;
    }

    protected abstract Cache<EntityCacheKey, N> cache();

    @Override
    public List<A> findByKeys(final List<String> keys) {
        return toList(neo4jClient.query(
                "MATCH (n:" + AnyRepoExt.node(anyUtils.anyTypeKind()) + ") WHERE n.id IN $keys RETURN n.id").
                bindAll(Map.of("keys", keys)).fetch().all(),
                "n.id",
                anyUtils.anyClass(),
                cache());
    }

    protected Optional<OffsetDateTime> findLastChange(final String key, final String node) {
        return neo4jClient.query("MATCH (n:" + node + " {id: $id}) "
                + "RETURN n.creationDate, n.lastChangeDate").
                bindAll(Map.of("id", key)).fetch().one().map(n -> {
            ZonedDateTime creationDate = (ZonedDateTime) n.get("n.creationDate");
            ZonedDateTime lastChangeDate = (ZonedDateTime) n.get("n.lastChangeDate");
            return Optional.ofNullable(lastChangeDate).orElse(creationDate).toOffsetDateTime();
        });
    }

    protected abstract void securityChecks(A any);

    @SuppressWarnings("unchecked")
    protected Optional<A> findById(final String key) {
        return findById(key, anyUtils.anyClass(), cache()).map(any -> (A) any);
    }

    @Transactional(readOnly = true)
    @Override
    public A authFind(final String key) {
        if (key == null) {
            throw new NotFoundException("Null key");
        }

        A any = findById(key).orElseThrow(() -> new NotFoundException(anyUtils.anyTypeKind().name() + ' ' + key));

        securityChecks(any);

        return any;
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

        PlainAttr<?> attr = anyUtils.newPlainAttr();
        attr.setSchema(schema);
        if (attrValue instanceof PlainAttrUniqueValue plainAttrUniqueValue) {
            attr.setUniqueValue(plainAttrUniqueValue);
        } else {
            attr.add(attrValue);
        }

        String op;
        Map<String, Object> parameters;
        if (ignoreCaseMatch) {
            op = "=~";
            parameters = Map.of("value", "(?i)" + AnyRepoExt.escapeForLikeRegex(POJOHelper.serialize(attr)));
        } else {
            op = "=";
            parameters = Map.of("value", POJOHelper.serialize(attr));
        }
        return toList(
                neo4jClient.query(
                        "MATCH (n:" + AnyRepoExt.node(anyUtils.anyTypeKind()) + ") "
                        + "WHERE n.`plainAttrs." + schema.getKey() + "` " + op + " $value RETURN n.id").
                        bindAll(parameters).fetch().all(),
                "n.id",
                anyUtils.anyClass(),
                cache());
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

    @Override
    public List<A> findByDerAttrValue(final DerSchema derSchema, final String value, final boolean ignoreCaseMatch) {
        if (derSchema == null) {
            LOG.error("No DerSchema");
            return List.of();
        }

        Parser parser = new Parser(derSchema.getExpression());

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
            return List.of();
        }

        // Contains used identifiers in order to avoid replications
        Set<String> used = new HashSet<>();

        // Create several clauses: one for eanch identifiers
        List<String> clauses = new ArrayList<>();
        Map<String, Object> parameters = new HashMap<>();
        for (int i = 0; i < identifiers.size(); i++) {
            if (!used.contains(identifiers.get(i))) {
                // verify schema existence and get schema type
                PlainSchema schema = plainSchemaDAO.findById(identifiers.get(i)).orElse(null);

                if (schema == null) {
                    LOG.error("Invalid schema '{}', ignoring", identifiers.get(i));
                } else {
                    PlainAttr<?> attr = anyUtils.newPlainAttr();
                    attr.setSchema(schema);
                    if (schema.isUniqueConstraint()) {
                        PlainAttrUniqueValue attrValue = anyUtils.newPlainAttrUniqueValue();
                        attrValue.setStringValue(attrValues.get(i));
                        attr.setUniqueValue(attrValue);
                    } else {
                        PlainAttrValue attrValue = anyUtils.newPlainAttrValue();
                        attrValue.setStringValue(attrValues.get(i));
                        attr.add(attrValue);
                    }

                    String op;
                    if (ignoreCaseMatch) {
                        op = "=~";
                        parameters.put(
                                identifiers.get(i),
                                "(?i)" + AnyRepoExt.escapeForLikeRegex(POJOHelper.serialize(attr)));
                    } else {
                        op = "=";
                        parameters.put(identifiers.get(i), POJOHelper.serialize(attr));
                    }
                    clauses.add("n.`plainAttrs." + schema.getKey() + "` " + op + " $" + identifiers.get(i));

                    used.add(identifiers.get(i));
                }
            }
        }

        LOG.debug("Generated where clauses {}", clauses);

        return toList(
                neo4jClient.query(
                        "MATCH (n:" + AnyRepoExt.node(anyUtils.anyTypeKind()) + ") "
                        + "WHERE " + clauses.stream().collect(Collectors.joining(" AND "))
                        + " RETURN n.id").
                        bindAll(parameters).fetch().all(),
                "n.id",
                anyUtils.anyClass(),
                cache());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, readOnly = true)
    @Override
    @SuppressWarnings("unchecked")
    public <S extends Schema> AllowedSchemas<S> findAllowedSchemas(final A any, final Class<S> reference) {
        AllowedSchemas<S> result = new AllowedSchemas<>();

        // schemas given by type and aux classes
        Set<AnyTypeClass> anyTypeOwnClasses = new HashSet<>();
        AnyType anyType = anyTypeDAO.findById(any.getType().getKey()).
                orElseThrow(() -> new NotFoundException("AnyType " + any.getType().getKey()));
        anyTypeOwnClasses.addAll(anyType.getClasses());
        any.getAuxClasses().forEach(atc -> {
            AnyTypeClass anyTypeClass = anyTypeClassDAO.findById(atc.getKey()).
                    orElseThrow(() -> new NotFoundException("AnyTypeClass " + atc.getKey()));
            anyTypeOwnClasses.add(anyTypeClass);
        });

        anyTypeOwnClasses.forEach(atc -> {
            if (reference.equals(PlainSchema.class)) {
                atc.getPlainSchemas().stream().
                        map(schema -> plainSchemaDAO.findById(schema.getKey())).
                        flatMap(Optional::stream).
                        forEach(schema -> result.getForSelf().add((S) schema));
            } else if (reference.equals(DerSchema.class)) {
                atc.getDerSchemas().stream().
                        map(schema -> derSchemaDAO.findById(schema.getKey())).
                        flatMap(Optional::stream).
                        forEach(schema -> result.getForSelf().add((S) schema));
            } else if (reference.equals(VirSchema.class)) {
                atc.getVirSchemas().stream().
                        map(schema -> virSchemaDAO.findById(schema.getKey())).
                        flatMap(Optional::stream).
                        forEach(schema -> result.getForSelf().add((S) schema));
            }
        });

        // schemas given by type extensions
        Map<Group, Set<AnyTypeClass>> typeExtensionClasses = new HashMap<>();
        switch (any) {
            case User user ->
                user.getMemberships().forEach(memb -> memb.getRightEnd().getTypeExtensions().forEach(typeExt -> {
                    Set<AnyTypeClass> typeExtClasses = new HashSet<>();
                    typeExt.getAuxClasses().forEach(atc -> {
                        AnyTypeClass anyTypeClass = anyTypeClassDAO.findById(atc.getKey()).
                                orElseThrow(() -> new NotFoundException("AnyTypeClass " + atc.getKey()));
                        typeExtClasses.add(anyTypeClass);
                    });

                    typeExtensionClasses.put(memb.getRightEnd(), typeExtClasses);
                }));

            case AnyObject anyObject ->
                anyObject.getMemberships().forEach(memb -> memb.getRightEnd().getTypeExtensions().stream().
                        filter(typeExt -> any.getType().equals(typeExt.getAnyType())).forEach(typeExt -> {

                    Set<AnyTypeClass> typeExtClasses = new HashSet<>();
                    typeExt.getAuxClasses().forEach(atc -> {
                        AnyTypeClass anyTypeClass = anyTypeClassDAO.findById(atc.getKey()).
                                orElseThrow(() -> new NotFoundException("AnyTypeClass " + atc.getKey()));
                        typeExtClasses.add(anyTypeClass);
                    });

                    typeExtensionClasses.put(memb.getRightEnd(), typeExtClasses);
                }));

            default -> {
            }
        }

        typeExtensionClasses.entrySet().stream().map(entry -> {
            result.getForMemberships().put(entry.getKey(), new HashSet<>());
            return entry;
        }).forEach(entry -> entry.getValue().forEach(atc -> {
            if (reference.equals(PlainSchema.class)) {
                atc.getPlainSchemas().stream().
                        map(schema -> plainSchemaDAO.findById(schema.getKey())).
                        flatMap(Optional::stream).
                        forEach(schema -> result.getForMemberships().get(entry.getKey()).add((S) schema));
            } else if (reference.equals(DerSchema.class)) {
                atc.getDerSchemas().stream().
                        map(schema -> derSchemaDAO.findById(schema.getKey())).
                        flatMap(Optional::stream).
                        forEach(schema -> result.getForMemberships().get(entry.getKey()).add((S) schema));
            } else if (reference.equals(VirSchema.class)) {
                atc.getVirSchemas().stream().
                        map(schema -> virSchemaDAO.findById(schema.getKey())).
                        flatMap(Optional::stream).
                        forEach(schema -> result.getForMemberships().get(entry.getKey()).add((S) schema));
            }
        }));

        return result;
    }

    @Transactional(readOnly = true)
    @Override
    public List<String> findDynRealms(final String key) {
        return neo4jClient.query(
                "MATCH (n {id: $id})-"
                + "[:" + DynRealmRepoExt.DYN_REALM_MEMBERSHIP_REL + "]-"
                + "(p:" + Neo4jDynRealm.NODE + ") "
                + "RETURN p.id").
                bindAll(Map.of("id", key)).fetch().all().stream().
                map(found -> found.get("p.id").toString()).distinct().toList();
    }

    @Override
    public List<A> findByResourcesContaining(final ExternalResource resource) {
        return findByRelationship(
                AnyRepoExt.node(anyUtils.anyTypeKind()),
                Neo4jExternalResource.NODE,
                resource.getKey(),
                anyUtils.anyClass(),
                cache());
    }

    protected void checkBeforeSave(final A any) {
        // check UNIQUE constraints
        any.getPlainAttrs().stream().filter(attr -> attr.getUniqueValue() != null).forEach(attr -> {
            Optional<A> other = findByPlainAttrUniqueValue(attr.getSchema(), attr.getUniqueValue(), false);
            if (other.isEmpty() || other.get().getKey().equals(any.getKey())) {
                LOG.debug("No duplicate value found for {}={}",
                        attr.getSchema().getKey(), attr.getUniqueValue().getValueAsString());
            } else {
                throw new DuplicateException("Duplicate value found for "
                        + attr.getSchema().getKey() + "=" + attr.getUniqueValue().getValueAsString());
            }
        });

        // update sysInfo
        OffsetDateTime now = OffsetDateTime.now();
        String who = AuthContextUtils.getWho();
        LOG.debug("Set last change date '{}' and modifier '{}' for '{}'", now, who, any);
        any.setLastModifier(who);
        any.setLastChangeDate(now);
    }

    @Override
    public void deleteById(final String key) {
        findById(key).ifPresent(this::delete);
    }
}
