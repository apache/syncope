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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.syncope.core.persistence.api.attrvalue.validation.InvalidEntityException;
import org.apache.syncope.core.persistence.api.dao.AccessTokenDAO;
import org.apache.syncope.core.persistence.api.dao.DelegationDAO;
import org.apache.syncope.core.persistence.api.dao.DerSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.DynRealmDAO;
import org.apache.syncope.core.persistence.api.dao.GroupDAO;
import org.apache.syncope.core.persistence.api.entity.PlainAttrValue;
import org.apache.syncope.core.persistence.api.entity.user.User;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAJSONUser;
import org.apache.syncope.core.persistence.api.dao.JPAJSONAnyDAO;
import org.apache.syncope.core.persistence.api.dao.PlainSchemaDAO;
import org.apache.syncope.core.persistence.api.dao.RealmDAO;
import org.apache.syncope.core.persistence.api.dao.RoleDAO;
import org.apache.syncope.core.persistence.api.entity.AnyUtilsFactory;
import org.apache.syncope.core.persistence.api.entity.DerSchema;
import org.apache.syncope.core.persistence.api.entity.PlainAttrUniqueValue;
import org.apache.syncope.core.persistence.api.entity.PlainSchema;
import org.apache.syncope.core.persistence.jpa.entity.user.JPAUser;
import org.apache.syncope.core.provisioning.api.event.AnyCreatedUpdatedEvent;
import org.apache.syncope.core.spring.security.AuthContextUtils;
import org.apache.syncope.core.spring.security.SecurityProperties;
import org.springframework.context.ApplicationEventPublisher;

public class JPAJSONUserDAO extends JPAUserDAO {

    protected final JPAJSONAnyDAO anyDAO;

    public JPAJSONUserDAO(
            final AnyUtilsFactory anyUtilsFactory,
            final ApplicationEventPublisher publisher,
            final PlainSchemaDAO plainSchemaDAO,
            final DerSchemaDAO derSchemaDAO,
            final DynRealmDAO dynRealmDAO,
            final RoleDAO roleDAO,
            final AccessTokenDAO accessTokenDAO,
            final RealmDAO realmDAO,
            final GroupDAO groupDAO,
            final DelegationDAO delegationDAO,
            final SecurityProperties securityProperties,
            final JPAJSONAnyDAO anyDAO) {

        super(anyUtilsFactory,
                publisher,
                plainSchemaDAO,
                derSchemaDAO,
                dynRealmDAO,
                roleDAO,
                accessTokenDAO,
                realmDAO,
                groupDAO,
                delegationDAO,
                securityProperties);
        this.anyDAO = anyDAO;
    }

    @Override
    public List<User> findByPlainAttrValue(
            final PlainSchema schema,
            final PlainAttrValue attrValue,
            final boolean ignoreCaseMatch) {

        return anyDAO.findByPlainAttrValue(
                JPAJSONUser.TABLE, anyUtils(), schema, attrValue, ignoreCaseMatch);
    }

    @Override
    public Optional<User> findByPlainAttrUniqueValue(
            final PlainSchema schema,
            final PlainAttrUniqueValue attrUniqueValue,
            final boolean ignoreCaseMatch) {

        return anyDAO.findByPlainAttrUniqueValue(
                JPAJSONUser.TABLE, anyUtils(), schema, attrUniqueValue, ignoreCaseMatch);
    }

    @Override
    public List<User> findByDerAttrValue(
            final DerSchema schema,
            final String value,
            final boolean ignoreCaseMatch) {

        return anyDAO.findByDerAttrValue(JPAJSONUser.TABLE, anyUtils(), schema, value, ignoreCaseMatch);
    }

    @Override
    protected Pair<User, Pair<Set<String>, Set<String>>> doSave(final User user) {
        // 1. save clear password value before save
        String clearPwd = user.getClearPassword();

        // 2. save
        User merged = entityManager().merge(user);

        // 3. set back the sole clear password value
        JPAUser.class.cast(merged).setClearPassword(clearPwd);

        // 4. enforce password and account policies
        try {
            enforcePolicies(merged);
        } catch (InvalidEntityException e) {
            entityManager().remove(merged);
            throw e;
        }

        // ensure that entity listeners are invoked at this point
        entityManager().flush();

        publisher.publishEvent(new AnyCreatedUpdatedEvent<>(this, merged, AuthContextUtils.getDomain()));

        roleDAO.refreshDynMemberships(merged);
        Pair<Set<String>, Set<String>> dynGroupMembs = groupDAO.refreshDynMemberships(merged);
        dynRealmDAO.refreshDynMemberships(merged);

        return Pair.of(merged, dynGroupMembs);
    }

    @Override
    public User save(final User user) {
        anyDAO.checkBeforeSave(JPAJSONUser.TABLE, anyUtils(), user);
        return super.save(user);
    }

    @Override
    public Pair<Set<String>, Set<String>> saveAndGetDynGroupMembs(final User user) {
        anyDAO.checkBeforeSave(JPAJSONUser.TABLE, anyUtils(), user);
        return super.saveAndGetDynGroupMembs(user);
    }
}
