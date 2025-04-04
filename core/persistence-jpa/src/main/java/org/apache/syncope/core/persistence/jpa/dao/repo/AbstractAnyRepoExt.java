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
package org.apache.syncope.core.persistence.jpa.dao.repo;

import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.sql.DataSource;
import org.apache.openjpa.persistence.OpenJPAEntityManagerFactorySPI;
import org.apache.syncope.core.persistence.api.dao.AllowedSchemas;
import org.apache.syncope.core.persistence.api.dao.DuplicateException;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.NotFoundException;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.entity.Any;
import org.apache.syncope.core.persistence.api.entity.AnyTypeClass;
import org.apache.syncope.core.persistence.api.entity.AnyUtils;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.DynRealm;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.api.entity.Schema;
import org.apache.syncope.core.persistence.api.entity.VirSchema;
import org.apache.syncope.core.persistence.api.entity.anyobject.AnyObject;
import org.apache.syncope.core.persistence.api.entity.group.Group;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.common.dao.AnyFinder;
import org.apache.syncope.core.persistence.jpa.entity.AbstractAttributable;
import org.apache.syncope.core.persistence.jpa.entity.anyobject.JPAAnyObject;
import org.apache.syncope.core.persistence.jpa.entity.group.JPAGroup;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

public abstract class AbstractAnyRepoExt<A extends Any> implements AnyRepoExt<A> {

    protected static final Logger LOG = LoggerFactory.getLogger(AnyRepoExt.class);

    protected final DynRealmDAO dynRealmDAO;

    protected final PlainSchemaDAO plainSchemaDAO;

    protected final EntityManager entityManager;

    protected final AnyFinder anyFinder;

    protected final AnyUtils anyUtils;

    protected final String table;

    protected AbstractAnyRepoExt(
            final DynRealmDAO dynRealmDAO,
            final PlainSchemaDAO plainSchemaDAO,
            final EntityManager entityManager,
            final AnyFinder anyFinder,
            final AnyUtils anyUtils) {

        this.dynRealmDAO = dynRealmDAO;
        this.plainSchemaDAO = plainSchemaDAO;
        this.entityManager = entityManager;
        this.anyFinder = anyFinder;
        this.anyUtils = anyUtils;
        switch (anyUtils.anyTypeKind()) {
            case ANY_OBJECT:
                table = JPAAnyObject.TABLE;
                break;

            case GROUP:
                table = JPAGroup.TABLE;
                break;

            case USER:
            default:
                table = JPAUser.TABLE;
        }
    }

    @Transactional(readOnly = true)
    @Override
    public Optional<OffsetDateTime> findLastChange(final String key) {
        OpenJPAEntityManagerFactorySPI emf = entityManager.getEntityManagerFactory().
                unwrap(OpenJPAEntityManagerFactorySPI.class);
        return new JdbcTemplate((DataSource) emf.getConfiguration().getConnectionFactory()).query(
                "SELECT creationDate, lastChangeDate FROM " + table + " WHERE id=?",
                rs -> {
                    if (rs.next()) {
                        OffsetDateTime creationDate = rs.getObject(1, OffsetDateTime.class);
                        OffsetDateTime lastChangeDate = rs.getObject(2, OffsetDateTime.class);
                        return Optional.ofNullable(lastChangeDate).or(() -> Optional.ofNullable(creationDate));
                    }

                    return Optional.empty();
                },
                key);
    }

    protected abstract void securityChecks(A any);

    @SuppressWarnings("unchecked")
    protected Optional<A> findById(final String key) {
        return Optional.ofNullable((A) entityManager.find(anyUtils.anyClass(), key));
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
    public List<A> findByDerAttrValue(final String expression, final String value, final boolean ignoreCaseMatch) {
        return anyFinder.findByDerAttrValue(anyUtils.anyTypeKind(), expression, value, ignoreCaseMatch);
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
        switch (any) {
            case User user ->
                user.getMemberships().forEach(memb -> memb.getRightEnd().getTypeExtensions().
                        forEach(typeExt -> typeExtensionClasses.put(memb.getRightEnd(), typeExt.getAuxClasses())));
            case AnyObject anyObject ->
                anyObject.getMemberships().forEach(memb -> memb.getRightEnd().getTypeExtensions().stream().
                        filter(typeExt -> any.getType().equals(typeExt.getAnyType())).
                        forEach(typeExt -> typeExtensionClasses.put(memb.getRightEnd(), typeExt.getAuxClasses())));
            default -> {
            }
        }

        typeExtensionClasses.entrySet().stream().peek(
                entry -> result.getForMemberships().put(entry.getKey(), new HashSet<>())).
                forEach(entry -> entry.getValue().forEach(typeClass -> {
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

    @Transactional(readOnly = true)
    @Override
    public List<String> findDynRealms(final String key) {
        Query query = entityManager.createNativeQuery(
                "SELECT dynRealm_id FROM " + DynRealmRepoExt.DYNMEMB_TABLE + " WHERE any_id=?");
        query.setParameter(1, key);

        @SuppressWarnings("unchecked")
        List<Object> result = query.getResultList();
        return result.stream().
                map(dynRealm -> dynRealmDAO.findById(dynRealm.toString())).
                flatMap(Optional::stream).
                map(DynRealm::getKey).
                distinct().
                toList();
    }

    protected <T extends AbstractAttributable> void checkBeforeSave(final T attributable) {
        // check UNIQUE constraints
        new ArrayList<>(attributable.getPlainAttrsList()).stream().
                filter(attr -> attr.getUniqueValue() != null).
                forEach(attr -> {
                    if (plainSchemaDAO.existsPlainAttrUniqueValue(
                            anyUtils,
                            attributable.getKey(),
                            plainSchemaDAO.findById(attr.getSchema()).
                                    orElseThrow(() -> new NotFoundException("PlainSchema " + attr.getSchema())),
                            attr.getUniqueValue())) {

                        throw new DuplicateException("Duplicate value found for "
                                + attr.getSchema() + "=" + attr.getUniqueValue().getValueAsString());
                    } else {
                        LOG.debug("No duplicate value found for {}={}",
                                attr.getSchema(), attr.getUniqueValue().getValueAsString());
                    }
                });

        // update sysInfo
        if (attributable instanceof Any any) {
            OffsetDateTime now = OffsetDateTime.now();
            String who = AuthContextUtils.getWho();
            LOG.debug("Set last change date '{}' and modifier '{}' for '{}'", now, who, any);
            any.setLastModifier(who);
            any.setLastChangeDate(now);
        }
    }

    @Override
    public void deleteById(final String key) {
        findById(key).ifPresent(this::delete);
    }
}
